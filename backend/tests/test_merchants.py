import pytest

@pytest.mark.asyncio
async def test_register_merchant_success(client):
    payload = {
        "business_name": "Patel Sweets & Snacks",
        "upi_id": "patelsweets@oksbi",
        "mobile_number": "9823456789"
    }
    response = await client.post("/api/v1/merchants/register", json=payload)
    assert response.status_code == 201
    data = response.json()
    assert data["business_name"] == "Patel Sweets & Snacks"
    assert data["upi_id"] == "patelsweets@oksbi"
    assert "id" in data
    assert "api_key" in data

@pytest.mark.asyncio
async def test_register_merchant_invalid_upi(client):
    payload = {
        "business_name": "Invalid Store",
        "upi_id": "invalid_upi_format_no_at"
    }
    response = await client.post("/api/v1/merchants/register", json=payload)
    assert response.status_code == 422 # Validation error

@pytest.mark.asyncio
async def test_get_merchant_by_id(client):
    # Register first
    payload = {
        "business_name": "Gupta Medical Store",
        "upi_id": "guptamedical@icici",
        "mobile_number": "9123456780"
    }
    create_res = await client.post("/api/v1/merchants/register", json=payload)
    merchant_id = create_res.json()["id"]

    get_res = await client.get(f"/api/v1/merchants/{merchant_id}")
    assert get_res.status_code == 200
    assert get_res.json()["business_name"] == "Gupta Medical Store"

