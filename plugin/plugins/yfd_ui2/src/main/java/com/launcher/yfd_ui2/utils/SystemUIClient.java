package com.launcher.yfd_ui2.utils;

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
import com.launcher.yfd_ui2.R;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class SystemUIClient {
    private ISystemUIService mService;

    private static final String TAG = SystemUIClient.class.getSimpleName();
    private boolean mBound = false;
    private Context mContext;

    public static final String SETTINGS_FREEFORM_APP_PACKAGE_NAME = "freeform_app_package_name";
    public static final String SETTINGS_FREEFORM_APP_CLAZZ_NAME = "freeform_app_clazz_name";
    public static final String SETTINGS_UI_HAS_FREEFORM = "ui_has_freeform";

    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    public static final int WINDOWING_MODE_FREEFORM = 5;
    public static final int HIDE_FREEFORM = 0x10;
    public static final int OPEN_APP_TO_FREEFORM = 0x11;

    public static final String NAVI_GOOGLE_PKG = "com.google.android.apps.maps";
    public static final String NAVI_GOOGLE_CLAZZ = "com.google.android.maps.MapsActivity";

    public static final String NAVI_GAODE_PKG = "com.google.android.apps.maps";

    public static final String MUSIC_PKG = "com.awell.localmusic";
    public static final String NAVI_GAODE_CLAZZ = "com.google.android.maps.MapsActivity";



    private Rect mRect = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mBound = true;
            if (startFreeformI != null) {
                startFreeformI.startFreeform();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    public SystemUIClient(Context context) {
        mContext = context;
    }

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
                Rect rect= (Rect) bundle.get("android:activity.launchBounds");
                mService.startOrSetFreeformType(intent, bundle, windowType);
            } catch (Exception e) {
                Log.e(TAG, "startFreeform: error==>" + e.getMessage());
            }
        }
    }

    public void startOrSetFreeformType(Context context, int windowType) {
        String pkg = Settings.System.getString(context.getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME);
        String clazz = Settings.System.getString(context.getContentResolver(), SETTINGS_FREEFORM_APP_CLAZZ_NAME);

        Log.i(TAG, "startOrSetFreeformType: huang pkg= " + pkg + ", clazz= " + clazz + " type=>" + windowType);

        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(clazz)) {
            pkg = NAVI_GOOGLE_PKG;
            clazz = NAVI_GOOGLE_CLAZZ;
        }

        if (!isAppInstalled(context, pkg)) {
            pkg = "com.autonavi.amapauto";
            clazz = "com.autonavi.amapauto.MainMapActivity";
        }


        if (!isAppRunning(context, pkg)) {
            startShowFreeform(context, "app not running", OPEN_APP_TO_FREEFORM, pkg, clazz);
        } else {
            startShowFreeform(context, "app is running", windowType, pkg, clazz);
        }

    }

    public String getFreeformPkg(Context context) {
        return Settings.System.getString(context.getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME);
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
        Log.i(TAG, "startShowFreeform: huang reason=>" + reason);
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
        try {
            Method method = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
            method.invoke(activityOptions, WINDOWING_MODE_FREEFORM);
        } catch (Exception e) {
            Log.e(TAG, "startFreeFormActivity: error==> " + e.getMessage());
        }
        activityOptions.setLaunchBounds(getNav(context));
        return activityOptions;
    }

    private Rect getNav(Context context) {
        if (mRect != null) {
            return mRect;
        }

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
        height = (int) (height / 2);
        if (position == 1) {
            rect.left += height - 10;
            rect.right += height - 10;
        } else if (position == 2) {
            rect.left -= height - 10;
            rect.right -= height - 10;
        } else if (position == 3) {
            rect.bottom -= height;
        }
        return rect;
    }

    private String getLaunchActivityFromPakInfo(Context context, String packageName) {

        if (TextUtils.isEmpty(packageName))
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

    /**
     * 检查应用是否安装
     * @param context 上下文
     * @param packageName 包名
     * @return true表示已安装，false表示未安装
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查应用是否安装并可启动
     * @param context 上下文
     * @param packageName 包名
     * @return true表示可启动，false表示不可启动
     */
    public static boolean isAppLaunchable(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        return launchIntent != null;
    }


    public void setRect(Rect rect) {
        this.mRect = rect;
    }

    public Rect getRect() {
        return mRect;
    }

    private SystemStartFreeform startFreeformI;

    public void setStartFreeformI(SystemStartFreeform startFreeformI) {
        this.startFreeformI = startFreeformI;
    }

    public interface SystemStartFreeform {
        public void startFreeform();
    }

}