package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.widget.NumberPickerView;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/9/13.
 */
public class TimingShutdownWatchActivity extends BaseActivity {
    public static final String TAG = TimingShutdownWatchActivity.class.getSimpleName();
    private String deviceId;
    private boolean hasEditPermission;
    private boolean dataInited;
    private boolean isOpen;
    private SwitchCompat mSwitch;
    private int mBootHour = 5, mBootMinute = 57, mShutHour = 23, mShutMinute = 5;
    private NumberPickerView mBootHourPicker, mBootMinutePicker, mShutHourPicker, mShutMinutePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timing_shutdown);
        setTitleBarTitle(R.string.timing_shut_down);
        if (mDeviceId == null) {
            L.w(TAG, "mDeviceId is null");
            finish();
            return;
        }
        hasEditPermission = hasEditPermission();
        deviceId = mDeviceId;

        initView();
        loadData();
    }

    private void initView() {
        View bootTimePicker = findViewById(R.id.boot_time_picker);
        mBootHourPicker = bootTimePicker.findViewById(R.id.hour_picker);
        mBootHourPicker.setSelectedTextColor(getResources().getColor(R.color.blue_tone));
        mBootHourPicker.setMinValue(0);
        mBootHourPicker.setMaxValue(23);
        mBootHourPicker.setValue(mBootHour);
        mBootHourPicker.setEnabled(hasEditPermission);
        mBootHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (mBootHour != newVal) {
                if (!pushDevConfigs(isOpen, newVal, mBootMinute, mShutHour, mShutMinute, Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE)) {
                    updateTimePicker(true);
                }
            }
        });
        mBootMinutePicker = bootTimePicker.findViewById(R.id.minute_picker);
        mBootMinutePicker.setSelectedTextColor(getResources().getColor(R.color.blue_tone));
        mBootMinutePicker.setMinValue(0);
        mBootMinutePicker.setMaxValue(59);
        mBootMinutePicker.setValue(mBootMinute);
        mBootMinutePicker.setEnabled(hasEditPermission);
        mBootMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (mBootMinute != newVal) {
                if (!pushDevConfigs(isOpen, mBootHour, newVal, mShutHour, mShutMinute, Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE)) {
                    updateTimePicker(true);
                }
            }
        });

        View shutdownTimePicker = findViewById(R.id.shutdown_time_picker);
        mShutHourPicker = shutdownTimePicker.findViewById(R.id.hour_picker);
        mShutHourPicker.setMinValue(0);
        mShutHourPicker.setMaxValue(23);
        mShutHourPicker.setValue(mShutHour);
        mShutHourPicker.setEnabled(hasEditPermission);
        mShutHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (mShutHour != newVal) {
                if (!pushDevConfigs(isOpen, mBootHour, mBootMinute, newVal, mShutMinute, Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE)) {
                    updateTimePicker(true);
                }
            }
        });
        mShutMinutePicker = shutdownTimePicker.findViewById(R.id.minute_picker);
        mShutMinutePicker.setMinValue(0);
        mShutMinutePicker.setMaxValue(59);
        mShutMinutePicker.setValue(mShutMinute);
        mShutMinutePicker.setEnabled(hasEditPermission);
        mShutMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (mShutMinute != newVal) {
                if (!pushDevConfigs(isOpen, mBootHour, mBootMinute, mShutHour, newVal, Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE)) {
                    updateTimePicker(true);
                }
            }
        });

        mSwitch = findViewById(R.id.ib_switch);
        mSwitch.setEnabled(hasEditPermission);
        updateSwitch();
    }

    private void updateTimePicker(boolean smooth) {
        if (smooth) {
            mBootHourPicker.smoothScrollToValue(mBootHour, false);
            mBootMinutePicker.smoothScrollToValue(mBootMinute, false);
            mShutHourPicker.smoothScrollToValue(mShutHour, false);
            mShutMinutePicker.smoothScrollToValue(mShutMinute, false);
        } else {
            mBootHourPicker.setValue(mBootHour);
            mBootMinutePicker.setValue(mBootMinute);
            mShutHourPicker.setValue(mShutHour);
            mShutMinutePicker.setValue(mShutMinute);
        }
    }

    private void updateSwitch() {
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch.setChecked(isOpen);
        mSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mSwitch.setEnabled(hasEditPermission);
    }

    private void loadData() {
        getDataQueryDB();
        getDataQueryServer();
    }

    private boolean pushDevConfigs(boolean open, int bootHour, int bootMinute, int shutHour, int shutMinute, int flag) {
        if (TextUtils.isEmpty(deviceId)) {
            Log.e(TAG, "pushDevConfigs error ! deviceId is empty");
            return false;
        }

        Message.Functions.Builder functions = Message.Functions.newBuilder()
                .setChangedField(flag);

        if ((flag & (Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE | Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE)) != 0) {
            Calendar boot = Calendar.getInstance();
            boot.set(Calendar.HOUR_OF_DAY, bootHour);
            boot.set(Calendar.MINUTE, bootMinute);
            boot.set(Calendar.SECOND, 0);
            boot.set(Calendar.MILLISECOND, 0);
            Calendar shut = (Calendar) boot.clone();
            shut.set(Calendar.HOUR_OF_DAY, shutHour);
            shut.set(Calendar.MINUTE, shutMinute);
            if (Math.abs(shut.getTimeInMillis() - boot.getTimeInMillis()) < 1000L * 60 * 5) {
                toast(R.string.timing_time_5_min);
                return false;
            }

            if ((flag & Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE) != 0) {
                functions.setTimerPowerOn(Message.TimePoint.newBuilder().setTime(boot.getTimeInMillis() / 1000L));
            }
            if ((flag & Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE) != 0) {
                functions.setTimerPowerOff(Message.TimePoint.newBuilder().setTime(shut.getTimeInMillis() / 1000L));
            }
        }

        if ((flag & Message.Functions.FieldFlag.TIMER_POWER_ON_OFF_VALUE) != 0) {
            functions.setTimerPowerOnOff(open);
        }

        Message.PushDevConfReqMsg reqMsg = Message.PushDevConfReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .setFlag(Message.DevConfFlag.DCF_FUNCTIONS_VALUE)
                .setConf(Message.DevConf.newBuilder().setFuncs(functions))
                .build();

        popWaitingDialog(R.string.saving_settings);

        L.v(TAG, "pushDevConfigs PushDevConfReqMsg: " + reqMsg);

        exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.PushDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.i(TAG, "saving timing shutdown success");
                                    GreenUtils.saveConfigs(reqMsg.getConf(), flag, deviceId);

                                    dismissDialog();

                                    if (flag == Message.Functions.FieldFlag.TIMER_POWER_ON_OFF_VALUE) {
                                        isOpen = open;
                                        updateSwitch();
                                    }
                                    if ((flag & (Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE | Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE)) != 0) {
                                        if ((flag & Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE) != 0) {
                                            mBootHour = bootHour;
                                            mBootMinute = bootMinute;
                                        }
                                        if ((flag & Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE) != 0) {
                                            mShutHour = shutHour;
                                            mShutMinute = shutMinute;
                                        }
                                        updateTimePicker(true);
                                    }
                                    return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "pushDevConfigs() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "pushDevConfigs() -> exec() -> onResponse() process failure", e);
                            return false;
                        }
                        popErrorDialog(getString(R.string.setup_failed));
                        updateSwitch();
                        updateTimePicker(true);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "pushDevConfigs() -> exec() -> onException()", cause);
                        popErrorDialog(getString(R.string.setup_failed));
                        updateSwitch();
                        updateTimePicker(true);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
        return true;
    }

    private void getDataQueryDB() {
        List<DeviceEntity> list = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (list.isEmpty()) {
            updateSwitch();
            updateTimePicker(false);
        } else {
            DeviceEntity deviceEntity = list.get(0);
            Message.Functions functions = deviceEntity.getFunctions();
            initViewData(functions, false, false);
        }
    }

    private void getDataQueryServer() {
        if (TextUtils.isEmpty(deviceId)) {
            Log.e(TAG, "getDataQueryServer error ! devId is NuLL");
            return;
        }
        popWaitingDialog(R.string.loading);
        exec(
                Message.FetchDevConfReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .setFlag(Message.DevConfFlag.DCF_FUNCTIONS_VALUE)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.FetchDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "TimingShutdown getDataServer: " + rspMsg);
                            if (Message.ErrorCode.SUCCESS == rspMsg.getErrCode()) {
                                if (rspMsg.getFlag() == Message.DevConfFlag.DCF_FUNCTIONS_VALUE) {
                                    Message.DevConf configs = rspMsg.getConf();
                                    GreenUtils.saveConfigs(configs, rspMsg.getFlag(), deviceId);
                                    dismissDialog();
                                    if (configs.getFuncs() != null)
                                        initViewData(configs.getFuncs(), true, true);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        popErrorDialog(cause instanceof TimeoutException ? R.string.load_timeout : R.string.load_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void initViewData(Message.Functions funcs, boolean checkFieldFlag, boolean smooth) {
        Calendar cal = Calendar.getInstance();

        long bootTime = funcs.getTimerPowerOn().getTime();
        if ((!checkFieldFlag || (funcs.getChangedField() & Message.Functions.FieldFlag.TIMER_POWER_ON_VALUE) != 0) && bootTime != 0) {
            cal.setTimeInMillis(bootTime * 1000L);
            mBootHour = cal.get(Calendar.HOUR_OF_DAY);
            mBootMinute = cal.get(Calendar.MINUTE);
        }
        long shutTime = funcs.getTimerPowerOff().getTime();
        if ((!checkFieldFlag || (funcs.getChangedField() & Message.Functions.FieldFlag.TIMER_POWER_OFF_VALUE) != 0) && shutTime != 0) {
            cal.setTimeInMillis(shutTime * 1000L);
            mShutHour = cal.get(Calendar.HOUR_OF_DAY);
            mShutMinute = cal.get(Calendar.MINUTE);
        }

        if (!checkFieldFlag || (funcs.getChangedField() & Message.Functions.FieldFlag.TIMER_POWER_ON_OFF_VALUE) != 0) {
            isOpen = funcs.getTimerPowerOnOff();
        }

        updateTimePicker(smooth);
        updateSwitch();
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked != isOpen) {
                mSwitch.setOnCheckedChangeListener(null);
                mSwitch.setChecked(!isChecked);
                mSwitch.setEnabled(false);
                if (!pushDevConfigs(isChecked, mBootHour, mBootMinute, mShutHour, mShutMinute, Message.Functions.FieldFlag.TIMER_POWER_ON_OFF_VALUE)) {
                    mSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
                    mSwitch.setEnabled(hasEditPermission);
                }
            }
        }
    };

    @Override
    public void onDevConfChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfChangedReqMsg reqMsg) {
        if (!reqMsg.getDeviceId().equals(mDeviceId)) {
            return;
        }
        if ((reqMsg.getFlag() & Message.DevConfFlag.DCF_FUNCTIONS_VALUE) == 0) {
            return;
        }
        Message.Functions funcs = reqMsg.getConf().getFuncs();
        initViewData(funcs, true, true);
    }
}
