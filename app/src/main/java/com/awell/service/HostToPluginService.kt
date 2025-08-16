package com.awell.service

import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.net.Uri
import android.os.Bundle
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


class HostToPluginService : Service() {

    private val TAG: String = "HostToPluginService"
    val mNullStr = "null"

    var mSongName: String? = null

    private var albumArtProvider: AlbumArtProvider

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
     * 和AwellAutoApi服务的通信
     */
    val mediaLibrary = AwellLibrary(AwellTool.OPEN)

    /**
     * AwellAutoApi服务的回调实现
     * 接收来及其他程序的信息
     */
    val mDataListener = AwellLibrary.OnDataListener { bundle: Bundle? ->
        bundle?.let {
            saveTempValue(bundle)
            notifyClientDataChanged(bundle)
        } ?: run {
            Log.e(TAG, "AwellLibrary.OnDataListener onResult:  bundle is null!!")
        }
    }

    init {
        albumArtProvider = AlbumArtProvider(mAppContext)
        mMediaListener.initDependencies(mAppContext)
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

        override fun pluginToHostWithBundle(bundle: Bundle?): String? {
            Log.i(TAG, "pluginToHost: receiver bundle==>${bundle}")
            return mediaLibrary.setDataEvent(bundle)
        }

        override fun pluginToHostWithStr(status: String?): String? {
            Log.i(TAG, "pluginToHostWithStr: huang host receiver str=>${status}")
            return mediaLibrary.setDataEvent(status)
        }

        override fun getCurrentMeidaPlayingPkg(): String? {
            return mMediaListener.currentPlayingPackage
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
                pkg?.let {
                    if (pkg.contains("cn.kuwo.kwmusiccar")
                        && command != null && command.contains("start")
                    ) {
                        Log.i(TAG, "saveTempValue: huang updateMediaController start register ==>")
                        mMediaListener.startCallbacks()
                    } else if (pkg.contains("localmusic")
                        || pkg.contains("/system/bin/gocsdk")
                    ) {
                        mMediaListener.removeCallbacks()
                    }
                }
            }
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

                uri?.let {
                    val updateArtUriBundle = Bundle()
                    updateArtUriBundle.putString(
                        AwellTool.STATUS_ACCEPT,
                        MusicWidget.OTHER_MUSIC_PLAY_IMAGE
                    )
                    updateArtUriBundle.putString(AwellTool.VALUE_M1, uri.toString())
                    notifyClientDataChanged(updateArtUriBundle)
                }

            } catch (e: CancellationException) {
                //任务取消
            } catch (e: Exception) {
                //任务异常
                Log.i(TAG, "handleLocalMusicImageByScope: huang error=${e}")
            }
        }
    }

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

        val trackInfo = TrackInfo(
            null,
            songId,
            albumId,
            filePath,
            metadata = when {
                //酷我音乐在后台，切换本地音乐播放，MediaController还持有metadata
                //如果本地音乐这时候获取图片会直接拿到metadata的图片，需要使用传递过来的VALUE_M3判断
                //useMetadata为null代表本地音乐发送图片处理请求，不在metadata对象获取专辑图片
                artUri != null -> {
                    val originMetadata = mMediaListener.getmMediaController()?.metadata;
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
        val count = listeners.beginBroadcast()
        try {
            for (i in 0..<count) {
                val listener: IDataChangeInterface = listeners.getBroadcastItem(i)
                listener.onDataChanged(bundle)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        } finally {
            listeners.finishBroadcast()
        }
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
}


@Parcelize
data class MusicPlayInfo(
    val songName: String,
    val singerName: String,
    val album: String
) : Parcelable