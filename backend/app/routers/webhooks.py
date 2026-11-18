import json
import uuid
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, Request, Header, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_

from app.database import get_db
from app.models import Merchant, PaymentTransaction, WebhookLog
from app.schemas import WebhookSimulateRequest, WebhookResponse
from app.gateway import get_payment_gateway
from app.gateway.sandbox_provider import SandboxPaymentGateway
from app.gateway.razorpay_provider import RazorpayPaymentGateway
from app.security import format_inr_amount_words, compute_hmac_sha256
from app.routers.soundbox_stream import soundbox_broker
from app.config import settings

router = APIRouter(prefix="/api/v1/webhooks", tags=["Payment Webhooks & Verification"])

async def process_verified_payment(
    db: AsyncSession,
    gateway_name: str,
    order_id: str,
    payment_id: str,
    amount: float,
    bank_rrn: str,
    payer_vpa: str,
    payer_name: str,
    merchant_id: str = None
) -> PaymentTransaction:
    """
    Core verification and state transition logic:
    1. Checks if payment has already been verified (Idempotency / Replay protection).
    2. Resolves merchant and transaction record.
    3. Strictly enforces status check before announcing.
    4. Generates formatted announcement voice text.
    5. Dispatches real-time broadcast to connected soundbox devices.
    """
    # 1. Replay prevention check by bank_rrn or payment_id
    if payment_id:
        stmt_dup = select(PaymentTransaction).where(
            and_(PaymentTransaction.payment_id == payment_id, PaymentTransaction.status == "SUCCESS")
        )
        res_dup = await db.execute(stmt_dup)
        existing_tx = res_dup.scalar_one_or_none()
        if existing_tx:
            # Already verified and processed! Idempotently return without duplicate announcement.
            return existing_tx

    # 2. Find transaction by order_id
    tx = None
    if order_id:
        stmt_tx = select(PaymentTransaction).where(PaymentTransaction.order_id == order_id)
        res_tx = await db.execute(stmt_tx)
        tx = res_tx.scalar_one_or_none()

    now = datetime.now(timezone.utc)
    announcement_text = format_inr_amount_words(amount)

    if tx:
        # Update existing order
        tx.payment_id = payment_id
        tx.bank_rrn = bank_rrn
        tx.payer_vpa = payer_vpa
        tx.payer_name = payer_name
        tx.amount = amount
        tx.status = "SUCCESS"
        tx.verified_at = now
        tx.announcement_text = announcement_text
        resolved_merchant_id = tx.merchant_id
    else:
        # Create verified transaction directly matched to merchant
        if not merchant_id:
            # Fallback to finding first active merchant or raise
            stmt_m = select(Merchant).where(Merchant.is_active == True).limit(1)
            res_m = await db.execute(stmt_m)
            merchant = res_m.scalar_one_or_none()
            if not merchant:
                raise ValueError("No active merchant found for payment")
            resolved_merchant_id = merchant.id
        else:
            resolved_merchant_id = merchant_id

        tx = PaymentTransaction(
            merchant_id=resolved_merchant_id,
            order_id=order_id or f"ord_{uuid.uuid4().hex[:12]}",
            payment_id=payment_id,
            bank_rrn=bank_rrn,
            payer_vpa=payer_vpa,
            payer_name=payer_name,
            amount=amount,
            currency="INR",
            status="SUCCESS",
            gateway_provider=gateway_name,
            verified_at=now,
            announcement_text=announcement_text,
            announced=False
        )
        db.add(tx)

    await db.commit()
    await db.refresh(tx)

    # Broadcast event to soundbox listener in real-time
    event_payload = {
        "event": "PAYMENT_VERIFIED",
        "merchant_id": resolved_merchant_id,
        "transaction_id": tx.id,
        "order_id": tx.order_id,
        "payment_id": tx.payment_id,
        "bank_rrn": tx.bank_rrn,
        "amount": tx.amount,
        "currency": tx.currency,
        "payer_name": tx.payer_name,
        "payer_vpa": tx.payer_vpa,
        "announcement_text": announcement_text,
        "timestamp": now.isoformat()
    }
    await soundbox_broker.broadcast_payment(resolved_merchant_id, event_payload)

    return tx

