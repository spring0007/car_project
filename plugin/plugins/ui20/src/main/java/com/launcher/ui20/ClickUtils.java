package com.launcher.ui20;

public class ClickUtils {
    private static long lastClickTime = 0;
    private static final int MIN_CLICK_DELAY_TIME = 300; // 最小点击间隔1秒

    public static boolean isFastClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < MIN_CLICK_DELAY_TIME) {
            return true;
        }
        lastClickTime = currentTime;
        return false;
    }
}
