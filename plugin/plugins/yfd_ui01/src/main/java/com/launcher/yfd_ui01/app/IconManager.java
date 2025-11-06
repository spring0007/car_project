package com.launcher.yfd_ui01.app;

import com.launcher.yfd_ui01.R;
import java.util.HashMap;
import java.util.Map;

public class IconManager {
    //private Context context;
    private static Map<String, Integer> customIcons;
    
    public IconManager() {
        customIcons = new HashMap<>();
        loadCustomIcons();
    }
    
    /**
     * 加载自定义图标资源
     */
    private void loadCustomIcons() {
        // 这里可以加载预定义的自定义图标
        // 例如从assets目录或特定资源文件夹加载
        try {

            customIcons.put("com.awell.localvideo", R.drawable.yfd_ui1_video);
            customIcons.put("com.awell.localmusic", R.drawable.yfd_ui1_music);
            customIcons.put("com.awell.radio", R.drawable.yfd_ui1_radio);
            customIcons.put("com.awell.bluetooth", R.drawable.yfd_ui1_bluetooth);
////          customIcons.put( "com.awell.navigation", R.drawable.yfd_ui1_navi);
//            customIcons.put("com.android.dialer", R.drawable.yfd_ui1_iphone);
//            customIcons.put("com.android.calculator2", R.drawable.yfd_ui1_jisuanqi);
//            customIcons.put("com.android.browser", R.drawable.yfd_ui1_liulanqi);
//            //customIcons.put( "com.android.calendar"            , R.drawable.yfd_ui1_rili);
//            //customIcons.put( "com.android.soundrecorder"            , R.drawable.yfd_ui1_luyinji);
//            //customIcons.put( "com.android.deskclock"           , R.drawable.yfd_ui1_shizhong);
////  customIcons.put(  "com.android.gallery3d"//            , R.drawable.yfd_ui1_tuku );
            customIcons.put("com.mediatek.filemanager", R.drawable.yfd_ui1_file_manager);
//            customIcons.put("com.android.documentsui", R.drawable.yfd_ui1_download);
            customIcons.put("com.awell.carsetting", R.drawable.yfd_ui1_settings);
            customIcons.put("com.awell.backcar", R.drawable.yfd_ui1_aux);
//            customIcons.put("cn.kuwo.kwmusiccar", R.drawable.kuwoyinyue);
////		customIcons.put( "com.awell.soundeffect" , R.drawable.yfd_ui1_jhq);
            customIcons.put("com.awell.eqselect", R.drawable.yfd_ui1_dsp);
            customIcons.put("com.awell.canbus", R.drawable.yfd_ui1_streering_wheel);
//            customIcons.put("com.awell.canbus2", R.drawable.yfd_ui1_message);
//            customIcons.put("com.android.mms", R.drawable.yfd_ui1_navi);
//            customIcons.put("com.autonavi.amapauto", R.drawable.yfd_ui1_maps);
//            customIcons.put("com.txznet.txzsetting", R.drawable.yfd_ui1_ggvoice);
            customIcons.put("com.tima.carnet.vt", R.drawable.yfd_ui1_tlink5);
            customIcons.put("net.easyconn", R.drawable.yfd_ui1_tlink5);
            customIcons.put("com.zjinnova.zlink", R.drawable.yfd_ui1_tlink5);
            customIcons.put("com.awell.awellmanual", R.drawable.yfd_ui1_dev_tools); //说明书
            customIcons.put("com.google.android.apps.maps", R.drawable.yfd_ui1_maps);
            customIcons.put("com.google.android.youtube", R.drawable.yfd_ui1_youtube);
//            customIcons.put("com.android.vending", R.drawable.yfd_ui1_playstore);
            customIcons.put("com.android.chrome", R.drawable.yfd_ui1_chrome);
            customIcons.put("org.chromium.chrome", R.drawable.yfd_ui1_chrome);
//            customIcons.put("com.tinyapp.smartcar", R.drawable.yfd_ui1_ggvoice);
//            customIcons.put("com.awell.update", R.drawable.yfd_ui1_store);
//            customIcons.put("com.google.android.googlequicksearchbox", R.drawable.yfd_ui1_gg);
//            customIcons.put("com.kugou.android.auto", R.drawable.yfd_ui1_kugou);
//            customIcons.put("com.qiyi.video.pad", R.drawable.yfd_ui1_aiqitv);
//            customIcons.put("com.tencent.qqmusic", R.drawable.yfd_ui1_qqyinyue);
//            customIcons.put("com.tencent.qqlive.audiobox", R.drawable.yfd_ui1_tenxuntv);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getCustomIconsCount() {
        if (customIcons != null) {
            return customIcons.size();
        }
        return 0;
    }


    /**
     * 获取应用图标，优先使用自定义图标
     * @param packageName 应用包名
     * @return Drawable 图标
     */
    public Integer getIcon(String packageName ) {
        // 检查是否有自定义图标
        if (customIcons.containsKey(packageName)) {
            return customIcons.get(packageName);
        }
        // 使用默认图标
        return 0;
    }
}
