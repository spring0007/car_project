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
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.awell.impl.ModelImpl;
import com.awell.launcher.library.BuildConfig;
import com.awell.launcher.library.R;
import com.awell.control.AppsCustomizeControl;
import com.awell.utils.LogUtil;
import com.qihoo360.replugin.RePluginApplication;
import com.qihoo360.replugin.RePluginCallbacks;
import com.qihoo360.replugin.RePluginConfig;
import com.qihoo360.replugin.RePluginEventCallbacks;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

//import cn.kuwo.autosdk.api.KWAPI;

public class LauncherApplication extends RePluginApplication implements ViewModelStoreOwner {
    private static final String TAG = LauncherApplication.class.getSimpleName();
    private LauncherModel mModel;
    public IconCache mIconCache;
    ModelImpl model = new ModelImpl();
    private static boolean sIsScreenLarge;
    private static float sScreenDensity;
    private static final int sLongPressTimeout = 300;
    private static final String sSharedPreferencesKey = "com.awell.launcher2.prefs";
    private WeakReference<LauncherProvider> mLauncherProvider;

    private final ViewModelStore store = new ViewModelStore();

    public static Context mAppContext;

    //public  KWAPI kwapi;
    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = this;

        // set sIsScreenXLarge and sScreenDensity *before* creating icon cache
        sIsScreenLarge = getResources().getBoolean(R.bool.is_large_screen);
        sScreenDensity = getResources().getDisplayMetrics().density;
//        int debug = 0;
//        try {
//            debug = Integer.parseInt(SystemProperties.get("persist.sys.awell.logswitch", "1"));
//        } catch (NumberFormatException e) {
//            debug = 1;
//        }
//        if (debug == 1) {
//            D = true;
//        }

        if (isProcess(this, ":GuardService")) {
            LogUtil.i("onCreate: huang plugin ==>");
        }

