package com.awell.launcher;

import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.awell.launcher2.Launcher;
import com.awell.launcher2.LauncherApplication;
import com.awell.plugin_shadow.PluginHelper;

import com.awell.service.GpsSimulationService;
import com.tencent.shadow.sample.constant.Constant;
import com.tencent.shadow.dynamic.host.EnterCallback;

import java.lang.reflect.Method;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final String CLAZZ_NAME_LAUNCHER_1 = "com.example.launcher_plugin.UIActivity"; // plugin_1 apk
    private final String CLAZZ_NAME_LAUNCHER_2 = "com.example.plugin_2.UI2Activity"; // plugin_2 apk

    private final String partKey1 = "plugin-app";
    private final String partKey2 = "plugin2-app";
    private Handler mHandler = new Handler();
    private boolean mStartPlugin = true;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.select_launcher_layout);
//        initView();
//        startGpsService();

        if ("default".equals(getIntent().getStringExtra("launcher"))) {
            mStartPlugin = false;
        }
        if (!mStartPlugin) {
            Intent intent = new Intent(this, Launcher.class);
//            Intent intent = new Intent(this, UIActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            startPlugin();
        }
    }

    /**
     * 启动GPS位置模拟服务
     */
    private void startGpsService() {
        Intent service = new Intent();
        ComponentName componentName = new ComponentName(getPackageName(), "com.awell.service.GpsSimulationService");
        service.setComponent(componentName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 使用系统用户标识
            UserHandle userHandle = android.os.Process.myUserHandle();
            try {
                // 反射调用 startServiceAsUser（系统 API）
                Method method = Context.class.getMethod(
                        "startServiceAsUser",
                        Intent.class,
                        UserHandle.class
                );
                method.invoke(this, service, userHandle);
                Log.i(TAG, "startHostService: huang start service=>" + service);
            } catch (Exception e) {
                e.printStackTrace();
                // 降级方案
                startService(service);
            }
        } else {
            startService(service);
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!mStartPlugin) {
            Intent launcherIntent = new Intent(this, Launcher.class);
//            Intent launcherIntent = new Intent(this, UIActivity.class);
            launcherIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launcherIntent);
            finish();
        } else {
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
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.start_ui_activity) {
            startPlugin();
        }
    }


    public void startPlugin() {

        PluginHelper.getInstance().singlePool.execute(new Runnable() {
            @Override
            public void run() {
                ((LauncherApplication) getmAppContext()).loadPluginManager(PluginHelper.getInstance().pluginManagerFile);

                Bundle bundle = new Bundle();
                bundle.putString(Constant.KEY_PLUGIN_ZIP_PATH, PluginHelper.getInstance().pluginZipFile.getAbsolutePath());
                //bundle.putString(Constant.KEY_PLUGINS_APK_PATH, "/sdcard/launcher_plugin");
//                bundle.putString(Constant.KEY_PLUGIN_PART_KEY, partKey1);
//                bundle.putString(Constant.KEY_ACTIVITY_CLASSNAME, CLAZZ_NAME_LAUNCHER_1);
                bundle.putString(Constant.KEY_PLUGIN_PART_KEY, partKey2);
                bundle.putString(Constant.KEY_ACTIVITY_CLASSNAME, CLAZZ_NAME_LAUNCHER_2);
                ((LauncherApplication) getmAppContext()).getPluginManager()
                        .enter(getmAppContext(), Constant.FROM_ID_START_ACTIVITY, bundle, new EnterCallback() {
                            @Override
                            public void onShowLoadingView(final View view) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
//                                        mViewGroup.addView(view);
                                    }
                                });
                            }

                            @Override
                            public void onCloseLoadingView() {
                                finish();
                            }

                            @Override
                            public void onEnterComplete() {

                            }
                        });
            }
        });
    }
}
