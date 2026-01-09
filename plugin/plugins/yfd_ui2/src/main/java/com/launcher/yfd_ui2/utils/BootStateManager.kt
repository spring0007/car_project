package com.launcher.yfd_ui2.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings


object BootStateManager {
    private const val KEY_IS_FIRST_BOOT = "is_first_boot"

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
        LogUtil.i("packageName = $packageName")
        if (packageName != null) {
            // packageName = SystemProperties.get("persist.sys.awell.self.pkg", "")
            return true
        }else{
            packageName =
                Settings.System.getString(context.contentResolver, "bootapk_packname")
            if (packageName!=null)
                return true
        }
        val bootstatus = Settings.System.getInt(context.contentResolver, "navi_boot", 0)
        if (bootstatus != 0) {
            return true
        }
        try {
            val packageInfo: PackageInfo? =
                context.packageManager.getPackageInfo("cn.cardoor.zt360", PackageManager.GET_ACTIVITIES)
            if (packageInfo == null) {
                return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        return  false

    }



}