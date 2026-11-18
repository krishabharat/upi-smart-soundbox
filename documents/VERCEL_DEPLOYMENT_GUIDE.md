# 🚀 Vercel Deployment & Troubleshooting Guide

This guide explains how the UPI Smart Soundbox backend is deployed to **Vercel** and why the `500 FUNCTION_INVOCATION_FAILED` error occurs and how it is resolved.

---

## 🔍 Understanding the Error: `500 FUNCTION_INVOCATION_FAILED`

When you saw:
```text
This page is unavailable
A function needed by this page temporarily failed.

500 FUNCTION_INVOCATION_FAILED
bom1::h62hh-1795013221295-c1e733574e73
```

### What Happened Behind the Scenes:
1. **`bom1`**: Indicates the Vercel edge/serverless region (**Mumbai, India**).
2. **Missing `requirements.txt` at Root**: Vercel clones your GitHub repository and looks for `requirements.txt` in the root folder to know what Python packages to install. Because `requirements.txt` was located inside `backend/`, Vercel did not install FastAPI, Pydantic, SQLAlchemy, or Uvicorn!
3. **Missing `api/index.py` Entrypoint**: Vercel Serverless Functions require an entry point in `api/index.py` that exposes the `app` instance.
4. **Missing `vercel.json` Routing**: Without `vercel.json`, Vercel does not route incoming traffic (`/`, `/health`, `/docs`, `/api/v1/...`) into the Python ASGI application.
5. **Read-Only Serverless Filesystem**: AWS Lambda/Vercel containers have a **read-only** filesystem (except `/tmp`). A default SQLite database trying to write to `./soundbox.db` will crash on startup with `sqlite3.OperationalError: unable to open database file`.

---

## 🛠️ The Solution Configured in the Repository

We have added all required configuration files to ensure 1-click seamless deployment on Vercel:

### 1. Root `requirements.txt`
Created at the repository root so Vercel's Python builder installs all dependencies during `pip install -r requirements.txt`:
- `fastapi`, `uvicorn`, `pydantic-settings`, `sqlalchemy`, `aiosqlite`, `greenlet`, etc.

### 2. `api/index.py` Entry Point
Directs Vercel's serverless runtime to import FastAPI from `backend/app/main.py`:
```python
import sys, os
root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
backend_dir = os.path.join(root_dir, "backend")
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)
from app.main import app
```

### 3. `vercel.json` URL Rewrites
Directs all incoming HTTP traffic to `api/index.py`:
```json
{
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/api/index.py"
    }
  ]
}
```

### 4. Serverless Auto-Detection in `config.py`
The backend now detects if it is running inside Vercel or AWS Lambda (`VERCEL=1` or `LAMBDA_TASK_ROOT`) and automatically switches the default database path to `/tmp/soundbox.db`, avoiding any read-only disk crashes!

---

## ⚡ How to Deploy or Redeploy on Vercel

1. Push your latest code to GitHub (already pushed!).
2. In your Vercel Dashboard:
   - Go to your Project $\rightarrow$ **Deployments**.
   - Click **Redeploy** on the latest commit (`fix: configure Vercel serverless deployment`).
3. Your Vercel URL (e.g. `https://your-project.vercel.app`) will now serve:
   - `https://your-project.vercel.app/` $\rightarrow$ Web Soundbox Simulator
   - `https://your-project.vercel.app/health` $\rightarrow$ JSON Health Check (`{"status": "HEALTHY"}`)
   - `https://your-project.vercel.app/docs` $\rightarrow$ Interactive Swagger UI!

---

## 💡 Important Note on Serverless vs Soundbox Streaming (SSE)

Vercel is a **Serverless (Function-as-a-Service)** platform:
- Serverless functions have execution timeouts (typically 10 to 60 seconds).
- While REST endpoints (`/health`, `/merchants`, `/payments/verify`, `/bridge/forward`, webhooks) work on Vercel, long-lived background SSE audio streams (`/api/v1/merchants/{id}/stream`) require an always-on connection.
- For 24/7 background streaming in production, platforms like **Render**, **Railway**, **Fly.io**, or an **Ubuntu VPS** are recommended alongside Vercel.

