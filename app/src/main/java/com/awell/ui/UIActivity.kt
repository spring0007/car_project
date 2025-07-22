package com.awell.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.ctrlview.FrequencyTextView
import com.awell.ctrlview.MusicWidget
import com.awell.ctrlview.VisualizerView
import com.awell.launcher.R
import com.awell.launcher.databinding.ActivityUiactivityBinding
import com.awell.launcher2.LauncherApplication
import com.awell.launcher2.LauncherApplication.getmAppContext
import com.awell.launcher2.MediaNotificationListener
import com.awell.model.MediaDataSelect
import com.awell.model.MediaViewModel
import com.awell.model.PlayImage
import com.awell.model.PlayInfo
import com.awell.model.PlayStatus
import com.awell.model.PlayTime
import com.awell.model.RadioInfo
import com.awell.utils.CommonData

class UIActivity : Activity(), View.OnClickListener, View.OnLongClickListener, LifecycleOwner {

    private val TAG = UIActivity::class.simpleName.toString()
    private lateinit var binding: ActivityUiactivityBinding
    private var mMediaListener = MediaNotificationListener()
    lateinit var llMusic: MusicWidget
    lateinit var mediaControl: AwellMediaControl
    lateinit var mediaViewModel: MediaViewModel
    lateinit var mWaveformView: VisualizerView
    lateinit var tvRadioButAFM: Button
    lateinit var tv_radio_freq: FrequencyTextView
    lateinit var tv_radio_am_fm: TextView
    lateinit var tv_radio_freq_unit: TextView
    private lateinit var lifecycleRegistry: LifecycleRegistry
    var thisActivity = this


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUiactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        initMediaMusic()
        binding.hotsetAllapp.setOnClickListener(this)

        findViewId()

        initMediaObserverView()
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }


    @CallSuper
    override fun onPause() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        super.onPause()
    }

    override fun onResume() {
        if (!AppsCustomizeControl.longClickUninstallAppToAppsView) {
            AppsCustomizeControl.hideApps()
        }
        super.onResume()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

    }

    @CallSuper
    override fun onStop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        mediaControl.mediaLibrary.release()
        mMediaListener.cleanup()
    }


    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private fun initMediaMusic() {

        mMediaListener.initDependencies(baseContext)
        mediaControl = AwellMediaControl()
        mediaViewModel =
            ViewModelProvider((getmAppContext() as? LauncherApplication?)!!).get<MediaViewModel>(
                MediaViewModel::class.java
            )

        llMusic = findViewById(R.id.music_widget_layout)
        llMusic.setMediaLibrary(mediaControl.mediaLibrary)
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


    private fun initMediaObserverView() {

        mediaViewModel.mediaState.observe(this, Observer { mediaDataSelect: MediaDataSelect ->
            llMusic.switchMediaController(
                mediaDataSelect.packName,
                mediaDataSelect.status,
                mediaDataSelect.mediaType,
                mediaDataSelect.curMedia
            )
            if ("com.awell.radio" == mediaDataSelect.packName) {
                if ("start" == mediaDataSelect.status) {
                    mWaveformView.startAnimation()
                } else if ("stop" == mediaDataSelect.status) {
                    mWaveformView.stopAnimation()
                }
            }
        })

        mediaViewModel.playStatus.observe(this, Observer { playStatus: PlayStatus ->
            llMusic.setCurMusicState(playStatus.status, playStatus.playAppType)
        })

        mediaViewModel.playInfo.observe(this, Observer { playInfo: PlayInfo ->
            llMusic.setMusicNameTextView(playInfo.songName, playInfo.appType)
            llMusic.setArtistNameTextView(playInfo.singerName, playInfo.appType)
            if ("NO_MUSIC_LIST" == playInfo.songName
                && "NO_MUSIC_LIST" == playInfo.singerName
                && "NO_MUSIC_LIST" == playInfo.album
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

            if (MusicWidget.OTHER_MUSIC == playInfo.appType) {
                if (!TextUtils.isEmpty(playInfo.songName)) {
                    llMusic.setMusicNameTextView(playInfo.songName, MusicWidget.OTHER_MUSIC)
                } else {
                    llMusic.setMusicNameTextView(
                        getResources().getString(R.string.click_play_music),
                        MusicWidget.OTHER_MUSIC
                    )
                }
                if (!TextUtils.isEmpty(playInfo.singerName)) {
                    llMusic.setArtistNameTextView(
                        playInfo.singerName,
                        MusicWidget.OTHER_MUSIC
                    )
                } else {
                    llMusic.setArtistNameTextView(
                        getResources().getString(R.string.music_artist),
                        MusicWidget.OTHER_MUSIC
                    )
                }
            }
        })

        mediaViewModel.playTime.observe(this, Observer<PlayTime> { playTime ->
            llMusic.setMusicSeekBar(
                playTime.currentTime.toInt(),
                playTime.totalTime.toInt(),
                playTime.playType
            )
        })

        mediaViewModel.playImage.observe(this, Observer<PlayImage> { playImage ->
            llMusic.setPlayImage(playImage.songId, playImage.albumId)
        })

        mediaViewModel.radioInfo.observe(this, Observer<RadioInfo> { radioInfo ->
            tv_radio_am_fm.text = radioInfo.radioType
            tvRadioButAFM.text = radioInfo.radioType
            tv_radio_freq.text = radioInfo.freq
            tv_radio_freq_unit.text = radioInfo.unit
        })
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
                AppsCustomizeControl.showApps(thisActivity)

            } else if (action == "CANBUS_CHANGE_SPEED_Unit") {
                //updateSpeedUnitText()
            } else if ("top_session_package_change" == action) {
                val sessionTopPkg = intent.getStringExtra("top_package")
                handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                Log.d(TAG, "88888-top_session_package_change:$sessionTopPkg")
            }
        }
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
            //Log.i(TAG, "0000----Switched to new package: " + value1);
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.hotset_allapp -> {
                AppsCustomizeControl.showApps(this)
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AppsCustomizeControl.hideApps()
    }

    override fun onNewIntent(intent: Intent?) {
        AppsCustomizeControl.hideApps()
        super.onNewIntent(intent)
    }

    override fun onLongClick(v: View?): Boolean {
        Log.i(TAG, "onLongClick: huang v=>${v}")
        return false;
    }

}