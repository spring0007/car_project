// IHostPluginInterface.aidl
package com.awell.launcher;

import com.awell.launcher.IDataChangeInterface;
import com.awell.data.TrackInfo;

// Declare any non-default types here with import statements

interface IHostPluginInterface {

    String getSongName();

    Bundle notifyData();

    String pluginToHostWithBundle(in Bundle bundle);
    String pluginToHostWithStr(in String status);

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