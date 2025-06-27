/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: frameworks/base/core/java/com/awell/aidl/awellface/IAwellCanbusInterface.aidl
 */
package com.awell.aidl.awellface;
/**
 * @hide
 */
public interface IAwellCanbusInterface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.awell.aidl.awellface.IAwellCanbusInterface
{
private static final java.lang.String DESCRIPTOR = "com.awell.aidl.awellface.IAwellCanbusInterface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.awell.aidl.awellface.IAwellCanbusInterface interface,
 * generating a proxy if needed.
 */
public static com.awell.aidl.awellface.IAwellCanbusInterface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.awell.aidl.awellface.IAwellCanbusInterface))) {
return ((com.awell.aidl.awellface.IAwellCanbusInterface)iin);
}
return new com.awell.aidl.awellface.IAwellCanbusInterface.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_onResult:
{
data.enforceInterface(DESCRIPTOR);
byte[] _arg0;
_arg0 = data.createByteArray();
int _arg1;
_arg1 = data.readInt();
this.onResult(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_enterOrQuitBackcar:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.enterOrQuitBackcar(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.awell.aidl.awellface.IAwellCanbusInterface
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void onResult(byte[] datas, int size) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeByteArray(datas);
_data.writeInt(size);
mRemote.transact(Stub.TRANSACTION_onResult, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void enterOrQuitBackcar(int state) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(state);
mRemote.transact(Stub.TRANSACTION_enterOrQuitBackcar, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onResult = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_enterOrQuitBackcar = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void onResult(byte[] datas, int size) throws android.os.RemoteException;
public void enterOrQuitBackcar(int state) throws android.os.RemoteException;
}
