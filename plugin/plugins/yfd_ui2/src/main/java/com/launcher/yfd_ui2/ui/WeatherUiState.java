package com.launcher.yfd_ui2.ui;


import com.launcher.yfd_ui2.data.model.DailyWeather;

import java.util.Collections;
import java.util.List;

public class WeatherUiState {

    private final boolean isLoading;
    private final List<DailyWeather> forecast;
    private final String error;

    public WeatherUiState(boolean isLoading, List<DailyWeather> forecast, String error) {
        this.isLoading = isLoading;
        this.forecast = forecast == null ? Collections.emptyList() : forecast;
        this.error = error;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public List<DailyWeather> getForecast() {
        return forecast;
    }

    public String getError() {
        return error;
    }
}





