package com.launcher.yfd_ui01.utils;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.systemui.awell.services.ISystemUIService;
import com.launcher.yfd_ui01.R;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

public class SystemUIClient {
    private static final String TAG = "SystemUIClient";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String SYSTEM_UI_SERVICE_CLASS = "com.android.systemui.awell.services.AwellSystemUIService";
	
	
	public static final String SETTINGS_FREEFORM_APP_PACKAGE_NAME = "freeform_app_package_name";
    public static final String SETTINGS_FREEFORM_APP_CLAZZ_NAME = "freeform_app_clazz_name";
	
	private final String free_packName = "com.google.android.apps.maps";
    private final String free_className = "com.google.android.maps.MapsActivity";
    
    // Window mode constants
    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    public static final int WINDOWING_MODE_FREEFORM = 5;
    public static final int HIDE_FREEFORM = 0x10;
    public static final int OPEN_APP_TO_FREEFORM = 0x11;
    
    // Freeform window dimensions
    public static final int DEFAULT_LEFT = 374; // 394 - 20
    public static final int DEFAULT_TOP = 60;
    public static final int DEFAULT_WIDTH = 529;
    public static final int DEFAULT_HEIGHT = 352;
    
    private ISystemUIService mService;
    private boolean mIsBound = false;
    private UIClientCallback mCallback;
	private Context mContext;
    private Rect mRect = null;
    

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mIsBound = true;
            Log.i(TAG, "Service connected successfully");
//            SystemProperties.set("persist.sys.lz.default_freeform_size", DEFAULT_WIDTH+","+DEFAULT_HEIGHT);
//            SystemProperties.set("persist.sys.lz.default_freeform_position", DEFAULT_LEFT+","+DEFAULT_TOP);
            if (mCallback != null) {
                mCallback.startFreeform();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
            
//            SystemProperties.set("persist.sys.lz.default_freeform_size", "0,0");
//            SystemProperties.set("persist.sys.lz.default_freeform_position", "-1,-1");
            Log.i(TAG, "Service disconnected");

        }
    };
	
	public SystemUIClient(Context context) {
        mContext = context;
    }
	
    /**
     * Bind to SystemUI service
     */
    public void bindToSystemUIService(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot bind service");
            return;
        }
        
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(SYSTEM_UI_PACKAGE, SYSTEM_UI_SERVICE_CLASS));
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect: " + e.getMessage());
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
                Log.w(TAG, "Error during disconnect: " + e.getMessage());
            }
        }
        mIsBound = false;
        mService = null;
    }

    /**
     * Set freeform window type
     */
    public void setFreeformType(int type) {
        if (!isServiceConnected()) {
            Log.w(TAG, "Service not connected, cannot set freeform type");
            return;
        }
        
        try {
            mService.setFreeformType(type);
            Log.i(TAG, "Freeform type set to: " + type);
        } catch (Exception e) {
            Log.e(TAG, "Failed to setFreeformType : " + e.getMessage());
        }
    }

    public void startOrSetFreeformType(Intent intent, ActivityOptions options, int windowType) {
        if (!isServiceConnected()) {
            Log.w(TAG, "Service not connected, cannot start freeform");
            return;
        }

        try {
            Bundle bundle = options.toBundle();
            Rect rect= (Rect) bundle.get("android:activity.launchBounds");
            mService.startOrSetFreeformTypeWithOptions(intent, bundle, windowType);
        } catch (Exception e) {
            Log.e(TAG, "startFreeform: error==>" + e.getMessage());
        }

    }
	
	

    /**
     * Start or set freeform window with calculated bounds
     */
    public void startOrSetFreeformType(Context context, int windowType) {
         String pkg = Settings.System.getString(context.getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME);
        String clazz = Settings.System.getString(context.getContentResolver(), SETTINGS_FREEFORM_APP_CLAZZ_NAME);

        Log.i(TAG, "startOrSetFreeformType: huang pkg= " + pkg + ", clazz= " + clazz + " type=>" + windowType);

        if (TextUtils.isEmpty(pkg) || TextUtils.isEmpty(clazz)) {
            pkg = free_packName;
            clazz = free_className;
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

    /**
     * Calculate freeform window bounds with navigation bar adjustment
     */
    private Rect calculateFreeformBounds(Context context) {
        Rect baseRect = new Rect(DEFAULT_LEFT, DEFAULT_TOP, 
                               DEFAULT_LEFT + DEFAULT_WIDTH, DEFAULT_TOP + DEFAULT_HEIGHT);
        return adjustBoundsForNavigationBar(context, baseRect);
    }

    /**
     * Adjust bounds based on navigation bar position
     */
    private Rect adjustBoundsForNavigationBar(Context context, Rect baseRect) {
        int position = android.os.SystemProperties.getInt("persist.sys.awell.navbar.position", 0);
        int navigationBarSize = getNavigationBarSize(context, position);

        if (position == 0 || navigationBarSize <= 0) {
            SystemProperties.set("persist.sys.lz.default_freeform_position", DEFAULT_LEFT + "," + DEFAULT_TOP);
            SystemProperties.set("persist.sys.lz.default_freeform_size", DEFAULT_WIDTH + "," + DEFAULT_HEIGHT);
            return baseRect;
        }
        
        int adjustedSize = navigationBarSize / 2;
        
        switch (position) {
            case 1: { // Left
                int left = DEFAULT_LEFT + adjustedSize ;
                SystemProperties.set("persist.sys.lz.default_freeform_position", left + "," + DEFAULT_TOP);
                SystemProperties.set("persist.sys.lz.default_freeform_size", DEFAULT_WIDTH + "," + DEFAULT_HEIGHT);
                return new Rect(
                        baseRect.left + adjustedSize,
                        baseRect.top,
                        baseRect.right + adjustedSize,
                        baseRect.bottom
                );
            }
            case 2: {// Right
                int left = DEFAULT_LEFT - adjustedSize ;
                SystemProperties.set("persist.sys.lz.default_freeform_position", left + "," + DEFAULT_TOP);
                SystemProperties.set("persist.sys.lz.default_freeform_size", DEFAULT_WIDTH + "," + DEFAULT_HEIGHT);
                return new Rect(
                        baseRect.left - adjustedSize ,
                        baseRect.top,
                        baseRect.right - adjustedSize,
                        baseRect.bottom
                );
            }
            case 3: { // Bottom
                int heights = DEFAULT_HEIGHT - navigationBarSize+10;
                SystemProperties.set("persist.sys.lz.default_freeform_position", DEFAULT_LEFT + "," + DEFAULT_TOP);
                SystemProperties.set("persist.sys.lz.default_freeform_size", DEFAULT_WIDTH + "," + heights);
                return new Rect(
                        baseRect.left,
                        baseRect.top,
                        baseRect.right,
                        baseRect.bottom - navigationBarSize
                );

            }
            default:
                SystemProperties.set("persist.sys.lz.default_freeform_position", DEFAULT_LEFT + "," + DEFAULT_TOP);
                SystemProperties.set("persist.sys.lz.default_freeform_size", DEFAULT_WIDTH + "," + DEFAULT_HEIGHT);
                return baseRect;
        }
    }

    /**
     * Get navigation bar size based on position
     */
    public static int getNavigationBarSize(Context context, int position) {
        try {
            //float density = context.getResources().getDisplayMetrics().density;
            //int densityDpi = context.getResources().getDisplayMetrics().densityDpi;
            float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;

            //Log.i(TAG,"density="+density+",densityDpi="+densityDpi +",scaledDensity="+scaledDensity);
            //注意:当导航栏在底部时，获取高度与实际高度不一致
           // if (position == 1 || position == 2) {
                return (int) (context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_width)/scaledDensity);
            //} else if (position == 3) {
            //    return context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_height);
            //}
        } catch (Exception e) {
            Log.e(TAG, "Error getting navigation bar size: " + e.getMessage());
        }
        return 0;
    }
	
	public void setRect(Rect rect) {
        this.mRect = rect;
    }

    public Rect getRect() {
        return mRect;
    }

    /**
     * Check if service is connected and ready
     */
    private boolean isServiceConnected() {
        return mIsBound && mService != null;
    }

    // Callback management
    public void setCallback(UIClientCallback callback) {
        this.mCallback = callback;
    }

    public interface UIClientCallback {
        void startFreeform();
    }

}