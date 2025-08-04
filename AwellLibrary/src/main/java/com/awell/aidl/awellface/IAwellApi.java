/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: frameworks/base/core/java/com/awell/aidl/awellface/IAwellApi.aidl
 */
package com.awell.aidl.awellface;
/**
 * Base class for Binder interfaces.  When defining a new interface,
 * you must derive it from IInterface.
 * 文件定义的接口在 AwellInterfaceService.java 中实现
 */

/** @hide */
public interface IAwellApi extends android.os.IInterface {
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder implements IAwellApi {
        private static final String DESCRIPTOR = "com.awell.aidl.awellface.IAwellApi";

        /** Construct the stub at attach it to the interface. */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.awell.aidl.awellface.IAwellApi interface,
         * generating a proxy if needed.
         */
        public static IAwellApi asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof IAwellApi))) {
                return ((IAwellApi) iin);
            }
            return new Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_getBinder: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    android.os.IBinder _result = this.getBinder(_arg0);
                    reply.writeNoException();
                    reply.writeStrongBinder(_result);
                    return true;
                }
                case TRANSACTION_dealKeyEvent: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    int _arg1;
                    _arg1 = data.readInt();
                    int _arg2;
                    _arg2 = data.readInt();
                    this.dealKeyEvent(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_sendDataToUart: {
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg0;
                    _arg0 = data.createByteArray();
                    int _arg1;
                    _arg1 = data.readInt();
                    this.sendDataToUart(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_regCanbusInterface: {
                    data.enforceInterface(DESCRIPTOR);
                    IAwellCanbusInterface _arg0;
                    _arg0 = IAwellCanbusInterface.Stub.asInterface(data.readStrongBinder());
                    int _arg1;
                    _arg1 = data.readInt();
                    this.regCanbusInterface(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregCanbusInterface: {
                    data.enforceInterface(DESCRIPTOR);
                    IAwellCanbusInterface _arg0;
                    _arg0 = IAwellCanbusInterface.Stub.asInterface(data.readStrongBinder());
                    int _arg1;
                    _arg1 = data.readInt();
                    this.unregCanbusInterface(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_sendCarEventData: {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    int _arg1;
                    _arg1 = data.readInt();
                    int[] _arg2;
                    _arg2 = data.createIntArray();
                    int _arg3;
                    _arg3 = data.readInt();
                    this.sendCarEventData(_arg0, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_regCarEventInfoListener: {
                    data.enforceInterface(DESCRIPTOR);
                    ICarEventInerface _arg0;
                    _arg0 = ICarEventInerface.Stub.asInterface(data.readStrongBinder());
                    this.regCarEventInfoListener(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregCarEventInfoListener: {
                    data.enforceInterface(DESCRIPTOR);
                    ICarEventInerface _arg0;
                    _arg0 = ICarEventInerface.Stub.asInterface(data.readStrongBinder());
                    this.unregCarEventInfoListener(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_awellmetafile_read: {
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg0;
                    int _arg0_length = data.readInt();
                    if ((_arg0_length < 0)) {
                        _arg0 = null;
                    } else {
                        _arg0 = new byte[_arg0_length];
                    }
                    int _arg1;
                    _arg1 = data.readInt();
                    int _arg2;
                    _arg2 = data.readInt();
                    int _arg3;
                    _arg3 = data.readInt();
                    int _result = this.awellmetafile_read(_arg0, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    reply.writeByteArray(_arg0);
                    return true;
                }
                case TRANSACTION_awellmetafile_write: {
                    data.enforceInterface(DESCRIPTOR);
                    byte[] _arg0;
                    _arg0 = data.createByteArray();
                    int _arg1;
                    _arg1 = data.readInt();
                    int _arg2;
                    _arg2 = data.readInt();
                    int _arg3;
                    _arg3 = data.readInt();
                    int _result = this.awellmetafile_write(_arg0, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_awellmetafile_flush: {
                    data.enforceInterface(DESCRIPTOR);
                    this.awellmetafile_flush();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_awellmetafile_close: {
                    data.enforceInterface(DESCRIPTOR);
                    this.awellmetafile_close();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_registerIAwellCallBack: {
                    data.enforceInterface(DESCRIPTOR);
                    com.awell.aidl.awellautointer.IAwellCallBack _arg0;
                    _arg0 = com.awell.aidl.awellautointer.IAwellCallBack.Stub.asInterface(data.readStrongBinder());
                    int _arg1;
                    _arg1 = data.readInt();
                    this.registerIAwellCallBack(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unRegisterIAwellCallBack: {
                    data.enforceInterface(DESCRIPTOR);
                    com.awell.aidl.awellautointer.IAwellCallBack _arg0;
                    _arg0 = com.awell.aidl.awellautointer.IAwellCallBack.Stub.asInterface(data.readStrongBinder());
                    int _arg1;
                    _arg1 = data.readInt();
                    this.unRegisterIAwellCallBack(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_sendData: {
                    data.enforceInterface(DESCRIPTOR);
                    android.os.Bundle _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = android.os.Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    this.sendData(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_registerIAwellInterfaceListener: {
                    data.enforceInterface(DESCRIPTOR);
                    com.awell.aidl.awellautointer.IAwellInterface _arg0;
                    _arg0 = com.awell.aidl.awellautointer.IAwellInterface.Stub.asInterface(data.readStrongBinder());
                    this.registerIAwellInterfaceListener(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unRegisterIAwellInterfaceListener: {
                    data.enforceInterface(DESCRIPTOR);
                    com.awell.aidl.awellautointer.IAwellInterface _arg0;
                    _arg0 = com.awell.aidl.awellautointer.IAwellInterface.Stub.asInterface(data.readStrongBinder());
                    this.unRegisterIAwellInterfaceListener(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setDataEvent: {
                    data.enforceInterface(DESCRIPTOR);
                    android.os.Bundle _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = android.os.Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _arg1;
                    _arg1 = data.readInt();
                    String _result = this.setDataEvent(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IAwellApi {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            /**
             *   getBinder by  bindertype
             *
             */
            @Override
            public android.os.IBinder getBinder(int bindertype) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                android.os.IBinder _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(bindertype);
                    mRemote.transact(Stub.TRANSACTION_getBinder, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readStrongBinder();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            /**  dealKeyEvent : 按键处理，这里的按键仅限自定义的按键，具体按键定义参考 ENUM_KEY.java
             *     keycode : 按键值 参考 ENUM_KEY.java
             *     status :  按键状态数据大小
             *     value ：  附加信息，如长按时，重复按键次数
             *
             */
            @Override
            public void dealKeyEvent(int keycode, int status, int value) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(keycode);
                    _data.writeInt(status);
                    _data.writeInt(value);
                    mRemote.transact(Stub.TRANSACTION_dealKeyEvent, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            /**  send data to uart : canbus 或 其他app 需要发送数据到MCU串口的用这个接口
             *   datas : 发送的数据
             *   size : 数据大小
             */
            @Override
            public void sendDataToUart(byte[] datas, int size) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(datas);
                    _data.writeInt(size);
                    mRemote.transact(Stub.TRANSACTION_sendDataToUart, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            /**
             *    APP register Interface for get canbus data by uart.
             */
            @Override
            public void regCanbusInterface(IAwellCanbusInterface callback, int pid) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((callback != null)) ? (callback.asBinder()) : (null)));
                    _data.writeInt(pid);
                    mRemote.transact(Stub.TRANSACTION_regCanbusInterface, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            /**
             *  APP unregister Interface when app destroy
             */
            @Override
            public void unregCanbusInterface(IAwellCanbusInterface callback, int pid) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((callback != null)) ? (callback.asBinder()) : (null)));
                    _data.writeInt(pid);
                    mRemote.transact(Stub.TRANSACTION_unregCanbusInterface, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            /**  canbus app send CarEventData  to others app:
             *   datatype: 发送的数据类型 ，例如：轨迹，雷达，转角等
             *   subtype : 数据子类型，有需要时自定义
             *	datas : 数据内容
             *   size: 数据大小
             */
            @Override
            public void sendCarEventData(int datatype, int subtype, int[] datas, int size) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(datatype);
                    _data.writeInt(subtype);
                    _data.writeIntArray(datas);
                    _data.writeInt(size);
                    mRemote.transact(Stub.TRANSACTION_sendCarEventData, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            /**
             *    reg  Listener for  get carevent.
             */
            @Override
            public void regCarEventInfoListener(ICarEventInerface carevent) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((carevent != null)) ? (carevent.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_regCarEventInfoListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            /**
             *    unreg  Listener of carevent.
             */
            @Override
            public void unregCarEventInfoListener(ICarEventInerface carevent) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((carevent != null)) ? (carevent.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_unregCarEventInfoListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int awellmetafile_read(byte[] data, int offset, int size, int mode) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((data == null)) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(data.length);
                    }
                    _data.writeInt(offset);
                    _data.writeInt(size);
                    _data.writeInt(mode);
                    mRemote.transact(Stub.TRANSACTION_awellmetafile_read, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                    _reply.readByteArray(data);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int awellmetafile_write(byte[] data, int offset, int size, int mode) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(data);
                    _data.writeInt(offset);
                    _data.writeInt(size);
                    _data.writeInt(mode);
                    mRemote.transact(Stub.TRANSACTION_awellmetafile_write, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void awellmetafile_flush() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_awellmetafile_flush, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void awellmetafile_close() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_awellmetafile_close, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerIAwellCallBack(com.awell.aidl.awellautointer.IAwellCallBack callback, int pid) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((callback != null)) ? (callback.asBinder()) : (null)));
                    _data.writeInt(pid);
                    mRemote.transact(Stub.TRANSACTION_registerIAwellCallBack, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unRegisterIAwellCallBack(com.awell.aidl.awellautointer.IAwellCallBack callback, int pid) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((callback != null)) ? (callback.asBinder()) : (null)));
                    _data.writeInt(pid);
                    mRemote.transact(Stub.TRANSACTION_unRegisterIAwellCallBack, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendData(android.os.Bundle bundle) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((bundle != null)) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_sendData, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerIAwellInterfaceListener(com.awell.aidl.awellautointer.IAwellInterface iAwellInterface) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((iAwellInterface != null)) ? (iAwellInterface.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_registerIAwellInterfaceListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unRegisterIAwellInterfaceListener(com.awell.aidl.awellautointer.IAwellInterface iAwellInterface) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((iAwellInterface != null)) ? (iAwellInterface.asBinder()) : (null)));
                    mRemote.transact(Stub.TRANSACTION_unRegisterIAwellInterfaceListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String setDataEvent(android.os.Bundle bundle, int flag) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((bundle != null)) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flag);
                    mRemote.transact(Stub.TRANSACTION_setDataEvent, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }

        static final int TRANSACTION_getBinder = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_dealKeyEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_sendDataToUart = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        static final int TRANSACTION_regCanbusInterface = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
        static final int TRANSACTION_unregCanbusInterface = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
        static final int TRANSACTION_sendCarEventData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
        static final int TRANSACTION_regCarEventInfoListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
        static final int TRANSACTION_unregCarEventInfoListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
        static final int TRANSACTION_awellmetafile_read = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
        static final int TRANSACTION_awellmetafile_write = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
        static final int TRANSACTION_awellmetafile_flush = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
        static final int TRANSACTION_awellmetafile_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
        static final int TRANSACTION_registerIAwellCallBack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
        static final int TRANSACTION_unRegisterIAwellCallBack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
        static final int TRANSACTION_sendData = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
        static final int TRANSACTION_registerIAwellInterfaceListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
        static final int TRANSACTION_unRegisterIAwellInterfaceListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
        static final int TRANSACTION_setDataEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
    }

    /**
     *   getBinder by  bindertype
     *
     */
    public android.os.IBinder getBinder(int bindertype) throws android.os.RemoteException;

    /**  dealKeyEvent : 按键处理，这里的按键仅限自定义的按键，具体按键定义参考 ENUM_KEY.java
     *     keycode : 按键值 参考 ENUM_KEY.java
     *     status :  按键状态数据大小
     *     value ：  附加信息，如长按时，重复按键次数
     *
     */
    public void dealKeyEvent(int keycode, int status, int value) throws android.os.RemoteException;

    /**  send data to uart : canbus 或 其他app 需要发送数据到MCU串口的用这个接口
     *   datas : 发送的数据
     *   size : 数据大小
     */
    public void sendDataToUart(byte[] datas, int size) throws android.os.RemoteException;

    /**
     *    APP register Interface for get canbus data by uart.
     */
    public void regCanbusInterface(IAwellCanbusInterface callback, int pid) throws android.os.RemoteException;

    /**
     *  APP unregister Interface when app destroy
     */
    public void unregCanbusInterface(IAwellCanbusInterface callback, int pid) throws android.os.RemoteException;

    /**  canbus app send CarEventData  to others app:
     *   datatype: 发送的数据类型 ，例如：轨迹，雷达，转角等
     *   subtype : 数据子类型，有需要时自定义
     *	datas : 数据内容
     *   size: 数据大小
     */
    public void sendCarEventData(int datatype, int subtype, int[] datas, int size) throws android.os.RemoteException;

    /**
     *    reg  Listener for  get carevent.
     */
    public void regCarEventInfoListener(ICarEventInerface carevent) throws android.os.RemoteException;

    /**
     *    unreg  Listener of carevent.
     */
    public void unregCarEventInfoListener(ICarEventInerface carevent) throws android.os.RemoteException;

    public int awellmetafile_read(byte[] data, int offset, int size, int mode) throws android.os.RemoteException;

    public int awellmetafile_write(byte[] data, int offset, int size, int mode) throws android.os.RemoteException;

    public void awellmetafile_flush() throws android.os.RemoteException;

    public void awellmetafile_close() throws android.os.RemoteException;

    public void registerIAwellCallBack(com.awell.aidl.awellautointer.IAwellCallBack callback, int pid) throws android.os.RemoteException;

    public void unRegisterIAwellCallBack(com.awell.aidl.awellautointer.IAwellCallBack callback, int pid) throws android.os.RemoteException;

    public void sendData(android.os.Bundle bundle) throws android.os.RemoteException;

    public void registerIAwellInterfaceListener(com.awell.aidl.awellautointer.IAwellInterface iAwellInterface) throws android.os.RemoteException;

    public void unRegisterIAwellInterfaceListener(com.awell.aidl.awellautointer.IAwellInterface iAwellInterface) throws android.os.RemoteException;

    public String setDataEvent(android.os.Bundle bundle, int flag) throws android.os.RemoteException;
}
