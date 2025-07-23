package com.awell.impl;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;

import com.awell.control.AppsCustomizeControl;
import com.tencent.shadow.core.runtime.ShadowActivity;

public class HostApps implements com.tencent.shadow.sample.host.lib.HostApps {
    private final String TAG = HostApps.class.getSimpleName();

    @Override
    public void showAllApps(ViewGroup group) {
        Log.i(TAG, "showAllApps: huang show all apps =>");
        AppsCustomizeControl.INSTANCE.showApps(group);
    }

    @Override
    public void printStr(String message) {
        Log.i(TAG, "printStr: huang message=>" + message);
    }

    @Override
    public void hideAllApps() {
        AppsCustomizeControl.INSTANCE.hideApps();

    }
}
