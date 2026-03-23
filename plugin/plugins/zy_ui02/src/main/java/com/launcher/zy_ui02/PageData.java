package com.launcher.zy_ui02;

public class PageData {
    private int layoutResId; // 存储XML布局资源ID

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
