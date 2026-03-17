package com.launcher.yfd_ui01.chemo2

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.MediaStore
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

    // 从存储设备加载图片
    private fun loadImagesFromStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            val images = mutableListOf<CarModelVersion>()
            
            // 加载 SD 卡图片 --这里可以加载U盘和SD 卡的图片,故屏蔽下一行代码
            loadImagesFromExternalStorage(images)
            
            // 加载 U 盘图片
            //loadImagesFromUsbStorage(images)
            
            // 加载已保存的自定义车模
            //loadSavedCustomCarImages(images)
            
            withContext(Dispatchers.Main) {
                updateImageList(images)
            }
        }
    }

    // 加载外部存储（SD卡）图片
    private fun loadImagesFromExternalStorage(images: MutableList<CarModelVersion>) {
        try {
            if (!isExternalStorageAvailable()) return
            
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA
            )
            
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            
            activity.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                
                while (cursor.moveToNext() && images.size < 50) { // 限制最多加载 50 张
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val path = cursor.getString(dataColumn)
                    val displayName = name.replace("\\.[^.]+$".toRegex(), "")
                    val contentUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                                    
                    // 根据路径判断是 SD 卡还是 U 盘
                    val source = if (path.startsWith("/storage/emulated/0/")) {
                        // 内部存储/SD 卡
                        CarModelSource.SDCARD
                    } else if (path.startsWith("/storage/") && !path.startsWith("/storage/emulated/")) {
                        // U 盘或其他外部存储设备（如：/storage/7B52-1410/）
                        CarModelSource.USB
                    } else {
                        // 其他情况默认当作 SD 卡
                        CarModelSource.SDCARD
                    }
                                    
                    images.add(CarModelVersion(name, displayName, contentUri.toString(), path, null,source))
                    //LogUtil.i("加载${if (source == CarModelSource.SDCARD) "SD 卡" else "U 盘"}图片：$displayName, uri:$contentUri, path: $path")
                
                }
            }
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "加载SD卡图片失败", e)
        }
    }

    // 加载 U 盘图片
    private fun loadImagesFromUsbStorage(images: MutableList<CarModelVersion>) {
        try {
            val storageManager = activity.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes = storageManager.storageVolumes
                
            LogUtil.d("存储卷数量：${storageVolumes.size}")
                
            storageVolumes.forEach { volume ->
                val volumeState = volume.state
                val volumeDesc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "路径=${volume.directory?.absolutePath}, 状态=$volumeState, 可移动=${volume.isRemovable}"
                } else {
                    "状态=$volumeState, 可移动=${volume.isRemovable}"
                }
                LogUtil.d("存储卷信息：$volumeDesc")
                    
                // 检查是否为已挂载的外部存储（包括 U 盘和 SD 卡）
                if (volumeState == android.os.Environment.MEDIA_MOUNTED) {
                    val volumePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        volume.directory?.absolutePath
                    } else {
                        // Android 10 及以下版本使用反射获取路径
                        getVolumePath(volume)
                    }
                        
                    if (!volumePath.isNullOrEmpty()) {
                        // 排除内部存储路径
                        if (!volumePath.startsWith("/storage/emulated/") && 
                            !volumePath.contains("emulated")) {
                            LogUtil.i("发现外部存储设备：$volumePath")
                            scanDirectoryForImages(images, volumePath)
                        } else {
                            LogUtil.d("跳过内部存储：$volumePath")
                        }
                    } else {
                        LogUtil.w("无法获取存储卷路径")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "加载 U 盘图片失败", e)
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

    // 扫描目录中的图片
    private fun scanDirectoryForImages(images: MutableList<CarModelVersion>, directoryPath: String) {
        try {
            val directory = File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) return
            
            val imageExtensions = listOf("jpg", "jpeg", "png", "webp", "bmp")
            
            directory.walkTopDown()
                .maxDepth(3) // 限制扫描深度
                .filter { it.isFile && imageExtensions.any { ext -> it.extension.equals(ext, true) } }
                .take(50 - images.size) // 限制总数
                .forEach { file ->
                    val uri = Uri.fromFile(file)
                    val displayName = file.name.toString().replace("\\.[^.]+$", "")

                    images.add(CarModelVersion(file.name,displayName,uri.toString(), file.absolutePath, null,CarModelSource.USB))
                    LogUtil.i("加载U盘图片: ${file.name}, displayName: $displayName, $uri, ${file.absolutePath}")
                }
        } catch (e: Exception) {
            Log.e("CarPopupWindow", "扫描目录失败: $directoryPath", e)
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
        val adapter = rvImages?.adapter as? ImagePickerAdapter
        adapter?.updateImages(images)
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