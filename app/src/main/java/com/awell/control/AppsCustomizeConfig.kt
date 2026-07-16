package com.awell.control

import android.content.Context

/**
 * 应用自定义配置数据类（扁平化结构）
 * 所有配置项直接作为属性，便于其他组件直接访问
 *
 * 使用原则：
 * 1. 只需传入需要修改的值，其他值保持默认
 * 2. Int 类型使用 -1 表示默认值/不修改
 * 3. Boolean? 类型使用 null 表示不修改
 *
 * 使用示例：
 * ```kotlin
 * // 示例 1：只修改图标大小
 * val config = AppsCustomizeConfig(
 *     iconSize = 120
 * )
 *
 * // 示例 2：同时修改多个配置
 * val config = AppsCustomizeConfig(
 *     iconSize = 100,
 *     iconPaddingTop = 15,
 *     columnCount = 8,
 *     rowCount = 3,
 *     fontSizeSp = 14,
 *     fontColor = Color.WHITE,
 *     autoWidthGap = true,
 *     backgroundTheme = 1
 * )
 *
 * // 示例 3：设置图标向右偏移 20 像素
 * val config = AppsCustomizeConfig(
 *     iconOffsetX = 20  // 正值向右，负值向左
 * )
 *
 * AppsCustomizeConfig config = new AppsCustomizeConfig(
 *     100, 0, 0, 15, 0,     // icon: size, paddingLeft, paddingRight, paddingTop, paddingBottom
 *     8, 3,                  // layout: columnCount, rowCount
 *     14, Color.WHITE,       // font: fontSizeSp, fontColor
 *     -1, -1,                // cell: cellWidthDp, cellHeightDp (不修改)
 *     true, null,            // gap: autoWidthGap=true, autoHeightGap=null (不修改)
 *     1                      // theme: backgroundTheme
 * );
 *
 * // Java Builder 模式（推荐）：只设置需要的属性
 * AppsCustomizeConfig config = new AppsCustomizeConfig.Builder()
 *     .setIconSize(100)
 *     .setIconPaddingTop(15)
 *     .build();
 *
 * // Java Builder 模式：设置图标偏移
 * AppsCustomizeConfig config = new AppsCustomizeConfig.Builder()
 *     .setIconOffsetX(20)  // 图标向右偏移 20 像素
 *     .build();
 *
 * // 示例 4：恢复所有默认值
 * AppsCustomizeControl.applyAppsCustomizeConfig(AppsCustomizeConfig.createDefault(context))
 * ```
 */
