package com.example.beaconkotlinapp.models

data class BeaconEvent(
    val mac_address: String,
    val latitude: Double?,
    val longitude: Double?,
    val phone_number: String,
    val event_type: String
)
