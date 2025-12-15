package com.android.systemui.awell.services;

import android.content.Intent;
import android.os.Bundle;

interface ISystemUIService {
    void setFreeformType(int type);
    void startOrSetFreeformType(in Intent intent, in Bundle options,int windowType);
}