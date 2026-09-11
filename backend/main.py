"""SakhiCare sync service and Care Desk operations API."""

from typing import Dict, Any, List, Optional
import os
import re
import json
import time
import hashlib
import asyncio
import uuid
from datetime import datetime, timezone
from fastapi import FastAPI, Request, status, BackgroundTasks, UploadFile, File, HTTPException, Header, Query, Depends
from fastapi.responses import HTMLResponse, StreamingResponse, FileResponse, RedirectResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from fhir_converter import generate_fhir_bundle
import onesignal_service
from triage_engine import evaluate_clinical_risk, ClinicalEvaluationResult
from speech_engine import extract_clinical_slots, transcribe_offline_audio, normalize_spoken_numbers

from database import init_db, get_db, SessionLocal
from models import (
    FacilityModel, WorkerModel, UserModel, PregnancyCaseModel,
    AssessmentModel, VoiceArtifactModel, CaseEventModel, OutboxItemModel,
    NotificationLogModel, TransportRequestModel
)
from auth import (
    create_access_token, decode_access_token, get_current_user,
    require_roles, TokenData, verify_password, hash_password,
    resolve_supabase_profile, AUTH_PROVIDER
)
from case_service import (
    ingest_sync_case_batch, get_case_detail, list_cases,
    acknowledge_case, update_transport,
    create_or_update_transport_request, list_transport_requests,
    recommend_facility_for_case, get_clinical_protocol_metadata
)
from notification_service import (
    list_notification_logs, trigger_emergency_escalation,
    send_notification, format_minimal_sms
)
import supabase_storage

APP_ENV = os.getenv("APP_ENV", "development").lower()
TEST_MODE = os.getenv("SAKHICARE_TEST_MODE", "false").lower() == "true"
CORS_ORIGINS = [origin.strip() for origin in os.getenv("CORS_ORIGINS", "http://localhost:5173,http://localhost:5175").split(",") if origin.strip()]
if APP_ENV == "production" and (not CORS_ORIGINS or any("localhost" in origin or "127.0.0.1" in origin for origin in CORS_ORIGINS)):
    raise RuntimeError("Production requires explicit non-localhost CORS_ORIGINS")
if APP_ENV == "production":
    required_production_config = {
        "SUPABASE_URL": os.getenv("SUPABASE_URL"),
        "SUPABASE_SERVICE_ROLE_KEY": os.getenv("SUPABASE_SERVICE_ROLE_KEY"),
        "SUPABASE_DATABASE_URL": os.getenv("SUPABASE_DATABASE_URL"),
        "SUPABASE_JWT_SECRET": os.getenv("SUPABASE_JWT_SECRET"),
        "SMS_GATEWAY_URL": os.getenv("SMS_GATEWAY_URL"),
        "SMS_GATEWAY_API_KEY": os.getenv("SMS_GATEWAY_API_KEY"),
        "EMERGENCY_MO_PHONE": os.getenv("EMERGENCY_MO_PHONE"),
        "SUPERVISOR_PHONE": os.getenv("SUPERVISOR_PHONE"),
        "ONESIGNAL_APP_ID": os.getenv("ONESIGNAL_APP_ID"),
        "ONESIGNAL_REST_API_KEY": os.getenv("ONESIGNAL_REST_API_KEY"),
    }
    missing_production_config = [name for name, value in required_production_config.items() if not value]
    if missing_production_config:
        raise RuntimeError(
            "Production configuration is incomplete. Set: " + ", ".join(missing_production_config)
        )

# Initialize the durable schema only. Real facilities, users, workers, and cases
# are provisioned by the deployment/admin workflow; the server never creates
# demo rows on startup.
init_db()

app = FastAPI(
    title="SakhiCare Care Desk & Sync API",
    description="Backend sync service, FHIR R4 converter, OneSignal push engine, Parakeet Speech AI, and Care Desk operations console for SakhiCare.",
    version="1.2.0",
)

# Enable CORS for external web integrations
app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory storage for synced cases
synced_cases_db: Dict[str, Dict[str, Any]] = {}

# SSE Live Event Listeners (Queues for streaming live updates to Care Desk)
sse_subscribers: List[asyncio.Queue] = []


class DangerSignsModel(BaseModel):
    bleeding: bool = False
    fever: bool = False
    headache: bool = False
    reduced_fetal_movement: bool = False
    convulsions_or_vision_loss: bool = False


class AssessmentSyncPayload(BaseModel):
    patient_id: Optional[str] = Field(None, json_schema_extra={"example": "SC-101"})
    patient_name: str = Field(..., json_schema_extra={"example": "Sunita Devi"})
    village: str = Field(..., json_schema_extra={"example": "Rampur"})
    blood_pressure: str = Field(..., json_schema_extra={"example": "145/95"})
    haemoglobin: float = Field(..., json_schema_extra={"example": 10.5})
    danger_signs: DangerSignsModel
    risk_level: Optional[str] = Field(None, json_schema_extra={"example": "RED"})
    timestamp: Optional[str] = Field(None, json_schema_extra={"example": "2026-08-19T06:00:00Z"})
    asha_worker_name: Optional[str] = None
    asha_device_id: Optional[str] = Field(None, json_schema_extra={"example": "device_asha_101"})
    latitude: Optional[float] = Field(None, ge=-90, le=90)
    longitude: Optional[float] = Field(None, ge=-180, le=180)
    location_accuracy_m: Optional[float] = Field(None, ge=0)
    location_captured_at: Optional[str] = None


class AdvisoryRequest(BaseModel):
    advisory_text: str = Field(..., json_schema_extra={"example": "Administer oral labetalol 100mg stat, arrange immediate transport to CHC."})
    sender: str = Field(..., json_schema_extra={"example": "Medical Officer"})


class DispatchRequest(BaseModel):
    vehicle_id: str = Field(..., json_schema_extra={"example": "108-AMB-001"})
    destination_facility: str = Field(..., json_schema_extra={"example": "Assigned referral facility"})


class LoginRequest(BaseModel):
    username: str
    password: str


class AcknowledgeCaseRequest(BaseModel):
    advisory_text: str
    referral_facility_id: Optional[str] = None


class TransportCaseRequest(BaseModel):
    vehicle_id: str
    destination_facility: str
    driver_phone: Optional[str] = None


class TransportBoardUpdateRequest(BaseModel):
    case_id: str
    status: str = "REQUESTED"
    vehicle_id: Optional[str] = None
    destination_facility_id: Optional[str] = None
    destination_facility_name: Optional[str] = None
    driver_name: Optional[str] = None
    driver_phone: Optional[str] = None
    call_attempt_notes: Optional[str] = None


