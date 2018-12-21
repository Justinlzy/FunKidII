package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.MoreActivityAdapter;
import com.cqkct.FunKidII.Ui.widget.TextMarkSeekBar;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/9/13.
 */

public class WatchSettingActivity extends BaseActivity {
    private static String TAG = WatchSettingActivity.class.getSimpleName();
    public static final int LOCATION_MODE_TEXT_TAG = 1;
    public static final String LOCATION_MODE_TEXT = "LOCATION_MODE_TEXT";

    private String userId, deviceId;

    private TextView tvBrightTime;
    private static final int DEF_TIME = 10;
    private int lightTime = DEF_TIME;
    private boolean hasEditPermission = false;
    private SwitchCompat mSwitch;
    private boolean calculatorStatus = false;
    private TextView tvLocationMode;
    private TextMarkSeekBar seekBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watch_setting);
        setTitleBarTitle(R.string.watch_setting);

        if (savedInstanceState != null) {
            userId = savedInstanceState.getString("userId");
            deviceId = savedInstanceState.getString("deviceId");
        }
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = mDeviceId;
        }
        hasEditPermission = hasEditPermission();
        initView();
        getData();
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.rl_location_mode:
                Intent intent = new Intent(this, LocationModeActivity.class);
                startActivityForResult(intent, LOCATION_MODE_TEXT_TAG);
                break;
        }
    }

    private void getData() {
        getDataQueryDB();
        getDataQueryServer();
        getLocationModeFromService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("userId", userId);
        outState.putString("deviceId", deviceId);
        super.onSaveInstanceState(outState);
    }

    private void initView() {
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        long func_module = 0;
        if (deviceInfo != null) {
            func_module = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule();
        }
        boolean showBrightScreenTime = (func_module & Message.FuncModule.FM_BRIGHT_SCREEN_TIME_VALUE) != 0;
        boolean showCalculator = (func_module & Message.FuncModule.FM_CALCULATOR_VALUE) != 0;

        LinearLayout ll_BrightScreenTime = findViewById(R.id.bright_screen_time);
        ll_BrightScreenTime.setVisibility(showBrightScreenTime ? View.VISIBLE : View.GONE);
        tvBrightTime = findViewById(R.id.watch_set_time_tv);
        tvBrightTime.setText(String.valueOf("(" + String.valueOf(lightTime) + getString(R.string.second) + ")"));

        seekBar = findViewById(R.id.seek_bar);
        seekBar.setVisibility(showBrightScreenTime ? View.VISIBLE : View.GONE);
        seekBar.setEnabled(hasEditPermission);
        seekBar.setProgress(String.valueOf(lightTime));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                String timeStr = seekBar.getProgressMarkText();
                try {
                    if (!timeStr.equals(String.valueOf(lightTime))) {
                        pushDevConfigs(Integer.valueOf(timeStr), calculatorStatus, Message.Functions.FieldFlag.WATCH_SET_LIGHT_VALUE);
                    }
                } catch (Exception e) {
                    L.w(TAG, "setOnSeekBarChangeListener onStopTrackingTouch getProgressMarkText valueOf", e);
                }
            }
        });
        findViewById(R.id.rl_cal).setVisibility(showCalculator ? View.VISIBLE : View.GONE);

        mSwitch = findViewById(R.id.switch_);
        mSwitch.setClickable(hasEditPermission);
        if (hasEditPermission) {
            mSwitch.setOnCheckedChangeListener(calculatorSwitchListener);
        }
        tvLocationMode = findViewById(R.id.location_mode);
    }

    CompoundButton.OnCheckedChangeListener calculatorSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (b != calculatorStatus) {
                mSwitch.setOnCheckedChangeListener(null);
                mSwitch.setChecked(!b);
                mSwitch.setEnabled(false);
                if (!pushDevConfigs(lightTime, !calculatorStatus, Message.Functions.FieldFlag.ENABLE_CALCULATOR_VALUE)) {
                    mSwitch.setOnCheckedChangeListener(calculatorSwitchListener);
                    mSwitch.setEnabled(true);
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LOCATION_MODE_TEXT_TAG:
                if (resultCode == RESULT_OK) {
                    Message.LocationMode locationMode = (Message.LocationMode) data.getSerializableExtra(LOCATION_MODE_TEXT);
                    initLocation(locationMode);
                }
                break;
            default:
                break;
        }
    }

    private void initLocation(Message.LocationMode locationMode) {
        switch (locationMode) {
            case LM_PASSIVE:
                tvLocationMode.setText(R.string.passive_mode);
                break;
            case LM_POWER_SAVING:
                tvLocationMode.setText(R.string.notify_message_save_power_mode);
                break;
            case LM_NORMAL:
                tvLocationMode.setText(R.string.normal_mode);
                break;
            case LM_USELESS:
                tvLocationMode.setText(R.string.none);
                break;
            default:
                break;
        }
    }

    //查询手表定位模式
    private void getLocationModeFromService() {
        String devId = mDeviceId;
        if (TextUtils.isEmpty(devId)) {
            L.e(TAG, " ERROR: decId is null");
            return;
        }
        popWaitingDialog(R.string.loading);
        exec(Message.GetLocationModeReqMsg.newBuilder().setDeviceId(devId).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        Message.GetLocationModeRspMsg rspMsg = response.getProtoBufMsg();

                        if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                            L.e(TAG, "ModifyLocationModeReqMsg: " + rspMsg);
                            dismissDialog();
                            Message.LocationMode locationMode = rspMsg.getLocationMode();
                            initLocation(locationMode);
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

    private boolean pushDevConfigs(final int lightTimes, final boolean cal,
                                   final long changeFlag) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "pushDevConfigs: deviceId is empty");
            return false;
        }

        if (mTlcService == null) {
            L.w(TAG, "pushDevConfigs: mTlcService == null");
            return false;
        }

        popWaitingDialog(R.string.please_wait);

        final Message.PushDevConfReqMsg reqMsg = Message.PushDevConfReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .setConf(Message.DevConf.newBuilder().setFuncs(
                        Message.Functions.newBuilder()
                                .setChangedField(changeFlag)
                                .setWatchSetLight(lightTimes)
                                .setEnableCalculator(cal)
                                .build()))
                .setFlag(Message.DevConfFlag.DCF_FUNCTIONS_VALUE)
                .build();

        exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.PushDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "setFunctionsS1() -> exec() -> onResponse() " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    GreenUtils.saveConfigsAsync(reqMsg.getConf(), reqMsg.getFlag(), reqMsg.getDeviceId());
                                    WatchSettingActivity.this.lightTime = lightTimes;
                                    WatchSettingActivity.this.calculatorStatus = cal;
                                    tvBrightTime.setText(String.valueOf("(" + String.valueOf(lightTime) + getString(R.string.second) + ")"));
                                    seekBar.setProgress(String.valueOf(lightTime));
                                    mSwitch.setChecked(cal);
                                    mSwitch.setOnCheckedChangeListener(calculatorSwitchListener);
                                    mSwitch.setEnabled(true);
                                    calculatorStatus = cal;
                                    dismissDialog();
                                    return false;
                                default:
                                    break;
                            }
                            L.w(TAG, "setFunctionsS1() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "setFunctionsS1() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.setup_failed);
                        mSwitch.setOnCheckedChangeListener(calculatorSwitchListener);
                        mSwitch.setEnabled(true);
                        seekBar.setProgress(String.valueOf(lightTime));
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        mSwitch.setOnCheckedChangeListener(calculatorSwitchListener);
                        mSwitch.setEnabled(true);
                        seekBar.setProgress(String.valueOf(lightTime));
                        popErrorDialog(cause instanceof TimeoutException ? R.string.setup_timeout : R.string.setup_failed);
                        L.e(TAG, "setFunctionsS1() -> exec() -> onException() calculatorStatus :" + calculatorStatus, cause);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );

        return true;
    }

    private void getDataQueryServer() {
        popWaitingDialog(R.string.loading);

        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "getDataQueryServer: deviceId is empty");
            return;
        }

        exec(
                Message.FetchDevConfReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .setFlag(Message.DevConfFlag.DCF_FUNCTIONS_VALUE | Message.DevConfFlag.DCF_FUNC_MODULE_INFO_VALUE)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.FetchDevConfRspMsg fetchConfigRspMsg = response.getProtoBufMsg();
                            L.v(TAG, "getDataQueryServer onResponse: " + fetchConfigRspMsg);
                            if (Message.ErrorCode.SUCCESS == fetchConfigRspMsg.getErrCode()) {
                                GreenUtils.saveConfigsAsync(fetchConfigRspMsg.getConf(), fetchConfigRspMsg.getFlag(), deviceId);
                                if ((fetchConfigRspMsg.getFlag() & Message.DevConfFlag.DCF_FUNCTIONS_VALUE) != 0) {
                                    Message.DevConf configs = fetchConfigRspMsg.getConf();
                                    initViewData(configs.getFuncs());
                                }
                                if ((fetchConfigRspMsg.getFlag() & Message.DevConfFlag.DCF_FUNC_MODULE_INFO_VALUE) != 0) {
                                    boolean supported = (fetchConfigRspMsg.getConf().getFuncModuleInfo().getFuncModule() & Message.FuncModule.FM_LOCATION_MODE_VALUE) != 0;
                                    findViewById(R.id.rl_location_mode).setVisibility(supported ? View.VISIBLE : View.GONE);
                                    findViewById(R.id.line).setVisibility(supported ? View.VISIBLE : View.GONE);
                                }
                                dismissDialog();
                                return false;
                            } else {
                                L.e(TAG, "getDataQueryServer onResponse: " + fetchConfigRspMsg.getErrCode());
                            }
                        } catch (Exception e) {
                            L.e(TAG, "getDataQueryServer onResponse", e);
                        }
                        popErrorDialog(R.string.load_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "getDataQueryServer onException", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.load_timeout : R.string.load_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void getDataQueryDB() {
        List<DeviceEntity> list = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).build().list();
        if (!list.isEmpty()) {
            DeviceEntity deviceEntity = list.get(0);
            Message.Functions functions = deviceEntity.getFunctions();
            initViewData(functions, deviceEntity.getLocationMode());

            boolean supportedLocationMode = (deviceEntity.getFuncModuleInfo().getFuncModule() & Message.FuncModule.FM_LOCATION_MODE_VALUE) != 0;
            findViewById(R.id.rl_location_mode).setVisibility(supportedLocationMode ? View.VISIBLE : View.GONE);
            findViewById(R.id.line).setVisibility(supportedLocationMode ? View.VISIBLE : View.GONE);
        }
    }

    private void initViewData(Message.Functions functions, int locationMode) {
        initViewData(functions);
        switch (locationMode) {
            case Message.LocationMode.LM_PASSIVE_VALUE:
                tvLocationMode.setText(R.string.passive_mode);
                break;
            case Message.LocationMode.LM_POWER_SAVING_VALUE:
                tvLocationMode.setText(R.string.save_power);
                break;
            case Message.LocationMode.LM_NORMAL_VALUE:
                tvLocationMode.setText(R.string.normal_mode);
                break;
            case Message.LocationMode.LM_USELESS_VALUE:
                tvLocationMode.setText(R.string.none);
                break;
            default:
                break;

        }
    }

    private void initViewData(Message.Functions functions) {
        int time = functions.getWatchSetLight();
        if (time != 0)
            lightTime = time;
        calculatorStatus = functions.getEnableCalculator();

        mSwitch.setOnCheckedChangeListener(null);
        mSwitch.setChecked(calculatorStatus);
        if (hasEditPermission) {
            mSwitch.setOnCheckedChangeListener(calculatorSwitchListener);
        }
        tvBrightTime.setText(String.valueOf("(" + String.valueOf(lightTime) + getString(R.string.second) + ")"));
        seekBar.setProgress(String.valueOf(lightTime));
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceId().equals(mDeviceId)) {
            int time = deviceInfo.getDeviceEntity().getFunctions().getWatchSetLight();
            if (time != 0 && time != lightTime) {
                lightTime = time;
                tvBrightTime.setText(String.valueOf("(" + String.valueOf(lightTime) + getString(R.string.second) + ")"));
                seekBar.setProgress(String.valueOf(lightTime));
            }
            if (deviceInfo.getDeviceEntity().getFunctions().getEnableCalculator() != calculatorStatus) {
                calculatorStatus = deviceInfo.getDeviceEntity().getFunctions().getEnableCalculator();
                mSwitch.setOnCheckedChangeListener(null);
                mSwitch.setChecked(calculatorStatus);
                if (hasEditPermission) {
                    mSwitch.setOnCheckedChangeListener(calculatorSwitchListener);
                }
            }
        }
    }


}

