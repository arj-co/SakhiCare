"""
SakhiCare Phase 4 Test Suite:
- Minimal privacy-preserving SMS (no patient full names over SMS)
- Truthful notification delivery states (never fabricate DELIVERED without verified provider receipt)
- Multi-channel emergency escalation chain with Block Supervisor fallback
- 108 Transport coordination state machine (REQUESTED -> CALL_ATTEMPTED -> CONFIRMED -> EN_ROUTE -> ARRIVED)
- Capability-based facility referral routing (Blood Bank / FRU vs PHC)
- Clinical protocol metadata sign-off (mohfw-hrp-v1.0) and scope of practice
"""

import pytest
from fastapi.testclient import TestClient
from main import app
from database import SessionLocal
from notification_service import format_minimal_sms, send_notification, trigger_emergency_escalation
from case_service import (
    create_or_update_transport_request, list_transport_requests,
    recommend_facility_for_case, get_clinical_protocol_metadata,
    ingest_sync_case_batch
)

client = TestClient(app)


def test_minimal_sms_privacy_guarantee():
    """Plain SMS must NEVER expose the patient's full name or sensitive identity."""
    patient_full_name = "Sunita Devi Sharma"
    case_id = "SC-TEST-99"
    village = "Gopalpur"
    danger_signs = ["Severe High BP", "Antepartum Bleeding"]

    sms_text = format_minimal_sms(
        case_id=case_id,
        urgency="RED",
        village=village,
        danger_signs=danger_signs,
        callback_phone="0612-2554401"
    )

    # STRICT PRIVACY: Patient's name must NOT appear in the SMS text
    assert patient_full_name not in sms_text
    assert "Sunita" not in sms_text
    assert "Devi" not in sms_text

    # Essential operational context MUST appear
    assert f"Case {case_id}" in sms_text
    assert "URGENT RED" in sms_text
    assert f"Area: {village}" in sms_text
    assert "0612-2554401" in sms_text
    assert "Antepartum Bleeding" in sms_text


def test_truthful_notification_delivery_status():
    """Notification adapter must report NOT_CONFIGURED instead of fabricating DELIVERED."""
    with SessionLocal() as db:
        res = send_notification(
            db=db,
            case_id="SC-TEST-101",
            channel="SMS",
            recipient="+919876543201",
            template_type="EMERGENCY_SMS",
            content_text="[SakhiCare] Case SC-TEST-101 | URGENT RED | Area: Rampur",
            actor_id="TEST_RUNNER"
        )

        # In test environment without SMS_GATEWAY_API_KEY, status must be NOT_CONFIGURED
        assert res["status"] == "NOT_CONFIGURED"
        assert res["provider_ref"] is None
        assert "not configured" in res["error_message"].lower()


def test_emergency_escalation_chain_with_supervisor_fallback():
    """When primary notification cannot be verified, escalation must trigger to supervisor."""
    with SessionLocal() as db:
        esc_result = trigger_emergency_escalation(
            db=db,
            case_id="SC-TEST-102",
            patient_name="Pooja Kumari",
            village="Rampur",
            urgency="RED",
            danger_signs=["Convulsions", "High BP"]
        )

        assert esc_result["case_id"] == "SC-TEST-102"
        assert esc_result["escalation_triggered"] is True
        notifs = esc_result["notifications"]
        assert len(notifs) >= 3  # Push + SMS + Escalation SMS to Supervisor

        # Verify supervisor was alerted
        supervisor_alert = next((n for n in notifs if n.get("escalated_to") == "SUPERVISOR_ANITA"), None)
        assert supervisor_alert is not None
        assert supervisor_alert["recipient"] == "+919876543202"
        assert "ESCALATION" in supervisor_alert["content_preview"]


