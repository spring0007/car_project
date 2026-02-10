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
import android.graphics.BitmapFactory
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
import com.launcher.yfd_ui01.chemo2.CarModelVersion
import com.launcher.yfd_ui01.chemo2.CarPopupWindow
import com.launcher.yfd_ui01.manager.FragmentAnimation
import com.launcher.yfd_ui01.pop.AppPopupWindow
import com.launcher.yfd_ui01.utils.FreeformUtils.startFreeformApp
import com.launcher.yfd_ui01.utils.LogUtil
import com.launcher.yfd_ui01.utils.SystemUIClient
import com.launcher.yfd_ui01.utils.SystemUIClient.OPEN_APP_TO_FREEFORM
import com.launcher.yfd_ui01.view.AppItemView
import com.launcher.yfd_ui01.view.DashboardView
import com.launcher.yfd_ui01.view.DialWidget
import com.launcher.yfd_ui01.view.MusicWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    //    private val BIN_DATA_SPEED_UNIT = 0x84
    private var accRecor: Boolean? = null
    lateinit var systemUIClient: SystemUIClient
    var viewAddNeedToStartFreeform: Boolean = false
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
        // 创建速度模拟器
//        speedSimulator = SpeedSimulator(object : SpeedSimulator.SpeedChangeListener {
//            override fun onSpeedChanged(speed: Int) {
//                // 在主线程中更新UI
//                runOnUiThread {
//                    // 您可以在这里处理其他与速度相关的逻辑
//                    LogUtil.i( "speed = $speed")
//                    dashboardView.udDataSpeed(speed)
//                   // mViewBinding.carSpeedTv.text = "$speed"
//                }
//            }
//        })
//
//        // 开始模拟
//        speedSimulator.startSimulation()

    }

    private fun getAppInfoByPkg(view: AppItemView,appNumber:Int,defaultPackage:String)
    {
        var pkg = Settings.System.getString(requireContext().contentResolver,"launcher_app_icon_$appNumber")
        if (TextUtils.isEmpty(pkg))
            pkg = defaultPackage
        var info = IconManager.getAppIcon(requireContext(),pkg)
        view.setAppName(info.label)
        view.setAppIcon(info.icon)

    }

    private fun initFreeform(view:View) {
        freeformBg = view.findViewById(R.id.freeform_bg)
        systemUIClient = SystemUIClient(swipeActivity)
        systemUIClient.bindToSystemUIService(swipeActivity)

    }
    private fun updateImagePosition(imageView: ImageView, reason:String) {
        Settings.System.putString(swipeActivity.contentResolver,"freeform_launcher_idle", "1");
        LogUtil.i("freeform_launcher_idle,1")
        val location = IntArray(2)
        imageView.getLocationOnScreen(location)
        val screenX = location[0]
        val screenY = location[1]
        val width = imageView.width
        val height = imageView.height
        LogUtil.i("reason=$reason,screenx=$screenX,screeny=$screenY")
        startFreeformApp(swipeActivity ,  Rect(screenX, screenY, screenX + width, screenY + height))
        systemUIClient.setmFreeformMode(OPEN_APP_TO_FREEFORM)
        //systemUIClient.startOrSetFreeformType( swipeActivity)

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
        //filter.addAction(Intent.ACTION_TIME_TICK)
        //filter.addAction(Intent.ACTION_DATE_CHANGED)
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            requireContext().registerReceiver(receiver, filter)
        }
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if(!isVisible)return
            LogUtil.i( "onReceive:huang action=$action")
            when (action) {
                CommonData.BROADCAST_LAMP_SWITCH -> {}
                CommonData.ACTION_ACC_ON -> {
                    if (freeformBg?.isVisibleOnScreen() == true)
                        updateImagePosition(freeformBg ,"acc_on")
                }
                CommonData.ACTION_ACC_OFF -> {
                    accRecor = true
                    imageUpdateJob?.cancel()
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
                    musicWidget.getCarPlayData(zlinkStatus, phoneMode)
                }

                "android.launcher.show.allApp" -> {
                    imageUpdateJob?.cancel()
                    systemUIClient?.hideFreeform()
                    LogUtil.i( "onClick: huang freeform to hide222==>")
                }

                "CANBUS_CHANGE_SPEED_Unit" -> {}
                "top_session_package_change" -> {
                    val sessionTopPkg = intent.getStringExtra("top_package")
                    handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                }

                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED /*,Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_TICK*/ -> {
                    dialWidget?.updateTimeSystem()
                }

                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    var reason = intent.getStringExtra("reason");
                    if (reason == "recentapps") {
                        canclePopupWindow()
                        imageUpdateJob?.cancel()
                        handlerFreeform.removeCallbacks(hideFreeformRunnable)
                        handlerFreeform.postDelayed(hideFreeformRunnable,100)

                    }else if (reason == "homekey") {//多任务；recent：最近 ,home键
                        canclePopupWindow()
                    }
                }
            }
        }
    }

    private val handlerFreeform = Handler(Looper.getMainLooper())
    private val hideFreeformRunnable = Runnable {
        if (systemUIClient.getmFreeformMode() == OPEN_APP_TO_FREEFORM ) {
            systemUIClient.hideFreeform()
            /*Settings.System.putString( swipeActivity.contentResolver,"freeform_launcher_idle",
                "0"
            );*/
             systemUIClient.fullScreenFreeform()
            LogUtil.i("freeform_launcher_idle,0")
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
    private fun getDisplayName(version: CarModelVersion): String {
        // 从路径中提取品牌和车型信息
        val parts = version.imagePath.split("/")
        if (parts.size >= 3) {
            val brand = formatBrandName(parts[1])
            val fileName = parts[2].substringBeforeLast(".")
            val modelGroup = fileName.substringBefore("_")
            return "$brand ${modelGroup.replaceFirstChar { it.uppercase() }} (${version.displayName})"
        }
        return version.displayName
    }
    private fun formatBrandName(folderName: String): String {
        return folderName.replaceFirstChar { it.uppercase() }
    }

    private fun saveCarVersion(version: CarModelVersion) {
        // 复制图片到应用私有目录
        val savedPath = copyCarImageToStorage(version)

        // 保存信息到 SharedPreferences
        val sharedPrefs =  requireContext().getSharedPreferences("car_model_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("car_version_name", version.displayName)
            putString("car_brand_folder", version.brandFolder)
            putString("car_image_path", version.imagePath)
            putString("car_asset_path", version.imagePath)
            putString("saved_image_path", savedPath)
            //putString("car_full_display_name", getDisplayName(version))
            apply()
        }
    }
    private fun copyCarImageToStorage(version: CarModelVersion): String? {
        if (version.imagePath.startsWith("chemo/")) {
            // 从assets复制到私有目录
            val fileName = "${version.brandFolder}_${version.name}.png"
            return CarDataScanner.copyImageToPrivateStorage(requireContext(), version.imagePath, fileName)
        }
        return null
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
                // 如果文件加载失败，从assets加载
                loadImageFromAssets(version.imagePath)
            }
        } else {
            // 从assets加载
            loadImageFromAssets(version.imagePath)
        }

        //carIcon.contentDescription = getDisplayName(version)
    }

    private fun loadImageFromAssets(assetPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = swipeActivity.assets.open(assetPath)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                withContext(Dispatchers.Main) {
                    bitmap?.let {
                        carIcon.setImageBitmap(it)
                    } ?: run {
                        carIcon.setImageResource(R.drawable.a3_2008_2012)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    carIcon.setImageResource(R.drawable.a3_2008_2012)
                }
                LogUtil.e("加载图片失败: $assetPath", e)
            }
        }
    }


    private fun restoreCarModel() {
        val sharedPrefs =  requireContext().getSharedPreferences("car_model_prefs", Context.MODE_PRIVATE)
        val savedPath = sharedPrefs.getString("saved_image_path", null)
        val assetPath = sharedPrefs.getString("car_asset_path", null)
        //val displayName = sharedPrefs.getString("car_full_display_name", null)

        if (savedPath != null /*&& displayName != null*/) {
            val file = File(savedPath)
            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(savedPath)
                    if(bitmap!=null) {
                        carIcon.setImageBitmap(bitmap)
                        // carIcon.contentDescription = displayName
                        return
                    }
                } catch (e: Exception) {
                    LogUtil.e( "恢复图片失败，尝试从assets加载", e)
                }
            }
        }

        // 尝试从assets恢复
        if (assetPath != null/* && displayName != null*/) {
            loadImageFromAssets(assetPath)
            //carIcon.contentDescription = displayName
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
        var isboot = true
        if (intent != null) {
            for (index in IconCache.WorkSpacePackageName.indices) {
                if (packName != IconCache.WorkSpacePackageName[index]) {
                    isboot = false
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
        var isboot = true
        for (index in IconCache.WorkSpacePackageName.indices) {
            if (packName != IconCache.WorkSpacePackageName[index]) {
                isboot = false
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
        locationManager.requestLocationUpdates("gps", 1000, 10f, locationListener, mHandle.looper)
    }

    override fun onStart() {
        super.onStart()
        LogUtil.i("lqq,onStart")
    }
    private var imageUpdateJob: Job? = null

    override fun onResume() {
        super.onResume()
        //LogUtil.i("lqq,onResume ,isVisible=$isVisible,isAdded=$isAdded, isMenuVisible= $isMenuVisible")
        LogUtil.i("lqq,onResume ")
        dialWidget?.startAnimation()
        imageUpdateJob?.cancel()


        if (checkTopAppLollipop(swipeActivity)) {

            // 启动新的协程任务
            imageUpdateJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(150)
                LogUtil.i("lqq,onResume ,isVisibleOnScreen="+freeformBg.isVisibleOnScreen())
                if (freeformBg.isVisibleOnScreen()) {
                    updateImagePosition(freeformBg, "onResume")
                }
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
     * 使用show/hide方式切换Fragment时，会调用此方法
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        LogUtil.i( "onHiddenChanged: hidden=$hidden")

        if (hidden) {
            // Fragment被隐藏，隐藏自由窗口
            imageUpdateJob?.cancel()
            systemUIClient.hideFreeform()
            Settings.System.putString(swipeActivity.contentResolver,"freeform_launcher_idle", "0");
            systemUIClient.fullScreenFreeform()
            LogUtil.i("freeform_launcher_idle,0")

        } else {
            // Fragment被显示，显示自由窗口
            freeformBg.post {
                if (freeformBg.isVisibleOnScreen()) {

                    //freeformBg.focusable = View.FOCUSABLE
                    updateImagePosition(freeformBg ,"hidden")
                }

            }
        }
    }


    override fun onStop() {
        super.onStop()
        LogUtil.i("freeform_launcher_idle,0")
        //imageUpdateJob?.cancel()
        if(systemUIClient.getmFreeformMode() == OPEN_APP_TO_FREEFORM) {
            systemUIClient.hideFreeform()
            Settings.System.putString(swipeActivity.contentResolver,"freeform_launcher_idle", "0");
            systemUIClient.fullScreenFreeform()
        }else
            Settings.System.putString(swipeActivity.contentResolver,"freeform_launcher_idle", "0");

        LogUtil.i("lqq,onStop")
    }

    override fun onPause() {
        super.onPause()
        dialWidget?.stopAnimation()
        dashboardView?.closeAnimation()
        canclePopupWindow()

        LogUtil.i("lqq,onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        receiver?.let {
            try {
                requireContext().unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // 接收器未注册，忽略此异常或记录日志
                e.printStackTrace()
            }
        }
        LogUtil.i("lqq,onDestroy")

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

