import uuid
import io
import qrcode
from fastapi import APIRouter, Depends, HTTPException, Query, status
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import List, Optional
from datetime import datetime, timezone

from app.database import get_db
from app.models import Merchant, PaymentTransaction
from app.schemas import (
    CreatePaymentOrderRequest,
    PaymentOrderResponse,
    PaymentTransactionResponse,
    AcknowledgeAnnouncementRequest
)
from app.gateway import get_payment_gateway
from app.security import generate_upi_uri, format_inr_amount_words

router = APIRouter(prefix="/api/v1", tags=["Payments"])

@router.post("/merchants/{merchant_id}/orders", response_model=PaymentOrderResponse)
async def create_payment_order(
    merchant_id: str,
    payload: CreatePaymentOrderRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Generate a payment order with standard NPCI UPI URI and transaction reference.
    Can be used for dynamic QR (specific amount) or static QR (open amount).
    """
    stmt = select(Merchant).where(Merchant.id == merchant_id, Merchant.is_active == True)
    result = await db.execute(stmt)
    merchant = result.scalar_one_or_none()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found or inactive")

    order_id = f"ord_{uuid.uuid4().hex[:12]}"
    gateway = get_payment_gateway()

    gateway_order = await gateway.create_payment_order(
        merchant_id=merchant.id,
        business_name=merchant.business_name,
        upi_id=merchant.upi_id,
        amount=payload.amount,
        order_id=order_id,
        note=payload.note or f"Payment to {merchant.business_name}"
    )

    upi_uri = gateway_order["upi_uri"]

    # Record initial pending transaction
    tx = PaymentTransaction(
        merchant_id=merchant.id,
        order_id=order_id,
        amount=payload.amount if payload.amount else 0.0,
        currency=payload.currency,
        status="PENDING",
        gateway_provider=gateway.provider_name,
        upi_uri=upi_uri
    )
    db.add(tx)
    await db.commit()

    return PaymentOrderResponse(
        order_id=order_id,
        merchant_id=merchant.id,
        business_name=merchant.business_name,
        upi_id=merchant.upi_id,
        amount=payload.amount,
        currency=payload.currency,
        upi_uri=upi_uri,
        qr_data=upi_uri
    )

@router.get("/merchants/{merchant_id}/transactions", response_model=List[PaymentTransactionResponse])
async def get_merchant_transactions(
    merchant_id: str,
    limit: int = Query(50, ge=1, le=200),
    status_filter: Optional[str] = Query(None, alias="status"),
    db: AsyncSession = Depends(get_db)
):
    """Get payment history for a merchant."""
    query = select(PaymentTransaction).where(PaymentTransaction.merchant_id == merchant_id)
    if status_filter:
        query = query.where(PaymentTransaction.status == status_filter.upper())
    query = query.order_by(PaymentTransaction.created_at.desc()).limit(limit)

    result = await db.execute(query)
    return result.scalars().all()

@router.get("/merchants/{merchant_id}/transactions/pending-announcements", response_model=List[PaymentTransactionResponse])
async def get_pending_announcements(
    merchant_id: str,
    db: AsyncSession = Depends(get_db)
):
    """
    Fetch verified transactions that have not yet been announced by the soundbox.
    Ensures zero missed payment voice announcements even if device was offline briefly.
    """
    query = select(PaymentTransaction).where(
        and_(
            PaymentTransaction.merchant_id == merchant_id,
            PaymentTransaction.status == "SUCCESS",
            PaymentTransaction.announced == False
        )
    ).order_by(PaymentTransaction.created_at.asc())

    result = await db.execute(query)
    return result.scalars().all()

@router.post("/transactions/{transaction_id}/ack")
async def acknowledge_announcement(
    transaction_id: str,
    db: AsyncSession = Depends(get_db)
):
    """
    Called by the Android soundbox app once the voice announcement has successfully spoken.
    Guarantees at-least-once announcement and strictly prevents duplicates.
    """
    stmt = select(PaymentTransaction).where(
        (PaymentTransaction.id == transaction_id) | (PaymentTransaction.order_id == transaction_id)
    )
    result = await db.execute(stmt)
    tx = result.scalar_one_or_none()
    if not tx:
        raise HTTPException(status_code=404, detail="Transaction not found")

    if not tx.announced:
        tx.announced = True
        tx.announced_at = datetime.now(timezone.utc)
        await db.commit()

    return {"success": True, "transaction_id": tx.id, "announced": True}

@router.get("/merchants/{merchant_id}/qr")
async def get_merchant_qr_code(
    merchant_id: str,
    amount: Optional[float] = Query(None, gt=0),
    db: AsyncSession = Depends(get_db)
):
    """
    Generate and stream high-resolution PNG QR Code for merchant UPI ID.
    Can be loaded directly in browser or downloaded.
    """
    stmt = select(Merchant).where(Merchant.id == merchant_id)
    result = await db.execute(stmt)
    merchant = result.scalar_one_or_none()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found")

    upi_uri = generate_upi_uri(
        upi_id=merchant.upi_id,
        business_name=merchant.business_name,
        amount=amount,
        mcc=merchant.mcc or "5411",
        note=f"Pay to {merchant.business_name}"
    )

    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=10,
        border=3,
    )
    qr.add_data(upi_uri)
    qr.make(fit=True)

    img = qr.make_image(fill_color="black", back_color="white")
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)

    return Response(content=buf.getvalue(), media_type="image/png")

