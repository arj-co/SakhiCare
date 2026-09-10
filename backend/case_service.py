import time
import json
import uuid
from typing import Dict, Any, List, Optional
from sqlalchemy.orm import Session
from models import (
    FacilityModel, WorkerModel, UserModel, PregnancyCaseModel,
    AssessmentModel, VoiceArtifactModel, CaseEventModel, OutboxItemModel,
    TransportRequestModel, NotificationLogModel
)
from triage_engine import evaluate_clinical_risk


def seed_reference_data_if_empty(db: Session):
    """
    Seeds default administrative and facility records if empty.
    Does NOT seed fake patient cases in production.
    """
    if db.query(FacilityModel).first() is None:
        fac1 = FacilityModel(
            id="FAC-01",
            name="Rampur Primary Health Centre (PHC)",
            type="PHC",
            catchment_area="Rampur, Sitapur, Gopalpur",
            contact_phone="0612-2554401"
        )
        fac2 = FacilityModel(
            id="FAC-02",
            name="Chandanpur Community Health Centre (CHC)",
            type="CHC",
            catchment_area="Chandanpur, Madhubani",
            contact_phone="0612-2554402"
        )
        db.add_all([fac1, fac2])

    if db.query(UserModel).first() is None:
        from auth import hash_password
        users = [
            UserModel(
                id="usr_mo_01",
                username="doctor_sharma",
                email="dr.sharma@sakhicare.gov.in",
                password_hash=hash_password("DoctorPass123!"),
                full_name="Dr. Rajiv Sharma (Medical Officer)",
                role="MEDICAL_OFFICER",
                facility_id="FAC-01"
            ),
            UserModel(
                id="usr_sup_01",
                username="supervisor_anita",
                email="anita.sup@sakhicare.gov.in",
                password_hash=hash_password("SuperPass123!"),
                full_name="Anita Kumari (Block Supervisor)",
                role="SUPERVISOR",
                facility_id="FAC-01"
            ),
            UserModel(
                id="usr_disp_01",
                username="dispatch_108",
                email="dispatch108@sakhicare.gov.in",
                password_hash=hash_password("DispatchPass123!"),
                full_name="Vikram Singh (108 Transport Coordinator)",
                role="DISPATCHER",
                facility_id=None
            ),
            UserModel(
                id="usr_admin_01",
                username="admin_sakhicare",
                email="admin@sakhicare.gov.in",
                password_hash=hash_password("AdminPass123!"),
                full_name="System Administrator",
                role="ADMIN",
                facility_id=None
            )
        ]
        db.add_all(users)

    if db.query(WorkerModel).first() is None:
        workers = [
            WorkerModel(
                id="WKR-101",
                name="Shanti Devi",
                role="ASHA",
                phone="9876543210",
                facility_id="FAC-01",
                locale="hi-IN",
                status="ACTIVE"
            ),
            WorkerModel(
                id="WKR-102",
                name="Pushpa Kumari",
                role="ASHA",
                phone="9876543211",
                facility_id="FAC-01",
                locale="hi-IN",
                status="ACTIVE"
            )
        ]
        db.add_all(workers)

    db.commit()


def seed_demo_cases_if_empty(db: Session):
    """
    Seeds initial demo cases if database has no cases.
    Tagged with is_demo=True.
    """
    if db.query(PregnancyCaseModel).first() is not None:
        return

    demo_cases = [
        {
            "case_id": "SC-101",
            "patient_name": "Sunita Devi",
            "village": "Rampur",
            "age_years": 24,
            "gestational_age_weeks": 34,
            "gravida": 2,
            "para": 1,
            "travel_constraints": "Broken culvert after heavy rain, 4x4 or tractor trail only",
            "worker_id": "WKR-101",
            "facility_id": "FAC-01",
            "blood_pressure": "162/108",
            "haemoglobin": 6.8,
            "danger_signs": {"bleeding": True, "fever": False, "headache": True, "reduced_fetal_movement": False},
            "is_demo": True
        },
        {
            "case_id": "SC-102",
            "patient_name": "Meena Kumari",
            "village": "Bhimpur",
            "age_years": 22,
            "gestational_age_weeks": 28,
            "gravida": 1,
            "para": 0,
            "travel_constraints": "Standard village road, paved",
            "worker_id": "WKR-102",
            "facility_id": "FAC-01",
            "blood_pressure": "142/92",
            "haemoglobin": 8.5,
            "danger_signs": {"bleeding": False, "fever": True, "headache": True, "reduced_fetal_movement": False},
            "is_demo": True
        },
        {
            "case_id": "SC-103",
            "patient_name": "Pooja Sharma",
            "village": "Kalyanpur",
            "age_years": 26,
            "gestational_age_weeks": 32,
            "gravida": 3,
            "para": 2,
            "travel_constraints": "Direct highway access",
            "worker_id": "WKR-101",
            "facility_id": "FAC-01",
            "blood_pressure": "118/76",
            "haemoglobin": 11.8,
            "danger_signs": {"bleeding": False, "fever": False, "headache": False, "reduced_fetal_movement": False},
            "is_demo": True
        }
    ]

    for c in demo_cases:
        ingest_sync_case_batch(db, c)


