package com.launcher.yfd_ui01.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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
                        Toast.makeText(context, R.string.unable_launch_application, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, R.string.error_launching_application, Toast.LENGTH_SHORT).show();
                }
            }
        });
        // 在 AppGridAdapter.java 的 getView 方法中找到 setOnLongClickListener 部分
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    // 获取当前应用信息
                    AppInfo appInfo = (AppInfo) getItem(position);
                    if (appInfo == null) {
                        return false;
                    }

                    String packageName = appInfo.getPackage_name();

                    // 检查是否为系统应用或不能卸载的应用
                    if (isSystemApp(packageName)) {
                        // 提示用户不能卸载系统应用
                        Toast.makeText(context, R.string.unable_uninstall_system_applications, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    // 检查是否为当前Launcher应用
                    if (isCurrentLauncherApp(packageName)) {
                        Toast.makeText(context, R.string.unable_uninstall_current_desktop_application, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    // 执行卸载操作
                    uninstallApp(packageName);

                } catch (Exception e) {
                    Toast.makeText(context, R.string.error_uninstalling_application, Toast.LENGTH_SHORT).show();
                }
                return true;
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
            Toast.makeText(context, "无法启动卸载程序", Toast.LENGTH_SHORT).show();
        }
    }

}
