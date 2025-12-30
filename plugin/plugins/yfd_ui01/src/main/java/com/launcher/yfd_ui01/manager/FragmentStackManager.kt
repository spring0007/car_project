package com.launcher.yfd_ui01.manager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.launcher.yfd_ui01.R

class FragmentStackManager(
    private val fragmentManager: FragmentManager,
    private val containerId: Int
) {
    
    companion object {
        private const val KEY_FRAGMENT_STACK = "fragment_stack_manager_state"
        private const val KEY_CURRENT_POSITION = "current_position"
        private const val KEY_FRAGMENT_TAGS = "fragment_tags"
        private const val KEY_DEFAULT_FRAGMENT_TAG = "default_fragment"
        private const val KEY_FRAGMENT_VISIBILITY = "fragment_visibility"
    }
    
    // Fragment 堆栈
    private val fragmentStack = mutableListOf<Fragment>()
    
    // Fragment 标签映射
    private val fragmentTags = mutableListOf<String>()
    
    // Fragment 可见性状态（true=显示，false=隐藏）
    private val fragmentVisibility = mutableListOf<Boolean>()
    
    // 当前显示的位置
    private var currentPosition = 0
    
    // 默认 Fragment 标签
    private var defaultFragmentTag: String? = null
    
    // 是否已设置默认 Fragment
    private var isDefaultFragmentSet = false
    
    // 初始化状态锁
    private var isInitializing = false
    
    /**
     * 设置默认 Fragment（必须第一个调用）
     */
    fun setDefaultFragment(fragment: Fragment, tag: String? = null): FragmentStackManager {
        if (isDefaultFragmentSet) {
            throw IllegalStateException("Default fragment has already been set")
        }
        
        isInitializing = true
        
        val fragmentTag = tag ?: generateFragmentTag(fragment)
        defaultFragmentTag = fragmentTag
        
        // 清空堆栈（如果有的话）
        clearAllFragments()
        
        // 添加默认 Fragment 并显示
        val transaction = fragmentManager.beginTransaction()
        
        // 添加 Fragment
        transaction.add(containerId, fragment, fragmentTag)
        
        // 更新内部状态
        fragmentStack.add(fragment)
        fragmentTags.add(fragmentTag)
        fragmentVisibility.add(true) // 默认Fragment显示
        
        currentPosition = 0
        
        isDefaultFragmentSet = true
        
        transaction.commit()
        
        // 等待事务完成
        fragmentManager.executePendingTransactions()
        
        isInitializing = false
        
        return this
    }
    
    /**
     * 添加 Fragment 并隐藏（不会立即显示）
     */
    fun addFragmentAndHide(
        fragment: Fragment,
        tag: String? = null
    ): String {
        if (!isDefaultFragmentSet) {
            throw IllegalStateException("Please call setDefaultFragment() first")
        }
        
        val fragmentTag = tag ?: generateFragmentTag(fragment)
        
        // 如果Fragment已存在，不重复添加
        if (fragmentTags.contains(fragmentTag)) {
            return fragmentTag
        }
        
        val transaction = fragmentManager.beginTransaction()
        
        // 添加Fragment但隐藏它
        transaction.add(containerId, fragment, fragmentTag)
        transaction.hide(fragment) // 关键：立即隐藏
        
        // 更新内部状态
        fragmentStack.add(fragment)
        fragmentTags.add(fragmentTag)
        fragmentVisibility.add(false) // 新添加的Fragment隐藏
        
        transaction.commit()
        
        return fragmentTag
    }
    
    /**
     * 添加Fragment并立即显示（隐藏当前Fragment）
     */
    fun addFragmentAndShow(
        fragment: Fragment,
        tag: String? = null,
        animation: FragmentAnimation? = null
    ): String {
        if (!isDefaultFragmentSet) {
            throw IllegalStateException("Please call setDefaultFragment() first")
        }
        
        val fragmentTag = tag ?: generateFragmentTag(fragment)
        
        val transaction = fragmentManager.beginTransaction()
        
        // 设置动画
        animation?.applyTo(transaction)
        
        // 隐藏当前Fragment
        if (currentPosition >= 0) {
            val currentFragment = fragmentStack[currentPosition]
            transaction.hide(currentFragment)
            fragmentVisibility[currentPosition] = false
        }
        
        // 检查Fragment是否已存在
        val existingIndex = fragmentTags.indexOf(fragmentTag)
        if (existingIndex >= 0) {
            // Fragment已存在，显示它
            transaction.show(fragmentStack[existingIndex])
            fragmentVisibility[existingIndex] = true
            currentPosition = existingIndex
        } else {
            // 添加新Fragment
            transaction.add(containerId, fragment, fragmentTag)
            
            // 更新内部状态
            fragmentStack.add(fragment)
            fragmentTags.add(fragmentTag)
            fragmentVisibility.add(true)
            currentPosition = fragmentStack.size - 1
        }
        
        transaction.addToBackStack("show_$fragmentTag")
        transaction.commit()
        
        return fragmentTag
    }
    
    /**
     * 批量初始化Fragment（添加并隐藏）
     */
    fun initializeFragments(vararg fragments: Pair<Fragment, String>) {
        if (!isDefaultFragmentSet) {
            throw IllegalStateException("Please call setDefaultFragment() first")
        }
        
        val transaction = fragmentManager.beginTransaction()
        
        fragments.forEach { (fragment, tag) ->
            // 如果Fragment已存在，跳过
            if (fragmentTags.contains(tag)) {
                return@forEach
            }
            
            // 添加Fragment并立即隐藏
            transaction.add(containerId, fragment, tag)
            transaction.hide(fragment)
            
            // 更新内部状态
            fragmentStack.add(fragment)
            fragmentTags.add(tag)
            fragmentVisibility.add(false)
        }
        
        transaction.commit()
    }
    
    /**
     * 显示指定Fragment（隐藏当前Fragment）
     */
    fun showFragment(
        tag: String,
        animation: FragmentAnimation? = null
    ): Boolean {
        val position = fragmentTags.indexOf(tag)
        if (position < 0 || position == currentPosition) {
            return false
        }
        
        val transaction = fragmentManager.beginTransaction()
        
        // 设置动画
        animation?.applyTo(transaction)
        
        // 隐藏当前Fragment
        if (currentPosition >= 0) {
            val currentFragment = fragmentStack[currentPosition]
            transaction.hide(currentFragment)
            fragmentVisibility[currentPosition] = false
        }
        
        // 显示目标Fragment
        val targetFragment = fragmentStack[position]
        transaction.show(targetFragment)
        fragmentVisibility[position] = true
        
        currentPosition = position
        
        transaction.addToBackStack("show_$tag")
        transaction.commit()
        
        return true
    }
    
    /**
     * 显示指定位置的Fragment
     */
    fun showFragmentAt(
        position: Int,
        animation: FragmentAnimation? = null
    ): Boolean {
        if (position < 0 || position >= fragmentStack.size || position == currentPosition) {
            return false
        }
        
        return showFragment(fragmentTags[position], animation)
    }
    
    /**
     * 显示默认Fragment
     */
    fun showDefaultFragment(animation: FragmentAnimation? = null): Boolean {
        defaultFragmentTag?.let { tag ->
            return showFragment(tag, animation)
        }
        return false
    }
    
    /**
     * 隐藏指定Fragment
     */
    fun hideFragment(tag: String): Boolean {
        val position = fragmentTags.indexOf(tag)
        if (position < 0 || !fragmentVisibility[position]) {
            return false
        }
        
        val transaction = fragmentManager.beginTransaction()
        
        val fragment = fragmentStack[position]
        transaction.hide(fragment)
        fragmentVisibility[position] = false
        
        // 如果隐藏的是当前显示的Fragment，显示默认Fragment
        if (position == currentPosition) {
            showDefaultFragment()
        }
        
        transaction.commit()
        return true
    }
    
    /**
     * 切换Fragment显示/隐藏状态
     */
    fun toggleFragment(tag: String, animation: FragmentAnimation? = null): Boolean {
        val position = fragmentTags.indexOf(tag)
        if (position < 0) {
            return false
        }
        
        return if (fragmentVisibility[position]) {
            hideFragment(tag)
        } else {
            showFragment(tag, animation)
        }
    }
    
    /**
     * 显示上一个Fragment（向左滑动）
     */
    fun showPrevious(animation: FragmentAnimation = FragmentAnimation.SLIDE_FROM_LEFT): Boolean {
        if (currentPosition > 0) {
            return showFragmentAt(currentPosition - 1, animation)
        }
        return false
    }
    
    /**
     * 显示下一个 Fragment（向右滑动）
     */
    fun showNext(animation: FragmentAnimation = FragmentAnimation.SLIDE_FROM_RIGHT): Boolean {
        if (currentPosition < fragmentStack.size - 1) {
            return showFragmentAt(currentPosition + 1, animation)
        }
        return false
    }

    /**
     * 滑动切换Fragment
     */
    fun slideToFragment(direction: SlideDirection, animation: FragmentAnimation? = null) {
        when (direction) {
            SlideDirection.LEFT -> showPrevious(animation ?: FragmentAnimation.SLIDE_FROM_RIGHT)
            SlideDirection.RIGHT -> showNext(animation ?: FragmentAnimation.SLIDE_FROM_LEFT)
            SlideDirection.UP -> {
                showNext(animation ?: FragmentAnimation.SLIDE_FROM_TOP)
            }
            SlideDirection.DOWN -> {
                showPrevious(animation ?: FragmentAnimation.SLIDE_FROM_BOTTOM)
            }
        }
    }

    /**
     * 返回到默认 Fragment
     */
    fun backToDefault(animation: FragmentAnimation = FragmentAnimation.SLIDE_FROM_RIGHT): Boolean {
        return showDefaultFragment(animation)
    }
    
    /**
     * 跳转到指定 Fragment
     */
    fun goToFragment(position: Int, animation: FragmentAnimation = FragmentAnimation.SLIDE_FROM_RIGHT): Boolean {
        return showFragmentAt(position, animation)
    }
    
    /**
     * 通过标签跳转到Fragment
     */
    fun goToFragmentByTag(tag: String, animation: FragmentAnimation = FragmentAnimation.SLIDE_FROM_RIGHT): Boolean {
        return showFragment(tag, animation)
    }
    
    /**
     * 替换当前 Fragment
     */
    fun replaceCurrentFragment(
        fragment: Fragment,
        tag: String? = null,
        animation: FragmentAnimation? = null
    ): String {
        if (currentPosition < 0) {
            throw IllegalStateException("No current fragment to replace")
        }
        
        val fragmentTag = tag ?: generateFragmentTag(fragment)
        val transaction = fragmentManager.beginTransaction()
        
        // 设置动画
        animation?.applyTo(transaction)
        
        // 移除当前 Fragment
        val currentFragment = fragmentStack[currentPosition]
        transaction.remove(currentFragment)
        
        // 添加新 Fragment
        transaction.add(containerId, fragment, fragmentTag)
        
        // 更新堆栈
        fragmentStack[currentPosition] = fragment
        fragmentTags[currentPosition] = fragmentTag
        fragmentVisibility[currentPosition] = true // 替换后显示
        
        // 如果是默认Fragment被替换，更新默认标签
        if (defaultFragmentTag == fragmentTags[currentPosition]) {
            defaultFragmentTag = fragmentTag
        }
        
        transaction.addToBackStack("replace_$fragmentTag")
        transaction.commit()
        
        return fragmentTag
    }
    
    /**
     * 移除指定 Fragment
     */
    fun removeFragment(tag: String): Boolean {
        val position = fragmentTags.indexOf(tag)
        if (position < 0) return false
        
        // 不能移除默认 Fragment
        if (tag == defaultFragmentTag) {
            throw IllegalStateException("Cannot remove default fragment")
        }
        
        val fragment = fragmentStack[position]
        val transaction = fragmentManager.beginTransaction()
        transaction.remove(fragment)
        transaction.commit()
        
        // 从堆栈中移除
        fragmentStack.removeAt(position)
        fragmentTags.removeAt(position)
        
        // 调整当前位置
        if (position <= currentPosition) {
            currentPosition--
        }
        
        return true
    }
    
    /**
     * 获取当前 Fragment
     */
    fun getCurrentFragment(): Fragment? {
        return if (currentPosition >= 0) fragmentStack[currentPosition] else null
    }
    
    /**
     * 获取默认 Fragment
     */
    fun getDefaultFragment(): Fragment? {
        return defaultFragmentTag?.let { tag ->
            val position = fragmentTags.indexOf(tag)
            if (position >= 0) fragmentStack[position] else null
        }
    }
    
    /**
     * 获取 Fragment 数量
     */
    fun getFragmentCount(): Int {
        return fragmentStack.size
    }
    
    /**
     * 获取当前 Fragment 位置
     */
    fun getCurrentPosition(): Int {
        return currentPosition
    }
    
    /**
     * 获取默认 Fragment 位置
     */
    fun getDefaultPosition(): Int {
        return defaultFragmentTag?.let { fragmentTags.indexOf(it) } ?: -1
    }
    
    /**
     * 获取 Fragment 标签
     */
    fun getFragmentTag(position: Int): String? {
        return if (position >= 0 && position < fragmentTags.size) {
            fragmentTags[position]
        } else {
            null
        }
    }
    
    /**
     * 获取Fragment可见性
     */
    fun isFragmentVisible(tag: String): Boolean {
        val position = fragmentTags.indexOf(tag)
        return if (position >= 0) fragmentVisibility[position] else false
    }
    
    /**
     * 获取Fragment在指定位置是否可见
     */
    fun isFragmentVisibleAt(position: Int): Boolean {
        return if (position >= 0 && position < fragmentVisibility.size) {
            fragmentVisibility[position]
        } else {
            false
        }
    }
    /*
     * 清空所有 Fragment（保留默认 Fragment）
     */
    fun clearAllFragments() {
        if (defaultFragmentTag == null) {
            // 没有默认Fragment，清空所有
            clearAll()
            return
        }
        
        // 保留默认 Fragment
        val defaultIndex = fragmentTags.indexOf(defaultFragmentTag!!)
        if (defaultIndex >= 0) {
            val defaultFragment = fragmentStack[defaultIndex]
            val defaultTag = fragmentTags[defaultIndex]
            
            // 移除除了默认Fragment之外的所有Fragment
            val transaction = fragmentManager.beginTransaction()
            
            fragmentStack.forEachIndexed { index, fragment ->
                if (index != defaultIndex) {
                    transaction.remove(fragment)
                }
            }
            
            transaction.commit()
            
            // 更新内部状态
            fragmentStack.clear()
            fragmentTags.clear()
            fragmentVisibility.clear()
            
            fragmentStack.add(defaultFragment)
            fragmentTags.add(defaultTag)
            fragmentVisibility.add(true)
            currentPosition = 0
        } else {
            clearAll()
        }
    }
    
    private fun clearAll() {
        val transaction = fragmentManager.beginTransaction()
        
        fragmentStack.forEach { fragment ->
            transaction.remove(fragment)
        }
        
        transaction.commit()
        
        fragmentStack.clear()
        fragmentTags.clear()
        fragmentVisibility.clear()
        currentPosition = -1
    }
    
    /**
     * 保存状态
     */
    fun saveState(outState: Bundle) {
        // 保存默认 Fragment 标签
        defaultFragmentTag?.let {
            outState.putString(KEY_DEFAULT_FRAGMENT_TAG, it)
        }
        
        // 保存当前位置
        outState.putInt(KEY_CURRENT_POSITION, currentPosition)
        
        // 保存Fragment标签列表
        outState.putStringArrayList(KEY_FRAGMENT_TAGS, ArrayList(fragmentTags))
        
        // 保存Fragment可见性状态
        val visibilityArray = BooleanArray(fragmentVisibility.size)
        fragmentVisibility.forEachIndexed { index, visible ->
            visibilityArray[index] = visible
        }
        outState.putBooleanArray(KEY_FRAGMENT_VISIBILITY, visibilityArray)
    }
    
    /**
     * 恢复状态
     */
    fun restoreState(savedInstanceState: Bundle) {
        // 恢复默认 Fragment 标签
        defaultFragmentTag = savedInstanceState.getString(KEY_DEFAULT_FRAGMENT_TAG)
        
        // 恢复当前位置
        currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, -1)
        
        // 恢复Fragment标签列表
        val savedTags = savedInstanceState.getStringArrayList(KEY_FRAGMENT_TAGS)
        savedTags?.let { tags ->
            fragmentTags.clear()
            fragmentTags.addAll(tags)
            
            // 重新获取 Fragment 实例
            fragmentStack.clear()
            tags.forEach { tag ->
                val fragment = fragmentManager.findFragmentByTag(tag)
                fragment?.let {
                    fragmentStack.add(it)
                }
            }
            
            // 恢复可见性状态
            val visibilityArray = savedInstanceState.getBooleanArray(KEY_FRAGMENT_VISIBILITY)
            fragmentVisibility.clear()
            if (visibilityArray != null && visibilityArray.size == fragmentStack.size) {
                visibilityArray.forEach { visible ->
                    fragmentVisibility.add(visible)
                }
            } else {
                // 默认状态：第一个显示，其他隐藏
                fragmentStack.forEachIndexed { index, _ ->
                    fragmentVisibility.add(index == 0)
                }
            }
            
            // 标记默认 Fragment 已设置
            if (defaultFragmentTag != null) {
                isDefaultFragmentSet = true
            }
        }
    }
    
    /**
     * 处理返回键
     */
    fun onBackPressed(): Boolean {
        if (currentPosition > 0) {
            // 返回到上一个Fragment
            return showFragmentAt(currentPosition - 1)
        }
        return false
    }
    
    /**
     * 打印Fragment堆栈状态（调试用）
     */
    fun printFragmentStack() {
        println("=== Fragment Stack Status ===")
        println("Default Fragment: $defaultFragmentTag")
        println("Current Position: $currentPosition")
        println("Total Fragments: ${fragmentStack.size}")
        println("Visible Fragments: ${fragmentVisibility.count { it }}")
        
        fragmentStack.forEachIndexed { index, fragment ->
            val tag = fragmentTags[index]
            val visible = fragmentVisibility[index]
            val isDefault = tag == defaultFragmentTag
            val isCurrent = index == currentPosition
            
            println("[$index] $tag - ${fragment::class.java.simpleName}")
            println("   Visible: $visible, Default: $isDefault, Current: $isCurrent")
        }
        println("============================")
    }
    
    /**
     * 生成 Fragment 标签
     */
    private fun generateFragmentTag(fragment: Fragment): String {
        return "${fragment::class.java.simpleName}_${System.currentTimeMillis()}"
    }
    enum class SlideDirection {
        LEFT, RIGHT, UP, DOWN
    }
}

