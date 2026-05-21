package com.launcher.zy_ui03

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
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
import com.awell.addapp.MyDbHelper
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
import com.launcher.zy_ui03.utils.ClickUtils
import com.launcher.zy_ui03.utils.IconManager
import com.launcher.zy_ui03.utils.LogUtil
import com.launcher.zy_ui03.view.MusicWidget
import kotlinx.coroutines.Runnable
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs

class UIActivity : Activity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding
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
            .build()

                
        AppsCustomizeControl.setPluginThemeMode(0xff ,false,true ,IconManager.PACKAGE_ICON_MAP, null, "com.launcher.zy_ui03")
        AppsCustomizeControl.applyAppsCustomizeConfig(config)
        // 初始化 IconManager，预加载资源
        IconManager.initialize(this)

    }



    override fun onResume() {
        super.onResume()
        LogUtil.i("onResume")
        isResumed = true

        // Verify all apps in the list are still installed
        verifyInstalledApps()

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
            locationManager.requestLocationUpdates("gps", 1000, 10f, mLocationListener, mHandle.looper)
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
        myDbHelper = MyDbHelper(this, "show_app", null, 1)
        sqLiteDatabase = myDbHelper.writableDatabase

        appInfoAdapter =
            AppInofAdapter(this, showAppInfoList, showPopupI, addSelectAppCallback)

        linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // 设置item垂直居中
        mViewBinding.rv.layoutManager = linearLayoutManager
        mViewBinding.rv.adapter = appInfoAdapter

        Thread {
            showHostApp()
        }.start()

    }

    private fun showHostApp() {
        // 刷新应用列表
        refreshAppList()

        runOnUiThread {
            appInfoAdapter.setContentList(showAppInfoList)
            appInfoAdapter.notifyDataSetChanged()
        }
    }

    private fun refreshAppList() {
        // 获取所有已安装应用
        allAppInfoList = getAllAppInfo(this, false)
        // 清空当前显示列表
        showAppInfoList = ArrayList()

        // 从数据库加载保存的应用
        val storageAppList = loadAppListFromDatabase()

        // 添加保存的应用到显示列表
        for (packageName in storageAppList) {
            val app = Utils.getAppInfoFromPackage(packageName, allAppInfoList)
            if (app != null) {
                showAppInfoList.add(app)
            }
        }

        // 如果没有保存的应用，加载默认应用
        if (showAppInfoList.isEmpty()) {
            loadDefaultApps()
        }

        // 保存应用到数据库
        saveAppListToDatabase()

        // 添加占位符
        addAppPlaceholder()
    }

    private fun loadAppListFromDatabase(): List<String> {
        val storageAppList = ArrayList<String>()
        val cursor = myDbHelper.writableDatabase.query("showapp", null, null, null, null, null, null)
        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range")
                val packageName = cursor.getString(cursor.getColumnIndex("packagename"))
                storageAppList.add(packageName)
                // 删除记录
                sqLiteDatabase.delete("showapp", "packagename=?", arrayOf(packageName))
            }
            cursor.close()
        }
        return storageAppList
    }

    private fun loadDefaultApps() {
        for (packName in Utils.getDefaultShowApp(this)) {
            val appInfo = Utils.getAppInfoFromPackage(packName, allAppInfoList)
            if (appInfo != null) {
                showAppInfoList.add(appInfo)
            }
        }
    }

    private fun saveAppListToDatabase() {
        for (storagePac in showAppInfoList) {
            if (storagePac != null) {
                val contentValues = ContentValues().apply {
                    put("packagename", storagePac.package_name)
                }
                sqLiteDatabase.insert("showapp", null, contentValues)
            }
        }
    }

    private fun addAppPlaceholder() {
        // 使用统一的占位符更新方法
        updateAppPlaceholder(false)
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

    private val showPopupI: ShowPopupI = object : ShowPopupI {
        override fun showPopup() {
            showPopupAllApp()
            backgroundAlpha(0.1f)
        }

        override fun hidePopup() {
            popupWindow.dismiss()
        }
    }

    private fun showPopupAllApp() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.layout_allapp, null)
        
        // 计算屏幕尺寸的 80% 和 60%
        val displayMetrics = resources.displayMetrics
        val popupWidth = (displayMetrics.widthPixels * 0.8).toInt()
        //val popupHeight = (displayMetrics.heightPixels * 0.8).toInt()
        
        popupWindow = PopupWindow(
            view,
            popupWidth  ,
           /* popupHeight */ WindowManager.LayoutParams.WRAP_CONTENT
        )

        popupWindow.isOutsideTouchable = true
        popupWindow.windowLayoutType = TYPE_APPLICATION_OVERLAY

        allAppInfoList = getAllAppInfo(this, false)

        val rvPop = view.findViewById<RecyclerView>(R.id.rv_pop_allapp)
        val gridLayoutManager = GridLayoutManager(this, 4)
        gridLayoutManager.spanCount = 2
        gridLayoutManager.orientation = RecyclerView.HORIZONTAL
        
        // 添加垂直方向间隔（50dp）- 只在行与行之间有间隔
        val verticalSpacing = (50 * resources.displayMetrics.density).toInt()
        rvPop.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.top = verticalSpacing
                outRect.bottom = verticalSpacing
            }
        })
        
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


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initBroadcastReceiver() {
        // 注册不需要 dataScheme 的广播
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
                  //  mViewBinding.ivLampSwitchBg.postDelayed({ accRecor = false }, 8 * 1000)

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
                    LogUtil.d("zlinkStatus:$zlinkStatus")
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
                        musicWidget.setCarPlayData(zlinkStatus, phoneMode)
                    }
                }

                "android.launcher.show.allApp" -> {
                    Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
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

                    if ("com.awell.radio" != pkg) {
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
                    musicWidget.setMusicNameTextView(songName, type)
                    musicWidget.setArtistNameTextView(singerName, type)
                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {
                        musicWidget.setMusicNameTextView(
                            getResources().getString(R.string.click_play_music), MusicWidget.MUSIC
                        )
                        musicWidget.setArtistNameTextView(
                            getResources().getString(R.string.music_artist), MusicWidget.MUSIC
                        )
                    }

                    if (MusicWidget.OTHER_MUSIC == type) {
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
                    }
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

                        }
                        mHandle.removeMessages(MSG_UPDATE_SPEED)
                        mHandle.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 2000)
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

        mViewBinding.homeAppNav.setOnClickListener {
            startActivity("com.awell.navigation", "com.awell.navigation.MainActivity")
        }

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

        LogUtil.w("appList size: ${appList.size}")

        // 预计算过滤条件，避免重复计算
        val needToShowSystemApps = Utils.needToShowPackageName
        val filterApps = Utils.filterAppPackageName
        val otherNeedToShow = Utils.otherNeedToShowPackageName

        // 先过滤出需要显示的应用包名列表
        val needShowPackages = mutableListOf<String>()
        val packageInfoMap = mutableMapOf<String, PackageInfo>()

        for (p in appList) {
            val packageName = p.applicationInfo.packageName
            val flags = p.applicationInfo.flags

            val shouldShow = when {
                (flags and ApplicationInfo.FLAG_SYSTEM) != 0 && packageName in needToShowSystemApps -> true
                (flags and ApplicationInfo.FLAG_SYSTEM) == 0 && packageName !in filterApps -> true
                (flags and ApplicationInfo.FLAG_SYSTEM) != 0 && packageName in otherNeedToShow -> true
                else -> false
            }

            if (shouldShow) {
                needShowPackages.add(packageName)
                packageInfoMap[packageName] = p
            }
        }

        LogUtil.w("needShowPackages size: ${needShowPackages.size}")

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
        sqLiteDatabase.delete("showapp", "packagename=?", arrayOf<String?>(packageName))

        // Refresh the adapter
        runOnUiThread(object : Runnable {
            override fun run() {
                if (appInfoAdapter != null) {
                    appInfoAdapter.setContentList(showAppInfoList)
                }
            }
        })
    }



    private fun verifyInstalledApps() {
        if (showAppInfoList == null || showAppInfoList.isEmpty()) return


        // 刷新应用列表
        refreshAppList()


        // 检查应用数量是否匹配
        checkAppCountMatch()


        // 刷新适配器
        if (appInfoAdapter != null) {
            appInfoAdapter.setContentList(showAppInfoList)
        }
    }

    private fun checkAppCountMatch() {
        // 获取系统中实际应用列表
        val actualAppList: ArrayList<AppInfo> = getAllAppInfo(this, false)
        // 获取系统中实际应用数量
        val actualAppCount = actualAppList.size
        // 获取当前应用列表中的应用数量
        val currentAppCount = if (allAppInfoList != null) allAppInfoList.size else 0


        // 检查数量是否匹配
        val countMatches = actualAppCount == currentAppCount
        // 检查包名是否匹配
        val packageNamesMatch = countMatches && comparePackageNames(actualAppList as java.util.ArrayList<AppInfo?>?,
            allAppInfoList as MutableList<AppInfo?>?
        )


        // 检查是否完全匹配
        if (!countMatches || !packageNamesMatch) {
            // 数量或包名不匹配，显示不匹配占位符
            updateAppPlaceholder(true)
        } else {
            // 数量和包名都匹配，显示正常占位符
            updateAppPlaceholder(false)
        }
    }

    private fun comparePackageNames(
        actualAppList: java.util.ArrayList<AppInfo?>?,
        currentAppList: MutableList<AppInfo?>?
    ): Boolean {
        if (actualAppList == null || currentAppList == null) {
            return false
        }


        // 创建包名集合进行比较
        val actualPackages: MutableSet<String?> = HashSet<String?>()
        for (appInfo in actualAppList) {
            if (appInfo != null && appInfo.package_name != null) {
                actualPackages.add(appInfo.package_name)
            }
        }

        val currentPackages: MutableSet<String?> = HashSet<String?>()
        for (appInfo in currentAppList) {
            if (appInfo != null && appInfo.package_name != null) {
                currentPackages.add(appInfo.package_name)
            }
        }


        // 检查两个集合是否相等
        return actualPackages == currentPackages
    }

    private fun updateAppPlaceholder(isMismatch: Boolean) {
        // 移除所有占位符
        removeAllPlaceholders()


        // 添加相应的占位符
        if (isMismatch) {
            // 添加不匹配占位符
            val mismatchPlaceholder = createMismatchPlaceholder()
            showAppInfoList.add(mismatchPlaceholder)
        } else {
            // 添加正常占位符
            showAppInfoList.add(placehodlerInfo)
        }
    }

    private fun removeAllPlaceholders() {
        for (i in showAppInfoList.indices.reversed()) {
            val appInfo: AppInfo? = showAppInfoList.get(i)
            if (appInfo != null) {
                // 移除正常占位符
                if (appInfo.getLabel() != null && appInfo.getLabel() == getString(R.string.add_app)) {
                    showAppInfoList.removeAt(i)
                } else if ("placeholder_mismatch" == appInfo.package_name) {
                    showAppInfoList.removeAt(i)
                }
            }
        }
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
        private const val DEFAULT_THEME_ID = 0
        private const val MAX_THEME_ID = 3
    }
    
    // 壁纸切换状态管理
    private var wallpaperTaskThread: Thread? = null
    @Volatile
    private var isWallpaperSwitching = false  // 是否正在切换壁纸
    private val WALLPAPER_SWITCH_TIMEOUT = 5000L  // 5秒超时
    private val wallpaperTimeoutHandler = Handler(Looper.getMainLooper())
    private val wallpaperTimeoutRunnable = Runnable {
        LogUtil.w("Wallpaper switch timeout, releasing resources")
        synchronized(this@UIActivity) {
            if (isWallpaperSwitching) {
                isWallpaperSwitching = false
                LogUtil.i("Wallpaper switch timeout - resources released")
            }
        }
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
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_ID, themeId).apply()
        // 设置系统壁纸 - 使用优化后的异步方法,支持任务取消和状态管理
        setWallpaperByThemeIdOptimized(this, themeId)
        val uiKey = resources.getString(R.string.ui_key)
        val themeIdStr = if (themeId < 10) "0$themeId" else "$themeId"
        SystemProperties.set(uiKey, "wallpaper_$themeIdStr")
        LogUtil.i("Theme switched to ID: $themeId")
        setStyleTheme(getThemeResId(themeId))
        // 更新音乐播放控件的主题（根据音乐状态切换不同颜色的图片）
        // 根据主题ID设置dayNight: 主题1、3,4为白色(0), 主题2为黑色(1)
        musicWidget.setTheme(themeId)

    }

    /**
     * Switch to next theme (cyclic: 1->2->3->4->1)
     */
    private fun switchToNextTheme() {
        // 检查是否可以切换壁纸
        synchronized(this@UIActivity) {
            if (isWallpaperSwitching) {
                LogUtil.w("Wallpaper is switching, please wait...")
                return
            }
            
            // 设置切换状态和超时定时器
            isWallpaperSwitching = true
            wallpaperTimeoutHandler.removeCallbacks(wallpaperTimeoutRunnable)
            wallpaperTimeoutHandler.postDelayed(wallpaperTimeoutRunnable, WALLPAPER_SWITCH_TIMEOUT)
            LogUtil.i("Start wallpaper switch, timeout in ${WALLPAPER_SWITCH_TIMEOUT}ms")
        }
        
        // 取消之前的壁纸设置任务
        cancelWallpaperTask()
        
        val currentThemeId = getSavedThemeId()
        val nextThemeId = if (currentThemeId >= MAX_THEME_ID) {
            0 // Cycle back to first theme
        } else {
            currentThemeId + 1
        }
        saveAndApplyTheme(nextThemeId)
    }
    
    /**
     * 取消当前正在执行的壁纸设置任务
     */
    private fun cancelWallpaperTask() {
        wallpaperTaskThread?.let { thread ->
            if (thread.isAlive) {
                LogUtil.d("Canceling previous wallpaper task")
                // 中断线程(需要在任务中检查interrupted状态)
                thread.interrupt()
            }
            wallpaperTaskThread = null
        }
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
     * 优化的壁纸设置方法 - 使用WallpaperManager确保全局生效
     * 优化点:
     * 1. 支持任务取消(通过interrupt)
     * 2. 使用Bitmap采样减少内存占用
     * 3. 在关键步骤检查interrupted状态,及时退出
     * 4. 异步执行不阻塞UI
     * 5. 使用suggestDesiredDimensions预设置尺寸,提升设置速度
     */
    private fun setWallpaperByThemeIdOptimized(context: Context, themeId: Int) {
       // val startTime = System.currentTimeMillis()
        LogUtil.i("setWallpaperByThemeIdOptimized start for theme: $themeId")
        
        // 创建新的壁纸设置任务
        val newTask = Thread {
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
                    
              //  LogUtil.i("Sample size: ${options.inSampleSize}")
                
                // 第三步:解码压缩后的图片
                bitmap = context.resources.openRawResource(wallpaperResId).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
                
                if (bitmap != null && !bitmap.isRecycled) {
                  //  LogUtil.i("Decoded bitmap size: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")
                    
                    // 第四步:设置壁纸(由于已调用suggestDesiredDimensions,此操作会更快)
                    // 传入null表示使用整个屏幕,确保壁纸全屏显示
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    
                  //  val elapsed = System.currentTimeMillis() - startTime
                  //  LogUtil.i("✓ Wallpaper set successfully for theme: $themeId (${originalWidth}x${originalHeight} -> ${bitmap.width}x${bitmap.height}) in ${elapsed}ms")
                    
                    // 壁纸切换成功，重置状态
                    synchronized(this@UIActivity) {
                        isWallpaperSwitching = false
                        wallpaperTimeoutHandler.removeCallbacks(wallpaperTimeoutRunnable)
                        LogUtil.i("Wallpaper switched successfully, ready for next switch")
                    }
                } else {
                    LogUtil.e("Failed to decode wallpaper for theme: $themeId")
                    // 失败时也释放锁，允许重试
                    synchronized(this@UIActivity) {
                        isWallpaperSwitching = false
                        wallpaperTimeoutHandler.removeCallbacks(wallpaperTimeoutRunnable)
                    }
                }
            } catch (e: InterruptedException) {
              //  val elapsed = System.currentTimeMillis() - startTime
              //  LogUtil.d("✗ Wallpaper task interrupted after ${elapsed}ms")
                // 中断时释放锁
                synchronized(this@UIActivity) {
                    isWallpaperSwitching = false
                    wallpaperTimeoutHandler.removeCallbacks(wallpaperTimeoutRunnable)
                }
            } catch (e: Exception) {
               // val elapsed = System.currentTimeMillis() - startTime
               // LogUtil.e("✗ setWallpaperByThemeIdOptimized error after ${elapsed}ms: ${e.message}")
                e.printStackTrace()
                // 异常时释放锁，允许重试
                synchronized(this@UIActivity) {
                    isWallpaperSwitching = false
                    wallpaperTimeoutHandler.removeCallbacks(wallpaperTimeoutRunnable)
                }
            } finally {
                // 确保bitmap被回收
                bitmap?.recycle()
                // 清理任务引用
                if (wallpaperTaskThread == Thread.currentThread()) {
                    wallpaperTaskThread = null
                }
            }
        }
        
        // 保存任务引用并启动
        wallpaperTaskThread = newTask
        newTask.start()
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



    //=============================主题切换 end===============================

}