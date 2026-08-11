"""
SakhiCare FastAPI Backend Server
Sync service & FHIR converter for SakhiCare offline-first maternal triage platform.
"""

from typing import Dict, Any, List, Optional
import re
from datetime import datetime, timezone
from fastapi import FastAPI, status
from pydantic import BaseModel, Field

from fhir_converter import generate_fhir_bundle

app = FastAPI(
    title="SakhiCare API",
    description="Backend sync service & FHIR converter for SakhiCare maternal triage platform",
    version="1.0.0",
)

# In-memory storage for synced cases
synced_cases_db: Dict[str, Dict[str, Any]] = {}


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
    timestamp: Optional[str] = Field(None, json_schema_extra={"example": "2026-08-10T02:00:00Z"})


class VoiceParseRequest(BaseModel):
    speech_text: str = Field(..., json_schema_extra={"example": "मरीज सुनीता देवी, गांव रामपुर, बीपी 145/95, हीमोग्लोबिन 10.2, बुखार और खून बहना"})


def calculate_triage_risk(danger_signs: DangerSignsModel, bp: str) -> str:
    # High BP check >= 140/90
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
    if danger_signs.fever or danger_signs.headache:
        return "AMBER"
    return "GREEN"


def parse_speech_dictation(text: str) -> Dict[str, Any]:
    """
    Intelligent multilingual speech regex parser extracting maternal health data from English & Devanagari Hindi dictations.
    """
    clean_text = text.strip()
    lower_text = clean_text.lower()

    # 1. Patient Name (English & Hindi "मरीज" / "नाम")
    name_en = re.search(r'(?:patient|name|patient name)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\svillage|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|$)', clean_text, re.IGNORECASE)
    name_hi = re.search(r'(?:मरीज|मरीज़|नाम)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sगांव|\sगाँव|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)]', clean_text, re.IGNORECASE)

    raw_name = (name_hi.group(1).strip() if name_hi else None) or (name_en.group(1).strip() if name_en else "Sunita Devi")

    # 2. Village (English & Hindi "गांव")
    village_en = re.search(r'(?:village|from)\s+(?:is\s+)?([a-zA-Z\s]+?)(?:,|\sbp|\sblood|\shaemoglobin|\shb|\sdanger|\sbleeding|$)', clean_text, re.IGNORECASE)
    village_hi = re.search(r'(?:गांव|गाँव|क्षेत्र)\s+(?:है\s+)?([\u0900-\u097F\s]+?)(?:,|\sबीपी|\sरक्तचाप|\sहीमोग्लोबिन|\sबुखार|\sखून|$)]', clean_text, re.IGNORECASE)

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


@app.get("/health", status_code=status.HTTP_200_OK)
def health_check() -> Dict[str, str]:
    return {"status": "ok", "service": "SakhiCare Sync Server", "version": "1.0.0"}


@app.post("/sync", status_code=status.HTTP_200_OK)
def sync_case(payload: AssessmentSyncPayload) -> Dict[str, Any]:
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
        "sync_status": "Synced"
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

    return {
        "message": "Case successfully synced and ingested into SakhiCare registry",
        "patient_id": patient_id,
        "risk_level": risk_level,
        "fhir_bundle_id": fhir_bundle["id"]
    }


@app.get("/cases", status_code=status.HTTP_200_OK)
def list_synced_cases() -> Dict[str, Any]:
    return {
        "count": len(synced_cases_db),
        "cases": list(synced_cases_db.values())
    }


@app.get("/fhir/export/{patient_id}", status_code=status.HTTP_200_OK)
def export_fhir_bundle(patient_id: str) -> Dict[str, Any]:
    if patient_id not in synced_cases_db:
        # Generate sample bundle if not found in db
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


@app.post("/voice-parse", status_code=status.HTTP_200_OK)
def parse_voice(request: VoiceParseRequest) -> Dict[str, Any]:
    return parse_speech_dictation(request.speech_text)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
