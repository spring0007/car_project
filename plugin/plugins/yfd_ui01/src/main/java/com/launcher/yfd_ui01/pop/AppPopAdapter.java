package com.launcher.yfd_ui01.pop;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.addapp.AppInfo;
import com.launcher.yfd_ui01.R;
import com.launcher.yfd_ui01.app.IconManager;

import java.util.List;

public class AppPopAdapter extends RecyclerView.Adapter<AppPopAdapter.Holder> {

    private final Context mContext;
    private final List<AppInfo> appList;
    private final IconManager iconManager;
    private final OnPopupActionListener mPopupActionListener;


    public AppPopAdapter(Context context, List<AppInfo> contentList, OnPopupActionListener popupActionListener) {
        this.appList = contentList;
        this.mContext = context;
        this.iconManager = IconManager.getInstance(context);
        this.mPopupActionListener = popupActionListener;
    }

    public void setContentList(List<AppInfo> contentList) {
        this.appList.clear();
        this.appList.addAll(contentList);
    }
    @NonNull
    @Override
    public AppPopAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_grid, parent, false);
        return new AppPopAdapter.Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AppPopAdapter.Holder holder, int position) {
        if (appList == null || position < 0 || position >= appList.size()) return;

        AppInfo appInfo = appList.get(position);
        holder.name.setText(appInfo.getLabel());

        // 优化图标加载：先设置占位符，再异步加载图标
        holder.icon.setImageResource(com.launcher.yfd_ui01.R.drawable.ic_app_placeholder);

        // 异步加载图标
        loadAppIcon(holder.icon, appInfo);

        // 预加载点击事件所需的资源
        holder.itemView.setTag(appInfo);
        holder.itemView.setOnClickListener(clickListener);
    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AppInfo appInfo = (AppInfo) v.getTag();
            if (appInfo == null) return;
            mPopupActionListener.updateImage(appInfo);
        }
    };

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

    @Override
    public int getItemCount() {
        return appList.size();
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
}
