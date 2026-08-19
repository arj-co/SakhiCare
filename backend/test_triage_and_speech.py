"""
Automated Pytest Suite for Multi-Metric Clinical Triage Model and Parakeet Speech Engine.
"""

import io
import wave
import pytest
from fastapi.testclient import TestClient
from main import app
from triage_engine import evaluate_clinical_risk
from speech_engine import extract_clinical_slots, normalize_spoken_numbers, transcribe_offline_audio

client = TestClient(app)


def test_severe_anemia_triggers_red_emergency():
    """Verify that severe anemia (Hb < 7.0) triggers RED emergency even with normal BP."""
    result = evaluate_clinical_risk(
        blood_pressure="120/80",
        haemoglobin=6.4,
        danger_signs={"bleeding": False, "fever": False, "headache": False, "reduced_fetal_movement": False}
    )
    assert result.risk_level == "RED"
    assert result.requires_blood_transfusion_alert is True
    assert any("Severe Anemia" in f for f in result.primary_factors)


def test_severe_hypertensive_crisis_triggers_red():
    """Verify that SBP >= 160 or DBP >= 110 triggers RED emergency."""
    result = evaluate_clinical_risk(
        blood_pressure="165/112",
        haemoglobin=11.2,
        danger_signs={"bleeding": False, "fever": False, "headache": False, "reduced_fetal_movement": False}
    )
    assert result.risk_level == "RED"
    assert result.requires_immediate_ambulance is True
    assert any("Severe Hypertensive Crisis" in f for f in result.primary_factors)


def test_obstetric_shock_triggers_red():
    """Verify that hypotension shock (BP < 90/50) triggers RED emergency."""
    result = evaluate_clinical_risk(
        blood_pressure="85/45",
        haemoglobin=10.0,
        danger_signs={"bleeding": False, "fever": False, "headache": False, "reduced_fetal_movement": False}
    )
    assert result.risk_level == "RED"
    assert any("Obstetric Shock" in f for f in result.primary_factors)


def test_compound_risk_matrix_triggers_red():
    """Verify that Moderate Anemia + Gestational HTN + Headache triggers RED compound escalation."""
    result = evaluate_clinical_risk(
        blood_pressure="145/92",
        haemoglobin=8.2,
        danger_signs={"bleeding": False, "fever": False, "headache": True, "reduced_fetal_movement": False}
    )
    assert result.risk_level == "RED"
    assert any("Compound" in f or "Eclampsia" in f for f in result.primary_factors)


def test_normal_parameters_trigger_green():
    """Verify that standard normal maternal checkup returns GREEN."""
    result = evaluate_clinical_risk(
        blood_pressure="118/76",
        haemoglobin=11.8,
        danger_signs={"bleeding": False, "fever": False, "headache": False, "reduced_fetal_movement": False}
    )
    assert result.risk_level == "GREEN"
    assert result.requires_immediate_ambulance is False


def test_spoken_number_normalization():
    """Verify that Indic and Hinglish number phrases normalize properly."""
    text1 = "मरीज अनीता बीपी एक सौ चालीस बटा नब्बे हीमोग्लोबिन दस दशमलव दो"
    norm1 = normalize_spoken_numbers(text1)
    assert "140/90" in norm1
    assert "10.2" in norm1

    text2 = "Patient Sunita village Rampur BP 150 over 100 haemoglobin 8.5"
    norm2 = normalize_spoken_numbers(text2)
    assert "150/100" in norm2


def test_speech_slot_filler_and_triage():
    """Verify speech slot filler returns extracted fields and clinical evaluation."""
    transcript = "मरीज सुनीता देवी, गांव रामपुर, बीपी 165/110, हीमोग्लोबिन 6.5, खून और सिरदर्द"
    slots = extract_clinical_slots(transcript)
    assert slots["patient_name"] in ("सुनीता देवी", "Sunita Devi")
    assert slots["blood_pressure"] == "165/110"
    assert slots["haemoglobin"] == 6.5
    assert slots["clinical_triage"]["risk_level"] == "RED"
    assert slots["clinical_triage"]["requires_immediate_ambulance"] is True


def test_audio_transcribe_endpoint():
    """Verify audio file upload endpoint with simulated WAV payload."""
    # Create in-memory dummy WAV header
    wav_io = io.BytesIO()
    with wave.open(wav_io, 'wb') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(16000)
        wav.writeframes(b'\x00\x00' * 16000)  # 1 second of audio
    wav_io.seek(0)

    files = {"file": ("dictation.wav", wav_io.read(), "audio/wav")}
    response = client.post("/api/v1/voice/transcribe-audio", files=files)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "Parakeet" in data["engine"]
    assert "slots" in data
