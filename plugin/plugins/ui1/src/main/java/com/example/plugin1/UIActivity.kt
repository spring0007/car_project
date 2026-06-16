package com.example.plugin1

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemProperties
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import androidx.core.graphics.toColorInt
import com.awell.control.AppsCustomizeControl
import com.awell.control.AppsCustomizeControl.setPluginThemeMode
import com.awell.control.AwellMediaControl
import com.awell.ctrlview.MusicWidget
import com.awell.launcher2.IconCache
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellTool
import com.awell.utils.CommonData
import com.awell.utils.Utils.startWallpaper
import com.example.plugin1.databinding.ActivityUiactivityBinding
import com.example.plugin1.databinding.SpeedLayoutBinding
import com.example.plugin1.databinding.WidgetLayoutRadioTwoBinding
import kotlin.math.abs


class UIActivity : Activity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName.toString()
    private lateinit var mBinding: ActivityUiactivityBinding
    private lateinit var mCarSpeedLayout: SpeedLayoutBinding
    private lateinit var mRadioLayout: WidgetLayoutRadioTwoBinding
    private var mMediaListener = MediaNotificationListener()
    lateinit var llMusic: MusicWidgetPlugin
    lateinit var mediaControl: AwellMediaControl

    private val mLauncherTypeDNUri: Uri = Settings.System.getUriFor("launcherTypeDN")
    private val mRadioUri: Uri = Settings.System.getUriFor("RadioIsLocOrDX")

    private val BIN_DATA_SPEED_UNIT = 0x84
    private val MSG_UPDATE_SPEED = 1
    private val MSG_CLEAR_SPEED = 2

    lateinit var mLocationManager: LocationManager

    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false

    private var accRecor: Boolean? = null
    var thisActivity = this


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Utils.setPluginWallpaper(resources, R.drawable.wallpaper_00)

