# SakhiCare v2.0.0 — Major Platform Release

### End-to-End Maternal Triage, Voice-to-Form Intake & 108 Emergency Coordination

SakhiCare v2.0.0 is the major release of the frontline maternal healthcare and emergency care response platform. Engineered specifically for ASHA workers in zero/low-connectivity environments, it provides deterministic clinical triage, secure voice-assisted intake, high-concurrency backend durability, and a human-centered Eyra-styled Care Desk portal.

---

### Key Capabilities in v2.0.0

#### 1. Android Frontline ASHA App (`app/`)
* **Offline-First Room Encryption**: SQLCipher 256-bit AES encrypted SQLite database integrated with Android Keystore.
* **MoHFW/WHO Triage Engine**: Zero-network deterministic clinical matrix classifying cases as `RED` (Critical), `AMBER` (Moderate), or `GREEN` (Normal).
* **Voice Note to Form Intake**: Dual-action voice capture (`AudioRecordManager`) with visual amplitude metering, offline speech-to-slot field extraction, and clinician-in-the-loop verification (`VoiceNoteToFormDialog`).
* **WorkManager Sync Engine**: Durable background queue featuring exponential backoff, SHA-256 integrity validation, and server deduplication.

#### 2. Durable Backend & FHIR R4 Engine (`backend/`)
* **PostgreSQL / SQLite Storage**: SQLAlchemy ORM schema supporting full ACID transaction guarantees.
* **RBAC & Security**: PBKDF2-HMAC-SHA256 password hashing with JWT Bearer authentication enforcing `MEDICAL_OFFICER`, `DISPATCHER`, `SUPERVISOR`, and `ADMIN` role permissions.
* **FHIR R4 Standardized Output**: Instant export of compliant FHIR bundles (`Patient`, `Encounter`, `Observation`, `Condition`, `CarePlan`).

#### 3. 108 Emergency Transport Coordination
* **Truthful Lifecycle State Machine**: `REQUESTED` ➔ `CALL_ATTEMPTED` ➔ `CONFIRMED` ➔ `EN_ROUTE` ➔ `ARRIVED` (or `FAILED`).
* **Operator Ticket Verification**: Strict state machine requiring call logs and ticket numbers without fabricated ambulance IDs.

#### 4. Privacy-Preserving SMS & Escalation Engine
* **Zero PII Plain SMS Delivery**: Outbound SMS payloads strip patient names, transmitting only anonymized Case ID, Risk Urgency, Village, and Callback Phone.
* **Automated Escalation**: Automatic fallback escalation to Block Supervisor when critical cases remain unconfirmed.

#### 5. React + Vite Care Desk Portal (`portal/`)
* **Eyra Modern Design System**: High-contrast, human-centered UI with DM Serif Display typography, medical teals, and real-time Server-Sent Events (SSE).
* **Command Hub**: Modules for Urgent Triage Queue, 108 Transport Dispatch, Notification Center, Facilities & ASHA Rosters, and Clinical Guidelines.

---

### Verification & Quality Assurance
* **Android Test Suite**: 41/41 Unit tests passed (`./gradlew test`).
* **Backend Test Suite**: 25/25 Pytest integration tests passed (`pytest`).
* **Build Target**: Clean Kotlin compilation with Gradle & OpenJDK 17.

---

### Release Assets
* **`SakhiCare_final.apk`**: Production Release Android APK (Version 2.0.0).
* **`SakhiCare-v2.0.0-release.apk`**: Versioned release APK package.
* **`SakhiCare-v2.0.0-debug.apk`**: Debug APK package with development logs enabled.
