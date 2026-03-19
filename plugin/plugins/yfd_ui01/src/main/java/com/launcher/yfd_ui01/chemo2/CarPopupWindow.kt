package com.launcher.yfd_ui01.chemo2

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.launcher.yfd_ui01.R
import com.launcher.yfd_ui01.pop.AppPopupWindow.OnPopupUpdateListener
import com.launcher.yfd_ui01.utils.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CarPopupWindow(
    private val activity: Activity,
    private val onCarSelected: ((CarModelVersion) -> Unit)? = null,
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
        // 添加 @ 符号作为自定义车模的入口（不显示在视图上）
        val letters =   ('@'..'Z').toList()
        recyclerView.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        
        recyclerView.adapter = LetterAdapter(letters, showCustomEntry = false) { letter ->
            scrollToLetter(letter)
        }
    }
    
    private fun initBrandList(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(activity)
        
        // 将Map转换为按字母排序的分组列表
        val groupedList = brandData.entries
            .sortedBy { it.key }
            .map { Pair(it.key, it.value.sortedBy { brand -> brand.name }) }

        recyclerView.adapter = BrandAdapter(
            groupedBrands = groupedList,
            onBrandClick = { brand ->
                showModelGroupList(brand)
            },
            onCustomCarClick = {
                showImagePicker()
            }
        )
    }
    
    private fun scrollToLetter(letter: Char) {
        val brandRv = currentContainer.findViewById<RecyclerView?>(R.id.rv_brands)
        val adapter = brandRv?.adapter as? BrandAdapter
        
        // 如果是 @ 符号，直接滚动到第一项（自定义车模）
        val position = if (letter == '@') {
            0
        } else {
            adapter?.getPositionForLetter(letter)
        }
        
        position?.let {
            val layoutManager = brandRv.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(it, 0)
            //brandRv.scrollToPosition(it)
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

//-----------custom car modle start---------------
    // 显示图片选择器
    private fun showImagePicker() {
        val imagePickerView = createImagePickerView()
        animateViewSwitch(imagePickerView)
        
        updateTitle(activity.getString(R.string.select_car_model))
        viewStack.push(imagePickerView)
        ivBack.isEnabled = true
        ivBack.visibility = View.VISIBLE
        
        // 加载图片
        loadImagesFromStorage()
    }

    // 创建图片选择器视图
    private fun createImagePickerView(): View {
        return LayoutInflater.from(activity).inflate(R.layout.layout_image_picker, null).apply {
            val rvImages = findViewById<RecyclerView>(R.id.rv_images)
            rvImages.layoutManager = GridLayoutManager(activity, 3)
            
            val adapter = ImagePickerAdapter(activity, emptyList()) { carModelVersion ->
                onCarSelected?.invoke(carModelVersion)
                dismiss()
            }
            rvImages.adapter = adapter

        }
    }

    // 从存储设备加载图片 - 优化版本
    private fun loadImagesFromStorage() {
        // 先检查权限
        if (!checkStoragePermission()) {
            LogUtil.e("没有存储权限，无法加载外部存储图片")
            showEmptyView("没有存储权限，无法加载图片")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val sdCardImages = mutableListOf<CarModelVersion>()
            val usbImages = mutableListOf<CarModelVersion>()
            
            // 1. 先快速加载 SD 卡（优先级高，通常更快）
            loadSDCardImages(sdCardImages)
            
            // 2. 立即更新 UI，显示 SD 卡图片
            withContext(Dispatchers.Main) {
                updateImageList(sdCardImages)
            }
            
            // 3. 异步加载 U 盘图片（不阻塞界面）
            loadUsbImages(usbImages)
            
            // 4. 合并结果并更新 UI
            val allImages = sdCardImages + usbImages
            withContext(Dispatchers.Main) {
                if (usbImages.isNotEmpty()) {
                    updateImageList(allImages)
                } else if (allImages.isEmpty()) {
                    // 如果没有任何图片，显示空视图
                    showEmptyView()
                }
            }
        }
    }

    /**
     * 加载 SD 卡图片（优先加载）
     */
    private fun loadSDCardImages(images: MutableList<CarModelVersion>) {
        try {
            val storageManager = activity.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes = storageManager.storageVolumes
            
            LogUtil.d("开始扫描 SD 卡，存储卷数量：${storageVolumes.size}")
            
            storageVolumes.forEach { volume ->
                val volumeState = volume.state
                
                // 只处理已挂载的 SD 卡
                if ((volumeState == android.os.Environment.MEDIA_MOUNTED || 
                     volumeState == android.os.Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    val volumePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        volume.directory?.absolutePath
                    } else {
                        getVolumePath(volume)
                    }
                    
                    // 只扫描 SD 卡路径（以 /storage/emulated/ 开头）
                    if (!volumePath.isNullOrEmpty() && 
                        volumePath.startsWith("/storage/emulated/")) {
                        LogUtil.i("发现 SD 卡：$volumePath")
                        scanDirectoryForImagesOptimized(images, volumePath, CarModelSource.SDCARD, 3)
                    }
                }
            }
            
            LogUtil.i("SD 卡扫描完成，共找到 ${images.size} 张图片")
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "扫描 SD 卡失败", e)
        }
    }
    
    /**
     * 加载 U 盘图片（后台加载）
     */
    private fun loadUsbImages(images: MutableList<CarModelVersion>) {
        try {
            val storageManager = activity.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes = storageManager.storageVolumes
            
            LogUtil.d("开始扫描 U 盘，存储卷数量：${storageVolumes.size}")
            
            storageVolumes.forEach { volume ->
                val volumeState = volume.state
                
                // 只处理已挂载的 U 盘（非内置存储）
                if ((volumeState == android.os.Environment.MEDIA_MOUNTED || 
                     volumeState == android.os.Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    val volumePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        volume.directory?.absolutePath
                    } else {
                        getVolumePath(volume)
                    }
                    
                    // 只扫描 U 盘路径（不以 /storage/emulated/ 开头）
                    if (!volumePath.isNullOrEmpty() && 
                        !volumePath.startsWith("/storage/emulated/")) {
                        LogUtil.i("发现 U 盘：$volumePath")
                        scanDirectoryForImagesOptimized(images, volumePath, CarModelSource.USB, 2)
                    }
                }
            }
            
            LogUtil.i("U 盘扫描完成，共找到 ${images.size} 张图片")
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "扫描 U 盘失败", e)
        }
    }
    
    /**
     * 优化后的目录扫描方法
     * @param maxDepth 最大扫描深度（SD 卡=3, U 盘=2）
     */
    private fun scanDirectoryForImagesOptimized(
        images: MutableList<CarModelVersion>, 
        directoryPath: String, 
        source: CarModelSource,
        maxDepth: Int = 3
    ) {
        try {
            val directory = File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) {
                LogUtil.w("目录不存在或不是目录：$directoryPath")
                return
            }
            
            // 优化的文件扩展名过滤
            val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")
            var count = 0
            val startTime = System.currentTimeMillis()
            
            // 使用序列优化过滤性能
            directory.walkTopDown()
                .maxDepth(maxDepth)
                .asSequence()
                .filter { it.isFile }
                .filter { file ->
                    // 快速文件名过滤
                    val ext = file.extension.lowercase()
                    ext in imageExtensions && 
                    !file.name.startsWith(".") &&
                    file.parentFile?.absolutePath?.contains(".thumbnails") != true
                }
                .take(50 - images.size) // 限制总数
                .forEach { file ->
                    try {
                        val uri = Uri.fromFile(file)
                        val displayName = file.nameWithoutExtension
                        val relativePath = file.absolutePath.substringAfter(directoryPath).trimStart('/')
                        
                        images.add(CarModelVersion(
                            file.name, 
                            displayName, 
                            uri.toString(), 
                            file.absolutePath, 
                            null, 
                            source
                        ))
                        count++
                    } catch (e: Exception) {
                        LogUtil.e("处理文件失败：${file.absolutePath}", e)
                    }
                }
            
            val elapsedTime = System.currentTimeMillis() - startTime
            LogUtil.i("扫描完成 [${if (source == CarModelSource.SDCARD) "SD 卡" else "U 盘"}] " +
                      "$directoryPath, 找到 $count 张图片，耗时 ${elapsedTime}ms")
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "扫描目录失败：$directoryPath", e)
        }
    }


    // Android 10 及以下版本通过反射获取 StorageVolume 的路径
    @Suppress("DEPRECATION")
    private fun getVolumePath(volume: Any): String? {
        return try {
            val method = volume.javaClass.getMethod("getPath")
            method.invoke(volume) as? String
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "获取存储卷路径失败", e)
            null
        }
    }

    // 加载已保存的自定义车模图片
    private fun loadSavedCustomCarImages(images: MutableList<CarModelVersion>) {
        try {
            val customDir = File(activity.filesDir, "user_cars")
            if (!customDir.exists()) return
            
            customDir.listFiles()?.sortedByDescending { it.lastModified() }
                ?.take(10) // 最多显示10个已保存的
                ?.forEach { file ->
                    val uri = Uri.fromFile(file)
                    images.add(0, CarModelVersion(file.name,file.name,uri.toString(),   file.absolutePath, null,CarModelSource.CUSTOM))
                    LogUtil.i("加载自定义车模图片: ${file.name}, $uri, ${file.absolutePath}")
                }
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "加载自定义车模图片失败", e)
        }
    }

    // 更新图片列表
    private fun updateImageList(images: List<CarModelVersion>) {
        val currentView = viewStack.peek()
        val rvImages = currentView?.findViewById<RecyclerView>(R.id.rv_images)
        val emptyView = currentView?.findViewById<View>(R.id.empty_view)
        val adapter = rvImages?.adapter as? ImagePickerAdapter
        
        // 根据是否有图片来显示/隐藏空视图
        if (images.isEmpty()) {
            rvImages?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
            LogUtil.d("显示空视图：暂无图片")
        } else {
            rvImages?.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
            adapter?.updateImages(images)
            LogUtil.d("更新图片列表：${images.size} 张")
        }
    }
    
    /**
     * 显示空视图
     * @param message 可选的提示消息
     */
    private fun showEmptyView(message: String? = null) {
        val currentView = viewStack.peek()
        val rvImages = currentView?.findViewById<RecyclerView>(R.id.rv_images)
        val emptyView = currentView?.findViewById<View>(R.id.empty_view)
        
        rvImages?.visibility = View.GONE
        emptyView?.visibility = View.VISIBLE
        
        message?.let {
            LogUtil.d("显示空视图：$message")
        }
    }

