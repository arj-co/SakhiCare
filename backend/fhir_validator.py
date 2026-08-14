from typing import Dict, Any

def validate_fhir_bundle(bundle: Dict[str, Any]) -> bool:
    if bundle.get("resourceType") != "Bundle":
        return False
    if "entry" not in bundle or not isinstance(bundle["entry"], list):
        return False
    for entry in bundle["entry"]:
        resource = entry.get("resource", {})
        if "resourceType" not in resource:
            return False
    return True
