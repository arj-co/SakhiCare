package com.sakhicare.app.fhir

import com.sakhicare.app.data.PatientCase

object FhirBundleConverter {

    fun caseToFhirJson(patientCase: PatientCase): String {
        val (sysBp, diaBp) = parseBp(patientCase.bloodPressure)
        val hbValue = parseHb(patientCase.haemoglobin)

        val bleedingCond = if (patientCase.dangerSigns.bleeding) """
    {
      "fullUrl": "urn:uuid:cond-bleeding-${patientCase.id}",
      "resource": {
        "resourceType": "Condition",
        "id": "cond-bleeding-${patientCase.id}",
        "clinicalStatus": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active" }] },
        "code": { "coding": [{ "system": "http://snomed.info/sct", "code": "289530006", "display": "Vaginal bleeding in pregnancy" }], "text": "Vaginal bleeding in pregnancy" },
        "subject": { "reference": "urn:uuid:patient-${patientCase.id}" }
      }
    },""" else ""

        val feverCond = if (patientCase.dangerSigns.fever) """
    {
      "fullUrl": "urn:uuid:cond-fever-${patientCase.id}",
      "resource": {
        "resourceType": "Condition",
        "id": "cond-fever-${patientCase.id}",
        "clinicalStatus": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active" }] },
        "code": { "coding": [{ "system": "http://snomed.info/sct", "code": "386661006", "display": "Fever in pregnancy" }], "text": "Fever in pregnancy" },
        "subject": { "reference": "urn:uuid:patient-${patientCase.id}" }
      }
    },""" else ""

        val headacheCond = if (patientCase.dangerSigns.headache) """
    {
      "fullUrl": "urn:uuid:cond-headache-${patientCase.id}",
      "resource": {
        "resourceType": "Condition",
        "id": "cond-headache-${patientCase.id}",
        "clinicalStatus": { "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active" }] },
        "code": { "coding": [{ "system": "http://snomed.info/sct", "code": "25064002", "display": "Severe headache" }], "text": "Severe headache" },
        "subject": { "reference": "urn:uuid:patient-${patientCase.id}" }
      }
    },""" else ""

        return """{
  "resourceType": "Bundle",
  "id": "sakhicare-bundle-${patientCase.id}",
  "type": "collection",
  "timestamp": "2026-08-17T10:00:00Z",
  "meta": {
    "tag": [
      {
        "system": "http://sakhicare.org/triage-risk",
        "code": "${patientCase.riskLevel.name}",
        "display": "${patientCase.riskLevel.name} Triage Risk"
      }
    ]
  },
  "entry": [
    {
      "fullUrl": "urn:uuid:patient-${patientCase.id}",
      "resource": {
        "resourceType": "Patient",
        "id": "${patientCase.id}",
        "active": true,
        "name": [
          {
            "use": "official",
            "text": "${patientCase.patientName}"
          }
        ],
        "gender": "female",
        "address": [
          {
            "city": "${patientCase.village}",
            "country": "India"
          }
        ]
      }
    },
    {
      "fullUrl": "urn:uuid:obs-bp-${patientCase.id}",
      "resource": {
        "resourceType": "Observation",
        "id": "obs-bp-${patientCase.id}",
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
        "subject": {
          "reference": "urn:uuid:patient-${patientCase.id}"
        },
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
              "value": $sysBp,
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
              "value": $diaBp,
              "unit": "mmHg",
              "system": "http://unitsofmeasure.org",
              "code": "mm[Hg]"
            }
          }
        ]
      }
    },
    {
      "fullUrl": "urn:uuid:obs-hb-${patientCase.id}",
      "resource": {
        "resourceType": "Observation",
        "id": "obs-hb-${patientCase.id}",
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
        "subject": {
          "reference": "urn:uuid:patient-${patientCase.id}"
        },
        "valueQuantity": {
          "value": $hbValue,
          "unit": "g/dL",
          "system": "http://unitsofmeasure.org",
          "code": "g/dL"
        }
      }
    }$bleedingCond$feverCond$headacheCond
  ]
}"""
    }

    private fun parseBp(bp: String): Pair<Int, Int> {
        val parts = bp.split("/")
        val sys = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 120
        val dia = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 80
        return Pair(sys, dia)
    }

    private fun parseHb(hb: String): Double {
        val cleaned = hb.replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull() ?: 11.0
    }
}
