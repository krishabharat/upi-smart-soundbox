# How to Run & Test Everything

This guide provides end-to-end instructions for running, testing, and verifying the entire UPI Smart Soundbox system on your local machine and on Android devices.

---

## 📋 Prerequisites

| Component | Minimum Requirement | Recommended |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS, or Ubuntu | Windows 11 / macOS Sonoma |
| **Python** | Python 3.10+ | Python 3.11 or 3.12 |
| **Android Studio** | Android Studio Hedgehog (2023.1.1)+ | Android Studio Ladybug (2024.2+) |
| **Android Device / Emulator** | Android 8.0 (API Level 26)+ | Android 11+ with Google Play Services |
| **Web Browser** | Chrome, Edge, Safari, or Firefox | Chrome / Edge (supports Web Speech API) |

---

## 🚀 1. Start the FastAPI Verification Backend

### Step 1: Open PowerShell / Terminal
Navigate to the project root directory:
```powershell
cd "c:\Users\Dev\Downloads\sound bocx"
```

### Step 2: Activate the Virtual Environment
```powershell
# Windows PowerShell
.\backend\venv\Scripts\Activate.ps1

# macOS / Linux
source backend/venv/bin/activate
```

### Step 3: Run the Backend Server
```powershell
python backend/run_backend.py
```
*Expected Output:*
```text
INFO:     Started server process [12345]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
```

### Step 4: Verify Interactive API Docs (Swagger)
Open your browser and navigate to:
[http://localhost:8000/docs](http://localhost:8000/docs)

You will see the complete documented REST and SSE endpoints:
- `POST /api/v1/merchants` (Register or get merchant profile)
- `POST /api/v1/payments/verify` (Manual/Simulated payment check)
- `POST /api/v1/webhooks/{provider}` (Payment gateway webhook)
- `POST /api/v1/bridge/forward` (Two-phone SMS/Notification bridge)
- `GET /api/v1/soundbox/stream/{merchant_id}` (Real-time SSE event stream)

---

## 💻 2. Test with the Interactive Web Simulator

We built a full-screen, responsive web simulator that runs in any browser and behaves exactly like the native Android app.

1. Open your browser to:
   [http://localhost:8000](http://localhost:8000)
2. **Tab 1: My QR**:
   - Enter your Business Name (e.g., `Sharma Kirana Store`).
   - Enter your UPI ID (e.g., `sharma@okhdfcbank`).
   - Click **Generate My QR**.
   - Your official NPCI UPI QR code will appear inside the merchant standee.
3. **Tab 2: Received History**:
   - Switch to **Received History**.
   - Notice **Today's Total Received: ₹0.00**.
4. **Trigger a Live Payment**:
   - In the "Simulate Payment" panel at the bottom:
     - Enter Amount: `250`
     - Enter Payer: `Rahul Kumar`
     - Click **Send Verified Payment**.
   - **Listen!** You will hear:
     1. The dual-tone fintech chime (*Ding-Dong!*).
     2. The speech engine clearly announcing:
        `"Payment received, 250 rupees. Thank you!"`
   - **Check History**:
     - Today's Total Received automatically jumps to **₹250.00**.
     - A green-badged transaction card appears with UTR: `UPI/432198765432`.

---

## 🤖 3. Build & Run the Android App in Android Studio

1. Launch **Android Studio**.
2. Select **Open** and choose the `android/` directory inside this project folder:
   `c:\Users\Dev\Downloads\sound bocx\android`
3. Wait for Gradle sync to complete (*approx. 1–2 minutes on first run*).
4. Connect an Android phone via USB with **USB Debugging** enabled (or start an Android Virtual Device emulator).
5. Click **Run 'app'** (`Shift + F10`).
6. **Grant Notification Access**:
   - When the app launches, tap **Enable Voice Alerts / Notification Access**.
   - Enable **UPI Smart Soundbox** in Android's Special App Access settings.
7. Test the app by scanning the QR standee or triggering simulated payments!

---

## 🧪 4. Run Automated Pytest Test Suite

To verify backend data integrity, deduplication logic, and security signatures, run the Pytest suite:

```powershell
.\backend\venv\Scripts\python.exe -m pytest backend/tests -v
```

### Expected Output:
```text
backend/tests/test_backend.py::test_health_check PASSED                    [ 14%]
backend/tests/test_backend.py::test_merchant_registration PASSED           [ 28%]
backend/tests/test_backend.py::test_record_payment PASSED                  [ 42%]
backend/tests/test_backend.py::test_duplicate_payment_prevention PASSED    [ 57%]
backend/tests/test_backend.py::test_today_total_calculation PASSED         [ 71%]
backend/tests/test_backend.py::test_bridge_forward_payment PASSED          [ 85%]
backend/tests/test_backend.py::test_webhook_signature_verification PASSED  [100%]

============================== 7 passed in 0.82s ==============================
```

---

## 🌉 5. Test the Two-Phone Cloud Bridge via Terminal

Want to simulate Mom's phone forwarding a real bank SMS to Dad's soundbox?

Run this PowerShell command in a new terminal:
```powershell
$body = @{
    merchant_id = "default-merchant"
    amount = 500.0
    bank_ref = "UPI432198000999"
    payer_name = "Pooja Verma"
    raw_message = "Your a/c XXX credited with Rs. 500.00 on 18-Nov-26 by UPI Ref UPI432198000999"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8000/api/v1/bridge/forward" -Method Post -ContentType "application/json" -Body $body
```

### Result:
- The server responds: `{"status": "SUCCESS", "amount": 500.0, "message": "Payment verified and broadcasted"}`.
- If your browser simulator or Android phone is open, it immediately announces:
  `"Payment received, 500 rupees. Thank you!"` in less than **300 milliseconds**!

---

### Next Step:
For deep engineering analysis and code architecture, read **[Key Technologies & Code Walkthrough](06_KEY_TECHNOLOGIES_AND_CODE_WALKTHROUGH.md)**!
