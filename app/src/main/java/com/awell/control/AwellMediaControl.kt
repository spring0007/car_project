package com.awell.control

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import com.awell.ctrlview.MusicWidget
import com.awell.launcher.IDataChangeInterface
import com.awell.launcher.IHostPluginInterface
import com.awell.launcher2.LauncherApplication.mAppContext
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellTool
import com.tencent.shadow.sample.host.lib.MediaViewModel
import java.lang.reflect.Method


class AwellMediaControl() {

    private val TAG = AwellMediaControl::class.simpleName
    val mNullStr = "null"
    private var mMediaListener = MediaNotificationListener()
    var updateMusicView: UpdateMediaDataToView? = null

    var mediaViewModel: MediaViewModel? = null

    private var hostService: IHostPluginInterface? = null
    private var isBound = false

    init {
        mMediaListener.initDependencies(mAppContext)
    }


    val mDataChangeListener = object : IDataChangeInterface.Stub() {
        override fun onDataChanged(bundle: Bundle?) {
            bundle?.let {
                val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)
                when (status) {
                    AwellTool.MEDIA_PLAY -> {
                        handleMediaPlay(bundle)
                    }

                    AwellTool.MUSIC.PLAY_STATUS -> {
                        handleMusicPlayStatus(bundle)
                    }

                    AwellTool.MUSIC.PLAY_NAME -> {
                        handleMusicPlayName(bundle)
                    }

                    AwellTool.MUSIC.PLAY_IMAGE -> {
                        handleMusicPlayImage(bundle)
                    }

                    AwellTool.MUSIC.PLAY_TIME -> {
                        handleMusicPlayTime(bundle)
                    }

                    AwellTool.BT.PLAY_STATUS -> {
                        handleBTPlayStatus(bundle)
                    }

                    AwellTool.BT.PLAY_NAME -> {
                        handleBTPlayName(bundle)
                    }

                    AwellTool.BT.PLAY_TIME -> {
                        handleBTPlayTime(bundle)
                    }

                    AwellTool.RADIO.FREQUENCY -> {
                        handleRadioFreq(bundle)
                    }

                    MusicWidget.OTHER_MUSIC_PLAYNAME -> {
                        handleOtherMusicPlayName(bundle)
                    }

                    MusicWidget.OTHER_MUSIC_PLAYSTATUS -> {
                        handleOtherMusicStatus(bundle)
                    }

                    MusicWidget.OTHER_MUSIC_TIME -> {
                        handleOtherMusicTime(bundle)
                    }
                }
            } ?: run {
                Log.e(TAG, " onResult:  bundle is null!!")
            }
        }
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            hostService = IHostPluginInterface.Stub.asInterface(binder)
            isBound = true
            hostService?.registerListener(mDataChangeListener)
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            hostService = null
            isBound = false
        }
    }

    fun bindDataService(context: Context) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.awell.launcher",
                "com.awell.service.HostToPluginService"
            )
        }
        val userHandle = android.os.Process.myUserHandle()
        try {
            // 使用反射调用系统API
            val method: Method = Context::class.java.getMethod(
                "bindServiceAsUser",
                Intent::class.java,
                ServiceConnection::class.java,
                Int::class.javaPrimitiveType,
                UserHandle::class.java
            )

            val bind: Boolean = method.invoke(
                context,
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE,
                userHandle
            ) as Boolean

            Log.i(TAG, "bindDataService success: $bind")
        } catch (e: Exception) {
            Log.e(TAG, "bindDataService failed", e)
            // 降级处理
            val bind = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(TAG, "bindDataService bindService: $bind")
        }

    }

    fun unBindDataService(context: Context) {
        if (isBound) {
            try {
                hostService?.unregisterListener(mDataChangeListener)
            } catch (e: RemoteException) {
                // Ignore
            }
            context.unbindService(serviceConnection)
        }
    }

    fun sendBundleToHost(bundle: Bundle): String {
        var data = "null"
        if (isBound) {
            data = hostService?.pluginToHostWithBundle(bundle).toString()
        }
        return data
    }

    fun sendStrToHost(string: String): String {
        var data = "null"
        if (isBound) {
            data = hostService?.pluginToHostWithStr(string).toString()
        }
        return data
    }

    private fun handleMediaPlay(bundle: Bundle) {
        Log.i(TAG, "handleMediaPlay: huang mediaViewModel=${mediaViewModel}")
        val pkg = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val command = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val mediaType = bundle.getInt(AwellTool.VALUE_M3, 3)
        val currentMedia = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.MUSIC)
        val isStartCommand = "start" == command
        val isLocalMusicPackage =
            (pkg.contains("localmusic") || pkg.contains("com.awell.bluetooth") || pkg.contains("/system/bin/gocsdk"))

        mMediaListener.let {
            if (isLocalMusicPackage && isStartCommand) it.removeCallbacks()
        }

        val isStopCommand = "stop" == command

        if (!isLocalMusicPackage && pkg == mMediaListener.currentPlayingPackage && isStopCommand) {
            mediaViewModel?.updatePlayStatus(bundle, false, MusicWidget.OTHER_MUSIC)
            updateMusicView?.updateViewPlayStatus(bundle, false, MusicWidget.OTHER_MUSIC)
        }

        mediaViewModel?.updateMediaState(bundle, pkg, command, mediaType, currentMedia)

        updateMusicView?.updateViewMusicPlay(bundle, pkg, command, mediaType, currentMedia)
    }

    private fun handleMusicPlayStatus(bundle: Bundle) {
        val musicStatus = bundle.getBoolean(AwellTool.VALUE_M1)
        mediaViewModel?.updatePlayStatus(bundle, musicStatus, MusicWidget.MUSIC)
        updateMusicView?.updateViewPlayStatus(bundle, musicStatus, MusicWidget.MUSIC)
    }

    private fun handleMusicPlayName(bundle: Bundle) {
        val songName = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val singerName = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val album = bundle.getString(AwellTool.VALUE_M3, mNullStr)
        mediaViewModel?.updatePlayInfo(bundle, songName, singerName, album, MusicWidget.MUSIC)
        updateMusicView?.updateViewPlayInfo(
            bundle,
            songName,
            singerName,
            album,
            MusicWidget.MUSIC
        )

    }

    private fun handleMusicPlayImage(bundle: Bundle) {
        val value1 = bundle.getString(AwellTool.VALUE_M1, "0 , 0")
        val str = value1.split(" , ".toRegex()).dropLastWhile {
            it.isEmpty()
        }.toTypedArray()
        val songId = str[0].toLong()
        val albumId = str[1].toLong()
        mediaViewModel?.updatePlayImage(bundle, songId, albumId)
        updateMusicView?.updateViewMusicPlayImage(bundle, songId, albumId)

    }

    private fun handleMusicPlayTime(bundle: Bundle) {
        val currentTime = bundle.getLong(AwellTool.VALUE_M1)
        val totalTime = bundle.getLong(AwellTool.VALUE_M2)
        mediaViewModel?.updatePlayTime(bundle, currentTime, totalTime, MusicWidget.MUSIC)
        updateMusicView?.updateViewPlayTime(bundle, currentTime, totalTime, MusicWidget.MUSIC)
    }

    private fun handleBTPlayStatus(bundle: Bundle) {
        val status = bundle.getBoolean(AwellTool.VALUE_M1)
        mediaViewModel?.updatePlayStatus(bundle, status, MusicWidget.BT)
        updateMusicView?.updateViewPlayStatus(bundle, status, MusicWidget.BT)

    }

    private fun handleBTPlayName(bundle: Bundle) {

        val songName = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val singerName = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val album = bundle.getString(AwellTool.VALUE_M3, mNullStr)
        mediaViewModel?.updatePlayInfo(bundle, songName, singerName, album, MusicWidget.BT)
        updateMusicView?.updateViewPlayInfo(bundle, songName, singerName, album, MusicWidget.BT)

    }

    private fun handleBTPlayTime(bundle: Bundle) {
        val currentTime = (bundle.getInt(AwellTool.VALUE_M1) * 1000).toLong()
        val totalTime = (bundle.getInt(AwellTool.VALUE_M2) * 1000).toLong()
        mediaViewModel?.updatePlayTime(bundle, currentTime, totalTime, MusicWidget.BT)
        updateMusicView?.updateViewPlayTime(bundle, currentTime, totalTime, MusicWidget.BT)
    }

    private fun handleRadioFreq(bundle: Bundle) {
        val fmOrAm = bundle.getString(AwellTool.VALUE_M1) ?: mNullStr
        val freq = bundle.getString(AwellTool.VALUE_M2) ?: mNullStr
        val unit = bundle.getString(AwellTool.VALUE_M3) ?: mNullStr
        mediaViewModel?.updateRadioInfo(bundle, freq, unit, fmOrAm)
        updateMusicView?.updateViewRadioFreq(bundle, fmOrAm, freq, unit)
    }

    private fun handleOtherMusicPlayName(bundle: Bundle) {
        val songName = bundle.getString(AwellTool.VALUE_M1) ?: mNullStr
        val singerName = bundle.getString(AwellTool.VALUE_M2) ?: mNullStr
        val album = bundle.getString(AwellTool.VALUE_M3) ?: mNullStr
        mediaViewModel?.updatePlayInfo(bundle, songName, singerName, album, MusicWidget.OTHER_MUSIC)
        updateMusicView?.updateViewPlayInfo(
            bundle,
            songName,
            singerName,
            album,
            MusicWidget.OTHER_MUSIC
        )
    }

    private fun handleOtherMusicStatus(bundle: Bundle) {
        val musicStatus = bundle.getBoolean(AwellTool.VALUE_M1)
        mediaViewModel?.updatePlayStatus(bundle, musicStatus, MusicWidget.OTHER_MUSIC)
        updateMusicView?.updateViewPlayStatus(
            bundle,
            musicStatus,
            MusicWidget.OTHER_MUSIC
        )
    }

    private fun handleOtherMusicTime(bundle: Bundle) {
        val currentTime = bundle.getLong(AwellTool.VALUE_M1)
        val totalTime = bundle.getLong(AwellTool.VALUE_M1)
        mediaViewModel?.updatePlayTime(bundle, currentTime, totalTime, MusicWidget.OTHER_MUSIC)
        updateMusicView?.updateViewPlayTime(
            bundle,
            currentTime,
            totalTime,
            MusicWidget.OTHER_MUSIC
        )
    }

    /**
     * 更新媒体信息接口
     * 如果不是用viewmodel观察数据变化
     * 可以实现该接口接收数据
     */
    interface UpdateMediaDataToView {

        fun updateViewMusicPlay(
            bundle: Bundle, pkg: String, command: String, mediaType: Int, currentMedia: Int
        )

        /**
         * true playing
         * false not playing
         */
        fun updateViewPlayStatus(bundle: Bundle, status: Boolean, type: Int)

        /**
         * songName 歌曲名
         * singerName 歌手名
         * album 专辑
         */

        fun updateViewMusicPlayImage(bundle: Bundle, songId: Long, albumId: Long)

        fun updateViewPlayInfo(
            bundle: Bundle,
            songName: String,
            singerName: String,
            album: String,
            type: Int
        )

        fun updateViewPlayTime(bundle: Bundle, currentTime: Long, totalTime: Long, type: Int)


        /**
         * fmOrAm FM or AM
         * freq 当前频率  87.5 or 531
         * unit 单位 MHz or KHz
         */
        fun updateViewRadioFreq(
            bundle: Bundle, fmOrAm: String, freq: String, unit: String
        )
    }
}