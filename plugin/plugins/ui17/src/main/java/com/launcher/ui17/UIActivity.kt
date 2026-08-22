package com.launcher.ui17

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Rect
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
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.RequiresPermission
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.awell.addapp.AddSelectAppCallback
import com.awell.addapp.AppInfo
import com.awell.addapp.AppListStorage
import com.awell.addapp.ShowPopupI
import com.awell.control.AppsCustomizeConfig
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.launcher2.IconCache
import com.awell.library.AwellTool
import com.awell.utils.CommonData
import com.awell.utils.Utils
import com.awell.utils.Utils.startWallpaper
import com.launcher.ui17.adapter.AppInofAdapter
import com.launcher.ui17.adapter.AppPopAdapter
import com.launcher.ui17.databinding.UiActivityBinding
import com.awell.utils.FreeformUtils.NAVI_GAODE_PKG
import com.awell.utils.FreeformUtils.NAVI_GOOGLE_PKG
import com.awell.utils.FreeformUtils.SETTINGS_FREEFORM_APP_PACKAGE_NAME
import com.awell.utils.FreeformUtils.startFreeformApp
import com.awell.library.util.LogUtil
import com.awell.library.util.SystemUIClient
import com.launcher.ui17.view.MusicWidget
import kotlinx.coroutines.Runnable
import java.io.File
import kotlin.math.abs

