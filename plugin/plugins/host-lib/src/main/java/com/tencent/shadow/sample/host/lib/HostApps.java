package com.tencent.shadow.sample.host.lib;

import android.app.Activity;
import android.view.ViewGroup;

public interface HostApps {

    void showAllApps(ViewGroup group);

    void printStr(String message);

    void hideAllApps();

    void loadApps();

}
