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

    static public final Map<String, Integer> PACKAGE_ICON_MAP_CIRCLE = new HashMap<>();

    static public final Map<String, Integer> PACKAGE_ICON_MAP_ZY_1  = new HashMap<>();

    static public final Map<String, Integer> PACKAGE_ICON_MAP_ZY_2  = new HashMap<>();
    static public final Map<String, Integer> PACKAGE_ICON_MAP_ZY_3  = new HashMap<>();

    static final String AIR_NAME = ".AwellCanbusAirView";
    static final String CANBUS_NAME = "com.awell.canbus";

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
        //PACKAGE_ICON_MAP_DEFAULT.put("net.easyconn", R.drawable.sf_zlink);
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
        PACKAGE_ICON_MAP_DEFAULT.put(AIR_NAME, R.drawable.ic_air_app);

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
        //PACKAGE_ICON_MAP_DARK.put("net.easyconn", R.drawable.sf_zlink_dark);
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
       // PACKAGE_ICON_MAP_YELLOW.put("net.easyconn", R.drawable.sf_zlink_yellow);
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
        //PACKAGE_ICON_MAP_YFD_2.put("net.easyconn", R.drawable.sf_zlink_yfd_2);
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



        // 圆形主题图标映射
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.localvideo", R.drawable.sf_video_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.localmusic", R.drawable.sf_music_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.radio", R.drawable.sf_radio_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.bluetooth", R.drawable.sf_bt_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.navigation", R.drawable.sf_navi_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.android.browser", R.drawable.sf_liulanqi_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.mediatek.filemanager", R.drawable.sf_wjgl_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.android.documentsui", R.drawable.sf_wjgl_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.carsetting", R.drawable.sf_shezhi_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.backcar", R.drawable.sf_wjsr_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("cn.kuwo.kwmusiccar", R.drawable.sf_kugou_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.eqselect", R.drawable.sf_jhq_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.canbus", R.drawable.sf_yuanche_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.canbus2", R.drawable.sf_yuanche_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.autonavi.amapauto", R.drawable.sf_gaode_circle);
        // PACKAGE_ICON_MAP_CIRCLE.put("com.tima.carnet.vt", R.drawable.sf_zlink_circle);
        //PACKAGE_ICON_MAP_CIRCLE.put("net.easyconn", R.drawable.sf_zlink_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.zjinnova.zlink", R.drawable.sf_zlink_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.awellmanual", R.drawable.sf_manual_circle);
        //PACKAGE_ICON_MAP_CIRCLE.put("com.google.android.apps.maps", R.drawable.sf_googlemap_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.google.android.youtube", R.drawable.sf_youtube_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.android.vending", R.drawable.sf_playstore_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.android.chrome", R.drawable.sf_liulanqi_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.android.gallery3d", R.drawable.sf_gallery_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("org.chromium.chrome", R.drawable.sf_liulanqi_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.electricfan", R.drawable.sf_fan_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.frontvideo", R.drawable.sf_front_video_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.keylight", R.drawable.sf_color_light_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.keystudy", R.drawable.sf_training_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.themesetting", R.drawable.sf_theme_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.ms.ms2160", R.drawable.sf_usb_video_output_circle);
        PACKAGE_ICON_MAP_CIRCLE.put("com.awell.weather", R.drawable.sf_weather_circle);

        // ZY1
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.localvideo", R.drawable.ic_video_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.localmusic", R.drawable.ic_music_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.radio", R.drawable.ic_radio_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.bluetooth", R.drawable.ic_bluetooth_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.navigation", R.drawable.ic_navi_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.android.browser", R.drawable.ic_browser_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.mediatek.filemanager", R.drawable.ic_file_manager_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.android.documentsui", R.drawable.ic_file_manager_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.carsetting", R.drawable.ic_car_setting);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.backcar", R.drawable.ic_usb_app);
        PACKAGE_ICON_MAP_ZY_1.put("cn.kuwo.kwmusiccar", R.drawable.ic_kw_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.eqselect", R.drawable.ic_jhq_app);
        PACKAGE_ICON_MAP_ZY_1.put(CANBUS_NAME, R.drawable.ic_car_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.canbus2", R.drawable.ic_car_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.autonavi.amapauto", R.drawable.ic_navi_app);
//        PACKAGE_ICON_MAP_ZY_1.put("com.tima.carnet.vt", R.drawable.sf_zlink_circle);
//        PACKAGE_ICON_MAP_ZY_1.put("net.easyconn", R.drawable.sf_zlink_circle);
//        PACKAGE_ICON_MAP_ZY_1.put("com.zjinnova.zlink", R.drawable.sf_zlink_circle);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.awellmanual", R.drawable.ic_manual_app);
//        PACKAGE_ICON_MAP_ZY_1.put("com.google.android.apps.maps", R.drawable.sf_googlemap_circle);
//        PACKAGE_ICON_MAP_ZY_1.put("com.google.android.youtube", R.drawable.sf_youtube_circle);
        PACKAGE_ICON_MAP_ZY_1.put("com.android.vending", R.drawable.ic_app_store);
        PACKAGE_ICON_MAP_ZY_1.put("com.android.chrome", R.drawable.ic_browser_app);
//        PACKAGE_ICON_MAP_ZY_1.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg_circle);
        PACKAGE_ICON_MAP_ZY_1.put("com.android.gallery3d", R.drawable.ic_gallery_app);
        PACKAGE_ICON_MAP_ZY_1.put("org.chromium.chrome", R.drawable.ic_browser_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.electricfan", R.drawable.ic_fan_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.frontvideo", R.drawable.ic_forwardview_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.keylight", R.drawable.ic_color_light_app);
//        PACKAGE_ICON_MAP_ZY_1.put("com.awell.keystudy", R.drawable.sf_training_circle);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.themesetting", R.drawable.ic_theme_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.ms.ms2160", R.drawable.ic_video_output_app);
        PACKAGE_ICON_MAP_ZY_1.put("com.awell.weather", R.drawable.ic_weather_app);
        PACKAGE_ICON_MAP_ZY_1.put(AIR_NAME, R.drawable.ic_air_app);

        // ZY2
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.localvideo", R.drawable.zy2_video);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.localmusic", R.drawable.zy2_music);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.radio", R.drawable.zy2_radio_app);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.bluetooth", R.drawable.zy2_bluetooth);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.navigation", R.drawable.zy2_gaode);
        PACKAGE_ICON_MAP_ZY_2.put("com.android.browser", R.drawable.zy2_liulanqi);
        PACKAGE_ICON_MAP_ZY_2.put("com.mediatek.filemanager", R.drawable.zy2_filemanager);
        PACKAGE_ICON_MAP_ZY_2.put("com.android.documentsui", R.drawable.zy2_filemanager);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.carsetting", R.drawable.zy2_car_setting);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.backcar", R.drawable.zy2_usb);
        PACKAGE_ICON_MAP_ZY_2.put("cn.kuwo.kwmusiccar", R.drawable.ay2_kw);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.eqselect", R.drawable.zy2_dsp);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.canbus", R.drawable.zy2_car_info);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.canbus2", R.drawable.zy2_car_info);
        PACKAGE_ICON_MAP_ZY_2.put("com.autonavi.amapauto", R.drawable.zy2_gaode);
