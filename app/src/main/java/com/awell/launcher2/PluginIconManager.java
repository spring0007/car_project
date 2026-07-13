package com.awell.launcher2;

import android.content.Context;

import com.awell.utils.LogUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件图标管理器
 * 负责从 RePlugin 插件中动态加载图标和背景资源
 *
 * 使用流程：
 * 1. 插件启动时调用 setPluginConfig() 设置插件包名和默认背景
 * 2. 插件启动时调用 setIconMap() 设置图标映射表
 * 3. 主应用通过 getPluginIconResId() 或 getPluginDefaultBackgroundResId() 获取资源 ID
 *
 * 注意：切换插件前应先调用 reset() 重置状态，避免旧插件配置残留
 */
public class PluginIconManager {

    private static final String TAG = "PluginIconManager";

    // -------------------- 插件配置 --------------------
    private static String pluginPackageName;
    private static String defaultPluginBgName;

    // 图标映射表（包名 -> 资源名称），使用不可变包装确保外部无法修改
    private static Map<String, String> iconNameMap = Collections.emptyMap();

    // -------------------- 配置设置 --------------------

    /**
     * 一次性设置插件全部配置
     * @param packageName 插件包名
     * @param bgResName   默认背景资源名称
     */
    public static void setPluginConfig(Map<String, String> sourceMap,String packageName, String bgResName) {
        setIconMap(sourceMap);
        pluginPackageName =  (packageName != null && !packageName.isEmpty()) ? packageName : null;;
        defaultPluginBgName = (bgResName != null && !bgResName.isEmpty()) ? bgResName : null;;

    }

    /**
     * 设置图标映射表
     */
    public static void setIconMap(Map<String, String> sourceMap) {
        if (sourceMap != null && !sourceMap.isEmpty()) {
            iconNameMap = new HashMap<>(sourceMap);
        } else {
            iconNameMap = Collections.emptyMap();
            LogUtil.w("setIconMap: sourceMap 为空或 null");
        }
    }

    /**
     * 获取指定包名的图标资源 ID
     */
    public static int getPluginIconResId(String packageName) {
        if (packageName == null) return 0;

        String resourceName = iconNameMap.get(packageName);
        if (resourceName == null || resourceName.isEmpty()) {
            return 0;
        }
        return getResourceIdFromPlugin(resourceName);
    }

    /**
     * 从插件获取指定资源名称的资源 ID
     */
    public static int getResourceIdFromPlugin(String resourceName) {
        if (resourceName == null || resourceName.isEmpty()) return 0;
        if (pluginPackageName == null || pluginPackageName.isEmpty()) return 0;

        try {
            Context pluginContext = com.qihoo360.replugin.RePlugin.fetchContext(pluginPackageName);
            if (pluginContext == null) {
                LogUtil.w("getResourceIdFromPlugin: 无法获取插件 Context, pkg=" + pluginPackageName);
                return 0;
            }

            return pluginContext.getResources().getIdentifier(
                    resourceName, "drawable", pluginPackageName
            );
        } catch (Exception e) {
            LogUtil.e("getResourceIdFromPlugin 失败: " + resourceName, e);
            return 0;
        }
    }

    /**
     * 获取插件默认背景资源 ID
     */
    public static int getPluginDefaultBackgroundResId() {
        if (defaultPluginBgName == null || defaultPluginBgName.isEmpty()) {
            return 0;
        }
        return getResourceIdFromPlugin(defaultPluginBgName);
    }

    // -------------------- 状态管理 --------------------

    /**
     * 重置所有状态（切换插件前必须调用）
     */
    public static void reset() {
        pluginPackageName = null;
        defaultPluginBgName = null;
        iconNameMap = Collections.emptyMap();
    }

    // -------------------- Getter / Setter --------------------

    public static String getPluginPackageName() {
        return pluginPackageName;
    }

}
