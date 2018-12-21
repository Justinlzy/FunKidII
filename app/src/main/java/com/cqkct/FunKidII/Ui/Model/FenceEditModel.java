package com.cqkct.FunKidII.Ui.Model;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cqkct.FunKidII.Ui.Activity.FenceEditActivity;
import com.cqkct.FunKidII.Ui.Listener.OperateDataListener;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class FenceEditModel {

    public static final String TAG = FenceEditModel.class.getSimpleName();

    private WeakReference<FenceEditActivity> a;

    public FenceEditModel(FenceEditActivity activity) {
        a = new WeakReference<>(activity);
    }


    //添加围栏
    public void addNewFence(String deviceId, Message.Fence.Builder mFenceBuilder, OperateDataListener listener) {
        a.get().exec(
                Message.AddFenceReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .addFence(mFenceBuilder.build())
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.AddFenceRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "addNewFence() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                listener.operateSuccess(rspMsg);
                                return false;
                            } else if (rspMsg.getErrCode() == Message.ErrorCode.OUT_OF_LIMIT) {

                                listener.operateFailure(Message.ErrorCode.OUT_OF_LIMIT);
                                return false;
                            }
                            L.w(TAG, "addNewFence() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "addNewFence() -> exec() -> onResponse() process failure", e);
                        }
                        listener.operateFailure(Message.ErrorCode.FAILURE);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "addNewFence() -> exec() -> onException(" + Message.PushDevConfRspMsg.class.getSimpleName() + ")", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                });
    }

    //modify fence
    public void modifyFence(String deviceId, Message.ModifyFenceReqMsg reqMsg, OperateDataListener listener) {
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "modifyFence: deviceId is empty");
            return;
        }

        a.get().exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyFenceRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "addNewFence() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                listener.operateSuccess(rspMsg);
                                return false;
                            } else if (rspMsg.getErrCode() == Message.ErrorCode.OUT_OF_LIMIT) {
                                listener.operateFailure(Message.ErrorCode.OUT_OF_LIMIT);
                                return false;
                            }
                            L.w(TAG, "addNewFence() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "addNewFence() -> exec() -> onResponse() process failure", e);
                        }
                        listener.operateFailure(Message.ErrorCode.FAILURE);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "addNewFence() -> exec() -> onException(" + Message.PushDevConfRspMsg.class.getSimpleName() + ")", cause);
                        listener.operateFailure(cause instanceof TimeoutException ? Message.ErrorCode.TIMEOUT : Message.ErrorCode.FAILURE);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                });

    }


}
