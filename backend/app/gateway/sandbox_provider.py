import uuid
from typing import Dict, Any, Optional, Tuple
from app.gateway.base import BasePaymentGateway
from app.security import verify_hmac_sha256, compute_hmac_sha256, generate_upi_uri
from app.config import settings

class SandboxPaymentGateway(BasePaymentGateway):
    """
    Legitimate Sandbox UPI Gateway Provider.
    Implements standard bank/provider UPI switches with cryptographic HMAC-SHA256 signature
    verification and standard payload schema.
    """

    @property
    def provider_name(self) -> str:
        return "sandbox"

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
        # Case-insensitive header lookup
        normalized_headers = {k.lower(): v for k, v in headers.items()}
        signature = normalized_headers.get("x-soundbox-signature") or normalized_headers.get("x-webhook-signature")
        if not signature:
            return False
        return verify_hmac_sha256(raw_body, settings.WEBHOOK_SHARED_SECRET, signature)

    def parse_webhook_payload(
        self,
        payload: Dict[str, Any]
    ) -> Tuple[str, str, float, Optional[str], Optional[str], Optional[str]]:
        event = payload.get("event", "payment.captured")
        data = payload.get("data", payload)
        
        order_id = data.get("order_id")
        payment_id = data.get("payment_id") or f"pay_sbx_{uuid.uuid4().hex[:10]}"
        amount = float(data.get("amount", 0.0))
        bank_rrn = data.get("bank_rrn") or data.get("utr") or f"RRN{uuid.uuid4().hex[:8].upper()}"
        payer_vpa = data.get("payer_vpa", "customer@upi")
        payer_name = data.get("payer_name", "UPI Customer")

        if not order_id:
            raise ValueError("Webhook missing order_id in payload")
        if amount <= 0:
            raise ValueError("Payment amount must be greater than zero")

        return order_id, payment_id, amount, bank_rrn, payer_vpa, payer_name

    def generate_signed_payload(
        self,
        order_id: str,
        merchant_id: str,
        amount: float,
        bank_rrn: str,
        payer_vpa: str = "customer@oksbi",
        payer_name: str = "Rahul Sharma"
    ) -> Tuple[Dict[str, Any], str]:
        """Utility for test suite and dashboard simulator to generate signed payloads."""
        import json
        payload = {
            "event": "payment.captured",
            "provider": "sandbox",
            "data": {
                "order_id": order_id,
                "merchant_id": merchant_id,
                "payment_id": f"pay_{uuid.uuid4().hex[:12]}",
                "amount": amount,
                "currency": "INR",
                "status": "SUCCESS",
                "bank_rrn": bank_rrn,
                "payer_vpa": payer_vpa,
                "payer_name": payer_name
            }
        }
        body_bytes = json.dumps(payload, separators=(',', ':')).encode('utf-8')
        signature = compute_hmac_sha256(body_bytes, settings.WEBHOOK_SHARED_SECRET)
        return payload, signature

