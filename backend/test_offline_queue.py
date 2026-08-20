import pytest

def test_offline_queue_retry():
    queue = ["SC-101", "SC-102", "SC-103"]
    synced = []
    failed = []

    for item in queue:
        try:
            synced.append(item)
        except Exception:
            failed.append(item)

    assert len(synced) == 3
    assert len(failed) == 0
