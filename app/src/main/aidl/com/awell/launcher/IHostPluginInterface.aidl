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

    String getPluginApkFilePath();


    void nextSong();
    void preSong();
    void togglePlayPause();
    void togglePause();

    void registerListener(IDataChangeInterface listener);
    void unregisterListener(IDataChangeInterface listener);

    /**
     * 返回宿主当前缓存的完整媒体状态(歌曲/歌手/播放暂停状态/进度/专辑图/电台频率)。
     * 返回的 Bundle 以 STATUS_ACCEPT 为 key 存放各媒体状态的子 Bundle,
     * 由客户端(AwellMediaControl)统一通过 UpdateMediaDataToView 接口按顺序更新 musicWidget。
     * 切换 plugin 后,新插件调用该方法即可立即同步 musicWidget 显示。
     */
    Bundle refreshCurrentMediaState();

}