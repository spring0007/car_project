package com.launcher.yfd_ui01.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.awell.addapp.AppInfo
import com.awell.control.AwellMediaControl
import com.awell.launcher2.IconCache
import com.awell.utils.CommonData
import com.awell.utils.Utils.startWallpaper
import com.launcher.yfd_ui01.MainActivity_YFD_UI01
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.app.IconManager
import com.launcher.yfd_ui01.chemo2.CarDataScanner
import com.launcher.yfd_ui01.chemo2.CarModelSource
import com.launcher.yfd_ui01.chemo2.CarModelVersion
import com.launcher.yfd_ui01.chemo2.CarPopupWindow
import com.launcher.yfd_ui01.manager.FragmentAnimation
import com.launcher.yfd_ui01.pop.AppPopupWindow
import com.awell.utils.FreeformUtils.startFreeformApp
import com.awell.library.util.LogUtil
import com.awell.library.util.SystemUIClient
import com.awell.utils.FreeformUtils.OPEN_APP_TO_FREEFORM
import com.launcher.yfd_ui01.view.AppItemView
import com.launcher.yfd_ui01.view.DashboardView
import com.launcher.yfd_ui01.view.DialWidget
import com.launcher.yfd_ui01.view.MusicWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class MainFragment : Fragment(), View.OnTouchListener,  AppPopupWindow.OnPopupUpdateListener {


    private val TAG = MainFragment::class.simpleName
    lateinit var mediaControl: AwellMediaControl
    private lateinit var musicWidget: MusicWidget
    private lateinit var locationManager: LocationManager
    private lateinit var dashboardView: DashboardView
    private lateinit var dialWidget: DialWidget
    private lateinit var freeformBg: ImageView
    private lateinit var carIcon: ImageView
    private lateinit var carIconLinear: LinearLayout
    private lateinit var hotsetWindowApp: AppItemView
    private lateinit var hotsetBtApp: AppItemView
    private lateinit var hotsetDspApp: AppItemView
    private lateinit var hotsetAllApp: AppItemView

    //private lateinit var speedSimulator: SpeedSimulator
    private val MSG_UPDATE_SPEED = 1
    private val MSG_CLEAR_SPEED = 2
    private val BIN_DATA_SPEED_UNIT = 0x84
    private var accRecor: Boolean = false
    lateinit var systemUIClient: SystemUIClient
 //   private lateinit var imagePreferences: ImagePreferences
//    private lateinit var appScope: AppCoroutineScope
//    private var imagePopupWindow: PopupWindow? = null
    private var appPopupWindow: AppPopupWindow? = null
    private var carPopupWindow: CarPopupWindow? = null
    protected lateinit var swipeActivity: MainActivity_YFD_UI01
    private var startX = 0f
    private var startY = 0f
    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        return view
        // return super.onCreateView(inflater, container, savedInstanceState)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeActivity = activity as MainActivity_YFD_UI01
        dashboardView = view.findViewById(R.id.car_speed_point)
        dialWidget = view.findViewById(R.id.dial_widget_layout)
        carIconLinear = view.findViewById(R.id.car_icon_linear)
        carIcon = view.findViewById(R.id.car_icon)
        hotsetWindowApp = view.findViewById(R.id.hotset_window_app)
        hotsetBtApp = view.findViewById(R.id.hotset_bt_app)
        hotsetDspApp = view.findViewById(R.id.hotset_dsp_app)
        hotsetAllApp = view.findViewById(R.id.hotset_all_app)
        getAppInfoByPkg(hotsetBtApp,1,"com.awell.bluetooth")
        getAppInfoByPkg(hotsetDspApp,2,"com.awell.eqselect")
        getAppInfoByPkg(hotsetAllApp,3,"com.launcher.yfd_ui01")

        LogUtil.i( "onViewCreated")
        view.setOnTouchListener(this)
        view.setOnLongClickListener{
            startWallpaper()
            true
        }

//        initPopouWindow()

        initMediaMusic(view)
        initFreeform(view)

        initTouchAndSpeedListener()
        clickStartApp()
        //chemo reset data
        restoreCarModel()
        initBroadcastReceiver()

    }

   /* private val updateRunnable = object : Runnable {
        override fun run() {
            // 生成 0～100 的随机整数
            val randomNumber = (0..50).random()

            // 延迟1秒再次执行
            val speedKm = randomNumber * 3.6
            val speedMild = speedKm / 1.6093
            val msg = mHandle.obtainMessage().apply {
                what = MSG_UPDATE_SPEED
                arg1 = speedKm.toInt()
                arg2 = speedMild.toInt()
            }
            mHandle.sendMessage(msg)
            mHandle.postDelayed(this, 1000L)
        }
    }*/

    private fun getAppInfoByPkg(view: AppItemView,appNumber:Int,defaultPackage:String)
    {
        var pkg = Settings.System.getString(requireContext().contentResolver,"launcher_app_icon_$appNumber")
        if (TextUtils.isEmpty(pkg))
            pkg = defaultPackage
        val info = IconManager.getAppIcon(requireContext(),pkg)
        view.appName = info.label
        view.setAppIcon(info.icon)

    }

    private fun initFreeform(view:View) {
        freeformBg = view.findViewById(R.id.freeform_bg)
        systemUIClient = SystemUIClient(swipeActivity)
        systemUIClient.bindToSystemUIService(swipeActivity)

    }
    private fun updateImagePosition(imageView: ImageView, reason: String) {
        Settings.System.putString(swipeActivity.contentResolver, "freeform_launcher_idle", "1")
        LogUtil.i("freeform_launcher_idle,1")
        val location = IntArray(2)
        imageView.getLocationOnScreen(location)
        val screenX = location[0]
        val screenY = location[1]
        val width = imageView.width
        val height = imageView.height
        LogUtil.i("reason=$reason,screenx=$screenX,screeny=$screenY")
        // 先标记为自由窗口模式,再启动小窗(与其它插件顺序一致,避免启动失败后模式停留在旧值)
        systemUIClient.setmFreeformMode(OPEN_APP_TO_FREEFORM)
        startFreeformApp(swipeActivity, Rect(screenX, screenY, screenX + width, screenY + height))
    }

    /**
     * 显示自由窗口:仅当 freeformBg 可见时重新定位;否则进入重试机制
     */
    private fun showFreeformWindowIfVisible(reason: String) {
        if (freeformBg.isVisibleOnScreen()) {
            updateImagePosition(freeformBg, reason)
        } else {
            LogUtil.w("$reason: freeformBg 不可见,启动重试机制")
            retryCheckVisibility(3)
        }
    }

    /**
     * 隐藏自由窗口(仅隐藏,不改变应用运行状态)
     */
    private fun hideFreeformWindow() {
        cancelPendingFreeformTasks()
        systemUIClient.hideFreeform()
        Settings.System.putString(swipeActivity.contentResolver, "freeform_launcher_idle", "0")
        LogUtil.i("freeform_launcher_idle,0")
    }

    /**
     * 隐藏自由窗口并全屏化(仅在自由窗口模式时执行)
     */
    private fun fullscreenFreeformWindow() {
        if (systemUIClient.getmFreeformMode() != OPEN_APP_TO_FREEFORM) return
        systemUIClient.hideFreeform()
        Settings.System.putString(swipeActivity.contentResolver, "freeform_launcher_idle", "0")
        systemUIClient.fullScreenFreeform()
        LogUtil.i("freeform_launcher_idle,0")
    }

    /**
     * 取消所有待执行的自由窗口任务(延时展示/可见性重试/延时隐藏)
     */
    private fun cancelPendingFreeformTasks() {
        imageUpdateJob?.cancel()
        pendingShowRunnable?.let { freeformBg.removeCallbacks(it) }
        pendingShowRunnable = null
        retryVisibilityRunnable?.let { freeformBg.removeCallbacks(it) }
        retryVisibilityRunnable = null
        handlerFreeform.removeCallbacks(hideFreeformRunnable)
    }

    /**
     * 初始化媒体信息
     * 以及绑定宿主服务
     *
     */
    private fun initMediaMusic(view:View) {

//        mMediaListener.initDependencies(baseContext)
        mediaControl = AwellMediaControl()
        LogUtil.i( "initMediaMusic: huang UI2 bind data service=>${this}")
        mediaControl.bindDataService(requireContext())
        mediaControl.updateMusicView = mediaImpl
        musicWidget = view.findViewById(R.id.music_widget_layout)
        musicWidget.setMediaLibrary(mediaControl)
        musicWidget.setActivity(requireContext(), musicWidget)
        mediaControl.refreshCurrentMediaState()

    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun initBroadcastReceiver() {
        // 基础广播过滤器(普通 action,无 data scheme)
        val filter = IntentFilter().apply {
            addAction(CommonData.BROADCAST_LAMP_SWITCH)
            addAction(CommonData.ACTION_ACC_ON)
            addAction(CommonData.ACTION_ACC_OFF)
            addAction(CommonData.ACTION_ZLINK)
            addAction(CommonData.ACTION_SHOW_ALL_APP)
            addAction(CommonData.BROADCAST_MEDIA_EXIT)
            addAction(CommonData.ACTION_SPEED_UNIT_CHANGE)
            addAction(CommonData.ACTION_TOP_SESSION_CHANGE)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            //filter.addAction(Intent.ACTION_TIME_TICK)
            //filter.addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            addAction("awellauto.backcar.on")
            addAction("com.awell.360floatview.fullscreen")
        }

        // U 盘插拔监听需要独立的 file data scheme 过滤器
        val mediaFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(receiver, filter, RECEIVER_EXPORTED)
            requireContext().registerReceiver(receiver, mediaFilter, RECEIVER_EXPORTED)
        } else {
            requireContext().registerReceiver(receiver, filter)
            requireContext().registerReceiver(receiver, mediaFilter)
        }
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            // 只有当 Fragment 处于活跃且可见状态时才处理广播
            if (!isResumed || !isVisible ) {
                LogUtil.d("Fragment not resumed, ignoring broadcast: ${intent?.action}")
                return
            }
            LogUtil.i( "onReceive:huang action=$action")
            when (action) {
                CommonData.BROADCAST_LAMP_SWITCH -> {}
                CommonData.ACTION_ACC_ON -> {
                    showFreeformWindowIfVisible("acc_on")

                    dashboardView?.postDelayed({ accRecor = false}, 8 * 1000)
                }
                CommonData.ACTION_ACC_OFF -> {
                    accRecor = true
                    hideFreeformWindow()
                }

                CommonData.BROADCAST_MEDIA_EXIT -> {
                    val pkg = intent.getStringExtra("package")
                    if (pkg != null && (pkg == "cn.kuwo.kwmusiccar" || pkg == "exitAll")) {

                    }
                }

                CommonData.ACTION_ZLINK -> {
                    val zlinkStatus = intent.getStringExtra("status")
                    val phoneMode = intent.getStringExtra("phoneMode")
                    LogUtil.d( "zlinkStatus:$zlinkStatus")
                    if (zlinkStatus == null) {
                        return
                    }
                    musicWidget.getCarPlayData(zlinkStatus, phoneMode)
                }

                CommonData.ACTION_SHOW_ALL_APP -> {
                    hideFreeformWindow()
                    LogUtil.i( "onClick: huang freeform to hide222==>")
                }

                CommonData.ACTION_SPEED_UNIT_CHANGE -> {}
                CommonData.ACTION_TOP_SESSION_CHANGE -> {
                    val sessionTopPkg = intent.getStringExtra(CommonData.EXTRA_TOP_PACKAGE)
                    LogUtil.i("ACTION_TOP_SESSION_CHANGE: sessionTopPkg=$sessionTopPkg")
                }

                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED /*,Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_TICK*/ -> {
                    dialWidget?.updateTimeSystem()
                }

                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    var reason = intent.getStringExtra("reason")
                    if (reason == "recentapps") {
                        canclePopupWindow()
                        cancelPendingFreeformTasks()
                        handlerFreeform.postDelayed(hideFreeformRunnable,100)

                    }else if (reason == "homekey") {//多任务；recent：最近 ,home键
                        canclePopupWindow()
                    }
                }
                "com.awell.360floatview.fullscreen",
                "awellauto.backcar.on" -> {
                    val freePkg = Settings.System.getString(requireContext().contentResolver,"freeform_app_package_name")
                    if("cn.cardoor.zt360".equals(freePkg)) {
                        canclePopupWindow()
                        systemUIClient.fullScreenFreeform()
                    }
                }
                
                // U 盘插拔事件处理
                Intent.ACTION_MEDIA_MOUNTED -> {
                    LogUtil.i("U 盘已插入：${intent.data}")
                    // 如果 carPopupWindow 正在显示，隐藏它
                    if (carPopupWindow != null && carPopupWindow!!.isShowing) {
                        carPopupWindow?.dismiss()
                        LogUtil.d("U 盘插入，隐藏 carPopupWindow")
                    }
                }
                
                Intent.ACTION_MEDIA_EJECT,
                Intent.ACTION_MEDIA_REMOVED -> {
                    LogUtil.i("U 盘已拔出：${intent.data}")
                    // 如果 carPopupWindow 正在显示，隐藏它
                    if (carPopupWindow != null && carPopupWindow!!.isShowing) {
                        carPopupWindow?.dismiss()
                        LogUtil.d("U 盘拔出，隐藏 carPopupWindow")
                    }
                }
            }
        }
    }

    private val handlerFreeform = Handler(Looper.getMainLooper())
    private val hideFreeformRunnable = Runnable {
        fullscreenFreeformWindow()
    }

    val mHandle: Handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_UPDATE_SPEED -> {
                        val speedKm = msg.arg1.toString()
                        val speedMile = msg.arg2.toString()
                        LogUtil.i("speedKm:$speedKm,speedMile:$speedMile ,accRecor:$accRecor,")
                        if (!accRecor) {
                            val unit = ByteArray(1)
                            CommonData.readDataToMeta(unit, BIN_DATA_SPEED_UNIT)
                            val unitData = unit[0].toInt()
                            if (unitData == 0) {
                                dashboardView.udDataSpeed(speedKm.toInt())
                                //mViewBinding.tvGpsSpeed.text = speedKm
                                //mViewBinding.tvGpsSpeedUnit.text = "KM/h"
                            } else if (unitData == 1) {
                                dashboardView.udDataSpeed(speedMile.toInt())
                                //mViewBinding.tvGpsSpeed.text = speedMile
                                //mViewBinding.tvGpsSpeedUnit.text = "mph"
                            }
                            // 归零机制:不能在 GPS 更新后 1 秒就清 0。
                            // GPS 的 minTime=1000ms 只是下限,低速行驶时受距离阈值影响,
                            // 实际上报间隔可达 2~5 秒,1 秒超时会在正常行驶时把指针清成 0(每 2 秒跳 0)。
                            // 改为 3 秒安全网定时器:正常行驶时每秒上报会不断刷新它,不会触发;
                            // 仅当 GPS 连续 3 秒无上报(停车/信号丢失)时才归零。
                            mHandle.removeMessages(MSG_CLEAR_SPEED)
                            mHandle.sendEmptyMessageDelayed(MSG_CLEAR_SPEED, 3000)
                        }
                    }
                    MSG_CLEAR_SPEED -> {
                        LogUtil.i("MSG_CLEAR_SPEED: 3 秒无GPS上报,归零")
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
        hotsetAllApp.setOnClickListener {
            //canclePopupWindow()
            var pkg = Settings.System.getString(requireContext().contentResolver,"launcher_app_icon_3")

            if(TextUtils.isEmpty(pkg) || pkg.equals("com.launcher.yfd_ui01")){
                swipeActivity.goToFragment(1, FragmentAnimation.FADE)
            }else{
                onClickFun(3,"com.awell.eqselect", "com.awell.eqselect.MainActivity")
            }
        }

        hotsetDspApp.setOnClickListener {
            onClickFun(2,"com.awell.eqselect", "com.awell.eqselect.MainActivity")
        }

        hotsetBtApp.setOnClickListener {
            onClickFun(1,"com.awell.bluetooth", "com.awell.bluetooth.MainActivity")
        }

        hotsetWindowApp.setOnClickListener {
            systemUIClient.fullScreenFreeform()

        }
        hotsetWindowApp.setOnLongClickListener {
            //cancelLongPressDetection()
            val mIntent = Intent()
            mIntent.setPackage("com.awell.carsetting")
            mIntent.component =
                ComponentName("com.awell.carsetting", "com.awell.carsetting.MainActivity")
            mIntent.putExtra("SelectDefaultId", 3)
            mIntent.putExtra("SelectDefaultFragment", 30)
            startActivityIntent(mIntent)
            true
        }

        hotsetBtApp.setOnLongClickListener{
            setupPopupWindow(1)
            true
        }
        hotsetDspApp.setOnLongClickListener{
            setupPopupWindow(2)
            true
        }
        hotsetAllApp.setOnLongClickListener {
            setupPopupWindow(3)
            true
        }

        carIconLinear.setOnLongClickListener {
            showCarModelSelector()
            true
        }
        dialWidget.setOnClickListener {
            val intent = Intent(Settings.ACTION_DATE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun onClickFun(appIndex:Int, pkg:String, clazz: String){
        var intent = startActivityByPkg(appIndex,pkg)
        if (intent==null)
            startActivityByPkgClazz(pkg, clazz)
        else{
            val freePkg = Settings.System.getString(requireContext().contentResolver,"freeform_app_package_name")
            if(intent.`package`.equals(freePkg))
                systemUIClient.fullScreenFreeform()
            else
            startActivityIntent(intent)
        }
    }
//---------------CarPopupWindow---------------
    private fun showCarModelSelector() {
        if(carPopupWindow!=null) {
            carPopupWindow!!.dismiss()
            carPopupWindow = null
        }

        carPopupWindow = CarPopupWindow(swipeActivity) { selectedVersion ->
            // 保存选中的车型版本
            saveCarVersion(selectedVersion)
            // 设置图片到ImageView
            setCarImageToImageView(selectedVersion)

//            Toast.makeText(
//                requireContext(),
//                "已设置车模: ${getDisplayName(selectedVersion)}",
//                Toast.LENGTH_SHORT
//            ).show()
        }

        if(carPopupWindow!=null) {
            carPopupWindow!!.setOnPopupUpdateListener(this)
            carPopupWindow!!.contentView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    carPopupWindow!!.contentView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    // PopupWindow显示后，显示半透明背景
                    backgroundAlpha(0.3f)
                }
            })

            carPopupWindow!!.showAtLocation(
                swipeActivity.findViewById(android.R.id.content),
                Gravity.CENTER,
                0,
                0
            )
        }

    }


    private fun saveCarVersion(version: CarModelVersion) {
        // 复制图片到应用私有目录
        val savedPath = copyCarImageToStorage(version)

        // 保存信息到 SharedPreferences
        val sharedPrefs =  requireContext().getSharedPreferences("car_model_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("car_version_name", version.displayName)
            putString("car_brand_folder", version.brandFolder)
            putString("car_asset_path", version.imagePath)//保存图片的assets路径
            putString("car_source", version.carSource.name)
            putString("saved_image_path", savedPath)
            //putString("car_full_display_name", getDisplayName(version))
            LogUtil.d("保存信息到 SharedPreferences: $sharedPrefs.getAll()")
            apply()
        }
    }
    private fun copyCarImageToStorage(version: CarModelVersion): String? {
        return when (version.carSource) {
            CarModelSource.USB,
            CarModelSource.SDCARD -> {
                // 从 SD 卡复制图片到私有目录
                copyImageFromContentUri(version.imagePath, version.name)
            }
            CarModelSource.CUSTOM -> {
                if (version.imagePath.startsWith("chemo/")) {
                    // 从 assets 复制到私有目录
                    val fileName = "${version.brandFolder}_${version.name}.png"
                    CarDataScanner.copyImageToPrivateStorage(requireContext(), version.imagePath, fileName)
                } else {
                    null
                }
            }
        }
    }

    /**
     * 从 SD 卡的 Content URI 复制图片到私有目录
     */
    private fun copyImageFromContentUri(contentUri: String, imageName: String): String? {
        return try {
            val uri = android.net.Uri.parse(contentUri)
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null

            // 创建目标目录
            val destDir = File(requireContext().filesDir, "saved_cars")
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            // 生成目标文件名
            val fileName = "sdcard_${imageName}"
            val destFile = File(destDir, fileName)

            // 复制文件
            val outputStream = FileOutputStream(destFile)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            LogUtil.i("从 SD 卡复制图片到私有目录：$fileName")
            destFile.absolutePath
        } catch (e: Exception) {
            LogUtil.e("从 SD 卡复制图片失败：$contentUri", e)
            null
        }
    }

    private fun setCarImageToImageView(version: CarModelVersion) {
        // 优先使用保存的路径
        val sharedPrefs =  requireContext().getSharedPreferences("car_model_prefs", Context.MODE_PRIVATE)
        val savedPath = sharedPrefs.getString("saved_image_path", null)

        if (savedPath != null && File(savedPath).exists()) {
            // 从保存的文件加载
            try {
                val bitmap = BitmapFactory.decodeFile(savedPath)
                carIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // 如果文件加载失败，重新加载
                loadImageFromAllSources(version)
            }
        } else {
            // 重新加载
            loadImageFromAllSources(version)
        }

        //carIcon.contentDescription = getDisplayName(version)
    }

    /**
     * 根据保存的 carSource 类型加载图片
     */
    private fun loadImageFromAllSources(version: CarModelVersion) {

        val source = try {
            version.carSource
        } catch (e: Exception) {
            CarModelSource.CUSTOM
        }
        
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bitmap = when (source) {
                CarModelSource.USB,
                CarModelSource.SDCARD -> {
                    // 从 SD 卡保存图片加载
                    loadBitmapFromSdCard(version.imagePath)
                }
                CarModelSource.CUSTOM -> {
                    // 从 assets 加载内置车模
                    loadBitmapFromAssets(version.imagePath)
                }
            }
                
            withContext(Dispatchers.Main) {
                bitmap?.let {
                    carIcon.setImageBitmap(it)
                } ?: run {
                    // 加载失败，使用默认背景
                    LogUtil.w("根据来源 $source 加载图片失败，使用默认背景：${version.imagePath}")
                    carIcon.setImageResource(R.drawable.a3_2008_2012)
                }
            }
        }
    }
    
    /**
     * 从 assets 加载 Bitmap
     */
    private fun loadBitmapFromAssets(assetPath: String): Bitmap? {
        return try {
            if (!assetPath.startsWith("chemo/")) {
                return null
            }
            val inputStream = swipeActivity.assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            LogUtil.e("从 assets 加载图片失败：$assetPath", e)
            null
        }
    }
    
    /**
     * 从 SD 卡加载图片
     */
    private fun loadBitmapFromSdCard(imagePath: String): Bitmap? {
        return try {
            // 检查私有目录中是否有保存的 SD 卡图片
            val savedCarsDir = File(requireContext().filesDir, "saved_cars")
            if (!savedCarsDir.exists()) {
                return null
            }
                
            // 查找以 "sdcard_" 开头的图片文件
            val sdcardFiles = savedCarsDir.listFiles { file ->
                file.isFile && file.name.startsWith("sdcard_")
            }?.sortedByDescending { it.lastModified() }
                
            if (!sdcardFiles.isNullOrEmpty()) {
                val targetFile = sdcardFiles.first()
                val bitmap = BitmapFactory.decodeFile(targetFile.absolutePath)
                if (bitmap != null) {
                    LogUtil.i("从 SD 卡保存图片加载成功：${targetFile.name}")
                    return bitmap
                }
            }
            null
        } catch (e: Exception) {
            LogUtil.e("从 SD 卡加载图片失败", e)
            null
        }
    }
    
    private fun restoreCarModel() {
        val sharedPrefs = requireContext().getSharedPreferences("car_model_prefs", Context.MODE_PRIVATE)
        val savedPath = sharedPrefs.getString("saved_image_path", null)
        //val brandFolder = sharedPrefs.getString("car_brand_folder", null)

        
        // 尝试从保存的路径恢复图片
        if (savedPath != null) {
            val file = File(savedPath)
            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(savedPath)
                    if (bitmap != null) {
                        carIcon.setImageBitmap(bitmap)
                        return
                    }
                } catch (e: Exception) {
                    LogUtil.e("恢复图片失败", e)
                }
            }
        }

        // 根据保存的 car_source 类型加载图片
        val assetPath = sharedPrefs.getString("car_asset_path", null)
        val carSource = sharedPrefs.getString("car_source", "CUSTOM")
        if (assetPath != null) {
            // 旧版本可能保存了非法的枚举名,解析失败时回退到内置车模
            val source = try {
                CarModelSource.valueOf(carSource ?: "CUSTOM")
            } catch (e: IllegalArgumentException) {
                LogUtil.w("无法识别的 car_source=$carSource,回退 CUSTOM")
                CarModelSource.CUSTOM
            }
            loadImageFromAllSources(CarModelVersion(imagePath = assetPath, name = "car_model_name",
                displayName = "car_model_display_name", brandFolder = null, carSource = source))
        }
    }