//        PACKAGE_ICON_MAP_ZY_2.put("com.tima.carnet.vt", R.drawable.sf_zlink_circle);
//        PACKAGE_ICON_MAP_ZY_2.put("net.easyconn", R.drawable.sf_zlink_circle);
//        PACKAGE_ICON_MAP_ZY_2.put("com.zjinnova.zlink", R.drawable.sf_zlink_circle);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.awellmanual", R.drawable.zy2_manual);
//        PACKAGE_ICON_MAP_ZY_2.put("com.google.android.apps.maps", R.drawable.sf_googlemap_circle);
//        PACKAGE_ICON_MAP_ZY_2.put("com.google.android.youtube", R.drawable.sf_youtube_circle);
        PACKAGE_ICON_MAP_ZY_2.put("com.android.vending", R.drawable.zy2_app_store);
        PACKAGE_ICON_MAP_ZY_2.put("com.android.chrome", R.drawable.zy2_liulanqi);
//        PACKAGE_ICON_MAP_ZY_2.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg_circle);
        PACKAGE_ICON_MAP_ZY_2.put("com.android.gallery3d", R.drawable.zy2_image_album);
        PACKAGE_ICON_MAP_ZY_2.put("org.chromium.chrome", R.drawable.zy2_liulanqi);
//        PACKAGE_ICON_MAP_ZY_2.put("com.awell.electricfan", R.drawable.ic_fan_app);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.frontvideo", R.drawable.zy2_forwardview);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.keylight", R.drawable.zy2_ic_color_light);
//        PACKAGE_ICON_MAP_ZY_2.put("com.awell.keystudy", R.drawable.sf_training_circle);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.themesetting", R.drawable.zy2_theme);
        PACKAGE_ICON_MAP_ZY_2.put("com.ms.ms2160", R.drawable.zy2_video_output);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.weather", R.drawable.zy2_weather);
        PACKAGE_ICON_MAP_ZY_2.put("com.awell.dspeffect", R.drawable.zy2_dsp);
        PACKAGE_ICON_MAP_ZY_2.put("com.android.settings", R.drawable.zy2_setting);
        PACKAGE_ICON_MAP_ZY_2.put(AIR_NAME, R.drawable.ic_air_app);


        // ZY3
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.localvideo", R.drawable.ic_video_app2);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.localmusic", R.drawable.ic_music_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.radio", R.drawable.ic_radio_app2);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.bluetooth", R.drawable.ic_bluetooth_app2);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.navigation", R.drawable.ic_navi_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.android.browser", R.drawable.ic_browser_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.mediatek.filemanager", R.drawable.ic_file_manager_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.android.documentsui", R.drawable.ic_file_manager_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.carsetting", R.drawable.ic_car_setting2);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.backcar", R.drawable.ic_usb_app);
        PACKAGE_ICON_MAP_ZY_3.put("cn.kuwo.kwmusiccar", R.drawable.ic_kw_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.eqselect", R.drawable.ic_jhq_app);
        PACKAGE_ICON_MAP_ZY_3.put(CANBUS_NAME, R.drawable.ic_car_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.canbus2", R.drawable.ic_car_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.autonavi.amapauto", R.drawable.ic_navi_app);
//        PACKAGE_ICON_MAP_ZY_3.put("com.tima.carnet.vt", R.drawable.sf_zlink_circle);
//        PACKAGE_ICON_MAP_ZY_3.put("net.easyconn", R.drawable.sf_zlink_circle);
//        PACKAGE_ICON_MAP_ZY_3.put("com.zjinnova.zlink", R.drawable.sf_zlink_circle);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.awellmanual", R.drawable.ic_manual_app);
//        PACKAGE_ICON_MAP_ZY_3.put("com.google.android.apps.maps", R.drawable.sf_googlemap_circle);
//        PACKAGE_ICON_MAP_ZY_3.put("com.google.android.youtube", R.drawable.sf_youtube_circle);
        PACKAGE_ICON_MAP_ZY_3.put("com.android.vending", R.drawable.ic_app_store);
        PACKAGE_ICON_MAP_ZY_3.put("com.android.chrome", R.drawable.ic_browser_app);
//        PACKAGE_ICON_MAP_ZY_3.put("com.google.android.googlequicksearchbox", R.drawable.sf_gg_circle);
        PACKAGE_ICON_MAP_ZY_3.put("com.android.gallery3d", R.drawable.ic_gallery_app);
        PACKAGE_ICON_MAP_ZY_3.put("org.chromium.chrome", R.drawable.ic_browser_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.electricfan", R.drawable.ic_fan_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.frontvideo", R.drawable.ic_forwardview_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.keylight", R.drawable.ic_color_light_app);
//        PACKAGE_ICON_MAP_ZY_3.put("com.awell.keystudy", R.drawable.sf_training_circle);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.themesetting", R.drawable.ic_theme_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.ms.ms2160", R.drawable.ic_video_output_app);
        PACKAGE_ICON_MAP_ZY_3.put("com.awell.weather", R.drawable.ic_weather_app);
        PACKAGE_ICON_MAP_ZY_3.put(AIR_NAME, R.drawable.ic_air_app);

    }


}
