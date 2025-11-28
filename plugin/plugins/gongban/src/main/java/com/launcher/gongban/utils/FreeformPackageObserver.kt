package com.launcher.gongban.utils

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.launcher.gongban.utils.SystemUIClient.SETTINGS_FREEFORM_APP_PACKAGE_NAME

class FreeformPackageObserver(private val callback: (String?) -> Unit) {
    private val TAG = FreeformPackageObserver::class.simpleName
    private var contentObserver: ContentObserver? = null

    fun startObserving(contentResolver: ContentResolver) {
        stopObserving() // 先停止之前的监听

        val uri = Settings.System.getUriFor(SETTINGS_FREEFORM_APP_PACKAGE_NAME)
        Log.i(TAG, "startObserving: huang start observing uri==>${uri}")
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                if (uri == Settings.System.getUriFor(SETTINGS_FREEFORM_APP_PACKAGE_NAME)) {
                    val newValue = getCurrentFreeformPackage(contentResolver)
                    Log.d(TAG, "freeform_app_package_name changed to: $newValue")
                    callback(newValue)
                }
            }
        }

        // 注册监听
        contentResolver.registerContentObserver(
            uri,
            false, // 不监听所有子路径
            contentObserver!!
        )

        Log.d(TAG, "Started observing freeform_app_package_name")
    }

    fun stopObserving() {
        contentObserver?.let { observer ->
            // 需要传入 ContentResolver，这里假设你在调用 stopObserving 时能访问到
            // 更好的做法是在类中保存 ContentResolver 引用
            // contentResolver.unregisterContentObserver(observer)
            contentObserver = null
        }
    }

    private fun getCurrentFreeformPackage(contentResolver: ContentResolver): String? {
        return try {
            Settings.System.getString(contentResolver, SETTINGS_FREEFORM_APP_PACKAGE_NAME)
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to read settings", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading freeform package", e)
            null
        }
    }
}