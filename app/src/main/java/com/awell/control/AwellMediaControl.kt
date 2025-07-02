package com.awell.control

import android.os.Bundle
import android.util.Log
import com.awell.ctrlview.MusicWidget
import com.awell.launcher2.LauncherApplication.mAppContext
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellLibrary
import com.awell.library.AwellTool

object AwellMediaControl {

    private val TAG = AwellMediaControl::class.simpleName
    val mNullStr = "Null"
    private var mMediaListener = MediaNotificationListener()
    private var updateMusicView: UpdateMediaDataToView? = null

    val mediaLibrary = AwellLibrary(AwellTool.OPEN)

    val mDataListener = AwellLibrary.OnDataListener { bundle: Bundle? ->
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

    init {
        mMediaListener.initDependencies(mAppContext)

        mediaLibrary.init(mAppContext)
        mediaLibrary.setOnDataListener(mDataListener)

    }

    private fun handleMediaPlay(bundle: Bundle) {

        val pkg = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val command = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val mediaType = bundle.getInt(AwellTool.VALUE_M3, 3)
        val currentMedia = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.MUSIC)
        val isStartCommand = "start" == command
        val isLocalMusicPackage =
            (pkg.contains("localmusic")
                    || pkg.contains("com.awell.bluetooth")
                    || pkg.contains("/system/bin/gocsdk"))

        mMediaListener.let {
            if (isLocalMusicPackage && isStartCommand)
                it.removeCallbacks()
        }
        updateMusicView?.updateViewMusicPlay(bundle, pkg, command, mediaType, currentMedia)
    }

    private fun handleMusicPlayStatus(bundle: Bundle) {
        val musicStatus = bundle.getBoolean(AwellTool.VALUE_M1)
        updateMusicView?.updateViewMusicPlayStatus(bundle, musicStatus)
    }

    private fun handleMusicPlayName(bundle: Bundle) {
        val songName = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val singerName = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val album = bundle.getString(AwellTool.VALUE_M3, mNullStr)
        updateMusicView?.updateViewMusicPlayName(bundle, songName, singerName, album)

    }

    private fun handleMusicPlayImage(bundle: Bundle) {
        val value1 = bundle.getString(AwellTool.VALUE_M1, "0 , 0")
        val str = value1.split(" , ".toRegex()).dropLastWhile {
            it.isEmpty()
        }.toTypedArray()
        val long1 = str[0].toLong()
        val long2 = str[1].toLong()
        updateMusicView?.updateViewMusicPlayImage(bundle, long1, long2)

    }

    private fun handleMusicPlayTime(bundle: Bundle) {
        val currentTime = bundle.getLong(AwellTool.VALUE_M1)
        val totalTime = bundle.getLong(AwellTool.VALUE_M2)
        updateMusicView?.updateViewMusicPlayTime(bundle, currentTime, totalTime)
    }

    private fun handleBTPlayStatus(bundle: Bundle) {
        val status = bundle.getBoolean(AwellTool.VALUE_M1)
        updateMusicView?.updateViewBTPlayStatus(bundle, status)

    }

    private fun handleBTPlayName(bundle: Bundle) {

        val songName = bundle.getString(AwellTool.VALUE_M1, mNullStr)
        val singerName = bundle.getString(AwellTool.VALUE_M2, mNullStr)
        val album = bundle.getString(AwellTool.VALUE_M3, mNullStr)
        updateMusicView?.updateViewBTPlayName(bundle, songName, singerName, album)

    }

    private fun handleBTPlayTime(bundle: Bundle) {
        val currentTime = (bundle.getInt(AwellTool.VALUE_M1) * 1000).toLong()
        val totalTime = (bundle.getInt(AwellTool.VALUE_M2) * 1000).toLong()
        updateMusicView?.updateViewBTPlayTime(bundle, currentTime, totalTime)
    }

    private fun handleRadioFreq(bundle: Bundle) {
        val fmOrAm = bundle.getString(AwellTool.VALUE_M1) ?: mNullStr
        val freq = bundle.getString(AwellTool.VALUE_M2) ?: mNullStr
        val unit = bundle.getString(AwellTool.VALUE_M3) ?: mNullStr
        updateMusicView?.updateViewRadioFreq(bundle, fmOrAm, freq, unit)
    }

    private fun handleOtherMusicPlayName(bundle: Bundle) {
        val songName = bundle.getString(AwellTool.VALUE_M1) ?: mNullStr
        val singerName = bundle.getString(AwellTool.VALUE_M2) ?: mNullStr
        val album = bundle.getString(AwellTool.VALUE_M3) ?: mNullStr
        updateMusicView?.updateViewOtherMusicPlayName(bundle, songName, singerName, album)
    }

    private fun handleOtherMusicStatus(bundle: Bundle) {
        val musicStatus = bundle.getBoolean(AwellTool.VALUE_M1)
        updateMusicView?.updateViewOtherMusicPlayStatus(bundle, musicStatus)
    }

    private fun handleOtherMusicTime(bundle: Bundle) {
        val currentTime = bundle.getLong(AwellTool.VALUE_M1)
        val totalTime = bundle.getLong(AwellTool.VALUE_M1)
        updateMusicView?.updateViewOtherMusicTime(bundle, currentTime, totalTime)
    }

    interface UpdateMediaDataToView {

        fun updateViewMusicPlay(
            bundle: Bundle,
            pkg: String,
            command: String,
            mediaType: Int,
            currentMedia: Int
        )

        /**
         * true playing
         * false not playing
         */
        fun updateViewMusicPlayStatus(bundle: Bundle, status: Boolean)

        /**
         * songName 歌曲名
         * singerName 歌手名
         * album 专辑
         */
        fun updateViewMusicPlayName(
            bundle: Bundle, songName: String, singerName: String, album: String
        )

        fun updateViewMusicPlayImage(bundle: Bundle, value1: Long, value2: Long)
        fun updateViewMusicPlayTime(
            bundle: Bundle, currentTime: Long, totalTime: Long
        )

        fun updateViewBTPlayStatus(bundle: Bundle, status: Boolean)
        fun updateViewBTPlayName(
            bundle: Bundle, songName: String, singerName: String, album: String
        )

        fun updateViewBTPlayTime(bundle: Bundle, currentTime: Long, totalTime: Long)

        /**
         * fmOrAm FM or AM
         * freq 当前频率  87.5 or 531
         * unit 单位 MHz or KHz
         */
        fun updateViewRadioFreq(
            bundle: Bundle, fmOrAm: String, freq: String, unit: String
        )

        fun updateViewOtherMusicPlayName(
            bundle: Bundle, songName: String, singerName: String, album: String
        )

        fun updateViewOtherMusicPlayStatus(bundle: Bundle, status: Boolean)
        fun updateViewOtherMusicTime(bundle: Bundle, currentTime: Long, totalTime: Long)
    }
}