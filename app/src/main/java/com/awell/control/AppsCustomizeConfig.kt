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
    val customStyle: Boolean= false, // 自定义样式，true 表示自定义样式,每行的图标数量固定 ;false 表示默认样式 ,每行的图标数量默认为7个,根据不同的分辨率自动适配图标数量
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

    // ========== Cell 配置 ==========
    val cellWidthDp: Int = -1,            // Cell 宽度（dp），-1 表示默认
    val cellHeightDp: Int = -1,           // Cell 高度（dp），-1 表示默认

    // ========== 间距配置 ==========
    val autoWidthGap: Boolean = false,    // 水平间距是否自动调整，false 表示不修改
    val autoHeightGap: Boolean = false,   // 垂直间距是否自动调整，false 表示不修改

    // ========== 主题配置 ==========
    val backgroundTheme: Int = -1,         // 页面背景主题，-1 表示不修改


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
         * 自定义样式
         * true=固定图标数量; false=自动适配
         */
        private var customStyle: Boolean = false
        
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
         * 页面背景主题
         * -1 表示不修改
         */
        private var backgroundTheme: Int = -1

        fun setCustomStyle(value: Boolean) = apply { this.customStyle = value }
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
        fun setCellWidthDp(value: Int) = apply { this.cellWidthDp = value }
        fun setCellHeightDp(value: Int) = apply { this.cellHeightDp = value }
        fun setAutoWidthGap(value: Boolean) = apply { this.autoWidthGap = value }
        fun setAutoHeightGap(value: Boolean) = apply { this.autoHeightGap = value }
        fun setBackgroundTheme(value: Int) = apply { this.backgroundTheme = value }

        fun build(): AppsCustomizeConfig = AppsCustomizeConfig(
            customStyle,
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
            cellWidthDp,
            cellHeightDp,
            autoWidthGap,
            autoHeightGap,
            backgroundTheme
        )
    }

    companion object {
        /**
         * 创建默认配置（全部使用系统默认值）
         * 用于恢复所有配置到默认状态
         */
        @JvmStatic
        fun createDefault(): AppsCustomizeConfig = AppsCustomizeConfig()

        /**
         * 大图标模式预设
         */
        @JvmStatic
        fun largeIconMode(mContext: Context): AppsCustomizeConfig = AppsCustomizeConfig(
            iconSize = 120,
        )

        /**
         * 密集布局模式预设
         */
        @JvmStatic
        fun denseLayoutMode(): AppsCustomizeConfig = AppsCustomizeConfig(
            columnCount = 8,
            rowCount = 3,
        )

        /**
         * 自定义网格模式预设
         * @param columns 列数
         * @param rows 行数
         */
        @JvmStatic
        fun customGridMode(columns: Int = 4, rows: Int = 3): AppsCustomizeConfig =
            AppsCustomizeConfig(
                columnCount = columns,
                rowCount = rows,
            )

        /**
         * 紧凑模式预设（小图标 + 多列）
         */
        @JvmStatic
        fun compactMode(): AppsCustomizeConfig = AppsCustomizeConfig(
            iconSize = 80,
            columnCount = 7,
            rowCount = 3,
        )
    }
}
