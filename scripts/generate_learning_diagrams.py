"""
generate_learning_diagrams.py
Generates clean, professional visual architecture and learning diagrams
for the UPI Smart Soundbox project using Pillow.
"""

import os
from PIL import Image, ImageDraw, ImageFont

def get_font(size: int, bold: bool = False):
    font_path = "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf"
    if not os.path.exists(font_path):
        font_path = "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"
    try:
        return ImageFont.truetype(font_path, size)
    except Exception:
        return ImageFont.load_default()

def draw_rounded_rect(draw, bbox, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(bbox, radius=radius, fill=fill, outline=outline, width=width)

def draw_arrow(draw, start, end, color, width=3, arrow_size=10):
    # start: (x1, y1), end: (x2, y2)
    draw.line([start, end], fill=color, width=width)
    x1, y1 = start
    x2, y2 = end
    import math
    angle = math.atan2(y2 - y1, x2 - x1)
    # arrow head points
    p1 = (x2 - arrow_size * math.cos(angle - math.pi / 6),
          y2 - arrow_size * math.sin(angle - math.pi / 6))
    p2 = (x2 - arrow_size * math.cos(angle + math.pi / 6),
          y2 - arrow_size * math.sin(angle + math.pi / 6))
    draw.polygon([end, p1, p2], fill=color)

def generate_stage_roadmap(output_path):
    width, height = 1400, 750
    img = Image.new("RGB", (width, height), "#0B132B")
    draw = ImageDraw.Draw(img)

    # Title & Subtitle
    title_font = get_font(32, bold=True)
    sub_font = get_font(18, bold=False)
    card_title_font = get_font(20, bold=True)
    body_font = get_font(14, bold=False)
    badge_font = get_font(13, bold=True)

    draw.text((60, 40), "UPI Smart Soundbox: Stage-by-Stage App Making Roadmap", font=title_font, fill="#FFFFFF")
    draw.text((60, 85), "Complete end-to-end engineering journey from concept to live production soundbox", font=sub_font, fill="#94A3B8")

    stages = [
        {
            "num": "STAGE 1",
            "title": "Core UI & QR Engine",
            "color": "#10B981", # Emerald
            "items": [
                "Only 2 Bottom Tabs (My QR + History)",
                "NPCI UPI QR Code Generation",
                "upi://pay?pa=... URI matrix encoder",
                "Local persistence via Jetpack DataStore",
                "Clean 'Scan & Pay' merchant standee",
                "Share & Download QR buttons",
                "Editable shop name & UPI VPA"
            ]
        },
        {
            "num": "STAGE 2",
            "title": "Payment Verification",
            "color": "#0EA5E9", # Sky Blue
            "items": [
                "FastAPI async backend engine",
                "Real payment verification architecture",
                "Webhook listener & Bank API bridge",
                "12-digit UTR idempotency check",
                "Duplicate payment prevention logic",
                "Room DB & SQLite local transaction store",
                "Today's Total Received auto-calculation"
            ]
        },
        {
            "num": "STAGE 3",
            "title": "Voice & Audio Engine",
            "color": "#F59E0B", # Amber
            "items": [
                "Two-stage announcement architecture",
                "Dual-tone Indian fintech chime (659Hz -> 880Hz)",
                "Android Text-to-Speech (TTS) integration",
                "Exact format: 'Payment received, X rupees'",
                "Audio focus management (pauses music)",
                "Duplicate voice trigger prevention",
                "Web Audio API simulator fallback"
            ]
        },
        {
            "num": "STAGE 4",
            "title": "Two-Phone Sync & Reliab.",
            "color": "#8B5CF6", # Purple
            "items": [
                "Dad's shop phone vs Mom's bank SIM",
                "NotificationListenerService for bank SMS",
                "Real-time Server-Sent Events (SSE) stream",
                "Instant cloud forwarder bridge (<500ms)",
                "Android Foreground Service with WakeLock",
                "Screen-off 24/7 background operation",
                "Automated Pytest & script test harness"
            ]
        }
    ]

    card_width = 295
    card_height = 540
    start_x = 60
    start_y = 140
    spacing = 25

    for i, st in enumerate(stages):
        x = start_x + i * (card_width + spacing)
        y = start_y
        
        # Card Background
        draw_rounded_rect(draw, (x, y, x + card_width, y + card_height), radius=16, fill="#1C2541", outline="#3A506B", width=2)
        
        # Top colored accent banner
        draw_rounded_rect(draw, (x, y, x + card_width, y + 10), radius=6, fill=st["color"])
        
        # Stage Badge
        badge_w, badge_h = 100, 28
        bx, by = x + 20, y + 25
        draw_rounded_rect(draw, (bx, by, bx + badge_w, by + badge_h), radius=6, fill=st["color"])
        draw.text((bx + 16, by + 5), st["num"], font=badge_font, fill="#0B132B")

        # Card Title
        draw.text((x + 20, y + 68), st["title"], font=card_title_font, fill="#FFFFFF")
        draw.line([(x + 20, y + 105), (x + card_width - 20, y + 105)], fill="#3A506B", width=1)

        # Bullets
        item_y = y + 125
        for item in st["items"]:
            draw.ellipse((x + 20, item_y + 4, x + 28, item_y + 12), fill=st["color"])
            # Break text if needed
            words = item.split()
            line1 = ""
            line2 = ""
            for w in words:
                if len(line1 + " " + w) < 28 and not line2:
                    line1 += (" " if line1 else "") + w
                else:
                    line2 += (" " if line2 else "") + w
            draw.text((x + 36, item_y), line1, font=body_font, fill="#E2E8F0")
            if line2:
                item_y += 20
                draw.text((x + 36, item_y), line2, font=body_font, fill="#94A3B8")
            item_y += 34

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, quality=95)
    print(f"Saved: {output_path}")

