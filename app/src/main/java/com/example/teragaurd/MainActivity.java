package com.example.teragaurd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private double latitude, longitude;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    // IMPORTANT: Replace "YOUR_API_KEY" with your actual OpenWeatherMap API key
    private static final String API_KEY = "72aa5ebf5045980623cd8ff3e86a6e01";

    private TextView txtTemp, txtAqi, txtAqiStatus, txtChartLoading;
    private LineChart temperatureChart;
    private ApiService apiService;
    private BottomNavigationView bottomNavigationView;
    private boolean dataFetched = false;
    private List<Long> forecastTimestamps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtTemp = findViewById(R.id.txtTemp);
        txtAqi = findViewById(R.id.txtAqi);
        txtAqiStatus = findViewById(R.id.txtAqiStatus);
        txtChartLoading = findViewById(R.id.txtChartLoading);
        temperatureChart = findViewById(R.id.temperatureChart);
        bottomNavigationView = findViewById(R.id.bottomNav);

        // Set loading state initially
        txtTemp.setText("Loading...");
        txtAqi.setText("...");
        txtAqiStatus.setText("");

        // Setup the temperature trend chart
        setupChart();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(MainActivity.this, profile_view_Activity.class));
                    return true;
                } else if (itemId == R.id.nav_map) {
                    startActivity(new Intent(MainActivity.this, MapActivity.class));
                    return true;
                } else if (itemId == R.id.nav_assist) {
                    startActivity(new Intent(MainActivity.this, EmergencyContactsActivity.class));
                    return true;
                }
                return false;
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup location callback for active location requests
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || dataFetched) return;
                
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    dataFetched = true;
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Log.d(TAG, "Got location from callback: " + latitude + ", " + longitude);
                    
                    fetchWeather(latitude, longitude);
                    fetchAqi(latitude, longitude);
                    fetchForecast(latitude, longitude);
                    
                    // Stop location updates after getting location
                    stopLocationUpdates();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataFetched = false;
        if (hasLocationPermission()) {
            getUserLocation();
        } else {
            requestLocationPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        if (fusedLocationClient == null) return;
        
        Log.d(TAG, "Getting user location...");
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && !dataFetched) {
                        dataFetched = true;
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d(TAG, "Got cached location: " + latitude + ", " + longitude);

                        fetchWeather(latitude, longitude);
                        fetchAqi(latitude, longitude);
                        fetchForecast(latitude, longitude);
                    } else if (!dataFetched) {
                        // No cached location available, request active location
                        Log.d(TAG, "No cached location, requesting active location...");
                        requestActiveLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get last location: " + e.getMessage());
                    if (!dataFetched) {
                        requestActiveLocation();
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void requestActiveLocation() {
        Log.d(TAG, "Requesting active location updates...");
        
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void fetchWeather(double lat, double lon) {
        apiService.getWeather(lat, lon, API_KEY, "metric")
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call,
                                           Response<WeatherResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse.Main main = response.body().getMain();
                            if (main != null) {
                                double temp = main.getTemp();
                                txtTemp.setText(Math.round(temp) + "°C");
                            }
                        } else {
                            Log.e("Weather", "Response error: " + response.code());
                            txtTemp.setText("--");
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        Log.e("Weather", "Network error: " + t.getMessage());
                        txtTemp.setText("--");
                    }
                });
    }

    private void fetchAqi(double lat, double lon) {
        apiService.getAqi(lat, lon, API_KEY)
                .enqueue(new Callback<AqiResponse>() {
                    @Override
                    public void onResponse(Call<AqiResponse> call, Response<AqiResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getList() != null && !response.body().getList().isEmpty()) {
                            AqiResponse.AqiData aqiData = response.body().getList().get(0);
                            if (aqiData != null && aqiData.getMain() != null) {
                                int aqi = aqiData.getMain().getAqi();
                                txtAqi.setText(String.valueOf(aqi));
                                setAqiStatus(aqi);
                            }
                        } else {
                             Log.e("AQI", "Response error: " + response.code());
                             txtAqi.setText("--");
                             txtAqiStatus.setText("");
                        }
                    }

                    @Override
                    public void onFailure(Call<AqiResponse> call, Throwable t) {
                        Log.e("AQI", "Network error: " + t.getMessage());
                        txtAqi.setText("--");
                        txtAqiStatus.setText("");
                    }
                });
    }

    private void setAqiStatus(int aqi) {
        String status;
        int color;
        // Check if resources are available to prevent crashes if context is lost (unlikely here but safe)
        if (getResources() == null) return;

        switch (aqi) {
            case 1:
                status = "Good";
                color = getResources().getColor(R.color.aqi_good, null);
                break;
            case 2:
                status = "Fair";
                color = getResources().getColor(R.color.aqi_fair, null);
                break;
            case 3:
                status = "Moderate";
                color = getResources().getColor(R.color.aqi_moderate, null);
                break;
            case 4:
                status = "Poor";
                color = getResources().getColor(R.color.aqi_poor, null);
                break;
            case 5:
                status = "Very Poor";
                color = getResources().getColor(R.color.aqi_very_poor, null);
                break;
            default:
                status = "";
                color = getResources().getColor(android.R.color.white, null);
        }
        txtAqiStatus.setText(status);
        txtAqi.setTextColor(color);
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Location permission is required to show weather data.", Toast.LENGTH_LONG).show();
                txtTemp.setText("--");
            }
        }
    }

    /**
     * Setup the temperature trend chart with proper styling
     */
    private void setupChart() {
        if (temperatureChart == null) return;

        // Basic chart configuration
        temperatureChart.setDrawGridBackground(false);
        temperatureChart.setDrawBorders(false);
        temperatureChart.setTouchEnabled(true);
        temperatureChart.setDragEnabled(true);
        temperatureChart.setScaleEnabled(false);
        temperatureChart.setPinchZoom(false);
        temperatureChart.setDoubleTapToZoomEnabled(false);
        temperatureChart.getDescription().setEnabled(false);
        temperatureChart.getLegend().setEnabled(false);
        temperatureChart.setExtraBottomOffset(8f);
        temperatureChart.setExtraTopOffset(8f);
        temperatureChart.setNoDataText("");

        // X-Axis configuration
        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#AAAAAA"));
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(4, true);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < forecastTimestamps.size()) {
                    return sdf.format(new Date(forecastTimestamps.get(index) * 1000L));
                }
                return "";
            }
        });

        // Left Y-Axis configuration
        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setTextColor(Color.parseColor("#AAAAAA"));
        leftAxis.setTextSize(10f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Math.round(value) + "°";
            }
        });

        // Disable right Y-Axis
        temperatureChart.getAxisRight().setEnabled(false);
    }

    /**
     * Fetch forecast data for 24-hour temperature trend
     * OpenWeatherMap forecast provides data in 3-hour intervals, so we get 8 data points for 24 hours
     */
    private void fetchForecast(double lat, double lon) {
        // 8 data points * 3 hours = 24 hours of forecast data
        apiService.getForecast(lat, lon, API_KEY, "metric", 8)
                .enqueue(new Callback<ForecastResponse>() {
                    @Override
                    public void onResponse(Call<ForecastResponse> call, Response<ForecastResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getList() != null) {
                            List<ForecastResponse.HourlyData> hourlyDataList = response.body().getList();
                            updateChart(hourlyDataList);
                        } else {
                            Log.e(TAG, "Forecast response error: " + response.code());
                            showChartError();
                        }
                    }

                    @Override
                    public void onFailure(Call<ForecastResponse> call, Throwable t) {
                        Log.e(TAG, "Forecast network error: " + t.getMessage());
                        showChartError();
                    }
                });
    }

    /**
     * Update the chart with forecast temperature data
     */
    private void updateChart(List<ForecastResponse.HourlyData> hourlyDataList) {
        if (temperatureChart == null || hourlyDataList == null || hourlyDataList.isEmpty()) {
            showChartError();
            return;
        }

        forecastTimestamps.clear();
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < hourlyDataList.size(); i++) {
            ForecastResponse.HourlyData data = hourlyDataList.get(i);
            if (data.getMain() != null) {
                entries.add(new Entry(i, (float) data.getMain().getTemp()));
                forecastTimestamps.add(data.getDt());
            }
        }

        if (entries.isEmpty()) {
            showChartError();
            return;
        }

        // Create dataset with styling
        LineDataSet dataSet = new LineDataSet(entries, "Temperature");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FF9800"));
        dataSet.setFillAlpha(50);
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#FF9800"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.parseColor("#1E1E1E"));
        dataSet.setCircleHoleRadius(2f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Math.round(value) + "°";
            }
        });

        LineData lineData = new LineData(dataSet);
        temperatureChart.setData(lineData);
        temperatureChart.invalidate();

        // Show chart and hide loading indicator
        txtChartLoading.setVisibility(View.GONE);
        temperatureChart.setVisibility(View.VISIBLE);
        temperatureChart.animateX(1000);
    }

    /**
     * Show error message when chart data cannot be loaded
     */
    private void showChartError() {
        if (txtChartLoading != null) {
            txtChartLoading.setText("Unable to load trend data");
        }
    }
}