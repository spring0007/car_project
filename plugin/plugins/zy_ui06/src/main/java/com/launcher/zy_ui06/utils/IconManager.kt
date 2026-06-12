package com.launcher.zy_ui06.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.launcher.zy_ui06.R
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 图标管理器
 * 支持根据主题模式加载不同的应用图标背景
 */
object IconManager {

    private const val TAG = "IconManager"

//    // 主题模式对应的背景资源
//    // 注意：如果资源不存在，会返回 null 并使用原始图标
//    private val themeBackgrounds = mapOf(
//        0 to R.drawable.sf_other_app3_circle,      // 主题 0: 使用启动器背景 (深色)
//        1 to R.drawable.sf_other_app3_circle,      // 主题 1: 默认
//        2 to R.drawable.sf_other_app3_circle,      // 主题 2: 使用启动器背景
//        3 to R.drawable.circle_shape               // 主题 3: 圆形背景
//    )

    // 圆形主题图标映射表
    val PACKAGE_ICON_MAP_CIRCLE_ONE: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.zy060_video,
        "com.awell.localmusic" to R.drawable.zy060_music,
        "com.awell.radio" to R.drawable.zy060_radio,
        "com.awell.bluetooth" to R.drawable.zy060_btphone,
        "com.awell.navigation" to R.drawable.zy060_navi,
        "com.android.browser" to R.drawable.zy060_liulanqi,
        "com.mediatek.filemanager" to R.drawable.zy060_wjgl,
        "com.android.documentsui" to R.drawable.zy060_wjgl,
        "com.awell.carsetting" to R.drawable.zy060_shezhi,
        "com.awell.backcar" to R.drawable.zy060_wjsr,
        "cn.kuwo.kwmusiccar" to R.drawable.zy060_kuwo,
        "com.awell.eqselect" to R.drawable.zy060_jhq,
        "com.awell.canbus" to R.drawable.zy060_yuanche,
        //"com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.zy060_navi,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.zy060_net_easyconn,
        "com.zjinnova.zlink" to R.drawable.zy060_link,
        "com.awell.awellmanual" to R.drawable.zy060_manual,
        "com.google.android.apps.maps" to R.drawable.zy060_googlemap,
        "com.google.android.youtube" to R.drawable.zy060_youtube,
        "com.android.vending" to R.drawable.zy060_playstore,
        "com.android.chrome" to R.drawable.zy060_liulanqi,
        //"com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.zy060_gallery,
        "org.chromium.chrome" to R.drawable.zy060_liulanqi,
        "com.awell.electricfan" to R.drawable.zy060_fan,
        "com.awell.frontvideo" to R.drawable.zy060_front_video,
       // "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.zy060_training,
        "com.awell.themesetting" to R.drawable.zy060_theme,
        "com.ms.ms2160" to R.drawable.zy060_usb_video_output,
        "com.awell.weather" to R.drawable.zy060_weather
    )

    val PACKAGE_ICON_MAP_CIRCLE_TWO: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.zy061_video,
        "com.awell.localmusic" to R.drawable.zy061_music,
        "com.awell.radio" to R.drawable.zy061_radio,
        "com.awell.bluetooth" to R.drawable.zy061_btphone,
        "com.awell.navigation" to R.drawable.zy061_navi,
        "com.android.browser" to R.drawable.zy061_liulanqi,
        "com.mediatek.filemanager" to R.drawable.zy061_wjgl,
        "com.android.documentsui" to R.drawable.zy061_wjgl,
        "com.awell.carsetting" to R.drawable.zy061_shezhi,
        "com.awell.backcar" to R.drawable.zy061_wjsr,
        "cn.kuwo.kwmusiccar" to R.drawable.zy061_kuwo,
        "com.awell.eqselect" to R.drawable.zy061_jhq,
        "com.awell.canbus" to R.drawable.zy061_yuanche,
        //"com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.zy061_navi,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.zy061_net_easyconn,
        "com.zjinnova.zlink" to R.drawable.zy061_link,
        "com.awell.awellmanual" to R.drawable.zy061_manual,
        "com.google.android.apps.maps" to R.drawable.zy061_googlemap,
        "com.google.android.youtube" to R.drawable.zy061_youtube,
        "com.android.vending" to R.drawable.zy061_playstore,
        "com.android.chrome" to R.drawable.zy061_liulanqi,
        //"com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.zy061_gallery,
        "org.chromium.chrome" to R.drawable.zy061_liulanqi,
        "com.awell.electricfan" to R.drawable.zy061_fan,
        "com.awell.frontvideo" to R.drawable.zy061_front_video,
        // "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.zy061_training,
        "com.awell.themesetting" to R.drawable.zy061_theme,
        "com.ms.ms2160" to R.drawable.zy061_usb_video_output,
        "com.awell.weather" to R.drawable.zy061_weather
    )

    val PACKAGE_ICON_MAP_CIRCLE_THREE: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.zy062_video,
        "com.awell.localmusic" to R.drawable.zy062_music,
        "com.awell.radio" to R.drawable.zy062_radio,
        "com.awell.bluetooth" to R.drawable.zy062_btphone,
        "com.awell.navigation" to R.drawable.zy062_navi,
        "com.android.browser" to R.drawable.zy062_liulanqi,
        "com.mediatek.filemanager" to R.drawable.zy062_wjgl,
        "com.android.documentsui" to R.drawable.zy062_wjgl,
        "com.awell.carsetting" to R.drawable.zy062_shezhi,
        "com.awell.backcar" to R.drawable.zy062_wjsr,
        "cn.kuwo.kwmusiccar" to R.drawable.zy062_kuwo,
        "com.awell.eqselect" to R.drawable.zy062_jhq,
        "com.awell.canbus" to R.drawable.zy062_yuanche,
        //"com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.zy062_navi,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.zy062_net_easyconn,
        "com.zjinnova.zlink" to R.drawable.zy062_link,
        "com.awell.awellmanual" to R.drawable.zy062_manual,
        "com.google.android.apps.maps" to R.drawable.zy062_googlemap,
        "com.google.android.youtube" to R.drawable.zy062_youtube,
        "com.android.vending" to R.drawable.zy062_playstore,
        "com.android.chrome" to R.drawable.zy062_liulanqi,
        //"com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.zy062_gallery,
        "org.chromium.chrome" to R.drawable.zy062_liulanqi,
        "com.awell.electricfan" to R.drawable.zy062_fan,
        "com.awell.frontvideo" to R.drawable.zy062_front_video,
        // "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.zy062_training,
        "com.awell.themesetting" to R.drawable.zy062_theme,
        "com.ms.ms2160" to R.drawable.zy062_usb_video_output,
        "com.awell.weather" to R.drawable.zy062_weather
    )
    val PACKAGE_ICON_MAP_CIRCLE_FOUR: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.zy063_video,
        "com.awell.localmusic" to R.drawable.zy063_music,
        "com.awell.radio" to R.drawable.zy063_radio,
        "com.awell.bluetooth" to R.drawable.zy063_btphone,
        "com.awell.navigation" to R.drawable.zy063_navi,
        "com.android.browser" to R.drawable.zy063_liulanqi,
        "com.mediatek.filemanager" to R.drawable.zy063_wjgl,
        "com.android.documentsui" to R.drawable.zy063_wjgl,
        "com.awell.carsetting" to R.drawable.zy063_shezhi,
        "com.awell.backcar" to R.drawable.zy063_wjsr,
        "cn.kuwo.kwmusiccar" to R.drawable.zy063_kuwo,
        "com.awell.eqselect" to R.drawable.zy063_jhq,
        "com.awell.canbus" to R.drawable.zy063_yuanche,
        //"com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.zy063_navi,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.zy063_net_easyconn,
        "com.zjinnova.zlink" to R.drawable.zy063_link,
        "com.awell.awellmanual" to R.drawable.zy063_manual,
        "com.google.android.apps.maps" to R.drawable.zy063_googlemap,
        "com.google.android.youtube" to R.drawable.zy063_youtube,
        "com.android.vending" to R.drawable.zy063_playstore,
        "com.android.chrome" to R.drawable.zy063_liulanqi,
        //"com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.zy063_gallery,
        "org.chromium.chrome" to R.drawable.zy063_liulanqi,
        "com.awell.electricfan" to R.drawable.zy063_fan,
        "com.awell.frontvideo" to R.drawable.zy063_front_video,
        // "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.zy063_training,
        "com.awell.themesetting" to R.drawable.zy063_theme,
        "com.ms.ms2160" to R.drawable.zy063_usb_video_output,
        "com.awell.weather" to R.drawable.zy063_weather
    )

    val PACKAGE_ICON_MAP_CIRCLE_FIVE: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.zy064_video,
        "com.awell.localmusic" to R.drawable.zy064_music,
        "com.awell.radio" to R.drawable.zy064_radio,
        "com.awell.bluetooth" to R.drawable.zy064_btphone,
        "com.awell.navigation" to R.drawable.zy064_navi,
        "com.android.browser" to R.drawable.zy064_liulanqi,
        "com.mediatek.filemanager" to R.drawable.zy064_wjgl,
        "com.android.documentsui" to R.drawable.zy064_wjgl,
        "com.awell.carsetting" to R.drawable.zy064_shezhi,
        "com.awell.backcar" to R.drawable.zy064_wjsr,
        "cn.kuwo.kwmusiccar" to R.drawable.zy064_kuwo,
        "com.awell.eqselect" to R.drawable.zy064_jhq,
        "com.awell.canbus" to R.drawable.zy064_yuanche,
        //"com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.zy064_navi,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.zy064_net_easyconn,
        "com.zjinnova.zlink" to R.drawable.zy064_link,
        "com.awell.awellmanual" to R.drawable.zy064_manual,
        "com.google.android.apps.maps" to R.drawable.zy064_googlemap,
        "com.google.android.youtube" to R.drawable.zy064_youtube,
        "com.android.vending" to R.drawable.zy064_playstore,
        "com.android.chrome" to R.drawable.zy064_liulanqi,
        //"com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.zy064_gallery,
        "org.chromium.chrome" to R.drawable.zy064_liulanqi,
        "com.awell.electricfan" to R.drawable.zy064_fan,
        "com.awell.frontvideo" to R.drawable.zy064_front_video,
        // "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.zy064_training,
        "com.awell.themesetting" to R.drawable.zy064_theme,
        "com.ms.ms2160" to R.drawable.zy064_usb_video_output,
        "com.awell.weather" to R.drawable.zy064_weather
    )
    val PACKAGE_ICON_MAP_CIRCLE_SIX: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.zy065_video,
        "com.awell.localmusic" to R.drawable.zy065_music,
        "com.awell.radio" to R.drawable.zy065_radio,
        "com.awell.bluetooth" to R.drawable.zy065_btphone,
        "com.awell.navigation" to R.drawable.zy065_navi,
        "com.android.browser" to R.drawable.zy065_liulanqi,
        "com.mediatek.filemanager" to R.drawable.zy065_wjgl,
        "com.android.documentsui" to R.drawable.zy065_wjgl,
        "com.awell.carsetting" to R.drawable.zy065_shezhi,
        "com.awell.backcar" to R.drawable.zy065_wjsr,
        "cn.kuwo.kwmusiccar" to R.drawable.zy065_kuwo,
        "com.awell.eqselect" to R.drawable.zy065_jhq,
        "com.awell.canbus" to R.drawable.zy065_yuanche,
        //"com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.zy065_navi,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.zy065_net_easyconn,
        "com.zjinnova.zlink" to R.drawable.zy065_link,
        "com.awell.awellmanual" to R.drawable.zy065_manual,
        "com.google.android.apps.maps" to R.drawable.zy065_googlemap,
        "com.google.android.youtube" to R.drawable.zy065_youtube,
        "com.android.vending" to R.drawable.zy065_playstore,
        "com.android.chrome" to R.drawable.zy065_liulanqi,
        //"com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.zy065_gallery,
        "org.chromium.chrome" to R.drawable.zy065_liulanqi,
        "com.awell.electricfan" to R.drawable.zy065_fan,
        "com.awell.frontvideo" to R.drawable.zy065_front_video,
        // "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.zy065_training,
        "com.awell.themesetting" to R.drawable.zy065_theme,
        "com.ms.ms2160" to R.drawable.zy065_usb_video_output,
        "com.awell.weather" to R.drawable.zy065_weather
    )

    val PACKAGE_ICON_MAP_CIRCLE_SEVEN: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.zy066_video,
        "com.awell.localmusic" to R.drawable.zy066_music,
        "com.awell.radio" to R.drawable.zy066_radio,
        "com.awell.bluetooth" to R.drawable.zy066_btphone,
        "com.awell.navigation" to R.drawable.zy066_navi,
        "com.android.browser" to R.drawable.zy066_liulanqi,
        "com.mediatek.filemanager" to R.drawable.zy066_wjgl,
        "com.android.documentsui" to R.drawable.zy066_wjgl,
        "com.awell.carsetting" to R.drawable.zy066_shezhi,
        "com.awell.backcar" to R.drawable.zy066_wjsr,
        "cn.kuwo.kwmusiccar" to R.drawable.zy066_kuwo,
        "com.awell.eqselect" to R.drawable.zy066_jhq,
        "com.awell.canbus" to R.drawable.zy066_yuanche,
        //"com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.zy066_navi,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.zy066_net_easyconn,
        "com.zjinnova.zlink" to R.drawable.zy066_link,
        "com.awell.awellmanual" to R.drawable.zy066_manual,
        "com.google.android.apps.maps" to R.drawable.zy066_googlemap,
        "com.google.android.youtube" to R.drawable.zy066_youtube,
        "com.android.vending" to R.drawable.zy066_playstore,
        "com.android.chrome" to R.drawable.zy066_liulanqi,
        //"com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.zy066_gallery,
        "org.chromium.chrome" to R.drawable.zy066_liulanqi,
        "com.awell.electricfan" to R.drawable.zy066_fan,
        "com.awell.frontvideo" to R.drawable.zy066_front_video,
        // "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.zy066_training,
        "com.awell.themesetting" to R.drawable.zy066_theme,
        "com.ms.ms2160" to R.drawable.zy066_usb_video_output,
        "com.awell.weather" to R.drawable.zy066_weather
    )


    // 当前主题模式，默认 0
    var currentThemeMode: Int = 0
        set(value) {
            field = value
            // 主题切换时清空缓存
            evictAll()
        }

    // 图标缓存 (LRU Cache, 最大容量 100)
    private val iconCache = LruCache<String, Drawable>(100)

    // 背景资源缓存 (预先加载)
    private val backgroundDrawableCache = ConcurrentHashMap<Int, Drawable>()

    // PackageManager 实例缓存
    private var packageManager: PackageManager? = null

    /**
     * 初始化管理器 (可选调用)
     */
    fun initialize(context: Context) {
        packageManager = context.packageManager
    }

    /**
     * 清空所有缓存
     */
    fun evictAll() {
        iconCache.evictAll()
    }

    /**
     * 获取应用图标，优先使用自定义图标
     * @param context 上下文
     * @param packageName 包名
     * @return Drawable 带主题背景的图标
     */
    fun getAppIcon(context: Context, packageName: String): Drawable {
        if (packageName.isEmpty()) {
            throw IllegalArgumentException("Package name cannot be empty")
        }

        // 1. 检查缓存
        val cacheKey = "${packageName}_$currentThemeMode"
        iconCache.get(cacheKey)?.let {
            return it
        }

        // 2. 检查是否是自定义图标 (快速路径)
        val customIconResId = getCustomIconForPackage(packageName, currentThemeMode)
        if (customIconResId != null) {
            try {
                val customIcon = ContextCompat.getDrawable(context, customIconResId)
                    ?: getDefaultIcon(context)
                iconCache.put(cacheKey, customIcon)
                return customIcon
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. 获取系统图标
        val pm = packageManager ?: context.packageManager
        val drawable = try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            val originalIcon = packageInfo.applicationInfo.loadIcon(pm)
            applyThemeBackground(context, originalIcon, packageName, currentThemeMode)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            getDefaultIcon(context)
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultIcon(context)
        }

        // 4. 缓存结果
        iconCache.put(cacheKey, drawable)
        return drawable
    }

    private fun getDefaultIcon(context: Context): Drawable {
        return try {
            ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
                ?: android.graphics.drawable.ColorDrawable()
        } catch (e: Exception) {
            android.graphics.drawable.ColorDrawable()
        }
    }

    /**
     * 为主题图标应用背景 (优化版，使用缓存)
     * @param context 上下文
     * @param originalIcon 原始图标
     * @param packageName 包名
     * @param themeMode 主题模式
     * @return 带背景的图标
     */
    private fun applyThemeBackground(context: Context, originalIcon: Drawable, packageName: String, themeMode: Int): Drawable {
        // 优先检查是否有自定义图标（根据包名）
        val customIconResId = getCustomIconForPackage(packageName, themeMode)

        return if (customIconResId != null) {
            // 如果有自定义图标，直接使用自定义图标
            try {
                ContextCompat.getDrawable(context, customIconResId) ?: originalIcon
            } catch (e: Exception) {
                e.printStackTrace()
                originalIcon
            }
        } else {
            // 如果没有自定义图标，直接返回原始图标
            originalIcon
        }
    }

    /**
     * 缩放图标到指定尺寸
     * @param context 上下文
     * @param original 原始图标
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     * @return 缩放后的图标
     */
    private fun scaleIconToSize(context: Context, original: Drawable, targetWidth: Int, targetHeight: Int): Drawable {
        val width = original.intrinsicWidth
        val height = original.intrinsicHeight

        // 如果原始尺寸为 0 或目标尺寸为 0，返回原图标
        if (width <= 0 || height <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return original
        }

        // 计算缩放比例，保持宽高比
        val scale = minOf(
            targetWidth.toFloat() / width,
            targetHeight.toFloat() / height
        )

        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        // 确保最小尺寸
        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return original
        }

        // 创建缩放后的 Bitmap
        val bitmap = createBitmap(scaledWidth, scaledHeight)
        val canvas = android.graphics.Canvas(bitmap)
        original.setBounds(0, 0, scaledWidth, scaledHeight)
        original.draw(canvas)

        return bitmap.toDrawable(context.resources)
    }

    /**
     * 根据包名和主题模式获取自定义图标资源 ID
     * @param packageName 包名
     * @param themeMode 主题模式
     * @return 自定义图标资源 ID，如果没有则返回 null
     */
    private fun getCustomIconForPackage(packageName: String, themeMode: Int): Int? {
        return when (themeMode) {
            0 -> PACKAGE_ICON_MAP_CIRCLE_ONE[packageName]
            1 -> PACKAGE_ICON_MAP_CIRCLE_TWO[packageName]
            2 -> PACKAGE_ICON_MAP_CIRCLE_THREE[packageName]
            3 -> PACKAGE_ICON_MAP_CIRCLE_FOUR[packageName]
            4 -> PACKAGE_ICON_MAP_CIRCLE_FIVE[packageName]
            5 -> PACKAGE_ICON_MAP_CIRCLE_SIX[packageName]
            6 -> PACKAGE_ICON_MAP_CIRCLE_SEVEN[packageName]
            else -> PACKAGE_ICON_MAP_CIRCLE_ONE[packageName]  // 其他主题暂不使用自定义图标
        }
    }

    ///=============================
    val PACKAGE_ICON_MAP_ONE: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy060_video1",
        "com.awell.localmusic" to "zy060_music1",
        "com.awell.radio" to "zy060_radio1",
        "com.awell.bluetooth" to "zy060_btphone",
        "com.awell.navigation" to "zy060_navi1",
        "com.android.browser" to "zy060_liulanqi",
        "com.mediatek.filemanager" to "zy060_wjgl",
        "com.android.documentsui" to "zy060_wjgl",
        "com.awell.carsetting" to "zy060_shezhi",
        "com.awell.backcar" to "zy060_wjsr",
        "cn.kuwo.kwmusiccar" to "zy060_kuwo",
        "com.awell.eqselect" to "zy060_jhq",
        "com.awell.canbus" to "zy060_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy060_navi1",
        // "com.tima.carnet.vt" to "sf_zlink_circle",
        "net.easyconn" to "zy060_net_easyconn",
        "com.zjinnova.zlink" to "zy060_link",
        "com.awell.awellmanual" to "zy060_manual",
         "com.google.android.apps.maps" to "zy060_googlemap",
        "com.google.android.youtube" to "zy060_youtube",
        "com.android.vending" to "zy060_playstore",
        "com.android.chrome" to "zy060_liulanqi",
        //"com.google.android.googlequicksearchbox" to "sf_gg_circle",
        "com.android.gallery3d" to "zy060_gallery",
        "org.chromium.chrome" to "zy060_liulanqi",
        "com.awell.electricfan" to "zy060_fan",
         "com.awell.frontvideo" to "zy060_front_video",
        // "com.awell.keylight" to "sf_color_light_circle",
        "com.awell.keystudy" to "zy060_training",
        "com.awell.themesetting" to "zy060_theme",
         "com.ms.ms2160" to "zy060_usb_video_output",
         "com.awell.weather" to "zy060_weather",
        ".AwellCanbusAirView" to "zy060_air"
    )

    val PACKAGE_ICON_MAP_TWO: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy061_video1",
        "com.awell.localmusic" to "zy061_music",
        "com.awell.radio" to "zy061_radio",
        "com.awell.bluetooth" to "zy061_btphone",
        "com.awell.navigation" to "zy061_navi1",
        "com.android.browser" to "zy061_liulanqi",
        "com.mediatek.filemanager" to "zy061_wjgl",
        "com.android.documentsui" to "zy061_wjgl",
        "com.awell.carsetting" to "zy061_shezhi",
        "com.awell.backcar" to "zy061_wjsr",
        "cn.kuwo.kwmusiccar" to "zy061_kuwo",
        "com.awell.eqselect" to "zy061_jhq",
        "com.awell.canbus" to "zy061_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy061_navi1",
        // "com.tima.carnet.vt" to "sf_zlink_circle",
        "net.easyconn" to "zy061_net_easyconn",
        "com.zjinnova.zlink" to "zy061_link",
        "com.awell.awellmanual" to "zy061_manual",
        "com.google.android.apps.maps" to "zy061_googlemap",
        "com.google.android.youtube" to "zy061_youtube",
        "com.android.vending" to "zy061_playstore",
        "com.android.chrome" to "zy061_liulanqi",
        //"com.google.android.googlequicksearchbox" to "sf_gg_circle",
        "com.android.gallery3d" to "zy061_gallery",
        "org.chromium.chrome" to "zy061_liulanqi",
        "com.awell.electricfan" to "zy061_fan",
        "com.awell.frontvideo" to "zy061_front_video",
        // "com.awell.keylight" to "sf_color_light_circle",
        "com.awell.keystudy" to "zy061_training",
        "com.awell.themesetting" to "zy061_theme",
        "com.ms.ms2160" to "zy061_usb_video_output",
        "com.awell.weather" to "zy061_weather",
        ".AwellCanbusAirView" to "zy061_air"
    )

    val PACKAGE_ICON_MAP_THREE: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy062_video1",
        "com.awell.localmusic" to "zy062_music",
        "com.awell.radio" to "zy062_radio",
        "com.awell.bluetooth" to "zy062_btphone",
        "com.awell.navigation" to "zy062_navi1",
        "com.android.browser" to "zy062_liulanqi",
        "com.mediatek.filemanager" to "zy062_wjgl",
        "com.android.documentsui" to "zy062_wjgl",
        "com.awell.carsetting" to "zy062_shezhi",
        "com.awell.backcar" to "zy062_wjsr",
        "cn.kuwo.kwmusiccar" to "zy062_kuwo",
        "com.awell.eqselect" to "zy062_jhq",
        "com.awell.canbus" to "zy062_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy062_navi1",
        // "com.tima.carnet.vt" to "sf_zlink_circle",
        "net.easyconn" to "zy062_net_easyconn",
        "com.zjinnova.zlink" to "zy062_link",
        "com.awell.awellmanual" to "zy062_manual",
        "com.google.android.apps.maps" to "zy062_googlemap",
        "com.google.android.youtube" to "zy062_youtube",
        "com.android.vending" to "zy062_playstore",
        "com.android.chrome" to "zy062_liulanqi",
        //"com.google.android.googlequicksearchbox" to "sf_gg_circle",
        "com.android.gallery3d" to "zy062_gallery",
        "org.chromium.chrome" to "zy062_liulanqi",
        "com.awell.electricfan" to "zy062_fan",
        "com.awell.frontvideo" to "zy062_front_video",
        // "com.awell.keylight" to "sf_color_light_circle",
        "com.awell.keystudy" to "zy062_training",
        "com.awell.themesetting" to "zy062_theme",
        "com.ms.ms2160" to "zy062_usb_video_output",
        "com.awell.weather" to "zy062_weather",
        ".AwellCanbusAirView" to "zy062_air"
    )
    val PACKAGE_ICON_MAP_FOUR: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy063_video1",
        "com.awell.localmusic" to "zy063_music",
        "com.awell.radio" to "zy063_radio",
        "com.awell.bluetooth" to "zy063_btphone",
        "com.awell.navigation" to "zy063_navi1",
        "com.android.browser" to "zy063_liulanqi",
        "com.mediatek.filemanager" to "zy063_wjgl",
        "com.android.documentsui" to "zy063_wjgl",
        "com.awell.carsetting" to "zy063_shezhi",
        "com.awell.backcar" to "zy063_wjsr",
        "cn.kuwo.kwmusiccar" to "zy063_kuwo",
        "com.awell.eqselect" to "zy063_jhq",
        "com.awell.canbus" to "zy063_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy063_navi1",
        // "com.tima.carnet.vt" to "sf_zlink_circle",
        "net.easyconn" to "zy063_net_easyconn",
        "com.zjinnova.zlink" to "zy063_link",
        "com.awell.awellmanual" to "zy063_manual",
        "com.google.android.apps.maps" to "zy063_googlemap",
        "com.google.android.youtube" to "zy063_youtube",
        "com.android.vending" to "zy063_playstore",
        "com.android.chrome" to "zy063_liulanqi",
        //"com.google.android.googlequicksearchbox" to "sf_gg_circle",
        "com.android.gallery3d" to "zy063_gallery",
        "org.chromium.chrome" to "zy063_liulanqi",
        "com.awell.electricfan" to "zy063_fan",
        "com.awell.frontvideo" to "zy063_front_video",
        // "com.awell.keylight" to "sf_color_light_circle",
        "com.awell.keystudy" to "zy063_training",
        "com.awell.themesetting" to "zy063_theme",
        "com.ms.ms2160" to "zy063_usb_video_output",
        "com.awell.weather" to "zy063_weather",
        ".AwellCanbusAirView" to "zy063_air"
    )
    val PACKAGE_ICON_MAP_FIVE: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy064_video1",
        "com.awell.localmusic" to "zy064_music",
        "com.awell.radio" to "zy064_radio",
        "com.awell.bluetooth" to "zy064_btphone",
        "com.awell.navigation" to "zy064_navi1",
        "com.android.browser" to "zy064_liulanqi",
        "com.mediatek.filemanager" to "zy064_wjgl",
        "com.android.documentsui" to "zy064_wjgl",
        "com.awell.carsetting" to "zy064_shezhi",
        "com.awell.backcar" to "zy064_wjsr",
        "cn.kuwo.kwmusiccar" to "zy064_kuwo",
        "com.awell.eqselect" to "zy064_jhq",
        "com.awell.canbus" to "zy064_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy064_navi1",
        // "com.tima.carnet.vt" to "sf_zlink_circle",
        "net.easyconn" to "zy064_net_easyconn",
        "com.zjinnova.zlink" to "zy064_link",
        "com.awell.awellmanual" to "zy064_manual",
        "com.google.android.apps.maps" to "zy064_googlemap",
        "com.google.android.youtube" to "zy064_youtube",
        "com.android.vending" to "zy064_playstore",
        "com.android.chrome" to "zy064_liulanqi",
        //"com.google.android.googlequicksearchbox" to "sf_gg_circle",
        "com.android.gallery3d" to "zy064_gallery",
        "org.chromium.chrome" to "zy064_liulanqi",
        "com.awell.electricfan" to "zy064_fan",
        "com.awell.frontvideo" to "zy064_front_video",
        // "com.awell.keylight" to "sf_color_light_circle",
        "com.awell.keystudy" to "zy064_training",
        "com.awell.themesetting" to "zy064_theme",
        "com.ms.ms2160" to "zy064_usb_video_output",
        "com.awell.weather" to "zy064_weather",
        ".AwellCanbusAirView" to "zy064_air"
    )

    val PACKAGE_ICON_MAP_SIX: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy065_video1",
        "com.awell.localmusic" to "zy065_music",
        "com.awell.radio" to "zy065_radio",
        "com.awell.bluetooth" to "zy065_btphone",
        "com.awell.navigation" to "zy065_navi1",
        "com.android.browser" to "zy065_liulanqi",
        "com.mediatek.filemanager" to "zy065_wjgl",
        "com.android.documentsui" to "zy065_wjgl",
        "com.awell.carsetting" to "zy065_shezhi",
        "com.awell.backcar" to "zy065_wjsr",
        "cn.kuwo.kwmusiccar" to "zy065_kuwo",
        "com.awell.eqselect" to "zy065_jhq",
        "com.awell.canbus" to "zy065_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy065_navi1",
        // "com.tima.carnet.vt" to "sf_zlink_circle",
        "net.easyconn" to "zy065_net_easyconn",
        "com.zjinnova.zlink" to "zy065_link",
        "com.awell.awellmanual" to "zy065_manual",
        "com.google.android.apps.maps" to "zy065_googlemap",
        "com.google.android.youtube" to "zy065_youtube",
        "com.android.vending" to "zy065_playstore",
        "com.android.chrome" to "zy065_liulanqi",
        //"com.google.android.googlequicksearchbox" to "sf_gg_circle",
        "com.android.gallery3d" to "zy065_gallery",
        "org.chromium.chrome" to "zy065_liulanqi",
        "com.awell.electricfan" to "zy065_fan",
        "com.awell.frontvideo" to "zy065_front_video",
        // "com.awell.keylight" to "sf_color_light_circle",
        "com.awell.keystudy" to "zy065_training",
        "com.awell.themesetting" to "zy065_theme",
        "com.ms.ms2160" to "zy065_usb_video_output",
        "com.awell.weather" to "zy065_weather",
        ".AwellCanbusAirView" to "zy065_air"
    )

    val PACKAGE_ICON_MAP_SEVEN: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy066_video1",
        "com.awell.localmusic" to "zy066_music",
        "com.awell.radio" to "zy066_radio",
        "com.awell.bluetooth" to "zy066_btphone",
        "com.awell.navigation" to "zy066_navi1",
        "com.android.browser" to "zy066_liulanqi",
        "com.mediatek.filemanager" to "zy066_wjgl",
        "com.android.documentsui" to "zy066_wjgl",
        "com.awell.carsetting" to "zy066_shezhi",
        "com.awell.backcar" to "zy066_wjsr",
        "cn.kuwo.kwmusiccar" to "zy066_kuwo",
        "com.awell.eqselect" to "zy066_jhq",
        "com.awell.canbus" to "zy066_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy066_navi1",
        // "com.tima.carnet.vt" to "sf_zlink_circle",
        "net.easyconn" to "zy066_net_easyconn",
        "com.zjinnova.zlink" to "zy066_link",
        "com.awell.awellmanual" to "zy066_manual",
        "com.google.android.apps.maps" to "zy066_googlemap",
        "com.google.android.youtube" to "zy066_youtube",
        "com.android.vending" to "zy066_playstore",
        "com.android.chrome" to "zy066_liulanqi",
        //"com.google.android.googlequicksearchbox" to "sf_gg_circle",
        "com.android.gallery3d" to "zy066_gallery",
        "org.chromium.chrome" to "zy066_liulanqi",
        "com.awell.electricfan" to "zy066_fan",
        "com.awell.frontvideo" to "zy066_front_video",
        // "com.awell.keylight" to "sf_color_light_circle",
        "com.awell.keystudy" to "zy066_training",
        "com.awell.themesetting" to "zy066_theme",
        "com.ms.ms2160" to "zy066_usb_video_output",
        "com.awell.weather" to "zy066_weather",
        ".AwellCanbusAirView" to "zy066_air"
    )



}