def generate_system_architecture(output_path):
    width, height = 1400, 800
    img = Image.new("RGB", (width, height), "#0F172A")
    draw = ImageDraw.Draw(img)

    title_font = get_font(32, bold=True)
    sub_font = get_font(18, bold=False)
    box_title_font = get_font(19, bold=True)
    body_font = get_font(13, bold=False)
    bold_body = get_font(14, bold=True)

    draw.text((60, 35), "UPI Smart Soundbox: End-to-End System Architecture", font=title_font, fill="#FFFFFF")
    draw.text((60, 78), "How a customer's scan becomes verified sound & history on the shopkeeper's phone", font=sub_font, fill="#94A3B8")

    # Column 1: Customer (Left)
    c1_x, c1_y, c1_w, c1_h = 60, 140, 240, 580
    draw_rounded_rect(draw, (c1_x, c1_y, c1_x + c1_w, c1_y + c1_h), radius=14, fill="#1E293B", outline="#475569", width=2)
    draw_rounded_rect(draw, (c1_x, c1_y, c1_x + c1_w, c1_y + 8), radius=4, fill="#38BDF8")
    draw.text((c1_x + 20, c1_y + 25), "1. CUSTOMER", font=box_title_font, fill="#38BDF8")
    draw.text((c1_x + 20, c1_y + 55), "Any UPI App (Payer)", font=bold_body, fill="#FFFFFF")
    c1_text = [
        "Customer scans QR:",
        "• Google Pay",
        "• PhonePe",
        "• Paytm / BHIM",
        "• Any Banking App",
        "",
        "Action:",
        "Customer enters UPI PIN",
        "Money debited from",
        "customer bank account."
    ]
    ty = c1_y + 95
    for line in c1_text:
        draw.text((c1_x + 20, ty), line, font=body_font, fill="#CBD5E1")
        ty += 24

    # Arrow 1 to 2
    draw_arrow(draw, (c1_x + c1_w, 280), (c1_x + c1_w + 60, 280), color="#38BDF8", width=3)
    draw.text((c1_x + c1_w + 5, 255), "NPCI / UPI", font=get_font(12, bold=True), fill="#38BDF8")

    # Column 2: Banking & Ingestion Layer
    c2_x, c2_y, c2_w, c2_h = 360, 140, 280, 580
    draw_rounded_rect(draw, (c2_x, c2_y, c2_x + c2_w, c2_y + c2_h), radius=14, fill="#1E293B", outline="#475569", width=2)
    draw_rounded_rect(draw, (c2_x, c2_y, c2_x + c2_w, c2_y + 8), radius=4, fill="#10B981")
    draw.text((c2_x + 20, c2_y + 25), "2. BANKING & ALERTS", font=box_title_font, fill="#10B981")
    draw.text((c2_x + 20, c2_y + 55), "Real Payment Ingestion", font=bold_body, fill="#FFFFFF")
    
    # Box 2A: Merchant Bank
    draw_rounded_rect(draw, (c2_x + 15, c2_y + 95, c2_x + c2_w - 15, c2_y + 270), radius=10, fill="#0F172A", outline="#334155")
    draw.text((c2_x + 25, c2_y + 105), "Route A: Bank SMS/Notification", font=get_font(13, bold=True), fill="#34D399")
    draw.text((c2_x + 25, c2_y + 130), "Mom's Phone (Holds Bank SIM)", font=body_font, fill="#CBD5E1")
    draw.text((c2_x + 25, c2_y + 155), "Receives SMS: 'Rs.250 credited", font=body_font, fill="#94A3B8")
    draw.text((c2_x + 25, c2_y + 175), "UPI/P2A/123456789012'", font=body_font, fill="#94A3B8")
    draw.text((c2_x + 25, c2_y + 200), "Auto-forwarded via Bridge API", font=get_font(12, bold=True), fill="#38BDF8")
    draw.text((c2_x + 25, c2_y + 225), "Latency: < 400 milliseconds", font=get_font(12, bold=False), fill="#A78BFA")

    # Box 2B: Official Gateway
    draw_rounded_rect(draw, (c2_x + 15, c2_y + 290, c2_x + c2_w - 15, c2_y + 465), radius=10, fill="#0F172A", outline="#334155")
    draw.text((c2_x + 25, c2_y + 300), "Route B: Business Webhook", font=get_font(13, bold=True), fill="#60A5FA")
    draw.text((c2_x + 25, c2_y + 325), "Razorpay / Cashfree / Bank API", font=body_font, fill="#CBD5E1")
    draw.text((c2_x + 25, c2_y + 350), "Official merchant account webhook", font=body_font, fill="#94A3B8")
    draw.text((c2_x + 25, c2_y + 375), "HMAC-SHA256 signature verification", font=body_font, fill="#94A3B8")
    draw.text((c2_x + 25, c2_y + 400), "Instant server-to-server callback", font=get_font(12, bold=True), fill="#38BDF8")

    # Arrow 2 to 3
    draw_arrow(draw, (c2_x + c2_w, 280), (c2_x + c2_w + 60, 280), color="#10B981", width=3)
    draw.text((c2_x + c2_w + 10, 255), "HTTPS POST", font=get_font(12, bold=True), fill="#10B981")

    # Column 3: FastAPI Backend Engine
    c3_x, c3_y, c3_w, c3_h = 700, 140, 310, 580
    draw_rounded_rect(draw, (c3_x, c3_y, c3_x + c3_w, c3_y + c3_h), radius=14, fill="#1E293B", outline="#475569", width=2)
    draw_rounded_rect(draw, (c3_x, c3_y, c3_x + c3_w, c3_y + 8), radius=4, fill="#F59E0B")
    draw.text((c3_x + 20, c3_y + 25), "3. BACKEND ENGINE", font=box_title_font, fill="#F59E0B")
    draw.text((c3_x + 20, c3_y + 55), "FastAPI + Deduplication", font=bold_body, fill="#FFFFFF")

    c3_text = [
        "1. Ingest Payload / Webhook",
        "2. Extract Amount, UTR, Payer",
        "3. Anti-Fraud Deduplication:",
        "   - Check 12-digit UTR in DB",
        "   - Reject replay / fake attempts",
        "4. Save to Verified Transactions",
        "5. Update Merchant Balance",
        "6. Broadcast event over SSE:",
        "   /api/v1/soundbox/stream/{id}",
        "   Real-time event push to device"
    ]
    ty = c3_y + 95
    for line in c3_text:
        draw.text((c3_x + 20, ty), line, font=body_font, fill="#CBD5E1" if not line.startswith("3.") else "#FCD34D")
        ty += 25

    # Arrow 3 to 4
    draw_arrow(draw, (c3_x + c3_w, 280), (c3_x + c3_w + 60, 280), color="#F59E0B", width=3)
    draw.text((c3_x + c3_w + 10, 255), "SSE Stream", font=get_font(12, bold=True), fill="#F59E0B")

    # Column 4: Dad's Android Soundbox App
    c4_x, c4_y, c4_w, c4_h = 1070, 140, 270, 580
    draw_rounded_rect(draw, (c4_x, c4_y, c4_x + c4_w, c4_y + c4_h), radius=14, fill="#1E293B", outline="#475569", width=2)
    draw_rounded_rect(draw, (c4_x, c4_y, c4_x + c4_w, c4_y + 8), radius=4, fill="#A855F7")
    draw.text((c4_x + 20, c4_y + 25), "4. DAD'S PHONE", font=box_title_font, fill="#A855F7")
    draw.text((c4_x + 20, c4_y + 55), "Soundbox App at Shop", font=bold_body, fill="#FFFFFF")

    c4_text = [
        "A. Soundbox Foreground Service",
        "   - Keeps SSE active in bg",
        "   - WakeLock prevents sleep",
        "",
        "B. Voice Engine Triggers:",
        "   - Dual chime (659 -> 880Hz)",
        "   - Android Text-To-Speech:",
        "   'Payment received, 250",
        "    rupees. Thank you!'",
        "",
        "C. Tab 2: Received History",
        "   - Today's Total: +Rs. 250",
        "   - Appends verified card",
        "   - Green 'Verified' badge"
    ]
    ty = c4_y + 95
    for line in c4_text:
        draw.text((c4_x + 20, ty), line, font=body_font, fill="#E2E8F0" if "Payment" in line else "#CBD5E1")
        ty += 24

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, quality=95)
    print(f"Saved: {output_path}")

