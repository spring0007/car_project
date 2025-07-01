package com.awell.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.awell.ctrlview.MusicWidget
import com.awell.launcher.R
import com.awell.launcher.databinding.ActivityUiactivityBinding
import com.awell.launcher2.Launcher
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellLibrary
import com.awell.library.AwellLibrary.OnDataListener
import com.awell.library.AwellTool
import com.awell.utils.CommonData

class UIActivity : Activity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName.toString()
    private lateinit var binding: ActivityUiactivityBinding
    private var mMediaListener = MediaNotificationListener()
    lateinit var llMusic: MusicWidget


    lateinit var mediaLibrary: AwellLibrary


    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = intent.action
            Log.i(TAG, "mainReceiver:$action")
            if (action == CommonData.BROADCAST_LAMP_SWITCH) {

//                if (intent.getIntExtra(
//                        "lamplet_state",
//                        0
//                    ) == 1
//                ) ivLampSwitchBg.setImageResource(R.drawable.open)
//                else ivLampSwitchBg.setImageResource(R.drawable.off)

            } else if (action == CommonData.ACTION_ACC_ON) {
//                if (ivLampSwitchBg != null)
//                    ivLampSwitchBg.postDelayed(Runnable {
//                        accRecor = false
//                    }, (8 * 1000).toLong())
            } else if (action == CommonData.ACTION_ACC_OFF) {
                //accRecor = true
            } else if (action == CommonData.BROADCAST_MEDIA_EXIT) {
                val packge = intent.getStringExtra("package")
                if (packge != null && (packge == "cn.kuwo.kwmusiccar" || packge == "exitAll")) {
                }
            } else if ("com.zjinnova.zlink" == action) {
                val zlinkStatus = intent.getStringExtra("status")
                val phoneMode = intent.getStringExtra("phoneMode")
                Log.d(TAG, "zlinkStatus:$zlinkStatus")
                if (zlinkStatus == null) {
                    return
                }
                llMusic.getCarPlayData(zlinkStatus, phoneMode)
            } else if (action == "android.launcher.show.allApp") {
                Log.d(TAG, "mainReceiver:" + intent.action)
                //showAllApps(true)
                //setSettingOrAndroidPage(true)
                //mModel.startLoader(true, -1)
            } else if (action == "CANBUS_CHANGE_SPEED_Unit") {
                //updateSpeedUnitText()
            } else if ("top_session_package_change" == action) {
                val sessionTopPkg = intent.getStringExtra("top_package")
                handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                Log.d(TAG, "88888-top_session_package_change:$sessionTopPkg")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUiactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mMediaListener.initDependencies(baseContext)


        llMusic = findViewById(R.id.music_widget_layout)
        llMusic.setActivity(this, llMusic)

        mediaLibrary = AwellLibrary(AwellTool.OPEN)
        mediaLibrary.init(this)
        mediaLibrary.setOnDataListener(mAwellLibraryDataListener)

        binding.hotsetAllapp.setOnClickListener(this)

    }


    override fun onDestroy() {
        super.onDestroy()
        mMediaListener.cleanup()
    }

    fun handleMediaPlaybackResult(value1: String, value2: String, value3: Int, value4: Int) {

        val oldPlayingPackage = mMediaListener.currentPlayingPackage
        val isStartCommand = "start" == value2
        val isStopCommand = "stop" == value2
        val isValidPackage = !TextUtils.isEmpty(value1)
        Log.i(
            TAG,
            "handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:value1=$value1 --oldPlayingPackage=$oldPlayingPackage--value2=$value2"
        )
        Log.i(
            TAG,
            "handleMediaPlaybackResult-- MUSIC_MEDIA_PLAY:isValidPackage=$isValidPackage --isStartCommand=$isStartCommand-isStopCommand=$isStopCommand"
        )
        // 处理本地音乐的特殊情况
        if (isValidPackage && (value1.contains("localmusic")
                    || value1.contains("com.awell.bluetooth")
                    || value1.contains("/system/bin/gocsdk"))
            && isStartCommand
        ) {
            mMediaListener.removeCallbacks()
            return
        }

        // 处理停止播放的情况
        //if (isValidPackage && isStopCommand) {
        //    mMediaListener.setCurrentPlayingPackage(null);
        //    return;
        //}

        // 处理开始播放的情况
        if (isValidPackage && isStartCommand) {
            // 当前没有播放或切换到新包时，更新并启动回调
            if (oldPlayingPackage != null && oldPlayingPackage != value1) {
                mMediaListener.togglePause() //有些播放器未暂停，手动暂停
                mMediaListener.removeCallbacks()
            }

            mMediaListener.setPlayingPackage(value1)
            mMediaListener.startCallbacks()
            //Log.i(TAG, "0000----Switched to new package: " + value1);
        }
    }

    private val mAwellLibraryDataListener =
        OnDataListener { bundle ->
            llMusic.post {
                if (bundle == null)
                    return@post
                val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)
                Log.i(Launcher.TAG, "onResult---status = $status  $bundle")
                when (status) {
                    AwellTool.MEDIA_PLAY -> {
                        val value1 = bundle.getString(AwellTool.VALUE_M1)
                        val value2 = bundle.getString(AwellTool.VALUE_M2)
                        val value3 = bundle.getInt(AwellTool.VALUE_M3, 3)
                        val value4 = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.MUSIC)
                        //if(value1!=null&&!value1.contains("localmusic")&&"start".equals(value2)){
                        //    value4=MusicWidget.OTHER_MUSIC;
                        //}
                        Log.i(
                            Launcher.TAG,
                            "onResult---MUSIC_MEDIA_PLAY : $value1 $value2 $value3 $value4"
                        )
                        Log.i(
                            Launcher.TAG,
                            "onResult---MUSIC_MEDIA_PLAY : " + Launcher.mMediaListener.currentPlayingPackage
                        )
                        //handleMediaPlaybackResult(value1,value2,value3,value4);
                        val isStartCommand = "start" == value2
                        val isStopCommand = "stop" == value2
                        val isValidPackage = !TextUtils.isEmpty(value1)
                        val isLocalMusicPackage =
                            (value1!!.contains("localmusic") || value1.contains("com.awell.bluetooth") || value1.contains(
                                "/system/bin/gocsdk"
                            ))
                        if (isValidPackage && Launcher.mMediaListener != null) {
                            if (isLocalMusicPackage && isStartCommand) {
                                Launcher.mMediaListener.removeCallbacks()
                            } else if (!isLocalMusicPackage
                                && value1 == Launcher.mMediaListener.currentPlayingPackage
                                && isStopCommand
                                && llMusic != null
                            ) {
                                llMusic.setCurMusicState(false, MusicWidget.OTHER_MUSIC)
                            }
                        }
                        llMusic.switchMediaController(value1, value2, value3, value4)
                        if ("com.awell.radio" == value1) {
                            if ("start" == value2) {
                                //mWaveformView.startAnimation()
                            } else {
                                //mWaveformView.stopAnimation()
                            }
                        }
                    }

                    AwellTool.MUSIC.PLAY_STATUS -> {
                        // 音乐监听 PLAY_STATUS 返回一个参数
                        // VALUE_M1 = (boolean)音乐播放状态 true 播放 false 没播放
                        val musicStatus = bundle.getBoolean(AwellTool.VALUE_M1)
                        Log.i(Launcher.TAG, "onResult---MUSIC_PLAY_STATUS : $musicStatus")
                        if (llMusic != null) {
                            llMusic.setCurMusicState(musicStatus, MusicWidget.MUSIC)
                        }
                    }

                    AwellTool.MUSIC.PLAY_NAME -> {
                        // 音乐监听 PLAY_NAME 返回三个参数
                        // VALUE_M1 = (String)歌曲名称
                        // VALUE_M2 = (String)歌手名称
                        // VALUE_M3 = (String)专辑
                        val value1 = bundle.getString(AwellTool.VALUE_M1)
                        val value2 = bundle.getString(AwellTool.VALUE_M2)
                        val value3 = bundle.getString(AwellTool.VALUE_M3)
                        val value4 = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.MUSIC)
                        Log.i(
                            Launcher.TAG,
                            "onResult---MUSIC_PLAY_NAME : $value1 $value2 $value3"
                        )
                        if (llMusic != null) {
                            llMusic.setMusicNameTextView(value1, MusicWidget.MUSIC)
                            llMusic.setArtistNameTextView(value2, MusicWidget.MUSIC)
                            if ("NO_MUSIC_LIST" == value1
                                && "NO_MUSIC_LIST" == value2
                                && "NO_MUSIC_LIST" == value3
                            ) {
                                llMusic.setMusicNameTextView(
                                    resources.getString(R.string.click_play_music),
                                    MusicWidget.MUSIC
                                )
                                llMusic.setArtistNameTextView(
                                    resources.getString(R.string.music_artist),
                                    MusicWidget.MUSIC
                                )
                            }
                        }
                    }

                    AwellTool.MUSIC.PLAY_IMAGE -> {
                        val value1 = bundle.getString(AwellTool.VALUE_M1)
                        Log.i(Launcher.TAG, "onResult---MUSIC_PLAY_IMAGE $value1")
                        val strs = value1!!.split(" , ".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        val long1 = strs[0].toLong()
                        val long2 = strs[1].toLong()
                        llMusic.setPlayImage(long1, long2)
                    }

                    AwellTool.MUSIC.PLAY_TIME -> {
                        // 音乐监听 PLAY_TIME 返回两个参数
                        // VALUE_M1 = (long)当期时间 单位毫秒
                        // VALUE_M2 = (long)总时间 单位毫秒
                        val value1 = bundle.getLong(AwellTool.VALUE_M1)
                        val value2 = bundle.getLong(AwellTool.VALUE_M2)
                        Log.i(Launcher.TAG, "onResult---MUSIC_PLAY_TIME : $value1-$value2")
                        if (llMusic != null) {
                            llMusic.setMusicSeekBar(
                                value1.toInt(),
                                value2.toInt(),
                                MusicWidget.MUSIC
                            )
                        }
                    }

                    AwellTool.BT.PLAY_STATUS -> {
                        // 蓝牙监听 PLAY_STATUS 返回一个参数
                        val musicStatus = bundle.getBoolean(AwellTool.VALUE_M1)
                        Log.i(Launcher.TAG, "onResult---BT_PLAY_STATUS : $musicStatus")
                        if (llMusic != null) {
                            llMusic.setCurMusicState(musicStatus, MusicWidget.BT)
                        }
                    }

                    AwellTool.BT.PLAY_NAME -> {
                        // 音乐监听 PLAY_NAME 返回三个参数
                        // VALUE_M1 = (String)歌曲名称
                        // VALUE_M2 = (String)歌手名称
                        // VALUE_M3 = (String)专辑
                        val value1 = bundle.getString(AwellTool.VALUE_M1)
                        val value2 = bundle.getString(AwellTool.VALUE_M2)
                        val value3 = bundle.getString(AwellTool.VALUE_M3)
                        Log.i(
                            Launcher.TAG,
                            "onResult---BT_PLAY_NAME : $value1 $value2 $value3"
                        )
                        if (llMusic != null) {
                            llMusic.setMusicNameTextView(value1, MusicWidget.BT)
                            llMusic.setArtistNameTextView(value2, MusicWidget.BT)
                        }
                    }

                    AwellTool.BT.PLAY_TIME -> {
                        // 音乐监听 PLAY_TIME 返回两个参数
                        // VALUE_M1 = (long)当期时间 单位秒
                        // VALUE_M2 = (long)总时间 单位秒
                        val value1 = (bundle.getInt(AwellTool.VALUE_M1) * 1000).toLong()
                        val value2 = (bundle.getInt(AwellTool.VALUE_M2) * 1000).toLong()
                        Log.i(
                            Launcher.TAG,
                            "onResult---BT_MUSIC_PLAY_TIME : $value1-$value2"
                        )
                        if (llMusic != null) {
                            llMusic.setMusicSeekBar(value1.toInt(), value2.toInt(), MusicWidget.BT)
                        }
                    }

                    AwellTool.RADIO.FREQUENCY -> {
                        // 收音机监听 FREQUENCY 返回三个参数
                        // VALUE_M1 = (String)FM or AM
                        // VALUE_M2 = (String)具体频率 87.5 or 531
                        // VALUE_M3 = (String)单位 MHz or KHz
                        val value1 = bundle.getString(AwellTool.VALUE_M1)
                        val value2 = bundle.getString(AwellTool.VALUE_M2)
                        val value3 = bundle.getString(AwellTool.VALUE_M3)
                        Log.i(Launcher.TAG, "$value1 $value2 $value3")
                        //tv_radio_am_fm.setText(value1)
                        //tvRadioButAFM.setText(value1)
                        //tv_radio_freq.setText(value2)
                        //tv_radio_freq_unit.setText(value3)
                    }

                    MusicWidget.OTHER_MUSIC_PLAYNAME -> {
                        // 音乐监听 PLAY_NAME 返回三个参数
                        // VALUE_M1 = (String)歌曲名称
                        // VALUE_M2 = (String)歌手名称
                        // VALUE_M3 = (String)专辑
                        val value1 = bundle.getString(AwellTool.VALUE_M1)
                        val value2 = bundle.getString(AwellTool.VALUE_M2)
                        val value3 = bundle.getString(AwellTool.VALUE_M3)
                        val value4 = bundle.getInt(AwellTool.VALUE_M4, MusicWidget.OTHER_MUSIC)
                        Log.i(
                            Launcher.TAG,
                            "onResult---OTHER_MUSIC_PLAYNAME : $value1 $value2 $value3"
                        )
                        if (llMusic != null) {
                            val currentMedia = MusicWidget.OTHER_MUSIC

                            if ("NO_MUSIC_LIST" == value1
                                && "NO_MUSIC_LIST" == value2
                                && "NO_MUSIC_LIST" == value3
                            ) {
                                llMusic.setMusicNameTextView(
                                    resources.getString(R.string.click_play_music),
                                    currentMedia
                                )
                                llMusic.setArtistNameTextView(
                                    resources.getString(R.string.music_artist),
                                    currentMedia
                                )
                            }

                            if (value1 != null && !TextUtils.isEmpty(value1)) {
                                llMusic.setMusicNameTextView(value1, currentMedia)
                            } else {
                                llMusic.setMusicNameTextView(
                                    resources.getString(R.string.click_play_music),
                                    currentMedia
                                )
                            }
                            if (value2 != null && !TextUtils.isEmpty(value2)) {
                                llMusic.setArtistNameTextView(value2, currentMedia)
                            } else {
                                llMusic.setArtistNameTextView(
                                    resources.getString(R.string.music_artist),
                                    currentMedia
                                )
                            }
                        }
                    }

                    MusicWidget.OTHER_MUSIC_PLAYSTATUS -> {
                        // 音乐监听 PLAY_STATUS 返回一个参数
                        // VALUE_M1 = (boolean)音乐播放状态 true 播放 false 没播放
                        val musicStatus = bundle.getBoolean(AwellTool.VALUE_M1)
                        Log.i(
                            Launcher.TAG,
                            "onResult---OTHER_MUSIC_PLAYSTATUS : $musicStatus"
                        )
                        if (llMusic != null) {
                            llMusic.setCurMusicState(musicStatus, MusicWidget.OTHER_MUSIC)
                        }
                    }

                    MusicWidget.OTHER_MUSIC_TIME -> {
                        // 音乐监听 PLAY_TIME 返回两个参数
                        // VALUE_M1 = (long)当期时间 单位毫秒
                        // VALUE_M2 = (long)总时间 单位毫秒
                        val value1 = bundle.getLong(AwellTool.VALUE_M1)
                        val value2 = bundle.getLong(AwellTool.VALUE_M2)
                        Log.i(
                            Launcher.TAG,
                            "onResult---OTHER_MUSIC_TIME : $value1-$value2"
                        )
                        if (llMusic != null) {
                            llMusic.setMusicSeekBar(
                                value1.toInt(),
                                value2.toInt(),
                                MusicWidget.OTHER_MUSIC
                            )
                        }
                    }
                }
            }
        }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.hotset_allapp -> {
                Log.i(TAG, "onClick: huang click all apps =>")
                AppsCustomizeControl.showApps(this)
            }
        }
    }

}