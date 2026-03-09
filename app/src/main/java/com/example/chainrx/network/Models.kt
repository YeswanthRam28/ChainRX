package com.example.chainrx.network

import com.google.gson.annotations.SerializedName

data class Shipment(
    val id: Int,
    val pickupLocation: String,
    val deliveryLocation: String,
    val cargoType: String,
    val paymentAmount: Long,
    val transporter: String?,
    val status: String
)

typealias ShipmentResponse = Shipment

data class ShipmentCreate(
    val pickupLocation: String,
    val deliveryLocation: String,
    val cargoType: String,
    val paymentAmount: Long
)

data class Bid(
    val id: Int,
    val shipmentId: Int? = null,
    val transporterId: Int? = null,
    val bidder: String? = null,
    val amount: Long,
    val deliveryTime: String
)

data class BidSubmit(
    val shipmentId: Int,
    val bidAmount: Long,
    val deliveryTime: String
)

data class TransporterSelect(
    val shipmentId: Int,
    val transporterAddress: String
)

data class AuthResponse(
    val access_token: String,
    val token_type: String,
    val role: String
)

data class TransactionResponse(
    @SerializedName("tx_hash")
    val txHash: String
)

// DAO Models
data class Proposal(
    val id: Int,
    val proposer: String,
    val title: String,
    val description: String,
    val votesFor: Int,
    val votesAgainst: Int,
    val community: String,
    val endTime: Long,
    val hasVoted: Boolean
)

data class ProposalCreate(
    val title: String,
    val description: String,
    val community: Int,
    val durationDays: Int = 3
)

data class ProposalVote(
    val proposalId: Int,
    val support: Boolean
)
