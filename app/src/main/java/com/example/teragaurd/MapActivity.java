package com.example.teragaurd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView temperatureTextView;
    private TextView aqiTextView;
    private TextView txtLocationName;
    private TextView txtCoordinates;
    private FloatingActionButton fabBack;
    private boolean dataFetched = false;
    private PointAnnotationManager pointAnnotationManager;
    private final String openWeatherMapApiKey = "72aa5ebf5045980623cd8ff3e86a6e01";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize views
        mapView = findViewById(R.id.mapView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        aqiTextView = findViewById(R.id.aqiTextView);
        txtLocationName = findViewById(R.id.txtLocationName);
        txtCoordinates = findViewById(R.id.txtCoordinates);
        fabBack = findViewById(R.id.fabBack);

        // Set loading state
        temperatureTextView.setText("Loading...");
        aqiTextView.setText("Loading...");
        txtLocationName.setText("Fetching location...");
        txtCoordinates.setText("");

        // Back button functionality
        fabBack.setOnClickListener(v -> finish());

        // Initialize Mapbox map with dark style
        mapView.getMapboxMap().loadStyleUri(Style.DARK, style -> {
            Log.d(TAG, "Mapbox style loaded successfully");
            
            // Setup annotation manager for markers
            AnnotationPlugin annotationPlugin = AnnotationPluginImplKt.getAnnotations(mapView);
            pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, mapView);
            
            // Check location permission and get location
            if (hasLocationPermission()) {
                getUserLocation();
            } else {
                requestLocationPermission();
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || dataFetched) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    dataFetched = true;
                    handleLocationUpdate(location);
                    stopLocationUpdates();
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void getUserLocation() {
        Log.d(TAG, "Getting user location...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && !dataFetched) {
                        dataFetched = true;
                        handleLocationUpdate(location);
                    } else if (!dataFetched) {
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
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void handleLocationUpdate(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        Log.d(TAG, "Location received: " + lat + ", " + lon);

        // Update coordinates display
        txtCoordinates.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon));

        // Get address from coordinates
        getAddressFromLocation(lat, lon);

        // Move map camera to location
        Point point = Point.fromLngLat(lon, lat);
        CameraOptions cameraOptions = new CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build();
        mapView.getMapboxMap().setCamera(cameraOptions);

        // Add marker at current location
        addMarkerAtLocation(lat, lon);

        // Fetch weather and AQI data
        fetchWeatherData(lat, lon);
        fetchAqiData(lat, lon);
    }

    private void addMarkerAtLocation(double lat, double lon) {
        if (pointAnnotationManager == null) return;

        // Create a bitmap from a drawable for the marker
        Drawable drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
        if (drawable == null) return;

        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        // Create point annotation (marker)
        PointAnnotationOptions pointAnnotationOptions = new PointAnnotationOptions()
                .withPoint(Point.fromLngLat(lon, lat))
                .withIconImage(bitmap);

        pointAnnotationManager.create(pointAnnotationOptions);
    }

    private void getAddressFromLocation(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String locationName = "";
                
                if (address.getLocality() != null) {
                    locationName = address.getLocality();
                } else if (address.getSubAdminArea() != null) {
                    locationName = address.getSubAdminArea();
                } else if (address.getAdminArea() != null) {
                    locationName = address.getAdminArea();
                }
                
                if (address.getCountryName() != null && !locationName.isEmpty()) {
                    locationName += ", " + address.getCountryName();
                }
                
                if (!locationName.isEmpty()) {
                    txtLocationName.setText(locationName);
                } else {
                    txtLocationName.setText("Unknown Location");
                }
            } else {
                txtLocationName.setText("Unknown Location");
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error: " + e.getMessage());
            txtLocationName.setText("Location unavailable");
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void fetchWeatherData(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<WeatherResponse> call = apiService.getWeather(latitude, longitude, openWeatherMapApiKey, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    if (weatherResponse.getMain() != null) {
                        double temp = weatherResponse.getMain().getTemp();
                        temperatureTextView.setText(Math.round(temp) + "°C");
                    }
                } else {
                    Log.e(TAG, "Weather API error: " + response.code());
                    temperatureTextView.setText("--");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e(TAG, "Weather API failure: " + t.getMessage());
                temperatureTextView.setText("--");
            }
        });
    }

    private void fetchAqiData(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<AqiResponse> call = apiService.getAqi(latitude, longitude, openWeatherMapApiKey);

        call.enqueue(new Callback<AqiResponse>() {
            @Override
            public void onResponse(Call<AqiResponse> call, Response<AqiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AqiResponse aqiResponse = response.body();
                    if (aqiResponse.getList() != null && !aqiResponse.getList().isEmpty()) {
                        int aqi = aqiResponse.getList().get(0).getMain().getAqi();
                        String aqiStatus = getAqiStatusText(aqi);
                        aqiTextView.setText(aqi + " (" + aqiStatus + ")");
                    }
                } else {
                    Log.e(TAG, "AQI API error: " + response.code());
                    aqiTextView.setText("--");
                }
            }

            @Override
            public void onFailure(Call<AqiResponse> call, Throwable t) {
                Log.e(TAG, "AQI API failure: " + t.getMessage());
                aqiTextView.setText("--");
            }
        });
    }

    private String getAqiStatusText(int aqi) {
        switch (aqi) {
            case 1: return "Good";
            case 2: return "Fair";
            case 3: return "Moderate";
            case 4: return "Poor";
            case 5: return "Very Poor";
            default: return "Unknown";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Location permission is required for map features", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