class EmergencyPushRequest(BaseModel):
    patient_id: str = Field(..., json_schema_extra={"example": "SC-101"})
    patient_name: str = Field(..., json_schema_extra={"example": "Sunita Devi"})
    village: str = Field(..., json_schema_extra={"example": "Rampur"})
    blood_pressure: str = Field(..., json_schema_extra={"example": "150/100"})
    danger_signs: Dict[str, bool] = Field(default_factory=dict)
    risk_level: str = Field("RED", json_schema_extra={"example": "RED"})


class BroadcastPushRequest(BaseModel):
    title: str = Field(..., json_schema_extra={"example": "High Maternal Triage Surge Alert"})
    message: str = Field(..., json_schema_extra={"example": "All ASHAs please review protocol for monsoon fever screening."})
    segment: str = Field("All", json_schema_extra={"example": "All"})


class DeviceRegistrationRequest(BaseModel):
    device_id: str = Field(..., json_schema_extra={"example": "dev-asha-001"})
    role: str = Field("ASHA", json_schema_extra={"example": "ASHA"})
    player_id: Optional[str] = Field(None, json_schema_extra={"example": "onesignal-player-uuid"})
    user_name: Optional[str] = Field(None, json_schema_extra={"example": "ASHA Sunita"})


class VoiceParseRequest(BaseModel):
    speech_text: str = Field(..., json_schema_extra={"example": "मरीज सुनीता देवी, गांव रामपुर, बीपी 145/95, हीमोग्लोबिन 10.2, बुखार और खून बहना"})


async def broadcast_sse_event(event_type: str, data: Dict[str, Any]):
    """Broadcasts a live event to all connected Care Desk screens."""
    message = json.dumps({"type": event_type, **data})
    for queue in sse_subscribers[:]:
        try:
            await queue.put(message)
        except Exception:
            sse_subscribers.remove(queue)


# ── Health & service entrypoint ──

@app.get("/", response_class=HTMLResponse)
def get_service_entrypoint():
    """Keep the API root explicit; the authenticated React portal is deployed separately."""
    return HTMLResponse("<h1>SakhiCare API</h1><p>Use the authenticated Care Desk portal.</p>")


@app.get("/health", status_code=status.HTTP_200_OK)
def health_check() -> Dict[str, Any]:
    with SessionLocal() as db:
        durable_case_count = db.query(PregnancyCaseModel).count()
    return {
        "status": "ok",
        "service": "SakhiCare Care Desk & Sync Server",
        "version": "1.2.0",
        "triage_engine": "MoHFW/WHO Multi-Metric Clinical Matrix",
        "speech_engine": "Parakeet-CTC FastConformer (Offline Ready)",
        "active_sse_clients": len(sse_subscribers),
        "total_cases": durable_case_count
    }


# ── Server-Sent Events (SSE) Live Stream ──

@app.get("/api/v1/live-stream")
async def sse_live_stream(request: Request, access_token: Optional[str] = Query(None)):
    if not access_token:
        raise HTTPException(status_code=401, detail="Bearer token required for live case stream")
    token_data = decode_access_token(access_token)
    if AUTH_PROVIDER == "supabase":
        resolve_supabase_profile(token_data)
    queue = asyncio.Queue()
    sse_subscribers.append(queue)

    async def event_generator():
        try:
            yield f"data: {json.dumps({'type': 'CONNECTED', 'message': 'Care Desk live stream connected'})}\n\n"
            while True:
                if await request.is_disconnected():
                    break
                try:
                    msg = await asyncio.wait_for(queue.get(), timeout=20.0)
                    yield f"data: {msg}\n\n"
                except asyncio.TimeoutError:
                    yield f": heartbeat\n\n"
        except asyncio.CancelledError:
            pass
        finally:
            if queue in sse_subscribers:
                sse_subscribers.remove(queue)

    return StreamingResponse(event_generator(), media_type="text/event-stream")


# ── Case Sync & Multi-Metric Clinical Triage ──

@app.post("/sync", status_code=status.HTTP_200_OK)
async def sync_case(
    payload: AssessmentSyncPayload,
    background_tasks: BackgroundTasks,
    user: TokenData = Depends(get_current_user),
) -> Dict[str, Any]:
    """
    Synchronizes an assessment and evaluates it against the comprehensive MoHFW / WHO clinical matrix.
    Triggers OneSignal emergency alert if RED triage.
    """
    patient_id = payload.patient_id or f"SC-{uuid.uuid4().hex[:12].upper()}"
    timestamp = payload.timestamp or datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    # Run Multi-Metric Clinical Evaluation
    clinical_eval: ClinicalEvaluationResult = evaluate_clinical_risk(
        blood_pressure=payload.blood_pressure,
        haemoglobin=payload.haemoglobin,
        danger_signs=payload.danger_signs.model_dump()
    )

    risk_level = clinical_eval.risk_level

    record = {
        "patient_id": patient_id,
        "patient_name": payload.patient_name,
        "village": payload.village,
        "blood_pressure": payload.blood_pressure,
        "haemoglobin": payload.haemoglobin,
        "danger_signs": payload.danger_signs.model_dump(),
        "risk_level": risk_level,
        "risk_score": clinical_eval.risk_score,
        "clinical_rationale": clinical_eval.clinical_rationale_en,
        "recommended_protocol": clinical_eval.recommended_protocol_en,
        "primary_factors": clinical_eval.primary_factors,
        "requires_immediate_ambulance": clinical_eval.requires_immediate_ambulance,
        "requires_blood_transfusion_alert": clinical_eval.requires_blood_transfusion_alert,
        "timestamp": timestamp,
        "sync_status": "Synced",
        "asha_worker_name": payload.asha_worker_name or user.username,
        "asha_device_id": payload.asha_device_id,
        "doctor_advisory": None,
        "ambulance_status": None
    }

    synced_cases_db[patient_id] = record

    # Persist to durable database
    with SessionLocal() as db:
        ingest_sync_case_batch(db, {
            "case_id": patient_id,
            "patient_name": payload.patient_name,
            "village": payload.village,
            "blood_pressure": payload.blood_pressure,
            "haemoglobin": payload.haemoglobin,
            "danger_signs": payload.danger_signs.model_dump(),
            "worker_id": "WKR-101" if TEST_MODE else user.user_id,
            "facility_id": "FAC-01" if TEST_MODE else user.facility_id,
            "latitude": payload.latitude,
            "longitude": payload.longitude,
            "location_accuracy_m": payload.location_accuracy_m,
            "location_captured_at": payload.location_captured_at
        })

    # Generate FHIR Bundle
    fhir_bundle = generate_fhir_bundle(
        patient_id=patient_id,
        patient_name=payload.patient_name,
        village=payload.village,
        blood_pressure=payload.blood_pressure,
        haemoglobin=payload.haemoglobin,
        danger_signs=payload.danger_signs.model_dump(),
        risk_level=risk_level,
        timestamp=timestamp
    )

    # 1. Broadcast to Care Desk via SSE
    await broadcast_sse_event("NEW_CASE", {"case": record})

    # 2. If RED risk, automatically trigger OneSignal Emergency Push Notification via REST API
    push_result = None
    if risk_level == "RED":
        push_result = await onesignal_service.send_emergency_triage_notification(
            patient_id=patient_id,
            patient_name=payload.patient_name,
            village=payload.village,
            blood_pressure=payload.blood_pressure,
            danger_signs=payload.danger_signs.model_dump(),
            risk_level=risk_level
        )

    return {
        "message": "Case successfully synced and ingested into SakhiCare Care Desk",
        "patient_id": patient_id,
        "risk_level": risk_level,
        "clinical_evaluation": clinical_eval.to_dict(),
        "fhir_bundle_id": fhir_bundle["id"],
        "onesignal_emergency_push": push_result
    }


