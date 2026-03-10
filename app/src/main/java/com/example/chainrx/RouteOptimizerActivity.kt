package com.example.chainrx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chainrx.auth.AuthManager
import com.example.chainrx.network.ApiClient
import com.example.chainrx.network.RouteRequest
import com.example.chainrx.network.Shipment
import kotlinx.coroutines.launch

class RouteOptimizerActivity : AppCompatActivity() {

    private lateinit var rvShipments: RecyclerView
    private lateinit var btnOptimize: Button
    private lateinit var loader: ProgressBar
    private val selectedShipments = mutableSetOf<Shipment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_optimizer)

        rvShipments = findViewById(R.id.rvShipments)
        btnOptimize = findViewById(R.id.btnOptimize)
        loader = findViewById(R.id.loader)

        rvShipments.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        btnOptimize.setOnClickListener {
            if (selectedShipments.size < 1) {
                Toast.makeText(this, "Select at least 1 shipment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            runOptimization()
        }

        loadShipments()
    }

    private fun loadShipments() {
        loader.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val all = ApiClient.getService(this@RouteOptimizerActivity).getShipments()
                // Filter for shipments assigned to me (In Transit or Bidding where I am transporter)
                // In a real app, we'd check transporter address
                val myShipments = all.filter { it.status == "2" } 
                
                if (myShipments.isEmpty()) {
                    Toast.makeText(this@RouteOptimizerActivity, "No active deliveries found to optimize.", Toast.LENGTH_LONG).show()
                }

                rvShipments.adapter = OptimizerAdapter(myShipments) { shipment, isSelected ->
                    if (isSelected) selectedShipments.add(shipment)
                    else selectedShipments.remove(shipment)
                }
            } catch (e: Exception) {
                Toast.makeText(this@RouteOptimizerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loader.visibility = View.GONE
            }
        }
    }

    private fun runOptimization() {
        loader.visibility = View.VISIBLE
        // Proper route optimization logic: Start from current location (optional) 
        // and visit all pickup/delivery points.
        // For simplicity, we optimize the sequence of all stop points.
        val addresses = mutableListOf<String>()
        selectedShipments.forEach {
            addresses.add(it.pickupLocation)
            addresses.add(it.deliveryLocation)
        }
        val distinctAddresses = addresses.distinct()
        
        lifecycleScope.launch {
            try {
                val result = ApiClient.getService(this@RouteOptimizerActivity).optimizeRoute(RouteRequest(distinctAddresses))
                showResultDialog(result.optimized_order)
            } catch (e: Exception) {
                // FALLBACK: If AI service is down, show a basic sequence
                showResultDialog(distinctAddresses)
                Toast.makeText(this@RouteOptimizerActivity, "AI Optimization offline. Showing default sequence.", Toast.LENGTH_SHORT).show()
            } finally {
                loader.visibility = View.GONE
            }
        }
    }

    private fun showResultDialog(order: List<String>) {
        val message = StringBuilder("AI Optimized Sequence:\n\n")
        order.forEachIndexed { index, s ->
            val icon = if (index == 0) "🚩 " else if (index == order.size - 1) "🏁 " else "📍 "
            message.append("$icon ${index + 1}. $s\n")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Optimal Route Found")
            .setMessage(message.toString())
            .setPositiveButton("Got it", null)
            .show()
    }

    inner class OptimizerAdapter(
        private val list: List<Shipment>,
        private val onSelect: (Shipment, Boolean) -> Unit
    ) : RecyclerView.Adapter<OptimizerAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title = v.findViewById<TextView>(R.id.tvTitle)
            val route = v.findViewById<TextView>(R.id.tvRoute)
            val checkbox = v.findViewById<CheckBox>(R.id.checkbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_optimizer_shipment, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.title.text = "Shipment #${item.id} (${item.cargoType})"
            holder.route.text = "${item.pickupLocation} ➔ ${item.deliveryLocation}"
            
            // Remove previous listener to avoid trigger on scroll
            holder.checkbox.setOnCheckedChangeListener(null)
            holder.checkbox.isChecked = selectedShipments.contains(item)
            
            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                onSelect(item, isChecked)
            }
            
            holder.itemView.setOnClickListener {
                holder.checkbox.toggle()
            }
        }

        override fun getItemCount() = list.size
    }
}
