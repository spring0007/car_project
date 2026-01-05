package com.launcher.yfd_ui01

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.launcher.yfd_ui01.databinding.FragmentMainBinding
import com.launcher.yfd_ui01.fragment.MainFragment
import com.launcher.yfd_ui01.fragment.MenuFragment
import com.launcher.yfd_ui01.manager.BackHandlerHelper
import com.launcher.yfd_ui01.manager.FragmentAnimation
import com.launcher.yfd_ui01.manager.FragmentStackManager
import com.launcher.yfd_ui01.utils.LogUtil
import kotlin.math.abs

class MainActivity_YFD_UI01 :  FragmentActivity() {
    private val TAG = "MainFragmentActivity"
    private lateinit var fragmentStackManager: FragmentStackManager
    private lateinit var mViewBinding: FragmentMainBinding
    
    // 标记是否正在重建
    private var isRecreating = false
    // 标记是否从MenuFragment失去焦点
    private var lostFocusFromMenu = false
    // 标记是否从其他应用返回
    private var returningFromOtherApp = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isRecreating = true
            LogUtil.d( "Activity正在重建")
        }
        
        setContentView(R.layout.activity_fragment)
        
        // 初始化ViewBinding
        mViewBinding = FragmentMainBinding.inflate(layoutInflater)
        
        // 初始化FragmentStackManager
        fragmentStackManager = FragmentStackManager(supportFragmentManager, R.id.fragment_container)

        if (savedInstanceState != null) {
            // 仅恢复状态标识与 fragment tag，若 fragment 未被系统恢复则初始化它们
            fragmentStackManager.restoreState(savedInstanceState)
            lostFocusFromMenu = savedInstanceState.getBoolean("lostFocusFromMenu", false)
            returningFromOtherApp = savedInstanceState.getBoolean("returningFromOtherApp", false)

            // 如果系统没有恢复 fragment（比如第一次创建或 fragment 被移除），则初始化
            val mainFrag = supportFragmentManager.findFragmentByTag(FragmentStackManager.TAG_MAIN)
            val menuFrag = supportFragmentManager.findFragmentByTag(FragmentStackManager.TAG_MENU)
            if (mainFrag == null || menuFrag == null) {
                initializeFragments()
            } else {
                // 保证内部状态一致
                LogUtil.d("Fragments 已由系统恢复")
            }
        } else {
            // 首次创建，初始化 Fragment
            initializeFragments()
        }
	// 监听用户交互
        setupUserInteractionListener()
        
        // 观察是否需要导航到主Fragment
        /*fragmentStackManager.shouldNavigateToMain.observe(this) { shouldNavigate ->
            if (shouldNavigate != null && shouldNavigate) {
                fragmentStackManager.checkAndReturnToMain()
            }
        }*/
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        fragmentStackManager.saveState(outState)
        outState.putBoolean("lostFocusFromMenu", lostFocusFromMenu)
        outState.putBoolean("returningFromOtherApp", returningFromOtherApp)
        LogUtil.d( "保存Activity状态")
    }

    private fun initializeFragments() {
        LogUtil.d( "初始化Fragment...")
        
        // 使用新的初始化方法
        fragmentStackManager.initializeFragments(
            MainFragment.newInstance(),
            MenuFragment.newInstance()
        )
    }

    /**
     * 获取当前Fragment位置
     */
    fun getCurrentFragmentPosition(): Int {
        return fragmentStackManager.getCurrentFragmentPosition()
    }

    /**
     * 跳转到指定Fragment
     */
    fun goToFragment(position: Int, animation: FragmentAnimation = FragmentAnimation.FADE) {
        val currentPosition = getCurrentFragmentPosition()
        LogUtil.i("currentPosition=$currentPosition,position=$position")
        
        if (position == currentPosition) return
        
        val tag = when (position) {
            0 -> FragmentStackManager.TAG_MAIN
            1 -> FragmentStackManager.TAG_MENU
            else -> FragmentStackManager.TAG_MAIN
        }
        
        fragmentStackManager.switchToFragment(tag, animation)
    }

    override fun onBackPressed() {
        LogUtil.i( "onBackPressed")
       /* if (!BackHandlerHelper.handleBackPress(this)) {
            super.onBackPressed();
        }*/
        val currentPosition = getCurrentFragmentPosition()
        
        if (currentPosition == 1) {
            // 在MenuFragment，返回MainFragment
            goToFragment(0, FragmentAnimation.FADE)
        } else {
            // 在主Fragment，检查返回栈
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                // 最小化到后台
                moveTaskToBack(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LogUtil.d( "onResume: Activity回到前台，isRecreating=$isRecreating, lostFocusFromMenu=$lostFocusFromMenu")
        
        // 处理从其他应用返回的情况
        handleReturnFromOtherApp()
        
        // 如果是重建后的第一次onResume，重置标志
        if (isRecreating) {
            isRecreating = false
        }
    }

    override fun onPause() {
        super.onPause()
        LogUtil.d( "onPause: Activity进入后台")
        
        // 检查当前是否在MenuFragment
        if (getCurrentFragmentPosition() == 1) {
            lostFocusFromMenu = true
            LogUtil.d( "从MenuFragment进入后台，标记需要返回MainFragment")
        }
    }

    override fun onStop() {
        super.onStop()
        LogUtil.d( "onStop: Activity进入后台")
    }
    /**
     * 处理从其他应用返回的情况
     */
    private fun handleReturnFromOtherApp() {
        if (returningFromOtherApp) {
            LogUtil.d("从其他应用返回")

            // 立即处理：如果之前是从 MenuFragment 失去焦点并且当前仍为 MenuFragment，直接无动画切换到 MainFragment
            if (lostFocusFromMenu && getCurrentFragmentPosition() == 1) {
                LogUtil.d( "从MenuFragment跳转到其他应用后返回，立即切换到MainFragment（无动画）")
                // 使用无动画切换，避免 UI 闪烁
                goToFragment(0, FragmentAnimation.NONE)
            }

            // 重置标志
            lostFocusFromMenu = false
            returningFromOtherApp = false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        LogUtil.d( "onWindowFocusChanged, hasFocus=$hasFocus")
        
        if (!hasFocus) {
            // Activity失去焦点，可能是跳转到其他应用
            LogUtil.d( "Activity失去焦点，可能跳转到其他应用")
            returningFromOtherApp = true
        } else {
            // Activity获得焦点
            LogUtil.d( "Activity获得焦点")
        }
    }
    
    /**
     * 设置用户交互监听
     */
    private fun setupUserInteractionListener() {
        // 监听ContentView的触摸事件
        val contentView = findViewById<ViewGroup>(android.R.id.content)
        contentView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 用户触摸屏幕，重置标记
                    lostFocusFromMenu = false
                    returningFromOtherApp = false
                }
            }
            false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LogUtil.d( "onDestroy: Activity销毁")
    }
}