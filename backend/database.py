import os
from sqlalchemy import create_engine, text
from sqlalchemy.orm import declarative_base, sessionmaker

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
os.makedirs(DATA_DIR, exist_ok=True)

APP_ENV = os.getenv("APP_ENV", "development").lower()
DEFAULT_SQLITE_URL = f"sqlite:///{os.path.join(DATA_DIR, 'sakhicare_durable.db')}"
DATABASE_URL = os.getenv("SUPABASE_DATABASE_URL") or os.getenv("DATABASE_URL") or DEFAULT_SQLITE_URL

if APP_ENV == "production" and not DATABASE_URL.startswith(("postgresql://", "postgres://")):
    raise RuntimeError("Production requires SUPABASE_DATABASE_URL or a PostgreSQL DATABASE_URL")

# Handle Heroku/Railway postgres:// vs postgresql:// dialect
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}

engine = create_engine(
    DATABASE_URL,
    connect_args=connect_args,
    echo=False
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    import models  # Ensure all models are registered
    # Production schema is managed by Supabase migrations. Local development/tests
    # may still create the compatibility schema automatically.
    if APP_ENV != "production":
        Base.metadata.create_all(bind=engine)
        _apply_local_compatibility_migrations()


def _apply_local_compatibility_migrations():
    """Additive migrations for the checked-in local SQLite dev database.

    Production is migrated by Supabase SQL; this only keeps an existing local
    developer database usable after adding location and Storage metadata.
    """
    if not DATABASE_URL.startswith("sqlite"):
        return
    additions = {
        "facilities": {
            "latitude": "REAL",
            "longitude": "REAL",
            "capabilities_json": "TEXT NOT NULL DEFAULT '[]'",
        },
        "pregnancy_cases": {
            "latitude": "REAL",
            "longitude": "REAL",
            "location_accuracy_m": "REAL",
            "location_captured_at": "INTEGER",
        },
        "voice_artifacts": {
            "storage_path": "TEXT",
        },
    }
    with engine.begin() as connection:
        for table, columns in additions.items():
            existing = {row[1] for row in connection.execute(text(f"PRAGMA table_info({table})"))}
            for column, column_type in columns.items():
                if column not in existing:
                    connection.execute(text(f"ALTER TABLE {table} ADD COLUMN {column} {column_type}"))
