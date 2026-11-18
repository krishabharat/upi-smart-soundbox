from abc import ABC, abstractmethod
from typing import Dict, Any, Optional, Tuple

class BasePaymentGateway(ABC):
    """Abstract interface for legitimate UPI payment gateway integrations."""

    @property
    @abstractmethod
    def provider_name(self) -> str:
        """Name of the provider, e.g. 'sandbox', 'razorpay', 'cashfree'."""
        pass

    @abstractmethod
    async def create_payment_order(
        self,
        merchant_id: str,
        business_name: str,
        upi_id: str,
        amount: Optional[float],
        order_id: str,
        note: str
    ) -> Dict[str, Any]:
        """Create a payment order/intent with the gateway."""
        pass

    @abstractmethod
    def verify_webhook_signature(
        self,
        raw_body: bytes,
        headers: Dict[str, str]
    ) -> bool:
        """Verify the cryptographic authenticity of an incoming gateway webhook."""
        pass

    @abstractmethod
    def parse_webhook_payload(
        self,
        payload: Dict[str, Any]
    ) -> Tuple[str, str, float, Optional[str], Optional[str], Optional[str]]:
        """
        Extract verified transaction details from webhook payload.
        Returns:
            (order_id, payment_id, amount, bank_rrn, payer_vpa, payer_name)
        """
        pass

