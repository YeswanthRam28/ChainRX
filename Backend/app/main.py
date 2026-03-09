from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.blockchain.web3_connector import Web3Connector
from app.routes import auth, shipments, bids, admin, tracking
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(
    title="ChainRX Healthcare Logistics API",
    description="Role-based backend for healthcare logistics with blockchain integration",
    version="2.0.0"
)

# CORS — allow Android app and Swagger UI
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Web3
web3_connector = Web3Connector()

# Mount routers
app.include_router(auth.router, prefix="/auth", tags=["Authentication"])
app.include_router(shipments.router, tags=["Shipments"])
app.include_router(bids.router, tags=["Bids"])
app.include_router(admin.router, prefix="/admin", tags=["Admin"])
app.include_router(tracking.router, tags=["Real-time Tracking"])

@app.get("/", tags=["Health"])
async def root():
    return {
        "service": "ChainRX Backend",
        "blockchain_connected": web3_connector.is_connected(),
        "version": "2.0.0"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
