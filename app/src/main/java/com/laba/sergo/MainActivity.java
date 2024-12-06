package com.laba.sergo;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
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

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView responseTextView;
    private EditText inputLocation;
    private Button searchButton;
    String apiKey1;
    String apiKey2;
    private Button startDateButton, endDateButton;
    private Button languageSwitchButton;
    private String startDate = "";
    private String endDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Properties properties = ConfigUtil.loadProperties(this);

        // Get the API keys
         apiKey1 = properties.getProperty("apiKey1");
         apiKey2 = properties.getProperty("apiKey2");
        // Ініціалізація UI елементів
        responseTextView = findViewById(R.id.responseTextView);
        inputLocation = findViewById(R.id.inputLocation);
        searchButton = findViewById(R.id.searchButton);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        languageSwitchButton = findViewById(R.id.languageSwitchButton);

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
                responseTextView.setText(getString(R.string.enter_location));
            }
        });

        // Обробка кнопки зміни мови
        languageSwitchButton.setOnClickListener(v -> switchLanguage());
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
                startDateButton.setText(getString(R.string.start) + " " + startDate);
            } else {
                endDate = selectedDate;
                endDateButton.setText(getString(R.string.end) + " " + endDate);
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
                HttpUrl url = new HttpUrl.Builder()
                        .scheme("https")
                        .host("meteostat.p.rapidapi.com")
                        .addPathSegment("point")
                        .addPathSegment("monthly")
                        .addQueryParameter("lat", String.valueOf(latitude))
                        .addQueryParameter("lon", String.valueOf(longitude))
                        .addQueryParameter("alt", "43")
                        .addQueryParameter("start", startDate)
                        .addQueryParameter("end", endDate)
                        .build();
                // Запит до API
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("x-rapidapi-key", apiKey2)
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
                HttpUrl url = new HttpUrl.Builder()
                        .scheme("https")
                        .host("api.opencagedata.com")
                        .addPathSegment("geocode")
                        .addPathSegment("v1")
                        .addPathSegment("json")
                        .addQueryParameter("q", location)
                        .addQueryParameter("key", apiKey1)
                        .build();
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
                        new Handler(Looper.getMainLooper()).post(() -> responseTextView.setText(getString(R.string.location_not_found)));
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
        formattedData.append(getString(R.string.weather_data)).append("\n\n");

        for (int i = 0; i < data.size(); i++) {
            JsonObject monthData = data.get(i).getAsJsonObject();

            String date = monthData.get("date").getAsString();
            String avgTemp = !monthData.get("tavg").isJsonNull() ? String.valueOf(monthData.get("tavg").getAsDouble()) : "N/A";
            String minTemp = !monthData.get("tmin").isJsonNull() ? String.valueOf(monthData.get("tmin").getAsDouble()) : "N/A";
            String maxTemp = !monthData.get("tmax").isJsonNull() ? String.valueOf(monthData.get("tmax").getAsDouble()) : "N/A";

            formattedData
                    .append(getString(R.string.date)).append(" ").append(date).append("\n")
                    .append(getString(R.string.avg_temp)).append(" ").append(avgTemp).append(" °C\n")
                    .append(getString(R.string.min_temp)).append(" ").append(minTemp).append(" °C\n")
                    .append(getString(R.string.max_temp)).append(" ").append(maxTemp).append(" °C\n\n");
        }

        return formattedData.toString();
    }

    private void switchLanguage() {
        String currentLanguage = loadLanguagePreference();
        String newLanguage = "uk";

        switch (currentLanguage) {
            case "uk":
                newLanguage = "en";
                break;
            case "en":
                newLanguage = "uk";
                break;

        }
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        System.out.println(configuration.getLocales());
        saveLanguagePreference(newLanguage);

        setLocale(newLanguage);

        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
    private String loadLanguagePreference() {
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        return preferences.getString("language", "uk");  // Default language is 'uk'
    }

    private void saveLanguagePreference(String language) {
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", language);
        editor.apply();
    }



    private void setLocale(String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}