data class AppsCustomizeConfig(
    // ========== 样式配置 ==========
    val themeMode: Int = 1,  //主题模式 1=默认
    val refresh: Boolean = true, // 是否立即刷新应用图标属性
    val iconSizeSame: Boolean = false, //是否保持图标大小不变（默认 false），true 表示plugin 图标大小与普通图标一致，


    // ========== 图标配置 ==========
    val iconSize: Int = -1,               // 图标大小（像素），-1 表示默认
    val iconTextPadding: Int = 0,       // 图标文本内边距（像素）
    val iconOffsetX: Int = 0,           // 图标水平偏移量（像素），正值向右，负值向左，0 表示不偏移
    val iconOffsetY: Int = 0,           // 图标垂直偏移量（像素），正值向下，负值向上，0 表示不偏移
    val textOrientation: Int = -1,      // 文本方向，-1 默认顶部- -1    (上-0, 下-1,左-2,右-3, )
    val textGravity: Int = -1,          // 文本重力，-1 默认顶部- -1    (左-0, 中-1, 右-2, )

    // ========== 布局配置 ==========
    val columnCount: Int = -1,            // 列数，-1 表示不修改
    val rowCount: Int = -1,               // 行数，-1 表示不修改

    // ========== 字体配置 ==========
    val fontSizeSp: Int = -1,             // 字体大小（sp），-1 表示默认
    val fontColor: Int = -1,              // 字体颜色（ARGB），-1 表示默认
    val textLine:Int = -1,                 //字体行数, 默认小于0   两行

    // ========== Cell 配置 ==========
    val cellWidthDp: Int = -1,            // Cell 宽度（dp），-1 表示默认
    val cellHeightDp: Int = -1,           // Cell 高度（dp），-1 表示默认
    val iconCropWidth: Int = 0, // 图标剪裁宽度
    val iconCropHeight: Int = 0, // 图标剪裁高度

    // ========== 间距配置 ==========
    val autoWidthGap: Boolean = false,    // 水平间距是否自动调整，false 表示不修改
    val autoHeightGap: Boolean = false,   // 垂直间距是否自动调整，false 表示不修改
    val iconGapX: Int = 0, // 图标水平间距（像素）
    val iconGapY: Int = 0, // 图标垂直间距（像素）


    // ========== 主题配置 ==========
    var iconMap: Map<String, String>? = null, //图标映射表
    val pluginPackageName: String? = null, //Plugin 包名
    val pluginOtherBgName: String? = null, // 默认插件背景图片名称
    val backgroundTheme: Int = -1,         // 页面背景主题，-1 表示不修改

    //============ 翻页器 ================
    val indicatorPanel: Int = 0, //翻页指示器样式


) {
    /**
     * Builder 模式，方便 Java 中按需设置属性
     *
     * Java 使用示例：
     * ```java
     * AppsCustomizeConfig config = new AppsCustomizeConfig.Builder()
     *     .setIconSize(100)
     *     .setIconPaddingTop(15)
     *     .build();
     * ```
     */
    class Builder {

        /**
         *  主题模式 1=默认
         */
        private var themeMode: Int = 1

        /**
         *  是否立即刷新应用图标属性
         */
        private var refresh: Boolean = true

        /**
         *  是否保持图标大小不变（默认 false），true 表示plugin 图标大小与普通图标一致，
         */
        private var iconSizeSame: Boolean = false

        /**
         *  图标映射表
         */
        private var iconMap: Map<String, String>? = null
        /**
         * 默认插件背景图片名称
         */
        private var pluginOtherBgName: String? = null
        /**
         * Plugin 包名
         */
        private var pluginPackageName: String? = null
        /**
         * 图标剪裁宽度 ,一般只设置iconCropWidth ，代表圆形
         */
        private var iconCropWidth: Int = 0
        /**
         * 图标剪裁高度 ,一般只设置iconCropHeight ，代表方形
         */
        private var iconCropHeight: Int = 0

        /**
         *  翻页指示器样式
         */
        private var indicatorPanel: Int = 0

        
        /**
         * 图标大小（像素）
         * -1 表示默认
         */
        private var iconSize: Int = -1

        /**
         * 图标文本内边距（像素）
         */
        private var iconTextPadding: Int = 0
        
        /**
         * 图标水平偏移量（像素）
         * 正值向右偏移，负值向左偏移，0 表示不偏移
         */
        private var iconOffsetX: Int = 0

        /**
         * 图标垂直偏移量（像素）
         * 正值向下偏移，负值向上偏移，0 表示不偏移
         */
        private var iconOffsetY: Int = 0
        
        /**
         * 文本方向
         * -1=默认 (上-0, 下-1, 左-2, 右-3)
         */
        private var textOrientation: Int = -1
        
        /**
         * 文本重力/对齐方式
         * -1=默认 (左-0, 中-1, 右-2)
         */
        private var textGravity: Int = -1
        
        /**
         * 列数
         * -1 表示不修改
         */
        private var columnCount: Int = -1
        
        /**
         * 行数
         * -1 表示不修改
         */
        private var rowCount: Int = -1
        
        /**
         * 字体大小（sp）
         * -1 表示默认
         */
        private var fontSizeSp: Int = -1
        
        /**
         * 字体颜色（ARGB）
         * -1 表示默认
         */
        private var fontColor: Int = -1

        /**
         * 字体行数
         * -1 默认两行
         */
        private var textLine: Int = -1
        
        /**
         * Cell 宽度（dp）
         * -1 表示默认
         */
        private var cellWidthDp: Int = -1
        
        /**
         * Cell 高度（dp）
         * -1 表示默认
         */
        private var cellHeightDp: Int = -1
        
        /**
         * 水平间距是否自动调整
         * null 表示不修改
         */
        private var autoWidthGap: Boolean = false
        
        /**
         * 垂直间距是否自动调整
         * false 表示不修改
         */
        private var autoHeightGap: Boolean = false

        /**
         * 图标水平间距（像素）
         * 0 表示不修改
         */
        private var iconGapX: Int = 0
        /**
         * 图标垂直间距（像素）
         * 0 表示不修改
         */
        private var iconGapY: Int = 0

        /**
         * 页面背景主题
         * -1 表示不修改
         */
        private var backgroundTheme: Int = -1

        fun setIconSizeSame(value: Boolean) = apply { this.iconSizeSame = value }
        fun setThemeMode(value: Int) = apply { this.themeMode = value }
        fun setRefresh(value: Boolean) = apply { this.refresh = value }
        fun setIconMap(value: Map<String, String>?) = apply { this.iconMap = value }
        fun setPluginOtherBgName(value: String?) = apply { this.pluginOtherBgName = value }
        fun setPluginPackageName(value: String?) = apply { this.pluginPackageName = value }
        fun setIconCropWidth(value: Int) = apply { this.iconCropWidth = value }
        fun setIconCropHeight(value: Int) = apply { this.iconCropHeight = value }
        fun setIndicatorPanel(value: Int) = apply { this.indicatorPanel = value }

        fun setIconSize(value: Int) = apply { this.iconSize = value }
        fun setIconTextPadding(value: Int) = apply { this.iconTextPadding = value }
        fun setIconOffsetX(value: Int) = apply { this.iconOffsetX = value }
        fun setIconOffsetY(value: Int) = apply { this.iconOffsetY = value }
        fun setTextOrientation(value: Int) = apply { this.textOrientation = value }
        fun setTextGravity(value: Int) = apply { this.textGravity = value }
        fun setColumnCount(value: Int) = apply { this.columnCount = value }
        fun setRowCount(value: Int) = apply { this.rowCount = value }
        fun setFontSizeSp(value: Int) = apply { this.fontSizeSp = value }
        fun setFontColor(value: Int) = apply { this.fontColor = value }
        fun setTextLine(value: Int) = apply { this.textLine = value }
        fun setCellWidthDp(value: Int) = apply { this.cellWidthDp = value }
        fun setCellHeightDp(value: Int) = apply { this.cellHeightDp = value }
        fun setAutoWidthGap(value: Boolean) = apply { this.autoWidthGap = value }
        fun setIconGapX(value: Int) = apply { this.iconGapX = value }
        fun setIconGapY(value: Int) = apply { this.iconGapY = value }
        fun setAutoHeightGap(value: Boolean) = apply { this.autoHeightGap = value }
        fun setBackgroundTheme(value: Int) = apply { this.backgroundTheme = value }

        fun build(): AppsCustomizeConfig = AppsCustomizeConfig(

            themeMode,
            refresh,
            iconSizeSame,
            iconSize,
            iconTextPadding,
            iconOffsetX,
            iconOffsetY,
            textOrientation,
            textGravity,
            columnCount,
            rowCount,
            fontSizeSp,
            fontColor,
            textLine,
            cellWidthDp,
            cellHeightDp,
            iconCropWidth,
            iconCropHeight,
            autoWidthGap,
            autoHeightGap,
            iconGapX = 0,
            iconGapY = 0,
            iconMap,
            pluginPackageName,
            pluginOtherBgName,
            backgroundTheme,
            indicatorPanel,

        )
    }

    override fun toString(): String {
        val props = mutableListOf<String>()
        if (themeMode != 1) props.add("themeMode=$themeMode")
        if (!refresh) props.add("refresh=$refresh")
        if (iconSizeSame) props.add("iconSizeSame=$iconSizeSame")
        if (iconSize != -1) props.add("iconSize=$iconSize")
        if (iconTextPadding != 0) props.add("iconTextPadding=$iconTextPadding")
        if (iconOffsetX != 0) props.add("iconOffsetX=$iconOffsetX")
        if (iconOffsetY != 0) props.add("iconOffsetY=$iconOffsetY")
        if (textOrientation != -1) props.add("textOrientation=$textOrientation")
        if (textGravity != -1) props.add("textGravity=$textGravity")
        if (columnCount != -1) props.add("columnCount=$columnCount")
        if (rowCount != -1) props.add("rowCount=$rowCount")
        if (fontSizeSp != -1) props.add("fontSizeSp=$fontSizeSp")
        if (fontColor != -1) props.add("fontColor=$fontColor")
        if (textLine != -1) props.add("textLine=$textLine")
        if (cellWidthDp != -1) props.add("cellWidthDp=$cellWidthDp")
        if (cellHeightDp != -1) props.add("cellHeightDp=$cellHeightDp")
        if (iconCropWidth != 0) props.add("iconCropWidth=$iconCropWidth")
        if (iconCropHeight != 0) props.add("iconCropHeight=$iconCropHeight")
        if (autoWidthGap) props.add("autoWidthGap=$autoWidthGap")
        if (autoHeightGap) props.add("autoHeightGap=$autoHeightGap")
        if (iconGapX != 0) props.add("iconGapX=$iconGapX")
        if (iconGapY != 0) props.add("iconGapY=$iconGapY")
        if (iconMap != null) props.add("iconMap=$iconMap")
        if (pluginPackageName != null) props.add("pluginPackageName=$pluginPackageName")
        if (pluginOtherBgName != null) props.add("pluginOtherBgName=$pluginOtherBgName")
        if (backgroundTheme != -1) props.add("backgroundTheme=$backgroundTheme")
        if (indicatorPanel != 0) props.add("indicatorPanel=$indicatorPanel")
        return "AppsCustomizeConfig(${props.joinToString(", ")})"
    }


    companion object {
        /**
         * 创建默认配置（全部使用系统默认值）
         * 用于恢复所有配置到默认状态
         */
        @JvmStatic
        fun createDefault(): AppsCustomizeConfig = AppsCustomizeConfig()

    }
}
