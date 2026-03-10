from fastapi import APIRouter, UploadFile, File, Form, Depends, HTTPException
from app.models.schemas import ShipmentCreate, TransporterSelect, ShipmentInfo
from app.services.blockchain_service import BlockchainService
from app.ipfs.ipfs_service import IPFSService
from app.blockchain.web3_connector import Web3Connector
from app.database.neon_db import SessionLocal
from app.services.database_service import DatabaseService
from app.auth.role_checker import RoleChecker, get_current_user
from app.models.db_models import User
from app.services.ai_advisor import AIAdvisor
from typing import List

router = APIRouter()

# ── Dependency Injections ──────────────────────────────────────────
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

def get_ipfs_service():
    return IPFSService()

def get_ai_advisor():
    return AIAdvisor()


# ── CREATE ─────────────────────────────────────────────────────────
@router.post("/createShipment", status_code=201)
async def create_shipment(
    shipment: ShipmentCreate,
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Hospital / Admin creates a new shipment on-chain."""
    return service.create_shipment(shipment, hospital_id=user.id)


# ── LIST ───────────────────────────────────────────────────────────
@router.get("/shipments")
async def get_all_shipments(
    user: User = Depends(get_current_user),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """
    Role-aware listing:
      Admin / Transport → all shipments
      Hospital           → only their own
    """
    if user.role in ("admin", "transport"):
        return service.get_shipments()
    return service.get_shipments(hospital_id=user.id)


# ── SINGLE SHIPMENT ───────────────────────────────────────────────
@router.get("/shipments/{shipment_id}")
async def get_shipment(
    shipment_id: int,
    user: User = Depends(get_current_user),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Fetch a single shipment by its on-chain ID."""
    return service.get_shipment(shipment_id)

# Keep legacy path too
@router.get("/shipment/{shipment_id}")
async def get_shipment_legacy(
    shipment_id: int,
    user: User = Depends(get_current_user),
    service: BlockchainService = Depends(get_blockchain_service)
):
    return service.get_shipment(shipment_id)


# ── SELECT TRANSPORTER ────────────────────────────────────────────
@router.post("/selectTransporter")
async def select_transporter(
    selection: TransporterSelect,
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Hospital picks a transporter from submitted bids."""
    return service.select_transporter(selection)


# ── ACCEPT BID (Select Transporter + Auto Deposit) ────────────────
@router.post("/shipments/{shipment_id}/accept-bid/{bid_id}")
async def accept_bid(
    shipment_id: int,
    bid_id: int,
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """
    Hospital accepts a bid: 
    1. Looks up the transporter's wallet from the bid
    2. Calls selectTransporter on-chain (BIDDING → PENDING)
    3. Auto-deposits escrow (PENDING → escrow locked)
    """
    from app.models.db_models import DB_Bid
    
    # Find the bid
    bid = service.db.db.query(DB_Bid).filter(DB_Bid.id == bid_id).first()
    if not bid:
        raise HTTPException(status_code=404, detail="Bid not found")
    
    # Get the transporter's wallet address
    transporter = service.db.db.query(User).filter(User.id == bid.transporter_id).first()
    if not transporter or not transporter.wallet_address:
        raise HTTPException(status_code=400, detail="Transporter has no wallet address")
    
    # Convert to checksum address (EIP-55)
    from web3 import Web3
    checksum_addr = Web3.to_checksum_address(transporter.wallet_address.lower())
    
    # Step 1: Select transporter on-chain (BIDDING → PENDING)
    selection = TransporterSelect(
        shipmentId=shipment_id,
        transporterAddress=checksum_addr
    )
    result = service.select_transporter(selection)
    
    # Wait for the first transaction to be mined so status changes to PENDING
    # This avoids nonce collisions and state requirement failures
    service.web3.wait_for_receipt(result["tx_hash"])
    
    # Step 2: Auto-deposit escrow (PENDING → escrow locked)
    try:
        deposit_result = service.deposit_escrow(shipment_id)
        result["deposit_tx"] = deposit_result.get("tx_hash")
    except Exception as e:
        # If deposit fails (insufficient funds), still return success for selection
        result["deposit_error"] = str(e)
    
    return result


# ── DEPOSIT ESCROW ────────────────────────────────────────────────
@router.post("/shipments/{shipment_id}/deposit")
async def deposit_escrow_path(
    shipment_id: int,
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Hospital deposits escrow into the smart contract."""
    return service.deposit_escrow(shipment_id)

# Legacy form-based
@router.post("/depositEscrow")
async def deposit_escrow_form(
    shipmentId: int = Form(...),
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    return service.deposit_escrow(shipmentId)


# ── CONFIRM PICKUP ────────────────────────────────────────────────
@router.post("/shipments/{shipment_id}/pickup")
async def confirm_pickup_path(
    shipment_id: int,
    user: User = Depends(RoleChecker(["transport", "hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Transporter confirms cargo collection."""
    return service.confirm_pickup(shipment_id)

# Legacy form-based
@router.post("/confirmPickup")
async def confirm_pickup_form(
    shipmentId: int = Form(...),
    user: User = Depends(RoleChecker(["transport", "hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    return service.confirm_pickup(shipmentId)


# ── CONFIRM DELIVERY ─────────────────────────────────────────────
@router.post("/shipments/{shipment_id}/delivery")
async def confirm_delivery_path(
    shipment_id: int,
    file: UploadFile = File(...),
    user: User = Depends(RoleChecker(["transport", "hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service),
    ipfs_service: IPFSService = Depends(get_ipfs_service)
):
    """Transporter uploads proof-of-delivery and confirms on-chain."""
    ipfs_hash = ipfs_service.upload_content(file.file, file.filename)
    return service.confirm_delivery(shipment_id, ipfs_hash)

# Legacy form-based
@router.post("/confirmDelivery")
async def confirm_delivery_form(
    shipmentId: int = Form(...),
    file: UploadFile = File(...),
    user: User = Depends(RoleChecker(["transport", "hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service),
    ipfs_service: IPFSService = Depends(get_ipfs_service)
):
    ipfs_hash = ipfs_service.upload_content(file.file, file.filename)
    return service.confirm_delivery(shipmentId, ipfs_hash)


# ── RELEASE PAYMENT ──────────────────────────────────────────────
@router.post("/shipments/{shipment_id}/release")
async def release_payment_path(
    shipment_id: int,
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Hospital releases escrowed funds to the transporter."""
    return service.release_payment(shipment_id)

# Legacy form-based
@router.post("/releasePayment")
async def release_payment_form(
    shipmentId: int = Form(...),
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service)
):
    return service.release_payment(shipmentId)
@router.get("/shipments/{shipment_id}/ai-recommendation")
async def get_ai_recommendation(
    shipment_id: int,
    user: User = Depends(RoleChecker(["hospital", "admin"])),
    service: BlockchainService = Depends(get_blockchain_service),
    ai_advisor: AIAdvisor = Depends(get_ai_advisor)
):
    """Get AI recommendation for selecting the best transporter."""
    from app.models.db_models import DB_Bid, User as DB_User
    
    # 1. Get Shipment details
    shipment = service.get_shipment(shipment_id)
    if not shipment:
        raise HTTPException(status_code=404, detail="Shipment not found")
        
    # 2. Get all bids for this shipment
    db_bids = service.db.db.query(DB_Bid).filter(DB_Bid.shipment_id == shipment_id).all()
    if not db_bids:
        return {"error": "No bids available for this shipment."}
        
    # 3. Format bids for AI enrichment (include reputation)
    bids_data = []
    for b in db_bids:
        transporter = service.db.db.query(DB_User).filter(DB_User.id == b.transporter_id).first()
        bids_data.append({
            "bid_id": b.id,
            "transporter_name": transporter.name if transporter else "Unknown",
            "reputation": transporter.reputation_score if transporter else 5.0,
            "amount": float(b.bid_amount),
            "delivery_time": b.estimated_time
        })
        
    # 4. Get AI Decision
    return await ai_advisor.get_best_bid(shipment, bids_data)
