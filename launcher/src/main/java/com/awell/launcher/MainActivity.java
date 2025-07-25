package com.awell.launcher;

import static android.os.Process.myPid;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.awell.launcher2.Launcher;
import com.awell.plugin_shadow.PluginLoadActivity;

import com.tencent.shadow.sample.constant.Constant;

import java.lang.reflect.Method;
import java.util.List;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final String CLAZZ_NAME_LAUNCHER = "com.example.launcher_plugin.UIActivity"; // plugin_1 apk
    /**
     *
     */
    private final String partKey = "plugin-app";

    private final String PROXY_ACTIVITY = "com.tencent.shadow.sample.plugin.runtime.PluginDefaultProxyActivity";
    private boolean startPlugin = true;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.select_launcher_layout);
        //initView();

        if (!startPlugin) {
            Intent intent = new Intent(this, Launcher.class);
            startActivity(intent);
            finish();
        } else {
            if (!bringTaskToFront(getBaseContext(), PROXY_ACTIVITY)) {
                startPlugin();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!bringTaskToFront(getBaseContext(), PROXY_ACTIVITY)) {
            startPlugin();
        }
    }

    private void initView() {
        findViewById(R.id.start_ui_activity).setOnClickListener(this);
        findViewById(R.id.start_launcher).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.start_launcher) {
            Intent intent = new Intent(this, Launcher.class);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.start_ui_activity) {
            startPlugin();
        }
    }

    private void startPlugin() {
        Log.i(TAG, "onClick: huang click start plugin 1 =>");
        Intent intent = new Intent(MainActivity.this, PluginLoadActivity.class);
        intent.putExtra(Constant.KEY_PLUGIN_PART_KEY, partKey);
        intent.putExtra(Constant.KEY_ACTIVITY_CLASSNAME, CLAZZ_NAME_LAUNCHER);
        Log.i(TAG, "onClick: huang intent=>" + intent);
        Log.i(TAG, "onClick: huang KEY_PLUGIN_PART_KEY=>" + intent.getStringExtra(Constant.KEY_PLUGIN_PART_KEY));
        Log.i(TAG, "onClick: huang KEY_ACTIVITY_CLASSNAME=>" + intent.getStringExtra(Constant.KEY_ACTIVITY_CLASSNAME));
        startActivity(intent);
        finish();
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean bringTaskToFront(Context context, String activity) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        try {
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            Method moveTaskToFrontMethod = activityManagerClass.getMethod(
                    "moveTaskToFront",
                    int.class,      // taskId
                    int.class       // flags
            );
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = am.getRunningTasks(Integer.MAX_VALUE);
            for (ActivityManager.RunningTaskInfo taskInfo : runningTaskInfos) {
                assert taskInfo.topActivity != null;
                if (taskInfo.topActivity.getClassName().equals(activity)) {
                    moveTaskToFrontMethod.invoke(am, taskInfo.taskId, 0);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "bringTaskToFront error: huang exception=>" + e);
            return false;
        }
    }
}
