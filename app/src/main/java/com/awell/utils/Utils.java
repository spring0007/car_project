package com.awell.utils;

import static com.awell.launcher2.Launcher.INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION;
import static com.awell.launcher2.LauncherApplication.getmAppContext;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.awell.addapp.AppInfo;
import com.awell.launcher.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();
    /**
     * 不需要显示的应用
     */
    public static List<String> filterAppPackageName = Arrays.asList("com.mediatek.gba"
            , "com.mediatek.ims"
            , "com.mediatek.ppl"
            , "com.android.cts.priv.ctsshim"
            , "com.android.providers.telephony"
            , "com.android.providers.calendar"
            , "com.android.providers.media"
            , "com.mediatek.fwk.plugin"
            , "com.mediatek.location.lppe.main"
            , "com.android.wallpapercropper"
            , "com.android.launcher"
            , "cn.awell.factorytest"
            , "com.mediatek.photopicker"
            , "com.android.documentsui"
            , "android.auto_generated_rro__"
            , "com.android.externalstorage"
            , "com.mediatek.ygps"
            , "com.mediatek.simprocessor"
            , "com.android.htmlviewer"
            , "com.android.companiondevicemanager"
            , "com.android.mms.service"
            , "com.android.providers.downloads"
            , "com.awell.testmode"
            , "com.awell.interfacesettings"
            , "com.mediatek.engineermode"
            , "com.mediatek.omacp"
            , "com.android.providers.applications"
            , "com.mediatek.galleryfeature.stereo.segment.copypaste"
            , "com.android.soundrecorder"
            , "com.mediatek.wfo.impl"
            , "com.android.defcontainer"
            , "com.android.providers.downloads.ui"
            , "com.android.pacprocessor"
            , "com.android.certinstaller"
            , "com.android.carrierconfig"
            , "android"
            , "com.android.contacts"
            , "com.android.mms"
            , "com.android.mtp"
            , "com.android.backupconfirm"
            , "com.mediatek.security"
            , "com.mediatek.bluetooth.dtt"
            , "com.txznet.txzsetting"
            , "com.android.provision"
            , "com.android.statementservice"
            , "com.mediatek.mdmlsample"
            , "com.mediatek.providers.drm"
            , "com.txznet.txz"
            , "com.android.providers.settings"
            , "com.android.sharedstoragebackup"
            , "com.txznet.adapter"
            , "com.android.printspooler"
            , "com.android.dreams.basic"
            , "com.android.webview"
            , "com.android.inputdevices"
            , "com.mediatek"
            , "com.example"
            , "com.mediatek.galleryfeature.stereo.segment.background"
            , "com.awell.screensaver"
            , "android.ext.shared"
            , "com.android.onetimeinitializer"
            , "com.awell.keystudy"
            , "com.android.server.telecom"
            , "com.awell.eqselect"
            , "com.mediatek.fancycolor"
            , "com.android.keychain"
            , "com.android.printservice.recommendation"
            , "com.android.galleryd"
            , "android.ext.services"
            , "com.android.calllogbackup"
            , "com.android.packageinstaller"
            , "com.svox.pico"
            , "com.awell.brake"
            , "com.android.proxyhandler"
            , "com.android.inputmethod.latin"
            , "com.android.managedprovisioning"
            , "cn.awell.awellplatformservice"
            , "com.mediatek.refocus"
            , "com.mediatek.calendarimporter"
            , "com.mediatek.thermalmanager"
            , "com.mediatek.callrecorder"
            , "com.awell.service"
            , "com.mediatek.webview"
            , "com.android.providers.partnerbookmarks"
            , "com.android.smspush"
            , "com.mtk.telephony"
            , "com.awell.allappinfo"
            , "com.android.storagemanager"
            , "com.android.bookmarkprovider"
            , "com.mediatek.mdmconfig"
            , "com.mediatek.lbs.em.ui"
            , "com.android.cts.ctsshim"
            , "com.mediatek.wifitest"
            , "com.awell.logoselect"
            , "com.android.vpndialogs"
            , "com.android.phone"
            , "com.android.shell"
            , "com.android.wallpaperbackup"
            , "com.android.providers.blockednumber"
            , "com.android.providers.userdictionary"
            , "com.android.emergency"
            , "com.android.location.fused"
            , "com.awell.floatbar"
            , "com.android.systemui"
            , "com.android.bluetoothmidiservice"
            , "com.iflytek.inputmethod.pad"
            , "com.mediatek.mtklogger"
            , "com.awell.factory"
            , "com.android.bluetooth"
            , "com.android.providers.contacts"
            , "com.android.captiveportallogin"
            , "com.mediatek.mtklogger.proxy"
            , "com.mediatek.dataprotection"
            , "com.mediatek.dataprotection"
            , "com.awell.soundeffect"
            , "com.awell.dspeffect"
            , "com.mediatek.lbs.em2.ui"
            , "com.android.dreams.basic"
            , "com.awell.navigation"
            , "com.txznet.txzsetting"
            , "com.txznet.adapter"
            , "com.android.gallery3d"
            , "org.simalliance.openmobileapi.uicc1terminal"
            , "org.simalliance.openmobileapi.uicc2terminal"
            , "org.simalliance.openmobileapi.eseterminal"
            , "org.simalliance.openmobileapi.uiccterminal"
            , "org.simalliance.openmobileapi.service"
            , "org.simalliance.openmobileapi.uiccterminal"
    );
    /*com.android.dreams.basic*/

    /**
     * 需要显示的应用
     */
    public static List<String> needToShowPackageName = Arrays.asList(
            "com.awell.carsetting", "com.android.browser", "com.android.calendar",
            "com.android.dialer", "com.android.calculator2",
            "com.android.deskclock", "com.mediatek.filemanager",
            "com.android.documentsui", "com.autonavi.amapauto",
            "com.awell.radio", "com.awell.localmusic", "com.awell.localvideo",
            "com.awell.backcar", "cn.kuwo.kwmusiccar", "com.awell.bluetooth",
//            "com.android.chrome","com.google.android.youtube","com.google.android.apps.maps","com.android.vending",
            "com.awell.canbus", "com.tima.carnet.vt", "com.android.mms", "com.zjinnova.zlink",
            "com.awell.eqselect", "com.awell.awellmanual"
    );
    /**
     * 需要显示的应用
     */
    public static List<String> otherNeedToShowPackageName = Arrays.asList("com.android.chrome", "com.google.android.youtube",
            "com.google.android.apps.maps", "com.android.vending");

    /**
     * 通过包名查找 APP信息
     *
     * @param packageName
     * @param allAppInfoList
     * @return
     */
    public static AppInfo getAppInfoFromPackage(String packageName, List<AppInfo> allAppInfoList) {
        Log.e("Log_Utils", "packageName = " + packageName);
        AppInfo getAppInfo = null;
        for (AppInfo appInfo : allAppInfoList)
            if (appInfo.package_name.equals(packageName))
                getAppInfo = appInfo;
        return getAppInfo;
    }

    /**
     * 默认显示的应用
     */
    public static String[] defaultShowApp = {
            "com.google.android.apps.maps",
            "com.google.android.youtube",
            "com.android.vending",
            "com.android.chrome",
            "com.awell.localmusic"};


    /**
     * 默认显示的应用
     */
    public static String[] defaultShowApp2 = {"com.android.dialer"};
    /**
     * 需要更换应用icon的应用包名
     */
    public static final String[] mHomePackName = {
            "com.awell.localvideo"
            , "com.awell.localmusic"
            , "com.awell.radio"
            , "com.awell.bluetooth"
//            , "com.awell.navigation"
            , "com.android.dialer"
            , "com.android.calculator2"
            , "com.android.browser"
            //           , "com.android.calendar"
            //           , "com.android.soundrecorder"
            //           , "com.android.deskclock"
//            , "com.android.gallery3d"
            , "com.mediatek.filemanager"
            , "com.android.documentsui"
            , "com.awell.carsetting"
            , "com.awell.backcar"
            , "cn.kuwo.kwmusiccar"
//			, "com.awell.soundeffect"
            , "com.awell.eqselect"
            , "com.awell.canbus"
            , "com.awell.canbus2"
            , "com.android.mms"
            , "com.autonavi.amapauto"
            , "com.txznet.txzsetting"
            , "com.tima.carnet.vt"
            , "net.easyconn"
            , "com.zjinnova.zlink"
            , "com.awell.awellmanual"
            , "com.google.android.apps.maps"
            , "com.google.android.youtube"
            , "com.android.vending"
            , "com.android.chrome"
            , "com.tinyapp.smartcar"
            , "com.awell.update"
            , "com.google.android.googlequicksearchbox"
            , "com.kugou.android.auto"
            , "com.qiyi.video.pad"
            , "com.tencent.qqmusic"
            , "com.tencent.qqlive.audiobox"
    };

    /**
     * 需要更换应用icon的资源文件
     */
    public static final int mHomeIcon[] = {
            R.drawable.sf_video
            , R.drawable.sf_music
            , R.drawable.sf_radio
            , R.drawable.sf_bt
//            , R.drawable.sf_navi
            , R.drawable.sf_iphone
            , R.drawable.sf_jisuanqi
            , R.drawable.sf_liulanqi
//            , R.drawable.sf_rili       //
//            , R.drawable.sf_luyinji     //
//            , R.drawable.sf_shizhong    //
//            , R.drawable.sf_tuku        //
            , R.drawable.sf_wjgl
            , R.drawable.sf_download
            , R.drawable.sf_shezhi
            , R.drawable.sf_wjsr
            , R.drawable.kuwoyinyue
            , R.drawable.sf_jhq
            , R.drawable.sf_yuanche
            , R.drawable.sf_yuanche
            , R.drawable.sf_message
            , R.drawable.sf_navi
            , R.drawable.sf_ggvoice
            , R.drawable.sf_zlink
            , R.drawable.sf_zlink
            , R.drawable.sf_zlink
            , R.drawable.sf_manual
            , R.drawable.sf_googlemap
            , R.drawable.sf_youtube
            , R.drawable.sf_playstore
            , R.drawable.sf_safri
            , R.drawable.sf_ggvoice
            , R.drawable.sf_store
            , R.drawable.sf_gg
            , R.drawable.sf_kugou
            , R.drawable.sf_aiqitv
            , R.drawable.sf_qqyinyue
            , R.drawable.sf_tenxuntv

    };

    /**
     * 需要更换应用icon的资源文件
     */
    public static final int mHomeIcon2[] = {
            R.drawable.sf_video
            , R.drawable.sf_music
            , R.drawable.sf_radio
            , R.drawable.sf_bt
//            , R.drawable.h_navi
            , R.drawable.sf_iphone
            , R.drawable.sf_jisuanqi
            , R.drawable.sf_liulanqi
//            , R.drawable.h_rili       //
//            , R.drawable.h_luyinji     //
//            , R.drawable.h_shizhong    //
//            , R.drawable.h_tuku        //
            , R.drawable.sf_wjgl
            , R.drawable.sf_download
            , R.drawable.sf_shezhi
            , R.drawable.sf_wjsr
            , R.drawable.kuwoyinyue
            , R.drawable.sf_jhq
            , R.drawable.sf_yuanche
            , R.drawable.sf_yuanche
            , R.drawable.sf_message
            , R.drawable.sf_navi
            , R.drawable.sf_ggvoice
            , R.drawable.sf_zlink
            , R.drawable.sf_zlink
            , R.drawable.sf_zlink
            , R.drawable.sf_manual
            , R.drawable.sf_googlemap
            , R.drawable.sf_youtube
            , R.drawable.sf_playstore
            , R.drawable.sf_safri
            , R.drawable.sf_ggvoice
            , R.drawable.sf_store
            , R.drawable.sf_gg
            , R.drawable.sf_kugou

    };

    public static final Set<String> mAppLevel_1 = Set.of(
            "com.awell.localmusic",
            "com.awell.bluetooth"
    );
    public static final Set<String> mAppLevel_2 = Set.of(
            "com.awell.radio",
            "com.awell.localvideo"
    );

    public static final Set<String> mAppLevel_3 = Set.of(
            "com.zjinnova.zlink"
    );

    public static final Set<String> mAppLevel_4 = Set.of(
            "com.awell.eqselect"
    );

    public static final Set<String> mAppLevel_5 = Set.of(
            "com.mediatek.filemanager"
    );

    public static final Set<String> mAppLevel_6 = Set.of(
            "com.awell.canbus",
            "com.awell.carsetting"
    );


    /**
     * icon背景图
     */
