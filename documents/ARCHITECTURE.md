# UPI Smart Soundbox – Architecture & Technical Design

## 1. Executive Summary

The **UPI Smart Soundbox** is an enterprise-grade, lightweight solution designed to turn any Android smartphone into a digital voice payment soundbox for Indian merchants. It replaces expensive dedicated hardware soundbox devices with an Android smartphone application powered by an ultra-reliable, cryptographically verified backend engine.

---

## 2. System Architecture

```
+---------------------------------------------------------------------------------------+
|                                    CUSTOMER DEVICE                                    |
|             (Google Pay / PhonePe / Paytm / BHIM / Any UPI 2.0 Application)           |
+---------------------------------------------------------------------------------------+
                                           |
                                           | 1. Scans UPI QR Code
                                           |    (upi://pay?pa=...&pn=...&am=...&tr=...)
                                           v
+---------------------------------------------------------------------------------------+
|                               NPCI / BANK UPI SWITCH                                  |
|                         (HDFC / ICICI / SBI / Axis / Gateway)                         |
+---------------------------------------------------------------------------------------+
                                           |
                                           | 2. Webhook / Server Callback
                                           |    Signed with HMAC-SHA256
                                           v
+---------------------------------------------------------------------------------------+
|                       UPI SOUNDBOX BACKEND SERVICE (FastAPI)                          |
|                                                                                       |
|   +--------------------------+  +--------------------------+  +--------------------+  |
|   | Cryptographic Verifier   |  | Idempotency & Replay     |  | Event Broadcaster  |  |
|   | (HMAC-SHA256 & Secret)   |  | Protection (DB Unique)   |  | (SSE & WebSocket)  |  |
|   +--------------------------+  +--------------------------+  +--------------------+  |
|                                           |                                           |
|                                           v                                           |
|                            [SQLite / PostgreSQL Storage]                              |
+---------------------------------------------------------------------------------------+
                                           |
                                           | 3. Real-Time Push Stream (SSE)
                                           |    `PAYMENT_VERIFIED`
                                           v
+---------------------------------------------------------------------------------------+
|                             ANDROID SOUNDBOX SMARTPHONE                               |
|                                                                                       |
|   +-------------------------------------------------------------------------------+   |
|   | UI Layer (Jetpack Compose Material 3)                                         |   |
|   | - Welcome Screen           - Merchant Registration     - Home Dashboard       |   |
|   | - My QR Code Screen        - Payment History Screen    - Settings Screen      |   |
|   +-------------------------------------------------------------------------------+   |
|                                           |                                           |
|   +-------------------------------------------------------------------------------+   |
|   | SoundboxForegroundService (Background & WakeLock Execution)                   |   |
|   | - Keeps CPU active during event delivery via PowerManager.WakeLock            |   |
|   | - Connects to Server-Sent Events stream with auto-reconnect backoff           |   |
|   +-------------------------------------------------------------------------------+   |
|                                           |                                           |
|   +-------------------------------------------------------------------------------+   |
|   | TextToSpeechManager                                                           |   |
|   | - Android Text-To-Speech (Indian English locale `en_IN`)                      |   |
|   | - Transient Audio Focus management (ducks background audio during speech)     |   |
|   | - In-memory O(1) concurrent duplicate speech guard                            |   |
|   | - Formats phrase: "Payment received, 100 rupees."                             |   |
|   +-------------------------------------------------------------------------------+   |
|                                           |                                           |
|   +-------------------------------------------------------------------------------+   |
|   | SoundboxDatabase (Room SQLite Local Persistence)                              |   |
|   | - Caches transactions locally for offline viewing and zero duplicate speech   |   |
|   | - Sends ACK (`/api/v1/transactions/{id}/ack`) to backend                      |   |
|   +-------------------------------------------------------------------------------+   |
+---------------------------------------------------------------------------------------+
```

---

## 3. Core Android Modules

### 3.1 `SoundboxForegroundService`
- **Purpose**: Runs 24/7 in the background without being killed by Android battery management.
- **Service Type**: `dataSync` / `connectedDevice` with an ongoing notification (`NotificationCompat.Builder`).
- **Wake Lock**: Acquires a temporary `PARTIAL_WAKE_LOCK` upon receiving a verified payment to guarantee speech finishes completely even if the phone screen is off.
- **Boot Persistence**: Registered with `SoundboxBootReceiver` to auto-restart upon device reboot.

### 3.2 `TextToSpeechManager`
- **Purpose**: Speaks verified payment amounts clearly through the phone speaker.
- **Audio Routing**: Uses `AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` with `STREAM_MUSIC` volume.
- **Audio Focus**: Automatically requests audio focus to duck or pause any media playing on the phone.
- **Duplicate Guard**: Double-layer check (in-memory concurrent set + Room DB lookup) to guarantee no payment is ever spoken twice.

### 3.3 Dynamic UPI QR Engine (`UPIUriGenerator` & `QRCodeBitmapGenerator`)
- Complies strictly with the NPCI UPI Linking Specification.
- Generates high-resolution bitmaps (up to 1024x1024) using ZXing.
- Provides native Android Sharesheet integration via `FileProvider`.
- Allows saving QR image directly to device gallery (`Pictures/UPI_Soundbox`).

---

## 4. Backend Engine & Verification Architecture

### 4.1 Cryptographic Signature Verification
Never trust client input or unverified notifications. The backend verifies all incoming webhooks using constant-time HMAC-SHA256:

$$\text{Signature} = \text{HMAC-SHA256}(\text{Raw Payload Bytes}, \text{Secret Key})$$

Constant-time comparison (`hmac.compare_digest`) prevents side-channel timing attacks.

### 4.2 Replay Attack Prevention
Every verified payment must contain a unique transaction reference:
- Gateway `payment_id` (e.g. `pay_...`)
- Bank Reference Number / UTR (`bank_rrn`, e.g. `499123456789`)
- Order reference (`order_id`, e.g. `ord_...`)

If a duplicate callback arrives for an already verified payment, the backend idempotently returns the existing status without re-broadcasting to the soundbox.

### 4.3 Real-Time Stream (Server-Sent Events)
The endpoint `GET /api/v1/merchants/{merchant_id}/stream` delivers events instantaneously with low bandwidth and minimal battery drain compared to continuous short polling. If the stream disconnects, the Android client uses exponential backoff and synchronizes any pending announcements via `/api/v1/merchants/{merchant_id}/transactions/pending-announcements`.

