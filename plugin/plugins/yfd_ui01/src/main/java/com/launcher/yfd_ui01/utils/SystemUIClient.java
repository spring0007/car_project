package com.launcher.yfd_ui01.utils;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;

import com.android.systemui.awell.services.ISystemUIService;
import com.launcher.yfd_ui01.R;

public class SystemUIClient {
    private static final String TAG = "SystemUIClient";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String SYSTEM_UI_SERVICE_CLASS = "com.android.systemui.awell.services.AwellSystemUIService";
    
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
    

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mIsBound = true;
            Log.i(TAG, "Service connected successfully");
            SystemProperties.set("persist.sys.lz.default_freeform_size", DEFAULT_WIDTH+","+DEFAULT_HEIGHT);
            SystemProperties.set("persist.sys.lz.default_freeform_position", DEFAULT_LEFT+","+DEFAULT_TOP);
            if (mCallback != null) {
                mCallback.startFreeform();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
            mCallback = null;
            SystemProperties.set("persist.sys.lz.default_freeform_size", "0,0");
            SystemProperties.set("persist.sys.lz.default_freeform_position", "-1,-1");
            Log.i(TAG, "Service disconnected");

        }
    };

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
        if (!isServiceConnected()) {
            Log.w(TAG, "Service not connected, cannot start freeform");
            return;
        }
        
        try {
            Rect bounds = calculateFreeformBounds(context);
            mService.startOrSetFreeformType(bounds, windowType);
            Log.i(TAG, "Freeform started with bounds: " + bounds + ", type: " + windowType);
        } catch (Exception e) {
			Log.e(TAG, "Failed to startOrSetFreeformType: " + e.getMessage());
        }
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