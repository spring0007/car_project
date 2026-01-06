package com.launcher.yfd_ui2.utils

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

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
    const val MUSIC_PKG: String = "com.awell.localmusic"
    const val NAVI_GAODE_CLAZZ: String = "com.autonavi.amapauto.MainMapActivity"

    val MUSIC_KUWO_PKG: String = "cn.kuwo.kwmusiccar"
    val MUSIC_KUWO_CLAZZ: String = "cn.kuwo.kwmusiccar.ui.MainActivity"
    //val MUSIC_KUWO_CLAZZ: String = "cn.kuwo.kwmusiccar.ui.WelcomeActivity"

    fun startFreeformApp(context: Context, rect: Rect) {

        putStringSettingsSystem(context, SETTINGS_UI_HAS_FREEFORM, "true")

        var pkg =
            Settings.System.getString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME)
        var clazz = Settings.System.getString(
            context.contentResolver,
            SETTINGS_FREEFORM_APP_CLAZZ_NAME
        )

        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(clazz)) {
            pkg = NAVI_GOOGLE_PKG
            clazz = NAVI_GOOGLE_CLAZZ
        }

        if (!isAppInstalled(context, pkg)) {
            pkg = NAVI_GAODE_PKG
            clazz = NAVI_GAODE_CLAZZ
        }

        if (isAppInstalled(context, pkg) && isKuWoMusic(context, pkg)) {
            clazz = MUSIC_KUWO_CLAZZ
        }
        if (isAppInstalled(context, pkg) && isGaoDeMap(pkg)) {
            clazz = NAVI_GAODE_CLAZZ
        }

        val options: ActivityOptions = makeLaunchOptions(context, rect)
        val intentFreeform = Intent()
        intentFreeform.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentFreeform.addCategory(Intent.CATEGORY_LAUNCHER)
        intentFreeform.setPackage(pkg)

        if (clazz != null) {
            val cmp = ComponentName(pkg, clazz)
            intentFreeform.setComponent(cmp)
        }
        context.startActivity(intentFreeform, options.toBundle())

    }

    private fun isKuWoMusic(context: Context, pkg: String?): Boolean {
        return pkg == MUSIC_KUWO_PKG
    }

    private fun isGaoDeMap(pkg: String):Boolean {
        return pkg== NAVI_GAODE_PKG
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
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
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