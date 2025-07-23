package com.example.launcher_plugin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import com.tencent.shadow.sample.host.lib.HostAppsHolder

class MainActivity : Activity() {
    private val TAG: String = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = findViewById<Button>(R.id.SHOW_ALL_APPS)
        btn.setOnClickListener {
            Log.i(TAG, "huang click show all apps==>${HostAppsHolder.apps} this=>${this}")
            HostAppsHolder.apps?.showAllApps(findViewById<ViewGroup>(android.R.id.content))
            HostAppsHolder.apps?.printStr("message from plugin")
        }
        val load_btn = findViewById<Button>(R.id.LOAD_ALL_APPS)
        load_btn.setOnClickListener {
            Log.i(TAG, "onCreate: huang click load all apps==>")
            HostAppsHolder.apps?.loadApps()

        }

    }

    override fun finish() {
        super.finish()
        Log.i(TAG, "finish: huang finish this =>${this}")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        HostAppsHolder.apps?.hideAllApps()
    }
}