# SakhiCare remodel plan

## Product decision

Remodel SakhiCare around one operational promise:

> Help an ASHA worker identify a maternal danger sign offline, start the approved first response, and hand the case to a medical officer and transport coordinator with a traceable status.

The Android app is the point-of-care tool. A separate React + Vite web portal is the Care Desk for authenticated medical officers, supervisors, and dispatch coordinators. The portal must never be presented as a live command centre unless the backend has durable data, authenticated access, and real delivery acknowledgements.

### Simplified voice decision

Do not build or claim a local LLM, clinical speech model, or voice-based diagnosis. Voice is a structured intake shortcut:

1. Record the worker's audio with explicit consent.
2. Produce a transcript when a speech service is available; otherwise retain the audio as queued evidence.
3. Organize recognized content into editable fields such as patient/case reference, symptoms, BP, Hb, gestational age, location, transport constraints, and notes.
4. Show the worker exactly what was extracted and let them correct or confirm it.
5. Store the raw audio, transcript, structured fields, language, timestamps, and processing status together.
6. Send the audio and confirmed fields to the Care Desk through the same encrypted outbox. Never infer a danger sign from an unconfirmed transcript, and never auto-submit a clinical result from speech alone.

The clinical rule engine consumes confirmed structured fields only. It is separate from transcription and does not need an LLM.

## Current implementation after the production cleanup

- Runtime starts with an empty local database and never seeds facilities, users, workers, or patient cases.
- Production case reads come from durable Supabase/Postgres records; missing cases return `404`.
- Portal access uses Supabase Auth plus `public.user_profiles`; role switching and hardcoded demo accounts are removed.
- ASHA onboarding requires an issued Care Desk account, an assigned facility, consent, and a local PIN. The access token is stored in encrypted preferences and used only for authenticated sync.
- Room is backed by SQLCipher with an Android Keystore-wrapped passphrase and additive migrations. Location is captured only with granted permission and retained with accuracy/timestamp provenance.
- Audio is retained as a private Storage artifact only after authenticated upload. Unconfigured speech processing returns an explicit unavailable state rather than a canned transcript.
- Missing SMS/OneSignal credentials return `NOT_CONFIGURED`; live provider IDs are required before a notification is called sent.
- Release builds no longer fall back to debug signing. CI must provide the deployment API/Supabase properties and a protected release keystore.

The remaining go-live work is operational and cannot be safely fabricated in source control:

1. Apply `supabase/migrations/0002_workers_and_auth_alignment.sql` to the connected Supabase project. The checked-in migration is ready, but the local Supabase CLI session is linked to a different project and the browser SQL editor was not available for a safe remote write.
2. Provision real Supabase Auth users, reviewed `public.user_profiles`, matching `public.workers`, referral facilities, and notification recipients.
3. Add SMS, OneSignal, Supabase database/JWT/Storage secrets to the backend secret manager. Production startup now fails fast if the required values are missing.
4. Obtain formal clinical review for the rule pack and translated emergency copy.
5. Build the signed release APK with the organisation's protected release keystore. The repository only produces an unsigned release when those credentials are absent.

## Historical implementation audit

### Real enough to preserve as a foundation

- Native Android/Jetpack Compose shell and a basic navigation model.
- An on-device rule engine exists in `app/src/main/java/com/sakhicare/app/data/TriageEngine.kt`.
- The rule engine covers BP, haemoglobin, bleeding, fever, headache, reduced fetal movement, and some compound risk.
- Android speech recognition is requested with `EXTRA_PREFER_OFFLINE`; availability still depends on the phone's installed speech service and language pack.
- The backend has FastAPI routes, a basic FHIR bundle converter, a deterministic clinical rule engine, and an SSE event shape that can be reused.
- The React/Vite Care Desk portal implements the operator workflow: queue, filters, case detail, advisory, and transport actions.

### Demo-only or falsely represented as production

