package com.example.beaconkotlinapp.models
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/beacon-event")
    fun sendBeaconEvent(@Body event: BeaconEvent): Call<Void>
}