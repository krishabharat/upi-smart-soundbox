import pytest
import json
from app.gateway.sandbox_provider import SandboxPaymentGateway
from app.security import compute_hmac_sha256, format_inr_amount_words
from app.config import settings

def test_announcement_phrase_formatting():
    assert format_inr_amount_words(50) == "Payment received, 50 rupees. Thank you!"
    assert format_inr_amount_words(100) == "Payment received, 100 rupees. Thank you!"
    assert format_inr_amount_words(250) == "Payment received, 250 rupees. Thank you!"
    assert format_inr_amount_words(1000) == "Payment received, 1,000 rupees. Thank you!"
    assert format_inr_amount_words(1) == "Payment received, 1 rupee. Thank you!"
    assert format_inr_amount_words(50.50) == "Payment received, 50 rupees and 50 paise. Thank you!"

@pytest.mark.asyncio
async def test_webhook_valid_signature_success(client):
    # Register merchant
    reg_res = await client.post("/api/v1/merchants/register", json={
        "business_name": "Verma Supermarket",
        "upi_id": "verma@okhdfcbank"
    })
    merchant_id = reg_res.json()["id"]

    gateway = SandboxPaymentGateway()
    payload, signature = gateway.generate_signed_payload(
        order_id="ord_test_9988",
        merchant_id=merchant_id,
        amount=250.0,
        bank_rrn="499123456789",
        payer_vpa="payer@oksbi",
        payer_name="Sanjay Verma"
    )

    body_bytes = json.dumps(payload, separators=(',', ':')).encode('utf-8')
    headers = {
        "Content-Type": "application/json",
        "X-Soundbox-Signature": signature
    }

    res = await client.post("/api/v1/webhooks/sandbox", content=body_bytes, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True
    assert data["amount"] == 250.0
    assert data["announcement_text"] == "Payment received, 250 rupees. Thank you!"

@pytest.mark.asyncio
async def test_webhook_invalid_signature_rejected(client):
    body_bytes = json.dumps({"event": "payment.captured", "data": {"amount": 500}}).encode('utf-8')
    headers = {
        "Content-Type": "application/json",
        "X-Soundbox-Signature": "invalid_forged_signature_hex"
    }
    res = await client.post("/api/v1/webhooks/sandbox", content=body_bytes, headers=headers)
    assert res.status_code == 401
    assert "Invalid webhook signature" in res.json()["detail"]

@pytest.mark.asyncio
async def test_simulate_endpoint_and_duplicate_prevention(client):
    # Register merchant
    reg_res = await client.post("/api/v1/merchants/register", json={
        "business_name": "Chai Point Express",
        "upi_id": "chaipoint@paytm"
    })
    merchant_id = reg_res.json()["id"]

    # Trigger simulation for ₹50
    sim_res = await client.post("/api/v1/webhooks/simulate", json={
        "merchant_id": merchant_id,
        "amount": 50.0,
        "payer_name": "Vikram Singh",
        "payer_vpa": "vikram@axisbank"
    })
    assert sim_res.status_code == 200
    data = sim_res.json()
    assert data["success"] is True
    assert data["announcement_text"] == "Payment received, 50 rupees. Thank you!"
    tx_id = data["transaction_id"]

    # Verify transaction appears in pending announcements
    pending_res = await client.get(f"/api/v1/merchants/{merchant_id}/transactions/pending-announcements")
    assert pending_res.status_code == 200
    pending_list = pending_res.json()
    assert len(pending_list) == 1
    assert pending_list[0]["id"] == tx_id
    assert pending_list[0]["announced"] is False

    # Acknowledge announcement from soundbox app
    ack_res = await client.post(f"/api/v1/transactions/{tx_id}/ack")
    assert ack_res.status_code == 200
    assert ack_res.json()["announced"] is True

    # Now pending announcements should be 0
    pending_res2 = await client.get(f"/api/v1/merchants/{merchant_id}/transactions/pending-announcements")
    assert len(pending_res2.json()) == 0

    # Today summary should reflect ₹50 total
    summary_res = await client.get(f"/api/v1/merchants/{merchant_id}/summary/today")
    assert summary_res.status_code == 200
    sum_data = summary_res.json()
    assert sum_data["total_amount"] == 50.0
    assert sum_data["transaction_count"] == 1

