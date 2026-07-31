package com.launcher.yfd_ui01.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            //Log.i("yfd_ui01", "BootReceiver: Boot completed")
            // 这里可以添加开机启动时需要执行的逻辑
        }
    }
}
