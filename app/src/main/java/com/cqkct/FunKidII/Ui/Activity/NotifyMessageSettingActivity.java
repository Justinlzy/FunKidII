package com.cqkct.FunKidII.Ui.Activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.CompoundButton;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class NotifyMessageSettingActivity extends BaseActivity {

    private static final String TAG = NotifyMessageSettingActivity.class.getSimpleName();

    public static final String DEVICE_ID_OF_SHOW = "DEVICE_ID_OF_SHOW";

    private static final Map<Integer, Integer> mChannelToViewIdMap = new HashMap<Integer, Integer>(){
        {
            put(Message.NotificationChannel.NC_DEVICE_INCIDENT_VALUE, R.id.device_activity_switch);
            put(Message.NotificationChannel.NC_SETTINGS_VALUE, R.id.change_settings_switch);
            put(Message.NotificationChannel.NC_CONTACTS_VALUE, R.id.contacts_change_switch);
            put(Message.NotificationChannel.NC_PRAISE_COLLECTION_VALUE, R.id.collect_praise_information_switch);
            put(Message.NotificationChannel.NC_DEVICE_CALL_VALUE, R.id.watch_call_switch);
            put(Message.NotificationChannel.NC_DEVICE_SOS_VALUE, R.id.sos_change_switch);
        }
    };
    private static final Map<Integer, Integer> mViewIdToChannelMap = new HashMap<Integer, Integer>() {
        {
            for (Map.Entry<Integer, Integer> entry : mChannelToViewIdMap.entrySet()) {
                put(entry.getValue(), entry.getKey());
            }
        }
    };
    private static final SparseArray<SwitchCompat> mChannelToViewMap = new SparseArray<>();

    private String mDeviceIdOfShow;
    private int mNotificationChannel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_message_setting);

        mDeviceIdOfShow = getIntent().getStringExtra(DEVICE_ID_OF_SHOW);
        if (TextUtils.isEmpty(mDeviceIdOfShow)) {
            L.e(TAG, "DEVICE_ID_OF_SHOW is empty! finish");
            finish();
            return;
        }

        initView();
        initData();
    }

    private void initData() {
        getWatchSettingStatusFromDb();
        getNotificationChannelFromService();
    }

    private void initView() {
        setTitleBarTitle(R.string.message_settings);

        for (Map.Entry<Integer, Integer> entry : mChannelToViewIdMap.entrySet()) {
            SwitchCompat view = findViewById(entry.getValue());
            view.setOnCheckedChangeListener(mOnCheckedChangeListener);
            mChannelToViewMap.put(entry.getKey(), view);
        }
    }

    CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Integer channelKey = mViewIdToChannelMap.get(buttonView.getId());
            if (channelKey == null) {
                return;
            }
            if (isNotificationChannelEnabled(channelKey) != isChecked) {
                buttonView.setOnCheckedChangeListener(null);
                buttonView.setChecked(!isChecked);
                buttonView.setEnabled(false);
                if (!modifyNotificationChannel(channelKey, isChecked)) {
                    buttonView.setOnCheckedChangeListener(mOnCheckedChangeListener);
                    buttonView.setEnabled(true);
                }
            }
        }
    };

    private boolean modifyNotificationChannel(int channelKey, boolean enable) {
        String deviceId = mDeviceIdOfShow;
        String userId = mUserId;
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId))
            return false;
        int channel = calcNotificationChannel(mNotificationChannel, channelKey, enable);
        popWaitingDialog(R.string.please_wait);
        exec(
                Message.SetNotificationChannelReqMsg.newBuilder()
                        .setUserId(userId)
                        .setDeviceId(deviceId)
                        .setChan(channel)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.SetNotificationChannelRspMsg rspMsg = response.getProtoBufMsg();
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                GreenUtils.saveNotificationChannel(userId, deviceId, channel);
                                mNotificationChannel = channel;
                                setNotificationChannelSwitch(channelKey);
                                dismissDialog();
                                return false;
                            }
                            L.w(TAG, "modifyNotificationChannel() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "modifyNotificationChannel() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.setting_failure);
                        setNotificationChannelSwitch(channelKey);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "modifyNotificationChannel() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.setup_timeout);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.setting_failure);
                        }
                        setNotificationChannelSwitch(channelKey);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
        return true;
    }

    private void getWatchSettingStatusFromDb() {
        mNotificationChannel = GreenUtils.getNotificationChannel(mUserId, mDeviceIdOfShow);
        initViewData();
    }

    private void getNotificationChannelFromService() {
        String userId = mUserId;
        String deviceId = mDeviceIdOfShow;
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId))
            return;
        popWaitingDialog(R.string.please_wait);
        exec(
                Message.GetNotificationChannelReqMsg.newBuilder()
                        .setUserId(userId)
                        .setDeviceId(deviceId)
                        .build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetNotificationChannelRspMsg rspMsg = response.getProtoBufMsg();
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                dismissDialog();
                                int ch = rspMsg.getChan();
                                GreenUtils.saveNotificationChannel(userId, deviceId, ch);
                                mNotificationChannel = ch;
                                initViewData();
                                return false;
                            }
                            L.w(TAG, "getNotificationChannelFromService() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "getNotificationChannelFromService() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.load_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "getNotificationChannelFromService() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.load_timeout);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.load_failure);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private boolean isNotificationChannelEnabled(int channelKey) {
        return isNotificationChannelEnabled(mNotificationChannel, channelKey);
    }

    public static boolean isNotificationChannelEnabled(int notificationChannel, int channelKey) {
        return (notificationChannel & channelKey) == channelKey;
    }

    private int calcNotificationChannel(int base, int channelKey, boolean enable) {
        if (enable) {
            return base | channelKey;
        } else {
            return base & ~channelKey;
        }
    }

    private void setNotificationChannelSwitch(int channelKey) {
        SwitchCompat view = mChannelToViewMap.get(channelKey);
        if (view == null) {
            return;
        }
        view.setOnCheckedChangeListener(null);
        view.setChecked(isNotificationChannelEnabled(channelKey));
        view.setOnCheckedChangeListener(mOnCheckedChangeListener);
        view.setEnabled(true);
    }

    private void initViewData() {
        for (Integer channelKey : mChannelToViewIdMap.keySet()) {
           setNotificationChannelSwitch(channelKey);
        }
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceIdOfShow)) {
            // 当前正在查看的宝贝被解绑，关闭这个页面
            finish();
        }
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }
}
