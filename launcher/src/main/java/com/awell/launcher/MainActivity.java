package com.awell.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.awell.launcher2.Launcher;
import com.awell.plugin_shadow.PluginLoadActivity;

import com.tencent.shadow.sample.constant.Constant;


public class MainActivity extends Activity implements View.OnClickListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private final String CLAZZ_NAME_LAUNCHER = "com.example.launcher_plugin.UIActivity"; // plugin_1 apk


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_launcher_layout);
        initView();

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
            Log.i(TAG, "onCreate: huang create activity==>" + intent);
            finish();
        } else if (v.getId() == R.id.start_ui_activity) {
            Log.i(TAG, "onClick: huang click start plugin 1 =>");
            Intent intent = new Intent(MainActivity.this, PluginLoadActivity.class);
            intent.putExtra(Constant.KEY_PLUGIN_PART_KEY, "plugin-app");
            intent.putExtra(Constant.KEY_ACTIVITY_CLASSNAME, CLAZZ_NAME_LAUNCHER);
            Log.i(TAG, "onClick: huang intent=>" + intent);
            Log.i(TAG, "onClick: huang KEY_PLUGIN_PART_KEY=>" + intent.getStringExtra(Constant.KEY_PLUGIN_PART_KEY));
            Log.i(TAG, "onClick: huang KEY_ACTIVITY_CLASSNAME=>" + intent.getStringExtra(Constant.KEY_ACTIVITY_CLASSNAME));
            startActivity(intent);
            finish();
        }
    }
}
