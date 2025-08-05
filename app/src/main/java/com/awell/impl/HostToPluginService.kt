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
import androidx.lifecycle.ViewModelProvider
import com.awell.launcher.IDataChangeInterface
import com.awell.launcher.IHostPluginInterface
import com.awell.launcher2.LauncherApplication
import com.awell.launcher2.LauncherApplication.mAppContext
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellLibrary
import com.awell.library.AwellTool
import com.awell.model.MediaViewModel


class HostToPluginService : Service() {

    private val TAG: String = "HostToPluginService"
    val mNullStr = "null"

    var mSongName: String? = null

    val listeners = RemoteCallbackList<IDataChangeInterface>()
    private var mMediaListener = MediaNotificationListener()

    val mediaLibrary = AwellLibrary(AwellTool.OPEN)
    val mDataListener = AwellLibrary.OnDataListener { bundle: Bundle? ->
        bundle?.let {
            notifyDataChanged(bundle)
            val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)
            when (status) {
                AwellTool.MUSIC.PLAY_NAME -> {
                    val songNameTemp = bundle.getString(AwellTool.VALUE_M1, mNullStr)
                    val singerName = bundle.getString(AwellTool.VALUE_M2, mNullStr)
                    val album = bundle.getString(AwellTool.VALUE_M3, mNullStr)
                    mSongName = songNameTemp
                }
            }
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
            Log.i(TAG, "registerListener: huang register listener ==>")
            listeners.register(listener)


        }

        override fun unregisterListener(listener: IDataChangeInterface) {
            listeners.unregister(listener)
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
}


@Parcelize
data class MusicInfo(
    val id: String,
    val value: String,
    val timestamp: Long
) : Parcelable