package com.awell.launcher.host;

import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity1"; //MainActivity.class.getSimpleName();
    private boolean D = false;

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
    private int retryCount = 0;
    private static final int MAX_RETRY = 3;               // 最多重试3次
    private final AtomicBoolean isInstalling = new AtomicBoolean(false); // 安装/启动状态锁
    
    // 优化4: 缓存已检查过的插件状态，避免重复查询
    private String lastCheckedPluginName = null;
    private boolean lastPluginValid = false;

    // 用于延迟finish Host的Runnable
    //private final Runnable finishHostRunnable = this::finish;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: huang launcher main activity create==>");

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

    /**
     * 安装与启动的入口线程（仅在尚未安装且无其他安装进行时执行）
     */
    private void initInstallThread() {
        if (!isInstalling.compareAndSet(false, true)) {
            Log.w(TAG, "initInstallThread: already installing, skip duplicate call");
            return;
        }
        new Thread(() -> {
            try {
                startPluginActivity();
            } finally {
                // 无论成功或最终失败，重置安装锁（后续若需重试会重新获取）
                isInstalling.set(false);
            }
        }).start();

    }

    private void realStartPlugin(@NonNull Message msg) {
        
        try {
            info = (PluginInfo) msg.obj;
            String clazz = Objects.requireNonNull(msg.getData().get("clazz")).toString();
            
            if (info != null) {
                //retryCount = 0;
                String packageName = info.getPackageName();
                Log.i(TAG, "realStartPlugin: huang start plugin info==" + info + ", clazz==>" + clazz + ",packageName==>" + packageName);
                long startTime = System.currentTimeMillis();
                Intent intent = RePlugin.createIntent(packageName, clazz);
                
                // 验证生成的 Intent 是否有效
                if (intent == null || intent.getComponent() == null) {
                    Log.e(TAG, "realStartPlugin: huang create intent failed, will reinstall plugin");
                    scheduleRetryOrFallback();
                    return;
                }

                intent.putExtra("boot", isFirstBoot);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                RePlugin.startActivity(MainActivity.this, intent);
                
                long endTime = System.currentTimeMillis();
                if (D) {
                    Log.i(TAG, "realStartPlugin: huang start plugin spend time=>" + (endTime - startTime));
                }

                // 延迟销毁宿主，确保插件有机会显示，避免因插件启动失败导致宿主被提前杀死
                //mainHandle.postDelayed(finishHostRunnable, 300);

            } else {
                // 插件信息为空 ↑ 说明安装仍未成功
                Log.e(TAG, "realStartPlugin: plugin info is null, will reinstall");
                scheduleRetryOrFallback();
            }
        } catch (Exception e) {
            Log.e(TAG, "realStartPlugin: exception " + e.getMessage(), e);
            scheduleRetryOrFallback();
        }
    }

    /**
     * 根据重试次数决定是再次尝试安装，还是降级到内置桌面
     */
    private void scheduleRetryOrFallback() {
        if (retryCount >= MAX_RETRY) {
            Log.e(TAG, "Max retry reached, fallback to internal launcher");
            fallbackToInternalLauncher();
            return;
        }
        retryCount++;
        Log.w(TAG, "scheduleRetryOrFallback: retry count=" + retryCount);
        
        // 优化2: 重试前检查插件是否已在运行，避免无效重试
        /*String apkName = SystemProperties.get(LAUNCHER_KEY, DEFAULT_KEY).replace(".apk", "");
        PluginInfo pluginInfo = RePlugin.getPluginInfo(apkName);
        if (pluginInfo != null && info != null) {
            Log.i(TAG, "scheduleRetryOrFallback: plugin info exists, skip retry and start directly");
            // 直接使用已有的info，不重新安装
            String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);
            Message message = buildPluginMsg(apkClazz);
            realStartPlugin(message);
            return;
        }*/
        
        // 重置安装锁并重新发起安装（如果当前没有正在进行中的安装）
        isInstalling.set(false);
        initInstallThread();
    }

    /**
     * 降级到内置桌面，保证设备始终可用
     */
    private void fallbackToInternalLauncher() {
        runOnUiThread(() -> {
            Log.w(TAG, "fallbackToInternalLauncher: starting internal Launcher");
            startInternalLauncher();
            isInstalling.set(false); // 释放锁，允许后续重置
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            D = Integer.parseInt(SystemProperties.get("persist.sys.awell.logswitch", "1")) == 1;

        } catch (NumberFormatException e) {
            D = false;
        }
        if (D)
            Log.i(TAG, "onResume: huang resume start plugin isFirstBoot==>" + isFirstBoot);
        if (!isFirstBoot) {
            String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);
            Message message = buildPluginMsg(apkClazz);
            realStartPlugin(message);
        } else {
            // 首次启动，且没有正在进行中的安装任务时才发起
            if (!isInstalling.get()) {
                initInstallThread();
            } else {
                Log.d(TAG, "onResume: installation already in progress, skip duplicate trigger");
            }
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
		//mainHandle.removeCallbacks(finishHostRunnable);
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
        if (isInstalling.get()) {
            Log.d(TAG, "onNewIntent: installation in progress, skip");
            return;
        }

        if (info != null) {
            String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);
            Intent pluginIntent = RePlugin.createIntent(info.getPackageName(), apkClazz);
            pluginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            RePlugin.startActivity(this, pluginIntent);
            Log.d(TAG, "onNewIntent: startActivity");
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (D) {
            Log.i(TAG, "onBackPressed: huang Intercept back==>");
        }
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
        String apkName = SystemProperties.get(LAUNCHER_KEY, DEFAULT_KEY);
        String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);

        String testApk = apkName + ".apk";
        String testApkPath = mExternalPluginPath + apkName + File.separator + testApk;
        Log.i(TAG, "startPluginActivity: huang apkName=" + apkName + ", apkClazz=" + apkClazz + ", path=" + testApkPath);
        simulateInstallExternalPlugin(testApkPath, testApk, apkClazz);
    }

    @Override
    public void onClick(View v) {

    }


    /**
     * 模拟安装或升级(覆盖安装)外置插件
     * 注意:为方便演示,外置插件临时放置到Host的assets/external目录下,具体说明见README</p>
     */
    private void simulateInstallExternalPlugin(String path, String name, String clazz) {

        File sourceApk = new File(path);
        if (!sourceApk.exists() || sourceApk.length() < 1024) {
            Log.e(TAG, "simulateInstallExternalPlugin: source APK invalid: " + path);
            // 直接判定为失败，交由上层重试或降级
            mainHandle.sendMessage(buildFailMessage());
            return;
        }
        
        // 文件是否已经存在？直接删除重来
        String pluginFileName = name; // 直接使用传入的名称作为文件名
        String pluginFilePath = getFilesDir().getAbsolutePath() + File.separator + pluginFileName;
        File pluginFile = new File(pluginFilePath);
        
        // 优化1: 检查插件是否已安装且可用，避免重复安装
        String pluginName = name.replace(".apk", "");
        
        // 使用缓存避免重复查询
        PluginInfo existingPlugin = null;
        if (pluginName.equals(lastCheckedPluginName)) {
            if (lastPluginValid && info != null) {
                existingPlugin = info;
                Log.i(TAG, "simulateInstallExternalPlugin: huang using cached plugin info");
            }
        } else {
            existingPlugin = RePlugin.getPluginInfo(pluginName);
            lastCheckedPluginName = pluginName;
            lastPluginValid = (existingPlugin != null);
        }
        
        if (existingPlugin != null && info != null) {
            if (retryCount > 0) {
                Log.w(TAG, "simulateInstallExternalPlugin: retry attempt, force reinstall");
                // 清除缓存信息，强制走安装流程
                info = null;
                lastPluginValid = false;
                // 删除已复制的文件
                FileUtils.deleteQuietly(new File(pluginFilePath));
            } else {
                Log.i(TAG, "simulateInstallExternalPlugin: huang plugin already installed, skip install");
                // 使用已有的info，不重新安装
                Utils.setPluginApkFilePath(path);
                Message message = buildPluginMsg(clazz);
                mainHandle.sendMessage(message);
                return;
            }
        }
        
        // 优化5: 检查本地文件是否已存在且完整，避免重复复制
        File copiedFile = new File(pluginFilePath);
        if (copiedFile.exists() && copiedFile.length() == sourceApk.length()) {
            Log.i(TAG, "simulateInstallExternalPlugin: huang local file exists and is valid, try install directly");
            // 文件已存在且大小一致，直接尝试安装
        } else {
            if (copiedFile.exists()) {
                if (D) {
                    Log.i(TAG, "simulateInstallExternalPlugin: huang delete incomplete existing file==>");
                }
                FileUtils.deleteQuietly(copiedFile);
            }
                
            // 开始复制
            if (!copyAssetsFileToAppFiles(path, pluginFileName)) {
                Log.e(TAG, "simulateInstallExternalPlugin: copy failed");
                mainHandle.sendMessage(buildFailMessage());
                return;
            }
            
            // 验证复制后的文件完整性
            copiedFile = new File(pluginFilePath);
            if (!copiedFile.exists() || copiedFile.length() != sourceApk.length()) {
                Log.e(TAG, "simulateInstallExternalPlugin: copied file validation failed, expected=" + 
                      sourceApk.length() + ", actual=" + (copiedFile.exists() ? copiedFile.length() : 0));
                mainHandle.sendMessage(buildFailMessage());
                return;
            }
        }

        info = null;
        
        try {
            // 尝试安装插件
            Log.i(TAG, "simulateInstallExternalPlugin: huang start install, path=" + pluginFilePath + ", size=" + copiedFile.length());
            long installStartTime = System.currentTimeMillis();
            info = RePlugin.install(pluginFilePath);
            long installEndTime = System.currentTimeMillis();
            
            if (info == null) {
                Log.e(TAG, "simulateInstallExternalPlugin: huang install plugin failed after " + (installEndTime - installStartTime) + "ms, file may be corrupted or signature mismatch");
                Log.e(TAG, "simulateInstallExternalPlugin: huang source apk path=" + path + ", size=" + sourceApk.length());
                Log.e(TAG, "simulateInstallExternalPlugin: huang copied apk path=" + pluginFilePath + ", size=" + copiedFile.length());
                
                // 更新缓存状态
                lastPluginValid = false;
                
                // 检查签名是否匹配
                try {
                    PackageManager pm = getPackageManager();
                    PackageInfo sourceInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES);
                    PackageInfo copiedInfo = pm.getPackageArchiveInfo(pluginFilePath, PackageManager.GET_SIGNATURES);
                    
                    if (sourceInfo != null && copiedInfo != null) {
                        Log.e(TAG, "simulateInstallExternalPlugin: huang source package=" + sourceInfo.packageName + 
                              ", copied package=" + copiedInfo.packageName);
                    }
                } catch (Exception sigEx) {
                    Log.e(TAG, "simulateInstallExternalPlugin: huang signature check error", sigEx);
                }
                
                // 安装失败,删除损坏的文件
                FileUtils.deleteQuietly(pluginFile);
            } else {
                Log.i(TAG, "simulateInstallExternalPlugin: huang install success in " + (installEndTime - installStartTime) + "ms, plugin info=" + info);
                // 更新缓存状态
                lastPluginValid = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang install exception: " + e.getMessage(), e);
            // 发生异常,删除可能损坏的文件
            FileUtils.deleteQuietly(pluginFile);
            info = null;
            lastPluginValid = false;
        }
    
        Utils.setPluginApkFilePath(path);
        
        // 构建消息时使用最新的info和传入的clazz，确保一致性
        Message message = buildPluginMsg(clazz);
        mainHandle.sendMessage(message);
    }
	
    /**
     * 从 APK 文件解析包名
     */
    private String getPackageNameFromApk(String apkPath) {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, 0);
            return pkgInfo != null ? pkgInfo.packageName : null;
        } catch (Exception e) {
            Log.e(TAG, "getPackageNameFromApk: error", e);
            return null;
        }
    }

    private Message buildFailMessage() {
        Message msg = mainHandle.obtainMessage();
        msg.what = 0x01;
        Bundle data = new Bundle();
        data.putString("clazz", SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ));
        msg.setData(data);
        msg.obj = null;  // info = null 会触发重试
        return msg;
    }


    /**
     * 从assets目录中复制某文件内容
     *
     * @param assetFileName assets目录下的Apk源文件路径
     * @param newFileName   复制到/data/data/package_name/files/目录下文件名
     */
    private boolean copyAssetsFileToAppFiles(String assetFileName, String newFileName) {
        int buffsize = 16384; // 优化3: 增大缓冲区到16KB，减少IO次数
        File srcFile = new File(assetFileName);
        if (!srcFile.exists()) {
            Log.e(TAG, "copyAssetsFileToAppFiles: source file not found: " + assetFileName);
            return false;
        }
        
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            is = new FileInputStream(srcFile);
            fos = this.openFileOutput(newFileName, Context.MODE_PRIVATE);
            
            byte[] buffer = new byte[buffsize];
            int byteCount;
            long totalBytes = 0;
            long startTime = System.currentTimeMillis();
            
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
                totalBytes += byteCount;
            }
            
            // 强制刷新并同步到存储
            fos.flush();
            fos.getFD().sync();
            
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "copyAssetsFileToAppFiles: copy success in " + (endTime - startTime) + "ms, total bytes=" + totalBytes + ", source size=" + srcFile.length());
            
            // 验证文件大小是否一致
            File destFile = new File(getFilesDir(), newFileName);
            if (destFile.length() != srcFile.length()) {
                Log.e(TAG, "copyAssetsFileToAppFiles: file size mismatch! source=" + srcFile.length() + 
                      ", dest=" + destFile.length());
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "copyAssetsFileToAppFiles: error", e);
            return false;
        } finally {
            // 确保资源被正确关闭
            try {
                if (fos != null) fos.close();
            } catch (Exception e) {
                Log.e(TAG, "copyAssetsFileToAppFiles: error closing fos", e);
            }
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                Log.e(TAG, "copyAssetsFileToAppFiles: error closing is", e);
            }
        }
    }

    private String lastFailedPlugin = "";
    private final LauncherApplication.PluginStartStatus pluginStartStatus = new LauncherApplication.PluginStartStatus() {
        @Override
        public void startPitActivityResult(String plugin, String activity, boolean result) {
            Log.i(TAG, "startPitActivityResult: plugin=" + plugin + ", activity=" + activity + ", result=" + result);

            if (result) {
                Log.i(TAG, "startPitActivityResult: plugin started successfully");
                retryCount = 0;
                isFirstBoot = false;
                lastFailedPlugin="";
            } else {
                Log.w(TAG, "startPitActivityResult: huang plugin start failed, will retry");
                isFirstBoot = false;
                String failKey = plugin + "@" + activity;
                if (failKey.equals(lastFailedPlugin)) {
                    Log.w(TAG, "startPitActivityResult: duplicate callback ignored");
                    return;
                }
                lastFailedPlugin = failKey;

                // 检查当前栈顶Activity，避免在非MainActivity时重复启动
                String topActivity = getTopActivity();
                String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);
                
                if ("com.awell.launcher.host.MainActivity".equals(topActivity) || apkClazz.equals(activity)) {
                    Log.i(TAG, "startPitActivityResult: huang top is "+topActivity+", retry with current info");
                    // 使用当前的info和clazz重试
                     scheduleRetryOrFallback();
                } else {
                    Log.i(TAG, "startPitActivityResult: huang top activity changed to: " + topActivity + ", no need to retry");
                }
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

                Log.d(TAG, "getTopActivity: huang Package: " + packageName + ", Class: " + className);

                return className;
            }
        } catch (Exception e) {
            Log.e(TAG, "getTopActivity: huang error=>" + e.getMessage());
        }
        return null;
    }


}
