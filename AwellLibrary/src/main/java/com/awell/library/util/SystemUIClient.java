package com.awell.library.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.systemui.awell.services.ISystemUIService;

/**
 * SystemUI 客户端 - 管理自由窗口的绑定和操作
 * 所有插件统一使用此版本，无需各自维护
 */
public class SystemUIClient {
    private static final String TAG = SystemUIClient.class.getSimpleName();
    public static final String MUSIC_PKG = "com.awell.localmusic";
    private ISystemUIService mService;
    private boolean mBound = false;
    private int mFreeformMode = 0;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mBound = true;
            mFreeformMode = 0;
            Log.i(TAG, "bindToSystemUIService: connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
            mFreeformMode = 0;
            Log.i(TAG, "bindToSystemUIService: disconnected");
        }
    };

    public SystemUIClient(Context context) {
    }

    public void bindToSystemUIService(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                "com.android.systemui",
                "com.android.systemui.awell.services.AwellSystemUIService"
        ));
        boolean result = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "bindToSystemUIService: result==>" + result);
    }

    public void unbindService(Context context) {
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
            mFreeformMode = 0;
        }
    }

    public void fullScreenFreeform() {
        if (mBound && mService != null) {
            try {
                mFreeformMode = 0x01;
                mService.fullScreenFreeform();
            } catch (Exception e) {
                Log.e(TAG, "fullScreenFreeform: error==>" + e.getMessage());
            }
        }
    }

    public void hideFreeform() {
        if (mBound && mService != null) {
            try {
                mFreeformMode = 0x10;
                mService.hideFreeform();
            } catch (Exception e) {
                Log.e(TAG, "hideFreeform: error==>" + e.getMessage());
            }
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

    public void startOrSetFreeformType(Intent intent, android.app.ActivityOptions options, int windowType) {
        if (mBound && mService != null) {
            try {
                Bundle bundle = options.toBundle();
                mService.startOrSetFreeformType(intent, bundle, windowType);
            } catch (Exception e) {
                Log.e(TAG, "startFreeform: error==>" + e.getMessage());
            }
        }
    }

    public int getmFreeformMode() {
        return mFreeformMode;
    }

    public void setmFreeformMode(int mFreeformMode) {
        this.mFreeformMode = mFreeformMode;
    }
}
