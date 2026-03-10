package com.example.chainrx.network

import android.content.Context
import com.example.chainrx.auth.AuthInterceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ChainRxService {
    // Auth
    @POST("auth/login")
    suspend fun login(@Body credentials: Map<String, String>): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body data: Map<String, String>): AuthResponse

    // Shipments
    @GET("shipments")
    suspend fun getShipments(): List<Shipment>

    @GET("shipments/{id}")
    suspend fun getShipment(@Path("id") id: Int): Shipment

    @POST("createShipment")
    suspend fun createShipment(@Body shipment: ShipmentCreate): Map<String, Any>

    @POST("shipments/{id}/deposit")
    suspend fun depositEscrow(@Path("id") id: Int): Map<String, Any>

    @POST("shipments/{id}/pickup")
    suspend fun confirmPickup(@Path("id") id: Int): Map<String, Any>

    @Multipart
    @POST("confirmDelivery")
    suspend fun confirmDelivery(
        @Part("shipmentId") shipmentId: Int,
        @Part file: MultipartBody.Part
    ): Map<String, Any>

    @POST("shipments/{id}/release")
    suspend fun releasePayment(@Path("id") id: Int): Map<String, Any>

    @POST("shipments/{id}/accept-bid/{bidId}")
    suspend fun acceptBid(@Path("id") shipmentId: Int, @Path("bidId") bidId: Int): Map<String, Any>

    // Bids
    @POST("submitBid")
    suspend fun submitBid(@Body bidData: BidSubmit): Map<String, Any>

    @GET("bids/{id}")
    suspend fun getBids(@Path("id") id: Int): List<Bid>

    // DAO / Community
    @GET("dao/proposals")
    suspend fun getProposals(@Query("community") community: Int): List<Proposal>

    @POST("dao/proposals")
    suspend fun createProposal(@Body proposal: ProposalCreate): TransactionResponse

    @POST("dao/proposals/vote")
    suspend fun voteProposal(@Body vote: ProposalVote): TransactionResponse

    @GET("shipments/{id}/ai-recommendation")
    suspend fun getAIRecommendation(@Path("id") id: Int): AIRecommendation

    // Optimizer
    @POST("optimizer/optimize")
    suspend fun optimizeRoute(@Body request: RouteRequest): OptimizedRoute
}

object ApiClient {
    const val BASE_URL = "http://172.16.46.70:8000/"

    fun getService(context: Context): ChainRxService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()
            
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChainRxService::class.java)
    }
}
