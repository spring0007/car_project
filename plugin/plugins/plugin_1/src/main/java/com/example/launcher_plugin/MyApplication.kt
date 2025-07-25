package com.example.launcher_plugin

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class MyApplication : Application(), ViewModelStoreOwner {
    private val appViewModelStore: ViewModelStore by lazy { ViewModelStore() }

    companion object {
        private lateinit var instance: MyApplication

        fun get(): MyApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}