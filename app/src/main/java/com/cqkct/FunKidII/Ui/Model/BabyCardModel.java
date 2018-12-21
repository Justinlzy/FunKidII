package com.cqkct.FunKidII.Ui.Model;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BabyCardActivity;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class BabyCardModel {

    public static final String TAG = BabyCardModel.class.getSimpleName();
    private WeakReference<BabyCardActivity> a;

    public BabyCardModel(BabyCardActivity activity) {
        this.a = new WeakReference<>(activity);
    }


    public void submitBabyInformationToService(BabyEntity entity, OperateDataListener listener) {

        Message.Baby.Builder builder = Message.Baby.newBuilder()
                .setAvatar(entity.getBabyAvatar())
                .setName(entity.getName())
                .setPhone(entity.getPhone())
                .setSex(entity.getSex() == Message.Baby.Sex.FEMALE_VALUE ? Message.Baby.Sex.FEMALE : Message.Baby.Sex.MALE)
                .setBirthday(entity.getBirthday());


        // 第一步:上传配置信息 第二步:保存信息至本地
        Message.DevConf devConf = Message.DevConf.newBuilder()
                .setBaby(builder.build())
                .build();

        final Message.PushDevConfReqMsg pushConfigReqMsg = Message.PushDevConfReqMsg.newBuilder()
                .setDeviceId(entity.getDeviceId())
                .setFlag(Message.DevConfFlag.DCF_BABY_VALUE)
                .setConf(devConf)
                .build();
        L.v(TAG, "submitBabyInformationToService: " + pushConfigReqMsg);
        a.get().exec(
                pushConfigReqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.PushDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "submitBabyInformationToService() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                listener.operateSuccess(pushConfigReqMsg);
                            } else {
                                listener.operateFailure(rspMsg.getErrCode());

                            }
                        } catch (Exception e) {
                            L.e(TAG, "submitBabyInformationToService() -> exec() -> onResponse() process failure", e);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "submitBabyInformationToService() -> exec() -> onException()", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    public void unbindDevice(String deviceId, String userId, OperateDataListener listener) {
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "unbindDevice: deviceId is empty");
            return;
        }
        List<BabyEntity> babyEntityList = GreenUtils.getBabyEntityDao().queryBuilder().where(BabyEntityDao.Properties.DeviceId.eq(deviceId)).list();
        if (babyEntityList.isEmpty()) {
            L.e(TAG, "not found bind relation on device: " + deviceId);
            return;
        }
        BabyEntity babyEntity = babyEntityList.get(0);
        if (babyEntity.getPermission() == Message.UsrDevAssoc.Permission.OWNER_VALUE) {

            ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                    .setTitle(a.get().getString(R.string.unbind))
                    .setMessage(a.get().getString(R.string.unbind_owner_device_tip))
                    .setPositiveButton(a.get().getString(R.string.ok), (dialog, which) -> doUnbindDevice(deviceId, userId, listener))
                    .setNegativeButton((a.get().getString(R.string.cancel)), null);
            dialogFragment.show(a.get().getSupportFragmentManager(), "showAssociateSuccess");
//            UnBindDeviceAndBindActivity.UnbindDeviceDialogFragment.newInstance(deviceId).show(a.get().getSupportFragmentManager(), "UnbindDeviceDialogFragment");
        } else {
            doUnbindDevice(deviceId, userId, listener);
        }
    }

    private void doUnbindDevice(final String deviceId, String userId, OperateDataListener listener) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "doUnbindDevice failure: userId isEmpty or deviceId isEmpty");
            return;
        }


        protocol.Message.UnbindDevReqMsg reqMsg = protocol.Message.UnbindDevReqMsg.newBuilder()
                .setUsrDevAssoc(protocol.Message.UsrDevAssoc.newBuilder()
                        .setUserId(userId)
                        .setDeviceId(deviceId)
                        .build())
                .build();

        a.get().exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            protocol.Message.UnbindDevRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "doUnbindDevice() -> exec() -> onResponse(): " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                case NOT_EXISTS:
                                    GreenUtils.clearDeviceWhenUnbind(deviceId, userId, rspMsg.getClearLevel());
                                    listener.operateSuccess(null);
                                    return false;
                                default:
                                    listener.operateFailure(rspMsg.getErrCode());
                                    break;

                            }
                        } catch (Exception e) {
                            L.d(TAG, "doUnbindDevice() -> exec() -> onResponse() process failure", e);
                        }
                        listener.operateFailure(Message.ErrorCode.FAILURE);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.d(TAG, "doUnbindDevice() -> exec() -> onException()", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }

    public void submitBabyRelation(Message.UsrDevAssoc usrDevAssoc, OperateDataListener listener) {

        L.v(TAG, "submitBabyRelation: " + usrDevAssoc);
        Message.ModifyUsrDevAssocReqMsg reqMsg = Message.ModifyUsrDevAssocReqMsg.newBuilder()
                .setUsrDevAssoc(usrDevAssoc)
                .build();
        BabyCardActivity activity = a.get();
        if (activity == null) {
            return;
        }
        activity.exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyUsrDevAssocRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "submitBabyRelation() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                if (usrDevAssoc.getUserId().equals(activity.mUserId)) {
                                    GreenUtils.saveUsrDevAssoc(reqMsg.getUsrDevAssoc());
                                }
