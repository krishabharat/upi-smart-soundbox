import httpx
import json
import time

BASE_URL = "http://localhost:8000"

def test_full_soundbox_lifecycle():
    print("\n" + "=" * 60)
    print(">>> RUNNING FULL END-TO-END UPI SOUNDBOX VERIFICATION TEST")
    print("=" * 60)

    client = httpx.Client(base_url=BASE_URL, timeout=10.0)

    # 1. Health check
    print("\n[Step 1] Checking backend health...")
    res = client.get("/health")
    assert res.status_code == 200, f"Health check failed: {res.text}"
    print("-> Backend is healthy:", res.json())

    # 2. Register Merchant
    print("\n[Step 2] Registering merchant store...")
    merchant_data = {
        "business_name": "Gupta Daily Provisions",
        "upi_id": "guptastore@okhdfcbank",
        "mobile_number": "9812345678"
    }
    res = client.post("/api/v1/merchants/register", json=merchant_data)
    assert res.status_code == 201, f"Merchant registration failed: {res.text}"
    merchant = res.json()
    merchant_id = merchant["id"]
    print(f"-> Merchant Registered: {merchant['business_name']} | ID: {merchant_id} | UPI: {merchant['upi_id']}")

    # 3. Generate Payment Order & UPI QR URI
    print("\n[Step 3] Generating payment order & standard UPI URI...")
    res = client.post(f"/api/v1/merchants/{merchant_id}/orders", json={"amount": 100.0, "note": "Provisions Payment"})
    assert res.status_code == 200, f"Order creation failed: {res.text}"
    order = res.json()
    print("-> Order Created:", order["order_id"])
    print("-> Standard NPCI UPI URI:", order["upi_uri"])

    # 4. Fetch QR code image
    print("\n[Step 4] Requesting merchant QR Code PNG...")
    res = client.get(f"/api/v1/merchants/{merchant_id}/qr")
    assert res.status_code == 200
    assert res.headers["content-type"] == "image/png"
    print(f"-> QR Code generated successfully ({len(res.content)} bytes)")

    # 5. Simulate Verified Payment Webhook (₹100)
    print("\n[Step 5] Customer makes payment of ₹100. Simulating Gateway Webhook with HMAC-SHA256...")
    sim_payload = {
        "merchant_id": merchant_id,
        "amount": 100.0,
        "payer_name": "Vikram Sethi",
        "payer_vpa": "vikram@oksbi",
        "order_id": order["order_id"]
    }
    res = client.post("/api/v1/webhooks/simulate", json=sim_payload)
    assert res.status_code == 200, f"Webhook simulation failed: {res.text}"
    webhook_res = res.json()
    print("-> Webhook Status:", webhook_res["message"])
    print("-> Verified Transaction ID:", webhook_res["transaction_id"])
    print("-> Text-to-Speech Announcement Text:", f'"{webhook_res["announcement_text"]}"')
    tx_id = webhook_res["transaction_id"]

    # 6. Check Pending Announcements
    print("\n[Step 6] Soundbox phone queries pending unannounced payments...")
    res = client.get(f"/api/v1/merchants/{merchant_id}/transactions/pending-announcements")
    assert res.status_code == 200
    pending = res.json()
    assert len(pending) >= 1
    assert any(p["id"] == tx_id for p in pending)
    print(f"-> Found {len(pending)} pending payment(s) waiting to be spoken.")

    # 7. Device Acknowledges Speech Done
    print("\n[Step 7] Soundbox speaker announces payment. Sending ACK to backend...")
    res = client.post(f"/api/v1/transactions/{tx_id}/ack")
    assert res.status_code == 200
    print("-> Acknowledged on backend:", res.json())

    # 8. Check Pending Announcements again (Should now be cleared)
    res = client.get(f"/api/v1/merchants/{merchant_id}/transactions/pending-announcements")
    pending_after = res.json()
    assert not any(p["id"] == tx_id for p in pending_after)
    print("-> Confirmed pending list no longer contains acknowledged transaction (Zero repeat speech!)")

    # 9. Verify Today's Collection Summary
    print("\n[Step 8] Verifying Today's Collection Dashboard Summary...")
    res = client.get(f"/api/v1/merchants/{merchant_id}/summary/today")
    assert res.status_code == 200
    summary = res.json()
    print(f"-> Today's Total Verified Amount: ₹{summary['total_amount']}")
    print(f"-> Today's Verified Transactions: {summary['transaction_count']}")
    assert summary["total_amount"] >= 100.0
    assert summary["transaction_count"] >= 1

    # 10. Simulate Another Payment (₹250)
    print("\n[Step 9] Simulating another customer payment of ₹250...")
    res = client.post("/api/v1/webhooks/simulate", json={
        "merchant_id": merchant_id,
        "amount": 250.0,
        "payer_name": "Priya Sharma",
        "payer_vpa": "priya@okhdfcbank"
    })
    assert res.status_code == 200
    data2 = res.json()
    print("-> Voice Announcement Text:", f'"{data2["announcement_text"]}"')

    # Re-check summary
    res = client.get(f"/api/v1/merchants/{merchant_id}/summary/today")
    summary2 = res.json()
    print(f"-> New Today's Total: ₹{summary2['total_amount']} ({summary2['transaction_count']} payments)")
    assert summary2["total_amount"] >= 350.0

    print("\n" + "=" * 60)
    print(">>> ALL SOUNDBOX VERIFICATION TESTS PASSED SUCCESSFULLY! <<<")
    print("=" * 60 + "\n")

if __name__ == "__main__":
    test_full_soundbox_lifecycle()

