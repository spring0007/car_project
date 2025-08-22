package com.example.plugin_2

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresPermission
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.ctrlview.MusicWidget
import com.awell.launcher2.IconCache
import com.awell.library.AwellTool
import com.awell.utils.CommonData
import com.awell.utils.Utils
import com.awell.utils.Utils.startWallpaper
import com.example.plugin_2.databinding.SpeedLayoutBinding
import com.example.plugin_2.databinding.Ui2ActivityBinding
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class UI2Activity : Activity() {

    private val TAG = UI2Activity::class.simpleName
    private lateinit var mViewBinding: Ui2ActivityBinding
    private lateinit var mCarSpeedLayout: SpeedLayoutBinding

    //    private var mMediaListener = MediaNotificationListener()
    lateinit var llMusic: MusicWidgetPlugin2
    lateinit var mediaControl: AwellMediaControl
    lateinit var locationManager: LocationManager

    private val MSG_UPDATE_SPEED = 1
    private val MSG_CLEAR_SPEED = 2
    private val BIN_DATA_SPEED_UNIT = 0x84
    private var accRecor: Boolean? = null

    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false

    var roadFrames: IntArray = intArrayOf(
        R.drawable.rainbow_01,
        R.drawable.rainbow_02,
        R.drawable.rainbow_03,
        R.drawable.rainbow_04,
        R.drawable.rainbow_05,
        R.drawable.rainbow_06,
        R.drawable.rainbow_07,
        R.drawable.rainbow_08,
        R.drawable.rainbow_09,
        R.drawable.rainbow_10,
        R.drawable.rainbow_11,
        R.drawable.rainbow_12,
        R.drawable.rainbow_13,
        R.drawable.rainbow_14,
        R.drawable.rainbow_15,
        R.drawable.rainbow_16,
        R.drawable.rainbow_17
    )

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Utils.setPluginWallpaper(resources, R.drawable.wallpaper_00)

        Log.i(TAG, "onCreate: huang ui2 create this==>${this}")
        mViewBinding = Ui2ActivityBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)

        initView()

        initMediaMusic()

        updateSpeedUnitText()

        initTouchAndSpeedListener()

        initBroadcastReceiver()

    }

    /**
     * 初始化长按切换壁纸和监听gps速度变化
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initTouchAndSpeedListener() {
        handler = Handler(Looper.getMainLooper())
        viewConfiguration = ViewConfiguration.get(this)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates("gps", 1000, 10f, locationListener, mHandle.looper)
    }

    private fun initView() {

        mCarSpeedLayout = SpeedLayoutBinding.bind(mViewBinding.includeCarSpeed.root)
        initCarView()

        clickStartApp()

    }

    private fun initCarView() {
        mViewBinding.ivLampSwitchBg.postDelayed({ accRecor = false }, 1 * 1000)
        if (Settings.System.getInt(contentResolver, "Headlamp", 0) == 1) {
            mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.open)
        } else {
            mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.off)
        }
    }

    var lastSpeed: Float = 0F

    val mAnimator: ValueAnimator by lazy {
        ValueAnimator.ofInt(0, roadFrames.size - 1).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                mViewBinding.ivAnimSpeedRoad.setImageResource(roadFrames[animation.animatedValue as Int])
            }
        }
    }

    val mDurationAnimator: ValueAnimator by lazy {
        ValueAnimator.ofInt(0).apply {
            duration = 1000
            addUpdateListener { animation ->
                val newDuration = animation.animatedValue as Int
                mAnimator.duration = newDuration.toLong()
            }
        }
    }

    private fun updateSpeedSmoothly(targetSpeed: Float) {
        val targetDuration = speedToDuration(targetSpeed)
        mDurationAnimator.setIntValues(mAnimator.duration.toInt(), targetDuration)
        mDurationAnimator.start()
    }

    private fun stopAnimation() {
        mAnimator.cancel()
        mViewBinding.ivAnimSpeedRoad.setImageResource(roadFrames[0])
    }

    private fun speedToDuration(speed: Float): Int {
        var speed = speed
        speed = max(0f, min(120f, speed)) // 约束范围
        return (1000 - (speed * 6.6667f)).toInt() // 1000 - (speed * (1000-200)/120)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initBroadcastReceiver() {
        val filter = IntentFilter()

        filter.addAction(CommonData.BROADCAST_LAMP_SWITCH)
        filter.addAction(CommonData.ACTION_ACC_ON)
        filter.addAction(CommonData.ACTION_ACC_OFF)
        filter.addAction("com.zjinnova.zlink")
        filter.addAction("android.launcher.show.allApp")
        filter.addAction(CommonData.BROADCAST_MEDIA_EXIT)
        filter.addAction("CANBUS_CHANGE_SPEED_Unit")
        filter.addAction("top_session_package_change")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        mMediaListener.cleanup()
        mediaControl.unBindDataService(this)
        unregisterReceiver(receiver)
        cancelLongPressDetection()
    }

    /**
     * 初始化媒体信息
     * 以及绑定宿主服务
     *
     */
    private fun initMediaMusic() {

//        mMediaListener.initDependencies(baseContext)
        mediaControl = AwellMediaControl()
        mediaControl.bindDataService(this)
        mediaControl.updateMusicView = mediaImpl

        llMusic =
            findViewById<MusicWidgetPlugin2>(mViewBinding.includeWidgetMusic.musicWidgetLayout.id)

        llMusic.setMediaLibrary(mediaControl)
//        llMusic.setMediaNotificationListener(mMediaListener)
        llMusic.setActivity(this, llMusic)

    }


    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.i(TAG, "onReceive:huang action=$action")
            when (action) {
                CommonData.BROADCAST_LAMP_SWITCH -> {
                    if (intent.getIntExtra("lamplet_state", 0) == 1) {
                        mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.open)
                    } else {
                        mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.off)
                    }
                }

                CommonData.ACTION_ACC_ON -> {
                    mViewBinding.ivLampSwitchBg.postDelayed({ accRecor = false }, 8 * 1000)
                }

                CommonData.ACTION_ACC_OFF -> {
                    accRecor = true
                }

                CommonData.BROADCAST_MEDIA_EXIT -> {
                    val pkg = intent.getStringExtra("package")
                    if (pkg != null && (pkg == "cn.kuwo.kwmusiccar" || pkg == "exitAll")) {

                    }
                }

                "com.zjinnova.zlink" -> {
                    val zlinkStatus = intent.getStringExtra("status")
                    val phoneMode = intent.getStringExtra("phoneMode")
                    Log.d(TAG, "zlinkStatus:$zlinkStatus")
                    if (zlinkStatus == null) {
                        return
                    }
                    Log.i(
                        TAG,
                        "onReceive: huang mediaControl.getCurrentPkgName()=>${mediaControl.getCurrentPkgName()}"
                    )
                    if (mediaControl.getCurrentPkgName()?.equals("com.zjinnova.zlink") == true) {
                        if ("REFRESH_JEPG" == zlinkStatus) {
                            updateCarplayImageAlbum()
                        }
                        llMusic.setCarPlayData(zlinkStatus, phoneMode)
                    }
                }

                "android.launcher.show.allApp" -> {
                    AppsCustomizeControl.showApps(findViewById<ViewGroup>(android.R.id.content))
                }

                "CANBUS_CHANGE_SPEED_Unit" -> {
                    updateSpeedUnitText()
                }

                "top_session_package_change" -> {
                    val sessionTopPkg = intent.getStringExtra("top_package")
                    handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                }
            }
        }

        private fun updateCarplayImageAlbum() {
            val sdcardDir = Environment.getExternalStorageDirectory()
            val imageFile = File(sdcardDir, "cp.jpg")
            val bundle = Bundle()
            bundle.putString(
                AwellTool.STATUS_ACCEPT, MusicWidget.OTHER_MUSIC_PLAY_IMAGE
            )
            bundle.putString(AwellTool.VALUE_M4, imageFile.absoluteFile.toString())
            mediaControl.sendBundleToInternal(bundle)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSpeedUnitText() {
        val unit = ByteArray(1)
        CommonData.readDataToMeta(unit, BIN_DATA_SPEED_UNIT)
        val unitData = unit[0].toInt()
        if (unitData == 0) {
            mCarSpeedLayout.tvGpsSpeedUnit.text = "KM/h"
        } else if (unitData == 1) {
            mCarSpeedLayout.tvGpsSpeedUnit.text = "mph"
        }
    }

    fun handleMediaPlaybackResult(value1: String, value2: String, value3: Int, value4: Int) {

        val oldPlayingPackage = mediaControl.getCurrentPkgName()
        val isStartCommand = "start" == value2
        val isStopCommand = "stop" == value2
        val isValidPackage = !TextUtils.isEmpty(value1)
        // 处理本地音乐的特殊情况
        if (isValidPackage && (value1.contains("localmusic")
                    || value1.contains("com.awell.bluetooth")
                    || value1.contains("/system/bin/gocsdk")) && isStartCommand
        ) {
//            mMediaListener.removeCallbacks()
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
//                mMediaListener.togglePause() //有些播放器未暂停，手动暂停
//                mMediaListener.removeCallbacks()
            }

//            mMediaListener.setPlayingPackage(value1)
//            mMediaListener.startCallbacks()
        }
    }

    /**
     * 实现媒体回调接口
     * 更新媒体信息FM、音乐播放等
     * 歌手、播放时间、播放状态等
     */
    private val mediaImpl: AwellMediaControl.UpdateMediaDataToView by lazy {
        object : AwellMediaControl.UpdateMediaDataToView {
            override fun updateViewMusicPlay(
                bundle: Bundle, pkg: String, command: String, mediaType: Int, currentMedia: Int
            ) {
                runOnUiThread {
                    llMusic.switchMediaController(
                        pkg, command, mediaType, currentMedia
                    )
                }
            }

            override fun updateViewPlayStatus(
                bundle: Bundle, status: Boolean, type: Int
            ) {
                runOnUiThread {
                    Log.i(
                        TAG,
                        "updateViewPlayStatus: huang update play status bundle=${bundle} status=${status} type=${type}"
                    )
                    llMusic.setCurMusicState(status, type)
                }
            }

            @SuppressLint("UseKtx")
            override fun updateViewMusicPlayImage(bundle: Bundle) {
                runOnUiThread {
                    val uriStr = bundle.getString(AwellTool.VALUE_M1, null)
                    val uri = uriStr?.let {
                        Uri.parse(it).takeIf { uri -> uri.scheme != null }
                    }
                    Log.i(TAG, "updateViewMusicPlayImage: huang uri=>${uri}")
                    llMusic.loadAlbumArtByUri(uri)
                }
            }

            override fun updateViewPlayInfo(
                bundle: Bundle, songName: String, singerName: String, album: String, type: Int
            ) {
                runOnUiThread {
                    //Log.i(TAG, "updateViewPlayInfo: huang bundle=${bundle}")
                    llMusic.setMusicNameTextView(songName, type)
                    llMusic.setArtistNameTextView(singerName, type)
                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {
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
                                singerName, MusicWidget.OTHER_MUSIC
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
                bundle: Bundle, currentTime: Long, totalTime: Long, type: Int
            ) {
                runOnUiThread {
                    llMusic.setMusicSeekBar(
                        currentTime.toInt(), totalTime.toInt(), type
                    )
                }
            }

            override fun updateViewRadioFreq(
                bundle: Bundle, fmOrAm: String, freq: String, unit: String
            ) {
                runOnUiThread {

                }
            }

            override fun handleOriginBundle(bundle: Bundle) {
                //todo update music widget
                val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)


            }
        }
    }


    val mHandle: Handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_UPDATE_SPEED -> {
                        val speedKm = msg.arg1.toString()
                        val speedMile = msg.arg2.toString()
                        mCarSpeedLayout.tvGpsSpeed.text = speedKm
                        if (accRecor == false) {
                            val unit = ByteArray(1)
                            CommonData.readDataToMeta(unit, BIN_DATA_SPEED_UNIT)
                            val unitData = unit[0].toInt()
                            if (unitData == 0) {
                                mCarSpeedLayout.tvGpsSpeed.text = speedKm
                                mCarSpeedLayout.tvGpsSpeedUnit.text = "KM/h"
                            } else if (unitData == 1) {
                                mCarSpeedLayout.tvGpsSpeed.text = speedMile
                                mCarSpeedLayout.tvGpsSpeedUnit.text = "mph"
                            }

                            if (speedKm.toInt() > 0 || speedMile.toInt() > 0) {
                                if (!mAnimator.isRunning) {
                                    mAnimator.start()
                                }
                                if ((speedKm.toInt() - lastSpeed) > 5) {
                                    updateSpeedSmoothly(speedKm.toFloat())
                                    lastSpeed = speedKm.toFloat()
                                }
                            } else {
                                stopAnimation()
                            }
                        }
                        mHandle.removeMessages(MSG_UPDATE_SPEED)
                        mHandle.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 2000)
                    }

                    MSG_CLEAR_SPEED -> {
                        mCarSpeedLayout.tvGpsSpeed.text = 0.toString()
                        stopAnimation()
                    }

                }
            }
        }
    }

    val locationListener by lazy {
        LocationListener { location ->
            location.run {
                if (hasSpeed()) {
                    val speedKm = speed * 3.6
                    val speedMild = speedKm / 1.6093
                    val msg = mHandle.obtainMessage().apply {
                        what = MSG_UPDATE_SPEED
                        arg1 = speedKm.toInt()
                        arg2 = speedMild.toInt()
                    }
                    mHandle.sendMessage(msg)
                }
            }
        }
    }

    private fun clickStartApp() {

        mViewBinding.hotsetNavi.setOnClickListener {
            startActivity(
                "com.awell.navigation",
                "com.awell.navigation.MainActivity"
            )
        }

        mViewBinding.hotsetVideo.setOnClickListener {
            startActivity(
                "com.awell.localvideo",
                "com.awell.localvideo.activity.VideoListActivity"
            )
        }

        mViewBinding.hotsetSetting.setOnClickListener {
            startActivity(
                "com.awell.carsetting",
                "com.awell.carsetting.MainActivity"
            )
        }

        mViewBinding.hotsetAllapp.setOnClickListener {
            AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
        }

        mViewBinding.hotsetRadio.setOnClickListener {
            startActivity(
                "com.awell.radio",
                "com.awell.radio.AwellFmActivity"
            )
        }

        mViewBinding.hotsetBluetooth.setOnClickListener {
            startActivity(
                "com.awell.bluetooth",
                "com.awell.bluetooth.MainActivity"
            )
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

    private fun startActivity(packName: String, className: String?) {
        val intent = packageManager.getLaunchIntentForPackage(packName)
        var isboot = true
        if (intent != null) {
            for (index in IconCache.WorkSpacePackageName.indices) {
                if (packName != IconCache.WorkSpacePackageName[index]) {
                    isboot = false
                    break
                }
            }
            if (packName.contains("com.autonavi")) {
                if (isboot) Settings.System.putString(getContentResolver(), "boot_apk1", packName)
            } else {
                if (isboot) Settings.System.putString(getContentResolver(), "boot_apk2", packName)
            }
            startActivity(intent)
        }
    }


    /**
     * 重写只为长按弹出壁纸选择
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 先让子 View 处理事件

        val consumed = super.dispatchTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 重置状态
                isEventConsumedByChild = false
                isLongPressPossible = true

                // 记录触摸起始位置
                startX = ev.rawX
                startY = ev.rawY

                // 启动长按检测
                startLongPressDetection()
            }

            MotionEvent.ACTION_MOVE -> {
                // 检查是否移动超过阈值
                val dx: Float = abs(ev.rawX - startX)
                val dy: Float = abs(ev.rawY - startY)
                var touchSlop = 0f
                viewConfiguration?.let { touchSlop = it.scaledTouchSlop.toFloat() }

                if (dx > touchSlop || dy > touchSlop) {
                    cancelLongPressDetection()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelLongPressDetection()
        }
        // 记录事件是否被子 View 消费
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            isEventConsumedByChild = consumed
        }

        return consumed
    }

    private var longPressRunnable: Runnable? = null

    private fun startLongPressDetection() {
        cancelLongPressDetection()

        longPressRunnable = Runnable { // 只有事件没有被消费且长按可能时才触发
            if (!isEventConsumedByChild && isLongPressPossible) {
                handleLongPressAction()
            }
        }
        handler?.postDelayed(
            longPressRunnable!!,
            ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    private fun cancelLongPressDetection() {
        if (longPressRunnable != null) {
            handler?.removeCallbacks(longPressRunnable!!)
            longPressRunnable = null
        }
    }

    private fun handleLongPressAction() {
        Utils.setPluginApkFilePath(mediaControl.getLoadPluginApkFilePath())
        startWallpaper()
    }

}