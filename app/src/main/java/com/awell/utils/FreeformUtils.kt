package com.awell.utils

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi

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

    /**
     * 本进程内**成功拉起过的小窗 app 包名**。
     * 只用来给 [isFreeformAppForceStopped] 做门槛:FLAG_STOPPED 对"出厂后从未启动过"的
     * 预装包同样为真,不能只凭它跳过自动拉起,否则小窗永远不显示。
     *
     * 必须按包记录,不能用"本进程拉起过某个小窗 app"这种进程级开关:进程先拉起过 Maps 之后,
     * 用户把配置切到另一个从未启动过的包(FLAG_STOPPED 自安装起就为真),进程级开关会把
     * "从未启动过"误判成"拉起过又被强停" → 插件守卫直接 return、[startFreeformApp] 根本不被
     * 调用,包永不启动、标志也就永不清除 → 该应用永远拉不起来(2026-09-14 log 定位)。
     */
    private val sFreeformAppLaunchedPkgs: MutableSet<String> =
        java.util.Collections.synchronizedSet(HashSet<String>())

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
        // 小窗已经"原地显示"在屏幕上时,这次 startActivity 不会带来任何变化:系统侧只会产生
        // 一次空过渡(Transition #N t=OPEN r=[] c=[] → "No transition roots … so abort"),
        // 桌面上表现为回桌面瞬间一次无动画闪烁。此时直接返回,不重复拉起。
        // 注意判据里的"真正可见"(见 isFreeformWindowInPlace):被 hideFreeform 收起的小窗
        // isVisible=false,不会走这里,仍然照旧走下面的启动流程把它拉回前台。
        if (isRun && isFreeformWindowInPlace(context, pkg, rect)) {
            Log.i(TAG, "startFreeformApp: freeform window already in place, skip relaunch")
            return
        }
        // 小窗 app 已在运行时,复用它当前显示的 activity。
        // 否则 clazz 为 null,intent 会用 getLaunchIntentForPackage() 给的入口 activity
        // (如 Waze 的 FreeMapAppActivity,standard 启动模式),每次回桌面都会新建 task/Activity 实例,
        // 应用侧表现为"被重启"。
        if (isRun) {
            clazz = top_Activity ?: configuredClazz(context, pkg)
        }

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
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        // FLAG_ACTIVITY_RESET_TASK_IF_NEEDED 会让系统对已存在的 task 执行 reset:把 launcher intent
        // 重新投递给根 activity(不匹配时甚至清栈重建),Waze 会因此重新处理 deep link、重建地图界面。
        // 只有全新启动(应用未运行)时才需要它。
        if (!isRun) {
            intentFreeform.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }

        intentFreeform.addCategory(Intent.CATEGORY_LAUNCHER)

        if (clazz != null) {
            val cmp = ComponentName(pkg, clazz)
            intentFreeform.setComponent(cmp)
        }
        try {
            context.startActivity(intentFreeform, options.toBundle())
            sFreeformAppLaunchedPkgs.add(pkg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start freeform app")
            Settings.System.putString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME, null)
        }
    }

    /**
     * 配置的小窗 app 是否被"强行停止"过(PackageManager 里带 FLAG_STOPPED)。
     *
     * 场景:最近任务"清除全部"、第三方清理/管家类应用会对包做 force-stop。此时小窗 app 的
     * service 被拆、包被置成 force-stopped,应用自己往往会 System.exit(0);如果 launcher 回到
     * 桌面后仍然无条件自动拉起,会看到"小窗闪一下又消失"(冷启动 → 应用立刻自杀)。
     * 系统侧已对小窗包豁免 force-stop([ActivityManagerService.forceStopPackage]、[ActivityTaskSupervisor.removeTask]),
     * 这里做兜底:识别到被 force-stop 就跳过自动拉起,等用户从应用抽屉显式点一次。
     *
     * 两个前提,避免误判:
     * 1. 只在本进程内**成功拉起过这个包**之后才生效(sFreeformAppLaunchedPkgs),否则出厂后
     *    从未启动过的预装包(FLAG_STOPPED 同为 true)会永远不自动显示。门槛必须按包记:
     *    若用"本进程拉起过某个小窗 app"的进程级开关,先拉起过 Maps,再把配置切到另一个
     *    从未启动过的包时,也会被误判成"拉起过又被强停" → 调用方直接 return、小窗永远不显示;
     * 2. 返回 true 不是死角 —— 任何进程启动都会让 AMS.startProcessLocked 调
     *    setPackageStoppedState(pkg,false) 清掉该标志,用户从应用抽屉启动一次即恢复自动拉起。
     */
    fun isFreeformAppForceStopped(context: Context): Boolean {
        val pkg =
            Settings.System.getString(context.contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME)
        if (TextUtils.isEmpty(pkg)) {
            return false
        }
        // 本进程没成功拉起过这个包,就不可能观察到"它被拉起后又被强停",一律放行 ——
        // 用户显式把某个应用配成小窗应用,就该正常显示它(2026-09-14 修复)。
        if (!sFreeformAppLaunchedPkgs.contains(pkg!!)) {
            Log.i(TAG, "isFreeformAppForceStopped: $pkg not launched by this process, allow start")
            return false
        }
        return try {
            val ai: ApplicationInfo = context.packageManager.getApplicationInfo(pkg!!, 0)
            val stopped = (ai.flags and ApplicationInfo.FLAG_STOPPED) != 0
            if (stopped) {
                Log.w(TAG, "isFreeformAppForceStopped: $pkg is force-stopped, skip auto start")
            }
            stopped
        } catch (e: java.lang.Exception) {
            // 读不到就当作正常,不要因为一次异常把小窗卡死。
            false
        }
    }

    private fun isKuWoMusic(context: Context, pkg: String?): Boolean {
        return pkg == MUSIC_KUWO_PKG
    }

    private fun isGaoDeMap(pkg: String): Boolean {
        return pkg == NAVI_GAODE_PKG
    }

    /**
     * 读取配置里记录的小窗 activity 类名。
     * 只有能解析到该组件时才返回,避免用失效配置启动导致 ActivityNotFoundException
     * 后把 freeform_app_package_name 清空。
     */
    private fun configuredClazz(context: Context, pkg: String): String? {
        val name: String = Settings.System.getString(
            context.contentResolver, SETTINGS_FREEFORM_APP_CLAZZ_NAME
        ) ?: return null
        if (name.isEmpty()) {
            return null
        }
        return try {
            context.packageManager.getActivityInfo(ComponentName(pkg, name), 0)
            name
        } catch (e: java.lang.Exception) {
            Log.w(TAG, "configuredClazz: $pkg/$name not resolvable, ignore")
            null
        }
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
            try {
                val top = info.topActivity
                // 优先取真正有 top activity 的 task。被遗留的空 task(只剩 baseActivity)会让
                // top_Activity 为 null,进而使 startFreeformApp 回退到入口 activity 并新建 task。
                if (top != null && top.packageName == packageName) {
                    top_Activity = top.className
                    appRun = true
                    break
                }
                if (info.baseActivity?.packageName == packageName) {
                    appRun = true
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
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
        if (TextUtils.isEmpty(pkg)) {
            return false
        }
        for (info in list) {
            info?.let {
                if (pkg.equals(it.topActivity?.packageName) ||
                    pkg.equals(it.baseActivity?.packageName)
                ) {
                    val mode = windowingModeOf(it)
                    val hasTop = it.topActivity != null
                    Log.i(
                        TAG,
                        "isFreeformAppRunning: found taskId=${it.id} mode=${mode}" +
                                " top=${it.topActivity} hasTop=$hasTop"
                    )
                    // 只有"确实处于小窗模式且还有 activity 存活"才算小窗已在运行。
                    // 已被 fullScreenFreeform 拉成全屏、或只剩一个遗留空 task 时返回 false,
                    // 由 launcher 重新拉起(与改动前行为一致,不会让用户看到空白小窗)。
                    // 反射读不到窗口模式时(理论不会发生)退回旧的"只看包名"判断。
                    if (mode == null) {
                        return hasTop
                    }
                    if (mode == WINDOWING_MODE_FREEFORM && hasTop) {
                        return true
                    }
                }
            }
        }
        Log.i(TAG, "isFreeformAppRunning: false")
        return false
    }

    /**
     * 读 RunningTaskInfo.windowingMode。
     * 该字段是 @SystemApi,公开 SDK 里不存在,只能反射;与 makeLaunchOptions 里
     * setLaunchWindowingMode/setTaskAlwaysOnTop 的做法一致。读不到返回 null,由调用方回退。
     */
    private fun windowingModeOf(info: ActivityManager.RunningTaskInfo): Int? {
        return try {
            ActivityManager.RunningTaskInfo::class.java
                .getField("windowingMode")
                .getInt(info)
        } catch (e: java.lang.Exception) {
            Log.w(TAG, "windowingModeOf: error==> ${e.message}")
            null
        }
    }

    /**
     * 配置的小窗 app 是否"已经原地显示"——处于 FREEFORM 模式、真正可见,且就在本次要显示的位置上。
     *
     * 用途:跳过无意义的重复拉起。插件每次 onResume 都会调用 [startFreeformApp],若小窗本来就
     * 显示在屏幕上(用户从全屏应用按 HOME 回桌面、小窗从未被收起),这次 startActivity 不产生
     * 任何变化,系统侧只留下一次空过渡(Transition #N t=OPEN r=[] c=[] → "No transition roots …
     * so abort"),表现为一次无动画闪烁。
     *
     * 判据必须用"系统算出来的真实可见性"(TaskInfo.isVisible),不能用"mode==FREEFORM 且 bounds 相同":
     * 厂商的 hideFreeformWindow 收起小窗只做 moveToBack + setAlwaysOnTop(false),
     * **既不改窗口模式也不改 bounds**(ActivityRecord 里 setWindowingMode/setBounds 两行是注释掉的),
     * 于是"已收起"和"已显示"两种状态下 mode 与位置完全一致,只有可见性不同:
     * - 已收起(isVisible=false):回桌面**必须**靠这次 startActivity 把小窗拉回前台
     *   (日志里 #203 那种带 m=TO_FRONT 的真实过渡),漏掉小窗就再也回不来;
     * - 已显示(isVisible=true):这次 startActivity 是纯空操作(#206 空过渡)。
     * 另外要求 isVisibleRequested 为真,以避开"正在被收起(画面还在但已不再请求可见)"的瞬间;
     * 位置(positionInParent)只作收敛:明确读到小窗在别处(用户拖过)才不跳过。
     *
     * 找不到对应 task、模式不是 FREEFORM、判定为不可见、或反射读不到字段时都返回 false,
     * 即退回改动前的"照旧拉起"行为,不会让小窗消失。
     */
    private fun isFreeformWindowInPlace(context: Context, pkg: String, rect: Rect): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (info in am.getRunningTasks(100)) {
            try {
                if (info?.topActivity?.packageName != pkg) continue
                if (windowingModeOf(info) != WINDOWING_MODE_FREEFORM) continue
                if (!boolFieldOf(info, "isVisible") || !boolFieldOf(info, "isVisibleRequested")) {
                    continue
                }
                // 位置只作"顺带"的收敛条件:只有明确读到小窗在别处(用户拖过)才不跳过 ——
                // 那种情况下这次拉起可能把它挪回配置位置,是有效果的。读不到位置(反射失败)
                // 不因此放弃跳过,免得修复被一个附加条件静默失效。
                val pos = pointFieldOf(info, "positionInParent")
                if (pos != null && (pos.x != rect.left || pos.y != rect.top)) {
                    Log.i(
                        TAG,
                        "isFreeformWindowInPlace: visible but pos=$pos != rect=$rect, not in place"
                    )
                    continue
                }
                Log.i(
                    TAG,
                    "isFreeformWindowInPlace: taskId=${info.id} mode=FREEFORM visible=true" +
                            " pos=$pos rect=$rect"
                )
                return true
            } catch (e: java.lang.Exception) {
                Log.w(TAG, "isFreeformWindowInPlace: error==> ${e.message}")
            }
        }
        return false
    }

    /**
     * 读 RunningTaskInfo 的布尔字段(isVisible / isVisibleRequested)。
     * 这两个字段在本分支的 android.app.TaskInfo 里都是 @hide(见 TaskInfo.java:334/340),
     * 公开 SDK 里不存在,只能反射,与 [windowingModeOf] 同一套做法。
     * 读不到时返回 false:可见性判定为假 → 照旧拉起,不会因为反射失败而漏掉拉回小窗。
     */
    private fun boolFieldOf(info: ActivityManager.RunningTaskInfo, name: String): Boolean {
        return try {
            ActivityManager.RunningTaskInfo::class.java
                .getField(name)
                .getBoolean(info)
        } catch (e: java.lang.Exception) {
            Log.w(TAG, "boolFieldOf: $name error==> ${e.message}")
            false
        }
    }

    /**
     * 读 RunningTaskInfo 的 Point 字段(positionInParent,同样是 @hide,TaskInfo.java:308)。
     * 它等于小窗 task 左上角在父容器里的位置,实测与自由窗口 bounds 的原点一致
     * (日志里 positionInParent=Point(358, 93) 对应 Rect(358, 93 - 1126, 482))。
     * 读不到返回 null,由调用方决定怎么回退。
     */
    private fun pointFieldOf(
        info: ActivityManager.RunningTaskInfo,
        name: String
    ): Point? {
        return try {
            ActivityManager.RunningTaskInfo::class.java
                .getField(name)
                .get(info) as? Point
        } catch (e: java.lang.Exception) {
            Log.w(TAG, "pointFieldOf: $name error==> ${e.message}")
            null
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
