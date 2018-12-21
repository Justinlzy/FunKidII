package com.cqkct.FunKidII.service;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;

import com.cqkct.FunKidII.Utils.L;

import java.io.File;
import java.util.Arrays;

public class DownloadManagerReceiver extends BroadcastReceiver {
    private static final String TAG = DownloadManagerReceiver.class.getSimpleName();

    private DownloadManager getDownloadManager(Context context) {
        return  (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            L.d(TAG, "用户点击了通知");

            // 点击下载进度通知时, 对应的下载ID以数组的方式传递
            long[] ids = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
            L.d(TAG, "ACTION_NOTIFICATION_CLICKED: ids: " + Arrays.toString(ids));

            DownloadManager downloadManager = getDownloadManager(context);
            for (long id : ids) {
                // 创建一个查询对象
                DownloadManager.Query query = new DownloadManager.Query();
                // 根据 下载ID 过滤结果
                query.setFilterById(id);
                // 执行查询, 返回一个 Cursor (相当于查询数据库)
                Cursor cursor = downloadManager.query(query);
                if (cursor == null)
                    continue;
                try {
                    if (!cursor.moveToFirst()) {
                        L.e(TAG, "ACTION_DOWNLOAD_COMPLETE downloadManager.query cursor.moveToFirst() return false");
                        continue;
                    }
                    String uriStr = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
                    if (!uriStr.startsWith(OkHttpRequestManager.APP_UPDATE_DOWNLOAD)) {
                        // 不是app更新的下载
                        L.v(TAG, "ACTION_DOWNLOAD_COMPLETE URI(" + uriStr + ") not app download URI(" + OkHttpRequestManager.APP_UPDATE_DOWNLOAD + ")");
                        continue;
                    }

                    // 下载请求的状态
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status != DownloadManager.STATUS_SUCCESSFUL) {
                        L.e(TAG, "ACTION_DOWNLOAD_COMPLETE failure (" + status + ")");
                        continue;
                    }

                    // 下载文件在本地保存的路径（Android 7.0 以后 COLUMN_LOCAL_FILENAME 字段被弃用, 需要用 COLUMN_LOCAL_URI 字段来获取本地文件路径的 Uri）
                    String fileUriStr = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    Uri fileUri = Uri.parse(fileUriStr);
                    installApp(context, fileUri);
                } finally {
                    cursor.close();
                }
            }

        } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            L.d(TAG, "下载完成");

            /*
             * 获取下载完成对应的下载ID, 这里下载完成指的不是下载成功, 下载失败也算是下载完成,
             * 所以接收到下载完成广播后, 还需要根据 id 手动查询对应下载请求的成功与失败.
             */
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            L.d(TAG, "ACTION_DOWNLOAD_COMPLETE: id: " + id);

            // 根据获取到的ID，查询是否下载成功


            DownloadManager downloadManager = getDownloadManager(context);
            // 创建一个查询对象
            DownloadManager.Query query = new DownloadManager.Query();
            // 根据 下载ID 过滤结果
            query.setFilterById(id);
            // 执行查询, 返回一个 Cursor (相当于查询数据库)
            Cursor cursor = downloadManager.query(query);
            if (cursor == null)
                return;
            try {
                if (!cursor.moveToFirst()) {
                    L.e(TAG, "ACTION_DOWNLOAD_COMPLETE downloadManager.query cursor.moveToFirst() return false");
                    return;
                }
                String uriStr = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
                if (!uriStr.startsWith(OkHttpRequestManager.APP_UPDATE_DOWNLOAD)) {
                    // 不是app更新的下载
                    L.v(TAG, "ACTION_DOWNLOAD_COMPLETE URI(" + uriStr + ") not app download URI(" + OkHttpRequestManager.APP_UPDATE_DOWNLOAD + ")");
                    return;
                }

                // 下载请求的状态
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    L.e(TAG, "ACTION_DOWNLOAD_COMPLETE failure (" + status + ")");
                    return;
                }

                // 下载文件在本地保存的路径（Android 7.0 以后 COLUMN_LOCAL_FILENAME 字段被弃用, 需要用 COLUMN_LOCAL_URI 字段来获取本地文件路径的 Uri）
                String fileUriStr = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                Uri fileUri = Uri.parse(fileUriStr);
                installApp(context, fileUri);
            } finally {
                cursor.close();
            }
        }
    }

    private void installApp(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file;
            if (uri.getScheme().toLowerCase().equals("file")) {
                file = new File(uri.getPath());
            } else {
                // see AppSettingActivity.downloadNewVersionApp()
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FunKidII.apk");
            }
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", // provider 的 authorities
                    file);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}