package com.example.launcher_plugin;

import android.app.Application;
import android.util.Log;

public class PluginApp extends Application {

    private final String TAG = PluginApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: huang create application==>");
    }
}