# ── Care Desk Actions: Clinical Advisory & Ambulance Dispatch ──

@app.post("/api/v1/cases/{patient_id}/advisory", status_code=status.HTTP_200_OK)
async def post_clinical_advisory(
    patient_id: str,
    request: AdvisoryRequest,
    user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "ADMIN", "SUPERVISOR")),
) -> Dict[str, Any]:
    with SessionLocal() as db:
        case = get_case_detail(db, patient_id)
    if not case:
        raise HTTPException(status_code=404, detail=f"Case {patient_id} not found")

    case["doctor_advisory"] = request.advisory_text
    case["advisory_sender"] = request.sender
    case["advisory_timestamp"] = datetime.now(timezone.utc).isoformat()

    with SessionLocal() as db:
        db_case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == patient_id).first()
        db_case.doctor_advisory = request.advisory_text
        db_case.updated_at = int(time.time())
        db.commit()

    await broadcast_sse_event("CASE_UPDATED", {"case": case})

    push_result = await onesignal_service.send_clinical_advisory_notification(
        patient_id=patient_id,
        patient_name=case["patient_name"],
        advisory_text=request.advisory_text,
        doctor_or_operator_name=request.sender,
        target_player_id=case.get("asha_device_id")
    )

    return {
        "status": "success",
        "message": f"Advisory recorded and pushed to ASHA worker for case {patient_id}",
        "patient_id": patient_id,
        "advisory": request.advisory_text,
        "notification_id": push_result.get("id"),
        "delivery": push_result.get("delivery")
    }


@app.post("/api/v1/cases/{patient_id}/dispatch", status_code=status.HTTP_200_OK)
async def dispatch_ambulance(
    patient_id: str,
    request: DispatchRequest,
    user: TokenData = Depends(require_roles("DISPATCHER", "MEDICAL_OFFICER", "ADMIN", "SUPERVISOR")),
) -> Dict[str, Any]:
    with SessionLocal() as db:
        case = get_case_detail(db, patient_id)
    if not case:
        raise HTTPException(status_code=404, detail=f"Case {patient_id} not found")
    # This legacy endpoint only records a dispatch request. A vehicle is not
    # marked confirmed/en-route until the transport board records that call.
    status_str = f"Ambulance {request.vehicle_id} - CALL_ATTEMPTED (Destination: {request.destination_facility})"
    case["ambulance_status"] = status_str
    case["ambulance_dispatched_at"] = datetime.now(timezone.utc).isoformat()

    with SessionLocal() as db:
        db_case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == patient_id).first()
        if db_case:
            db_case.ambulance_status = status_str
            db_case.updated_at = int(time.time())
            db.commit()

    await broadcast_sse_event("CASE_UPDATED", {"case": case})

    return {
        "status": "success",
        "patient_id": patient_id,
        "ambulance_status": status_str
    }


# ── Dedicated OneSignal REST API Endpoints ──

@app.post("/api/v1/notifications/send-emergency-alert", status_code=status.HTTP_200_OK)
async def api_send_emergency_push(payload: EmergencyPushRequest, user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "ADMIN", "SUPERVISOR"))) -> Dict[str, Any]:
    return await onesignal_service.send_emergency_triage_notification(
        patient_id=payload.patient_id,
        patient_name=payload.patient_name,
        village=payload.village,
        blood_pressure=payload.blood_pressure,
        danger_signs=payload.danger_signs,
        risk_level=payload.risk_level
    )


@app.post("/api/v1/notifications/send-advisory", status_code=status.HTTP_200_OK)
async def api_send_advisory_push(payload: AdvisoryRequest, patient_id: str = Query(...), user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "ADMIN"))) -> Dict[str, Any]:
    with SessionLocal() as db:
        case = get_case_detail(db, patient_id)
    if not case:
        raise HTTPException(status_code=404, detail=f"Case {patient_id} not found")
    patient_name = case["patient_name"]
    return await onesignal_service.send_clinical_advisory_notification(
        patient_id=patient_id,
        patient_name=patient_name,
        advisory_text=payload.advisory_text,
        doctor_or_operator_name=payload.sender
    )


@app.post("/api/v1/notifications/broadcast", status_code=status.HTTP_200_OK)
async def api_send_broadcast_push(payload: BroadcastPushRequest, user: TokenData = Depends(require_roles("SUPERVISOR", "ADMIN"))) -> Dict[str, Any]:
    return await onesignal_service.send_custom_broadcast(
        title=payload.title,
        message=payload.message,
        segment=payload.segment
    )


@app.post("/api/v1/devices/register", status_code=status.HTTP_200_OK)
def api_register_device(payload: DeviceRegistrationRequest, user: TokenData = Depends(get_current_user)) -> Dict[str, Any]:
    record = onesignal_service.register_device(
        device_id=payload.device_id,
        role=payload.role,
        player_id=payload.player_id,
        user_name=payload.user_name
    )
    return {"status": "success", "device": record}


@app.get("/api/v1/notifications/history", status_code=status.HTTP_200_OK)
def get_notification_history(user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "ADMIN", "SUPERVISOR"))) -> Dict[str, Any]:
    return {
        "count": len(onesignal_service.notification_history),
        "history": onesignal_service.notification_history
    }


# ── Speech AI & Clinical Evaluation Endpoints ──

@app.post("/voice-parse", status_code=status.HTTP_200_OK)
def parse_voice(request: VoiceParseRequest, user: TokenData = Depends(get_current_user)) -> Dict[str, Any]:
    return parse_speech_dictation(request.speech_text)


