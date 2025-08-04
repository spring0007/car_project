// IHostPluginInterface.aidl
package com.awell.launcher;

import com.awell.launcher.IDataChangeInterface;

// Declare any non-default types here with import statements

interface IHostPluginInterface {

    String getSongName();

    Bundle notifyData();

    String pluginToHostWithBundle(in Bundle bundle);
    String pluginToHostWithStr(in String status);

    void registerListener(IDataChangeInterface listener);
    void unregisterListener(IDataChangeInterface listener);

}