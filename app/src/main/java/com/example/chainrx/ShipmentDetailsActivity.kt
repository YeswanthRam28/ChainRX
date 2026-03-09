package com.example.chainrx

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.auth.AuthManager
import com.example.chainrx.network.ChainRxViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject

class ShipmentDetailsActivity : AppCompatActivity() {

    private val viewModel: ChainRxViewModel by viewModels()
    private var shipmentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipment_details)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        shipmentId = intent.getIntExtra("shipment_id", -1)
        if (shipmentId == -1) finish()

        val role = AuthManager.getRole(this) ?: "hospital"

        val tvInfo = findViewById<TextView>(R.id.tvDetailInfo)
        val tvStatus = findViewById<TextView>(R.id.tvDetailStatus)
        val btnBid = findViewById<Button>(R.id.btnSubmitBid)
        val etBid = findViewById<EditText>(R.id.etBidAmount)
        val btnEscrow = findViewById<Button>(R.id.btnDepositEscrow)
        val btnPickup = findViewById<Button>(R.id.btnConfirmPickup)
        val btnDelivery = findViewById<Button>(R.id.btnConfirmDelivery)
        val btnRelease = findViewById<Button>(R.id.btnReleasePayment)

        // Hide everything initially
        btnBid.visibility = View.GONE
        etBid.visibility = View.GONE
        btnEscrow.visibility = View.GONE
        btnPickup.visibility = View.GONE
        btnDelivery.visibility = View.GONE
        btnRelease.visibility = View.GONE
        
        val btnTracking = findViewById<Button>(R.id.btnLiveTracking)
        btnTracking.visibility = View.GONE

        val btnShowQR = findViewById<Button>(R.id.btnShowQR)
        val btnScanQR = findViewById<Button>(R.id.btnScanQR)
        btnShowQR.visibility = View.GONE
        btnScanQR.visibility = View.GONE

        // Click listeners
        btnBid.setOnClickListener {
            val intent = Intent(this, BidActivity::class.java)
            intent.putExtra("shipment_id", shipmentId)
            startActivity(intent)
        }

        btnEscrow.setOnClickListener {
            btnEscrow.isEnabled = false
            btnEscrow.text = "Processing..."
            viewModel.depositEscrow(shipmentId)
        }

        btnPickup.setOnClickListener {
            val intent = Intent(this, PickupActivity::class.java)
            intent.putExtra("shipment_id", shipmentId)
            startActivity(intent)
        }

        btnDelivery.setOnClickListener {
            val intent = Intent(this, DeliveryActivity::class.java)
            intent.putExtra("shipment_id", shipmentId)
            startActivity(intent)
        }

        btnRelease.setOnClickListener {
            btnRelease.isEnabled = false
            btnRelease.text = "Processing..."
            viewModel.releasePayment(shipmentId)
        }

        btnTracking.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java)
            intent.putExtra("shipment_id", shipmentId)
            startActivity(intent)
        }

        // ── QR ACTION HANDLERS ──────────────────────────────
        val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObj = JSONObject(result.contents)
                    val scannedId = jsonObj.getInt("id")
                    if (scannedId != shipmentId) {
                        Toast.makeText(this, "QR Code Mismatch!", Toast.LENGTH_LONG).show()
                        return@registerForActivityResult
                    }

                    val roleNow = AuthManager.getRole(this)
                    val shipment = viewModel.currentShipment.value
                    
                    if (roleNow == "transport" && shipment?.status == "0") {
                        // Transporter scanning Hospital's QR for Pickup
                        val intent = Intent(this, PickupActivity::class.java)
                        intent.putExtra("shipment_id", shipmentId)
                        startActivity(intent)
                    } else if (roleNow == "hospital" && shipment?.status == "2") {
                        // Hospital scanning Transporter's QR for Delivery
                        val intent = Intent(this, DeliveryActivity::class.java)
                        intent.putExtra("shipment_id", shipmentId)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid QR Format", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnShowQR.setOnClickListener {
            val shipment = viewModel.currentShipment.value ?: return@setOnClickListener
            val qrContent = JSONObject().apply {
                put("id", shipment.id)
                put("pickup", shipment.pickupLocation)
                put("delivery", shipment.deliveryLocation)
                put("cargo", shipment.cargoType)
                put("type", if (role == "hospital") "PICKUP_AUTHORIZATION" else "DELIVERY_VERIFICATION")
            }.toString()

            val intent = Intent(this, QRCodeActivity::class.java)
            intent.putExtra("qr_content", qrContent)
            intent.putExtra("qr_title", if (role == "hospital") "Pickup QR" else "Delivery QR")
            startActivity(intent)
        }

        btnScanQR.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan Shipment QR")
                setCameraId(0)
                setBeepEnabled(true)
                setBarcodeImageEnabled(true)
            }
            barcodeLauncher.launch(options)
        }

        // ── OBSERVE SHIPMENT ─────────────────────────────────
        lifecycleScope.launchWhenStarted {
            viewModel.currentShipment.collect { shipment ->
                shipment?.let {
                    /*
                     * Smart Contract Status Flow:
                     * 0 = PENDING   (transporter selected, awaiting escrow deposit)
                     * 1 = BIDDING   (shipment created, open for bids)
                     * 2 = IN_TRANSIT (pickup confirmed)
                     * 3 = DELIVERED (delivery confirmed, awaiting payment release)
                     * 4 = COMPLETED (payment released)
                     */
                    val statusLabel = when (it.status) {
                        "0" -> "⏳ AWAITING ESCROW"
                        "1" -> "📋 OPEN FOR BIDS"
                        "2" -> "🚛 IN TRANSIT"
                        "3" -> "📦 DELIVERED"
                        "4" -> "✅ COMPLETED"
                        else -> "UNKNOWN"
                    }

                    tvInfo.text = """
                        Shipment #${it.id}
                        Route: ${it.pickupLocation} → ${it.deliveryLocation}
                        Cargo: ${it.cargoType}
                        Payment: ${it.paymentAmount} SHM
                        Status: $statusLabel
                        Transporter: ${it.transporter ?: "Not Assigned"}
                    """.trimIndent()

                    // ── ROLE + STATUS BUTTON LOGIC ──────────────
                    when (role) {
                        "hospital" -> {
                            // Status 1 (BIDDING) → Hospital sees "View Bids" to select transporter
                            btnBid.text = "📋 View Bids & Select Transporter"
                            btnBid.visibility = if (it.status == "1") View.VISIBLE else View.GONE

                            // Status 0 (PENDING) → Hospital deposits escrow
                            btnEscrow.text = "💰 Deposit Escrow"
                            btnEscrow.visibility = if (it.status == "0") View.VISIBLE else View.GONE

                            // Status 3 (DELIVERED) → Hospital releases payment
                            btnRelease.text = "✅ Release Payment"
                            btnRelease.visibility = if (it.status == "3") View.VISIBLE else View.GONE
                            
                            // QR Logic for Hospital
                            // Status 0 (Awaiting Escrow/Pickup) -> Show QR for Transporter
                            btnShowQR.visibility = if (it.status == "0") View.VISIBLE else View.GONE
                            btnShowQR.text = "📱 Show Pickup QR"
                            
                            // Status 2 (IN_TRANSIT) -> Scan Transporter's device
                            btnScanQR.visibility = if (it.status == "2") View.VISIBLE else View.GONE
                            btnScanQR.text = "📷 Scan Delivery QR"
                            
                            // Tracking visibility for Hospital
                            btnTracking.visibility = if (it.status == "2") View.VISIBLE else View.GONE
                        }
                        "transport" -> {
                            // Status 1 (BIDDING) → Transporter can place bid
                            btnBid.text = "📋 Place Bid"
                            btnBid.visibility = if (it.status == "1") View.VISIBLE else View.GONE

                            // Status 0 (PENDING, escrow deposited) → Transporter confirms pickup
                            btnPickup.text = "🚛 Confirm Pickup"
                            // If Transporter is assigned, they must SCAN first
                            btnScanQR.visibility = if (it.status == "0" && it.transporter != null) View.VISIBLE else View.GONE
                            btnScanQR.text = "📷 Scan Pickup QR"
                            
                            // Simple button for manual if needed, but we prefer Scan
                            btnPickup.visibility = View.GONE 

                            // Status 2 (IN_TRANSIT) → Transporter confirms delivery
                            btnDelivery.text = "📦 Confirm Delivery"
                            btnShowQR.visibility = if (it.status == "2") View.VISIBLE else View.GONE
                            btnShowQR.text = "📱 Show Delivery QR"
                            
                            btnDelivery.visibility = View.GONE
                            
                            // Tracking visibility for Transporter
                            btnTracking.visibility = if (it.status == "2" || (it.status == "0" && it.transporter != null)) View.VISIBLE else View.GONE
                        }
                        "admin" -> {
                            // Admin sees all relevant buttons
                            btnBid.text = "📋 View Bids"
                            btnBid.visibility = if (it.status == "1") View.VISIBLE else View.GONE
                            btnEscrow.visibility = if (it.status == "0") View.VISIBLE else View.GONE
                            btnPickup.visibility = if (it.status == "0" && it.transporter != null) View.VISIBLE else View.GONE
                            btnDelivery.visibility = if (it.status == "2") View.VISIBLE else View.GONE
                            btnRelease.visibility = if (it.status == "3") View.VISIBLE else View.GONE
                        }
                    }

                    // For Hospital: if BIDDING, auto-fetch bids and show bid list
                    if (role == "hospital" && it.status == "1") {
                        viewModel.fetchBids(shipmentId)
                    }
                }
            }
        }

        // ── OBSERVE BIDS (Hospital sees them under the shipment) ──
        lifecycleScope.launchWhenStarted {
            viewModel.bids.collect { bids ->
                if (role == "hospital" && bids.isNotEmpty()) {
                    // Dynamically show bids with accept buttons
                    val container = findViewById<LinearLayout>(R.id.bidContainer)
                    if (container != null) {
                        container.removeAllViews()
                        container.visibility = View.VISIBLE

                        val header = TextView(this@ShipmentDetailsActivity).apply {
                            text = "━━━ Bids Received ━━━"
                            textSize = 16f
                            setPadding(0, 24, 0, 12)
                        }
                        container.addView(header)

                        bids.forEach { bid ->
                            val bidView = LinearLayout(this@ShipmentDetailsActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(0, 8, 0, 8)
                            }

                            val bidText = TextView(this@ShipmentDetailsActivity).apply {
                                text = "${bid.bidder ?: "Transporter"} — ${bid.amount} SHM, ${bid.deliveryTime}"
                                textSize = 14f
                                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            }

                            val acceptBtn = Button(this@ShipmentDetailsActivity).apply {
                                text = "Accept"
                                setOnClickListener {
                                    isEnabled = false
                                    text = "Accepting..."
                                    viewModel.acceptBid(shipmentId, bid.id)
                                }
                            }

                            bidView.addView(bidText)
                            bidView.addView(acceptBtn)
                            container.addView(bidView)
                        }
                    }
                }
            }
        }

        // ── STATUS MESSAGES ──────────────────────────────────
        lifecycleScope.launchWhenStarted {
            viewModel.statusMessage.collect { msg ->
                if (msg.isNotEmpty()) {
                    tvStatus.text = msg
                    Toast.makeText(this@ShipmentDetailsActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fetchShipment(shipmentId)
    }

    override fun onResume() {
        super.onResume()
        if (shipmentId != -1) viewModel.fetchShipment(shipmentId)
    }
}
