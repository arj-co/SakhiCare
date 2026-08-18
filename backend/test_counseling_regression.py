from llm_engine import generate_family_counseling_script

def test_hindi_script_contains_emergency_number():
    res = generate_family_counseling_script("सुनीता", "रामपुर", "RED", {"bleeding": True}, "160/110", 6.8, "hi")
    script = res["counseling_script"]
    assert "108" in script
    assert "अस्पताल" in script

def test_bengali_script_generation():
    res = generate_family_counseling_script("পূজা", "রামপুর", "RED", {"headache": True}, "160/110", 7.0, "bn")
    script = res["counseling_script"]
    assert "১০৮" in script or "হাসপাতাল" in script
