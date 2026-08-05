package com.awell.utils

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Objects

/**
 * 自由窗口工具类
 * 所有插件统一使用此版本，无需各自维护
 */
object FreeformUtils {

    private val TAG: String? = FreeformUtils::class.simpleName

    const val WINDOWING_MODE_FULLSCREEN: Int = 1
    const val WINDOWING_MODE_FREEFORM: Int = 5
    const val HIDE_FREEFORM: Int = 0x10
    const val OPEN_APP_TO_FREEFORM: Int = 0x11

    const val SETTINGS_FREEFORM_APP_PACKAGE_NAME: String = "freeform_app_package_name"
    const val SETTINGS_FREEFORM_APP_CLAZZ_NAME: String = "freeform_app_clazz_name"
    const val SETTINGS_UI_HAS_FREEFORM: String = "ui_has_freeform"
    var mOldHasFreeForm = "false"
    const val NAVI_GOOGLE_PKG: String = "com.google.android.apps.maps"
    const val NAVI_GOOGLE_CLAZZ: String = "com.google.android.maps.MapsActivity"
    const val NAVI_GAODE_PKG: String = "com.autonavi.amapauto"
    const val NAVI_GAODE_CLAZZ: String = "com.autonavi.amapauto.MainMapActivity"

    val MUSIC_KUWO_PKG: String = "cn.kuwo.kwmusiccar"

    private var top_Activity: String? = null

    fun startFreeformApp(context: Context, rect: Rect) {
        Log.i(TAG, "startFreeformApp");
        putStringSettingsSystem(context, SETTINGS_UI_HAS_FREEFORM, "true")
        Settings.System.putString(
            context.contentResolver, "freeform_last_bounds",
            rect.flattenToString()
        );

        var pkg =
            Settings.System.getString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME)
        var clazz: String? = null

        if (TextUtils.isEmpty(pkg) || !isAppInstalled(context, pkg)) {
            pkg = NAVI_GOOGLE_PKG
            clazz = NAVI_GOOGLE_CLAZZ
            Settings.System.putString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME, pkg)
            Settings.System.putString(context.contentResolver, SETTINGS_FREEFORM_APP_CLAZZ_NAME, NAVI_GOOGLE_CLAZZ)
        }

        val isRun = isAppRunning(context, pkg)
        // clazz 可能为 null(例如仅配置了包名未配置类名),直接 equals 会 NPE,使用空安全的 != 比较
        if (isRun && top_Activity != null && clazz != top_Activity)
            clazz = top_Activity

        if (isAppInstalled(context, pkg) && isGaoDeMap(pkg)) {
            clazz = NAVI_GAODE_CLAZZ
        }

        val options: ActivityOptions = makeLaunchOptions(context, rect)
        var intentFreeform = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intentFreeform == null) {
            intentFreeform = Intent(Intent.ACTION_MAIN)
            intentFreeform.setPackage(pkg)
        }

        intentFreeform.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )

        intentFreeform.addCategory(Intent.CATEGORY_LAUNCHER)

        if (clazz != null) {
            val cmp = ComponentName(pkg, clazz)
            intentFreeform.setComponent(cmp)
        }
        try {
            context.startActivity(intentFreeform, options.toBundle())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start freeform app")
            Settings.System.putString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME, null)
        }
    }

    private fun isKuWoMusic(context: Context, pkg: String?): Boolean {
        return pkg == MUSIC_KUWO_PKG
    }

    private fun isGaoDeMap(pkg: String): Boolean {
        return pkg == NAVI_GAODE_PKG
    }

    private fun makeLaunchOptions(context: Context, rect: Rect): ActivityOptions {
        val activityOptions = ActivityOptions.makeBasic()
        try {
            val method =
                ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
            method.invoke(activityOptions, WINDOWING_MODE_FREEFORM)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "startFreeFormActivity: error==> " + e.message)
        }
        activityOptions.setLaunchBounds(rect)

        try {
            val setTaskAlwaysOnTopMethod = ActivityOptions::class.java.getMethod(
                "setTaskAlwaysOnTop",
                Boolean::class.javaPrimitiveType
            )
            setTaskAlwaysOnTopMethod.invoke(activityOptions, true)
            Log.d(TAG, "setTaskAlwaysOnTop: true")
        } catch (e: Exception) {
            Log.e(TAG, "startFreeFormActivity: setTaskAlwaysOnTop error==> ${e.message}")
        }

        return activityOptions
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        try {
            val pm = context.packageManager
            val packageInfo =
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES) ?: return false
            if (!packageInfo.applicationInfo.enabled) {
                return false
            }
            return true
        } catch (e: java.lang.Exception) {
            return false
        }
    }

    private fun isAppRunning(context: Context, packageName: String): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val list = am.getRunningTasks(100)
        var appRun = false
        top_Activity = null
        for (info in list) {
            if (info.topActivity != null) {
                try {
                    if (info.topActivity!!.packageName == packageName ||
                        Objects.requireNonNull<ComponentName?>(info.baseActivity)
                            .getPackageName() == packageName
                    ) {
                        top_Activity = info.topActivity!!.className
                        appRun = true
                        break
                    }
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
        return appRun
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun isFreeformAppRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val list = am.getRunningTasks(100)
        val pkg =
            Settings.System.getString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME)
        Log.i(TAG, "isFreeformAppRunning: pkg=>${pkg}")
        for (info in list) {
            info?.let {
                if (pkg.equals(it.topActivity?.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    fun putStringSettingsSystem(context: Context, name: String, value: String) {
        mOldHasFreeForm = getStringSettingsSystem(context, name) ?: "false"
        if (mOldHasFreeForm != value) {
            Settings.System.putString(context.contentResolver, name, value)
            mOldHasFreeForm = value
        }
    }

    private fun getStringSettingsSystem(context: Context, name: String): String? {
        return Settings.System.getString(context.contentResolver, name)
    }
}
