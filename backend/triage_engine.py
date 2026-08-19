"""
SakhiCare Advanced Clinical Triage & Risk Evaluation Engine
Adheres to Indian Ministry of Health and Family Welfare (MoHFW), WHO, and ACOG
High-Risk Pregnancy (HRP) Clinical Guidelines.
"""

from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field
from pydantic import BaseModel, Field


class DangerSignsData(BaseModel):
    bleeding: bool = False
    fever: bool = False
    headache: bool = False
    reduced_fetal_movement: bool = False
    convulsions_or_vision_loss: bool = False


@dataclass
class ClinicalEvaluationResult:
    risk_level: str  # "RED", "AMBER", "GREEN"
    risk_score: int  # 0 to 100
    primary_factors: List[str] = field(default_factory=list)
    clinical_rationale_en: str = ""
    clinical_rationale_hi: str = ""
    recommended_protocol_en: str = ""
    recommended_protocol_hi: str = ""
    requires_immediate_ambulance: bool = False
    requires_blood_transfusion_alert: bool = False

    def to_dict(self) -> Dict[str, Any]:
        return {
            "risk_level": self.risk_level,
            "risk_score": self.risk_score,
            "primary_factors": self.primary_factors,
            "clinical_rationale_en": self.clinical_rationale_en,
            "clinical_rationale_hi": self.clinical_rationale_hi,
            "recommended_protocol_en": self.recommended_protocol_en,
            "recommended_protocol_hi": self.recommended_protocol_hi,
            "requires_immediate_ambulance": self.requires_immediate_ambulance,
            "requires_blood_transfusion_alert": self.requires_blood_transfusion_alert
        }


def parse_blood_pressure(bp_str: str) -> Tuple[int, int]:
    """Parses SBP and DBP from string format like '145/95' or '145 over 95'."""
    clean = bp_str.strip().replace("over", "/").replace("बटा", "/")
    parts = clean.split("/")
    if len(parts) == 2:
        try:
            return int(parts[0].strip()), int(parts[1].strip())
        except ValueError:
            pass
    return 120, 80


