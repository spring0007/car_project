package com.launcher.yks_ui01

/**
 * 图标管理器
 * 支持根据主题模式加载不同的应用图标背景
 */
object IconManager {

    private const val TAG = "IconManager"

    // 圆形主题图标映射表（包名 -> 资源名称）
    val PACKAGE_ICON_MAP_SQUARE: Map<String, String> = mapOf(
        "com.awell.localvideo" to "yks_video_square",
        "com.awell.localmusic" to "yks_music_square",
        "com.awell.radio" to "yks_radio_square",
        "com.awell.bluetooth" to "yks_bt_square",
         "com.awell.navigation" to "yks_navi_square",
        //"com.android.browser" to "yks_liulanqi_square",
        "com.mediatek.filemanager" to "yks_wjgl_square",
        "com.android.documentsui" to "yks_wjgl_square",
        //"com.awell.carsetting" to "yks_yuanche_square",
        //"com.awell.backcar" to "yks_wjsr_square",
        //"cn.kuwo.kwmusiccar" to "yks_kuwo_square",
        //"com.awell.eqselect" to "yks_dsp_square",
        //"com.awell.canbus" to "yks_shezhi_square",
        //"com.awell.canbus2" to "yks_shezhi_square",
        "com.autonavi.amapauto" to "yks_navi_square",
        "net.easyconn" to "yks_zlink_square",
        "com.zjinnova.zlink" to "yks_zlink_square",
        "com.awell.awellmanual" to "yks_manual_square",
        //"com.google.android.youtube" to "yks_youtube_square",
        //"com.android.vending" to "yks_playstore_square",
        //"com.android.chrome" to "yks_liulanqi_square",
        //"com.google.android.googlequicksearchbox" to "yks_gg_square",
        //"com.android.gallery3d" to "yks_gallery_square",
        //"org.chromium.chrome" to "yks_liulanqi_square",
        //"com.awell.electricfan" to "yks_fan_square",
        //"com.awell.frontvideo" to "yks_front_video_square",
        //"com.awell.keylight" to "yks_color_light_square",
        //"com.awell.keystudy" to "yks_training_square",
        //"com.awell.themesetting" to "yks_theme_square",
        //"com.ms.ms2160" to "yks_wjsr_square"
    )

    /**
     * 获取图标映射表（供外部访问）
     * @return 图标映射表
     */
    fun getPackageIconMap(): Map<String, String> {
        return PACKAGE_ICON_MAP_SQUARE
    }


}