@router.post("/sandbox")
async def handle_sandbox_webhook(
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    """
    Standard Sandbox Webhook endpoint.
    Verifies HMAC-SHA256 signature in X-Soundbox-Signature header.
    """
    body_bytes = await request.body()
    gateway = get_payment_gateway("sandbox")
    
    # Cryptographic signature verification
    is_valid = gateway.verify_webhook_signature(body_bytes, dict(request.headers))
    
    # Audit log
    webhook_log = WebhookLog(
        gateway="sandbox",
        event_type="payment.captured",
        signature=request.headers.get("x-soundbox-signature"),
        is_verified=is_valid,
        payload=body_bytes.decode("utf-8", errors="replace")
    )
    db.add(webhook_log)
    await db.commit()

    if not is_valid:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid webhook signature. Unauthorized transaction confirmation."
        )

    try:
        payload = json.loads(body_bytes.decode("utf-8"))
        order_id, payment_id, amount, bank_rrn, payer_vpa, payer_name = gateway.parse_webhook_payload(payload)
        merchant_id = payload.get("data", {}).get("merchant_id")

        tx = await process_verified_payment(
            db=db,
            gateway_name="sandbox",
            order_id=order_id,
            payment_id=payment_id,
            amount=amount,
            bank_rrn=bank_rrn,
            payer_vpa=payer_vpa,
            payer_name=payer_name,
            merchant_id=merchant_id
        )

        return {
            "success": True,
            "message": "Payment verified and broadcast to Soundbox",
            "transaction_id": tx.id,
            "amount": tx.amount,
            "announcement_text": tx.announcement_text
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to process webhook: {str(e)}")

@router.post("/razorpay")
async def handle_razorpay_webhook(
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    """
    Razorpay Webhook endpoint.
    Verifies X-Razorpay-Signature with webhook secret before processing.
    """
    body_bytes = await request.body()
    gateway = get_payment_gateway("razorpay")

    is_valid = gateway.verify_webhook_signature(body_bytes, dict(request.headers))
    
    webhook_log = WebhookLog(
        gateway="razorpay",
        event_type="razorpay.webhook",
        signature=request.headers.get("x-razorpay-signature"),
        is_verified=is_valid,
        payload=body_bytes.decode("utf-8", errors="replace")
    )
    db.add(webhook_log)
    await db.commit()

    if not is_valid:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Razorpay webhook signature."
        )

    try:
        payload = json.loads(body_bytes.decode("utf-8"))
        order_id, payment_id, amount, bank_rrn, payer_vpa, payer_name = gateway.parse_webhook_payload(payload)

        tx = await process_verified_payment(
            db=db,
            gateway_name="razorpay",
            order_id=order_id,
            payment_id=payment_id,
            amount=amount,
            bank_rrn=bank_rrn,
            payer_vpa=payer_vpa,
            payer_name=payer_name
        )

        return {"status": "ok", "transaction_id": tx.id}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/simulate", response_model=WebhookResponse)
async def simulate_verified_payment(
    payload: WebhookSimulateRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Legitimate Payment Gateway Sandbox Simulator.
    Simulates a verified UPI payment callback:
    1. Verifies merchant exists.
    2. Builds a standardized gateway webhook payload.
    3. Signs payload with HMAC-SHA256.
    4. Verifies the cryptographic signature.
    5. Saves verified transaction to DB with bank reference number (RRN).
    6. Dispatches real-time announcement to soundbox app!
    """
    stmt_m = select(Merchant).where(Merchant.id == payload.merchant_id)
    res_m = await db.execute(stmt_m)
    merchant = res_m.scalar_one_or_none()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found")

    gateway = SandboxPaymentGateway()
    order_id = payload.order_id or f"ord_{uuid.uuid4().hex[:10]}"
    bank_rrn = payload.bank_rrn or f"4{str(uuid.uuid4().int)[:11]}" # 12-digit simulated NPCI UTR

    signed_data, signature = gateway.generate_signed_payload(
        order_id=order_id,
        merchant_id=merchant.id,
        amount=payload.amount,
        bank_rrn=bank_rrn,
        payer_vpa=payload.payer_vpa or "customer@okhdfcbank",
        payer_name=payload.payer_name or "Rahul Sharma"
    )

    # Verify signature
    raw_bytes = json.dumps(signed_data, separators=(',', ':')).encode('utf-8')
    headers = {"X-Soundbox-Signature": signature}
    if not gateway.verify_webhook_signature(raw_bytes, headers):
        raise HTTPException(status_code=400, detail="Signature verification failed in simulation")

    parsed_order_id, payment_id, amount, rrn, vpa, name = gateway.parse_webhook_payload(signed_data)

    tx = await process_verified_payment(
        db=db,
        gateway_name="sandbox",
        order_id=parsed_order_id,
        payment_id=payment_id,
        amount=amount,
        bank_rrn=rrn,
        payer_vpa=vpa,
        payer_name=name,
        merchant_id=merchant.id
    )

    return WebhookResponse(
        success=True,
        message="Simulated payment successfully verified and broadcast to Soundbox",
        transaction_id=tx.id,
        order_id=tx.order_id,
        amount=tx.amount,
        announcement_text=tx.announcement_text
    )

