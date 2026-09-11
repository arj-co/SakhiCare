"""
SakhiCare Notification & Escalation Engine (Phase 4)
Handles multi-channel notifications (Push, SMS, Voice Call) behind a unified interface with:
- Minimal privacy-preserving SMS content (no patient full names in SMS)
- Truthful delivery status: QUEUED, SENT, DELIVERED, FAILED, NOT_CONFIGURED
- Automated escalation chain for unacknowledged RED emergency cases
- Append-only audit logging in NotificationLogModel
"""

import os
import time
import json
import uuid
import logging
import httpx
from typing import Dict, Any, List, Optional
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from models import NotificationLogModel, CaseEventModel, PregnancyCaseModel

logger = logging.getLogger("sakhicare.notifications")

# Configuration from environment variables
SMS_GATEWAY_URL = os.getenv("SMS_GATEWAY_URL", "")
SMS_GATEWAY_API_KEY = os.getenv("SMS_GATEWAY_API_KEY", "")
SMS_SENDER_ID = os.getenv("SMS_SENDER_ID", "SAKHI")
APP_ENV = os.getenv("APP_ENV", "development").lower()
TEST_MODE = os.getenv("SAKHICARE_TEST_MODE", "false").lower() == "true"
DEFAULT_EMERGENCY_CALLBACK = os.getenv("DEFAULT_EMERGENCY_CALLBACK", "+919876543299" if TEST_MODE else "")
EMERGENCY_MO_PHONE = os.getenv("EMERGENCY_MO_PHONE", "+919876543201" if TEST_MODE else "")
SUPERVISOR_PHONE = os.getenv("SUPERVISOR_PHONE", "+919876543202" if TEST_MODE else "")
EMERGENCY_MO_PUSH_TARGET = os.getenv("EMERGENCY_MO_PUSH_TARGET", "MO_SHARMA" if TEST_MODE else "")
SUPERVISOR_PUSH_TARGET = os.getenv("SUPERVISOR_PUSH_TARGET", "SUPERVISOR_ANITA" if TEST_MODE else "")


def format_minimal_sms(
    case_id: str,
    urgency: str,
    village: str,
    danger_signs: List[str],
    callback_phone: Optional[str] = None
) -> str:
    """
    Constructs privacy-preserving minimal SMS template.
    Strictly avoids exposing patient full names or detailed personal data over plain SMS.
    Format: [SakhiCare Alert] Case <ID> | URGENT <RISK> | Village: <Village> | Signs: <Signs> | Callback: <Phone>
    """
    signs_summary = ", ".join(danger_signs[:3]) if danger_signs else "Severe High BP"
    callback = callback_phone or DEFAULT_EMERGENCY_CALLBACK or "configured care desk"
    return f"[SakhiCare] Case {case_id} | URGENT {urgency} | Area: {village} | Signs: {signs_summary} | Call: {callback}"


def send_notification(
    db: Session,
    case_id: str,
    channel: str,  # PUSH, SMS, VOICE_CALL
    recipient: str,
    template_type: str,
    content_text: str,
    actor_id: str = "SYSTEM_ESCALATION"
) -> Dict[str, Any]:
    """
    Dispatches notification via designated adapter and records truthful delivery state.
    Never fabricates DELIVERED if credentials are missing or unconfirmed.
    """
    notification_id = f"notif_{uuid.uuid4().hex[:12]}"
    created_at = int(time.time())

    status = "NOT_CONFIGURED"
    provider_ref = None
    error_message = None

    if channel == "SMS":
        if SMS_GATEWAY_API_KEY and SMS_GATEWAY_URL:
            try:
                response = httpx.post(
                    SMS_GATEWAY_URL,
                    headers={"Authorization": f"Bearer {SMS_GATEWAY_API_KEY}", "Content-Type": "application/json"},
                    json={"to": recipient, "sender_id": SMS_SENDER_ID, "message": content_text},
                    timeout=10.0,
                )
                if 200 <= response.status_code < 300:
                    body = response.json() if response.content else {}
                    status = "SENT"
                    provider_ref = str(body.get("id") or body.get("message_id") or body.get("sid") or "") or None
                    if not provider_ref:
                        status = "UNKNOWN"
                        error_message = "SMS provider accepted request without a message identifier"
                else:
                    status = "FAILED"
                    error_message = f"SMS provider HTTP {response.status_code}: {response.text[:300]}"
            except Exception as exc:
                status = "FAILED"
                error_message = f"SMS provider request failed: {exc}"
        else:
            # Explicitly mark as NOT_CONFIGURED without faking success
            status = "NOT_CONFIGURED"
            error_message = "SMS gateway credentials (SMS_GATEWAY_API_KEY) not configured in environment"

    elif channel == "PUSH":
        onesignal_key = os.getenv("ONESIGNAL_REST_API_KEY", "")
        if onesignal_key and "placeholder" not in onesignal_key.lower():
            app_id = os.getenv("ONESIGNAL_APP_ID", "")
            if not app_id:
                status = "NOT_CONFIGURED"
                error_message = "ONESIGNAL_APP_ID is missing"
            else:
                try:
                    response = httpx.post(
                        "https://onesignal.com/api/v1/notifications",
                        headers={"Authorization": f"Basic {onesignal_key}", "Content-Type": "application/json"},
                        json={
                            "app_id": app_id,
                            "included_segments": ["Care Desk Staff"],
                            "headings": {"en": template_type},
                            "contents": {"en": content_text},
                            "data": {"case_id": case_id, "type": template_type},
                        },
                        timeout=10.0,
                    )
                    body = response.json() if response.content else {}
                    if 200 <= response.status_code < 300 and body.get("id"):
                        status = "SENT"
                        provider_ref = str(body["id"])
                    else:
                        status = "FAILED"
                        error_message = f"OneSignal HTTP {response.status_code}: {response.text[:300]}"
                except Exception as exc:
                    status = "FAILED"
                    error_message = f"OneSignal request failed: {exc}"
        else:
            status = "NOT_CONFIGURED"
            error_message = "OneSignal REST API key not configured; no push was sent"

    elif channel == "VOICE_CALL":
        status = "NOT_CONFIGURED"
        error_message = "Automated voice-call IVR adapter not configured"

    # Record notification in durable database
    log_entry = NotificationLogModel(
        id=notification_id,
        case_id=case_id,
        channel=channel,
        recipient=recipient,
        template_type=template_type,
        content_preview=content_text[:500],
        status=status,
        provider_ref=provider_ref,
        error_message=error_message,
        created_at=created_at,
        delivered_at=created_at if status == "DELIVERED" else None
    )
    db.add(log_entry)

    # Record case event
    evt = CaseEventModel(
        id=f"EVT-{int(time.time() * 1000)}-{uuid.uuid4().hex[:6]}",
        case_id=case_id,
        event_type="NOTIFICATION_DISPATCHED",
        actor_id=actor_id,
        actor_role="SYSTEM",
        summary=f"{channel} notification ({template_type}) status: {status}",
        details_json=json.dumps({
            "notification_id": notification_id,
            "channel": channel,
            "status": status,
            "recipient": recipient,
            "content": content_text
        }),
        occurred_at=created_at
    )
    db.add(evt)
    db.commit()

    return {
        "notification_id": notification_id,
        "case_id": case_id,
        "channel": channel,
        "recipient": recipient,
        "status": status,
        "provider_ref": provider_ref,
        "content_preview": content_text,
        "error_message": error_message,
        "created_at": created_at
    }


