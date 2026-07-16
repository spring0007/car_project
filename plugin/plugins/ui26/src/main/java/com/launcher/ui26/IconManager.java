package com.launcher.ui26;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 图标管理器
 * 支持根据主题模式加载不同的应用图标背景
 */
public final class IconManager {

    private static final String TAG = "IconManager";
    private static final Map<String, String> PACKAGE_ICON_MAP;
    private static final Map<String, Integer> PACKAGE_ICON_MAP_DARK ;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("com.awell.localvideo", "ic_video");
        map.put("com.awell.localmusic", "ic_music");
        map.put("com.awell.radio", "ic_radio");
         map.put("com.awell.bluetooth", "ic_btphone");
        map.put("com.awell.navigation", "ic_navi");
        map.put("com.android.browser", "ic_liulanqi");
        map.put("com.mediatek.filemanager", "ic_wjgl");
        map.put("com.android.documentsui", "ic_wjgl");
        map.put("com.awell.carsetting", "ic_shezhi");
        map.put("com.awell.backcar", "ic_wjsr");
        map.put("cn.kuwo.kwmusiccar", "ic_kuwo");
        map.put("com.awell.eqselect", "ic_jhq");
         map.put("com.awell.canbus", "ic_yuanche");
        // map.put("com.awell.canbus2", "ic_yuanche");
        //map.put("com.autonavi.amapauto", "ic_navi");
        // map.put("com.tima.carnet.vt", "ic_zlink");
        map.put("net.easyconn", "ic_net_easyconn");
        // map.put("com.zjinnova.zlink", "ic_zlink");
        map.put("com.awell.awellmanual", "ic_manual");
        //map.put("com.google.android.apps.maps", "ic_googlemap");
        //map.put("com.google.android.youtube", "ic_youtube");
        //map.put("com.android.vending", "ic_playstore");
        map.put("com.android.chrome", "ic_liulanqi");
        //map.put("com.google.android.googlequicksearchbox", "ic_gg");
        map.put("com.android.gallery3d", "ic_gallery");
        map.put("org.chromium.chrome", "ic_liulanqi");
        //map.put("com.awell.electricfan", "ic_fan");
        map.put("com.awell.frontvideo", "ic_front_video");
        // map.put("com.awell.keylight", "ic_color_light");
        map.put("com.awell.keystudy", "ic_training");
        map.put("com.awell.themesetting", "ic_theme");
         map.put("com.ms.ms2160", "ic_usb_video_output");
        map.put("com.awell.weather", "ic_weather");
        map.put(".AwellCanbusAirView", "ic_air");

        // 包装为不可变Map，与Kotlin的mapOf行为一致
        PACKAGE_ICON_MAP = Collections.unmodifiableMap(map);
    }

    static {
        Map<String, Integer> map = new HashMap<>();
        map.put("com.awell.localvideo", R.drawable.ic_video);
        map.put("com.awell.localmusic", R.drawable.ic_music);
        map.put("com.awell.radio", R.drawable.ic_radio);
        map.put("com.awell.bluetooth", R.drawable.ic_btphone);
        map.put("com.awell.navigation", R.drawable.ic_navi);
        map.put("com.android.browser", R.drawable.ic_liulanqi);
        map.put("com.mediatek.filemanager", R.drawable.ic_wjgl);
        map.put("com.android.documentsui", R.drawable.ic_wjgl);
        map.put("com.awell.carsetting", R.drawable.ic_shezhi);
        map.put("com.awell.backcar", R.drawable.ic_wjsr);
        map.put("cn.kuwo.kwmusiccar", R.drawable.ic_kuwo);
        map.put("com.awell.eqselect", R.drawable.ic_jhq);
        map.put("com.awell.canbus", R.drawable.ic_yuanche);
        // map.put("com.awell.canbus2", R.drawable.ic_yuanche);
        //map.put("com.autonavi.amapauto", R.drawable.ic_navi);
        // map.put("com.tima.carnet.vt", R.drawable.ic_zlink);
        map.put("net.easyconn", R.drawable.ic_net_easyconn);
        // map.put("com.zjinnova.zlink", R.drawable.ic_zlink);
        map.put("com.awell.awellmanual", R.drawable.ic_manual);
        //map.put("com.google.android.apps.maps", R.drawable.ic_googlemap);
        //map.put("com.google.android.youtube", R.drawable.ic_youtube);
        //map.put("com.android.vending", R.drawable.ic_playstore);
        map.put("com.android.chrome", R.drawable.ic_liulanqi);
        //map.put("com.google.android.googlequicksearchbox", R.drawable.ic_gg);
        map.put("com.android.gallery3d", R.drawable.ic_gallery);
        map.put("org.chromium.chrome", R.drawable.ic_liulanqi);
        //map.put("com.awell.electricfan", R.drawable.ic_fan);
        map.put("com.awell.frontvideo", R.drawable.ic_front_video);
        // map.put("com.awell.keylight", R.drawable.ic_color_light);
        map.put("com.awell.keystudy", R.drawable.ic_training);
        map.put("com.awell.themesetting", R.drawable.ic_theme);
        map.put("com.ms.ms2160", R.drawable.ic_usb_video_output);
        map.put("com.awell.weather", R.drawable.ic_weather);
        map.put(".AwellCanbusAirView", R.drawable.ic_air);

        // 包装为不可变Map，与Kotlin的mapOf行为一致
        PACKAGE_ICON_MAP_DARK = Collections.unmodifiableMap(map);
    }

    private IconManager() {
        // 私有构造函数，防止外部实例化
    }

    /**
     * 获取图标管理器实例（如果需要单例对象本身）
     * @return IconManager单例
     */
    public static IconManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 获取图标映射表（供外部访问）
     * @return 不可变的图标映射表
     */
    public static Map<String, String> getPackageIconMap() {
        return PACKAGE_ICON_MAP;
    }

    public static Map<String, Integer> getPackageIconDarkMap() {
        return PACKAGE_ICON_MAP_DARK;
    }

    // 静态内部类持有单例
    private static final class Holder {
        private static final IconManager INSTANCE = new IconManager();
    }
}