package com.cqkct.FunKidII.service.tlc;

import android.support.annotation.NonNull;

import com.cqkct.FunKidII.service.Pkt;

import protocol.Message;

public interface OnEventListener {
    /**
     * 网络连接成功
     */
    void onConnected();

    /**
     * 网络连接丢失
     */
    void onDisconnected();

    /**
     * 已登录
     *
     * @param userId 登录成功的用户名
     */
    void onLoggedin(@NonNull String userId);

    /**
     * 已登出
     */
    void onLoggedout(@NonNull String userId);

    /**
     * 通知管理员有用户绑定设备
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onBindDeviceRequest(@NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg);

    /**
     * 用户绑定或者解绑成功
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求 （管理员的同意与否的信息）
     */
    void onDeviceBind(@NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg);

    /**
     * 解绑成功通知
     * 如果解绑成功，通知被解绑用户
     * 拥有者解绑后，通知所有用户
     * 用户收到此通知后，主动更新相应数据；后台不另行通知
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDeviceUnbind(@NonNull Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg);

    /**
     * 配置更新通知
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevConfChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfChangedReqMsg reqMsg);

    /**
     * 配置已被手表同步通知
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevConfSynced(@NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfSyncedReqMsg reqMsg);

    /**
     * 设备的围栏信息有变动
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevFenceChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyFenceChangedReqMsg reqMsg);

    /**
     * Sos 触发
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevSosChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifySosChangedReqMsg reqMsg);

    /**
     * Sos 已被手表同步
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevSosSynced(@NonNull Pkt reqPkt, @NonNull Message.NotifySosSyncedReqMsg reqMsg);

    /**
     * 联系人变更
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevContactChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyContactChangedReqMsg reqMsg);

    /**
     * 联系人已被手表同步
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevContactSynced(@NonNull Pkt reqPkt, @NonNull Message.NotifyContactSyncedReqMsg reqMsg);

    /**
     * 闹钟变更
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onAlarmClockChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyAlarmClockChangedReqMsg reqMsg);

    /**
     * 闹钟已被手表同步
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onAlarmClockSynced(@NonNull Pkt reqPkt, @NonNull Message.NotifyAlarmClockSyncedReqMsg reqMsg);

    /**
     * 上课禁用变更
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onClassDisableChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyClassDisableChangedReqMsg reqMsg);

    /**
     * 上课禁用已被手表同步
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onClassDisableSynced(@NonNull Pkt reqPkt, @NonNull Message.NotifyClassDisableSyncedReqMsg reqMsg);

    /**
     * 上学守护变更
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onSchoolGuardChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifySchoolGuardChangedReqMsg reqMsg);

    /**
     * 集赞变更
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onPraiseChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyPraiseChangedReqMsg reqMsg);

    /**
     * 服务器推送实时定位S3
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onLocateS3(@NonNull Pkt reqPkt, @NonNull Message.LocateS3ReqMsg reqMsg);

    /**
     * 服务器推送设备在线/离线
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDeviceOnline(@NonNull Pkt reqPkt, @NonNull Message.NotifyOnlineStatusOfDevReqMsg reqMsg);

    /**
     * 服务器推送设备定位位置信息
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDevicePosition(@NonNull Pkt reqPkt, @NonNull Message.NotifyDevicePositionReqMsg reqMsg);


    /**
     * 服务器推送设备定位Mode信息
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onLocationModeChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyLocationModeChangedReqMsg reqMsg);

    /**
     * 设备事件
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDeviceIncident(@NonNull Pkt reqPkt, @NonNull Message.NotifyIncidentReqMsg reqMsg);

    /**
     * 设备好友变动
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDeviceFriendChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyFriendChangedReqMsg reqMsg);


    /**
     * 用户与设备关联信息被修改
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onUsrDevAssocModified(@NonNull Pkt reqPkt, @NonNull Message.NotifyUsrDevAssocModifiedReqMsg reqMsg);


    /**
     * 查找手表S3
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onFindDeviceS3(@NonNull Pkt reqPkt, @NonNull Message.FindDeviceS3ReqMsg reqMsg);

    /**
     * 手表拍照S3
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onTakePhotoS3(@NonNull Pkt reqPkt, @NonNull Message.TakePhotoS3ReqMsg reqMsg);

    /**
     * 单向通话S3
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onSimplexCallS3(@NonNull Pkt reqPkt, @NonNull Message.SimplexCallS3ReqMsg reqMsg);

    /**
     * 查询手表传感器信息S3
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onFetchDeviceSensorDataS3(@NonNull Pkt reqPkt, @NonNull Message.FetchDeviceSensorDataS3ReqMsg reqMsg);

    /**
     * 手表新的传感器信息报告
     *
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onDeviceSensorData(@NonNull Pkt reqPkt, @NonNull Message.NotifyDeviceSensorDataReqMsg reqMsg);

    /**
     * 新文本消息
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onNewMicroChatMessage(@NonNull Pkt reqPkt, @NonNull Message.NotifyChatMessageReqMsg reqMsg);


    /**
     * 新群组消息
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onNewMicroChatGroupMessage(@NonNull Pkt reqPkt, @NonNull Message.NotifyGroupChatMessageReqMsg reqMsg);

    /**
     * 新文本消息
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onNotifySMSAgentNewSMS(@NonNull Pkt reqPkt, @NonNull Message.NotifySMSAgentNewSMSReqMsg reqMsg);

    /**
     * 新文本消息
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onNotifyChatGroupMemberChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyChatGroupMemberChangedReqMsg reqMsg);


    /**
     * sos号码顺序修改通知
     * @param reqPkt 服务器发过来的请求
     * @param reqMsg 服务器发过来的请求
     */
    void onNotifySosCallOrderChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifySosCallOrderChangedReqMsg reqMsg);

    /**
     * 未找到处理器的 TLV
     *
     * @param reqPkt         服务器发过来的请求
     * @param responseSetter OnResponseSetter
     */
    void onNotProcessedPkt(@NonNull Pkt reqPkt, @NonNull OnResponseSetter responseSetter);
}