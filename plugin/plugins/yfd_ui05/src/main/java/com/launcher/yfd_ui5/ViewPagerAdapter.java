package com.launcher.yfd_ui5;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.awell.library.util.PageData;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {

    private List<PageData> pageDataList;
    private MainActivityUI5 activity;

    public ViewPagerAdapter(List<PageData> pageDataList, MainActivityUI5 activity) {
        this.pageDataList = pageDataList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ViewPagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewPagerViewHolder holder, int position) {
        // 在第一个页面中查找并初始化 MusicWidget
        if (position == 0) {
            View musicWidgetView = holder.itemView.findViewById(R.id.music_widget_layout);
            if (musicWidgetView instanceof MusicWidget && activity != null) {
                activity.setMusicWidget((MusicWidget) musicWidgetView);
            }
            
            // 直接在 ViewHolder 中更新蓝牙状态，而不是通过 Activity
            updateBtStatusInViewHolder(holder.itemView);
        }
    }
    
    /**
     * 在 ViewHolder 中直接更新蓝牙状态
     */
    private void updateBtStatusInViewHolder(View itemView) {
        TextView btPhoneSubTv = itemView.findViewById(R.id.bt_phone_sub_tv);
        if (btPhoneSubTv != null && activity != null) {
            int mBTStatus = Settings.System.getInt( activity.getContentResolver(), "awell_bt_status", 0);
            Log.d("ViewPagerAdapter", "mBTStatus = " + mBTStatus);
            
            if (mBTStatus == 0 || mBTStatus == 1){
                btPhoneSubTv.setText(R.string.no_phone_connected);
            } else if (mBTStatus == 2){
                btPhoneSubTv.setText(R.string.phone_connected);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return pageDataList.get(position).getLayoutResId();
    }

    @Override
    public int getItemCount() {
        return pageDataList.size();
    }

    static class ViewPagerViewHolder extends RecyclerView.ViewHolder {
        public ViewPagerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
