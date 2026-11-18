# UPI Smart Soundbox – Android & Backend System

[![Python](https://img.shields.io/badge/Python-3.11%20%7C%203.12%20%7C%203.14-blue.svg)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115+-009688.svg)](https://fastapi.tiangolo.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Tests-100%25%20Passing-brightgreen.svg)]()

A complete, production-grade digital UPI Soundbox solution designed for Indian merchants. Converts an ordinary Android smartphone into an instant, reliable UPI payment soundbox with automated English voice announcements, NPCI UPI QR code generation, real-time background operation, and secure cryptographic payment verification.

---

## Highlights & Features

- **Merchant Registration**: Simple setup for Business Name, UPI ID (VPA), and Mobile Number. Saved securely on device and backed up to server.
- **Automatic UPI QR Code Generator**: Produces compliant NPCI UPI payment QR codes (`upi://pay?...`) with full-screen display, share via Android Sharesheet, and download to device gallery. Supports open-amount and pre-filled invoice amounts.
- **Cryptographic Payment Detection & Verification**:
  - Secure backend webhook processor with HMAC-SHA256 signature verification.
  - Zero fake receipts: Never announces based on customer SMS or screenshots.
  - Replay and duplicate attack protection using Bank Reference Numbers (UTR/RRN).
- **Automatic Voice Announcements**:
  - Instant announcements using Android Text-to-Speech:
    - *"Payment received, 50 rupees."*
    - *"Payment received, 100 rupees."*
    - *"Payment received, 250 rupees."*
    - *"Payment received, 1,000 rupees."*
  - Dual-layer duplicate speech guard (in-memory concurrent cache + Room SQLite check).
  - Audio focus management (automatically ducks or pauses other apps playing audio).
  - Background execution support via Android Foreground Service + WakeLock.
  - Built-in sound test button.
- **Today's Collection & Payment History**:
  - Home dashboard display of today's verified earnings.
  - Full chronological list of transactions with Bank UTR, status badge, and customer name.
- **Interactive Gateway Simulator & Web Portal**:
  - Live interactive web portal at `http://localhost:8000/simulator` to test transactions, generate webhooks, and preview soundbox audio directly in the browser!

---

## Project Structure

```
sound bocx/
├── backend/                       # Python FastAPI Backend
│   ├── app/
│   │   ├── config.py              # Configuration & Environment
│   │   ├── database.py            # Async SQLite & PostgreSQL engine
│   │   ├── models.py              # SQLAlchemy DB models (Merchant, Transaction, WebhookLog)
│   │   ├── schemas.py             # Pydantic v2 schemas
│   │   ├── security.py            # HMAC-SHA256, UPI URI generator, Speech formatter
│   │   ├── gateway/               # Gateway abstraction (Sandbox, Razorpay)
│   │   ├── routers/               # API endpoints (Merchants, Payments, Webhooks, Stream)
│   │   └── static/
│   │       └── simulator.html     # Interactive Web Simulator & Portal
│   ├── tests/                     # Pytest automated test suite
│   ├── requirements.txt           # Python dependencies
│   └── run_backend.py             # Server launcher
├── android/                       # Native Android Project
│   ├── build.gradle.kts           # Root gradle
│   ├── app/
│   │   ├── build.gradle.kts       # App dependencies (Compose, Room, Retrofit, ZXing)
│   │   └── src/main/
│   │       ├── AndroidManifest.xml# Permissions (Foreground service, WakeLock, Boot)
│   │       └── java/com/upisoundbox/app/
│   │           ├── MainActivity.kt
│   │           ├── SoundboxApplication.kt
│   │           ├── data/          # Room DB, Preferences, Retrofit, SSE stream
│   │           ├── service/       # Foreground service, TTS engine, Boot receiver
│   │           ├── ui/            # Jetpack Compose UI (Theme, Components, Screens)
│   │           └── utils/         # QR generator, UPI URI builder, Speech formatter
├── docs/                          # In-depth technical documentation
│   ├── ARCHITECTURE.md            # System architecture & sequence diagrams
│   ├── NPCI_UPI_SPECIFICATION.md  # Official UPI Deep Linking reference
│   └── GATEWAY_INTEGRATION_GUIDE.md# Razorpay / Cashfree / Bank API setup guide
└── scripts/
    └── test_soundbox_flow.py      # End-to-end integration test script
```

---

## Quickstart Guide

### 1. Backend Service & Simulator

Ensure Python 3.11+ is installed.

```powershell
# Navigate to workspace
cd "c:\Users\Dev\Downloads\sound bocx"

# Activate virtual environment
.\backend\venv\Scripts\Activate.ps1

# Run tests
pytest backend/tests -v

# Start backend server
python backend/run_backend.py
```

The server starts at **`http://localhost:8000`**.

- **Interactive Gateway Simulator**: Open **`http://localhost:8000/simulator`** in any browser.
- **OpenAPI Swagger Documentation**: Open **`http://localhost:8000/docs`**.

### 2. End-to-End Simulation Script

Run the automated simulation script while the backend is running:

```powershell
python scripts/test_soundbox_flow.py
```

This script:
1. Registers a new merchant.
2. Generates an NPCI compliant UPI QR URI.
3. Simulates a customer paying ₹100.
4. Verifies the HMAC-SHA256 signature and records the Bank UTR.
5. Verifies the voice announcement phrase: *"Payment received, 100 rupees."*
6. Simulates the device speech acknowledgment and verifies zero duplicate speech!

---

## Android Application Setup

1. Open the **`android/`** folder in Android Studio (Ladybug / Koala / Hedgehog).
2. Allow Gradle sync to complete.
3. Run on an Android emulator or physical device (Android 8.0+ / API 26+).
4. Configure the server URL in **Settings**:
   - For Android Emulator: `http://10.0.2.2:8000`
   - For Physical Device: `http://<your-computer-ip>:8000`
5. Register the store with your Business Name and UPI ID.
6. The app immediately displays your payment QR code and begins listening for payments!

---

## Verification & Security Architecture

1. **Server-Side Verification**: The Android app never possesses payment gateway secrets.
2. **HMAC-SHA256 Verification**: Every webhook payload is verified using constant-time comparison.
3. **Idempotency Guarantee**: Prevents duplicate announcements through a multi-tier guard:
   - Backend database check for duplicate `payment_id` or `bank_rrn`.
   - Android in-memory concurrent thread-safe lookup.
   - Android Room SQLite persistent check.
4. **Reliable Background Execution**: Uses an Android Foreground Service with ongoing notification and CPU WakeLock to guarantee speech finishes even when the screen is locked.

