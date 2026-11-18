import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from app.config import settings
from app.database import init_db, AsyncSessionLocal
from app.models import Merchant
from app.routers import merchants, payments, webhooks, soundbox_stream, bridge
from sqlalchemy import select

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize DB schema
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
    return FileResponse(SIMULATOR_FILE)

@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "HEALTHY",
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "gateway_mode": settings.GATEWAY_PROVIDER
    }

