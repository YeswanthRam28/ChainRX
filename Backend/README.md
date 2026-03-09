# ChainRX Blockchain Backend - FastAPI

A Python-based FastAPI backend that interfaces with the Polygon Blockchain and IPFS (Pinata) for a healthcare logistics platform.

## Setup Instructions

1.  **Environment Setup**:
    *   Navigate to the `Backend` directory.
    *   Create a virtual environment: `python -m venv venv`.
    *   Activate it:
        *   Windows: `venv\Scripts\activate`
        *   macOS/Linux: `source venv/bin/activate`
    *   Install dependencies: `pip install -r requirements.txt`.

2.  **Configuration**:
    *   Copy `.env.example` to `.env`.
    *   Fill in your Polygon RPC URL (e.g., from Alchemy or Infura), your deployed contract address, and your private key.
    *   Add your Pinata API keys for IPFS integration.

3.  **Run the Server**:
    *   Execute: `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`.
    *   Check if it is running at `http://localhost:8000`.

## Example cURL Requests

### Create a Shipment
```bash
curl -X POST "http://localhost:8000/createShipment" \
     -H "Content-Type: application/json" \
     -d '{
           "pickupLocation": "Main Warehouse, Boston",
           "deliveryLocation": "General Hospital, NYC",
           "cargoType": "Insulin Vials",
           "paymentAmount": 10000000000000000
         }'
```

### Submit a Bid
```bash
curl -X POST "http://localhost:8000/submitBid" \
     -H "Content-Type: application/json" \
     -d '{
           "shipmentId": 1,
           "bidAmount": 9500000000000000,
           "deliveryTime": "12 hours"
         }'
```

### Select a Transporter
```bash
curl -X POST "http://localhost:8000/selectTransporter" \
     -H "Content-Type: application/json" \
     -d '{
           "shipmentId": 1,
           "transporterAddress": "0xTransporterWalletAddress"
         }'
```

### Confirm Delivery (with File Upload)
```bash
curl -X POST "http://localhost:8000/confirmDelivery" \
     -F "shipmentId=1" \
     -F "file=@/path/to/delivery_proof.jpg"
```

### Release Payment
```bash
curl -X POST "http://localhost:8000/releasePayment" \
     -F "shipmentId=1"
```
