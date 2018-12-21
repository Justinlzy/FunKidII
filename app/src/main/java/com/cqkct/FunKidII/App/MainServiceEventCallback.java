package com.cqkct.FunKidII.App;

import android.support.annotation.NonNull;

import com.cqkct.FunKidII.service.Pkt;

import protocol.Message;

public interface MainServiceEventCallback {
    /**
     * 有用户请求绑定设备
     * @param toUser 消息接收者
     * @param reqPkt 消息数据
     * @param reqMsg 解码后的消息
     * @param reqUserInfo 请求人的用户信息
     */
    void onBindRequest(@NonNull String toUser, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg, @NonNull Message.FetchUsrDevParticRspMsg usrDevPartic);
}