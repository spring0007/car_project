package com.launcher.zy_ui06

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.widget.CheckBox
import android.widget.PopupWindow
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.awell.addapp.AddSelectAppCallback
import com.awell.addapp.AppInfo
import com.awell.addapp.ShowPopupI
import com.awell.control.AppsCustomizeConfig
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.launcher2.IconCache
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellTool
import com.awell.utils.CommonData
import com.awell.utils.Utils.startWallpaper
import com.launcher.zy_ui06.adapter.AppPopAdapter
import com.launcher.zy_ui06.control.StyleParser
import com.launcher.zy_ui06.databinding.UiActivityBinding
import com.awell.library.util.ClickUtils
import com.launcher.zy_ui06.utils.IconManager
import com.awell.library.util.LogUtil
import com.launcher.zy_ui06.view.MusicWidget
import kotlinx.coroutines.Runnable
import java.io.File
import kotlin.math.abs

class UIActivity : Activity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding
    private lateinit var locationManager: LocationManager

    private val MSG_UPDATE_SPEED = 1
    private val MSG_CLEAR_SPEED = 2
    private val BIN_DATA_SPEED_UNIT = 0x84
    private var accRecor: Boolean = false

    val PERMISSION_REQUEST_CODE: Int = 100
    private lateinit var allAppInfoList: List<AppInfo>

    lateinit var popupWindow: PopupWindow

    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private val viewConfiguration by lazy { ViewConfiguration.get(this) }
    private lateinit var musicWidget: MusicWidget
    private lateinit var mediaControl: AwellMediaControl
    private var mMediaListener = MediaNotificationListener()

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false
    private var isResumed = false
    //应用代号
    private var APP_NUMBER = 0
    //默认包名
    private val defaultPackageNum0 = "com.awell.navigation"
    private val defaultPackageNum1 = "com.awell.localvideo"
    private lateinit var customizeConfig : AppsCustomizeConfig

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewBinding = UiActivityBinding.inflate(layoutInflater)

        setContentView(mViewBinding.root)
        LogUtil.setIsDebuggable()


        initView()
        // 通过配置对象直接应用
        val themeId = getSavedThemeId()
        val savedThemeResId = getThemeResId(themeId)
        setStyleTheme(savedThemeResId)

        initMediaMusic()

        updateSpeedUnitText()

        initTouchAndSpeedListener()

        initBroadcastReceiver()

        AppsCustomizeControl.setActivity(this)
        
        // 设置主题模式，并同步到 IconManager
        val cellHeight = resources.getDimensionPixelSize(R.dimen.cell_height_dp)
        val cellWidth = resources.getDimensionPixelSize(R.dimen.cell_width_dp)
        //val icSize = resources.getDimensionPixelSize(R.dimen.app_icon_size)
        customizeConfig = AppsCustomizeConfig.Builder()
            .setThemeMode(0xff)
            .setRefresh(false)
            .setIconMap(getAppTheme(themeId))
            .setPluginPackageName("com.launcher.zy_ui06")
            .setIndicatorPanel(1)
            .setIconSize(resources.getDimensionPixelSize(R.dimen.app_icon_size))
            .setIconTextPadding(resources.getDimensionPixelSize(R.dimen.app_icon_text_padding))
            .setColumnCount(5)
            .setRowCount(2)
            .setAutoWidthGap(true)
            .setAutoHeightGap(true)
            .setFontSizeSp(resources.getDimensionPixelSize(R.dimen.font_size_sp)) // 20
            .setFontColor(Color.WHITE)
            .setCellWidthDp(cellWidth) //358
            .setCellHeightDp(cellHeight)
            .build()

                
        AppsCustomizeControl.setPluginThemeMode(customizeConfig)
        // 初始化 IconManager，预加载资源
        IconManager.initialize(this)

    }

    override fun onResume() {
        super.onResume()
        LogUtil.i("onResume")
        isResumed = true
        initAddAppView()

    }

    override fun onPause() {
        super.onPause()
        LogUtil.i("onPause")
        isResumed = false

    }

    override fun onStop() {
        super.onStop()
        if ( ::popupWindow.isInitialized &&  popupWindow.isShowing) {
            popupWindow.dismiss()
        }
    }

    /**
     * 初始化长按切换壁纸和监听gps速度变化
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initTouchAndSpeedListener() {
        handler = Handler(Looper.getMainLooper())

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if(locationManager!= null)
            locationManager.requestLocationUpdates("gps", CommonData.MEMENTINE, CommonData.MAXINSTANCES, mLocationListener, mHandle.looper)
    }

    private fun initView() {

        clickStartApp()

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initAddAppView() {
        allAppInfoList = ArrayList<AppInfo>()
        IconManager.currentThemeMode = getSavedThemeId()
        updateShortcutView(0, mViewBinding.ivNavIcon, mViewBinding.tvNavTitle)
        updateShortcutView(1, mViewBinding.ivVideoIcon, mViewBinding.tvVideoTitle)
    }

    private fun updateShortcutView(appNumber: Int, iconView: android.widget.ImageView, titleView: android.widget.TextView) {
        val pkg = getAppInfo(appNumber)
        iconView.setImageDrawable(IconManager.getAppIcon(this, pkg))
        titleView.text = getAppNameByPackageName(pkg)
    }

    private fun setShortCutDrawable(){
        updateShortcutView(0, mViewBinding.ivNavIcon, mViewBinding.tvNavTitle)
        updateShortcutView(1, mViewBinding.ivVideoIcon, mViewBinding.tvVideoTitle)
    }


    /**
     * 根据包名获取应用名称
     * @param context Context对象
     * @param packageName 应用包名
     * @return 应用名称，失败时返回null
     */
    fun getAppNameByPackageName( packageName: String): String? {

        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun startActivityByPkg(appNumber:Int,defaultPackage:String) {
        var pkg = Settings.System.getString(contentResolver,"launcher_app_icon_$appNumber")
        if (TextUtils.isEmpty(pkg) || !isAppInstalled(pkg)) {
            pkg = defaultPackage
            saveAppInfo(appNumber,pkg)
        }

        var intent =packageManager.getLaunchIntentForPackage(pkg)
        startActivity( intent)
    }


    private fun getAppInfo(appNumber:Int):String {
        var pkg = Settings.System.getString(contentResolver,"launcher_app_icon_$appNumber")
        if (TextUtils.isEmpty(pkg) || !isAppInstalled(pkg)) {
            if(appNumber == 0) {
                pkg = defaultPackageNum0
            }else {
                pkg = defaultPackageNum1
            }
            saveAppInfo(appNumber,pkg)
        }

        return pkg
    }
    private fun saveAppInfo(appNumber:Int,pkg:String) {
        Settings.System.putString(contentResolver,"launcher_app_icon_$appNumber",pkg)
    }


    val addSelectAppCallback: AddSelectAppCallback = object : AddSelectAppCallback {
        @SuppressLint( "SuspiciousIndentation")
        override fun addAppInfo(appInfo: AppInfo) {
            val packAge = getAppInfo(APP_NUMBER)
                if (packAge.equals(appInfo.package_name)) {
                    popupWindow.dismiss()
                    showPopupI.hidePopup()
                    return
                }
            saveAppInfo(APP_NUMBER,appInfo.package_name)
            val drawableBg = IconManager.getAppIcon(applicationContext,appInfo.package_name)
            if(APP_NUMBER ==0){
                mViewBinding.ivNavIcon.setImageDrawable(drawableBg)
                mViewBinding.tvNavTitle.text = appInfo.label

            }else if(APP_NUMBER ==1){
                mViewBinding.ivVideoIcon.setImageDrawable(drawableBg)
                mViewBinding.tvVideoTitle.text = appInfo.label
            }


            showPopupI.hidePopup()
        }

        override fun removeAppInfo(packageName: String?) {

            //appInfoAdapter.setContentList(showAppInfoList)
        }
    }

    private val showPopupI: ShowPopupI = object : ShowPopupI {
        override fun showPopup() {
            showPopupAllApp()
           // backgroundAlpha(0.1f)
        }

        override fun hidePopup() {
            popupWindow.dismiss()
        }
    }

    private fun showPopupAllApp() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.layout_allapp, null)
        
        // 计算屏幕尺寸的 80% 和 60%
        //val displayMetrics = resources.displayMetrics
       // val popupWidth = (displayMetrics.widthPixels * 0.8).toInt()
        //val popupHeight = (displayMetrics.heightPixels * 0.8).toInt()
        
        popupWindow = PopupWindow(
            view,
            WindowManager.LayoutParams.MATCH_PARENT  ,
           WindowManager.LayoutParams.MATCH_PARENT,
            true
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.windowLayoutType = TYPE_APPLICATION_OVERLAY
        view.setOnClickListener { popupWindow.dismiss() }

        allAppInfoList = getAllAppInfo(this, false )

        val rvPop = view.findViewById<RecyclerView>(R.id.rv_pop_allapp)
        val gridLayoutManager = GridLayoutManager(this, 4)
        gridLayoutManager.spanCount = 4
        gridLayoutManager.orientation = RecyclerView.VERTICAL
        
        // 添加垂直方向间隔（50dp）- 只在行与行之间有间隔
        val verticalSpacing = (20 * resources.displayMetrics.density).toInt()
        rvPop.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.top = verticalSpacing
                outRect.bottom = verticalSpacing
            }
        })
        
        val appInfoAdapter = AppPopAdapter(this, allAppInfoList, addSelectAppCallback)
        rvPop.layoutManager = gridLayoutManager
        rvPop.adapter = appInfoAdapter

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0,0)
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initBroadcastReceiver() {
        // 注册不需要 dataScheme 的广播
        val filter = IntentFilter()
        filter.addAction(CommonData.BROADCAST_LAMP_SWITCH)
        filter.addAction(CommonData.ACTION_ACC_ON)
        filter.addAction(CommonData.ACTION_ACC_OFF)
        filter.addAction(CommonData.ACTION_ZLINK)
        filter.addAction(CommonData.ACTION_SHOW_ALL_APP)
        filter.addAction(CommonData.BROADCAST_MEDIA_EXIT)
        filter.addAction(CommonData.ACTION_SPEED_UNIT_CHANGE)
        filter.addAction(CommonData.ACTION_TOP_SESSION_CHANGE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        
        // 单独注册需要 dataScheme 的广播
        val packageFilter = IntentFilter()
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        packageFilter.addDataScheme("package")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, packageFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, packageFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaListener.cleanup()
        unregisterReceiver(receiver)

        AppsCustomizeControl.setActivity(null)
        cancelLongPressDetection()
        AppsCustomizeControl.hideApps()
        unregisterCustomerListener()
        mHandle.removeCallbacksAndMessages(null)
        try {
            mediaControl.unBindDataService(this)
        } catch (e: Exception) {
            LogUtil.e("onDestroy: unBindDataService error=>${e.message}")
        }
    }

    private fun unregisterCustomerListener() {
         try {
             locationManager.removeUpdates(mLocationListener)
         } catch (e: Exception) {
            LogUtil.e("unregisterCustomerListener: removeGpsStatusListener error=>${e.message}")
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
        
        // 通过 ViewBinding 获取 MusicWidget (include 的 root 就是 MusicWidget 实例)
        musicWidget = mViewBinding.layoutMusicWidget.root
        musicWidget.setMediaLibrary(mediaControl)
        musicWidget.setActivity(this, musicWidget)
        mediaControl.refreshCurrentMediaState()

    }


    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(!isResumed){
                LogUtil.i("onReceive,The current interface is not visible")
                return
            }
            val action = intent?.action
            LogUtil.i("action=$action")
            when (action) {
                CommonData.BROADCAST_LAMP_SWITCH -> {
                    if (intent.getIntExtra("lamplet_state", 0) == 1) {
                       // mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.open)
                    } else {
                       // mViewBinding.ivLampSwitchBg.setImageResource(R.drawable.off)
                    }
                }

                CommonData.ACTION_ACC_ON -> {
                    if(mViewBinding.tvGpsSpeed!=null)
                        mViewBinding.tvGpsSpeed.postDelayed({ accRecor = false }, 8 * 1000)

                }

                CommonData.ACTION_ACC_OFF -> {
                    accRecor = true

                }

                CommonData.BROADCAST_MEDIA_EXIT -> {
                    val pkg = intent.getStringExtra("package")
                    if (pkg != null && (pkg == "cn.kuwo.kwmusiccar" || pkg == "exitAll")) {

                    }
                }

                CommonData.ACTION_ZLINK -> {
                    val zlinkStatus = intent.getStringExtra("status")
                    val phoneMode = intent.getStringExtra("phoneMode")
                    LogUtil.d("zlinkStatus:$zlinkStatus")
                    if (zlinkStatus == null) {
                        return
                    }
                    LogUtil.i(

                        "onReceive: mediaControl.getCurrentPkgName()=>${mediaControl.getCurrentPkgName()}"
                    )
                    if (mediaControl.getCurrentPkgName()?.equals(CommonData.ACTION_ZLINK) == true) {
                        if ("REFRESH_JEPG" == zlinkStatus) {
                            updateCarplayImageAlbum()
                        }
                        musicWidget.setCarPlayData(zlinkStatus, phoneMode)
                    }
                }

                CommonData.ACTION_SHOW_ALL_APP -> {
                    Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
                    LogUtil.w("freeform_launcher_idle,0")
                    AppsCustomizeControl.showApps(findViewById<ViewGroup>(android.R.id.content))
                }

                CommonData.ACTION_SPEED_UNIT_CHANGE -> {
                    updateSpeedUnitText()
                }

                CommonData.ACTION_TOP_SESSION_CHANGE -> {
                    val sessionTopPkg = intent.getStringExtra(CommonData.EXTRA_TOP_PACKAGE)
                    handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                }

                Intent.ACTION_PACKAGE_REMOVED -> {
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        var packageName = intent.getDataString()
                        if (packageName != null) {
                            // Remove the package scheme prefix
                            packageName = packageName.replace("package:", "")
                            getAppInfo(0)?.let {
                                if (it == packageName) {
                                    saveAppInfo(0, defaultPackageNum0)
                                }
                            }
                            getAppInfo(1)?.let {
                                if (it == packageName) {
                                    saveAppInfo(1, defaultPackageNum1)
                                }
                            }

                            // Update the app list to remove the uninstalled app
                            //updateAppListAfterUninstall(packageName)
                        }
                    }
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
            mViewBinding.tvGpsSpeedUnit.text = "km/h"
        } else if (unitData == 1) {
            mViewBinding.tvGpsSpeedUnit.text = "mph"
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

                    if ("com.awell.localmusic".equals(pkg) ) {
                        musicWidget.switchMediaController(
                            pkg, command, mediaType, currentMedia
                        )
                    }
                }
            }

            override fun updateViewPlayStatus(
                bundle: Bundle, status: Boolean, type: Int
            ) {
                runOnUiThread {
                    if (type == MusicWidget.MUSIC)
                        musicWidget.setCurMusicState(status, type)
                }
            }

            @SuppressLint("UseKtx")
            override fun updateViewMusicPlayImage(bundle: Bundle) {
                runOnUiThread {
                    val uriStr = bundle.getString(AwellTool.VALUE_M1, null)
                    val uri = uriStr?.let {
                        Uri.parse(it).takeIf { uri -> uri.scheme != null }
                    }
                    musicWidget.loadAlbumArtByUri(uri)
                }
            }

            override fun updateViewPlayInfo(
                bundle: Bundle, songName: String, singerName: String, album: String, type: Int
            ) {
                runOnUiThread {

                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {
                        musicWidget.setMusicNameTextView("",MusicWidget.MUSIC)
                        musicWidget.setArtistNameTextView(
                            getResources().getString(R.string.music_artist),
                            MusicWidget.MUSIC
                        )
                    }else{
                        musicWidget.setMusicNameTextView(songName, type)
                        musicWidget.setArtistNameTextView(singerName, type)
                    }
//                    musicWidget.setMusicNameTextView(songName, type)
//                    musicWidget.setArtistNameTextView(singerName, type)
//                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {
//                        musicWidget.setMusicNameTextView(
//                            /*getResources().getString(R.string.click_play_music)*/ "", MusicWidget.MUSIC
//                        )
//                        musicWidget.setArtistNameTextView(
//                            getResources().getString(R.string.music_artist), MusicWidget.MUSIC
//                        )
//                    }
//
//                    if (MusicWidget.OTHER_MUSIC == type) {
//                        if (!TextUtils.isEmpty(songName)) {
//                            musicWidget.setMusicNameTextView(songName, MusicWidget.OTHER_MUSIC)
//                        } else {
//                            musicWidget.setMusicNameTextView(
//                                /*getResources().getString(R.string.click_play_music)*/"",
//                                MusicWidget.OTHER_MUSIC
//                            )
//                        }
//                        if (!TextUtils.isEmpty(singerName)) {
//                            musicWidget.setArtistNameTextView(
//                                singerName, MusicWidget.OTHER_MUSIC
//                            )
//                        } else {
//                            musicWidget.setArtistNameTextView(
//                                getResources().getString(R.string.music_artist),
//                                MusicWidget.OTHER_MUSIC
//                            )
//                        }
//                    }
                }
            }

            override fun updateViewPlayTime(
                bundle: Bundle, currentTime: Long, totalTime: Long, type: Int
            ) {
                /* runOnUiThread {
                    llMusic.setMusicSeekBar(
                        currentTime.toInt(), totalTime.toInt(), type
                    )
                } */
            }

            override fun updateViewRadioFreq(bundle: Bundle, fmOrAm: String, freq: String, unit: String ) {
                runOnUiThread {
                    runOnUiThread(java.lang.Runnable {
                        mViewBinding.layoutRadioLayout.tvRadioFreq.text = freq +"  "+ unit
                      //  mViewBinding.layoutRadioLayout.tvRadioAmFm.text = fmOrAm

                    })
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
                        if (!accRecor) {
                            val unit = ByteArray(1)
                            CommonData.readDataToMeta(unit, BIN_DATA_SPEED_UNIT)
                            val unitData = unit[0].toInt()
                            if (unitData == 0) {
                                mViewBinding.tvGpsSpeed.text = speedKm
                                mViewBinding.tvGpsSpeedUnit.text = "km/h"
                            } else if (unitData == 1) {
                                mViewBinding.tvGpsSpeed.text = speedMile
                                mViewBinding.tvGpsSpeedUnit.text = "mph"
                            }
                            mHandle.removeMessages(MSG_CLEAR_SPEED)
                            // GPS 实际上报间隔(minTime 下限+低速距离阈值)可达 2~5 秒,
                            // 2 秒超时会在正常行驶时把速度清成 0(每 2 秒跳 0),改为 3 秒安全网
                            mHandle.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 3000)
                        }
                    }

                    MSG_CLEAR_SPEED -> {
                        mViewBinding.tvGpsSpeed.text = 0.toString()
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

    private fun clickStartApp() {

//        mViewBinding.homeAppNav.setOnClickListener {
//            startActivity("com.awell.navigation", "com.awell.navigation.MainActivity")
//        }

        mViewBinding.ivNavBg.setOnLongClickListener{
            APP_NUMBER = 0
            showPopupI.showPopup()
            return@setOnLongClickListener true
        }

        mViewBinding.ivVideoBg.setOnLongClickListener {
            APP_NUMBER = 1
            showPopupI.showPopup()
            return@setOnLongClickListener true
        }

        mViewBinding.llTime.setOnClickListener(this)
        //mViewBinding.layoutRadioLayout.radioLayout.setOnClickListener(this)
        mViewBinding.layoutRadioLayout.ivRadioNext.setOnClickListener(this)
        mViewBinding.layoutRadioLayout.ivRadioPre.setOnClickListener(this)
        mViewBinding.homeAppSetting.setOnClickListener(this)
        mViewBinding.homeAppAllApp.setOnClickListener(this)
        mViewBinding.homeAppBluetooth.setOnClickListener(this)
        mViewBinding.homeAppEq.setOnClickListener(this)


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

    private fun getAllAppInfo(context: Context, isFilterSystem: Boolean): ArrayList<AppInfo> {
        val appBeanList: ArrayList<AppInfo> = ArrayList()

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val packageManager = context.packageManager
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        val needToBlockApps = mutableListOf<String?>(
             "com.awell.localmusic", "com.awell.radio","com.awell.launcher.host", "com.android.inputmethod.latin", "com.iflytek.inputmethod.pad"
         );
        val appList: List<PackageInfo> = resolveInfos.mapNotNull { resolveInfo ->
            try {
                packageManager.getPackageInfo(resolveInfo.activityInfo.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }.distinctBy { it.packageName }.filter { it.packageName !in needToBlockApps }

        LogUtil.w("appList size: ${appList.size}")

        // 预计算过滤条件，避免重复计算
        //val needToShowSystemApps =Utils.needToShowPackageName
        //val filterApps = Utils.filterAppPackageName
        //val otherNeedToShow = Utils.otherNeedToShowPackageName


        // 先过滤出需要显示的应用包名列表
        val needShowPackages = mutableListOf<String>()
        val packageInfoMap = mutableMapOf<String, PackageInfo>()

        for (p in appList) {
            val packageName = p.applicationInfo.packageName
            //val flags = p.applicationInfo.flags

            /* val shouldShow = when {

                (flags and ApplicationInfo.FLAG_SYSTEM) != 0 && packageName in needToShowSystemApps -> true
                (flags and ApplicationInfo.FLAG_SYSTEM) == 0 && packageName !in filterApps -> true
                (flags and ApplicationInfo.FLAG_SYSTEM) != 0 && packageName in otherNeedToShow -> true
                else -> false
            }


            if (shouldShow) {*/
                needShowPackages.add(packageName)
                packageInfoMap[packageName] = p
            //}
        }

        LogUtil.w("needShowPackages size: ${needShowPackages.size}")
        IconManager.currentThemeMode =getSavedThemeId()

        // 临时使用普通加载方式 (规避协程问题)
        for (packageName in needShowPackages) {
            val bean = AppInfo()
            bean.setIcon(IconManager.getAppIcon(context, packageName))
            val p = packageInfoMap[packageName]
            if (p != null) {
                bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString())
                bean.setPackage_name(packageName)
                bean.setFlags(p.applicationInfo.flags)
                appBeanList.add(bean)
            }
        }

        LogUtil.w("appBeanList size: ${appBeanList.size}")
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

    override fun onClick(v: View) {

        when (v.id) {

            mViewBinding.ivRadioBg.id -> {
                startActivity("com.awell.radio", "com.awell.radio.AwellFmActivity")
            }

            mViewBinding.layoutRadioLayout.ivRadioNext.id -> {
                if (ClickUtils.isFastClick()) {
                    return
                }
                mediaControl.sendStrToHost(AwellTool.RADIO.NEXT)

            }
            mViewBinding.layoutRadioLayout.ivRadioPre.id -> {
                if (ClickUtils.isFastClick()) {
                    return
                }
                mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS)
            }
//            mViewBinding.layoutRadioLayout.ivRadioPlayPause.id -> {
//            }
//            mViewBinding.layoutRadioLayout.tvRadioAmFm.id -> {
//                mediaControl.sendStrToHost(AwellTool.RADIO.SET_FMAM)
//            }

            mViewBinding.llTime.id-> {
                val intent = Intent(Settings.ACTION_DATE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            mViewBinding.hostSwitch.id -> {
                if (ClickUtils.isFastClickFiveSecond()) {//5秒内快速点击无效, 防止重复切换主题壁纸
                   return
                }
                showPopupColor.showPopup()
            }
            mViewBinding.ivNavBg.id -> {
                startActivityByPkg(0,defaultPackageNum0)
            }
            mViewBinding.ivVideoBg.id -> {
                startActivityByPkg(1,defaultPackageNum1)
            }
            mViewBinding.homeAppSetting.id -> {
                startActivity(
                    "com.awell.carsetting", "com.awell.carsetting.MainActivity"
                )
            }
            mViewBinding.homeAppAllApp.id -> {
                AppsCustomizeControl.showApps(findViewById(android.R.id.content));
            }
            mViewBinding.homeAppBluetooth.id -> {
                startActivity(
                    "com.awell.bluetooth", "com.awell.bluetooth.MainActivity"
                )
            }
            mViewBinding.homeAppEq.id -> {
                startActivity("com.awell.eqselect", "com.awell.eqselect.MainActivity");
            }
            mViewBinding.tvGpsSpeedUnit.id -> {
                if (ClickUtils.isFastClick()) {
                    return
                }
                val unit = ByteArray(1)
                CommonData.readDataToMeta(unit, BIN_DATA_SPEED_UNIT)
                val unitData = unit[0].toInt()
                if (unitData == 0) {//km/h
                    mViewBinding.tvGpsSpeedUnit.text = "mph"
                    unit[0]=1
                } else if (unitData == 1) {//mph
                    mViewBinding.tvGpsSpeedUnit.text = "km/h"
                    unit[0]=0
                }
                CommonData.writeDataToMeta( unit, BIN_DATA_SPEED_UNIT)
            }

        }
    }

    //======================主题切换 start======================================
    
    /**
     * 数据类：定义属性ID与ImageView的映射关系
     */
    private data class ThemeImageMapping(
        val attrId: Int,
        val imageView: android.widget.ImageView
    )
    
    /**
     * 批量设置ImageView的主题资源
     * @param styleResId 样式资源ID
     * @param mappings 属性ID与ImageView的映射列表
     */
    private fun applyThemeImages(styleResId: Int, vararg mappings: ThemeImageMapping) {
        mappings.forEach { mapping ->
            val bgResId = StyleParser.getResourceIdFromStyle(this, styleResId, mapping.attrId)
            if (bgResId != 0) {
                mapping.imageView.setImageResource(bgResId)
            }
        }
    }
    
    private fun setStyleTheme(styleResId: Int) {
        // 批量应用图片资源 - 使用声明式配置，易于维护和扩展
        applyThemeImages(
            styleResId,
            ThemeImageMapping(R.attr.all_app_bg, mViewBinding.homeAppAllApp),
            ThemeImageMapping(R.attr.bt_app_bg, mViewBinding.homeAppBluetooth),
            ThemeImageMapping(R.attr.setting_app_bg, mViewBinding.homeAppSetting),
            ThemeImageMapping(R.attr.eq_app_bg, mViewBinding.homeAppEq),

            ThemeImageMapping(R.attr.radio_bg, mViewBinding.ivRadioBg),
            ThemeImageMapping(R.attr.navi_bg, mViewBinding.ivNavBg),
            ThemeImageMapping(R.attr.music_bg, mViewBinding.layoutMusicWidget.ivMusicBg),
            ThemeImageMapping(R.attr.video_bg, mViewBinding.ivVideoBg),

        )

        // 应用颜色资源
        val themeBgResId = StyleParser.getResourceIdFromStyle(this, styleResId, R.attr.ui_theme_bg)
        if (themeBgResId != 0) {
            mViewBinding.ivBg.setImageResource(themeBgResId)
        }

    }

    //保存数据
    companion object {
        private const val PREFS_NAME = "zy_lanbo_theme"
        private const val KEY_THEME_ID = "theme_id"  // Store simple ID (1-4), NOT resource ID
        private const val DEFAULT_THEME_ID = 0
        private const val MAX_THEME_ID = 6
    }

    /**
     * Get saved theme ID from SharedPreferences
     * @return Theme ID (1-4), or DEFAULT_THEME_ID if not found
     */
    private fun getSavedThemeId(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId = prefs.getInt(KEY_THEME_ID, DEFAULT_THEME_ID)
        return themeId.coerceIn(DEFAULT_THEME_ID, MAX_THEME_ID) // Ensure valid range
    }

    /**
     * Convert theme ID to theme resource ID
     */
    private fun getThemeResId(themeId: Int): Int {
        return when (themeId) {
            0 -> R.style.AppTheme0
            1 -> R.style.AppTheme1
            2 -> R.style.AppTheme2
            3 -> R.style.AppTheme3
            4 -> R.style.AppTheme4
            5 -> R.style.AppTheme5
            6 -> R.style.AppTheme6
            else -> R.style.AppTheme0
        }
    }

    /**
     * Save theme ID to SharedPreferences and apply the theme
     */
    private fun saveAndApplyTheme(themeId: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_ID, themeId).apply()
        setStyleTheme(getThemeResId(themeId))
        // 更新音乐播放控件的主题（根据音乐状态切换不同颜色的图片）
        musicWidget.setTheme(themeId)
        //刷新快捷方式图标
        IconManager.currentThemeMode = themeId
        setShortCutDrawable()
        if (customizeConfig!= null)
            customizeConfig.iconMap = getAppTheme(themeId)
        AppsCustomizeControl.setPluginThemeMode(customizeConfig)


    }

    //=============================主题切换 end===============================

    private val showPopupColor: ShowPopupI = object : ShowPopupI {
        override fun showPopup() {
            setPopupColor()
          // backgroundAlpha(0.1f)
        }

        override fun hidePopup() {
            popupWindow.dismiss()
        }
    }


    private fun setPopupColor() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.layout_select_color, null)

        // 计算屏幕尺寸的 80% 和 60%
        //val displayMetrics = resources.displayMetrics
        //val popupWidth = (displayMetrics.widthPixels * 0.8).toInt()
        //val popupHeight = (displayMetrics.heightPixels * 0.8).toInt()

        popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        view.setOnClickListener { popupWindow.dismiss() }

        popupWindow.isOutsideTouchable = true
        popupWindow.windowLayoutType = TYPE_APPLICATION_OVERLAY
        val colorId = getSavedThemeId()
        val bgMain = view.findViewById<ConstraintLayout>(R.id.select_dialog_main)
        bgMain.setBackgroundResource(getSelectColorBg(colorId))
        val ivColor1 = view.findViewById<CheckBox>(R.id.iv_color1)
        val ivColor2 = view.findViewById<CheckBox>(R.id.iv_color2)
        val ivColor3 = view.findViewById<CheckBox>(R.id.iv_color3)
        val ivColor4 = view.findViewById<CheckBox>(R.id.iv_color4)
        val ivColor5 = view.findViewById<CheckBox>(R.id.iv_color5)
        val ivColor6 = view.findViewById<CheckBox>(R.id.iv_color6)
        val ivColor7 = view.findViewById<CheckBox>(R.id.iv_color7)
        when (colorId) {
            0 -> {  ivColor1.isChecked = true }
            1 -> { ivColor2.isChecked = true }
            2 -> { ivColor3.isChecked = true }
            3 -> { ivColor4.isChecked = true }
            4 -> { ivColor5.isChecked = true }
            5 -> { ivColor6.isChecked = true }
            6 -> { ivColor7.isChecked = true }
        }
        listOf(ivColor1, ivColor2, ivColor3,ivColor4,ivColor5,ivColor6,ivColor7).forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    saveAndApplyTheme(buttonView.tag.toString().toInt())
                    popupWindow.dismiss()
                }
            }
        }

        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0)
    }

    private fun getSelectColorBg(colorId: Int): Int {
        return when (colorId) {
            0 -> R.drawable.ic_home_layout_bg0
            1 -> R.drawable.ic_home_layout_bg1
            2 -> R.drawable.ic_home_layout_bg2
            3 -> R.drawable.ic_home_layout_bg3
            4 -> R.drawable.ic_home_layout_bg4
            5 -> R.drawable.ic_home_layout_bg5
            6 -> R.drawable.ic_home_layout_bg6
            else -> R.drawable.ic_home_layout_bg0
        }
    }
    private fun getAppTheme(appNumber:Int): Map<String, String> {
        return when (appNumber) {
            0 -> IconManager.PACKAGE_ICON_MAP_ONE
            1 -> IconManager.PACKAGE_ICON_MAP_TWO
            2 -> IconManager.PACKAGE_ICON_MAP_THREE
            3 -> IconManager.PACKAGE_ICON_MAP_FOUR
            4 -> IconManager.PACKAGE_ICON_MAP_FIVE
            5 -> IconManager.PACKAGE_ICON_MAP_SIX
            6 -> IconManager.PACKAGE_ICON_MAP_SEVEN
            else -> IconManager.PACKAGE_ICON_MAP_ONE
        }
    }
}