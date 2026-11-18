from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, and_
from datetime import datetime, timezone, time
from typing import List
from app.database import get_db
from app.models import Merchant, PaymentTransaction
from app.schemas import (
    MerchantCreate,
    MerchantUpdate,
    MerchantResponse,
    TodayCollectionSummary,
    PaymentTransactionResponse
)

router = APIRouter(prefix="/api/v1/merchants", tags=["Merchants"])

@router.post("/register", response_model=MerchantResponse, status_code=status.HTTP_201_CREATED)
async def register_merchant(
    payload: MerchantCreate,
    db: AsyncSession = Depends(get_db)
):
    """
    Register or find existing merchant by UPI ID and Business Name.
    Secures merchant profile and generates a soundbox API key.
    """
    # Check if merchant with this UPI ID already exists
    stmt = select(Merchant).where(Merchant.upi_id == payload.upi_id)
    result = await db.execute(stmt)
    existing = result.scalar_one_or_none()

    if existing:
        # Update existing profile
        existing.business_name = payload.business_name
        if payload.mobile_number:
            existing.mobile_number = payload.mobile_number
        if payload.mcc:
            existing.mcc = payload.mcc
        existing.is_active = True
        await db.commit()
        await db.refresh(existing)
        return existing

    # Create new merchant
    merchant = Merchant(
        business_name=payload.business_name,
        upi_id=payload.upi_id,
        mobile_number=payload.mobile_number,
        mcc=payload.mcc or "5411"
    )
    db.add(merchant)
    await db.commit()
    await db.refresh(merchant)
    return merchant

@router.get("", response_model=List[MerchantResponse])
async def list_merchants(db: AsyncSession = Depends(get_db)):
    """List all registered merchants."""
    stmt = select(Merchant).order_by(Merchant.created_at.desc())
    result = await db.execute(stmt)
    return result.scalars().all()

@router.get("/{merchant_id}", response_model=MerchantResponse)
async def get_merchant(merchant_id: str, db: AsyncSession = Depends(get_db)):
    """Get merchant profile by ID."""
    stmt = select(Merchant).where(Merchant.id == merchant_id)
    result = await db.execute(stmt)
    merchant = result.scalar_one_or_none()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found")
    return merchant

@router.get("/{merchant_id}/summary/today", response_model=TodayCollectionSummary)
async def get_today_summary(merchant_id: str, db: AsyncSession = Depends(get_db)):
    """
    Get today's total verified payments and transaction count.
    Used for the Home Dashboard.
    """
    # Check merchant
    stmt_m = select(Merchant).where(Merchant.id == merchant_id)
    res_m = await db.execute(stmt_m)
    merchant = res_m.scalar_one_or_none()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found")

    now = datetime.now(timezone.utc)
    start_of_today = datetime.combine(now.date(), time.min, tzinfo=timezone.utc)

    # Query sum and count of successful verified payments today
    stmt = select(
        func.coalesce(func.sum(PaymentTransaction.amount), 0.0).label("total_amount"),
        func.count(PaymentTransaction.id).label("total_count")
    ).where(
        and_(
            PaymentTransaction.merchant_id == merchant_id,
            PaymentTransaction.status == "SUCCESS",
            PaymentTransaction.created_at >= start_of_today
        )
    )
    result = await db.execute(stmt)
    row = result.first()
    total_amount = float(row[0]) if row else 0.0
    total_count = int(row[1]) if row else 0

    # Get latest transaction
    stmt_latest = select(PaymentTransaction).where(
        PaymentTransaction.merchant_id == merchant_id,
        PaymentTransaction.status == "SUCCESS"
    ).order_by(PaymentTransaction.created_at.desc()).limit(1)
    res_latest = await db.execute(stmt_latest)
    latest_tx = res_latest.scalar_one_or_none()

    return TodayCollectionSummary(
        merchant_id=merchant_id,
        date=now.strftime("%Y-%m-%d"),
        total_amount=round(total_amount, 2),
        transaction_count=total_count,
        latest_transaction=PaymentTransactionResponse.model_validate(latest_tx) if latest_tx else None
    )

