package com.android.systemui.awell.services;

interface ISystemUIServiceCallback {
    void onConnected(int resultCode);
    void onDisconnected();
}