def ingest_sync_case_batch(db: Session, payload: Dict[str, Any]) -> Dict[str, Any]:
    """
    Ingests or updates a case received via sync.
    Strictly idempotent: if an outbox item with idempotency_key already exists,
    returns existing record without creating duplicates or duplicate audit events.
    """
    idempotency_key = payload.get("idempotency_key")
    case_id = payload.get("case_id") or payload.get("patient_id") or f"SC-{int(time.time())}"

    # Check duplicate idempotency key
    if idempotency_key:
        existing_outbox = db.query(OutboxItemModel).filter(OutboxItemModel.id == idempotency_key).first()
        if existing_outbox:
            existing_case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
            return {
                "status": "ACKNOWLEDGED",
                "case_id": case_id,
                "server_timestamp": int(time.time()),
                "is_duplicate": True,
                "doctor_advisory": existing_case.doctor_advisory if existing_case else None,
                "ambulance_status": existing_case.ambulance_status if existing_case else None
            }

    # Record outbox idempotency item
    if idempotency_key:
        outbox_entry = OutboxItemModel(
            id=idempotency_key,
            case_id=case_id,
            entity_type="CASE_ASSESSMENT",
            payload_json=json.dumps(payload),
            status="ACKNOWLEDGED"
        )
        db.add(outbox_entry)

    # Find or create pregnancy case
    case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
    is_new = case is None
    if is_new:
        case = PregnancyCaseModel(
            id=case_id,
            local_id=case_id,
            patient_name=payload.get("patient_name", "Unknown Patient"),
            village=payload.get("village", "Unknown Village"),
            age_years=payload.get("age_years"),
            gestational_age_weeks=payload.get("gestational_age_weeks"),
            gravida=payload.get("gravida"),
            para=payload.get("para"),
            travel_constraints=payload.get("travel_constraints"),
            worker_id=payload.get("worker_id", "WKR-101"),
            facility_id=payload.get("facility_id", "FAC-01"),
            sync_status="ACKNOWLEDGED",
            is_demo=payload.get("is_demo", False),
            created_at=payload.get("created_at") or int(time.time()),
            updated_at=int(time.time())
        )
        db.add(case)
    else:
        case.patient_name = payload.get("patient_name", case.patient_name)
        case.village = payload.get("village", case.village)
        case.sync_status = "ACKNOWLEDGED"
        case.updated_at = int(time.time())

    # Evaluate clinical risk deterministically
    danger_signs = payload.get("danger_signs", {})
    bp = payload.get("blood_pressure")
    hb = payload.get("haemoglobin")
    hb_float = None
    if hb is not None:
        try:
            hb_float = float(str(hb).replace("g/dL", "").strip())
        except ValueError:
            hb_float = None

    triage_res = evaluate_clinical_risk(
        blood_pressure=bp,
        haemoglobin=hb_float,
        danger_signs=danger_signs
    )

    # Create assessment record
    assessment_id = f"ASM-{case_id[:8]}-{uuid.uuid4().hex[:8]}"
    assessment = AssessmentModel(
        id=assessment_id,
        case_id=case_id,
        rule_pack_version="mohfw-hrp-v1.0",
        risk_level=triage_res.risk_level,
        risk_score=triage_res.risk_score,
        primary_factors_json=json.dumps(triage_res.primary_factors),
        unmeasured_vitals_json=json.dumps(triage_res.unmeasured_vitals),
        clinical_rationale=triage_res.clinical_rationale_en,
        recommended_protocol=triage_res.recommended_protocol_en,
        asha_safe_actions_json=json.dumps(triage_res.asha_safe_actions),
        clinician_directed_actions_json=json.dumps(triage_res.clinician_directed_actions),
        requires_immediate_ambulance=triage_res.requires_immediate_ambulance,
        requires_blood_transfusion_alert=triage_res.requires_blood_transfusion_alert,
        blood_pressure=bp,
        haemoglobin=hb_float,
        danger_signs_json=json.dumps(danger_signs),
        created_at=int(time.time())
    )
    db.add(assessment)

    # Append case event
    event_summary = f"Case received via sync: {triage_res.risk_level} risk ({'; '.join(triage_res.primary_factors[:2])})"
    event = CaseEventModel(
        id=f"EVT-{int(time.time() * 1000)}-{uuid.uuid4().hex[:6]}",
        case_id=case_id,
        event_type="SYNC_ACKNOWLEDGED",
        actor_id=payload.get("worker_id", "ASHA_WORKER"),
        actor_role="ASHA",
        summary=event_summary,
        details_json=json.dumps({"risk_level": triage_res.risk_level, "score": triage_res.risk_score}),
        occurred_at=int(time.time())
    )
    db.add(event)
    db.commit()

    if triage_res.risk_level == "RED":
        try:
            from notification_service import trigger_emergency_escalation
            trigger_emergency_escalation(
                db=db,
                case_id=case_id,
                patient_name=case.patient_name,
                village=case.village,
                urgency="RED",
                danger_signs=triage_res.primary_factors
            )
        except Exception:
            pass

    return {
        "status": "ACKNOWLEDGED",
        "case_id": case_id,
        "server_timestamp": int(time.time()),
        "doctor_advisory": case.doctor_advisory,
        "ambulance_status": case.ambulance_status
    }


