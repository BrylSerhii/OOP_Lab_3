package com.laba.sergo;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView responseTextView;
    private EditText inputLocation;
    private Button searchButton;
    private Button startDateButton, endDateButton;
    private String startDate = "";
    private String endDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ініціалізація UI елементів
        responseTextView = findViewById(R.id.responseTextView);
        inputLocation = findViewById(R.id.inputLocation);
        searchButton = findViewById(R.id.searchButton);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);

        // Обробка кнопки вибору початкової дати
        startDateButton.setOnClickListener(v -> showDatePickerDialog(true));

        // Обробка кнопки вибору кінцевої дати
        endDateButton.setOnClickListener(v -> showDatePickerDialog(false));

        // Обробка кнопки пошуку
        searchButton.setOnClickListener(v -> {
            String location = inputLocation.getText().toString().trim();
            if (!location.isEmpty() && !startDate.isEmpty() && !endDate.isEmpty()) {
                fetchCoordinates(location); // Fetch coordinates based on the entered city name
            } else {
                responseTextView.setText("Будь ласка, введіть всі дані.");
            }
        });
    }

    private void showDatePickerDialog(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
            if (isStartDate) {
                startDate = selectedDate;
                startDateButton.setText("Початок: " + startDate);
            } else {
                endDate = selectedDate;
                endDateButton.setText("Кінець: " + endDate);
            }
        }, year, month, day);

        datePickerDialog.show();
    }

    @SuppressLint("NewApi")
    private void fetchWeatherData(String latitude, String longitude, String startDate, String endDate) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Log.d(TAG, "OkHttpClient created");

                // Запит до API
                Request request = new Request.Builder()
                        .url("https://meteostat.p.rapidapi.com/point/monthly?lat=" + latitude + "&lon=" + longitude + "&alt=43&start=" + startDate + "&end=" + endDate)
                        .get()
                        .addHeader("x-rapidapi-key", "f588b4e3bfmsh6739b8f50b38334p12c1d9jsna343a676d8b1")
                        .addHeader("x-rapidapi-host", "meteostat.p.rapidapi.com")
                        .build();
                Log.d(TAG, "Request created: " + request.url());

                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Response received: " + responseData);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        responseTextView.setText(formatWeatherData(responseData));
                    });
                } else {
                    Log.e(TAG, "Request failed with code: " + response.code());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
            }
        }).start();
    }

    private void fetchCoordinates(String location) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = "https://api.opencagedata.com/geocode/v1/json?q=" + location + "&key=d0073317559641f09251322c4fc953a0";

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Coordinates response received: " + responseData);

                    JsonObject jsonResponse = new Gson().fromJson(responseData, JsonObject.class);
                    JsonArray results = jsonResponse.getAsJsonArray("results");

                    if (results.size() > 0) {
                        JsonObject geometry = results.get(0).getAsJsonObject().getAsJsonObject("geometry");
                        double latitude = geometry.get("lat").getAsDouble();
                        double longitude = geometry.get("lng").getAsDouble();

                        // Після отримання координат викликаємо метод для запиту погоди
                        fetchWeatherData(Double.toString(latitude), Double.toString(longitude), startDate, endDate);
                    } else {
                        Log.e(TAG, "Location not found.");
                        new Handler(Looper.getMainLooper()).post(() -> responseTextView.setText("Місто не знайдено."));
                    }
                } else {
                    Log.e(TAG, "Request failed with code: " + response.code());
                }
            } catch (IOException e) {
                Log.e(TAG, "Error: " + e.getMessage(), e);
            }
        }).start();
    }

    private String formatWeatherData(String rawData) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(rawData, JsonObject.class);
        JsonArray data = jsonResponse.getAsJsonArray("data");

        StringBuilder formattedData = new StringBuilder();
        formattedData.append("Погода за вибраний період:\n\n");

        for (int i = 0; i < data.size(); i++) {
            JsonObject monthData = data.get(i).getAsJsonObject();

            String date = monthData.get("date").getAsString();
            String avgTemp = !monthData.get("tavg").isJsonNull() ? String.valueOf(monthData.get("tavg").getAsDouble()) : "N/A";
            String minTemp = !monthData.get("tmin").isJsonNull() ? String.valueOf(monthData.get("tmin").getAsDouble()) : "N/A";
            String maxTemp = !monthData.get("tmax").isJsonNull() ? String.valueOf(monthData.get("tmax").getAsDouble()) : "N/A";

            formattedData
                    .append("Дата: ").append(date).append("\n")
                    .append("Середня температура: ").append(avgTemp).append(" °C\n")
                    .append("Мін. температура: ").append(minTemp).append(" °C\n")
                    .append("Макс. температура: ").append(maxTemp).append(" °C\n\n");
        }

        return formattedData.toString();
    }
}
