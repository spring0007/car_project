package com.launcher.yfd_ui2.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {    
            BootStateManager.INSTANCE.markBootCompleted(context.getApplicationContext());
        }else if(Intent.ACTION_SHUTDOWN.equals(intent.getAction())){
            BootStateManager.INSTANCE.resetForShutdown(context.getApplicationContext());
        }
    }
}