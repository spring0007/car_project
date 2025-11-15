package com.launcher.yfd_ui01.app;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.launcher.yfd_ui01.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class IconManager {
    private static final String TAG = "IconManager";
    private static final int MAX_CACHE_SIZE = 50;
    
    private final Context appContext;
    private static volatile IconManager sInstance;

    // 使用不可变集合提高性能
    private static final Map<String, Integer> CUSTOM_ICONS;
    public static final List<String> PACKAGE_ORDER_LIST;
    public static final List<String> NEED_TO_SHOW_PACKAGE_NAMES;

    // 静态初始化块，避免重复创建
    static {
        // 包名顺序列表
        PACKAGE_ORDER_LIST = Collections.unmodifiableList(Arrays.asList(
            "com.awell.localmusic", "com.awell.radio", "com.awell.localvideo",
            "com.awell.bluetooth", "com.awell.dspeffect", "com.google.android.apps.maps",
            "com.google.android.youtube", "com.android.chrome", "com.awell.themesetting"
        ));

        // 需要显示的应用
        NEED_TO_SHOW_PACKAGE_NAMES = Collections.unmodifiableList(Arrays.asList(
            "com.android.browser", "com.android.dialer", "com.android.mms",
            "com.android.calculator2", "com.android.deskclock", "com.mediatek.filemanager",
            "com.android.documentsui", "com.autonavi.amapauto", "com.awell.radio",
            "com.awell.localmusic", "com.awell.localvideo", "com.awell.backcar",
            "cn.kuwo.kwmusiccar", "com.awell.bluetooth", "com.awell.canbus",
            "com.tima.carnet.vt", "com.zjinnova.zlink", "com.awell.eqselect",
            "com.awell.awellmanual", "com.awell.themesetting", "com.awell.carsetting",
            "com.android.chrome", "com.google.android.youtube", "com.google.android.apps.maps",
            "com.android.vending", "org.chromium.chrome"
        ));

        // 自定义图标映射
        Map<String, Integer> tempIcons = new HashMap<>();
        try {
            tempIcons.put("com.awell.localvideo", R.drawable.yfd_ui1_video);
            tempIcons.put("com.awell.localmusic", R.drawable.yfd_ui1_music);
            tempIcons.put("com.awell.radio", R.drawable.yfd_ui1_radio);
            tempIcons.put("com.awell.bluetooth", R.drawable.yfd_ui1_bluetooth);
            tempIcons.put("com.awell.themesetting", R.drawable.yfd_ui1_theme);
            tempIcons.put("com.mediatek.filemanager", R.drawable.yfd_ui1_file_manager);
            tempIcons.put("com.awell.carsetting", R.drawable.yfd_ui1_settings);
            tempIcons.put("com.awell.backcar", R.drawable.yfd_ui1_aux);
            tempIcons.put("com.awell.eqselect", R.drawable.yfd_ui1_dsp);
            tempIcons.put("com.awell.canbus", R.drawable.yfd_ui1_streering_wheel);
            tempIcons.put("com.tima.carnet.vt", R.drawable.yfd_ui1_tlink5);
            tempIcons.put("net.easyconn", R.drawable.yfd_ui1_tlink5);
            tempIcons.put("com.zjinnova.zlink", R.drawable.yfd_ui1_tlink5);
            tempIcons.put("com.awell.awellmanual", R.drawable.yfd_ui1_dev_tools);
            tempIcons.put("com.google.android.apps.maps", R.drawable.yfd_ui1_maps);
            tempIcons.put("com.google.android.youtube", R.drawable.yfd_ui1_youtube);
            tempIcons.put("com.android.vending", R.drawable.yfd_ui1_play_store);
            tempIcons.put("com.android.chrome", R.drawable.yfd_ui1_chrome);
            tempIcons.put("org.chromium.chrome", R.drawable.yfd_ui1_chrome);
            tempIcons.put("com.facebook.katana", R.drawable.yfd_ui1_facebook);
            tempIcons.put("com.awell.electricfan",R.drawable.yfd_ui1_fan);
        } catch (Exception e) {
            // 静默处理初始化异常
        }
        CUSTOM_ICONS = Collections.unmodifiableMap(tempIcons);
    }

    // LRU缓存
    private final Map<String, Drawable> drawableCache = new LinkedHashMap<String, Drawable>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Drawable> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private IconManager(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
    }

    /**
     * 初始化单例
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
     * 获取单例
     */
    public static IconManager getInstance(Context contextIfNeeded) {
        if (sInstance == null) {
            init(contextIfNeeded != null ? contextIfNeeded.getApplicationContext() : null);
        }
        return sInstance;
    }

    public static IconManager getInstance() {
        return sInstance;
    }

    /**
     * 获取自定义图标数量
     */
    public static int getCustomIconsCount() {
        return CUSTOM_ICONS.size();
    }

    /**
     * 获取应用图标，优先使用自定义图标
     */
    public Drawable getIcon(String packageName) {
        if (packageName == null) return null;

        // 先检查缓存
        Drawable cached;
        synchronized (drawableCache) {
            cached = drawableCache.get(packageName);
            if (cached != null) {
                return cached;
            }
        }

        // 检查自定义图标
        Integer resId = CUSTOM_ICONS.get(packageName);
        if (resId != null && appContext != null) {
            try {
                Drawable drawable = ContextCompat.getDrawable(appContext, resId);
                if (drawable != null) {
                    synchronized (drawableCache) {
                        drawableCache.put(packageName, drawable);
                    }
                    return drawable;
                }
            } catch (Exception e) {
                // 静默处理资源加载异常
            }
        }

        return null;
    }
    
    /**
     * 批量预加载常用图标
     */
    public void preloadCommonIcons() {
        if (appContext == null) return;
        
        // 在后台线程预加载
        new Thread(() -> {
            for (String packageName : PACKAGE_ORDER_LIST) {
                synchronized (drawableCache) {
                    if (drawableCache.containsKey(packageName)) continue;
                }
                
                Integer resId = CUSTOM_ICONS.get(packageName);
                if (resId != null) {
                    try {
                        Drawable drawable = ContextCompat.getDrawable(appContext, resId);
                        if (drawable != null) {
                            synchronized (drawableCache) {
                                drawableCache.put(packageName, drawable);
                            }
                        }
                    } catch (Exception e) {
                        // 静默处理单个图标加载失败
                    }
                }
            }
        }).start();
    }
    
    /**
     * 裁剪缓存到指定大小
     */
    private void trimCacheToSize(int targetSize) {
        if (drawableCache.size() <= targetSize) return;
        
        Iterator<Map.Entry<String, Drawable>> iterator = drawableCache.entrySet().iterator();
        while (iterator.hasNext()) {
            drawableCache.size();
            iterator.next();
            iterator.remove();
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        synchronized (drawableCache) {
            return "Cache size: " + drawableCache.size() + "/" + MAX_CACHE_SIZE;
        }
    }

    public void clearCache() {
        synchronized (drawableCache) {
            drawableCache.clear();
        }
    }
}