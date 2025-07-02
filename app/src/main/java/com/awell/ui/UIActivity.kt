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
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.ctrlview.MusicWidget
import com.awell.launcher.R
import com.awell.launcher.databinding.ActivityUiactivityBinding
import com.awell.launcher2.MediaNotificationListener
import com.awell.utils.CommonData

class UIActivity : Activity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName.toString()
    private lateinit var binding: ActivityUiactivityBinding
    private var mMediaListener = MediaNotificationListener()
    lateinit var llMusic: MusicWidget


    var thisActivity = this


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
        Log.i(TAG, "onCreate: huang set media library =>${AwellMediaControl.mediaLibrary}")
        llMusic.setMediaLibrary(AwellMediaControl.mediaLibrary)
        llMusic.setActivity(this, llMusic)


        binding.hotsetAllapp.setOnClickListener(this)


    }


    override fun onDestroy() {
        super.onDestroy()
        mMediaListener.cleanup()
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

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume: huang ==>")
        AppsCustomizeControl.hideApps()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.i(TAG, "onBackPressed: huang ==>")
        AppsCustomizeControl.hideApps()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i(TAG, "onNewIntent: huang ==>")
        AppsCustomizeControl.hideApps()
    }
}