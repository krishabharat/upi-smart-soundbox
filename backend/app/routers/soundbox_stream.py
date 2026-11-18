import asyncio
import json
from typing import Dict, Set
from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from app.database import get_db
from app.models import Merchant, PaymentTransaction
from app.schemas import PaymentTransactionResponse

router = APIRouter(prefix="/api/v1/merchants", tags=["Soundbox Stream"])

# In-memory pub/sub broker for active soundbox clients per merchant
class SoundboxConnectionManager:
    def __init__(self):
        # merchant_id -> Set of asyncio.Queue
        self._listeners: Dict[str, Set[asyncio.Queue]] = {}
        self._lock = asyncio.Lock()

    async def connect(self, merchant_id: str) -> asyncio.Queue:
        async with self._lock:
            if merchant_id not in self._listeners:
                self._listeners[merchant_id] = set()
            queue = asyncio.Queue(maxsize=100)
            self._listeners[merchant_id].add(queue)
            return queue

    async def disconnect(self, merchant_id: str, queue: asyncio.Queue):
        async with self._lock:
            if merchant_id in self._listeners:
                self._listeners[merchant_id].discard(queue)
                if not self._listeners[merchant_id]:
                    del self._listeners[merchant_id]

    async def broadcast_payment(self, merchant_id: str, data: dict):
        async with self._lock:
            listeners = self._listeners.get(merchant_id, set()).copy()
            
        for queue in listeners:
            try:
                queue.put_nowait(data)
            except asyncio.QueueFull:
                pass

soundbox_broker = SoundboxConnectionManager()

@router.get("/{merchant_id}/stream")
async def stream_soundbox_events(
    merchant_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    """
    Server-Sent Events (SSE) endpoint for real-time payment notifications.
    The Android soundbox service connects to this stream.
    When a payment is verified, the event is immediately pushed to the phone.
    """
    # Verify merchant exists
    stmt = select(Merchant).where(Merchant.id == merchant_id, Merchant.is_active == True)
    result = await db.execute(stmt)
    merchant = result.scalar_one_or_none()
    if not merchant:
        raise HTTPException(status_code=404, detail="Merchant not found or inactive")

    queue = await soundbox_broker.connect(merchant_id)

    async def event_generator():
        try:
            # Send initial connected heartbeat
            welcome_data = {
                "event": "CONNECTED",
                "merchant_id": merchant_id,
                "business_name": merchant.business_name,
                "status": "SOUNDBOX_READY"
            }
            yield f"data: {json.dumps(welcome_data)}\n\n"

            while True:
                # Disconnect if client closed connection
                if await request.is_disconnected():
                    break

                try:
                    # Wait for next event or send periodic keep-alive comment
                    event_data = await asyncio.wait_for(queue.get(), timeout=15.0)
                    yield f"data: {json.dumps(event_data)}\n\n"
                except asyncio.TimeoutError:
                    # Keep-alive heartbeat comment so network sockets stay alive
                    yield ": ping\n\n"

        except asyncio.CancelledError:
            pass
        finally:
            await soundbox_broker.disconnect(merchant_id, queue)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )

