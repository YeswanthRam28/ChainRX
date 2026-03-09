// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * @title ChainRX: Healthcare Logistics Smart Contract
 * @dev Handles shipments, bidding, escrow, and milestone confirmations on the blockchain.
 */
contract ChainRX {
    enum ShipmentStatus { PENDING, BIDDING, IN_TRANSIT, DELIVERED, COMPLETED, CANCELLED }

    struct Bid {
        address transporter;
        uint256 amount;
        string deliveryTime;
    }

    struct Shipment {
        address creator;
        string pickupLocation;
        string deliveryLocation;
        string cargoType;
        uint256 paymentAmount;
        address transporter;
        ShipmentStatus status;
        string deliveryProofIPFS;
        bool isEscrowDeposited;
        uint256 bidCount;
    }

    mapping(uint256 => Shipment) public shipments;
    mapping(uint256 => mapping(uint256 => Bid)) public shipmentBids;
    uint256 public shipmentCounter;

    event ShipmentCreated(uint256 indexed shipmentId, address creator, string cargoType);
    event BidSubmitted(uint256 indexed shipmentId, address transporter, uint256 amount);
    event TransporterSelected(uint256 indexed shipmentId, address transporter);
    event EscrowDeposited(uint256 indexed shipmentId, uint256 amount);
    event PickupConfirmed(uint256 indexed shipmentId);
    event DeliveryConfirmed(uint256 indexed shipmentId, string ipfsHash);
    event PaymentReleased(uint256 indexed shipmentId, address transporter, uint256 amount);

    address public owner;
    
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this");
        _;
    }

    modifier onlyRelayerOrCreator(uint256 _shipmentId) {
        require(msg.sender == shipments[_shipmentId].creator || msg.sender == owner, "Not creator or relayer");
        _;
    }

    modifier onlyRelayerOrTransporter(uint256 _shipmentId) {
        require(msg.sender == shipments[_shipmentId].transporter || msg.sender == owner, "Not transporter or relayer");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    /**
     * @dev Create a new shipment request
     */
    function createShipment(
        string memory _pickupLocation,
        string memory _deliveryLocation,
        string memory _cargoType,
        uint256 _paymentAmount
    ) public returns (uint256) {
        shipmentCounter++;
        shipments[shipmentCounter] = Shipment({
            creator: msg.sender,
            pickupLocation: _pickupLocation,
            deliveryLocation: _deliveryLocation,
            cargoType: _cargoType,
            paymentAmount: _paymentAmount,
            transporter: address(0),
            status: ShipmentStatus.BIDDING,
            deliveryProofIPFS: "",
            isEscrowDeposited: false,
            bidCount: 0
        });

        emit ShipmentCreated(shipmentCounter, msg.sender, _cargoType);
        return shipmentCounter;
    }

    /**
     * @dev Transporters submit bids for a shipment
     */
    function submitBid(uint256 _shipmentId, uint256 _bidAmount, string memory _deliveryTime) public {
        Shipment storage s = shipments[_shipmentId];
        require(s.status == ShipmentStatus.BIDDING, "Shipment is not open for bidding");
        require(_bidAmount > 0, "Bid must be greater than zero");

        s.bidCount++;
        shipmentBids[_shipmentId][s.bidCount] = Bid({
            transporter: msg.sender,
            amount: _bidAmount,
            deliveryTime: _deliveryTime
        });

        emit BidSubmitted(_shipmentId, msg.sender, _bidAmount);
    }

    /**
     * @dev Creator selects a transporter from the bids
     */
    function selectTransporter(uint256 _shipmentId, address _transporterAddress) public onlyRelayerOrCreator(_shipmentId) {
        Shipment storage s = shipments[_shipmentId];
        require(s.status == ShipmentStatus.BIDDING, "Transporter already selected or shipment closed");
        
        s.transporter = _transporterAddress;
        s.status = ShipmentStatus.PENDING;

        emit TransporterSelected(_shipmentId, _transporterAddress);
    }

    /**
     * @dev Creator deposits the payment amount into the contract escrow
     */
    function depositEscrow(uint256 _shipmentId) public payable onlyRelayerOrCreator(_shipmentId) {
        Shipment storage s = shipments[_shipmentId];
        require(s.status == ShipmentStatus.PENDING, "Cannot deposit escrow at this stage");
        require(msg.value == s.paymentAmount, "Incorrect payment amount");
        require(s.transporter != address(0), "No transporter selected");

        s.isEscrowDeposited = true;
        
        emit EscrowDeposited(_shipmentId, msg.value);
    }

    /**
     * @dev Transporter confirms picking up the cargo
     */
    function confirmPickup(uint256 _shipmentId) public onlyRelayerOrTransporter(_shipmentId) {
        Shipment storage s = shipments[_shipmentId];
        require(s.isEscrowDeposited, "Escrow must be deposited before pickup");
        require(s.status == ShipmentStatus.PENDING, "Status error");

        s.status = ShipmentStatus.IN_TRANSIT;
        emit PickupConfirmed(_shipmentId);
    }

    /**
     * @dev Transporter confirms delivery and uploads IPFS proof
     */
    function confirmDelivery(uint256 _shipmentId, string memory _ipfsHash) public onlyRelayerOrTransporter(_shipmentId) {
        Shipment storage s = shipments[_shipmentId];
        require(s.status == ShipmentStatus.IN_TRANSIT, "Must be in transit to confirm delivery");

        s.deliveryProofIPFS = _ipfsHash;
        s.status = ShipmentStatus.DELIVERED;

        emit DeliveryConfirmed(_shipmentId, _ipfsHash);
    }

    /**
     * @dev Creator releases payment from escrow to the transporter
     */
    function releasePayment(uint256 _shipmentId) public onlyRelayerOrCreator(_shipmentId) {
        Shipment storage s = shipments[_shipmentId];
        require(s.status == ShipmentStatus.DELIVERED, "Shipment not delivered yet");
        require(s.isEscrowDeposited, "No funds in escrow");

        uint256 payment = s.paymentAmount;
        address transporterAddr = s.transporter;

        s.status = ShipmentStatus.COMPLETED;
        s.isEscrowDeposited = false;

        (bool success, ) = payable(transporterAddr).call{value: payment}("");
        require(success, "Payment transfer failed");

        emit PaymentReleased(_shipmentId, transporterAddr, payment);
    }

    /**
     * @dev Get basic shipment details
     */
    function getShipment(uint256 _id) public view returns (
        string memory pickupLocation,
        string memory deliveryLocation,
        string memory cargoType,
        uint256 paymentAmount,
        address transporter,
        ShipmentStatus status
    ) {
        Shipment memory s = shipments[_id];
        return (
            s.pickupLocation,
            s.deliveryLocation,
            s.cargoType,
            s.paymentAmount,
            s.transporter,
            s.status
        );
    }
}
