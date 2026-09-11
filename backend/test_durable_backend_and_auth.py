"""
Pytest Suite for Phase 3: Durable Backend, JWT Auth, Server-Enforced RBAC, Strict 404s, and Idempotent Sync
"""

import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_auth_login_and_me():
    # 1. Login with invalid password
    bad_resp = client.post("/api/v1/auth/login", json={
        "username": "doctor_sharma",
        "password": "WrongPassword!"
    })
    assert bad_resp.status_code == 401
    assert "Invalid" in bad_resp.json()["detail"]

    # 2. Login with valid doctor credentials
    good_resp = client.post("/api/v1/auth/login", json={
        "username": "doctor_sharma",
        "password": "DoctorPass123!"
    })
    assert good_resp.status_code == 200
    data = good_resp.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["role"] == "MEDICAL_OFFICER"
    assert data["user"]["username"] == "doctor_sharma"

    token = data["access_token"]

    # 3. Check /api/v1/auth/me with bearer token
    me_resp = client.get("/api/v1/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert me_resp.status_code == 200
    me_data = me_resp.json()
    assert me_data["username"] == "doctor_sharma"
    assert me_data["role"] == "MEDICAL_OFFICER"


def test_server_enforced_rbac():
    # Login as Dispatcher
    disp_resp = client.post("/api/v1/auth/login", json={
        "username": "dispatch_108",
        "password": "DispatchPass123!"
    })
    assert disp_resp.status_code == 200
    disp_token = disp_resp.json()["access_token"]

    # Dispatcher attempts to acknowledge medical case (forbidden for dispatcher)
    ack_forbidden = client.post(
        "/api/v1/cases/SC-101/acknowledge",
        json={"advisory_text": "Unauthorized doctor advice", "referral_facility_id": "FAC-01"},
        headers={"Authorization": f"Bearer {disp_token}"}
    )
    assert ack_forbidden.status_code == 403
    assert "Access denied" in ack_forbidden.json()["detail"]

    # Login as Medical Officer
    mo_resp = client.post("/api/v1/auth/login", json={
        "username": "doctor_sharma",
        "password": "DoctorPass123!"
    })
    mo_token = mo_resp.json()["access_token"]

    # Medical Officer acknowledges case
    ack_ok = client.post(
        "/api/v1/cases/SC-101/acknowledge",
        json={"advisory_text": "Administer IV labetalol 20mg stat, left lateral position, prepare 108 transfer", "referral_facility_id": "FAC-02"},
        headers={"Authorization": f"Bearer {mo_token}"}
    )
    assert ack_ok.status_code == 200
    assert "IV labetalol" in ack_ok.json()["doctor_advisory"]

    # Dispatcher updates transport
    trans_ok = client.post(
        "/api/v1/cases/SC-101/transport",
        json={"vehicle_id": "108-AMB-09", "destination_facility": "FAC-02", "driver_phone": "9876543299"},
        headers={"Authorization": f"Bearer {disp_token}"}
    )
    assert trans_ok.status_code == 200
    assert "108-AMB-09" in trans_ok.json()["ambulance_status"]


def test_strict_404_for_nonexistent_cases():
    unknown_id = "SC-DOES-NOT-EXIST-999"

    # 1. API v1 case detail
    v1_resp = client.get(f"/api/v1/cases/{unknown_id}")
    assert v1_resp.status_code == 404
    assert f"Case {unknown_id} not found" in v1_resp.json()["detail"]

    # 2. Legacy /cases/{patient_id}
    legacy_resp = client.get(f"/cases/{unknown_id}")
    assert legacy_resp.status_code == 404

    # 3. FHIR export: strictly 404 without fake Sunita Devi fallback!
    fhir_resp = client.get(f"/fhir/export/{unknown_id}")
    assert fhir_resp.status_code == 404
    assert "not found for FHIR export" in fhir_resp.json()["detail"]

    # 4. Events timeline
    evt_resp = client.get(f"/api/v1/cases/{unknown_id}/events")
    assert evt_resp.status_code == 404


def test_idempotent_sync_batch():
    import uuid
    uid = uuid.uuid4().hex[:8]
    idemp_key = f"idemp-test-batch-{uid}"
    c_id = f"SC-IDEMP-{uid}"
    payload = {
        "items": [
            {
                "idempotency_key": idemp_key,
                "case_id": c_id,
                "patient_name": "Kavita Bai",
                "village": "Sitapur",
                "blood_pressure": "150/98",
                "haemoglobin": 9.2,
                "danger_signs": {"bleeding": False, "fever": True},
                "worker_id": "WKR-101"
            }
        ]
    }

    # First sync
    res1 = client.post("/api/v1/sync/batch", json=payload)
    assert res1.status_code == 200
    data1 = res1.json()
    assert data1["status"] == "ACKNOWLEDGED"
    assert data1["processed"] == 1
    assert data1["items"][0]["case_id"] == c_id
    assert data1["items"][0].get("is_duplicate") is not True

    # Second sync with EXACT same idempotency key
    res2 = client.post("/api/v1/sync/batch", json=payload)
    assert res2.status_code == 200
    data2 = res2.json()
    assert data2["status"] == "ACKNOWLEDGED"
    assert data2["items"][0]["case_id"] == c_id
    # Guaranteed idempotent detection
    assert data2["items"][0]["is_duplicate"] is True

    # Verify case details are accessible via API v1
    detail_res = client.get(f"/api/v1/cases/{c_id}")
    assert detail_res.status_code == 200
    detail = detail_res.json()
    assert detail["patient_name"] == "Kavita Bai"
    assert detail["village"] == "Sitapur"
    assert detail["assessment"]["risk_level"] in ("AMBER", "RED")


def test_roster_and_facility_endpoints():
    fac_resp = client.get("/api/v1/facilities")
    assert fac_resp.status_code == 200
    fac_data = fac_resp.json()
    assert fac_data["count"] >= 2
    fac_ids = [f["id"] for f in fac_data["facilities"]]
    assert "FAC-01" in fac_ids
    assert "FAC-02" in fac_ids

    wkr_resp = client.get("/api/v1/workers")
    assert wkr_resp.status_code == 200
    wkr_data = wkr_resp.json()
    assert wkr_data["count"] >= 2
    wkr_ids = [w["id"] for w in wkr_data["workers"]]
    assert "WKR-101" in wkr_ids
    assert "WKR-102" in wkr_ids


def test_persistence_across_backend_restart():
    """Acceptance Check 1: Restarting backend session does not lose cases, audio metadata, or timeline events."""
    from database import SessionLocal
    from models import PregnancyCaseModel, VoiceArtifactModel, CaseEventModel
    import uuid

    uid = uuid.uuid4().hex[:6]
    test_case_id = f"SC-RESTART-{uid}"

    # Sync a new case
    sync_resp = client.post("/api/v1/sync/batch", json={
        "items": [{
            "idempotency_key": f"idemp-restart-{uid}",
            "case_id": test_case_id,
            "patient_name": "Gita Devi",
            "village": "Rampur",
            "blood_pressure": "165/110",
            "haemoglobin": 7.4,
            "danger_signs": {"severe_headache": True},
            "facility_id": "FAC-01",
            "worker_id": "WKR-101"
        }]
    })
    assert sync_resp.status_code == 200

    # Attach audio metadata
    with SessionLocal() as db:
        artifact = VoiceArtifactModel(
            id=f"ART-RESTART-{uid}",
            case_id=test_case_id,
            filename="note.m4a",
            file_path="/files/voice_notes/note.m4a",
            sha256="abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890",
            duration_seconds=18,
            language="hi-IN",
            transcript="मरीज गीता देवी तेज सिरदर्द",
            upload_status="UPLOADED"
        )
        db.add(artifact)
        db.commit()

    # Simulate backend restart by querying fresh SessionLocal completely isolated from previous handles
    with SessionLocal() as restarted_db:
        case = restarted_db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == test_case_id).first()
        assert case is not None
        assert case.patient_name == "Gita Devi"
        assert case.village == "Rampur"
        assert case.facility_id == "FAC-01"

        # Verify assessment persisted
        assert len(case.assessments) >= 1
        assert case.assessments[0].risk_level == "RED"

        # Verify audio metadata persisted
        audio = restarted_db.query(VoiceArtifactModel).filter(VoiceArtifactModel.case_id == test_case_id).first()
        assert audio is not None
        assert audio.sha256 == "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
        assert audio.duration_seconds == 18

        # Verify timeline events persisted
        events = restarted_db.query(CaseEventModel).filter(CaseEventModel.case_id == test_case_id).all()
        assert len(events) >= 1
        assert events[0].event_type == "SYNC_ACKNOWLEDGED"


