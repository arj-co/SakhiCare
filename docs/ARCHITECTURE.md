# SakhiCare System Architecture & C4 Model

## C4 Context & Container Overview
SakhiCare provides dual-layer resilience for rural maternal healthcare:
1. **Frontline Edge (Android APK)**: Runs fully offline with Google On-Device STT + Standalone FastConformer pipeline.
2. **Care Desk Cloud Hub (FastAPI + SSE)**: Aggregates patient telemetry and dispatches OneSignal alerts to 108 emergency units.

## Offline Sync State Machine
```
[Assessment Recorded] -> [Local StateList] -> [Network Monitor Check]
    | (Offline)                                  | (Online)
    v                                            v
[Stored Locally in Memory]               [POST /sync/batch] -> [Care Desk SSE Stream]
```