//-------------------------------
    private fun setupPopupWindow(appNumber: Int) {
        // 初始化PopupWindow，传入自定义数字参数
        appPopupWindow = AppPopupWindow(requireContext(), appNumber)
        appPopupWindow?.setOnPopupUpdateListener(this)
        // 获取PopupWindow的内容视图并设置内部按钮点击事件
//        popupWindow?.getContentView()?.let { popupView ->
//          //  setupPopupButtons(popupView)
//        }
        appPopupWindow?.contentView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                appPopupWindow?.contentView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                // PopupWindow显示后，显示半透明背景
                backgroundAlpha(0.3f)
            }
        })
        appPopupWindow?.show(swipeActivity.findViewById(android.R.id.content),0,10)

    }
    private fun startActivityByPkgClazz(packName: String, className: String?) {
        LogUtil.i("startActivityByPkgClazz,packName=$packName")
        val intent = swipeActivity.packageManager.getLaunchIntentForPackage(packName)
        var isboot = false
        if (intent != null) {
            for (index in IconCache.WorkSpacePackageName.indices) {
                if (packName == IconCache.WorkSpacePackageName[index]) {
                    isboot = true
                    break
                }
            }
            if (packName.contains("com.autonavi")) {
                if (isboot) Settings.System.putString(requireContext().contentResolver, "boot_apk1", packName)
            } else {
                if (isboot) Settings.System.putString(requireContext().contentResolver, "boot_apk2", packName)
            }
            swipeActivity.applicationContext.startActivity(intent)
        }
    }

    private fun startActivityIntent(intent: Intent) {
        LogUtil.i("startActivityIntent")
        val packName = intent.`package`
        var isboot = false
        for (index in IconCache.WorkSpacePackageName.indices) {
            if (packName == IconCache.WorkSpacePackageName[index]) {
                isboot = true
                break
            }
        }
        if (packName != null && packName.contains("com.autonavi")) {
            if (isboot) Settings.System.putString(requireContext().contentResolver, "boot_apk1", packName)
        } else {
            if (isboot) Settings.System.putString(requireContext().contentResolver, "boot_apk2", packName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        swipeActivity.applicationContext.startActivity(intent)
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
        locationManager = requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
        // minDistance 改为 0:原来 10m 阈值会让低速行驶/静止时 GPS 不上报,
        // 指针只能靠超时清 0,导致每 1~2 秒跳 0。0 米后按 minTime 每秒稳定上报,
        // 静止时也能每秒上报 0 速,指针平滑归零。
        locationManager.requestLocationUpdates("gps", CommonData.MEMENTINE, CommonData.MAXINSTANCES, mLocationListener, mHandle.looper)
    }

    override fun onStart() {
        super.onStart()
        LogUtil.i("lqq,onStart")
    }
    private var imageUpdateJob: Job? = null
    private var pendingShowRunnable: Runnable? = null
    private var retryVisibilityRunnable: Runnable? = null

    override fun onResume() {
        super.onResume()
        LogUtil.i("lqq,onResume ")
        dialWidget?.startAnimation()
        cancelPendingFreeformTasks()


        if (checkTopAppLollipop(swipeActivity)) {

            // 启动新的协程任务
            imageUpdateJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(150)
                LogUtil.i("lqq,onResume ,isVisibleOnScreen="+freeformBg.isVisibleOnScreen())
                showFreeformWindowIfVisible("onResume")
            }
        }
    }

    @SuppressLint("ServiceCast")
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


    /**
     * 使用show/hide方式切换 Fragment 时，会调用此方法
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        LogUtil.i( "onHiddenChanged: hidden=$hidden")

        if (hidden) {
            // Fragment 被隐藏，取消待执行任务并隐藏自由窗口
            cancelPendingFreeformTasks()
            fullscreenFreeformWindow()

        } else {
            // Fragment 被显示，显示自由窗口
            // 使用可取消的延时任务,确保视图完成布局和测量后再显示
            pendingShowRunnable?.let { freeformBg.removeCallbacks(it) }
            val runnable = Runnable {
                pendingShowRunnable = null
                LogUtil.i("freeformBg.isVisibleOnScreen()="+freeformBg.isVisibleOnScreen())
                showFreeformWindowIfVisible("hidden")
            }
            pendingShowRunnable = runnable
            freeformBg.postDelayed(runnable, 150)
        }
    }
    
    /**
     * 重试检查视图可见性
     * @param maxRetries 最大重试次数
     */
    private fun retryCheckVisibility(maxRetries: Int) {
        var retryCount = 0

        val checkRunnable = object : Runnable {
            override fun run() {
                if (retryCount >= maxRetries) {
                    LogUtil.w("达到最大重试次数，放弃检查")
                    retryVisibilityRunnable = null
                    return
                }

                retryCount++
                LogUtil.i("第${retryCount}次重试检查 freeformBg 可见性")

                if (freeformBg.isVisibleOnScreen()) {
                    LogUtil.i("重试成功，freeformBg 已可见")
                    retryVisibilityRunnable = null
                    updateImagePosition(freeformBg, "retry_$retryCount")
                } else {
                    // 继续重试
                    freeformBg.postDelayed(this, 100)
                }
            }
        }

        retryVisibilityRunnable = checkRunnable
        freeformBg.postDelayed(checkRunnable, 100)
    }


    override fun onStop() {
        super.onStop()
        LogUtil.i("freeform_launcher_idle,0")
        cancelPendingFreeformTasks()
        fullscreenFreeformWindow()
        Settings.System.putString(swipeActivity.contentResolver,"freeform_launcher_idle", "0")

        LogUtil.i("lqq,onStop")
    }

    override fun onPause() {
        super.onPause()
        dialWidget?.stopAnimation()
        dashboardView?.closeAnimation()
        canclePopupWindow()

        LogUtil.i("lqq,onPause")
    }
    override fun onDestroyView() {
        mHandle.removeCallbacksAndMessages(null)
        dashboardView?.releaseResources()
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPendingFreeformTasks()
        receiver?.let {
            try {
                requireContext().unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // 接收器未注册，忽略此异常或记录日志
                e.printStackTrace()
            }
        }
        LogUtil.i("lqq,onDestroy")
        unregisterCustomerListener()
        // 取消所有协程
   //     appScope.cancelAll()
        //canclePopupWindow()
        try {
            systemUIClient.unbindService(swipeActivity)
        } catch (e: Exception) {
            LogUtil.w( "onDestroy:  unbind systemUIClient service==>${this}")
        }
        try {
            mediaControl.unBindDataService(requireContext())
            LogUtil.i( "onDestroy: huang unbind data service==>${this}")
        } catch (e: Exception) {
            LogUtil.e( "onDestroy: unBindDataService error=>${e.message}")
        }

        // 停止速度模拟器
        //speedSimulator?.stopSimulation()
    }

    private fun unregisterCustomerListener() {
        try {
            locationManager.removeUpdates(mLocationListener)
        } catch (e: Exception) {
            LogUtil.e( "unregisterCustomerListener: removeGpsStatusListener error=>${e.message}")
        }

    }
    //*************************PopupWindow************************************



    private fun backgroundAlpha(alpha: Float) {
        val lp = swipeActivity.window.attributes
        lp.alpha = alpha //0.0-1.0
        swipeActivity.window.attributes = lp
    }

    private fun canclePopupWindow() {
        // 关闭弹窗
//        imagePopupWindow?.let {
//            if (it.isShowing) {
//                it.dismiss()
//            }
//            imagePopupWindow = null
//        }

        carPopupWindow?.let {
            if (it.isShowing) {
                it.dismiss()
            }
            carPopupWindow = null
        }
        appPopupWindow?.let {
            if (it.isShowing) {
                it.dismiss()
            }
            appPopupWindow = null
        }



    }
    private fun startActivityByPkg(appNumber:Int,defaultPackage:String): Intent? {
        var pkg = Settings.System.getString(requireContext().contentResolver,"launcher_app_icon_$appNumber")
        if (TextUtils.isEmpty(pkg))
            pkg = defaultPackage

        var intent =requireContext().packageManager.getLaunchIntentForPackage(pkg)
        return intent


    }

    override fun updateAppImage(info: AppInfo?) {
        if(info!=null){
            if(info.flags == 1) {
                hotsetBtApp.setAppIcon(info.icon)
                hotsetBtApp.appName = info.label
            }else if(info.flags == 2) {
                hotsetDspApp.setAppIcon(info.icon)
                hotsetDspApp.appName = info.label
            }else if(info.flags == 3) {
                hotsetAllApp.setAppIcon(info.icon)
                hotsetAllApp.appName = info.label
            }

        }

    }

    override fun backgroundAlphaWindow(alpha: Float) {
        backgroundAlpha(alpha)
    }


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event== null) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 记录触摸起始位置
                startX = event.x
                startY = event.y
                LogUtil.i("lqq,ACTION_DOWN,startX= $startX ,startY= $startY")
            }

            MotionEvent.ACTION_MOVE -> {
                // 检查是否移动超过阈值
                val dx= event.x - startX
                val dy =event.y - startY
                LogUtil.i("lqq,ACTION_MOVE,dx= $dx ,dy= $dy")
                if(dy<-100 && abs(dx)< 80 ){

                    swipeActivity.goToFragment(1, FragmentAnimation.FADE)

//                    swipeActivity.supportFragmentManager.beginTransaction()
//                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
//                        .replace(R.id.fragment_container, MenuFragment())
//                        .addToBackStack(null)
//                        .commitAllowingStateLoss()
                    return true
                }

            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                LogUtil.i("lqq,ACTION_UP")

            }
        }
        return false
    }

}