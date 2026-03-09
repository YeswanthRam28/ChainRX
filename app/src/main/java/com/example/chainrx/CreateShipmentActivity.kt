package com.example.chainrx

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.chainrx.network.ChainRxViewModel
import kotlinx.coroutines.launch

class CreateShipmentActivity : AppCompatActivity() {

    private val viewModel: ChainRxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_shipment)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val etPickup = findViewById<EditText>(R.id.etPickup)
        val etDelivery = findViewById<EditText>(R.id.etDelivery)
        val etCargo = findViewById<EditText>(R.id.etCargo)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnCreate = findViewById<Button>(R.id.btnCreate)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnCreate.setOnClickListener {
            val pickup = etPickup.text.toString()
            val delivery = etDelivery.text.toString()
            val cargo = etCargo.text.toString()
            val amountStr = etAmount.text.toString()

            if (pickup.isNotEmpty() && delivery.isNotEmpty() && cargo.isNotEmpty() && amountStr.isNotEmpty()) {
                val amount = amountStr.toLong()
                viewModel.createShipment(pickup, delivery, cargo, amount)
            } else {
                tvStatus.text = "Please fill all fields"
            }
        }

        // Observe ViewModel state
        lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                btnCreate.isEnabled = !isLoading
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.statusMessage.collect { message ->
                tvStatus.text = message
            }
        }
    }
}
