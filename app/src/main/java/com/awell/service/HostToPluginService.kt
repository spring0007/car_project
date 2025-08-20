package com.awell.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.Parcelable
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.awell.ctrlview.MusicWidget
import com.awell.data.TrackInfo
import com.awell.launcher.IDataChangeInterface
import com.awell.launcher.IHostPluginInterface
import com.awell.launcher2.LauncherApplication.mAppContext
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellLibrary
import com.awell.library.AwellTool
import com.awell.model.AlbumArtProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import kotlin.math.log


class HostToPluginService : Service() {

    private val TAG: String = "HostToPluginService"
    val mNullStr = "null"

    var mSongName: String? = null

    private var albumArtProvider: AlbumArtProvider

    /**
     * 添加同步锁对象
     * fix bug IllegalStateException: beginBroadcast() called while already in a broadcast
     */
    private val broadcastLock = Any()

    /**
     * 防止同一线程嵌套重入
     * fix bug IllegalStateException: beginBroadcast() called while already in a broadcast
     */
    @Volatile
    private var isBroadcasting = false

    private var pendingBundle: Bundle? = null // 暂存最新待处理数据

    /**
     * data数据显示
     */
    var mMusicPlayInfo: MusicPlayInfo? = null

    /**
     * 客户端注册的监听
     */
    val listeners = RemoteCallbackList<IDataChangeInterface>()
    private var mMediaListener = MediaNotificationListener()

    /**
     * 注册到media session服务里三方媒体
     */
    private val MEDIA_SESSION_PKG_KEYWORDS = setOf(
        "cn.kuwo.kwmusiccar",
        "com.zjinnova.zlink",
    )

    private val LOCAL_MEDIA_PKG = setOf(
        "/system/bin/gocsdk",
        "com.awell.localmusic",
    )

    /**
     * 和AwellAutoApi服务的通信
     * 由于framework下也有相同的包名、类名
     * 该对象优先实例化framework下的类
     * AwellLibrary模块下的类修改无效
     */
    val mediaLibrary = AwellLibrary(AwellTool.OPEN)

