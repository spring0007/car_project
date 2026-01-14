package com.launcher.yfd_ui01.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.SystemProperties
import android.provider.Settings
import com.awell.utils.CommonData

object BootStateManager {
    private const val KEY_IS_FIRST_BOOT = "is_first_boot"
    private const val META_REVERSING_CAMERA_TYPE: Int = 0x120 // 倒车摄像头类型选择（0=persist, 1=普通，2=360）
    private const val META_360_STARTUP_ENABLED: Int = 0x121 // 360开机启动开关（0=开启，1=关闭）
    /**
     * 是否是开机后的第一次检测
     */
    fun isFirstBootAfterReboot(context: Context): Boolean {
        val firstBoot = Settings.System.getInt(context.contentResolver, KEY_IS_FIRST_BOOT,1)
        LogUtil.d("firstBoot=$firstBoot")
        return if (firstBoot==0)
            getBootApkPkg(context)
        else
            false
    }

    /**
     * 标记开机已完成
     */
    fun markBootCompleted(context: Context ) {
        Settings.System.putInt(context.contentResolver, KEY_IS_FIRST_BOOT,1)
        LogUtil.d("Boot completed marked")
    }

    /**
     * 重置为初始状态（在关机时调用）
     */
    fun resetForShutdown(context: Context) {
        Settings.System.putInt(context.contentResolver, KEY_IS_FIRST_BOOT,0)
        LogUtil.d("State reset for shutdown")
    }

    private fun getBootApkPkg(context:Context): Boolean{
        var packageName:String?
        packageName =  Settings.System.getString(context.contentResolver, "self_start_packageName")
        //防止开机未收到广播
        Settings.System.putInt(context.contentResolver, KEY_IS_FIRST_BOOT,1)
        if (packageName != null) {
            // packageName = SystemProperties.get("persist.sys.awell.self.pkg", "")
            return getInstallAPK(context,packageName)
        }else{
            packageName =
                Settings.System.getString(context.contentResolver, "bootapk_packname")
            if (packageName!=null) {
                return getInstallAPK(context,packageName)
            }
        }
        var bootstatus = Settings.System.getInt(context.contentResolver, "navi_boot", 0)
        if (bootstatus != 0) {
            return true
        }

        val unit = ByteArray(2)
        CommonData.readDataToMeta(unit, META_REVERSING_CAMERA_TYPE)

        bootstatus = unit[0].toInt() and 0xff
        if (bootstatus == 0) {
            bootstatus = SystemProperties.getInt("persist.sys.awell.reversing.camera.type", 1)
        }

        if (bootstatus == 2 && (unit[1].toInt() and 0xff) == 0) {
            return getInstallAPK(context,"cn.cardoor.zt360")
        }

        return false

    }

    private fun getInstallAPK(context: Context,pkg: String): Boolean{

        try {
            val packageInfo: PackageInfo? =
                context.packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            if (packageInfo != null) {
                LogUtil.d("getBootApkPkg true,pkg=$pkg")
                return true
            }
            LogUtil.d("getBootApkPkg false,pkg=$pkg")
            return false
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }



}