| Area | Evidence in the repository | Actual behaviour | Correct fix |
|---|---|---|---|
| App launch/onboarding | `MainActivity.kt:72` starts at `Screen.Dashboard`; `OnboardingScreen.kt:50-51` contains prefilled identity values | Onboarding is unreachable in the normal app flow, and the worker profile is not persisted or registered | Add persisted first-run state, real worker registration, phone/OTP or approved facility-code authentication, language/device permissions, and a clear consent step |
| Local database | `AppDatabasePlaceholder.kt`; Room/SQLCipher dependencies are commented out in `app/build.gradle.kts:65-68` | No Room database and no encrypted SQLite; data is not durable | Implement Room entities/DAOs, SQLCipher or an equivalent supported encrypted store, Android Keystore key wrapping, migrations, and lock/timeout behaviour |
| Secure key storage | `SecureStorageStub.kt` always returns an alias and `true` | It does not create, retrieve, or validate a hardware-backed key | Generate a Keystore AES key, wrap a random database passphrase, fail closed when key access fails, and add instrumentation tests |
| Case storage | `PatientRepository.kt` uses `mutableStateListOf` with five seeded cases | Cases disappear on process death; the initial dashboard is fabricated | Replace repository with Room-backed flows; ship an empty state in production and keep demo seed data behind an explicit demo build flag |
| Sync | `SyncWorkerPlaceholder.kt` returns a string; `PatientRepository.syncAllPending()` only changes `Pending` to `Synced` | No HTTP request, retry, idempotency, conflict handling, or server acknowledgement | Implement WorkManager + network constraint + authenticated API client + outbox states (`PENDING`, `UPLOADING`, `ACKED`, `FAILED`, `NEEDS_REVIEW`) |
| Backend persistence | `backend/main.py:37-38` uses `synced_cases_db` | Server restart loses every case | Add PostgreSQL in production, SQLite only for local development, SQLAlchemy models, Alembic migrations, indexes, and backups |
| Seeded server data | `backend/main.py:101-161` seeds Sunita/Meena/Pooja | Portal opens with invented cases that look operational | Remove startup seeding from production; use a separate demo fixture/command and a visible “Demo mode” label |
| Unknown case lookup | `backend/main.py:580-591` returns a fabricated Sunita/Rampur RED bundle for any missing ID | A nonexistent patient can appear to have a clinical record | Return `404` for unknown cases and test it |
| Push notifications | `onesignal_service.py:148-171` silently switches to `simulated_success` with a fake ID | “Delivered” can mean “not configured” | Make delivery states explicit (`NOT_CONFIGURED`, `QUEUED`, `SENT`, `FAILED`); never report simulated delivery as success in production |
| Transport dispatch | `backend/main.py:348-375` constructs a status string and broadcasts it | No 108/ambulance provider call, driver acknowledgement, or ETA source | Integrate an approved dispatch channel or use a manual call workflow; capture who called, when, destination, confirmation, and last-known status |
| SMS | No SMS provider, sender identity, delivery receipt, or consent model exists | SMS is not implemented | Add a provider adapter, template registry, rate limit, delivery callbacks, PII minimisation, opt-in/configuration, and an audit record; retain a phone-call fallback |
| Speech | `VoiceHelper.kt` is regex/keyword parsing; `speech_llm.py:168-174` returns a fixed transcript for any WAV | It is not a trustworthy transcription pipeline and can invent patient/vitals when audio is unrecognised | Remove the Speech-LLM path; implement audio capture + transcript/field extraction as a draft, with explicit confirmation and raw-audio upload |
| “Speech-LLM” | `speech_llm.py` uses canned fallback names, village, BP and Hb | The branding overstates capability and creates unsafe fabricated data | Delete or quarantine the module; call the feature “Voice note to form” and show `Unprocessed`, `Draft`, `Confirmed`, or `Failed` states |
| AI copilot | `SakhiAiCopilotScreen.kt:90-128` uses keyword branches and canned clinical text | It is a static FAQ and adds risk without helping the core workflow | Remove it from the primary product path; replace it with a reviewed danger-sign/help library and case-linked medical-officer escalation |
| Clinical advice | Backend and UI include medication/procedure-like advice such as paracetamol, IV access, oxytocin, and transfusion language | The app may be interpreted as prescribing or directing procedures beyond ASHA scope | Have a named clinical reviewer approve every action card; split ASHA-safe first response from clinician-only actions; version and sign every protocol |
| Clinical logic drift | Android `TriageEngine.kt` and backend `triage_engine.py` implement separate thresholds, factors, and action text; Kotlin has four danger signs while the backend also accepts convulsions/vision loss | The same encounter can produce different risk and guidance on phone vs server | Define one versioned rule-pack contract, generate shared fixtures, run parity tests, and store the rule-pack version with every assessment |
| Network state | `MainActivity.kt:76-78,118` exposes a manual online/offline toggle | A demo control can override real connectivity and imply a sync that did not happen | Remove from release builds; use a test-only injection and show real connectivity plus last successful sync |
| Alert portal | `care_desk.py` embeds a large vanilla HTML page in the API | There is no separate React/Vite portal, role-based access, or durable operator session | Create `portal/` as a React + Vite app, use typed API contracts, authenticated routes, and a backend-for-frontend or API gateway |
| SSE “live” status | SSE is backed by process-local queues | It works only for connected browser tabs on one process and has no replay cursor | Persist events, expose `Last-Event-ID`/polling fallback, and use SSE only as a UI acceleration layer |
| FHIR | Conversion exists, but export can use fabricated data and there is no validation gate on sync | Interoperability is a mapping demo, not a governed exchange | Validate bundles, record source timestamps and terminology, and make export read only from an existing case |
| Security | `allow_origins=["*"]`; no visible auth, roles, audit trail, or device binding | Sensitive maternal data is exposed as an open demo API | Add OIDC/JWT or a controlled facility auth flow, RBAC, tenant/facility scoping, TLS, audit events, request IDs, and least-privilege service credentials |
| Tests | `test_offline_queue.py` appends every item without a failure path; audio tests use dummy WAV; pytest is declared but not installed in the current environment | Several tests prove mocked behaviour, not integration | Add unit, property, contract, instrumentation, sync-failure, encryption, and end-to-end tests using explicit fakes with assertions about delivery state |

