package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class LocationModeActivity extends BaseActivity {

    public static final String TAG = LocationModeActivity.class.getSimpleName();


    private String devId;
    private int mMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_mode);
        setTitleBarTitle(R.string.location_mode);

        getLocationMode();
    }


    private void getLocationMode() {
        devId = mDeviceId;
        getLocationModeFromDB();
        getLocationModeFromService();
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        super.onDebouncedClick(view, viewId);
        switch (viewId) {
            case R.id.passive_mode_bt:
                L.v(TAG, "setOnCheckedChangeListener, select_passive_mode");
                mMode = Message.LocationMode.LM_PASSIVE_VALUE;
                modifyLocationMode(mMode);
                break;
            case R.id.save_power_mode_bt:
                L.v(TAG, "setOnCheckedChangeListener, select_save_power_mode");
                mMode = Message.LocationMode.LM_POWER_SAVING_VALUE;
                modifyLocationMode(mMode);
                break;
            case R.id.normal_mode_bt:
                L.v(TAG, "setOnCheckedChangeListener, select_normal_mode");
                mMode = Message.LocationMode.LM_NORMAL_VALUE;
                modifyLocationMode(mMode);
                break;

        }
    }


    //修改手表定位模式
    private void modifyLocationMode(int mode) {
        if (TextUtils.isEmpty(devId)) {
            L.e(TAG, " ERROR: decId is null");
            return;
        }
        Message.LocationMode locationMode;
        if (mode == Message.LocationMode.LM_PASSIVE_VALUE)
            locationMode = Message.LocationMode.LM_PASSIVE;
        else if (mode == Message.LocationMode.LM_POWER_SAVING_VALUE)
            locationMode = Message.LocationMode.LM_POWER_SAVING;
        else if (mode == Message.LocationMode.LM_NORMAL_VALUE)
            locationMode = Message.LocationMode.LM_NORMAL;
        else locationMode = Message.LocationMode.LM_USELESS;

        popWaitingDialog(R.string.loading);
        Message.ModifyLocationModeReqMsg reqMsg = Message.ModifyLocationModeReqMsg.newBuilder().setDeviceId(devId)
                .setLocationMode(locationMode)
                .build();
        exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyLocationModeRspMsg rspMsg = response.getProtoBufMsg();

                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                L.e(TAG, "ModifyLocationModeReqMsg: " + reqMsg);
                                GreenUtils.upDataLocationMode(locationMode, devId);
                                Intent intent = new Intent();
                                intent.putExtra(WatchSettingActivity.LOCATION_MODE_TEXT, locationMode);
                                setResult(RESULT_OK, intent);
                                dismissDialog();
                                finish();
                                return false;
                            }
                            L.w(TAG, "modifyLocationMode() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.w(TAG, "modifyLocationMode() -> exec() -> onResponse(): Exception: ", e);
                        }
                        popErrorDialog(R.string.setting_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "modifyLocationMode() -> exec() -> onException()", cause);
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
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
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
                            L.e(TAG, "GetLocationModeReqMsg: " + rspMsg);
                            dismissDialog();
                            Message.LocationMode locationMode = rspMsg.getLocationMode();
                            GreenUtils.upDataLocationMode(locationMode, devId);
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

    private void getLocationModeFromDB() {
        if (TextUtils.isEmpty(devId)) {
            L.e(TAG, " ERROR: decId is null");
            return;
        }
        List<DeviceEntity> deviceEntities = GreenUtils.getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(devId)).build().list();
        if (!deviceEntities.isEmpty()) {
            DeviceEntity deviceEntity = deviceEntities.get(0);
            mMode = deviceEntity.getLocationMode();
        }
    }


    @Override
    public void onLocationModeChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyLocationModeChangedReqMsg reqMsg) {
        String deviceId = reqMsg.getDeviceId();
        if (devId.equals(deviceId)) {
            mMode = reqMsg.getLocationModeValue();
        }
    }
}