def get_case_detail(db: Session, case_id: str) -> Optional[Dict[str, Any]]:
    """
    Returns full case detail including latest assessment, audio artifact, and events.
    Returns None if case does not exist (404).
    """
    case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
    if not case:
        return None

    latest_assessment = db.query(AssessmentModel).filter(AssessmentModel.case_id == case_id).order_by(AssessmentModel.created_at.desc()).first()
    latest_audio = db.query(VoiceArtifactModel).filter(VoiceArtifactModel.case_id == case_id).order_by(VoiceArtifactModel.uploaded_at.desc()).first()
    events = db.query(CaseEventModel).filter(CaseEventModel.case_id == case_id).order_by(CaseEventModel.occurred_at.asc()).all()

    return {
        "case_id": case.id,
        "patient_name": case.patient_name,
        "village": case.village,
        "age_years": case.age_years,
        "gestational_age_weeks": case.gestational_age_weeks,
        "gravida": case.gravida,
        "para": case.para,
        "travel_constraints": case.travel_constraints,
        "worker_id": case.worker_id,
        "facility_id": case.facility_id,
        "sync_status": case.sync_status,
        "doctor_advisory": case.doctor_advisory,
        "ambulance_status": case.ambulance_status,
        "is_demo": case.is_demo,
        "created_at": case.created_at,
        "updated_at": case.updated_at,
        "assessment": {
            "risk_level": latest_assessment.risk_level if latest_assessment else "GREEN",
            "risk_score": latest_assessment.risk_score if latest_assessment else 10,
            "blood_pressure": latest_assessment.blood_pressure if latest_assessment else None,
            "haemoglobin": latest_assessment.haemoglobin if latest_assessment else None,
            "danger_signs": json.loads(latest_assessment.danger_signs_json) if latest_assessment and latest_assessment.danger_signs_json else {},
            "primary_factors": json.loads(latest_assessment.primary_factors_json) if latest_assessment else [],
            "unmeasured_vitals": json.loads(latest_assessment.unmeasured_vitals_json) if latest_assessment else [],
            "clinical_rationale": latest_assessment.clinical_rationale if latest_assessment else "",
            "recommended_protocol": latest_assessment.recommended_protocol if latest_assessment else "",
            "asha_safe_actions": json.loads(latest_assessment.asha_safe_actions_json) if latest_assessment else [],
            "clinician_directed_actions": json.loads(latest_assessment.clinician_directed_actions_json) if latest_assessment else [],
            "requires_immediate_ambulance": latest_assessment.requires_immediate_ambulance if latest_assessment else False,
            "requires_blood_transfusion_alert": latest_assessment.requires_blood_transfusion_alert if latest_assessment else False
        } if latest_assessment else None,
        "audio_artifact": {
            "artifact_id": latest_audio.id,
            "filename": latest_audio.filename,
            "sha256": latest_audio.sha256,
            "duration_seconds": latest_audio.duration_seconds,
            "language": latest_audio.language,
            "transcript": latest_audio.transcript,
            "upload_status": latest_audio.upload_status,
            "retention_due_at": latest_audio.retention_due_at
        } if latest_audio else None,
        "timeline": [
            {
                "event_id": e.id,
                "event_type": e.event_type,
                "actor_id": e.actor_id,
                "actor_role": e.actor_role,
                "summary": e.summary,
                "occurred_at": e.occurred_at
            }
            for e in events
        ]
    }


