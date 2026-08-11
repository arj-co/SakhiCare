"""
Automated Pytest Suite for SakhiCare FastAPI Backend & FHIR Engine
"""

from fastapi.testclient import TestClient
from main import app, parse_speech_dictation
from fhir_converter import generate_fhir_bundle

client = TestClient(app)


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "SakhiCare" in data["service"]


def test_sync_case_endpoint():
    payload = {
        "patient_id": "SC-TEST-001",
        "patient_name": "Anita Roy",
        "village": "Rampur",
        "blood_pressure": "145/95",
        "haemoglobin": 10.2,
        "danger_signs": {
            "bleeding": True,
            "fever": False,
            "headache": False,
            "reduced_fetal_movement": False
        }
    }
    response = client.post("/sync", json=payload)
    assert response.status_code == 200
    res_data = response.json()
    assert res_data["patient_id"] == "SC-TEST-001"
    assert res_data["risk_level"] == "RED"

    # Verify listing synced cases
    list_res = client.get("/cases")
    assert list_res.status_code == 200
    cases_data = list_res.json()
    assert cases_data["count"] >= 1
    assert any(c["patient_id"] == "SC-TEST-001" for c in cases_data["cases"])


def test_fhir_export_endpoint():
    response = client.get("/fhir/export/SC-TEST-001")
    assert response.status_code == 200
    bundle = response.json()
    assert bundle["resourceType"] == "Bundle"
    assert bundle["type"] == "collection"
    assert len(bundle["entry"]) >= 3  # Patient + BP + Hb


def test_voice_parse_endpoint():
    speech = "Patient Sunita Devi village Rampur BP 145 over 95 haemoglobin 10.2 fever true severe headache"
    response = client.post("/voice-parse", json={"speech_text": speech})
    assert response.status_code == 200
    data = response.json()
    assert data["patient_name"] == "Sunita Devi"
    assert data["village"] == "Rampur"
    assert data["blood_pressure"] == "145/95"
    assert data["haemoglobin"] == 10.2
    assert data["danger_signs"]["fever"] is True
    assert data["danger_signs"]["headache"] is True
    assert data["risk_level"] == "RED"


def test_fhir_bundle_generator():
    bundle = generate_fhir_bundle(
        patient_id="SC-999",
        patient_name="Pooja Sharma",
        village="Sitapur",
        blood_pressure="120/80",
        haemoglobin=11.5,
        danger_signs={"bleeding": False, "fever": False, "headache": False, "reduced_fetal_movement": False},
        risk_level="GREEN"
    )
    assert bundle["resourceType"] == "Bundle"
    assert bundle["id"] == "sakhicare-bundle-SC-999"
    # Find Patient resource
    patient_entry = next(e for e in bundle["entry"] if e["resource"]["resourceType"] == "Patient")
    assert patient_entry["resource"]["name"][0]["given"][0] == "Pooja"
