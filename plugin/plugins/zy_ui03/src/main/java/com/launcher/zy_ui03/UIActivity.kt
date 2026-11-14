package com.launcher.zy_ui03

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.os.SystemProperties
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.widget.PopupWindow
import androidx.annotation.RequiresPermission
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.awell.addapp.AddSelectAppCallback
import com.awell.addapp.AppInfo
import com.awell.addapp.AppListStorage
import com.awell.addapp.ShowPopupI
import com.awell.control.AppsCustomizeConfig
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.launcher2.IconCache
import com.awell.launcher2.MediaNotificationListener
import com.awell.library.AwellTool
import com.awell.utils.CommonData
import com.awell.utils.Utils
import com.awell.utils.Utils.startWallpaper
import com.launcher.zy_ui03.adapter.AppInofAdapter
import com.launcher.zy_ui03.adapter.AppPopAdapter
import com.launcher.zy_ui03.control.StyleParser
import com.launcher.zy_ui03.databinding.UiActivityBinding
import com.awell.library.util.ClickUtils
import com.launcher.zy_ui03.utils.IconManager
import com.awell.library.util.LogUtil
import com.launcher.zy_ui03.view.MusicWidget
import kotlinx.coroutines.Runnable
import java.io.File
import kotlin.math.abs

class UIActivity : Activity(), View.OnClickListener {

    //private val TAG = UIActivity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding
    private lateinit var locationManager: LocationManager

    private val MSG_UPDATE_SPEED = 1
    private val MSG_CLEAR_SPEED = 2
    private val BIN_DATA_SPEED_UNIT = 0x84
    private var accRecor: Boolean = false

    val PERMISSION_REQUEST_CODE: Int = 100

    //private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var appInfoAdapter: AppInofAdapter
    private lateinit var allAppInfoList: List<AppInfo>
    private lateinit var showAppInfoList: ArrayList<AppInfo>

    lateinit var popupWindow: PopupWindow

    private lateinit var placehodlerInfo: AppInfo
    //private lateinit var myDbHelper: MyDbHelper
    //private lateinit var sqLiteDatabase: SQLiteDatabase

    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null
    private lateinit var musicWidget: MusicWidget
    private lateinit var mediaControl: AwellMediaControl
    private var mMediaListener = MediaNotificationListener()

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false
    private var isResumed = false

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mViewBinding = UiActivityBinding.inflate(layoutInflater)

        setContentView(mViewBinding.root)
        LogUtil.setIsDebuggable()


        initView()
        // 通过配置对象直接应用
        val savedThemeResId = getThemeResId(getSavedThemeId())
        setStyleTheme(savedThemeResId)
        // 校验主题与壁纸是否一致（上次设壁纸失败 / 进程被杀 / prefs 丢失时自动补设）
        syncWallpaperWithSavedTheme()

        initMediaMusic()

        updateSpeedUnitText()

        initTouchAndSpeedListener()

        initBroadcastReceiver()

        AppsCustomizeControl.setActivity(this)
        
