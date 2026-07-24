package com.awell.library.util;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * 天气 Provider 合约定义
 * 所有插件统一使用此版本，无需各自维护
 */
public class WeatherContract {
    public static final String AUTHORITY = "com.awell.weather.provider";

    public static class CurrentWeather implements BaseColumns {
        public static final String PATH_CURRENT = "current";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_CURRENT);

        public static final String COLUMN_LOCATION = "location";
        public static final String COLUMN_TMP = "tmp";
        public static final String COLUMN_TMP_MIN = "tmpMin";
        public static final String COLUMN_TMP_MAX = "tmpMax";
        public static final String COLUMN_COND_CODE = "condCode";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_WIND_DIR = "windDir";
        public static final String COLUMN_WIND_SPEED = "windSpeed";
        public static final String COLUMN_HUMIDITY = "humidity";
        public static final String COLUMN_UV = "uv";
        public static final String COLUMN_SUNRISE = "sr";
        public static final String COLUMN_SUNSET = "ss";
        public static final String COLUMN_MOONRISE = "mr";
        public static final String COLUMN_MOONSET = "ms";
        public static final String COLUMN_MOON_PHASE = "mp";
        public static final String COLUMN_UPDATE_TIME = "updateTime";
    }

    public static class Forecast implements BaseColumns {
        public static final String PATH_FORECAST = "forecast";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_FORECAST);

        public static final String COLUMN_LOCATION = "location";
        public static final String COLUMN_TMP = "tmp";
        public static final String COLUMN_TMP_MIN = "tmpMin";
        public static final String COLUMN_TMP_MAX = "tmpMax";
        public static final String COLUMN_COND_CODE = "condCode";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_WIND_DIR = "windDir";
        public static final String COLUMN_WIND_SPEED = "windSpeed";
        public static final String COLUMN_HUMIDITY = "humidity";
        public static final String COLUMN_UV = "uv";
        public static final String COLUMN_SUNRISE = "sr";
        public static final String COLUMN_SUNSET = "ss";
        public static final String COLUMN_MOONRISE = "mr";
        public static final String COLUMN_MOONSET = "ms";
        public static final String COLUMN_MOON_PHASE = "mp";
    }
}
