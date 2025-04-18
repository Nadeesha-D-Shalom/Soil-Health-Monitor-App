package com.example.soilhealthmonitorapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.example.soilhealthmonitorapp.ui.theme.SoilHealthMonitorAppTheme
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoilHealthMonitorAppTheme {
                // State variables to hold the sensor data
                var soilHealth by remember { mutableStateOf("Loading...") }
                var waterLevel by remember { mutableStateOf("Loading...") }
                var temperature by remember { mutableStateOf("Loading...") }
                var humidity by remember { mutableStateOf("Loading...") }
                var isFanOn by remember { mutableStateOf(false) }  // Fan status
                var isPumpOn by remember { mutableStateOf(false) } // Water pump status
                var errorMessage by remember { mutableStateOf("") } // Error message

                // Fetch sensor data from the backend periodically
                LaunchedEffect(Unit) {
                    while (true) {
                        fetchSensorData(
                            onSuccess = { data ->
                                soilHealth = data.soil_health
                                waterLevel = data.water_level
                                temperature = data.temperature
                                humidity = data.humidity

                                // Auto-fan logic based on temperature
                                val tempValue = temperature.replace("°C", "").toFloatOrNull()
                                if (tempValue != null && tempValue >= 34 && !isFanOn) {
                                    isFanOn = true
                                    Log.d("MainActivity", "Fan turned on due to temperature >= 34°C")
                                } else if (tempValue != null && tempValue < 34 && isFanOn) {
                                    isFanOn = false
                                    Log.d("MainActivity", "Fan turned off due to temperature < 34°C")
                                }

                                // Water pump status logic
                                isPumpOn = data.pump_status == "on" // Update pump status
                            },
                            onError = { error ->
                                errorMessage = error
                                Log.e("MainActivity", "Error fetching data: $error")
                            }
                        )
                        delay(10000) // Fetch data every 10 seconds
                    }
                }

                // Composable to display the sensor data
                MainScreen(
                    soilHealth = soilHealth,
                    waterLevel = waterLevel,
                    temperature = temperature,
                    humidity = humidity,
                    isFanOn = isFanOn,
                    isPumpOn = isPumpOn, // Pass the pump status to MainScreen
                    errorMessage = errorMessage
                )
            }
        }
    }

    private fun fetchSensorData(onSuccess: (SensorData) -> Unit, onError: (String) -> Unit) {
        val apiService = ApiClient.retrofit.create(ApiService::class.java)
        val call = apiService.getSensorData()

        call.enqueue(object : Callback<SensorData> {
            override fun onResponse(call: Call<SensorData>, response: Response<SensorData>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        Log.d("MainActivity", "Received Sensor Data: $data")  // Log the received data
                        Log.d("MainActivity", "Pump Status: ${data.pump_status}")  // Log pump status
                        onSuccess(data)
                    }
                } else {
                    onError("Failed to fetch data")
                }
            }

            override fun onFailure(call: Call<SensorData>, t: Throwable) {
                onError(t.message ?: "Unknown error")
            }
        })
    }
}

@Composable
fun MainScreen(
    soilHealth: String,
    waterLevel: String,
    temperature: String,
    humidity: String,
    isFanOn: Boolean,
    isPumpOn: Boolean,
    errorMessage: String // Error message to display in case of issues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF040720)), // Updated background color to #040720
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Soil Health Monitoring APP",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 24.sp,
            color = Color.White // Title color set to white
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage.isNotEmpty()) {
            // Display error message if there's an error
            Text(text = "Error: $errorMessage", color = Color.Red, fontSize = 18.sp)
        } else {
            // Display Soil Health
            StatusBox(label = "Soil Health", value = "$soilHealth%")

            // Display Water Level
            StatusBox(label = "Water Level", value = "$waterLevel%")

            // Check water level and display the warning if less than 15%
            val waterLevelValue = waterLevel.toFloatOrNull() ?: 0f
            if (waterLevelValue < 15) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Water reservoir is low! Go to the tank to dump and refill water.",
                    color = Color.Red,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }

            // Display Temperature
            StatusBox(label = "Temperature", value = "$temperature°C")

            // Display Humidity
            StatusBox(label = "Humidity", value = "$humidity%")

            Spacer(modifier = Modifier.height(24.dp))

            // Blinking Fan Status Message
            if (isFanOn) {
                BlinkingText(message = "⚠️ Fan is working")
            }

            // Log the pump status to verify
            Log.d("MainScreen", "Pump Status in UI: $isPumpOn")

            // Blinking Water Pump Status Message
            if (isPumpOn) {
                BlinkingText(message = "💧 Water is adding")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatusBox(label: String, value: String) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(Color(0xFFE0EAF5))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", fontSize = 18.sp)
        Text(text = value, fontSize = 18.sp)
    }
}

@Composable
fun BlinkingText(message: String) {
    var alpha by remember { mutableStateOf(1f) }  // State variable for opacity

    // Toggle the opacity between 0f and 1f periodically
    LaunchedEffect(Unit) {
        while (true) {
            alpha = if (alpha == 1f) 0f else 1f  // Toggle between visible and invisible
            delay(500)  // Blink every 500 milliseconds
        }
    }

    Text(
        text = message,
        fontSize = 20.sp,
        color = Color.Red,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)  // Apply the opacity to the text
    )
}

@Composable
@Preview(showBackground = true)
fun PreviewMainScreen() {
    SoilHealthMonitorAppTheme {
        MainScreen("0%", "0%", "0°C", "0%", isFanOn = true, isPumpOn = true, errorMessage = "")
    }
}
