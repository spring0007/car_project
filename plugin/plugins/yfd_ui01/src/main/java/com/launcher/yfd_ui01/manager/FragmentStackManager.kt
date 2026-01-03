package com.launcher.yfd_ui01.manager

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.launcher.yfd_ui01.R

class FragmentStackManager(
    private val fragmentManager: FragmentManager,
    private val containerId: Int
) {
    companion object {
        const val KEY_CURRENT_TAG = "current_fragment_tag"
        const val KEY_FRAGMENTS_ADDED = "fragments_added"
        const val TAG_MAIN = "MainFragment"
        const val TAG_MENU = "MenuFragment"
    }
    
    private var currentFragmentTag = TAG_MAIN
    private var fragmentsAdded = false

    /**
     * 初始化Fragment堆栈
     */
    fun initializeFragments(mainFragment: Fragment, menuFragment: Fragment) {
        if (fragmentsAdded) return

        // 先清空已有的Fragment
        clearAllFragments()
        
        // 使用show/hide方式：先添加两个Fragment，然后显示一个隐藏一个
        val transaction = fragmentManager.beginTransaction()
        
        // 添加主Fragment
        transaction.add(containerId, mainFragment, TAG_MAIN)
        
        // 添加菜单Fragment
        transaction.add(containerId, menuFragment, TAG_MENU)
        
        // 隐藏菜单Fragment
        transaction.hide(menuFragment)
        
        transaction.commitNow()
        
        fragmentsAdded = true
        currentFragmentTag = TAG_MAIN
    }
    
    /**
     * 切换到指定Fragment（使用show/hide）
     */
    fun switchToFragment(tag: String, animation: FragmentAnimation = FragmentAnimation.FADE) {
        if (tag == currentFragmentTag) return
        
        val targetFragment = fragmentManager.findFragmentByTag(tag)
        val currentFragment = fragmentManager.findFragmentByTag(currentFragmentTag)
        
        if (targetFragment != null && currentFragment != null) {
            val transaction = fragmentManager.beginTransaction()
            
            // 设置动画
            when (animation) {
                FragmentAnimation.FADE -> {
                    transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                }
                FragmentAnimation.SLIDE_FROM_LEFT -> {
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                FragmentAnimation.SLIDE_FROM_RIGHT -> {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                }
                FragmentAnimation.NONE -> {
                    // 无动画
                }
            }
            
            // 隐藏当前Fragment，显示目标Fragment
            transaction.hide(currentFragment)
            transaction.show(targetFragment)

            // 添加到返回栈
            transaction.addToBackStack("switch_${currentFragmentTag}_to_$tag")
            
            // 安全提交事务
            transaction.commitAllowingStateLoss()
            
            currentFragmentTag = tag
            Log.d("FragmentStackManager", "切换到Fragment: $tag")
        }
    }
    /**
     * 更新当前Fragment标签
     */
    private fun updateCurrentFragmentTag() {
        // 遍历Fragment，找到当前显示的Fragment
        val fragments = fragmentManager.fragments
        for (fragment in fragments) {
            if (fragment.isVisible) {
                val tag = fragment.tag
                if (tag == TAG_MAIN || tag == TAG_MENU) {
                    currentFragmentTag = tag
                    break
                }
            }
        }
    }

    /**
     * 获取当前显示的Fragment
     */
    fun getCurrentFragment(): Fragment? {
        return fragmentManager.findFragmentByTag(currentFragmentTag)
    }
    
    /**
     * 获取当前Fragment位置
     */
    fun getCurrentFragmentPosition(): Int {
        return when (currentFragmentTag) {
            TAG_MAIN -> 0
            TAG_MENU -> 1
            else -> 0
        }
    }

    /**
     * 获取Fragment总数
     */
    val fragmentCount: Int = 2 // MainFragment和MenuFragment
    

    
    /**
     * 保存状态
     */
    fun saveState(outState: Bundle): Boolean {
        outState.putString(KEY_CURRENT_TAG, currentFragmentTag)
        return true
    }
    
    /**
     * 恢复状态
     */
    fun restoreState(savedInstanceState: Bundle): Boolean {
        currentFragmentTag = savedInstanceState.getString(KEY_CURRENT_TAG, TAG_MAIN)
        return true
    }

    /**
     * 清空所有Fragment
     */
    private fun clearAllFragments() {
        val transaction = fragmentManager.beginTransaction()
        fragmentManager.fragments.forEach {
            transaction.remove(it)
        }
        
        try {
            transaction.commitNow()
        } catch (e: IllegalStateException) {
            transaction.commitAllowingStateLoss()
        }
        
        // 清空返回栈
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
    
    /**
     * 滑动方向枚举
     */
    enum class SlideDirection {
        UP, DOWN, LEFT, RIGHT
    }
}

/**
 * Fragment动画类型
 */
enum class FragmentAnimation {
    NONE, FADE, SLIDE_FROM_LEFT, SLIDE_FROM_RIGHT
}