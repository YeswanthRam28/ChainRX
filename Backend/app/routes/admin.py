from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database.neon_db import SessionLocal
from app.models.db_models import User, DB_Shipment, DB_Bid
from app.auth.role_checker import RoleChecker, get_current_user
from typing import List

router = APIRouter()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# ── LIST ALL USERS ────────────────────────────────────────────────
@router.get("/users")
async def list_users(
    user: User = Depends(RoleChecker(["admin"])),
    db: Session = Depends(get_db)
):
    """Admin-only: list every registered user."""
    users = db.query(User).all()
    return [
        {
            "id": u.id,
            "name": u.name,
            "email": u.email,
            "role": u.role,
            "walletAddress": u.wallet_address,
            "reputationScore": u.reputation_score,
            "accountStatus": u.account_status
        } for u in users
    ]


# ── SUSPEND / ACTIVATE USER ──────────────────────────────────────
@router.put("/users/{user_id}/status")
async def update_user_status(
    user_id: int,
    status: str,  # query param: ?status=suspended or ?status=active
    admin: User = Depends(RoleChecker(["admin"])),
    db: Session = Depends(get_db)
):
    """Admin-only: suspend or reactivate a user account."""
    target = db.query(User).filter(User.id == user_id).first()
    if not target:
        raise HTTPException(status_code=404, detail="User not found")
    if status not in ("active", "suspended"):
        raise HTTPException(status_code=400, detail="Status must be 'active' or 'suspended'")
    target.account_status = status
    db.commit()
    return {"message": f"User {target.name} is now {status}"}


# ── DELETE USER ───────────────────────────────────────────────────
@router.delete("/users/{user_id}")
async def delete_user(
    user_id: int,
    admin: User = Depends(RoleChecker(["admin"])),
    db: Session = Depends(get_db)
):
    """Admin-only: remove a user from the system."""
    target = db.query(User).filter(User.id == user_id).first()
    if not target:
        raise HTTPException(status_code=404, detail="User not found")
    db.delete(target)
    db.commit()
    return {"message": f"User {target.name} deleted"}


# ── ADMIN DASHBOARD STATS ────────────────────────────────────────
@router.get("/dashboard")
async def admin_dashboard(
    admin: User = Depends(RoleChecker(["admin"])),
    db: Session = Depends(get_db)
):
    """Admin-only: platform-wide stats at a glance."""
    total_users = db.query(User).count()
    total_shipments = db.query(DB_Shipment).count()
    total_bids = db.query(DB_Bid).count()
    hospitals = db.query(User).filter(User.role == "hospital").count()
    transporters = db.query(User).filter(User.role == "transport").count()

    # Shipment status breakdown
    bidding = db.query(DB_Shipment).filter(DB_Shipment.status == "1").count()
    in_transit = db.query(DB_Shipment).filter(DB_Shipment.status == "2").count()
    delivered = db.query(DB_Shipment).filter(DB_Shipment.status == "3").count()
    completed = db.query(DB_Shipment).filter(DB_Shipment.status.in_(["4", "5"])).count()

    return {
        "totalUsers": total_users,
        "hospitals": hospitals,
        "transporters": transporters,
        "totalShipments": total_shipments,
        "totalBids": total_bids,
        "shipmentBreakdown": {
            "bidding": bidding,
            "inTransit": in_transit,
            "delivered": delivered,
            "completed": completed
        }
    }


# ── AUDIT LOG (all shipments with details) ────────────────────────
@router.get("/audit")
async def audit_log(
    admin: User = Depends(RoleChecker(["admin"])),
    db: Session = Depends(get_db)
):
    """Admin-only: full audit trail of all shipments and their current states."""
    shipments = db.query(DB_Shipment).order_by(DB_Shipment.created_at.desc()).all()
    result = []
    for s in shipments:
        hospital = db.query(User).filter(User.id == s.hospital_id).first()
        transporter = db.query(User).filter(User.id == s.transporter_id).first()
        result.append({
            "shipmentId": s.id,
            "hospital": hospital.name if hospital else "Unknown",
            "transporter": transporter.name if transporter else "Unassigned",
            "route": f"{s.pickup_location} → {s.delivery_location}",
            "cargoType": s.cargo_type,
            "escrowAmount": s.escrow_amount,
            "status": s.status,
            "createdAt": str(s.created_at) if s.created_at else None
        })
    return result
