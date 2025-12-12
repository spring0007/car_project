package com.launcher.yfd_ui01

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import com.awell.control.AwellMediaControl
import com.awell.launcher2.IconCache
import com.awell.utils.CommonData
import com.awell.utils.Utils.startWallpaper
import com.launcher.yfd_ui01.app.GlobalViewManager
import com.launcher.yfd_ui01.app.IconManager
import com.launcher.yfd_ui01.app.SpeedSimulator
import com.launcher.yfd_ui01.databinding.ActivityMainUi01Binding
import com.launcher.yfd_ui01.utils.SystemUIClient
import com.launcher.yfd_ui01.utils.SystemUIClient.HIDE_FREEFORM
import com.launcher.yfd_ui01.utils.SystemUIClient.OPEN_APP_TO_FREEFORM
import com.launcher.yfd_ui01.utils.SystemUIClient.WINDOWING_MODE_FULLSCREEN
import java.lang.Math.abs

class MainActivity_YFD_UI01 : Activity() {
    private val TAG = MainActivity_YFD_UI01::class.simpleName
    private  lateinit var mViewBinding: ActivityMainUi01Binding

    lateinit var mediaControl: AwellMediaControl
    private lateinit var musicWidget: MusicWidget
    private lateinit var locationManager: LocationManager
    private lateinit var dashboardView: DashboardView
    private lateinit var dialWidget: DialWidget
    //private lateinit var speedSimulator: SpeedSimulator
    private val MSG_UPDATE_SPEED = 1
    private val MSG_CLEAR_SPEED = 2
//    private val BIN_DATA_SPEED_UNIT = 0x84
    private var accRecor: Boolean? = null
    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null
//    private var isGlobalLayoutListenerAdded = false
//    private var lastVisibleState: Boolean? = null
    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false
    lateinit var systemUIClient: SystemUIClient
    var viewAddNeedToStartFreeform: Boolean = false
//    var oldFreeformPkg: String? = null
    private val freeformImpl: SystemUIClient.UIClientCallback =
        SystemUIClient.UIClientCallback {
            if (systemUIClient.rect == null) {
                //服务绑定比视图初始化快，rect未设置，启动Launcher，切换Launcher等
                viewAddNeedToStartFreeform = true
            } else {
                systemUIClient.startOrSetFreeformType(this, OPEN_APP_TO_FREEFORM)
              //  oldFreeformPkg = systemUIClient.getFreeformPkg(this)
            }
        }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	    mViewBinding = ActivityMainUi01Binding.inflate(layoutInflater)
        Settings.System.putString(mViewBinding.root.context.contentResolver, "ui_has_freeform", "true")
        IconManager.init(mViewBinding.root.context);
        setContentView(mViewBinding.root)
        initView()
        initMediaMusic()
        initTouchAndSpeedListener()
        initBroadcastReceiver()
        initFreeform()
        // 创建速度模拟器
//        speedSimulator = SpeedSimulator(object : SpeedSimulator.SpeedChangeListener {
//            override fun onSpeedChanged(speed: Int) {
//                // 在主线程中更新UI
//                runOnUiThread {
//                    // 您可以在这里处理其他与速度相关的逻辑
//                    Log.i(TAG, "speed = $speed")
//                    dashboardView.udDataSpeed(speed)
//                   // mViewBinding.carSpeedTv.text = "$speed"
//                }
//            }
//        })
//
//        // 开始模拟
//        speedSimulator.startSimulation()

    }
    override fun onResume() {
        super.onResume()
        findViewById<ImageView>(R.id.freeform_bg).post {
            if (findViewById<ImageView>(R.id.freeform_bg).isVisibleOnScreen()) {
                updateImagePosition(findViewById(R.id.freeform_bg))
            }
        }

    }
    
    override fun onPause() {
        super.onPause()
        Log.i(TAG, "lqq,onPause")
        dialWidget?.stopAnimation()
        dashboardView?.closeAnimation()
    }
    
    private fun initFreeform() {

        setupPositionListener()
        systemUIClient = SystemUIClient(this)
        systemUIClient.bindToSystemUIService(this)
        systemUIClient.setCallback(freeformImpl)

    }

    private fun setupPositionListener() {
        val freeformImage = findViewById<ImageView>(R.id.freeform_bg)

        val layoutListener = View.OnLayoutChangeListener { _, left, top, right, bottom,
                                                           oldLeft, oldTop, oldRight, oldBottom ->

            // 检查位置是否变化或者视图是否可见
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                if (freeformImage.isVisibleOnScreen()) {
                    updateImagePosition(freeformImage)
                }
            }
        }

        freeformImage.addOnLayoutChangeListener(layoutListener)
    }

    private fun updateImagePosition(imageView: ImageView) {
        val location = IntArray(2)
        imageView.getLocationOnScreen(location)
        val screenX = location[0]
        val screenY = location[1]
        val width = imageView.width
        val height = imageView.height

        systemUIClient.rect = Rect(screenX, screenY, screenX + width, screenY + height)
        Log.i(TAG, "updateImagePosition: huang Starting freeform... rect=${systemUIClient.rect}")
        systemUIClient.startOrSetFreeformType(
            mViewBinding.root.context,
            OPEN_APP_TO_FREEFORM
        )
       // oldFreeformPkg = systemUIClient.getFreeformPkg(baseContext)
    }

    private fun View.isVisibleOnScreen(): Boolean {
        if (!isShown || !isAttachedToWindow) {
            return false
        }

        val location = IntArray(2)
        getLocationOnScreen(location)

        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        return location[0] + width > 0 &&
                location[1] + height > 0 &&
                location[0] < screenWidth &&
                location[1] < screenHeight
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

        dashboardView = mViewBinding.carSpeedPoint
        dialWidget = mViewBinding.layoutDialWidget.dialWidgetLayout
        dialWidget.findViews(this, dialWidget)


        clickStartApp()
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
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_DATE_CHANGED)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }
    
    /**
     * 初始化媒体信息
     * 以及绑定宿主服务
     *
     */
    private fun initMediaMusic() {

//        mMediaListener.initDependencies(baseContext)
        mediaControl = AwellMediaControl()
        Log.i(TAG, "initMediaMusic: huang UI2 bind data service=>${this}")
        mediaControl.bindDataService(this)
        mediaControl.updateMusicView = mediaImpl
	    musicWidget = mViewBinding.layoutMusicWidget.musicWidgetLayout
	    musicWidget.setMediaLibrary(mediaControl)
        musicWidget.setActivity(this, musicWidget)

    }
    
    override fun onDestroy() {
        super.onDestroy()
        Settings.System.putString(mViewBinding.root.context.contentResolver, "ui_has_freeform", "false")
        unregisterReceiver(receiver)
        systemUIClient.unbindService(this)
        distoryAllApps(this);
        cancelLongPressDetection()
        try {
            mediaControl.unBindDataService(this)
            Log.i(TAG, "onDestroy: huang unbind data service==>${this}")
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: unBindDataService error=>${e.message}")
        }

        // 停止速度模拟器
        //speedSimulator?.stopSimulation()


    }


    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.i(TAG, "onReceive:huang action=$action")
            when (action) {
                CommonData.BROADCAST_LAMP_SWITCH -> {}
                CommonData.ACTION_ACC_ON -> {}
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
                    musicWidget.getCarPlayData(zlinkStatus, phoneMode)
                }

                "android.launcher.show.allApp" -> {
                   // AppsCustomizeControl.showApps(findViewById<ViewGroup>(android.R.id.content))
                    showAllApps(context)

                    systemUIClient?.startOrSetFreeformType(context, HIDE_FREEFORM)
                    systemUIClient?.startOrSetFreeformType(context, WINDOWING_MODE_FULLSCREEN)
                    Log.i(TAG, "onClick: huang freeform to hide222==>")
                }

                "CANBUS_CHANGE_SPEED_Unit" -> {}
                "top_session_package_change" -> {
                    val sessionTopPkg = intent.getStringExtra("top_package")
                    handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                }

                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_TICK -> {
                    dialWidget?.updateTimeSysem()
                }
            }
        }
    }
    
    fun handleMediaPlaybackResult(value1: String, value2: String, value3: Int, value4: Int) {

        val oldPlayingPackage = mediaControl.getCurrentPkgName()
        val isStartCommand = "start" == value2
        val isStopCommand = "stop" == value2
        val isValidPackage = !TextUtils.isEmpty(value1)
        // 处理本地音乐的特殊情况
        if (isValidPackage && (value1.contains("localmusic") || value1.contains("com.awell.bluetooth") || value1.contains(
                "/system/bin/gocsdk"
            )) && isStartCommand
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

        val mHandle: Handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_UPDATE_SPEED -> {
                        val speedKm = msg.arg1.toString()
                        val speedMile = msg.arg2.toString()
//                        mViewBinding.tvGpsSpeed.text = speedKm
//                        if (accRecor == false) {
//                            val unit = ByteArray(1)
//                            CommonData.readDataToMeta(unit, BIN_DATA_SPEED_UNIT)
//                            val unitData = unit[0].toInt()
//                            if (unitData == 0) {
//                                mViewBinding.tvGpsSpeed.text = speedKm
//                                mViewBinding.tvGpsSpeedUnit.text = "KM/h"
//                            } else if (unitData == 1) {
//                                mViewBinding.tvGpsSpeed.text = speedMile
//                                mViewBinding.tvGpsSpeedUnit.text = "mph"
//                            }
//
//                            if (speedKm.toInt() > 0 || speedMile.toInt() > 0) {
//                                if (!mAnimator.isRunning) {
//                                    mAnimator.start()
//                                }
//                                if ((speedKm.toInt() - lastSpeed) > 5) {
//                                    updateSpeedSmoothly(speedKm.toFloat())
//                                    lastSpeed = speedKm.toFloat()
//                                }
//                            } else {
//                                stopAnimation()
//                            }
//                        }
                        mHandle.removeMessages(MSG_UPDATE_SPEED)
                        mHandle.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 2000)
                    }

                    MSG_CLEAR_SPEED -> {
                        //mViewBinding.tvGpsSpeed.text = 0.toString()
                        //stopAnimation()
                        dashboardView.udDataSpeed(0)
                    }

                }
            }
        }
    }

    /**
     * 实现媒体回调接口
     * 更新媒体信息FM、音乐播放等
     * 歌手、播放时间、播放状态等
     */
    private val mediaImpl = object : AwellMediaControl.UpdateMediaDataToView {
        override fun updateViewMusicPlay(bundle: Bundle, pkg: String, command: String, mediaType: Int, currentMedia: Int) {
            if ("com.awell.radio" != pkg) {
                musicWidget.switchMediaController(pkg, command, mediaType, currentMedia)
            }
        }

        override fun updateViewPlayStatus(bundle: Bundle, status: Boolean, type: Int) {
            musicWidget.setCurMusicState(status, type)
        }

        override fun updateViewMusicPlayImage(bundle: Bundle) {}

        override fun updateViewPlayInfo(bundle: Bundle, songName: String, singerName: String, album: String, type: Int) {
            musicWidget.setMusicNameTextView(songName, type)
            musicWidget.setArtistNameTextView(singerName, type)
            if ("NO_MUSIC_LIST" == songName
                && "NO_MUSIC_LIST" == singerName
                && "NO_MUSIC_LIST" == album) {
                musicWidget.setMusicNameTextView(resources.getString(R.string.click_play_music), MusicWidget.MUSIC)
                musicWidget.setArtistNameTextView(resources.getString(R.string.music_artist), MusicWidget.MUSIC)
            }

            if (MusicWidget.OTHER_MUSIC == type) {
                if (!TextUtils.isEmpty(songName)) {
                    musicWidget.setMusicNameTextView(songName, MusicWidget.OTHER_MUSIC)
                } else {
                    musicWidget.setMusicNameTextView(resources.getString(R.string.click_play_music), MusicWidget.OTHER_MUSIC)
                }
                if (!TextUtils.isEmpty(singerName)) {
                    musicWidget.setArtistNameTextView(singerName, MusicWidget.OTHER_MUSIC)
                } else {
                    musicWidget.setArtistNameTextView(resources.getString(R.string.music_artist), MusicWidget.OTHER_MUSIC)
                }
            }
        }

        override fun updateViewPlayTime(bundle: Bundle, currentTime: Long, totalTime: Long, type: Int) {
            musicWidget.setMusicSeekBar(currentTime.toInt(), totalTime.toInt(), type)
        }

        override fun updateViewRadioFreq(bundle: Bundle, fmOrAm: String, freq: String, unit: String) {}

        override fun handleOriginBundle(bundle: Bundle) {}
    }

    val locationListener by lazy {
        LocationListener { location ->
            location.run {
                if (hasSpeed()) {
                    val speed = (location.speed * 3.6).toInt() // m/s ---> km/h
                    val speedMile = (speed / 1.6093).toInt() // km/h  ---> miles/h
                    dashboardView.udDataSpeed(speed)
                    val msg = mHandle.obtainMessage().apply {
                        what = MSG_UPDATE_SPEED
                        arg1 = speed
                        arg2 = speedMile
                    }
                    mHandle.sendMessage(msg)
                }
            }
        }
    }
    private fun clickStartApp() {
        mViewBinding.hotsetAllApp.setOnClickListener {
            systemUIClient.startOrSetFreeformType(this, HIDE_FREEFORM)
            systemUIClient.startOrSetFreeformType(this, WINDOWING_MODE_FULLSCREEN)
           showAllApps(this)

        }
        mViewBinding.hotsetDspApp.setOnClickListener {
            startActivity("com.awell.eqselect", "com.awell.eqselect.MainActivity")
        }
        mViewBinding.hotsetBtApp.setOnClickListener {
            startActivity(
                "com.awell.bluetooth", "com.awell.bluetooth.MainActivity"
            )
        }

        mViewBinding.hotsetWindowApp.setOnClickListener {
            systemUIClient.startOrSetFreeformType(this, WINDOWING_MODE_FULLSCREEN)
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        //AppsCustomizeControl.hideApps()
	hideAllApps(this)
        systemUIClient.startOrSetFreeformType(this, OPEN_APP_TO_FREEFORM)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //AppsCustomizeControl.hideApps()
	hideAllApps(this)
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
            longPressRunnable!!, ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    private fun cancelLongPressDetection() {
        if (longPressRunnable != null) {
            handler?.removeCallbacks(longPressRunnable!!)
            longPressRunnable = null
        }
    }

    private fun handleLongPressAction() {
        startWallpaper()
    }
    
    private fun showAllApps(context: Context?) {
        val root = findViewById<View>(android.R.id.content)
        try {
            GlobalViewManager.getInstance(context).showApps(root as ViewGroup, 0)
            Log.i(TAG, "GlobalViewManager.showApps ")
        } catch (e: Exception) {
            Log.w(TAG, "GlobalViewManager.showApps failed", e)
        }
        //AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))

    }

    private fun hideAllApps(context: Context) {
        try {
            GlobalViewManager.getInstance(context).hideApps()
        } catch (e: Exception) {
            Log.w(TAG, "GlobalViewManager.hideApps failed", e)
        }
    }

    private fun distoryAllApps(context: Context) {
        try {
            GlobalViewManager.getInstance(context).destroy()
        } catch (e: Exception) {
            Log.w(TAG, "GlobalViewManager.destroy failed", e)
        }

        //AppsCustomizeControl.setActivity(null)
        //AppsCustomizeControl.hideApps()
    }

    private fun isAppsShow(): Boolean {
        return GlobalViewManager.getInstance(this).isAllShowing
    }
    
}