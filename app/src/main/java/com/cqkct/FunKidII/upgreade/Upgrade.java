package com.cqkct.FunKidII.upgreade;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Utils.AndroidPermissions;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.service.OkHttpRequestManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public class Upgrade {
    private static final String TAG = Upgrade.class.getSimpleName();

    private BaseActivity mActivity;
    private int mWriteExternalStoragePermissionRequestCode;

    /**
     * App 升级 utility
     * @param activity Activity
     * @param writeExternalStoragePermissionRequestCode requestPermissions 请求码，以及 startActivityForResult 请求码
     *                                                  需要在 onRequestPermissionsResult 和 onActivityResult 使用。
     */
    public Upgrade(BaseActivity activity, int writeExternalStoragePermissionRequestCode) {
        mActivity = activity;
        mWriteExternalStoragePermissionRequestCode = writeExternalStoragePermissionRequestCode;
    }

    public void upgradeGuide(String ver) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (!hasStorePermission()) {
                return;
            }

            ConnectivityManager connectivityManager = getConnectivityManager();
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                if (networkInfo.isAvailable() && networkInfo.isConnected()) {
                    showMobileNetworkTip(ver);
                    return;
                }
            }
            downloadNewVersionApp(ver, DownloadManager.Request.NETWORK_WIFI);
        } else {
            mActivity.toast(R.string.download_sd_card_not_use);
        }
    }

    private void showStorePermissionGuide() {
        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.external_storage_permission)
                .setMessage(R.string.please_enable_external_storage_permission_in_setting)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = AndroidPermissions.permissionSettingPageIntent(mActivity);
                        mActivity.startActivityForResult(intent, mWriteExternalStoragePermissionRequestCode);
                    }
                })
                .show();
    }

    private boolean hasStorePermission() {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (AndroidPermissions.shouldShowGuide(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showStorePermissionGuide();
            } else {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, mWriteExternalStoragePermissionRequestCode);
            }
            return false;
        } else {
            return true;
        }
    }

    private ConnectivityManager mConnectivityManager;
    private synchronized ConnectivityManager getConnectivityManager() {
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        return mConnectivityManager;
    }

    private void downloadNewVersionApp(String ver, int networkType) {
        final String packageName = "com.android.providers.downloads";
        int state = mActivity.getPackageManager().getApplicationEnabledSetting(packageName);
        //检测下载管理器是否被禁用
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.download_hint)
                    .setMessage(R.string.download_download_manager_ban)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            try {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + packageName));
                                mActivity.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                mActivity.startActivity(intent);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(OkHttpRequestManager.APP_UPDATE_DOWNLOAD));
        // 设置下载的文件存储的地址，我们这里将下载的apk文件存在 /Download 目录下面
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "FunKidII.apk");
        // 设置下载文件类型
        request.setMimeType("application/vnd.android.package-archive");
        // 设置现在的文件可以被 MediaScanner 扫描到。
        request.allowScanningByMediaScanner();

        // 设置允许使用的网络类型
        request.setAllowedNetworkTypes(networkType);
        if (networkType == DownloadManager.Request.NETWORK_WIFI) {
            request.setAllowedOverMetered(false);
        } else {
            request.setAllowedOverMetered(true);
        }

        // 设置通知的标题
        request.setTitle(mActivity.getString(R.string.app_name) + ver);
        // 设置下载的时候Notification的可见性。
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDescription("");
        request.setVisibleInDownloadsUi(true);


        // 获取下载管理器服务的实例, 添加下载任务
        DownloadManager downloadManager = getDownloadManager();
        // 将下载请求加入下载队列, 返回一个下载ID
        long downloadId = downloadManager.enqueue(request);
        if (downloadId == -1L) {
            L.e(TAG, "downloadNewVersionApp downloadManager.enqueue(request) return -1");
        }

        new DownloadingDialog(this, downloadId).show();
    }

    private void showMobileNetworkTip(final String ver) {
        new AlertDialog.Builder(mActivity)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.app_upgrade)
                .setMessage(R.string.download_now_use_phone_network)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadNewVersionApp(ver, DownloadManager.Request.NETWORK_MOBILE);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    private static class DownloadingDialog {
        private WeakReference<Upgrade> mU;
        private long mDownloadId;
        private AlertDialog dialog;
        private Timer timer;
        private TimerTask timerTask;
        private TextView statusView;
        private ProgressBar progressBar;
        private TextView progressView, totalView;

        DownloadingDialog(Upgrade u, long downloadId) {
            mU = new WeakReference<>(u);
            mDownloadId = downloadId;

            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Upgrade u = mU.get();
                    if (u == null) {
                        timer.cancel();
                        return;
                    }
                    u.mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            queryProgress();
                        }
                    });
                }
            };

            View view = u.mActivity.getLayoutInflater().inflate(R.layout.downloading_dialog_layout, null);
            dialog = new AlertDialog.Builder(u.mActivity)
                    .setTitle(R.string.app_upgrade)
                    .setView(view)
                    .setCancelable(false)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Upgrade u = mU.get();
                            if (u == null)
                                return;
                            u.getDownloadManager().remove(mDownloadId);
                            timer.cancel();
                        }
                    })
                    .setNeutralButton(R.string.hide, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            statusView = view.findViewById(R.id.status);
            progressBar = view.findViewById(R.id.progress_bar);
            progressView = view.findViewById(R.id.progress_text);
            totalView = view.findViewById(R.id.total);

            progressBar.setProgress(0);
            progressView.setText(Utils.bytesText(0));
            totalView.setText(Utils.bytesText(0));
            queryProgress();
        }

        public void show() {
            dialog.show();
            timer.schedule(timerTask, 0, 1000);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    timer.cancel();
                }
            });
        }

        private void queryProgress() {
            Upgrade u = mU.get();
            if (u == null)
                return;
            DownloadManager downloadManager = u.getDownloadManager();
            if (downloadManager == null)
                return;
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(mDownloadId));
            if (cursor == null)
                return;
            try {
                if (!cursor.moveToFirst()) {
                    statusView.setText(R.string.download_manager_canceled);
                    Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    positiveButton.setText(R.string.ok);
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });
                    positiveButton.setVisibility(View.VISIBLE);

                    Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                    negativeButton.setVisibility(View.GONE);

                    Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                    neutralButton.setVisibility(View.GONE);
                    return;
                }
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                // 已经下载的字节数
                long bytesDownload = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                int progress = 0;
                if (bytesTotal > 0) {
                    progress = (int) (bytesDownload * 100 / bytesTotal);
                }
                L.d(TAG, "queryProgress status=" + status + ", bytesDownload=" + bytesDownload + " bytesTotal=" + bytesTotal + " progress=" + progress);
                String statusString;
                switch (status) {
                    case DownloadManager.STATUS_PENDING:
                        statusString = u.mActivity.getString(R.string.download_manager_STATUS_PENDING);
                        break;
                    case DownloadManager.STATUS_RUNNING:
                        statusString = u.mActivity.getString(R.string.download_manager_STATUS_RUNNING);
                        break;
                    case DownloadManager.STATUS_PAUSED:
//                        statusString = u.mActivity.getString(R.string.download_manager_STATUS_PAUSED);
                        statusString = u.mActivity.getString(R.string.download_manager_STATUS_RUNNING);
                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        statusString = u.mActivity.getString(R.string.download_manager_STATUS_SUCCESSFUL);
                        progress = 100;
//                        bytesDownload = bytesTotal;
                        timer.cancel();
                        break;
                    case DownloadManager.STATUS_FAILED:
                    default:
                        statusString = u.mActivity.getString(R.string.download_manager_STATUS_FAILED);
                        timer.cancel();
                        break;
                }
                statusView.setText(statusString);
                progressBar.setProgress(progress);
                progressView.setText(Utils.bytesText(bytesDownload));
                totalView.setText(Utils.bytesText(bytesTotal < 0 ? 0 : bytesTotal));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    String uriStr = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    final Uri uri = Uri.parse(uriStr);
                    Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    positiveButton.setText(R.string.install);
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Upgrade u = mU.get();
                            if (u == null)
                                return;
                            u.installApp(uri);
                            dialog.dismiss();
                        }
                    });
                    positiveButton.setVisibility(View.VISIBLE);

                    Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                    negativeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                    neutralButton.setVisibility(View.GONE);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private DownloadManager mDownloadManager;
    private synchronized DownloadManager getDownloadManager() {
        if (mDownloadManager == null) {
            mDownloadManager = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        }
        return mDownloadManager;
    }

    private void installApp(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file;
            if (uri.getScheme().toLowerCase().equals("file")) {
                file = new File(uri.getPath());
            } else {
                // see AppSettingActivity.downloadNewVersionApp()
                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FunKidII.apk");
            }
            uri = FileProvider.getUriForFile(mActivity,
                    mActivity.getPackageName() + ".provider", // provider 的 authorities
                    file);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            mActivity.startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivity(intent);
        }
    }
}
