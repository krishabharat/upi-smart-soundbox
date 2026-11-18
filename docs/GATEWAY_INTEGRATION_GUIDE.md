# Payment Gateway & Bank API Integration Guide

This guide details how to integrate live UPI payment aggregators or direct bank switch APIs with the UPI Smart Soundbox backend.

---

## 1. Supported Providers

1. **Built-in Sandbox / Simulator**: Out of the box, zero credentials required. Generates cryptographically authentic HMAC-SHA256 callbacks.
2. **Razorpay UPI QR & Payment Links**: Full webhook support (`X-Razorpay-Signature`).
3. **Cashfree AutoCollect / Smart UPI**: Instant bank reconciliation webhooks.
4. **Direct Bank UPI Switches**: HDFC EazyPay, ICICI UPI Gateway, Axis Bank Bharat Connect.

---

## 2. Integrating Razorpay

### Step 1: Obtain API Keys and Webhook Secret
1. Log in to your Razorpay Dashboard (Test or Live mode).
2. Go to **Settings** > **API Keys** to get your `Key ID` and `Key Secret`.
3. Go to **Settings** > **Webhooks** > **Add New Webhook**:
   - **Webhook URL**: `https://your-domain.com/api/v1/webhooks/razorpay`
   - **Secret**: Set a strong random secret (e.g. `rzp_sec_9938102391238`)
   - **Active Events**: Check `payment.captured` and `order.paid`.

### Step 2: Configure Environment Variables
In `backend/.env`:
```env
GATEWAY_PROVIDER=razorpay
RAZORPAY_KEY_ID=rzp_live_your_actual_key
RAZORPAY_KEY_SECRET=your_actual_secret
RAZORPAY_WEBHOOK_SECRET=rzp_sec_9938102391238
```

---

## 3. Integrating Cashfree Payments

### Step 1: Configure Webhook
1. Go to Cashfree Merchant Dashboard > **Developers** > **Webhooks**.
2. Add Webhook endpoint: `https://your-domain.com/api/v1/webhooks/cashfree`
3. Event: `PAYMENT_SUCCESS`.

### Step 2: Configure Environment Variables
```env
GATEWAY_PROVIDER=cashfree
CASHFREE_APP_ID=your_cashfree_app_id
CASHFREE_SECRET_KEY=your_cashfree_secret_key
```

---

## 4. Webhook Security Checklist

1. **Always Verify Signatures**: The backend strictly validates signatures before any status update.
2. **Store Raw Request Body**: Signature computation requires the exact byte representation before JSON parsing.
3. **Idempotency**: Prevent replay attacks by checking if `bank_rrn` or `payment_id` has already been recorded.
4. **No Plaintext Secrets in Android**: The Android app **never** stores secret keys; all verification happens server-side.