    /**
     * AwellAutoApi服务的回调实现
     * 接收来及其他程序的信息
     */
    val mDataListener = AwellLibrary.OnDataListener { bundle: Bundle? ->
        bundle?.let {

            //printThreadInfo(bundle)
            saveTempValue(bundle)

            val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)
            if (status != MusicWidget.OTHER_MUSIC_PLAY_IMAGE
                && status != AwellTool.MUSIC.PLAY_IMAGE
            ) {
                //客户端不暂时不处理图片，saveTempValue稍后会通知客户端处理
                notifyClientDataChanged(bundle)
            }

        } ?: run {
            Log.e(TAG, "AwellLibrary.OnDataListener onResult:  bundle is null!!")
        }
    }

    init {
        albumArtProvider = AlbumArtProvider(mAppContext)
        mMediaListener.initDependencies(mAppContext)
        Log.i(TAG, "huang init ==>: mediaLibrary=${mediaLibrary.javaClass.classLoader}")
        mediaLibrary.init(mAppContext)
        mediaLibrary.setOnDataListener(mDataListener)
    }

    // 协程作用域管理
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
            Log.e("Service", "Coroutine error", e)
        }
    )

    // 并发控制
    private val trackMutex = Mutex()
    private var currentJob: Job? = null


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    /**
     * mBinder:代表IHostPluginInterface实例
     * 客户端和服务端的通信函数列表
     *
     */
    private val mBinder: IHostPluginInterface.Stub = object : IHostPluginInterface.Stub() {

        override fun getSongName(): String? {
            Log.i(TAG, "getSongName: huang get song name =>${mSongName}")
            return mSongName ?: mNullStr
        }

        override fun notifyData(): Bundle? {
            Log.i(TAG, "notifyData: huang notify plugin data change ==>")
            return null
        }

        override fun pluginToOtherAppWithBundle(bundle: Bundle?): String? {
            Log.i(TAG, "pluginToOtherAppWithBundle: huang bundle=>${bundle}")
            return mediaLibrary.setDataEvent(bundle)
        }

        override fun pluginToOtherAppWithStr(status: String?): String? {
            Log.i(TAG, "pluginToOtherAppWithStr: huang status=${status}")
            return mediaLibrary.setDataEvent(status)
        }

        override fun pluginToInternalImplWithBundle(bundle: Bundle?) {
            Log.i(TAG, "pluginToInternalImplWithBundle: huang bundle=>${bundle}")
            mMediaListener.sendDataToAwellApi(bundle)
        }

        override fun getCurrentMeidaPlayingPkg(): String? {
            return mMediaListener.playingPackageName
        }

        override fun setCurrentTrack(trackInfo: TrackInfo?) {
//            trackInfo?.let {
//                albumArtProvider.updateTrack(it)
//            }

        }

        override fun getCurrnetAlbumArtUri(): Uri? {
            return albumArtProvider.getCurrentArtUri()
        }

        override fun nextSong() {
            mMediaListener.skipToNext()
        }

        override fun preSong() {
            mMediaListener.skipToPrevious()
        }

        override fun togglePlayPause() {
            mMediaListener.togglePlayPause()
        }

        override fun togglePause() {
            mMediaListener.togglePause()
        }


        override fun registerListener(listener: IDataChangeInterface) {
            listeners.register(listener)
            mMusicPlayInfo?.let {
                notifyClientDataChanged(musicPlayInfoToBundle(mMusicPlayInfo!!))
            }

        }

        override fun unregisterListener(listener: IDataChangeInterface) {
            listeners.unregister(listener)
        }
    }

    /**
     * 服务端保留一份数据
     * 当客户端切换时，
     * 可以通过aidl更新到客户端的数据显示
     */
    private fun saveTempValue(bundle: Bundle) {
        val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)
        when (status) {
            AwellTool.MUSIC.PLAY_NAME -> {
                //保存本地音乐播放的信息
                mMusicPlayInfo = bundleToMusicPlayInfo(bundle)
            }

            MusicWidget.OTHER_MUSIC_PLAY_IMAGE,
            AwellTool.MUSIC.PLAY_IMAGE -> {
                handleLocalMusicImageByScope(bundle)
            }

            AwellTool.MEDIA_PLAY -> {
                //三方应用注册到media session服务里，需要监听；本地音乐等媒体未注册到media session，移除监听
                //只有在start播放的时候监听，其他命令移除监听
                val pkg = bundle.getString(AwellTool.VALUE_M1, null)
                val command = bundle.getString(AwellTool.VALUE_M2, null)


                when {
                    //只在start的时候更新播放的包名
                    command == "start" -> {
                        pkg?.let {
                            mMediaListener.playingPackageName = pkg
                            Log.i(TAG, "saveTempValue: huang pkg==>${pkg} command=${command}")
                            if (isMediaSessionPkg(pkg, command)) {
                                //只有三方注册到media session服务里的媒体开始播放的时候才注册监听
                                mMediaListener.startCallbacks()

                                //单独更新carplay图片
                                if (pkg.contains("com.zjinnova.zlink")) {
                                    updateCarplayImageAlbum()
                                }

                            } else if (isLocalMediaStart(pkg, command)) {
                                //只有未注册到media session服务里的媒体开始播放的时候
                                //才将注册到media session服务里的媒体断开回调
                                mMediaListener.removeCallbacks()

                                //蓝牙音乐没有专辑图片，设置默认图片
                                setDefaultBtArt(pkg, bundle)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查是否是本地媒体开始播放 command==start
     * local music
     * bt music
     * ...
     */
    private fun isLocalMediaStart(pkg: String?, command: String?): Boolean {
        if (pkg == null || command == null)
            return false
        return command.contains("start", ignoreCase = true)
                && LOCAL_MEDIA_PKG.any { keyword ->
            pkg.contains(keyword, ignoreCase = true)
        }
    }


    /**
     * 检查是否是三方媒体开始播放 command==start
     */
    private fun isMediaSessionPkg(pkg: String?, command: String?): Boolean {
        if (pkg == null || command == null)
            return false

        return command.contains("start", ignoreCase = true)
                && MEDIA_SESSION_PKG_KEYWORDS.any { keyword ->
            pkg.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 处理蓝牙图片
     * 蓝牙音乐无专辑图片设为默认
     */
    private fun setDefaultBtArt(pkg: String, bundle: Bundle) {
        if (pkg.contains("/system/bin/gocsdk")) {
            //更新图片 delete value
            val tempBundle = bundle.deepCopy()
            tempBundle.putString(AwellTool.VALUE_M1, null)
            tempBundle.putString(AwellTool.VALUE_M3, null)
            handleLocalMusicImageByScope(tempBundle)
        }
    }

    /**
     * 处理carplay图片
     * carplay播放酷狗音乐会有广播通知图片更新
     * 切换成其他媒体，在切回carplay不发送广播通知图片更新
     * 需要单独处理
     */
    private fun updateCarplayImageAlbum() {
        val sdcardDir = Environment.getExternalStorageDirectory()
        val imageFile = File(sdcardDir, "cp.jpg")

        if (imageFile.exists()) {
            val bundle = Bundle()
            bundle.putString(
                AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_PLAY_IMAGE
            )
            bundle.putString(AwellTool.VALUE_M4, imageFile.absoluteFile.toString())
            handleLocalMusicImageByScope(bundle)
        }
    }

    /**
     * 处理音乐图片
     *
     */
    private fun handleLocalMusicImageByScope(bundle: Bundle) {

        //确保同一时间只有一个任务执行
        currentJob?.cancel()
        currentJob = serviceScope.launch {
            try {
                var uri: Uri? = null
                trackMutex.withLock {
                    uri = processTrackUpdate(bundle)
                }
                val updateArtUriBundle = Bundle()
                updateArtUriBundle.putString(
                    AwellTool.STATUS_ACCEPT,
                    MusicWidget.OTHER_MUSIC_PLAY_IMAGE
                )
                updateArtUriBundle.putString(AwellTool.VALUE_M1, null)
                uri?.let {
                    updateArtUriBundle.putString(AwellTool.VALUE_M1, uri.toString())
                }
                notifyClientDataChanged(updateArtUriBundle)

            } catch (e: CancellationException) {
                //任务取消
                Log.i(TAG, "handleLocalMusicImageByScope: huang cancellation mission=${e.message}")
            } catch (e: Exception) {
                //任务异常
                Log.i(TAG, "handleLocalMusicImageByScope: huang error=${e}")
            }
        }
    }

    /**
     * 解析bundle提供给albumArtProvider处理图片
     */
    private suspend fun processTrackUpdate(bundle: Bundle) = withContext(Dispatchers.IO) {
        val value1 = bundle.getString(AwellTool.VALUE_M1, "0 , 0")
        //播放本地音乐，解析播放的图片
        val str = value1.split(" , ".toRegex()).dropLastWhile {
            it.isEmpty()
        }.toTypedArray()
        val songId = str[0].toLong()
        val albumId = str[1].toLong()
        val filePath = bundle.getString(AwellTool.VALUE_M2, null)
        val artUri = bundle.getString(AwellTool.VALUE_M3, null)
        val imagePath = bundle.getString(AwellTool.VALUE_M4, null)

        val trackInfo = TrackInfo(
            null,
            songId,
            albumId,
            filePath,
            imagePath = imagePath,
            metadata = when {
                //酷我音乐在后台，切换本地音乐播放，MediaController还持有metadata
                //如果本地音乐这时候获取图片会直接拿到metadata的图片，需要使用传递过来的VALUE_M3判断
                //useMetadata为null代表本地音乐发送图片处理请求，不在metadata对象获取专辑图片
                //artUri != "default" 酷我音乐切换到本地音乐更新图片
                artUri != null && artUri != "default" -> {
                    val originMetadata = mMediaListener.getmMediaController()?.metadata
                    val builder = MediaMetadata.Builder(originMetadata)
                    builder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, artUri)
                    builder.build()
                }

                else -> null
            }
        )
        albumArtProvider.updateTrack(trackInfo)
    }

    /**
     * 通过aidl更新bundle数据到客户端的
     * 客户端可解析bundle获取所需数据
     */
    private fun notifyClientDataChanged(bundle: Bundle) {
        // 步骤1：原子性更新待处理数据
        var shouldProcessImmediately = false
        synchronized(broadcastLock) {
            // 保存最新数据（覆盖旧值）
            pendingBundle = bundle

            // 检查是否可立即处理
            if (!isBroadcasting) {
                isBroadcasting = true
                shouldProcessImmediately = true
            }
        }

        // 步骤2：立即处理或等待后续处理
        if (shouldProcessImmediately) {
            processPendingBundles()
        }
    }

    /**
     * 处理所有待发数据（确保处理最新数据）
     */
    private fun processPendingBundles() {
        var currentBundle: Bundle?

        do {
            // 步骤3：原子获取最新数据
            synchronized(broadcastLock) {
                currentBundle = pendingBundle
                pendingBundle = null // 清空暂存
            }

            // 步骤4：处理有效数据
            currentBundle?.let { bundle ->
                try {
                    // 实际广播逻辑
                    val count = listeners.beginBroadcast()
                    try {
                        for (i in 0..<count) {
                            listeners.getBroadcastItem(i).onDataChanged(bundle)
                        }
                    } catch (e: RemoteException) {
                        Log.e(
                            TAG,
                            "processPendingBundles: huang RemoteException in broadcast ${e.message}"
                        )
                    } finally {
                        listeners.finishBroadcast()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "processPendingBundles: huang Error processing bundle ${e.message}")
                }
            }

            // 步骤5：检查是否有新数据到达
            synchronized(broadcastLock) {
                currentBundle = if (pendingBundle != null) {
                    // 有新数据则继续循环
                    pendingBundle.also { pendingBundle = null }
                } else {
                    // 无新数据则结束处理
                    isBroadcasting = false
                    null
                }
            }
        } while (currentBundle != null)
    }

    private fun bundleToMusicPlayInfo(bundle: Bundle): MusicPlayInfo {
        val song = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val singer = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val album = bundle.getString(AwellTool.VALUE_M3, mNullStr)
        val music = MusicPlayInfo(
            songName = song,
            singerName = singer,
            album = album
        )
        return music
    }

    private fun musicPlayInfoToBundle(music: MusicPlayInfo): Bundle {
        val b = Bundle()
        music.let {
            b.putString(AwellTool.STATUS_ACCEPT, AwellTool.MUSIC.PLAY_NAME)
            b.putString(AwellTool.VALUE_M1, music.songName)
            b.putString(AwellTool.VALUE_M2, music.singerName)
            b.putString(AwellTool.VALUE_M3, music.album)
        }
        return b
    }

    private fun printThreadInfo(bundle: Bundle) {

        val callPid = Binder.getCallingPid()
        val callUid = Binder.getCallingUid()
        val currentThread = Thread.currentThread()
        val threadInfo = "Thread:${currentThread.name} (ID=${currentThread.id})"
        val callingProcessName = runCatching {
            val manager =
                mAppContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningProcess = manager.runningAppProcesses
            runningProcess?.find { it.pid == callPid }?.processName
        }.getOrNull()
        Log.i(TAG, "printThreadInfo: huang bundle =>${bundle}")
        Log.i(
            TAG,
            "printThreadInfo: huang Pid=$callPid Uid=$callUid processName=$callingProcessName"
        )
        Log.i(TAG, "printThreadInfo: huang thread info=>${threadInfo}")
    }
}


@Parcelize
data class MusicPlayInfo(
    val songName: String,
    val singerName: String,
    val album: String
) : Parcelable