package com.launcher.yfd_ui2

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Rect
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemProperties
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.awell.addapp.AddSelectAppCallback
import com.awell.addapp.AppInfo
import com.awell.addapp.MyDbHelper
import com.awell.addapp.ShowPopupI
import com.awell.control.AppsCustomizeControl
import com.awell.control.AppsCustomizeControl.setPluginThemeMode
import com.awell.control.AwellMediaControl
import com.awell.ctrlview.MusicWidget
import com.awell.launcher2.IconCache
import com.awell.library.AwellTool
import com.awell.utils.CommonData
import com.awell.utils.Utils
import com.awell.utils.Utils.startWallpaper
import com.launcher.yfd_ui2.adapter.AppInofAdapter
import com.launcher.yfd_ui2.adapter.AppPopAdapter
import com.launcher.yfd_ui2.databinding.UiActivityBinding
import com.launcher.yfd_ui2.utils.BootStateManager
import com.launcher.yfd_ui2.utils.FreeformUtils.NAVI_GAODE_PKG
import com.launcher.yfd_ui2.utils.FreeformUtils.NAVI_GOOGLE_PKG
import com.launcher.yfd_ui2.utils.FreeformUtils.SETTINGS_FREEFORM_APP_PACKAGE_NAME
import com.launcher.yfd_ui2.utils.FreeformUtils.startFreeformApp
import com.launcher.yfd_ui2.utils.LogUtil
import com.launcher.yfd_ui2.utils.SystemUIClient
import com.launcher.yfd_ui2.utils.SystemUIClient.MUSIC_PKG
import com.launcher.yfd_ui2.utils.WeatherHelper
import com.launcher.yfd_ui2.utils.WeatherIconLoader
import com.launcher.yfd_ui2.utils.WeatherTextMapper
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class YFD_UIActivity : Activity(), View.OnClickListener {

    private var isWeatherTimerRunning: Boolean = false
    private val TAG = YFD_UIActivity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding

    lateinit var mediaControl: AwellMediaControl
    private lateinit var locationManager: LocationManager

    private val MSG_UPDATE_SPEED = 1
    private val MSG_CLEAR_SPEED = 2
    private val BIN_DATA_SPEED_UNIT = 0x84
    private var accRecor: Boolean? = null

    val PERMISSION_REQUEST_CODE: Int = 100

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var appInfoAdapter: AppInofAdapter
    private lateinit var allAppInfoList: List<AppInfo>
    private lateinit var showAppInfoList: ArrayList<AppInfo>

    lateinit var popupWindow: PopupWindow

    private lateinit var placehodlerInfo: AppInfo
    private lateinit var myDbHelper: MyDbHelper
    private lateinit var sqLiteDatabase: SQLiteDatabase

    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false

    private var roadFrames: IntArray = intArrayOf(
        R.drawable.car_effect_driving_1,
        R.drawable.car_effect_driving_2,
        R.drawable.car_effect_driving_3,
    )

    lateinit var systemUIClient: SystemUIClient

    private val weatherRefreshRunnable: Runnable = object : Runnable {
        override fun run() {
            loadWeatherData()
            handler?.postDelayed(this, (60 * 1000).toLong())
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        mViewBinding = UiActivityBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)

        initView()

        initMediaMusic()

        updateSpeedUnitText()

        initTouchAndSpeedListener()

        initBroadcastReceiver()

        AppsCustomizeControl.setActivity(this)

        initFreeformControl()
        Settings.System.putString(contentResolver, "ui_has_freeform", "true")

        setPluginThemeMode(100)

    }
    private val handlerFreeform = Handler(Looper.getMainLooper())
    private val freeformRunnable = Runnable {
        if (findViewById<ImageView>(R.id.freeform_image).isVisibleOnScreen()) {
            updateImagePosition(findViewById(R.id.freeform_image), "onResume")
        }
    }
    override fun onResume() {
        super.onResume()
        LogUtil.i("onResume")
        /*findViewById<ImageView>(R.id.freeform_image).postDelayed({
            if (findViewById<ImageView>(R.id.freeform_image).isVisibleOnScreen()) {
                updateImagePosition(findViewById(R.id.freeform_image), "onResume")
            }
        }, 100)*/
        handlerFreeform.removeCallbacks(freeformRunnable)
        // 重新发送新的延时任务
        if(!BootStateManager.isFirstBootAfterReboot(this.applicationContext))
            handlerFreeform.postDelayed(freeformRunnable, 100)

        if (!isWeatherTimerRunning) {
            handler?.postDelayed(weatherRefreshRunnable, 0)
            isWeatherTimerRunning = true
        }

    }

    override fun onStop() {
        super.onStop()
        LogUtil.i("onStop")

        if (systemUIClient.getmFreeformMode()==0x05) {
            systemUIClient.hideFreeform()
            Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
            systemUIClient.fullScreenFreeform()
        }else
            Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
        LogUtil.w("freeform_launcher_idle,0")
    }

    override fun onPause() {
        super.onPause()
        LogUtil.i("onPause")
        handler?.removeCallbacks(weatherRefreshRunnable)
        handlerFreeform.removeCallbacks(freeformRunnable)
        isWeatherTimerRunning = false
    }

    private fun initFreeformControl() {

        systemUIClient = SystemUIClient(this)
        systemUIClient.bindToSystemUIService(this)
        appInfoAdapter.setSystemUIClient(systemUIClient)
    }

    private fun updateImagePosition(imageView: ImageView, reason: String) {
        val location = IntArray(2)
        imageView.getLocationOnScreen(location)
        val screenX = location[0]
        val screenY = location[1]
        val width = imageView.width
        val height = imageView.height

        val rect = Rect(screenX, screenY, screenX + width, screenY + height)
        SystemProperties.set("persist.sys.lz.default_freeform_bounds", "$screenX,$screenY,$width,$height")
        Settings.System.putString(contentResolver,"freeform_launcher_idle", "1");
        LogUtil.w("freeform_launcher_idle,1")
        //todo bootapk_packname bootapk_classname -- adb shell settings get system bootapk_packname
        //todo 开机时候检查时否有开机自启的apk，有，不启动小窗，没有，启动小窗
        LogUtil.i( "updateImagePosition: huang rect=>${rect} reason=${reason}")
        systemUIClient.setmFreeformMode(5)
        startFreeformApp(this, rect)

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

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, "com.awell.weather.permission.READ_WEATHER")
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("com.awell.weather.permission.READ_WEATHER"),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // 权限已授予，开始查询
            loadWeatherData()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWeatherData()
            } else {
                // 处理权限被拒绝
                //showPermissionDeniedMessage()
                LogUtil.e( "onRequestPermissionsResult: not have permission==>")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadWeatherData() {
        Thread {
            val info = WeatherHelper.getCurrentWeather(this)
            //LogUtil.i( "loadWeatherData: huang info=>${info}")
            handler?.post {
                info?.also {
                    mViewBinding.weatherTemp.text = "${it.temperature}°"
                    mViewBinding.weatherCondition.text = (WeatherTextMapper.description(
                        mViewBinding.getRoot().context,
                        it.condCode
                    ))
                    WeatherIconLoader.load(mViewBinding.weatherImage, it.condCode)
                } ?: run {
                    mViewBinding.weatherTemp.text = ""
                    mViewBinding.weatherCondition.text = ""
                    WeatherIconLoader.load(mViewBinding.weatherImage, "100")
                }
            }
        }.start()
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

        initCarView()

        clickStartApp()

        initAddAppView()

        initWeather()

    }

    private fun initWeather() {
        checkAndRequestPermission()

        //handler?.postDelayed(weatherRefreshRunnable, 0)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initAddAppView() {
        showAppInfoList = ArrayList<AppInfo>()
        allAppInfoList = ArrayList<AppInfo>()

        placehodlerInfo = AppInfo()
        placehodlerInfo.setIcon(getDrawable(R.drawable.sf_app_add_icon))
        placehodlerInfo.setLabel(getString(R.string.add_app))
        // 获取已保存需要显示的app包名，如果没有，则显示默认
        myDbHelper = MyDbHelper(this, "show_app", null, 1)
        sqLiteDatabase = myDbHelper.writableDatabase

        appInfoAdapter =
            AppInofAdapter(this, showAppInfoList, showPopupI, addSelectAppCallback)

        linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        mViewBinding.rv.setLayoutManager(linearLayoutManager)
        mViewBinding.rv.setAdapter(appInfoAdapter)

        Thread {
            showHostApp()
        }.start()

    }

    private fun showHostApp() {
        val storageAppList: MutableList<String> = ArrayList()

        allAppInfoList = getAllAppInfo(this, false)

        val cursor: Cursor =
            myDbHelper.getWritableDatabase().query("showapp", null, null, null, null, null, null)
        while (cursor.moveToNext()) {
            @SuppressLint("Range") val packageName =
                cursor.getString(cursor.getColumnIndex("packagename"))
            storageAppList.add(packageName)
            // 删除记录
            sqLiteDatabase.delete("showapp", "packagename=?", arrayOf<String>(packageName))
        }
        for (packageName in storageAppList) {
            val app = Utils.getAppInfoFromPackage(packageName, allAppInfoList)
            if (app != null) {
                showAppInfoList.add(
                    Utils.getAppInfoFromPackage(packageName, allAppInfoList)
                )
            }
        }
        cursor.close()

        // 如果数据库中没有数据，加载默认数据
        if (showAppInfoList.isEmpty()) {
            for (packName in Utils.defaultShowApp) {
                val appInfo = Utils.getAppInfoFromPackage(packName, allAppInfoList)
                if (appInfo != null) {
                    showAppInfoList.add(Utils.getAppInfoFromPackage(packName, allAppInfoList))
                }
            }
        }

        // 添加到数据库
        for (storagePac in showAppInfoList) {
            val contentValues = ContentValues()
            contentValues.put("packagename", storagePac.package_name)
            sqLiteDatabase.insert("showapp", null, contentValues)
        }
        showAppInfoList.add(placehodlerInfo)

        runOnUiThread {
            appInfoAdapter.setContentList(showAppInfoList)
            appInfoAdapter.notifyDataSetChanged()
        }

    }

    private fun initCarView() {
        mViewBinding.ivLampSwitchBg.postDelayed({ accRecor = false }, 1 * 1000)
        if (Settings.System.getInt(contentResolver, "Headlamp", 0) == 1) {
            mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.open)
        } else {
            mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.off)
        }
    }

    val addSelectAppCallback: AddSelectAppCallback = object : AddSelectAppCallback {
        @SuppressLint("NotifyDataSetChanged")
        override fun addAppInfo(appInfo: AppInfo) {
            for (pack in showAppInfoList)
                if (pack.package_name.equals(appInfo.package_name)) {
                    popupWindow.dismiss()
                    showPopupI.hidePopup()
                    return
                }
            showAppInfoList.remove(placehodlerInfo)

            showAppInfoList.add(appInfo)
            showAppInfoList.add(placehodlerInfo)
            appInfoAdapter.setContentList(showAppInfoList)
            appInfoAdapter.notifyDataSetChanged()
            thread {
                val contentValues = ContentValues().apply {
                    put("packagename", appInfo.package_name)
                }
                sqLiteDatabase.insert("showapp", null, contentValues)
            }


            showPopupI.hidePopup()
        }

        override fun removeAppInfo(packageName: String?) {
            sqLiteDatabase.delete("showapp", "packagename=?", arrayOf(packageName))
            for (pack in showAppInfoList)
                if (pack.package_name.equals(packageName)) {
                    showAppInfoList.remove(pack)
                    break
                }

            appInfoAdapter.setContentList(showAppInfoList)
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

    private val showPopupI: ShowPopupI = object : ShowPopupI {
        override fun showPopup() {
            showPopupAllApp()
            backgroundAlpha(0.2f)
        }

        override fun hidePopup() {
            popupWindow.dismiss()
        }
    }

    private fun showPopupAllApp() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.layout_allapp, null)
        popupWindow = PopupWindow(
            view,
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.windowLayoutType = TYPE_APPLICATION_OVERLAY

        allAppInfoList = getAllAppInfo(this, false)

        val rvPop = view.findViewById<RecyclerView>(R.id.rv_pop_allapp)
        val gridLayoutManager = GridLayoutManager(this, 4)
        gridLayoutManager.spanCount = 2
        gridLayoutManager.orientation = RecyclerView.HORIZONTAL
        val appInfoAdapter = AppPopAdapter(this, allAppInfoList, addSelectAppCallback)
        rvPop.layoutManager = gridLayoutManager
        rvPop.adapter = appInfoAdapter

        popupWindow.setOnDismissListener {
            backgroundAlpha(1.0f)
        }

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 10)
    }

    private fun backgroundAlpha(alpha: Float) {
        val lp = window.attributes
        lp.alpha = alpha //0.0-1.0
        window.attributes = lp
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
        unregisterReceiver(receiver)
        systemUIClient.unbindService(this)
        AppsCustomizeControl.setActivity(null)
        cancelLongPressDetection()
        AppsCustomizeControl.hideApps()
        try {
            mediaControl.unBindDataService(this)
        } catch (e: Exception) {
            LogUtil.e( "onDestroy: unBindDataService error=>${e.message}")
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
        mediaControl.bindDataService(this)
        mediaControl.updateMusicView = mediaImpl

    }


    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
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
                    findViewById<ImageView>(R.id.freeform_image).post {
                        if (findViewById<ImageView>(R.id.freeform_image).isVisibleOnScreen()) {
                            updateImagePosition(findViewById(R.id.freeform_image), "acc_on")
                        }
                    }

                }

                CommonData.ACTION_ACC_OFF -> {
                    accRecor = true
                    systemUIClient?.hideFreeform()
                }

                CommonData.BROADCAST_MEDIA_EXIT -> {
                    val pkg = intent.getStringExtra("package")
                    if (pkg != null && (pkg == "cn.kuwo.kwmusiccar" || pkg == "exitAll")) {

                    }
                }

                "com.zjinnova.zlink" -> {
                    val zlinkStatus = intent.getStringExtra("status")
                    val phoneMode = intent.getStringExtra("phoneMode")
                    LogUtil.d( "zlinkStatus:$zlinkStatus")
                    if (zlinkStatus == null) {
                        return
                    }
                    LogUtil.i(

                        "onReceive: mediaControl.getCurrentPkgName()=>${mediaControl.getCurrentPkgName()}"
                    )
                    if (mediaControl.getCurrentPkgName()?.equals("com.zjinnova.zlink") == true) {
                        if ("REFRESH_JEPG" == zlinkStatus) {
                            updateCarplayImageAlbum()
                        }
                    }
                }

                "android.launcher.show.allApp" -> {
                    Settings.System.putString(contentResolver,"freeform_launcher_idle", "0");
                    LogUtil.w("freeform_launcher_idle,0")
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
            mViewBinding.tvGpsSpeedUnit.text = "KM/h"
        } else if (unitData == 1) {
            mViewBinding.tvGpsSpeedUnit.text = "mph"
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

                    if ("com.awell.radio" != pkg) {
                    }

                }
            }

            override fun updateViewPlayStatus(
                bundle: Bundle, status: Boolean, type: Int
            ) {
                runOnUiThread {}
            }

            @SuppressLint("UseKtx")
            override fun updateViewMusicPlayImage(bundle: Bundle) {
                runOnUiThread {
                    val uriStr = bundle.getString(AwellTool.VALUE_M1, null)
                    val uri = uriStr?.let {
                        Uri.parse(it).takeIf { uri -> uri.scheme != null }
                    }
                }
            }

            override fun updateViewPlayInfo(
                bundle: Bundle, songName: String, singerName: String, album: String, type: Int
            ) {
                runOnUiThread {
                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {

                    }

                    if (MusicWidget.OTHER_MUSIC == type) {
                        if (!TextUtils.isEmpty(songName)) {
                        } else {

                        }
                        if (!TextUtils.isEmpty(singerName)) {

                        } else {

                        }
                    }
                }
            }

            override fun updateViewPlayTime(
                bundle: Bundle, currentTime: Long, totalTime: Long, type: Int
            ) {
                runOnUiThread {

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
                        mViewBinding.tvGpsSpeed.text = speedKm
                        if (accRecor == false) {
                            val unit = ByteArray(1)
                            CommonData.readDataToMeta(unit, BIN_DATA_SPEED_UNIT)
                            val unitData = unit[0].toInt()
                            if (unitData == 0) {
                                mViewBinding.tvGpsSpeed.text = speedKm
                                mViewBinding.tvGpsSpeedUnit.text = "KM/h"
                            } else if (unitData == 1) {
                                mViewBinding.tvGpsSpeed.text = speedMile
                                mViewBinding.tvGpsSpeedUnit.text = "mph"
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
                        mViewBinding.tvGpsSpeed.text = 0.toString()
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

        mViewBinding.homeAppNavi.setOnClickListener {
            val pkg =
                Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)
            when (pkg) {
                NAVI_GAODE_PKG,
                NAVI_GOOGLE_PKG -> systemUIClient.fullScreenFreeform()

                else -> startActivity(
                    "com.awell.navigation", "com.awell.navigation.MainActivity"
                )
            }
        }

        mViewBinding.homeAppMusic.setOnClickListener {
            val pkg =
                Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)

            when (pkg) {
                MUSIC_PKG -> {
                    systemUIClient.fullScreenFreeform()

                }

                else -> {
                    startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity")
                }
            }

        }


        mViewBinding.homeAppSetting.setOnClickListener {
            startActivity(
                "com.awell.carsetting", "com.awell.carsetting.MainActivity"
            )
        }

        mViewBinding.homeAppAllApp.setOnClickListener {

            LogUtil.w("freeform_launcher_idle,0")
            systemUIClient.hideFreeform()
            Settings.System.putString(contentResolver,"freeform_launcher_idle", "0");
            systemUIClient.fullScreenFreeform()


            AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
        }

        mViewBinding.homeAppBluetooth.setOnClickListener {
            startActivity(
                "com.awell.bluetooth", "com.awell.bluetooth.MainActivity"
            )
        }

        mViewBinding.freeformFullScreen.setOnClickListener(this)
        mViewBinding.freeformFullScreen.setOnLongClickListener {
            cancelLongPressDetection()
            val mIntent = Intent()
            mIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            mIntent.setPackage("com.awell.carsetting")
            mIntent.component =
                ComponentName("com.awell.carsetting", "com.awell.carsetting.MainActivity")
            mIntent.putExtra("SelectDefaultId", 3)
            mIntent.putExtra("SelectDefaultFragment", 30)
            startActivity(mIntent)
            true
        }
        mViewBinding.weatherTemp.setOnClickListener(this)
        mViewBinding.weatherImage.setOnClickListener(this)
        mViewBinding.weatherCondition.setOnClickListener(this)


        mViewBinding.clockView.setOnClickListener(this)
        mViewBinding.clockAmPm.setOnClickListener(this)
        mViewBinding.clockTime.setOnClickListener(this)
        mViewBinding.clockData.setOnClickListener(this)
        mViewBinding.textWeek.setOnClickListener(this)


    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AppsCustomizeControl.hideApps()
        Settings.System.putString(contentResolver,"freeform_launcher_idle", "1");
        LogUtil.w("freeform_launcher_idle,1")
        findViewById<ImageView>(R.id.freeform_image).post {
            if (findViewById<ImageView>(R.id.freeform_image).isVisibleOnScreen()) {
                updateImagePosition(findViewById(R.id.freeform_image), " back press")
            }
        }
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

    private fun getAllAppInfo(context: Context, isFilterSystem: Boolean): ArrayList<AppInfo> {
        val appBeanList: ArrayList<AppInfo> = ArrayList()

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val packageManager = context.packageManager
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        val appList: List<PackageInfo> = resolveInfos.mapNotNull { resolveInfo ->
            try {
                packageManager.getPackageInfo(resolveInfo.activityInfo.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }.distinctBy { it.packageName }


        for (p in appList) {
            val bean = AppInfo()
            bean.setIcon(p.applicationInfo.loadIcon(packageManager))
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString())
            val pack = p.applicationInfo.packageName
            bean.setPackage_name(pack)
            val flags = p.applicationInfo.flags
            bean.setFlags(flags)
            if ((flags and ApplicationInfo.FLAG_SYSTEM) != 0
                && Utils.needToShowPackageName.contains(pack)
            ) {
                appBeanList.add(bean)
            } else if ((flags and ApplicationInfo.FLAG_SYSTEM) == 0
                && !Utils.filterAppPackageName.contains(pack)
            ) {
                appBeanList.add(bean)
            } else if ((flags and ApplicationInfo.FLAG_SYSTEM) != 0
                && Utils.otherNeedToShowPackageName.contains(pack)
            ) {
                appBeanList.add(bean)
            }

        }

        return appBeanList
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

    override fun onClick(v: View?) {
        when (v?.id) {
            mViewBinding.freeformFullScreen.id -> {
                systemUIClient.fullScreenFreeform()
            }

            mViewBinding.weatherTemp.id,
            mViewBinding.weatherCondition.id,
            mViewBinding.weatherImage.id -> {
                val intent = Intent()
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.component =
                    ComponentName("com.awell.weather", "com.awell.weather.MainActivity")
                startActivity(intent)
            }

            mViewBinding.clockView.id,
            mViewBinding.clockAmPm.id,
            mViewBinding.clockTime.id,
            mViewBinding.clockData.id,
            mViewBinding.textWeek.id -> {
                val intent = Intent(Settings.ACTION_DATE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

}