def trigger_emergency_escalation(
    db: Session,
    case_id: str,
    patient_name: str,
    village: str,
    urgency: str,
    danger_signs: List[str]
) -> Dict[str, Any]:
    """
    Executes Phase 4 escalation policy:
    1. Primary: Push notification to the configured on-duty Medical Officer
    2. Minimal SMS alert to PHC Medical Officer callback
    3. If primary channel is NOT_CONFIGURED or fails, visibly escalates to the configured supervisor
    """
    results = []

    # 1. Primary Push Alert
    push_msg = f"EMERGENCY: {urgency} Risk case in {village}. Immediate clinical review required."
    push_res = send_notification(
        db=db,
        case_id=case_id,
        channel="PUSH",
        recipient=EMERGENCY_MO_PUSH_TARGET or "UNCONFIGURED_MO_PUSH_TARGET",
        template_type="EMERGENCY_TRIAGE_ALERT",
        content_text=push_msg,
        actor_id="TRIAGE_ENGINE"
    )
    results.append(push_res)

    # 2. Minimal Privacy-Safe SMS
    sms_text = format_minimal_sms(
        case_id=case_id,
        urgency=urgency,
        village=village,
        danger_signs=danger_signs
    )
    sms_res = send_notification(
        db=db,
        case_id=case_id,
        channel="SMS",
        recipient=EMERGENCY_MO_PHONE or "UNCONFIGURED_MO_RECIPIENT",
        template_type="MINIMAL_EMERGENCY_SMS",
        content_text=sms_text,
        actor_id="TRIAGE_ENGINE"
    )
    results.append(sms_res)

    # 3. Escalation Check: If Push or SMS is unconfigured or failed, record escalation to Supervisor
    escalation_needed = any(r["status"] in ("FAILED", "NOT_CONFIGURED") for r in [push_res, sms_res])
    if escalation_needed:
        esc_sms = f"[SakhiCare ESCALATION] Unconfirmed primary alert for Case {case_id} ({village}). Escalated to Block Supervisor."
        esc_res = send_notification(
            db=db,
            case_id=case_id,
            channel="SMS",
            recipient=SUPERVISOR_PHONE or "UNCONFIGURED_SUPERVISOR_RECIPIENT",
            template_type="ESCALATION_SUPERVISOR_ALERT",
            content_text=esc_sms,
            actor_id="ESCALATION_ENGINE"
        )
        esc_res["escalated_to"] = SUPERVISOR_PUSH_TARGET or SUPERVISOR_PHONE or "UNCONFIGURED_SUPERVISOR"
        results.append(esc_res)

    return {
        "case_id": case_id,
        "escalation_triggered": escalation_needed,
        "notifications": results
    }


def list_notification_logs(db: Session, case_id: Optional[str] = None, limit: int = 50) -> List[Dict[str, Any]]:
    """
    Returns auditable history of all notification attempts.
    """
    query = db.query(NotificationLogModel)
    if case_id:
        query = query.filter(NotificationLogModel.case_id == case_id)
    logs = query.order_by(NotificationLogModel.created_at.desc()).limit(limit).all()

    return [
        {
            "id": l.id,
            "case_id": l.case_id,
            "channel": l.channel,
            "recipient": l.recipient,
            "template_type": l.template_type,
            "content_preview": l.content_preview,
            "status": l.status,
            "provider_ref": l.provider_ref,
            "error_message": l.error_message,
            "escalated_to": l.escalated_to,
            "created_at": l.created_at,
            "delivered_at": l.delivered_at
        }
        for l in logs
    ]
