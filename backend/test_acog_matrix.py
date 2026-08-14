import pytest
from triage_engine import evaluate_clinical_risk

def test_isolated_systolic_hypertension():
    res = evaluate_clinical_risk("165/85", 11.5, {})
    assert res.risk_level == "RED"
    assert "Severe Hypertensive Crisis" in res.clinical_rationale

def test_isolated_diastolic_hypertension():
    res = evaluate_clinical_risk("130/112", 11.5, {})
    assert res.risk_level == "RED"
    assert "Severe Hypertensive Crisis" in res.clinical_rationale

def test_severe_anemia_threshold():
    res = evaluate_clinical_risk("120/80", 6.5, {})
    assert res.risk_level == "RED"
    assert "Severe Anemia" in res.clinical_rationale
