package com.launcher.yfd_ui4.utils;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.launcher.yfd_ui4.R;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeatherIconLoader {

    private static final String TAG = WeatherIconLoader.class.getSimpleName();

    private WeatherIconLoader() {
    }

    private static final String ASSET_DIR = "weather-icons";

    public static void load(ImageView imageView, String code) {
        Context context = imageView.getContext();
        String normalized = iconExists(context, code + ".svg") ? code : "100";
        String path = ASSET_DIR + "/" + normalized + ".svg";
        try {
            SVG svg;
            try (java.io.InputStream inputStream = context.getAssets().open(path)) {
                StringBuilder contentBuilder = new StringBuilder();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    contentBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
                String svgContent = contentBuilder.toString();

                int iconColor = ContextCompat.getColor(context, R.color.white);
                String iconColorHex = String.format("#%06X", (0xFFFFFF & iconColor));

                svgContent = svgContent.replaceAll("fill=[\"']currentColor[\"']", "fill=\"" + iconColorHex + "\"");

                svgContent = svgContent.replaceAll("fill=\"[^\"]*\"", "fill=\"" + iconColorHex + "\"");
                svgContent = svgContent.replaceAll("fill='[^']*'", "fill=\"" + iconColorHex + "\"");

                Pattern pathPattern = Pattern.compile("<path([^>]*?)(/?)>");
                Matcher matcher = pathPattern.matcher(svgContent);
                StringBuffer result = new StringBuffer();
                while (matcher.find()) {
                    String attrs = matcher.group(1);
                    String closing = matcher.group(2);
                    String replacement;
                    if (!attrs.contains("fill=")) {
                        replacement = "<path" + attrs + " fill=\"" + iconColorHex + "\"" + closing + ">";
                    } else {
                        replacement = matcher.group(0);
                    }
                    matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
                }
                matcher.appendTail(result);
                svgContent = result.toString();
                Log.i(TAG, "svgContent=>" + svgContent);
                svg = SVG.getFromString(svgContent);
            }
            PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
            imageView.setLayerType(ImageView.LAYER_TYPE_SOFTWARE, null);
            imageView.setImageDrawable(drawable);
        } catch (IOException | SVGParseException e) {
            imageView.setImageDrawable(ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.drawable.ic_launcher_foreground,
                    context.getTheme()
            ));
            Log.e(TAG, "load: huang error=>" + e);
        }
    }

    private static boolean iconExists(Context context, String fileName) {
        try {
            String[] files = context.getAssets().list(ASSET_DIR);
            if (files == null) {
                return false;
            }
            for (String file : files) {
                if (fileName.equals(file)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }
    private static final Map<String, Integer> ICONS = new HashMap<>();

    static {
        ICONS.put("100", R.drawable.weather_qing_icon);
        ICONS.put("101", R.drawable.weather_duoyun_icon);
        ICONS.put("102", R.drawable.weather_duoyun_icon);
        ICONS.put("103", R.drawable.weather_duoyun_icon);
        ICONS.put("104", R.drawable.weather_yin_icon);
        ICONS.put("300", R.drawable.weather_zhenyu_icon);
        ICONS.put("301", R.drawable.weather_dabaoyu_icon);
        ICONS.put("302", R.drawable.weather_leizhenyu_icon);
        ICONS.put("303", R.drawable.weather_leizhenyu_icon);
        ICONS.put("304", R.drawable.weather_leizhenyubanyoubingbao_icon);
        ICONS.put("305", R.drawable.weather_xiaoyu_icon);
        ICONS.put("306", R.drawable.weather_zhongyu_icon);
        ICONS.put("307", R.drawable.weather_dayu_icon);
        ICONS.put("308", R.drawable.weather_tedabaoyu_icon);
        ICONS.put("309", R.drawable.weather_xiaoyu_icon);
        ICONS.put("310", R.drawable.weather_dayu_icon);
        ICONS.put("311", R.drawable.weather_dayu_icon);
        ICONS.put("312", R.drawable.weather_tedabaoyu_icon);
        ICONS.put("313", R.drawable.weather_tedabaoyu_icon);
        ICONS.put("400", R.drawable.weather_xiaoxue_icon);
        ICONS.put("401", R.drawable.weather_zhongxue_icon);
        ICONS.put("402", R.drawable.weather_daxue_icon);
        ICONS.put("403", R.drawable.weather_baoxue_icon);
        ICONS.put("404", R.drawable.weather_bingyu_icon);
        ICONS.put("405", R.drawable.weather_yujiaxue_icon);
        ICONS.put("406", R.drawable.weather_zhenxue_night_icon);
        ICONS.put("407", R.drawable.weather_xiaoxue_icon);
        ICONS.put("500", R.drawable.weather_wu_icon);
        ICONS.put("501", R.drawable.weather_wu_icon);
        ICONS.put("502", R.drawable.weather_mai_icon);
        ICONS.put("503", R.drawable.weather_mai_icon);
        ICONS.put("504", R.drawable.weather_wu_icon);
        ICONS.put("507", R.drawable.weather_shachenbao_icon);
        ICONS.put("508", R.drawable.weather_yangsha_icon);
    }

    public static void getWeatherIcon(ImageView imageView,String code) {
        if (code == null) {
            imageView.setImageResource(R.drawable.weather_na_icon);
            return;
        }
        Integer resId = ICONS.get(code);
        if (resId == null  || resId <= 0) {
            imageView.setImageResource(R.drawable.weather_na_icon);
            return;
        }
        imageView.setImageResource( resId);
    }
}

