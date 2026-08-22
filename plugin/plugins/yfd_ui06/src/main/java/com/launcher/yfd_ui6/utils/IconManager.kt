package com.launcher.yfd_ui6.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import com.launcher.yfd_ui6.R

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
       // "com.awell.carsetting" to "apple_yuanche_square",
        "com.awell.backcar" to "apple_wjsr_square",
        //"cn.kuwo.kwmusiccar" to "apple_kuwo_square",
        "com.awell.eqselect" to "apple_dsp_square",
        "com.awell.canbus" to "apple_shezhi_square",
        "com.awell.canbus2" to "apple_shezhi_square",
        "com.autonavi.amapauto" to "apple_gaode_square",
        "net.easyconn" to "apple_zlink_square",
        "com.zjinnova.zlink" to "apple_zlink_square",
        "com.awell.awellmanual" to "apple_manual_square",
       // "com.google.android.youtube" to "apple_youtube_square",
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
       // "com.ms.ms2160" to "apple_wjsr_square"
    )

    val PACKAGE_ICON_MAP: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.host_video_small,
        "com.awell.localmusic" to R.drawable.host_music_small,
        "com.awell.radio" to R.drawable.host_radio_small,
        "com.awell.bluetooth" to R.drawable.host_phone_small,
        "com.awell.navigation" to R.drawable.host_navigation_small,
        "com.android.browser" to R.drawable.host_brower_small,
        "com.mediatek.filemanager" to R.drawable.host_wjgl_small,
       // "com.android.documentsui" to R.drawable.host_wjgl_small,
         "com.awell.carsetting" to R.drawable.host_carinfo_small,
        "com.awell.backcar" to R.drawable.host_wjsr_small,
        //"cn.kuwo.kwmusiccar" to "apple_kuwo_square",
        "com.awell.eqselect" to R.drawable.host_dsp_small,
        "com.awell.canbus" to R.drawable.host_shezhi_small,
       /// "com.awell.canbus2" to R.drawable.host_shezhi_small,
        //"com.autonavi.amapauto" to R.drawable.host_gaode_small,
        //"net.easyconn" to R.drawable.host_zlink_small,
        "com.zjinnova.zlink" to R.drawable.host_zlink_small,
        "com.awell.awellmanual" to R.drawable.host_manual_small,
         "com.google.android.youtube" to R.drawable.host_youtube_small,
        "com.android.vending" to R.drawable.host_playstore_small,
        "com.android.chrome" to R.drawable.host_brower_small,
       // "com.google.android.googlequicksearchbox" to R.drawable.host_gg_small,
        "com.android.gallery3d" to R.drawable.host_gallery_small,
        "org.chromium.chrome" to R.drawable.host_brower_small,
        "com.awell.electricfan" to R.drawable.host_fan_small,
        //"com.awell.frontvideo" to R.drawable.host_front_video_small,
        //"com.awell.keylight" to R.drawable.host_color_light_small,
        "com.awell.keystudy" to R.drawable.host_training_small,
        "com.awell.themesetting" to R.drawable.host_theme_small,
        "com.ms.ms2160" to R.drawable.host_wjsc_small,
        "com.awell.weather" to R.drawable.host_weather_small,
        "com.android.gallery3d" to R.drawable.host_gallery_small,
        "cn.cardoor.zt360" to R.drawable.host_zt360_small
    )

    /**
     * 获取图标映射表（供外部访问）
     * @return 图标映射表
     */
    fun getPackageIconMap(): Map<String, String> {
        return PACKAGE_ICON_MAP_SQUARE
    }

    /**
     * 获取应用自定义图标
     * @param context 上下文
     * @param packageName 包名
     * @return 自定义图标，包名不在映射表中时返回 null（由调用方回退到应用自带图标）
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    @JvmStatic
    fun getIcon(context: Context, packageName: String): Drawable? {
        val resId = PACKAGE_ICON_MAP[packageName] ?: return null
        return context.getDrawable(resId)
    }


}
