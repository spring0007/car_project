package com.launcher.yfd_ui01.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.systemui.awell.services.ISystemUIService;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class SystemUIClient {
    private ISystemUIService mService;

    private static final String TAG = SystemUIClient.class.getSimpleName();
    private boolean mBound = false;

    private final String free_packName = "com.autonavi.amapauto";
    private final String free_className = "com.autonavi.amapauto.MainMapActivity";

    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    public static final int WINDOWING_MODE_FREEFORM = 5;
    private static final int HIDE_FREEFORM = 0x10;
    private static final int OPEN_APP_TO_FREEFORM = 0x11;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    public void bindToSystemUIService(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.systemui",
                "com.android.systemui.awell.services.AwellSystemUIService"
        ));
        boolean result = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "bindToSystemUIService: huang bind systemUI result==>" + result);

    }

    public void unbindService(Context context) {
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
    }

    public void setFreeformType(int type) {
        if (mBound && mService != null) {
            try {
                mService.setFreeformType(type);
            } catch (Exception e) {
                Log.e(TAG, "setFreeformType: error==>" + e.getMessage());
            }
        }
    }

    public void startOrSetFreeformType(Intent intent, ActivityOptions options, int windowType) {
        if (mBound && mService != null) {
            try {
                Bundle bundle = options.toBundle();
                mService.startOrSetFreeformType(intent, bundle, windowType);
            } catch (Exception e) {
                Log.e(TAG, "startFreeform: error==>" + e.getMessage());
            }
        }
    }

    public void startOrSetFreeformType(Context context, String pkg, String clazz, int windowType) {
        if (!isAppRunning(context, pkg)) {
            startShowFreeform(context, "app not running", OPEN_APP_TO_FREEFORM, pkg, clazz);
        } else {
            startShowFreeform(context, "app not is running", windowType, pkg, clazz);
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

    private void startShowFreeform(Context context, String reason, int windowType, String pkg, String clazz) {
        Log.i(TAG, "startShowFreeform: reason=>" + reason);
        ActivityOptions options = makeLaunchOptions();
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

    private ActivityOptions makeLaunchOptions() {
        ActivityOptions activityOptions = ActivityOptions.makeBasic();
        int freeformWidth = 515;
        int freeformHeight = 352;
        //居中显示
        int left = 394;
        int top = 60;
        try {
            Method method = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
            method.invoke(activityOptions, WINDOWING_MODE_FREEFORM);
        } catch (Exception e) {
            Log.e(TAG, "startFreeFormActivity: error==> " + e.getMessage());
        }
        activityOptions.setLaunchBounds(new Rect(left, top, freeformWidth + left, freeformHeight + top));
        return activityOptions;
    }
}