def generate_two_phone_diagram(output_path):
    width, height = 1400, 750
    img = Image.new("RGB", (width, height), "#0B132B")
    draw = ImageDraw.Draw(img)

    title_font = get_font(32, bold=True)
    sub_font = get_font(18, bold=False)
    box_title = get_font(22, bold=True)
    body_font = get_font(14, bold=False)
    bold_font = get_font(15, bold=True)

    draw.text((60, 35), "The Two-Phone Solution: Dad's Shop vs Mom's Bank SIM", font=title_font, fill="#FFFFFF")
    draw.text((60, 80), "How payments are announced at the shop even when the bank SMS arrives on another phone at home", font=sub_font, fill="#94A3B8")

    # Box 1: Mom's Phone (At Home)
    m_x, m_y, m_w, m_h = 60, 140, 380, 540
    draw_rounded_rect(draw, (m_x, m_y, m_x + m_w, m_y + m_h), radius=16, fill="#1C2541", outline="#3B82F6", width=2)
    draw_rounded_rect(draw, (m_x, m_y, m_x + m_w, m_y + 10), radius=5, fill="#3B82F6")
    draw.text((m_x + 25, m_y + 30), "AT HOME: Mom's Phone", font=box_title, fill="#60A5FA")
    draw.text((m_x + 25, m_y + 65), "Holds the Bank-Registered SIM Card", font=bold_font, fill="#93C5FD")

    m_lines = [
        "Reality in Indian Small Businesses:",
        "The bank account is often registered to a",
        "home SIM card. Dad works at the shop.",
        "",
        "Step 1: Customer scans QR at shop.",
        "Step 2: Bank sends SMS to Mom's phone:",
        "  'Your a/c XXX credited by Rs 250.00",
        "   on 18-Nov-26 by UPI Ref 432198765432'",
        "",
        "Step 3: Background Bridge Forwarder",
        "  - NotificationListenerService catches SMS",
        "  - Parses Amount (250) and UTR (432198...)",
        "  - HTTP POST to Backend in < 200ms"
    ]
    ty = m_y + 110
    for line in m_lines:
        draw.text((m_x + 25, ty), line, font=body_font, fill="#F8FAFC" if "credited" in line else "#CBD5E1")
        ty += 26

    # Box 2: Cloud Sync Engine (Middle)
    c_x, c_y, c_w, c_h = 490, 190, 420, 440
    draw_rounded_rect(draw, (c_x, c_y, c_x + c_w, c_y + c_h), radius=16, fill="#1E293B", outline="#10B981", width=2)
    draw_rounded_rect(draw, (c_x, c_y, c_x + c_w, c_y + 10), radius=5, fill="#10B981")
    draw.text((c_x + 25, c_y + 30), "CLOUD SYNC ENGINE", font=box_title, fill="#34D399")
    draw.text((c_x + 25, c_y + 65), "Instant Bank Bridge & Deduplication", font=bold_font, fill="#A7F3D0")

    c_lines = [
        "1. Instant Endpoint: /api/v1/bridge/forward",
        "2. UTR Verification & Deduplication",
        "   Checks if transaction reference was already",
        "   announced to prevent duplicate alerts.",
        "3. Database Commit (Postgres / Room)",
        "4. SSE Broadcast Event:",
        "   event: payment_verified",
        "   data: { amount: 250, utr: 432198... }",
        "5. Push to Dad's active connection (< 100ms)"
    ]
    ty = c_y + 110
    for line in c_lines:
        draw.text((c_x + 25, ty), line, font=body_font, fill="#F8FAFC" if "event:" in line else "#CBD5E1")
        ty += 26

    # Arrows
    draw_arrow(draw, (m_x + m_w, 380), (c_x, 380), color="#60A5FA", width=3)
    draw.text((m_x + m_w + 10, 355), "Instant API", font=get_font(12, bold=True), fill="#60A5FA")

    # Box 3: Dad's Phone (At Shop)
    d_x, d_y, d_w, d_h = 960, 140, 380, 540
    draw_rounded_rect(draw, (d_x, d_y, d_x + d_w, d_y + d_h), radius=16, fill="#1C2541", outline="#F59E0B", width=2)
    draw_rounded_rect(draw, (d_x, d_y, d_x + d_w, d_y + 10), radius=5, fill="#F59E0B")
    draw.text((d_x + 25, d_y + 30), "AT SHOP: Dad's Phone", font=box_title, fill="#FBBF24")
    draw.text((d_x + 25, d_y + 65), "UPI Smart Soundbox Speaker App", font=bold_font, fill="#FDE68A")

    d_lines = [
        "Dad does NOT have the bank SIM,",
        "YET gets 100% real payment alerts!",
        "",
        "Action at Shop (Zero Hassle):",
        "1. Soundbox receives SSE event instantly.",
        "2. Plays loud dual chime (Ding-Dong!).",
        "3. Clear Voice Announcement:",
        "   'Payment received, 250 rupees.",
        "    Thank you!'",
        "4. History updates with verified badge.",
        "5. Dad packs customer's groceries safely",
        "   without calling home or checking SMS!"
    ]
    ty = d_y + 110
    for line in d_lines:
        draw.text((d_x + 25, ty), line, font=body_font, fill="#FBBF24" if "Payment received" in line else "#CBD5E1")
        ty += 26

    draw_arrow(draw, (c_x + c_w, 380), (d_x, 380), color="#10B981", width=3)
    draw.text((c_x + c_w + 10, 355), "SSE Push", font=get_font(12, bold=True), fill="#10B981")

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, quality=95)
    print(f"Saved: {output_path}")

