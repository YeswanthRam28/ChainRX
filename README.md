# ChainRX: Blockchain-Powered Medical Cold-Chain Logistics

![ChainRX Logo](https://img.shields.io/badge/ChainRX-Logistics-teal)

ChainRX is a decentralized application designed to transform medical logistics. It leverages the **Shardeum Blockchain** (Mezame Testnet) to ensure transparency, accountability, and security in the transportation of pharmaceutical goods.

## 🚀 Key Features

- **Decentralized Escrow**: Payments are locked in a smart contract and only released after delivery confirmation.
- **Relayer Pattern**: Backend handles gas fees and transaction signing for a seamless user experience.
- **QR Code Verification**: Secure handshakes during pickup and delivery via encrypted QR codes.
- **Live Real-time Tracking**: Monitor transporter movements in real-time via WebSockets and Google Maps.
- **Role-Based Access**: Specialized dashboards for Hospitals (Senders/Receivers) and Transporters.
- **IPFS Proof-of-Delivery**: Immutable photo evidence stored on IPFS via Pinata.

## 🛠 Tech Stack

- **Android**: Kotlin, Material 3, Google Maps SDK, ZXing (QR), WebSockets.
- **Backend**: FastAPI (Python), SQLAlchemy, Web3.py, NeonDB (PostgreSQL).
- **Blockchain**: Solidity (Smart Contracts), Shardeum Mezame Testnet.
- **Storage**: IPFS (Pinata).

## 📂 Project Structure

```text
ChainRX/
├── app/                # Android Mobile Application (Kotlin)
├── Backend/            # FastAPI Python Server
└── Blockchain/         # Solidity Smart Contracts & Hardhat Config
```

## ⚙️ Getting Started

### 1. Smart Contract
- Navigate to `Blockchain/`
- Install dependencies: `npm install`
- Deploy to Shardeum: `npx hardhat run scripts/deploy.js --network shardeum`

### 2. Backend API
- Navigate to `Backend/`
- Install dependencies: `pip install -r requirements.txt`
- Configure `.env` (RPC URL, Contract Address, Private Key, Pinata Keys)
- Run server: `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`

### 3. Android App
- Open the `ChainRX` root folder in Android Studio.
- Add your Google Maps API Key to `app/src/main/AndroidManifest.xml`.
- Update `ApiClient.BASE_URL` in `app/src/main/java/com/example/chainrx/network/ChainRxApi.kt` to your server's IP.
- Build and run on an Android device/emulator.

## 📜 Smart Contract Status
- **Network**: Shardeum Mezame
- **Contract Address**: `0x3ce4d1cFB16C3B50eaa594B20b13AA28729E671b`

## 🔒 Security
- Private keys and sensitive credentials must be stored in `.env` files (excluded from Git).
- Use `Relayer` pattern to prevent unauthorized contract calls.

---
Built with ❤️ for secure healthcare logistics.
