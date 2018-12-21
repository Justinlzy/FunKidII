package com.cqkct.FunKidII.Ui.Model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class GuardianModel {

    public static final String TAG = GuardianModel.class.getSimpleName();

    WeakReference<BaseActivity> wA;

    public GuardianModel(BaseActivity activity) {
        wA = new WeakReference<>(activity);
    }

    public Message.SchoolGuard loadData(String deviceId) {

        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "loadData deviceId is empty");
            return null;
        }
        Message.SchoolGuard schoolGuard = null;
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            schoolGuard = deviceInfo.getDeviceEntity().getSchoolGuard();
        } else {
            List<DeviceEntity> list = GreenUtils.getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(deviceId)).list();
            if (!list.isEmpty()) {
                schoolGuard = list.get(0).getSchoolGuard();
            }
        }
        return schoolGuard;
    }

    //获取上学守护
    public void getSchoolGuardianData(String devId, OperateDataListener listener) {

        if (TextUtils.isEmpty(devId)) {
            listener.operateFailure(Message.ErrorCode.FAILURE);
            return;
        }

        wA.get().exec(
                Message.GetSchoolGuardReqMsg.newBuilder()
                        .setDeviceId(devId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetSchoolGuardRspMsg rspMsg = response.getProtoBufMsg();
                            L.e(TAG, "getSchoolGuardianData Rsp: " + rspMsg.getErrCode());
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS || rspMsg.getErrCode() == Message.ErrorCode.NOT_EXISTS) {
                                Message.SchoolGuard school_guard = rspMsg.getGuard();
                                GreenUtils.saveSchoolGuard(devId, school_guard);
                                listener.operateSuccess(school_guard);
                                return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        listener.operateFailure(Message.ErrorCode.FAILURE);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                });
    }

    public boolean modifyGuardianSchool(Message.ModifySchoolGuardReqMsg reqMsg, String deviceId, OperateDataListener listener) {

        if (TextUtils.isEmpty(deviceId)) {
            listener.operateFailure(Message.ErrorCode.FAILURE);
            return false;
        }

        wA.get().exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifySchoolGuardRspMsg rspMsg = response.getProtoBufMsg();

                            switch (rspMsg.getErrCode()) {
                                case SUCCESS: {
                                    Message.SchoolGuard schoolGuard = rspMsg.getGuard();
                                    GreenUtils.saveSchoolGuard(deviceId, schoolGuard);
                                    listener.operateSuccess(rspMsg);
                                }
                                break;
                                case FAILURE:
                                    listener.operateFailure(Message.ErrorCode.FAILURE);
                                    L.e(TAG, "onResponse FAILURE");
                                    return false;
                                case NO_DEVICE:
                                    listener.operateFailure(Message.ErrorCode.NO_DEVICE);
                                    L.e(TAG, "onResponse NODEVICE");
                                    return false;
                                case OFFLINE:
                                    listener.operateFailure(Message.ErrorCode.OFFLINE);
                                    L.e(TAG, "onResponse OFFLINE");
                                    return false;
                                case INVALID_PARAM:
                                    listener.operateFailure(Message.ErrorCode.INVALID_PARAM);
                                    L.e(TAG, "onResponse INVALID_PARAM");
                                    return false;

                            }
                            return false;
                        } catch (Exception e) {
                            L.e(TAG, "addNewFence() -> exec() -> onResponse(" + response + ") process failure", e);
                            listener.operateFailure(Message.ErrorCode.FAILURE);
                            e.printStackTrace();
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "ModifySchoolGuard() -> exec() -> onException(" + Message.ModifySchoolGuardRspMsg.class.getSimpleName() + ")", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }

        );
        return true;
    }

}

