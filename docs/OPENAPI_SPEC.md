# SakhiCare OpenAPI 3.1 Specification

**Service**: SakhiCare Backend API  
**Base URL**: `/api/v1` (with legacy fallback support for `/health` and `/sync/batch`)  
**Protocol Pack**: `mohfw-hrp-v1.0`  
**Authentication**: Bearer JWT (`HS256`, Supabase Auth or Local RBAC Token)

---

## 1. Authentication & Session

### `POST /api/v1/auth/login`
Authenticates a staff member (Medical Officer, Transport Dispatcher, Supervisor, or Administrator) with local credentials and issues a scoped JWT.
- **Request Body**:
  ```json
  {
    "username": "doctor_sharma",
    "password": "DoctorPass123!"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "bearer",
    "user": {
      "id": "usr_doc_01",
      "username": "doctor_sharma",
      "full_name": "Dr. Rajiv Sharma",
      "role": "MEDICAL_OFFICER",
      "facility_id": "FAC-01"
    }
  }
  ```

### `GET /api/v1/auth/me`
Retrieves the profile and role claims of the currently authenticated token bearer.
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**: Token claims (`user_id`, `username`, `role`, `facility_id`).

---

## 2. Ingestion & Case Management

### `POST /api/v1/sync/batch` (or `POST /sync/batch`)
Idempotent batch ingestion for offline encounters synced from the Android ASHA client.
- **Headers**: `Authorization: Bearer <token>` (or authenticated worker session)
- **Request Body**:
  ```json
  {
    "items": [
      {
        "idempotency_key": "idemp-case-001",
        "case_id": "SC-20260911-001",
        "patient_name": "Gita Devi",
        "age_years": 24,
        "village": "Rampur",
        "gestational_age_weeks": 32,
        "gravida": 2,
        "para": 1,
        "blood_pressure": "165/110",
        "haemoglobin": 6.8,
        "danger_signs": {
          "bleeding": true,
          "severe_headache": true
        },
        "risk_level": "RED",
        "risk_score": 85,
        "facility_id": "FAC-01",
        "worker_id": "WKR-101",
        "timestamp": 1757563200000,
        "is_demo": false
      }
    ]
  }
  ```
- **Response (200 OK)**: Ingestion summary with processed count, errors, and server timestamps.

