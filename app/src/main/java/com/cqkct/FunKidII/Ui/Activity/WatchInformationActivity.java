package com.cqkct.FunKidII.Ui.Activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by Administrator on 2017/8/3.
 */

public class WatchInformationActivity extends BaseActivity {
    private static final String TAG = WatchInformationActivity.class.getSimpleName();

    static private final LongSparseArray<Integer> hwFeatureMap = new LongSparseArray<>();
    static {
        hwFeatureMap.put(protocol.Message.HwFeature.HWF_GPS_VALUE, R.id.has_gps);
        hwFeatureMap.put(protocol.Message.HwFeature.HWF_WIFI_VALUE, R.id.has_wifi);
        hwFeatureMap.put(protocol.Message.HwFeature.HWF_ACCELEROMETER_VALUE, R.id.has_g_sensor);
    }
    private TextView model, hwVer, swVer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_information);
        setTitleBarTitle(R.string.title_device_info);

        initView();

        initData();
    }

    private void initView() {
        model = (TextView) findViewById(R.id.model);
        model.setSelected(true);
        hwVer = (TextView) findViewById(R.id.hw_ver);
        hwVer.setSelected(true);
        swVer = (TextView) findViewById(R.id.sw_ver);
        swVer.setSelected(true);
    }

    private void updateView(protocol.Message.DeviceSysInfo deviceSysInfo) {
        if (deviceSysInfo == null)
            return;
        if (TextUtils.isEmpty(deviceSysInfo.getModel())) {
            L.w(TAG, "updateView: deviceSysInfo.getModelNum is empty");
            return;
        }

        String showModel = deviceSysInfo.getCustomModel();
        if (TextUtils.isEmpty(showModel))
            showModel = deviceSysInfo.getModel();
        model.setText(showModel);
        if (!TextUtils.isEmpty(deviceSysInfo.getHwVer()))
            hwVer.setText(deviceSysInfo.getHwVer());
        if (!TextUtils.isEmpty(deviceSysInfo.getSwVer()))
            swVer.setText(deviceSysInfo.getSwVer());

        long hwFeature = deviceSysInfo.getHwFeature();
        for (int i = 0; i < 64; ++i) {
            long mask = 1L << i;
            if ((hwFeature & mask) != 0) {
                hwFeature ^= mask;
                Integer resId = hwFeatureMap.get(mask);
                if (resId != null && resId != 0) {
                    findViewById(resId).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void initData() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "initData: deviceId is empty");
            return;
        }

        List<DeviceEntity> list = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(deviceId))
                .build().list();
        if (list.isEmpty()) {
            queryDeviceSysInfoFromServer();
            return;
        }
        DeviceEntity deviceEntity = list.get(0);
        protocol.Message.DeviceSysInfo deviceSysInfo = deviceEntity.getSysInfo();
        if (TextUtils.isEmpty(deviceSysInfo.getModel())) {
            queryDeviceSysInfoFromServer();
            return;
        }
        updateView(deviceSysInfo);
    }

    private void queryDeviceSysInfoFromServer() {
        String deviceId = mDeviceId;
        final String userId = mUserId;
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(userId)) {
            L.w(TAG, "queryFromServer: deviceId or userId is empty");
            return;
        }

        final protocol.Message.FetchDevConfReqMsg reqMsg = protocol.Message.FetchDevConfReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .setFlag(Message.DevConfFlag.DCF_DEV_SYS_INFO_VALUE)
                .build();

        popWaitingDialog(R.string.loading);

        exec(
                reqMsg,

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.FetchDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "queryDeviceSysInfoFromServer() -> exec() -> onResponse(): " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case NO_DEVICE:
                                    L.e(TAG, "queryDeviceSysInfoFromServer() -> exec() -> onResponse() device not exists");
                                    break;
                                case SUCCESS: {
                                    long flag = rspMsg.getFlag();
                                    GreenUtils.saveConfigs(rspMsg.getConf(), flag, reqMsg.getDeviceId());
                                    updateView(rspMsg.getConf().getDevSysInfo());
                                    dismissDialog();
                                }
                                break;
                                case FAILURE:
                                    L.e(TAG, "queryDeviceSysInfoFromServer() -> exec() -> onResponse() failure");
                                    popErrorDialog(R.string.load_failure);
                                    break;
                                default:
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "queryDeviceSysInfoFromServer() -> exec() -> onResponse() process failure", e);
                            popErrorDialog(R.string.load_failure);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "queryDeviceSysInfoFromServer() -> exec(" + Message.FetchDevConfReqMsg.class.getSimpleName() + ") -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? getString(R.string.load_timeout) :getString(R.string.load_failure));
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }
}