        initHostModule();
    }

    public void hostInit() {
        //HostAppsHolder.init(new HostAppsImpl());
        initLauncherModel();
    }

    public void initLauncherModel() {
        if (mModel != null) {
            return;
        }
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
        mModel.prewarmIconCache();
        mModel.startLoader(true, -1);
        AppsCustomizeControl.INSTANCE.initialize(this, mModel, mIconCache);
    }

    private void initHostModule() {
         LogUtil.i("initHostModule: huang getPackageName()=>" + getPackageName());
        if (isProcess(this, getPackageName())) {
//            if (getPackageName().equals(BuildConfig.APPLICATION_ID)) {
            //运行在宿主进程
            startHostService();
            hostInit();
            LogUtil.i("Host onCreate: huang application create mAppContext==>" + mAppContext);
//            }
        }

    }

    private void startHostService() {
        Intent service = new Intent();
        ComponentName componentName = new ComponentName(getPackageName(), "com.awell.service.HostToPluginService");
        service.setComponent(componentName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 使用系统用户标识
            UserHandle userHandle = android.os.Process.myUserHandle();
            try {
                // 反射调用 startServiceAsUser（系统 API）
                Method method = Context.class.getMethod(
                        "startServiceAsUser",
                        Intent.class,
                        UserHandle.class
                );
                method.invoke(this, service, userHandle);
                LogUtil.i("startHostService: huang start service=>" + service);
            } catch (Exception e) {
                e.printStackTrace();
                // 降级方案
                startService(service);
            }
        } else {
            startService(service);
        }
    }

    private static boolean isProcess(Context context, String processName) {
        String currentProcName = "";
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == myPid()) {
                currentProcName = processInfo.processName;
                LogUtil.i("isProcess: huang currentProcName=>" + currentProcName);
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
        if (mLauncherProvider == null) {
            LogUtil.e("getLauncherProvider: mLauncherProvider is null");
            return null;
        }
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


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // FIXME 允许接收rpRunPlugin等Gradle Task，发布时请务必关掉，以免出现问题
        //RePlugin.enableDebugger(base, BuildConfig.DEBUG);
    }

    // ----------
    // 自定义行为
    // ----------

    /**
     * RePlugin允许提供各种“自定义”的行为，让您“无需修改源代码”，即可实现相应的功能
     */
    @Override
    protected RePluginConfig createConfig() {
        RePluginConfig c = new RePluginConfig();

        // 允许“插件使用宿主类”。默认为“关闭”
        c.setUseHostClassIfNotFound(true);

        // FIXME RePlugin默认会对安装的外置插件进行签名校验，这里先关掉，避免调试时出现签名错误
        //c.setVerifySign(!BuildConfig.DEBUG);
        c.setVerifySign(false);

        // 针对“安装失败”等情况来做进一步的事件处理
        c.setEventCallbacks(new HostEventCallbacks(this));


        // FIXME 若宿主为Release，则此处应加上您认为"合法"的插件的签名，例如，可以写上"宿主"自己的。
        // RePlugin.addCertSignature("AAAAAAAAA");

        // 在Art上，优化第一次loadDex的速度
        // c.setOptimizeArtLoadDex(true);
        return c;
    }

    @Override
    protected RePluginCallbacks createCallbacks() {


        return new HostCallbacks(this);
    }


    /**
     * 宿主针对RePlugin的自定义行为
     */
    private static class HostCallbacks extends RePluginCallbacks {

        private static final String TAG = "HostCallbacks";

        private HostCallbacks(Context context) {
            super(context);
        }

        @Override
        public boolean onPluginNotExistsForActivity(Context context, String plugin, Intent intent, int process) {
            // FIXME 当插件"没有安装"时触发此逻辑，可打开您的"下载对话框"并开始下载。
            // FIXME 其中"intent"需传递到"对话框"内，这样可在下载完成后，打开这个插件的Activity
            if (BuildConfig.DEBUG) {
                LogUtil.d("onPluginNotExistsForActivity: Start download... p=" + plugin + "; i=" + intent);
            }
            return super.onPluginNotExistsForActivity(context, plugin, intent, process);
        }
    }

    private class HostEventCallbacks extends RePluginEventCallbacks {

        private static final String TAG = "HostEventCallbacks";

        public HostEventCallbacks(Context context) {
            super(context);
        }

        @Override
        public void onInstallPluginFailed(String path, InstallResult code) {
            // FIXME 当插件安装失败时触发此逻辑。您可以在此处做“打点统计”，也可以针对安装失败情况做“特殊处理”
            // 大部分可以通过RePlugin.install的返回值来判断是否成功
            if (BuildConfig.DEBUG) {
                LogUtil.d("onInstallPluginFailed: Failed! path=" + path + "; r=" + code);
            }
            super.onInstallPluginFailed(path, code);
        }

        @Override
        public void onStartActivityCompleted(String plugin, String activity, boolean result) {
            // FIXME 当打开 Activity 成功时触发此逻辑，可在这里做一些 APM、打点统计等相关工作
            LogUtil.i("onStartActivityCompleted: huang plugin =" + plugin+",activity =" + activity+",result =" + result);
        
            // 避免重复回调导致无限循环
            // 只有在插件启动失败时才需要重试，成功时不需要再次触发
            if (startStatus != null && !result) {
                startStatus.startPitActivityResult(plugin, activity, result);
            }
            super.onStartActivityCompleted(plugin, activity, result);
        }

        @Override
        public void onPrepareAllocPitActivity(Intent intent) {
            LogUtil.i("onPrepareAllocPitActivity: huang intent=>" + intent);
            super.onPrepareAllocPitActivity(intent);
        }

        @Override
        public void onPrepareStartPitActivity(Context context, Intent intent, Intent pittedIntent) {
            LogUtil.i("onPrepareStartPitActivity: context=>" + context+" ,intent="+intent+", pittedIntent=>" + pittedIntent);
            // pittedIntent=>Intent { cat=[process:-2147483648,plugin:com.example.plugin_2,activity:com.example.plugin_2.UI2Activity,container:com.awell.launcher.loader.a.ActivityN1NRNTS5,counter:0]
            // cmp=com.awell.launcher/.loader.a.ActivityN1NRNTS5 (has extras) }

            super.onPrepareStartPitActivity(context, intent, pittedIntent);
        }
    }

    private PluginStartStatus startStatus;

    public void setStartStatus(PluginStartStatus startStatus) {
        this.startStatus = startStatus;
    }

    public interface PluginStartStatus {
        void startPitActivityResult(String plugin, String activity, boolean result);
    }


}
