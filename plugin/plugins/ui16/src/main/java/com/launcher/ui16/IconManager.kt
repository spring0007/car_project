package com.launcher.ui16

/**
 * 图标管理器
 * 支持根据主题模式加载不同的应用图标背景
 */
object IconManager {

    private const val TAG = "IconManager"

    // 圆形主题图标映射表（包名 -> 资源名称）
    val PACKAGE_ICON_MAP_BLACK: Map<String, String> = mapOf(
        //"com.awell.localvideo" to "sf_video_black",
        //"com.awell.localmusic" to "sf_music_black",
        //"com.awell.radio" to "sf_radio_black",
        //"com.awell.bluetooth" to "sf_bt_black",
       // "com.awell.navigation" to "sf_navi_black",
        "com.android.browser" to "sf_liulanqi_black",
        "com.mediatek.filemanager" to "sf_wjgl_black",
        "com.android.documentsui" to "sf_wjgl_black",
        "com.awell.carsetting" to "sf_yuanche_black",
        "com.awell.backcar" to "sf_wjsr_black",
        "cn.kuwo.kwmusiccar" to "sf_kuwo_black",
        "com.awell.eqselect" to "sf_dsp_black",
        "com.awell.canbus" to "sf_shezhi_black",
        "com.awell.canbus2" to "sf_shezhi_black",
        "com.autonavi.amapauto" to "sf_gaode_black",
        "net.easyconn" to "sf_zlink_black",
        "com.zjinnova.zlink" to "sf_zlink_black",
        "com.awell.awellmanual" to "sf_manual_black",
        "com.google.android.youtube" to "sf_youtube_black",
        "com.android.vending" to "sf_playstore_black",
        "com.android.chrome" to "sf_liulanqi_black",
        "com.google.android.googlequicksearchbox" to "sf_gg_black",
        "com.android.gallery3d" to "sf_gallery_black",
        "org.chromium.chrome" to "sf_liulanqi_black",
        "com.awell.electricfan" to "sf_fan_black",
        "com.awell.frontvideo" to "sf_front_video_black",
        "com.awell.keylight" to "sf_color_light_black",
        "com.awell.keystudy" to "sf_training_black",
        "com.awell.themesetting" to "sf_theme_black",
        "com.ms.ms2160" to "sf_wjsr_black"
    )

    /**
     * 获取图标映射表（供外部访问）
     * @return 图标映射表
     */
    fun getPackageIconMap(): Map<String, String> {
        return PACKAGE_ICON_MAP_BLACK
    }
    // 主题模式对应的背景资源
    // 注意：如果资源不存在，会返回 null 并使用原始图标
    fun getDefaultIconBackground():  String{

        return "sf_other_app3_black"
    }


}
