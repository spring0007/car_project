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
import com.awell.utils.Utils;
import com.tencent.shadow.sample.constant.Constant;
import com.tencent.shadow.dynamic.host.EnterCallback;
import com.tencent.shadow.sample.host.lib.HostAppsHolder;

import android.os.SystemProperties;
import android.view.ViewGroup;

import java.io.File;
import java.lang.reflect.Method;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final String CLAZZ_NAME_LAUNCHER_1 = "com.example.launcher_plugin.UIActivity"; // plugin_1 apk
    private final String CLAZZ_NAME_LAUNCHER_2 = "com.example.plugin_2.UI2Activity"; // plugin_2 apk

    private final String partKey1 = "plugin-app";
    private final String partKey2 = "plugin2-app";

    private String plugin_pkg_key = "persist.sys.launcher.key"; //value : plugin-app/plugin2-app
    private String plugin_clazz_key = "persist.sys.launcher.clazz"; //value : plugin app class name

    /**
     * 外部保存的插件文件路径
     */
    private final String mExternalPluginPath = "/sdcard/launcher_plugin";
    private Handler mHandler = new Handler();
    private boolean mStartPlugin = true;

    private ViewGroup mViewGroup;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_launcher_layout);
//        initView();
//        startGpsService();

//        mViewGroup = findViewById(R.id.container);


        if ("default".equals(getIntent().getStringExtra("launcher"))) {
            mStartPlugin = false;
        }
        if (!mStartPlugin) {
            startInternalLauncher();
        } else {
            startPlugin();
        }
    }

    private void startInternalLauncher() {
        Intent intent = new Intent(this, Launcher.class);
//            Intent intent = new Intent(this, UIActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * 启动GPS位置模拟服务
     * GPS模拟位置可用
     * adb shell appops set com.awell.launcher android:mock_location allow
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
                Log.i(TAG, "startGpsService: huang start service=>" + service);
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
            startInternalLauncher();
        } else {
            startPlugin();
        }
    }

    private void initView() {

    }

    @Override
    public void onClick(View v) {

    }


    public void startPlugin() {

        PluginHelper.getInstance().singlePool.execute(new Runnable() {
            @Override
            public void run() {
                String plugin_key = SystemProperties.get(plugin_pkg_key, partKey1);
                String plugin_clazz = SystemProperties.get(plugin_clazz_key, CLAZZ_NAME_LAUNCHER_1);

                ((LauncherApplication) getmAppContext()).loadPluginManager(PluginHelper.getInstance().pluginManagerFile);

                Bundle bundle = new Bundle();
                bundle.putString(Constant.KEY_PLUGIN_ZIP_PATH, PluginHelper.getInstance().pluginZipFile.getAbsolutePath());
                if (new File(mExternalPluginPath).exists()) {
                    bundle.putString(Constant.KEY_PLUGINS_APK_PATH, mExternalPluginPath);
                }
                bundle.putString(Constant.KEY_PLUGIN_PART_KEY, plugin_key);
                bundle.putString(Constant.KEY_ACTIVITY_CLASSNAME, plugin_clazz);
//                bundle.putString(Constant.KEY_PLUGIN_PART_KEY, partKey2);
//                bundle.putString(Constant.KEY_ACTIVITY_CLASSNAME, CLAZZ_NAME_LAUNCHER_2);
                ((LauncherApplication) getmAppContext()).getPluginManager()
                        .enter(getmAppContext(), Constant.FROM_ID_START_ACTIVITY, bundle, new EnterCallback() {
                            @Override
                            public void onShowLoadingView(final View view) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //mViewGroup.addView(view);
                                    }
                                });
                            }

                            @Override
                            public void onCloseLoadingView(String pluginPath) {
                                Log.i(TAG, "onCloseLoadingView: huang plugin Path=>" + pluginPath);
                                if (pluginPath != null) {
                                    Utils.setPluginApkFilePath(pluginPath);
                                    finish();
                                } else {
                                    startInternalLauncher();
                                }
                            }

                            @Override
                            public void onEnterComplete() {

                            }
                        });
            }
        });
    }
}
