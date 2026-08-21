import time
from typing import Any, Optional, Dict

class SimpleCache:
    def __init__(self, ttl_seconds: int = 300):
        self.ttl = ttl_seconds
        self.cache: Dict[str, tuple] = {}

    def get(self, key: str) -> Optional[Any]:
        if key not in self.cache: return None
        val, ts = self.cache[key]
        if time.time() - ts > self.ttl:
            del self.cache[key]
            return None
        return val

    def set(self, key: str, value: Any):
        self.cache[key] = (value, time.time())
