package com.launcher.yfd_ui01.utils;


import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.systemui.awell.services.ISystemUIService;
import com.launcher.yfd_ui01.R;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class SystemUIClient {
    private static final String TAG = "SystemUIClient";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String SYSTEM_UI_SERVICE_CLASS = "com.android.systemui.awell.services.AwellSystemUIService";
//    public static final String SETTINGS_FREEFORM_APP_PACKAGE_NAME = "freeform_app_package_name";
//    public static final String SETTINGS_FREEFORM_APP_CLAZZ_NAME = "freeform_app_clazz_name";
//
//    private final String free_packName = "com.google.android.apps.maps";
//    private final String free_className = "com.google.android.maps.MapsActivity";
//
//    private final String  GAODE_PKG = "com.autonavi.amapauto";
//    private final String  GAODE_CLAZZ = "com.autonavi.amapauto.MainMapActivity";
//    private final String  MUSIC_KUWO_PKG1 = "cn.kuwo.player";
//    private final String  MUSIC_KUWO_CLAZZ1 = "cn.kuwo.player.activities.MainActivity";


    // Window mode constants
    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    public static final int WINDOWING_MODE_FREEFORM = 5;
    public static final int HIDE_FREEFORM = 0x10;//16
    public static final int OPEN_APP_TO_FREEFORM = 0x11;//17
    public static final int CLOSE_FREEFORM = 0x12;//18
//    private long lastUpdateTime = 0;
//    private static final int UPDATE_INTERVAL = 1000; // 1000ms
    private int lastFreeFormType = 0;

    private ISystemUIService mService;
    private boolean mIsBound = false;
   // private Rect mRect = null;
    private int mFreeformMode = 0;


    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mIsBound = true;
            LogUtil.i( "Service connected successfully");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
            lastFreeFormType = 0;
            LogUtil.i( "Service disconnected");

        }
    };

    public SystemUIClient(Context context) {
    }
	
    /**
     * Bind to SystemUI service
     */
    public void bindToSystemUIService(Context context) {
        if (context == null) {
            LogUtil.e( "Context is null, cannot bind service");
            return;
        }
        
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(SYSTEM_UI_PACKAGE, SYSTEM_UI_SERVICE_CLASS));
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        } catch (Exception e) {
            LogUtil.e( "Failed to connect: " + e.getMessage());
        }
    }

    /**
     * Unbind from SystemUI service
     */
    public void unbindService(Context context) {
        if (mIsBound && context != null) {
        	try {
            	context.unbindService(mConnection);
            } catch (Exception e) {
                LogUtil.w( "Error during disconnect: " + e.getMessage());
            }
        }
        mIsBound = false;
        mService = null;
    }

    public void fullScreenFreeform() {
        if (isServiceConnected()) {
            try {
                LogUtil.e( "fullScreenFreeform" );
                mFreeformMode = WINDOWING_MODE_FULLSCREEN;
                mService.fullScreenFreeform();
            } catch (Exception e) {
                LogUtil.e( "fullScreenFreeform: error==>" + e.getMessage());
            }
        }
    }

    public void hideFreeform() {
        if (isServiceConnected()) {
            try {
                LogUtil.e( "hideFreeform" );
                mFreeformMode = HIDE_FREEFORM;
                mService.hideFreeform();
            } catch (Exception e) {
                LogUtil.e( "hideFreeform: error==>" + e.getMessage());
            }
        }
    }

//    private String top_Activity =null;
    /**
     * Start or set freeform window with calculated bounds
     */
//    public void startOrSetFreeformType(Context context) {
//
//        String pkg = Settings.System.getString(context.getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME);
//        String clazz = Settings.System.getString(context.getContentResolver(), SETTINGS_FREEFORM_APP_CLAZZ_NAME);
//
//        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(clazz)) {
//            pkg = free_packName;
//            clazz = free_className;
//            try {
//                Settings.System.putString(context.getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME, pkg);
//                Settings.System.putString(context.getContentResolver(), SETTINGS_FREEFORM_APP_CLAZZ_NAME, clazz);
//            } catch (Exception e) {
//                LogUtil.w( "Failed to write default freeform settings: " + e.getMessage());
//            }
//        }
//
//        if (!isAppInstalled(context, pkg)) {
//            pkg = free_packName;
//            clazz = free_className;
//            try {
//                Settings.System.putString(context.getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME, pkg);
//                Settings.System.putString(context.getContentResolver(), SETTINGS_FREEFORM_APP_CLAZZ_NAME, clazz);
//            } catch (Exception e) {
//                LogUtil.w( "Failed to write fallback freeform settings: " + e.getMessage());
//            }
//        }
//
//        if (isAppRunning(context, pkg)) {
//            startShowFreeform(context,  true,"app is running", OPEN_APP_TO_FREEFORM, pkg, clazz);
//        } else {
//            startShowFreeform(context, false,"app not running", OPEN_APP_TO_FREEFORM, pkg, clazz);
//        }
//    }

