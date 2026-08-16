import pytest
from speech_llm import SpeechLLMProcessor

def test_bhojpuri_phrase_normalizer():
    raw = "मरीज ललिता देवी गांव भोजपुर बीपी एक सौ पचास बटा सौ हीमोग्लोबिन सात दशमलव दो"
    res = SpeechLLMProcessor.extract_and_reason_from_speech(raw)
    assert res["extracted_patient"]["blood_pressure"] == "150/100"
    assert res["extracted_patient"]["haemoglobin"] == 7.2
    assert res["clinical_triage"]["risk_level"] == "RED"
