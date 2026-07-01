package com.launcher.zy_ui03.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.awell.addapp.AddSelectAppCallback;
import com.awell.addapp.AppInfo;
import com.awell.addapp.ShowPopupI;
import com.launcher.zy_ui03.R;
import com.launcher.zy_ui03.utils.IconManager;


import java.util.ArrayList;
import java.util.List;

public class AppInofAdapter extends RecyclerView.Adapter<AppInofAdapter.ViewHolder> {

    private Context mContext;

    private final List<AppInfo> contentList = new ArrayList<>();

    // 应用名称文本颜色(支持主题切换)
    private int appNameTextColor = android.graphics.Color.WHITE; // 默认白色

    public void setContentList(List<AppInfo> contentList) {
        this.contentList.clear();
        this.contentList.addAll(contentList);
        notifyDataSetChanged();
    }
    
    /**
     * 设置应用名称的文本颜色
     * @param color 颜色值
     */
    public void setAppNameTextColor(int color) {
        this.appNameTextColor = color;
        notifyDataSetChanged();
    }

    private final ShowPopupI showPopupI;
    private final AddSelectAppCallback addSelectAppCallback;

    public AppInofAdapter(Context context, List<AppInfo> contentList, ShowPopupI showPopupI,
                          AddSelectAppCallback addSelectAppCallback) {
        this.contentList.addAll(contentList);
        this.mContext = context;
        this.showPopupI = showPopupI;
        this.addSelectAppCallback = addSelectAppCallback;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_app_rv_item, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        AppInfo appInfo = contentList.get(position);
        if (appInfo == null) return;

        if ("placeholder_mismatch".equals(appInfo.package_name)){
            holder.iv_app_icon.setImageDrawable(appInfo.getIcon());
        }else {
            Drawable icon_bg = IconManager.INSTANCE.getAppIcon(mContext, appInfo.package_name);
            if (icon_bg != null ) {
                holder.iv_app_icon.setImageDrawable(icon_bg);
            } else {

                try {
                    // 获取 PackageManager 实例
                    PackageManager pm = mContext.getPackageManager();
                    // 直接通过包名获取图标 Drawable 对象
                    Drawable icon = pm.getApplicationIcon(appInfo.package_name);
                    // 将图标设置到 ImageView 中
                    holder.iv_app_icon.setImageDrawable(icon);
                } catch (PackageManager.NameNotFoundException e) {
                    // 如果包名不存在，会抛出此异常
                    e.printStackTrace();
                    // 可以在这里设置一个默认图标
                   // holder.iv_app_icon.setImageResource(R.drawable.sf_app_navi);
                }
            }
        }
        holder.tv_app_name.setText(appInfo.getLabel());
        holder.tv_app_name.setTextColor(appNameTextColor); // 应用主题颜色

        holder.ll_item.setOnLongClickListener(view -> {
            contentList.remove(position);
            notifyDataSetChanged();
            addSelectAppCallback.removeAppInfo(appInfo.package_name);
            return true;
        });

        holder.ll_item.setOnClickListener(vie -> {
            if (appInfo == null || appInfo.package_name == null) return;
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(appInfo.package_name);

            if (intent != null) {
                mContext.startActivity(intent);
            }


        });

        if (position == contentList.size() - 1) {
            holder.ll_item.setOnClickListener(vie -> {
                showPopupI.showPopup();
            });

            holder.ll_item.setOnLongClickListener(view -> false);

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

        private RelativeLayout ll_item;
        private ImageView iv_app_icon;
        private TextView tv_app_name;
//        private ImageView iv_app_icon_bg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ll_item = itemView.findViewById(R.id.ll_item);
            iv_app_icon = itemView.findViewById(R.id.iv_app_icon);
            tv_app_name = itemView.findViewById(R.id.tv_app_name);
        }
    }
}
