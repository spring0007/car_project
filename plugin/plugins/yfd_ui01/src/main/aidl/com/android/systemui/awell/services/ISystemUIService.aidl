package com.android.systemui.awell.services;

import android.content.Intent;
import android.os.Bundle;
import com.android.systemui.awell.services.ISystemUIServiceCallback;

interface ISystemUIService {
    void setFreeformType(int type);
    void startOrSetFreeformType(in Intent intent, in Bundle options,int windowType);
    void registerCallback(ISystemUIServiceCallback callback);
    void unregisterCallback(ISystemUIServiceCallback callback);
}