def parse_speech_dictation(text: str) -> Dict[str, Any]:
    """
    Multilingual speech slot-filler with multi-metric clinical triage evaluation.
    """
    slots = extract_clinical_slots(text)
    triage = slots["clinical_triage"]
    return {
        "patient_name": slots["patient_name"],
        "village": slots["village"],
        "blood_pressure": slots["blood_pressure"],
        "haemoglobin": slots["haemoglobin"],
        "danger_signs": slots["danger_signs"],
        "risk_level": triage["risk_level"],
        "risk_score": triage["risk_score"],
        "clinical_rationale": triage["clinical_rationale_en"],
        "recommended_protocol": triage["recommended_protocol_en"],
        "parsed_from_speech": text
    }


def reject_local_llm_in_production() -> None:
    if APP_ENV == "production":
        raise HTTPException(status_code=status.HTTP_410_GONE, detail="Local LLM endpoints are disabled; use deterministic field organization and approved triage rules")


@app.post("/api/v1/voice/evaluate-dictation", status_code=status.HTTP_200_OK)
def evaluate_dictation_endpoint(
    request: VoiceParseRequest,
    user: TokenData = Depends(get_current_user),
) -> Dict[str, Any]:
    """
    Direct endpoint for clinical speech evaluation with full multi-metric triage output.
    """
    return extract_clinical_slots(request.speech_text)


@app.post("/api/v1/voice/transcribe-audio", status_code=status.HTTP_200_OK)
async def transcribe_audio_endpoint(
    file: UploadFile = File(...),
    user: TokenData = Depends(get_current_user),
) -> Dict[str, Any]:
    """
    Ingests recorded audio from mobile phone and executes offline Parakeet/FastConformer acoustic pipeline.
    """
    reject_local_llm_in_production()
    content = await file.read()
    return transcribe_offline_audio(content, filename=file.filename or "audio.wav")


# ── Phase 2: Audited Case Audio Endpoints ──
AUDIO_STORAGE_DIR = os.path.join(os.path.dirname(__file__), "data", "audio")
os.makedirs(AUDIO_STORAGE_DIR, exist_ok=True)
audio_artifacts_db: Dict[str, Dict[str, Any]] = {}


@app.post("/api/v1/cases/{case_id}/audio", status_code=status.HTTP_201_CREATED)
async def upload_case_audio(
    case_id: str,
    file: UploadFile = File(...),
    duration_seconds: int = Query(0),
    language: str = Query("hi-IN"),
    x_idempotency_key: Optional[str] = Header(None, alias="X-Idempotency-Key"),
    x_audio_sha256: Optional[str] = Header(None, alias="X-Audio-SHA256"),
    user: TokenData = Depends(require_roles("ASHA", "MEDICAL_OFFICER", "ADMIN"))
) -> Dict[str, Any]:
    """
    Ingests raw recorded audio note for a case with SHA-256 validation and audit trail.
    """
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty audio payload")

    computed_sha = hashlib.sha256(content).hexdigest()
    if x_audio_sha256 and x_audio_sha256.lower() != computed_sha.lower():
        raise HTTPException(status_code=400, detail=f"SHA-256 checksum mismatch: header={x_audio_sha256}, actual={computed_sha}")

    artifact_id = f"aud_{computed_sha[:12]}"
    file_ext = os.path.splitext(file.filename or "")[1] or ".m4a"
    dest_filename = f"{case_id}_{artifact_id}{file_ext}"
    dest_path = os.path.join(AUDIO_STORAGE_DIR, dest_filename)
    storage_path = f"cases/{case_id}/{dest_filename}"
    if APP_ENV == "production":
        try:
            supabase_storage.upload(storage_path, content, file.content_type or "audio/m4a")
        except RuntimeError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc
        local_path = None
    else:
        with open(dest_path, "wb") as f:
            f.write(content)
        local_path = dest_path

    artifact_meta = {
        "id": artifact_id,
        "case_id": case_id,
        "file_path": local_path,
        "storage_path": storage_path,
        "filename": dest_filename,
        "mime_type": file.content_type or "audio/m4a",
        "file_size_bytes": len(content),
        "sha256": computed_sha,
        "language": language,
        "duration_seconds": duration_seconds,
        "upload_status": "UPLOADED",
        "uploaded_at": int(time.time()),
        "retention_due_at": int(time.time() + 90 * 86400)
    }
    audio_artifacts_db[case_id] = artifact_meta

    # Log audit event
    audit_event = {
        "event_id": f"evt_{int(time.time() * 1000)}",
        "case_id": case_id,
        "event_type": "AUDIO_UPLOADED",
        "actor_id": "ASHA_WORKER",
        "actor_role": "ASHA",
        "summary": f"Voice note uploaded ({len(content)} bytes, SHA: {computed_sha[:8]}...)",
        "occurred_at": int(time.time())
    }
    # Link to case if present in synced_cases_db
    if case_id in synced_cases_db:
        synced_cases_db[case_id]["audio_artifact"] = artifact_meta
        if "timeline" in synced_cases_db[case_id]:
            synced_cases_db[case_id]["timeline"].append(audit_event)

    # Persist artifact & audit event to durable database
    with SessionLocal() as db:
        case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
        if case:
            existing_artifact = db.query(VoiceArtifactModel).filter(VoiceArtifactModel.case_id == case_id).first()
            if existing_artifact:
                existing_artifact.file_path = local_path
                existing_artifact.storage_path = storage_path
                existing_artifact.filename = dest_filename
                existing_artifact.mime_type = file.content_type or "audio/m4a"
                existing_artifact.file_size_bytes = len(content)
                existing_artifact.sha256 = computed_sha
                existing_artifact.language = language
                existing_artifact.duration_seconds = duration_seconds
                existing_artifact.upload_status = "UPLOADED"
                existing_artifact.uploaded_at = int(time.time())
            else:
                db_art = VoiceArtifactModel(
                    id=artifact_id,
                    case_id=case_id,
                    file_path=local_path,
                    storage_path=storage_path,
                    filename=dest_filename,
                    mime_type=file.content_type or "audio/m4a",
                    file_size_bytes=len(content),
                    sha256=computed_sha,
                    language=language,
                    duration_seconds=duration_seconds,
                    upload_status="UPLOADED",
                    retention_due_at=int(time.time() + 90 * 86400),
                    uploaded_at=int(time.time())
                )
                db.add(db_art)

            db_evt = CaseEventModel(
                id=f"EVT-{int(time.time() * 1000)}",
                case_id=case_id,
                event_type="AUDIO_UPLOADED",
                actor_id="ASHA_WORKER",
                actor_role="ASHA",
                summary=f"Voice note uploaded ({len(content)} bytes, SHA: {computed_sha[:8]}...)",
                occurred_at=int(time.time())
            )
            db.add(db_evt)
            db.commit()

    return {
        "status": "UPLOADED",
        "artifact_id": artifact_id,
        "case_id": case_id,
        "sha256": computed_sha,
        "file_size_bytes": len(content),
        "retention_due_at": artifact_meta["retention_due_at"]
    }