def generate_app_screens_diagram(output_path):
    width, height = 1400, 750
    img = Image.new("RGB", (width, height), "#0F172A")
    draw = ImageDraw.Draw(img)

    title_font = get_font(32, bold=True)
    sub_font = get_font(18, bold=False)
    screen_title = get_font(20, bold=True)
    body_font = get_font(13, bold=False)
    btn_font = get_font(14, bold=True)

    draw.text((60, 35), "UPI Smart Soundbox: Two-Tab Android User Experience", font=title_font, fill="#FFFFFF")
    draw.text((60, 78), "Designed strictly with 2 tabs: Minimalist, zero distractions, built for fast merchant workflow", font=sub_font, fill="#94A3B8")

    # Phone Mockup 1: Tab 1 (My QR)
    p1_x, p1_y, p1_w, p1_h = 100, 130, 380, 580
    draw_rounded_rect(draw, (p1_x, p1_y, p1_x + p1_w, p1_y + p1_h), radius=30, fill="#FFFFFF", outline="#334155", width=6)
    
    # Status bar
    draw_rounded_rect(draw, (p1_x + 120, p1_y + 10, p1_x + 260, p1_y + 26), radius=8, fill="#0F172A")
    
    # App Bar
    draw_rounded_rect(draw, (p1_x + 6, p1_y + 36, p1_x + p1_w - 6, p1_y + 85), radius=0, fill="#0F766E")
    draw.text((p1_x + 25, p1_y + 48), "My QR & Soundbox", font=screen_title, fill="#FFFFFF")

    # Standee Card
    s_x, s_y, s_w, s_h = p1_x + 30, p1_y + 100, 320, 380
    draw_rounded_rect(draw, (s_x, s_y, s_x + s_w, s_y + s_h), radius=16, fill="#F8FAFC", outline="#CBD5E1", width=2)
    # Standee Header
    draw_rounded_rect(draw, (s_x, s_y, s_x + s_w, s_y + 40), radius=6, fill="#0F766E")
    draw.text((s_x + 105, s_y + 10), "Scan & Pay", font=get_font(16, bold=True), fill="#FFFFFF")
    
    # QR Placeholder Box
    qr_box = (s_x + 55, s_y + 55, s_x + 265, s_y + 265)
    draw_rounded_rect(draw, qr_box, radius=8, fill="#FFFFFF", outline="#0F766E", width=2)
    draw.text((s_x + 95, s_y + 145), "[ NPCI UPI QR ]", font=get_font(16, bold=True), fill="#0F766E")
    draw.text((s_x + 75, s_y + 175), "BHIM / GPay / PhonePe", font=get_font(12, bold=False), fill="#64748B")

    # Merchant details
    draw.text((s_x + 70, s_y + 280), "Sharma Kirana Store", font=get_font(18, bold=True), fill="#0F172A")
    draw.text((s_x + 100, s_y + 310), "sharma@okhdfcbank", font=get_font(14, bold=False), fill="#0F766E")
    draw.text((s_x + 80, s_y + 340), "Accepted with Voice Alerts", font=get_font(12, bold=False), fill="#64748B")

    # Action Buttons
    draw_rounded_rect(draw, (s_x, s_y + 395, s_x + 150, s_y + 430), radius=8, fill="#0F766E")
    draw.text((s_x + 35, s_y + 403), "Share QR", font=btn_font, fill="#FFFFFF")
    draw_rounded_rect(draw, (s_x + 170, s_y + 395, s_x + s_w, s_y + 430), radius=8, fill="#E2E8F0", outline="#CBD5E1")
    draw.text((s_x + 185, s_y + 403), "Download QR", font=btn_font, fill="#0F172A")

    # Bottom Nav
    draw_rounded_rect(draw, (p1_x + 6, p1_y + p1_h - 60, p1_x + p1_w - 6, p1_y + p1_h - 6), radius=0, fill="#FFFFFF", outline="#E2E8F0")
    draw.text((p1_x + 65, p1_y + p1_h - 42), "★ My QR (Active)", font=get_font(14, bold=True), fill="#0F766E")
    draw.text((p1_x + 230, p1_y + p1_h - 42), "Received History", font=get_font(14, bold=False), fill="#64748B")

    # Phone Mockup 2: Tab 2 (Received History)
    p2_x, p2_y, p2_w, p2_h = 560, 130, 380, 580
    draw_rounded_rect(draw, (p2_x, p2_y, p2_x + p2_w, p2_y + p2_h), radius=30, fill="#FFFFFF", outline="#334155", width=6)
    
    # Status bar
    draw_rounded_rect(draw, (p2_x + 120, p2_y + 10, p2_x + 260, p2_y + 26), radius=8, fill="#0F172A")
    
    # App Bar
    draw_rounded_rect(draw, (p2_x + 6, p2_y + 36, p2_x + p2_w - 6, p2_y + 85), radius=0, fill="#0F766E")
    draw.text((p2_x + 25, p2_y + 48), "Received Payments", font=screen_title, fill="#FFFFFF")

    # Today's Total Banner
    tot_box = (p2_x + 20, p2_y + 100, p2_x + p2_w - 20, p2_y + 195)
    draw_rounded_rect(draw, tot_box, radius=14, fill="#0F766E")
    draw.text((p2_x + 35, p2_y + 115), "TODAY'S TOTAL RECEIVED", font=get_font(13, bold=True), fill="#A7F3D0")
    draw.text((p2_x + 35, p2_y + 140), "₹ 3,450.00", font=get_font(28, bold=True), fill="#FFFFFF")

    # Transaction items
    tx_samples = [
        {"amt": "₹ 250.00", "time": "Just Now", "ref": "UPI/432198765432", "status": "VERIFIED"},
        {"amt": "₹ 1,200.00", "time": "18 Nov, 04:12 PM", "ref": "UPI/432198765001", "status": "VERIFIED"},
        {"amt": "₹ 80.00", "time": "18 Nov, 02:45 PM", "ref": "UPI/432198764890", "status": "VERIFIED"},
        {"amt": "₹ 450.00", "time": "18 Nov, 01:10 PM", "ref": "UPI/432198763211", "status": "VERIFIED"}
    ]
    iy = p2_y + 215
    for tx in tx_samples:
        draw_rounded_rect(draw, (p2_x + 20, iy, p2_x + p2_w - 20, iy + 65), radius=10, fill="#F8FAFC", outline="#E2E8F0")
        draw.text((p2_x + 35, iy + 12), tx["amt"], font=get_font(18, bold=True), fill="#0F172A")
        draw.text((p2_x + 35, iy + 38), tx["time"] + " • " + tx["ref"], font=get_font(11, bold=False), fill="#64748B")
        
        # Badge
        draw_rounded_rect(draw, (p2_x + p2_w - 105, iy + 14, p2_x + p2_w - 30, iy + 38), radius=6, fill="#DCFCE7")
        draw.text((p2_x + p2_w - 97, iy + 18), tx["status"], font=get_font(10, bold=True), fill="#15803D")
        iy += 75

    # Bottom Nav
    draw_rounded_rect(draw, (p2_x + 6, p2_y + p2_h - 60, p2_x + p2_w - 6, p2_y + p2_h - 6), radius=0, fill="#FFFFFF", outline="#E2E8F0")
    draw.text((p2_x + 65, p2_y + p2_h - 42), "My QR", font=get_font(14, bold=False), fill="#64748B")
    draw.text((p2_x + 210, p2_y + p2_h - 42), "★ History (Active)", font=get_font(14, bold=True), fill="#0F766E")

    # Column 3: Design Principles Callout
    d_x, d_y, d_w, d_h = 1000, 130, 350, 580
    draw_rounded_rect(draw, (d_x, d_y, d_x + d_w, d_y + d_h), radius=16, fill="#1E293B", outline="#475569", width=2)
    draw_rounded_rect(draw, (d_x, d_y, d_x + d_w, d_y + 10), radius=5, fill="#10B981")
    draw.text((d_x + 25, d_y + 30), "Core Design Principles", font=get_font(20, bold=True), fill="#34D399")
    draw.text((d_x + 25, d_y + 60), "Built for Indian Shopkeepers", font=get_font(14, bold=False), fill="#94A3B8")

    principles = [
        "1. Strictly Two Tabs",
        "No complex dashboards, no nested menus.",
        "Zero confusion for elderly shopkeepers.",
        "",
        "2. High Contrast Standee",
        "Customer scans easily from 3-4 feet away.",
        "Clear NPCI UPI formatting.",
        "",
        "3. Real Money Only",
        "Never adds speculative scans.",
        "Only 100% verified credits enter history.",
        "",
        "4. Audible Feedback",
        "Loud dual-tone chime cuts through",
        "busy market noise.",
        "",
        "5. Permanent Persistence",
        "Never lose merchant details upon reboot."
    ]
    ty = d_y + 100
    for p in principles:
        draw.text((d_x + 25, ty), p, font=body_font, fill="#F8FAFC" if p.startswith("1.") or p.startswith("2.") or p.startswith("3.") or p.startswith("4.") or p.startswith("5.") else "#94A3B8")
        ty += 24

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, quality=95)
    print(f"Saved: {output_path}")

