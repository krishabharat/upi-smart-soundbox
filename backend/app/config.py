import os
from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict

is_serverless = (
    os.getenv("VERCEL") is not None
    or os.getenv("AWS_LAMBDA_FUNCTION_NAME") is not None
    or os.getenv("LAMBDA_TASK_ROOT") is not None
)
default_db_path = "/tmp/soundbox.db" if is_serverless else "./soundbox.db"
default_db_url = f"sqlite+aiosqlite:///{default_db_path}"

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="allow")

    APP_NAME: str = "UPI Smart Soundbox Backend API"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    
    # Database: In serverless (Vercel/Lambda), filesystem is read-only except /tmp
    DATABASE_URL: str = os.getenv("DATABASE_URL", default_db_url)
    
    # Server configuration
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    
    # Security
    SECRET_KEY: str = os.getenv("SECRET_KEY", "soundbox_secure_jwt_secret_key_prod_2026")
    WEBHOOK_SHARED_SECRET: str = os.getenv("WEBHOOK_SHARED_SECRET", "whsec_soundbox_production_secret_key_8832")
    
    # Gateway settings
    GATEWAY_PROVIDER: str = os.getenv("GATEWAY_PROVIDER", "sandbox") # 'sandbox', 'razorpay', 'cashfree'
    RAZORPAY_KEY_ID: Optional[str] = os.getenv("RAZORPAY_KEY_ID", "rzp_test_sample_key")
    RAZORPAY_KEY_SECRET: Optional[str] = os.getenv("RAZORPAY_KEY_SECRET", "rzp_test_sample_secret")
    RAZORPAY_WEBHOOK_SECRET: Optional[str] = os.getenv("RAZORPAY_WEBHOOK_SECRET", "rzp_webhook_secret_key_12345")
    
    CASHFREE_APP_ID: Optional[str] = os.getenv("CASHFREE_APP_ID", "cf_test_app_id")
    CASHFREE_SECRET_KEY: Optional[str] = os.getenv("CASHFREE_SECRET_KEY", "cf_test_secret_key")
    
    # NPCI Default Merchant Category Code for retail store
    DEFAULT_MCC: str = "5411" # Grocery stores, supermarkets

settings = Settings()
