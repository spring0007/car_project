package com.example.launcher_plugin

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class MyApplication : Application(), ViewModelStoreOwner {
    private val TAG: String = MyApplication.javaClass.simpleName
    private val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }

    companion object {
        private lateinit var instance: MyApplication

        fun get(): MyApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "onCreate: huang my application==>${instance}")
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}