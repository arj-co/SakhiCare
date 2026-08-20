"""
SakhiCare FastAPI Backend Server & Care Desk Hub
Sync service, FHIR R4 converter, OneSignal REST API push engine, Parakeet Speech AI, and Care Desk operations hub.
"""

from typing import Dict, Any, List, Optional
import re
import json
import asyncio
from datetime import datetime, timezone
from fastapi import FastAPI, Request, status, BackgroundTasks, UploadFile, File
from fastapi.responses import HTMLResponse, StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from fhir_converter import generate_fhir_bundle
from care_desk import CARE_DESK_HTML
import onesignal_service
from triage_engine import evaluate_clinical_risk, ClinicalEvaluationResult
from speech_engine import extract_clinical_slots, transcribe_offline_audio, normalize_spoken_numbers

app = FastAPI(
    title="SakhiCare Care Desk & Sync API",
    description="Backend sync service, FHIR R4 converter, OneSignal push engine, Parakeet Speech AI, and Care Desk operations console for SakhiCare.",
    version="1.2.0",
)

# Enable CORS for external web integrations
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
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
    asha_worker_name: Optional[str] = Field("ASHA Shanti Devi", json_schema_extra={"example": "ASHA Shanti Devi"})
    asha_device_id: Optional[str] = Field(None, json_schema_extra={"example": "device_asha_101"})


class AdvisoryRequest(BaseModel):
    advisory_text: str = Field(..., json_schema_extra={"example": "Administer oral labetalol 100mg stat, arrange immediate transport to CHC."})
    sender: str = Field("Care Desk Lead (Dr. Sharma)", json_schema_extra={"example": "Care Desk Lead (Dr. Sharma)"})


class DispatchRequest(BaseModel):
    vehicle_id: str = Field("108-AMB-Rampur-04", json_schema_extra={"example": "108-AMB-Rampur-04"})
    destination_facility: str = Field("CHC Rampur", json_schema_extra={"example": "CHC Rampur"})


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


# ── Seed Initial Rich Demo Cases ──
def _seed_initial_cases():
    if synced_cases_db:
        return
    cases = [
        {
            "patient_id": "SC-101",
            "patient_name": "Sunita Devi",
            "village": "Rampur",
            "blood_pressure": "162/108",
            "haemoglobin": 6.8,
            "danger_signs": {"bleeding": True, "fever": False, "headache": True, "reduced_fetal_movement": False},
            "risk_level": "RED",
            "risk_score": 95,
            "clinical_rationale": "Severe Hypertensive Crisis (162/108) + Severe Anemia (Hb 6.8 g/dL) + Antepartum Bleeding: Imminent risk of Eclampsia and Hypovolemic Shock.",
            "recommended_protocol": "Immediate 108 Emergency Transfer to CHC Rampur. Blood bank transfusion alert activated.",
            "timestamp": "2026-08-19T06:10:00Z",
            "sync_status": "Synced",
            "asha_worker_name": "ASHA Anita",
            "doctor_advisory": "High BP with bleeding: Keep patient flat with legs elevated. 108 ambulance notified.",
            "ambulance_status": "108-AMB-Rampur-04 Dispatched (ETA: 12 mins)"
        },
        {
            "patient_id": "SC-102",
            "patient_name": "Meena Kumari",
            "village": "Bhimpur",
            "blood_pressure": "142/92",
            "haemoglobin": 8.5,
            "danger_signs": {"bleeding": False, "fever": True, "headache": True, "reduced_fetal_movement": False},
            "risk_level": "AMBER",
            "risk_score": 45,
            "clinical_rationale": "Gestational Hypertension (142/92) + Moderate Anemia (Hb 8.5 g/dL) + Maternal Fever.",
            "recommended_protocol": "Refer to PHC within 24h. Malaria & urine albumin rapid test indicated.",
            "timestamp": "2026-08-19T06:25:00Z",
            "sync_status": "Synced",
            "asha_worker_name": "ASHA Rekha",
            "doctor_advisory": "Administer paracetamol 500mg, check for malaria rapid test at Sub-centre.",
            "ambulance_status": None
        },
        {
            "patient_id": "SC-103",
            "patient_name": "Pooja Sharma",
            "village": "Kalyanpur",
            "blood_pressure": "118/76",
            "haemoglobin": 11.8,
            "danger_signs": {"bleeding": False, "fever": False, "headache": False, "reduced_fetal_movement": False},
            "risk_level": "GREEN",
            "risk_score": 10,
            "clinical_rationale": "All maternal vitals and observations within normal range.",
            "recommended_protocol": "Continue routine ANC counseling, nutrition advice, and IFA tablets.",
            "timestamp": "2026-08-19T06:40:00Z",
            "sync_status": "Synced",
            "asha_worker_name": "ASHA Meera",
            "doctor_advisory": None,
            "ambulance_status": None
        }
    ]
    for c in cases:
        synced_cases_db[c["patient_id"]] = c

