# How to Setup Real-Time Voice Announcements on Dad's Phone (When Bank SIM is in Mom's Phone)

This guide solves the exact real-world problem:
> **"My Dad runs a shop/workplace, but the bank account and UPI QR code are linked to Mom's phone number at home. When customers pay, Dad doesn't get the alert, customers show fake screenshots, and Dad gets frustrated."**

Here are the 2 best, proven ways to make Dad's phone speak the exact payment amount in real-time at his shop.

---

## METHOD 1: The "Add Staff" Feature in Google Pay for Business / PhonePe (Official, Free & Instant)

If the QR code at your Dad's shop is linked to Google Pay for Business, PhonePe for Business, or Paytm Business:

You do **NOT** need to change the bank mobile number! They have a built-in feature specifically for this:

### Steps on Mom's Phone (At Home):
1. Open **Google Pay for Business** (or PhonePe Business) on Mom's phone.
2. Tap **Settings** (or Business Profile).
3. Tap **Manage Staff** / **Cashier Management**.
4. Tap **Add Staff / Cashier**.
5. Enter your **Dad's Mobile Number** and name.
6. Select role: **Cashier / Staff**.

### Steps on Dad's Phone (At Workplace):
1. Install our **UPI Smart Soundbox** app on Dad's phone.
2. When a customer scans the QR code at the shop:
   - Google Pay / PhonePe automatically sends the payment alert to **BOTH Mom's phone AND Dad's phone at the same time**!
   - Our app on Dad's phone intercepts the alert and instantly announces through his speaker:
     > 🔔 *"Payment received, 250 rupees. Thank you!"*
   - The transaction immediately shows in Dad's **Received History** with the real Bank UTR.
   - Customers can **never cheat your Dad** with fake screenshots again!

---

## METHOD 2: Automatic 2-Phone Bridge Sync (Works with Personal Bank Accounts & Regular UPI)

If Mom's account is a regular personal savings account (HDFC, SBI, ICICI, etc.) where only bank SMS arrives:

We built the **Soundbox Bridge API** (`POST /api/v1/bridge/forward`):

### How it works:
1. When a customer pays at the shop, your bank sends an SMS to Mom's phone at home (e.g., *"A/c *1234 credited with Rs. 200.00 via UPI Ref 491234567890"*).
2. Mom's phone automatically forwards this SMS to your backend server in 0.5 seconds (using a free SMS Forwarder app or MacroDroid on Mom's phone).
3. The server validates the credit alert and pushes it live over the internet to **Dad's phone at the shop**.
4. Dad's phone rings the chime and speaks:
   > 🔔 *"Payment received, 200 rupees. Thank you!"*
5. The payment appears in Dad's **Received History**.

### Easy 2-Minute Setup on Mom's Phone:
1. Install **SMS Forwarder** (free from Google Play Store or F-Droid) on Mom's phone.
2. Add a Forwarding Rule:
   - **Sender filter**: Enter your bank SMS sender (e.g. `HDFCBK`, `SBIUPI`, `ICICIB`, `AXISBK`).
   - **Target**: Webhook URL $\rightarrow$ `http://<your-server-ip>:8000/api/v1/bridge/forward`.
   - **Payload**:
     ```json
     {
       "merchant_id": "mer_f5c049c4f33c",
       "sms_text": "%SMS_BODY%"
     }
     ```
3. Whenever a payment arrives at home, Mom's phone forwards the alert to Dad's phone automatically. Dad never has to call Mom, and no customer can fool him!

