def calculate_shock_index(heart_rate: int, systolic_bp: int) -> float:
    if systolic_bp <= 0: return 0.0
    return round(heart_rate / float(systolic_bp), 2)

def evaluate_pph_risk(shock_index: float, bleeding: bool) -> str:
    if bleeding and shock_index >= 0.9:
        return "HIGH_RISK_OBSTETRIC_SHOCK"
    elif bleeding:
        return "MODERATE_RISK_ACTIVE_BLEED"
    return "LOW_RISK"
