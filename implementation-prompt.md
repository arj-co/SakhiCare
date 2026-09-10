# SakhiCare — four-phase implementation prompt

Copy the prompt below into the coding agent that will implement SakhiCare.

---

You are implementing SakhiCare, an offline-first maternal danger-sign screening and response system for ASHA workers in villages with unreliable connectivity.

Read these files before changing code:

- `plan.md`
- `rules.md`
- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/OPENAPI_SPEC.md`

## Product mission

Help an ASHA worker identify maternal danger signs offline, organize the encounter, start approved first-response actions, and hand the case to a medical officer and transport coordinator with a traceable status.

The product has two clients:

1. Android ASHA app: offline point-of-care assessment, encrypted local storage, voice-note capture, queueing, and case follow-up.
2. React + Vite Care Desk portal: authenticated medical-officer/admin view of incoming cases, worker management, clinical acknowledgement, guidance, transport coordination, SMS status, and audit history.

## Non-negotiable product decisions

- Do not build a local LLM.
- Do not claim clinical speech intelligence, voice diagnosis, or automatic medical reasoning.
- Voice is only a structured intake shortcut: record audio, organize recognized content into editable fields, let the worker confirm it, and send the original audio plus confirmed JSON fields.
- Never invent patient names, villages, BP, Hb, symptoms, ambulance IDs, ETAs, delivery receipts, or medical-officer acknowledgements.
- Never silently convert missing measurements into normal measurements.
- The clinical rule engine consumes confirmed structured fields only.
- Clinical rules are deterministic, versioned, reviewed, and separate from transcription.
- Demo data and simulated integrations must be isolated behind an explicit demo/test mode and visibly labelled.
- A case is not `Synced` until the server acknowledges it.
- A notification is not `Delivered` unless the provider supplies a delivery result.
- A transport request is not `Dispatched` without a real integration confirmation or a clearly labelled manual confirmation.
- Preserve the user's existing unrelated work and do not use destructive git operations.

## Current repository reality

The current Android app is Jetpack Compose. It contains a seeded in-memory `PatientRepository`, placeholder Room/SQLCipher and WorkManager classes, a deterministic Android triage engine, a regex-based `VoiceHelper`, and a demo Care Desk embedded in FastAPI as HTML. The backend currently uses in-memory cases, seeded demo records, simulated OneSignal delivery, and fabricated fallback records.

Replace demo behaviour progressively. Do not rewrite the entire repository blindly. Keep useful existing UI and triage foundations where they meet the rules, but remove or quarantine misleading claims and fabricated states.

## Shared domain model

Implement these concepts consistently in Android, backend, and portal:

- `Worker`: identity, role, phone, facility, locale, status, last seen.
- `Facility`: name, type, catchment, location, capabilities, contact routes.
- `PregnancyCase`: local case ID, minimal demographics, gestational age, pregnancy history, worker, facility, timestamps.
- `Assessment`: confirmed observations, danger signs, provenance, rule-pack version, risk level, rationale, action state.
- `Observation`: type, value, unit, measured time, source (`MANUAL`, `VOICE`, `DEVICE`), confidence, worker confirmation.
- `VoiceArtifact`: encrypted audio reference, language, duration, transcript, processing state, upload state, retention deadline.
- `Referral`: destination, urgency, reason, worker actions, medical-officer actions, timestamps.
- `TransportRequest`: channel, provider reference, status, ETA only when sourced, confirmation, timestamps.
- `Notification`: recipient, channel, template, status (`QUEUED`, `SENT`, `DELIVERED`, `FAILED`, `UNKNOWN`, `NOT_CONFIGURED`), provider ID, timestamps.
- `CaseEvent`: append-only timeline event, actor, timestamp, event type, metadata.
- `OutboxItem`: local idempotency key, entity type, encrypted payload, retry count, next retry time, state, last error.

## Clinical scope

Support the approved screening flow for:

- heavy vaginal bleeding;
- convulsions, fainting, or unconsciousness;
- severe headache or blurred vision;
- severe abdominal pain;
- severe breathlessness or chest pain;
- fever with severe illness;
- premature labour or water breaking early;
- absent or reduced fetal movement;
- severe hypertension or hypotension;
- haemoglobin when available;
- gestational age, pregnancy history, and travel constraints.

The app must show what was found, approved ASHA-safe first-response actions, what requires medical-officer direction, and the next escalation step. Medication doses, IV procedures, transfusion decisions, and diagnoses require clinician-owned, versioned content and must not be presented as autonomous ASHA instructions.

# Phase 1 — Offline ASHA foundation

## Objective

Make the Android app a truthful, durable, offline-first assessment tool.

## Work

1. Add compatible Room, KSP, WorkManager, Retrofit/OkHttp, and the selected supported SQLCipher integration to the Android build.
2. Implement Keystore-backed encryption key creation and encrypted Room database opening. Do not use a hard-coded key or a stub that only returns `true`.
3. Add entities, DAOs, migrations, repositories, and flows for workers, cases, assessments, observations, events, and outbox items.
4. Replace the seeded `PatientRepository` with Room-backed data. Production starts empty. Demo fixtures are available only through an explicit demo build/configuration.
5. Make onboarding the true first-run route. Persist language, worker identity, facility, local unlock method, permission state, consent, and protocol-pack version.
6. Replace the long form with a progressive encounter flow: case identification, immediate danger signs, gestational/pregnancy history, measurements, travel constraints, confirmation.
7. Add explicit `Not measured`, `Unknown`, and `Unable to answer` values. Never default blank BP/Hb to normal values.
8. Align Android rules with one shared clinical contract. Include the complete danger-sign set and store the rule-pack version.
9. Implement local states: `DRAFT`, `SAVED_LOCALLY`, `QUEUED`, `UPLOADING`, `ACKNOWLEDGED`, `FAILED`, `NEEDS_REVIEW`.
10. Implement a real WorkManager outbox worker with network constraints, exponential backoff, idempotency keys, retry limits, and failure visibility.
11. Remove the release UI's manual online/offline override.
12. Make case status, pending count, last successful sync, and failure reason visible without implying remote delivery.

## Phase 1 acceptance checks

- Fresh install opens onboarding, not a prefilled dashboard.
- No seeded patient appears in production mode.
- Create a case offline, kill the app, restart it, and find the case unchanged.
- Encrypted database cannot be opened without the Keystore-protected key.
- Missing BP/Hb remain missing and do not become `120/80` or `11.0`.
- The same clinical fixture returns the same result on Android and backend.
- A failed upload remains queued and retryable; it never becomes synced locally without acknowledgement.
- Every mutation creates a local case event.

Stop after Phase 1 and report changed files, migrations, tests, and any dependency/environment blocker.

# Phase 2 — Voice note to form

## Objective

Make speech a safe, simple way to capture information without building an LLM.

## Work

1. Add microphone consent and a clear explanation: “Your voice note will be attached to this case and sent to the Care Desk when possible.”
2. Implement start, pause, resume, stop, playback, delete, and re-record controls.
3. Save audio immediately into encrypted app-private storage and link it to the draft case.
4. Add metadata: language, duration, capture time, device, processing status, checksum, upload status, retention deadline.
5. Use the phone's speech service or a configured transcription endpoint only as an intake utility. Do not call it an LLM or clinical AI.
6. Organize recognized content into editable fields: name/case reference, village/location, symptoms, BP, Hb, gestational age, pregnancy history, transport constraints, and notes.
7. Display the transcript and highlight the fields extracted from it. The worker confirms every field before it can affect triage.
8. Keep unrecognized content in transcript/audio. Do not guess missing fields or infer a danger sign from uncertain text.
9. Upload the original audio and confirmed structured JSON as separate but linked outbox items. Support retry/resume and checksum validation.
10. If transcription fails, keep the audio, mark `Voice review needed`, and let the worker complete the form manually.
11. In the portal, allow authorized users to play/download audio according to access rules and view transcript beside structured fields.
12. Add audio retention and deletion policy; record access to the audio in the audit trail.

## Phase 2 acceptance checks

- Record audio completely offline.
- Restart the app and the audio remains linked to the draft case.
- A transcription failure does not create fabricated patient data.
- The worker can correct every extracted field before triage.
- Audio and confirmed JSON retry independently and remain linked by case ID.
- The UI visibly distinguishes `Audio saved locally`, `Waiting to upload`, `Uploaded`, and `Processing failed`.
- The portal can display the original audio only to an authorized role.

Stop after Phase 2 and report the audio format, storage/retention behaviour, upload protocol, tests, and known device limitations.

# Phase 3 — Durable backend and React + Vite Care Desk

## Objective

Replace the in-memory demo backend and embedded HTML desk with a durable, authenticated operator system.

## Work

1. Add PostgreSQL for production, SQLAlchemy models, Alembic migrations, indexes, and a local developer profile.
2. Remove startup seed data from production. Add a separate demo fixture command/configuration with visible demo mode.
3. Add authentication and server-enforced RBAC for Medical Officer, Supervisor, Dispatcher, and Administrator.
4. Scope every case by facility/tenant and worker permissions.
5. Replace fabricated unknown-case fallbacks with correct `404` responses.
6. Implement versioned `/api/v1` routes for worker registration, cases, assessments, outbox sync, audio upload, notifications, referrals, transport, advisory messages, and audit events.
7. Make sync idempotent. Repeated uploads with the same idempotency key must not create duplicate assessments or alerts.
8. Persist case events and provide a replayable event feed. SSE may accelerate the UI but must not be the source of truth; add reconnect and polling fallback.
9. Create `portal/` using TypeScript, React, Vite, typed API contracts, route guards, and a query/cache layer.
10. Build portal screens: sign-in, urgent queue, all cases, case timeline, worker directory, facility directory, transport board, notifications, audit log, and protocol settings.
11. The urgent queue must show risk, age, worker, area, triggers, sync state, acknowledgement, transport state, and next action.
12. Case detail must support acknowledge, send clinical guidance, choose referral facility, request/record transport, record contact attempts, and close the case.
13. Every portal mutation shows actor, timestamp, confirmation, and audit history.

## Phase 3 acceptance checks

- Restarting the backend does not lose cases, audio metadata, notifications, or timeline events.
- A worker cannot access another facility's cases.
- Two portal sessions receive a new case and remain consistent after refresh/reconnect.
- Portal roles are enforced by the backend, not only hidden in the UI.
- Unknown case IDs return `404`.
- Duplicate sync does not duplicate the case or alert.
- A medical officer can acknowledge a case and the worker receives the advisory after sync.
- Portal never shows “live” when its event connection is stale.

Stop after Phase 3 and report the schema/migrations, auth model, API contract, portal routes, and end-to-end test results.

# Phase 4 — Escalation, SMS, transport, and release hardening

## Objective

Make response coordination honest, auditable, and ready for a controlled pilot.

## Work

1. Add notification adapters behind a common interface: in-app/push, SMS, and optional voice-call handoff.
2. SMS must use configurable provider credentials, approved templates, rate limits, delivery callbacks, retries, and audit records.
3. Keep SMS minimal: case reference, urgency, area, short sign summary, and callback route. Do not include a full clinical record by default.
4. Separate notification state from clinical state. `SMS sent` does not mean `medical officer acknowledged`.
5. For RED, create an urgent portal task and configured medical-officer alert. For AMBER, create a review task. Do not interrupt for GREEN unless configured.
6. Implement transport as either an approved integration or a manual call-and-confirm workflow. Never invent an ambulance ID or ETA.
7. Record destination, requester, channel, provider/manual reference, status, time, confirmation, and failure/escalation reason.
8. Add facility routing based on capabilities, distance/travel constraints, hours, and configured referral rules.
9. Add clinical sign-off workflow for rule thresholds, action cards, translations, and protocol-pack versions.
10. Add encryption, auth, CORS allow-list, TLS, secret management, rate limiting, audit review, backup/restore, and retention tests.
11. Add Android and portal accessibility, low-end device, large-font, slow-network, offline-restart, and failure-state tests.
12. Remove or quarantine fake network toggles, canned audio fallbacks, fake delivery IDs, seeded patients, and release-only demo controls.
13. Update README, screenshots, API docs, and demo script so every claim matches actual behaviour.

## Phase 4 acceptance checks

- A RED case creates an auditable alert attempt with real delivery state.
- Provider failure is visible and triggers the configured fallback/escalation path.
- SMS contains minimal approved content and no fake delivery success.
- Transport is shown as `Requested`, `Call attempted`, `Confirmed`, `En route`, `Arrived`, or `Failed`, with evidence for each state.
- Clinical rules, translations, and action cards have version and reviewer metadata.
- Security, accessibility, offline, sync, duplicate, and recovery tests pass.
- Release build contains no demo seed data, fake fallback transcript, manual connectivity override, or simulated-success label disguised as production.

## Working style for every phase

- Inspect before editing; make the smallest coherent change.
- Prefer shared contracts and generated types over duplicate hand-written schemas.
- Add tests with each feature, especially failure and offline tests.
- Use explicit TODOs for unavailable external credentials or provider integrations.
- Do not silently weaken security or clinical safety to make a demo pass.
- After each phase, summarize: files changed, data migrations, commands run, tests passed, tests blocked, and remaining risks.
- Do not proceed to the next phase until its acceptance checks pass or the user explicitly accepts the exceptions.

Begin with Phase 1 only.

---
