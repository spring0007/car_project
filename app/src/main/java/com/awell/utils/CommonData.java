package com.awell.utils;

import android.os.RemoteException;
import android.os.ServiceManager;

import com.awell.aidl.awellface.IAwellApi;

public class CommonData {
    /**
     * 小灯广播
     */
    public static final String BROADCAST_LAMP_SWITCH = "awellauto.headlamp.on";


    /**
     * headlamp on
     */
    public static final String ACTION_HEADLAMP_ON = "awellauto.headlamp.on";

    /**
     * headlamp off
     */
    public static final String ACTION_HEADLAMP_OFF = "awellauto.headlamp.off";

    /**
     * acc on
     */
    public static final String ACTION_ACC_ON = "awellauto.acc.on";

    /**
     * acc off
     */
    public static final String ACTION_ACC_OFF = "awellauto.acc.off";

    /**
     *
     */
    public static final String BROADCAST_MEDIA_EXIT = "awell.media.exit";


    /**
     * GPS车速
     * 数据key
     */
    public static final String BROADCAST_GPS_SPEED = "awell.gps.speed";
    public static final String FLAG_KM_MILE = "kmOrMile";
    public static final String KEY_KM_SPEED = "speed_km";
    public static final String KEY_MILE_SPEED = "speed_mile";

    /**
     * ZLink (CarPlay/Android Auto) 连接状态广播
     */
    public static final String ACTION_ZLINK = "com.zjinnova.zlink";

    /**
     * 显示所有应用广播
     */
    public static final String ACTION_SHOW_ALL_APP = "android.launcher.show.allApp";

    /**
     * 车速单位变更广播
     */
    public static final String ACTION_SPEED_UNIT_CHANGE = "CANBUS_CHANGE_SPEED_Unit";

    /**
     * 顶部 Activity 变更广播
     */
    public static final String ACTION_TOP_SESSION_CHANGE = "top_session_package_change";
    public static final String EXTRA_TOP_PACKAGE = "top_package";

    /**
     * 按键广播
     */
    public static final String ACTION_TOUCH_KEY = "com.awellauto.key";
    public static final String EXTRA_KEYCODE = "keycode";
    public static final String EXTRA_KEY_STATUS = "status";
    public static final long MEMENTINE = 1000;
    public static final float MAXINSTANCES = 0.1f;


    /**
     * 获取当前车速单位
     * data == 0；公里/小时
     * data == 1；英里/小时
     */
    static IAwellApi mawellapi;

    public static IAwellApi getAwellApi() {
        if (mawellapi == null) {
            mawellapi = IAwellApi.Stub.asInterface(ServiceManager.getService("AwellAutoApi"));
        }
        return mawellapi;
    }

    public static void readDataToMeta(byte[] data, int offset) {
        try {
            int size = getAwellApi().awellmetafile_read(data, offset, data.length, 1);
//            LogUtil.i(size == data.length ? "awellmetafile_read success !!!!!! = " + size : "awellmetafile_read fail !!!!!! = " + size);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

//        LogUtil.i("!!!!!  " + ToolClass.bytesToHexString(data,data.length));
    }

    public static void writeDataToMeta(byte[] send,int offset){
        try {
            getAwellApi().awellmetafile_write(send,offset,send.length,1);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}

