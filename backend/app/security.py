import hmac
import hashlib
import re
from typing import Optional
from fastapi import Header, HTTPException, status, Security
from fastapi.security.api_key import APIKeyHeader
from app.config import settings

API_KEY_HEADER = APIKeyHeader(name="X-API-KEY", auto_error=False)

def verify_hmac_sha256(payload: bytes, secret: str, expected_signature: str) -> bool:
    """
    Verify HMAC-SHA256 signature using constant-time comparison
    to prevent timing attacks.
    """
    if not secret or not expected_signature:
        return False
    
    computed_signature = hmac.new(
        key=secret.encode("utf-8"),
        msg=payload,
        digestmod=hashlib.sha256
    ).hexdigest()
    
    return hmac.compare_digest(computed_signature.lower(), expected_signature.lower())

def compute_hmac_sha256(payload: bytes, secret: str) -> str:
    """Compute HMAC-SHA256 hex digest for a given payload and secret."""
    return hmac.new(
        key=secret.encode("utf-8"),
        msg=payload,
        digestmod=hashlib.sha256
    ).hexdigest()

def format_inr_amount_words(amount: float) -> str:
    """
    Format payment amount into soundbox voice announcement phrase with closing gratitude.
    Examples:
    - 'Payment received, 50 rupees. Thank you!'
    - 'Payment received, 100 rupees. Thank you!'
    - 'Payment received, 250 rupees. Thank you!'
    - 'Payment received, 1,000 rupees. Thank you!'
    """
    # Round to 2 decimal places
    amount = round(amount, 2)
    rupees = int(amount)
    paise = int(round((amount - rupees) * 100))

    # Format rupees with standard commas (e.g. 1,000)
    rupees_str = f"{rupees:,}"
    
    if rupees == 1 and paise == 0:
        return "Payment received, 1 rupee. Thank you!"
    elif paise > 0:
        if rupees == 0:
            return f"Payment received, {paise} paise. Thank you!"
        return f"Payment received, {rupees_str} rupees and {paise} paise. Thank you!"
    else:
        return f"Payment received, {rupees_str} rupees. Thank you!"

def generate_upi_uri(
    upi_id: str,
    business_name: str,
    amount: Optional[float] = None,
    order_id: Optional[str] = None,
    mcc: str = "5411",
    note: str = "Payment to Merchant"
) -> str:
    """
    Generate standard NPCI UPI payment URI.
    Format: upi://pay?pa=...&pn=...&mc=...&tid=...&tr=...&am=...&cu=INR&tn=...
    """
    import urllib.parse
    
    params = [
        ("pa", upi_id),
        ("pn", business_name),
        ("mc", mcc or "5411"),
        ("cu", "INR"),
        ("tn", note)
    ]
    
    if order_id:
        params.append(("tr", order_id)) # Transaction reference ID
        params.append(("tid", order_id)) # Terminal transaction ID
    
    if amount is not None and amount > 0:
        params.append(("am", f"{amount:.2f}"))
        
    query_string = urllib.parse.urlencode(params, quote_via=urllib.parse.quote)
    return f"upi://pay?{query_string}"

