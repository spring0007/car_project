package com.launcher.yfd_ui01

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import com.awell.launcher2.IconCache
import com.awell.utils.CommonData
import com.awell.utils.Utils.startWallpaper
import com.launcher.yfd_ui01.databinding.ActivityMainUi01Binding
import com.launcher.yfd_ui01.utils.SystemUIClient
import com.launcher.yfd_ui01.utils.SystemUIClient.HIDE_FREEFORM
import com.launcher.yfd_ui01.utils.SystemUIClient.OPEN_APP_TO_FREEFORM
import com.launcher.yfd_ui01.utils.SystemUIClient.WINDOWING_MODE_FULLSCREEN
import java.lang.Math.abs

class MainActivity_YFD_UI01 : Activity() {
    private val TAG = MainActivity_YFD_UI01::class.simpleName
    private  lateinit var mViewBinding: ActivityMainUi01Binding

    private lateinit var locationManager: LocationManager

    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null
    private var isGlobalLayoutListenerAdded = false
    private var lastVisibleState: Boolean? = null
    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false
    lateinit var systemUIClient: SystemUIClient
    var viewAddNeedToStartFreeform: Boolean = false
    var oldFreeformPkg: String? = null
    private var accRecor = false
    private val freeformImpl: SystemUIClient.UIClientCallback =
        SystemUIClient.UIClientCallback {
            if (systemUIClient.rect == null) {
                //服务绑定比视图初始化快，rect未设置，启动Launcher，切换Launcher等
                viewAddNeedToStartFreeform = true
            } else {
                systemUIClient.startOrSetFreeformType(this, OPEN_APP_TO_FREEFORM)
              //  oldFreeformPkg = systemUIClient.getFreeformPkg(this)
            }
        }
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
	    mViewBinding = ActivityMainUi01Binding.inflate(layoutInflater)
        Settings.System.putString(mViewBinding.root.context.contentResolver, "ui_has_freeform", "true")
        setContentView(mViewBinding.root)
        initView()
        initTouchAndSpeedListener()
        initBroadcastReceiver()
        initFreeform()
    }
    override fun onResume() {
        super.onResume()
        findViewById<ImageView>(R.id.freeform_bg).post {
            if (findViewById<ImageView>(R.id.freeform_bg).isVisibleOnScreen()) {
                updateImagePosition(findViewById(R.id.freeform_bg))
            }
        }

    }
    private fun initFreeform() {

        setupPositionListener()

        systemUIClient = SystemUIClient(this)
        systemUIClient.bindToSystemUIService(this)
        systemUIClient.setCallback(freeformImpl)

    }

    private fun setupPositionListener() {
        val freeformImage = findViewById<ImageView>(R.id.freeform_bg)

        val layoutListener = View.OnLayoutChangeListener { _, left, top, right, bottom,
                                                           oldLeft, oldTop, oldRight, oldBottom ->

            // 检查位置是否变化或者视图是否可见
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                if (freeformImage.isVisibleOnScreen()) {
                    updateImagePosition(freeformImage)
                }
            }
        }

        freeformImage.addOnLayoutChangeListener(layoutListener)
    }

    private fun updateImagePosition(imageView: ImageView) {
        val location = IntArray(2)
        imageView.getLocationOnScreen(location)
        val screenX = location[0]
        val screenY = location[1]
        val width = imageView.width
        val height = imageView.height

        systemUIClient.rect = Rect(screenX, screenY, screenX + width, screenY + height)
        Log.i(TAG, "updateImagePosition: huang Starting freeform... rect=${systemUIClient.rect}")
        systemUIClient.startOrSetFreeformType(
            mViewBinding.root.context,
            OPEN_APP_TO_FREEFORM
        )
       // oldFreeformPkg = systemUIClient.getFreeformPkg(baseContext)
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

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
       // locationManager.requestLocationUpdates("gps", 1000, 10f, locationListener, mHandle.looper)
    }

    private fun initView() {
        clickStartApp()
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
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_DATE_CHANGED)
        
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
        Settings.System.putString(mViewBinding.root.context.contentResolver, "ui_has_freeform", "false")
    }
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.i(TAG, "onReceive:huang action=$action")
            when (action) {
                CommonData.BROADCAST_LAMP_SWITCH -> {}
                CommonData.ACTION_ACC_ON -> {}
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
                    /*Log.i(
                        TAG,
                        "onReceive: huang mediaControl.getCurrentPkgName()=>${mediaControl.getCurrentPkgName()}"
                    )*/
                   /* if (mediaControl.getCurrentPkgName()?.equals("com.zjinnova.zlink") == true) {
                        if ("REFRESH_JEPG" == zlinkStatus) {
                            updateCarplayImageAlbum()
                        }
                    }*/
                }

                "android.launcher.show.allApp" -> {
                   // AppsCustomizeControl.showApps(findViewById<ViewGroup>(android.R.id.content))
                }

                "CANBUS_CHANGE_SPEED_Unit" -> {}
                "top_session_package_change" -> {
                    val sessionTopPkg = intent.getStringExtra("top_package")
                  //  handleMediaPlaybackResult(sessionTopPkg!!, "start", 3, 4)
                }

                /*Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_TICK -> {
                    dialWidget?.updateTimeSysem()
                }*/
            }
        }
    }

    private fun clickStartApp() {
        mViewBinding.hotsetAllApp.setOnClickListener {
            systemUIClient.startOrSetFreeformType(this, HIDE_FREEFORM)
            systemUIClient.startOrSetFreeformType(this, WINDOWING_MODE_FULLSCREEN)
           // AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(android.R.id.content))
        }

        mViewBinding.hotsetDspApp.setOnClickListener {
            startActivity("com.awell.eqselect", "com.awell.eqselect.MainActivity")
        }
	
	    mViewBinding.hotsetBtApp.setOnClickListener {
            startActivity(
                "com.awell.bluetooth", "com.awell.bluetooth.MainActivity"
            )
        }

        mViewBinding.hotsetWindowApp.setOnClickListener {
            systemUIClient.startOrSetFreeformType(this, WINDOWING_MODE_FULLSCREEN)
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        //AppsCustomizeControl.hideApps()
        systemUIClient.startOrSetFreeformType(this, OPEN_APP_TO_FREEFORM)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //AppsCustomizeControl.hideApps()
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
}