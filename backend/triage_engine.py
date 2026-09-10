"""
SakhiCare Deterministic Clinical Triage & Risk Evaluation Engine
Protocol Pack: mohfw-hrp-v1.0
Adheres to Indian Ministry of Health and Family Welfare (MoHFW), WHO, and ACOG
High-Risk Pregnancy (HRP) Clinical Guidelines.
"""

from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field
from pydantic import BaseModel, Field

PROTOCOL_VERSION = "mohfw-hrp-v1.0"


class DangerSignsData(BaseModel):
    bleeding: bool = False
    fever: bool = False
    headache: bool = False
    severe_headache: bool = False
    reduced_fetal_movement: bool = False
    convulsions_or_vision_loss: bool = False
    severe_abdominal_pain: bool = False
    severe_breathlessness: bool = False
    premature_labour_water_broke: bool = False


@dataclass
class ClinicalEvaluationResult:
    risk_level: str  # "RED", "AMBER", "GREEN"
    risk_score: int  # 0 to 100
    primary_factors: List[str] = field(default_factory=list)
    unmeasured_vitals: List[str] = field(default_factory=list)
    clinical_rationale_en: str = ""
    clinical_rationale_hi: str = ""
    recommended_protocol_en: str = ""
    recommended_protocol_hi: str = ""
    asha_safe_actions: List[str] = field(default_factory=list)
    clinician_directed_actions: List[str] = field(default_factory=list)
    requires_immediate_ambulance: bool = False
    requires_blood_transfusion_alert: bool = False
    rule_pack_version: str = PROTOCOL_VERSION

    @property
    def clinical_rationale(self) -> str:
        """Backward compatibility alias for clinical_rationale_en"""
        return self.clinical_rationale_en

    def to_dict(self) -> Dict[str, Any]:
        return {
            "risk_level": self.risk_level,
            "risk_score": self.risk_score,
            "primary_factors": self.primary_factors,
            "unmeasured_vitals": self.unmeasured_vitals,
            "clinical_rationale": self.clinical_rationale_en,
            "clinical_rationale_en": self.clinical_rationale_en,
            "clinical_rationale_hi": self.clinical_rationale_hi,
            "recommended_protocol_en": self.recommended_protocol_en,
            "recommended_protocol_hi": self.recommended_protocol_hi,
            "asha_safe_actions": self.asha_safe_actions,
            "clinician_directed_actions": self.clinician_directed_actions,
            "requires_immediate_ambulance": self.requires_immediate_ambulance,
            "requires_blood_transfusion_alert": self.requires_blood_transfusion_alert,
            "rule_pack_version": self.rule_pack_version
        }


def parse_blood_pressure(bp_str: Optional[str]) -> Tuple[Optional[int], Optional[int]]:
    """
    Parses SBP and DBP from string format like '145/95' or '145 over 95'.
    Returns (None, None) if missing, unmeasured, or malformed. Never silently defaults!
    """
    if not bp_str or bp_str.strip().lower() in ("not measured", "unknown", "none", ""):
        return None, None
    clean = bp_str.strip().replace("over", "/").replace("बटा", "/")
    parts = clean.split("/")
    if len(parts) == 2:
        try:
            return int(parts[0].strip()), int(parts[1].strip())
        except ValueError:
            pass
    return None, None


