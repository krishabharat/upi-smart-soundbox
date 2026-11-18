# Stage 1: Core UI & QR Generation Engine

In Stage 1, we built the foundation of the UPI Smart Soundbox Android application. The focus was to design an intuitive, distraction-free merchant experience that generates official NPCI-compliant payment QR codes and persists merchant details permanently.

![App Screens and UI Flow](images/04_app_screens_and_ui_flow.png)

---

## 🎯 Stage 1 Objectives

1. **Strict Two-Tab Architecture**:
   - Tab 1: **My QR** (Merchant setup, QR standee, sharing, downloading, editing).
   - Tab 2: **Received History** (Today's total received and verified payment feed).
   - *Strict Rule*: No complex dashboards, no pricing pages, no subscription upsells, no login walls.
2. **NPCI-Compliant UPI QR Code**:
   - Generate standards-compliant UPI deep-link QR codes accepted by Google Pay, PhonePe, Paytm, BHIM, and all Indian banking apps.
3. **Local Persistence with Jetpack DataStore**:
   - Store Business Name and UPI ID locally on the device so the merchant never has to re-type them.
4. **Standee Actions**:
   - Allow the merchant to share the QR image via WhatsApp or download it to the device gallery for printing.
   - Allow instant editing of merchant details.

---

## 📐 1. UI & UX Design Philosophy

Indian merchants operate in fast-paced, high-stress environments (*crowded markets, noisy streets, customers waiting in line*). They cannot navigate complex menus or read small text.

### Key Design Decisions:
- **Primary Color**: Emerald Green (`#0F766E` / `#10B981`) representing trust, wealth, and successful fintech operations.
- **Background**: Crisp white and slate (`#F8FAFC`) with high-contrast text (`#0F172A`).
- **Typography**: Large, bold fonts for easy readability from 3–4 feet away.
- **Fixed Bottom Navigation**: Two large icons and text labels fixed at the bottom:
  ```kotlin
  NavigationBar(
      containerColor = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp
  ) {
      NavigationBarItem(
          selected = currentDestination == SoundboxDestinations.MY_QR,
          onClick = { navController.navigate(SoundboxDestinations.MY_QR) },
          icon = { Icon(Icons.Default.QrCode2, contentDescription = "My QR") },
          label = { Text("My QR") }
      )
      NavigationBarItem(
          selected = currentDestination == SoundboxDestinations.HISTORY,
          onClick = { navController.navigate(SoundboxDestinations.HISTORY) },
          icon = { Icon(Icons.Default.History, contentDescription = "History") },
          label = { Text("Received History") }
      )
  }
  ```

---

## 📱 2. NPCI Standard UPI QR Specification

Under the hood, a UPI QR code is a URI string conforming strictly to the National Payments Corporation of India (NPCI) specification:

```text
upi://pay?pa=<UPI_ID>&pn=<BUSINESS_NAME>&cu=INR
```

### Parameters Breakdown:
| Parameter | Full Name | Purpose | Example |
| :--- | :--- | :--- | :--- |
| `pa` | Payee Address | The merchant's Virtual Payment Address (VPA) / UPI ID | `sharma.kirana@okhdfcbank` |
| `pn` | Payee Name | The legal or trade business name | `Sharma Kirana Store` |
| `cu` | Currency Code | The currency of the transaction (Indian Rupee) | `INR` |
| `mc` *(optional)* | Merchant Category | 4-digit MCC code for business categories | `5411` (Grocery stores) |

### Kotlin QR Matrix Generator:
We use ZXing (`com.google.zxing`) to transform the URI string into a crisp, high-density black-and-white bitmap:

```kotlin
fun generateQRCodeBitmap(content: String, size: Int = 512): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1 // Minimal white border
    )
    val bitMatrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        hints
    )
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(
                x, y,
                if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
        }
    }
    return bitmap
}
```

---

## 💾 3. Local Persistence with Jetpack DataStore

To guarantee that shopkeeper details survive device reboots, app updates, and cache clears without requiring a cloud login, we use **Jetpack DataStore Preferences**:

```kotlin
class MerchantPreferencesRepository(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "merchant_prefs")

    private val KEY_BUSINESS_NAME = stringPreferencesKey("business_name")
    private val KEY_UPI_ID = stringPreferencesKey("upi_id")
    private val KEY_QR_GENERATED = booleanPreferencesKey("qr_generated")

    val merchantProfile: Flow<MerchantProfile> = context.dataStore.data.map { prefs ->
        MerchantProfile(
            businessName = prefs[KEY_BUSINESS_NAME] ?: "",
            upiId = prefs[KEY_UPI_ID] ?: "",
            isQrGenerated = prefs[KEY_QR_GENERATED] ?: false
        )
    }

    suspend fun saveProfile(name: String, upiId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BUSINESS_NAME] = name
            prefs[KEY_UPI_ID] = upiId
            prefs[KEY_QR_GENERATED] = true
        }
    }
}
```

---

## 📤 4. Share & Download QR Standee

Shopkeepers need to print their QR standee or send it to their sign maker on WhatsApp.

1. **Download Standee**: Captures the Jetpack Compose standee layout as an `ARGB_8888` PNG and saves it directly to the Android `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` gallery.
2. **Share Standee**: Generates an Android `ACTION_SEND` Intent with a secure `FileProvider` URI so WhatsApp, Telegram, or Email can attach the standee image instantly.

```kotlin
fun shareStandee(context: Context, bitmap: Bitmap) {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()
    val file = File(cachePath, "my_upi_qr.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val contentUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Standee via"))
}
```

---

## 🔄 5. Editing Merchant Details

If the shopkeeper changes their bank account or updates their shop name, they click **"Edit Details"**:
- The standee collapses into the pre-filled form.
- The merchant updates their details.
- Clicking **"Generate My QR"** re-saves the profile to DataStore and re-renders the new QR matrix dynamically.

---

### Next Step:
Now that the QR standee is generated, continue to **[Stage 2: Payment Verification & Backend Engine](02_STAGE_2_PAYMENT_VERIFICATION_AND_BACKEND.md)** to learn how real payments are captured and verified!