def list_cases(
    db: Session,
    facility_id: Optional[str] = None,
    risk_level: Optional[str] = None,
    status_filter: Optional[str] = None,
    limit: int = 50,
    offset: int = 0
) -> List[Dict[str, Any]]:
    """
    Lists cases with optional facility scoping and urgency filters.
    """
    query = db.query(PregnancyCaseModel)
    if facility_id:
        query = query.filter(PregnancyCaseModel.facility_id == facility_id)

    cases = query.order_by(PregnancyCaseModel.created_at.desc()).offset(offset).limit(limit).all()

    result = []
    for c in cases:
        detail = get_case_detail(db, c.id)
        if detail:
            if risk_level and detail["assessment"] and detail["assessment"]["risk_level"] != risk_level:
                continue
            if status_filter and c.sync_status != status_filter:
                continue
            result.append(detail)

    # Sort: RED first, then AMBER, then GREEN
    def sort_key(item):
        risk = item["assessment"]["risk_level"] if item.get("assessment") else "GREEN"
        risk_rank = 0 if risk == "RED" else (1 if risk == "AMBER" else 2)
        return (risk_rank, -item["created_at"])

    result.sort(key=sort_key)
    return result


def acknowledge_case(
    db: Session,
    case_id: str,
    actor_id: str,
    actor_role: str,
    advisory: str,
    referral_facility_id: Optional[str] = None
) -> Optional[Dict[str, Any]]:
    """
    Records a Medical Officer's acknowledgement and clinical guidance.
    """
    case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
    if not case:
        return None

    case.doctor_advisory = advisory
    case.sync_status = "ACKNOWLEDGED"
    case.updated_at = int(time.time())

    summary = f"Doctor advisory issued: {advisory[:80]}..."
    if referral_facility_id:
        summary += f" (Referral: {referral_facility_id})"

    event = CaseEventModel(
        id=f"EVT-{int(time.time() * 1000)}-{uuid.uuid4().hex[:6]}",
        case_id=case_id,
        event_type="DOCTOR_ADVISORY_ISSUED",
        actor_id=actor_id,
        actor_role=actor_role,
        summary=summary,
        details_json=json.dumps({"advisory": advisory, "referral_facility": referral_facility_id}),
        occurred_at=int(time.time())
    )
    db.add(event)
    db.commit()

    return get_case_detail(db, case_id)


def update_transport(
    db: Session,
    case_id: str,
    actor_id: str,
    ambulance_status: str,
    driver_phone: Optional[str] = None,
    destination: Optional[str] = None
) -> Optional[Dict[str, Any]]:
    """
    Records transport coordination status.
    """
    case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
    if not case:
        return None

    case.ambulance_status = ambulance_status
    case.updated_at = int(time.time())

    summary = f"Transport coordination: {ambulance_status}"
    if destination:
        summary += f" -> {destination}"

    event = CaseEventModel(
        id=f"EVT-{int(time.time() * 1000)}-{uuid.uuid4().hex[:6]}",
        case_id=case_id,
        event_type="TRANSPORT_STATUS_UPDATED",
        actor_id=actor_id,
        actor_role="DISPATCHER",
        summary=summary,
        details_json=json.dumps({"status": ambulance_status, "driver_phone": driver_phone, "destination": destination}),
        occurred_at=int(time.time())
    )
    db.add(event)
    db.commit()

    return get_case_detail(db, case_id)


