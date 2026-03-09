package com.example.chainrx

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.network.ChainRxViewModel

class BidActivity : AppCompatActivity() {

    private val viewModel: ChainRxViewModel by viewModels()
    private var shipmentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bid)

        shipmentId = intent.getIntExtra("shipment_id", -1)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val tvId = findViewById<TextView>(R.id.tvShipmentId)
        val tvPickup = findViewById<TextView>(R.id.tvPickup)
        val tvDelivery = findViewById<TextView>(R.id.tvDelivery)
        val etAmount = findViewById<EditText>(R.id.etBidAmount)
        val etTime = findViewById<EditText>(R.id.etTime)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitBid)

        tvId.text = "Shipment #$shipmentId"

        btnSubmit.setOnClickListener {
            val amountText = etAmount.text.toString()
            val time = etTime.text.toString()

            if (amountText.isEmpty()) {
                Toast.makeText(this, "Enter a bid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (time.isEmpty()) {
                Toast.makeText(this, "Enter a delivery time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toLongOrNull() ?: 0L
            if (amount <= 0) {
                Toast.makeText(this, "Bid must be greater than 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."
            viewModel.submitBid(shipmentId, amount, time)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.currentShipment.collect { shipment ->
                shipment?.let {
                    tvPickup.text = it.pickupLocation
                    tvDelivery.text = it.deliveryLocation
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.statusMessage.collect { msg ->
                if (msg.isNotEmpty()) {
                    Toast.makeText(this@BidActivity, msg, Toast.LENGTH_SHORT).show()

                    if (msg.contains("Bid Submitted", ignoreCase = true)) {
                        finish()
                    } else {
                        // Re-enable button on error
                        btnSubmit.isEnabled = true
                        btnSubmit.text = "Submit Bid"
                    }
                }
            }
        }

        viewModel.fetchShipment(shipmentId)
    }
}
