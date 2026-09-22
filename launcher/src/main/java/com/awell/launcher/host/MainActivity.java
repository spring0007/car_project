package com.awell.launcher.host;

import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.awell.launcher.host.databinding.SelectLauncherLayoutBinding;
import com.awell.launcher2.Launcher;
import com.awell.launcher2.LauncherApplication;

import com.awell.utils.LogUtil;
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

    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean D = false;
    private final String LAUNCHER_KEY = "persist.sys.launcher.key"; //value : plugin-app/plugin2-app
    private final String LAUNCHER_CLAZZ = "persist.sys.launcher.clazz"; //value : plugin app class name
    private static final String mFreeformPkgSettings = "freeform_app_package_name";
    private final String mExternalPluginPath = "/system/priv-app/";
    private static final String SP_NAME = "launcher_prefs";
    private static final String KEY_FIRST_BOOT = "is_first_boot";
    private SelectLauncherLayoutBinding binding;

    private Handler mainHandle;
    private static boolean isFirstBoot = true;
    private static volatile PluginInfo sLastInfo = null;
    private PluginInfo info;
    // 进程级重试计数: 实例重建(onDestroy->onCreate)不再清零, 否则重试上限形同虚设
    private static int retryCount = 0;
    private static final int MAX_RETRY = 3;               // 最多重试3次
    private static final AtomicBoolean isInstalling = new AtomicBoolean(false);
    
    // 优化4: 缓存已检查过的插件状态，避免重复查询
    private static String lastCheckedPluginName = null;
    private static boolean lastPluginValid = false;

    // 用于延迟finish Host的Runnable
    //private final Runnable finishHostRunnable = this::finish;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.i( "onCreate: huang launcher main activity create==>");

        binding = SelectLauncherLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //initView();
        //startGpsService();
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        isFirstBoot = sp.getBoolean(KEY_FIRST_BOOT, true);
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

        // 新实例恢复进程级最近一次成功的插件: 分屏按 HOME 常新建 MainActivity,
        // 若 info 不恢复, 新实例会误判主题未加载而重复走首启安装(拷贝5.6MB + RePlugin.install)。
        if (info == null && sLastInfo != null) {
            info = sLastInfo;
        }

    }

    /**
     * 安装与启动的入口线程（仅在尚未安装且无其他安装进行时执行）
     */
    private void initInstallThread() {
        if (!isInstalling.compareAndSet(false, true)) {
            LogUtil.w( "initInstallThread: already installing, skip duplicate call");
            return;
        }
        new Thread(() -> {
            try {
                startPluginActivity();
            } finally {
                // 必须与上面的 CAS 成对释放: 若不释放, 后续重试的 CAS 永远失败, 安装流程卡死
                isInstalling.set(false);
            }
        }).start();

    }

    private void realStartPlugin(@NonNull Message msg) {
        
        try {
            // 失败消息(buildFailMessage)的 obj 为 null, 不得覆盖已安装成功的 info,
            // 否则过期的失败消息会把刚装好的 info 清掉, 引发假失败与降级误触发
            if (msg.obj != null) {
                info = (PluginInfo) msg.obj;
            }
            String clazz = Objects.requireNonNull(msg.getData().get("clazz")).toString();
            boolean clearTask = msg.getData().getBoolean("clearTask", false);

            if (info != null) {
                //retryCount = 0;
                String packageName = info.getPackageName();
                LogUtil.i("realStartPlugin: huang start plugin info==" + info + ", clazz==>" + clazz + ",packageName==>" + packageName + ",clearTask==>" + clearTask);

                // 防御：目标类不属于当前插件包 → info 已过期（主题切换后残留），交由重试流程重新解析/安装
                if (!clazz.startsWith(packageName + ".")) {
                    LogUtil.w( "realStartPlugin: clazz=" + clazz + " not in plugin=" + packageName + ", re-resolve");
                    scheduleRetryOrFallback();
                    return;
                }
                rememberPlugin(info);

                long startTime = System.currentTimeMillis();
                Intent intent = RePlugin.createIntent(packageName, clazz);

                // 验证生成的 Intent 是否有效
                if (intent == null || intent.getComponent() == null) {
                    LogUtil.e( "realStartPlugin: huang create intent failed, will reinstall plugin");
                    scheduleRetryOrFallback();
                    return;
                }

                // 禁止添加 putExtra("boot", ...): 定制版 replugin-host-lib 对 boot=true 的 Intent 直接 return false 拒启, 会被误判为启动失败并耗尽重试; 全工程无读取方
                if (clearTask && !"mt6755".equals(Build.HARDWARE)) {
                    // 仅真正(重)安装后才清空旧任务: 保证加载的是本次安装的新插件
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                }
                RePlugin.startActivity(MainActivity.this, intent);
                
                long endTime = System.currentTimeMillis();
                LogUtil.i( "huang start plugin spend time=>" + (endTime - startTime));


                // 延迟销毁宿主，确保插件有机会显示，避免因插件启动失败导致宿主被提前杀死
                //mainHandle.postDelayed(finishHostRunnable, 300);

            } else {
                // 插件信息为空 ↑ 说明安装仍未成功
                LogUtil.e( "realStartPlugin: plugin info is null, will reinstall");
                scheduleRetryOrFallback();
            }
        } catch (Exception e) {
            LogUtil.e("realStartPlugin: exception " + e.getMessage(), e);
            scheduleRetryOrFallback();
        }
    }

    private boolean infoMatchesTargetClazz(String apkClazz) {
        return info != null
                && apkClazz != null
                && apkClazz.startsWith(info.getPackageName() + ".");
    }

    /**
     * 根据重试次数决定是再次尝试安装，还是降级到内置桌面
     */
    private void scheduleRetryOrFallback() {
        if (retryCount >= MAX_RETRY) {
            LogUtil.e( "Max retry reached, fallback to internal launcher");
            fallbackToInternalLauncher();
            return;
        }
        retryCount++;
        LogUtil.w("retry count=" + retryCount);

        String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, "");
        if (infoMatchesTargetClazz(apkClazz)) {
            LogUtil.w("info valid, retry start only (no reinstall)");
            mainHandle.sendMessage(buildPluginMsg(apkClazz, false));
            return;
        }

        // 不强行清锁再起新线程: 若上一个安装线程仍在运行, CAS 会失败并跳过,
        // 该线程结束时会自行发出结果消息, 流程不会丢; 强行清锁会造成两个线程并发装同一个 APK。
        initInstallThread();
    }

    /**
     * 降级到内置桌面，保证设备始终可用
     */
    private void fallbackToInternalLauncher() {
        runOnUiThread(() -> {
            LogUtil.w("fallbackToInternalLauncher: starting internal Launcher");
            sLastInfo = null;
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
            LogUtil.i( "onResume: huang resume start plugin isFirstBoot==>" + isFirstBoot);
        if (!isFirstBoot) {
            String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, "");
            if (infoMatchesTargetClazz(apkClazz)) {
                // info 已指向目标插件 → 直接用缓存 info 启动，避免主题切换时用旧插件包名错启
                Message message = buildPluginMsg(apkClazz);
                realStartPlugin(message);
                return;
            }
        }
        // 首次启动，且没有正在进行中的安装任务时才发起
        if (!isInstalling.get()) {
            initInstallThread();
        } else {
            LogUtil.d( "onResume: installation already in progress, skip duplicate trigger");
        }
    }

    @NonNull
    private Message buildPluginMsg(String apkClazz) {
        return buildPluginMsg(apkClazz, false);
    }

    @NonNull
    private Message buildPluginMsg(String apkClazz, boolean clearTask) {
        Message message = mainHandle.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("clazz", apkClazz);
        bundle.putBoolean("clearTask", clearTask);   // true=刚(重)装完成, 需清空旧任务
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
            LogUtil.i( "onDestroy: launcher main activity destroy==>");
        }
    }

    private void startInternalLauncher() {
        Intent intent = new Intent(this, Launcher.class);
//            Intent intent = new Intent(this, UIActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //startPluginActivity();
        if (isInstalling.get()) {
            LogUtil.d( "onNewIntent: installation in progress, skip");
            return;
        }

        String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, "");
        if (infoMatchesTargetClazz(apkClazz)) {
            // 目标就是当前已加载插件 → 直接 bring to front（保留 NEW_TASK|CLEAR_TOP 语义）
            Intent pluginIntent = RePlugin.createIntent(info.getPackageName(), apkClazz);
            if (pluginIntent != null && pluginIntent.getComponent() != null) {
                pluginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                RePlugin.startActivity(this, pluginIntent);
                LogUtil.d( "onNewIntent: startActivity");
            }
        } else {
            // 主题已切换/首次启动：缓存 info 不匹配新 clazz → 先解析/安装目标插件再启动
            initInstallThread();
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (D) {
            LogUtil.i( "onBackPressed: huang Intercept back==>");
        }
    }


    private synchronized void startPluginActivity() {
        String apkName = SystemProperties.get(LAUNCHER_KEY, "");
        String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, "");

        String testApk = apkName + ".apk";
        String testApkPath = mExternalPluginPath + apkName + File.separator + testApk;
        LogUtil.i( "huang apkName=" + apkName + ", apkClazz=" + apkClazz + ", path=" + testApkPath);
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
            LogUtil.e( "source APK invalid: " + path);
            // 直接判定为失败，交由上层重试或降级
            mainHandle.sendMessage(buildFailMessage());
            return;
        }
        
        // 文件是否已经存在？直接删除重来
        String pluginFileName = name; // 直接使用传入的名称作为文件名
        String pluginFilePath = getFilesDir().getAbsolutePath() + File.separator + pluginFileName;
        File pluginFile = new File(pluginFilePath);
        
        String targetPkg = (clazz != null && clazz.contains("."))
                ? clazz.substring(0, clazz.lastIndexOf('.'))
                : null;

        // 使用缓存避免重复查询
        PluginInfo existingPlugin = null;
        if (targetPkg != null && targetPkg.equals(lastCheckedPluginName)) {
            if (lastPluginValid && info != null) {
                existingPlugin = info;
                LogUtil.i( "huang using cached plugin info");
            }
        } else {
            existingPlugin = findPluginByPackage(targetPkg);
            lastCheckedPluginName = targetPkg;
            lastPluginValid = (existingPlugin != null);
        }

        if (existingPlugin != null) {
            if (retryCount > 0) {
                LogUtil.w( "retry attempt, force reinstall");
                // 清除缓存信息，强制走安装流程
                info = null;
                lastPluginValid = false;
                // 删除已复制的文件
                FileUtils.deleteQuietly(new File(pluginFilePath));
            } else if (clazz.startsWith(existingPlugin.getPackageName() + ".")
                    && isPluginApkCurrent(existingPlugin, sourceApk)) {
                LogUtil.i( "huang plugin already installed & apk matches current source, skip install");
                info = existingPlugin;
                rememberPlugin(info);
                Utils.setPluginApkFilePath(path);
                Message message = buildPluginMsg(clazz, false);
                mainHandle.sendMessage(message);
                return;
            } else if (clazz.startsWith(existingPlugin.getPackageName() + ".")) {
               LogUtil.w( "installed apk outdated vs /system source (OTA v1->v2?). "
                        + "re-copy & reinstall. installed=" + existingPlugin.getPath()
                        + ", srcSize=" + sourceApk.length());
                info = null;
                lastPluginValid = false;
                FileUtils.deleteQuietly(new File(pluginFilePath));
            } else {
                // 已安装插件与目标类不匹配（key 与 clazz 配置不一致，即内部包名/类名已不同），不复用，继续走安装流程
                LogUtil.w( "clazz=" + clazz + " not belong to plugin="
                        + existingPlugin.getPackageName() + ", fall through to install");
            }
        }
        
        // 优化5: 检查本地文件是否已存在且完整，避免重复复制
        File copiedFile = new File(pluginFilePath);
        if (copiedFile.exists() && copiedFile.length() == sourceApk.length()) {
            LogUtil.i( "huang local file exists and is valid, try install directly");
            // 文件已存在且大小一致，直接尝试安装
        } else {
            if (copiedFile.exists()) {
                if (D) {
                    LogUtil.i( "huang delete incomplete existing file==>");
                }
                FileUtils.deleteQuietly(copiedFile);
            }
                
            // 开始复制
            if (!copyAssetsFileToAppFiles(path, pluginFileName)) {
                LogUtil.e( "copy failed");
                mainHandle.sendMessage(buildFailMessage());
                return;
            }
            
            // 验证复制后的文件完整性
            copiedFile = new File(pluginFilePath);
            if (!copiedFile.exists() || copiedFile.length() != sourceApk.length()) {
                LogUtil.e( "copied file validation failed, expected=" +
                      sourceApk.length() + ", actual=" + (copiedFile.exists() ? copiedFile.length() : 0));
                mainHandle.sendMessage(buildFailMessage());
                return;
            }
        }

        info = null;
        
        try {
            // 尝试安装插件
            LogUtil.i( "huang start install, path=" + pluginFilePath + ", size=" + copiedFile.length());
            long installStartTime = System.currentTimeMillis();
            info = RePlugin.install(pluginFilePath);
            long installEndTime = System.currentTimeMillis();
            
            if (info == null) {
                LogUtil.e( "huang install plugin failed after " + (installEndTime - installStartTime) + "ms, file may be corrupted or signature mismatch");
                LogUtil.e( "source apk path=" + path + ", size=" + sourceApk.length());
                LogUtil.e( "copied apk path=" + pluginFilePath + ", size=" + copiedFile.length());
                
                // 更新缓存状态
                lastPluginValid = false;
                sLastInfo = null;
                
                // 检查签名是否匹配
                try {
                    PackageManager pm = getPackageManager();
                    PackageInfo sourceInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES);
                    PackageInfo copiedInfo = pm.getPackageArchiveInfo(pluginFilePath, PackageManager.GET_SIGNATURES);
                    
                    if (sourceInfo != null && copiedInfo != null) {
                        LogUtil.e( "huang source package=" + sourceInfo.packageName +
                              ", copied package=" + copiedInfo.packageName);
                    }
                } catch (Exception sigEx) {
                    LogUtil.e( "huang signature check error", sigEx);
                }
                
                // 安装失败,删除损坏的文件
                FileUtils.deleteQuietly(pluginFile);
            } else {
                LogUtil.i( "huang install success in " + (installEndTime - installStartTime) + "ms, plugin info=" + info);
                // 更新缓存状态 + 记录进程级 info
                lastPluginValid = true;
                rememberPlugin(info);
            }
        } catch (Exception e) {
            LogUtil.e( "huang install exception: " + e.getMessage(), e);
            // 发生异常,删除可能损坏的文件
            FileUtils.deleteQuietly(pluginFile);
            info = null;
            lastPluginValid = false;
        }
    
        Utils.setPluginApkFilePath(path);

        Message message = buildPluginMsg(clazz, true);
        mainHandle.sendMessage(message);
    }

    /**
     * 记录进程级最近一次匹配/安装成功的插件, 供新 MainActivity 实例(onCreate)恢复,
     * 避免同一进程内每次回桌面都重新走首启安装。
     */
    private void rememberPlugin(PluginInfo pi) {
        if (pi != null) {
            sLastInfo = pi;
        }
    }

    /**
     * 按目标类包名查找已安装插件。
     * 只以「真实包名」(getPackageName)为准做对比 —— LauncherUI1.apk 只是外层文件名,
     * 同一文件名可能对应不同版本的内部包名/类名, 因此绝不用文件名/alias 去判定插件身份。
     * (RePlugin.getPluginInfo(pkg) 内部同时按包名与 alias 查表, 这里先用它, 再兜底遍历列表按包名匹配。)
     */
    private PluginInfo findPluginByPackage(String pkg) {
        if (pkg == null) {
            return null;
        }
        PluginInfo pi = RePlugin.getPluginInfo(pkg);
        if (pi != null) {
            return pi;
        }
        List<PluginInfo> all = RePlugin.getPluginInfoList();
        if (all != null) {
            for (PluginInfo p : all) {
                if (p != null && pkg.equals(p.getPackageName())) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * 判断“已安装插件”对应的 apk 是否仍是当前 /system 源上的那一份(内容层面粗校验, 此处用文件大小)。
     * 目的: OTA v1→v2 升级后外层文件名仍为 LauncherUI1.apk, 但内部包名/类名/代码已换代。
     * 若仅靠文件名命中就复用, 会加载到 OTA 前的旧插件; 源文件大小变了即视为需要重装最新版。
     */
    private boolean isPluginApkCurrent(PluginInfo pi, File sourceApk) {
        if (pi == null || sourceApk == null || !sourceApk.exists()) {
            return false;
        }
        String apkPath = pi.getPath();
        if (apkPath == null || apkPath.isEmpty()) {
            return false;
        }
        File installedApk = new File(apkPath);
        if (!installedApk.exists()) return false;
        // 先比较大小快速判断，若大小相同再比较 MD5（可选）
        if (installedApk.length() != sourceApk.length()) return false;
        // 如果不希望增加耗时，可只比较大小；若想更可靠，取消注释以下代码：
        //String installedMd5 = getFileMD5(installedApk);
        //String sourceMd5 = getFileMD5(sourceApk);
        //return installedMd5 != null && installedMd5.equals(sourceMd5);
        return true; // 只比较大小
    }

    private String getFileMD5(File file) {
        if (file == null || !file.exists()) return null;
        try (InputStream is = new FileInputStream(file)) {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            LogUtil.e("getFileMD5 error", e);
            return null;
        }
    }


    private Message buildFailMessage() {
        Message msg = mainHandle.obtainMessage();
        msg.what = 0x01;
        Bundle data = new Bundle();
        data.putString("clazz", SystemProperties.get(LAUNCHER_CLAZZ, ""));
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
            LogUtil.e( "copyAssetsFileToAppFiles: source file not found: " + assetFileName);
            return false;
        }
        
        // 先写临时文件, 校验通过后再原子改名为目标文件:
        // 避免并发场景下读方看到写了一半/被截断的 APK(RePlugin READ_PKG_INFO_FAIL)
        final String tmpName = newFileName + ".tmp";
        File destFile = new File(getFilesDir(), newFileName);
        File tmpFile = new File(getFilesDir(), tmpName);

        FileOutputStream fos = null;
        InputStream is = null;
        try {
            is = new FileInputStream(srcFile);
            fos = this.openFileOutput(tmpName, Context.MODE_PRIVATE);

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
            LogUtil.d( "copyAssetsFileToAppFiles: copy success in " + (endTime - startTime) + "ms, total bytes=" + totalBytes + ", source size=" + srcFile.length());
            
            // 验证文件大小是否一致
            if (tmpFile.length() != srcFile.length()) {
                LogUtil.e( "copyAssetsFileToAppFiles: file size mismatch! source=" + srcFile.length() +
                      ", dest=" + tmpFile.length());
                return false;
            }

            // 校验通过才覆盖目标文件, 同一目录内 renameTo 为原子操作, 读方不会读到半截文件
            FileUtils.deleteQuietly(destFile);
            return tmpFile.renameTo(destFile);
        } catch (Exception e) {
            LogUtil.e( "copyAssetsFileToAppFiles: error", e);
            return false;
        } finally {
            // 确保资源被正确关闭
            try {
                if (fos != null) fos.close();
            } catch (Exception e) {
                LogUtil.e( "copyAssetsFileToAppFiles: error closing fos", e);
            }
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                LogUtil.e( "copyAssetsFileToAppFiles: error closing is", e);
            }
            // 成功时 tmpFile 已被改名, 此处为空操作; 失败时清掉残留的半截临时文件
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    private String lastFailedPlugin = "";
    private final LauncherApplication.PluginStartStatus pluginStartStatus = new LauncherApplication.PluginStartStatus() {
        @Override
        public void startPitActivityResult(String plugin, String activity, boolean result) {
            LogUtil.i( "plugin=" + plugin + ", activity=" + activity + ", result=" + result);

            if (result) {
                LogUtil.i( "plugin started successfully");
                retryCount = 0;
                isFirstBoot = false;
                isInstalling.set(false);
                getSharedPreferences(SP_NAME, MODE_PRIVATE)
                        .edit().putBoolean(KEY_FIRST_BOOT, false).apply();
                lastFailedPlugin="";
            } else {
                LogUtil.w( "startPitActivityResult: huang plugin start failed, will retry");
                isFirstBoot = false;
                String failKey = plugin + "@" + activity;
                if (failKey.equals(lastFailedPlugin)) {
                    LogUtil.w( "startPitActivityResult: duplicate callback ignored");
                    return;
                }
                lastFailedPlugin = failKey;

                // 检查当前栈顶Activity，避免在非MainActivity时重复启动
                String topActivity = getTopActivity();
                String apkClazz = SystemProperties.get(LAUNCHER_CLAZZ, "");
                
                if ("com.awell.launcher.host.MainActivity".equals(topActivity) || apkClazz.equals(activity)) {
                    LogUtil.i( "startPitActivityResult: huang top is "+topActivity+", retry with current info");
                    // 使用当前的info和clazz重试
                     scheduleRetryOrFallback();
                } else {
                    LogUtil.i( "startPitActivityResult: huang top activity changed to: " + topActivity + ", no need to retry");
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

                LogUtil.d( "getTopActivity: huang Package: " + packageName + ", Class: " + className);

                return className;
            }
        } catch (Exception e) {
            LogUtil.e( "getTopActivity: huang error=>" + e.getMessage());
        }
        return null;
    }


}