//    private void startShowFreeform(Context context, boolean isRun, String reason, int windowType, String pkg, String clazz) {
//        LogUtil.i( "startOrSetFreeformType: huang pkg= " + pkg + ", clazz= " + clazz + " type=>" + windowType +",isRun="+isRun+",reason=>"+reason);
//        ActivityOptions options = makeLaunchOptions(context);
//
//        //Intent intentFreeform = new Intent();
//        Intent intentFreeform = context.getPackageManager().getLaunchIntentForPackage(pkg);
//        if (intentFreeform == null) {
//            LogUtil.i( "startShowFreeform: hintentFreeformn= null");
//            return;
//        }
//        LogUtil.i("isRun="+isRun+",clazz="+clazz+",top_Activity="+top_Activity);
//        if(isRun && (!clazz.equals(top_Activity))){
//            clazz = top_Activity;
//
//        }
//
//        if(pkg.equals(GAODE_PKG)) {
//            clazz = GAODE_CLAZZ;
//        }
//
//        if(pkg.equals(MUSIC_KUWO_PKG1)) {
//            clazz = MUSIC_KUWO_CLAZZ1;
//        }
//
//        intentFreeform.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
//                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
//                    Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
//                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
//                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//
//
//           // intentFreeform.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//           // intentFreeform.addCategory(Intent.CATEGORY_LAUNCHER);
//
//        intentFreeform.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//        intentFreeform.setPackage(pkg);
//
//        if (clazz != null) {
//            ComponentName cmp = new ComponentName(pkg, clazz);
//            intentFreeform.setComponent(cmp);
//        }
//
//
//        if (!isServiceConnected()) {
//            LogUtil.w( "Service not connected, cannot start freeform2");
//            return;
//        }
//        /*final long currentTime = System.currentTimeMillis();
//        if ((currentTime - lastUpdateTime < UPDATE_INTERVAL) && (lastFreeFormType == windowType)) {
//            LogUtil.w( "The time interval is too short, cannot start freeform3");
//            return;
//        }
//        lastFreeFormType = windowType;
//        lastUpdateTime = currentTime;*/
//
//        try {
//            //Bundle bundle = options.toBundle();
//            mFreeformMode = windowType;
//            LogUtil.i( "startOrSetFreeformType: huang  , type=>" + windowType);
//            mService.startOrSetFreeformTypeWithOptions(intentFreeform, options.toBundle(), windowType);
//        } catch (Exception e) {
//            LogUtil.e( "startFreeform: error==>" + e.getMessage());
//        }
//
//      //  startOrSetFreeformType(intentFreeform, options, windowType);
//
//    }


//    private ActivityOptions makeLaunchOptions(Context context) {
//
//        //ActivityOptions activityOptions = ActivityOptions.makeScaleUpAnimation(view,0,0,view.getWidth(),view.getHeight());
//        ActivityOptions activityOptions = ActivityOptions.makeBasic();
//        /*ActivityOptions.makeCustomAnimation(
//                context,
//                R.anim.slide_from_bottom_right,  // 自定义右下角进入动画
//                R.anim.no_animation  // 退出动画
//        ); */ //
//
//
//        try {
//            Method method = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
//            method.invoke(activityOptions, WINDOWING_MODE_FREEFORM);
//        } catch (Exception e) {
//            LogUtil.e( "startFreeFormActivity: error==> " + e.getMessage());
//        }
//        try {
//            Method setTaskAlwaysOnTopMethod = ActivityOptions.class.getMethod(
//                    "setTaskAlwaysOnTop",
//                    boolean.class
//            );
//            setTaskAlwaysOnTopMethod.invoke(activityOptions, true);
//            LogUtil.d( "setTaskAlwaysOnTop: true");
//        } catch (Exception e) {
//            LogUtil.e( "startFreeFormActivity: setTaskAlwaysOnTop error==> " + e.getMessage());
//        }
//        activityOptions.setLaunchBounds(getRect());
//        return activityOptions;
//    }
//
//
//    /**
//     * 检查应用是否安装
//     *
//     * @param context     上下文
//     * @param packageName 包名
//     * @return true表示已安装，false表示未安装
//     */
//    public static boolean isAppInstalled(Context context, String packageName) {
//        try {
//            PackageManager pm = context.getPackageManager();
//            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
//            return true;
//        } catch (PackageManager.NameNotFoundException e) {
//            return false;
//        }
//    }
//
//    private boolean isAppRunning(Context context, String packageName) {
//        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
//        boolean appRun = false;
//        top_Activity = null;
//        for (ActivityManager.RunningTaskInfo info : list) {
//            if (info.topActivity != null) {
//                try {
//                    if (info.topActivity.getPackageName().equals(packageName) ||
//                            Objects.requireNonNull(info.baseActivity).getPackageName().equals(packageName))  {
//                        top_Activity= info.topActivity.getClassName();
//                        appRun = true;
//                        break;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return appRun;
//    }

//    public void setRect(Rect rect) {
//        this.mRect = rect;
//    }
//
//    public Rect getRect() {
//        LogUtil.i("mRect="+mRect);
//        return mRect;
//    }

    /**
     * Check if service is connected and ready
     */
    private boolean isServiceConnected() {
        return mIsBound && mService != null;
    }


    public int getmFreeformMode() {
        return mFreeformMode;
    }

    public void setmFreeformMode(int mFreeformMode) {
        this.mFreeformMode = mFreeformMode;
    }



}