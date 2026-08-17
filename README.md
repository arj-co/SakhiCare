# SakhiCare

**An offline-first maternal healthcare assessment and triage platform for frontline health workers in rural settings.**

SakhiCare empowers Auxiliary Nurse Midwives (ANMs) and Accredited Social Health Activists (ASHAs) with instant clinical danger sign triage, offline case logging, and background server synchronization. It brings low-latency decision support to remote clinical encounters.

---

<div align="left">
  <img src="https://img.shields.io/badge/Android-Kotlin-004D40?style=flat-square&logo=android&logoColor=FFFFFF&labelColor=00796B" alt="Android" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-1.7.0-004D40?style=flat-square&logo=jetpackcompose&logoColor=FFFFFF&labelColor=00796B" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.0-004D40?style=flat-square&logo=kotlin&logoColor=FFFFFF&labelColor=00796B" alt="Kotlin" />
  <img src="https://img.shields.io/badge/FastAPI-0.111.0-004D40?style=flat-square&logo=fastapi&logoColor=FFFFFF&labelColor=00796B" alt="FastAPI" />
  <img src="https://img.shields.io/badge/Python-3.11+-004D40?style=flat-square&logo=python&logoColor=FFFFFF&labelColor=00796B" alt="Python" />
  <img src="https://img.shields.io/badge/PostgreSQL-16.0-004D40?style=flat-square&logo=postgresql&logoColor=FFFFFF&labelColor=00796B" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/FHIR-R4-004D40?style=flat-square&logo=hl7&logoColor=FFFFFF&labelColor=00796B" alt="FHIR R4" />
  <img src="https://img.shields.io/badge/Room-2.6.1-004D40?style=flat-square&logo=sqlite&logoColor=FFFFFF&labelColor=00796B" alt="Room" />
  <img src="https://img.shields.io/badge/WorkManager-2.9.0-004D40?style=flat-square&logo=android&logoColor=FFFFFF&labelColor=00796B" alt="WorkManager" />
