import time
from sqlalchemy import Column, String, Integer, Float, Boolean, Text, BigInteger, ForeignKey, Index
from sqlalchemy.orm import relationship
from database import Base


class FacilityModel(Base):
    __tablename__ = "facilities"

    id = Column(String(64), primary_key=True, index=True)
    name = Column(String(128), nullable=False)
    type = Column(String(32), default="PHC")  # PHC, CHC, DH
    catchment_area = Column(String(128), nullable=True)
    contact_phone = Column(String(32), nullable=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)
    capabilities_json = Column(Text, default="[]")
    created_at = Column(BigInteger, default=lambda: int(time.time()))

    cases = relationship("PregnancyCaseModel", back_populates="facility")
    workers = relationship("WorkerModel", back_populates="facility")


class WorkerModel(Base):
    __tablename__ = "workers"

    id = Column(String(64), primary_key=True, index=True)
    name = Column(String(128), nullable=False)
    role = Column(String(32), default="ASHA")
    phone = Column(String(32), nullable=False)
    facility_id = Column(String(64), ForeignKey("facilities.id"), nullable=False, index=True)
    locale = Column(String(16), default="hi-IN")
    status = Column(String(32), default="ACTIVE")
    pin_hash = Column(String(128), nullable=True)
    last_seen_at = Column(BigInteger, default=lambda: int(time.time()))

    facility = relationship("FacilityModel", back_populates="workers")
    cases = relationship("PregnancyCaseModel", back_populates="worker")


class UserModel(Base):
    __tablename__ = "users"

    id = Column(String(64), primary_key=True, index=True)
    username = Column(String(64), unique=True, index=True, nullable=False)
    email = Column(String(128), unique=True, index=True, nullable=False)
    password_hash = Column(String(128), nullable=False)
    full_name = Column(String(128), nullable=False)
    role = Column(String(32), default="MEDICAL_OFFICER")  # MEDICAL_OFFICER, SUPERVISOR, DISPATCHER, ADMIN
    facility_id = Column(String(64), ForeignKey("facilities.id"), nullable=True, index=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(BigInteger, default=lambda: int(time.time()))


class PregnancyCaseModel(Base):
    __tablename__ = "pregnancy_cases"

    id = Column(String(64), primary_key=True, index=True)
    local_id = Column(String(64), index=True, nullable=False)
    patient_name = Column(String(128), nullable=False, index=True)
    village = Column(String(128), nullable=False, index=True)
    age_years = Column(Integer, nullable=True)
    gestational_age_weeks = Column(Integer, nullable=True)
    gravida = Column(Integer, nullable=True)
    para = Column(Integer, nullable=True)
    travel_constraints = Column(String(256), nullable=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)
    location_accuracy_m = Column(Float, nullable=True)
    location_captured_at = Column(BigInteger, nullable=True)
    worker_id = Column(String(64), ForeignKey("workers.id"), nullable=True, index=True)
    facility_id = Column(String(64), ForeignKey("facilities.id"), nullable=True, index=True)
    sync_status = Column(String(32), default="ACKNOWLEDGED", index=True)
    doctor_advisory = Column(Text, nullable=True)
    ambulance_status = Column(String(64), nullable=True)
    is_demo = Column(Boolean, default=False, index=True)
    created_at = Column(BigInteger, default=lambda: int(time.time()), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time()))

    facility = relationship("FacilityModel", back_populates="cases")
    worker = relationship("WorkerModel", back_populates="cases")
    assessments = relationship("AssessmentModel", back_populates="case", cascade="all, delete-orphan")
    voice_artifacts = relationship("VoiceArtifactModel", back_populates="case", cascade="all, delete-orphan")
    events = relationship("CaseEventModel", back_populates="case", cascade="all, delete-orphan")


class AssessmentModel(Base):
    __tablename__ = "assessments"

    id = Column(String(64), primary_key=True, index=True)
    case_id = Column(String(64), ForeignKey("pregnancy_cases.id", ondelete="CASCADE"), nullable=False, index=True)
    rule_pack_version = Column(String(32), default="mohfw-hrp-v1.0")
    risk_level = Column(String(16), nullable=False, index=True)  # RED, AMBER, GREEN
    risk_score = Column(Integer, default=10)
    primary_factors_json = Column(Text, default="[]")
    unmeasured_vitals_json = Column(Text, default="[]")
    clinical_rationale = Column(Text, nullable=True)
    recommended_protocol = Column(Text, nullable=True)
    asha_safe_actions_json = Column(Text, default="[]")
    clinician_directed_actions_json = Column(Text, default="[]")
    requires_immediate_ambulance = Column(Boolean, default=False)
    requires_blood_transfusion_alert = Column(Boolean, default=False)
    blood_pressure = Column(String(32), nullable=True)
    haemoglobin = Column(Float, nullable=True)
    danger_signs_json = Column(Text, default="{}")
    created_at = Column(BigInteger, default=lambda: int(time.time()))

    case = relationship("PregnancyCaseModel", back_populates="assessments")


