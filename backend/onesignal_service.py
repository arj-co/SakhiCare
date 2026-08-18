"""
SakhiCare OneSignal REST API Engine
Handles emergency triage push notifications, clinical advisories, device registration, and broadcast alerts.
Targeting OneSignal REST API v9 / v1: https://onesignal.com/api/v1/notifications
"""

import os
import json
import logging
from typing import Dict, Any, List, Optional
from datetime import datetime, timezone
import httpx

logger = logging.getLogger("sakhicare.onesignal")
logging.basicConfig(level=logging.INFO)

# Configuration from environment variables
ONESIGNAL_APP_ID = os.getenv("ONESIGNAL_APP_ID", "sakhicare-app-id-placeholder")
ONESIGNAL_REST_API_KEY = os.getenv("ONESIGNAL_REST_API_KEY", "")
ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications"

# In-memory registry of registered device player IDs & channels
registered_devices: Dict[str, Dict[str, Any]] = {}
notification_history: List[Dict[str, Any]] = []


async def send_emergency_triage_notification(
    patient_id: str,
    patient_name: str,
    village: str,
    blood_pressure: str,
    danger_signs: Dict[str, bool],
    risk_level: str = "RED",
    app_id: Optional[str] = None,
    api_key: Optional[str] = None
) -> Dict[str, Any]:
    """
    Dispatches a high-priority emergency push notification to Care Desk operators and on-call medical staff
    when a RED risk maternal case is synchronized.
    """
    effective_app_id = app_id or ONESIGNAL_APP_ID
    effective_api_key = api_key or ONESIGNAL_REST_API_KEY

    # Build active danger signs summary
    active_signs = [k.replace('_', ' ').title() for k, v in danger_signs.items() if v]
    signs_str = ", ".join(active_signs) if active_signs else "Severe High BP (>=140/90)"

    title = f"🚨 EMERGENCY: High-Risk Case ({patient_id})"
    body = f"Patient {patient_name} in {village} | BP: {blood_pressure} | Signs: {signs_str}. Immediate triage required!"

    payload = {
        "app_id": effective_app_id,
        "included_segments": ["Care Desk Staff", "All", "Active Users"],
        "headings": {"en": title, "hi": f"🚨 आपातकालीन अलर्ट: उच्च जोखिम ({patient_id})"},
        "contents": {"en": body, "hi": f"मरीज {patient_name}, गांव {village} | बीपी: {blood_pressure}। तुरंत सहायता आवश्यक है।"},
        "data": {
            "type": "EMERGENCY_TRIAGE",
            "patient_id": patient_id,
            "patient_name": patient_name,
            "village": village,
            "blood_pressure": blood_pressure,
            "danger_signs": danger_signs,
            "risk_level": risk_level,
            "timestamp": datetime.now(timezone.utc).isoformat()
        },
        "android_channel_id": "sakhicare_emergency",
        "priority": 10,
        "android_sound": "emergency_alert",
        "small_icon": "ic_launcher"
    }

    return await _dispatch_onesignal_request(payload, effective_api_key, category="EMERGENCY_TRIAGE")


async def send_clinical_advisory_notification(
    patient_id: str,
    patient_name: str,
    advisory_text: str,
    doctor_or_operator_name: str = "Care Desk Specialist",
    target_player_id: Optional[str] = None,
    app_id: Optional[str] = None,
    api_key: Optional[str] = None
) -> Dict[str, Any]:
    """
    Sends targeted clinical guidance / pre-hospital instructions from the Care Desk back to the ASHA worker's device.
    """
    effective_app_id = app_id or ONESIGNAL_APP_ID
    effective_api_key = api_key or ONESIGNAL_REST_API_KEY

    title = f"📋 Care Desk Advisory: Case {patient_id}"
    body = f"{doctor_or_operator_name}: \"{advisory_text}\""

    payload = {
        "app_id": effective_app_id,
        "headings": {"en": title, "hi": f"📋 केयर डेस्क निर्देश: केस {patient_id}"},
        "contents": {"en": body, "hi": f"{doctor_or_operator_name}: \"{advisory_text}\""},
        "data": {
            "type": "CARE_DESK_ADVISORY",
            "patient_id": patient_id,
            "patient_name": patient_name,
            "advisory_text": advisory_text,
            "sender": doctor_or_operator_name,
            "timestamp": datetime.now(timezone.utc).isoformat()
        },
        "android_channel_id": "sakhicare_advisories",
        "priority": 10
    }

    if target_player_id:
        payload["include_player_ids"] = [target_player_id]
    else:
        payload["included_segments"] = ["ASHA Workers", "All"]

    return await _dispatch_onesignal_request(payload, effective_api_key, category="CARE_DESK_ADVISORY")


