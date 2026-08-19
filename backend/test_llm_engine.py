"""
Pytest Suite for SakhiAI Clinical Medical LLM Engine (UN SDG 3 Track).
"""

import pytest
from fastapi.testclient import TestClient
from main import app
from llm_engine import (
    chat_sakhi_copilot,
    generate_family_counseling_script,
    generate_clinical_differential
)

client = TestClient(app)


def test_sakhi_copilot_hindi_query():
    """Verify copilot responds in Hindi to clinical emergency questions."""
    query = "8वें महीने में महिला को तेज सिरदर्द और बीपी 160/110 है, क्या प्राथमिक उपचार दें?"
    res = chat_sakhi_copilot(query=query, language="hi")
    assert res["status"] == "success"
    assert "सखीकेयर क्लिनिकल सहायक" in res["reply"]
    assert "108" in res["reply"]
    assert any("Pre-eclampsia" in p or "Eclampsia" in p for p in res["matched_protocols"])


def test_sakhi_copilot_english_query():
    """Verify copilot responds in English with WHO/MoHFW guidance."""
    query = "How to manage acute postpartum hemorrhage before hospital transport?"
    res = chat_sakhi_copilot(query=query, language="en")
    assert res["status"] == "success"
    assert "SakhiCare Clinical Copilot" in res["reply"]
    assert "Oxytocin" in res["reply"] or "IV" in res["reply"]


def test_counseling_script_generation_multilingual():
    """Verify vernacular family persuasion script generator in Hindi and Bengali."""
    # Hindi script
    res_hi = generate_family_counseling_script(
        patient_name="Sunita Devi",
        village="Rampur",
        risk_level="RED",
        danger_signs={"bleeding": True, "headache": True},
        blood_pressure="160/110",
        language="hi"
    )
    assert res_hi["status"] == "success"
    assert "आशा दीदी" in res_hi["counseling_script"]
    assert "108 एम्बुलेंस" in res_hi["counseling_script"]

    # Bengali script
    res_bn = generate_family_counseling_script(
        patient_name="Anita Devi",
        village="Chandpur",
        risk_level="RED",
        danger_signs={"bleeding": True},
        blood_pressure="150/100",
        language="bn"
    )
    assert res_bn["status"] == "success"
    assert "আশা দিদি" in res_bn["counseling_script"]
    assert "১০৮" in res_bn["counseling_script"]


def test_clinical_differential_diagnosis():
    """Verify structured medical differential diagnosis generation."""
    res = generate_clinical_differential(
        patient_id="SC-101",
        patient_name="Sunita Devi",
        blood_pressure="165/110",
        haemoglobin=6.5,
        danger_signs={"bleeding": True, "headache": True}
    )
    assert res["status"] == "success"
    assert any("Pre-eclampsia" in d for d in res["differential_diagnoses"])
    assert any("Anemia" in d for d in res["differential_diagnoses"])
    assert any("Placenta" in d or "Abruption" in d for d in res["differential_diagnoses"])
    assert len(res["clinical_actions"]) >= 2


def test_copilot_api_endpoint():
    """Verify POST /api/v1/ai/copilot endpoint."""
    payload = {
        "query": "महिला को तेज बुखार और कंपकंपी है",
        "language": "hi"
    }
    response = client.post("/api/v1/ai/copilot", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "सखीकेयर" in data["reply"]


def test_counseling_api_endpoint():
    """Verify POST /api/v1/ai/counseling-script endpoint."""
    payload = {
        "patient_name": "Meena Devi",
        "village": "Sitapur",
        "risk_level": "RED",
        "danger_signs": {"bleeding": True},
        "blood_pressure": "155/95",
        "haemoglobin": 8.0,
        "language": "hi"
    }
    response = client.post("/api/v1/ai/counseling-script", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert "Meena Devi" in data["counseling_script"]
