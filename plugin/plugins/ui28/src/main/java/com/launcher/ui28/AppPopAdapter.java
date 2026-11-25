package com.launcher.ui28;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.addapp.AddSelectAppCallback;
import com.awell.addapp.AppInfo;

import java.util.List;

/**
 * 应用选择弹窗的列表适配器
 */
public class AppPopAdapter extends RecyclerView.Adapter<AppPopAdapter.ViewHolder> {

    private final List<AppInfo> contentList;
    private final AddSelectAppCallback addSelectAppCallback;

    public AppPopAdapter(List<AppInfo> contentList, AddSelectAppCallback addSelectAppCallback) {
        this.contentList = contentList;
        this.addSelectAppCallback = addSelectAppCallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_pop_app_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo appInfo = contentList.get(position);
        ImageManager.loadIcon(holder.itemView.getContext(), holder.ivAppIcon, appInfo.package_name);
        holder.tvAppName.setText(appInfo.getLabel());
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout llItem;
        private final ImageView ivAppIcon;
        private final TextView tvAppName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llItem = itemView.findViewById(R.id.ll_item2);
            ivAppIcon = itemView.findViewById(R.id.iv_app_icon2);
            tvAppName = itemView.findViewById(R.id.tv_app_name2);

            llItem.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position >= 0 && position < contentList.size()) {
                    addSelectAppCallback.addAppInfo(contentList.get(position));
                }
            });
        }
    }
}
