package com.example.teragaurd;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("weather")
    Call<WeatherResponse> getWeather(@Query("lat") double lat, @Query("lon") double lon, @Query("appid") String apiKey, @Query("units") String units);

    @GET("air_pollution")
    Call<AqiResponse> getAqi(@Query("lat") double lat, @Query("lon") double lon, @Query("appid") String apiKey);

    // Forecast API for 24-hour temperature trend (provides data in 3-hour intervals for 5 days)
    @GET("forecast")
    Call<ForecastResponse> getForecast(@Query("lat") double lat, @Query("lon") double lon, @Query("appid") String apiKey, @Query("units") String units, @Query("cnt") int count);
}