def evaluate_clinical_risk(
    blood_pressure: Optional[str],
    haemoglobin: Optional[float],
    danger_signs: Dict[str, bool]
) -> ClinicalEvaluationResult:
    """
    Evaluates multi-metric maternal health triage according to mohfw-hrp-v1.0:
    1. Danger signs: Bleeding, Convulsions, Severe Abdominal Pain, Severe Breathlessness, Preterm Labour, Fetal Distress
    2. Blood Pressure: Hypertensive Crisis (>=160/110), Shock (<90/50), Gestational HTN (140-159/90-109)
    3. Haemoglobin: Severe Anemia (<7.0 g/dL), Moderate Anemia (7.0-9.9 g/dL)
    4. Compound Risk Correlator
    5. Preserves explicit 'Not measured' state without silent defaults.
    """
    sbp, dbp = parse_blood_pressure(blood_pressure)
    factors: List[str] = []
    unmeasured: List[str] = []
    score = 10  # Baseline

    # Track missing measurements explicitly
    if sbp is None or dbp is None:
        unmeasured.append("Blood Pressure: Not measured")
    if haemoglobin is None:
        unmeasured.append("Haemoglobin: Not measured")

    has_bleeding = danger_signs.get("bleeding", False)
    has_convulsions = danger_signs.get("convulsions_or_vision_loss", False) or danger_signs.get("convulsions", False)
    has_headache = danger_signs.get("severe_headache", False) or danger_signs.get("headache", False)
    has_abdominal_pain = danger_signs.get("severe_abdominal_pain", False)
    has_breathlessness = danger_signs.get("severe_breathlessness", False)
    has_fever = danger_signs.get("fever", False)
    has_labour_early = danger_signs.get("premature_labour_water_broke", False)
    has_fetal_distress = danger_signs.get("reduced_fetal_movement", False)

    is_emergency_red = False
    is_urgent_amber = False
    needs_ambulance = False
    needs_blood = False

    # ── 1. Blood Pressure Stratification ──
    if sbp is not None and dbp is not None:
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
    if haemoglobin is not None:
        if haemoglobin < 7.0:
            is_emergency_red = True
            needs_ambulance = True
            needs_blood = True
            score += 40
            factors.append(f"Severe Anemia (Hb: {haemoglobin:.1f} g/dL) - Transfusion Risk")
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

    if has_convulsions:
        is_emergency_red = True
        needs_ambulance = True
        score += 50
        factors.append("Convulsions / Eclamptic Fits / Unconsciousness")

    if has_abdominal_pain:
        is_emergency_red = True
        needs_ambulance = True
        score += 40
        factors.append("Severe Abdominal Pain (Possible Abruption or Ectopic/Rupture)")

    if has_breathlessness:
        is_emergency_red = True
        needs_ambulance = True
        score += 40
        factors.append("Severe Breathlessness / Chest Pain (Possible Pulmonary Edema)")

    if has_labour_early:
        is_emergency_red = True
        needs_ambulance = True
        score += 40
        factors.append("Premature Labour Pains / Water Breaking Early (Preterm ROM)")

    if has_fetal_distress:
        is_emergency_red = True
        needs_ambulance = True
        score += 35
        factors.append("Acute Fetal Distress (Reduced / Absent Fetal Movement in 3rd Trimester)")

    if has_headache:
        if (sbp is not None and sbp >= 140) or (dbp is not None and dbp >= 90):
            is_emergency_red = True
            needs_ambulance = True
            score += 35
            factors.append("Severe Headache with Hypertension (Impending Eclampsia Alert)")
        else:
            is_urgent_amber = True
            score += 15
            factors.append("Persistent Severe Headache / Visual Blurring")

    if has_fever:
        if is_emergency_red or (sbp is not None and sbp < 90):
            factors.append("High Fever with Septic Warning Signs")
        else:
            is_urgent_amber = True
            score += 15
            factors.append("Maternal Pyrexia / Fever (Possible Systemic or Intrauterine Infection)")

    # ── 4. Compound Interaction Evaluation ──
    if haemoglobin is not None and (7.0 <= haemoglobin < 10.0):
        if (sbp is not None and sbp >= 140) or (dbp is not None and dbp >= 90):
            if has_fever or has_headache:
                is_emergency_red = True
                needs_ambulance = True
                factors.append("Compound High Risk (Moderate Anemia + Gestational HTN + Symptoms)")

    # Cap score at 100
    risk_score = min(100, score)

    # ASHA-safe actions vs clinician-directed actions
    asha_safe_actions = []
    clinician_directed_actions = []

    if is_emergency_red or risk_score >= 60:
        risk_level = "RED"
        rationale_en = f"Critical Emergency: {'; '.join(factors)}. High risk of maternal-fetal mortality."
        rationale_hi = f"अत्यंत गंभीर आपातकाल: {'; '.join(factors)}। तुरंत अस्पताल रेफरल अनिवार्य है।"
        protocol_en = "1) Call 108 ambulance immediately. 2) Position patient in left lateral tilt. 3) Alert Medical Officer. 4) Do not give oral solids/fluids if fitting/unconscious."
        protocol_hi = "1) तुरंत 108 एम्बुलेंस बुलाएं। 2) महिला को बाईं करवट लिटाएं। 3) चिकित्सा अधिकारी को तत्काल सूचित करें।"
        asha_safe_actions = [
            "Call 108 ambulance immediately — do not delay referral",
            "Position mother in left lateral tilt position",
            "Keep airways clear; do not place objects in mouth if fitting",
            "Notify family members and village coordinator",
            "Contact Primary Health Centre (PHC) Medical Officer"
        ]
        clinician_directed_actions = [
            "IV access line placement (Clinician order required)",
            "Antihypertensive or Magnesium Sulphate administration (Clinician only)",
            "Blood grouping, cross-matching & transfusion authorization (Clinician only)"
        ]
    elif is_urgent_amber or risk_score >= 30:
        risk_level = "AMBER"
        rationale_en = f"High Priority Observation: {'; '.join(factors)}. Requires clinical consultation within 24 hours."
        rationale_hi = f"उच्च प्राथमिकता निगरानी: {'; '.join(factors)}। 24 घंटे के भीतर डॉक्टर से जांच कराएं।"
        protocol_en = "1) Refer to Primary Health Centre within 24 hours. 2) Daily BP & temperature monitoring. 3) Plan transport."
        protocol_hi = "1) 24 घंटे में नजदीकी प्राथमिक स्वास्थ्य केंद्र भेजें। 2) प्रतिदिन बीपी और तापमान जांचें।"
        asha_safe_actions = [
            "Advise family on referral to PHC within 24 hours",
            "Schedule daily follow-up visit for BP and symptom check",
            "Identify emergency transport contact in case signs worsen"
        ]
        clinician_directed_actions = [
            "Clinical diagnosis and lab order (Malaria, Urine Protein, Complete Blood Count)",
            "Medication prescription (Antipyretics, Antihypertensives, Therapeutic Iron)"
        ]
    else:
        risk_level = "GREEN"
        if unmeasured:
            rationale_en = f"No danger sign detected from information entered. Unmeasured: {', '.join(unmeasured)}."
            rationale_hi = f"दर्ज जानकारी के आधार पर कोई खतरे का लक्षण नहीं मिला। शेष जांच: {', '.join(unmeasured)}।"
        else:
            rationale_en = "All maternal vitals and clinical observations within normal gestational parameters."
            rationale_hi = "सभी लक्षण और जांच सामान्य हैं।"
        protocol_en = "Continue routine Antenatal Care (ANC) counseling, nutrition guidance, and daily IFA supplementation."
        protocol_hi = "नियमित एएनसी जांच, पौष्टिक आहार और आयरन-फोलिक एसिड गोलियां जारी रखें।"
        asha_safe_actions = [
            "Continue routine Antenatal Care (ANC) counseling",
            "Advise nutritious iron-rich diet and daily IFA tablets",
            "Counsel family on recognizing maternal danger signs"
        ]
        clinician_directed_actions = [
            "Schedule routine 2nd/3rd trimester medical officer review"
        ]

    return ClinicalEvaluationResult(
        risk_level=risk_level,
        risk_score=risk_score,
        primary_factors=factors if factors else ["Normal checkup"],
        unmeasured_vitals=unmeasured,
        clinical_rationale_en=rationale_en,
        clinical_rationale_hi=rationale_hi,
        recommended_protocol_en=protocol_en,
        recommended_protocol_hi=protocol_hi,
        asha_safe_actions=asha_safe_actions,
        clinician_directed_actions=clinician_directed_actions,
        requires_immediate_ambulance=needs_ambulance,
        requires_blood_transfusion_alert=needs_blood,
        rule_pack_version=PROTOCOL_VERSION
    )