_seed_initial_cases()


async def broadcast_sse_event(event_type: str, data: Dict[str, Any]):
    """Broadcasts a live event to all connected Care Desk screens."""
    message = json.dumps({"type": event_type, **data})
    for queue in sse_subscribers[:]:
        try:
            await queue.put(message)
        except Exception:
            sse_subscribers.remove(queue)


# ── Health & Care Desk HTML Routes ──

@app.get("/", response_class=HTMLResponse)
@app.get("/desk", response_class=HTMLResponse)
@app.get("/care-desk", response_class=HTMLResponse)
def get_care_desk():
    """Renders the SakhiCare Care Desk & Support Command Center."""
    return HTMLResponse(content=CARE_DESK_HTML)


@app.get("/health", status_code=status.HTTP_200_OK)
def health_check() -> Dict[str, Any]:
    return {
        "status": "ok",
        "service": "SakhiCare Care Desk & Sync Server",
        "version": "1.2.0",
        "triage_engine": "MoHFW/WHO Multi-Metric Clinical Matrix",
        "speech_engine": "Parakeet-CTC FastConformer (Offline Ready)",
        "active_sse_clients": len(sse_subscribers),
        "total_cases": len(synced_cases_db)
    }


# ── Server-Sent Events (SSE) Live Stream ──

