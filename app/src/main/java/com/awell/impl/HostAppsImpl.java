package com.awell.impl;

import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.util.Log;
import android.view.ViewGroup;

import com.awell.control.AppsCustomizeControl;
import com.awell.control.HostApps;
import com.awell.launcher2.LauncherApplication;

public class HostAppsImpl implements HostApps {
    private final String TAG = HostAppsImpl.class.getSimpleName();

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

    @Override
    public void loadApps() {
        Log.i(TAG, "loadApps: huang load all apps==>");
        ((LauncherApplication) getmAppContext()).getModel().startLoader(true, -1);
    }
}
