from fastapi import APIRouter, Depends, HTTPException
from app.models.schemas import BidSubmit
from app.services.blockchain_service import BlockchainService
from app.blockchain.web3_connector import Web3Connector
from app.database.neon_db import SessionLocal
from app.services.database_service import DatabaseService
from app.auth.role_checker import RoleChecker, get_current_user
from app.models.db_models import User, DB_Bid

router = APIRouter()

# ── Dependencies ──────────────────────────────────────────────────
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def get_db_service(db: SessionLocal = Depends(get_db)):
    return DatabaseService(db)

def get_blockchain_service(db_service: DatabaseService = Depends(get_db_service)):
    return BlockchainService(Web3Connector(), db_service)


# ── SUBMIT BID ────────────────────────────────────────────────────
@router.post("/submitBid")
async def submit_bid(
    bid: BidSubmit,
    user: User = Depends(RoleChecker(["transport"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Transport Providers submit their price/time for a shipment."""
    bid_amount = bid.get_amount()
    
    # Save bid to NeonDB for hospital review
    if service.db:
        service.db.save_bid({
            "shipment_id": bid.shipmentId,
            "transporter_id": user.id,
            "bidder": user.wallet_address or user.name,
            "amount": bid_amount,
            "delivery_time": bid.deliveryTime
        })
    return service.submit_bid(bid)


# ── GET BIDS ──────────────────────────────────────────────────────
@router.get("/bids/{shipment_id}")
async def get_bids(
    shipment_id: int,
    user: User = Depends(RoleChecker(["hospital", "admin", "transport"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """View all bids for a particular shipment."""
    if not service.db:
        return []

    results = service.db.db.query(DB_Bid).filter(DB_Bid.shipment_id == shipment_id).all()

    return [
        {
            "id": b.id,
            "shipmentId": b.shipment_id,
            "transporterId": b.transporter_id,
            "bidder": b.bidder,
            "amount": b.bid_amount,
            "deliveryTime": b.estimated_time
        } for b in results
    ]
