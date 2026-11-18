import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, HTMLResponse
from fastapi.staticfiles import StaticFiles

from app.config import settings
from app.database import init_db, AsyncSessionLocal
from app.models import Merchant
from app.routers import merchants, payments, webhooks, soundbox_stream, bridge
from sqlalchemy import select

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize DB schema with serverless safety
    try:
        await init_db()
        
        # Auto-seed a default sample merchant if database is empty
        async with AsyncSessionLocal() as session:
            result = await session.execute(select(Merchant).limit(1))
            merchant = result.scalar_one_or_none()
            if not merchant:
                sample_merchant = Merchant(
                    business_name="Sharma Kirana & General Store",
                    upi_id="sharmakirana@okhdfcbank",
                    mobile_number="9876543210",
                    mcc="5411"
                )
                session.add(sample_merchant)
                await session.commit()
                print(f"[UPI Soundbox] Seeded sample merchant: {sample_merchant.business_name} ({sample_merchant.id})")
    except Exception as e:
        print(f"[UPI Soundbox] Lifespan database initialization warning: {e}")
            
    yield

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="Backend verification engine, webhook processor, and real-time event broadcaster for Android UPI Soundbox.",
    lifespan=lifespan
)

# CORS Middleware for Android app and browser simulator
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Vercel Serverless Path Normalizer Middleware
@app.middleware("http")
async def vercel_path_normalizer(request, call_next):
    path = request.scope.get("path", "")
    for prefix in ["/api/index.py", "/api/index"]:
        if path.startswith(prefix):
            remainder = path[len(prefix):]
            request.scope["path"] = remainder if remainder else "/"
            break
    if request.scope.get("path") in ["/api", "/api/"]:
        request.scope["path"] = "/"
    elif request.scope.get("path") in ["/api/docs", "/api/docs/"]:
        request.scope["path"] = "/docs"
    elif request.scope.get("path") in ["/api/health", "/api/health/"]:
        request.scope["path"] = "/health"
    elif request.scope.get("path") in ["/api/openapi.json"]:
        request.scope["path"] = "/openapi.json"
    return await call_next(request)

# Include API Routers
app.include_router(merchants.router)
app.include_router(payments.router)
app.include_router(webhooks.router)
app.include_router(soundbox_stream.router)
app.include_router(bridge.router)

# Serve Simulator UI
SIMULATOR_FILE = os.path.join(os.path.dirname(__file__), "static", "simulator.html")

@app.get("/", include_in_schema=False)
@app.get("/simulator", include_in_schema=False)
async def get_simulator_portal():
    if os.path.exists(SIMULATOR_FILE):
        with open(SIMULATOR_FILE, "r", encoding="utf-8") as f:
            return HTMLResponse(content=f.read())
    return HTMLResponse(content="<h1>UPI Smart Soundbox Backend is Running!</h1><p>Visit /docs for OpenAPI documentation.</p>")

@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "HEALTHY",
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "gateway_mode": settings.GATEWAY_PROVIDER
    }

