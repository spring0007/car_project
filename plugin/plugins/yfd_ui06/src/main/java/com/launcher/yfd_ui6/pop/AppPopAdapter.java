package com.launcher.yfd_ui6.pop;

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
import com.launcher.yfd_ui6.R;
import com.launcher.yfd_ui6.utils.IconManager;

import java.util.List;

public class AppPopAdapter extends RecyclerView.Adapter<AppPopAdapter.Holder> {

    private final Context mContext;
    private final List<AppInfo> appList;
    //private final IconManager iconManager;
    private final OnPopupUpdateListener onPopupUpdateListener;


    public AppPopAdapter(Context context, List<AppInfo> contentList, OnPopupUpdateListener onPopupUpdateListener) {
        this.appList = contentList;
        this.mContext = context;
       // this.iconManager = IconManager.getInstance(context);
        this.onPopupUpdateListener = onPopupUpdateListener;
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
        holder.icon.setImageResource(R.drawable.host_carinfo_small);

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
            onPopupUpdateListener.updateAppImage(appInfo);
        }
    };

    private void loadAppIcon(ImageView imageView, AppInfo appInfo) {
        if (appInfo == null || imageView == null) return;
        // 然后异步检查是否有自定义图标
        new android.os.Handler().postDelayed(() -> {
          //  if (iconManager != null) {
                Drawable customIcon = IconManager.getIcon(mContext,appInfo.getPackage_name());
                if (customIcon != null) {
                    imageView.setImageDrawable(customIcon);
                }
         //   }
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
