package com.awell.launcher.host;

import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.TaskInfo;
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
    private static int logswitch = Integer.parseInt(SystemProperties.get("persist.sys.awell.logswitch","1"));
    private boolean D = (logswitch == 1);

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
        Log.i(TAG, "onCreate: huang launcher main activity create==>");

//        int debug = 0;
//        try {
//            debug = Integer.parseInt(SystemProperties.get("persist.sys.awell.logswitch", "1"));
//        } catch (NumberFormatException e) {
//            debug = 1;
//        }
//        if (debug == 1) {
//            D = true;
//        }

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
            if (D) {
                Log.i(TAG, "realStartPlugin: huang start plugin spend time=>" + (endTime - startTime));
            }
        } else {
            if (D) {
                Log.e(TAG, "handleMessage: install external plugin failed");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (D)
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
     * 模拟安装或升级(覆盖安装)外置插件
     * 注意:为方便演示,外置插件临时放置到Host的assets/external目录下,具体说明见README</p>
     */
    private void simulateInstallExternalPlugin(String path, String name, String clazz) {
    
        // 检查源文件是否存在且可读
        File sourceFile = new File(path);
        if (!sourceFile.exists()) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang source file not exists: " + path);
            return;
        }
            
        long sourceFileSize = sourceFile.length();
        if (sourceFileSize == 0) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang source file is empty: " + path);
            return;
        }

        // 【关键修复】清理 RePlugin 内部缓存目录,避免只读文件冲突
        cleanRePluginCache(name);
    
        // 文件是否已经存在?直接删除重来
        String pluginFilePath = getFilesDir().getAbsolutePath() + File.separator + name;
        File pluginFile = new File(pluginFilePath);
        if (pluginFile.exists()) {
            if (D) {
                Log.i(TAG, "simulateInstallExternalPlugin: huang delete old plugin==>");
            }
            FileUtils.deleteQuietly(pluginFile);
        }
            
        // 开始复制
        boolean copySuccess = copyAssetsFileToAppFiles(path, name);
        if (!copySuccess) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang copy file failed");
            return;
        }
            
        // 验证复制后的文件完整性
        if (!pluginFile.exists()) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang copied file not exists");
            return;
        }
            
        long copiedFileSize = pluginFile.length();
        if (copiedFileSize != sourceFileSize) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang file size mismatch! source: " + 
                    sourceFileSize + ", copied: " + copiedFileSize);
            // 删除损坏的文件
            FileUtils.deleteQuietly(pluginFile);
            return;
        }
            
        if (copiedFileSize == 0) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang copied file is empty");
            FileUtils.deleteQuietly(pluginFile);
            return;
        }
    
        info = null;
        try {
            // 尝试安装插件
            info = RePlugin.install(pluginFilePath);
            if (info == null) {
                Log.e(TAG, "simulateInstallExternalPlugin: huang install plugin failed, file may be corrupted");
                // 安装失败,删除损坏的文件
                FileUtils.deleteQuietly(pluginFile);
            } else {
                if (D) {
                    Log.i(TAG, "simulateInstallExternalPlugin: huang install success: " + info.getName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "simulateInstallExternalPlugin: huang install exception: " + e.getMessage(), e);
            // 发生异常,删除可能损坏的文件
            FileUtils.deleteQuietly(pluginFile);
            info = null;
        }
    
        Utils.setPluginApkFilePath(path);
        Message message = buildPluginMsg(clazz);
        mainHandle.sendMessage(message);
    
    }

    /**
     * 清理 RePlugin 内部缓存目录,防止只读文件导致安装失败
     * RePlugin 会将插件复制到 app_p_a 目录,如果该目录存在只读文件会导致安装失败
     * 
     * @param pluginName 插件名称
     */
    private void cleanRePluginCache(String pluginName) {
        try {
            // RePlugin 缓存目录: /data/user/0/{package}/app_p_a/
            File cacheDir = new File(getFilesDir().getParent(), "app_p_a");
            if (cacheDir.exists() && cacheDir.isDirectory()) {
                // 查找并删除与当前插件相关的缓存文件
                File[] cachedFiles = cacheDir.listFiles((dir, filename) -> 
                    filename.contains(pluginName.replace(".apk", "")) || 
                    filename.endsWith(".jar") ||
                    filename.endsWith(".odex")
                );
                
                if (cachedFiles != null && cachedFiles.length > 0) {
                    if (D) {
                        Log.i(TAG, "cleanRePluginCache: huang found " + cachedFiles.length + " cached files to clean");
                    }
                    for (File cachedFile : cachedFiles) {
                        // 强制设置可写权限
                        if (!cachedFile.canWrite()) {
                            cachedFile.setWritable(true);
                            if (D) {
                                Log.i(TAG, "cleanRePluginCache: huang set writable for: " + cachedFile.getName());
                            }
                        }
                        
                        boolean deleted = cachedFile.delete();
                        if (D) {
                            Log.i(TAG, "cleanRePluginCache: huang delete " + cachedFile.getName() + ": " + deleted);
                        }
                    }
                }
                
                // 同时清理 oat 目录中的 odex 文件
                File oatDir = new File(cacheDir, "oat");
                if (oatDir.exists()) {
                    deleteRecursively(oatDir);
                }
                
                // 清理 native lib 目录
                File nlibDir = new File(getFilesDir().getParent(), "app_p_n");
                if (nlibDir.exists()) {
                    deleteRecursively(nlibDir);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "cleanRePluginCache: huang error: " + e.getMessage(), e);
        }
    }

    /**
     * 递归删除目录及其内容
     */
    private boolean deleteRecursively(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        // 强制设置可写权限后再删除
        if (!fileOrDirectory.canWrite()) {
            fileOrDirectory.setWritable(true);
        }
        return fileOrDirectory.delete();
    }

    /**
     * 从assets目录中复制某文件内容
     *
     * @param assetFileName assets目录下的Apk源文件路径
     * @param newFileName   复制到/data/data/package_name/files/目录下文件名
     * @return true if copy success, false otherwise
     */
    private boolean copyAssetsFileToAppFiles(String assetFileName, String newFileName) {
        int buffsize = 8192; // 增大缓冲区提高复制效率
        long totalBytesRead = 0;

        File sourceFile = new File(assetFileName);
        long expectedSize = sourceFile.length();

        try (
                InputStream is = new FileInputStream(assetFileName);
                FileOutputStream fos = this.openFileOutput(newFileName, Context.MODE_PRIVATE)) {
            byte[] buffer = new byte[buffsize];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
                totalBytesRead += byteCount;
            }
            fos.flush();
            
            // 验证复制的字节数
            if (expectedSize > 0 && totalBytesRead != expectedSize) {
                Log.e(TAG, "copyAssetsFileToAppFiles: huang size mismatch! expected: " + 
                        expectedSize + ", actual: " + totalBytesRead);
                // 删除不完整的文件
                fos.close();
                File destFile = new File(getFilesDir(), newFileName);
                if (destFile.exists()) {
                    destFile.delete();
                }
                return false;
            }

            if (D) {
                Log.i(TAG, "copyAssetsFileToAppFiles: huang copy success, size: " + totalBytesRead);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "copyAssetsFileToAppFiles: huang error: " + e.getMessage(), e);
            // 发生异常时删除可能不完整的文件
            File destFile = new File(getFilesDir(), newFileName);
            if (destFile.exists()) {
                destFile.delete();
            }
            return false;
        }
    }


    private boolean isPluginStarting = false; // 防止重复启动插件

    private final LauncherApplication.PluginStartStatus pluginStartStatus = new LauncherApplication.PluginStartStatus() {
        @Override
        public void startPitActivityResult(String plugin, String activity, boolean result) {
            //result = false;

            if (D) {
                Log.i(TAG, "startPitActivityResult: huang start result=>" + result);
            }

            // 如果已经成功启动过插件，不再重复启动，避免无限循环
            if (isPluginStarting) {
                if (D) {
                    Log.d(TAG, "startPitActivityResult: huang plugin already started, ignore");
                }
                return;
            }

            if (result) {
                Log.i(TAG, "startPitActivityResult: huang not finish main activity=>");
                //finish();
            } else {
                isFirstBoot = false;
                String topActivity = getTopActivity();
                String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, DEFAULT_CLAZZ);
                
                // 检查是否需要重新启动插件
                if ("com.awell.launcher.host.MainActivity".equals(topActivity) || apkClazz.equals(activity)) {
                    // 设置标志位，防止重复调用
                    isPluginStarting = true;
                    Message message = buildPluginMsg(apkClazz);
                    realStartPlugin(message);
                    
                    // 重置标志位（延迟一点，确保启动完成）
                    mainHandle.postDelayed(() -> isPluginStarting = false, 1000);
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
                if (D){
                    Log.d(TAG, "getTopActivity: huang Package: " + packageName + ", Class: " + className);
                }
                return className;
            }
        } catch (Exception e) {
            Log.e(TAG, "getTopActivity: huang error=>" + e.getMessage());
        }
        return null;
    }


}
