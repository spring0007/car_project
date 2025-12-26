package com.awell.launcher.host;

import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.awell.launcher.host.databinding.SelectLauncherLayoutBinding;
import com.awell.launcher2.Launcher;
import com.awell.launcher2.LauncherApplication;

import com.awell.utils.Utils;
import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.model.PluginInfo;
import com.qihoo360.replugin.utils.FileUtils;

import android.os.SystemProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean D = true;

    private final String LAUNCHER_KEY = "persist.sys.launcher.key"; //value : plugin-app/plugin2-app
    private final String LAUNCHER_CLAZZ = "persist.sys.launcher.clazz"; //value : plugin app class name


    private static final String mFreeformPkgSettings = "freeform_app_package_name";


    private final String DEFAULT_KEY = "LauncherUI8";
    private final String DEFAULT_CLAZZ = "com.launcher.ui8.MainActivityUI8";

    /**
     * 外部保存的插件文件路径
     */
    private final String mExternalPluginPath = "/system/priv-app/";
    private SelectLauncherLayoutBinding binding;

    private Handler mainHandle;

    private boolean isFirstBoot = true;
    private PluginInfo info;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int debug = 0;
        try {
            debug = Integer.parseInt(SystemProperties.get("persist.sys.awell.logswitch", "1"));
        } catch (NumberFormatException e) {
            debug = 1;
        }
        if (debug == 1) {
            D = true;
        }

        if (D) {
            Log.i(TAG, "onCreate: huang launcher main activity create==>");
        }


        binding = SelectLauncherLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
        //startGpsService();

        mainHandle = new Handler(getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == 0x01) {
                    realStartPlugin(msg);
                    return true;
                }
                return false;
            }
        });

        LauncherApplication launcherApplication = (LauncherApplication) getmAppContext();
        launcherApplication.setStartStatus(pluginStartStatus);

    }

    private void initThread() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                startPluginActivity();
            }
        }).start();

    }

    private void realStartPlugin(@NonNull Message msg) {
        info = (PluginInfo) msg.obj;
        String clazz = Objects.requireNonNull(msg.getData().get("clazz")).toString();
        if (info != null) {
            long startTime = System.currentTimeMillis();
            Intent intent = RePlugin.createIntent(info.getName(), clazz);
            intent.putExtra("boot", isFirstBoot);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            RePlugin.startActivity(MainActivity.this, intent);
            long endTime = System.currentTimeMillis();
            Log.i(TAG, "realStartPlugin: huang start plugin spend time=>" + (endTime - startTime));
        } else {
            Log.e(TAG, "handleMessage: install external plugin failed");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: huang resume start plugin isFirstBoot==>" + isFirstBoot);
        if (!isFirstBoot) {
            String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);
            Message message = buildPluginMsg(apkClazz);
            realStartPlugin(message);
        } else {
            initThread();
        }
    }

    @NonNull
    private Message buildPluginMsg(String apkClazz) {
        Message message = mainHandle.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("clazz", apkClazz);
        message.setData(bundle);
        message.what = 0x01;
        message.obj = info;
        return message;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        LauncherApplication launcherApplication = (LauncherApplication) getmAppContext();
//        launcherApplication.setStartStatus(null);
        if (D) {
            Log.i(TAG, "onDestroy: huang launcher main activity destroy==>");
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
                if (D) {
                    Log.i(TAG, "startGpsService: huang start service=>" + service);
                }
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
        //startPluginActivity();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Log.i(TAG, "onBackPressed: huang Intercept back==>");
    }

    private void initView() {
        binding.startDemo1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startPluginActivity();
            }
        });
    }

    private synchronized void startPluginActivity() {
        if (D) {
            Log.i(TAG, "startPluginActivity: huang start plugin activity==>");
        }

        String apkName = SystemProperties.get(LAUNCHER_KEY, DEFAULT_KEY);
        String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);

        String testApk = apkName + ".apk";
        String testApkPath = mExternalPluginPath + File.separator + apkName + File.separator + testApk;

        simulateInstallExternalPlugin(testApkPath, testApk, apkClazz);

    }

    @Override
    public void onClick(View v) {

    }


    /**
     * 模拟安装或升级（覆盖安装）外置插件
     * 注意：为方便演示，外置插件临时放置到Host的assets/external目录下，具体说明见README</p>
     */
    private void simulateInstallExternalPlugin(String path, String name, String clazz) {

        // 文件是否已经存在？直接删除重来
        String pluginFilePath = getFilesDir().getAbsolutePath() + File.separator + name;
        File pluginFile = new File(pluginFilePath);
        if (pluginFile.exists()) {
            if (D) {
                Log.i(TAG, "simulateInstallExternalPlugin: huang delete ==>");
            }
            FileUtils.deleteQuietly(pluginFile);
        }
        // 开始复制
        copyAssetsFileToAppFiles(path, name);

        info = null;
        if (pluginFile.exists()) {
            info = RePlugin.install(pluginFilePath);
        }
        if (D) {
            Log.i(TAG, "simulateInstallExternalPlugin: huang info=>" + info);
            Log.i(TAG, "simulateInstallExternalPlugin: huang path=>" + path + " exists=>" + (new File(path)).exists());
        }

        Utils.setPluginApkFilePath(path);
        Message message = buildPluginMsg(clazz);
        mainHandle.sendMessage(message);

    }

    /**
     * 从assets目录中复制某文件内容
     *
     * @param assetFileName assets目录下的Apk源文件路径
     * @param newFileName   复制到/data/data/package_name/files/目录下文件名
     */
    private void copyAssetsFileToAppFiles(String assetFileName, String newFileName) {
        int buffsize = 1024;

        try (
                InputStream is = new FileInputStream(assetFileName);
                FileOutputStream fos = this.openFileOutput(newFileName, Context.MODE_PRIVATE)) {
            //            is = this.getAssets().open(assetFileName);
            int byteCount = 0;
            byte[] buffer = new byte[buffsize];
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private final LauncherApplication.PluginStartStatus pluginStartStatus = new LauncherApplication.PluginStartStatus() {
        @Override
        public void startPitActivityResult(String plugin, String activity, boolean result) {
            //result = false;

            if (D) {
                Log.i(TAG, "startPitActivityResult: huang start result=>" + result);
            }

            if (result) {
                Log.i(TAG, "startPitActivityResult: huang not finish main activity=>");
                //finish();
            } else {
                isFirstBoot = false;
                String topActivity = getTopActivity();
                if ("com.awell.launcher.host.MainActivity".equals(topActivity) || topIsFreeform()) {
                    String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);
                    Message message = buildPluginMsg(apkClazz);
                    realStartPlugin(message);
                }
                //startInternalLauncher();
                //finish();
            }
        }
    };

    public String getTopActivity() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            // 传入参数1表示只获取最顶部的1个任务
            List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
            if (runningTasks != null && !runningTasks.isEmpty()) {
                ComponentName topActivity = runningTasks.get(0).topActivity;
                String packageName = topActivity.getPackageName();
                String className = topActivity.getClassName();
                Log.d(TAG, "getTopActivity: Package: " + packageName + ", Class: " + className);
                return className;
            }
        } catch (Exception e) {
            Log.e(TAG, "getTopActivity:  error=>" + e.getMessage());
        }
        return null;
    }

    private boolean topIsFreeform() {
        String free_packName = Settings.System.getString(getContentResolver(), mFreeformPkgSettings);

        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            // 传入参数1表示只获取最顶部的1个任务
            List<ActivityManager.RunningTaskInfo> runningTasks = am.getRunningTasks(1);
            if (runningTasks != null && !runningTasks.isEmpty()) {
                ComponentName topActivity = runningTasks.get(0).topActivity;
                String packageName = topActivity.getPackageName();
                if (packageName.contains(free_packName)) {
                    if (D) {
                        Log.i(TAG, "topIsFreeform: huang top app is freeform app=>");
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "topIsFreeform:  error=>" + e.getMessage());
        }


        return false;
    }

}
