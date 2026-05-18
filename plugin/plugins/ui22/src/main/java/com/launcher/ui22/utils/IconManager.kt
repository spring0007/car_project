package com.launcher.ui22.utils

/**
 * 图标管理器
 * 支持根据主题模式加载不同的应用图标背景
 */
object IconManager {

    private const val TAG = "IconManager"

    // 圆形主题图标映射表（包名 -> 资源名称）
    val PACKAGE_ICON_MAP_SQUARE: Map<String, String> = mapOf(
        "com.awell.localvideo" to "apple_video_square",
        "com.awell.localmusic" to "apple_music_square",
        "com.awell.radio" to "apple_radio_square",
        "com.awell.bluetooth" to "apple_bt_square",
         "com.awell.navigation" to "apple_navi_square",
        "com.android.browser" to "apple_liulanqi_square",
        "com.mediatek.filemanager" to "apple_wjgl_square",
        "com.android.documentsui" to "apple_wjgl_square",
        "com.awell.carsetting" to "apple_yuanche_square",
        "com.awell.backcar" to "apple_wjsr_square",
        "cn.kuwo.kwmusiccar" to "apple_kuwo_square",
        "com.awell.eqselect" to "apple_dsp_square",
        "com.awell.canbus" to "apple_shezhi_square",
        "com.awell.canbus2" to "apple_shezhi_square",
        "com.autonavi.amapauto" to "apple_gaode_square",
        "net.easyconn" to "apple_zlink_square",
        "com.zjinnova.zlink" to "apple_zlink_square",
        "com.awell.awellmanual" to "apple_manual_square",
        "com.google.android.youtube" to "apple_youtube_square",
        "com.android.vending" to "apple_playstore_square",
        "com.android.chrome" to "apple_liulanqi_square",
        "com.google.android.googlequicksearchbox" to "apple_gg_square",
        "com.android.gallery3d" to "apple_gallery_square",
        "org.chromium.chrome" to "apple_liulanqi_square",
        "com.awell.electricfan" to "apple_fan_square",
        "com.awell.frontvideo" to "apple_front_video_square",
        "com.awell.keylight" to "apple_color_light_square",
        "com.awell.keystudy" to "apple_training_square",
        "com.awell.themesetting" to "apple_theme_square",
        "com.ms.ms2160" to "apple_wjsr_square"
    )

    /**
     * 获取图标映射表（供外部访问）
     * @return 图标映射表
     */
    fun getPackageIconMap(): Map<String, String> {
        return PACKAGE_ICON_MAP_SQUARE
    }


}
