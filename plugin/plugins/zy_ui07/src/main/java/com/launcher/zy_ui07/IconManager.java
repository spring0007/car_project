package com.launcher.zy_ui07;

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

    static {
        Map<String, String> map = new HashMap<>();
        map.put("com.awell.localvideo", "zy07_video");
        map.put("com.awell.localmusic", "zy07_music");
        map.put("com.awell.radio", "zy07_radio");
         map.put("com.awell.bluetooth", "zy07_btphone");
        map.put("com.awell.navigation", "zy07_navi");
        map.put("com.android.browser", "zy07_liulanqi");
        map.put("com.mediatek.filemanager", "zy07_wjgl");
        map.put("com.android.documentsui", "zy07_wjgl");
        map.put("com.awell.carsetting", "zy07_shezhi");
        map.put("com.awell.backcar", "zy07_wjsr");
        map.put("cn.kuwo.kwmusiccar", "zy07_kuwo");
        map.put("com.awell.eqselect", "zy07_jhq");
         map.put("com.awell.canbus", "zy07_yuanche");
        // map.put("com.awell.canbus2", "zy07_yuanche");
        //map.put("com.autonavi.amapauto", "zy07_navi");
        // map.put("com.tima.carnet.vt", "zy07_zlink");
        map.put("net.easyconn", "zy07_net_easyconn");
        // map.put("com.zjinnova.zlink", "zy07_zlink");
        map.put("com.awell.awellmanual", "zy07_manual");
        map.put("com.google.android.apps.maps", "zy07_googlemap");
        map.put("com.google.android.youtube", "zy07_youtube");
        map.put("com.android.vending", "zy07_playstore");
        map.put("com.android.chrome", "zy07_liulanqi");
        map.put("com.google.android.googlequicksearchbox", "zy07_gg");
        map.put("com.android.gallery3d", "zy07_gallery");
        map.put("org.chromium.chrome", "zy07_liulanqi");
        map.put("com.awell.electricfan", "zy07_fan");
        map.put("com.awell.frontvideo", "zy07_front_video");
        // map.put("com.awell.keylight", "zy07_color_light");
        map.put("com.awell.keystudy", "zy07_training");
        map.put("com.awell.themesetting", "zy07_theme");
         map.put("com.ms.ms2160", "zy07_usb_video_output");
        map.put("com.awell.weather", "zy07_weather");
        map.put(".AwellCanbusAirView", "zy07_air");

        // 包装为不可变Map，与Kotlin的mapOf行为一致
        PACKAGE_ICON_MAP = Collections.unmodifiableMap(map);
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

    // 静态内部类持有单例
    private static final class Holder {
        private static final IconManager INSTANCE = new IconManager();
    }
}