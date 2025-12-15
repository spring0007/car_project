package com.launcher.yfd_ui2.data.model;

import com.google.gson.annotations.SerializedName;

public class DailyWeather {

    @SerializedName("tmp")
    private final String tmp;
    @SerializedName("condCode")
    private final String condCode;
    @SerializedName("location")
    private final String location;
    @SerializedName("tmpMax")
    private final String tmpMax;
    @SerializedName("tmpMin")
    private final String tmpMin;
    @SerializedName("date")
    private final String date;
    @SerializedName("windDir")
    private final int windDir;
    @SerializedName("uv")
    private final int uv;
    @SerializedName("humidity")
    private final int humidity;
    @SerializedName("windSpeed")
    private final String windSpeed;
    @SerializedName("sr")
    private final String sunrise;
    @SerializedName("ss")
    private final String sunset;
    @SerializedName("mr")
    private final String moonrise;
    @SerializedName("ms")
    private final String moonset;
    @SerializedName("mp")
    private final String moonPhase;

    public DailyWeather(String tmp,
                        String condCode,
                        String location,
                        String tmpMax,
                        String tmpMin,
                        String date,
                        int windDir,
                        int uv,
                        int humidity,
                        String windSpeed,
                        String sunrise,
                        String sunset,
                        String moonrise,
                        String moonset,
                        String moonPhase) {
        this.tmp = tmp;
        this.condCode = condCode;
        this.location = location;
        this.tmpMax = tmpMax;
        this.tmpMin = tmpMin;
        this.date = date;
        this.windDir = windDir;
        this.uv = uv;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.moonrise = moonrise;
        this.moonset = moonset;
        this.moonPhase = moonPhase;
    }

    public String getTmp() {
        return tmp;
    }

    public String getCondCode() {
        return condCode;
    }

    public String getLocation() {
        return location;
    }

    public String getTmpMax() {
        return tmpMax;
    }

    public String getTmpMin() {
        return tmpMin;
    }

    public String getDate() {
        return date;
    }

    public int getWindDir() {
        return windDir;
    }

    public int getUv() {
        return uv;
    }

    public int getHumidity() {
        return humidity;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public String getSunrise() {
        return sunrise;
    }

    public String getSunset() {
        return sunset;
    }

    public String getMoonrise() {
        return moonrise;
    }

    public String getMoonset() {
        return moonset;
    }

    public String getMoonPhase() {
        return moonPhase;
    }
}





