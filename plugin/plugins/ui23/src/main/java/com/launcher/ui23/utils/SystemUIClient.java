package com.launcher.ui23.utils;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.android.systemui.awell.services.ISystemUIService;

public class SystemUIClient {
    private ISystemUIService mService;

    private static final String TAG = SystemUIClient.class.getSimpleName();
    private boolean mBound = false;

    public static final String MUSIC_PKG = "com.awell.localmusic";
    private int mFreeformMode = 0;


    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISystemUIService.Stub.asInterface(service);
            mBound = true;
            mFreeformMode = 0;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
            mFreeformMode = 0;
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
        LogUtil.i( "bindToSystemUIService: huang bind systemUI result==>" + result);

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
                LogUtil.i( "fullScreenFreeform" );
                mFreeformMode = 0x01;
                mService.fullScreenFreeform();
            } catch (Exception e) {
                LogUtil.e( "fullScreenFreeform: error==>" + e.getMessage());
            }
        }
    }

    public void hideFreeform() {
        if (mBound && mService != null) {
            try {
                LogUtil.i( "hideFreeform" );
                mFreeformMode = 0x10;
                mService.hideFreeform();
            } catch (Exception e) {
                LogUtil.e( "hideFreeform: error==>" + e.getMessage());
            }
        }
    }


    public void setFreeformType(int type) {
        if (mBound && mService != null) {
            try {

                mService.setFreeformType(type);
            } catch (Exception e) {
                LogUtil.e( "setFreeformType: error==>" + e.getMessage());
            }
        }
    }

    public void startOrSetFreeformType(Intent intent, ActivityOptions options, int windowType) {
        if (mBound && mService != null) {
            try {
                Bundle bundle = options.toBundle();
                mService.startOrSetFreeformType(intent, bundle, windowType);
            } catch (Exception e) {
                LogUtil.e( "startFreeform: error==>" + e.getMessage());
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