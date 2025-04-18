package com.example.soilhealthmonitorapp

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://192.168.1.211:8080/" // Your backend server URL

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Set connection timeout
        .readTimeout(30, TimeUnit.SECONDS)     // Set read timeout
        .writeTimeout(30, TimeUnit.SECONDS)    // Set write timeout
        .build()

    // Retrofit instance
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)                  // Set base URL for the backend API
            .client(okHttpClient)               // Attach OkHttpClient for handling requests
            .addConverterFactory(GsonConverterFactory.create())  // Use Gson converter to handle JSON
            .build()
    }
}
