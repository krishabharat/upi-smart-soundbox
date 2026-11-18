import json
from typing import Dict, Any, Optional, Tuple
from app.gateway.base import BasePaymentGateway
from app.security import verify_hmac_sha256, generate_upi_uri
from app.config import settings

class RazorpayPaymentGateway(BasePaymentGateway):
    """
    Legitimate Razorpay Payment Gateway integration for UPI.
    Processes live/test UPI QR payments and validates webhook signatures.
    """

    @property
    def provider_name(self) -> str:
        return "razorpay"

    async def create_payment_order(
        self,
        merchant_id: str,
        business_name: str,
        upi_id: str,
        amount: Optional[float],
        order_id: str,
        note: str
    ) -> Dict[str, Any]:
        upi_uri = generate_upi_uri(
            upi_id=upi_id,
            business_name=business_name,
            amount=amount,
            order_id=order_id,
            mcc=settings.DEFAULT_MCC,
            note=note
        )
        return {
            "order_id": order_id,
            "provider": self.provider_name,
            "upi_uri": upi_uri,
            "qr_data": upi_uri,
            "status": "ACTIVE"
        }

    def verify_webhook_signature(
        self,
        raw_body: bytes,
        headers: Dict[str, str]
    ) -> bool:
        normalized_headers = {k.lower(): v for k, v in headers.items()}
        signature = normalized_headers.get("x-razorpay-signature")
        if not signature:
            return False
        return verify_hmac_sha256(raw_body, settings.RAZORPAY_WEBHOOK_SECRET, signature)

    def parse_webhook_payload(
        self,
        payload: Dict[str, Any]
    ) -> Tuple[str, str, float, Optional[str], Optional[str], Optional[str]]:
        event = payload.get("event")
        if event not in ["payment.captured", "order.paid"]:
            raise ValueError(f"Ignored non-success event: {event}")

        payment_entity = payload.get("payload", {}).get("payment", {}).get("entity", {})
        
        # Razorpay sends amount in smallest currency unit (paise)
        raw_amount = payment_entity.get("amount", 0)
        amount = float(raw_amount) / 100.0

        order_id = payment_entity.get("notes", {}).get("soundbox_order_id") or payment_entity.get("order_id")
        payment_id = payment_entity.get("id")
        bank_rrn = payment_entity.get("acquirer_data", {}).get("rrn") or payment_entity.get("acquirer_data", {}).get("upi_transaction_id")
        payer_vpa = payment_entity.get("vpa")
        payer_name = payment_entity.get("notes", {}).get("customer_name") or "UPI Customer"

        if not order_id:
            order_id = f"rzp_{payment_id}"

        return order_id, payment_id, amount, bank_rrn, payer_vpa, payer_name

