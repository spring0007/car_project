package com.launcher.zy_ui04

/**
 * 图标管理器
 * 支持根据主题模式加载不同的应用图标背景
 */
object IconManager {

    private const val TAG = "IconManager"

    /**
     * 获取图标映射表（供外部访问）
     * @return 图标映射表
     */
    fun getPackageIconMap(): Map<String, String> {
        return PACKAGE_ICON_MAP
    }

    ///=============================
    val PACKAGE_ICON_MAP: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy04_video",
        "com.awell.localmusic" to "zy04_music",
        "com.awell.radio" to "zy04_radio",
       // "com.awell.bluetooth" to "zy04_btphone",
        "com.awell.navigation" to "zy04_navi",
        "com.android.browser" to "zy04_liulanqi",
        "com.mediatek.filemanager" to "zy04_wjgl",
        "com.android.documentsui" to "zy04_wjgl",
        "com.awell.carsetting" to "zy04_shezhi",
        "com.awell.backcar" to "zy04_wjsr",
        //"cn.kuwo.kwmusiccar" to "zy04_kugou",
        "com.awell.eqselect" to "zy04_jhq",
        //"com.awell.canbus" to "zy04_yuanche",
        //"com.awell.canbus2" to "zy04_yuanche",
        "com.autonavi.amapauto" to "zy04_navi",
        // "com.tima.carnet.vt" to "zy04_zlink",
        //"net.easyconn" to "zy04_zlink",
        "com.zjinnova.zlink" to "zy04_link",
        "com.awell.awellmanual" to "zy04_manual",
        // "com.google.android.apps.maps" to "zy04_googlemap",
        "com.google.android.youtube" to "zy04_youtube",
        "com.android.vending" to "zy04_playstore",
        "com.android.chrome" to "zy04_liulanqi",
        "com.google.android.googlequicksearchbox" to "zy04_gg",
        "com.android.gallery3d" to "zy04_gallery",
        "org.chromium.chrome" to "zy04_liulanqi",
        "com.awell.electricfan" to "zy04_fan",
        // "com.awell.frontvideo" to "zy04_front_video",
        // "com.awell.keylight" to "zy04_color_light",
        "com.awell.keystudy" to "zy04_training",
        "com.awell.themesetting" to "zy04_theme",
        // "com.ms.ms2160" to "zy04_usb_video_output",
        // "com.awell.weather" to "zy04_weather"
    )



}
