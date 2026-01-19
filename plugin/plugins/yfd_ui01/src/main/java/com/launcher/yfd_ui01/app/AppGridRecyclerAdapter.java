package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.Rect;

import com.awell.addapp.AppInfo;
import com.launcher.yfd_ui01.R;
import com.launcher.yfd_ui01.utils.LogUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RecyclerView adapter for a single page (grid) of apps.
 */
public class AppGridRecyclerAdapter extends RecyclerView.Adapter<AppGridRecyclerAdapter.Holder> {
    private final Context context;
    private final List<AppInfo> appList;
    private final IconManager iconManager;
    private OnItemSizeCalculatedListener sizeListener;
    private boolean hasCalculatedSize = false;
    public AppGridRecyclerAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = new ArrayList<>(appList);
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

        loadIconAsync(holder.icon, appInfo.package_name);
        //appList.set(position,appInfo); //替换含有图片的info
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
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private void loadIconAsync(ImageView imageView, String packageName) {
        imageView.setImageResource(R.drawable.yfd_ui1_apk_installer);  // 先设置占位符

        executor.execute(() -> {
            try {
                if (iconManager != null) {
                    //然后异步检查是否有自定义图标
                    Drawable customIcon = iconManager.getIcon(packageName);
                    if (customIcon != null) {
                        imageView.setImageDrawable(customIcon);
                        return;
                    }
                }

                PackageManager pm = imageView.getContext().getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                Drawable icon = appInfo.loadIcon(pm);

                handler.post(() -> {
                    if (imageView.getTag() != null && imageView.getTag().equals(packageName)) {
                        imageView.setImageDrawable(icon);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        // 保存当前加载的包名到Tag
        imageView.setTag(packageName);
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

    /**
     * 安全设置数据列表
     */
    public void setAppList(List<AppInfo> newList) {
        // 检查是否需要更新
        if (appList == newList ) {
            return; // 同一个引用，无需更新
        }

        // 2. 确保在主线程执行
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            new Handler(Looper.getMainLooper()).post(() -> setAppList(newList));
            return;
        }

        // 3. 暂停动画
        if (recyclerViewRef != null && recyclerViewRef.get() != null) {
            Objects.requireNonNull(recyclerViewRef.get().getItemAnimator()).endAnimations();
        }

        try {
            // 4. 安全地创建旧列表的拷贝（避免 SubList 问题）
            List<AppInfo> oldListCopy;
            if (appList == null || appList.isEmpty()) {
                oldListCopy = new ArrayList<>();
            } else {
                // 使用手动复制方式，避免使用 SubList 的 toArray 方法
                oldListCopy = new ArrayList<>(appList.size());
                oldListCopy.addAll(appList);
            }

            // 5. 安全地创建新列表的拷贝
            List<AppInfo> newListCopy;
            if (newList == null || newList.isEmpty()) {
                newListCopy = new ArrayList<>();
            } else {
                // 同样使用手动复制
                newListCopy = new ArrayList<>(newList.size());
                newListCopy.addAll(newList);
            }

            // 6. 计算差异
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                    new AppDiffCallback(oldListCopy, newListCopy),
                    true
            );

            // 7. 更新数据源
            appList.clear();
            appList.addAll(newListCopy);

            // 8. 应用更新
            diffResult.dispatchUpdatesTo(this);

        } catch (Exception e) {
            // 如果发生异常，回退到简单的 notifyDataSetChanged
            LogUtil.e("Error updating adapter data", e);
            if (newList != null) {
                appList.clear();
                appList.addAll(newList);
            } else {
                appList.clear();
            }
            notifyDataSetChanged();
        }
    }

    // 添加对 RecyclerView 的弱引用
    private WeakReference<RecyclerView> recyclerViewRef;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerViewRef = new WeakReference<>(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (recyclerViewRef != null) {
            recyclerViewRef.clear();
            recyclerViewRef = null;
        }
    }

    // 添加 DiffUtil.Callback 内部类
    private static class AppDiffCallback extends DiffUtil.Callback {
        private final List<AppInfo> oldList;
        private final List<AppInfo> newList;

        public AppDiffCallback(List<AppInfo> oldList, List<AppInfo> newList) {
            this.oldList = oldList != null ? oldList : new ArrayList<>();
            this.newList = newList != null ? new ArrayList<>() : new ArrayList<>();
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            AppInfo oldItem = oldList.get(oldItemPosition);
            AppInfo newItem = newList.get(newItemPosition);
            // 使用包名作为唯一标识
            return oldItem != null && newItem != null &&
                    oldItem.getPackage_name().equals(newItem.getPackage_name());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AppInfo oldItem = oldList.get(oldItemPosition);
            AppInfo newItem = newList.get(newItemPosition);

            if (oldItem == null || newItem == null) return false;

            // 比较应用标签是否相同
            boolean labelSame = oldItem.getLabel().equals(newItem.getLabel());
            // 这里可以添加其他需要比较的字段
            return labelSame;
        }

        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            // 如果需要部分更新，可以返回具体的变更信息
            // 例如，如果只是图标更新，可以返回一个标志位
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}