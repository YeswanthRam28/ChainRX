# ChainRX: Role-Based UI Features Guide 📱🏛️

ChainRX provides a tailored experience for different participants in the medical logistics ecosystem. Each role has a unique dashboard designed for their specific workflows.

---

## 🏥 1. Hospital Dashboard (Sender/Receiver)
The primary interface for healthcare facilities to manage medical shipments and governance.

### **Core UI Features:**
*   **Shipment Creation**: Integrated form to request a new medical transport (Pickup/Delivery locations, Cargo Type, Payment Amt).
*   **Bid Management**: View real-time bids from transporters. Buttons to **"View Bids"** and **"Accept Bid"** on live requests.
*   **Escrow Control**: Secure **"Deposit Escrow"** button to lock funds in the smart contract before pickup.
*   **Live Handshake (QR)**:
    *   **"Show QR"**: Generate a secure pickup code for the transporter to scan at the hospital.
    *   **"Scan QR"**: Scan the transporter's delivery code to confirm successful reception of goods.
*   **Release Payment**: One-click **"Release Payment"** button that triggers the smart contract to transfer locked funds to the transporter after delivery.
*   **Live Tracking**: Access real-time GPS movements of the assigned transporter via **OSMDroid**.

---

## 🚛 2. Transporter Dashboard
Optimized for logistics providers to find work and manage transit tasks.

### **Core UI Features:**
*   **Marketplace View**: List of all available shipments open for bidding.
*   **Bidding System**: **"Place Bid"** action to submit a quote (Amount + Estimated Delivery Time) directly to the blockchain.
*   **My Tasks**: Specialized list for shipments assigned to the transporter.
*   **Live Handshake (QR)**:
    *   **"Scan QR"**: Scan the hospital's pickup code to officially start the delivery (`IN_TRANSIT` status).
    *   **"Show QR"**: Display a delivery confirmation code to be scanned by the receiving hospital.
*   **Self-Broadcast Tracking**: While on active delivery, the **"Live Tracking"** screen automatically broadcasts the transporter's coordinates to the hospital.
*   **IPFS Proof**: Upload final photos or documents as proof-of-delivery, which are hashed and stored on **IPFS**.

---

## 🛡️ 3. Admin Dashboard
A high-level oversight panel for system monitoring and auditing.

### **Core UI Features:**
*   **Global Audit**: View every shipment in the system regardless of participating parties.
*   **Blockchain Verification**: Status badges showing the **Live On-Chain state** of every transaction (Escrowed, In Transit, Released).
*   **Reputation Monitoring**: Overview of user status and system health.
*   **Relayer Supervision**: Access to system-wide logs and transaction histories.

---

## 🗳️ 4. Decentralized Community (DAO)
Available to all registered users via the **Bottom Navigation Bar**.

### **Feature Set:**
*   **Role-Locked DAOs**:
    *   **General Pool**: Open to everyone for platform-wide ideas.
    *   **Hospital Council**: Only hospitals can vote/create proposals here.
    *   **Transport Union**: Only transporters can vote/create proposals here.
*   **Proposal Creation**: Floating Action Button to draft and broadcast new governance proposals.
*   **Transparent Voting**:
    *   **Live Pro-Con Count**: See tally of ✅ For and ❌ Against votes in real-time.
    *   **Voted Status**: Visual indicators to show if you have already participated in a specific proposal.
    *   **Expiry Countdown**: Automatic timer showing when the governance window closes.

---

## 🧭 Global Navigation
*   **Bottom Navigation**: Quick access to **Home**, **Shipments**, and **Community**.
*   **Real-time Alerts**: Toast notifications for successful blockchain syncs and error handling.
*   **Auth Badge**: Visual role indicator (e.g., "ROLE: HOSPITAL") at the top of the dashboard.
