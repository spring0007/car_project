/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.awell.plugin_shadow;

import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.awell.launcher.BuildConfig;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PluginHelper {

    /**
     * 动态加载的插件管理apk
     */
    public final static String sPluginManagerName = "pluginmanager.apk";

    /**
     * 动态加载的插件包，里面包含以下几个部分，插件apk，插件框架apk（loader apk和runtime apk）, apk信息配置关系json文件
     */
    public final static String sPluginZip = BuildConfig.DEBUG ? "plugin-debug.zip" : "plugin-release.zip";


    public File pluginManagerFile;

    public File pluginZipFile;

    public ExecutorService singlePool = Executors.newSingleThreadExecutor();

    private Context mContext;

    private static PluginHelper sInstance = new PluginHelper();
    private final String TAG = PluginHelper.class.getSimpleName();

    public static PluginHelper getInstance() {
        return sInstance;
    }

    private PluginHelper() {
    }

    public void init(Context context) {
        pluginManagerFile = new File(context.getFilesDir(), sPluginManagerName);
        pluginZipFile = new File(context.getFilesDir(), sPluginZip);

        mContext = context.getApplicationContext();

        singlePool.execute(new Runnable() {
            @Override
            public void run() {
                preparePlugin();
            }
        });

    }

    public void checkDebugStatus() {
        boolean isDebuggable = (getmAppContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        Log.e("DEBUG_CHECK", "BuildConfig.DEBUG = " + BuildConfig.DEBUG);
        Log.e("DEBUG_CHECK", "isDebuggable = " + isDebuggable);
        Log.e("DEBUG_CHECK", "Build type = " + BuildConfig.BUILD_TYPE);
    }

    private void preparePlugin() {
        checkDebugStatus();

        String path = "/sdcard/launcher_plugin";

        File managerApk = new File(path, sPluginManagerName);
        File zipPath = new File(path, sPluginZip);
        try {
            //noinspection ResultOfMethodCallIgnored
            pluginManagerFile.setWritable(true);
//            InputStream is = new FileInputStream(managerApk);
//            InputStream zip = new FileInputStream(zipPath);
            String[] fileList = mContext.getAssets().list(""); // 根目录
            Log.i(TAG, "preparePlugin: huang file list=>" + Arrays.toString(fileList));
            InputStream is = mContext.getAssets().open(sPluginManagerName);
            Log.i(TAG, "preparePlugin: huang finish open plugin sPluginManagerName=>" + sPluginManagerName);
            Log.i(TAG, "preparePlugin: huang build type =>" + BuildConfig.DEBUG);
            InputStream zip = mContext.getAssets().open("plugin-debug.zip");
//            InputStream zip = mContext.getAssets().open(sPluginZip);
            Log.i(TAG, "preparePlugin: huang finish open plugin sPluginZip=>" + sPluginZip);

            FileUtils.copyInputStreamToFile(is, pluginManagerFile);
            FileUtils.copyInputStreamToFile(zip, pluginZipFile);

        } catch (IOException e) {
            throw new RuntimeException("复制apk出错", e);
        }
    }


}