@app.get("/api/v1/cases/{case_id}/audio")
async def get_case_audio(case_id: str, user: TokenData = Depends(require_roles("ASHA", "MEDICAL_OFFICER", "SUPERVISOR", "ADMIN"))):
    """
    Streams the case's recorded audio note to authorized Care Desk operators with audit tracking.
    """
    artifact = audio_artifacts_db.get(case_id)
    if not artifact:
        with SessionLocal() as db:
            db_art = db.query(VoiceArtifactModel).filter(VoiceArtifactModel.case_id == case_id).first()
            if db_art:
                artifact = {
                    "file_path": db_art.file_path,
                    "storage_path": db_art.storage_path,
                    "mime_type": db_art.mime_type,
                    "filename": db_art.filename
                }

    if not artifact:
        raise HTTPException(status_code=404, detail="Audio recording not found for case")

    if artifact.get("storage_path") and APP_ENV == "production":
        try:
            return RedirectResponse(supabase_storage.signed_url(artifact["storage_path"]), status_code=307)
        except RuntimeError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc

    if not artifact.get("file_path") or not os.path.exists(artifact["file_path"]):
        raise HTTPException(status_code=404, detail="Audio recording not found for case")

    # Record access audit event
    if case_id in synced_cases_db and "timeline" in synced_cases_db[case_id]:
        synced_cases_db[case_id]["timeline"].append({
            "event_id": f"evt_{int(time.time() * 1000)}",
            "case_id": case_id,
            "event_type": "AUDIO_ACCESSED",
            "actor_id": "CARE_DESK_OPERATOR",
            "actor_role": "MEDICAL_OFFICER",
            "summary": "Audio note streamed by Care Desk operator",
            "occurred_at": int(time.time())
        })

    with SessionLocal() as db:
        case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
        if case:
            db.add(CaseEventModel(
                id=f"EVT-{int(time.time() * 1000)}",
                case_id=case_id,
                event_type="AUDIO_ACCESSED",
                actor_id="CARE_DESK_OPERATOR",
                actor_role="MEDICAL_OFFICER",
                summary="Audio note streamed by Care Desk operator",
                occurred_at=int(time.time())
            ))
            db.commit()

    return FileResponse(
        artifact["file_path"],
        media_type=artifact.get("mime_type", "audio/m4a"),
        filename=artifact.get("filename", f"{case_id}.m4a")
    )


@app.delete("/api/v1/cases/{case_id}/audio", status_code=status.HTTP_200_OK)
async def delete_case_audio(case_id: str, user: TokenData = Depends(get_current_user)):
    """
    Deletes audio note per retention/privacy policy with audit log.
    """
    if APP_ENV == "production" and user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Only an administrator can delete retained audio")
    artifact = audio_artifacts_db.get(case_id)
    dest_path = None
    if artifact:
        dest_path = artifact.get("file_path")
        storage_path = artifact.get("storage_path")
        del audio_artifacts_db[case_id]
    else:
        with SessionLocal() as db:
            db_art = db.query(VoiceArtifactModel).filter(VoiceArtifactModel.case_id == case_id).first()
            if db_art:
                dest_path = db_art.file_path
                storage_path = db_art.storage_path

    if not dest_path and not storage_path:
        raise HTTPException(status_code=404, detail="Audio recording not found for case")

    if dest_path and os.path.exists(dest_path):
        os.remove(dest_path)
    if storage_path and APP_ENV == "production":
        try:
            supabase_storage.delete(storage_path)
        except RuntimeError as exc:
            raise HTTPException(status_code=503, detail=str(exc)) from exc

    if case_id in synced_cases_db and "timeline" in synced_cases_db[case_id]:
        synced_cases_db[case_id]["timeline"].append({
            "event_id": f"evt_{int(time.time() * 1000)}",
            "case_id": case_id,
            "event_type": "AUDIO_DELETED",
            "actor_id": "ADMIN_POLICY",
            "actor_role": "SYSTEM",
            "summary": "Audio file deleted per retention policy",
            "occurred_at": int(time.time())
        })

    with SessionLocal() as db:
        db_art = db.query(VoiceArtifactModel).filter(VoiceArtifactModel.case_id == case_id).first()
        if db_art:
            db.delete(db_art)
        db.add(CaseEventModel(
            id=f"EVT-{int(time.time() * 1000)}",
            case_id=case_id,
            event_type="AUDIO_DELETED",
            actor_id="ADMIN_POLICY",
            actor_role="SYSTEM",
            summary="Audio file deleted per retention policy",
            occurred_at=int(time.time())
        ))
        db.commit()

    return {"status": "DELETED", "case_id": case_id}


# ── SakhiAI Medical LLM Endpoints (UN SDG 3 Track) ──

class CopilotChatRequest(BaseModel):
    query: str = Field(..., json_schema_extra={"example": "8वें महीने में तेज सिरदर्द और 150/100 बीपी है, क्या प्राथमिक देखभाल दें?"})
    language: str = Field("hi", json_schema_extra={"example": "hi"})
    case_context: Optional[Dict[str, Any]] = None


class CounselingScriptRequest(BaseModel):
    patient_name: str = Field(..., json_schema_extra={"example": "Sunita Devi"})
    village: str = Field(..., json_schema_extra={"example": "Rampur"})
    risk_level: str = Field(..., json_schema_extra={"example": "RED"})
    danger_signs: Dict[str, bool] = Field(default_factory=dict)
    blood_pressure: str = Field(..., json_schema_extra={"example": "155/98"})
    haemoglobin: float = Field(..., json_schema_extra={"example": 8.5})
    language: str = Field("hi", json_schema_extra={"example": "hi"})


class DifferentialDiagnosisRequest(BaseModel):
    patient_id: str = Field(..., json_schema_extra={"example": "CASE-ID"})
    patient_name: str = Field(..., json_schema_extra={"example": "Pregnant woman"})
    blood_pressure: str = Field(..., json_schema_extra={"example": "165/110"})
    haemoglobin: float = Field(..., json_schema_extra={"example": 6.8})
    danger_signs: Dict[str, bool] = Field(default_factory=dict)


@app.post("/api/v1/ai/copilot", status_code=status.HTTP_200_OK)
def api_chat_copilot(request: CopilotChatRequest, user: TokenData = Depends(require_roles("ASHA", "MEDICAL_OFFICER", "SUPERVISOR", "ADMIN"))) -> Dict[str, Any]:
    """
    Conversational SakhiAI Copilot for frontline health workers (MoHFW / WHO guidelines).
    """
    reject_local_llm_in_production()
    import llm_engine
    return llm_engine.chat_sakhi_copilot(
        query=request.query,
        case_context=request.case_context,
        language=request.language
    )