        // 设置主题模式，并同步到 IconManager
       // AppsCustomizeControl.setAppIconSize(getResources().getDimensionPixelSize(R.dimen.app_icon_size))
        //val cellHeight = resources.getDimensionPixelSize(R.dimen.cell_height_dp)//126
        val cellHeight = resources.getDimensionPixelSize(R.dimen.cell_height_dp)
        val cellWidth = resources.getDimensionPixelSize(R.dimen.cell_width_dp)
        val icSize = resources.getDimensionPixelSize(R.dimen.app_icon_size)
        val config = AppsCustomizeConfig.Builder()
            .setIconSize(resources.getDimensionPixelSize(R.dimen.app_icon_size))
            .setIconOffsetX((cellHeight - icSize) / 2)
            .setIconTextPadding(resources.getDimensionPixelSize(R.dimen.app_icon_text_padding) + (cellHeight - icSize - 4) / 2)
            .setColumnCount(3)
            .setRowCount(4)
            .setAutoWidthGap(true)
            .setAutoHeightGap(true)
            .setFontSizeSp(resources.getDimensionPixelSize(R.dimen.font_size_sp)) // 20
            .setTextOrientation(2)
            .setTextGravity(Gravity.CENTER_VERTICAL or Gravity.START)
            .setFontColor(Color.WHITE)
            .setCellWidthDp(cellWidth) //358
            .setCellHeightDp(cellHeight)
            .setBackgroundTheme(1)
            .setThemeMode(0xff)
            .setRefresh(false)
            .setIconSizeSame(true)
            .setIconMap(IconManager.PACKAGE_ICON_MAP)
            .setPluginPackageName("com.launcher.zy_ui03")
            .build()
        AppsCustomizeControl.setPluginThemeMode(config)
        // 初始化 IconManager，预加载资源
        IconManager.initialize(this)

    }



    override fun onResume() {
        super.onResume()
        LogUtil.i("onResume")
        isResumed = true

        // Verify all apps in the list are still installed
        verifyInstalledApps()

        // 回前台再校验一次主题/壁纸一致性（例如上次设壁纸超时失败后自动补设）
        syncWallpaperWithSavedTheme()

    }

    override fun onPause() {
        super.onPause()
        LogUtil.i("onPause")
        isResumed = false

    }



    /**
     * 初始化长按切换壁纸和监听gps速度变化
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initTouchAndSpeedListener() {
        handler = Handler(Looper.getMainLooper())
        viewConfiguration = ViewConfiguration.get(this)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if(locationManager!= null)
            locationManager.requestLocationUpdates("gps", CommonData.MEMENTINE, CommonData.MAXINSTANCES, mLocationListener, mHandle.looper)
    }

    private fun initView() {

        clickStartApp()

        initAddAppView()

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initAddAppView() {
        showAppInfoList = ArrayList<AppInfo>()
        allAppInfoList = ArrayList<AppInfo>()

        placehodlerInfo = createMismatchPlaceholder();
        // 获取已保存需要显示的app包名，如果没有，则显示默认
        //myDbHelper = MyDbHelper(this, "show_app", null, 1)
        //sqLiteDatabase = myDbHelper.writableDatabase

        appInfoAdapter =
            AppInofAdapter(this, showAppInfoList, showPopupI, addSelectAppCallback)

        // 设置item垂直居中
        mViewBinding.rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mViewBinding.rv.adapter = appInfoAdapter

        Thread {
            showHostApp()
        }.start()

    }

    private fun showHostApp() {
        // 刷新应用列表
        refreshAppListAsync()

        runOnUiThread {
            appInfoAdapter.setContentList(showAppInfoList)
            appInfoAdapter.notifyDataSetChanged()
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
            AppListStorage.addItem(this@UIActivity, appInfo.package_name)

            showPopupI.hidePopup()
        }

        override fun removeAppInfo(packageName: String?) {
            AppListStorage.removeItem(this@UIActivity, packageName);
            for (pack in showAppInfoList)
                if (pack.package_name.equals(packageName)) {
                    showAppInfoList.remove(pack)
                    break
                }

            appInfoAdapter.setContentList(showAppInfoList)
        }

    }

    var lastSpeed: Float = 0F

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

        allAppInfoList = Utils.getAllAppInfo(this, false)

        val rvPop = view.findViewById<RecyclerView>(R.id.rv_pop_allapp)
        val gridLayoutManager = GridLayoutManager(this, 2)
        gridLayoutManager.orientation = RecyclerView.HORIZONTAL
        
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
                    if( mViewBinding.tvGpsSpeed!=null)
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
                            // Update the app list to remove the uninstalled app
                            updateAppListAfterUninstall(packageName)
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
            mViewBinding.tvGpsSpeedUnit.text = "KM/h"
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

//                    if ("com.awell.radio" == pkg) {
//                        if ("start" == command) {
//                            mRadioLayout.waveformView.startAnimation()
//                            //mWaveformView.startAnimation()
//                        } else if ("stop" == command) {
//                            mRadioLayout.waveformView.stopAnimation()
//                            //mWaveformView.stopAnimation()
//                        }
//                    }
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

                    //musicWidget.setMusicNameTextView(songName, type)
                    //musicWidget.setArtistNameTextView(singerName, type)
                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {
                        musicWidget.setMusicNameTextView(
                            getResources().getString(R.string.click_play_music), MusicWidget.MUSIC
                        )
                        musicWidget.setArtistNameTextView(
                            getResources().getString(R.string.music_artist), MusicWidget.MUSIC
                        )
                    }else{
                        musicWidget.setMusicNameTextView(songName, type)
                        musicWidget.setArtistNameTextView(singerName, type)
                    }


                   /* if (MusicWidget.OTHER_MUSIC == type) {
                        if (!TextUtils.isEmpty(songName)) {
                            musicWidget.setMusicNameTextView(songName, MusicWidget.OTHER_MUSIC)
                        } else {
                            musicWidget.setMusicNameTextView(
                                getResources().getString(R.string.click_play_music),
                                MusicWidget.OTHER_MUSIC
                            )
                        }
                        if (!TextUtils.isEmpty(singerName)) {
                            musicWidget.setArtistNameTextView(
                                singerName, MusicWidget.OTHER_MUSIC
                            )
                        } else {
                            musicWidget.setArtistNameTextView(
                                getResources().getString(R.string.music_artist),
                                MusicWidget.OTHER_MUSIC
                            )
                        }
                    }*/
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
                        mViewBinding.layoutRadioLayout.tvRadioFreq.text = freq
                        mViewBinding.layoutRadioLayout.tvRadioAmFm.text = fmOrAm

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
                                mViewBinding.tvGpsSpeedUnit.text = "KM/h"
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

        mViewBinding.homeSwitchStyle.setOnClickListener {
            /*if (ClickUtils.isFastClickFiveSecond()) {//5秒内快速点击无效, 防止重复切换主题壁纸
                return@setOnClickListener
            }*/
            switchToNextTheme()

        }


        mViewBinding.homeAppSetting.setOnClickListener {
            startActivity(
                "com.awell.carsetting", "com.awell.carsetting.MainActivity"
            )
        }

        mViewBinding.homeAppAllApp.setOnClickListener {
            AppsCustomizeControl.showApps(findViewById(android.R.id.content));
        }

        mViewBinding.homeAppBluetooth.setOnClickListener {
            startActivity(
                "com.awell.bluetooth", "com.awell.bluetooth.MainActivity"
            )
        }

