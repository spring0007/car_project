package com.awell.launcher2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.awell.utils.CommonData;
import com.awell.utils.LogUtil;

/**
 * CanBus 应用显示开关(persist.sys.awell.canbus.display)变更接收器。
 *
 * 与其它 receiver 不同, 这个是在 AndroidManifest.xml 里**静态注册**的(不是 runtime 注册):
 * 只有静态注册, 系统才能在宿主进程不在(被回收/未启动)时把进程拉起来投递广播;
 * runtime 注册的接收器只在进程存活期间有效。
 *
 * 注意: 静态注册的 receiver 在 Android 8.0+ **收不到隐式广播**, 所以发送方必须指定包名或组件:
 *   Intent i = new Intent(CommonData.ACTION_CANBUS_DISPLAY_CHANGE);
 *   i.setPackage("com.awell.launcher.host");   // 或 setComponent(显式指定本 receiver)
 *   i.putExtra(CommonData.EXTRA_CANBUS_DISPLAY, v);
 *   context.sendBroadcast(i);
 *
 * 安全性: 本 receiver 不做任何鉴权, 任何应用都能触发。但它是幂等的 ——
 * 只根据 SystemProperties 的当前值决定是否重载列表, 伪造广播最多造成一次多余的列表重载,
 * 无法改变显示结果。
 */
public class CanBusDisplayReceiver extends BroadcastReceiver {
    private static final String TAG = "CanBusDisplayReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null
                || !CommonData.ACTION_CANBUS_DISPLAY_CHANGE.equals(intent.getAction())) {
            return;
        }
        LogUtil.i(TAG + " onReceive: action=" + intent.getAction()
                + ", extra=" + intent.getStringExtra(CommonData.EXTRA_CANBUS_DISPLAY));

        // 进程是被这条广播拉起来的话, Application.onCreate() 里的首次加载已经会读到新值,
        // refreshAppListIfCanBusDisplayChanged() 内部靠"值比对"判断, 这种情况不会重复重载
        LauncherApplication.checkCanBusDisplayChanged(context);
    }
}