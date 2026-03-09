from sqlalchemy.orm import Session
from app.models.db_models import DB_Shipment, DB_Bid
from typing import Optional

class DatabaseService:
    def __init__(self, db: Session):
        self.db = db

    def sync_shipment(self, shipment_data: dict):
        try:
            # SQLAlchemy upsert
            db_item = self.db.query(DB_Shipment).filter(DB_Shipment.id == shipment_data['id']).first()
            if db_item:
                db_item.pickup_location = shipment_data['pickup_location']
                db_item.delivery_location = shipment_data['delivery_location']
                db_item.cargo_type = shipment_data['cargo_type']
                db_item.escrow_amount = shipment_data['payment_amount']
                db_item.transporter_id = shipment_data.get('transporter')
                db_item.status = shipment_data['status']
            else:
                new_item = DB_Shipment(
                    id=shipment_data['id'],
                    hospital_id=shipment_data.get('hospital_id'),
                    pickup_location=shipment_data['pickup_location'],
                    delivery_location=shipment_data['delivery_location'],
                    cargo_type=shipment_data['cargo_type'],
                    escrow_amount=shipment_data['payment_amount'],
                    transporter_id=shipment_data.get('transporter'),
                    status=shipment_data['status']
                )
                self.db.add(new_item)
            self.db.commit()
        except Exception as e:
            self.db.rollback()
            print(f"NeonDB Sync Error: {e}")

    def get_all_shipments(self, hospital_id=None, transporter_id=None):
        try:
            query = self.db.query(DB_Shipment)
            if hospital_id:
                query = query.filter(DB_Shipment.hospital_id == hospital_id)
            if transporter_id:
                query = query.filter(DB_Shipment.transporter_id == transporter_id)
                
            results = query.all()
            return [
                {
                    "id": item.id,
                    "pickupLocation": item.pickup_location,
                    "deliveryLocation": item.delivery_location,
                    "cargoType": item.cargo_type,
                    "paymentAmount": item.escrow_amount, # Match DB column name
                    "transporter": item.transporter_id,
                    "status": item.status
                } for item in results
            ]
        except Exception as e:
            print(f"NeonDB Query Error: {e}")
            return []

    def save_bid(self, bid_data: dict):
        try:
            new_bid = DB_Bid(
                shipment_id=bid_data['shipment_id'],
                transporter_id=bid_data.get('transporter_id'),
                bidder=bid_data.get('bidder', ''),
                bid_amount=bid_data['amount'],
                estimated_time=bid_data['delivery_time']
            )
            self.db.add(new_bid)
            self.db.commit()
        except Exception as e:
            self.db.rollback()
            print(f"NeonDB Bid Save Error: {e}")

    def update_shipment_status(self, shipment_id: int, status: str, transporter_id: Optional[int] = None):
        """Directly update DB status (used for mock mode/sync)."""
        try:
            db_item = self.db.query(DB_Shipment).filter(DB_Shipment.id == shipment_id).first()
            if db_item:
                db_item.status = status
                if transporter_id is not None:
                    db_item.transporter_id = transporter_id
                self.db.commit()
                return True
        except Exception as e:
            self.db.rollback()
            print(f"NeonDB Update Error: {e}")
        return False
