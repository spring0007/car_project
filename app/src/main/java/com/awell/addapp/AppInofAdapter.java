package com.awell.addapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.awell.launcher.library.R;

import java.util.ArrayList;
import java.util.List;

public class AppInofAdapter extends RecyclerView.Adapter<AppInofAdapter.ViewHolder> {

    private Context mContext;

    private List<AppInfo> contentList = new ArrayList<>();

    public void setContentList(List<AppInfo> contentList) {
        this.contentList.clear();
        this.contentList.addAll(contentList);
        notifyDataSetChanged();
    }

    private ShowPopupI showPopupI;
    private AddSelectAppCallback addSelectAppCallback;

    public AppInofAdapter(Context context, List<AppInfo> contentList, ShowPopupI showPopupI, AddSelectAppCallback addSelectAppCallback) {
        this.contentList.addAll(contentList);
        this.mContext = context;
        this.showPopupI = showPopupI;
        this.addSelectAppCallback = addSelectAppCallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_app_rv_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        AppInfo appInfo = contentList.get(position);
        if (appInfo == null) return;
        holder.iv_app_icon.setImageDrawable(appInfo.getIcon());
        holder.tv_app_name.setText(appInfo.getLabel());
        holder.iv_app_icon_bg.setBackground(appInfo.getIcon_bg());

        holder.iv_app_layout.setOnLongClickListener(view -> {
            contentList.remove(position);
            notifyDataSetChanged();
            addSelectAppCallback.removeAppInfo(appInfo.package_name);
            return true;
        });

        holder.iv_app_layout.setOnClickListener(vie -> {
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(appInfo.package_name);
            mContext.startActivity(intent);
        });

        if (position == contentList.size() - 1) {
            holder.iv_app_layout.setOnClickListener(vie -> {
                showPopupI.showPopup();
            });

            holder.iv_app_layout.setOnLongClickListener(view -> false);

//            Log.e("getwidth"," width = " + holder.ll_item.getLayoutParams().width);
            /*RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(65,65);
            layoutParams.setMargins(32,30,0,0);
            holder.iv_app_icon.setLayoutParams(layoutParams);*/
        }

    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private RelativeLayout iv_app_layout;
        private LinearLayout ll_item;
        private ImageView iv_app_icon, iv_app_icon_bg;
        private TextView tv_app_name;
//        private ImageView iv_app_icon_bg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ll_item = itemView.findViewById(R.id.ll_item);
            iv_app_icon = itemView.findViewById(R.id.iv_app_icon);
            tv_app_name = itemView.findViewById(R.id.tv_app_name);
            iv_app_icon_bg = itemView.findViewById(R.id.iv_app_icon_bg);
            iv_app_layout = itemView.findViewById(R.id.app_layout);
        }
    }
}
