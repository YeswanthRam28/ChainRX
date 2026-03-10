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
            if (selectedShipments.size < 2) {
                Toast.makeText(this, "Select at least 2 locations", Toast.LENGTH_SHORT).show()
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
                // Filter for shipments where I am the transporter and it's active
                // For demo, we just show all deliveries assigned to me
                val myShipments = all.filter { it.status == "1" || it.status == "2" } 
                
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
        val addresses = selectedShipments.flatMap { listOf(it.pickupLocation, it.deliveryLocation) }.distinct()
        
        lifecycleScope.launch {
            try {
                val result = ApiClient.getService(this@RouteOptimizerActivity).optimizeRoute(RouteRequest(addresses))
                showResultDialog(result.optimized_order)
            } catch (e: Exception) {
                Toast.makeText(this@RouteOptimizerActivity, "Optimization failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loader.visibility = View.GONE
            }
        }
    }

    private fun showResultDialog(order: List<String>) {
        val message = StringBuilder("AI Optimized Sequence:\n\n")
        order.forEachIndexed { index, s ->
            message.append("${index + 1}. $s\n")
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
            holder.title.text = "Shipment #${item.id} - ${item.cargoType}"
            holder.route.text = "${item.pickupLocation} → ${item.deliveryLocation}"
            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                onSelect(item, isChecked)
            }
        }

        override fun getItemCount() = list.size
    }
}