@app.post("/api/v1/ai/counseling-script", status_code=status.HTTP_200_OK)
def api_generate_counseling_script(request: CounselingScriptRequest, user: TokenData = Depends(require_roles("ASHA", "MEDICAL_OFFICER", "SUPERVISOR", "ADMIN"))) -> Dict[str, Any]:
    """
    Generates culturally empathetic, vernacular family counseling scripts (Hindi, Bengali, Marathi, Kannada, English)
    to help ASHA workers persuade hesitant rural families for emergency hospital transfer.
    """
    reject_local_llm_in_production()
    import llm_engine
    return llm_engine.generate_family_counseling_script(
        patient_name=request.patient_name,
        village=request.village,
        risk_level=request.risk_level,
        danger_signs=request.danger_signs,
        blood_pressure=request.blood_pressure,
        haemoglobin=request.haemoglobin,
        language=request.language
    )


@app.post("/api/v1/ai/differential-diagnosis", status_code=status.HTTP_200_OK)
def api_generate_differential(request: DifferentialDiagnosisRequest, user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "SUPERVISOR", "ADMIN"))) -> Dict[str, Any]:
    """
    Generates structured Medical Officer differential diagnosis and pre-hospital management instructions.
    """
    reject_local_llm_in_production()
    import llm_engine
    return llm_engine.generate_clinical_differential(
        patient_id=request.patient_id,
        patient_name=request.patient_name,
        blood_pressure=request.blood_pressure,
        haemoglobin=request.haemoglobin,
        danger_signs=request.danger_signs
    )


# ── Speech-LLM Unified Audio-to-Reasoning Endpoints (SDG Track) ──

class SpeechLLMTextRequest(BaseModel):
    spoken_transcript: str = Field(..., json_schema_extra={"example": "मरीज सुनीता देवी गांव रामपुर बीपी एक सौ साठ बटा एक सौ दस हीमोग्लोबिन छह दशमलव आठ तेज सिरदर्द और खून बहना"})


@app.post("/api/v1/speech-llm/process-transcript", status_code=status.HTTP_200_OK)
def api_speech_llm_process_transcript(
    request: SpeechLLMTextRequest,
    user: TokenData = Depends(get_current_user),
) -> Dict[str, Any]:
    """
    Direct Speech-LLM Pipeline: Applies LLM phonetic corrections, extracts clinical entities,
    evaluates multi-metric triage, and generates differential diagnosis & counseling in a single pass.
    """
    reject_local_llm_in_production()
    from speech_llm import SpeechLLMProcessor
    return SpeechLLMProcessor.extract_and_reason_from_speech(request.spoken_transcript)


@app.post("/api/v1/speech-llm/process-audio", status_code=status.HTTP_200_OK)
async def api_speech_llm_process_audio(
    file: UploadFile = File(...),
    user: TokenData = Depends(get_current_user),
) -> Dict[str, Any]:
    """
    Unified Audio-to-Reasoning Speech-LLM: Ingests audio bytes and outputs complete medical assessment.
    """
    reject_local_llm_in_production()
    from speech_llm import SpeechLLMProcessor
    content = await file.read()
    return SpeechLLMProcessor.transcribe_audio_with_llm(content, filename=file.filename or "audio.wav")


# ── Case Management & FHIR Export ──

@app.get("/cases", status_code=status.HTTP_200_OK)
def list_synced_cases(user: TokenData = Depends(get_current_user)) -> Dict[str, Any]:
    with SessionLocal() as db:
        db_cases = list_cases(db)
        if db_cases:
            cases_flat = []
            for c in db_cases:
                asm = c.get("assessment") or {}
                cases_flat.append({
                    "patient_id": c["case_id"],
                    "patient_name": c["patient_name"],
                    "village": c["village"],
                    "blood_pressure": asm.get("blood_pressure"),
                    "haemoglobin": asm.get("haemoglobin"),
                    "danger_signs": asm.get("danger_signs", {}),
                    "risk_level": asm.get("risk_level", "GREEN"),
                    "risk_score": asm.get("risk_score", 10),
                    "clinical_rationale": asm.get("clinical_rationale"),
                    "recommended_protocol": asm.get("recommended_protocol"),
                    "sync_status": c.get("sync_status", "Synced"),
                    "doctor_advisory": c.get("doctor_advisory"),
                    "ambulance_status": c.get("ambulance_status")
                })
            return {
                "count": len(cases_flat),
                "cases": cases_flat
            }

        return {"count": 0, "cases": []}


@app.get("/cases/{patient_id}", status_code=status.HTTP_200_OK)
def get_single_case(patient_id: str, user: TokenData = Depends(get_current_user)) -> Dict[str, Any]:
    with SessionLocal() as db:
        detail = get_case_detail(db, patient_id)
        if detail:
            return detail
    raise HTTPException(status_code=404, detail=f"Case {patient_id} not found")


@app.get("/fhir/export/{patient_id}", status_code=status.HTTP_200_OK)
def export_fhir_bundle(patient_id: str, user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "SUPERVISOR", "ADMIN"))) -> Dict[str, Any]:
    with SessionLocal() as db:
        detail = get_case_detail(db, patient_id)
        if detail:
            asm = detail.get("assessment") or {}
            return generate_fhir_bundle(
                patient_id=detail["case_id"],
                patient_name=detail["patient_name"],
                village=detail["village"],
                blood_pressure=asm.get("blood_pressure"),
                haemoglobin=asm.get("haemoglobin"),
                danger_signs=asm.get("danger_signs") or {},
                risk_level=asm.get("risk_level", "GREEN")
            )

    raise HTTPException(status_code=404, detail=f"Case {patient_id} not found for FHIR export")


# ── Phase 3: Durable Backend API v1 Endpoints ──

@app.post("/api/v1/auth/login")
@app.post("/api/auth/login")
def api_login(req: LoginRequest):
    if APP_ENV == "production":
        raise HTTPException(status_code=status.HTTP_410_GONE, detail="Password login is disabled; use Supabase Auth")
    with SessionLocal() as db:
        user = db.query(UserModel).filter(UserModel.username == req.username).first()
        if not user or not verify_password(req.password, user.password_hash):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid username or password"
            )
        token = create_access_token(
            user_id=user.id,
            username=user.username,
            role=user.role,
            facility_id=user.facility_id
        )
        return {
            "access_token": token,
            "token_type": "bearer",
            "user": {
                "id": user.id,
                "username": user.username,
                "full_name": user.full_name,
                "role": user.role,
                "facility_id": user.facility_id,
                "email": user.email
            }
        }


