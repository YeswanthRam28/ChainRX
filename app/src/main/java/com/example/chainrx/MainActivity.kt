package com.example.chainrx

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.chainrx.auth.AuthManager
import com.example.chainrx.network.ChainRxViewModel
import com.example.chainrx.network.ShipmentResponse

class MainActivity : AppCompatActivity() {

    private val viewModel: ChainRxViewModel by viewModels()
    private lateinit var adapter: ShipmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val role = AuthManager.getRole(this) ?: "hospital"
        val userName = AuthManager.getName(this) ?: "User"

        val rvShipments = findViewById<RecyclerView>(R.id.rvShipments)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val cvCreateShipment = findViewById<View>(R.id.cvCreateShipment)
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvRoleBadge = findViewById<TextView>(R.id.tvRoleBadge)
        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)

        // ── ROLE-AWARE DASHBOARD ──────────────────────────────
        when (role) {
            "hospital" -> {
                tvGreeting.text = "My Shipments"
                tvRoleBadge.text = "HOSPITAL"
                cvCreateShipment.visibility = View.VISIBLE
                // Tabs: Active / Past
            }
            "transport" -> {
                tvGreeting.text = "Marketplace"
                tvRoleBadge.text = "TRANSPORT"
                cvCreateShipment.visibility = View.GONE // Transport can't create shipments
                // Change tab labels
                tabLayout.getTabAt(0)?.text = "Available"
                tabLayout.getTabAt(1)?.text = "My Deliveries"
            }
            "admin" -> {
                tvGreeting.text = "Admin Panel"
                tvRoleBadge.text = "ADMIN"
                cvCreateShipment.visibility = View.GONE // Admin supervises, doesn't create
                tabLayout.getTabAt(0)?.text = "All Shipments"
                tabLayout.getTabAt(1)?.text = "Audit"
            }
        }

        adapter = ShipmentAdapter(role) { shipment ->
            val intent = Intent(this, ShipmentDetailsActivity::class.java)
            intent.putExtra("shipment_id", shipment.id)
            startActivity(intent)
        }

        rvShipments.layoutManager = LinearLayoutManager(this)
        rvShipments.adapter = adapter

        cvCreateShipment.setOnClickListener {
            startActivity(Intent(this, CreateShipmentActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.fetchShipments()
        }

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                viewModel.fetchShipments()
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        lifecycleScope.launchWhenStarted {
            viewModel.shipments.collect { shipments ->
                adapter.submitList(shipments)
                swipeRefresh.isRefreshing = false
            }
        }

        // ── BOTTOM NAVIGATION ────────────────────────────────
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_community -> {
                    startActivity(Intent(this, DAOActivity::class.java))
                    false // Don't highlight it permanently if we are just jumping to another activity
                }
                else -> true
            }
        }

        viewModel.fetchShipments()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchShipments()
    }
}

class ShipmentAdapter(
    private val userRole: String,
    private val onClick: (ShipmentResponse) -> Unit
) : RecyclerView.Adapter<ShipmentAdapter.ViewHolder>() {
    private var list = listOf<ShipmentResponse>()

    fun submitList(newList: List<ShipmentResponse>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shipment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item, userRole, onClick)
    }

    override fun getItemCount() = list.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: ShipmentResponse, role: String, onClick: (ShipmentResponse) -> Unit) {
            itemView.findViewById<TextView>(R.id.tvShipmentId).text = "Shipment #${item.id}"
            itemView.findViewById<TextView>(R.id.tvRoute).text = "${item.pickupLocation} → ${item.deliveryLocation}"
            itemView.findViewById<TextView>(R.id.tvCargo).text = item.cargoType

            val statusLabel = when(item.status) {
                "0" -> "⏳ PENDING"
                "1" -> "📋 BIDDING"
                "2" -> "🚛 IN TRANSIT"
                "3" -> "📦 DELIVERED"
                "4" -> "✅ COMPLETED"
                else -> "UNKNOWN"
            }
            itemView.findViewById<TextView>(R.id.tvStatus).text = statusLabel

            // Role-aware detail button label
            val btnDetail = itemView.findViewById<Button>(R.id.btnDetail)
            when (role) {
                "hospital" -> btnDetail.text = if (item.status == "1") "View Bids" else "Details"
                "transport" -> btnDetail.text = if (item.status == "1") "Place Bid" else "Track"
                "admin" -> btnDetail.text = "Audit"
            }

            btnDetail.setOnClickListener { onClick(item) }
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
