package com.cqkct.FunKidII.Ui.Model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.FenceListActivity;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.db.Entity.FenceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class FenceListMode {

    public static final String TAG = FenceListMode.class.getSimpleName();

    private WeakReference<FenceListActivity> mA;

    public FenceListMode(FenceListActivity a) {
        mA = new WeakReference<>(a);
    }


    public int loadLimit(String mDeviceId) {
        if (TextUtils.isEmpty(mDeviceId)) {
            L.e(TAG, "getFenceInfoInDb: deviceId is empty");
            return 0;
        }
        List<DeviceEntity> deviceEntities = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(mDeviceId))
                .build().list();
        if (!deviceEntities.isEmpty()) {
            DeviceEntity deviceEntity = deviceEntities.get(0);
            Message.DeviceSysInfo sysInfo = deviceEntity.getSysInfo();
            int cnt = sysInfo.getLimit().getCountOfFence();
            if (cnt > 0)
                return cnt;
        }
        return 0;
    }


    private void loadData(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "loadData deviceId is empty");
            return;
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
        if (schoolGuard != null) {
//            updateView(schoolGuard);
        }
    }



    //获取上学守护
    private void getSchoolGuardianData(String devId, OperateDataListener listener) {
//        popWaitingDialog(R.string.loading);
        if (TextUtils.isEmpty(devId))
            return;

        mA.get().exec(
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
//                                updateView(school_guard);
//                                dismissDialog();
                                return false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        popErrorDialog(R.string.load_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
//                        if (cause instanceof TimeoutException)
//                            popErrorDialog(R.string.load_timeout);
//                        else
//                            popErrorDialog(R.string.load_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                });
    }



    //删除围栏
    public void deleteFence(String deviceId, FenceEntity entity, OperateDataListener listener) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "deleteFence: deviceId is empty: " + deviceId);
            return;
        }
        if (entity == null) {
            L.w(TAG, "delete FenceEntity is null");
            return;
        }

        mA.get().exec(
                protocol.Message.DelFenceReqMsg.newBuilder().setDeviceId(deviceId)
                        .addFenceId(entity.getFenceId())
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            protocol.Message.DelFenceRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "deleteFence() -> exec() -> onResponse()" + rspMsg);
                            if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                                GreenUtils.deleteFence(deviceId, entity.getFenceId());
                                listener.operateSuccess(null);
                                return false;
                            }
                            L.w(TAG, "deleteFence() -> exec() -> onResponse() process Pkt failure: " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "deleteFence() -> exec() -> onResponse() process Pkt failure", e);
                            listener.operateFailure(Message.ErrorCode.FAILURE);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "deleteFence() -> exec() -> onException()", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);

                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }

    public void getFenceInfo(String deviceId, OperateDataListener listener) {
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "getFenceInfo() device ID: " + deviceId);
            listener.operateFailure(Message.ErrorCode.FAILURE);
            return;
        }

        mA.get().exec(
                protocol.Message.GetFenceReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .build(),

                new TlcService.OnExecListener() {

                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            protocol.Message.GetFenceRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "getFenceInfo() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                                listener.operateSuccess(rspMsg);
                                return false;
                            }
                            L.w(TAG, "getFenceInfo() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "getFenceInfo() -> exec() -> onResponse():  process Pkt failure", e);
                        }
                        listener.operateFailure(Message.ErrorCode.FAILURE);

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "getFenceInfo() -> exec(TAG_FETCH_CONF CONF_FLAG_FENCES) -> onException()", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);

                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }


}
