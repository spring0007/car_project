package com.launcher.yfd_ui6.pop

import com.awell.addapp.AppInfo

interface OnPopupUpdateListener {
    /**
     * 数字参数变化时调用（可选）
     * @param  info
     */
    fun updateAppImage(info: AppInfo)

    fun backgroundAlphaWindow(alpha: Float)
}