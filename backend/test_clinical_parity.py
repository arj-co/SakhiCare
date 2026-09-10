"""
Automated Clinical Parity Test Suite
Validates backend triage engine against shared/clinical_triage_fixtures.json
Ensures deterministic agreement with protocol pack mohfw-hrp-v1.0.
"""

import json
import os
import pytest
from triage_engine import evaluate_clinical_risk, PROTOCOL_VERSION


def load_fixtures():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    fixtures_path = os.path.join(current_dir, "..", "shared", "clinical_triage_fixtures.json")
    with open(fixtures_path, "r", encoding="utf-8") as f:
        return json.load(f)


def test_protocol_version_matches():
    data = load_fixtures()
    assert data["protocol_version"] == PROTOCOL_VERSION == "mohfw-hrp-v1.0"


def test_all_fixtures_parity():
    data = load_fixtures()
    fixtures = data["fixtures"]
    assert len(fixtures) >= 10

    for fixture in fixtures:
        fid = fixture["id"]
        inp = fixture["input"]
        exp = fixture["expected"]

        result = evaluate_clinical_risk(
            blood_pressure=inp["blood_pressure"],
            haemoglobin=inp["haemoglobin"],
            danger_signs=inp["danger_signs"]
        )

        assert result.risk_level == exp["risk_level"], (
            f"Fixture {fid} failed: expected risk {exp['risk_level']}, got {result.risk_level}. "
            f"Factors: {result.primary_factors}"
        )
        assert result.requires_immediate_ambulance == exp["requires_ambulance"], (
            f"Fixture {fid} ambulance mismatch: expected {exp['requires_ambulance']}, got {result.requires_immediate_ambulance}"
        )
        assert result.requires_blood_transfusion_alert == exp["requires_blood_alert"], (
            f"Fixture {fid} blood alert mismatch: expected {exp['requires_blood_alert']}, got {result.requires_blood_transfusion_alert}"
        )
        assert result.rule_pack_version == "mohfw-hrp-v1.0"


def test_no_silent_defaults_for_missing_vitals():
    # Calling evaluate with null BP and null Hb must track them as unmeasured
    result = evaluate_clinical_risk(blood_pressure=None, haemoglobin=None, danger_signs={})
    assert result.risk_level == "GREEN"
    assert any("Blood Pressure: Not measured" in u for u in result.unmeasured_vitals)
    assert any("Haemoglobin: Not measured" in u for u in result.unmeasured_vitals)
    assert "Unmeasured:" in result.clinical_rationale_en
