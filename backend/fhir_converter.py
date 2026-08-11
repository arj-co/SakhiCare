"""
SakhiCare FHIR R4 Bundle Converter Engine
Converts SakhiCare assessment payloads into standard HL7 FHIR R4 JSON Bundles.
"""

from typing import Dict, Any, List
import uuid
from datetime import datetime, timezone


def generate_fhir_bundle(
    patient_id: str,
    patient_name: str,
    village: str,
    blood_pressure: str,
    haemoglobin: float,
    danger_signs: Dict[str, bool],
    risk_level: str = "GREEN",
    timestamp: str = None
) -> Dict[str, Any]:
    """
    Generate an HL7 FHIR R4 collection bundle containing Patient,
    Vital Sign Observations (BP, Hb), and Condition resources for reported danger signs.
    """
    if not timestamp:
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    patient_uuid = f"urn:uuid:{patient_id or str(uuid.uuid4())}"

    # Split name into given and family name if possible
    name_parts = patient_name.strip().split(" ", 1)
    given_name = name_parts[0] if name_parts else "Unknown"
    family_name = name_parts[1] if len(name_parts) > 1 else ""

    patient_resource = {
        "resourceType": "Patient",
        "id": patient_id or "patient-001",
        "active": True,
        "name": [
            {
                "use": "official",
                "given": [given_name],
                "family": family_name
            }
        ],
        "gender": "female",
        "address": [
            {
                "city": village,
                "country": "India"
            }
        ]
    }

    entries: List[Dict[str, Any]] = [
        {
            "fullUrl": patient_uuid,
            "resource": patient_resource
        }
    ]

    # Parse Blood Pressure
    try:
        bp_parts = blood_pressure.strip().split("/")
        sys_val = int(bp_parts[0].strip())
        dia_val = int(bp_parts[1].strip())
    except (ValueError, IndexError):
        sys_val = 120
        dia_val = 80

    bp_observation = {
        "resourceType": "Observation",
        "id": f"obs-bp-{patient_id}",
        "status": "final",
        "category": [
            {
                "coding": [
                    {
                        "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                        "code": "vital-signs",
                        "display": "Vital Signs"
                    }
                ]
            }
        ],
        "code": {
            "coding": [
                {
                    "system": "http://loinc.org",
                    "code": "85354-9",
                    "display": "Blood pressure panel with all children optional"
                }
            ],
            "text": "Blood Pressure"
        },
        "subject": {"reference": patient_uuid},
        "effectiveDateTime": timestamp,
        "component": [
            {
                "code": {
                    "coding": [
                        {
                            "system": "http://loinc.org",
                            "code": "8480-6",
                            "display": "Systolic blood pressure"
                        }
                    ]
                },
                "valueQuantity": {
                    "value": sys_val,
                    "unit": "mmHg",
                    "system": "http://unitsofmeasure.org",
                    "code": "mm[Hg]"
                }
            },
            {
                "code": {
                    "coding": [
                        {
                            "system": "http://loinc.org",
                            "code": "8462-4",
                            "display": "Diastolic blood pressure"
                        }
                    ]
                },
                "valueQuantity": {
                    "value": dia_val,
                    "unit": "mmHg",
                    "system": "http://unitsofmeasure.org",
                    "code": "mm[Hg]"
                }
            }
        ]
    }
    entries.append({
        "fullUrl": f"urn:uuid:obs-bp-{patient_id}",
        "resource": bp_observation
    })

    # Haemoglobin Observation
    hb_observation = {
        "resourceType": "Observation",
        "id": f"obs-hb-{patient_id}",
        "status": "final",
        "category": [
            {
                "coding": [
                    {
                        "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                        "code": "vital-signs",
                        "display": "Vital Signs"
                    }
                ]
            }
        ],
        "code": {
            "coding": [
                {
                    "system": "http://loinc.org",
                    "code": "718-7",
                    "display": "Hemoglobin [Mass/volume] in Blood"
                }
            ],
            "text": "Haemoglobin"
        },
        "subject": {"reference": patient_uuid},
        "effectiveDateTime": timestamp,
        "valueQuantity": {
            "value": haemoglobin,
            "unit": "g/dL",
            "system": "http://unitsofmeasure.org",
            "code": "g/dL"
        }
    }
    entries.append({
        "fullUrl": f"urn:uuid:obs-hb-{patient_id}",
        "resource": hb_observation
    })

    # Danger Signs Conditions
    danger_sign_codes = {
        "bleeding": ("289530006", "Vaginal bleeding in pregnancy"),
        "fever": ("386661006", "Fever in pregnancy"),
        "headache": ("25064002", "Severe headache"),
        "reduced_fetal_movement": ("289439004", "Decreased fetal movement")
    }

    for sign_key, is_present in danger_signs.items():
        if is_present and sign_key in danger_sign_codes:
            code, display = danger_sign_codes[sign_key]
            condition_resource = {
                "resourceType": "Condition",
                "id": f"cond-{sign_key}-{patient_id}",
                "clinicalStatus": {
                    "coding": [
                        {
                            "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
                            "code": "active"
                        }
                    ]
                },
                "code": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": code,
                            "display": display
                        }
                    ],
                    "text": display
                },
                "subject": {"reference": patient_uuid},
                "onsetDateTime": timestamp
            }
            entries.append({
                "fullUrl": f"urn:uuid:cond-{sign_key}-{patient_id}",
                "resource": condition_resource
            })

    return {
        "resourceType": "Bundle",
        "id": f"sakhicare-bundle-{patient_id}",
        "type": "collection",
        "timestamp": timestamp,
        "meta": {
            "tag": [
                {
                    "system": "http://sakhicare.org/triage-risk",
                    "code": risk_level,
                    "display": f"{risk_level} Triage Risk"
                }
            ]
        },
        "entry": entries
    }
