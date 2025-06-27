/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: frameworks/base/core/java/com/awell/aidl/awellface/ICarEventInerface.aidl
 */
package com.awell.aidl.awellface;
/**
 * @hide
 */
public interface ICarEventInerface extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.awell.aidl.awellface.ICarEventInerface
{
private static final java.lang.String DESCRIPTOR = "com.awell.aidl.awellface.ICarEventInerface";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.awell.aidl.awellface.ICarEventInerface interface,
 * generating a proxy if needed.
 */
public static com.awell.aidl.awellface.ICarEventInerface asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.awell.aidl.awellface.ICarEventInerface))) {
return ((com.awell.aidl.awellface.ICarEventInerface)iin);
}
return new com.awell.aidl.awellface.ICarEventInerface.Stub.Proxy(obj);
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
case TRANSACTION_onCarEvent:
{
data.enforceInterface(DESCRIPTOR);
int[] _arg0;
_arg0 = data.createIntArray();
this.onCarEvent(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getCallerId:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getCallerId();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getCallerpkg:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getCallerpkg();
reply.writeNoException();
reply.writeString(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.awell.aidl.awellface.ICarEventInerface
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
@Override public void onCarEvent(int[] eventdatas) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeIntArray(eventdatas);
mRemote.transact(Stub.TRANSACTION_onCarEvent, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getCallerId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCallerId, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getCallerpkg() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getCallerpkg, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_onCarEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getCallerId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getCallerpkg = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
}
public void onCarEvent(int[] eventdatas) throws android.os.RemoteException;
public int getCallerId() throws android.os.RemoteException;
public java.lang.String getCallerpkg() throws android.os.RemoteException;
}
