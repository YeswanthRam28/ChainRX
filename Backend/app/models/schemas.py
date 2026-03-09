from pydantic import BaseModel
from typing import Optional, List

class ShipmentCreate(BaseModel):
    pickupLocation: str
    deliveryLocation: str
    cargoType: str
    paymentAmount: int

class BidSubmit(BaseModel):
    shipmentId: int
    bidAmount: Optional[int] = None
    amount: Optional[int] = None
    deliveryTime: str
    
    def get_amount(self) -> int:
        """Return the bid amount, preferring bidAmount over amount."""
        return self.bidAmount or self.amount or 0

class TransporterSelect(BaseModel):
    shipmentId: int
    transporterAddress: str

class ShipmentInfo(BaseModel):
    id: int
    pickupLocation: str
    deliveryLocation: str
    cargoType: str
    paymentAmount: int
    transporter: Optional[str] = None
    status: str

class BidInfo(BaseModel):
    shipmentId: int
    bidder: str
    amount: int
    deliveryTime: str

# ── DAO SCHEMAS ───────────────────────────────────────────────────
class ProposalCreate(BaseModel):
    title: str
    description: str
    community: int # 0=GENERAL, 1=HOSPITAL, 2=TRANSPORT
    durationDays: int = 7

class ProposalVote(BaseModel):
    proposalId: int
    support: bool

class ProposalInfo(BaseModel):
    id: int
    proposer: str
    title: str
    description: str
    votesFor: int
    votesAgainst: int
    community: str
    endTime: int
    hasVoted: bool = False
