package com.launcher.zy_ui06.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import com.launcher.zy_ui06.R
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

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
            else -> PACKAGE_ICON_MAP_CIRCLE_ONE[packageName]  // 其他主题暂不使用自定义图标
        }
    }

    ///=============================
    val PACKAGE_ICON_MAP: Map<String, String> = mapOf(
        "com.awell.localvideo" to "zy060_video",
        "com.awell.localmusic" to "zy060_music",
        "com.awell.radio" to "zy060_radio",
        "com.awell.bluetooth" to "zy060_btphone",
        "com.awell.navigation" to "zy060_navi",
        "com.android.browser" to "zy060_liulanqi",
        "com.mediatek.filemanager" to "zy060_wjgl",
        "com.android.documentsui" to "zy060_wjgl",
        "com.awell.carsetting" to "zy060_shezhi",
        "com.awell.backcar" to "zy060_wjsr",
        "cn.kuwo.kwmusiccar" to "zy060_kuwo",
        "com.awell.eqselect" to "zy060_jhq",
        "com.awell.canbus" to "zy060_yuanche",
        //"com.awell.canbus2" to "sf_yuanche_circle",
        "com.autonavi.amapauto" to "zy060_navi",
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
         "com.awell.weather" to "zy060_weather"
    )



}