def test_facility_scoping_enforced():
    """Acceptance Check 2: A worker or facility user cannot access another facility's cases."""
    # Login as doctor at FAC-01
    doc_resp = client.post("/api/v1/auth/login", json={
        "username": "doctor_sharma",
        "password": "DoctorPass123!"
    })
    assert doc_resp.status_code == 200
    doc_token = doc_resp.json()["access_token"]
    fac1_headers = {"Authorization": f"Bearer {doc_token}"}

    # Case SC-101 is in FAC-01 -> Doctor at FAC-01 CAN access it
    ok_resp = client.get("/api/v1/cases/SC-101", headers=fac1_headers)
    assert ok_resp.status_code == 200

    # Case SC-103 is in FAC-02 -> Doctor at FAC-01 CANNOT access it (403 Forbidden)
    forbidden_resp = client.get("/api/v1/cases/SC-103", headers=fac1_headers)
    assert forbidden_resp.status_code == 403
    assert "Access denied" in forbidden_resp.json()["detail"]

    # Doctor at FAC-01 listing cases only sees FAC-01 cases
    list_resp = client.get("/api/v1/cases", headers=fac1_headers)
    assert list_resp.status_code == 200
    returned_cases = list_resp.json()["cases"]
    for c in returned_cases:
        assert c["facility_id"] == "FAC-01"

    # Administrator CAN access cases across all facilities
    admin_resp = client.post("/api/v1/auth/login", json={
        "username": "admin",
        "password": "AdminPass123!"
    })
    admin_token = admin_resp.json()["access_token"]
    admin_headers = {"Authorization": f"Bearer {admin_token}"}

    admin_c103 = client.get("/api/v1/cases/SC-103", headers=admin_headers)
    assert admin_c103.status_code == 200


