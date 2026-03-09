package com.example.chainrx.network

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chainrx.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ChainRxViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = ApiClient.getService(application)
    private val context = application.applicationContext

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _shipments = MutableStateFlow<List<Shipment>>(emptyList())
    val shipments: StateFlow<List<Shipment>> = _shipments

    private val _currentShipment = MutableStateFlow<Shipment?>(null)
    val currentShipment: StateFlow<Shipment?> = _currentShipment

    private val _loginResult = MutableStateFlow<String?>(null)
    val loginResult: StateFlow<String?> = _loginResult

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = apiService.login(mapOf("email" to email, "password" to pass))
                AuthManager.saveAuth(context, result.access_token, result.role, email)
                _loginResult.value = "SUCCESS"
            } catch (e: Exception) {
                _statusMessage.value = "Login Failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, pass: String, role: String, wallet: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mapOf(
                    "name" to name,
                    "email" to email,
                    "password" to pass,
                    "role" to role,
                    "wallet_address" to wallet
                )
                val result = apiService.register(data)
                AuthManager.saveAuth(context, result.access_token, result.role, email)
                _loginResult.value = "SUCCESS"
            } catch (e: Exception) {
                _statusMessage.value = "Registration Failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchShipments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _shipments.value = apiService.getShipments()
            } catch (e: Exception) {
                _statusMessage.value = "Fetch Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchShipment(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _currentShipment.value = apiService.getShipment(id)
            } catch (e: Exception) {
                _statusMessage.value = "Fetch Detail Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createShipment(pickup: String, delivery: String, cargo: String, amount: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val shipment = ShipmentCreate(pickup, delivery, cargo, amount)
                apiService.createShipment(shipment)
                _statusMessage.value = "Shipment Created!"
                fetchShipments()
            } catch (e: Exception) {
                _statusMessage.value = "Creation Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitBid(shipmentId: Int, amount: Long, time: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bid = BidSubmit(
                    shipmentId = shipmentId,
                    bidAmount = amount,
                    deliveryTime = time
                )
                apiService.submitBid(bid)
                _statusMessage.value = "Bid Submitted!"
            } catch (e: Exception) {
                _statusMessage.value = "Bidding Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun depositEscrow(shipmentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.depositEscrow(shipmentId)
                _statusMessage.value = "Escrow Deposited!"
                fetchShipment(shipmentId)
            } catch (e: Exception) {
                _statusMessage.value = "Escrow Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmPickup(shipmentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.confirmPickup(shipmentId)
                _statusMessage.value = "Pickup Confirmed!"
                fetchShipment(shipmentId)
            } catch (e: Exception) {
                _statusMessage.value = "Pickup Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmDelivery(shipmentId: Int, file: File) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val filePart = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                apiService.confirmDelivery(shipmentId, filePart)
                _statusMessage.value = "Delivery Confirmed!"
                fetchShipment(shipmentId)
            } catch (e: Exception) {
                _statusMessage.value = "Delivery Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun releasePayment(shipmentId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.releasePayment(shipmentId)
                _statusMessage.value = "Payment Released!"
                fetchShipment(shipmentId)
            } catch (e: Exception) {
                _statusMessage.value = "Release Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── BIDS ──────────────────────────────────────────────────
    private val _bids = MutableStateFlow<List<Bid>>(emptyList())
    val bids: StateFlow<List<Bid>> = _bids

    fun fetchBids(shipmentId: Int) {
        viewModelScope.launch {
            try {
                _bids.value = apiService.getBids(shipmentId)
            } catch (e: Exception) {
                _statusMessage.value = "Bids Error: ${e.message}"
            }
        }
    }

    fun acceptBid(shipmentId: Int, bidId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.acceptBid(shipmentId, bidId)
                _statusMessage.value = "Transporter Selected!"
                fetchShipment(shipmentId)
            } catch (e: Exception) {
                _statusMessage.value = "Accept Bid Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
