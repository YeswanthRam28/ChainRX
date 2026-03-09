package com.example.chainrx

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.network.ChainRxViewModel
import java.io.File
import java.io.FileOutputStream

class DeliveryActivity : AppCompatActivity() {

    private val viewModel: ChainRxViewModel by viewModels()
    private var shipmentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery)

        shipmentId = intent.getIntExtra("shipment_id", -1)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val tvId = findViewById<TextView>(R.id.tvShipmentId)
        val tvDelivery = findViewById<TextView>(R.id.tvDelivery)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmDelivery)

        tvId.text = "Shipment #$shipmentId"

        val role = com.example.chainrx.auth.AuthManager.getRole(this) ?: "transport"
        if (role == "hospital") {
            btnConfirm.text = "Confirm Reception"
        }

        btnConfirm.setOnClickListener {
            // Create a dummy file for the proof (as verification demo)
            val dummyFile = File(cacheDir, "proof_$shipmentId.txt")
            FileOutputStream(dummyFile).use { it.write("Delivery Confirmed Verification".toByteArray()) }
            
            viewModel.confirmDelivery(shipmentId, dummyFile)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.currentShipment.collect { shipment ->
                shipment?.let {
                    tvDelivery.text = it.deliveryLocation
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.statusMessage.collect {
                if (it.contains("Delivery Confirmed", ignoreCase = true)) {
                    startActivity(Intent(this@DeliveryActivity, SuccessActivity::class.java))
                    finish()
                }
            }
        }

        viewModel.fetchShipment(shipmentId)
    }
}
