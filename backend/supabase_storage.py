"""Small, server-only adapter for private Supabase Storage."""

import os
from typing import Optional
import httpx

SUPABASE_URL = os.getenv("SUPABASE_URL", "").rstrip("/")
SUPABASE_SERVICE_ROLE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY", "")
SUPABASE_STORAGE_BUCKET = os.getenv("SUPABASE_STORAGE_BUCKET", "sakhicare-audio")


def configured() -> bool:
    return bool(SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY and SUPABASE_STORAGE_BUCKET)


def _headers(content_type: Optional[str] = None) -> dict[str, str]:
    headers = {
        "Authorization": f"Bearer {SUPABASE_SERVICE_ROLE_KEY}",
        "apikey": SUPABASE_SERVICE_ROLE_KEY,
    }
    if content_type:
        headers["Content-Type"] = content_type
    return headers


def upload(path: str, content: bytes, content_type: str) -> str:
    if not configured():
        raise RuntimeError("Supabase Storage is not configured")
    response = httpx.post(
        f"{SUPABASE_URL}/storage/v1/object/{SUPABASE_STORAGE_BUCKET}/{path}",
        content=content,
        headers={**_headers(content_type), "x-upsert": "true"},
        timeout=30.0,
    )
    if response.status_code not in (200, 201):
        raise RuntimeError(f"Supabase Storage upload failed ({response.status_code}): {response.text[:300]}")
    return path


def signed_url(path: str, expires_in: int = 300) -> str:
    if not configured():
        raise RuntimeError("Supabase Storage is not configured")
    response = httpx.post(
        f"{SUPABASE_URL}/storage/v1/object/sign/{SUPABASE_STORAGE_BUCKET}",
        json={"expiresIn": expires_in, "paths": [path]},
        headers={**_headers(), "Content-Type": "application/json"},
        timeout=10.0,
    )
    if response.status_code not in (200, 201):
        raise RuntimeError(f"Supabase Storage signing failed ({response.status_code}): {response.text[:300]}")
    signed = response.json().get("signedURL") or response.json().get("signedUrl")
    if not signed:
        raise RuntimeError("Supabase Storage did not return a signed URL")
    return signed if signed.startswith("http") else f"{SUPABASE_URL}/storage/v1{signed}"


def delete(path: str) -> None:
    if not configured():
        raise RuntimeError("Supabase Storage is not configured")
    response = httpx.delete(
        f"{SUPABASE_URL}/storage/v1/object/{SUPABASE_STORAGE_BUCKET}",
        json={"prefixes": [path]},
        headers={**_headers(), "Content-Type": "application/json"},
        timeout=10.0,
    )
    if response.status_code not in (200, 204):
        raise RuntimeError(f"Supabase Storage delete failed ({response.status_code}): {response.text[:300]}")
