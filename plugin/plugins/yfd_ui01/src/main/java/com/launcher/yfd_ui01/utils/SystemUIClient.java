package com.launcher.yfd_ui01.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.systemui.awell.services.ISystemUIService;
import com.android.systemui.awell.services.ISystemUIServiceCallback;
import com.launcher.yfd_ui01.R;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class SystemUIClient {
    private ISystemUIService mService;

    private static final String TAG = "SystemUIClient";
    private boolean mBound = false;
    private boolean mServiceReady = false;
    private String free_packName;
    private String free_className;

    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    public static final int WINDOWING_MODE_FREEFORM = 5;
    public static final int HIDE_FREEFORM = 0x10;
    public static final int OPEN_APP_TO_FREEFORM = 0x11;

    //   private final String free_packName = "com.autonavi.amapauto";
    //   private final String FREE_CLAZZ = "com.autonavi.amapauto.MainMapActivity";
    //private final String free_packName = "com.google.android.apps.maps";
    //private final String free_className = "com.google.android.maps.MapsActivity";
//    private final String  free_packName = "com.baidu.naviauto";
//    private final String  free_className = "com.baidu.baidumaps.MapsActivity";

    // AIDL回调实现
    private final ISystemUIServiceCallback mSystemUIServiceCallback = new ISystemUIServiceCallback.Stub() {
        @Override
        public void onConnected(int resultCode) throws RemoteException {
            Log.i(TAG, "lqq, onServiceCallback: connected with resultCode=" + resultCode);
            mServiceReady = (resultCode == 1);// 1 indicates success
        }

        @Override
        public void onDisconnected() throws RemoteException {
            Log.i(TAG, "lqq, onServiceCallback: disconnected");
            mServiceReady = false;
        }

    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mBound = true;
            free_packName = SystemProperties.get("persist.sys.lz.default_freeform_pkg","com.google.android.apps.maps");
            free_className = SystemProperties.get("persist.sys.lz.default_freeform_class", "com.google.android.maps.MapsActivity");
            // 注册回调
            registerCallback();
            Log.i(TAG, "lqq, onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "lqq, onServiceDisconnected");
            mServiceReady = false;
            mService = null;
            mBound = false;
        }
    };

    // 注册回调到服务
    private void registerCallback() {
        if (mBound && mService != null) {
            try {
                mService.registerCallback(mSystemUIServiceCallback);
                Log.i(TAG, "lqq, Callback registered successfully");
            } catch (RemoteException e) {
                Log.e(TAG, "lqq, Failed to register callback: " + e.getMessage());
            }
        }
    }

    // 取消注册回调
    private void unregisterCallback() {
        if (mBound && mService != null) {
            try {
                mService.unregisterCallback(mSystemUIServiceCallback);
                Log.i(TAG, "lqq, Callback unregistered");
            } catch (RemoteException e) {
                Log.e(TAG, "lqq, Failed to unregister callback: " + e.getMessage());
            }
        }
    }

    public void bindToSystemUIService(Context context) {
        if (mBound) {
            Log.i(TAG, "lqq, Service already bound");
            return;
        }
        
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.systemui",
                "com.android.systemui.awell.services.AwellSystemUIService"
        ));
        boolean result = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "lqq, bindToSystemUIService: huang bind systemUI result==>" + result);
    }

    public void unbindService(Context context) {
        if (mBound) {
            // 先取消注册回调
            unregisterCallback();
            
            context.unbindService(mConnection);
            mBound = false;
            mServiceReady = false;
            Log.i(TAG, "lqq, Service unbound");
        }
    }

    // 检查服务是否就绪
    public boolean isServiceReady() {
        return mBound && mServiceReady;
    }

    // 等待服务就绪的方法（带超时）
    public void waitForServiceReady(long timeoutMs, final ServiceReadyCallback callback) {
        if (isServiceReady()) {
            callback.onReady(true);
            return;
        }
        
        final long startTime = System.currentTimeMillis();
        final android.os.Handler handler = new android.os.Handler();
        
        Runnable checkRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceReady()) {
                    callback.onReady(true);
                } else if (System.currentTimeMillis() - startTime > timeoutMs) {
                    callback.onReady(false);
                } else {
                    handler.postDelayed(this, 100); // 每100ms检查一次
                }
            }
        };
        
        handler.postDelayed(checkRunnable, 100);
    }

    public interface ServiceReadyCallback {
        void onReady(boolean ready);
    }

    public void setFreeformType(int type) {
        if (!isServiceReady()) {
            Log.e(TAG, "lqq, setFreeformType: Service not ready");
            return;
        }
        
        try {
            mService.setFreeformType(type);
            Log.i(TAG, "lqq, setFreeformType called with type: " + type);
        } catch (Exception e) {
            Log.e(TAG, "lqq, setFreeformType: error==>" + e.getMessage());
        }
    }

    public void startOrSetFreeformType(Intent intent, ActivityOptions options, int windowType) {
        if (!isServiceReady()) {
            Log.e(TAG, "lqq, startOrSetFreeformType: Service not ready");
            return;
        }
        
        try {
            Bundle bundle = options.toBundle();
            mService.startOrSetFreeformType(intent, bundle, windowType);
            Log.i(TAG, "lqq, startOrSetFreeformType called with windowType: " + windowType);
        } catch (Exception e) {
            Log.e(TAG, "lqq, startFreeform: error==>" + e.getMessage());
        }
    }

    public void startOrSetFreeformType(Context context, int windowType) {
        // 先检查服务是否就绪
        if (!isServiceReady()) {
            Log.e(TAG, "lqq, startOrSetFreeformType: Service not ready, binding first...");
            bindToSystemUIService(context);
            
            // 等待服务就绪
            waitForServiceReady(3000, new ServiceReadyCallback() {
                @Override
                public void onReady(boolean ready) {
                    if (ready) {
                        // 服务就绪后执行操作
                        executeStartOrSetFreeformType(context, windowType);
                    } else {
                        Log.e(TAG, "lqq, Service not ready within timeout");
                    }
                }
            });
        } else {
            executeStartOrSetFreeformType(context, windowType);
        }
    }

    private void executeStartOrSetFreeformType(Context context, int windowType) {
        String pkg = Settings.System.getString(context.getContentResolver(), "freeform_app_package_name");
        String clazz = Settings.System.getString(context.getContentResolver(), "freeform_app_clazz_name");
        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(clazz) ) {
            pkg = free_packName;
            clazz = free_className;

        }
        if(!isAppExist(context,pkg)){
            pkg = free_packName;
            clazz = free_className;
            Settings.System.putString(context.getContentResolver(), "freeform_app_package_name",free_packName);
            Settings.System.putString(context.getContentResolver(), "freeform_app_clazz_name",free_className);
        }
        Log.i(TAG, "lqq, startOrSetFreeformType, pkg= " + pkg + ", clazz= " + clazz);
        if (!isAppRunning(context, pkg)) {
            startShowFreeform(context, "app not running", OPEN_APP_TO_FREEFORM, pkg, clazz);
        } else {
            startShowFreeform(context, "app is running", windowType, pkg, clazz);
        }

    }


    private boolean isAppRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo info : list) {
            assert info.topActivity != null;
            if (info.topActivity.getPackageName().equals(packageName) ||
                    Objects.requireNonNull(info.baseActivity).getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAppExist(Context context,String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

    }

    private void startShowFreeform(Context context, String reason, int windowType, String pkg, String clazz) {
        Log.i(TAG, "lqq, startShowFreeform: reason=>" + reason);
        ActivityOptions options = makeLaunchOptions(context);
        Intent intentFreeform = new Intent();
        intentFreeform.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intentFreeform.addCategory(Intent.CATEGORY_LAUNCHER);
        intentFreeform.setPackage(pkg);

        if (clazz != null) {
            ComponentName cmp = new ComponentName(pkg, clazz);
            intentFreeform.setComponent(cmp);
        }

        startOrSetFreeformType(intentFreeform, options, windowType);
    }

    private ActivityOptions makeLaunchOptions(Context context) {
        ActivityOptions activityOptions = ActivityOptions.makeBasic();

//        int freeformWidth = 525;
//        int freeformHeight = 352;
//        //居中显示
//        int left = 384;
//        int top = 60;
        try {
            Method method = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
            method.invoke(activityOptions, WINDOWING_MODE_FREEFORM);
        } catch (Exception e) {
            Log.e(TAG, "startFreeFormActivity: error==> " + e.getMessage());
        }
        activityOptions.setLaunchBounds(/*new Rect(left, top, freeformWidth + left, freeformHeight + top)*/ getNav(context));
        return activityOptions;
    }

    private Rect getNav(Context context) {

//        int freeformWidth = 515;
//        int freeformHeight = 352;
//        //居中显示
        int left = 394 -20;
//        int top = 60;
        Rect rect = new Rect(left, 60, left + 525, 60 + 352);
        // 0=Hide, 1=Left, 2=Right, 3=Bottom(default)
        final int position = android.os.SystemProperties.getInt("persist.sys.awell.navbar.position", 0);

        if (position == 0)
            return rect;
        int height = 0;
        if (position == 1 || position == 2) {
            height = context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_width);
        } else if (position == 3) {
            height = context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_height);
        }
        Log.i(TAG, "lqq, position=" + position + ", heihgt=" + height);
        height = (int) (height / 2);
        if (position == 1) {
            rect.left += height-10;
            rect.right += height-10;
        } else if (position == 2) {
            rect.left -= height-10;
            rect.right -= height-10;
        } else if (position == 3) {
            rect.bottom -= height;
        }
        return rect;
    }

}