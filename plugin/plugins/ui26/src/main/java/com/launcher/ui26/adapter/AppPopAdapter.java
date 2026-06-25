package com.launcher.ui26.adapter;

import android.content.Context;
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
import com.awell.launcher.library.R;

import java.util.List;

public class AppPopAdapter extends RecyclerView.Adapter<AppPopAdapter.ViewHolder> {

    private Context mContext;
    private List<AppInfo> contentList;

    public void setContentList(List<AppInfo> contentList) {
        this.contentList.clear();
        this.contentList.addAll(contentList);
    }

    private AddSelectAppCallback addSelectAppCallback;

    public AppPopAdapter(Context context, List<AppInfo> contentList, AddSelectAppCallback addSelectAppCallback) {
        this.contentList = contentList;
        this.mContext = context;
        this.addSelectAppCallback = addSelectAppCallback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_app_rv_item2, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        AppInfo appInfo = contentList.get(position);
        holder.iv_app_icon.setImageDrawable(appInfo.getIcon());
        holder.tv_app_name.setText(appInfo.getLabel());

        String pckName = appInfo.getPackage_name();

         /*int r = new Random().nextInt(4);
        holder.iv_app_icon_bg.setImageResource(Utils.iconBg[r]);
        Log.e("kkkkkkkk"," r = " + r);*/

     /*   holder.ll_item.setOnLongClickListener(view -> {
            contentList.remove(position);
            notifyDataSetChanged();
            return true;
        });*/

        holder.ll_item.setOnClickListener(vie -> {
            /*Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(appInfo.package_name);
            mContext.startActivity(intent);*/
            addSelectAppCallback.addAppInfo(appInfo);
        });


    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private RelativeLayout ll_item;
        private ImageView iv_app_icon;
        private TextView tv_app_name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ll_item = itemView.findViewById(R.id.ll_item2);
            iv_app_icon = itemView.findViewById(R.id.iv_app_icon2);
            tv_app_name = itemView.findViewById(R.id.tv_app_name2);
        }
    }
}
