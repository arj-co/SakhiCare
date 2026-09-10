import io
import hashlib
import pytest
from fastapi.testclient import TestClient
from main import app, audio_artifacts_db

client = TestClient(app)

def test_case_audio_upload_valid_sha256():
    case_id = "SC-AUD-001"
    sample_audio_bytes = b"\x00\x01\x02\x03\x04\x05\x06\x07" * 512  # 4096 bytes
    expected_sha = hashlib.sha256(sample_audio_bytes).hexdigest()

    files = {"file": ("note.m4a", io.BytesIO(sample_audio_bytes), "audio/m4a")}
    headers = {
        "X-Idempotency-Key": "idemp-aud-01",
        "X-Audio-SHA256": expected_sha
    }
    params = {
        "duration_seconds": 15,
        "language": "hi-IN"
    }

    response = client.post(
        f"/api/v1/cases/{case_id}/audio",
        files=files,
        headers=headers,
        params=params
    )

    assert response.status_code == 201
    data = response.json()
    assert data["status"] == "UPLOADED"
    assert data["case_id"] == case_id
    assert data["sha256"] == expected_sha
    assert data["file_size_bytes"] == len(sample_audio_bytes)
    assert "artifact_id" in data
    assert "retention_due_at" in data


def test_case_audio_upload_sha256_mismatch():
    case_id = "SC-AUD-002"
    sample_audio_bytes = b"different_bytes_here"
    mismatched_sha = "0000000000000000000000000000000000000000000000000000000000000000"

    files = {"file": ("note.m4a", io.BytesIO(sample_audio_bytes), "audio/m4a")}
    headers = {
        "X-Idempotency-Key": "idemp-aud-02",
        "X-Audio-SHA256": mismatched_sha
    }

    response = client.post(
        f"/api/v1/cases/{case_id}/audio",
        files=files,
        headers=headers
    )

    assert response.status_code == 400
    assert "checksum mismatch" in response.json()["detail"]


def test_case_audio_get_and_stream():
    case_id = "SC-AUD-003"
    audio_content = b"AUDIO_DATA_FOR_STREAMING_TEST" * 100
    sha = hashlib.sha256(audio_content).hexdigest()

    # Upload first
    files = {"file": ("note.m4a", io.BytesIO(audio_content), "audio/m4a")}
    client.post(
        f"/api/v1/cases/{case_id}/audio",
        files=files,
        headers={"X-Audio-SHA256": sha}
    )

    # Retrieve / stream
    response = client.get(f"/api/v1/cases/{case_id}/audio")
    assert response.status_code == 200
    assert response.content == audio_content
    assert "audio/m4a" in response.headers.get("content-type", "")


def test_case_audio_not_found():
    response = client.get("/api/v1/cases/NONEXISTENT-CASE-ID/audio")
    assert response.status_code == 404
    assert "Audio recording not found" in response.json()["detail"]


def test_case_audio_retention_delete():
    case_id = "SC-AUD-004"
    audio_content = b"TEMP_AUDIO_FOR_DELETION"

    files = {"file": ("temp.m4a", io.BytesIO(audio_content), "audio/m4a")}
    client.post(f"/api/v1/cases/{case_id}/audio", files=files)

    # Delete
    del_resp = client.delete(f"/api/v1/cases/{case_id}/audio")
    assert del_resp.status_code == 200
    assert del_resp.json()["status"] == "DELETED"

    # Verify subsequent GET returns 404
    get_resp = client.get(f"/api/v1/cases/{case_id}/audio")
    assert get_resp.status_code == 404