//    public static int[] iconBg = {R.drawable.sf_a_bg,R.drawable.sf_b_bg,R.drawable.sf_c_bg,
//            R.drawable.sf_d_bg,R.drawable.sf_e_bg};
    public static boolean startActivity(View v, Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            // Only launch using the new animation if the shortcut has not opted
            // out (this is a
            // private contract between launcher and may be ignored in the
            // future).
            boolean useLaunchAnimation = (v != null) && !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
            if (useLaunchAnimation) {
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

                getmAppContext().startActivity(intent, opts.toBundle());
            } else {
                getmAppContext().startActivity(intent);
            }
            return true;
        } catch (SecurityException e) {
            Toast.makeText(getmAppContext(), R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity " + "or use the exported attribute for this activity. " + "tag=" + tag + " intent=" + intent, e);
        }
        return false;
    }

    public static boolean startActivitySafely(View v, Intent intent, Object tag) {
        boolean success = false;
        try {
            success = startActivity(v, intent, tag);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getmAppContext(), R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }

    public static void startWallpaper() {
//        showWorkspace(true);
        Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        List<ResolveInfo> lists = getmAppContext().getPackageManager().queryIntentActivities(pickWallpaper, 0);
        List<Intent> listIntent = new ArrayList<>();
        pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        pickWallpaper.setPackage(getmAppContext().getPackageName());
        listIntent.add(pickWallpaper);
        for (ResolveInfo info : lists) {
            String pkgName = info.activityInfo.packageName;
            Log.d(TAG, pkgName);
            if (!pkgName.contains("launcher")) {
                pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
                pickWallpaper.setPackage(pkgName);
                listIntent.add(pickWallpaper);
            }
        }
        Intent chooser = Intent.createChooser(new Intent(), getmAppContext().getText(R.string.chooser_wallpaper));
        // NOTE: Adds a configure option to the chooser if the wallpaper supports Removed in Eclair MR1
        // WallpaperManager wm = (WallpaperManager) getSystemService(Context.WALLPAPER_SERVICE);
        // WallpaperInfo wi = wm.getWallpaperInfo();
        // if (wi != null && wi.getSettingsActivity() != null) {
        // LabeledIntent li = new LabeledIntent(getPackageName(),
        // R.string.configure_wallpaper, 0);
        // li.setClassName(wi.getPackageName(), wi.getSettingsActivity());
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, listIntent.toArray(new Parcelable[]{}));
        // }
        startActivity(null, chooser, "startWallpaper");
        //startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);

    }
}

