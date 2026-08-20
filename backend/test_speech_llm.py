"""
Pytest Suite for Speech-LLM Unified Audio-to-Reasoning Model.
"""

import io
import wave
import pytest
from fastapi.testclient import TestClient
from main import app
from speech_llm import SpeechLLMProcessor

client = TestClient(app)


def test_speech_llm_phonetic_correction():
    """Verify that speech recognizer phonetic errors are corrected by LLM semantic rules."""
    raw = "patient Anita Devi village Rampur BP one sixty over one ten haemoglobin six point eight severe headache and bleeding"
    res = SpeechLLMProcessor.extract_and_reason_from_speech(raw)
    
    assert res["status"] == "success"
    assert res["extracted_patient"]["blood_pressure"] == "160/110"
    assert res["extracted_patient"]["danger_signs"]["bleeding"] is True
    assert res["clinical_triage"]["risk_level"] == "RED"
    assert any("Pre-eclampsia" in d for d in res["differential_diagnosis"])
    assert "आशा दीदी" in res["vernacular_counseling_script"] or "108" in res["vernacular_counseling_script"]


def test_speech_llm_devanagari_reasoning():
    """Verify speech-LLM reasoning on Devanagari Hindi clinical dictation."""
    raw = "मरीज मीरा कुमार गांव सीतापुर बीपी एक सौ बीस बटा अस्सी हीमोग्लोबिन ग्यारह कोई लक्षण नहीं"
    res = SpeechLLMProcessor.extract_and_reason_from_speech(raw)
    
    assert res["status"] == "success"
    assert res["extracted_patient"]["blood_pressure"] == "120/80"
    assert res["clinical_triage"]["risk_level"] == "GREEN"


def test_speech_llm_transcript_api_endpoint():
    """Verify POST /api/v1/speech-llm/process-transcript endpoint."""
    payload = {
        "spoken_transcript": "मरीज कविता देवी गांव रामपुर बीपी 165/112 हीमोग्लोबिन 6.4 तेज सिरदर्द और रक्तस्राव"
    }
    response = client.post("/api/v1/speech-llm/process-transcript", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "Sakhi-SpeechLLM" in data["speech_llm_model"]
    assert data["clinical_triage"]["risk_level"] == "RED"


def test_speech_llm_audio_api_endpoint():
    """Verify POST /api/v1/speech-llm/process-audio endpoint."""
    wav_io = io.BytesIO()
    with wave.open(wav_io, 'wb') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(16000)
        wav.writeframes(b'\x00\x00' * 16000)
    wav_io.seek(0)

    files = {"file": ("recording.wav", wav_io.read(), "audio/wav")}
    response = client.post("/api/v1/speech-llm/process-audio", files=files)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "extracted_patient" in data
    assert "differential_diagnosis" in data
