package com.launcher.yfd_ui01


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

import com.launcher.yfd_ui01.databinding.FragmentMainBinding
import com.launcher.yfd_ui01.fragment.MainFragment
import com.launcher.yfd_ui01.fragment.MenuFragment
import com.launcher.yfd_ui01.manager.FragmentAnimation
import com.launcher.yfd_ui01.manager.FragmentStackManager
import kotlin.math.abs

class MainActivity_YFD_UI01 :  AppCompatActivity() {
    private val TAG = "MainFragmentActivity"
    lateinit var fragmentStackManager: FragmentStackManager
    private lateinit var mViewBinding: FragmentMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)
        fragmentStackManager =    FragmentStackManager(supportFragmentManager, R.id.fragment_container)
        if (savedInstanceState == null) {
            initializeFragments()
        } else {
            // 恢复状态
            fragmentStackManager.restoreState(savedInstanceState)
        }
        mViewBinding = FragmentMainBinding.inflate(layoutInflater)
        // Activity入场动画
        //overridePendingTransition(R.anim.slide_in_bottom, 0)

        //setupGestureDetector()
        // 打印Fragment堆栈状态（调试）
        fragmentStackManager.printFragmentStack()
    }


    private fun initializeFragments() {
        Log.d("FragmentStack", "初始化Fragment...")

        // 1. 设置默认Fragment（第一个，会显示）
        fragmentStackManager.setDefaultFragment(
            MainFragment.newInstance(),"MainFragment")

        // 2. 添加其他Fragment（默认隐藏）
        fragmentStackManager.addFragmentAndHide(
            MenuFragment.newInstance(), "MenuFragment")
    }

    /**
     * Fragment滑动回调
     */
    protected open fun onFragmentSwiped(direction: FragmentStackManager.SlideDirection) {
        Log.i(TAG,"lqq,onFragmentSwiped,direction="+direction)
        // 子类可以重写此方法
        //updateUIForCurrentFragment()
        val position = getCurrentFragmentPosition()
        val total = fragmentStackManager.getFragmentCount()
        Log.i(TAG,"lqq,position="+position+",total="+total)
        if(position==0 && direction  == FragmentStackManager.SlideDirection.UP )
            fragmentStackManager.slideToFragment(FragmentStackManager.SlideDirection.UP)


    }
    
    /**
     * 更新UI（如标题、指示器等）
     */
    private fun updateUIForCurrentFragment() {
        val position = getCurrentFragmentPosition()
        val total = fragmentStackManager.getFragmentCount()

        Log.i(TAG,"lqq,position="+position+",total="+total)

    }
    
    /**
     * 获取当前Fragment位置
     */
    fun getCurrentFragmentPosition(): Int {
        // 这里需要根据实际情况实现
        return when (fragmentStackManager.getCurrentFragment()) {
            is MainFragment -> 0
            is MenuFragment -> 1
//            is FragmentC -> 2
            else -> 0
        }
    }

    /**
     * 跳转到指定Fragment
     */
    fun goToFragment(position: Int, animation: FragmentAnimation = FragmentAnimation.SLIDE_FROM_LEFT) {
        val currentPosition = getCurrentFragmentPosition()
        Log.i(TAG,"currentPosition="+currentPosition+",position="+position)
        if (position == currentPosition) return
        
        val direction = if (position > currentPosition) {
            FragmentStackManager.SlideDirection.LEFT
        } else {
            FragmentStackManager.SlideDirection.RIGHT
        }
        
        // 计算需要滑动的次数
        val steps = abs(position - currentPosition)
        repeat(steps) {
            fragmentStackManager.slideToFragment(direction, animation)
        }
    }


    override fun onBackPressed() {
        Log.i(TAG,"lqq,onBackPressed")
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
            val position = getCurrentFragmentPosition() - 1
            // 更新当前Fragment位置
            val newPosition = maxOf(0, position)
            Log.i(TAG,"lqq,onBackPressed="+position+",newPosition="+newPosition)
            updateUIForCurrentFragment()
        } /*else {
            finish()
            overridePendingTransition(0, R.anim.slide_out_bottom)
        }*/
    }
}