## Target real-world workflow

1. **Worker setup**: select language, verify ASHA identity and facility assignment, set a local PIN/biometric unlock, grant microphone/notification permissions, download the approved protocol pack and language pack, and show “works offline” with the last pack version.
2. **Start encounter**: search for an existing woman by local ID/phone/name, or create a minimum new record. Avoid collecting unnecessary personally identifying information. Explain consent and how the record is used.
3. **Screen in the right order**: ask immediate danger signs first (heavy bleeding, fits/unconsciousness, severe headache/blurred vision, severe breathlessness/chest pain, fever with severe illness, labour/water breaking early, absent/reduced fetal movement, severe abdominal pain), then gestational age, BP, pulse/temperature if available, Hb if available, pregnancy history, and travel constraints.
4. **Capture with provenance**: every value stores source (`MANUAL`, `VOICE`, `DEVICE`), timestamp, unit, and confirmation state. Voice fills a draft; the worker confirms or corrects each field.
5. **Offline decision**: a versioned, deterministic rule pack produces `RED`, `AMBER`, or `GREEN`, the triggered signs, missing measurements, and an action card. Missing data must never silently become normal defaults.
6. **First response**: show only approved ASHA-safe actions, “do not delay referral” language, and a prominent call/transport step for RED. The worker can mark “family informed”, “medical officer called”, and “transport requested”.
7. **Queue and handoff**: save immediately to encrypted local storage. If offline, show exactly what is queued and the next retry condition. If online, upload with an idempotency key and wait for server acknowledgement.
8. **Care Desk response**: portal places the case in a queue ordered by urgency and age. An authorized medical officer acknowledges, adds guidance, selects/refers a facility, and records transport coordination. Every state change is timestamped and attributed.
9. **Worker follow-through**: advisories and case status return to the worker when connected; SMS is a secondary notification to the configured medical officer, not proof of clinical receipt. The ASHA can continue updating the case offline.
10. **Closure**: capture arrival/referral outcome, failed transport/escalation reason, and follow-up due date. Use this for quality improvement, not punitive worker scoring.

## Recommended repository shape

```text
app/                         Android ASHA app
  .../data/db/               Room entities, DAOs, migrations, encrypted DB
  .../data/sync/             Outbox, API client, WorkManager
  .../clinical/              Versioned rule pack and action catalogue
  .../onboarding/            Registration, identity, permissions, local auth
portal/                      React + Vite Care Desk portal
backend/                     FastAPI API, auth, domain services, persistence
  models/                    SQLAlchemy models and migrations
  services/                  triage, sync, notification, SMS, dispatch, audit
  api/                       versioned routes and schemas
shared/                      OpenAPI-generated types / fixture contracts
```

## Data model to implement first

- `Worker`: id, name, role, phone, facility_id, locale, status, last_seen_at.
- `Facility`: id, name, type, catchment_area, lat/lon, capabilities, contact routes.
- `PregnancyCase`: id, local_id, minimal demographics, gestational_age, worker_id, facility_id, created_at.
- `Assessment`: id, case_id, observations, danger_signs, source/provenance, rule_pack_version, risk_level, rationale, created_at.
- `Observation`: assessment_id, type (`BP`, `HB`, `TEMP`, `PULSE`, `FETAL_MOVEMENT`, etc.), value, unit, measured_at, source, confidence, confirmed_by_worker.
- `VoiceArtifact`: id, case_id, local_audio_uri, encrypted_blob/reference, language, duration, transcript, processing_status, confirmed_at, uploaded_at, retention_due_at.
- `Referral`: assessment_id, destination_facility_id, reason, urgency, worker_actions, requested_at, acknowledged_at, arrival_at.
- `TransportRequest`: referral_id, channel, provider_reference, requested_by, status, eta, confirmation, last_updated_at.
- `Notification`: case_id, recipient, channel, template_id, payload_hash, status, provider_id, sent_at, delivered_at, failure_reason.
- `CaseEvent`: case_id, event_type, actor_id, occurred_at, metadata; append-only audit stream.
- `OutboxItem`: local_id/idempotency_key, entity_type, payload, attempts, next_attempt_at, status, last_error.