//        mViewBinding.freeformFullScreen.setOnClickListener(this)
//        mViewBinding.freeformFullScreen.setOnLongClickListener {
//            cancelLongPressDetection()
//            val mIntent = Intent()
//            mIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            mIntent.setPackage("com.awell.carsetting")
//            mIntent.component =
//                ComponentName("com.awell.carsetting", "com.awell.carsetting.MainActivity")
//            mIntent.putExtra("SelectDefaultId", 3)
//            mIntent.putExtra("SelectDefaultFragment", 30)
//            startActivity(mIntent)
//            true
//        }


        mViewBinding.llTime.setOnClickListener(this)
        mViewBinding.layoutRadioLayout.radioLayout.setOnClickListener(this)
        mViewBinding.layoutRadioLayout.ivRadioNext.setOnClickListener(this)
        mViewBinding.layoutRadioLayout.ivRadioPre.setOnClickListener(this)
       // mViewBinding.layoutRadioLayout.ivRadioPlayPause.setOnClickListener(this)
        mViewBinding.layoutRadioLayout.tvRadioAmFm.setOnClickListener(this)

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
        // 长按进入系统壁纸选择器：壁纸从此由用户自定义，主题切换不再自动接管。
        // 1) 作废在途的主题壁纸任务，避免它晚到把用户选的壁纸覆盖掉；
        // 2) 打标记，避免启动/回前台的一致性校验把用户选的壁纸覆盖成主题壁纸
        wallpaperGeneration.incrementAndGet()
        pendingWallpaperThemeId = WALLPAPER_THEME_UNKNOWN
        saveAppliedWallpaperThemeId(this, WALLPAPER_THEME_CUSTOM)
        startWallpaper()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
