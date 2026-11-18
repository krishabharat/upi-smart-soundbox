# 📚 UPI Smart Soundbox: Complete Learning Series

Welcome to the official learning series for the **UPI Smart Soundbox**! This course takes you step-by-step through the design, architecture, frontend development, backend engineering, audio synthesis, and real-time bank integration of the project.

---

## 🖼️ Visual Architecture Diagrams

All visual diagrams are located in [`learning/images/`](images/):
1. **[01_stage_by_stage_roadmap.png](images/01_stage_by_stage_roadmap.png)** - 4-Stage engineering roadmap.
2. **[02_system_architecture.png](images/02_system_architecture.png)** - Complete customer-to-speaker architectural flow.
3. **[03_two_phone_dad_solution.png](images/03_two_phone_dad_solution.png)** - Dad's shop vs Mom's bank SIM solution.
4. **[04_app_screens_and_ui_flow.png](images/04_app_screens_and_ui_flow.png)** - The minimalist 2-tab Android UI.
5. **[05_payment_verification_lifecycle.png](images/05_payment_verification_lifecycle.png)** - Anti-fraud deduplication & audio lifecycle.

---

## 📖 Chapter Index

- **[00. Overview & Learning Roadmap](00_OVERVIEW_AND_LEARNING_GUIDE.md)**
  - Why physical soundbox rentals are inefficient.
  - Core problem statement and project mission.
  - System architecture overview.

- **[01. Stage 1: Core UI & QR Engine](01_STAGE_1_CORE_UI_AND_QR_ENGINE.md)**
  - Why strictly 2 tabs (*My QR* and *Received History*).
  - NPCI UPI URI specification (`upi://pay?pa=...`).
  - Jetpack DataStore persistent local merchant profile.
  - Sharing and downloading high-contrast QR standees.

- **[02. Stage 2: Payment Verification & Backend Engine](02_STAGE_2_PAYMENT_VERIFICATION_AND_BACKEND.md)**
  - The critical truth: why static QR codes do not ping phones.
  - FastAPI async architecture with SQLite / PostgreSQL.
  - 12-digit UTR idempotency check & deduplication logic.
  - Anti-fraud rules to prevent fake payment scams.

- **[03. Stage 3: Voice Announcement & Audio Engine](03_STAGE_3_VOICE_ANNOUNCEMENT_AND_AUDIO_ENGINE.md)**
  - The psychology of Indian fintech soundboxes.
  - Dual-tone chime synthesis ($659.25\text{ Hz} \rightarrow 880\text{ Hz}$).
  - Android Text-to-Speech (TTS) configuration.
  - Audio focus ducking and duplicate audio prevention locks.

- **[04. Stage 4: The Two-Phone Problem & Background Sync](04_STAGE_4_THE_TWO_PHONE_PROBLEM_AND_BACKGROUND_SYNC.md)**
  - Solving the shopkeeper dilemma (Dad's shop phone vs Mom's bank SIM).
  - Solution A: Google Pay / PhonePe Business Staff / Cashier mode.
  - Solution B: `NotificationListenerService` + Cloud Bridge API (`POST /api/v1/bridge/forward`).
  - Server-Sent Events (SSE) streaming vs WebSockets.
  - Android `ForegroundService` with `WakeLock` for 24/7 background operation.

- **[05. How to Run & Test Everything](05_HOW_TO_RUN_AND_TEST_EVERYTHING.md)**
  - Running the FastAPI backend on `http://localhost:8000`.
  - Testing with the interactive Web Simulator.
  - Building and running the Android APK in Android Studio.
  - Running the automated Pytest test suite.
  - Simulating bank payments via PowerShell / curl.

- **[06. Key Technologies & Code Walkthrough](06_KEY_TECHNOLOGIES_AND_CODE_WALKTHROUGH.md)**
  - Comprehensive technology breakdown across Kotlin, Compose, FastAPI, and Web APIs.
  - Line-by-line review of core files and design patterns.
