package com.launcher.yfd_ui01.app;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.launcher.yfd_ui01.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class IconManager {
    private final Context appContext;
    private static Map<String, Integer> customIcons;
    private static volatile IconManager sInstance;

    private static final int MAX_CACHE_SIZE = 50; // 最大缓存图标数量
    // 使用LRU缓存策略
    private final Map<String, Drawable> drawableCache = new LinkedHashMap<String, Drawable>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Drawable> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private IconManager(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        customIcons = new HashMap<>();
        loadCustomIcons();
        // 如果有 application context，则校验并预缓存有效资源
        if (this.appContext != null) {
           // validateAndCacheResources();
        }
    }

    /**
     * 初始化单例（建议在 Application 或主 Activity onCreate 时调用一次）
     */
    public static void init(Context context) {
        if (sInstance == null) {
            synchronized (IconManager.class) {
                if (sInstance == null) {
                    sInstance = new IconManager(context);
                }
            }
        }
    }

    /**
     * 获取单例，如果未初始化则返回一个临时实例（不建议）
     */
    public static IconManager getInstance(Context contextIfNeeded) {
        if (sInstance == null) {
            // 尝试初始化一次以保证后续调用安全
            init(contextIfNeeded != null ? contextIfNeeded.getApplicationContext() : null);
        }
        return sInstance;
    }

    /**
     * 获取已经初始化的单例（可能为 null，如果未调用 init）
     */
    public static IconManager getInstance() {
        return sInstance;
    }

    // 定义包名顺序列表
    public static final List<String> packageOrderList = Arrays.asList(
            "com.awell.localmusic",
            "com.awell.radio",
            "com.awell.localvideo",
            "com.awell.bluetooth",
            "com.awell.dspeffect",
            "com.google.android.apps.maps",
            "com.google.android.youtube",
            "com.android.chrome",
            "com.awell.themesetting"
            // ... 其他预定义包名
    );

    /**
     * 需要显示的应用
     */
    public static List<String> needToShowPackageName = Arrays.asList(
            "com.android.browser", "com.android.dialer", "com.android.mms","com.android.calculator2","com.android.deskclock", "com.mediatek.filemanager",
            "com.android.documentsui", "com.autonavi.amapauto",
            "com.awell.radio", "com.awell.localmusic", "com.awell.localvideo",
            "com.awell.backcar", "cn.kuwo.kwmusiccar", "com.awell.bluetooth",
//            "com.android.chrome","com.google.android.youtube","com.google.android.apps.maps","com.android.vending",
            "com.awell.canbus", "com.tima.carnet.vt",  "com.zjinnova.zlink",
            "com.awell.eqselect", "com.awell.awellmanual","com.awell.themesetting", "com.awell.carsetting",
            "com.android.chrome", "com.google.android.youtube",
            "com.google.android.apps.maps", "com.android.vending","org.chromium.chrome"
    );


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
            customIcons.put("com.awell.themesetting", R.drawable.yfd_ui1_theme);
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
            customIcons.put("com.android.vending", R.drawable.yfd_ui1_play_store);
            customIcons.put("com.android.chrome", R.drawable.yfd_ui1_chrome);
            customIcons.put("org.chromium.chrome", R.drawable.yfd_ui1_chrome);
            customIcons.put("com.facebook.katana", R.drawable.yfd_ui1_facebook);
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

    /**
     * 校验 customIcons 中的资源 id 是否在当前上下文可用，若可用则缓存其 Drawable，否则移除该映射。
     */
    private void validateAndCacheResources() {
        java.util.Iterator<Map.Entry<String, Integer>> it = customIcons.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            String pkg = entry.getKey();
            Integer resId = entry.getValue();
            try {
                Drawable d = ContextCompat.getDrawable(appContext, resId);
                if (d != null) {
                    drawableCache.put(pkg, d);
                } else {
                    // resource missing or cannot be loaded, 移除映射
                    it.remove();
                }
            } catch (Exception e) {
                // 任何异常都视为资源无效并移除
                it.remove();
            }
        }
    }

    public static int getCustomIconsCount() {
        if (customIcons != null) {
            return customIcons.size();
        }
        return 0;
    }

    /**
     * 获取应用图标，优先使用自定义图标（返回 Drawable，若无自定义图标则返回 null）
     * @param packageName 应用包名
     * @return Drawable 图标或 null
     */
    public Drawable getIcon(String packageName) {
        if (packageName == null) return null;

        // 先返回缓存的 Drawable（若存在）
        if (drawableCache.containsKey(packageName)) {
            return drawableCache.get(packageName);
        }

        // 再尝试通过 resource id 加载
        if (customIcons != null && customIcons.containsKey(packageName) && appContext != null) {
            Integer resId = customIcons.get(packageName);
            try {
                Drawable d = ContextCompat.getDrawable(appContext, resId);
                if (d != null) {
                    drawableCache.put(packageName, d);
                    return d;
                }
            } catch (Exception e) {
                // 记录并回退为 null
                android.util.Log.w("IconManager", "Failed to load icon resource for " + packageName + ": " + resId, e);
            }
        }

        return null;
    }
    
    /**
     * 清理缓存
     */
    public synchronized void clearCache() {
        drawableCache.clear();
    }
    
    /**
     * 根据内存压力调整缓存大小
     */
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            synchronized (this) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                    // 内存严重不足，清理大部分缓存
                    trimCacheToSize(MAX_CACHE_SIZE / 4);
                } else if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                    // 内存中度不足，清理一半缓存
                    trimCacheToSize(MAX_CACHE_SIZE / 2);
                }
            }
        }
    }
    
    /**
     * 将缓存裁剪到指定大小
     */
    private synchronized void trimCacheToSize(int targetSize) {
        if (drawableCache.size() <= targetSize) return;
        
        Iterator<Map.Entry<String, Drawable>> iterator = drawableCache.entrySet().iterator();
        while (iterator.hasNext() && drawableCache.size() > targetSize) {
            iterator.next();
            iterator.remove();
        }
    }
}