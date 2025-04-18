package com.example.soilhealthmonitorapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

data class SensorData(
    val soil_health: String,
    val water_level: String,
    val temperature: String,
    val humidity: String,
    val pump_status: String
)

// Data class for fan control
data class FanControlRequest(
    val fan_status: String  // "on" or "off"
)

// Interface to define your API endpoints
interface ApiService {

    // Get request to fetch the latest sensor data
    @GET("sensorData")
    fun getSensorData(): Call<SensorData>

    // Post request to update the sensor data (if needed in the future)
    @POST("updateSensor")
    fun updateSensorData(@Body sensorData: SensorData): Call<Void>

    // Post request to control the fan (on/off)
    @POST("fanControl")
    fun controlFan(@Body fanControlRequest: FanControlRequest): Call<Void>
}
