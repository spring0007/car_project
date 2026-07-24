package com.awell.library.util;

/**
 * 防重复点击工具类
 * 所有插件统一使用此版本，无需各自维护
 */
public class ClickUtils {
    private static long lastClickTime = 0;
    private static final int MIN_CLICK_DELAY_TIME = 300;

    private static long lastFiveSecondClickTime = 0;
    private static final long MIN_FIVE_SECOND_CLICK_TIME = 5000;

    public static boolean isFastClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < MIN_CLICK_DELAY_TIME) {
            return true;
        }
        lastClickTime = currentTime;
        return false;
    }

    /** 5秒内快速点击无效，用于防止重复切换主题壁纸等重量级操作 */
    public static boolean isFastClickFiveSecond() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFiveSecondClickTime < MIN_FIVE_SECOND_CLICK_TIME) {
            return true;
        }
        lastFiveSecondClickTime = currentTime;
        return false;
    }
}
