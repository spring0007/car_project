package com.example.launcher_plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.ctrlview.FrequencyTextView
import com.awell.ctrlview.MusicWidget
import com.awell.ctrlview.VisualizerView
import com.awell.launcher2.LauncherApplication
import com.awell.launcher2.MediaNotificationListener
import com.awell.utils.CommonData
import com.example.launcher_plugin.databinding.ActivityUiactivityBinding


class UIActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName.toString()
    private lateinit var binding: ActivityUiactivityBinding
    private var mMediaListener = MediaNotificationListener()
    lateinit var llMusic: MusicWidgetPlugin
    lateinit var mediaControl: AwellMediaControl
    lateinit var mWaveformView: VisualizerView
    lateinit var tvRadioButAFM: Button
    lateinit var tv_radio_freq: FrequencyTextView
    lateinit var tv_radio_am_fm: TextView
    lateinit var tv_radio_freq_unit: TextView


    var thisActivity = this


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (LauncherApplication.getmAppContext() as LauncherApplication).getModel()
            .startLoader(true, -1)

        binding = ActivityUiactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.hotsetAllapp.setOnClickListener(this)

        findViewId()
        initMediaMusic()

    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaListener.cleanup()
    }


    private fun initMediaMusic() {

        mMediaListener.initDependencies(baseContext)
        mediaControl = AwellMediaControl()
        mediaControl.bindDataService(this)
        mediaControl.updateMusicView = mediaImpl

        llMusic = findViewById<MusicWidgetPlugin>(R.id.music_widget_layout)

        llMusic.setMediaLibrary(mediaControl)
        llMusic.setActivity(this, llMusic)
    }

    private fun findViewId() {
        mWaveformView = findViewById<VisualizerView>(R.id.waveformView)
        mWaveformView.setOnClickListener(this)

        tv_radio_am_fm = findViewById<TextView>(R.id.tv_radio_am_fm)
        tv_radio_am_fm.setOnClickListener(this)

        tvRadioButAFM = findViewById<Button>(R.id.iv_radio_setFM)
        tvRadioButAFM.setOnClickListener(this)

        tv_radio_freq = findViewById<FrequencyTextView>(R.id.tv_radio_freq)
        tv_radio_freq.setOnClickListener(this)

        tv_radio_freq_unit = findViewById<TextView>(R.id.tv_radio_freq_unit)
    }

    private val mediaImpl: AwellMediaControl.UpdateMediaDataToView =
        object : AwellMediaControl.UpdateMediaDataToView {
            override fun updateViewMusicPlay(
                bundle: Bundle,
                pkg: String,
                command: String,
                mediaType: Int,
                currentMedia: Int
            ) {
                runOnUiThread {
                    llMusic.switchMediaController(
                        pkg,
                        command,
                        mediaType,
                        currentMedia
                    )
                    if ("com.awell.radio" == pkg) {
                        if ("start" == command) {
                            mWaveformView.startAnimation()
                        } else if ("stop" == command) {
                            mWaveformView.stopAnimation()
                        }
                    }
                }
            }

            override fun updateViewPlayStatus(
                bundle: Bundle,
                status: Boolean,
                type: Int
            ) {
                runOnUiThread {
                    llMusic.setCurMusicState(status, type)
                }
            }

            override fun updateViewMusicPlayImage(
                bundle: Bundle,
                songId: Long,
                albumId: Long
            ) {
                runOnUiThread {
                    llMusic.setPlayImage(songId, albumId)
                }
            }

            override fun updateViewPlayInfo(
                bundle: Bundle,
                songName: String,
                singerName: String,
                album: String,
                type: Int
            ) {
                runOnUiThread {
                    llMusic.setMusicNameTextView(songName, type)
                    llMusic.setArtistNameTextView(singerName, type)
                    if ("NO_MUSIC_LIST" == songName
                        && "NO_MUSIC_LIST" == singerName
                        && "NO_MUSIC_LIST" == album
                    ) {
                        llMusic.setMusicNameTextView(
                            getResources().getString(R.string.click_play_music),
                            MusicWidget.MUSIC
                        )
                        llMusic.setArtistNameTextView(
                            getResources().getString(R.string.music_artist),
                            MusicWidget.MUSIC
                        )
                    }

                    if (MusicWidget.OTHER_MUSIC == type) {
                        if (!TextUtils.isEmpty(songName)) {
                            llMusic.setMusicNameTextView(songName, MusicWidget.OTHER_MUSIC)
                        } else {
                            llMusic.setMusicNameTextView(
                                getResources().getString(R.string.click_play_music),
                                MusicWidget.OTHER_MUSIC
                            )
                        }
                        if (!TextUtils.isEmpty(singerName)) {
                            llMusic.setArtistNameTextView(
                                singerName,
                                MusicWidget.OTHER_MUSIC
                            )
                        } else {
                            llMusic.setArtistNameTextView(
                                getResources().getString(R.string.music_artist),
                                MusicWidget.OTHER_MUSIC
                            )
                        }
                    }
                }
            }

            override fun updateViewPlayTime(
                bundle: Bundle,
                currentTime: Long,
                totalTime: Long,
                type: Int
            ) {
                runOnUiThread {
                    llMusic.setMusicSeekBar(
                        currentTime.toInt(),
                        totalTime.toInt(),
                        type
                    )
                }
            }

            override fun updateViewRadioFreq(
                bundle: Bundle,
                fmOrAm: String,
                freq: String,
                unit: String
            ) {
                runOnUiThread {
                    tv_radio_am_fm.text = fmOrAm
                    tvRadioButAFM.text = fmOrAm
                    tv_radio_freq.text = freq
                    tv_radio_freq_unit.text = unit
                }
            }
        }

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
                AppsCustomizeControl.showApps(thisActivity.findViewById<ViewGroup>(android.R.id.content))

            } else if (action == "CANBUS_CHANGE_SPEED_Unit") {
                //updateSpeedUnitText()
            } else if ("top_session_package_change" == action) {
                val sessionTopPkg = intent.getStringExtra("top_package")
                handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
            }
        }
    }


    fun handleMediaPlaybackResult(value1: String, value2: String, value3: Int, value4: Int) {

        val oldPlayingPackage = mMediaListener.currentPlayingPackage
        val isStartCommand = "start" == value2
        val isStopCommand = "stop" == value2
        val isValidPackage = !TextUtils.isEmpty(value1)
        // 处理本地音乐的特殊情况
        if (isValidPackage && (value1.contains("localmusic") || value1.contains("com.awell.bluetooth") || value1.contains(
                "/system/bin/gocsdk"
            )) && isStartCommand
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
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.hotset_allapp -> {
                AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AppsCustomizeControl.hideApps()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        AppsCustomizeControl.hideApps()
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return super.dispatchKeyEvent(event)
    }
}