/**
 * Fragment动画配置
 */
sealed class FragmentAnimation(val enterAnim: Int, val exitAnim: Int, val popEnterAnim: Int, val popExitAnim: Int) {
    object SLIDE_FROM_RIGHT : FragmentAnimation(
        R.anim.slide_in_right,
        R.anim.slide_out_left,
        R.anim.slide_in_left,
        R.anim.slide_out_right
    )
    
    object SLIDE_FROM_LEFT : FragmentAnimation(
        R.anim.slide_in_left,
        R.anim.slide_out_right,
        R.anim.slide_in_right,
        R.anim.slide_out_left
    )
    
    object SLIDE_FROM_BOTTOM : FragmentAnimation(
        R.anim.slide_in_bottom,
        R.anim.slide_out_top,
        R.anim.slide_in_top,
        R.anim.slide_out_bottom
    )
    
    object SLIDE_FROM_TOP : FragmentAnimation(
        R.anim.slide_in_top,
        R.anim.slide_out_bottom,
        R.anim.slide_in_bottom,
        R.anim.slide_out_top
    )
    
    object FADE : FragmentAnimation(
        R.anim.fade_in,
        R.anim.fade_out,
        R.anim.fade_in,
        R.anim.fade_out
    )
    
    fun applyTo(transaction: FragmentTransaction) {
        transaction.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
    }
}