package com.cqkct.FunKidII.App;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import protocol.Message;

public interface TlcServiceEventCallback {

    /**
     * TCP 连接成功
     * <p>
     *     需要小心使用<br/>
     *     仅仅是 TCP 网络连接成功，正常的业务应该等到登录成功({@link #onLoggedin(TlcService, String, boolean)})后再请求
     * </p>
     * @param tlcService TlcService
     * @param isSticky 滞留状态
     */
    void onConnected(TlcService tlcService, boolean isSticky);

    void onDisconnected(TlcService tlcService, boolean isSticky);

    void onLoggedin(TlcService tlcService, @NonNull String userId, boolean isSticky);

    void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky);

    void onBindDeviceRequest(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg);

    void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg);

    void onDeviceUnbind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg);

    void onDevConfChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfChangedReqMsg reqMsg);

    void onDevConfSynced(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfSyncedReqMsg reqMsg);

    void onDevFenceChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyFenceChangedReqMsg reqMsg);

    void onDevSosChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifySosChangedReqMsg reqMsg);

    void onDevSosSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifySosSyncedReqMsg reqMsg);

    void onDevContactChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyContactChangedReqMsg reqMsg);

    void onDevContactSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyContactSyncedReqMsg reqMsg);

    void onAlarmClockChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyAlarmClockChangedReqMsg reqMsg);

    void onAlarmClockSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyAlarmClockSyncedReqMsg reqMsg);

    void onClassDisableChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyClassDisableChangedReqMsg reqMsg);

    void onClassDisableSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyClassDisableSyncedReqMsg reqMsg);

    void onSchoolGuardChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifySchoolGuardChangedReqMsg reqMsg);

    void onPraiseChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyPraiseChangedReqMsg reqMsg);

    void onLocateS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.LocateS3ReqMsg reqMsg);

    void onDevicePosition(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevicePositionReqMsg reqMsg);

    void onLocationModeChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyLocationModeChangedReqMsg reqMsg);

    void onDeviceIncident(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyIncidentReqMsg reqMsg);

    void onUsrDevAssocModified(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUsrDevAssocModifiedReqMsg reqMsg);

    void onFindDeviceS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.FindDeviceS3ReqMsg reqMsg);

    void onTakePhotoS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.TakePhotoS3ReqMsg reqMsg);

    void onSimplexCallS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.SimplexCallS3ReqMsg reqMsg);

    void onFetchDevStatusInfoS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.FetchDeviceSensorDataS3ReqMsg reqMsg);

    void onDeviceSensorData(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDeviceSensorDataReqMsg reqMsg);

    void onNewMicroChatMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatMessageReqMsg reqMsg);

    void onNewMicroChatGroupMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyGroupChatMessageReqMsg reqMsg);

    void onNotifySMSAgentNewSMS(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySMSAgentNewSMSReqMsg reqMsg);

    void onDeviceFriendChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyFriendChangedReqMsg reqMsg);

    void onChatGroupMemberChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatGroupMemberChangedReqMsg reqMsg);

    void onNotifySosCallOrderChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySosCallOrderChangedReqMsg reqMsg);

    void onNotProcessedPkt(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull OnResponseSetter responseSetter);
}