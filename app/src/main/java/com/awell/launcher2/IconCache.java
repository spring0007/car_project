/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.awell.launcher2;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.HashMap;

import com.awell.control.AppsCustomizeControl;
import com.awell.launcher.library.R;
import com.awell.utils.Utils;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {
    @SuppressWarnings("unused")
    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static class CacheEntry {
        public Bitmap icon;
        public String title;
    }

    static Drawable bmp = null;
    static public final String WorkSpacePackageName[] = {
            "com.acloud.stub.localradio",
            "com.acloud.stub.cdplay",
            "com.acloud.stub.newonlinemusic",
            "com.acloud.stub.video",
            "com.acloud.stub.localmusic",
            "com.acloud.stub.newonlineradio",
            "com.autonavi.xmgd.navigator",
            "com.acloud.stub.news",
            "com.autochips.bluetooth",
            "com.pve.onekeysos",
            "com.android.browser",
            "com.autochips.avin",
            "com.autochips.HDMI",
    };
    static public final String SettingsPackageName[] = {
            "com.pve.xysecurity",
            "com.pve.wifi",
            "com.pve.gpsinfo",
            "com.acloud.stub.onekeyclean",
            "com.android.xy.volumesetting",
            "com.android.xysysteminfo",
            "com.pve.time",
            "com.pve.wallpaper",
            "com.pve.sysrestore",
            "com.xy.brightsetting",
            "com.pve.language",
            "com.pve.aps",
            "com.pve.steering",
            "com.pve.logoselector",
            "com.awell.pvcanset",
            "com.xy.usbsettings",
            "com.xy.screensettings",
            "com.xy.avoutsettings",
            "com.acloud.stub.localradio",
            "com.acloud.stub.cdplay",
            "com.acloud.stub.newonlinemusic",
            "com.acloud.stub.video",
            "com.acloud.stub.localmusic",
            "com.acloud.stub.newonlineradio",
            "com.autonavi.xmgd.navigator",
            "com.acloud.stub.news",
            "com.autochips.bluetooth",
            "com.pve.onekeysos",
            "com.android.browser",
            "com.autochips.avin",
            "com.autochips.HDMI",
            "cld.navi.c2739.mainframe",
            "com.android.settings",
            "com.pve.naviguide",
    };

    static public final String[] mHomePackageName_lehang = {
            "com.awell.localvideo"
            , "com.awell.localmusic"
            , "com.awell.radio"
            , "com.awell.bluetooth"

            , "com.awell.navigation"
            , "com.android.dialer"
            , "com.android.calculator2"
            , "com.android.browser"

            , "com.mediatek.filemanager"
            , "com.android.documentsui"
            , "com.awell.carsetting"
            , "com.awell.backcar"

            , "cn.kuwo.kwmusiccar"
            , "com.awell.eqselect"
            , "com.awell.canbus"
            , "com.awell.canbus2"

            , "com.android.mms"
            , "com.autonavi.amapauto"
         //   , "com.txznet.txzsetting"
            , "com.tima.carnet.vt"

            , "net.easyconn"
            , "com.zjinnova.zlink"
            , "com.awell.awellmanual"
            , "com.google.android.apps.maps"

            , "com.google.android.youtube"
            , "com.android.vending"
            , "com.android.chrome"
            , "com.android.soundrecorder"

            , "com.android.calendar"
            , "com.android.deskclock"
            , "com.android.contacts"
            , "com.android.gallery3d"

            , "com.mediatek.camera"
            , "org.chromium.chrome"
            , "com.android.quicksearchbox"
            , "com.android.stk"

            , "com.android.settings"
            , "com.awell.electricfan"
            , "com.awell.frontvideo"
            , "com.awell.keylight"
            , "com.awell.keystudy"
            , "com.awell.themesetting"
            , "com.ms.ms2160"
    };

    public final int mHomePackageIcon_116_lehang_2[] = {
            R.drawable.sf_video
            , R.drawable.sf_music
            , R.drawable.sf_radio
            , R.drawable.sf_bt

            , R.drawable.sf_navi
            , R.drawable.sf_iphone
            , R.drawable.sf_jisuanqi
            , R.drawable.sf_liulanqi

            , R.drawable.sf_wjgl
            , R.drawable.sf_wjgl
            , R.drawable.sf_shezhi
            , R.drawable.sf_wjsr

            , R.drawable.kuwoyinyue
            , R.drawable.sf_jhq
            , R.drawable.sf_yuanche
            , R.drawable.sf_yuanche

            , R.drawable.sf_message
            , R.drawable.sf_gaode
         //   , R.drawable.sf_ggvoice
            , R.drawable.sf_zlink

            , R.drawable.sf_zlink
            , R.drawable.sf_zlink
            , R.drawable.sf_manual
            , R.drawable.sf_googlemap

            , R.drawable.sf_youtube
            , R.drawable.sf_playstore
            , R.drawable.sf_safri
            , R.drawable.sf_soundrecorder

            , R.drawable.sf_calender
            , R.drawable.sf_clock
            , R.drawable.sf_contact
            , R.drawable.sf_gallery

            , R.drawable.sf_camera
            , R.drawable.sf_chrome
            , R.drawable.sf_search
            , R.drawable.sf_simtools

            , R.drawable.sf_system_setting
            , R.drawable.sf_fan
            , R.drawable.sf_front_video
            , R.drawable.sf_color_light
            , R.drawable.sf_training
            , R.drawable.sf_theme
            , R.drawable.sf_usb_video_output
    };

    public final int mHomePackageIcon_116_lehang_2_dark[] = {
            R.drawable.sf_video_dark
            , R.drawable.sf_music_dark
            , R.drawable.sf_radio_dark
            , R.drawable.sf_bt_dark

            , R.drawable.sf_navi_dark
            , R.drawable.sf_iphone
            , R.drawable.sf_jisuanqi
            , R.drawable.sf_liulanqi_dark

            , R.drawable.sf_wjgl_dark
            , R.drawable.sf_wjgl_dark
            , R.drawable.sf_shezhi_dark
            , R.drawable.sf_wjsr_dark

            , R.drawable.kuwoyinyue_dark
            , R.drawable.sf_jhq_dark
            , R.drawable.sf_yuanche_dark
            , R.drawable.sf_yuanche_dark

            , R.drawable.sf_message
            , R.drawable.sf_gaode_dark
          //  , R.drawable.sf_ggvoice
            , R.drawable.sf_zlink_dark

            , R.drawable.sf_zlink_dark
            , R.drawable.sf_zlink_dark
            , R.drawable.sf_manual_dark
            , R.drawable.sf_googlemap_dark

            , R.drawable.sf_youtube_dark
            , R.drawable.sf_playstore_dark
            , R.drawable.sf_safri_dark
            , R.drawable.sf_soundrecorder

            , R.drawable.sf_calender
            , R.drawable.sf_clock
            , R.drawable.sf_contact
            , R.drawable.sf_gallery_dark

            , R.drawable.sf_camera
            , R.drawable.sf_chrome_dark
            , R.drawable.sf_search
            , R.drawable.sf_simtools

            , R.drawable.sf_system_setting
            , R.drawable.sf_fan_dark
            , R.drawable.sf_front_video_dark
            , R.drawable.sf_color_light_dark
            , R.drawable.sf_training_dark
            , R.drawable.sf_theme_dark
            , R.drawable.sf_usb_video_output_dark
    };

    public final int mHomePackageIcon_116_lehang_2_yellow[] = {
            R.drawable.sf_video_yellow
            , R.drawable.sf_music_yellow
            , R.drawable.sf_radio_yellow
            , R.drawable.sf_bt_yellow

            , R.drawable.sf_navi_yellow
            , R.drawable.sf_iphone
            , R.drawable.sf_jisuanqi
            , R.drawable.sf_liulanqi_yellow

            , R.drawable.sf_wjgl_yellow
            , R.drawable.sf_wjgl_yellow
            , R.drawable.sf_shezhi_yellow
            , R.drawable.sf_wjsr_yellow

            , R.drawable.kuwoyinyue_yellow
            , R.drawable.sf_jhq_yellow
            , R.drawable.sf_yuanche_yellow
            , R.drawable.sf_yuanche_yellow

            , R.drawable.sf_message
            , R.drawable.sf_gaode_yellow
       //     , R.drawable.sf_ggvoice
            , R.drawable.sf_zlink_yellow

            , R.drawable.sf_zlink_yellow
            , R.drawable.sf_zlink_yellow
            , R.drawable.sf_manual_yellow
            , R.drawable.sf_googlemap_yellow

            , R.drawable.sf_youtube_yellow
            , R.drawable.sf_playstore_yellow
            , R.drawable.sf_safri_yellow
            , R.drawable.sf_soundrecorder

            , R.drawable.sf_calender
            , R.drawable.sf_clock
            , R.drawable.sf_contact
            , R.drawable.sf_gallery_yellow

            , R.drawable.sf_camera
            , R.drawable.sf_chrome_yellow
            , R.drawable.sf_search
            , R.drawable.sf_simtools

            , R.drawable.sf_system_setting
            , R.drawable.sf_fan_yellow
            , R.drawable.sf_front_video_yellow
            , R.drawable.sf_color_light_yellow
            , R.drawable.sf_training_yellow
            , R.drawable.sf_theme_yellow
            , R.drawable.sf_usb_video_output_yellow
    };

    private final Bitmap mDefaultIcon;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final HashMap<ComponentName, CacheEntry> mCache =
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    private int mIconDpi;
    private int themeMode = 0;

    public IconCache(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mIconDpi = activityManager.getLauncherLargeIconDensity();

        // need to set mIconDpi before getting default icon
        mDefaultIcon = makeDefaultIcon();
    }

    public void setPluginThemeMode(int themeMode) {
        // 当themeMode发生变化时
        if (this.themeMode != themeMode) {
            this.themeMode = themeMode;
            Log.d(TAG, "setPluginThemeMode: themeMode changed to " + themeMode);

            // 清除缓存，这样下次获取图标时会重新加载
            flush();

            // 通知LauncherModel刷新图标 - 需要通过LauncherApplication获取
            if (mContext instanceof LauncherApplication) {
                LauncherApplication app = (LauncherApplication) mContext;
                LauncherModel model = app.getModel();
                if (model != null) {
                    // 强制重置加载状态，确保重新加载所有应用
                    model.resetLoadedState(true, false);
                    // 触发重新加载所有应用图标
                    model.startLoader(true, -1);
                }
            }
        }
    }

    private int getIconResource(int index) {
       // return themeMode == 0 ? mHomePackageIcon_116_lehang_2_dark[index] : mHomePackageIcon_116_lehang_2[index];
        if (themeMode == 0){
            return mHomePackageIcon_116_lehang_2_dark[index];
        } else if (themeMode == 1){
            return mHomePackageIcon_116_lehang_2[index];
        }else if (themeMode == 2){
            return mHomePackageIcon_116_lehang_2_yellow[index];
        }
        return mHomePackageIcon_116_lehang_2[index];
    }


    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.mipmap.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawable(iconId);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ResolveInfo info) {
        return getFullResIcon(info.activityInfo);
    }

    public Drawable getFullResIcon(ActivityInfo info) {

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
            mCache.remove(componentName);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
            mCache.clear();
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(ApplicationInfo application, ResolveInfo info,
                                HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info, labelCache);
            String packageName = info.activityInfo.applicationInfo.packageName;
            application.title = entry.title;
            boolean customIconSet = false;
            for (int i = 0; i < mHomePackageIcon_116_lehang_2.length; i++) {
                Utilities.FLAG = false;
                if (packageName.contains(mHomePackageName_lehang[i])) {
                    bmp = getFullResIcon(mContext.getResources(), getIconResource(i));
                    application.iconBitmap = Utilities.createIconBitmap(bmp, mContext, packageName);
                    entry.icon = application.iconBitmap;
                    customIconSet = true;
                    break;
                }
//                else {
//                    application.iconBitmap = entry.icon;
//                }
            }
            if (!customIconSet) {
                application.iconBitmap = entry.icon;
            }
        }
    }

    public Bitmap getIcon(Intent intent) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();

            if (resolveInfo == null || component == null) {
                return mDefaultIcon;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, null);
            String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
            for (int i = 0; i < mHomePackageIcon_116_lehang_2.length; i++) {
                int nIndex = Utilities.isContains(packageName, mHomePackageName_lehang);

                if (-1 != nIndex) {
                    Drawable bmp = getFullResIcon(mContext.getResources(), getIconResource(nIndex));
                    entry.icon = Utilities.createIconBitmap(bmp, mContext, packageName);
                }
            }

            return entry.icon;
        }
    }

    public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo,
                          HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, labelCache);
            String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
            for (int i = 0; i < mHomePackageIcon_116_lehang_2.length; i++) {
                if (packageName.contains(mHomePackageName_lehang[i])) {
                    bmp = getFullResIcon(mContext.getResources(),
                            getIconResource(i));
                    entry.icon = Utilities.createIconBitmap(bmp, mContext, packageName);
                break;
                }
            }
            return entry.icon;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return mDefaultIcon == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info,
                                   HashMap<Object, CharSequence> labelCache) {
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(componentName, entry);     //(应用名------图片+标题)

            ComponentName key = LauncherModel.getComponentNameFromResolveInfo(info);
            if (labelCache != null && labelCache.containsKey(key)) {
                entry.title = labelCache.get(key).toString();
            } else {
                entry.title = info.loadLabel(mPackageManager).toString();
                if (labelCache != null) {
                    labelCache.put(key, entry.title);
                }
            }

            if (entry.title == null) {
                entry.title = info.activityInfo.name;
            }

            entry.icon = Utilities.createIconBitmap(
                    getFullResIcon(info), mContext, "");
        }
        return entry;
    }

    public HashMap<ComponentName, Bitmap> getAllIcons() {
        synchronized (mCache) {
            HashMap<ComponentName, Bitmap> set = new HashMap<ComponentName, Bitmap>();
            for (ComponentName cn : mCache.keySet()) {
                final CacheEntry e = mCache.get(cn);
                set.put(cn, e.icon);
            }
            return set;
        }
    }
}
