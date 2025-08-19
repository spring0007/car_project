package com.awell.library;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;

import com.awell.aidl.awellautointer.IAwellInterface;
import com.awell.aidl.awellface.IAwellApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class AwellLibrary {

    private static final String TAG = "AwellLibrary";
    private Context mContext;
    private IAwellApi mAwellApiService;
    private String mCallerName = "";
    private Handler mHandler;
    private IAwellInterface iAwellInterface;
    private OnDataListener mDataListener;
    private int openFlag = 0; //应用未开启时是否开启  0：不开启  1：开启

    public AwellLibrary() {
        this(null);
    }

    public AwellLibrary(int openFlag) {
        this(null);
        this.openFlag = openFlag;
    }

    public AwellLibrary(Looper looper) {
        Log.i(TAG, "looper = " + looper);
        if (null == looper) {
            looper = Looper.myLooper();
            if (null == looper) {
                looper = Looper.getMainLooper();
            }
        }
        mHandler = new EventHandler(looper);
    }

    /**
     * 初始化 AwellLibrary
     *
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        mCallerName = getAppName(Process.myPid());
        Log.i(TAG, "init mCallerName = " + mCallerName + " Pid = " + Process.myPid());
        iAwellInterface = new IAwellInterface.Stub() {

            @Override
            public void onCallBackEvent(Bundle bundle) throws RemoteException {
                if (bundle == null) return;
                Message msg = Message.obtain(mHandler, 0x100);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }

            @Override
            public int getCallerId() throws RemoteException {
                return Process.myPid();
            }

            @Override
            public String getCallerpkg() throws RemoteException {
                return mCallerName;
            }
        };

        try {
            mAwellApiService = IAwellApi.Stub.asInterface(ServiceManager.getService("AwellAutoApi"));
        } catch (Exception e) {
            Log.e(TAG, "init Exception = " + e.toString());
        }
    }

    /**
     * 释放注册
     */
    public void release() {
        mDataListener = null;
        try {
            if (mAwellApiService != null && iAwellInterface != null)
                mAwellApiService.unRegisterIAwellInterfaceListener(iAwellInterface);
        } catch (Exception e) {
            Log.e(TAG, "release Exception = " + e.toString());
        }
    }

    /**
     * 发送数据
     *
     * @param bundle 类型
     * @return
     */
    public String setDataEvent(Bundle bundle) {

        if (bundle == null || mContext == null) return null;
        Log.i(TAG, "setDataEvent Package = " + mContext.getPackageName() + "  " + getBundle(bundle));
        String string = getReturnSplit(bundle.getString(AwellTool.STATUS_SEND, null));
        try {
            if (mAwellApiService != null) {
                String str = mAwellApiService.setDataEvent(bundle, openFlag);
                Log.i(TAG, "str = " + str);
                if (str == null || str.equals(""))
                    str = string;
                return str;
            }
        } catch (Exception e) {
            Log.e(TAG, "setDataEvent Exception = " + e.toString());
        }
        return string;
    }

    /**
     * 发送不带参数据类型
     *
     * @param status 只传一个状态
     * @return
     */
    public String setDataEvent(String status) {
        Bundle bundle = new Bundle(1);
        bundle.putString(AwellTool.STATUS_SEND, status);
        return setDataEvent(bundle);
    }

    /**
     * 发送带一个int参数据类型
     *
     * @param status 状态
     * @param value1 参数1
     * @return
     */
    public String setDataEvent(String status, int value1) {
        Bundle bundle = new Bundle(2);
        bundle.putString(AwellTool.STATUS_SEND, status);
        bundle.putInt(AwellTool.VALUE_M1, value1);
        return setDataEvent(bundle);
    }

    /**
     * 发送带一个boolean参数据类型
     *
     * @param status 状态
     * @param value1 参数1
     * @return
     */
    public String setDataEvent(String status, boolean value1) {
        Bundle bundle = new Bundle(2);
        bundle.putString(AwellTool.STATUS_SEND, status);
        bundle.putBoolean(AwellTool.VALUE_M1, value1);
        return setDataEvent(bundle);
    }

    /**
     * 发送带一个String参数据类型
     *
     * @param status 状态
     * @param value1 参数1
     * @return
     */
    public String setDataEvent(String status, String value1) {
        Bundle bundle = new Bundle(2);
        bundle.putString(AwellTool.STATUS_SEND, status);
        bundle.putString(AwellTool.VALUE_M1, value1);
        return setDataEvent(bundle);
    }

    /**
     * 发送带两个String参数据类型
     *
     * @param status 状态
     * @param value1 参数1
     * @param value2 参数2
     * @return
     */
    public String setDataEvent(String status, String value1, String value2) {
        Bundle bundle = new Bundle(3);
        bundle.putString(AwellTool.STATUS_SEND, status);
        bundle.putString(AwellTool.VALUE_M1, value1);
        bundle.putString(AwellTool.VALUE_M2, value2);
        return setDataEvent(bundle);
    }

    /**
     * 注册回调监听
     *
     * @param listener
     */
    public void setOnDataListener(OnDataListener listener) {
        Log.i(TAG, "listener = " + listener);
        if (listener != null) {
            try {
                if (mAwellApiService != null && iAwellInterface != null) {
                    mAwellApiService.registerIAwellInterfaceListener(iAwellInterface);
                    mDataListener = listener;
                }
            } catch (Exception e) {
                Log.e(TAG, "setOnDataListener Exception = " + e.toString());
            }
        }
    }

    /**
     * 回调监听接口
     */
    public interface OnDataListener {
        void onResult(Bundle bundle);
    }

    /**
     * 根据pid获取包名
     *
     * @param pid
     * @return
     */
    private String getAppName(int pid) {
        String processName = "";
        File f = new File("/proc/" + pid + "/cmdline"); ///proc/4253/cmdline
        if (pid != 0 && f != null && f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"), 256)) {
                String line;
                if ((line = br.readLine()) != null) {
                    processName = line.trim();
                    Log.i(TAG, "getAppName() = " + processName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return processName;
    }

    /**
     * 显示bundle全部内容
     *
     * @param bundle
     * @return
     */
    private String getBundle(Bundle bundle) {
//        Bundle[{value_m1=0, awellStatusSend=radio_setFmAm1}]

        StringBuffer sb = new StringBuffer();
        sb.append("Bundle[{");
        for (String key : bundle.keySet()) {
            sb.append(key + "=" + bundle.get(key) + ",");
        }
        sb.append("}]");
        return sb.toString();
    }

    private String getReturnSplit(String name) {
        if (TextUtils.isEmpty(name)) return null;
        int i, j, number;
        String str = "";
        for (i = 0; i < AwellTool.ReturnSplit.length; i++) {
            if (AwellTool.ReturnSplit[i][1].equals(name)) {
                number = Integer.parseInt(AwellTool.ReturnSplit[i][0]);
                for (j = 0; j < number; j++) {
                    str = (j == 0 ? "" : str + AwellTool.SPLIT + "");
                }
            }
        }
        return str;
    }

    /**
     * Handler监听
     */
    private class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            //Log.i(TAG,"handleMessage() start what = " + msg.what);
            if (msg.what == 0x100) {
                if (mDataListener == null) {
                    return;
                }
                Bundle bundle = msg.getData();
                if (bundle != null)
                    mDataListener.onResult(bundle);
                Log.i(TAG,"handleMessage() over " + mCallerName + "  " + getBundle(bundle));
            }
        }
    }
}
