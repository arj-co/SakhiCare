import csv
import io
from typing import List, Dict, Any

def export_cases_to_csv(cases: List[Dict[str, Any]]) -> str:
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["ID", "Patient Name", "Village", "BP", "Hb", "Risk Level", "Sync Status"])
    for c in cases:
        writer.writerow([c.get("id"), c.get("patient_name"), c.get("village"), c.get("blood_pressure"), c.get("haemoglobin"), c.get("risk_level"), c.get("sync_status")])
    return output.getvalue()