</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Product Experience](#product-experience)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Design Philosophy](#design-philosophy)
- [Local Development](#local-development)
- [Future Roadmap](#future-roadmap)
- [Team](#team)
- [License](#license)

---

## Overview

SakhiCare is designed specifically for high-volume, low-connectivity healthcare environments. In rural clinical settings, frontline health workers often perform critical maternal screenings without reliable internet access. SakhiCare offers immediate rule-based danger sign calculation (Red / Amber / Green triage), local case persistence, and scheduled background sync when connectivity resumes.

---

## Features

- **Instant Clinical Danger Sign Triage** — Real-time assessment based on maternal vital signs (Blood Pressure, Haemoglobin) and qualitative danger symptoms (Bleeding, Fever, Severe Headache, Reduced Fetal Movement).
- **Clear Visual Risk Stratification**:
  - **RED**: Immediate emergency referral triggered by vaginal bleeding or high blood pressure ($\ge 140/90\text{ mmHg}$).
  - **AMBER**: High-priority observation triggered by fever or severe headache.
  - **GREEN**: Normal maternal checkup.
- **Offline-First Dashboard** — Displays connectivity status, pending sync count, quick assessment triggers, and recent case history.
- **FastAPI Sync Server** — Lightweight Python backend ready for data ingestion and FHIR bundle transformation.
- **Interoperability Ready** — Included FHIR R4 JSON bundle specs for seamless EHR integration.

---

## Product Experience

```
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│ 1. Open Dashboard│ ───> │ 2. New Assessment│ ───> │ 3. Instant Triage│
└──────────────────┘      └──────────────────┘      └──────────────────┘
         ▲                                                   │
         │                                                   ▼
┌──────────────────┐                                ┌──────────────────┐
│ 5. Backend Sync  │ <───────────────────────────── │ 4. Review Cases  │
└──────────────────┘                                └──────────────────┘
```

1. **Dashboard** — Monitor network status badge ("Offline") and pending record count.
2. **New Assessment** — Input maternal vitals (BP, Hb) and select observed danger signs.
3. **Instant Triage** — Receive immediate Red/Amber/Green color-coded referral advisories.
4. **Review Cases** — Browse local case histories under "My Cases".
5. **Backend Sync** — Tap "Sync Now" to push pending records to the FastAPI server.

---

## Architecture

```mermaid
graph TD
    classDef mobile fill:#E0F2F1,stroke:#00796B,stroke-width:1px,color:#004D40;
    classDef storage fill:#E8F5E9,stroke:#388E3C,stroke-width:1px,color:#1B5E20;
    classDef server fill:#FFF3E0,stroke:#F57C00,stroke-width:1px,color:#E65100;

    A[ANM / ASHA User]:::mobile -->|Inputs Vitals & Symptoms| B[Jetpack Compose UI]:::mobile
    B -->|Rule Engine Triage| C[Risk Assessment Card Red/Amber/Green]:::mobile
    B -->|Local Write| D[Room DB + SQLCipher Encrypted]:::storage
    D -->|Queued Items| E[WorkManager Sync Worker]:::storage
    E -->|REST API POST /sync| F[FastAPI Backend Server]:::server
    F -->|Persistence Placeholder| G[PostgreSQL Database]:::storage
    F -->|Bundle Export| H[FHIR R4 JSON Converter]:::server
```

---

## Technology Stack

| Component | Technology | Version | Purpose |
|---|---|---|---|
| **Language** | Kotlin | 2.0.0 | Primary language for Android application |
| **UI Framework** | Jetpack Compose | 1.7.0 | Declarative UI framework for modern Android UI |
| **Local Database** | Room + SQLCipher (Placeholder) | 2.6.1 | Local encrypted SQLite database layer |
| **Background Sync** | WorkManager (Placeholder) | 2.9.0 | Deferred background synchronization manager |
| **Backend Framework**| FastAPI | 0.111.0 | High-performance Python REST API server |
| **Backend Runtime**  | Python | 3.11+ | Execution engine for backend services |
| **Database Engine**  | PostgreSQL (Placeholder) | 16.0 | Production relational database engine |
| **Data Standard**   | FHIR R4 | 4.0.1 | Standardized healthcare data payload format |

---

## Design Philosophy

SakhiCare uses a clinical color system designed for high usability under sunlight and high-stress rural clinical workflows.

- **Teal Primary (`#00796B` / `#004D40`)** — Professional medical backdrop inspiring calm and confidence.
- **Red Emergency (`#D32F2F`)** — High urgency indicator for immediate maternal referral.
- **Amber Warning (`#F57C00`)** — Moderate risk indicator requiring secondary monitoring.
- **Green Normal (`#388E3C`)** — Clear confirmation of normal maternal parameters.

---

## Local Development

### Prerequisites

- **Android Studio** (Koala or newer) with JDK 17+
- **Python** 3.11+
- **pip** package installer

---

### 1. Running the FastAPI Backend

```bash
# Navigate to backend directory
cd backend

# Create virtual environment (optional)
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install requirements
pip install -r requirements.txt

# Start FastAPI dev server
uvicorn main:app --reload --port 8000
```

Verify backend health at: [http://127.0.0.1:8000/health](http://127.0.0.1:8000/health)

---

### 2. Running the Android Application

1. Open Android Studio.
2. Select **Open an Existing Project** and browse to the repository root directory.
3. Allow Gradle to sync dependencies.
4. Run the project on an Android Emulator or connected physical device (API 26+).

---

## Future Roadmap

The current repository provides a clean architecture and UI starter template. The following components are explicitly defined as future production work:

- [x] **On-Device Speech-To-Text (STT / SST)** — Natural language voice input assistant for ANMs/ASHAs to dictate patient details, vitals, and danger signs hands-free.
- [x] **Full FHIR R4 Integration Engine** — Automated bi-directional converter mapping SakhiCare assessment payloads into standard HL7 FHIR `Patient`, `Observation` (LOINC 85354-9 BP, 718-7 Hb), and `Condition` bundles.
- [x] **FastAPI Sync Server & Pytest Suite** — High-performance REST sync server with `/sync`, `/cases`, `/fhir/export/{id}`, `/voice-parse` endpoints and automated test suite.
- [ ] **SQLCipher Integration** — Encrypt Room database using real passphrase keys generated from Android Keystore.
- [ ] **WorkManager Exponential Backoff Sync** — Real HTTP transport sync engine using Retrofit + WorkManager retry policies.
- [ ] **Firebase Cloud Messaging (FCM)** — High-priority push notifications for critical patient emergency alerts.
- [ ] **PostgreSQL Database Pipeline** — Live SQLAlchemy ORM models, Alembic migrations, and database connection pools.

---

## Team

- **Aarya S**
- **Arjun S**

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
