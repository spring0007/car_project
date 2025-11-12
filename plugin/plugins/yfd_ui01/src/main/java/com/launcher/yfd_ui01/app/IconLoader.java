package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.launcher.yfd_ui01.R;

/**
 * 优化的图标加载器，使用Glide进行高效图片加载
 */
public final class IconLoader {
    // 预创建的请求选项，避免重复构建
    private static final RequestOptions DEFAULT_OPTIONS = new RequestOptions()
            .centerCrop()
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder);

    private IconLoader() {}

    /**
     * 加载图标到ImageView
     */
    public static void loadIcon(Context ctx, Object model, ImageView target) {
        if (!isValidParams(ctx, model, target)) return;
        
        try {
            getRequestManager(ctx)
                    .load(model)
                    .apply(DEFAULT_OPTIONS)
                    .into(target);
        } catch (Exception e) {
            // 静默处理异常，避免崩溃
            setFallbackDrawable(target);
        }
    }

    /**
     * 批量预加载图标到缓存
     */
    public static void preloadIcons(Context ctx, java.util.List<Object> models) {
        if (ctx == null || models == null || models.isEmpty()) return;
        
        RequestManager requestManager = getRequestManager(ctx);
        for (Object model : models) {
            if (model != null) {
                try {
                    requestManager.load(model).apply(DEFAULT_OPTIONS).preload();
                } catch (Exception ignored) {
                    // 忽略单个项目加载失败
                }
            }
        }
    }

    /**
     * 参数有效性检查
     */
    private static boolean isValidParams(Context ctx, Object model, ImageView target) {
        return ctx != null && model != null && target != null;
    }

    /**
     * 获取Glide请求管理器
     */
    private static RequestManager getRequestManager(Context ctx) {
        return Glide.with(ctx);
    }

    /**
     * 设置备用图标
     */
    private static void setFallbackDrawable(ImageView target) {
        if (target != null) {
            try {
                target.setImageResource(R.drawable.ic_app_placeholder);
            } catch (Exception ignored) {
                // 最终备用方案
            }
        }
    }
}
