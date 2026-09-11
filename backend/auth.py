import os
import time
import jwt
from typing import Optional, List
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from sqlalchemy import text
from database import SessionLocal

import hashlib
import hmac
import secrets

APP_ENV = os.getenv("APP_ENV", "development").lower()
TEST_MODE = os.getenv("SAKHICARE_TEST_MODE", "false").lower() == "true"
AUTH_PROVIDER = os.getenv("AUTH_PROVIDER", "supabase" if APP_ENV == "production" else "local")
SECRET_KEY = os.getenv("SAKHICARE_JWT_SECRET")
SUPABASE_JWT_SECRET = os.getenv("SUPABASE_JWT_SECRET")
if APP_ENV == "production" and AUTH_PROVIDER == "supabase" and not SUPABASE_JWT_SECRET:
    raise RuntimeError("Production Supabase auth requires SUPABASE_JWT_SECRET")
if not SECRET_KEY:
    SECRET_KEY = SUPABASE_JWT_SECRET or "sakhicare-development-secret-key-must-be-at-least-32-bytes"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_SECONDS = 86400 * 7  # 7 days

security = HTTPBearer(auto_error=False)


class TokenData(BaseModel):
    user_id: str
    username: str
    role: str
    facility_id: Optional[str] = None


def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    key = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt.encode("utf-8"), 100000)
    return f"pbkdf2_sha256${salt}${key.hex()}"


def verify_password(plain_password: str, hashed_password: str) -> bool:
    if not hashed_password:
        return False
    if hashed_password.startswith("pbkdf2_sha256$"):
        parts = hashed_password.split("$")
        if len(parts) == 3:
            _, salt, key_hex = parts
            computed = hashlib.pbkdf2_hmac("sha256", plain_password.encode("utf-8"), salt.encode("utf-8"), 100000).hex()
            return hmac.compare_digest(computed, key_hex)
    return plain_password == hashed_password


def create_access_token(user_id: str, username: str, role: str, facility_id: Optional[str] = None) -> str:
    now = int(time.time())
    payload = {
        "sub": user_id,
        "username": username,
        "role": role,
        "facility_id": facility_id,
        "iat": now,
        "exp": now + ACCESS_TOKEN_EXPIRE_SECONDS
    }
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)


def decode_access_token(token: str) -> TokenData:
    try:
        signing_key = SUPABASE_JWT_SECRET if AUTH_PROVIDER == "supabase" else SECRET_KEY
        options = {"verify_aud": False} if AUTH_PROVIDER == "supabase" else {}
        payload = jwt.decode(token, signing_key, algorithms=[ALGORITHM], options=options)
        user_id: str = payload.get("sub")
        username: str = payload.get("username")
        app_metadata = payload.get("app_metadata") or {}
        user_metadata = payload.get("user_metadata") or {}
        role: str = payload.get("role") or app_metadata.get("role") or user_metadata.get("role")
        facility_id: Optional[str] = payload.get("facility_id") or app_metadata.get("facility_id") or user_metadata.get("facility_id")
        if user_id is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token claims"
            )
        return TokenData(user_id=user_id, username=username or user_id, role=role or "AUTHENTICATED", facility_id=facility_id)
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired authentication token"
        )


def resolve_supabase_profile(token_data: TokenData) -> TokenData:
    """Resolve role/facility from the server-owned profile table, not client claims."""
    try:
        with SessionLocal() as db:
            profile = db.execute(
                text("""
                    select full_name, role, facility_id
                    from public.user_profiles
                    where id = :user_id and is_active = true
                """),
                {"user_id": token_data.user_id},
            ).mappings().first()
    except Exception as exc:
        if APP_ENV == "production":
            raise HTTPException(status_code=503, detail="User profile service unavailable") from exc
        return token_data

    if not profile:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Active SakhiCare user profile is required")

    return TokenData(
        user_id=token_data.user_id,
        username=profile["full_name"] or token_data.username,
        role=profile["role"],
        facility_id=profile["facility_id"],
    )


async def get_current_user(credentials: Optional[HTTPAuthorizationCredentials] = Depends(security)) -> TokenData:
    if not credentials:
        if not TEST_MODE:
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Bearer authentication required")
        # Explicit test-only compatibility. Never enabled by a normal app start.
        return TokenData(user_id="test-user", username="test-user", role="ADMIN", facility_id=None)
    token_data = decode_access_token(credentials.credentials)
    return resolve_supabase_profile(token_data) if AUTH_PROVIDER == "supabase" else token_data


def require_roles(*allowed_roles: str):
    def role_checker(current_user: TokenData = Depends(get_current_user)):
        if current_user.role not in allowed_roles and current_user.role != "ADMIN":
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Access denied: Requires one of roles {allowed_roles}, but current user has {current_user.role}"
            )
        return current_user
    return role_checker
