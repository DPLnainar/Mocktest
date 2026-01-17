
fileName = "crash_analysis_full.txt"
try:
    with open(fileName, "r", encoding="utf-8") as f:
        lines = f.readlines()
        
    for i, line in enumerate(lines):
        if "Exception" in line or "Error" in line:
            # Print context
            print(f"LINE {i}: {line.strip()}")
            for j in range(1, 15):
                if i+j < len(lines):
                    print(f"  {lines[i+j].strip()}")
            print("-" * 20)
            if i > 5000: break # Limit
except Exception as e:
    print(e)
