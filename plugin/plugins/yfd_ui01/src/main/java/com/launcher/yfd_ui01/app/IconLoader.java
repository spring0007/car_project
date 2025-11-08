package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.launcher.yfd_ui01.R;

/**
 * Centralized helper for loading icons with Glide and shared RequestOptions.
 */
public final class IconLoader {
    private static final RequestOptions DEFAULT_OPTIONS = new RequestOptions()
            .centerCrop()
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder);

    private IconLoader() {}

    public static RequestOptions getDefaultOptions() {
        return DEFAULT_OPTIONS;
    }

    public static void loadIcon(Context ctx, Object model, ImageView target) {
        if (ctx == null || target == null) return;
        try {
            Glide.with(ctx)
                    .load(model)
                    .apply(DEFAULT_OPTIONS)
                    .into(target);
        } catch (Exception ignored) {
            // swallow to avoid crashes during image loading; callers may set fallback drawable
        }
    }

    /**
     * Preload a list of icon models into Glide's cache. Models can be resource ids, Drawables,
     * package names (if you plan to resolve them before calling), or URIs handled by Glide.
     */
    public static void preloadIcons(Context ctx, java.util.List<Object> models) {
        if (ctx == null || models == null || models.isEmpty()) return;
        try {
            for (Object model : models) {
                try {
                    Glide.with(ctx).load(model).apply(DEFAULT_OPTIONS).preload();
                } catch (Exception ignored) {
                    // ignore per-item failures
                }
            }
        } catch (Exception ignored) {
            // guard overall preload
        }
    }
}
