import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;

public class Main {

    public static void main(String[] args) throws IOException {
        // Start HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/updateSensor", new SensorUpdateHandler());
        server.createContext("/sensorData", new SensorDataHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8080");
    }

    // Handler for updating sensor data via HTTP POST
    static class SensorUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("Received request body: " + requestBody);

                try {
                    if (requestBody == null || requestBody.isEmpty()) {
                        throw new JSONException("Request body is empty or null");
                    }

                    JSONObject json = new JSONObject(requestBody);
                    String soilHealth = json.optString("soil_health", "N/A");
                    String waterLevel = json.optString("water_level", "N/A");
                    String temperature = json.optString("temperature", "N/A");
                    String humidity = json.optString("humidity", "N/A");
                    String pumpStatus = json.optString("pump_status", "N/A");

                    System.out.println("Parsed Data -> Soil Health: " + soilHealth +
                            ", Water Level: " + waterLevel + ", Temperature: " + temperature +
                            ", Humidity: " + humidity + ", Pump Status: " + pumpStatus);

                    // Save data to DB
                    saveSensorData(soilHealth, waterLevel, temperature, humidity, pumpStatus);

                    String response = "{\"status\":\"success\", \"message\":\"Data inserted successfully.\"}";
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } catch (JSONException e) {
                    String response = "{\"status\":\"error\",\"message\":\"Invalid JSON format.\"}";
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String response = "{\"status\":\"error\",\"message\":\"Internal Server Error.\"}";
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                }
            }
        }

        // Save sensor data to DB
        private void saveSensorData(String soilHealth, String waterLevel, String temperature, String humidity, String pumpStatus) {
            String jdbcUrl = "jdbc:mysql://localhost:3306/soilmonitordb";
            String username = "root";
            String password = "Nadeesha@242002";

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement preparedStatement = connection.prepareStatement(
                         "INSERT INTO sensordata (soil_health, water_level, temperature, humidity, pump_status) VALUES (?, ?, ?, ?, ?)")) {

                preparedStatement.setString(1, soilHealth);
                preparedStatement.setString(2, waterLevel);
                preparedStatement.setString(3, temperature);
                preparedStatement.setString(4, humidity);
                preparedStatement.setString(5, pumpStatus);

                System.out.println("Inserting into DB -> Soil Health: " + soilHealth +
                        ", Water Level: " + waterLevel + ", Temperature: " + temperature +
                        ", Humidity: " + humidity + ", Pump Status: " + pumpStatus);

                int rowsInserted = preparedStatement.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Data inserted successfully into the database.");
                } else {
                    System.err.println("Failed to insert data into the database.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Handler for fetching latest sensor data via HTTP GET
    static class SensorDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = fetchLatestSensorData();
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

        // Fetch the latest sensor data from DB
        private String fetchLatestSensorData() {
            String jdbcUrl = "jdbc:mysql://localhost:3306/soilmonitordb";
            String username = "root";
            String password = "Nadeesha@242002";
            String result = "{}";

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(
                         "SELECT soil_health, water_level, temperature, humidity, pump_status FROM sensordata ORDER BY id DESC LIMIT 1")) {

                if (resultSet.next()) {
                    JSONObject json = new JSONObject();
                    json.put("soil_health", resultSet.getString("soil_health"));
                    json.put("water_level", resultSet.getString("water_level"));
                    json.put("temperature", resultSet.getString("temperature"));
                    json.put("humidity", resultSet.getString("humidity"));
                    json.put("pump_status", resultSet.getString("pump_status"));

                    result = json.toString();
                } else {
                    result = "{\"message\":\"No data found.\"}";
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = "{\"message\":\"Error fetching data from database.\"}";
            }


            return result;
        }
    }
}

