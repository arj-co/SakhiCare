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