//        (LauncherApplication.getmAppContext() as LauncherApplication).getModel()
//            .startLoader(true, -1)

        mBinding = ActivityUiactivityBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        findViewId()

        initMediaMusic()
        initRadioWidget()
        initCarView()
        updateSpeedUnitText()

        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (mLocationManager != null) {
            mLocationManager.requestLocationUpdates("gps", 1000, 10f, mLocationListener, mHandle.looper)
        } else {
            Log.e(TAG, "LocationManager is null, cannot request location updates");
        }

        handler = Handler(Looper.getMainLooper())
        viewConfiguration = ViewConfiguration.get(this)

        val value = Settings.System.getInt(contentResolver, "launcherTypeDN", 0)
        dayNightImages(if (value == 1) 1 else 0)

        initBroadcastReceiver()
        AppsCustomizeControl.setActivity(this)
        setPluginThemeMode(1)


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

    private fun initCarView() {
        mBinding.ivMainXiaodeng.postDelayed({ accRecor = false }, 8 * 1000)
        if (Settings.System.getInt(contentResolver, "Headlamp", 0) == 1) {
            mBinding.ivMainXiaodeng.setImageResource(R.drawable.open)
        } else {
            mBinding.ivMainXiaodeng.setImageResource(R.drawable.off)
        }
    }


    val settingsObserver: ContentObserver = object : ContentObserver(Handler()) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (mRadioUri == uri) {
                val isLocOrDx = Settings.System.getInt(contentResolver, "RadioIsLocOrDX", 0)
                setLocOrDx(isLocOrDx)
            }
        }
    }

    private fun setLocOrDx(isLocOrDx: Int) {
        when (isLocOrDx) {
            0 -> {
                mRadioLayout.ivRadioSetYC.text = getString(R.string.radio_loc)
            }

            1 -> {
                mRadioLayout.ivRadioSetYC.text = getString(R.string.radio_dx)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initRadioWidget() {
        val isLocOrDx = Settings.System.getInt(contentResolver, "RadioIsLocOrDX", 1)
        val isFMOrAM = Settings.System.getInt(contentResolver, "RadioIsFMOrAM", 0)

        if (isFMOrAM == 0) {
            mRadioLayout.tvRadioAmFm.text = "FM"
            mRadioLayout.ivRadioSetFM.text = "FM"
            mRadioLayout.tvRadioFreqUnit.text = "MHz"
        } else if (isFMOrAM == 1) {
            mRadioLayout.tvRadioAmFm.text = "AM"
            mRadioLayout.ivRadioSetFM.text = "AM"
            mRadioLayout.tvRadioFreqUnit.text = "KHz"
        }

        setLocOrDx(isLocOrDx)

        contentResolver.registerContentObserver(mLauncherTypeDNUri, false, settingsObserver)
        contentResolver.registerContentObserver(mRadioUri, false, settingsObserver)
    }


    override fun onDestroy() {
        super.onDestroy()
        mMediaListener.cleanup()
        AppsCustomizeControl.setActivity(null)
        unregisterReceiver(receiver)
        cancelLongPressDetection()
        AppsCustomizeControl.hideApps()
        cleanListener()
        try {
            mediaControl.unBindDataService(this)
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: unBindDataService error=>${e.message}")
        }
    }

    private fun cleanListener() {

        try {
            mLocationManager.removeUpdates(mLocationListener)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }


    /**
     * 初始化媒体信息
     * 以及绑定宿主服务
     *
     */
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

        mCarSpeedLayout = SpeedLayoutBinding.bind(mBinding.includeSpeedLayout.root)
        mRadioLayout = WidgetLayoutRadioTwoBinding.bind(mBinding.includeRadio.root)

        mBinding.hotsetVideo.setOnClickListener(this)
        mBinding.hotsetNavi.setOnClickListener(this)
        mBinding.hotsetAllapp.setOnClickListener(this)
        mBinding.hotsetSetting.setOnClickListener(this)
        mBinding.hotsetBluetooth.setOnClickListener(this)

        mRadioLayout.waveformView.setOnClickListener(this)
        mRadioLayout.tvRadioAmFm.setOnClickListener(this)
        mRadioLayout.ivRadioSetFM.setOnClickListener(this)
        mRadioLayout.tvRadioFreq.setOnClickListener(this)

        mRadioLayout.ivRadioSetYC.setOnClickListener {
            mediaControl.sendStrToHost(AwellTool.RADIO.SET_LocDX)
        }

        mRadioLayout.ivRadioPre.setOnClickListener {
            mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS)
        }

        mRadioLayout.ivRadioNext.setOnClickListener {
            mediaControl.sendStrToHost(AwellTool.RADIO.NEXT)
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
                            Log.e(TAG, "unit Data = $unitData")
                            if (unitData == 0) {
                                mCarSpeedLayout.tvGpsSpeed.text = speedKm
                                mCarSpeedLayout.tvGpsSpeedUnit.text = "KM/h"
                            } else if (unitData == 1) {
                                mCarSpeedLayout.tvGpsSpeed.text = speedMile
                                mCarSpeedLayout.tvGpsSpeedUnit.text = "mph"
                            }
                        }
                        mHandle.removeMessages(MSG_UPDATE_SPEED)
                        mHandle.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 2000)
                    }

                    MSG_CLEAR_SPEED -> {
                        mCarSpeedLayout.tvGpsSpeed.text = "" + 0
                    }

                }
            }
        }
    }

    val mLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasSpeed()) {
                val speedKm = location.speed * 3.6
                val speedMild = speedKm / 1.6093
                val msg = mHandle.obtainMessage().apply {
                    what = MSG_UPDATE_SPEED
                    arg1 = speedKm.toInt()
                    arg2 = speedMild.toInt()
                }
                mHandle.sendMessage(msg)
            }
        }
        override fun onProviderDisabled(provider: String) { }
        override fun onProviderEnabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    var radio_bar_icon_bgID: IntArray =
        intArrayOf(R.drawable.radio_bar_icon_bg, R.drawable.radio_bar_icon_bg_n)
    var sf_radio_preID: IntArray = intArrayOf(R.drawable.sf_radio_pre, R.drawable.sf_radio_pre_n)
    var sf_radio_nextID: IntArray = intArrayOf(R.drawable.sf_radio_next, R.drawable.sf_radio_next_n)
    var textColorId: IntArray =
        intArrayOf("#99ffffff".toColorInt(), "#99ffffff".toColorInt())
    var date_bgID: IntArray = intArrayOf(R.drawable.date_bg, R.drawable.date_bg_n)

    private fun dayNightImages(dayNight: Int) {
        llMusic.setDayNight(dayNight)

        mRadioLayout.radioIv.setBackgroundResource(radio_bar_icon_bgID[dayNight])
        mRadioLayout.ivRadioPre.setImageResource(sf_radio_preID[dayNight])
        mRadioLayout.ivRadioNext.setImageResource(sf_radio_nextID[dayNight])
        mRadioLayout.tvRadioAmFm.setTextColor(textColorId[dayNight])
        mRadioLayout.tvRadioFreq.setFlag(dayNight)
        mRadioLayout.tvRadioFreqUnit.setTextColor(textColorId[dayNight])
        mRadioLayout.radioControlLl.setBackgroundResource(date_bgID[dayNight])
        mRadioLayout.ivRadioSetFM.setTextColor(textColorId[dayNight])
        mRadioLayout.ivRadioSetSearch.setTextColor(textColorId[dayNight])
        mRadioLayout.ivRadioSetYC.setTextColor(textColorId[dayNight])

        mCarSpeedLayout.tvGpsSpeed.setTextColor("#ccffffff".toColorInt())
        mCarSpeedLayout.tvGpsSpeedUnit.setTextColor("#ccffffff".toColorInt())

    }

    /**
     * 实现媒体回调接口
     * 更新媒体信息FM、音乐播放等
     * 歌手、播放时间、播放状态等
     */
    private val mediaImpl: AwellMediaControl.UpdateMediaDataToView =
        object : AwellMediaControl.UpdateMediaDataToView {
            override fun updateViewMusicPlay(
                bundle: Bundle, pkg: String, command: String, mediaType: Int, currentMedia: Int
            ) {
                runOnUiThread {

                    if ("com.awell.radio" != pkg) {
                        llMusic.switchMediaController(
                            pkg, command, mediaType, currentMedia
                        )
                    }

                    if ("com.awell.radio" == pkg) {
                        if ("start" == command) {
                            mRadioLayout.waveformView.startAnimation()
                            //mWaveformView.startAnimation()
                        } else if ("stop" == command) {
                            mRadioLayout.waveformView.stopAnimation()
                            //mWaveformView.stopAnimation()
                        }
                    }
                }
            }

            override fun updateViewPlayStatus(
                bundle: Bundle, status: Boolean, type: Int
            ) {
                runOnUiThread {
                    llMusic.setCurMusicState(status, type)
                }
            }

            override fun updateViewMusicPlayImage(
                bundle: Bundle
            ) {
                runOnUiThread {
                    //llMusic.setPlayImage(songId, albumId)
                }
            }

            override fun updateViewPlayInfo(
                bundle: Bundle, songName: String, singerName: String, album: String, type: Int
            ) {
                runOnUiThread {
                    llMusic.setMusicNameTextView(songName, type)
                    llMusic.setArtistNameTextView(singerName, type)
                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {
                        llMusic.setMusicNameTextView(
                            getResources().getString(R.string.click_play_music), MusicWidget.MUSIC
                        )
                        llMusic.setArtistNameTextView(
                            getResources().getString(R.string.music_artist), MusicWidget.MUSIC
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
                    mRadioLayout.tvRadioAmFm.text = fmOrAm
                    mRadioLayout.ivRadioSetFM.text = fmOrAm
                    mRadioLayout.tvRadioFreq.text = freq
                    mRadioLayout.tvRadioFreqUnit.text = unit
                }
            }

            override fun handleOriginBundle(bundle: Bundle) {
            }
        }


    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                CommonData.BROADCAST_LAMP_SWITCH -> {
                    if (intent.getIntExtra("lamplet_state", 0) == 1) {
                        mBinding.ivMainXiaodeng.setImageResource(R.drawable.open)
                    } else {
                        mBinding.ivMainXiaodeng.setImageResource(R.drawable.off)
                    }
                }

                CommonData.ACTION_ACC_ON -> {
                    mBinding.ivMainXiaodeng.postDelayed({ accRecor = false }, 8 * 1000)
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
                    llMusic.getCarPlayData(zlinkStatus, phoneMode)
                }

                "android.launcher.show.allApp" -> {
                    //showAllApps(true)
                    //setSettingOrAndroidPage(true)
                    //mModel.startLoader(true, -1)
                    AppsCustomizeControl.showApps(thisActivity.findViewById<ViewGroup>(android.R.id.content))
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
    }


    fun handleMediaPlaybackResult(value1: String, value2: String, value3: Int, value4: Int) {

        val oldPlayingPackage = mMediaListener.playingPackageName
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

            mMediaListener.setPlayingPackageName(value1)
            mMediaListener.startCallbacks()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {

            mRadioLayout.tvRadioAmFm.id, mRadioLayout.ivRadioSetFM.id -> {
                mediaControl.sendStrToHost(AwellTool.RADIO.SET_FMAM)
            }

            R.id.tv_radio_freq, R.id.waveformView -> {
                startActivity("com.awell.radio", "com.awell.radio.AwellFmActivity")
            }

            R.id.hotset_video -> {
                /*startActivity(
                    "com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity"
                )*/
                startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity")
            }

            R.id.iv_main_xiaodeng, R.id.rl_car, R.id.hotset_navi -> {
                val navPkg = SystemProperties.get("persist.sys.navi.packagename", "")

                //String navClazz = SystemProperties.get("persist.sys.navi.clazzname", "");
                if (TextUtils.isEmpty(navPkg) || !getNaviApps(navPkg)) startActivity(
                    "com.awell.navigation",
                    "com.awell.navigation.MainActivity"
                )
                else {
                    val intent = this.getPackageManager().getLaunchIntentForPackage(navPkg!!)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        startActivity("com.awell.navigation", "com.awell.navigation.MainActivity")
                    }
                }
            }

            R.id.hotset_allapp -> {
                AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
            }

            R.id.hotset_setting -> {
                startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity")
            }

            R.id.hotset_bluetooth -> {
                startActivity("com.awell.bluetooth", "com.awell.bluetooth.MainActivity")
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
        //Utils.setPluginApkFilePath(mediaControl.getLoadPluginApkFilePath())
        startWallpaper()
    }

    private fun startActivity(packName: String, className: String?) {
        val intent = packageManager.getLaunchIntentForPackage(packName)
        var isBoot = true
        if (intent != null) {
            for (index in IconCache.WorkSpacePackageName.indices) {
                Log.d(TAG, "packName=$packName")
                if (packName != IconCache.WorkSpacePackageName[index]) {
                    isBoot = false
                    break
                }
            }
            if (packName.contains("com.autonavi")) {
                if (isBoot) Settings.System.putString(getContentResolver(), "boot_apk1", packName)
            } else {
                if (isBoot) Settings.System.putString(getContentResolver(), "boot_apk2", packName)
            }
            startActivity(intent)
        }
    }
    fun getNaviApps(appName: String): Boolean {
        try {
            val appinfo =
                this.getPackageManager().getApplicationInfo(appName, PackageManager.GET_META_DATA)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            //e.printStackTrace();
            Log.i(TAG, "Exception = $e")
        }
        return false
    }
}