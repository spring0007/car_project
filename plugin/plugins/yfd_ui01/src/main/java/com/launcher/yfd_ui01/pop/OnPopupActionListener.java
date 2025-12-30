package com.launcher.yfd_ui01.pop;

import com.awell.addapp.AppInfo;

/**
 * PopupWindow操作监听器接口
 */
interface OnPopupActionListener {
    /**
     * PopupWindow消失时调用
     */
    void onPopupDismissed();

    /**
     * 数字参数变化时调用（可选）
     * @param  info
     */
    void updateImage(AppInfo info);
}
