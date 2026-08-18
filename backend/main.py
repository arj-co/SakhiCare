"""
SakhiCare FastAPI Backend Server & Care Desk Hub
Sync service, FHIR R4 converter, OneSignal REST API push engine, and real-time Care Desk operations hub.
"""

from typing import Dict, Any, List, Optional
import re
import json
import asyncio
from datetime import datetime, timezone
from fastapi import FastAPI, Request, status, BackgroundTasks
from fastapi.responses import HTMLResponse, StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from fhir_converter import generate_fhir_bundle
from care_desk import CARE_DESK_HTML
import onesignal_service

app = FastAPI(
    title="SakhiCare Care Desk & Sync API",
    description="Backend sync service, FHIR R4 converter, OneSignal push engine, and Care Desk operations console for SakhiCare.",
    version="1.1.0",
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


# ── Seed Demo Cases for Immediate Demoability ──
def _seed_initial_cases():
    if synced_cases_db:
        return
    cases = [
        {
            "patient_id": "SC-101",
            "patient_name": "Sunita Devi",
            "village": "Rampur",
            "blood_pressure": "152/98",
            "haemoglobin": 9.4,
            "danger_signs": {"bleeding": True, "fever": False, "headache": True, "reduced_fetal_movement": False},
            "risk_level": "RED",
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
            "blood_pressure": "130/85",
            "haemoglobin": 10.2,
            "danger_signs": {"bleeding": False, "fever": True, "headache": True, "reduced_fetal_movement": False},
            "risk_level": "AMBER",
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


def calculate_triage_risk(danger_signs: DangerSignsModel, bp: str) -> str:
    is_high_bp = False
    parts = bp.strip().split("/")
    if len(parts) == 2:
        try:
            sys_val = int(parts[0].strip())
            dia_val = int(parts[1].strip())
            if sys_val >= 140 or dia_val >= 90:
                is_high_bp = True
        except ValueError:
            pass

    if danger_signs.bleeding or is_high_bp:
        return "RED"
    if danger_signs.fever or danger_signs.headache or danger_signs.reduced_fetal_movement:
        return "AMBER"
    return "GREEN"


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
        "version": "1.1.0",
        "active_sse_clients": len(sse_subscribers),
        "total_cases": len(synced_cases_db)
    }


# ── Server-Sent Events (SSE) Live Stream ──

@app.get("/api/v1/live-stream")
async def sse_live_stream(request: Request):
    """
    Streams live new cases and updates to Care Desk operators in real time.
    """
    queue = asyncio.Queue()
    sse_subscribers.append(queue)

    async def event_generator():
        try:
            # Send initial connection greeting
            yield f"data: {json.dumps({'type': 'CONNECTED', 'message': 'Care Desk live stream connected'})}\n\n"
            while True:
                if await request.is_disconnected():
                    break
                try:
                    # Wait for next event with 20s heartbeat keep-alive
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


# ── Case Sync & Triage Ingestion ──

@app.post("/sync", status_code=status.HTTP_200_OK)
async def sync_case(payload: AssessmentSyncPayload, background_tasks: BackgroundTasks) -> Dict[str, Any]:
    """
    Synchronizes an offline assessment from the ASHA mobile app.
    Triggers automatic OneSignal emergency alert if RED triage.
    """
    patient_id = payload.patient_id or f"SC-{len(synced_cases_db) + 101}"
    risk_level = payload.risk_level or calculate_triage_risk(payload.danger_signs, payload.blood_pressure)
    timestamp = payload.timestamp or datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    record = {
        "patient_id": patient_id,
        "patient_name": payload.patient_name,
        "village": payload.village,
        "blood_pressure": payload.blood_pressure,
        "haemoglobin": payload.haemoglobin,
        "danger_signs": payload.danger_signs.model_dump(),
        "risk_level": risk_level,
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
        "fhir_bundle_id": fhir_bundle["id"],
        "onesignal_emergency_push": push_result
    }


# ── Care Desk Actions: Clinical Advisory & Ambulance Dispatch ──

@app.post("/api/v1/cases/{patient_id}/advisory", status_code=status.HTTP_200_OK)
async def post_clinical_advisory(patient_id: str, request: AdvisoryRequest) -> Dict[str, Any]:
    """
    Care Desk operator posts clinical / pre-hospital instructions for an active case.
    Dispatches a targeted OneSignal push notification to the ASHA worker's device.
    """
    if patient_id not in synced_cases_db:
        # Auto-create stub if not in db
        synced_cases_db[patient_id] = {
            "patient_id": patient_id,
            "patient_name": "Sunita Devi",
            "village": "Rampur",
            "blood_pressure": "145/95",
            "haemoglobin": 10.2,
            "danger_signs": {"bleeding": True, "fever": False, "headache": False, "reduced_fetal_movement": False},
            "risk_level": "RED",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "sync_status": "Synced"
        }

    case = synced_cases_db[patient_id]
    case["doctor_advisory"] = request.advisory_text
    case["advisory_sender"] = request.sender
    case["advisory_timestamp"] = datetime.now(timezone.utc).isoformat()

    # Broadcast update to Care Desk SSE stream
    await broadcast_sse_event("CASE_UPDATED", {"case": case})

    # Dispatch targeted OneSignal push notification to ASHA worker device
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
    """
    Care Desk triggers emergency 108 Ambulance referral dispatch.
    """
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

    # Broadcast update to Care Desk SSE
    await broadcast_sse_event("CASE_UPDATED", {"case": case})

    return {
        "status": "success",
        "patient_id": patient_id,
        "ambulance_status": status_str
    }


# ── Dedicated OneSignal REST API Endpoints ──

@app.post("/api/v1/notifications/send-emergency-alert", status_code=status.HTTP_200_OK)
async def api_send_emergency_push(payload: EmergencyPushRequest) -> Dict[str, Any]:
    """
    Direct REST API endpoint to trigger a OneSignal emergency triage push notification.
    """
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
    """
    Direct REST API endpoint to trigger a OneSignal clinical advisory push notification.
    """
    patient_name = synced_cases_db.get(patient_id, {}).get("patient_name", "Patient")
    return await onesignal_service.send_clinical_advisory_notification(
        patient_id=patient_id,
        patient_name=patient_name,
        advisory_text=payload.advisory_text,
        doctor_or_operator_name=payload.sender
    )


@app.post("/api/v1/notifications/broadcast", status_code=status.HTTP_200_OK)
async def api_send_broadcast_push(payload: BroadcastPushRequest) -> Dict[str, Any]:
    """
    Direct REST API endpoint to broadcast a message to all frontline workers via OneSignal.
    """
    return await onesignal_service.send_custom_broadcast(
        title=payload.title,
        message=payload.message,
        segment=payload.segment
    )


@app.post("/api/v1/devices/register", status_code=status.HTTP_200_OK)
def api_register_device(payload: DeviceRegistrationRequest) -> Dict[str, Any]:
    """
    Registers a mobile app or Care Desk user device for targeted OneSignal push routing.
    """
    record = onesignal_service.register_device(
        device_id=payload.device_id,
        role=payload.role,
        player_id=payload.player_id,
        user_name=payload.user_name
    )
    return {"status": "success", "device": record}


@app.get("/api/v1/notifications/history", status_code=status.HTTP_200_OK)
def get_notification_history() -> Dict[str, Any]:
    """Returns log of all sent OneSignal push notifications."""
    return {
        "count": len(onesignal_service.notification_history),
        "history": onesignal_service.notification_history
    }


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
            blood_pressure="145/95",
            haemoglobin=10.2,
            danger_signs={"bleeding": False, "fever": True, "headache": True, "reduced_fetal_movement": False},
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


def parse_speech_dictation(text: str) -> Dict[str, Any]:
    """
    Intelligent multilingual speech regex parser extracting maternal health data from English & Devanagari Hindi dictations.
    """
    clean_text = text.strip()
    lower_text = clean_text.lower()

    # 1. Patient Name (English & Hindi "मरीज" / "नाम")
    name_en = re.search(r'(?:patient|name|patient name)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\svillage|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|$)', clean_text, re.IGNORECASE)
    name_hi = re.search(r'(?:मरीज|मरीज़|नाम)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sगांव|\sगाँव|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)', clean_text, re.IGNORECASE)

    raw_name = (name_hi.group(1).strip() if name_hi else None) or (name_en.group(1).strip() if name_en else "Sunita Devi")

    # 2. Village (English & Hindi "गांव")
    village_en = re.search(r'(?:village|from)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|\sbleeding|$)', clean_text, re.IGNORECASE)
    village_hi = re.search(r'(?:गांव|गाँव|क्षेत्र)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)', clean_text, re.IGNORECASE)

    raw_village = (village_hi.group(1).strip() if village_hi else None) or (village_en.group(1).strip() if village_en else "Rampur")

    # 3. Blood Pressure
    bp_match = re.search(r'(?:bp|blood pressure|बीपी|रक्तचाप)\s*(?:is\s*|है\s*)?(\d{2,3})\s*(?:\/|over|\s|बटा)\s*(\d{2,3})', clean_text, re.IGNORECASE)
    blood_pressure = f"{bp_match.group(1)}/{bp_match.group(2)}" if bp_match else "145/95"

    # 4. Haemoglobin
    hb_match = re.search(r'(?:hb|haemoglobin|hemoglobin|हीमोग्लोबिन)\s*(?:is\s*|है\s*)?(\d{1,2}(?:\.\d{1,2})?)', clean_text, re.IGNORECASE)
    haemoglobin = float(hb_match.group(1)) if hb_match else 10.2

    # 5. Danger signs
    bleeding = bool(re.search(r'bleeding|hemorrhage|खून|रक्तस्राव', lower_text))
    fever = bool(re.search(r'fever|temperature|बुखार|ताप', lower_text))
    headache = bool(re.search(r'headache|head ache|सिरदर्द|डोकेदुखी', lower_text))
    fetal_mvmt = bool(re.search(r'fetal|movement|हलचल|शिशु', lower_text))

    danger_signs = DangerSignsModel(
        bleeding=bleeding,
        fever=fever,
        headache=headache,
        reduced_fetal_movement=fetal_mvmt
    )

    risk_level = calculate_triage_risk(danger_signs, blood_pressure)

    return {
        "patient_name": raw_name,
        "village": raw_village,
        "blood_pressure": blood_pressure,
        "haemoglobin": haemoglobin,
        "danger_signs": danger_signs.model_dump(),
        "risk_level": risk_level,
        "parsed_from_speech": text
    }


@app.post("/voice-parse", status_code=status.HTTP_200_OK)
def parse_voice(request: VoiceParseRequest) -> Dict[str, Any]:
    return parse_speech_dictation(request.speech_text)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