class VoiceArtifactModel(Base):
    __tablename__ = "voice_artifacts"

    id = Column(String(64), primary_key=True, index=True)
    case_id = Column(String(64), ForeignKey("pregnancy_cases.id", ondelete="CASCADE"), nullable=False, index=True)
    # storage_path is a private Supabase Storage object path in production.
    # file_path remains a local-development compatibility field only.
    storage_path = Column(String(256), nullable=True)
    file_path = Column(String(256), nullable=True)
    filename = Column(String(128), nullable=False)
    mime_type = Column(String(64), default="audio/m4a")
    file_size_bytes = Column(BigInteger, default=0)
    sha256 = Column(String(64), nullable=False, index=True)
    language = Column(String(16), default="hi-IN")
    duration_seconds = Column(Integer, default=0)
    transcript = Column(Text, nullable=True)
    processing_status = Column(String(32), default="UNPROCESSED")
    upload_status = Column(String(32), default="UPLOADED")
    retention_due_at = Column(BigInteger, default=lambda: int(time.time() + 90 * 86400))
    uploaded_at = Column(BigInteger, default=lambda: int(time.time()))

    case = relationship("PregnancyCaseModel", back_populates="voice_artifacts")


class CaseEventModel(Base):
    __tablename__ = "case_events"

    id = Column(String(64), primary_key=True, index=True)
    case_id = Column(String(64), ForeignKey("pregnancy_cases.id", ondelete="CASCADE"), nullable=False, index=True)
    event_type = Column(String(64), nullable=False, index=True)  # CASE_CREATED, ADVISORY_SENT, TRANSPORT_REQUESTED, etc.
    actor_id = Column(String(64), nullable=False)
    actor_role = Column(String(32), default="SYSTEM")
    summary = Column(String(256), nullable=False)
    details_json = Column(Text, nullable=True)
    occurred_at = Column(BigInteger, default=lambda: int(time.time()), index=True)

    case = relationship("PregnancyCaseModel", back_populates="events")


class OutboxItemModel(Base):
    __tablename__ = "outbox_items"

    id = Column(String(64), primary_key=True, index=True)  # Idempotency key
    case_id = Column(String(64), nullable=False, index=True)
    entity_type = Column(String(32), default="CASE_ASSESSMENT")
    payload_json = Column(Text, nullable=False)
    attempts = Column(Integer, default=0)
    status = Column(String(32), default="ACKNOWLEDGED")
    last_error = Column(Text, nullable=True)
    created_at = Column(BigInteger, default=lambda: int(time.time()))
    updated_at = Column(BigInteger, default=lambda: int(time.time()))


class TransportRequestModel(Base):
    __tablename__ = "transport_requests"

    id = Column(String(64), primary_key=True, index=True)
    case_id = Column(String(64), ForeignKey("pregnancy_cases.id", ondelete="CASCADE"), nullable=False, index=True)
    status = Column(String(32), default="REQUESTED", index=True)  # REQUESTED, CALL_ATTEMPTED, CONFIRMED, EN_ROUTE, ARRIVED, FAILED
    vehicle_id = Column(String(64), nullable=True)
    destination_facility_id = Column(String(64), ForeignKey("facilities.id"), nullable=True)
    destination_facility_name = Column(String(128), nullable=True)
    driver_name = Column(String(128), nullable=True)
    driver_phone = Column(String(32), nullable=True)
    call_attempt_notes = Column(Text, nullable=True)
    confirmed_by = Column(String(64), nullable=True)
    created_at = Column(BigInteger, default=lambda: int(time.time()), index=True)
    updated_at = Column(BigInteger, default=lambda: int(time.time()))

    case = relationship("PregnancyCaseModel")


class NotificationLogModel(Base):
    __tablename__ = "notification_logs"

    id = Column(String(64), primary_key=True, index=True)
    case_id = Column(String(64), nullable=True, index=True)
    channel = Column(String(32), default="PUSH", index=True)  # PUSH, SMS, VOICE_CALL
    recipient = Column(String(128), nullable=False)
    template_type = Column(String(64), default="EMERGENCY_TRIAGE_ALERT")
    content_preview = Column(String(512), nullable=False)
    status = Column(String(32), default="SENT", index=True)  # QUEUED, SENT, DELIVERED, FAILED, UNKNOWN, NOT_CONFIGURED
    provider_ref = Column(String(128), nullable=True)
    error_message = Column(Text, nullable=True)
    escalated_to = Column(String(64), nullable=True)
    created_at = Column(BigInteger, default=lambda: int(time.time()), index=True)
    delivered_at = Column(BigInteger, nullable=True)
