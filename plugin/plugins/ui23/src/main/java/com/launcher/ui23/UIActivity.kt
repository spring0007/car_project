package com.launcher.ui23

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.os.SystemProperties
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.awell.control.AppsCustomizeConfig
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.launcher2.IconCache
import com.awell.library.AwellTool
import com.awell.utils.CommonData
import com.awell.utils.Utils.startWallpaper
import com.launcher.ui23.databinding.UiActivityBinding
import com.awell.library.util.ClickUtils
import com.awell.utils.FreeformUtils
import com.awell.utils.FreeformUtils.NAVI_GAODE_PKG
import com.awell.utils.FreeformUtils.NAVI_GOOGLE_PKG
import com.awell.utils.FreeformUtils.SETTINGS_FREEFORM_APP_PACKAGE_NAME
import com.awell.utils.FreeformUtils.startFreeformApp
import com.launcher.ui23.utils.IconManager
import com.awell.library.util.LogUtil
import com.awell.library.util.SystemUIClient
import com.awell.library.util.WeatherHelper
import com.launcher.ui23.utils.WeatherIconLoader
import com.launcher.ui23.utils.WeatherTextMapper
import com.launcher.ui23.view.MusicWidget
import kotlinx.coroutines.Runnable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class UIActivity : Activity(), View.OnClickListener {

    private val TAG = UIActivity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding

    lateinit var mediaControl: AwellMediaControl
    private lateinit var musicWidget: MusicWidget
    private var accRecor: Boolean = false

    val PERMISSION_REQUEST_CODE: Int = 100


    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false
    private var isResumed = false

    lateinit var systemUIClient: SystemUIClient

    private var isWeatherTimerRunning = false
    private val weatherRefreshRunnable: java.lang.Runnable = object : java.lang.Runnable {
        override fun run() {
            loadWeatherData()
            handler!!.postDelayed(this, (60 * 1000).toLong())
        }
    }



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


        AppsCustomizeControl.setPluginThemeMode(AppsCustomizeConfig.Builder()
            .setIconSize(resources.getDimensionPixelSize(R.dimen.app_icon_size))
            .setFontColor(Color.WHITE)
            .setFontSizeSp(resources.getDimensionPixelSize(R.dimen.font_size_sp)) // 20
            .setThemeMode(0xff)
            .setRefresh(false)
            .setIconMap(IconManager.getPackageIconMap())
            .setPluginPackageName("com.launcher.ui23")
            .setPluginOtherBgName("apple_other_app3_square")
            .build());
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

        if (!isWeatherTimerRunning) {
            handler!!.postDelayed(weatherRefreshRunnable, 0)
            isWeatherTimerRunning = true
        }

    }

    private fun checkTopAppLollipop(context: Context): Boolean {
        //val myPackageName = "com.awell.launcher.host"
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
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
        handler!!.removeCallbacks(weatherRefreshRunnable)
        isWeatherTimerRunning = false
    }

    override fun onStop() {
        super.onStop()
        LogUtil.i("onStop")

        /*if (systemUIClient.getmFreeformMode() == 0x05) {
            systemUIClient.hideFreeform()
            Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
            systemUIClient.fullScreenFreeform()
        } else*/
            Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
        LogUtil.w("freeform_launcher_idle,0")
    }

    private fun initFreeformControl() {

        systemUIClient = SystemUIClient(this)
        systemUIClient.bindToSystemUIService(this)
    }

    private fun updateImagePosition(imageView: ImageView, reason: String) {
        // 被"强行停止"(最近任务清除全部 / 第三方清理)时不自动拉起小窗:拉起只会冷启动
        // 入口界面,而小窗 app 正因 service 被拆而退出,表现为"小窗先显示再闪消失"。
        // 系统侧已对小窗包豁免 force-stop,这里只做兜底;详见 FreeformUtils.isFreeformAppForceStopped。
        if (FreeformUtils.isFreeformAppForceStopped(this)) {
            return
        }
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

    }


    var lastSpeed: Float = 0F


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

//        //单独注册需要 dataScheme 的广播
//        val packageFilter = IntentFilter()
//        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
//        packageFilter.addDataScheme("package")
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            registerReceiver(receiver, packageFilter, RECEIVER_EXPORTED)
//        } else {
//            registerReceiver(receiver, packageFilter)
//        }
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
                    LogUtil.i("onReceive: mediaControl.getCurrentPkgName()=>${mediaControl.getCurrentPkgName()}")
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
                        systemUIClient.fullScreenFreeform()
                    }
                }