class UIActivity : Activity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding

    lateinit var mediaControl: AwellMediaControl
    private lateinit var musicWidget: MusicWidget
    private var accRecor: Boolean = false

    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false
    private var isResumed = false

    lateinit var systemUIClient: SystemUIClient

    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var appInfoAdapter: AppInofAdapter
    private lateinit var allAppInfoList: List<AppInfo>
    private lateinit var showAppInfoList: ArrayList<AppInfo>
    lateinit var popupWindow: PopupWindow
    private lateinit var placehodlerInfo: AppInfo


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        mViewBinding = UiActivityBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        LogUtil.setIsDebuggable()

        initView()

        initMediaMusic()

        initTouchAndSpeedListener()

        initBroadcastReceiver()

        AppsCustomizeControl.setActivity(this)

        initFreeformControl()
        Settings.System.putString(contentResolver, "ui_has_freeform", "true")

        // 设置主题模式，并同步到 IconManager
        AppsCustomizeControl.setPluginThemeMode(AppsCustomizeConfig.Builder().setThemeMode(1).build())

    }

    private val handlerFreeform = Handler(Looper.getMainLooper())

    private val freeformRunnable = Runnable {
        if (findViewById<ImageView>(R.id.freeform_image).isVisibleOnScreen()) {
            /*Log.i(
                TAG,
                "huang running=>: isFreeformAppRunning()=>${FreeformUtils.isFreeformAppRunning(this)}"
            )
            if (!FreeformUtils.isFreeformAppRunning(this)) {*/
            updateImagePosition(findViewById(R.id.freeform_image), "onResume")
            //}
        }
    }

    override fun onResume() {
        super.onResume()
        LogUtil.i("onResume")
        isResumed = true
        /*findViewById<ImageView>(R.id.freeform_image).postDelayed({
            if (findViewById<ImageView>(R.id.freeform_image).isVisibleOnScreen()) {
                updateImagePosition(findViewById(R.id.freeform_image), "onResume")
            }
        }, 100)*/
        handlerFreeform.removeCallbacks(freeformRunnable)
        // 重新发送新的延时任务
        if (checkTopAppLollipop(this))
            handlerFreeform.postDelayed(freeformRunnable, 150)

        // Verify all apps in the list are still installed
        verifyInstalledApps()

    }

    private fun checkTopAppLollipop(context: Context): Boolean {
        //val myPackageName = "com.awell.launcher.host"
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return try {
            val tasks = am.appTasks
            if (tasks.isNotEmpty()) {
                val taskInfo = tasks[0].taskInfo
                if (taskInfo != null && taskInfo.topActivity != null) {
                    return taskInfo.topActivity!!.packageName == "com.awell.launcher.host"
                }
            }
            false
        } catch (e: SecurityException) {
            LogUtil.e(e.message)
            true
        }
    }

    override fun onPause() {
        super.onPause()
        LogUtil.i("onPause")
        isResumed = false
        handlerFreeform.removeCallbacks(freeformRunnable)
    }

    override fun onStop() {
        super.onStop()
        LogUtil.i("onStop")

        if (systemUIClient.getmFreeformMode() == 0x05) {
            systemUIClient.hideFreeform()
            Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
            systemUIClient.fullScreenFreeform()
        } else
            Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
        LogUtil.w("freeform_launcher_idle,0")
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
        Settings.System.putString(contentResolver, "freeform_launcher_idle", "1");
        LogUtil.w("freeform_launcher_idle,1")
        //todo bootapk_packname bootapk_classname -- adb shell settings get system bootapk_packname
        //todo 开机时候检查时否有开机自启的apk，有，不启动小窗，没有，启动小窗
        LogUtil.i("updateImagePosition: huang rect=>${rect} reason=${reason}")
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

    /**
     * 初始化长按切换壁纸和监听gps速度变化
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun initTouchAndSpeedListener() {
        handler = Handler(Looper.getMainLooper())
        viewConfiguration = ViewConfiguration.get(this)
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
        mViewBinding.rvApps.layoutManager = GridLayoutManager(this, 2,RecyclerView.VERTICAL,false)
        mViewBinding.rvApps.adapter = appInfoAdapter

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

        allAppInfoList = getAllAppInfo(this, false)

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
        filter.addAction(CommonData.ACTION_TOP_SESSION_CHANGE)
        filter.addAction("awellauto.backcar.on")

        filter.addAction("com.awell.360floatview.fullscreen")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        //单独注册需要 dataScheme 的广播
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
        //        mMediaListener.cleanup()
        unregisterReceiver(receiver)
        systemUIClient.unbindService(this)
        AppsCustomizeControl.setActivity(null)
        cancelLongPressDetection()
        AppsCustomizeControl.hideApps()
        try {
            mediaControl.unBindDataService(this)
        } catch (e: Exception) {
            LogUtil.e("onDestroy: unBindDataService error=>${e.message}")
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

        musicWidget = mViewBinding.layoutMusicWidget.root
        musicWidget.setMediaLibrary(mediaControl)
        musicWidget.setActivity(this, musicWidget)
        mediaControl.refreshCurrentMediaState()

    }


    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isResumed) {
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
                    findViewById<ImageView>(R.id.freeform_image).post {
                        if (findViewById<ImageView>(R.id.freeform_image).isVisibleOnScreen()) {
                            updateImagePosition(findViewById(R.id.freeform_image), "acc_on")
                        }
                    }

                }

                CommonData.ACTION_ACC_OFF -> {
                    //accRecor = true
                    systemUIClient?.hideFreeform()
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


                CommonData.ACTION_TOP_SESSION_CHANGE -> {
                    val sessionTopPkg = intent.getStringExtra(CommonData.EXTRA_TOP_PACKAGE)
                    handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                }

                "com.awell.360floatview.fullscreen",
                "awellauto.backcar.on" -> {
                    val freePkg = Settings.System.getString(contentResolver, "freeform_app_package_name")
                    if ("cn.cardoor.zt360".equals(freePkg)) {
                        if (::popupWindow.isInitialized && popupWindow.isShowing) {
                            popupWindow.dismiss()
                        }
                        systemUIClient.fullScreenFreeform()
                    }
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
                if ("com.awell.radio" != pkg) {
                    musicWidget.switchMediaController(
                        pkg, command, mediaType, currentMedia
                    )
                }
            }

            override fun updateViewPlayStatus(bundle: Bundle, status: Boolean, type: Int) {
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
                            getResources().getString(R.string.click_play_music),
                            MusicWidget.MUSIC
                        )
                        musicWidget.setArtistNameTextView(
                            getResources().getString(R.string.music_artist),
                            MusicWidget.MUSIC
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

            override fun updateViewPlayTime(bundle: Bundle, currentTime: Long, totalTime: Long, type: Int) {
//                runOnUiThread {
//
//                }
            }

            override fun updateViewRadioFreq(bundle: Bundle, fmOrAm: String, freq: String, unit: String) {
//                runOnUiThread {
//                    runOnUiThread(java.lang.Runnable {
//                        mViewBinding.layoutRadioLayout.tvRadioFreq.text = freq
//                        mViewBinding.layoutRadioLayout.tvRadioAmFm.text = fmOrAm
//                        if (fmOrAm == "FM") {
//                            mViewBinding.layoutRadioLayout.fmScaleView.setRadioMode(FMMarkView.RadioMode.FM)
//                        } else {
//                            mViewBinding.layoutRadioLayout.fmScaleView.setRadioMode(FMMarkView.RadioMode.AM)
//                        }
//                        mViewBinding.layoutRadioLayout.fmScaleView.setBandFrequency(freq.toFloat())
//                        //mViewBinding.layoutRadioLayout.tvRadioUnit.setText(unit)
//                    })
//                }
            }

            override fun handleOriginBundle(bundle: Bundle) {
                //todo update music widget
                val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)

            }
        }
    }

    private fun clickStartApp() {

        /*mViewBinding.homeAppMap.setOnClickListener {
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
    mViewBinding.homeAppVideo.setOnClickListener {
            val pkg =
                Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)

            when (pkg) {
                "com.google.android.youtube" -> {
                    systemUIClient.fullScreenFreeform()

                }

                else -> {
                    startActivity("com.google.android.youtube", "com.google.android.youtube.app.honeycomb.Shell\$HomeActivity")
                }
            }

        }


        mViewBinding.homeAppGoogleplay.setOnClickListener {
            val pkg =
                Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)

            when (pkg) {
                "com.android.vending" -> {
                    systemUIClient.fullScreenFreeform()

                }
                else -> {
                    startActivity("com.android.vending", "com.android.vending.AssetBrowserActivity")
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
            Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
            systemUIClient.fullScreenFreeform()


            AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
        }

        mViewBinding.homeAppBluetooth.setOnClickListener {
            val pkg =
                Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)

            when (pkg) {
                "com.spotify.music" -> {
                    systemUIClient.fullScreenFreeform()
                }

                else -> {
                    startActivity("com.spotify.music", "com.spotify.music.MainActivity")
                }
            }
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
*/

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AppsCustomizeControl.hideApps()
        Settings.System.putString(contentResolver, "freeform_launcher_idle", "1");
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
        // 获取所有输入法包名
        val imePackages = mutableSetOf<String>()
        val imeIntent = Intent("android.view.InputMethod")
        val imeServices = packageManager.queryIntentServices(imeIntent, 0)
        imeServices.forEach { imePackages.add(it.serviceInfo.packageName) }

        // 获取所有launcher包名（CATEGORY_HOME）
        val launcherPackages = mutableSetOf<String>()
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homeActivities = packageManager.queryIntentActivities(homeIntent, 0)
        homeActivities.forEach { launcherPackages.add(it.activityInfo.packageName) }

        val noNeedToShow = Utils.notDisplayedPackageName

        val appList: List<PackageInfo> = resolveInfos.mapNotNull { resolveInfo ->
            try {
                packageManager.getPackageInfo(resolveInfo.activityInfo.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }.distinctBy { it.packageName }
            .filter { pkgInfo ->
            val pkg = pkgInfo.packageName
            !imePackages.contains(pkg) && !launcherPackages.contains(pkg) && !noNeedToShow.contains(pkg)
        }

        //LogUtil.w("appList size: ${appList.size}")

        for (p in appList) {
            //val packageName = p.applicationInfo.packageName
            //val flags = p.applicationInfo.flags
            //LogUtil.i("packageName=${p.applicationInfo.packageName}")
            val bean = AppInfo()
            //bean.setIcon(IconManager.getAppIcon(context, packageName))
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString())
            bean.setPackage_name( p.applicationInfo.packageName)
            bean.setFlags(p.applicationInfo.flags)
            appBeanList.add(bean)

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

            mViewBinding.hostNavIv.id -> {
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


            mViewBinding.hostAppIv.id -> {
                LogUtil.w("freeform_launcher_idle,0")
                systemUIClient.hideFreeform()
                Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
                systemUIClient.fullScreenFreeform()
                AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
            }

            mViewBinding.hostSettingIv.id -> {

                startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity")
            }


            mViewBinding.hostMusicIv.id -> {
                val pkg = Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)
                when (pkg) {
                    "com.awell.localmusic" -> {
                        systemUIClient.fullScreenFreeform()
                    }

                    else -> {
                        startActivity("com.awell.localmusic", "com.awell.localmusic.MainActivity")
                    }
                }
            }

            mViewBinding.hostPhoneIv.id -> {
                val btIntent = Intent("com.awell.bluetooth")
                btIntent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity")
                btIntent.putExtra("bt_preference_key", 0)
                startActivity(btIntent)
            }
        }
    }

//============================start===============================================

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
        val freshAllList: ArrayList<AppInfo> = getAllAppInfo(this, false)
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
        //val defaultApps = arrayOf<String>("com.google.android.apps.maps", "com.awell.eqselect", "com.awell.bluetooth", "com.awell.localmusic")
        for (packName  in Utils.getDefaultShowApp(this)) {
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


    private fun createMismatchPlaceholder(): AppInfo {
        val mismatchPlaceholder = AppInfo()
        //mismatchPlaceholder.setIcon(resources.getDrawable(R.drawable.sf_app_add_icon))
        mismatchPlaceholder.setLabel(getString(R.string.add_app))
        mismatchPlaceholder.package_name = "placeholder_mismatch"
        return mismatchPlaceholder
    }
//============================end===============================================

}