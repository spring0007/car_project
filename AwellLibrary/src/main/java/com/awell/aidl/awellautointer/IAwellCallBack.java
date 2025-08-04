/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: frameworks/base/core/java/com/awell/aidl/awellautointer/IAwellCallBack.aidl
 */
package com.awell.aidl.awellautointer;
// Declare any non-default types here with import statements

/**
 * @hide
 */
public interface IAwellCallBack extends android.os.IInterface {
    /**
     * Local-side IPC implementation stub class.
     */
    public static abstract class Stub extends android.os.Binder implements com.awell.aidl.awellautointer.IAwellCallBack {
        private static final java.lang.String DESCRIPTOR = "com.awell.aidl.awellautointer.IAwellCallBack";

        /**
         * Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an com.awell.aidl.awellautointer.IAwellCallBack interface,
         * generating a proxy if needed.
         */
        public static com.awell.aidl.awellautointer.IAwellCallBack asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof com.awell.aidl.awellautointer.IAwellCallBack))) {
                return ((com.awell.aidl.awellautointer.IAwellCallBack) iin);
            }
            return new com.awell.aidl.awellautointer.IAwellCallBack.Stub.Proxy(obj);
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
                case TRANSACTION_onResult: {
                    data.enforceInterface(DESCRIPTOR);
                    android.os.Bundle _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = android.os.Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    java.lang.String _result = this.onResult(_arg0);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements com.awell.aidl.awellautointer.IAwellCallBack {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public java.lang.String onResult(android.os.Bundle bundle) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((bundle != null)) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_onResult, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }

        static final int TRANSACTION_onResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    }

    public java.lang.String onResult(android.os.Bundle bundle) throws android.os.RemoteException;
}
