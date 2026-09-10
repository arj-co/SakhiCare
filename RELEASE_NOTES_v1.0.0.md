# SakhiCare v1.0.0 — Production Release

### Offline Maternal Danger-Sign Screening & Emergency Response System

SakhiCare is an offline-first clinical decision support and emergency coordination system built for frontline ASHA workers operating in rural areas with unreliable connectivity.

---

### Key Features in v1.0.0

#### 1. Android Frontline ASHA App (`app/`)
* **Offline-First Storage**: Room database with SQLCipher 256-bit AES encryption (`AppDatabase`).
* **Deterministic Clinical Matrix**: Full MoHFW/WHO maternal high-risk pregnancy triage (`RED`, `AMBER`, `GREEN`) without network dependencies.
* **Voice Note to Form Intake**: Dual-action voice capture (`AudioRecordManager`) with live audio amplitude metering, offline speech-to-slot extraction (`VoiceFormOrganizer`), and worker-supervised review dialog (`VoiceNoteToFormDialog`).
* **Audit & Retention**: SHA-256 audio integrity checks and 90-day retention policies.
* **Idempotent Sync Engine**: Background `WorkManager` queue with exponential backoff and server deduplication.

#### 2. Durable Backend & API (`backend/`)
* **Durable Schema**: SQLAlchemy ORM with SQLite (dev) / PostgreSQL (prod) durability.
* **Security & RBAC**: PBKDF2-HMAC-SHA256 password hashing and JWT authentication enforcing roles (`MEDICAL_OFFICER`, `SUPERVISOR`, `DISPATCHER`, `ADMIN`).
* **Strict 404 Guarantees**: Clean RESTful contracts without fabricated fallback data.
* **FHIR R4 Interoperability**: Generates full FHIR bundles (`Patient`, `Encounter`, `Observation`, `Condition`, `CarePlan`).

#### 3. 108 Emergency Transport Coordination Board
* **Truthful Lifecycle**: `REQUESTED` ➔ `CALL_ATTEMPTED` ➔ `CONFIRMED` ➔ `EN_ROUTE` ➔ `ARRIVED` (or `FAILED`).
* **Evidence Required**: Requires call attempt notes and verified operator tickets; never fabricates ambulance IDs or ETAs.

#### 4. Multi-Channel Escalation & Privacy-Preserving SMS
* **Zero PII Plain SMS**: Transmits case ID, risk urgency, village, and callback phone. Excludes patient names from unencrypted SMS.
* **Truthful Delivery State**: Accurately records `NOT_CONFIGURED` or `SENT` rather than faking `DELIVERED` when provider receipt is absent.
* **Supervisor Fallback**: Automatically escalates unconfirmed critical cases to the Block Supervisor.

#### 5. React + Vite Care Desk Portal (`portal/`)
* **Live SSE Command Hub**: Real-time queue ingestion and updates.
* **Interactive Modules**: Urgent Queue, 108 Transport Board, Notification Center, Facilities Roster, ASHA Worker Roster, and Clinical Protocol Guidelines.

---

### Included Release Assets
* **`SakhiCare-v1.0.0-release.apk`**: Production release APK (signed, ready to install on Android devices).
* **`SakhiCare-v1.0.0-debug.apk`**: Debug APK with development logs enabled.