//    // 保存选中的图片
//    private fun saveSelectedImage() {
//        selectedImageUri?.let { uri ->
//            copySelectedImageToPrivateStorage(uri)
//        }
//    }

    // 检查外部存储是否可用
    private fun isExternalStorageAvailable(): Boolean {
        return try {
            val externalStorageState = android.os.Environment.getExternalStorageState()
            (externalStorageState == android.os.Environment.MEDIA_MOUNTED ||
                    externalStorageState == android.os.Environment.MEDIA_MOUNTED_READ_ONLY)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查存储权限
     */
    private fun checkStoragePermission(): Boolean {
        return try {
            when {
                // Android 11+ (API 30+) 需要 MANAGE_EXTERNAL_STORAGE 权限
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    if (Environment.isExternalStorageManager()) {
                        true
                    } else {
                        LogUtil.w("需要所有文件管理权限，请在设置中授予")
                        // 可以在这里引导用户到设置页面授予权限
                        false
                    }
                }
                // Android 6.0-10 (API 23-29) 需要 READ_EXTERNAL_STORAGE 权限
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    val hasReadPermission = ContextCompat.checkSelfPermission(
                        activity,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (!hasReadPermission) {
                        LogUtil.w("需要读取外部存储权限")
                    }
                    hasReadPermission
                }
                // Android 5.x 及以下不需要运行时权限
                else -> true
            }
        } catch (e: Exception) {
            LogUtil.e("检查权限失败", e)
            false
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


    //-----------custom car modle end---------------

}