## Implementation order

### Phase 0 — Truth and safety baseline

- Remove production claims that are not implemented: “100% offline AI”, “live”, “dispatched”, “doctor notified”, and “FHIR-ready” unless their exact state is true.
- Keep test fixtures outside normal startup; production has no demo mode or seeded records.
- Freeze a clinical scope with a named reviewer; create a protocol catalogue with version, source, approval date, and expiry/review date.
- Expand danger-sign and missing-data states before polishing the UI.

**Exit check:** an evaluator can tell whether every displayed record, alert, transport state, and notification is real, queued, failed, or demo.

### Phase 1 — Android offline core

- Add Room, KSP, WorkManager, Retrofit/OkHttp, and the chosen SQLCipher integration using versions compatible with the current Android/Kotlin toolchain.
- Implement `Worker`, `PregnancyCase`, `Assessment`, `OutboxItem`, and `CaseEvent` entities plus migrations.
- Implement Keystore-backed key creation and encrypted database opening; wipe only via an explicit administrative reset flow.
- Replace `PatientRepository` with repositories backed by `Flow`/`StateFlow`.
- Remove default vitals (`120/80`, `11.0`) and require explicit “not measured” values.
- Align the Android and backend clinical contract: include gestational age, pregnancy history, temperature/pulse when available, convulsions/vision changes, severe abdominal pain, labour/water breaking, and travel constraints; keep `not measured` distinct from `normal`.
- Add real sync state and retry UI. Uploads must be idempotent and never become `Synced` before server acknowledgement.

**Exit check:** kill/restart the app offline and all cases remain; inspect the database file and verify it cannot be opened without the key; simulate failures and verify items remain queued.

### Phase 2 — Real onboarding and encounter flow

- Launch onboarding based on persisted local state.
- Register the worker against the backend and facility roster. Use a mock provider only in demo builds.
- Add language selection before content, local auth, permissions, protocol/language-pack download, consent, and a short guided practice assessment.
- Build the assessment as a progressive, one-question-at-a-time flow with “I do not know/not measured”, repeat BP, gestational age, and confirmation screens.
- Add “Voice note to form”: record audio, attach it to the draft case, transcribe/organize when possible, highlight extracted fields, and require confirmation before triage.

**Exit check:** a new worker can complete setup without prefilled names, complete a realistic red-flag encounter offline, and understand the next action without a trainer explaining the screen.

### Phase 3 — Durable backend and secure API

- Add PostgreSQL + SQLAlchemy + Alembic; keep SQLite only as a local developer profile.
- Add auth, RBAC, facility scoping, request validation, rate limiting, audit events, CORS allow-list, TLS deployment, and secret management.
- Replace process-local cases and SSE queues with durable records and a replayable event/outbox mechanism.
- Make `/cases/{id}` return `404` for unknown cases; remove demo fallbacks from all APIs.
- Version API contracts under `/api/v1` and generate Android/portal types from OpenAPI.

**Exit check:** restart the backend and all cases/events remain; an ASHA cannot read another facility's cases; every mutation has an actor and audit event.

### Phase 4 — React + Vite Care Desk portal

- Create `portal/` with TypeScript, React, Vite, a query/cache layer, typed API client, route guards, and a responsive operator layout.
- Roles: Medical Officer, Supervisor, Dispatcher, and Administrator. The admin can manage workers/facilities/templates; clinical users can only see assigned facilities; dispatcher sees transport fields without unnecessary clinical detail.
- Screens: sign-in, urgent queue, all cases, case timeline, worker directory, facility directory, transport board, notifications, audit log, and protocol/version settings.
- Urgent queue must show severity, minutes since received, worker, location, vital triggers, acknowledgement state, and next action.
- Detail view must support acknowledge, clinical guidance, referral recommendation, transport request, contact attempt, status changes, and closure, all with confirmation and audit.
- Use SSE for low-latency updates with reconnect and polling fallback; never rely on SSE as the source of truth.

**Exit check:** two browser sessions see a new acknowledged case; refresh/reconnect does not lose it; role restrictions are enforced server-side, not only in the UI.

### Phase 5 — Voice upload, alerts, SMS, and transport coordination

