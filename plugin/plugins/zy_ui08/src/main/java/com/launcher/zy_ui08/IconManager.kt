package com.launcher.zy_ui8

/**
 * 图标管理器
 * 支持根据主题模式加载不同的应用图标背景
 */
object IconManager {

    private const val TAG = "IconManager"

    // 圆形主题图标映射表（包名 -> 资源名称）
    val PACKAGE_ICON_MAP: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy_ui8_video",
        "com.awell.localmusic" to "zy_ui8_music",
        "com.awell.radio" to "zy_ui8_radio",
        "com.awell.bluetooth" to "zy_ui8_bt",
         "com.awell.navigation" to "zy_ui8_navi",
        "com.android.browser" to "zy_ui8_liulanqi",
        "com.mediatek.filemanager" to "zy_ui8_wjgl",
        "com.android.documentsui" to "zy_ui8_wjgl",
        "com.awell.carsetting" to "zy_ui8_yuanche",
        "com.awell.backcar" to "zy_ui8_wjsr",
        "cn.kuwo.kwmusiccar" to "zy_ui8_kuwo",
        "com.awell.eqselect" to "zy_ui8_dsp",
        "com.awell.canbus" to "zy_ui8_shezhi",
        "com.awell.canbus2" to "zy_ui8_shezhi",
        "com.autonavi.amapauto" to "zy_ui8_navi",
        "com.google.android.apps.maps" to "zy_ui8_navi",
        "net.easyconn" to "zy_ui8_zlink",
        "com.zjinnova.zlink" to "zy_ui8_zlink",
        "com.awell.awellmanual" to "zy_ui8_manual",
        "com.google.android.youtube" to "zy_ui8_youtube",
        "com.android.vending" to "zy_ui8_playstore",
        "com.android.chrome" to "zy_ui8_liulanqi",
        "com.google.android.googlequicksearchbox" to "zy_ui8_gg",
        "com.android.gallery3d" to "zy_ui8_gallery",
        "org.chromium.chrome" to "zy_ui8_liulanqi",
        "com.awell.electricfan" to "zy_ui8_fan",
        "com.awell.frontvideo" to "zy_ui8_front_video",
        "com.awell.keylight" to "zy_ui8_color_light",
        "com.awell.keystudy" to "zy_ui8_training",
        "com.awell.themesetting" to "zy_ui8_theme",
        "com.awell.weather" to "zy_ui8_weather",
        "com.awell.eqselect" to "zy_ui8_jhq",
        "com.ms.ms2160" to "zy_ui8_wjsr",
        "cn.kuwo.kwmusiccar" to "zy_ui8_kuwo",
        ".AwellCanbusAirView" to "zy_ui8_air"
    )

    /**
     * 获取图标映射表（供外部访问）
     * @return 图标映射表
     */
    fun getPackageIconMap(): Map<String, String> {
        return PACKAGE_ICON_MAP
    }


}
