package com.cqkct.FunKidII.Utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import com.cqkct.FunKidII.R;


/**
 * 判断是否有网络工具类
 * @author zhaixiang$.
 * @explain
 * @time 2016/8/13$ 14:40$.
 */
public class NetWorkUtils {

    /**
     * 检测网络是否可用
     * @param context
     * @return
     */
    public static boolean isConnect(Context context) {

        boolean flag = false;
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                // 获取网络连接管理的对象
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    // 判断当前网络是否已经连接
                    if (info.getState() == NetworkInfo.State.CONNECTED) {
                        flag = info.isAvailable();
                        return flag;
                    }
                }else{
                    setNetwork(context);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 网络未连接时，调用设置方法
     */
    private static void setNetwork(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(context.getString(R.string.network_prompt));
        builder.setMessage(context.getString(R.string.net_error));
        builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = null;
                /**
                 * 判断手机系统的版本！如果API大于10 就是3.0+
                 * 因为3.0以上的版本的设置和3.0以下的设置不一样，调用的方法不同
                 */
                if (Build.VERSION.SDK_INT > 10) {
                    intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                } else {
                    intent = new Intent();
                    ComponentName component = new ComponentName(
                            "com.android.settings",
                            "com.android.settings.WirelessSettings");
                    intent.setComponent(component);
                    intent.setAction("android.intent.action.VIEW");
                }
                context.startActivity(intent);
            }
        });
        builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create();
        builder.show();
    }
}
