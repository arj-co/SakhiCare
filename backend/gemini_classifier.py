"""Small, optional Gemini second-opinion classifier for synced encounters.

Deterministic triage remains the safety authority. Gemini is recorded as an
audited secondary classification and is never allowed to downgrade a case.
"""
import json
import os
import urllib.request
import base64
from typing import Any, Dict, Optional


def classify_case_with_gemini(case: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key:
        return None

    model = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")
    prompt = f"""You are a maternal-health triage quality reviewer. Return JSON only.
Review this encounter using the supplied observations. Classify risk as exactly
RED, AMBER, or GREEN, give a 0-100 confidence, and a one-sentence rationale.
Do not invent missing observations. RED means immediate emergency referral.
This is a second opinion; deterministic clinical rules remain authoritative.

Encounter: {json.dumps(case, ensure_ascii=False)}
JSON schema: {{"risk_level":"RED|AMBER|GREEN","confidence":0,"rationale":"..."}}"""
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    payload = json.dumps({
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"responseMimeType": "application/json"},
    }).encode("utf-8")
    request = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            data = json.loads(response.read().decode("utf-8"))
        text = data["candidates"][0]["content"]["parts"][0]["text"]
        text = text.replace("```json", "").replace("```", "").strip()
        try:
            result = json.loads(text)
        except json.JSONDecodeError:
            start, end = text.find("{"), text.rfind("}")
            if start < 0 or end <= start:
                return None
            result = json.loads(text[start:end + 1])
        if result.get("risk_level") not in {"RED", "AMBER", "GREEN"}:
            return None
        return {
            "provider": "gemini",
            "model": model,
            "risk_level": result["risk_level"],
            "confidence": max(0, min(100, int(result.get("confidence", 0)))),
            "rationale": str(result.get("rationale", ""))[:1000],
        }
    except Exception:
        return None


def extract_voice_with_gemini(audio: bytes, mime_type: str = "audio/m4a", language: str = "hi-IN") -> Optional[Dict[str, Any]]:
    """Transcribe a short voice note and extract form fields in one audited call."""
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if not api_key or not audio:
        return None
    model = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")
    prompt = f'''You are a careful maternal-health intake assistant. Listen to this {language} voice note.
Return JSON only. Transcribe what is said and extract only explicitly stated fields. Do not guess.
Schema: {{"transcript":"", "patient_name":null, "village":null, "blood_pressure":null, "haemoglobin":null,
"danger_signs":{{}}, "risk_level":"RED|AMBER|GREEN|UNKNOWN", "rationale":""}}'''
    payload = json.dumps({
        "contents": [{"parts": [
            {"text": prompt},
            {"inlineData": {"mimeType": mime_type, "data": base64.b64encode(audio).decode("ascii")}}
        ]}],
        "generationConfig": {"responseMimeType": "application/json"},
    }).encode("utf-8")
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    request = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            data = json.loads(response.read().decode("utf-8"))
        text = data["candidates"][0]["content"]["parts"][0]["text"].replace("```json", "").replace("```", "").strip()
        result = json.loads(text)
        if not isinstance(result, dict):
            return None
        result["provider"], result["model"] = "gemini", model
        return result
    except Exception:
        return None
