package com.awell.control;

import android.util.Log;

public class HostAppsHolder {
    public static HostApps apps = null;
    private static final String TAG = HostAppsHolder.class.getSimpleName();

    public static void init(HostApps apps) {
        Log.i(TAG, "init: huang apps=>" + apps);
        HostAppsHolder.apps = apps;
    }



}