def evaluate_clinical_risk(
    blood_pressure: str,
    haemoglobin: float,
    danger_signs: Dict[str, bool]
) -> ClinicalEvaluationResult:
    """
    Evaluates multi-metric maternal health triage:
    1. Blood Pressure: Hypertensive Crisis (>=160/110), Shock (<90/50), Gestational HTN (140-159/90-109)
    2. Haemoglobin: Severe Anemia (<7.0 g/dL), Moderate Anemia (7.0-9.9 g/dL), Normal (>=10.0 g/dL)
    3. Danger Signs: Bleeding (APH/PPH), Fetal Distress, Convulsions, Headache, Sepsis
    4. Compound Risk Correlator
    """
    sbp, dbp = parse_blood_pressure(blood_pressure)
    factors: List[str] = []
    score = 10  # Baseline normal score

    has_bleeding = danger_signs.get("bleeding", False)
    has_fever = danger_signs.get("fever", False)
    has_headache = danger_signs.get("headache", False)
    has_fetal_distress = danger_signs.get("reduced_fetal_movement", False)
    has_convulsions = danger_signs.get("convulsions_or_vision_loss", False)

    is_emergency_red = False
    is_urgent_amber = False
    needs_ambulance = False
    needs_blood = False

    # ── 1. Blood Pressure Stratification ──
    if sbp >= 160 or dbp >= 110:
        is_emergency_red = True
        needs_ambulance = True
        score += 45
        factors.append(f"Severe Hypertensive Crisis / Pre-eclampsia (BP: {sbp}/{dbp} mmHg)")
    elif sbp < 90 or dbp < 50:
        is_emergency_red = True
        needs_ambulance = True
        score += 50
        factors.append(f"Obstetric Shock / Severe Hypotension (BP: {sbp}/{dbp} mmHg)")
    elif (140 <= sbp < 160) or (90 <= dbp < 110):
        is_urgent_amber = True
        score += 25
        factors.append(f"Gestational Hypertension (BP: {sbp}/{dbp} mmHg)")
    elif (130 <= sbp < 140) or (85 <= dbp < 90):
        score += 10
        factors.append(f"High-Normal Blood Pressure (BP: {sbp}/{dbp} mmHg)")

    # ── 2. Haemoglobin (Anemia) Stratification ──
    if haemoglobin < 7.0:
        is_emergency_red = True
        needs_blood = True
        score += 40
        factors.append(f"Severe Anemia (Hb: {haemoglobin:.1f} g/dL) - Imminent Heart Failure Risk")
    elif 7.0 <= haemoglobin < 10.0:
        is_urgent_amber = True
        score += 20
        factors.append(f"Moderate Anemia (Hb: {haemoglobin:.1f} g/dL)")
    elif 10.0 <= haemoglobin < 11.0:
        score += 5
        factors.append(f"Mild Anemia (Hb: {haemoglobin:.1f} g/dL)")

    # ── 3. Danger Signs Stratification ──
    if has_bleeding:
        is_emergency_red = True
        needs_ambulance = True
        score += 45
        factors.append("Antepartum / Postpartum Vaginal Hemorrhage (Severe Danger Sign)")

    if has_fetal_distress:
        is_emergency_red = True
        needs_ambulance = True
        score += 35
        factors.append("Acute Fetal Distress (Reduced / Absent Fetal Movement in 3rd Trimester)")

    if has_convulsions:
        is_emergency_red = True
        needs_ambulance = True
        score += 50
        factors.append("Eclamptic Seizures / Severe Neurological Deficit")

    if has_headache:
        if sbp >= 140 or dbp >= 90:
            # Headache + HTN = Impending Eclampsia
            is_emergency_red = True
            needs_ambulance = True
            score += 35
            factors.append("Severe Headache with Hypertension (Impending Eclampsia Alert)")
        else:
            is_urgent_amber = True
            score += 15
            factors.append("Persistent Severe Headache")

    if has_fever:
        if is_emergency_red or sbp < 90:
            factors.append("High Fever with Septic Warning Signs")
        else:
            is_urgent_amber = True
            score += 15
            factors.append("Maternal Pyrexia / Fever (Possible Intrauterine or Systemic Infection)")

    # ── 4. Compound Interaction Evaluation ──
    # Example: Moderate Anemia + Gestational HTN + Fever -> Escalate to RED
    if (7.0 <= haemoglobin < 10.0) and ((140 <= sbp) or (90 <= dbp)) and (has_fever or has_headache):
        is_emergency_red = True
        needs_ambulance = True
        factors.append("Compound High-Risk Matrix (Moderate Anemia + Gestational HTN + Symptoms)")

    # Cap score at 100
    risk_score = min(100, score)

    # Determine Final Risk Level
    if is_emergency_red or risk_score >= 60:
        risk_level = "RED"
        rationale_en = f"Critical Emergency: {'; '.join(factors)}. High risk of maternal-fetal mortality."
        rationale_hi = f"अत्यंत गंभीर आपातकाल: {'; '.join(factors)}। तुरंत अस्पताल रेफरल अनिवार्य है।"
        protocol_en = "1) Initiate emergency 108 ambulance transport immediately. 2) Maintain left lateral tilt position. 3) Keep IV access ready. 4) Contact PHC Medical Officer."
        protocol_hi = "1) तुरंत 108 एम्बुलेंस बुलाएं। 2) महिला को बाईं करवट लिटाएं। 3) नजदीकी सीएचसी/पीएचसी डॉक्टर को तुरंत सूचित करें।"
    elif is_urgent_amber or risk_score >= 30:
        risk_level = "AMBER"
        rationale_en = f"High Priority Observation: {'; '.join(factors)}. Requires clinical consultation within 24 hours."
        rationale_hi = f"उच्च प्राथमिकता निगरानी: {'; '.join(factors)}। 24 घंटे के भीतर डॉक्टर से जांच कराएं।"
        protocol_en = "1) Refer to Primary Health Centre within 24-48h. 2) Prescribe supplemental iron / anti-pyretic if advised. 3) Re-check BP daily."
        protocol_hi = "1) 24 घंटे में नजदीकी स्वास्थ्य केंद्र भेजें। 2) प्रतिदिन बीपी और बुखार की जांच करें।"
    else:
        risk_level = "GREEN"
        rationale_en = "Vitals and clinical observations within normal gestational parameters."
        rationale_hi = "सभी लक्षण और जांच सामान्य हैं।"
        protocol_en = "Continue routine Antenatal Care (ANC) checkups, nutrition counseling, and IFA tablets."
        protocol_hi = "नियमित एएनसी जांच, पौष्टिक आहार और आयरन-फोलिक एसिड गोलियां जारी रखें।"

    return ClinicalEvaluationResult(
        risk_level=risk_level,
        risk_score=risk_score,
        primary_factors=factors if factors else ["Normal checkup"],
        clinical_rationale_en=rationale_en,
        clinical_rationale_hi=rationale_hi,
        recommended_protocol_en=protocol_en,
        recommended_protocol_hi=protocol_hi,
        requires_immediate_ambulance=needs_ambulance,
        requires_blood_transfusion_alert=needs_blood
    )