- Upload raw audio as a case attachment with resumable transfer, checksum, encryption, retention policy, and access audit.
- Keep transcript and structured fields separately so the medical officer can compare the organized form with the original recording.
- If transcription fails, send the audio and mark the case `Voice review needed`; do not fabricate fields or block the worker from submitting a manually completed case.

- Define escalation policy: RED creates a portal queue item and a configured medical-officer alert; AMBER creates a review task; GREEN is stored without interruptive alerting.
- Add notification adapters behind one interface: in-app/push, SMS, and optional voice call. Every adapter records queued/sent/delivered/failed/unknown.
- SMS must contain minimal data: case reference, urgency, village/area, triggered sign summary, and callback number; do not include a full name or detailed clinical record by default.
- Add delivery callbacks, retry/backoff, duplicate suppression, quiet-hour rules for non-RED alerts, and a visible “SMS not confirmed” state.
- For transport, start with a manual call-and-confirm workflow if no approved API is available. Never invent an ambulance ID or ETA.

**Exit check:** a RED case produces an auditable alert attempt; a failed provider is visible and escalates to the next channel; the portal cannot display “dispatched” without a real confirmation or explicitly labelled manual entry.

### Phase 6 — Clinical validation and release hardening

- Create a labelled synthetic test set for each danger sign, combinations, gestational stage, missing data, malformed voice, duplicate submission, and offline recovery.
- Obtain clinical sign-off for rule thresholds, action language, translations, and referral facility mapping.
- Add accessibility, low-end Android, small-screen, slow-network, encryption, backup/restore, and disaster-recovery tests.
- Provide a report that clearly separates validated clinical rules from future ML; do not claim model accuracy without a held-out evaluation set.

## Recommended implementation plans

Use four implementation plans. Four is enough to keep the work understandable without splitting tightly coupled work into artificial micro-projects:

1. **Plan 1 — Offline ASHA foundation**: real onboarding, local auth, Room + SQLCipher/Keystore, case/assessment schema, progressive assessment, deterministic triage, and truthful offline states.
2. **Plan 2 — Voice note to form**: audio capture, consent, encrypted local attachment, transcript/field organizer, editable confirmation, resumable upload, retention, and failure handling. No local LLM.
3. **Plan 3 — Care Desk portal and durable backend**: PostgreSQL, authenticated API, worker/facility roles, idempotent sync, React + Vite urgent queue, case timeline, medical-officer acknowledgement, and audit log.
4. **Plan 4 — Escalation and release hardening**: SMS provider, manual/real transport workflow, notification delivery states, facility routing, clinical validation, security/accessibility testing, demo-mode removal, and deployment.

Each plan should have its own checklist, test cases, and acceptance demo. Do not begin Plan 3 until Plan 1 can preserve an offline case across app restart, and do not begin external SMS/transport work until Plan 3 has durable case state and audit events.

## Immediate next sprint

1. Remove seeded cases and fabricated fallbacks from production paths; add visible demo mode.
2. Implement the Room/SQLCipher/Keystore vertical slice for one assessment and its outbox.
3. Make onboarding the real first-run route and persist worker/facility/language state.
4. Replace the submit flow's silent defaults with explicit missing-data choices and an action review screen.
5. Add the backend `Worker`, `Assessment`, `VoiceArtifact`, `CaseEvent`, and `OutboxItem` tables/API with idempotent sync.
6. Replace the AI/copilot work with a simple voice-note-to-form vertical slice: record, save offline, organize fields, confirm, upload audio + JSON.
7. Scaffold `portal/` and deliver the urgent queue + case detail before adding SMS or transport integrations.

## Definition of done for the hackathon prototype

- The operational demo starts empty and requires real provisioned worker, facility, and Auth records.
- The ASHA can complete a Hindi or English assessment fully offline.
- The ASHA can record a voice note offline; the audio survives restart, remains linked to the case, and is clearly marked pending upload/transcription.
- Speech never silently creates clinical facts: extracted fields are editable and confirmed before triage.
- The screen explains each detected danger sign and an approved first response.
- The assessment is encrypted locally and survives app restart.
- When connectivity returns, the case moves through `Queued → Uploading → Acknowledged` with a real backend record.
- The portal shows the worker, case, risk rationale, age, and action status.
- A medical officer can acknowledge and send guidance; the worker sees it after sync.
- Transport is either a real integration or an explicitly labelled manual call workflow.
- SMS is either a tested provider integration or clearly marked “not configured”; no fake success is shown.
- Any test fixtures, simulated delivery, and test overrides are isolated behind `SAKHICARE_TEST_MODE=true` and are never shipped in release builds.
