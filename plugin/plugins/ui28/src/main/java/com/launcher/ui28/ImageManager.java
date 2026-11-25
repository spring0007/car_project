package com.launcher.ui28;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.ImageView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ImageManager {

    private static final String TAG = "ImageManager";

    public static final Map<String, Integer> mHomePackName = new HashMap<>();
    static {
        mHomePackName.put("com.awell.localvideo", R.drawable.sf_video);
        mHomePackName.put("com.awell.localmusic", R.drawable.sf_music);
        mHomePackName.put("com.awell.radio", R.drawable.sf_radio);
        mHomePackName.put("com.awell.bluetooth", R.drawable.sf_bt);
        mHomePackName.put("com.awell.navigation", R.drawable.sf_navi);
        mHomePackName.put("com.android.browser", R.drawable.sf_liulanqi);
        mHomePackName.put("com.mediatek.filemanager", R.drawable.sf_wjgl);
        mHomePackName.put("com.android.documentsui", R.drawable.sf_wjgl);
        mHomePackName.put("com.awell.carsetting", R.drawable.sf_shezhi);
        mHomePackName.put("com.awell.backcar", R.drawable.sf_wjsr);
        mHomePackName.put("cn.kuwo.kwmusiccar", R.drawable.sf_kwmusiccar);
        mHomePackName.put("com.awell.eqselect", R.drawable.sf_jhq);
        mHomePackName.put("com.awell.canbus", R.drawable.sf_yuanche);
        mHomePackName.put("com.awell.canbus2", R.drawable.sf_yuanche);
        mHomePackName.put("com.autonavi.amapauto", R.drawable.sf_gaode);
        // mHomePackName.put("com.tima.carnet.vt", R.drawable.sf_zlink);
        //mHomePackName.put("net.easyconn", R.drawable.sf_zlink);
        mHomePackName.put("com.zjinnova.zlink", R.drawable.sf_zlink);
        mHomePackName.put("com.awell.awellmanual", R.drawable.sf_manual);
        mHomePackName.put("com.google.android.apps.maps", R.drawable.sf_googlemap);
        mHomePackName.put("com.google.android.youtube", R.drawable.sf_youtube);
        mHomePackName.put("com.android.vending", R.drawable.sf_playstore);
        mHomePackName.put("com.android.chrome", R.drawable.sf_safri);
        mHomePackName.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg);
        mHomePackName.put("com.android.gallery3d", R.drawable.sf_gallery);
        mHomePackName.put("org.chromium.chrome", R.drawable.sf_chrome);
        mHomePackName.put("com.awell.electricfan", R.drawable.sf_fan);
        mHomePackName.put("com.awell.frontvideo", R.drawable.sf_front_video);
        mHomePackName.put("com.awell.keylight", R.drawable.sf_color_light);
        mHomePackName.put("com.awell.keystudy", R.drawable.sf_training);
        mHomePackName.put("com.awell.themesetting", R.drawable.sf_theme);
        mHomePackName.put("com.ms.ms2160", R.drawable.sf_usb_video_output);
        mHomePackName.put("com.awell.weather", R.drawable.sf_weather);
    }

    // 包名 -> 图片资源名(不带 R.drawable. 前缀),供宿主 PluginIconManager 使用
    private static final Map<String, String> sPackageIconMap = new HashMap<>();
    static {
        sPackageIconMap.put("com.awell.localvideo", "sf_video");
        sPackageIconMap.put("com.awell.localmusic", "sf_music");
        sPackageIconMap.put("com.awell.radio", "sf_radio");
        sPackageIconMap.put("com.awell.bluetooth", "sf_bt");
        sPackageIconMap.put("com.awell.navigation", "sf_navi");
        sPackageIconMap.put("com.android.browser", "sf_liulanqi");
        sPackageIconMap.put("com.mediatek.filemanager", "sf_wjgl");
        sPackageIconMap.put("com.android.documentsui", "sf_wjgl");
        sPackageIconMap.put("com.awell.carsetting", "sf_shezhi");
        sPackageIconMap.put("com.awell.backcar", "sf_wjsr");
        sPackageIconMap.put("cn.kuwo.kwmusiccar", "sf_kwmusiccar");
        sPackageIconMap.put("com.awell.eqselect", "sf_jhq");
        sPackageIconMap.put("com.awell.canbus", "sf_yuanche");
        sPackageIconMap.put("com.awell.canbus2", "sf_yuanche");
        sPackageIconMap.put("com.autonavi.amapauto", "sf_gaode");
        sPackageIconMap.put("com.zjinnova.zlink", "sf_zlink");
        sPackageIconMap.put("com.awell.awellmanual", "sf_manual");
        sPackageIconMap.put("com.google.android.apps.maps", "sf_googlemap");
        sPackageIconMap.put("com.google.android.youtube", "sf_youtube");
        sPackageIconMap.put("com.android.vending", "sf_playstore");
        sPackageIconMap.put("com.android.chrome", "sf_safri");
        sPackageIconMap.put("com.google.android.googlequicksearchbox", "sf_gg");
        sPackageIconMap.put("com.android.gallery3d", "sf_gallery");
        sPackageIconMap.put("org.chromium.chrome", "sf_chrome");
        sPackageIconMap.put("com.awell.electricfan", "sf_fan");
        sPackageIconMap.put("com.awell.frontvideo", "sf_front_video");
        sPackageIconMap.put("com.awell.keylight", "sf_color_light");
        sPackageIconMap.put("com.awell.keystudy", "sf_training");
        sPackageIconMap.put("com.awell.themesetting", "sf_theme");
        sPackageIconMap.put("com.ms.ms2160", "sf_usb_video_output");
        sPackageIconMap.put("com.awell.weather", "sf_weather");
        sPackageIconMap.put(".AwellCanbusAirView", "sf_air_round");
    }

    /**
     * 优先使用内置图标,没有则取应用自身图标
     */
    public static void loadIcon(Context context, ImageView view, String packageName) {
        if (view == null || packageName == null) {
            return;
        }
        Integer resId = mHomePackName.get(packageName);
        if (resId != null) {
            view.setImageResource(resId);
            return;
        }
        try {
            view.setImageDrawable(context.getPackageManager().getApplicationIcon(packageName));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "icon not found: " + packageName);
            view.setImageDrawable(null);
        }
    }

    public static Map<String, String> getPackageIconMap() {
        return sPackageIconMap;

    }
}