async def send_custom_broadcast(
    title: str,
    message: str,
    segment: str = "All",
    extra_data: Optional[Dict[str, Any]] = None,
    app_id: Optional[str] = None,
    api_key: Optional[str] = None
) -> Dict[str, Any]:
    """
    Sends a general broadcast notification to a specific group or all frontline workers.
    """
    effective_app_id = app_id or ONESIGNAL_APP_ID
    effective_api_key = api_key or ONESIGNAL_REST_API_KEY

    payload = {
        "app_id": effective_app_id,
        "included_segments": [segment],
        "headings": {"en": title},
        "contents": {"en": message},
        "data": extra_data or {"type": "BROADCAST", "timestamp": datetime.now(timezone.utc).isoformat()}
    }

    return await _dispatch_onesignal_request(payload, effective_api_key, category="BROADCAST")


async def _dispatch_onesignal_request(payload: Dict[str, Any], api_key: str, category: str) -> Dict[str, Any]:
    """
    Executes the OneSignal REST API call or falls back to simulation mode if API key is not configured.
    """
    timestamp = datetime.now(timezone.utc).isoformat()
    
    # If API key is not set or placeholder, simulate delivery seamlessly
    if not api_key or "placeholder" in api_key.lower() or not api_key.strip():
        simulated_id = f"sim-os-{int(datetime.now().timestamp() * 1000)}"
        record = {
            "status": "success",
            "mode": "simulated",
            "notification_id": simulated_id,
            "category": category,
            "recipients": 1,
            "title": payload.get("headings", {}).get("en", "Notification"),
            "body": payload.get("contents", {}).get("en", ""),
            "payload": payload,
            "timestamp": timestamp
        }
        notification_history.append(record)
        logger.info(f"⚡ [OneSignal SIMULATOR] Dispatched {category} notification: {payload.get('headings', {}).get('en')} -> Recipients: Simulated active subscribers")
        return {
            "id": simulated_id,
            "recipients": 1,
            "delivery": "simulated_success",
            "message": "OneSignal API notification simulated successfully (Configure ONESIGNAL_REST_API_KEY in .env for live cloud push)",
            "details": record
        }

    # Execute real HTTP request to OneSignal API
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            headers = {
                "Authorization": f"Basic {api_key}",
                "Content-Type": "application/json; charset=utf-8"
            }
            response = await client.post(ONESIGNAL_API_URL, json=payload, headers=headers)
            
            if response.status_code in (200, 201):
                data = response.json()
                record = {
                    "status": "success",
                    "mode": "live_api",
                    "notification_id": data.get("id"),
                    "category": category,
                    "recipients": data.get("recipients", 0),
                    "title": payload.get("headings", {}).get("en", ""),
                    "body": payload.get("contents", {}).get("en", ""),
                    "timestamp": timestamp
                }
                notification_history.append(record)
                logger.info(f"✅ [OneSignal LIVE] Sent {category} notification id={data.get('id')} to {data.get('recipients')} recipients")
                return {
                    "id": data.get("id"),
                    "recipients": data.get("recipients", 0),
                    "delivery": "live_onesignal_api",
                    "response": data
                }
            else:
                logger.warning(f"⚠️ [OneSignal API Warning] Status {response.status_code}: {response.text}")
                return {
                    "id": f"err-os-{int(datetime.now().timestamp())}",
                    "recipients": 0,
                    "delivery": "api_error",
                    "status_code": response.status_code,
                    "error": response.text
                }
    except Exception as e:
        logger.error(f"❌ [OneSignal Error] Failed to reach OneSignal API: {e}")
        return {
            "id": f"err-os-{int(datetime.now().timestamp())}",
            "recipients": 0,
            "delivery": "network_exception",
            "error": str(e)
        }


def register_device(device_id: str, role: str = "ASHA", player_id: Optional[str] = None, user_name: Optional[str] = None) -> Dict[str, Any]:
    """
    Registers a frontline worker or Care Desk operator device in the system.
    """
    record = {
        "device_id": device_id,
        "player_id": player_id or f"player_{device_id}",
        "role": role,
        "user_name": user_name or f"User-{device_id[:6]}",
        "registered_at": datetime.now(timezone.utc).isoformat(),
        "status": "active"
    }
    registered_devices[device_id] = record
    return record
