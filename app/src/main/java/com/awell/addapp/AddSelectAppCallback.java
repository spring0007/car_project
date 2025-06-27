package com.awell.addapp;

/**
 * 添加选择的应用到显示列表
 */
public interface AddSelectAppCallback {
    void addAppInfo(AppInfo appInfo);

    void removeAppInfo(String packageName);
}
