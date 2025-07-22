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
    }

    override fun finish() {
        super.finish()
        Log.i(TAG, "finish: huang finish this =>${this}")
    }


}