//                                GreenUtils.addOrModifyContact(usrDevAssoc.getDeviceId(), );
                                GreenUtils.tryModifyFamilyChatGroupMember(reqMsg.getUsrDevAssoc());
                                listener.operateSuccess(usrDevAssoc);
                            } else {
                                listener.operateFailure(rspMsg.getErrCode());

                            }
                        } catch (Exception e) {
                            L.e(TAG, "submitBabyInformationToService() -> exec() -> onResponse() process failure", e);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "submitBabyInformationToService() -> exec() -> onException()", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }


//
//    List<BabyEntity> getDbBabyInformation(String userId) {
//
//        BabyEntityDao dao = GreenUtils.getBabyEntityDao();
//        List<BabyEntity> dbBabies = dao.queryBuilder()
//                .where(BabyEntityDao.Properties.UserId.eq(userId))
//                .list();
//
//        if (dbBabies.isEmpty()) {
//            L.e("BabyCardModel", "getBabyInformation not found data where userId == " + deviceId + " and UserId == ");
//            return null;
//        }
//        dao.detachAll();
//        return dbBabies;
//    }

//
//    void queryBabyFromServer(List<String> devId, String userId, OperateDataListener listener) {
//        if (TextUtils.isEmpty(devId) || TextUtils.isEmpty(userId)) {
//            L.w(TAG, "queryFromServer: deviceId or userId is empty");
//            return;
//        }
//
//        final protocol.Message.FetchDevConfReqMsg reqMsg = protocol.Message.FetchDevConfReqMsg.newBuilder()
//                .setDeviceId(devId)
//                .setFlag(Message.DevConfFlag.DCF_BABY_VALUE)
//                .build();
//
//        a.popWaitingDialog(R.string.loading);
//
//        a.exec(
//                reqMsg,
//
//                new TlcService.OnExecListener() {
//                    @Override
//                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
//                        try {
//                            Message.FetchDevConfRspMsg rspMsg = response.getProtoBufMsg();
//                            L.v(TAG, "queryBabyFromServer() -> exec() -> onResponse(): " + rspMsg);
//                            switch (rspMsg.getErrCode()) {
//                                case SUCCESS: {
//                                    listener.operateSuccess(rspMsg);
//                                    long flag = rspMsg.getFlag();
//                                    GreenUtils.saveConfigs(rspMsg.getConf(), flag, reqMsg.getDeviceId(), true);
//                                    originBaby = rspMsg.getConf().getBaby();
////                                    updateView(originBaby);
//                                    a.dismissDialog();
//                                }
//                                break;
//                                case FAILURE:
//                                    listener.operateFailure(false);
//                                    L.e(TAG, "queryBabyFromServer() -> exec() -> onResponse() failure");
//                                    a.popErrorDialog(R.string.load_failure);
//                                    break;
//                                default:
//                                    break;
//                            }
//                        } catch (Exception e) {
//                            listener.operateFailure(false);
//                            L.e(TAG, "queryBabyFromServer() -> exec() -> onResponse() process failure", e);
//                            a.popErrorDialog(R.string.load_failure);
//                        }
//
//                        return false;
//                    }
//
//                    @Override
//                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
//                        L.e(TAG, "queryBabyFromServer() -> exec(" + Message.FetchDevConfReqMsg.class.getSimpleName() + ") -> onException()", cause);
//                        a.popErrorDialog(cause instanceof TimeoutException ? R.string.load_timeout : R.string.load_failure);
//                        listener.operateFailure(cause instanceof TimeoutException);
//                    }
//
//                    @Override
//                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
//                        // never to here for this exec
//                    }
//                }
//        );
//    }


}
