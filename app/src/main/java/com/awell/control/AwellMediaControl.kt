package com.awell.control

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import com.awell.ctrlview.MusicWidget
import com.awell.launcher.IDataChangeInterface
import com.awell.launcher.IHostPluginInterface
import com.awell.library.AwellTool
import java.lang.reflect.Method


/**
 * Activity 持有该对象
 * MusicWidget 持有该对象用于控制音乐播放、上下曲切换等
 */
class AwellMediaControl() {

    private val TAG = AwellMediaControl::class.simpleName
    val mNullStr = "null"

    var updateMusicView: UpdateMediaDataToView? = null

    var mediaViewModel: MediaViewModel? = null

    private var hostService: IHostPluginInterface? = null
    private var isBound = false

    init {
    }


    private val LOCAL_MEDIA_PKG = setOf(
        "/system/bin/gocsdk",
        "com.awell.bluetooth",
        "com.awell.localmusic",
        "com.awell.radio",
        "cn.kuwo.kwmusiccar",
        "com.zjinnova.zlink",
    )
	
	  /**
     * 安全地从 Bundle 获取 Int 值
     */
    private fun Bundle.getSafeInt(key: String, defaultValue: Int): Int {
        return try {
            when (val value = get(key)) {
                is Int -> value
                is String -> value.toIntOrNull() ?: defaultValue
                is Number -> value.toInt()
                else -> defaultValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Int for key: $key, using default: $defaultValue", e)
            defaultValue
        }
    }
    /**
     * 安全地从 Bundle 获取 Long 值
     */
    private fun Bundle.getSafeLong(key: String, defaultValue: Long): Long {
        return try {
            when (val value = get(key)) {
                is Long -> value
                is Int -> value.toLong()
                is String -> value.toLongOrNull() ?: defaultValue
                is Number -> value.toLong()
                else -> defaultValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Long for key: $key, using default: $defaultValue", e)
            defaultValue
        }
    }

    /**
     * 安全地从 Bundle 获取 Boolean 值
     */
    private fun Bundle.getSafeBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            when (val value = get(key)) {
                is Boolean -> value
                is String -> value.toBoolean()
                is Int -> value != 0
                else -> defaultValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get Boolean for key: $key, using default: $defaultValue", e)
            defaultValue
        }
    }
	

    /**
     * 实现服务端的回调
     * 第一次注册和服务端有数据更新时回调
     * 服务端会发送数据过来，
     * 其他程序也会发送信息过来，但都是通过服务端通知到这里
     * 服务端负责转发来自其他程序的信息
     *
     */
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

                    MusicWidget.OTHER_MUSIC_PLAY_IMAGE,
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

                    else -> {
                        handleOriginBundle(bundle)
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

    /**
     * 获取当前媒体播放的包名
     */
    fun getCurrentPkgName(): String? {
        return hostService?.currentMeidaPlayingPkg
    }

    /**
     * 客户端解绑服务端
     */
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

    /**
     * 发送到其他程序的数据
     * client --> host --> otherApp
     */
    fun sendBundleToOtherApp(bundle: Bundle): String {
        var data = "default value"
        if (isBound) {
            data = hostService?.pluginToOtherAppWithBundle(bundle).toString()
        }
        return data
    }

    /**
     * 会回调到客户端内部实现
     * client --> host --> client Impl
     */
    fun sendBundleToInternal(bundle: Bundle) {
        if (isBound) {
            hostService?.pluginToInternalImplWithBundle(bundle).toString()
        }
    }

    /**
     * 客户端发送数据到服务端
     * 用于自定义的数据处理如本地音乐应用的上下曲
     * client --> host --> otherApp
     */
    fun sendStrToHost(string: String): String {
        var data = "default value"
        if (isBound) {
            data = hostService?.pluginToOtherAppWithStr(string).toString()
        }
        return data
    }

    /**
     * 三方的下一曲切换。注册到media session的媒体应用
     */
    fun sendNextToHost() {
        hostService?.nextSong()
    }

    /**
     * 三方应用的上一曲切换，注册到media session的媒体应用
     */
    fun sendPreToHost() {
        hostService?.preSong()
    }

    fun sendTogglePlayPause() {
        hostService?.togglePlayPause()
    }

    fun sendTogglePause() {
        hostService?.togglePause()
    }

    fun getAlbumArtByUri(): Uri? {
        return hostService?.currnetAlbumArtUri
    }

    fun getLoadPluginApkFilePath(): String? {
        return hostService?.pluginApkFilePath
    }

    /**
     * 客户端绑定都到服务端
     * Host:Client
     * 1:n
     */
    fun bindDataService(context: Context) {
        val intent = Intent().apply {
            component = ComponentName(
                context.packageName,
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

    private fun handleMediaPlay(bundle: Bundle) {
        val pkg = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val command = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val mediaType = bundle.getSafeInt(AwellTool.VALUE_M3, 3)
        val currentMedia = bundle.getSafeInt(AwellTool.VALUE_M4, MusicWidget.MUSIC)
        val isStartCommand = "start" == command
        val isLocalMusicPackage = (pkg.contains("localmusic")
                || pkg.contains("com.awell.bluetooth")
                || pkg.contains("/system/bin/gocsdk"))

        val isStopCommand = "stop" == command

        if (!isLocalMusicPackage && pkg == hostService?.currentMeidaPlayingPkg && isStopCommand) {
            mediaViewModel?.updatePlayStatus(bundle, false, MusicWidget.OTHER_MUSIC)
            updateMusicView?.updateViewPlayStatus(bundle, false, MusicWidget.OTHER_MUSIC)
        }

        //back_car 也会发送媒体数据过来，需要过滤
        if (isMediaPkg(pkg) /*|| (mediaType > MusicWidget.BT && mediaType == android.media.AudioManager.STREAM_MUSIC)*/) {
            mediaViewModel?.updateMediaState(bundle, pkg, command, mediaType, currentMedia)
            updateMusicView?.updateViewMusicPlay(bundle, pkg, command, mediaType, currentMedia)
        }

    }

    private fun isMediaPkg(pkg: String?): Boolean = LOCAL_MEDIA_PKG.any { keyword ->
        pkg?.contains(keyword, ignoreCase = true) ?: false
    }

    private fun handleMusicPlayStatus(bundle: Bundle) {
		val musicStatus = bundle.getSafeBoolean(AwellTool.VALUE_M1, false)
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
        mediaViewModel?.updatePlayImage(bundle)
        updateMusicView?.updateViewMusicPlayImage(bundle)

    }

    private fun handleMusicPlayTime(bundle: Bundle) {
		val currentTime = bundle.getSafeLong(AwellTool.VALUE_M1, 0L)
        val totalTime = bundle.getSafeLong(AwellTool.VALUE_M2, 0L)
        mediaViewModel?.updatePlayTime(bundle, currentTime, totalTime, MusicWidget.MUSIC)
        updateMusicView?.updateViewPlayTime(bundle, currentTime, totalTime, MusicWidget.MUSIC)
    }

    private fun handleBTPlayStatus(bundle: Bundle) {
		val status = bundle.getSafeBoolean(AwellTool.VALUE_M1, false)
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
		val currentTimeValue = bundle.getSafeInt(AwellTool.VALUE_M1, 0)
        val totalTimeValue = bundle.getSafeInt(AwellTool.VALUE_M2, 0)
        val currentTime = (currentTimeValue * 1000).toLong()
        val totalTime = (totalTimeValue * 1000).toLong()
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
        val musicStatus = bundle.getSafeBoolean(AwellTool.VALUE_M1, false)
        mediaViewModel?.updatePlayStatus(bundle, musicStatus, MusicWidget.OTHER_MUSIC)
        updateMusicView?.updateViewPlayStatus(
            bundle,
            musicStatus,
            MusicWidget.OTHER_MUSIC
        )
    }

    private fun handleOtherMusicTime(bundle: Bundle) {
        val currentTime = bundle.getSafeLong(AwellTool.VALUE_M1, 0L)
        val totalTime = bundle.getSafeLong(AwellTool.VALUE_M2, 0L)
        mediaViewModel?.updatePlayTime(bundle, currentTime, totalTime, MusicWidget.OTHER_MUSIC)
        updateMusicView?.updateViewPlayTime(
            bundle,
            currentTime,
            totalTime,
            MusicWidget.OTHER_MUSIC
        )
    }


    private fun handleOriginBundle(bundle: Bundle) {
        mediaViewModel?.handleOriginBundle(bundle)
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

        fun updateViewMusicPlayImage(bundle: Bundle)

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

        fun handleOriginBundle(bundle: Bundle)
    }
}