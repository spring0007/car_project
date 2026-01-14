package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Rect;

import com.awell.addapp.AppInfo;
import com.launcher.yfd_ui01.R;

import java.util.List;

/**
 * RecyclerView adapter for a single page (grid) of apps.
 */
public class AppGridRecyclerAdapter extends RecyclerView.Adapter<AppGridRecyclerAdapter.Holder> {
    private final Context context;
    private List<AppInfo> appList;
    private final IconManager iconManager;
    private OnItemSizeCalculatedListener sizeListener;
    private boolean hasCalculatedSize = false;
    public AppGridRecyclerAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
        this.iconManager = IconManager.getInstance(context);
        setHasStableIds(true);
    }

    public void setOnItemSizeCalculatedListener(OnItemSizeCalculatedListener listener) {
        this.sizeListener = listener;
    }

    /**
     * 创建网格间距装饰器
     */
    public static RecyclerView.ItemDecoration createGridSpacingItemDecoration(final int spanCount,
                                                                              final int leftSpacing,
                                                                              final int topSpacing,
                                                                              final boolean includeEdge) {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == RecyclerView.NO_POSITION) return;

                //int column = position % spanCount;

                if (includeEdge) {
                    // 左右都有边距，且间距均匀分布
                    //outRect.left = leftSpacing - column * leftSpacing / spanCount;
                    //outRect.right = (column + 1) * leftSpacing / spanCount;
                    outRect.left = leftSpacing / 2;
                    outRect.right = leftSpacing / 2;

                    // 上下间距
                    if (position < spanCount) { // 第一行
                        outRect.top = topSpacing;
                    }
                    outRect.bottom = topSpacing; // 所有行底部间距
                } else {
                    // 无边缘间距：只在item之间有间距
                    //outRect.left = column * leftSpacing / spanCount;
                    //outRect.right = leftSpacing - (column + 1) * leftSpacing / spanCount;
                    outRect.left = leftSpacing / 2;
                    outRect.right = leftSpacing / 2;

                    if (position >= spanCount) {
                        outRect.top = topSpacing; // 非第一行的顶部间距
                    }
                    outRect.bottom = 0;
                }

            }
        };
    }

    public void setAppList(List<AppInfo> list) {
        this.appList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_app_grid, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (appList == null || position < 0 || position >= appList.size()) return;
        
        AppInfo appInfo = appList.get(position);
        holder.name.setText(appInfo.getLabel());

        // 优化图标加载：先设置占位符，再异步加载图标
        holder.icon.setImageResource(R.drawable.ic_app_placeholder);
        
        // 异步加载图标
        loadAppIcon(holder.icon, appInfo);
        // 预加载点击事件所需的资源
        holder.itemView.setTag(appInfo);
        holder.itemView.setOnClickListener(clickListener);
        holder.itemView.setOnLongClickListener(longClickListener);

        // 在 View 布局完成后获取尺寸
        if (!hasCalculatedSize && holder.itemView.getWidth() == 0) {
            holder.itemView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            holder.itemView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            int itemWidth = holder.itemView.getWidth();
                            int itemHeight = holder.itemView.getHeight();

                            if (itemWidth > 0 && itemHeight > 0 && sizeListener != null) {
                                hasCalculatedSize = true;
                                sizeListener.onItemSizeCalculated(itemWidth, itemHeight);
                            }
                        }
                    });
        } else if (!hasCalculatedSize && holder.itemView.getWidth() > 0) {
            // 如果已经有尺寸，直接使用
            int itemWidth = holder.itemView.getWidth();
            int itemHeight = holder.itemView.getHeight();

            if (itemWidth > 0 && itemHeight > 0 && sizeListener != null) {
                hasCalculatedSize = true;
                sizeListener.onItemSizeCalculated(itemWidth, itemHeight);
            }
        }
    }

    // 重置尺寸计算状态（当布局变化时调用）
    public void resetSizeCalculation() {
        hasCalculatedSize = false;
    }

	   /**
     * 异步加载应用图标
     */
    private void loadAppIcon(ImageView imageView, AppInfo appInfo) {
        if (appInfo == null || imageView == null) return;
        
        // 先尝试使用应用自带的图标
        Drawable appIcon = appInfo.getIcon();
        if (appIcon != null) {
            imageView.setImageDrawable(appIcon);
        }
        
        // 然后异步检查是否有自定义图标
        new android.os.Handler().postDelayed(() -> {
            if (iconManager != null) {
                Drawable customIcon = iconManager.getIcon(appInfo.getPackage_name());
                if (customIcon != null && imageView.getTag() == appInfo) {
                    imageView.setImageDrawable(customIcon);
                }
            }
        }, 10); // 轻微延迟，确保UI先更新
    }

    // 使用预定义的监听器避免重复创建
    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AppInfo appInfo = (AppInfo) v.getTag();
            if (appInfo == null) return;
            
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
        }
    };

    private final View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            AppInfo appInfo = (AppInfo) v.getTag();
            if (appInfo == null) return true;
            
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
        }
    };

    @Override
    public int getItemCount() {
        return appList == null ? 0 : appList.size();
    }

    @Override
    public long getItemId(int position) {
        if (appList == null || position < 0 || position >= appList.size()) 
            return RecyclerView.NO_ID;
        
        AppInfo info = appList.get(position);
        if (info == null) return position;
        
        String pkg = info.getPackage_name();
        return pkg != null ? pkg.hashCode() : position;
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.item_app_icon);
            name = itemView.findViewById(R.id.item_app_name);
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
            Toast.makeText(context, R.string.uninstall, Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnItemSizeCalculatedListener {
        void onItemSizeCalculated(int itemWidth, int itemHeight);
    }
}