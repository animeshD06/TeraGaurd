package com.example.teragaurd;

import java.util.List;

/**
 * Response model for OpenWeatherMap Forecast API
 * Used to get hourly temperature data for the 24-hour trend chart
 */
public class ForecastResponse {
    private List<HourlyData> list;

    public List<HourlyData> getList() {
        return list;
    }

    public void setList(List<HourlyData> list) {
        this.list = list;
    }

    public static class HourlyData {
        private long dt; // Unix timestamp
        private Main main;

        public long getDt() {
            return dt;
        }

        public void setDt(long dt) {
            this.dt = dt;
        }

        public Main getMain() {
            return main;
        }

        public void setMain(Main main) {
            this.main = main;
        }
    }

    public static class Main {
        private double temp;
        private double feels_like;
        private double temp_min;
        private double temp_max;
        private int humidity;

        public double getTemp() {
            return temp;
        }

        public void setTemp(double temp) {
            this.temp = temp;
        }

        public double getFeelsLike() {
            return feels_like;
        }

        public void setFeelsLike(double feels_like) {
            this.feels_like = feels_like;
        }

        public double getTempMin() {
            return temp_min;
        }

        public void setTempMin(double temp_min) {
            this.temp_min = temp_min;
        }

        public double getTempMax() {
            return temp_max;
        }

        public void setTempMax(double temp_max) {
            this.temp_max = temp_max;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }
    }
}
