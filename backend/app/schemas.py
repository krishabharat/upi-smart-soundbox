from pydantic import BaseModel, Field, field_validator, ConfigDict
from typing import Optional, List
from datetime import datetime
import re

UPI_ID_REGEX = re.compile(r"^[a-zA-Z0-9.\-_]{2,64}@[a-zA-Z0-9.\-_]{2,64}$")

class MerchantCreate(BaseModel):
    business_name: str = Field(..., min_length=2, max_length=150, description="Name of shop or business")
    upi_id: str = Field(..., min_length=3, max_length=100, description="Merchant UPI ID / VPA (e.g. store@okhdfcbank)")
    mobile_number: Optional[str] = Field(None, max_length=15, description="Shopkeeper phone number (optional)")
    mcc: Optional[str] = Field("5411", max_length=10, description="Merchant Category Code")

    @field_validator("upi_id")
    @classmethod
    def validate_upi_id(cls, v: str) -> str:
        clean_v = v.strip().lower()
        if not UPI_ID_REGEX.match(clean_v):
            raise ValueError("Invalid UPI ID format. Must follow standard pattern (e.g., merchant@bank or store@upi)")
        return clean_v

    @field_validator("business_name")
    @classmethod
    def validate_name(cls, v: str) -> str:
        clean_v = v.strip()
        if len(clean_v) < 2:
            raise ValueError("Business name must have at least 2 characters")
        return clean_v

class MerchantUpdate(BaseModel):
    business_name: Optional[str] = Field(None, min_length=2, max_length=150)
    upi_id: Optional[str] = Field(None, min_length=3, max_length=100)
    mobile_number: Optional[str] = None

class MerchantResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    business_name: str
    upi_id: str
    mobile_number: Optional[str] = None
    mcc: str
    api_key: str
    is_active: bool
    created_at: datetime

class CreatePaymentOrderRequest(BaseModel):
    amount: Optional[float] = Field(None, gt=0, description="Amount in INR (optional; if omitted, customer enters amount)")
    currency: str = Field("INR", max_length=5)
    note: Optional[str] = Field("Payment to Store", max_length=100)

class PaymentOrderResponse(BaseModel):
    order_id: str
    merchant_id: str
    business_name: str
    upi_id: str
    amount: Optional[float] = None
    currency: str
    upi_uri: str
    qr_data: str

class PaymentTransactionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    merchant_id: str
    order_id: str
    payment_id: Optional[str] = None
    bank_rrn: Optional[str] = None
    payer_vpa: Optional[str] = None
    payer_name: Optional[str] = None
    amount: float
    currency: str
    status: str
    gateway_provider: str
    upi_uri: Optional[str] = None
    announced: bool
    announced_at: Optional[datetime] = None
    announcement_text: Optional[str] = None
    created_at: datetime
    verified_at: Optional[datetime] = None

class AcknowledgeAnnouncementRequest(BaseModel):
    transaction_id: str

class TodayCollectionSummary(BaseModel):
    merchant_id: str
    date: str
    total_amount: float
    transaction_count: int
    latest_transaction: Optional[PaymentTransactionResponse] = None

class WebhookSimulateRequest(BaseModel):
    merchant_id: str
    amount: float = Field(..., gt=0, description="Payment amount in INR")
    payer_name: Optional[str] = Field("Rahul Sharma", description="Simulated customer name")
    payer_vpa: Optional[str] = Field("customer@okhdfcbank", description="Simulated customer UPI ID")
    bank_rrn: Optional[str] = Field(None, description="12-digit simulated Bank Reference Number")
    order_id: Optional[str] = Field(None, description="Optional existing order_id or will auto-create")

class WebhookResponse(BaseModel):
    success: bool
    message: str
    transaction_id: Optional[str] = None
    order_id: Optional[str] = None
    amount: Optional[float] = None
    announcement_text: Optional[str] = None
