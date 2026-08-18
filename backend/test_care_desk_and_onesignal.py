"""
Pytest suite for SakhiCare Care Desk, OneSignal REST API push engine, and bi-directional advisory lifecycle.
"""

import pytest
from fastapi.testclient import TestClient
from main import app
import onesignal_service

client = TestClient(app)


def test_care_desk_html_endpoint():
    """Verify that Care Desk HTML console is served with 200 OK."""
    response = client.get("/desk")
    assert response.status_code == 200
    assert "SakhiCare Care Desk" in response.text
    assert "Maternal Health Support Desk" in response.text

    alias_resp = client.get("/care-desk")
    assert alias_resp.status_code == 200


def test_health_check_endpoint():
    """Verify health check returns server status and metrics."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "version" in data
    assert "total_cases" in data


def test_sync_red_case_triggers_onesignal_push():
    """Verify that syncing a RED risk assessment triggers OneSignal emergency triage push."""
    payload = {
        "patient_id": "SC-TEST-RED-999",
        "patient_name": "Radhika Devi",
        "village": "Sonpur",
        "blood_pressure": "165/110",
        "haemoglobin": 8.5,
        "danger_signs": {
            "bleeding": True,
            "fever": False,
            "headache": True,
            "reduced_fetal_movement": False
        },
        "asha_worker_name": "ASHA Geeta",
        "asha_device_id": "device-asha-999"
    }

    response = client.post("/sync", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["risk_level"] == "RED"
    assert "fhir_bundle_id" in data
    assert data["onesignal_emergency_push"] is not None
    assert data["onesignal_emergency_push"]["delivery"] in ("simulated_success", "live_onesignal_api")


def test_post_care_desk_clinical_advisory():
    """Verify posting a doctor clinical advisory updates case and triggers ASHA push."""
    patient_id = "SC-TEST-RED-999"
    advisory_payload = {
        "advisory_text": "Administer oral labetalol 100mg stat, maintain left lateral tilt, transfer to CHC Sonpur.",
        "sender": "Care Desk Lead (Dr. Verma)"
    }

    response = client.post(f"/api/v1/cases/{patient_id}/advisory", json=advisory_payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["advisory"] == advisory_payload["advisory_text"]
    assert "notification_id" in data

    # Verify case details updated in cases list
    cases_resp = client.get("/cases")
    cases = {c["patient_id"]: c for c in cases_resp.json()["cases"]}
    assert patient_id in cases
    assert cases[patient_id]["doctor_advisory"] == advisory_payload["advisory_text"]


def test_dispatch_ambulance_endpoint():
    """Verify 108 ambulance dispatch updates vehicle status."""
    patient_id = "SC-TEST-RED-999"
    dispatch_payload = {
        "vehicle_id": "108-AMB-Sonpur-02",
        "destination_facility": "CHC Sonpur"
    }

    response = client.post(f"/api/v1/cases/{patient_id}/dispatch", json=dispatch_payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "108-AMB-Sonpur-02" in data["ambulance_status"]


def test_direct_onesignal_api_endpoints():
    """Verify direct REST API endpoints for emergency alert, advisory, broadcast, and device registration."""
    # 1. Direct Emergency Push
    em_resp = client.post("/api/v1/notifications/send-emergency-alert", json={
        "patient_id": "SC-API-101",
        "patient_name": "Priyanka Sharma",
        "village": "Rampur",
        "blood_pressure": "155/100",
        "danger_signs": {"bleeding": True},
        "risk_level": "RED"
    })
    assert em_resp.status_code == 200
    assert "id" in em_resp.json()

    # 2. Direct Broadcast
    bc_resp = client.post("/api/v1/notifications/broadcast", json={
        "title": "Severe Heatwave Warning",
        "message": "Advise all pregnant women to increase electrolyte intake.",
        "segment": "All"
    })
    assert bc_resp.status_code == 200
    assert "id" in bc_resp.json()

    # 3. Device Registration
    reg_resp = client.post("/api/v1/devices/register", json={
        "device_id": "dev-asha-777",
        "role": "ASHA",
        "player_id": "os-player-777-uuid",
        "user_name": "ASHA Shanti"
    })
    assert reg_resp.status_code == 200
    assert reg_resp.json()["device"]["device_id"] == "dev-asha-777"

    # 4. History check
    hist_resp = client.get("/api/v1/notifications/history")
    assert hist_resp.status_code == 200
    assert hist_resp.json()["count"] > 0
