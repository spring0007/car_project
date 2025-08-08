package com.example.plugin_2

import android.R
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.awell.control.AppsCustomizeControl
import com.awell.launcher2.IconCache
import com.awell.launcher2.Launcher
import com.awell.utils.Utils.startWallpaper
import com.example.plugin_2.databinding.UiActivityBinding
import kotlin.math.abs

class UI2Activity : Activity() {

    private val TAG = UI2Activity::class.simpleName
    private lateinit var mViewBinding: UiActivityBinding
    private var handler: Handler? = null
    private var startX = 0f
    private var startY = 0f
    private var viewConfiguration: ViewConfiguration? = null

    // 跟踪事件消费状态
    private var isEventConsumedByChild = false
    private var isLongPressPossible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = UiActivityBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)

        initView()

        handler = Handler(Looper.getMainLooper())
        viewConfiguration = ViewConfiguration.get(this)

    }

    private fun initView() {

        clickStartApp()

    }

    private fun clickStartApp() {

        mViewBinding.hotsetNavi.setOnClickListener {
            startActivity(
                "com.awell.navigation",
                "com.awell.navigation.MainActivity"
            )
        }

        mViewBinding.hotsetVideo.setOnClickListener {
            startActivity(
                "com.awell.localvideo",
                "com.awell.localvideo.activity.VideoListActivity"
            )
        }

        mViewBinding.hotsetSetting.setOnClickListener {
            startActivity(
                "com.awell.carsetting",
                "com.awell.carsetting.MainActivity"
            )
        }

        mViewBinding.hotsetAllapp.setOnClickListener {
            AppsCustomizeControl.showApps(this.findViewById<ViewGroup>(R.id.content))
        }

        mViewBinding.hotsetRadio.setOnClickListener {
            startActivity(
                "com.awell.radio",
                "com.awell.radio.AwellFmActivity"
            )
        }

        mViewBinding.hotsetBluetooth.setOnClickListener {
            startActivity(
                "com.awell.bluetooth",
                "com.awell.bluetooth.MainActivity"
            )
        }
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
                Log.d(Launcher.TAG, "packagename11=$packName")
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
            longPressRunnable!!,
            ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    private fun cancelLongPressDetection() {
        if (longPressRunnable != null) {
            handler?.removeCallbacks(longPressRunnable!!)
            longPressRunnable = null
        }
    }

    private fun handleLongPressAction() {
        Log.i(TAG, "handleLongPressAction: huang start wall paper=>")
        startWallpaper()
    }

}