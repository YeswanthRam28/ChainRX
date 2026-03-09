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
