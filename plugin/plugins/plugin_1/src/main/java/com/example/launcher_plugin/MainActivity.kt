package com.example.launcher_plugin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class MainActivity : Activity() {
    private val TAG: String = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val intent = Intent(this, UIActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        Log.i(TAG, "onCreate: huang start activity =>$intent")
        startActivity(intent)
        finish()
    }

    override fun finish() {
        super.finish()
        Log.i(TAG, "finish: huang finish this =>${this}")
    }
}