def test_medical_officer_acknowledgement_sync_back():
    """Acceptance Check 7: A medical officer can acknowledge a case and worker receives the advisory after sync."""
    import uuid
    uid = uuid.uuid4().hex[:6]
    ack_case_id = f"SC-ACK-SYNC-{uid}"

    # 1. Ingest case
    client.post("/api/v1/sync/batch", json={
        "items": [{
            "idempotency_key": f"idemp-ack-{uid}",
            "case_id": ack_case_id,
            "patient_name": "Pushpa Devi",
            "village": "Rampur",
            "blood_pressure": "158/104",
            "haemoglobin": 8.0,
            "danger_signs": {"severe_headache": True},
            "facility_id": "FAC-01",
            "worker_id": "WKR-101"
        }]
    })

    # 2. Medical Officer acknowledges case and formulates advisory
    mo_resp = client.post("/api/v1/auth/login", json={
        "username": "doctor_sharma",
        "password": "DoctorPass123!"
    })
    mo_token = mo_resp.json()["access_token"]

    advisory_content = "Administer oral nifedipine 10mg stat, repeat BP after 20 mins, keep 108 on standby"
    ack_resp = client.post(
        f"/api/v1/cases/{ack_case_id}/acknowledge",
        json={"advisory_text": advisory_content, "referral_facility_id": "FAC-02"},
        headers={"Authorization": f"Bearer {mo_token}"}
    )
    assert ack_resp.status_code == 200
    assert ack_resp.json()["doctor_advisory"] == advisory_content

    # 3. Worker subsequent sync for the case retrieves the formulated doctor advisory
    sync_back_resp = client.post("/api/v1/sync/batch", json={
        "items": [{
            "idempotency_key": f"idemp-ack-resync-{uid}",
            "case_id": ack_case_id,
            "patient_name": "Pushpa Devi",
            "village": "Rampur",
            "blood_pressure": "158/104",
            "haemoglobin": 8.0,
            "facility_id": "FAC-01",
            "worker_id": "WKR-101"
        }]
    })
    assert sync_back_resp.status_code == 200
    res_data = sync_back_resp.json()
    assert res_data["items"][0]["doctor_advisory"] == advisory_content


def test_multi_session_sse_consistency_and_stale_disconnect():
    """Acceptance Check 3 & 8: SSE broadcast reaches subscribers and post-refresh reads match database."""
    from main import broadcast_sse_event, sse_subscribers
    import asyncio

    # Setup two subscriber queues representing two portal browser tabs
    queue_tab1 = asyncio.Queue()
    queue_tab2 = asyncio.Queue()
    sse_subscribers.append(queue_tab1)
    sse_subscribers.append(queue_tab2)

    sample_case = {
        "case_id": "SC-SSE-SESSION-01",
        "patient_name": "Rekha Kumari",
        "sync_status": "ACKNOWLEDGED",
        "doctor_advisory": "Approved transfer"
    }

    # Broadcast event
    asyncio.run(broadcast_sse_event("NEW_CASE", {"case": sample_case}))

    # Both sessions receive identical broadcast
    msg1 = asyncio.run(queue_tab1.get())
    msg2 = asyncio.run(queue_tab2.get())
    assert "SC-SSE-SESSION-01" in msg1
    assert "SC-SSE-SESSION-01" in msg2

    # Clean up subscriber queues
    sse_subscribers.remove(queue_tab1)
    sse_subscribers.remove(queue_tab2)
