package com.awell.addapp;  // 与 MyDbHelper 同包，便于统一管理

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

import kotlin.jvm.Volatile;

public class AppListStorage {

    private static final String PREF_NAME = "awell_show_app_list";
    private static final String KEY_LIST = "app_list";

    // 内存缓存 — 避免重复磁盘IO
    @Volatile
    private static List<String> sCachedList = null;

    // ============ 读取 ============
    public static List<String> load(Context context) {
        // 内存缓存命中：0ms
        if (sCachedList != null) {
            return new ArrayList<>(sCachedList);
        }
        // 从 SP 读取：首读 ~10ms
        String raw = getPref(context).getString(KEY_LIST, "");
        List<String> list = parseList(raw);
        sCachedList = new ArrayList<>(list);  // 写入缓存
        return list;
    }

    // ============ 写入（全量覆盖） ============
    public static void save(Context context, List<String> appList) {
        // 更新缓存
        sCachedList = appList == null ? new ArrayList<>() : new ArrayList<>(appList);
        // 异步写入磁盘（不阻塞调用线程）
        getPref(context).edit().putString(KEY_LIST, serialize(sCachedList)).apply();
    }

    // ============ 添加单个 ============
    public static void addItem(Context context, String packageName) {
        List<String> list = load(context);
        if (!list.contains(packageName)) {
            list.add(packageName);
            save(context, list);
        }
    }

    // ============ 移除单个 ============
    public static void removeItem(Context context, String packageName) {
        List<String> list = load(context);
        if (list.remove(packageName)) {
            save(context, list);
        }
    }

    // ============ 清空 ============
    public static void clear(Context context) {
        sCachedList = new ArrayList<>();
        getPref(context).edit().remove(KEY_LIST).apply();
    }

    // ============ 序列化 ============
    // 使用 JSON 数组格式，兼容任何特殊字符
    private static String serialize(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");  // 转义包名中的引号
        }
        sb.append("]");
        return sb.toString();
    }

    private static List<String> parseList(String raw) {
        if (raw == null || raw.isEmpty() || "[]".equals(raw)) {
            return new ArrayList<>();
        }
        // 去除首尾 []，按 "," 分割
        String trimmed = raw.substring(1, raw.length() - 1);
        List<String> list = new ArrayList<>();
        int start = 0;
        while (start < trimmed.length()) {
            int quoteStart = trimmed.indexOf('"', start);
            int quoteEnd = trimmed.indexOf('"', quoteStart + 1);
            if (quoteStart == -1 || quoteEnd == -1) break;
            list.add(trimmed.substring(quoteStart + 1, quoteEnd));
            start = quoteEnd + 1;
        }
        return list;
    }

    private static SharedPreferences getPref(Context context) {
        // 使用宿主 Application Context，确保跨插件共享同一文件
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
