package com.example.plugin_2

import android.app.Activity
import android.os.Bundle
import android.util.Log

class UIActivity : Activity() {

    private val TAG = UIActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate: huang create activity==>")
    }

}