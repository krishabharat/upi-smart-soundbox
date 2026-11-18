import uvicorn
import os
import sys

# Ensure current directory is in python path
current_dir = os.path.dirname(os.path.abspath(__file__))
if current_dir not in sys.path:
    sys.path.insert(0, current_dir)

if __name__ == "__main__":
    print("=" * 65)
    print("  UPI SMART SOUNDBOX - BACKEND SERVICE")
    print("  Server URL: http://localhost:8000")
    print("  Interactive Gateway Simulator: http://localhost:8000/simulator")
    print("  OpenAPI Documentation: http://localhost:8000/docs")
    print("=" * 65)
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=False)

