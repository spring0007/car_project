/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.awell.launcher2;

import static android.os.Process.myPid;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.awell.impl.HostApps;
import com.awell.impl.ModelImpl;
import com.awell.launcher.R;
import com.awell.control.AppsCustomizeControl;
import com.awell.plugin_shadow.AndroidLogLoggerFactory;
import com.awell.plugin_shadow.PluginHelper;
import com.awell.plugin_shadow.manager.Shadow;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.dynamic.host.DynamicRuntime;
import com.tencent.shadow.dynamic.host.PluginManager;
import com.tencent.shadow.sample.host.lib.HostAppsHolder;
import com.tencent.shadow.sample.host.lib.HostUiLayerProvider;

import java.io.File;
import java.lang.ref.WeakReference;

//import cn.kuwo.autosdk.api.KWAPI;

public class LauncherApplication extends Application implements ViewModelStoreOwner {
    private static final String TAG = LauncherApplication.class.getSimpleName();
    private LauncherModel mModel;
    public IconCache mIconCache;
    ModelImpl model = new ModelImpl();
    private static boolean sIsScreenLarge;
    private static float sScreenDensity;
    private static int sLongPressTimeout = 300;
    private static final String sSharedPreferencesKey = "com.awell.launcher2.prefs";
    WeakReference<LauncherProvider> mLauncherProvider;

    private final ViewModelStore store = new ViewModelStore();
    private PluginManager mPluginManager;


    public static Context mAppContext;

    //public  KWAPI kwapi;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: huang application create this==>" + this);
        mAppContext = this;

        // set sIsScreenXLarge and sScreenDensity *before* creating icon cache
        sIsScreenLarge = getResources().getBoolean(R.bool.is_large_screen);
        sScreenDensity = getResources().getDisplayMetrics().density;

        initLauncherModel();

        if (isProcess(this, ":plugin")) {
            Log.i(TAG, "onCreate: huang plugin ==>");
        }

        initPluginModel();


    }

    private void initLauncherModel() {
        Log.i(TAG, "initLauncherModel: huang init model only one ==>");
        mIconCache = new IconCache(this);
        mModel = new LauncherModel(this, mIconCache);
        mModel.initialize(model);
        // Register intent receivers
        registerBroadcastLauncherModel();
        // Register for changes to the favorites
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);
        //kwapi = KWAPI.createKWAPI(this, "auto");
//        mModel.startLoader(true, -1);
        AppsCustomizeControl.INSTANCE.initialize(this, mModel, mIconCache);
    }

    private void initPluginModel() {
        detectNonSdkApiUsageOnAndroidP();
        setWebViewDataDirectorySuffix();
        LoggerFactory.setILoggerFactory(new AndroidLogLoggerFactory());

        if (isProcess(this, ":plugin")) {
            //在全动态架构中，Activity组件没有打包在宿主而是位于被动态加载的runtime，
            //为了防止插件crash后，系统自动恢复crash前的Activity组件，此时由于没有加载runtime而发生classNotFound异常，导致二次crash
            //因此这里恢复加载上一次的runtime
            DynamicRuntime.recoveryRuntime(this);
        }

        if (isProcess(this, getPackageName())) {
            PluginHelper.getInstance().init(this);
        }

        HostUiLayerProvider.init(this);

        HostAppsHolder.init(new HostApps());

    }

    private static void detectNonSdkApiUsageOnAndroidP() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        builder.detectNonSdkApiUsage();
        StrictMode.setVmPolicy(builder.build());
    }

    private static void setWebViewDataDirectorySuffix() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        WebView.setDataDirectorySuffix(Application.getProcessName());
    }

    public void loadPluginManager(File apk) {
        if (mPluginManager == null) {
            mPluginManager = Shadow.getPluginManager(apk);
        }
    }

    public PluginManager getPluginManager() {
        return mPluginManager;
    }

    private static boolean isProcess(Context context, String processName) {
        String currentProcName = "";
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == myPid()) {
                currentProcName = processInfo.processName;
                break;
            }
        }

        return currentProcName.endsWith(processName);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerBroadcastLauncherModel() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        registerReceiver(mModel, filter, RECEIVER_EXPORTED);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        registerReceiver(mModel, filter);
    }

    public static Context getmAppContext() {
        return mAppContext;
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        store.clear();
        unregisterReceiver(mModel);
        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);
        mModel.stopLoader();
        mAppContext = null;
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            // If the database has ever changed, then we really need to force a reload of the
            // workspace on the next load
            mModel.resetLoadedState(false, true);
            mModel.startLoaderFromBackground();
        }
    };

    LauncherModel setLauncher(Launcher launcher) {
        mModel.initialize(launcher);
        return mModel;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    void setLauncherProvider(LauncherProvider provider) {
        mLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    LauncherProvider getLauncherProvider() {
        return mLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return sSharedPreferencesKey;
    }

    public static boolean isScreenLarge() {
        return sIsScreenLarge;
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
    }

    public static float getScreenDensity() {
        return sScreenDensity;
    }

    public static int getLongPressTimeout() {
        return sLongPressTimeout;
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return store;
    }
}
