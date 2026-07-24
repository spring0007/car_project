package com.awell.library.util;

/**
 * 页面数据持有类，存储XML布局资源ID
 * 所有插件统一使用此版本，无需各自维护
 */
public class PageData {
    private int layoutResId;

    public PageData(int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public int getLayoutResId() {
        return layoutResId;
    }

    public void setLayoutResId(int layoutResId) {
        this.layoutResId = layoutResId;
    }
}