//            mViewBinding.freeformFullScreen.id -> {
//                systemUIClient.fullScreenFreeform()
//            }
            mViewBinding.layoutRadioLayout.radioLayout.id -> {
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
            mViewBinding.layoutRadioLayout.tvRadioAmFm.id -> {
                mediaControl.sendStrToHost(AwellTool.RADIO.SET_FMAM)
            }


            mViewBinding.llTime.id-> {
                val intent = Intent(Settings.ACTION_DATE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
            mViewBinding.homeAppNav.id,
            mViewBinding.ivCarBg.id -> {
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
        }
    }

    private fun updateAppListAfterUninstall(packageName: String) {
        // Remove from showAppInfoList
        val iterator: MutableIterator<AppInfo?> = showAppInfoList.iterator()
        while (iterator.hasNext()) {
            val appInfo = iterator.next()
            if (appInfo != null && packageName == appInfo.package_name) {
                iterator.remove()
                break
            }
        }

        // Remove from database
        AppListStorage.removeItem(this, packageName);

        // Refresh the adapter
        runOnUiThread(object : Runnable {
            override fun run() {
                if (appInfoAdapter != null) {
                    appInfoAdapter.setContentList(showAppInfoList)
                }
            }
        })
    }

    private fun refreshAppListAsync() {
        // 1. 后台：获取所有已安装应用
        val freshAllList: ArrayList<AppInfo> = Utils.getAllAppInfo(this, false)
        allAppInfoList = freshAllList

        // 2. 后台：从 SP 加载保存的应用
        val storageAppList = AppListStorage.load(this)

        // 3. 后台：构建新的显示列表
        val newShowList: MutableList<AppInfo> = ArrayList<AppInfo>()
        for (packageName in storageAppList) {
            val app = Utils.getAppInfoFromPackage(packageName, freshAllList)
            if (app != null) {
                newShowList.add(app)
            }
        }
        if (newShowList.isEmpty()) {
            // 后台：加载默认应用
            loadDefaultAppsInto(newShowList, freshAllList)
        }
        // 4. 后台：保存到 SP
        saveAppListToPref(newShowList)
        // 5. 后台：添加占位符
        addAppPlaceholderTo(newShowList)

        // 6. 统一替换 showAppInfoList
        showAppInfoList = newShowList as ArrayList<AppInfo>
    }

    private fun loadDefaultAppsInto(target: MutableList<AppInfo>, allApps: MutableList<AppInfo>) {
        val defaultApps = arrayOf<String>("com.google.android.apps.maps", "com.awell.eqselect", "com.awell.bluetooth", "com.awell.localmusic","cn.kuwo.kwmusiccar")
        for (packName  in defaultApps) {
            val appInfo = Utils.getAppInfoFromPackage(packName, allApps)
            if (appInfo != null) target.add(appInfo)
        }
    }

    private fun saveAppListToPref(list: MutableList<AppInfo>) {
        val packageNames: MutableList<String> = ArrayList<String>()
        for (app in list) {
            if (app != null) packageNames.add(app.package_name)
        }
        AppListStorage.save(this, packageNames)
    }

    private fun addAppPlaceholderTo(list: MutableList<AppInfo>) {
        list.add(placehodlerInfo)
    }
    private fun verifyInstalledApps() {
        if (showAppInfoList == null || showAppInfoList.isEmpty()) return

        // 刷新应用列表
        if (appInfoAdapter != null) {
            appInfoAdapter.setContentList(showAppInfoList)
        }

        // 检查应用数量是否匹配
        //checkAppCountMatch()


        // 检查应用数量是否匹配
        //checkAppCountMatch();

        // 刷新适配器
        Thread(java.lang.Runnable {
            // 后台加载所有已安装应用并更新 showAppInfoList
            refreshAppListAsync()
            runOnUiThread(java.lang.Runnable {
                if (appInfoAdapter != null) {
                    appInfoAdapter.setContentList(showAppInfoList)
                }
                com.awell.utils.LogUtil.i("end ,verifyInstalledApps")
            })
        }).start()
    }


    /**
     * 创建占位符应用信息（用于“添加应用”按钮）
     * 注意：此方法会根据当前主题动态加载图标
     */
    private fun createMismatchPlaceholder(): AppInfo {
        val mismatchPlaceholder = AppInfo()
        
        // 从当前主题中获取 add_app_bg 资源
        val currentThemeResId = getThemeResId(getSavedThemeId())
        val addAppBgResId = StyleParser.getResourceIdFromStyle(this, currentThemeResId, R.attr.add_app_bg)
        
        // 如果主题中没有定义，则使用默认资源
        val iconResId = if (addAppBgResId != 0) addAppBgResId else R.drawable.app_add_bg1
        
        mismatchPlaceholder.setIcon(getDrawable(iconResId))
        mismatchPlaceholder.setLabel(getString(R.string.add_app))
        mismatchPlaceholder.package_name = "placeholder_mismatch"
        return mismatchPlaceholder
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
    
    /**
     * 更新占位符图标的主题资源
     * 当切换主题时调用此方法刷新"添加应用"按钮的图标
     */
    private fun updatePlaceholderTheme() {
        // 从当前主题中获取新的图标资源
        val currentThemeResId = getThemeResId(getSavedThemeId())
        val addAppBgResId = StyleParser.getResourceIdFromStyle(this, currentThemeResId, R.attr.add_app_bg)
        val iconResId = if (addAppBgResId != 0) addAppBgResId else R.drawable.app_add_bg1
            
        // 直接更新现有占位符对象的图标(保持对象引用不变)
        placehodlerInfo.setIcon(getDrawable(iconResId))
            
        // 通知适配器刷新UI
        if (::appInfoAdapter.isInitialized) {
            appInfoAdapter.notifyDataSetChanged()
        }
    }
    
    private fun setStyleTheme(styleResId: Int) {
        // 批量应用图片资源 - 使用声明式配置，易于维护和扩展
        applyThemeImages(
            styleResId,
            ThemeImageMapping(R.attr.hotset_switch_bg, mViewBinding.homeSwitchStyle),
                ThemeImageMapping(R.attr.all_app_bg, mViewBinding.homeAppAllApp),
                ThemeImageMapping(R.attr.nav_app_bg, mViewBinding.homeAppNav),
                ThemeImageMapping(R.attr.bt_app_bg, mViewBinding.homeAppBluetooth),
                ThemeImageMapping(R.attr.setting_app_bg, mViewBinding.homeAppSetting),
                ThemeImageMapping(R.attr.home_left_bg, mViewBinding.homeLeft),
                ThemeImageMapping(R.attr.music_bg, mViewBinding.ivMusicBg),
                ThemeImageMapping(R.attr.music_left_bg, mViewBinding.layoutMusicWidget.musicWidgetPre),
                ThemeImageMapping(R.attr.music_right_bg, mViewBinding.layoutMusicWidget.musicWidgetNext),
                ThemeImageMapping(R.attr.music_play_bg, mViewBinding.layoutMusicWidget.musicWidgetPlay),
                ThemeImageMapping(R.attr.music_album_bg, mViewBinding.layoutMusicWidget.imgSongArtBg),
                ThemeImageMapping(R.attr.radio_icon_bg, mViewBinding.layoutRadioLayout.ivRadioIcon),
                ThemeImageMapping(R.attr.radio_bg, mViewBinding.ivRadioBg),
                ThemeImageMapping(R.attr.music_left_bg, mViewBinding.layoutRadioLayout.ivRadioPre),
                ThemeImageMapping(R.attr.music_right_bg, mViewBinding.layoutRadioLayout.ivRadioNext),
                ThemeImageMapping(R.attr.car_bg, mViewBinding.ivCarBg),
                ThemeImageMapping(R.attr.shortcut_bg, mViewBinding.homeBottom),

        )

        // 应用颜色资源
        val color = StyleParser.getColorFromStyle(this, styleResId, R.attr.chage_text_color)
        mViewBinding.tvGpsSpeed.setTextColor(color)
        mViewBinding.tvGpsSpeedUnit.setTextColor(color)
        mViewBinding.layoutMusicWidget.musicName.setTextColor(color)
        mViewBinding.layoutMusicWidget.musicArtist.setTextColor(color)
        mViewBinding.layoutRadioLayout.tvRadioFreq.setTextColor(color)
        mViewBinding.layoutRadioLayout.tvRadioAmFm.setTextColor(color)

      /*  val themeBgResId = StyleParser.getResourceIdFromStyle(this, styleResId, R.attr.ui_theme_bg)
        if (themeBgResId != 0) {
            mViewBinding.rlMainLayout.setBackgroundResource(themeBgResId)
        }*/
        
        // 更新应用列表中文本的颜色
        if (::appInfoAdapter.isInitialized) {
            appInfoAdapter.setAppNameTextColor(color)
        }

        // 更新占位符图标的主题
        updatePlaceholderTheme()



    }

    //保存数据
    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_ID = "theme_id"  // Store simple ID (1-4), NOT resource ID
        // 记录"哪个主题的壁纸已经真正落地"，用于启动/回前台时做一致性校验
        private const val KEY_WALLPAPER_THEME_ID = "wallpaper_theme_id"
        private const val WALLPAPER_THEME_UNKNOWN = -2   // 还没成功设过壁纸
        private const val WALLPAPER_THEME_CUSTOM = -1    // 用户长按自定义过壁纸，主题不再接管
        private const val DEFAULT_THEME_ID = 0
        private const val MAX_THEME_ID = 3
        private const val WALLPAPER_MAX_RETRY = 1        // 设壁纸失败后的重试次数

        /**
         * 壁纸只有一个写线程：
         * 1. 串行化，杜绝"旧任务比新任务后落地"造成的主题/壁纸错配；
         * 2. 队列里的过期任务用 generation 直接丢弃，只保留最新一次请求。
         * 队列放在 companion 里，Activity 重建后仍在途的任务不会被重复创建。
         */
        private val wallpaperExecutor: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "launcher-wallpaper").apply { isDaemon = true }
            }

        /** 每次请求壁纸 +1；任务启动、写壁纸前、写完之后都比对，过期即作废 */
        private val wallpaperGeneration = java.util.concurrent.atomic.AtomicLong(0)

        /** 正在排队/正在处理的目标主题，避免重复入队；放弃时清空，让一致性校验可以补设 */
        @Volatile
        private var pendingWallpaperThemeId = WALLPAPER_THEME_UNKNOWN
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
            0 -> R.style.AppTheme1
            1 -> R.style.AppTheme2
            2 -> R.style.AppTheme3
            3 -> R.style.AppTheme4
            else -> R.style.AppTheme1
        }
    }

    /**
     * Save theme ID to SharedPreferences and apply the theme
     */
    private fun saveAndApplyTheme(themeId: Int) {
        saveThemeId(themeId)

        // 壁纸只能异步设置：先入队（单线程串行队列），尽量缩短"主题已换、壁纸未换"的窗口
        requestWallpaper(this, themeId)

        applyThemeUi(themeId)
        LogUtil.i("Theme switched to ID: $themeId")
    }

    /**
     * 主题ID落盘
     * commit() 同步落盘：apply() 是异步写，切换后插件进程被杀/重载会导致主题ID丢失，
     * 而系统壁纸已经生效，重启后就变成"旧主题 + 新壁纸"
     */
    private fun saveThemeId(themeId: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_THEME_ID, themeId).commit()
    }

    /**
     * 只切换界面主题（系统属性 + 样式 + 音乐控件），不触碰壁纸；壁纸设置失败回滚时复用
     */
    private fun applyThemeUi(themeId: Int) {
        val uiKey = resources.getString(R.string.ui_key)
        val themeIdStr = if (themeId < 10) "0$themeId" else "$themeId"
        SystemProperties.set(uiKey, "wallpaper_$themeIdStr")
        setStyleTheme(getThemeResId(themeId))
        // 更新音乐播放控件的主题（根据音乐状态切换不同颜色的图片）
        // 根据主题ID设置dayNight: 主题1、3,4为白色(0), 主题2为黑色(1)
        musicWidget.setTheme(themeId)

    }

    /**
     * Switch to next theme (cyclic: 1->2->3->4->1)
     * 不再用布尔闸门拦截点击：壁纸任务由单线程串行执行 + generation 丢弃过期任务，
     * 连续切换时最终落地的一定是最后一次请求的壁纸，不会出现"旧壁纸盖新主题"。
     */
    private fun switchToNextTheme() {
        val currentThemeId = getSavedThemeId()
        val nextThemeId = if (currentThemeId >= MAX_THEME_ID) {
            0 // Cycle back to first theme
        } else {
            currentThemeId + 1
        }
        saveAndApplyTheme(nextThemeId)
    }

    /**
     * 读取"已经落地的壁纸主题"（WALLPAPER_THEME_UNKNOWN 表示还没成功设过）
     */
    private fun getAppliedWallpaperThemeId(): Int {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_WALLPAPER_THEME_ID, WALLPAPER_THEME_UNKNOWN)
    }

    /**
     * 记录"已经落地的壁纸主题"（同步落盘，进程被杀也不会丢）
     */
    private fun saveAppliedWallpaperThemeId(context: Context, themeId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WALLPAPER_THEME_ID, themeId).commit()
    }

    /**
     * 启动/回前台时校验主题与壁纸是否一致：
     * 上次设壁纸失败、进程被杀、SharedPreferences 丢失等情况会在这里自动补设，
     * 避免"主题和壁纸对不上"一直保留到用户下次手动切换。
     * 用户长按自定义过壁纸（WALLPAPER_THEME_CUSTOM）时不做接管。
     */
    private fun syncWallpaperWithSavedTheme() {
        val themeId = getSavedThemeId()
        val appliedThemeId = getAppliedWallpaperThemeId()
        if (appliedThemeId == WALLPAPER_THEME_CUSTOM) {
            LogUtil.i("Wallpaper is custom, skip sync")
            return
        }
        if (appliedThemeId == themeId || pendingWallpaperThemeId == themeId) {
            return
        }
        LogUtil.i("Wallpaper theme($appliedThemeId) != current theme($themeId), resync wallpaper")
        requestWallpaper(this, themeId)
    }

    /**
     * Initialize theme on startup
     * @return Theme resource ID for ContextThemeWrapper
     */
    private fun getInitialThemeResId(): Int {
        val themeId = getSavedThemeId()
        return getThemeResId(themeId)
    }

    /**
     * 请求把某个主题的壁纸设为系统壁纸。
     * 所有壁纸写入都走这里：单线程串行 + generation 丢弃过期任务，
     * 保证"最后请求的那个主题"才是最终落地的那张壁纸。
     */
    private fun requestWallpaper(context: Context, themeId: Int) {
        // 注意：这里必须用插件 Activity 的 Context（插件资源 0x7e 只有它能解析），
        // 任务生命周期只有一次解码+setBitmap，结束后即释放引用
        val generation = wallpaperGeneration.incrementAndGet()
        pendingWallpaperThemeId = themeId
        LogUtil.i("requestWallpaper theme=$themeId generation=$generation")
        wallpaperExecutor.execute {
            // 排队期间已经被更新的请求取代，直接丢弃
            if (wallpaperGeneration.get() != generation) {
                LogUtil.d("Drop stale wallpaper task, theme=$themeId")
                return@execute
            }
            applyWallpaper(context, themeId, generation, 0)
        }
    }

    /**
     * 真正设置壁纸（运行在单线程串行队列中）
     * @param attempt 第几次尝试，失败会重试 WALLPAPER_MAX_RETRY 次
     */
    private fun applyWallpaper(context: Context, themeId: Int, generation: Long, attempt: Int) {
        var bitmap: Bitmap? = null
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)

            // 根据主题ID选择壁纸资源
            val wallpaperResId = when (themeId) {
                0 -> R.drawable.wallpaper_00
                1 -> R.drawable.wallpaper_01
                2 -> R.drawable.wallpaper_02
                3 -> R.drawable.wallpaper_03
                else -> R.drawable.wallpaper_00
            }

            // 获取屏幕尺寸用于计算合适的采样率
            val displayMetrics = context.resources.displayMetrics
            val targetWidth = displayMetrics.widthPixels
            val targetHeight = displayMetrics.heightPixels
            LogUtil.d("Screen size: ${targetWidth}x${targetHeight}")

            // 关键优化: 预先设置期望的壁纸尺寸,加快setBitmap速度
            wallpaperManager.suggestDesiredDimensions(targetWidth, targetHeight)

            // 第一步:仅获取图片原始尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.resources.openRawResource(wallpaperResId).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            LogUtil.d("Original image size: ${originalWidth}x${originalHeight}")

            // 第二步:计算采样率
            options.inSampleSize = calculateInSampleSize(
                originalWidth, originalHeight, targetWidth, targetHeight
            )
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // 使用RGB_565减少50%内存
            options.inDither = true // 启用抖动,改善RGB_565的画质

            // 第三步:解码压缩后的图片
            bitmap = context.resources.openRawResource(wallpaperResId).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (bitmap == null || bitmap.isRecycled) {
                retryWallpaper(context, themeId, generation, attempt, "decode failed")
                return
            }

            // 解码期间用户可能又切了主题：过期任务作废，绝不用旧壁纸覆盖新主题
            if (wallpaperGeneration.get() != generation) {
                LogUtil.d("Drop stale wallpaper task before setBitmap, theme=$themeId")
                return
            }

            // 第四步:设置壁纸(由于已调用suggestDesiredDimensions,此操作会更快)
            // 传入null表示使用整个屏幕,确保壁纸全屏显示
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)

            if (wallpaperGeneration.get() != generation) {
                // setBitmap 期间又来了新的切换请求，最终状态由最新任务负责，这里不记录
                LogUtil.i("Wallpaper $themeId applied but superseded, newer task will take over")
                return
            }

            // 记录"已落地的壁纸主题"，供启动/回前台做一致性校验
            saveAppliedWallpaperThemeId(context, themeId)
            pendingWallpaperThemeId = WALLPAPER_THEME_UNKNOWN
            LogUtil.i("Wallpaper switched successfully, theme=$themeId")
        } catch (e: Exception) {
            LogUtil.e("setWallpaper error, theme=$themeId, attempt=$attempt, msg=${e.message}")
            e.printStackTrace()
            retryWallpaper(context, themeId, generation, attempt, e.message ?: "unknown")
        } finally {
            // 确保bitmap被回收
            bitmap?.recycle()
        }
    }

    /**
     * 设置壁纸失败时重试；过期任务不重试（新请求会覆盖它）
     */
    private fun retryWallpaper(
        context: Context,
        themeId: Int,
        generation: Long,
        attempt: Int,
        reason: String
    ) {
        if (wallpaperGeneration.get() != generation) {
            LogUtil.d("Wallpaper task superseded, skip retry, theme=$themeId")
            return
        }
        if (attempt >= WALLPAPER_MAX_RETRY) {
            // 放弃重试：不写 KEY_WALLPAPER_THEME_ID，启动/回前台时的一致性校验会自动补设
            pendingWallpaperThemeId = WALLPAPER_THEME_UNKNOWN
            LogUtil.e("Give up setting wallpaper for theme=$themeId, reason=$reason")
            // 壁纸最终没设上，把界面主题回滚到"上一次真正落地的那套壁纸"，保证看到的是一套
            rollbackThemeIfNeeded(context, themeId)
            return
        }
        LogUtil.w("Retry setting wallpaper for theme=$themeId, reason=$reason")
        wallpaperExecutor.execute {
            if (wallpaperGeneration.get() != generation) {
                return@execute
            }
            applyWallpaper(context, themeId, generation, attempt + 1)
        }
    }
    
    /**
     * 壁纸最终没设上时，把界面主题回滚到"上一次真正落地的那套壁纸"对应的主题，
     * 避免长时间停在"主题和壁纸对不上"的状态。
     * 只有用户没有再切主题时才回滚；自定义壁纸(-1)/未知状态(-2)不参与回滚。
     */
    private fun rollbackThemeIfNeeded(context: Context, failedThemeId: Int) {
        val activity = context as? UIActivity ?: return
        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
            // 期间用户又切了主题，交给最新一次切换处理
            if (activity.getSavedThemeId() != failedThemeId) return@runOnUiThread
            val appliedThemeId = activity.getAppliedWallpaperThemeId()
            if (appliedThemeId < 0 || appliedThemeId == failedThemeId) return@runOnUiThread
            LogUtil.w("Rollback theme $failedThemeId -> $appliedThemeId (wallpaper set failed)")
            activity.saveThemeId(appliedThemeId)
            activity.applyThemeUi(appliedThemeId)
        }
    }

    /**
     * 计算Bitmap采样率
     * @return 采样率,必须是2的幂次方(1, 2, 4, 8...)
     */
    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        
        // 只有当原图大于目标尺寸时才进行采样
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            
            // 计算最大的2的幂次方采样率,使得采样后的图片仍大于等于目标尺寸
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    fun getNaviApps(appName: String): Boolean {
        try {
            val appinfo =
                this.getPackageManager().getApplicationInfo(appName, PackageManager.GET_META_DATA)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            //e.printStackTrace();
            LogUtil.i("getNaviApps,Exception = $e")
        }
        return false
    }

}