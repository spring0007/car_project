package com.launcher.ui25.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import com.launcher.ui25.R
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
       // "com.awell.localvideo" to R.drawable.sf_video_circle,
       // "com.awell.localmusic" to R.drawable.sf_music_circle,
        "com.awell.radio" to R.drawable.sf_radio_circle,
        "com.awell.bluetooth" to R.drawable.sf_bt_circle,
        "com.awell.navigation" to R.drawable.sf_navigation_circle,
        /*"com.android.browser" to R.drawable.sf_liulanqi_circle,
        "com.mediatek.filemanager" to R.drawable.sf_wjgl_circle,
        "com.android.documentsui" to R.drawable.sf_wjgl_circle,
        "com.awell.carsetting" to R.drawable.sf_shezhi_circle,
        "com.awell.backcar" to R.drawable.sf_wjsr_circle,
        "cn.kuwo.kwmusiccar" to R.drawable.sf_kugou_circle,
        "com.awell.eqselect" to R.drawable.sf_jhq_circle,
        "com.awell.canbus" to R.drawable.sf_yuanche_circle,
        "com.awell.canbus2" to R.drawable.sf_yuanche_circle,
        "com.autonavi.amapauto" to R.drawable.sf_gaode_circle,
        // "com.tima.carnet.vt" to R.drawable.sf_zlink_circle,
        "net.easyconn" to R.drawable.sf_zlink_circle,
        "com.zjinnova.zlink" to R.drawable.sf_zlink_circle,
        "com.awell.awellmanual" to R.drawable.sf_manual_circle,
        // "com.google.android.apps.maps" to R.drawable.sf_googlemap_circle,
        "com.google.android.youtube" to R.drawable.sf_youtube_circle,
        "com.android.vending" to R.drawable.sf_playstore_circle,
        "com.android.chrome" to R.drawable.sf_liulanqi_circle,
        "com.google.android.googlequicksearchbox" to R.drawable.sf_gg_circle,
        "com.android.gallery3d" to R.drawable.sf_gallery_circle,
        "org.chromium.chrome" to R.drawable.sf_liulanqi_circle,
        "com.awell.electricfan" to R.drawable.sf_fan_circle,
        "com.awell.frontvideo" to R.drawable.sf_front_video_circle,
        "com.awell.keylight" to R.drawable.sf_color_light_circle,
        "com.awell.keystudy" to R.drawable.sf_training_circle,
        "com.awell.themesetting" to R.drawable.sf_theme_circle,
        "com.ms.ms2160" to R.drawable.sf_usb_video_output_circle,
        "com.awell.weather" to R.drawable.sf_weather_circle*/
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


}
