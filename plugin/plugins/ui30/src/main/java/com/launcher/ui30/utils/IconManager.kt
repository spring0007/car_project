package com.launcher.ui30.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import com.launcher.ui30.R
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


    // 圆形主题图标映射表
    val PACKAGE_ICON_MAP_CIRCLE: Map<String, Int> = mapOf(
        "com.awell.localvideo" to R.drawable.ic_video_app2,
        "com.awell.localmusic" to R.drawable.ic_music_app,
        "com.awell.radio" to R.drawable.ic_radio_app2,
        "com.awell.bluetooth" to R.drawable.ic_bluetooth_app2,
        "com.awell.navigation" to R.drawable.ic_navi_app,
        "com.android.browser" to R.drawable.ic_browser_app,
        "com.mediatek.filemanager" to R.drawable.ic_file_manager_app,
        "com.android.documentsui" to R.drawable.ic_file_manager_app,
        "com.awell.carsetting" to R.drawable.ic_car_setting2,
        "com.awell.backcar" to R.drawable.ic_usb_app,
        "cn.kuwo.kwmusiccar" to R.drawable.ic_kw_app,
        "com.awell.eqselect" to R.drawable.ic_jhq_app,
        "com.awell.canbus" to R.drawable.ic_car_app,
        "com.awell.canbus2" to R.drawable.ic_car_app,
        "com.autonavi.amapauto" to R.drawable.ic_navi_app,
//        "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
//        "net.easyconn" to R.drawable.sf_zlink_circle,
//        "com.zjinnova.zlink" to R.drawable.sf_zlink_circle,
        "com.awell.awellmanual" to R.drawable.ic_manual_app,
//        "com.google.android.apps.maps" to R.drawable.sf_googlemap_circle,
//        "com.google.android.youtube" to R.drawable.sf_youtube_circle,
        "com.android.vending" to R.drawable.ic_app_store,
        "com.android.chrome" to R.drawable.ic_browser_app,
//        "com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.ic_gallery_app,
        "org.chromium.chrome" to R.drawable.ic_browser_app,
        "com.awell.electricfan" to R.drawable.ic_fan_app,
        "com.awell.frontvideo" to R.drawable.ic_forwardview_app,
        "com.awell.keylight" to R.drawable.ic_color_light_app,
//        "com.awell.keystudy" to R.drawable.sf_training_circle,
        "com.awell.themesetting" to R.drawable.ic_theme_app,
        "com.ms.ms2160" to R.drawable.ic_video_output_app,
        "com.awell.weather" to R.drawable.ic_weather_app,
     //   ".AwellCanbusAirView" to R.drawable.ic_air_app
    )

    // 当前主题模式，默认 1
    var currentThemeMode: Int = 1
        set(value) {
            field = value
            // 主题切换时清空缓存
            evictAll()
        }

    // 图标缓存 (LRU Cache, 最大容量 100)
    private val iconCache = LruCache<String, Drawable>(100)

    // PackageManager 实例缓存
    private var packageManager: PackageManager? = null

    /**
     * 初始化管理器 (可选调用，用于预加载资源)
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
            packageInfo.applicationInfo.loadIcon(pm)
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
     * 根据包名和主题模式获取自定义图标资源 ID
     * @param packageName 包名
     * @param themeMode 主题模式
     * @return 自定义图标资源 ID，如果没有则返回 null
     */
    private fun getCustomIconForPackage(packageName: String, themeMode: Int): Int? {
        return/* when (themeMode) {
            1 ->*/ PACKAGE_ICON_MAP_CIRCLE[packageName]  // 圆形主题使用自定义图标映射
        /*else -> null  // 其他主题暂不使用自定义图标
    }*/
    }
    public fun getPackageIconMap(): Map<String, String> {
        return PACKAGE_ICON_MAP_CIRCLE.mapValues { it.value.toString() }.toMap()
    }


}
