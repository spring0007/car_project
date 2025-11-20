package com.launcher.yfd_ui01.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.systemui.awell.services.ISystemUIService;
import com.launcher.yfd_ui01.R;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class SystemUIClient {
    private ISystemUIService mService;

    private static final String TAG = SystemUIClient.class.getSimpleName();
    private boolean mBound = false;

    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    public static final int WINDOWING_MODE_FREEFORM = 5;
    private static final int HIDE_FREEFORM = 0x10;
    private static final int OPEN_APP_TO_FREEFORM = 0x11;

    //   private final String free_packName = "com.autonavi.amapauto";
    //   private final String FREE_CLAZZ = "com.autonavi.amapauto.MainMapActivity";
    private final String free_packName = "com.google.android.apps.maps";
    private final String free_className = "com.google.android.maps.MapsActivity";

//    private final String  free_packName = "com.baidu.naviauto";
//    private final String  free_className = "com.baidu.baidumaps.MapsActivity";

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
                Log.i(TAG,"lqq,startOrSetFreeformType111");
            } catch (Exception e) {
                Log.e(TAG, "startFreeform: error==>" + e.getMessage());
            }
        }
    }

    public void startOrSetFreeformType(Context context, int windowType) {
        //Settings.System.putString(getContentResolver(), "freeform_app_package_name", free_packName);
        String pkg = Settings.System.getString(context.getContentResolver(), "freeform_app_package_name");
        String clazz = Settings.System.getString(context.getContentResolver(), "freeform_app_clazz_name");

        //String clazz =getLaunchActivityFromPakInfo(context,pkg);
        Log.i(TAG,"lqq,startOrSetFreeformType,pkg= "+pkg +", clazz= "+clazz);
        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(clazz)) {
            pkg = free_packName;
            clazz = free_className;
        }

        if (!isAppRunning(context, pkg)) {
            startShowFreeform(context, "app not running", OPEN_APP_TO_FREEFORM ,pkg,clazz);
        } else {
            startShowFreeform(context, "app not is running", windowType,pkg,clazz);
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
        Log.i(TAG, "lqq,startShowFreeform: reason=>" + reason);
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
        int freeformWidth = 525;
        int freeformHeight = 352;
        //居中显示
        int left = 384;
        int top = 60;
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
//        int left = 394;
//        int top = 60;
        Rect rect = new Rect(394, 60, 394 + 525, 60 + 352);
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
        Log.i(TAG, "position=" + position + ",heihgt=" + height);
        height = (int) (height/2);
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

    private String getLaunchActivityFromPakInfo(Context context,String packageName) {

        if(TextUtils.isEmpty(packageName))
            return null;

        try {
            PackageManager pm = context.getPackageManager();

            // 方法1: 通过Intent查询LAUNCHER Activity
            Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launcherIntent.setPackage(packageName);

            List<ResolveInfo> resolveList = pm.queryIntentActivities(launcherIntent, 0);

//            if (resolveList.isEmpty()) {
//                // 方法2: 通过PackageInfo查询
//                return getLaunchActivityFromPackageInfo(pm, packageName);
//            }

            // 处理多个启动Activity的情况
            if (resolveList.size() == 1) {
                return resolveList.get(0).activityInfo.name;
            } else {
                //return selectPrimaryActivity(pm, resolveList);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}