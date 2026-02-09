package com.awell.launcher2;

import com.awell.launcher.library.R;

import java.util.HashMap;
import java.util.Map;

public class IconPkgMap {

    //todo Move to plugin app

    // 默认主题图标映射
    static public final Map<String, Integer> PACKAGE_ICON_MAP_DEFAULT = new HashMap<>();

    // 暗色主题图标映射
    static public final Map<String, Integer> PACKAGE_ICON_MAP_DARK = new HashMap<>();

    // 黄色主题图标映射
    static public final Map<String, Integer> PACKAGE_ICON_MAP_YELLOW = new HashMap<>();

    static public final Map<String, Integer> PACKAGE_ICON_MAP_YFD_2 = new HashMap<>();


    static {
        // 默认主题图标映射
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.localvideo", R.drawable.sf_video);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.localmusic", R.drawable.sf_music);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.radio", R.drawable.sf_radio);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.bluetooth", R.drawable.sf_bt);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.navigation", R.drawable.sf_navi);
        PACKAGE_ICON_MAP_DEFAULT.put("com.android.browser", R.drawable.sf_liulanqi);
        PACKAGE_ICON_MAP_DEFAULT.put("com.mediatek.filemanager", R.drawable.sf_wjgl);
        PACKAGE_ICON_MAP_DEFAULT.put("com.android.documentsui", R.drawable.sf_wjgl);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.carsetting", R.drawable.sf_shezhi);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.backcar", R.drawable.sf_wjsr);
        PACKAGE_ICON_MAP_DEFAULT.put("cn.kuwo.kwmusiccar", R.drawable.kuwoyinyue);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.eqselect", R.drawable.sf_jhq);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.canbus", R.drawable.sf_yuanche);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.canbus2", R.drawable.sf_yuanche);
        PACKAGE_ICON_MAP_DEFAULT.put("com.autonavi.amapauto", R.drawable.sf_gaode);
       // PACKAGE_ICON_MAP_DEFAULT.put("com.tima.carnet.vt", R.drawable.sf_zlink);
        PACKAGE_ICON_MAP_DEFAULT.put("net.easyconn", R.drawable.sf_zlink);
        PACKAGE_ICON_MAP_DEFAULT.put("com.zjinnova.zlink", R.drawable.sf_zlink);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.awellmanual", R.drawable.sf_manual);
        PACKAGE_ICON_MAP_DEFAULT.put("com.google.android.apps.maps", R.drawable.sf_googlemap);
        PACKAGE_ICON_MAP_DEFAULT.put("com.google.android.youtube", R.drawable.sf_youtube);
        PACKAGE_ICON_MAP_DEFAULT.put("com.android.vending", R.drawable.sf_playstore);
        PACKAGE_ICON_MAP_DEFAULT.put("com.android.chrome", R.drawable.sf_safri);
        PACKAGE_ICON_MAP_DEFAULT.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg);
        PACKAGE_ICON_MAP_DEFAULT.put("com.android.gallery3d", R.drawable.sf_gallery);
        PACKAGE_ICON_MAP_DEFAULT.put("org.chromium.chrome", R.drawable.sf_chrome);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.electricfan", R.drawable.sf_fan);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.frontvideo", R.drawable.sf_front_video);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.keylight", R.drawable.sf_color_light);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.keystudy", R.drawable.sf_training);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.themesetting", R.drawable.sf_theme);
        PACKAGE_ICON_MAP_DEFAULT.put("com.ms.ms2160", R.drawable.sf_usb_video_output);
        PACKAGE_ICON_MAP_DEFAULT.put("com.awell.weather", R.drawable.sf_weather);

        // 暗色主题图标映射
        PACKAGE_ICON_MAP_DARK.put("com.awell.localvideo", R.drawable.sf_video_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.localmusic", R.drawable.sf_music_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.radio", R.drawable.sf_radio_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.bluetooth", R.drawable.sf_bt_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.navigation", R.drawable.sf_navi_dark);
        PACKAGE_ICON_MAP_DARK.put("com.android.browser", R.drawable.sf_liulanqi_dark);
        PACKAGE_ICON_MAP_DARK.put("com.mediatek.filemanager", R.drawable.sf_wjgl_dark);
        PACKAGE_ICON_MAP_DARK.put("com.android.documentsui", R.drawable.sf_wjgl_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.carsetting", R.drawable.sf_shezhi_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.backcar", R.drawable.sf_wjsr_dark);
        PACKAGE_ICON_MAP_DARK.put("cn.kuwo.kwmusiccar", R.drawable.kuwoyinyue_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.eqselect", R.drawable.sf_jhq_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.canbus", R.drawable.sf_yuanche_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.canbus2", R.drawable.sf_yuanche_dark);
        PACKAGE_ICON_MAP_DARK.put("com.autonavi.amapauto", R.drawable.sf_gaode_dark);
       // PACKAGE_ICON_MAP_DARK.put("com.tima.carnet.vt", R.drawable.sf_zlink_dark);
        PACKAGE_ICON_MAP_DARK.put("net.easyconn", R.drawable.sf_zlink_dark);
        PACKAGE_ICON_MAP_DARK.put("com.zjinnova.zlink", R.drawable.sf_zlink_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.awellmanual", R.drawable.sf_manual_dark);
        PACKAGE_ICON_MAP_DARK.put("com.google.android.apps.maps", R.drawable.sf_googlemap_dark);
        PACKAGE_ICON_MAP_DARK.put("com.google.android.youtube", R.drawable.sf_youtube_dark);
        PACKAGE_ICON_MAP_DARK.put("com.android.vending", R.drawable.sf_playstore_dark);
        PACKAGE_ICON_MAP_DARK.put("com.android.chrome", R.drawable.sf_safri_dark);
        PACKAGE_ICON_MAP_DARK.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg_dark);
        PACKAGE_ICON_MAP_DARK.put("com.android.gallery3d", R.drawable.sf_gallery_dark);
        PACKAGE_ICON_MAP_DARK.put("org.chromium.chrome", R.drawable.sf_chrome_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.electricfan", R.drawable.sf_fan_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.frontvideo", R.drawable.sf_front_video_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.keylight", R.drawable.sf_color_light_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.keystudy", R.drawable.sf_training_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.themesetting", R.drawable.sf_theme_dark);
        PACKAGE_ICON_MAP_DARK.put("com.ms.ms2160", R.drawable.sf_usb_video_output_dark);
        PACKAGE_ICON_MAP_DARK.put("com.awell.weather", R.drawable.sf_weather_dark);

        // 黄色主题图标映射
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.localvideo", R.drawable.sf_video_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.localmusic", R.drawable.sf_music_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.radio", R.drawable.sf_radio_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.bluetooth", R.drawable.sf_bt_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.navigation", R.drawable.sf_navi_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.android.browser", R.drawable.sf_liulanqi_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.mediatek.filemanager", R.drawable.sf_wjgl_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.android.documentsui", R.drawable.sf_wjgl_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.carsetting", R.drawable.sf_shezhi_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.backcar", R.drawable.sf_wjsr_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("cn.kuwo.kwmusiccar", R.drawable.kuwoyinyue_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.eqselect", R.drawable.sf_jhq_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.canbus", R.drawable.sf_yuanche_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.canbus2", R.drawable.sf_yuanche_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.autonavi.amapauto", R.drawable.sf_gaode_yellow);
       // PACKAGE_ICON_MAP_YELLOW.put("com.tima.carnet.vt", R.drawable.sf_zlink_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("net.easyconn", R.drawable.sf_zlink_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.zjinnova.zlink", R.drawable.sf_zlink_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.awellmanual", R.drawable.sf_manual_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.google.android.apps.maps", R.drawable.sf_googlemap_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.google.android.youtube", R.drawable.sf_youtube_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.android.vending", R.drawable.sf_playstore_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.android.chrome", R.drawable.sf_safri_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.android.gallery3d", R.drawable.sf_gallery_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("org.chromium.chrome", R.drawable.sf_chrome_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.electricfan", R.drawable.sf_fan_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.frontvideo", R.drawable.sf_front_video_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.keylight", R.drawable.sf_color_light_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.keystudy", R.drawable.sf_training_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.themesetting", R.drawable.sf_theme_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.ms.ms2160", R.drawable.sf_usb_video_output_yellow);
        PACKAGE_ICON_MAP_YELLOW.put("com.awell.weather", R.drawable.sf_weather_yellow);


        PACKAGE_ICON_MAP_YFD_2.put("com.awell.localvideo", R.drawable.sf_video_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.localmusic", R.drawable.sf_music_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.radio", R.drawable.sf_radio_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.bluetooth", R.drawable.sf_bt_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.navigation", R.drawable.sf_navi_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.android.browser", R.drawable.sf_liulanqi_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.mediatek.filemanager", R.drawable.sf_wjgl_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.android.documentsui", R.drawable.sf_wjgl_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.carsetting", R.drawable.sf_shezhi_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.backcar", R.drawable.sf_wjsr_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("cn.kuwo.kwmusiccar", R.drawable.kuwoyinyue_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.eqselect", R.drawable.sf_jhq_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.canbus", R.drawable.sf_yuanche_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.canbus2", R.drawable.sf_yuanche_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.autonavi.amapauto", R.drawable.sf_gaode);
       // PACKAGE_ICON_MAP_YFD_2.put("com.tima.carnet.vt", R.drawable.sf_zlink_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("net.easyconn", R.drawable.sf_zlink_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.zjinnova.zlink", R.drawable.sf_zlink_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.awellmanual", R.drawable.sf_manual_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.google.android.apps.maps", R.drawable.sf_googlemap_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.google.android.youtube", R.drawable.sf_youtube_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.android.vending", R.drawable.sf_playstore_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.android.chrome", R.drawable.sf_safri_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.android.gallery3d", R.drawable.sf_gallery_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("org.chromium.chrome", R.drawable.sf_safri_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.electricfan", R.drawable.sf_fan_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.frontvideo", R.drawable.sf_front_video_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.keylight", R.drawable.sf_color_light);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.keystudy", R.drawable.sf_training_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.themesetting", R.drawable.sf_theme_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.ms.ms2160", R.drawable.sf_usb_video_output_yfd_2);
        PACKAGE_ICON_MAP_YFD_2.put("com.awell.weather", R.drawable.sf_weather_yfd_2);

    }


}