def generate_verification_lifecycle(output_path):
    width, height = 1400, 700
    img = Image.new("RGB", (width, height), "#0B132B")
    draw = ImageDraw.Draw(img)

    title_font = get_font(32, bold=True)
    sub_font = get_font(18, bold=False)
    step_title = get_font(18, bold=True)
    body_font = get_font(13, bold=False)

    draw.text((60, 35), "UPI Payment Verification & Anti-Fraud Lifecycle", font=title_font, fill="#FFFFFF")
    draw.text((60, 80), "How transactions are verified, deduplicated, and announced in under 1 second", font=sub_font, fill="#94A3B8")

    steps = [
        {
            "step": "STEP 1",
            "title": "Customer Scans & Pays",
            "color": "#38BDF8",
            "lines": [
                "Customer scans UPI QR",
                "Enters amount & UPI PIN",
                "NPCI routes payment",
                "Customer debited"
            ]
        },
        {
            "step": "STEP 2",
            "title": "Bank Credits Account",
            "color": "#10B981",
            "lines": [
                "Merchant bank receives funds",
                "Generates 12-digit UTR",
                "Emits Bank Credit SMS /",
                "Payment Gateway Webhook"
            ]
        },
        {
            "step": "STEP 3",
            "title": "Deduplication & Verification",
            "color": "#F59E0B",
            "lines": [
                "Checks UTR idempotency",
                "Rejects replay attacks",
                "Validates amount > 0",
                "Persists to database"
            ]
        },
        {
            "step": "STEP 4",
            "title": "Dual-Tone Sound Chime",
            "color": "#EC4899",
            "lines": [
                "SSE push to soundbox",
                "Audio focus requested",
                "Plays 659Hz -> 880Hz chime",
                "Cuts through shop noise"
            ]
        },
        {
            "step": "STEP 5",
            "title": "Voice TTS & History",
            "color": "#8B5CF6",
            "lines": [
                "Speaks: 'Payment received,",
                "250 rupees. Thank you!'",
                "Today's total updated",
                "Transaction card displayed"
            ]
        }
    ]

    card_w = 230
    card_h = 440
    start_x = 60
    start_y = 150
    spacing = 40

    for i, s in enumerate(steps):
        x = start_x + i * (card_w + spacing)
        y = start_y
        
        draw_rounded_rect(draw, (x, y, x + card_w, y + card_h), radius=14, fill="#1C2541", outline="#3A506B", width=2)
        draw_rounded_rect(draw, (x, y, x + card_w, y + 8), radius=4, fill=s["color"])
        
        # Step badge
        draw_rounded_rect(draw, (x + 15, y + 20, x + 90, y + 45), radius=6, fill=s["color"])
        draw.text((x + 25, y + 25), s["step"], font=get_font(12, bold=True), fill="#0B132B")
        
        # Title
        words = s["title"].split()
        t1 = " ".join(words[:2])
        t2 = " ".join(words[2:]) if len(words) > 2 else ""
        draw.text((x + 15, y + 60), t1, font=step_title, fill="#FFFFFF")
        if t2:
            draw.text((x + 15, y + 85), t2, font=step_title, fill="#FFFFFF")
            
        draw.line([(x + 15, y + 120), (x + card_w - 15, y + 120)], fill="#3A506B", width=1)
        
        ty = y + 140
        for line in s["lines"]:
            draw.text((x + 15, ty), line, font=body_font, fill="#CBD5E1")
            ty += 28

        # Arrow to next step
        if i < len(steps) - 1:
            arrow_start = (x + card_w, y + card_h // 2)
            arrow_end = (x + card_w + spacing, y + card_h // 2)
            draw_arrow(draw, arrow_start, arrow_end, color=s["color"], width=3, arrow_size=8)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, quality=95)
    print(f"Saved: {output_path}")

if __name__ == "__main__":
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    learning_img_dir = os.path.join(base_dir, "learning", "images")
    docs_img_dir = os.path.join(base_dir, "docs", "images")
    
    for target_dir in [learning_img_dir, docs_img_dir]:
        generate_stage_roadmap(os.path.join(target_dir, "01_stage_by_stage_roadmap.png"))
        generate_system_architecture(os.path.join(target_dir, "02_system_architecture.png"))
        generate_two_phone_diagram(os.path.join(target_dir, "03_two_phone_dad_solution.png"))
        generate_app_screens_diagram(os.path.join(target_dir, "04_app_screens_and_ui_flow.png"))
        generate_verification_lifecycle(os.path.join(target_dir, "05_payment_verification_lifecycle.png"))
    print("All diagrams generated successfully!")