@app.get("/api/v1/auth/me")
def api_get_current_user_profile(user: TokenData = Depends(get_current_user)):
    if APP_ENV == "production" and os.getenv("AUTH_PROVIDER", "supabase").lower() == "supabase":
        return {
            "id": user.user_id,
            "username": user.username,
            "full_name": user.username,
            "role": user.role,
            "facility_id": user.facility_id,
            "email": None
        }
    with SessionLocal() as db:
        db_user = db.query(UserModel).filter(UserModel.id == user.user_id).first()
        if not db_user:
            return {
                "id": user.user_id,
                "username": user.username,
                "role": user.role,
                "facility_id": user.facility_id,
                "full_name": user.username
            }
        return {
            "id": db_user.id,
            "username": db_user.username,
            "full_name": db_user.full_name,
            "role": db_user.role,
            "facility_id": db_user.facility_id,
            "email": db_user.email
        }


@app.get("/api/v1/cases")
def api_list_cases(
    facility_id: Optional[str] = None,
    risk_level: Optional[str] = None,
    status: Optional[str] = None,
    include_demo: bool = True,
    limit: int = 50,
    offset: int = 0,
    user: TokenData = Depends(get_current_user)
):
    target_facility_id = facility_id
    if user.role not in ("ADMIN", "SUPERVISOR") and user.facility_id:
        target_facility_id = user.facility_id

    with SessionLocal() as db:
        cases = list_cases(
            db,
            facility_id=target_facility_id,
            risk_level=risk_level,
            status_filter=status,
            include_demo=include_demo,
            limit=limit,
            offset=offset
        )
        return {
            "count": len(cases),
            "cases": cases
        }


@app.get("/api/v1/cases/{case_id}")
def api_get_case_detail(case_id: str, user: TokenData = Depends(get_current_user)):
    with SessionLocal() as db:
        detail = get_case_detail(db, case_id)
        if not detail:
            raise HTTPException(status_code=404, detail=f"Case {case_id} not found")
        # Enforce facility scoping
        if user.role not in ("ADMIN", "SUPERVISOR") and user.facility_id:
            if detail.get("facility_id") and detail.get("facility_id") != user.facility_id:
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail=f"Access denied: User assigned to {user.facility_id} cannot access case from {detail.get('facility_id')}"
                )
        return detail


@app.post("/api/v1/demo/seed")
def api_seed_demo_fixtures(user: TokenData = Depends(get_current_user)):
    if APP_ENV == "production" and user.role != "ADMIN":
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Seeding demo fixtures is disabled in production")
    from seed_demo import seed_demo_data
    with SessionLocal() as db:
        seed_demo_data(db)
    return {"status": "SUCCESS", "message": "Demo fixtures successfully seeded"}


@app.post("/api/v1/cases/{case_id}/acknowledge")
async def api_acknowledge_case(
    case_id: str,
    req: AcknowledgeCaseRequest,
    user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "ADMIN"))
):
    with SessionLocal() as db:
        if user.role != "ADMIN" and user.facility_id:
            existing = get_case_detail(db, case_id)
            if existing and existing.get("facility_id") and existing.get("facility_id") != user.facility_id:
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail=f"Access denied: Case {case_id} belongs to facility {existing.get('facility_id')}"
                )

        updated = acknowledge_case(
            db,
            case_id=case_id,
            actor_id=user.user_id,
            actor_role=user.role,
            advisory=req.advisory_text,
            referral_facility_id=req.referral_facility_id
        )
        if not updated:
            raise HTTPException(status_code=404, detail=f"Case {case_id} not found")

        # Keep in-memory synced_cases_db in sync
        if case_id in synced_cases_db:
            synced_cases_db[case_id]["doctor_advisory"] = req.advisory_text

        await broadcast_sse_event("CASE_UPDATED", {"case": updated})

        try:
            await onesignal_service.send_clinical_advisory_notification(
                patient_id=case_id,
                patient_name=updated["patient_name"],
                advisory_text=req.advisory_text,
                doctor_or_operator_name=user.username
            )
        except Exception:
            pass

        return updated


@app.post("/api/v1/cases/{case_id}/transport")
async def api_transport_case(
    case_id: str,
    req: TransportCaseRequest,
    user: TokenData = Depends(require_roles("DISPATCHER", "MEDICAL_OFFICER", "ADMIN"))
):
    with SessionLocal() as db:
        updated = update_transport(
            db,
            case_id=case_id,
            actor_id=user.user_id,
            ambulance_status=f"{req.vehicle_id} Dispatched",
            driver_phone=req.driver_phone,
            destination=req.destination_facility
        )
        if not updated:
            raise HTTPException(status_code=404, detail=f"Case {case_id} not found")

        if case_id in synced_cases_db:
            synced_cases_db[case_id]["ambulance_status"] = f"{req.vehicle_id} Dispatched"

        await broadcast_sse_event("CASE_UPDATED", {"case": updated})
        return updated


@app.get("/api/v1/cases/{case_id}/events")
def api_get_case_events(case_id: str, user: TokenData = Depends(get_current_user)):
    with SessionLocal() as db:
        case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
        if not case:
            raise HTTPException(status_code=404, detail=f"Case {case_id} not found")
        events = db.query(CaseEventModel).filter(CaseEventModel.case_id == case_id).order_by(CaseEventModel.occurred_at.asc()).all()
        return {
            "case_id": case_id,
            "count": len(events),
            "events": [
                {
                    "id": e.id,
                    "event_type": e.event_type,
                    "actor_id": e.actor_id,
                    "actor_role": e.actor_role,
                    "summary": e.summary,
                    "details": json.loads(e.details_json) if e.details_json else None,
                    "occurred_at": e.occurred_at
                }
                for e in events
            ]
        }


@app.post("/sync/batch")
@app.post("/api/v1/sync/batch")
async def api_sync_batch(request: Request, user: TokenData = Depends(get_current_user)):
    payload = await request.json()
    items_list = []
    if isinstance(payload, list):
        items_list = payload
    elif isinstance(payload, dict) and "items" in payload:
        items_list = payload["items"]
    elif isinstance(payload, dict):
        items_list = [payload]

    results = []
    with SessionLocal() as db:
        for item in items_list:
            item_dict = item if isinstance(item, dict) else item.model_dump()
            if APP_ENV == "production":
                item_dict["worker_id"] = user.user_id
            elif TEST_MODE:
                item_dict.setdefault("worker_id", "WKR-101")
                item_dict.setdefault("facility_id", "FAC-01")
            res = ingest_sync_case_batch(db, item_dict)
            results.append(res)
    return {
        "status": "ACKNOWLEDGED",
        "processed": len(results),
        "items": results
    }


@app.get("/api/v1/facilities")
def api_list_facilities(user: TokenData = Depends(get_current_user)):
    with SessionLocal() as db:
        facs = db.query(FacilityModel).all()
        return {
            "count": len(facs),
            "facilities": [
                {
                    "id": f.id,
                    "name": f.name,
                    "type": f.type,
                    "catchment_area": f.catchment_area,
                    "contact_phone": f.contact_phone
                }
                for f in facs
            ]
        }


