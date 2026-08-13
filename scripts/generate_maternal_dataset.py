import random
import json

def generate_synthetic_dataset(n=100):
    villages = ["Rampur", "Sitapur", "Bhimpur", "Gopalpur", "Kalyanpur", "Chandpur"]
    records = []
    for i in range(1, n + 1):
        systolic = random.randint(90, 175)
        diastolic = random.randint(55, 115)
        hb = round(random.uniform(5.5, 13.5), 1)
        records.append({
            "id": f"SC-SYN-{i:04d}",
            "name": f"Patient {i}",
            "village": random.choice(villages),
            "bp": f"{systolic}/{diastolic}",
            "hb": hb,
            "danger_signs": {
                "bleeding": random.random() < 0.1,
                "fever": random.random() < 0.15,
                "headache": random.random() < 0.2,
                "fetal_distress": random.random() < 0.08
            }
        })
    return records

if __name__ == "__main__":
    data = generate_synthetic_dataset(50)
    print(f"Generated {len(data)} synthetic maternal records.")
