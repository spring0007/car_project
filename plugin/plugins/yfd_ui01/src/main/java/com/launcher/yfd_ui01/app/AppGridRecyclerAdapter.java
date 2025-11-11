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

/**
 * RecyclerView adapter for a single page (grid) of apps.
 */
public class AppGridRecyclerAdapter extends RecyclerView.Adapter<AppGridRecyclerAdapter.Holder> {
    private final Context context;
    private List<AppInfo> appList;

    public AppGridRecyclerAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
        // 启用稳定ID以提高性能
        setHasStableIds(true);
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
                
                int column = position % spanCount;
                
                if (includeEdge) {
                    // left and right spacing: distribute so that edges get spacing and inter-item gaps are spacing
                    float leftPercent = (float)(spanCount - column) / spanCount;
                    float rightPercent = (float)(column + 1) / spanCount;

                    outRect.left = (int)(leftSpacing * leftPercent);
                    outRect.right = (int)(leftSpacing * rightPercent);

                    /// 上下间距
                    if (position < spanCount) { // top edge
                        outRect.top = topSpacing;
                    }
                    outRect.bottom = topSpacing; // item bottom
                } else {
                    // no edge spacing: full spacing only between items
                    float leftPercent = (float)(spanCount - column - 1) / spanCount;
                    float rightPercent = (float)column / spanCount;

                    outRect.left = (int)(leftSpacing * leftPercent);
                    outRect.right = (int)(leftSpacing * rightPercent);

                    if (position >= spanCount) {
                        outRect.top = topSpacing; // item top
                    }
                    outRect.bottom =0;

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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_grid, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (appList == null || position < 0 || position >= appList.size()) return;
        
        AppInfo appInfo = appList.get(position);
        holder.name.setText(appInfo.getLabel());

        // 优化图片加载：直接设置图片，避免异常处理的开销
        holder.icon.setImageDrawable(appInfo.getIcon());
        
        // 预加载点击事件所需的资源
        holder.itemView.setTag(appInfo);
        holder.itemView.setOnClickListener(clickListener);
        holder.itemView.setOnLongClickListener(longClickListener);
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