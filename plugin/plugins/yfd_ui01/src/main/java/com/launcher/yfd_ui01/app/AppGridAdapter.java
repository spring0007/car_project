package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.awell.addapp.AppInfo;
import com.launcher.yfd_ui01.R;
import java.util.List;

public class AppGridAdapter extends BaseAdapter {
    private Context context;
    private List<AppInfo> appList;
    private LayoutInflater inflater;

    public AppGridAdapter(Context context, List<AppInfo> appList) {
        this.context = context;
        this.appList = appList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return appList.size();
    }

    @Override
    public Object getItem(int position) {
        return appList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app_grid, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.app_icon);
            holder.name = convertView.findViewById(R.id.app_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        // 在getView方法中，在convertView创建或复用后添加点击事件
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取点击位置对应的应用信息
                AppInfo appInfo = (AppInfo) getItem(position);

                // 启动对应的APK应用
                try {
                    Intent intent = context.getPackageManager().getLaunchIntentForPackage(appInfo.getPackage_name());
                    if (intent != null) {
                        context.startActivity(intent);
                    } else {
                        // 如果无法启动应用，可以提示用户或进行其他处理
                        Toast.makeText(context, "无法启动应用", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "启动应用时出错", Toast.LENGTH_SHORT).show();
                }
            }
        });


        AppInfo appInfo = appList.get(position);
        holder.icon.setImageDrawable(appInfo.getIcon());
        holder.name.setText(appInfo.getLabel());

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
    }
}
