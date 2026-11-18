# NPCI UPI Deep Linking & QR Code Specification

## 1. UPI Payment URI Standard

According to the National Payments Corporation of India (NPCI) Universal Linking specifications, standard UPI payment URIs adhere to the following schema:

```
upi://pay?<parameter-key>=<parameter-value>&<parameter-key>=<parameter-value>...
```

### Parameter Reference

| Parameter | Name | Mandatory | Description | Example |
|---|---|---|---|---|
| `pa` | Payee Virtual Payment Address | **Yes** | The merchant's verified UPI ID. | `sharma.store@okhdfcbank` |
| `pn` | Payee Name | **Yes** | Business or shop name displayed to customer. | `Sharma Kirana Store` |
| `mc` | Merchant Category Code | Optional | 4-digit standard MCC. Default: `5411` (Grocery stores). | `5411` |
| `tr` | Transaction Reference ID | Recommended | Merchant's unique order reference for reconciliation. | `ord_7c3a9f018e` |
| `tid` | Terminal Transaction ID | Optional | Terminal or device ID for merchant tracking. | `tid_soundbox_01` |
| `am` | Transaction Amount | Optional | Decimal payment amount (INR). If omitted, customer inputs amount. | `100.00` |
| `cu` | Currency Code | **Yes** | Standard currency code. Must be `INR`. | `INR` |
| `tn` | Transaction Note | Recommended | Descriptive note shown in customer's passbook. | `Payment to Sharma Store` |

---

## 2. Dynamic vs Static QR Codes

### Static Merchant QR (General Counter Display)
- **Amount (`am`)**: Omitted.
- **Transaction Ref (`tr`)**: Omitted or generic merchant handle.
- **Customer Experience**: Customer scans QR, types the bill amount, enters UPI MPIN, and completes payment.
- **Soundbox Verification**: Verification relies on the bank's UPI switch pushing the incoming credit webhook matched to the merchant's UPI ID.

### Dynamic Payment QR (Invoice / Specific Bill)
- **Amount (`am`)**: Pre-filled (e.g. `150.00`).
- **Transaction Ref (`tr`)**: Unique UUID / order ID.
- **Customer Experience**: Customer scans QR, the exact amount `₹150.00` is already filled in and locked, customer only enters MPIN.
- **Soundbox Verification**: Backend matches exact `order_id` in webhook callback, instantly verifying bill clearance.

---

## 3. Sample Compliant URIs

### Static QR Code URI:
```
upi://pay?pa=sharma.store@okhdfcbank&pn=Sharma%20Kirana%20Store&mc=5411&cu=INR&tn=Payment%20to%20Sharma%20Store
```

### Dynamic QR Code URI for ₹250.00:
```
upi://pay?pa=sharma.store@okhdfcbank&pn=Sharma%20Kirana%20Store&mc=5411&cu=INR&am=250.00&tr=ord_b831fa2c90&tid=ord_b831fa2c90&tn=Bill%20Payment
```

