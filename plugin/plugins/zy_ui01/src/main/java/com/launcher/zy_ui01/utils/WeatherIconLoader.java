package com.launcher.zy_ui01.utils;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.launcher.zy_ui01.R;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
}

