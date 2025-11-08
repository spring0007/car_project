package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Rect;

import com.awell.addapp.AppInfo;
import com.launcher.yfd_ui01.R;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RecyclerView adapter for a single page (grid) of apps.
 */
public class AppGridRecyclerAdapter extends RecyclerView.Adapter<AppGridRecyclerAdapter.Holder> {
    private final Context context;
    private List<AppInfo> appList;
    // Use centralized IconLoader for Glide RequestOptions and loading

    // Cache of ItemDecoration instances keyed by spanCount_spacingDp_includeEdge
    private static final Map<String, RecyclerView.ItemDecoration> decorationCache = new ConcurrentHashMap<>();

    public AppGridRecyclerAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
        // Enable stable ids to help RecyclerView keep view holders and reduce rebinds
        try {
            setHasStableIds(true);
        } catch (Exception ignored) {
        }
    }

    /**
     * Create a grid spacing decoration that avoids double spacing at edges.
     * Uses the common formula to distribute spacing across columns so outer edges aren't doubled.
     * @param ctx context for converting dp to px
     * @param spanCount number of columns
     * @param spacingDp desired spacing in dp
     */
    private static RecyclerView.ItemDecoration createGridSpacingItemDecoration(final Context ctx, final int spanCount, final int spacingDp, final boolean includeEdge) {
        final int spacing = (int) (ctx.getResources().getDisplayMetrics().density * spacingDp + 0.5f);
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view); // item position
                if (position == RecyclerView.NO_POSITION) return;
                int column = position % spanCount; // item column

                int total = 0;
                if (parent.getAdapter() != null) total = parent.getAdapter().getItemCount();
                int rows = (int) Math.ceil((double) total / spanCount);
                int currentRow = position / spanCount;

                if (includeEdge) {
                    // left and right spacing: distribute so that edges get spacing and inter-item gaps are spacing
                    outRect.left = spacing - column * spacing / spanCount;
                    outRect.right = (column + 1) * spacing / spanCount;

                    // top spacing only for rows after the first
                    outRect.top = currentRow == 0 ? spacing : spacing;
                    // bottom: avoid extra bottom for last row if you want tighter fit
                    outRect.bottom = (currentRow == rows - 1) ? spacing : spacing;
                } else {
                    // no edge spacing: full spacing only between items
                    outRect.left = column * spacing / spanCount;
                    outRect.right = spacing - (column + 1) * spacing / spanCount;

                    outRect.top = currentRow == 0 ? 0 : spacing;
                    outRect.bottom = (currentRow == rows - 1) ? 0 : 0;
                }
            }
        };
    }

    /**
     * Get a cached ItemDecoration for the given parameters. The decoration is created once and reused.
     */
    public static RecyclerView.ItemDecoration getGridItemDecoration(final Context ctx, final int spanCount, final int spacingDp, final boolean includeEdge) {
        String key = spanCount + "_" + spacingDp + "_" + (includeEdge ? "1" : "0");
        return decorationCache.computeIfAbsent(key, k -> createGridSpacingItemDecoration(ctx, spanCount, spacingDp, includeEdge));
    }

    public void setAppList(List<AppInfo> list) {
        this.appList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_grid, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (appList == null || position < 0 || position >= appList.size()) return;
        AppInfo appInfo = appList.get(position);
        holder.name.setText(appInfo.getLabel());

        // Use centralized IconLoader to load icons with shared RequestOptions and caching
        try {
            IconLoader.loadIcon(holder.icon.getContext(), appInfo.getIcon(), holder.icon);
        } catch (Exception e) {
            try {
                holder.icon.setImageDrawable(appInfo.getIcon());
            } catch (Exception ignored) {
            }
        }

        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(appInfo.getPackage_name());
                if (intent != null) {
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, R.string.unable_launch_application, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(context, R.string.error_launching_application, Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            try {
                if (isSystemApp(appInfo.getPackage_name())) {
                    Toast.makeText(context, R.string.unable_uninstall_system_applications, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (isCurrentLauncherApp(appInfo.getPackage_name())) {
                    Toast.makeText(context, R.string.unable_uninstall_current_desktop_application, Toast.LENGTH_SHORT).show();
                    return true;
                }
                uninstallApp(appInfo.getPackage_name());
            } catch (Exception e) {
                Toast.makeText(context, R.string.error_uninstalling_application, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return appList == null ? 0 : appList.size();
    }

    @Override
    public long getItemId(int position) {
        if (appList == null || position < 0 || position >= appList.size()) return RecyclerView.NO_ID;
        AppInfo info = appList.get(position);
        if (info == null) return position;
        String pkg = info.getPackage_name();
        if (pkg != null) return pkg.hashCode();
        return position;
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
        }
    }

    // 检查是否为系统应用
    private boolean isSystemApp(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // 检查是否为当前Launcher应用
    private boolean isCurrentLauncherApp(String packageName) {
        return context.getPackageName().equals(packageName);
    }

    // 执行卸载操作
    private void uninstallApp(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "无法启动卸载程序", Toast.LENGTH_SHORT).show();
        }
    }
}
