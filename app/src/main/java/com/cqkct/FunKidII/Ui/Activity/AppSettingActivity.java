package com.cqkct.FunKidII.Ui.Activity;

import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Ui.view.NumberPickerBlue;
import com.cqkct.FunKidII.Utils.CacheUtil;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PreferencesUtils;
import com.cqkct.FunKidII.Utils.UTIL;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.service.OkHttpRequestManager;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;
import com.cqkct.FunKidII.upgreade.Upgrade;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


/**
 * Created by Administrator on 2017/8/8.
 */

public class AppSettingActivity extends BaseActivity {
    private static final String TAG = AppSettingActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private SwitchCompat iv_ring, iv_shake;

    private int notificationConf;
    private boolean mAreNotificationsEnabled;
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
    private static final int ACTIVITY_REQUEST_CODE_ENABLE_NOTIFICATIONS = 1;
    private TextView tv_cache_size, tvArea;

    private CalcCacheSizeTask mCalcCacheSizeTask; // 由 tv_cache_size 保护
    private CleanCacheTask mCleanCacheTask; // 由 tv_cache_size 保护
    private int areaType;

    //下载进度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_setting);
        setTitleBarTitle(R.string.app_setting);
        initView();
        synchronized (tv_cache_size) {
            mCalcCacheSizeTask = new CalcCacheSizeTask(this);
            mCalcCacheSizeTask.execute();
        }
    }

    private void initView() {
        tv_cache_size = findViewById(R.id.cache_size);
        tvArea = findViewById(R.id.app_area);
        ((TextView) findViewById(R.id.version_text)).setText(String.valueOf("v" + UTIL.getVersion(this)));

        iv_ring = findViewById(R.id.switch_ring);
        iv_ring.setOnCheckedChangeListener(soundOnCheckedChangeListener);
        iv_shake = findViewById(R.id.switch_shake);
        iv_shake.setOnCheckedChangeListener(vibrateOnCheckedChangeListener);
        tvArea = findViewById(R.id.app_area);
        notificationConf = getPreferencesWrapper().getNotificationConf();
        areaType = getPreferencesWrapper().getAppArea();
        setTvArea(tvArea, areaType);

    }

    private void showAreaDialog(Context context) {
        final int[] editArea = new int[1];
        final String[] displayValues = new String[]{context.getString(R.string.internal_version), context.getString(R.string.other_area)};

        View mView = LayoutInflater.from(context).inflate(R.layout.app_set_area_dialog, null);
        final Dialog alertDialog = createDialog(context, mView);
        alertDialog.show();

        TextView title = mView.findViewById(R.id.dialog_title);
        title.setText(R.string.select_areas);

        NumberPickerBlue numberPicker = mView.findViewById(R.id.sex_pick);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(displayValues.length - 1);
        numberPicker.setDisplayedValues(displayValues);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        Utils.setNumberPickerDividerColor(numberPicker, context);


        editArea[0] = areaType <= 0 ? 0 : areaType - 1;
        numberPicker.setValue(editArea[0]);

        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> editArea[0] = newVal);

        mView.findViewById(R.id.ok).setOnClickListener(v -> {
            alertDialog.dismiss();
            areaType = editArea[0] == 0 ? PreferencesWrapper.APP_AREA_CHINA : PreferencesWrapper.APP_AREA_OVER_SEA;
            saveArea(areaType);
            setTvArea(tvArea, areaType);

        });
        mView.findViewById(R.id.cancel).setOnClickListener(v -> alertDialog.dismiss());

    }

    private void saveArea(int notificationConf) {
        switch (notificationConf) {
            case PreferencesWrapper.APP_AREA_CHINA:
                getPreferencesWrapper().setAppArea(PreferencesWrapper.APP_AREA_CHINA);
                EventBus.getDefault().postSticky(new Event.AppAreaSwitched(PreferencesWrapper.APP_AREA_CHINA));
                break;
            case PreferencesWrapper.APP_AREA_OVER_SEA:
                getPreferencesWrapper().setAppArea(PreferencesWrapper.APP_AREA_OVER_SEA);
                EventBus.getDefault().postSticky(new Event.AppAreaSwitched(PreferencesWrapper.APP_AREA_OVER_SEA));
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAreNotificationsEnabled = isNotificationEnabled(this);
        showNotificationConf();
    }

    public void setTvArea(TextView tvArea, int areaType) {
        switch (areaType) {
            case PreferencesWrapper.APP_AREA_CHINA:
                tvArea.setText(R.string.internal_version);
                break;
            case PreferencesWrapper.APP_AREA_OVER_SEA:
                tvArea.setText(R.string.other_area);
                break;
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.rl_change_password:
                startActivity(new Intent(this, ChangePasswordActivity.class));
                break;
            case R.id.rl_clean_cache:
                processCleanCaches();
                break;
            case R.id.rl_check_update:
                checkUpdate();
                break;
            case R.id.rl_about_us:
                startActivity(new Intent(this, AboutUsActivity.class));
                break;
            case R.id.bt_login_out:
                ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                        .setMessage(getString(R.string.login_out_message))
                        .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                            PreferencesUtils.delete(AppSettingActivity.this);
                            try {
                                mTlcService.logout();
                            } catch (Exception e) {
                                L.e(TAG, "mTlcService.logout() error", e);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null);
                dialogFragment.show(getSupportFragmentManager(), "LoginOutDialog");
                break;
            case R.id.version_switch:
                showAreaDialog(this);
                break;
        }
    }


    private boolean areNotificationsEnabled(int notificationOption) {
        return mAreNotificationsEnabled && (notificationConf & notificationOption) != 0;
    }

    private void toggleNotificationConf(int notificationOption) {
        if ((notificationConf & notificationOption) != 0) {
            notificationConf &= ~notificationOption;
        } else {
            notificationConf |= notificationOption;
        }
        showNotificationConf();
        getPreferencesWrapper().setNotificationConf(notificationConf);
    }


    private void showNotificationConf() {
        iv_ring.setOnCheckedChangeListener(null);
        iv_ring.setChecked(areNotificationsEnabled(Notification.DEFAULT_SOUND));
        iv_ring.setOnCheckedChangeListener(soundOnCheckedChangeListener);
        iv_ring.setEnabled(true);

        iv_shake.setOnCheckedChangeListener(null);
        iv_shake.setChecked(areNotificationsEnabled(Notification.DEFAULT_VIBRATE));
        iv_shake.setOnCheckedChangeListener(vibrateOnCheckedChangeListener);
        iv_shake.setEnabled(true);
    }

    private CompoundButton.OnCheckedChangeListener soundOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            iv_ring.setOnCheckedChangeListener(null);
            iv_ring.setEnabled(false);
            iv_ring.setChecked(!isChecked);
            if (!areNotificationsEnabled(Notification.DEFAULT_SOUND) && !mAreNotificationsEnabled) {
                showEnableNotificationDialog();
                iv_ring.setOnCheckedChangeListener(this);
                iv_ring.setEnabled(true);
            } else {
                toggleNotificationConf(Notification.DEFAULT_SOUND);
            }
        }
    };
    private CompoundButton.OnCheckedChangeListener vibrateOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            iv_shake.setOnCheckedChangeListener(null);
            iv_shake.setEnabled(false);
            iv_shake.setChecked(!isChecked);
            if (!areNotificationsEnabled(Notification.DEFAULT_SOUND) && !mAreNotificationsEnabled) {
                showEnableNotificationDialog();
                iv_shake.setOnCheckedChangeListener(this);
                iv_shake.setEnabled(true);
            } else {
                toggleNotificationConf(Notification.DEFAULT_VIBRATE);
            }
        }
    };

    private void showEnableNotificationDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.enable_app_notification)
                .setMessage(R.string.please_enable_app_notification)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", AppSettingActivity.this.getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, ACTIVITY_REQUEST_CODE_ENABLE_NOTIFICATIONS);
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkUpdate();
                } else {
                    toast(R.string.no_write_external_storage_permission);
                }
                break;
            default:
                break;
        }
    }

    private static class CalcCacheSizeTask extends AsyncTask<String, String, Long> {

        private WeakReference<AppSettingActivity> mA;

        CalcCacheSizeTask(AppSettingActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected void onPreExecute() {
            AppSettingActivity a = mA.get();
            if (a == null)
                return;
            a.tv_cache_size.setText(R.string.calculating);
        }

        @Override
        protected Long doInBackground(String... strings) {
            long total = 0;

            do {
                if (mA.get() == null)
                    return null;
            } while (false);

            try {
                total += CacheUtil.getFolderSize(FileUtils.getExternalStorageLogDirectoryFile());
            } catch (Exception e) {
                L.e(TAG, "SD_LOG_DIR", e);
            }

            do {
                if (mA.get() == null)
                    return null;
            } while (false);

            try {
                total += CacheUtil.getFolderSize(FileUtils.getExternalStorageImageCacheDirFile());
            } catch (Exception e) {
                L.e(TAG, "SD_IMAGE_CACHE_DIR", e);
            }

            do {
                AppSettingActivity a = mA.get();
                if (a == null)
                    return null;
            } while (false);

            // TODO: 聊天文件


            total += calcSysCache(mA.get());

            return total;
        }

        public static long calcSysCache(Context context) {
            if (context == null)
                return 0;
            try {
                return CacheUtil.getFolderSize(context.getCacheDir());
            } catch (Exception e) {
                L.i(TAG, "calcSysCache", e);
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Long size) {
            AppSettingActivity a = mA.get();
            if (a == null)
                return;
            if (size == null)
                size = 0L;
            a.tv_cache_size.setText(Utils.bytesText(size));
            synchronized (a.tv_cache_size) {
                a.mCalcCacheSizeTask = null;
                if (a.mCleanCacheTask != null) {
                    a.mCleanCacheTask.execute();
                }
            }
        }
    }

    private void processCleanCaches() {
        popWaitingDialog(R.string.please_wait);
        synchronized (tv_cache_size) {
            mCleanCacheTask = new CleanCacheTask(this);
            if (mCalcCacheSizeTask == null) {
                // 计算已经结束，直接启动清理任务
                mCleanCacheTask.execute();
            } else {
                // 还在计算中，等待计算结束
                // 计算结束后，会自动运行 mCleanCacheTask
            }
        }
    }

    public static boolean isNotificationEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            return manager.areNotificationsEnabled();
        } else {
            AppOpsManager mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            ApplicationInfo appInfo = context.getApplicationInfo();
            String pkg = context.getApplicationContext().getPackageName();
            int uid = appInfo.uid;

            try {
                Method checkOpNoThrowMethod = AppOpsManager.class.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String.class);
                Field opPostNotificationValue = AppOpsManager.class.getDeclaredField(OP_POST_NOTIFICATION);

                int value = (Integer) opPostNotificationValue.get(Integer.class);
                return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) == AppOpsManager.MODE_ALLOWED);
            } catch (Exception e) {
                L.w(TAG, "isNotificationEnabled", e);
            }
            return false;
        }
    }


    private static class CleanCacheTask extends AsyncTask<String, String, String> {
        private WeakReference<AppSettingActivity> mA;

        CleanCacheTask(AppSettingActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected String doInBackground(String... strings) {
            do {
                if (mA.get() == null)
                    return null;
            } while (false);

            FileUtils.deleteDirectory(FileUtils.getExternalStorageLogDirectoryFile());

            do {
                if (mA.get() == null)
                    return null;
            } while (false);

            File[] fileList = FileUtils.getExternalStorageImageCacheDirFile().listFiles();
            for (File f : fileList) {
                deleteDir(f);
            }

            // TODO: 删除聊天文件


            // 删除 android APP 缓存
            deleteCache(mA.get());

            return null;
        }

        public static void deleteCache(Context context) {
            if (context == null)
                return;
            try {
                File dir = context.getCacheDir();
                deleteDir(dir);
            } catch (Exception e) {
                L.i(TAG, "deleteCache", e);
            }
        }

        public static boolean deleteDir(File dir) {
            if (dir == null) {
                return false;
            }
            if (dir.isFile()) {
                return dir.delete();
            }
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (String aChildren : children) {
                    if (!deleteDir(new File(dir, aChildren))) {
                        return false;
                    }
                }
                return dir.delete();
            }
            return false;
        }

        @Override
        protected void onPostExecute(String s) {
            AppSettingActivity a = mA.get();
            if (a == null)
                return;
            a.tv_cache_size.setText(Utils.bytesText(0));
            synchronized (a.tv_cache_size) {
                a.mCleanCacheTask = null;
            }
            a.popInfoDialog(R.string.app_setting_cache_cleaning_completed);
        }
    }

    private void checkUpdate() {
        popWaitingDialog(R.string.check_upgrade);
        String url = OkHttpRequestManager.APP_CHECK_VERSION;
        L.e(TAG, "Check App Version URL: " + url);
        OkHttpRequestManager.getInstance(this).updateAPK(url, new OkHttpRequestManager.ReqCallBack<UpdateInfo>() {
            @Override
            public void onReqSuccess(final UpdateInfo result) {
                runOnUiThread(() -> {
                    try {
                        PackageManager packageManager = getPackageManager();
                        PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
                        if (result.getVersionCode() > packageInfo.versionCode) {
                            dismissDialog();
                            mTaskHandler.obtainMessage(TaskHandler.SHOW_UPDATE_TIP, result).sendToTarget();
                        } else {
                            popInfoDialog(R.string.is_updated);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        L.e(TAG, "check ver failure", e);
                        popErrorDialog(R.string.check_upgrade_failure);
                    }
                });
            }

            @Override
            public void onReqFailed(final String errorMsg) {
                L.e(TAG, "check ver failure: " + errorMsg);
                runOnUiThread(() -> {
                    if (errorMsg.contains("No address associated with hostname")) {
                        popErrorDialog(R.string.network_quality_poor);
                        return;
                    }
                    try {
                        JSONObject err = new JSONObject(errorMsg);
                        err.getBoolean("success");
                        // 无任何新版本信息时，服务器返回 {"sid":null,"success":false,"code":"","description":"未找到相关安装包！","data":null}
                        // 我们认为当前app是最新版本
                        popInfoDialog(R.string.is_updated);
                        return;
                    } catch (JSONException e) {
                        L.e(TAG, "checkUpdate: onReqFailed: ", e);
                    }
                    popErrorDialog(R.string.check_upgrade_failure);
                });
            }
        });
    }

    private Upgrade mUpgrade;

    public void showUpdateDialog(final UpdateInfo updateInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle(R.string.app_upgrade);
        builder.setMessage(getString(R.string.app_upgrade_to_, updateInfo.getVersion()));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            if (mUpgrade == null) {
                mUpgrade = new Upgrade(AppSettingActivity.this, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
            mUpgrade.upgradeGuide(" v" + updateInfo.version);
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    public static class UpdateInfo implements Serializable {
        private String version;
        private String description;
        private int versionCode;
        private long length;

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }


        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }
    }

    private TaskHandler mTaskHandler = new TaskHandler(this);

    private static class TaskHandler extends Handler {
        static final int SHOW_UPDATE_TIP = 0;

        private WeakReference<AppSettingActivity> mA;

        TaskHandler(AppSettingActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(Message msg) {
            AppSettingActivity a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {
                case SHOW_UPDATE_TIP:
                    a.showUpdateDialog((UpdateInfo) msg.obj);
                    break;
            }
        }
    }
}
