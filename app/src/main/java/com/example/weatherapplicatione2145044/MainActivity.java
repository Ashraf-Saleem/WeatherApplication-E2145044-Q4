package com.example.weatherapplicatione2145044;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String PREFS_NAME = "WeatherAppPrefs";
    private static final String KEY_LAST_CITY = "lastCity";

    private FusedLocationProviderClient fusedLocationClient;
    private EditText etSearchLocation;
    private TextView tvLocation, tvAddress, tvTime, tvWeather, tvTemperature, tvHumidity, tvDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etSearchLocation = findViewById(R.id.etSearchLocation);
        tvLocation = findViewById(R.id.tvLocation);
        tvAddress = findViewById(R.id.tvAddress);
        tvTime = findViewById(R.id.tvTime);
        tvWeather = findViewById(R.id.tvWeather);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvDescription = findViewById(R.id.tvDescription);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String location = etSearchLocation.getText().toString().trim();
                if (!TextUtils.isEmpty(location)) {
                    searchLocation(location);
                    saveLastSearchedCity(location);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a location", Toast.LENGTH_SHORT).show();
                }
                displayCurrentTime();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }


        displayCurrentTime();
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            tvLocation.setText("Location : " + latitude + ", " + longitude);
                            fetchAddress(latitude, longitude);
                            fetchWeatherData(latitude, longitude);
                        } else {
                            Toast.makeText(MainActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                tvAddress.setText("Address : " + addressText);
            } else {
                tvAddress.setText("Address not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvAddress.setText("Error fetching address");
        }
    }

    private void fetchWeatherData(double latitude, double longitude) {
        String apiKey = "b54ac7c53e05e2f5c11897c4356cafa5";
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + apiKey + "&units=metric";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);

                    JSONObject main = jsonObject.getJSONObject("main");
                    double temperature = main.getDouble("temp");
                    int humidity = main.getInt("humidity");

                    JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
                    String description = weather.getString("description");

                    runOnUiThread(() -> {
                        tvWeather.setText("Weather");
                        tvTemperature.setText("Temperature : " + temperature + "°C ");
                        tvHumidity.setText("Humidity : " + humidity + "% ");
                        tvDescription.setText("Description : " + description);
                    });
                } else {
                    runOnUiThread(() -> tvWeather.setText("Failed to get weather data"));
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> tvWeather.setText("Error fetching weather data"));
            }
        }).start();
    }

    private void searchLocation(String location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(location, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                tvLocation.setText("Location: " + latitude + ", " + longitude);
                fetchAddress(latitude, longitude);
                fetchWeatherData(latitude, longitude);
            } else {
                Toast.makeText(MainActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            tvAddress.setText("Error fetching address");
        }
    }

    private void displayCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        tvTime.setText("Time: " + currentTime);
    }

    private void saveLastSearchedCity(String city) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_CITY, city);
        editor.apply();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
