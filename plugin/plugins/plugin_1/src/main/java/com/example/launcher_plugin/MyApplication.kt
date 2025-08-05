package com.example.launcher_plugin

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class MyApplication : Application(), ViewModelStoreOwner, LifecycleOwner {
    private val TAG: String = MyApplication.javaClass.simpleName
    private val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }
    private val lifecycleRegistry = LifecycleRegistry(this)

    companion object {
        private lateinit var instance: MyApplication

        fun get(): MyApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        Log.i(TAG, "onCreate: huang my application==>${instance}")
    }

    override fun onTerminate() {
        super.onTerminate()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}