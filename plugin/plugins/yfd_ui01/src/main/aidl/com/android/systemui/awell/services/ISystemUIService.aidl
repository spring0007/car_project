package com.android.systemui.awell.services;

import android.graphics.Rect;

interface ISystemUIService {
    void setFreeformType(int type);
    void startOrSetFreeformType(in Rect rect,int windowType);
}