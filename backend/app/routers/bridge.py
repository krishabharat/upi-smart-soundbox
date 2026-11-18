import re
import uuid
from datetime import datetime, timezone
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.models import Merchant, PaymentTransaction
from app.routers.webhooks import process_verified_payment
from app.routers.soundbox_stream import soundbox_broker

router = APIRouter(prefix="/api/v1/bridge", tags=["Two-Phone Soundbox Bridge"])

class BridgeForwardRequest(BaseModel):
    merchant_id: str
    sms_text: Optional[str] = Field(None, description="Raw bank credit SMS or notification text from Mom's phone")
    amount: Optional[float] = Field(None, description="Explicit amount if already parsed")
    payer_name: Optional[str] = Field(None, description="Customer name if available")
    utr: Optional[str] = Field(None, description="Bank reference number (UTR/RRN)")

AMOUNT_REGEX = re.compile(
    r"(?:credited(?:\s+by|\s+with)?|received|payment\s+of|credit\s+of|rcvd|deposited)\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d{1,2})?)",
    re.IGNORECASE
)
ALT_AMOUNT_REGEX = re.compile(
    r"(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:credited|received|added)",
    re.IGNORECASE
)
UTR_REGEX = re.compile(
    r"(?:ref|rrn|utr|upi\s*ref|txn(?:[\s_]*id)?)[^\d\w]*([0-9a-zA-Z]{8,16})",
    re.IGNORECASE
)
SENDER_REGEX = re.compile(
    r"(?:from|by)\s+([A-Za-z\s]{2,25}?)(?:\s+via|\s+on|\s+through|\.|\,|$)",
    re.IGNORECASE
)

def parse_bank_alert(text: str):
    lower = text.lower()
    # Reject debit or OTP
    if "debited" in lower or "spent" in lower or "withdrawn" in lower or "otp" in lower:
        return None, None, None

    amount = None
    match = AMOUNT_REGEX.search(text)
    if match:
        amount = float(match.group(1).replace(",", ""))
    else:
        alt_match = ALT_AMOUNT_REGEX.search(text)
        if alt_match:
            amount = float(alt_match.group(1).replace(",", ""))

    utr = None
    utr_match = UTR_REGEX.search(text)
    if utr_match:
        utr = utr_match.group(1).strip()

    sender = "UPI Customer"
    sender_match = SENDER_REGEX.search(text)
    if sender_match:
        cand = sender_match.group(1).strip()
        if len(cand) > 1 and cand.lower() != "upi":
            sender = cand

    return amount, utr, sender

@router.post("/forward")
async def forward_from_home_phone(
    payload: BridgeForwardRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Two-Phone Bridge Sync Endpoint:
    Receives bank credit alerts from Mom's phone (at home) and immediately
    syncs and announces them on Dad's phone (at the workplace) in real time!
    """
    # 1. Verify merchant exists
    stmt_m = select(Merchant).where(Merchant.id == payload.merchant_id)
    res_m = await db.execute(stmt_m)
    merchant = res_m.scalar_one_or_none()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant store not found")

    amount = payload.amount
    utr = payload.utr
    sender = payload.payer_name or "UPI Customer"

    # If raw SMS/notification text was provided, parse it
    if payload.sms_text:
        parsed_amt, parsed_utr, parsed_sender = parse_bank_alert(payload.sms_text)
        if parsed_amt is not None:
            amount = parsed_amt
        if parsed_utr:
            utr = parsed_utr
        if parsed_sender and not payload.payer_name:
            sender = parsed_sender

    if amount is None or amount <= 0:
        raise HTTPException(
            status_code=400,
            detail="Could not detect a valid credit payment amount from notification text."
        )

    if not utr:
        utr = f"RRN{uuid.uuid4().hex[:8].upper()}"

    payment_id = f"pay_bridge_{utr}"
    order_id = f"ord_bridge_{uuid.uuid4().hex[:10]}"

    tx = await process_verified_payment(
        db=db,
        gateway_name="bridge_sync",
        order_id=order_id,
        payment_id=payment_id,
        amount=amount,
        bank_rrn=utr,
        payer_vpa=f"{sender.lower().replace(' ', '')}@upi",
        payer_name=sender,
        merchant_id=merchant.id
    )

    return {
        "success": True,
        "message": "Payment forwarded and announced on Dad's phone at workplace!",
        "transaction_id": tx.id,
        "amount": tx.amount,
        "announcement_text": tx.announcement_text,
        "bank_rrn": tx.bank_rrn
    }

