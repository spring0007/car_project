package com.launcher.yfd_ui6

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import com.awell.addapp.AppInfo
import com.awell.control.AppsCustomizeConfig
import com.awell.control.AppsCustomizeControl
import com.awell.control.AwellMediaControl
import com.awell.launcher2.IconCache
import com.awell.library.AwellTool
import com.awell.library.util.LogUtil
import com.awell.library.util.SystemUIClient
import com.awell.library.util.WeatherHelper
import com.awell.utils.CommonData
import com.awell.utils.FreeformUtils.NAVI_GAODE_PKG
import com.awell.utils.FreeformUtils.NAVI_GOOGLE_PKG
import com.awell.utils.FreeformUtils.OPEN_APP_TO_FREEFORM
import com.awell.utils.FreeformUtils.SETTINGS_FREEFORM_APP_PACKAGE_NAME
import com.awell.utils.FreeformUtils.startFreeformApp
import com.awell.utils.Utils.startWallpaper
import com.launcher.yfd_ui6.chemo2.CarDataScanner
import com.launcher.yfd_ui6.chemo2.CarModelSource
import com.launcher.yfd_ui6.chemo2.CarModelVersion
import com.launcher.yfd_ui6.chemo2.CarPopupWindow
import com.launcher.yfd_ui6.databinding.UiActivityBinding
import com.launcher.yfd_ui6.pop.AppPopupWindow
import com.launcher.yfd_ui6.pop.OnPopupUpdateListener
import com.launcher.yfd_ui6.utils.IconManager
import com.launcher.yfd_ui6.utils.WeatherIconLoader
import com.launcher.yfd_ui6.utils.WeatherTextMapper
import com.launcher.yfd_ui6.view.AppItemView
import com.launcher.yfd_ui6.view.MusicWidget
import kotlinx.coroutines.Runnable
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class UIActivity : Activity(), View.OnClickListener, OnPopupUpdateListener ,View.OnLongClickListener {

    private val TAG = UIActivity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding

    lateinit var mediaControl: AwellMediaControl
    private lateinit var musicWidget: MusicWidget
    private lateinit var freeformBg: ImageView
    private var accRecor: Boolean = false
    val PERMISSION_REQUEST_CODE: Int = 100
    private var handler: Handler? = null
    private var carPopupWindow: CarPopupWindow? = null
    private var appPopupWindow: AppPopupWindow? = null
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
        restoreCarModel()
        showApps()
        initBroadcastReceiver()

        AppsCustomizeControl.setActivity(this)

        initFreeformControl()
        Settings.System.putString(contentResolver, "ui_has_freeform", "true")


        AppsCustomizeControl.setPluginThemeMode(
            AppsCustomizeConfig.Builder()
                .setIconSize(resources.getDimensionPixelSize(R.dimen.app_icon_size))
                .setFontColor(Color.WHITE)
                .setFontSizeSp(resources.getDimensionPixelSize(R.dimen.font_size_sp)) // 20
                .setIconOffsetY(resources.getDimensionPixelSize(R.dimen.icon_offset_y))
                .setIconTextPadding(resources.getDimensionPixelSize(R.dimen.text_padding))
                .setThemeMode(0xff)
                .setRefresh(false)
                .setTextLine(1)
                .setIconMap(IconManager.getPackageIconMap())
                .setPluginPackageName("com.launcher.yfd_ui6")
                .setIconSizeSame(true)
                .setCellWidthDp(resources.getDimensionPixelSize(R.dimen.cell_width))
                .setCellHeightDp(resources.getDimensionPixelSize(R.dimen.cell_height))
                .setAutoHeightGap(true)
                .setAutoWidthGap(true)
                .setBackgroundTheme(2)
                .setRowCount(3)
                .setColumnCount(5)
                //.setPluginOtherBgName("apple_other_app3_square")
                .build()
        )
    }

    private val handlerFreeform = Handler(Looper.getMainLooper())
    private val hideFreeformRunnable = Runnable {
        fullscreenFreeformWindow()
    }
    private val freeformRunnable = Runnable {
        if (freeformBg.isVisibleOnScreen()) {
            updateImagePosition(freeformBg, "onResume")
        }
    }
    private var retryVisibilityRunnable: Runnable? = null

    override fun onResume() {
        super.onResume()
        LogUtil.i("onResume")
        isResumed = true
        cancelPendingFreeformTasks()

        if (checkTopAppLollipop(this)) {
            // 移除旧任务后重新调度，避免重复启动（与 ui24/ui25 一致）
            handlerFreeform.postDelayed(freeformRunnable, 150)
        }

        if (!isWeatherTimerRunning) {
            handler!!.postDelayed(weatherRefreshRunnable, 0)
            isWeatherTimerRunning = true
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        showApps()
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

    override fun onPause() {
        super.onPause()
        LogUtil.i("onPause")
        isResumed = false
        handler!!.removeCallbacks(weatherRefreshRunnable)
        isWeatherTimerRunning = false
        canclePopupWindow()
    }

    override fun onStop() {
        super.onStop()
        LogUtil.i("onStop")

        cancelPendingFreeformTasks()
        fullscreenFreeformWindow()
        Settings.System.putString(contentResolver, "freeform_launcher_idle", "0")
        LogUtil.w("freeform_launcher_idle,0")
    }

    private fun initFreeformControl() {
        freeformBg = mViewBinding.freeformImage
        systemUIClient = SystemUIClient(this)
        systemUIClient.bindToSystemUIService(this)
    }

    private fun updateImagePosition(imageView: ImageView, reason: String) {
        if (imageView.width <= 0 || imageView.height <= 0) {
            LogUtil.w("reason=$reason, freeformBg 未完成布局，跳过")
            return
        }
        Settings.System.putString(contentResolver, "freeform_launcher_idle", "1")
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
        startFreeformApp(this, Rect(screenX, screenY, screenX + width, screenY + height))
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
        Settings.System.putString(contentResolver, "freeform_launcher_idle", "0")
        LogUtil.i("freeform_launcher_idle,0")
    }

    /**
     * 隐藏自由窗口并全屏化(仅在自由窗口模式时执行)
     */
    private fun fullscreenFreeformWindow() {
        if (systemUIClient.getmFreeformMode() != OPEN_APP_TO_FREEFORM) return
        systemUIClient.hideFreeform()
        Settings.System.putString(contentResolver, "freeform_launcher_idle", "0")
        systemUIClient.fullScreenFreeform()
        LogUtil.i("freeform_launcher_idle,0")
    }

    /**
     * 取消所有待执行的自由窗口任务(延时展示/可见性重试/延时隐藏)
     */
    private fun cancelPendingFreeformTasks() {
        handlerFreeform.removeCallbacks(freeformRunnable)
        retryVisibilityRunnable?.let { freeformBg.removeCallbacks(it) }
        retryVisibilityRunnable = null
        handlerFreeform.removeCallbacks(hideFreeformRunnable)
    }

    //---------------CarPopupWindow---------------
    private fun showCarModelSelector() {
        if (carPopupWindow != null) {
            carPopupWindow!!.dismiss()
            carPopupWindow = null
        }

        carPopupWindow = CarPopupWindow(this) { selectedVersion ->
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

        if (carPopupWindow != null) {
            carPopupWindow!!.setOnPopupUpdateListener(this)
            carPopupWindow!!.contentView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    carPopupWindow!!.contentView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    // PopupWindow显示后，显示半透明背景
                    backgroundAlpha(0.3f)
                }
            })

            carPopupWindow!!.showAtLocation(
                findViewById(android.R.id.content),
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
        val sharedPrefs = getSharedPreferences("car_model_prefs", MODE_PRIVATE)
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
                    CarDataScanner.copyImageToPrivateStorage(this, version.imagePath, fileName)
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
            val uri = Uri.parse(contentUri)
            val inputStream = this.contentResolver.openInputStream(uri) ?: return null

            // 创建目标目录
            val destDir = File(this.filesDir, "saved_cars")
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
        val sharedPrefs = getSharedPreferences("car_model_prefs", MODE_PRIVATE)
        val savedPath = sharedPrefs.getString("saved_image_path", null)

        if (savedPath != null && File(savedPath).exists()) {
            // 从保存的文件加载
            try {
                val bitmap = BitmapFactory.decodeFile(savedPath)
                mViewBinding.carIcon.setImageBitmap(bitmap)
            } catch (_: Exception) {
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
        } catch (_: Exception) {
            CarModelSource.CUSTOM
        }

        Thread(java.lang.Runnable {
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

            runOnUiThread {
                bitmap?.let {
                    mViewBinding.carIcon.setImageBitmap(it)
                } ?: run {
                    // 加载失败，使用默认背景
                    LogUtil.w("根据来源 $source 加载图片失败，使用默认背景：${version.imagePath}")
                    mViewBinding.carIcon.setImageResource(R.drawable.a3_2008_2012)
                }
            }
        }).start()
    }

    /**
     * 从 assets 加载 Bitmap
     */
    private fun loadBitmapFromAssets(assetPath: String): Bitmap? {
        return try {
            if (!assetPath.startsWith("chemo/")) {
                return null
            }
            val inputStream = assets.open(assetPath)
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
            val savedCarsDir = File(filesDir, "saved_cars")
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
        val sharedPrefs = getSharedPreferences("car_model_prefs", MODE_PRIVATE)
        val savedPath = sharedPrefs.getString("saved_image_path", null)
        //val brandFolder = sharedPrefs.getString("car_brand_folder", null)


        // 尝试从保存的路径恢复图片
        if (savedPath != null) {
            val file = File(savedPath)
            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(savedPath)
                    if (bitmap != null) {
                        mViewBinding.carIcon.setImageBitmap(bitmap)
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
            } catch (_: IllegalArgumentException) {
                LogUtil.w("无法识别的 car_source=$carSource,回退 CUSTOM")
                CarModelSource.CUSTOM
            }
            loadImageFromAllSources(
                CarModelVersion(
                    imagePath = assetPath, name = "car_model_name",
                    displayName = "car_model_display_name", brandFolder = null, carSource = source
                )
            )
        }
    }
    //*************************PopupWindow************************************


    /**
     * 弹窗关闭时恢复窗口透明度（CarPopupWindow 回调）
     */
    override fun backgroundAlphaWindow(alpha: Float) {
        backgroundAlpha(alpha)
    }

    /**
     * AppPopupWindow 选择应用后的回调：按 flag 刷新对应槽位
     */
    override fun updateAppImage(info: AppInfo) {
         val flag = info.flags
         refreshAppItem(flag)

    }

    private fun backgroundAlpha(alpha: Float) {
        val lp = window.attributes
        lp.alpha = alpha //0.0-1.0
        window.attributes = lp
    }

    private fun canclePopupWindow() {
        // 关闭弹窗
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

    private fun View.isVisibleOnScreen(): Boolean {
        if (!isShown || !isAttachedToWindow || width <= 0 || height <= 0) {
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
        // carIconLinear = findViewById(R.id.car_icon_linear)
        // carIcon = findViewById(R.id.car_icon)
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
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        filter.addAction("com.awell.360floatview.fullscreen")

        // U 盘插拔监听需要独立的 file data scheme 过滤器
        val mediaFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addDataScheme("file")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
            registerReceiver(receiver, mediaFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
            registerReceiver(receiver, mediaFilter)
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
        cancelPendingFreeformTasks()
        receiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // 接收器未注册，忽略此异常或记录日志
                e.printStackTrace()
            }
        }
        try {
            systemUIClient.unbindService(this)
        } catch (e: Exception) {
            LogUtil.w("onDestroy:  unbind systemUIClient service==>${this}")
        }
        AppsCustomizeControl.setActivity(null)
        cancelLongPressDetection()
        AppsCustomizeControl.hideApps()
        try {
            mediaControl.unBindDataService(this)
        } catch (e: Exception) {
            LogUtil.e("onDestroy: unBindDataService error=>${e.message}")
        }
        LogUtil.i("lqq,onDestroy")

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
                    showFreeformWindowIfVisible("acc_on")

                }

                CommonData.ACTION_ACC_OFF -> {
                    //accRecor = true
                    hideFreeformWindow()
                    canclePopupWindow()
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
                    hideFreeformWindow()
                    AppsCustomizeControl.showApps(findViewById<ViewGroup>(android.R.id.content))
                }


                CommonData.ACTION_TOP_SESSION_CHANGE -> {
                    val sessionTopPkg = intent.getStringExtra(CommonData.EXTRA_TOP_PACKAGE)
                    handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                }

                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    var reason = intent.getStringExtra("reason")
                    if (reason == "recentapps") {
                        canclePopupWindow()
                        cancelPendingFreeformTasks()
                        handlerFreeform.postDelayed(hideFreeformRunnable, 100)

                    } else if (reason == "homekey") {//多任务；recent：最近 ,home键
                        canclePopupWindow()
                    }
                }

                "com.awell.360floatview.fullscreen",
                "awellauto.backcar.on" -> {
                    val freePkg = Settings.System.getString(contentResolver, "freeform_app_package_name")
                    if ("cn.cardoor.zt360".equals(freePkg)) {
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

                    if ("NO_MUSIC_LIST" == songName && "NO_MUSIC_LIST" == singerName && "NO_MUSIC_LIST" == album) {
                        musicWidget.setMusicNameTextView(
                            getResources().getString(R.string.click_play_music),
                            MusicWidget.MUSIC
                        )
                        musicWidget.setArtistNameTextView(
                            getResources().getString(R.string.music_artist),
                            MusicWidget.MUSIC
                        )
                    } else {

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
                        } else {
                            musicWidget.setMusicNameTextView(songName, type)
                            musicWidget.setArtistNameTextView(singerName, type)
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
//                    mViewBinding.tvRadioFreq.text = freq
//                    mViewBinding.tvRadioAmFm.text = fmOrAm
//                        if (fmOrAm == "FM") {
//                            mViewBinding.layoutRadioLayout.fmScaleView.setRadioMode(FMMarkView.RadioMode.FM)
//                        } else {
//                            mViewBinding.layoutRadioLayout.fmScaleView.setRadioMode(FMMarkView.RadioMode.AM)
//                        }
//                        mViewBinding.layoutRadioLayout.fmScaleView.setBandFrequency(freq.toFloat())
                    //mViewBinding.layoutRadioLayout.tvRadioUnit.setText(unit)
                }
            }

            override fun handleOriginBundle(bundle: Bundle) {
                //todo update music widget
                val status = bundle.getString(AwellTool.STATUS_ACCEPT, AwellTool.DEFAULT_S)

            }
        }
    }


    private fun clickStartApp() {

        mViewBinding.appItem1.setOnLongClickListener(this)
        mViewBinding.appItem2.setOnLongClickListener(this)
        mViewBinding.appItem3.setOnLongClickListener(this)
        mViewBinding.appItem4.setOnLongClickListener(this)
        mViewBinding.appItem5.setOnLongClickListener(this)
        mViewBinding.appItem6.setOnLongClickListener(this)
        mViewBinding.appItem7.setOnLongClickListener(this)
        mViewBinding.appItem8.setOnLongClickListener(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AppsCustomizeControl.hideApps()
        Settings.System.putString(contentResolver, "freeform_launcher_idle", "1")
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

     override fun onLongClick(v: View?): Boolean {
         if (v is AppItemView) {
             val flag = v.getFlag()
             if (flag in 1..8) {
                 showAppSelector(flag)
                 return true
             }
         }
         return false
     }

    /**
     * 弹出应用选择窗口（列表 = IconManager.PACKAGE_ICON_MAP 中的应用）
     */
    private fun showAppSelector(flag: Int) {
        appPopupWindow = AppPopupWindow(this, flag)
        appPopupWindow?.setOnPopupUpdateListener(this)
        appPopupWindow?.show(mViewBinding.root, 0, 0)
        //popup.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0,0)
    }

    override fun onClick(v: View?) {
        LogUtil.w("onClick ${v?.id}")
        when (v?.id) {
            //mViewBinding.appItem1.id,
            mViewBinding.hostNavTv.id,
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

            mViewBinding.hostAppTv.id,
            mViewBinding.hostAppIv.id -> {

                systemUIClient.hideFreeform()
                Settings.System.putString(contentResolver, "freeform_launcher_idle", "0")
                systemUIClient.fullScreenFreeform()
                AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
            }

            //mViewBinding.appItem8.id,
            mViewBinding.hostSettingTv.id,
            mViewBinding.hostSettingIv.id -> {
                startActivityItem("com.awell.carsetting", "com.awell.carsetting.MainActivity")
            }

            mViewBinding.weatherImage.id -> {
                startActivityItem("com.awell.weather", "com.awell.weather.MainActivity")
            }

            //mViewBinding.appItem5.id,
            mViewBinding.hostMusicTv.id,
            mViewBinding.hostMusicIv.id -> {
                startActivityItem("com.awell.localmusic", "com.awell.localmusic.MainActivity")
            }

            //mViewBinding.appItem4.id,
            mViewBinding.hostPhoneTv.id,
            mViewBinding.hostPhoneIv.id -> {
                val pkg = Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)
                when (pkg) {
                    "com.awell.bluetooth" -> {
                        systemUIClient.fullScreenFreeform()
                    }

                    else -> {
                        val btIntent = Intent("com.awell.bluetooth")
                        btIntent.setClassName("com.awell.bluetooth", "com.awell.bluetooth.MainActivity")
                        btIntent.putExtra("bt_preference_key", 0)
                        startActivity(btIntent)
                    }
                }
            }
            mViewBinding.carIcon.id -> {
                showCarModelSelector()
            }

            /*mViewBinding.appItem2.id -> {
                startActivityItem("com.awell.radio", "com.awell.radio.MainActivity")
            }*/

            /*mViewBinding.appItem3.id -> {
                startActivityItem("com.awell.localvideo", "com.awell.localvideo.activity.VideoListActivity")
            }*/

            /*mViewBinding.appItem6.id -> {
                startActivityItem("com.android.gallery3d", null)
            }*/

            /*mViewBinding.appItem7.id -> {
                val url = SystemProperties.get("persist.sys.lz.HOME_PAGE", "http://m.baidu.com")
                val chromeIntent = Intent(Intent.ACTION_VIEW)
                chromeIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                chromeIntent.setData(Uri.parse(url))
                chromeIntent.`package`

                // 检查是否有应用可以处理此Intent
                if (chromeIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(chromeIntent)
                }
            }*/
            mViewBinding.appItem1.id,
            mViewBinding.appItem2.id,
            mViewBinding.appItem3.id,
            mViewBinding.appItem4.id,
            mViewBinding.appItem5.id,
            mViewBinding.appItem6.id,
            mViewBinding.appItem7.id,
            mViewBinding.appItem8.id -> {
                val appView = v as AppItemView
                val flag = appView.flag
                startAppByFlag(flag)
            }
        }
    }

    private fun startActivityItem(packName: String, className: String?) {
        val pkg =
            Settings.System.getString(getContentResolver(), SETTINGS_FREEFORM_APP_PACKAGE_NAME)

        if(packName == pkg || (packName == "com.awell.navigation"&&(pkg == NAVI_GOOGLE_PKG || pkg == NAVI_GAODE_PKG))) {
            systemUIClient.fullScreenFreeform()
        }else{
            startActivity(packName, className)
        }
    }

    private fun loadWeatherData() {
        Thread {
            val weatherInfo: WeatherHelper.WeatherInfo? = WeatherHelper.getCurrentWeather(this)
            Log.i(TAG, "loadWeatherData: huang info=>" + weatherInfo)
            handler!!.post {
                if (weatherInfo != null) {
                    mViewBinding.weaterTvTemp.text = weatherInfo.temperature + "℃"
                    val weather: String = (WeatherTextMapper.description(this, weatherInfo.condCode))
                    mViewBinding.weaterTvTemp.visibility = View.VISIBLE

                    mViewBinding.tvWeatherDesc.text = weather
                    mViewBinding.tvWeatherDesc.setSingleLine(true)
                    WeatherIconLoader.getWeatherIcon(mViewBinding.weatherIvInfo, weatherInfo.condCode)
                    mViewBinding.tvWeatherCity.text = weatherInfo.location
                    mViewBinding.tvWeatherCity.visibility = View.VISIBLE

                    //val dateFormat = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault())
                    //val dateStr = dateFormat.format(Date(weatherInfo.updateTime))

                } else {
                    mViewBinding.weatherIvInfo.setImageResource(R.drawable.weather_na)
                    mViewBinding.tvWeatherCity.visibility = View.GONE
                   // mViewBinding.tvWeatherDesc.visibility = View.GONE
                    mViewBinding.tvWeatherDesc.setText(R.string.no_weather_information_available_at_the_moment)
                    mViewBinding.tvWeatherDesc.setLines(4)
                    mViewBinding.weaterTvTemp.visibility = View.GONE
                }
            }
        }.start()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWeatherData()
            } else {
                // 处理权限被拒绝
                //showPermissionDeniedMessage()
                Log.e(TAG, "onRequestPermissionsResult: not have permission==>")
            }
        }
    }

    //====================================
    /**
     * 初始化 8 个应用快捷槽位：按序填充包名默认值（PACKAGE_ICON_MAP 前八个）、图标与名称
     */
    fun showApps() {
         for (flag in 1..8) {
             refreshAppItem(flag)
         }
       /* val url = SystemProperties.get("persist.sys.lz.HOME_PAGE", "http://m.baidu.com")
        val chromeIntent = Intent(Intent.ACTION_VIEW)
        chromeIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        chromeIntent.setData(Uri.parse(url))//
        var packageName = "com.android.chrome"
        if (chromeIntent.`package` != null && chromeIntent.`package` != "")
            packageName = chromeIntent.`package`!!
        val name = getAppNameByPackageName(packageName)
        if (name != null) {
            mViewBinding.appItem7.appName = name
        }*/

    }

    /**
     * 读取槽位包名（Settings 持久化，重启保留）；未配置或未安装时按 PACKAGE_ICON_MAP 前八个写入默认值
     */
    private fun getAppInfo(flag: Int): String? {
        if (flag !in 1..8) return null
        val saved = Settings.System.getString(contentResolver, "$APP_ICON_PREF$flag")
        if (saved != null && saved.isNotEmpty() && isAppInstalled(saved)) {
            return saved
        }
        //val default = defaultAppPackages().getOrNull(flag - 1) ?: return null
        val default = defaultApp[flag - 1]
        if (!isAppInstalled(default)) return null
        Settings.System.putString(contentResolver, "$APP_ICON_PREF$flag", default)
        return default
    }
    private fun isAppInstalled(packageName: String): Boolean =
        runCatching { packageManager.getPackageInfo(packageName, 0) }.isSuccess

    private fun getAppNameByPackageName(packageName: String): String? =
        runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrNull()

    /**
     * 刷新单个槽位的图标与名称（图标优先取 IconManager 主题图标，无则回退系统图标）
     */

    var defaultApp = arrayOf("com.awell.navigation", "com.awell.radio","com.awell.localvideo","com.awell.bluetooth",
        "com.awell.localmusic","com.android.gallery3d","com.android.chrome","com.awell.carsetting")
    private fun refreshAppItem(flag: Int) {
        if (flag !in 1..8) return
        val pkg = getAppInfo(flag) ?: return
        val item = appItems[flag - 1]
        val icon = IconManager.getIcon(this, pkg)
            ?: runCatching { packageManager.getApplicationIcon(pkg) }.getOrNull()
        if (icon != null) {
            item.setAppIcon(icon)
        }
        val name = getAppNameByPackageName(pkg)
        if (name != null) {
            item.appName = name
        }
    }

    /**
     * 根据槽位 flag 启动对应应用
     */
    private fun startAppByFlag(flag: Int) {
        val pkg = getAppInfo(flag) ?: return
        LogUtil.i("startAppByFlag: $pkg")
        startActivityItem(pkg, null)
    }

    /**
     * 8 个槽位视图（flag 对应下标 +1），lazy 保证在 mViewBinding 初始化后才访问
     */
    private val appItems: List<AppItemView> by lazy {
        listOf(
            mViewBinding.appItem1,
            mViewBinding.appItem2,
            mViewBinding.appItem3,
            mViewBinding.appItem4,
            mViewBinding.appItem5,
            mViewBinding.appItem6,
            mViewBinding.appItem7,
            mViewBinding.appItem8
        )
    }

    companion object {
        private const val APP_ICON_PREF = "launcher_app_icon_"
    }
}
