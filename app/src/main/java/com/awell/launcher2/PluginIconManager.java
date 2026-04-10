package com.awell.launcher2;

import android.content.Context;
import android.util.Log;

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
 * @author Awell
 */
public class PluginIconManager {
    
    private static final String TAG = "PluginIconManager";
    
    // Plugin 配置
    private static String pluginPackageName = null;
    private static String defaultPluginBgName = null;
    
    // Plugin 图标映射表（包名 -> 资源名称）
    private static final Map<String, String> PACKAGE_ICON_MAP_PLUGIN_NAMES = new HashMap<>();
    
    /**
     * 获取 Plugin 包名
     * @return Plugin 包名，如果未设置返回 null
     */
    public static String getPluginPackageName() {
        return pluginPackageName;
    }

    /**
     * 设置 Plugin 包名（必须在加载图标前调用）
     * @param packageName Plugin 包名
     */
    public static void setPluginPackageName(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            pluginPackageName = packageName;
         //   Log.i(TAG, "setPluginPackageName: " + packageName);
        }
    }

    /**
     * 获取 Plugin 默认背景资源名称
     * @return 背景资源名称，如果未设置返回 null
     */
    public static String getDefaultPluginBgName() {
        return defaultPluginBgName;
    }

    /**
     * 设置 Plugin 默认背景资源名称（必须在加载图标前调用）
     * @param resName 背景资源名称
     */
    public static void setDefaultPluginBgName(String resName) {
        if (resName != null && !resName.isEmpty()) {
            defaultPluginBgName = resName;
          //  Log.i(TAG, "setDefaultPluginBgName: " + resName);
        }
    }

    /**
     * 同时设置 Plugin 包名和默认背景资源名称
     * @param packageName Plugin 包名
     * @param bgResName 默认背景资源名称
     */
    public static void setPluginConfig(String packageName, String bgResName) {
        setPluginPackageName(packageName);
        setDefaultPluginBgName(bgResName);
    }

    /**
     * 设置自定义图标映射表（存储资源名称）
     * @param sourceMap 源映射表（来自 Plugin 的图标映射，key=包名, value=资源名称）
     */
    public static void setIconMap(Map<String, String> sourceMap) {
        if (sourceMap != null) {
            PACKAGE_ICON_MAP_PLUGIN_NAMES.clear();
            PACKAGE_ICON_MAP_PLUGIN_NAMES.putAll(sourceMap);
          //  Log.i(TAG, "setIconMap: 已设置 PLUGIN 图标映射表，共 " + PACKAGE_ICON_MAP_PLUGIN_NAMES.size() + " 个图标");
        } else {
            Log.w(TAG, "setIconMap: sourceMap 为 null");
        }
    }

    /**
     * 获取 Plugin 图标的资源名称
     * @param packageName 包名
     * @return 资源名称，如果不存在返回 null
     */
    public static String getPluginIconName(String packageName) {
        return PACKAGE_ICON_MAP_PLUGIN_NAMES.get(packageName);
    }

    /**
     * 根据应用包名从 Plugin 获取图标资源 ID
     * @param packageName 应用包名
     * @return 资源 ID，如果不存在返回 0
     */
    public static int getPluginIconResId(String packageName) {
        try {
            // 1. 获取资源名称
            String resourceName = getPluginIconName(packageName);
            if (resourceName == null || resourceName.isEmpty()) {
               // Log.w(TAG, "getPluginIconResId: 资源名称为空, packageName=" + packageName);
                return 0;
            }
            
            // 2. 调用通用方法获取资源 ID
            return getResourceIdFromPlugin(resourceName);
            
        } catch (Exception e) {
            Log.e(TAG, "getPluginIconResId: 获取失败, packageName=" + packageName, e);
            return 0;
        }
    }
    
    /**
     * 从 Plugin 获取指定资源名称的资源 ID（通用方法）
     * @param resourceName 资源名称
     * @return 资源 ID，如果不存在返回 0
     */
    public static int getResourceIdFromPlugin(String resourceName) {
        try {
            // 1. 获取 Plugin 包名
            String pluginPkg = getPluginPackageName();
            if (pluginPkg == null || pluginPkg.isEmpty()) {
              //  Log.w(TAG, "getResourceIdFromPlugin: Plugin 包名为空");
                return 0;
            }

            // 2. 通过 RePlugin 获取插件 Context
            Context pluginContext = com.qihoo360.replugin.RePlugin.fetchContext(pluginPkg);
            if (pluginContext == null) {
            //    Log.w(TAG, "getResourceIdFromPlugin: 无法获取插件 Context, pluginPkg=" + pluginPkg);
                return 0;
            }

            // 3. 获取资源 ID
            int resId = pluginContext.getResources().getIdentifier(
                    resourceName,
                    "drawable",
                    pluginPkg
            );

           /* if (resId == 0) {
                Log.w(TAG, "getResourceIdFromPlugin: 资源不存在, resourceName=" + resourceName + ", pluginPkg=" + pluginPkg);
            } else {
                Log.i(TAG, "getResourceIdFromPlugin: 成功获取, resourceName=" + resourceName + ", resId=" + resId);
            }*/
           // Log.i(TAG, "getResourceIdFromPlugin: resId=" + resId);
            return resId;

        } catch (Exception e) {
            Log.e(TAG, "getResourceIdFromPlugin: 获取失败, resourceName=" + resourceName, e);
            return 0;
        }
    }

    /**
     * 从 Plugin 获取默认背景资源 ID
     * @return 资源 ID，如果加载失败返回 0
     */
    public static int getPluginDefaultBackgroundResId() {
        try {
            // 1. 获取背景资源名称
            String bgResName = getDefaultPluginBgName();
            if (bgResName == null || bgResName.isEmpty()) {
              //  Log.w(TAG, "getPluginDefaultBackgroundResId: 背景资源名称为空");
                return 0;
            }

            // 2. 调用通用方法获取资源 ID
            return getResourceIdFromPlugin(bgResName);
            
        } catch (Exception e) {
            Log.e(TAG, "getPluginDefaultBackgroundResId: 获取失败", e);
            return 0;
        }
    }
}
