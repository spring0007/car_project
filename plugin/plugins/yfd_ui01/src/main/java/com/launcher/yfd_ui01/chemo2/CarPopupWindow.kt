package com.launcher.yfd_ui01.chemo2

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.widget.ProgressBar
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.pop.AppPopupWindow.OnPopupUpdateListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarPopupWindow(
    private val activity: Activity,
    private val onCarSelected: ((CarModelVersion) -> Unit)? = null
) : PopupWindow(activity) {
    
    private val viewStack = java.util.Stack<View>()
    private lateinit var currentContainer: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var ivBack: ImageView
    private var updateListener: OnPopupUpdateListener? = null
    private var brandData: Map<Char, List<CarBrand>> = emptyMap()
    
    init {
        initPopupWindow()
        loadData()
    }
    
    private fun initPopupWindow() {
        // 设置弹窗大小（不覆盖整个屏幕，只显示部分）
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        width =  (screenWidth * 0.8).toInt() //WindowManager.LayoutParams.MATCH_PARENT
        height = (screenHeight * 0.7).toInt() // 占屏幕高度的80%
        
        isFocusable = true
        isOutsideTouchable = true
        isTouchable = true
        windowLayoutType = TYPE_APPLICATION_OVERLAY
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // 创建根布局
        contentView = createRootView()
        
        // 设置动画
        animationStyle = R.style.PopupAnimation
        
        // 设置关闭监听
        setOnDismissListener {
            updateListener?.backgroundAlphaWindow(1.0f)
            viewStack.clear()
        }
    }

    public fun setOnPopupUpdateListener(onupdateListener: OnPopupUpdateListener){
        updateListener = onupdateListener
    }
    
    private fun createRootView(): View {
        return LinearLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            //setBackgroundColor(Color.WHITE)
            setBackgroundResource(R.drawable.popup_background)
            
            // 添加标题栏
            addView(createTitleBar())
            
            // 添加关闭按钮
           // addView(createCloseButton())
            
            // 添加内容容器
            currentContainer = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            addView(currentContainer)
        }
    }
    
    private fun createTitleBar(): LinearLayout {
        return LinearLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(80)
            )
            //setBackgroundColor(ContextCompat.getColor(activity, R.color.white))
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            
            // 返回按钮
            ivBack = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(56),
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                setImageResource(R.drawable.ic_back)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                //setColorFilter(Color.WHITE)
                isEnabled = false
                visibility = View.GONE
                setOnClickListener { goBack() }
            }
            
            // 标题
            tvTitle = TextView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                gravity = Gravity.CENTER_HORIZONTAL
                setTextColor(Color.BLACK)
                textSize = 36f
               // typeface = Typeface.DEFAULT_BOLD
                text = context.getString(R.string.choose_brand)  //"选择车模"
                setPadding(dpToPx(16), 0, dpToPx(16), 0)
            }
            
            addView(ivBack)
            addView(tvTitle)
        }
    }
    
    private fun createCloseButton(): View {
        return TextView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            text = context.getString(android.R.string.cancel)// "取消"
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary))
            textSize = 16f
           // typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setOnClickListener { dismiss() }
        }
    }
    
    private fun loadData() {
        showLoadingView()
        
        CoroutineScope(Dispatchers.IO).launch {
            brandData = CarDataScanner.scanCarData(activity)
            
            withContext(Dispatchers.Main) {
                hideLoadingView()
                showBrandList()
            }
        }
    }
    
    private fun showLoadingView() {
        currentContainer.removeAllViews()
        
        val loadingView = LinearLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            
            addView(ProgressBar(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(48),
                    dpToPx(48)
                )
            })
            
            addView(TextView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(16)
                }
                text = "正在加载车模..."
                textSize = 14f
                setTextColor(Color.GRAY)
            })
        }
        
        currentContainer.addView(loadingView)
    }
    
    private fun hideLoadingView() {
        currentContainer.removeAllViews()
    }
    
    private fun showBrandList() {
        currentContainer.removeAllViews()
        
        val brandView = createBrandListView()
        currentContainer.addView(brandView)
        
        viewStack.clear()
        viewStack.push(brandView)
        updateTitle(activity.getString(R.string.choose_brand))
        ivBack.isEnabled = false
        ivBack.visibility = View.GONE
    }
    
    private fun createBrandListView(): View {
        return LayoutInflater.from(activity).inflate(R.layout.layout_car_brand_list, null).apply {
            // 字母导航
            val letterRv = findViewById<RecyclerView>(R.id.rv_letters)
            initLetterNavigation(letterRv)
            
            // 品牌列表
            val brandRv = findViewById<RecyclerView>(R.id.rv_brands)
            initBrandList(brandRv)
        }
    }
    
    private fun initLetterNavigation(recyclerView: RecyclerView) {
        val letters = ('A'..'Z').toList()
        recyclerView.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        
        recyclerView.adapter = LetterAdapter(letters) { letter ->
            scrollToLetter(letter)
        }
    }
    
    private fun initBrandList(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        
//        val items = mutableListOf<Any>()
//        brandData.entries.sortedBy { it.key }.forEach { (letter, brands) ->
//            if (brands.isNotEmpty()) {
//                items.add(letter)
//                items.addAll(brands)
//            }
//        }
//
//        recyclerView.adapter = BrandAdapter(items) { brand ->
//            showModelGroupList(brand)
//        }

        ///***

        // 将Map转换为按字母排序的分组列表
        val groupedList = brandData.entries
            .sortedBy { it.key }
            .map { Pair(it.key, it.value.sortedBy { brand -> brand.name }) }

        recyclerView.adapter = BrandAdapter(groupedList) { brand ->
            showModelGroupList(brand)
        }
    }
    
    private fun scrollToLetter(letter: Char) {
        val brandRv = currentContainer.findViewById<RecyclerView?>(R.id.rv_brands)
        val adapter = brandRv?.adapter as? BrandAdapter
        adapter?.getPositionForLetter(letter)?.let { position ->
            val layoutManager = brandRv.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(position, 0)
            //brandRv.scrollToPosition(position)
        }
    }
    
    private fun showModelGroupList(brand: CarBrand) {
        if (brand.modelGroups.isEmpty()) {
           // Toast.makeText(activity, "该车企暂无车型数据", Toast.LENGTH_SHORT).show()
            return
        }
        
        val modelGroupView = createModelGroupListView(brand)
        animateViewSwitch(modelGroupView)
        
        updateTitle(brand.name)
        viewStack.push(modelGroupView)
        ivBack.isEnabled = true
        ivBack.visibility = View.VISIBLE
    }
    
    private fun createModelGroupListView(brand: CarBrand): View {
        return LayoutInflater.from(activity).inflate(R.layout.layout_model_group_list, null).apply {
            val recyclerView = findViewById<RecyclerView>(R.id.rv_model_groups)
            recyclerView.layoutManager = GridLayoutManager(activity, 4)
            
            recyclerView.adapter = ModelGroupAdapter(brand.modelGroups) { modelGroup ->
                showModelVersionsList(brand, modelGroup)
            }
        }
    }
    
    private fun showModelVersionsList(brand: CarBrand, modelGroup: CarModelGroup) {
        if (modelGroup.versions.isEmpty()) {
            //Toast.makeText(activity, "该车型暂无版本数据", Toast.LENGTH_SHORT).show()
            return
        }
        
        val versionView = createModelVersionsListView(brand, modelGroup)
        animateViewSwitch(versionView)
        
        updateTitle("${brand.name} - ${modelGroup.displayName}")
        viewStack.push(versionView)
    }

    //车型年代
    private fun createModelVersionsListView(brand: CarBrand, modelGroup: CarModelGroup): View {
        return LayoutInflater.from(activity).inflate(R.layout.layout_model_versions_list, null).apply {
            val recyclerView = findViewById<RecyclerView>(R.id.rv_model_versions)
            recyclerView.layoutManager = GridLayoutManager(activity, 3)
            
            recyclerView.adapter = ModelVersionAdapter(modelGroup.versions) { version ->
                onCarSelected?.invoke(version)
                dismiss()
            }
        }
    }
    
    private fun animateViewSwitch(newView: View) {
        val oldView = currentContainer.getChildAt(0)
        newView.alpha = 0f
        
        currentContainer.removeAllViews()
        currentContainer.addView(newView)
        
        newView.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }
    
    private fun updateTitle(title: String) {
        tvTitle.text = title
    }
    
    private fun goBack() {
        if (viewStack.size > 1) {
            viewStack.pop()
            val previousView = viewStack.peek()
            
            animateViewSwitch(previousView)
            
            // 根据视图类型更新标题
            when {
                viewStack.size == 1 -> {
                    updateTitle(  activity.getString(R.string.choose_brand))
                    ivBack.isEnabled = false
                    ivBack.visibility = View.GONE
                }
                previousView.findViewById<RecyclerView?>(R.id.rv_model_groups) != null -> {
                    // 返回到车型分组列表
                    updateTitle(previousView.tag?.toString() ?: activity.getString(R.string.choose_model))
                }
            }
        }
    }
    
//    fun showCarAtLocation(parent: View, gravity: Int, x: Int, y: Int) {
//        // 计算显示位置（在ImageView下方）
//        val location = IntArray(2)
//        parent.getLocationOnScreen(location)
//
//        val offsetY = parent.height + dpToPx(10) // 在下方10dp处显示
//
//        super.showAtLocation(parent, gravity, x, location[1] + offsetY)
//    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * activity.resources.displayMetrics.density).toInt()
    }
}