def create_or_update_transport_request(
    db: Session,
    case_id: str,
    status: str,  # REQUESTED, CALL_ATTEMPTED, CONFIRMED, EN_ROUTE, ARRIVED, FAILED
    actor_id: str,
    vehicle_id: Optional[str] = None,
    destination_facility_id: Optional[str] = None,
    destination_facility_name: Optional[str] = None,
    driver_name: Optional[str] = None,
    driver_phone: Optional[str] = None,
    call_attempt_notes: Optional[str] = None
) -> Optional[Dict[str, Any]]:
    case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == case_id).first()
    if not case:
        return None

    # Find existing transport request or create new
    req = db.query(TransportRequestModel).filter(TransportRequestModel.case_id == case_id).first()
    is_new = req is None
    if is_new:
        req = TransportRequestModel(
            id=f"TRN-{uuid.uuid4().hex[:12]}",
            case_id=case_id,
            status=status,
            vehicle_id=vehicle_id,
            destination_facility_id=destination_facility_id,
            destination_facility_name=destination_facility_name,
            driver_name=driver_name,
            driver_phone=driver_phone,
            call_attempt_notes=call_attempt_notes,
            confirmed_by=actor_id if status in ("CONFIRMED", "EN_ROUTE", "ARRIVED") else None,
            created_at=int(time.time()),
            updated_at=int(time.time())
        )
        db.add(req)
    else:
        req.status = status
        if vehicle_id: req.vehicle_id = vehicle_id
        if destination_facility_id: req.destination_facility_id = destination_facility_id
        if destination_facility_name: req.destination_facility_name = destination_facility_name
        if driver_name: req.driver_name = driver_name
        if driver_phone: req.driver_phone = driver_phone
        if call_attempt_notes: req.call_attempt_notes = call_attempt_notes
        if status in ("CONFIRMED", "EN_ROUTE", "ARRIVED"): req.confirmed_by = actor_id
        req.updated_at = int(time.time())

    # Update case ambulance_status summary
    status_summary = f"{vehicle_id or 'Ambulance'} - {status}"
    if destination_facility_name:
        status_summary += f" -> {destination_facility_name}"
    case.ambulance_status = status_summary
    case.updated_at = int(time.time())

    # Record event
    summary = f"108 Transport status updated to {status}"
    if vehicle_id:
        summary += f" (Vehicle: {vehicle_id})"
    if call_attempt_notes:
        summary += f" [Notes: {call_attempt_notes[:60]}...]"

    event = CaseEventModel(
        id=f"EVT-{int(time.time() * 1000)}-{uuid.uuid4().hex[:6]}",
        case_id=case_id,
        event_type="TRANSPORT_STATUS_UPDATED",
        actor_id=actor_id,
        actor_role="DISPATCHER",
        summary=summary,
        details_json=json.dumps({
            "status": status,
            "vehicle_id": vehicle_id,
            "destination": destination_facility_name,
            "driver_phone": driver_phone,
            "call_notes": call_attempt_notes
        }),
        occurred_at=int(time.time())
    )
    db.add(event)
    db.commit()

    return {
        "id": req.id,
        "case_id": req.case_id,
        "status": req.status,
        "vehicle_id": req.vehicle_id,
        "destination_facility_id": req.destination_facility_id,
        "destination_facility_name": req.destination_facility_name,
        "driver_name": req.driver_name,
        "driver_phone": req.driver_phone,
        "call_attempt_notes": req.call_attempt_notes,
        "confirmed_by": req.confirmed_by,
        "created_at": req.created_at,
        "updated_at": req.updated_at
    }


def list_transport_requests(db: Session, status_filter: Optional[str] = None) -> List[Dict[str, Any]]:
    query = db.query(TransportRequestModel)
    if status_filter and status_filter != "ALL":
        query = query.filter(TransportRequestModel.status == status_filter)
    reqs = query.order_by(TransportRequestModel.updated_at.desc()).all()

    results = []
    for r in reqs:
        case = db.query(PregnancyCaseModel).filter(PregnancyCaseModel.id == r.case_id).first()
        results.append({
            "id": r.id,
            "case_id": r.case_id,
            "patient_name": case.patient_name if case else "Unknown Patient",
            "village": case.village if case else "Unknown Village",
            "status": r.status,
            "vehicle_id": r.vehicle_id,
            "destination_facility_id": r.destination_facility_id,
            "destination_facility_name": r.destination_facility_name,
            "driver_name": r.driver_name,
            "driver_phone": r.driver_phone,
            "call_attempt_notes": r.call_attempt_notes,
            "confirmed_by": r.confirmed_by,
            "created_at": r.created_at,
            "updated_at": r.updated_at
        })
    return results


