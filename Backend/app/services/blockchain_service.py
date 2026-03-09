from app.blockchain.web3_connector import Web3Connector
from app.models.schemas import ShipmentCreate, BidSubmit, TransporterSelect
from app.services.database_service import DatabaseService
from fastapi import HTTPException
from typing import Optional
from app.models.db_models import User

class BlockchainService:
    def __init__(self, web3_connector: Web3Connector, db_service: Optional[DatabaseService] = None):
        self.web3 = web3_connector
        self.contract = self.web3.get_contract()
        self.db = db_service

    def create_shipment(self, shipment: ShipmentCreate, hospital_id: int):
        """Creates a shipment on the blockchain."""
        try:
            function_call = self.contract.functions.createShipment(
                shipment.pickupLocation,
                shipment.deliveryLocation,
                shipment.cargoType,
                self.web3.w3.to_wei(shipment.paymentAmount, 'ether')
            )
            tx_hash = self.web3.sign_and_send_transaction(function_call)
            
            # Wait for receipt to get the shipment ID from events
            receipt = self.web3.w3.eth.wait_for_transaction_receipt(tx_hash)
            
            # Extract logs for ShipmentCreated event
            event_logs = self.contract.events.ShipmentCreated().process_receipt(receipt)
            shipment_id = None
            if event_logs:
                shipment_id = event_logs[0]['args']['shipmentId']
            
            # Sync to DB for dashboard filters (hospital_id mapping)
            if shipment_id is not None:
                # Use override to ensure it's saved to the correct hospital user
                self.get_shipment(shipment_id, hospital_id_override=hospital_id)
                
            return {"tx_hash": tx_hash, "shipmentId": shipment_id}
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    def submit_bid(self, bid: BidSubmit):
        """Submits a transporter bid on-chain."""
        try:
            bid_amount = bid.get_amount()
            function_call = self.contract.functions.submitBid(
                bid.shipmentId,
                self.web3.w3.to_wei(bid_amount, 'ether'),
                bid.deliveryTime
            )
            tx_hash = self.web3.sign_and_send_transaction(function_call)
                
            return {"tx_hash": tx_hash}
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    def select_transporter(self, selection: TransporterSelect):
        """Hospital picks a transporter on-chain."""
        try:
            function_call = self.contract.functions.selectTransporter(
                selection.shipmentId,
                selection.transporterAddress
            )
            tx_hash = self.web3.sign_and_send_transaction(function_call)
            
            # Wait for receipt and sync DB
            self.get_shipment(selection.shipmentId)
            
            return {"tx_hash": tx_hash}
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    def deposit_escrow(self, shipment_id: int):
        """Hospitals locks funds in escrow on-chain."""
        try:
            # Get amount from blockchain source of truth
            shipment_data = self.contract.functions.shipments(shipment_id).call()
            payment_amount = shipment_data[4]
            
            function_call = self.contract.functions.depositEscrow(shipment_id)
            tx_hash = self.web3.sign_and_send_transaction(function_call, value=payment_amount)
            
            # Sync update to DB
            self.get_shipment(shipment_id)
            
            return {"tx_hash": tx_hash}
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    def confirm_pickup(self, shipment_id: int):
        """Transporter confirms package receipt."""
        try:
            function_call = self.contract.functions.confirmPickup(shipment_id)
            tx_hash = self.web3.sign_and_send_transaction(function_call)
            
            # Sync update to DB
            self.get_shipment(shipment_id)
            
            return {"tx_hash": tx_hash}
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    def confirm_delivery(self, shipment_id: int, ipfs_hash: str):
        """Transporter confirms delivery on-chain."""
        try:
            function_call = self.contract.functions.confirmDelivery(shipment_id, ipfs_hash)
            tx_hash = self.web3.sign_and_send_transaction(function_call)
            
            # Sync update to DB
            self.get_shipment(shipment_id)
            
            return {"tx_hash": tx_hash}
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    def release_payment(self, shipment_id: int):
        """Hospital releases locked escrow to Transporter."""
        try:
            function_call = self.contract.functions.releasePayment(shipment_id)
            tx_hash = self.web3.sign_and_send_transaction(function_call)
            
            # Sync update to DB
            self.get_shipment(shipment_id)
            
            return {"tx_hash": tx_hash}
        except Exception as e:
            raise HTTPException(status_code=400, detail=str(e))

    def get_shipment(self, shipment_id: int, hospital_id_override: Optional[int] = None):
        """Returns single shipment data from Blockchain (Master Source)."""
        try:
            # ON-CHAIN QUERY
            data = self.contract.functions.shipments(shipment_id).call()
            
            creator_addr = data[0]
            transporter_addr = data[5]
            status = str(data[6])
            
            hospital_id = hospital_id_override
            transporter_id = None
            
            # Map addresses to DB User IDs for frontend convenience
            if self.db:
                # Only lookup hospital if not provided (overridden during creation)
                if hospital_id is None:
                    hospital_user = self.db.db.query(User).filter(User.wallet_address == creator_addr).first()
                    if hospital_user:
                        hospital_id = hospital_user.id
                
                if transporter_addr != "0x0000000000000000000000000000000000000000":
                    trans_user = self.db.db.query(User).filter(User.wallet_address == transporter_addr).first()
                    if trans_user:
                        transporter_id = trans_user.id

            shipment_dict = {
                "id": shipment_id,
                "hospital_id": hospital_id,
                "pickupLocation": data[1],
                "deliveryLocation": data[2],
                "cargoType": data[3],
                "paymentAmount": data[4],
                "transporter": transporter_id,
                "status": status
            }
            
            # Update DB (Caches the BC state, but doesn't override it)
            if self.db:
                self.db.sync_shipment({
                    "id": shipment_id,
                    "hospital_id": hospital_id,
                    "pickup_location": data[1],
                    "delivery_location": data[2],
                    "cargo_type": data[3],
                    "payment_amount": data[4],
                    "transporter": transporter_id,
                    "status": status
                })
                
            return shipment_dict
        except Exception as e:
            raise HTTPException(status_code=404, detail=f"Shipment #{shipment_id} not found on-chain: {e}")

    def get_shipments(self, hospital_id=None, transporter_id=None):
        """Returns shipments from Database (NeonDB) to allow user-managed deletions."""
        if self.db:
            return self.db.get_all_shipments(hospital_id, transporter_id)
        return []
