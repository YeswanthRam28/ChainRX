# ChainRX: Blockchain-Powered Medical Logistics 🏥📦

ChainRX is a decentralized logistics platform designed to optimize medical resource transport with immutable transparency, secure escrow payments, and real-time tracking.

## 🚀 Key Features

- **Escrow-Based Payments**: Funds are locked in a smart contract and released only upon successful delivery confirmation.
- **Relayer Pattern**: Backend handles gas fees and transaction signing for a seamless user experience.
- **QR Handshake**: Secure pickup and delivery verification using encrypted QR codes.
- **Real-time GPS Tracking**: Monitor shipments in real-time via WebSockets and OpenStreetMap.
- **Decentralized DAO Community**: Dedicated governance forums for Hospitals and Transporters to vote on platform proposals.
- **Role-Based Access**: Specialized dashboards for Hospitals (Senders/Receivers) and Transporters.
- **IPFS Proof-of-Delivery**: Immutable photo evidence stored on IPFS via Pinata.

## 🛠️ Tech Stack

**Frontend (Mobile)**: Kotlin, Android SDK, Material 3, OSMDroid, ZXing QR.
**Backend (Relayer)**: FastAPI (Python), Web3.py, SQLAlchemy, NeonDB (PostgreSQL), Java-WebSocket.
**Blockchain**: Solidity (Contract), Hardhat, Shardeum/Polygon Testnet.
**Storage**: IPFS (via Pinata).

## 📁 Project Structure

```text
ChainRX/
├── app/               # Android Application (Kotlin)
├── Backend/           # FastAPI Relayer Server (Python)
├── Blockchain/        # Smart Contracts & Deployment (Solidity)
└── README.md          # Project Documentation
```

## ⚙️ Setup & Installation

### 1. Smart Contract
1. Go to `Blockchain/`
2. `npm install`
3. Create `.env` with `POLYGON_RPC_URL` and `PRIVATE_KEY`
4. Deploy: `npx hardhat run scripts/deploy.js --network shardeum`

### 2. Backend Relayer
1. Go to `Backend/`
2. `pip install -r requirements.txt`
3. Create `.env` with:
   - `NEON_DATABASE_URL`
   - `CONTRACT_ADDRESS`
   - `PRIVATE_KEY`
   - `PINATA_API_KEY` / `PINATA_SECRET_KEY`
4. Run: `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`

### 3. Android App
1. Open `app/` in Android Studio.
2. Update `BASE_URL` in `ChainRxApi.kt` to point to your backend IP.
3. Build and run on emulator or physical device.

## 📜 Smart Contracts

- **Shipment Contract**: `0x3ce4d1cFB16C3B50eaa594B20b13AA28729E671b` (Shardeum)
- **Community DAO**: `0xB8a97FFfaF22D16f01b96d8BE7b1Aa5441608e1A`

## ⚖️ License
MIT License
