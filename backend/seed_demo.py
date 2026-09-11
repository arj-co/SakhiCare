"""
SakhiCare Demo and Test Fixtures Seeder
Provisions verified facilities, workers, users, and marked demo cases.
Used strictly for development, testing, and explicit demo environments.
"""

import json
import time
import os
import sys
import io
import wave
import struct
import math

# Ensure backend path is in sys.path
sys.path.insert(0, os.path.dirname(__file__))

from database import SessionLocal, init_db
from models import (
    FacilityModel, WorkerModel, UserModel, PregnancyCaseModel,
    AssessmentModel, CaseEventModel, VoiceArtifactModel, NotificationLogModel, TransportRequestModel
)
from auth import hash_password
from gemini_classifier import classify_case_with_gemini
import supabase_storage


def seed_demo_data(db=None):
    close_db = False
    if db is None:
        init_db()
        db = SessionLocal()
        close_db = True

    try:
        now = int(time.time())

        # 1. Facilities
        facilities_data = [
            {
                "id": "FAC-01",
                "name": "Rampur Primary Health Centre",
                "type": "PHC",
                "catchment_area": "Rampur Sector A",
                "contact_phone": "0612-220011",
                "latitude": 25.5941,
                "longitude": 85.1376,
                "capabilities_json": json.dumps(["BASIC_OBSTETRICS", "STABILIZATION", "ORAL_ANTIHYPERTENSIVE"])
            },
            {
                "id": "FAC-02",
                "name": "Kalyanpur Community Health Centre",
                "type": "CHC",
                "catchment_area": "Kalyanpur Block",
                "contact_phone": "0612-220022",
                "latitude": 25.6120,
                "longitude": 85.1500,
                "capabilities_json": json.dumps(["BLOOD_BANK", "CESAREAN_SECTION", "ICU", "IV_LABETALOL", "MAGNESIUM_SULFATE"])
            }
        ]

        for f_data in facilities_data:
            fac = db.query(FacilityModel).filter(FacilityModel.id == f_data["id"]).first()
            if not fac:
                fac = FacilityModel(**f_data)
                db.add(fac)
            else:
                for k, v in f_data.items():
                    setattr(fac, k, v)

        db.commit()

        # 2. Workers
        workers_data = [
            {
                "id": "WKR-101",
                "name": "Anita Devi",
                "role": "ASHA",
                "phone": "9876543210",
                "facility_id": "FAC-01",
                "locale": "hi-IN",
                "status": "ACTIVE"
            },
            {
                "id": "WKR-102",
                "name": "Sunita Kumari",
                "role": "ASHA",
                "phone": "9876543211",
                "facility_id": "FAC-02",
                "locale": "hi-IN",
                "status": "ACTIVE"
            },
            {
                "id": "WKR-103",
                "name": "Poonam Kumari",
                "role": "ASHA",
                "phone": "9876543212",
                "facility_id": "FAC-01",
                "locale": "hi-IN",
                "status": "ACTIVE"
            },
            {
                "id": "WKR-104",
                "name": "Rekha Devi",
                "role": "ASHA",
                "phone": "9876543213",
                "facility_id": "FAC-01",
                "locale": "hi-IN",
                "status": "ACTIVE"
            }
        ]

        for w_data in workers_data:
            wkr = db.query(WorkerModel).filter(WorkerModel.id == w_data["id"]).first()
            if not wkr:
                wkr = WorkerModel(**w_data)
                db.add(wkr)
            else:
                for k, v in w_data.items():
                    setattr(wkr, k, v)

        db.commit()

        # 3. Users
        users_data = [
            {
                "id": "usr_doc_01",
                "username": "doctor_sharma",
                "email": "doctor.sharma@sakhicare.gov.in",
                "password_hash": hash_password("DoctorPass123!"),
                "full_name": "Dr. Sharma",
                "role": "MEDICAL_OFFICER",
                "facility_id": "FAC-01",
                "is_active": True
            },
            {
                "id": "usr_disp_01",
                "username": "dispatch_108",
                "email": "dispatch108@sakhicare.gov.in",
                "password_hash": hash_password("DispatchPass123!"),
                "full_name": "Dispatch 108",
                "role": "DISPATCHER",
                "facility_id": "FAC-01",
                "is_active": True
            },
            {
                "id": "usr_admin_01",
                "username": "admin",
                "email": "admin@sakhicare.gov.in",
                "password_hash": hash_password("AdminPass123!"),
                "full_name": "System Administrator",
                "role": "ADMIN",
                "facility_id": None,
                "is_active": True
            },
            {
                "id": "usr_sup_01",
                "username": "supervisor_anita",
                "email": "supervisor.anita@sakhicare.gov.in",
                "password_hash": hash_password("SupervisorPass123!"),
                "full_name": "Anita Supervisor",
                "role": "SUPERVISOR",
                "facility_id": "FAC-01",
                "is_active": True
            }
        ]

        for u_data in users_data:
            user = db.query(UserModel).filter(UserModel.username == u_data["username"]).first()
            if user and user.id != u_data["id"]:
                db.delete(user)
                db.commit()
                user = None

            if not user:
                user = UserModel(**u_data)
                db.add(user)
            else:
                user.password_hash = u_data["password_hash"]
                user.full_name = u_data["full_name"]
                user.role = u_data["role"]
                user.facility_id = u_data["facility_id"]
                user.is_active = True

        db.commit()

        # 4. Standard Demo Cases (marked with is_demo=True)
        cases_data = [
            {
                "id": "SC-101",
                "local_id": "SC-101",
                "patient_name": "Sunita Devi",
                "village": "Rampur",
                "age_years": 24,
                "gestational_age_weeks": 34,
                "gravida": 2,
                "para": 1,
                "travel_constraints": "Night, road flooded",
                "latitude": 25.5945,
                "longitude": 85.1380,
                "worker_id": "WKR-101",
                "facility_id": "FAC-01",
                "sync_status": "ACKNOWLEDGED",
                "is_demo": True,
                "created_at": now - 3600,
                "assessment": {
                    "id": "ASM-SC-101",
                    "case_id": "SC-101",
                    "risk_level": "RED",
                    "risk_score": 85,
                    "blood_pressure": "160/110",
                    "haemoglobin": 8.5,
                    "primary_factors_json": json.dumps(["Severe Hypertensive Crisis (SBP >= 160 or DBP >= 110)", "Severe Headache / Visual Disturbance"]),
                    "clinical_rationale": "Critical maternal hypertensive emergency with neurological symptoms.",
                    "recommended_protocol": "Immediate 108 emergency transport to CHC with ICU/Labetalol capabilities.",
                    "asha_safe_actions_json": json.dumps(["Place in left lateral tilt", "Do not give oral fluids if drowsy", "Alert 108 ambulance"]),
                    "clinician_directed_actions_json": json.dumps(["Administer IV Labetalol 20mg", "Loading dose Magnesium Sulfate 4g IV + 10g IM"])
                }
            },
            {
                "id": "SC-102",
                "local_id": "SC-102",
                "patient_name": "Pooja Kumari",
                "village": "Rampur",
                "age_years": 22,
                "gestational_age_weeks": 32,
                "gravida": 1,
                "para": 0,
                "travel_constraints": "Rough terrain",
                "latitude": 25.5950,
                "longitude": 85.1390,
                "worker_id": "WKR-101",
                "facility_id": "FAC-01",
                "sync_status": "ACKNOWLEDGED",
                "is_demo": True,
                "created_at": now - 7200,
                "assessment": {
                    "id": "ASM-SC-102",
                    "case_id": "SC-102",
                    "risk_level": "RED",
                    "risk_score": 90,
                    "blood_pressure": "145/95",
                    "haemoglobin": 9.2,
                    "primary_factors_json": json.dumps(["Convulsions or Loss of Consciousness (Eclampsia)"]),
                    "clinical_rationale": "Active eclamptic emergency. High risk of maternal-fetal hypoxia.",
                    "recommended_protocol": "Urgent emergency stabilization and immediate transfer.",
                    "asha_safe_actions_json": json.dumps(["Clear airway, do not put spoon in mouth", "Protect from injury", "Call 108 immediately"]),
                    "clinician_directed_actions_json": json.dumps(["Magnesium sulfate eclampsia protocol", "Oxygen via mask 8-10 L/min"])
                }
            },
            {
                "id": "SC-103",
                "local_id": "SC-103",
                "patient_name": "Meena Devi",
                "village": "Kalyanpur",
                "age_years": 28,
                "gestational_age_weeks": 30,
                "gravida": 3,
                "para": 2,
                "travel_constraints": None,
                "latitude": 25.6130,
                "longitude": 85.1510,
                "worker_id": "WKR-102",
                "facility_id": "FAC-02",
                "sync_status": "ACKNOWLEDGED",
                "is_demo": True,
                "created_at": now - 10800,
                "assessment": {
                    "id": "ASM-SC-103",
                    "case_id": "SC-103",
                    "risk_level": "AMBER",
                    "risk_score": 50,
                    "blood_pressure": "138/88",
                    "haemoglobin": 9.8,
                    "primary_factors_json": json.dumps(["High Fever (>= 38°C / 100.4°F) / Suspected Sepsis"]),
                    "clinical_rationale": "Maternal febrile illness requires evaluation at PHC within 24 hours.",
                    "recommended_protocol": "Refer to PHC for malaria/sepsis workup.",
                    "asha_safe_actions_json": json.dumps(["Tepid sponging", "Ensure adequate oral hydration", "Escort to PHC"]),
                    "clinician_directed_actions_json": json.dumps(["Check peripheral smear for MP", "Urine routine and culture"])
                }
            },
            {
                "id": "SC-104", "local_id": "SC-104", "patient_name": "Kavita Singh", "village": "Rampur",
                "age_years": 26, "gestational_age_weeks": 36, "gravida": 2, "para": 1,
                "travel_constraints": "Night travel difficult", "worker_id": "WKR-103", "facility_id": "FAC-01",
                "sync_status": "ACKNOWLEDGED", "is_demo": True, "created_at": now - 14400,
                "assessment": {
                    "id": "ASM-SC-104", "case_id": "SC-104", "risk_level": "AMBER", "risk_score": 42,
                    "blood_pressure": "148/96", "haemoglobin": 10.2,
                    "primary_factors_json": json.dumps(["Gestational Hypertension"]),
                    "clinical_rationale": "Raised blood pressure needs repeat assessment within 24 hours.",
                    "recommended_protocol": "PHC review within 24 hours.",
                    "asha_safe_actions_json": json.dumps(["Rest and repeat BP", "Escort to PHC"]),
                    "clinician_directed_actions_json": json.dumps(["Repeat BP and urine protein"])
                }
            },
            {
                "id": "SC-105", "local_id": "SC-105", "patient_name": "Rani Kumari", "village": "Bela",
                "age_years": 21, "gestational_age_weeks": 24, "gravida": 1, "para": 0,
                "travel_constraints": "Reliable road access", "worker_id": "WKR-104", "facility_id": "FAC-01",
                "sync_status": "ACKNOWLEDGED", "is_demo": True, "created_at": now - 18000,
                "assessment": {
                    "id": "ASM-SC-105", "case_id": "SC-105", "risk_level": "GREEN", "risk_score": 10,
                    "blood_pressure": "118/76", "haemoglobin": 11.4,
                    "primary_factors_json": json.dumps([]),
                    "clinical_rationale": "Routine antenatal screening with no danger signs recorded.",
                    "recommended_protocol": "Continue routine ANC schedule.",
                    "asha_safe_actions_json": json.dumps(["Nutrition and rest counselling"]),
                    "clinician_directed_actions_json": json.dumps([])
                }
            },
            {
                "id": "SC-106", "local_id": "SC-106", "patient_name": "Nisha Devi", "village": "Bela",
                "age_years": 30, "gestational_age_weeks": 29, "gravida": 3, "para": 2,
                "travel_constraints": "Flooded bridge", "worker_id": "WKR-101", "facility_id": "FAC-01",
                "sync_status": "ACKNOWLEDGED", "is_demo": True, "created_at": now - 21600,
                "assessment": {
                    "id": "ASM-SC-106", "case_id": "SC-106", "risk_level": "RED", "risk_score": 78,
                    "blood_pressure": "170/112", "haemoglobin": 6.8,
                    "primary_factors_json": json.dumps(["Severe Hypertensive Crisis", "Severe Maternal Anemia"]),
                    "clinical_rationale": "Combined severe hypertension and anemia require immediate referral.",
                    "recommended_protocol": "Immediate 108 transport to CHC blood storage centre.",
                    "asha_safe_actions_json": json.dumps(["Call 108", "Left lateral tilt", "Keep patient calm"]),
                    "clinician_directed_actions_json": json.dumps(["Urgent stabilization", "Cross-match blood"])
                }
            }
        ]

        for c_data in cases_data:
            asm_data = c_data.pop("assessment")
            case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == c_data["id"]).first()
            if not case:
                case = PregnancyCaseModel(**c_data)
                db.add(case)
                db.flush()

                asm = AssessmentModel(**asm_data)
                db.add(asm)

                # Initial Case Event
                evt = CaseEventModel(
                    id=f"EVT-{c_data['id']}-01",
                    case_id=c_data["id"],
                    event_type="ASSESSMENT_COMPLETED",
                    actor_id=c_data["worker_id"],
                    actor_role="ASHA",
                    summary=f"Encounter recorded for {c_data['patient_name']}: {asm_data['risk_level']}",
                    details_json=json.dumps({"risk": asm_data["risk_level"], "bp": asm_data["blood_pressure"]}),
                    occurred_at=c_data["created_at"]
                )
                db.add(evt)
            else:
                for k, v in c_data.items():
                    setattr(case, k, v)

        # 5. Four short, listenable demo voice notes. In production these go
        # straight to the private shared Supabase Storage bucket.
        audio_dir = os.path.join(os.path.dirname(__file__), "data", "audio")
        os.makedirs(audio_dir, exist_ok=True)
        for index, case_id in enumerate(("SC-101", "SC-102", "SC-104", "SC-106"), start=1):
            sample = io.BytesIO()
            with wave.open(sample, "wb") as wav:
                wav.setnchannels(1)
                wav.setsampwidth(2)
                wav.setframerate(8000)
                frames = b"".join(struct.pack("<h", int(3500 * math.sin(2 * math.pi * (350 + index * 40) * n / 8000))) for n in range(8000))
                wav.writeframes(frames)
            audio_bytes = sample.getvalue()
            sha = __import__("hashlib").sha256(audio_bytes).hexdigest()
            filename = f"{case_id}_demo_voice.wav"
            storage_path = f"cases/{case_id}/{filename}"
            local_path = os.path.join(audio_dir, filename)
            if os.getenv("APP_ENV", "development").lower() == "production":
                if supabase_storage.is_configured():
                    supabase_storage.upload(storage_path, audio_bytes, "audio/wav")
                else:
                    continue
                file_path = None
            else:
                with open(local_path, "wb") as audio_file:
                    audio_file.write(audio_bytes)
                file_path = local_path
            artifact = db.query(VoiceArtifactModel).filter(VoiceArtifactModel.case_id == case_id).first()
            if not artifact:
                db.add(VoiceArtifactModel(
                    id=f"aud_demo_{case_id}", case_id=case_id, storage_path=storage_path,
                    file_path=file_path, filename=filename, mime_type="audio/wav",
                    file_size_bytes=len(audio_bytes), sha256=sha, language="hi-IN",
                    duration_seconds=1, transcript="Demo voice note for clinical review.",
                    processing_status="CONFIRMED", upload_status="UPLOADED",
                    retention_due_at=now + 90 * 86400, uploaded_at=now
                ))

        # 6. Seed the desk with privacy-safe message history and one dispatch.
        if not db.query(NotificationLogModel).filter(NotificationLogModel.id == "notif_demo_SC-101").first():
            db.add(NotificationLogModel(
                id="notif_demo_SC-101", case_id="SC-101", channel="PUSH",
                recipient="MO_SHARMA", template_type="EMERGENCY_TRIAGE_ALERT",
                content_preview="RED triage alert: Case SC-101 in Rampur requires immediate review.",
                status="SENT", provider_ref="demo-push-101", created_at=now - 3500, delivered_at=now - 3490
            ))
            db.add(NotificationLogModel(
                id="notif_demo_SC-106", case_id="SC-106", channel="SMS",
                recipient="SUPERVISOR_ANITA", template_type="ESCALATION_SUPERVISOR_ALERT",
                content_preview="RED escalation: Case SC-106 | Area: Bela | Immediate referral required.",
                status="DELIVERED", provider_ref="demo-sms-106", created_at=now - 21000, delivered_at=now - 20990
            ))
        if not db.query(TransportRequestModel).filter(TransportRequestModel.id == "transport_demo_SC-101").first():
            db.add(TransportRequestModel(
                id="transport_demo_SC-101", case_id="SC-101", status="EN_ROUTE",
                vehicle_id="108-AMB-Rampur-09", destination_facility_id="FAC-02",
                destination_facility_name="Kalyanpur Community Health Centre",
                driver_name="Santosh Yadav", driver_phone="+91 98765 43210",
                call_attempt_notes="Demo dispatch confirmed by 108 operator.",
                confirmed_by="dispatch_108", created_at=now - 3300, updated_at=now - 3000
            ))

        # Store Gemini's second opinion in the case timeline when configured.
        for case_data in cases_data:
            review = classify_case_with_gemini({
                "case_id": case_data["id"], "patient_name": case_data["patient_name"],
                "blood_pressure": case_data.get("blood_pressure"),
                "haemoglobin": (case_data.get("assessment") or {}).get("haemoglobin"),
                "deterministic_risk": (case_data.get("assessment") or {}).get("risk_level"),
            })
            if review and not db.query(CaseEventModel).filter(CaseEventModel.case_id == case_data["id"], CaseEventModel.event_type == "GEMINI_RISK_REVIEW").first():
                db.add(CaseEventModel(
                    id=f"EVT-GEMINI-{case_data['id']}", case_id=case_data["id"],
                    event_type="GEMINI_RISK_REVIEW", actor_id="GEMINI", actor_role="AI_REVIEWER",
                    summary=f"Gemini second opinion: {review['risk_level']} ({review['confidence']}% confidence)",
                    details_json=json.dumps(review), occurred_at=now
                ))

        db.commit()
        print(f"Successfully seeded {len(facilities_data)} facilities, {len(workers_data)} workers, {len(users_data)} users, and {len(cases_data)} demo cases.")
    finally:
        if close_db:
            db.close()


if __name__ == "__main__":
    seed_demo_data()