def test_transport_coordination_lifecycle_and_api():
    """Test manual call-and-confirm transport lifecycle via API."""
    # 1. Login as 108 Dispatcher
    disp_resp = client.post("/api/v1/auth/login", json={
        "username": "dispatch_108",
        "password": "DispatchPass123!"
    })
    assert disp_resp.status_code == 200
    token = disp_resp.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Ingest a test RED case
    ingest_resp = client.post("/api/v1/sync/batch", json=[{
        "patient_id": "SC-TRANS-01",
        "patient_name": "Radhika Devi",
        "village": "Rampur",
        "blood_pressure": "168/112",
        "haemoglobin": 6.2,
        "danger_signs": {"bleeding": True},
        "risk_level": "RED",
        "risk_score": 95,
        "timestamp": "2026-08-19T08:00:00Z"
    }])
    assert ingest_resp.status_code == 200

    # 2. Step 1: Record CALL_ATTEMPTED with call notes
    step1_resp = client.post("/api/v1/transport/requests", json={
        "case_id": "SC-TRANS-01",
        "status": "CALL_ATTEMPTED",
        "call_attempt_notes": "Called 108 central desk. Spoke with Operator Sunita, ticket #108-BIH-449."
    }, headers=headers)
    assert step1_resp.status_code == 200
    s1_data = step1_resp.json()
    assert s1_data["status"] == "CALL_ATTEMPTED"
    assert "Operator Sunita" in s1_data["call_attempt_notes"]

    # 3. Step 2: Transition to CONFIRMED with driver details
    step2_resp = client.post("/api/v1/transport/requests", json={
        "case_id": "SC-TRANS-01",
        "status": "CONFIRMED",
        "vehicle_id": "108-AMB-Rampur-09",
        "driver_name": "Santosh Yadav",
        "driver_phone": "+919876543333",
        "destination_facility_name": "Chandanpur Community Health Centre (CHC)",
        "call_attempt_notes": "Driver assigned. Departing depot."
    }, headers=headers)
    assert step2_resp.status_code == 200
    s2_data = step2_resp.json()
    assert s2_data["status"] == "CONFIRMED"
    assert s2_data["vehicle_id"] == "108-AMB-Rampur-09"
    assert s2_data["driver_name"] == "Santosh Yadav"
    assert s2_data["confirmed_by"] == "usr_disp_01"

    # 4. Step 3: Transition to EN_ROUTE
    step3_resp = client.post("/api/v1/transport/requests", json={
        "case_id": "SC-TRANS-01",
        "status": "EN_ROUTE",
        "vehicle_id": "108-AMB-Rampur-09"
    }, headers=headers)
    assert step3_resp.status_code == 200
    assert step3_resp.json()["status"] == "EN_ROUTE"

    # 5. List transport requests from Transport Board endpoint
    list_resp = client.get("/api/v1/transport/requests")
    assert list_resp.status_code == 200
    board_data = list_resp.json()
    assert board_data["count"] >= 1
    item = next((i for i in board_data["items"] if i["case_id"] == "SC-TRANS-01"), None)
    assert item is not None
    assert item["status"] == "EN_ROUTE"
    assert item["vehicle_id"] == "108-AMB-Rampur-09"


def test_facility_recommendation_routing():
    """Severe complication (severe anemia or bleeding) must route to CHC with Blood Bank."""
    # Seed severe case
    with SessionLocal() as db:
        ingest_sync_case_batch(db, {
            "patient_id": "SC-ROUTING-SEVERE",
            "patient_name": "Kavita Singh",
            "haemoglobin": 6.1,  # Severe anemia < 7.0
            "blood_pressure": "140/90",
            "danger_signs": {"bleeding": True},
            "risk_level": "RED"
        })

        ingest_sync_case_batch(db, {
            "patient_id": "SC-ROUTING-MILD",
            "patient_name": "Geeta Sharma",
            "haemoglobin": 11.5,
            "blood_pressure": "118/74",
            "danger_signs": {},
            "risk_level": "GREEN"
        })

    # Test severe case routing
    sev_resp = client.get("/api/v1/cases/SC-ROUTING-SEVERE/recommend-facility")
    assert sev_resp.status_code == 200
    sev_data = sev_resp.json()
    assert sev_data["level"] == "CHC"
    assert "FAC-02" in sev_data["recommended_facility_id"]
    assert any("Blood Bank" in c for c in sev_data["capabilities"])

    # Test mild case routing
    mild_resp = client.get("/api/v1/cases/SC-ROUTING-MILD/recommend-facility")
    assert mild_resp.status_code == 200
    mild_data = mild_resp.json()
    assert mild_data["level"] == "PHC"
    assert "FAC-01" in mild_data["recommended_facility_id"]


def test_clinical_protocol_metadata_endpoint():
    """Protocol metadata must return signed-off guidelines and clinical scope of practice."""
    resp = client.get("/api/v1/clinical/protocols")
    assert resp.status_code == 200
    data = resp.json()

    assert data["rule_pack_version"] == "mohfw-hrp-v1.0"
    assert data["status"] == "CLINICALLY_VALIDATED"
    assert data["clinical_reviewer"]["name"] == "Dr. Rajiv Sharma"
    assert "MBBS, MD" in data["clinical_reviewer"]["qualifications"]

    # Scope of practice checks
    scope = data["scope_of_practice"]
    assert any("108" in act for act in scope["asha_safe_actions"])
    assert any("Magnesium Sulphate" in act for act in scope["clinician_directed_actions"])
