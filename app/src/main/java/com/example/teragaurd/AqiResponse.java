package com.example.teragaurd;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AqiResponse {
    @SerializedName("list")
    private List<AqiData> list;

    public List<AqiData> getList() {
        return list;
    }

    public static class AqiData {
        @SerializedName("main")
        private MainAqi main;

        public MainAqi getMain() {
            return main;
        }
    }

    public static class MainAqi {
        @SerializedName("aqi")
        private int aqi;

        public int getAqi() {
            return aqi;
        }
    }
}
