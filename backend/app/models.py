import uuid
from datetime import datetime, timezone
from sqlalchemy import Column, String, Float, Boolean, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from app.database import Base

def generate_uuid() -> str:
    return str(uuid.uuid4())

def get_utc_now() -> datetime:
    return datetime.now(timezone.utc)

class Merchant(Base):
    __tablename__ = "merchants"

    id = Column(String(50), primary_key=True, default=lambda: f"mer_{uuid.uuid4().hex[:12]}")
    business_name = Column(String(255), nullable=False)
    upi_id = Column(String(255), nullable=False, index=True)
    mobile_number = Column(String(20), nullable=True)
    mcc = Column(String(10), default="5411")
    api_key = Column(String(64), unique=True, nullable=False, default=lambda: f"sb_live_{uuid.uuid4().hex}")
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), default=get_utc_now)
    updated_at = Column(DateTime(timezone=True), default=get_utc_now, onupdate=get_utc_now)

    transactions = relationship("PaymentTransaction", back_populates="merchant", cascade="all, delete-orphan")

class PaymentTransaction(Base):
    __tablename__ = "payment_transactions"

    id = Column(String(50), primary_key=True, default=lambda: f"tx_{uuid.uuid4().hex[:14]}")
    merchant_id = Column(String(50), ForeignKey("merchants.id"), nullable=False, index=True)
    order_id = Column(String(100), unique=True, nullable=False, index=True)
    payment_id = Column(String(100), nullable=True, index=True)
    bank_rrn = Column(String(50), nullable=True, index=True)  # 12-digit UTR/RRN
    payer_vpa = Column(String(255), nullable=True)
    payer_name = Column(String(255), nullable=True)
    amount = Column(Float, nullable=False)
    currency = Column(String(10), default="INR")
    status = Column(String(30), default="PENDING", index=True)  # PENDING, SUCCESS, FAILED
    gateway_provider = Column(String(50), default="sandbox")
    upi_uri = Column(Text, nullable=True)
    
    # Soundbox voice announcement tracking
    announced = Column(Boolean, default=False, index=True)
    announced_at = Column(DateTime(timezone=True), nullable=True)
    announcement_text = Column(String(255), nullable=True)
    
    raw_webhook_payload = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), default=get_utc_now, index=True)
    verified_at = Column(DateTime(timezone=True), nullable=True)

    merchant = relationship("Merchant", back_populates="transactions")

class WebhookLog(Base):
    __tablename__ = "webhook_logs"

    id = Column(String(50), primary_key=True, default=lambda: f"wh_{uuid.uuid4().hex[:12]}")
    gateway = Column(String(50), nullable=False)
    event_type = Column(String(100), nullable=False)
    signature = Column(String(255), nullable=True)
    is_verified = Column(Boolean, default=False)
    payload = Column(Text, nullable=False)
    created_at = Column(DateTime(timezone=True), default=get_utc_now)

