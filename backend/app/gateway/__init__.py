from app.gateway.base import BasePaymentGateway
from app.gateway.sandbox_provider import SandboxPaymentGateway
from app.gateway.razorpay_provider import RazorpayPaymentGateway
from app.config import settings

_gateways = {
    "sandbox": SandboxPaymentGateway(),
    "razorpay": RazorpayPaymentGateway()
}

def get_payment_gateway(provider_name: str = None) -> BasePaymentGateway:
    name = provider_name or settings.GATEWAY_PROVIDER
    if name not in _gateways:
        return _gateways["sandbox"]
    return _gateways[name]

