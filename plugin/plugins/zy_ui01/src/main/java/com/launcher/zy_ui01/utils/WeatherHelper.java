package com.launcher.zy_ui01.utils;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.launcher.zy_ui01.model.WeatherContract;

import java.util.ArrayList;
import java.util.List;

public class WeatherHelper {
    private static String TAG = WeatherHelper.class.getSimpleName();

    /**
     * 获取当前天气信息
     * @return WeatherInfo 对象，如果查询失败或没有数据返回 null
     */
    public static WeatherInfo getCurrentWeather(Context context) {
        Uri uri = WeatherContract.CurrentWeather.CONTENT_URI;
        String[] projection = {
                WeatherContract.CurrentWeather.COLUMN_LOCATION,
                WeatherContract.CurrentWeather.COLUMN_TMP,
                WeatherContract.CurrentWeather.COLUMN_TMP_MIN,
                WeatherContract.CurrentWeather.COLUMN_TMP_MAX,
                WeatherContract.CurrentWeather.COLUMN_COND_CODE,
                WeatherContract.CurrentWeather.COLUMN_UPDATE_TIME
        };

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.i(TAG, "cursor is null");
                return null;
            }

            WeatherInfo info = new WeatherInfo();
            int index;

            index = cursor.getColumnIndex(WeatherContract.CurrentWeather.COLUMN_LOCATION);
            info.location = index >= 0 ? cursor.getString(index) : null;

            index = cursor.getColumnIndex(WeatherContract.CurrentWeather.COLUMN_TMP);
            info.temperature = index >= 0 ? cursor.getString(index) : null;

            index = cursor.getColumnIndex(WeatherContract.CurrentWeather.COLUMN_TMP_MIN);
            info.tempMin = index >= 0 ? cursor.getString(index) : null;

            index = cursor.getColumnIndex(WeatherContract.CurrentWeather.COLUMN_TMP_MAX);
            info.tempMax = index >= 0 ? cursor.getString(index) : null;

            index = cursor.getColumnIndex(WeatherContract.CurrentWeather.COLUMN_COND_CODE);
            info.condCode = index >= 0 ? cursor.getString(index) : null;

            index = cursor.getColumnIndex(WeatherContract.CurrentWeather.COLUMN_UPDATE_TIME);
            info.updateTime = index >= 0 ? cursor.getLong(index) : 0;

            return info;
        } catch (SecurityException e) {
            Log.e("WeatherHelper", "Permission denied", e);
            return null;
        } catch (Exception e) {
            Log.e("WeatherHelper", "Error querying weather", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 获取天气预报（多天）
     */
    public static List<ForecastInfo> getForecast(Context context) {
        Uri uri = WeatherContract.Forecast.CONTENT_URI;
        String[] projection = {
                WeatherContract.Forecast.COLUMN_DATE,
                WeatherContract.Forecast.COLUMN_TMP_MIN,
                WeatherContract.Forecast.COLUMN_TMP_MAX,
                WeatherContract.Forecast.COLUMN_COND_CODE
        };

        List<ForecastInfo> forecastList = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor == null) {
                return forecastList;
            }

            while (cursor.moveToNext()) {
                ForecastInfo info = new ForecastInfo();

                int index = cursor.getColumnIndex(WeatherContract.Forecast.COLUMN_DATE);
                info.date = index >= 0 ? cursor.getString(index) : null;

                index = cursor.getColumnIndex(WeatherContract.Forecast.COLUMN_TMP_MIN);
                info.tempMin = index >= 0 ? cursor.getString(index) : null;

                index = cursor.getColumnIndex(WeatherContract.Forecast.COLUMN_TMP_MAX);
                info.tempMax = index >= 0 ? cursor.getString(index) : null;

                index = cursor.getColumnIndex(WeatherContract.Forecast.COLUMN_COND_CODE);
                info.condCode = index >= 0 ? cursor.getString(index) : null;

                forecastList.add(info);
            }
        } catch (Exception e) {
            Log.e("WeatherHelper", "Error querying forecast", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return forecastList;
    }

    /**
     * 数据类
     */
    public static class WeatherInfo {
        public String location;
        public String temperature;
        public String tempMin;
        public String tempMax;
        public String condCode;
        public long updateTime; // 时间戳（毫秒）
    }

    public static class ForecastInfo {
        public String date;
        public String tempMin;
        public String tempMax;
        public String condCode;
    }
}