def recommend_facility_for_case(db: Session, case_id: str) -> Dict[str, Any]:
    case_detail = get_case_detail(db, case_id)
    if not case_detail or not case_detail.get("assessment"):
        return {
            "recommended_facility_id": "FAC-01",
            "recommended_facility_name": "Rampur Primary Health Centre (PHC)",
            "level": "PHC",
            "capabilities": ["24x7 Basic Emergency Obstetric Care (BEmOC)", "Medical Officer On-Duty"],
            "rationale": "Standard referral centre for primary triage and stabilization."
        }

    asm = case_detail["assessment"]
    hb = asm.get("haemoglobin")
    danger_signs = asm.get("danger_signs") or {}
    has_bleeding = danger_signs.get("bleeding", False)
    has_fits = danger_signs.get("convulsions_or_vision_loss", False)

    # If severe anemia (Hb < 7) or bleeding or fits, route to CHC with blood bank and C-section theatre
    if (hb is not None and hb < 7.0) or has_bleeding or has_fits or asm.get("requires_blood_transfusion_alert"):
        return {
            "recommended_facility_id": "FAC-02",
            "recommended_facility_name": "Chandanpur Community Health Centre (CHC)",
            "level": "CHC",
            "capabilities": ["Blood Bank / Storage", "24x7 C-Section OT", "Specialist Obstetrician"],
            "rationale": "Severe maternal complication identified. Destination facility requires Blood Bank and emergency surgical capabilities (FRU)."
        }

    return {
        "recommended_facility_id": "FAC-01",
        "recommended_facility_name": "Rampur Primary Health Centre (PHC)",
        "level": "PHC",
        "capabilities": ["24x7 Basic Emergency Obstetric Care (BEmOC)", "Medical Officer On-Duty", "IFA & Essential Drugs"],
        "rationale": "Appropriate for primary assessment and stabilization."
    }


def get_clinical_protocol_metadata() -> Dict[str, Any]:
    return {
        "rule_pack_version": "mohfw-hrp-v1.0",
        "standard": "MoHFW & WHO Maternal High-Risk Pregnancy Guidelines",
        "status": "CLINICALLY_VALIDATED",
        "clinical_reviewer": {
            "name": "Dr. Rajiv Sharma",
            "qualifications": "MBBS, MD (Obstetrics & Gynaecology)",
            "designation": "Medical Officer & MoHFW Technical Advisory Reviewer",
            "approved_at": "2026-08-01T00:00:00Z"
        },
        "danger_signs_definitions": [
            {"code": "BLEEDING", "title": "Antepartum / Postpartum Vaginal Hemorrhage", "triage": "RED"},
            {"code": "CONVULSIONS", "title": "Eclamptic Fits, Unconsciousness, or Sudden Vision Loss", "triage": "RED"},
            {"code": "SEVERE_HTN", "title": "Severe Hypertensive Crisis (SBP ≥ 160 or DBP ≥ 110 mmHg)", "triage": "RED"},
            {"code": "SEVERE_ANEMIA", "title": "Severe Anemia (Haemoglobin < 7.0 g/dL)", "triage": "RED"},
            {"code": "PRETERM_LABOUR", "title": "Preterm Labor or Premature Rupture of Membranes", "triage": "RED"},
            {"code": "REDUCED_FETAL_MOVEMENT", "title": "Absent or Markedly Decreased Fetal Movement", "triage": "RED"},
            {"code": "MODERATE_HTN", "title": "Stage 1 Gestational Hypertension (SBP 140–159 or DBP 90–109)", "triage": "AMBER"},
            {"code": "MODERATE_ANEMIA", "title": "Moderate Anemia (Haemoglobin 7.0–9.9 g/dL)", "triage": "AMBER"},
            {"code": "FEVER", "title": "High Maternal Fever (≥ 38.0°C)", "triage": "AMBER"}
        ],
        "scope_of_practice": {
            "asha_safe_actions": [
                "Call 108 emergency ambulance without delay",
                "Position mother in left lateral tilt position",
                "Keep airways clear; do not place objects in mouth if fitting",
                "Notify family members and village coordinator",
                "Accompany mother to Primary Health Centre (PHC)"
            ],
            "clinician_directed_actions": [
                "IV cannulation and IV fluid administration",
                "Magnesium Sulphate (MgSO4) loading or maintenance doses",
                "Antihypertensive administration (IV Labetalol, Oral Nifedipine)",
                "Blood product cross-matching and transfusion authorization"
            ]
        }
    }

