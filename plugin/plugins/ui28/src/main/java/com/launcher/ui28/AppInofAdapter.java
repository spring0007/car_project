package com.launcher.ui28;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.addapp.AddSelectAppCallback;
import com.awell.addapp.AppInfo;
import com.awell.addapp.ShowPopupI;

import java.util.ArrayList;
import java.util.List;

public class AppInofAdapter extends RecyclerView.Adapter<AppInofAdapter.ViewHolder> {

    // 首页最多 6 格:6 个应用,或 5 个应用 + 1 个添加按钮
    private static final int MAX_ITEM_COUNT = 6;
    private static final String PLACEHOLDER_PACKAGE = "placeholder_mismatch";

    private final List<AppInfo> contentList = new ArrayList<>();
    private final ShowPopupI showPopupI;
    private final AddSelectAppCallback addSelectAppCallback;

    public AppInofAdapter(List<AppInfo> contentList, ShowPopupI showPopupI,
                          AddSelectAppCallback addSelectAppCallback) {
        this.showPopupI = showPopupI;
        this.addSelectAppCallback = addSelectAppCallback;
        setContentList(contentList);
    }

    public void setContentList(List<AppInfo> list) {
        int count = Math.min(list.size(), MAX_ITEM_COUNT);
        List<AppInfo> limited = new ArrayList<>(list.subList(0, count));
        contentList.clear();
        contentList.addAll(limited);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_app_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo appInfo = contentList.get(position);
        if (appInfo == null) {
            return;
        }
        if (PLACEHOLDER_PACKAGE.equals(appInfo.package_name)) {
            holder.ivAppIcon.setImageResource(R.drawable.sf_app_add_icon);
        } else {
            ImageManager.loadIcon(holder.itemView.getContext(), holder.ivAppIcon, appInfo.package_name);
        }
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    private AppInfo getAppInfo(int position) {
        return position >= 0 && position < contentList.size() ? contentList.get(position) : null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAppIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon);
            View clickView = itemView.findViewById(R.id.app_layout);

            clickView.setOnClickListener(v -> {
                AppInfo appInfo = getAppInfo(getAdapterPosition());
                if (appInfo == null) {
                    return;
                }
                if (PLACEHOLDER_PACKAGE.equals(appInfo.package_name)) {
                    showPopupI.showPopup();
                } else {
                    openApp(itemView.getContext(), appInfo.package_name);
                }
            });

            clickView.setOnLongClickListener(v -> {
                AppInfo appInfo = getAppInfo(getAdapterPosition());
                if (appInfo == null || PLACEHOLDER_PACKAGE.equals(appInfo.package_name)) {
                    return false;
                }
                addSelectAppCallback.removeAppInfo(appInfo.package_name);
                return true;
            });
        }
    }

    private static void openApp(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
