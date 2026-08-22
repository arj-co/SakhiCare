from typing import Dict, Any, List

def evaluate_neonatal_danger_signs(temperature_c: float, feeding_well: bool, breathing_rate_cpm: int, chest_indrawing: bool) -> Dict[str, Any]:
    alerts = []
    if temperature_c < 36.0:
        alerts.append("Severe Hypothermia: Apply immediate Kangaroo Mother Care (KMC) and warm wraps.")
    if not feeding_well:
        alerts.append("Inability to feed: High risk marker for Neonatal Sepsis.")
    if breathing_rate_cpm > 60 or chest_indrawing:
        alerts.append("Severe Respiratory Distress: Immediate oxygenation & SNCU referral required.")

    return {
        "status": "DANGER" if alerts else "NORMAL",
        "alerts": alerts,
        "action": "Urgent SNCU Transfer" if alerts else "Routine Postnatal Care"
    }