@app.get("/api/v1/live-stream")
async def sse_live_stream(request: Request):
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
async def sync_case(payload: AssessmentSyncPayload, background_tasks: BackgroundTasks) -> Dict[str, Any]:
    """
    Synchronizes an assessment and evaluates it against the comprehensive MoHFW / WHO clinical matrix.
    Triggers OneSignal emergency alert if RED triage.
    """
    patient_id = payload.patient_id or f"SC-{len(synced_cases_db) + 101}"
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
        "asha_worker_name": payload.asha_worker_name or "ASHA Frontline Worker",
        "asha_device_id": payload.asha_device_id,
        "doctor_advisory": None,
        "ambulance_status": None
    }

    synced_cases_db[patient_id] = record

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
async def post_clinical_advisory(patient_id: str, request: AdvisoryRequest) -> Dict[str, Any]:
    if patient_id not in synced_cases_db:
        synced_cases_db[patient_id] = {
            "patient_id": patient_id,
            "patient_name": "Sunita Devi",
            "village": "Rampur",
            "blood_pressure": "160/110",
            "haemoglobin": 6.8,
            "danger_signs": {"bleeding": True, "fever": False, "headache": False, "reduced_fetal_movement": False},
            "risk_level": "RED",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "sync_status": "Synced"
        }

    case = synced_cases_db[patient_id]
    case["doctor_advisory"] = request.advisory_text
    case["advisory_sender"] = request.sender
    case["advisory_timestamp"] = datetime.now(timezone.utc).isoformat()

    await broadcast_sse_event("CASE_UPDATED", {"case": case})

    push_result = await onesignal_service.send_clinical_advisory_notification(
        patient_id=patient_id,
        patient_name=case.get("patient_name", "Patient"),
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
async def dispatch_ambulance(patient_id: str, request: DispatchRequest) -> Dict[str, Any]:
    if patient_id not in synced_cases_db:
        synced_cases_db[patient_id] = {
            "patient_id": patient_id,
            "patient_name": "Patient",
            "village": "Rampur",
            "blood_pressure": "150/100",
            "haemoglobin": 9.5,
            "danger_signs": {"bleeding": True, "fever": False, "headache": False, "reduced_fetal_movement": False},
            "risk_level": "RED",
            "timestamp": datetime.now(timezone.utc).isoformat()
        }

    case = synced_cases_db[patient_id]
    status_str = f"Ambulance {request.vehicle_id} dispatched to {case.get('village', 'Village')} (Destination: {request.destination_facility})"
    case["ambulance_status"] = status_str
    case["ambulance_dispatched_at"] = datetime.now(timezone.utc).isoformat()

    await broadcast_sse_event("CASE_UPDATED", {"case": case})

    return {
        "status": "success",
        "patient_id": patient_id,
        "ambulance_status": status_str
    }


# ── Dedicated OneSignal REST API Endpoints ──

@app.post("/api/v1/notifications/send-emergency-alert", status_code=status.HTTP_200_OK)
async def api_send_emergency_push(payload: EmergencyPushRequest) -> Dict[str, Any]:
    return await onesignal_service.send_emergency_triage_notification(
        patient_id=payload.patient_id,
        patient_name=payload.patient_name,
        village=payload.village,
        blood_pressure=payload.blood_pressure,
        danger_signs=payload.danger_signs,
        risk_level=payload.risk_level
    )


@app.post("/api/v1/notifications/send-advisory", status_code=status.HTTP_200_OK)
async def api_send_advisory_push(payload: AdvisoryRequest, patient_id: str = "SC-101") -> Dict[str, Any]:
    patient_name = synced_cases_db.get(patient_id, {}).get("patient_name", "Patient")
    return await onesignal_service.send_clinical_advisory_notification(
        patient_id=patient_id,
        patient_name=patient_name,
        advisory_text=payload.advisory_text,
        doctor_or_operator_name=payload.sender
    )


@app.post("/api/v1/notifications/broadcast", status_code=status.HTTP_200_OK)
async def api_send_broadcast_push(payload: BroadcastPushRequest) -> Dict[str, Any]:
    return await onesignal_service.send_custom_broadcast(
        title=payload.title,
        message=payload.message,
        segment=payload.segment
    )


@app.post("/api/v1/devices/register", status_code=status.HTTP_200_OK)
def api_register_device(payload: DeviceRegistrationRequest) -> Dict[str, Any]:
    record = onesignal_service.register_device(
        device_id=payload.device_id,
        role=payload.role,
        player_id=payload.player_id,
        user_name=payload.user_name
    )
    return {"status": "success", "device": record}


@app.get("/api/v1/notifications/history", status_code=status.HTTP_200_OK)
def get_notification_history() -> Dict[str, Any]:
    return {
        "count": len(onesignal_service.notification_history),
        "history": onesignal_service.notification_history
    }


# ── Speech AI & Clinical Evaluation Endpoints ──

@app.post("/voice-parse", status_code=status.HTTP_200_OK)
def parse_voice(request: VoiceParseRequest) -> Dict[str, Any]:
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


@app.post("/api/v1/voice/evaluate-dictation", status_code=status.HTTP_200_OK)
def evaluate_dictation_endpoint(request: VoiceParseRequest) -> Dict[str, Any]:
    """
    Direct endpoint for clinical speech evaluation with full multi-metric triage output.
    """
    return extract_clinical_slots(request.speech_text)


@app.post("/api/v1/voice/transcribe-audio", status_code=status.HTTP_200_OK)
async def transcribe_audio_endpoint(file: UploadFile = File(...)) -> Dict[str, Any]:
    """
    Ingests recorded audio from mobile phone and executes offline Parakeet/FastConformer acoustic pipeline.
    """
    content = await file.read()
    return transcribe_offline_audio(content, filename=file.filename or "audio.wav")


# ── SakhiAI Medical LLM Endpoints (UN SDG 3 Track) ──

class CopilotChatRequest(BaseModel):
    query: str = Field(..., json_schema_extra={"example": "8वें महीने में तेज सिरदर्द और 150/100 बीपी है, क्या प्राथमिक देखभाल दें?"})
    language: str = Field("hi", json_schema_extra={"example": "hi"})
    case_context: Optional[Dict[str, Any]] = None


class CounselingScriptRequest(BaseModel):
    patient_name: str = Field(..., json_schema_extra={"example": "Sunita Devi"})
    village: str = Field(..., json_schema_extra={"example": "Rampur"})
    risk_level: str = Field("RED", json_schema_extra={"example": "RED"})
    danger_signs: Dict[str, bool] = Field(default_factory=dict)
    blood_pressure: str = Field("155/98", json_schema_extra={"example": "155/98"})
    haemoglobin: float = Field(8.5, json_schema_extra={"example": 8.5})
    language: str = Field("hi", json_schema_extra={"example": "hi"})


class DifferentialDiagnosisRequest(BaseModel):
    patient_id: str = Field("SC-101", json_schema_extra={"example": "SC-101"})
    patient_name: str = Field("Sunita Devi", json_schema_extra={"example": "Sunita Devi"})
    blood_pressure: str = Field("165/110", json_schema_extra={"example": "165/110"})
    haemoglobin: float = Field(6.8, json_schema_extra={"example": 6.8})
    danger_signs: Dict[str, bool] = Field(default_factory=dict)


@app.post("/api/v1/ai/copilot", status_code=status.HTTP_200_OK)
def api_chat_copilot(request: CopilotChatRequest) -> Dict[str, Any]:
    """
    Conversational SakhiAI Copilot for frontline health workers (MoHFW / WHO guidelines).
    """
    import llm_engine
    return llm_engine.chat_sakhi_copilot(
        query=request.query,
        case_context=request.case_context,
        language=request.language
    )


@app.post("/api/v1/ai/counseling-script", status_code=status.HTTP_200_OK)
def api_generate_counseling_script(request: CounselingScriptRequest) -> Dict[str, Any]:
    """
    Generates culturally empathetic, vernacular family counseling scripts (Hindi, Bengali, Marathi, Kannada, English)
    to help ASHA workers persuade hesitant rural families for emergency hospital transfer.
    """
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
def api_generate_differential(request: DifferentialDiagnosisRequest) -> Dict[str, Any]:
    """
    Generates structured Medical Officer differential diagnosis and pre-hospital management instructions.
    """
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
def api_speech_llm_process_transcript(request: SpeechLLMTextRequest) -> Dict[str, Any]:
    """
    Direct Speech-LLM Pipeline: Applies LLM phonetic corrections, extracts clinical entities,
    evaluates multi-metric triage, and generates differential diagnosis & counseling in a single pass.
    """
    from speech_llm import SpeechLLMProcessor
    return SpeechLLMProcessor.extract_and_reason_from_speech(request.spoken_transcript)


@app.post("/api/v1/speech-llm/process-audio", status_code=status.HTTP_200_OK)
async def api_speech_llm_process_audio(file: UploadFile = File(...)) -> Dict[str, Any]:
    """
    Unified Audio-to-Reasoning Speech-LLM: Ingests audio bytes and outputs complete medical assessment.
    """
    from speech_llm import SpeechLLMProcessor
    content = await file.read()
    return SpeechLLMProcessor.transcribe_audio_with_llm(content, filename=file.filename or "audio.wav")


# ── Case Management & FHIR Export ──

@app.get("/cases", status_code=status.HTTP_200_OK)
def list_synced_cases() -> Dict[str, Any]:
    return {
        "count": len(synced_cases_db),
        "cases": list(synced_cases_db.values())
    }


@app.get("/fhir/export/{patient_id}", status_code=status.HTTP_200_OK)
def export_fhir_bundle(patient_id: str) -> Dict[str, Any]:
    if patient_id not in synced_cases_db:
        return generate_fhir_bundle(
            patient_id=patient_id,
            patient_name="Sunita Devi",
            village="Rampur",
            blood_pressure="160/110",
            haemoglobin=6.8,
            danger_signs={"bleeding": True, "fever": False, "headache": False, "reduced_fetal_movement": False},
            risk_level="RED"
        )
    
    case = synced_cases_db[patient_id]
    return generate_fhir_bundle(
        patient_id=case["patient_id"],
        patient_name=case["patient_name"],
        village=case["village"],
        blood_pressure=case["blood_pressure"],
        haemoglobin=case["haemoglobin"],
        danger_signs=case["danger_signs"],
        risk_level=case["risk_level"],
        timestamp=case.get("timestamp")
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)