//                Intent.ACTION_PACKAGE_REMOVED -> {
//                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
//                        var packageName = intent.getDataString()
//                        if (packageName != null) {
//                            // Remove the package scheme prefix
//                            packageName = packageName.replace("package:", "")
//                            // Update the app list to remove the uninstalled app
//                            updateAppListAfterUninstall(packageName)
//                        }
//                    }
//                }
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
                runOnUiThread {
                    musicWidget.setMusicSeekBar(currentTime.toInt(), totalTime.toInt(), type)
                }
            }

            override fun updateViewRadioFreq(bundle: Bundle, fmOrAm: String, freq: String, unit: String) {
                runOnUiThread {
                    runOnUiThread(java.lang.Runnable {
                        mViewBinding.tvRadioFreq.text = freq
                        mViewBinding.tvRadioAmFm.text = fmOrAm
//                        if (fmOrAm == "FM") {
//                            mViewBinding.layoutRadioLayout.fmScaleView.setRadioMode(FMMarkView.RadioMode.FM)
//                        } else {
//                            mViewBinding.layoutRadioLayout.fmScaleView.setRadioMode(FMMarkView.RadioMode.AM)
//                        }
//                        mViewBinding.layoutRadioLayout.fmScaleView.setBandFrequency(freq.toFloat())
                        //mViewBinding.layoutRadioLayout.tvRadioUnit.setText(unit)
                    })
                }
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
            mViewBinding.hostNavTv.id ,
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

            mViewBinding.hostAppTv.id ,
            mViewBinding.hostAppIv.id -> {
                LogUtil.w("freeform_launcher_idle,0")
                systemUIClient.hideFreeform()
                Settings.System.putString(contentResolver, "freeform_launcher_idle", "0");
                systemUIClient.fullScreenFreeform()
                AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
            }
            mViewBinding.hostSettingTv.id ,
            mViewBinding.hostSettingIv.id -> {

                startActivity("com.awell.carsetting", "com.awell.carsetting.MainActivity")
            }
            mViewBinding.weatherImage.id -> {
                startActivity("com.awell.weather", "com.awell.weather.MainActivity")
            }
            mViewBinding.hostMusicTv.id ,
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
            mViewBinding.hostPhoneTv.id,
            mViewBinding.hostPhoneIv.id -> {
                val btIntent = Intent("com.awell.bluetooth")
                btIntent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity")
                btIntent.putExtra("bt_preference_key", 0)
                startActivity(btIntent)
            }
            mViewBinding.radioImage.id,
            mViewBinding.hostRadioTv.id,
            mViewBinding.hostRadioIv.id -> {
                startActivity("com.awell.radio", "com.awell.radio.MainActivity")
            }
            mViewBinding.hostBrowerIv.id,
            mViewBinding.hostBrowerTv.id -> {
                val url = SystemProperties.get("persist.sys.lz.HOME_PAGE", "http://m.baidu.com")
                val chromeIntent = Intent(Intent.ACTION_VIEW)
                chromeIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                chromeIntent.setData(Uri.parse(url))

                // 检查是否有应用可以处理此Intent
                if (chromeIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(chromeIntent)
                }
            }

            mViewBinding.ivRadioNext.id -> {
                if (ClickUtils.isFastClick()) {
                    return
                }
                mediaControl.sendStrToHost(AwellTool.RADIO.NEXT)
            }

            mViewBinding.ivRadioPre.id -> {
                if (ClickUtils.isFastClick()) {
                    return
                }
                mediaControl.sendStrToHost(AwellTool.RADIO.PREVIOUS)
            }

            mViewBinding.tvRadioAmFm.id -> {
                if (ClickUtils.isFastClick()) {
                    return
                }
                mediaControl.sendStrToHost(AwellTool.RADIO.SET_FMAM)
            }
        }
    }

        //----------------
        private fun loadWeatherData() {
            Log.i(TAG, "loadWeatherData: huang info=>")
            Thread(java.lang.Runnable {
                val weatherInfo: WeatherHelper.WeatherInfo? = WeatherHelper.getCurrentWeather(this)
                Log.i(TAG, "loadWeatherData: huang info=>" + weatherInfo)
                handler!!.post(java.lang.Runnable {
                    if (weatherInfo != null) {
                        mViewBinding.weatherIvInfoHide?.visibility = View.GONE
                        mViewBinding.weaterTvTemp.text = weatherInfo.temperature + "℃"
                        val weather: String = (WeatherTextMapper.description(this, weatherInfo.condCode))
                        mViewBinding.weaterTvTemp.visibility = View.VISIBLE

                        mViewBinding.tvWeatherDesc.visibility = View.VISIBLE
                        mViewBinding.tvWeatherDesc.text = weather
                        WeatherIconLoader.getWeatherIcon(mViewBinding.weatherIvInfo, weatherInfo.condCode)
                        mViewBinding.weatherIvInfo.visibility = View.VISIBLE

                        mViewBinding.tvWeatherCity.text = weatherInfo.location
                        mViewBinding.tvWeatherCity.visibility = View.VISIBLE
                        mViewBinding.ivWeatherIcon.visibility = View.VISIBLE

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault())
                        val dateStr = dateFormat.format(Date(weatherInfo.updateTime))
                        mViewBinding.weatherDate.text = dateStr
                        mViewBinding.weatherDate.visibility = View.VISIBLE

                    } else {
                        mViewBinding.weatherIvInfoHide?.visibility = View.VISIBLE
                        mViewBinding.weatherIvInfo.visibility = View.GONE
                        mViewBinding.tvWeatherCity.visibility = View.GONE
                        mViewBinding.tvWeatherDesc.visibility = View.GONE
                        mViewBinding.ivWeatherIcon.visibility = View.GONE
                        mViewBinding.weatherDate.visibility = View.GONE
                        mViewBinding.weaterTvTemp.visibility = View.GONE
                    }
                })
            }).start()
        }



        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == PERMISSION_REQUEST_CODE) {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadWeatherData()
                } else {
                    // 处理权限被拒绝
                    //showPermissionDeniedMessage()
                    Log.e(TAG, "onRequestPermissionsResult: not have permission==>")
                }
            }
        }
        //----------------------
}