package com.example.chainrx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.chainrx.auth.AuthManager
import com.google.android.gms.location.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.URI

class TrackingActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var transporterMarker: Marker? = null
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var shipmentId: Int = -1
    private var role: String = "hospital"
    
    private var webSocketClient: WebSocketClient? = null
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMDroid Configuration - Must be done before setContentView
        Configuration.getInstance().load(this, android.preference.PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_tracking)

        shipmentId = intent.getIntExtra("shipment_id", -1)
        role = AuthManager.getRole(this) ?: "hospital"
        
        tvStatus = findViewById(R.id.tvTrackingStatus)
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        // Default center (can be updated when location comes)
        mapController.setCenter(GeoPoint(0.0, 0.0))

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        connectWebSocket()
    }

    private fun connectWebSocket() {
        // Use the common BASE_URL from ApiClient
        val baseUrl = com.example.chainrx.network.ApiClient.BASE_URL.replace("http://", "ws://").replace("https://", "wss://")
        val uri = URI("${baseUrl}ws/tracking/$shipmentId")

        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                runOnUiThread {
                    tvStatus.text = if (role == "transport") "📡 Broadcasting location..." else "📡 Connected. Waiting for transporter..."
                }
                if (role == "transport") {
                    startLocationTracking()
                }
            }

            override fun onMessage(message: String?) {
                if (role == "hospital" && message != null) {
                    try {
                        val data = JSONObject(message)
                        val lat = data.getDouble("lat")
                        val lng = data.getDouble("lng")
                        updateTransporterMarker(lat, lng)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                runOnUiThread { tvStatus.text = "❌ Connection closed" }
            }

            override fun onError(ex: Exception?) {
                runOnUiThread { tvStatus.text = "⚠️ Error: ${ex?.message}" }
            }
        }
        webSocketClient?.connect()
    }

    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val data = JSONObject().apply {
                        put("lat", location.latitude)
                        put("lng", location.longitude)
                    }
                    webSocketClient?.send(data.toString())
                    updateTransporterMarker(location.latitude, location.longitude)
                }
            }
        }, Looper.getMainLooper())
    }

    private fun updateTransporterMarker(lat: Double, lng: Double) {
        runOnUiThread {
            val geoPoint = GeoPoint(lat, lng)
            if (transporterMarker == null) {
                transporterMarker = Marker(mapView)
                transporterMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                transporterMarker?.title = "Transporter"
                mapView.overlays.add(transporterMarker)
                mapView.controller.animateTo(geoPoint)
            }
            
            transporterMarker?.position = geoPoint
            mapView.invalidate() // Refresh map

            if (role == "hospital") {
                tvStatus.text = "🚚 Transporter is moving..."
            }
        }
    }

    override fun onResume() { 
        super.onResume()
        mapView.onResume() 
    }
    
    override fun onPause() { 
        super.onPause()
        mapView.onPause() 
    }
    
    override fun onDestroy() { 
        super.onDestroy()
        webSocketClient?.close()
        mapView.onDetach() 
    }
}
