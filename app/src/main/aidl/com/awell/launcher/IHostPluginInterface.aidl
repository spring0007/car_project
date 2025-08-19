// IHostPluginInterface.aidl
package com.awell.launcher;

import com.awell.launcher.IDataChangeInterface;
import com.awell.data.TrackInfo;

// Declare any non-default types here with import statements

interface IHostPluginInterface {

    String getSongName();

    Bundle notifyData();

    String pluginToOtherAppWithBundle(in Bundle bundle);
    String pluginToOtherAppWithStr(in String status);

    void pluginToInternalImplWithBundle(in Bundle bundle);

    String getCurrentMeidaPlayingPkg();

    void setCurrentTrack(in TrackInfo trackInfo);
    Uri getCurrnetAlbumArtUri();


    void nextSong();
    void preSong();
    void togglePlayPause();
    void togglePause();

    void registerListener(IDataChangeInterface listener);
    void unregisterListener(IDataChangeInterface listener);

}