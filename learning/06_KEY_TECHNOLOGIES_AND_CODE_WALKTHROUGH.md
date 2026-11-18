# Key Technologies & Code Walkthrough

This document breaks down every technology used in the UPI Smart Soundbox project and provides a comprehensive code walkthrough of key files.

![System Architecture](images/02_system_architecture.png)

---

## 🛠️ Complete Technology Stack

### 1. Android Client Stack
- **Language**: Kotlin 1.9+ (Modern, expressive, memory-safe).
- **UI Framework**: Jetpack Compose + Material Design 3 (Declarative reactive UI without XML).
- **Local Key-Value Store**: Jetpack DataStore Preferences (Asynchronous, transactional merchant settings).
- **Relational Database**: Android Room ORM + SQLite (Encrypted, ACID-compliant transaction records).
- **Audio Synthesizer & Speech**: Android `TextToSpeech` + `AudioManager` with audio ducking.
- **Background Execution**: `ForegroundService` with `WakeLock` and `NotificationListenerService`.
- **QR Generation**: Google ZXing (`com.google.zxing:core:3.5.3`).

### 2. Backend Server Stack
- **Runtime**: Python 3.10+ / 3.11 / 3.12.
- **Web Framework**: FastAPI (High-performance async ASGI web framework based on Starlette).
- **ASGI Server**: Uvicorn (Lightning-fast async HTTP/1.1 and WebSocket/SSE server).
- **ORM / Database**: SQLAlchemy 2.0 Async + SQLite (development) / PostgreSQL (production).
- **Validation**: Pydantic v2 (`BaseModel`, `SettingsConfigDict`).
- **Real-Time Streaming**: `sse-starlette` (Server-Sent Events broadcaster).
- **Testing**: Pytest + HTTPX async test client.

### 3. Web Simulator Stack
- **UI & Layout**: Responsive CSS3 Mobile Viewport with Tailwind-inspired fintech palettes.
- **Audio Synthesis**: Web Audio API (`AudioContext`, `OscillatorNode`, `GainNode`).
- **Voice Speech**: Web Speech API (`window.speechSynthesis`).
- **Live Stream**: Native HTML5 `EventSource`.

---

## 📂 Source Code Structure & Key Files

```text
├── android/
│   └── app/src/main/
│       ├── AndroidManifest.xml                         # Permissions, Services, Provider
│       └── java/com/upisoundbox/
│           ├── MainActivity.kt                         # App Entry & Navigation Host
│           ├── ui/
│           │   ├── MainSoundboxScreen.kt               # Two-Tab Scaffold & Bottom Bar
│           │   ├── SoundboxDestinations.kt             # Navigation Routes
│           │   ├── screens/
│           │   │   ├── MyQRScreen.kt                   # Tab 1: QR Standee & Form
│           │   │   └── ReceivedHistoryScreen.kt        # Tab 2: Today's Total & List
│           │   └── theme/                              # Fintech Emerald Color Scheme
│           ├── data/
│           │   ├── local/
│           │   │   ├── SoundboxDatabase.kt             # Room DB Definition
│           │   │   ├── TransactionDao.kt               # SQL Queries (Today Total, Feed)
│           │   │   └── TransactionEntity.kt            # Transaction Data Class
│           │   └── repository/
│           │       └── MerchantPreferencesRepository.kt# Jetpack DataStore
│           └── service/
│               ├── SoundboxForegroundService.kt        # 24/7 Background Service + WakeLock
│               ├── TextToSpeechManager.kt              # Dual Chime + TTS Engine
│               └── UPIPaymentNotificationListenerService.kt # Bank SMS/App Interceptor
├── backend/
│   ├── app/
│   │   ├── main.py                                     # FastAPI App, CORS, Static Mounting
│   │   ├── config.py                                   # Pydantic v2 Environment Settings
│   │   ├── database.py                                 # Async SQLAlchemy Session Engine
│   │   ├── models.py                                   # Merchant & Transaction SQL Tables
│   │   ├── schemas.py                                  # Pydantic Schemas
│   │   ├── security.py                                 # HMAC-SHA256 Signature Validator
│   │   ├── routers/
│   │   │   ├── merchants.py                            # Profile Endpoints
│   │   │   ├── payments.py                             # Verification & History Endpoints
│   │   │   ├── webhooks.py                             # Payment Gateway Webhook Receiver
│   │   │   ├── soundbox_stream.py                      # SSE Real-Time Broadcaster
│   │   │   └── bridge.py                               # Two-Phone SMS Bridge Forwarder
│   │   └── static/
│   │       └── simulator.html                          # Full-Featured Web Soundbox
│   └── tests/
│       └── test_backend.py                             # 7 Automated Pytest Verification Tests
├── docs/                                               # Technical Specifications
└── learning/                                           # Comprehensive Learning Modules & Diagrams
```

---

## 🔍 Code Walkthrough: Key Components

### 1. Tab 1: NPCI QR Generation (`MyQRScreen.kt`)
The screen formats the URI and draws the merchant standee card:
```kotlin
val upiUri = remember(businessName, upiId) {
    Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId.trim())
        .appendQueryParameter("pn", businessName.trim())
        .appendQueryParameter("cu", "INR")
        .build()
        .toString()
}
```

### 2. Deduplication & Idempotency (`payment_service.py`)
To prevent replay attacks and duplicate announcements, every payment is matched against its unique 12-digit UTR:
```python
query = select(Transaction).where(Transaction.bank_ref == bank_ref)
existing = await db.scalar(query)
if existing:
    # Duplicate detected! Do not announce or re-add
    return existing, False
```

### 3. Server-Sent Events Broadcaster (`soundbox_stream.py`)
Maintains an in-memory registry of active merchant soundboxes. When money is verified, all connected devices for that merchant receive the push packet simultaneously:
```python
active_connections: Dict[str, Set[asyncio.Queue]] = defaultdict(set)

async def broadcast_to_merchant(merchant_id: str, event_data: dict):
    if merchant_id in active_connections:
        for queue in active_connections[merchant_id]:
            await queue.put(event_data)
```

### 4. Background WakeLock & Foreground Notification (`SoundboxForegroundService.kt`)
Keeps the CPU awake and prevents the Android OS from killing the real-time payment listener:
```kotlin
val notification = NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("UPI Smart Soundbox Active")
    .setContentText("Listening for incoming verified payments...")
    .setSmallIcon(R.drawable.ic_soundbox_active)
    .setOngoing(true)
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .build()

startForeground(NOTIFICATION_ID, notification)
wakeLock.acquire(10 * 60 * 1000L) // Partial wake lock
```

---

## 💡 Summary of Key Architectural Decisions

1. **Why Compose instead of XML?**
   Jetpack Compose reduces boilerplate by 60%, eliminates fragment lifecycles, and provides smooth reactive state management (`StateFlow` and `collectAsStateWithLifecycle`).
2. **Why SQLite/Room instead of Cloud-Only?**
   Network signals in Indian markets can fluctuate. A local Room database ensures the merchant can view their transaction history even during internet drops.
3. **Why Dual-Tone Chime before Voice?**
   Audio human-factor studies show that an immediate acoustic cue (ding-dong chime) alerts human hearing before speech processing begins, ensuring 100% of the spoken numbers are understood.

