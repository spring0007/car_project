/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: frameworks/base/core/java/android/os/IPowerManager.aidl
 */
package android.os;
/** @hide */
public interface IPowerManager extends IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends Binder implements IPowerManager
{
private static final String DESCRIPTOR = "android.os.IPowerManager";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an android.os.IPowerManager interface,
 * generating a proxy if needed.
 */
public static IPowerManager asInterface(IBinder obj)
{
if ((obj==null)) {
return null;
}
IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof IPowerManager))) {
return ((IPowerManager)iin);
}
return new Proxy(obj);
}
@Override public IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_acquireWakeLock:
{
data.enforceInterface(DESCRIPTOR);
IBinder _arg0;
_arg0 = data.readStrongBinder();
int _arg1;
_arg1 = data.readInt();
String _arg2;
_arg2 = data.readString();
String _arg3;
_arg3 = data.readString();
WorkSource _arg4;
if ((0!=data.readInt())) {
_arg4 = WorkSource.CREATOR.createFromParcel(data);
}
else {
_arg4 = null;
}
String _arg5;
_arg5 = data.readString();
this.acquireWakeLock(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5);
reply.writeNoException();
return true;
}
case TRANSACTION_acquireWakeLockWithUid:
{
data.enforceInterface(DESCRIPTOR);
IBinder _arg0;
_arg0 = data.readStrongBinder();
int _arg1;
_arg1 = data.readInt();
String _arg2;
_arg2 = data.readString();
String _arg3;
_arg3 = data.readString();
int _arg4;
_arg4 = data.readInt();
this.acquireWakeLockWithUid(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_releaseWakeLock:
{
data.enforceInterface(DESCRIPTOR);
IBinder _arg0;
_arg0 = data.readStrongBinder();
int _arg1;
_arg1 = data.readInt();
this.releaseWakeLock(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_updateWakeLockUids:
{
data.enforceInterface(DESCRIPTOR);
IBinder _arg0;
_arg0 = data.readStrongBinder();
int[] _arg1;
_arg1 = data.createIntArray();
this.updateWakeLockUids(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_powerHint:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
int _arg1;
_arg1 = data.readInt();
this.powerHint(_arg0, _arg1);
return true;
}
case TRANSACTION_updateWakeLockWorkSource:
{
data.enforceInterface(DESCRIPTOR);
IBinder _arg0;
_arg0 = data.readStrongBinder();
WorkSource _arg1;
if ((0!=data.readInt())) {
_arg1 = WorkSource.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
String _arg2;
_arg2 = data.readString();
this.updateWakeLockWorkSource(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_isWakeLockLevelSupported:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.isWakeLockLevelSupported(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_userActivity:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
this.userActivity(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_wakeUp:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
String _arg1;
_arg1 = data.readString();
String _arg2;
_arg2 = data.readString();
this.wakeUp(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_goToSleep:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
this.goToSleep(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_nap:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
this.nap(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isInteractive:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isInteractive();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isPowerSaveMode:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPowerSaveMode();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getPowerSaveState:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
reply.writeNoException();

return true;
}
case TRANSACTION_setPowerSaveMode:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _result = this.setPowerSaveMode(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isDeviceIdleMode:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isDeviceIdleMode();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_isLightDeviceIdleMode:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isLightDeviceIdleMode();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_reboot:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
String _arg1;
_arg1 = data.readString();
boolean _arg2;
_arg2 = (0!=data.readInt());
this.reboot(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_rebootSafeMode:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _arg1;
_arg1 = (0!=data.readInt());
this.rebootSafeMode(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_shutdown:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
String _arg1;
_arg1 = data.readString();
boolean _arg2;
_arg2 = (0!=data.readInt());
this.shutdown(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_crash:
{
data.enforceInterface(DESCRIPTOR);
String _arg0;
_arg0 = data.readString();
this.crash(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getLastShutdownReason:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getLastShutdownReason();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setStayOnSetting:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setStayOnSetting(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_boostScreenBrightness:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
this.boostScreenBrightness(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_isScreenBrightnessBoosted:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isScreenBrightnessBoosted();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setTemporaryScreenBrightnessSettingOverride:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setTemporaryScreenBrightnessSettingOverride(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setTemporaryScreenAutoBrightnessAdjustmentSettingOverride:
{
data.enforceInterface(DESCRIPTOR);
float _arg0;
_arg0 = data.readFloat();
this.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setBacklightOffForWfd:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setBacklightOffForWfd(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setAttentionLight:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
int _arg1;
_arg1 = data.readInt();
this.setAttentionLight(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements IPowerManager
{
private IBinder mRemote;
Proxy(IBinder remote)
{
mRemote = remote;
}
@Override public IBinder asBinder()
{
return mRemote;
}
public String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
// WARNING: When methods are inserted or deleted, the transaction IDs in
// frameworks/native/include/powermanager/IPowerManager.h must be updated to match the order in this file.
//
// When a method's argument list is changed, BnPowerManager's corresponding serialization code (if any) in
// frameworks/native/services/powermanager/IPowerManager.cpp must be updated.

@Override public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder(lock);
_data.writeInt(flags);
_data.writeString(tag);
_data.writeString(packageName);
if ((ws!=null)) {
_data.writeInt(1);
ws.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeString(historyTag);
mRemote.transact(Stub.TRANSACTION_acquireWakeLock, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uidtoblame) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder(lock);
_data.writeInt(flags);
_data.writeString(tag);
_data.writeString(packageName);
_data.writeInt(uidtoblame);
mRemote.transact(Stub.TRANSACTION_acquireWakeLockWithUid, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void releaseWakeLock(IBinder lock, int flags) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder(lock);
_data.writeInt(flags);
mRemote.transact(Stub.TRANSACTION_releaseWakeLock, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void updateWakeLockUids(IBinder lock, int[] uids) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder(lock);
_data.writeIntArray(uids);
mRemote.transact(Stub.TRANSACTION_updateWakeLockUids, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void powerHint(int hintId, int data) throws RemoteException
{
Parcel _data = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(hintId);
_data.writeInt(data);
mRemote.transact(Stub.TRANSACTION_powerHint, _data, null, IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
@Override public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder(lock);
if ((ws!=null)) {
_data.writeInt(1);
ws.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeString(historyTag);
mRemote.transact(Stub.TRANSACTION_updateWakeLockWorkSource, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isWakeLockLevelSupported(int level) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(level);
mRemote.transact(Stub.TRANSACTION_isWakeLockLevelSupported, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void userActivity(long time, int event, int flags) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(time);
_data.writeInt(event);
_data.writeInt(flags);
mRemote.transact(Stub.TRANSACTION_userActivity, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void wakeUp(long time, String reason, String opPackageName) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(time);
_data.writeString(reason);
_data.writeString(opPackageName);
mRemote.transact(Stub.TRANSACTION_wakeUp, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void goToSleep(long time, int reason, int flags) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(time);
_data.writeInt(reason);
_data.writeInt(flags);
mRemote.transact(Stub.TRANSACTION_goToSleep, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void nap(long time) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(time);
mRemote.transact(Stub.TRANSACTION_nap, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isInteractive() throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isInteractive, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isPowerSaveMode() throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPowerSaveMode, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}

@Override public boolean setPowerSaveMode(boolean mode) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((mode)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setPowerSaveMode, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isDeviceIdleMode() throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isDeviceIdleMode, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isLightDeviceIdleMode() throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isLightDeviceIdleMode, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void reboot(boolean confirm, String reason, boolean wait) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((confirm)?(1):(0)));
_data.writeString(reason);
_data.writeInt(((wait)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_reboot, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void rebootSafeMode(boolean confirm, boolean wait) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((confirm)?(1):(0)));
_data.writeInt(((wait)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_rebootSafeMode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void shutdown(boolean confirm, String reason, boolean wait) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((confirm)?(1):(0)));
_data.writeString(reason);
_data.writeInt(((wait)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_shutdown, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void crash(String message) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(message);
mRemote.transact(Stub.TRANSACTION_crash, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getLastShutdownReason() throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLastShutdownReason, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setStayOnSetting(int val) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(val);
mRemote.transact(Stub.TRANSACTION_setStayOnSetting, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void boostScreenBrightness(long time) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(time);
mRemote.transact(Stub.TRANSACTION_boostScreenBrightness, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public boolean isScreenBrightnessBoosted() throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isScreenBrightnessBoosted, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
// temporarily overrides the screen brightness settings to allow the user to
// see the effect of a settings change without applying it immediately

@Override public void setTemporaryScreenBrightnessSettingOverride(int brightness) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(brightness);
mRemote.transact(Stub.TRANSACTION_setTemporaryScreenBrightnessSettingOverride, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(float adj) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeFloat(adj);
mRemote.transact(Stub.TRANSACTION_setTemporaryScreenAutoBrightnessAdjustmentSettingOverride, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
//M add For WFD feature support

@Override public void setBacklightOffForWfd(boolean enable) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((enable)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setBacklightOffForWfd, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
// sets the attention light (used by phone app only)

@Override public void setAttentionLight(boolean on, int color) throws RemoteException
{
Parcel _data = Parcel.obtain();
Parcel _reply = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((on)?(1):(0)));
_data.writeInt(color);
mRemote.transact(Stub.TRANSACTION_setAttentionLight, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_acquireWakeLock = (IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_acquireWakeLockWithUid = (IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_releaseWakeLock = (IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_updateWakeLockUids = (IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_powerHint = (IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_updateWakeLockWorkSource = (IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_isWakeLockLevelSupported = (IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_userActivity = (IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_wakeUp = (IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_goToSleep = (IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_nap = (IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_isInteractive = (IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_isPowerSaveMode = (IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_getPowerSaveState = (IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_setPowerSaveMode = (IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_isDeviceIdleMode = (IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_isLightDeviceIdleMode = (IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_reboot = (IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_rebootSafeMode = (IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_shutdown = (IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_crash = (IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_getLastShutdownReason = (IBinder.FIRST_CALL_TRANSACTION + 21);
static final int TRANSACTION_setStayOnSetting = (IBinder.FIRST_CALL_TRANSACTION + 22);
static final int TRANSACTION_boostScreenBrightness = (IBinder.FIRST_CALL_TRANSACTION + 23);
static final int TRANSACTION_isScreenBrightnessBoosted = (IBinder.FIRST_CALL_TRANSACTION + 24);
static final int TRANSACTION_setTemporaryScreenBrightnessSettingOverride = (IBinder.FIRST_CALL_TRANSACTION + 25);
static final int TRANSACTION_setTemporaryScreenAutoBrightnessAdjustmentSettingOverride = (IBinder.FIRST_CALL_TRANSACTION + 26);
static final int TRANSACTION_setBacklightOffForWfd = (IBinder.FIRST_CALL_TRANSACTION + 27);
static final int TRANSACTION_setAttentionLight = (IBinder.FIRST_CALL_TRANSACTION + 28);
}
// WARNING: When methods are inserted or deleted, the transaction IDs in
// frameworks/native/include/powermanager/IPowerManager.h must be updated to match the order in this file.
//
// When a method's argument list is changed, BnPowerManager's corresponding serialization code (if any) in
// frameworks/native/services/powermanager/IPowerManager.cpp must be updated.

public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) throws RemoteException;
public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uidtoblame) throws RemoteException;
public void releaseWakeLock(IBinder lock, int flags) throws RemoteException;
public void updateWakeLockUids(IBinder lock, int[] uids) throws RemoteException;
public void powerHint(int hintId, int data) throws RemoteException;
public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) throws RemoteException;
public boolean isWakeLockLevelSupported(int level) throws RemoteException;
public void userActivity(long time, int event, int flags) throws RemoteException;
public void wakeUp(long time, String reason, String opPackageName) throws RemoteException;
public void goToSleep(long time, int reason, int flags) throws RemoteException;
public void nap(long time) throws RemoteException;
public boolean isInteractive() throws RemoteException;
public boolean isPowerSaveMode() throws RemoteException;
public boolean setPowerSaveMode(boolean mode) throws RemoteException;
public boolean isDeviceIdleMode() throws RemoteException;
public boolean isLightDeviceIdleMode() throws RemoteException;
public void reboot(boolean confirm, String reason, boolean wait) throws RemoteException;
public void rebootSafeMode(boolean confirm, boolean wait) throws RemoteException;
public void shutdown(boolean confirm, String reason, boolean wait) throws RemoteException;
public void crash(String message) throws RemoteException;
public int getLastShutdownReason() throws RemoteException;
public void setStayOnSetting(int val) throws RemoteException;
public void boostScreenBrightness(long time) throws RemoteException;
public boolean isScreenBrightnessBoosted() throws RemoteException;
// temporarily overrides the screen brightness settings to allow the user to
// see the effect of a settings change without applying it immediately

public void setTemporaryScreenBrightnessSettingOverride(int brightness) throws RemoteException;
public void setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(float adj) throws RemoteException;
//M add For WFD feature support

public void setBacklightOffForWfd(boolean enable) throws RemoteException;
// sets the attention light (used by phone app only)

public void setAttentionLight(boolean on, int color) throws RemoteException;
}
