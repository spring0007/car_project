package com.android.systemui.awell.services;

import android.graphics.Rect;
import android.content.Intent;
import android.os.Bundle;

interface ISystemUIService {
    void setFreeformType(int type);
    void startOrSetFreeformTypeWithOptions(in Intent intent, in Bundle options,int windowType);
    void startOrSetFreeformType(in Rect rect,int windowType);
}