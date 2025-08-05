package com.awell.impl

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import com.awell.launcher.IDataChangeInterface
import com.awell.launcher.IHostPluginInterface
import com.awell.launcher2.LauncherApplication.mAppContext
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellLibrary
import com.awell.library.AwellTool


class HostToPluginService : Service() {

    private val TAG: String = "HostToPluginService"
    val mNullStr = "null"

    var mSongName: String? = null

    var mMusicPlayInfo: MusicPlayInfo? = null

    val listeners = RemoteCallbackList<IDataChangeInterface>()
    private var mMediaListener = MediaNotificationListener()

    val mediaLibrary = AwellLibrary(AwellTool.OPEN)
    val mDataListener = AwellLibrary.OnDataListener { bundle: Bundle? ->
        bundle?.let {
            notifyDataChanged(bundle)
            saveTempValue(bundle)
        } ?: run {
            Log.e(TAG, "AwellLibrary.OnDataListener onResult:  bundle is null!!")
        }
    }

    init {
        mMediaListener.initDependencies(mAppContext)
        mediaLibrary.init(mAppContext)
        mediaLibrary.setOnDataListener(mDataListener)
        Log.i(TAG, "huang init awell media control mAppContext=>${mAppContext}")
    }

    override fun onBind(intent: Intent): IBinder {
        Log.i(TAG, "onBind: huang bind ==>${intent}")
        return mBinder
    }

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


        override fun registerListener(listener: IDataChangeInterface) {
            Log.i(TAG, "registerListener: huang register listener musicPlayInfo==>${mMusicPlayInfo}")
            listeners.register(listener)
            mMusicPlayInfo?.let {
                notifyDataChanged(musicPlayInfoToBundle(mMusicPlayInfo!!))
            }

        }

        override fun unregisterListener(listener: IDataChangeInterface) {
            listeners.unregister(listener)
        }
    }

    /**
     * 服务端保留一份数据
     * 当客户端切换时，
     * 可以更新到客户端的数据显示
     */
    private fun saveTempValue(bundle: Bundle) {
        val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)
        when (status) {
            AwellTool.MUSIC.PLAY_NAME -> {
                mMusicPlayInfo = bundleToMusicPlayInfo(bundle)
            }
        }
    }
    private fun notifyDataChanged(bundle: Bundle) {

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