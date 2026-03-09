package com.example.chainrx

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.network.ChainRxViewModel

class PickupActivity : AppCompatActivity() {

    private val viewModel: ChainRxViewModel by viewModels()
    private var shipmentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pickup)

        shipmentId = intent.getIntExtra("shipment_id", -1)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val tvId = findViewById<TextView>(R.id.tvShipmentId)
        val tvPickup = findViewById<TextView>(R.id.tvPickup)
        val btnAddPhoto = findViewById<Button>(R.id.btnAddPhoto)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmPickup)

        tvId.text = "Shipment #$shipmentId"

        btnConfirm.setOnClickListener {
            viewModel.confirmPickup(shipmentId)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.currentShipment.collect { shipment ->
                shipment?.let {
                    tvPickup.text = it.pickupLocation
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.statusMessage.collect {
                if (it.contains("Pickup Confirmed", ignoreCase = true)) {
                    finish()
                }
            }
        }

        viewModel.fetchShipment(shipmentId)
    }
}