@app.get("/api/v1/workers")
def api_list_workers(user: TokenData = Depends(get_current_user)):
    with SessionLocal() as db:
        workers = db.query(WorkerModel).all()
        return {
            "count": len(workers),
            "workers": [
                {
                    "id": w.id,
                    "name": w.name,
                    "role": w.role,
                    "phone": w.phone,
                    "facility_id": w.facility_id,
                    "locale": w.locale,
                    "status": w.status,
                    "last_seen_at": w.last_seen_at
                }
                for w in workers
            ]
        }


# ── Phase 4: Transport Board, Facility Routing, and Notification Escalation ──

@app.get("/api/v1/transport/requests")
def api_list_transport_requests(
    status: Optional[str] = None,
    user: TokenData = Depends(require_roles("DISPATCHER", "MEDICAL_OFFICER", "ADMIN", "SUPERVISOR")),
):
    """
    Returns auditable transport coordination requests for the 108 Transport Board.
    Truthful state tracking: REQUESTED, CALL_ATTEMPTED, CONFIRMED, EN_ROUTE, ARRIVED, FAILED.
    """
    with SessionLocal() as db:
        requests = list_transport_requests(db, status_filter=status)
        return {"count": len(requests), "items": requests}


@app.post("/api/v1/transport/requests")
async def api_update_transport_request(
    payload: TransportBoardUpdateRequest,
    user: TokenData = Depends(require_roles("DISPATCHER", "MEDICAL_OFFICER", "ADMIN", "SUPERVISOR"))
):
    """
    Updates or creates transport dispatch record with call-and-confirm audit notes.
    """
    with SessionLocal() as db:
        res = create_or_update_transport_request(
            db=db,
            case_id=payload.case_id,
            status=payload.status,
            actor_id=user.user_id,
            vehicle_id=payload.vehicle_id,
            destination_facility_id=payload.destination_facility_id,
            destination_facility_name=payload.destination_facility_name,
            driver_name=payload.driver_name,
            driver_phone=payload.driver_phone,
            call_attempt_notes=payload.call_attempt_notes
        )
        if not res:
            raise HTTPException(status_code=404, detail=f"Case {payload.case_id} not found")

        # Sync in-memory DB if present
        if payload.case_id in synced_cases_db:
            synced_cases_db[payload.case_id]["ambulance_status"] = f"{payload.vehicle_id or 'Ambulance'} - {payload.status}"

        await broadcast_sse_event("TRANSPORT_UPDATED", {"transport": res, "case_id": payload.case_id})
        return res


@app.get("/api/v1/cases/{case_id}/recommend-facility")
def api_recommend_facility(case_id: str, user: TokenData = Depends(get_current_user)):
    """
    Capability-based facility referral recommendation:
    Severe anemia (Hb < 7) or Antepartum Hemorrhage -> FRU / CHC with Blood Bank;
    Standard triage -> Primary Health Centre (PHC).
    """
    with SessionLocal() as db:
        rec = recommend_facility_for_case(db, case_id)
        return rec


@app.get("/api/v1/clinical/protocols")
def api_clinical_protocols(user: TokenData = Depends(get_current_user)):
    """
    Returns signed-off clinical protocol metadata (mohfw-hrp-v1.0),
    clinical reviewer credentials, triage matrix definitions, and clinical scope boundaries.
    """
    return get_clinical_protocol_metadata()


@app.get("/api/v1/notifications/logs")
def api_list_notifications(case_id: Optional[str] = None, user: TokenData = Depends(get_current_user)):
    """
    Returns auditable notification delivery logs with truthful status tracking:
    QUEUED, SENT, DELIVERED, FAILED, NOT_CONFIGURED.
    """
    with SessionLocal() as db:
        logs = list_notification_logs(db, case_id=case_id)
        return {"count": len(logs), "logs": logs}


@app.post("/api/v1/notifications/escalate/{case_id}")
async def api_escalate_notification(
    case_id: str,
    user: TokenData = Depends(require_roles("MEDICAL_OFFICER", "ADMIN", "SUPERVISOR", "DISPATCHER"))
):
    """
    Triggers emergency multi-channel notification escalation for an urgent case.
    Dispatches minimal privacy-safe SMS and escalates to Block Supervisor if primary channel unconfigured.
    """
    with SessionLocal() as db:
        detail = get_case_detail(db, case_id)
        if not detail:
            raise HTTPException(status_code=404, detail=f"Case {case_id} not found")

        asm = detail.get("assessment") or {}
        danger_signs_dict = asm.get("danger_signs") or {}
        signs_list = [k.replace("_", " ").title() for k, v in danger_signs_dict.items() if v]

        res = trigger_emergency_escalation(
            db=db,
            case_id=case_id,
            patient_name=detail.get("patient_name", "Unknown Patient"),
            village=detail.get("village", "Unknown Village"),
            urgency=detail.get("risk_level", "RED"),
            danger_signs=signs_list
        )

        await broadcast_sse_event("ESCALATION_TRIGGERED", {"case_id": case_id, "escalation": res})
        return res


class SmsDeliveryCallback(BaseModel):
    provider_ref: str
    status: str  # DELIVERED, FAILED, UNDELIVERED
    timestamp: Optional[int] = None


@app.post("/api/v1/notifications/callbacks/sms")
def api_sms_delivery_callback(callback: SmsDeliveryCallback):
    """
    Records provider delivery receipts without modifying clinical acknowledgement states.
    Phase 4 requirement: Separates notification state from clinical state.
    """
    with SessionLocal() as db:
        log_entry = db.query(NotificationLogModel).filter(NotificationLogModel.provider_ref == callback.provider_ref).first()
        if not log_entry:
            log_entry = db.query(NotificationLogModel).filter(NotificationLogModel.id == callback.provider_ref).first()
        if not log_entry:
            raise HTTPException(status_code=404, detail="Notification log not found for receipt")

        log_entry.status = callback.status.upper()
        if callback.status.upper() == "DELIVERED":
            log_entry.delivered_at = callback.timestamp or int(time.time())
        db.commit()
        return {"status": "SUCCESS", "log_id": log_entry.id, "delivery_status": log_entry.status}


@app.post("/api/v1/demo/purge")
def api_purge_demo_cases(user: TokenData = Depends(require_roles("ADMIN"))):
    """
    Purges all records marked with is_demo=True without affecting real production cases.
    """
    with SessionLocal() as db:
        demo_cases = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.is_demo == True).all()
        count = len(demo_cases)
        for c in demo_cases:
            db.delete(c)
        db.commit()
        return {"status": "SUCCESS", "purged_count": count}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
