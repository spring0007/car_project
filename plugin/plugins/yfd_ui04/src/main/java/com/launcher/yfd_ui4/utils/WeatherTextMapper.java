package com.launcher.yfd_ui4.utils;

import android.content.Context;

import androidx.annotation.StringRes;

import com.launcher.yfd_ui4.R;

import java.util.HashMap;
import java.util.Map;

public final class WeatherTextMapper {

    private WeatherTextMapper() {
    }

    private static final Map<String, Integer> DESCRIPTIONS = new HashMap<>();

    static {
        DESCRIPTIONS.put("100", R.string.weather_desc_100);
        DESCRIPTIONS.put("101", R.string.weather_desc_101);
        DESCRIPTIONS.put("102", R.string.weather_desc_102);
        DESCRIPTIONS.put("103", R.string.weather_desc_103);
        DESCRIPTIONS.put("104", R.string.weather_desc_104);
        DESCRIPTIONS.put("300", R.string.weather_desc_300);
        DESCRIPTIONS.put("301", R.string.weather_desc_301);
        DESCRIPTIONS.put("302", R.string.weather_desc_302);
        DESCRIPTIONS.put("303", R.string.weather_desc_303);
        DESCRIPTIONS.put("304", R.string.weather_desc_304);
        DESCRIPTIONS.put("305", R.string.weather_desc_305);
        DESCRIPTIONS.put("306", R.string.weather_desc_306);
        DESCRIPTIONS.put("307", R.string.weather_desc_307);
        DESCRIPTIONS.put("308", R.string.weather_desc_308);
        DESCRIPTIONS.put("309", R.string.weather_desc_309);
        DESCRIPTIONS.put("310", R.string.weather_desc_310);
        DESCRIPTIONS.put("311", R.string.weather_desc_311);
        DESCRIPTIONS.put("312", R.string.weather_desc_312);
        DESCRIPTIONS.put("313", R.string.weather_desc_313);
        DESCRIPTIONS.put("400", R.string.weather_desc_400);
        DESCRIPTIONS.put("401", R.string.weather_desc_401);
        DESCRIPTIONS.put("402", R.string.weather_desc_402);
        DESCRIPTIONS.put("403", R.string.weather_desc_403);
        DESCRIPTIONS.put("404", R.string.weather_desc_404);
        DESCRIPTIONS.put("405", R.string.weather_desc_405);
        DESCRIPTIONS.put("406", R.string.weather_desc_406);
        DESCRIPTIONS.put("407", R.string.weather_desc_407);
        DESCRIPTIONS.put("500", R.string.weather_desc_500);
        DESCRIPTIONS.put("501", R.string.weather_desc_501);
        DESCRIPTIONS.put("502", R.string.weather_desc_502);
        DESCRIPTIONS.put("503", R.string.weather_desc_503);
        DESCRIPTIONS.put("504", R.string.weather_desc_504);
        DESCRIPTIONS.put("507", R.string.weather_desc_507);
        DESCRIPTIONS.put("508", R.string.weather_desc_508);
    }

    public static String description(Context context, String code) {
        if (code == null) {
            return context.getString(R.string.weather_desc_unknown);
        }
        @StringRes Integer resId = DESCRIPTIONS.get(code);
        if (resId == null) {
            return context.getString(R.string.weather_desc_unknown);
        }
        return context.getString(resId);
    }


}
