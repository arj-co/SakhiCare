"""
SakhiCare Demo and Test Fixtures Seeder
Provisions verified facilities, workers, users, and marked demo cases.
Used strictly for development, testing, and explicit demo environments.
"""

import json
import time
import os
import sys

# Ensure backend path is in sys.path
sys.path.insert(0, os.path.dirname(__file__))

from database import SessionLocal, init_db
from models import (
    FacilityModel, WorkerModel, UserModel, PregnancyCaseModel,
    AssessmentModel, CaseEventModel
)
from auth import hash_password


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

        db.commit()
        print(f"Successfully seeded {len(facilities_data)} facilities, {len(workers_data)} workers, {len(users_data)} users, and {len(cases_data)} demo cases.")
    finally:
        if close_db:
            db.close()


if __name__ == "__main__":
    seed_demo_data()
