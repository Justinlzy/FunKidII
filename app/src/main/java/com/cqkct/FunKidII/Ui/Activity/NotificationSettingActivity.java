package com.cqkct.FunKidII.Ui.Activity;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.L;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by Administrator on 2018/1/25.
 */

public class NotificationSettingActivity extends BaseActivity {
    private static final String TAG = NotificationSettingActivity.class.getSimpleName();

    private SwitchCompat iv_ring;
    private SwitchCompat iv_shake;
    private int notificationConf;
    private boolean mAreNotificationsEnabled;
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";

    private static final int ACTIVITY_REQUEST_CODE_ENABLE_NOTIFICATIONS = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_setting);
        setTitleBarTitle(R.string.notification);

        iv_ring = findViewById(R.id.switch_ring);
        iv_ring.setOnCheckedChangeListener(soundOnCheckedChangeListener);
        iv_shake = findViewById(R.id.switch_shake);
        iv_shake.setOnCheckedChangeListener(vibrateOnCheckedChangeListener);

        notificationConf = getPreferencesWrapper().getNotificationConf();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAreNotificationsEnabled = isNotificationEnabled(this);
        showNotificationConf();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_ENABLE_NOTIFICATIONS) {
            // do noting
        }
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

    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.rl_notification_ring:
                if (!areNotificationsEnabled(Notification.DEFAULT_SOUND) && !mAreNotificationsEnabled) {
                    showEnableNotificationDialog();
                } else {
                    toggleNotificationConf(Notification.DEFAULT_SOUND);
                }
                break;
            case R.id.rl_notification_shake:
                if (!areNotificationsEnabled(Notification.DEFAULT_VIBRATE) && !mAreNotificationsEnabled) {
                    showEnableNotificationDialog();
                } else {
                    toggleNotificationConf(Notification.DEFAULT_VIBRATE);
                }
                break;
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

    private void showEnableNotificationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.enable_app_notification)
                .setMessage(R.string.please_enable_app_notification)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", NotificationSettingActivity.this.getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, ACTIVITY_REQUEST_CODE_ENABLE_NOTIFICATIONS);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
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
}