### `GET /api/v1/cases`
Lists all maternal pregnancy cases filtered by facility permissions and query filters.
- **Headers**: `Authorization: Bearer <token>`
- **Query Parameters**:
  - `facility_id` (optional): Filter cases by facility (enforces user's assigned facility unless Admin/Supervisor).
  - `urgency` (optional): `RED`, `AMBER`, `GREEN`.
  - `status` (optional): `OPEN`, `ACKNOWLEDGED`, `CLOSED`.
  - `include_demo` (optional, boolean): Defaults to `false` in production.
- **Response (200 OK)**: Array of serialized case summaries.

### `GET /api/v1/cases/{case_id}`
Returns full clinical details, assessment timeline, latest vital signs, and audit history for a case.
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**: Complete case representation.
- **Response (403 Forbidden)**: Access denied if user is facility-scoped to another facility.
- **Response (404 Not Found)**: Case does not exist.

### `POST /api/v1/cases/{case_id}/acknowledge`
Allows an authenticated Medical Officer or Administrator to clinically review a case, submit directives, and update urgency.
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "advisory_text": "Administer oral nifedipine 10mg stat, repeat BP after 20 mins, dispatch 108 ambulance.",
    "referral_facility_id": "FAC-02",
    "updated_urgency": "RED"
  }
  ```
- **Response (200 OK)**: Updated case event and acknowledgement record.
- **Response (403 Forbidden)**: Dispatchers or unauthorized roles are rejected.

---

## 3. Audio & Voice Artifacts

### `POST /api/v1/cases/{case_id}/audio`
Uploads voice note recording associated with an encounter with SHA-256 integrity verification.
- **Headers**:
  - `Authorization: Bearer <token>`
  - `X-Idempotency-Key: <unique-uuid>`
  - `X-Audio-SHA256: <hex-sha256-digest>`
- **Form Data**: `file` (multipart/form-data audio file), `duration_seconds`, `language`
- **Response (201 Created)**: Artifact ID, storage path, content type, SHA-256 checksum.
- **Response (400 Bad Request)**: Checksum mismatch or invalid file.

### `GET /api/v1/cases/{case_id}/audio`
Streams or generates a pre-signed download URL for the voice recording.
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**: Audio byte stream or storage redirect.

### `DELETE /api/v1/cases/{case_id}/audio`
Applies HIPAA/MoHFW audio retention policy: purges audio artifact once clinical notes and triage are verified.
- **Headers**: `Authorization: Bearer <token>`
- **Response (200 OK)**: Deletion confirmation.

---

## 4. 108 Emergency Transport Coordination

### `POST /api/v1/transport/requests`
Creates a new 108 ambulance dispatch request with automatic capability-based facility recommendation.
- **Headers**: `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "case_id": "SC-20260911-001",
    "pickup_location": "Rampur Anganwadi Centre",
    "urgency": "EMERGENCY_RED",
    "primary_clinical_condition": "Severe Anemia & Antepartum Bleeding"
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "transport_id": "TR-108-729104",
    "case_id": "SC-20260911-001",
    "status": "REQUESTED",
    "recommended_facility": {
      "id": "FAC-02",
      "name": "Kalyanpur Community Health Centre",
      "tier": "CHC",
      "capabilities": ["BLOOD_BANK", "OBGYN_ON_DUTY", "SURGICAL_OT"],
      "rationale": "High transfusion risk requires Blood Bank capable facility"
    }
  }
  ```

### `PATCH /api/v1/transport/requests/{transport_id}/status`
Updates transport coordination lifecycle state.
- **Permitted States**: `REQUESTED` -> `CALL_ATTEMPTED` -> `CONFIRMED` -> `EN_ROUTE` -> `ARRIVED` -> `FAILED`
- **Request Body**:
  ```json
  {
    "status": "CONFIRMED",
    "vehicle_number": "BR-01-GA-1081",
    "driver_phone": "+919876543299",
    "call_notes": "108 dispatch accepted. Ambulance rolling from Kalyanpur depot."
  }
  ```
- **Response (200 OK)**: Updated transport state.

---

## 5. Escalation & SMS Notifications

### `POST /api/v1/notifications/callbacks/sms`
Webhook callback receiver for SMS delivery receipts from telecommunication gateways. Updates delivery status without modifying clinical acknowledgement status.
- **Request Body**:
  ```json
  {
    "gateway_message_id": "gw-msg-88123",
    "delivery_status": "DELIVERED",
    "delivered_at": "2026-09-11T09:30:00Z"
  }
  ```
- **Response (200 OK)**: Acknowledged delivery record update.

---

## 6. Clinical Protocol & Governance

### `GET /api/v1/protocol/metadata`
Provides verifiable clinical governance metadata for auditing triage algorithms.
- **Response (200 OK)**:
  ```json
  {
    "rule_pack_version": "mohfw-hrp-v1.0",
    "guideline_source": "Government of India MoHFW High-Risk Pregnancy Guidelines & WHO IMPAC",
    "clinical_validation_status": "CLINICALLY_VALIDATED",
    "reviewed_by": "Dr. Rajiv Sharma (Obstetric Medicine Lead)",
    "last_reviewed_at": "2026-09-01T00:00:00Z"
  }
  ```

---

## 7. Demo Data Lifecycle

### `POST /api/v1/demo/seed`
Seeds deterministic clinical training records (`is_demo=True`) for drills and offline exercises.
- **Headers**: `Authorization: Bearer <admin_token>`
- **Response (200 OK)**: Summary of seeded demo records.

### `POST /api/v1/demo/purge`
Completely cleans and purges all records marked with `is_demo=True`, restoring pure production state.
- **Headers**: `Authorization: Bearer <admin_token>`
- **Response (200 OK)**: Summary of purged records.

---

## 8. Real-Time & Interoperability

### `GET /api/v1/cases/stream`
Server-Sent Events (SSE) feed providing real-time triage updates to the Care Desk portal with automatic heartbeat pinging every 15 seconds.

### `GET /fhir/export/{patient_id}`
Exports patient encounter as a compliant HL7 FHIR R4 Bundle with LOINC-coded observations (`85354-9` Blood Pressure panel, `718-7` Haemoglobin).
