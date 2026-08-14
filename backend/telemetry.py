from typing import Dict, Any

class SystemTelemetry:
    _total_evaluations = 0
    _total_voice_transcriptions = 0
    _total_emergency_dispatches = 0

    @classmethod
    def record_eval(cls): cls._total_evaluations += 1
    @classmethod
    def record_voice(cls): cls._total_voice_transcriptions += 1
    @classmethod
    def record_dispatch(cls): cls._total_emergency_dispatches += 1

    @classmethod
    def get_metrics(cls) -> Dict[str, Any]:
        return {
            "evaluations_total": cls._total_evaluations,
            "voice_transcriptions_total": cls._total_voice_transcriptions,
            "emergency_dispatches_total": cls._total_emergency_dispatches
        }
