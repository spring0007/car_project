package com.launcher.ui25.utils

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
    //const val MUSIC_PKG: String = "com.awell.localmusic"
    const val NAVI_GAODE_CLAZZ: String = "com.autonavi.amapauto.MainMapActivity"

    val MUSIC_KUWO_PKG: String = "cn.kuwo.kwmusiccar"
    //val MUSIC_KUWO_CLAZZ: String = "cn.kuwo.kwmusiccar.ui.MainActivity"

    //val MUSIC_KUWO_CLAZZ: String = "cn.kuwo.kwmusiccar.ui.WelcomeActivity"
    private var top_Activity: String? = null

    fun startFreeformApp(context: Context, rect: Rect) {
        LogUtil.i("startFreeformApp");
        putStringSettingsSystem(context, SETTINGS_UI_HAS_FREEFORM, "true")
        Settings.System.putString(
            context.contentResolver, "freeform_last_bounds",
            rect.flattenToString()
        );

        var pkg =
            Settings.System.getString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME)
        var clazz: String? = null
            //Settings.System.getString( context.contentResolver, SETTINGS_FREEFORM_APP_CLAZZ_NAME)

        if (TextUtils.isEmpty(pkg) || !isAppInstalled(context, pkg)) {
            pkg = NAVI_GOOGLE_PKG
            clazz = NAVI_GOOGLE_CLAZZ
            Settings.System.putString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME, pkg)
            Settings.System.putString(context.contentResolver, SETTINGS_FREEFORM_APP_CLAZZ_NAME, NAVI_GOOGLE_CLAZZ)
        }


        val isRun = isAppRunning(context,pkg)
        if(isRun && top_Activity!=null &&  (!clazz.equals(top_Activity)))
            clazz = top_Activity

        /*if (isAppInstalled(context, pkg) && isKuWoMusic(context, pkg)) {
            clazz = MUSIC_KUWO_CLAZZ
        }*/
        if (isAppInstalled(context, pkg) && isGaoDeMap(pkg)) {
            clazz = NAVI_GAODE_CLAZZ
        }

        val options: ActivityOptions = makeLaunchOptions(context, rect)
        var intentFreeform = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intentFreeform==null) {
            intentFreeform = Intent(Intent.ACTION_MAIN);
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
            LogUtil.e("Failed to start freeform app")
            // �����ڴ˴�������Ч��ϵͳ���ã������´��ٴ�
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
            LogUtil.e("startFreeFormActivity: error==> " + e.message)
        }
        activityOptions.setLaunchBounds(rect)

        try {
            val setTaskAlwaysOnTopMethod = ActivityOptions::class.java.getMethod(
                "setTaskAlwaysOnTop",
                Boolean::class.javaPrimitiveType
            )
            setTaskAlwaysOnTopMethod.invoke(activityOptions, true)
            LogUtil.d("setTaskAlwaysOnTop: true")
        } catch (e: Exception) {
            LogUtil.e("startFreeFormActivity: setTaskAlwaysOnTop error==> ${e.message}")
        }

        return activityOptions
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        try {
            val pm = context.packageManager

            // 1.Check if the application exists
            val packageInfo =
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES) ?: return false

            // 2. Check if the application is available (non disabled state)
            if (!packageInfo.applicationInfo.enabled) {
                return false
            }

            // 3. Get Startup Intent
            val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return false

            // 4. Check if the target activity exists
            val resolveInfo = pm.resolveActivity(launchIntent, 0) ?: return false

            // 5.Check if you have permission to start
            val requiredPermission = resolveInfo.activityInfo.permission
            if (requiredPermission != null) {
                if (context.checkCallingOrSelfPermission(requiredPermission) !== PackageManager.PERMISSION_GRANTED) {
                    // Request permission
                    //ActivityCompat.requestPermissions((Activity) context,
                    //	new String[]{requiredPermission},
                    //	1001);
                    return false
                }
            }

            return true


            /*PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo != null;*/
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
        Log.i(TAG, "isFreeformAppRunning: huang pkg=>${pkg}")
        for (info in list) {
            info?.let {
                Log.i(TAG, "isFreeformAppRunning: huang package=>${it.topActivity?.packageName}")
                Log.i(TAG, "isFreeformAppRunning: huang task id=>" + it.taskId)
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