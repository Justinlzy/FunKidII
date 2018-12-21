package com.cqkct.FunKidII.service.tlc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.service.Pkt;

import protocol.Message;

interface PktProcessor {
    interface OnResponse {
        void setResponse(@NonNull Pkt rspPkt);
    }

    void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull Pkt reqPkt, @NonNull OnResponse responseCallback);
}


/**
 * Pkt processor for ForceExitReqMsg
 */
class PktPForceExit implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull Pkt reqPkt, @NonNull OnResponse responseCallback) {
        if (tlcService != null) {
            tlcService.onExtrudedLoggedout();
        }
        protocol.Message.ForceExitReqMsg reqMsg = null;
        protocol.Message.ForceExitRspMsg.Builder rspMsg = Message.ForceExitRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.ForceExitReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
    }
}

/**
 * Pkt processor for NotifyAdminBindDevReqMsg
 */
class PktPNotifyAdminBindDev implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        try {
            protocol.Message.NotifyAdminBindDevReqMsg reqMsg = reqPkt.getProtoBufMsg();
            if (eventListener != null) {
                eventListener.onBindDeviceRequest(reqPkt, reqMsg);
            }
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyAdminBindDevReqMsg.class.getSimpleName(), e);
        }
    }
}

/**
 * Pkt processor for NotifyUserBindDevReqMsg
 */
class PktPNotifyUserBindDev implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyUserBindDevReqMsg reqMsg = null;
        protocol.Message.NotifyUserBindDevRspMsg.Builder rspMsg = Message.NotifyUserBindDevRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyUserBindDevReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDeviceBind(reqPkt, reqMsg);
        }
    }
}


/**
 * Pkt processor for NotifyUserUnbindDevReqMsg
 */
class PktPNotifyUserUnbindDev implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyUserUnbindDevReqMsg reqMsg = null;
        protocol.Message.NotifyUserUnbindDevRspMsg.Builder rspMsg = Message.NotifyUserUnbindDevRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyUserUnbindDevReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDeviceUnbind(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyDevConfChangedReqMsg
 */
class PktPNotifyDevConfChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyDevConfChangedReqMsg reqMsg = null;
        protocol.Message.NotifyDevConfChangedRspMsg.Builder rspMsg = Message.NotifyDevConfChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyDevConfChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevConfChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyDevConfSyncedReqMsg
 */
class PktPNotifyDevConfSynced implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyDevConfSyncedReqMsg reqMsg = null;
        protocol.Message.NotifyDevConfSyncedRspMsg.Builder rspMsg = Message.NotifyDevConfSyncedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyDevConfSyncedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevConfSynced(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyFenceChangedReqMsg
 */
class PktPNotifyFenceChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyFenceChangedReqMsg reqMsg = null;
        protocol.Message.NotifyFenceChangedRspMsg.Builder rspMsg = Message.NotifyFenceChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyFenceChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevFenceChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifySosChangedReqMsg
 */
class PktPNotifySosChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifySosChangedReqMsg reqMsg = null;
        protocol.Message.NotifySosChangedRspMsg.Builder rspMsg = Message.NotifySosChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifySosChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevSosChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifySosSyncedReqMsg
 */
class PktPNotifySosSynced implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifySosSyncedReqMsg reqMsg = null;
        protocol.Message.NotifySosSyncedRspMsg.Builder rspMsg = Message.NotifySosSyncedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifySosSyncedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevSosSynced(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyContactChangedReqMsg
 */
class PktPNotifyContactChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyContactChangedReqMsg reqMsg = null;
        protocol.Message.NotifyContactChangedRspMsg.Builder rspMsg = Message.NotifyContactChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyContactChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevContactChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyContactSyncedReqMsg
 */
class PktPNotifyContactSynced implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyContactSyncedReqMsg reqMsg = null;
        protocol.Message.NotifyContactSyncedRspMsg.Builder rspMsg = Message.NotifyContactSyncedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyContactSyncedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevContactSynced(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyAlarmClockChangedReqMsg
 */
class PktPNotifyAlarmClockChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyAlarmClockChangedReqMsg reqMsg = null;
        protocol.Message.NotifyAlarmClockChangedRspMsg.Builder rspMsg = Message.NotifyAlarmClockChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyAlarmClockChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onAlarmClockChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyAlarmClockSyncedReqMsg
 */
class PktPNotifyAlarmClockSynced implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyAlarmClockSyncedReqMsg reqMsg = null;
        protocol.Message.NotifyAlarmClockSyncedRspMsg.Builder rspMsg = Message.NotifyAlarmClockSyncedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyAlarmClockSyncedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onAlarmClockSynced(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyClassDisableChangedReqMsg
 */
class PktPNotifyClassDisableChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyClassDisableChangedReqMsg reqMsg = null;
        protocol.Message.NotifyClassDisableChangedRspMsg.Builder rspMsg = Message.NotifyClassDisableChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyClassDisableChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onClassDisableChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyClassDisableSyncedReqMsg
 */
class PktPNotifyClassDisableSynced implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyClassDisableSyncedReqMsg reqMsg = null;
        protocol.Message.NotifyClassDisableSyncedRspMsg.Builder rspMsg = Message.NotifyClassDisableSyncedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyClassDisableSyncedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onClassDisableSynced(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifySchoolGuardChangedReqMsg
 */
class PktPNotifySchoolGuardChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifySchoolGuardChangedReqMsg reqMsg = null;
        protocol.Message.NotifySchoolGuardChangedRspMsg.Builder rspMsg = Message.NotifySchoolGuardChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifySchoolGuardChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onSchoolGuardChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyPraiseChangedReqMsg
 */
class PktPNotifyPraiseChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyPraiseChangedReqMsg reqMsg = null;
        protocol.Message.NotifyPraiseChangedRspMsg.Builder rspMsg = Message.NotifyPraiseChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyPraiseChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onPraiseChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for LocateS3ReqMsg
 */
class PktPLocateS3 implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.LocateS3ReqMsg reqMsg = null;
        protocol.Message.LocateS3RspMsg.Builder rspMsg = Message.LocateS3RspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.LocateS3ReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onLocateS3(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyOnlineStatusOfDevReqMsg
 */
class PktPNotifyOnlineStatusOfDev implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyOnlineStatusOfDevReqMsg reqMsg = null;
        protocol.Message.NotifyOnlineStatusOfDevRspMsg.Builder rspMsg = Message.NotifyOnlineStatusOfDevRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyOnlineStatusOfDevReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDeviceOnline(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyDevicePositionReqMsg
 */
class PktPNotifyDevicePosition implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyDevicePositionReqMsg reqMsg = null;
        protocol.Message.NotifyDevicePositionRspMsg.Builder rspMsg = Message.NotifyDevicePositionRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyDevicePositionReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDevicePosition(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyLocationModeChangedRspMsg
 */
class PktPNotifyLocationModeChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyLocationModeChangedReqMsg reqMsg = null;
        protocol.Message.NotifyLocationModeChangedRspMsg.Builder rspMsg = Message.NotifyLocationModeChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyLocationModeChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onLocationModeChanged(reqPkt, reqMsg);
        }
    }
}


/**
 * Pkt processor for NotifyIncidentReqMsg
 */
class PktPNotifyIncident implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyIncidentReqMsg reqMsg = null;
        protocol.Message.NotifyIncidentRspMsg.Builder rspMsg = Message.NotifyIncidentRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyIncidentReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDeviceIncident(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyFriendChangedReqMsg
 */
class PktNotifyFriendChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyFriendChangedReqMsg reqMsg = null;
        protocol.Message.NotifyFriendChangedRspMsg.Builder rspMsg = Message.NotifyFriendChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyFriendChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDeviceFriendChanged(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyUsrDevAssocModifiedReqMsg
 */
class PktPNotifyUsrDevAssocModified implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyUsrDevAssocModifiedReqMsg reqMsg = null;
        protocol.Message.NotifyUsrDevAssocModifiedRspMsg.Builder rspMsg = Message.NotifyUsrDevAssocModifiedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyUsrDevAssocModifiedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onUsrDevAssocModified(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for FindDeviceS3ReqMsg
 */
class PktPFindDeviceS3 implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.FindDeviceS3ReqMsg reqMsg = null;
        protocol.Message.FindDeviceS3RspMsg.Builder rspMsg = Message.FindDeviceS3RspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.FindDeviceS3ReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onFindDeviceS3(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for TakePhotoS3ReqMsg
 */
class PktPTakePhotoS3 implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.TakePhotoS3ReqMsg reqMsg = null;
        protocol.Message.TakePhotoS3RspMsg.Builder rspMsg = Message.TakePhotoS3RspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.TakePhotoS3ReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onTakePhotoS3(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for SimplexCallS3ReqMsg
 */
class PktPSimplexCallS3 implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, @Nullable OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.SimplexCallS3ReqMsg reqMsg = null;
        protocol.Message.SimplexCallS3RspMsg.Builder rspMsg = Message.SimplexCallS3RspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.SimplexCallS3ReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onSimplexCallS3(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for FetchDeviceSensorDataS3ReqMsg
 */
class PktPFetchDeviceSensorDataS3 implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.FetchDeviceSensorDataS3ReqMsg reqMsg = null;
        protocol.Message.FetchDeviceSensorDataS3RspMsg.Builder rspMsg = Message.FetchDeviceSensorDataS3RspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.FetchDeviceSensorDataS3ReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onFetchDeviceSensorDataS3(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyDeviceSensorDataReqMsg
 */
class PktPNotifyDeviceSensorData implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyDeviceSensorDataReqMsg reqMsg = null;
        protocol.Message.NotifyDeviceSensorDataRspMsg.Builder rspMsg = Message.NotifyDeviceSensorDataRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyDeviceSensorDataReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onDeviceSensorData(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyMicroChatEmoticonReqMsg
 */
class PktPNotifyMicroChatEmoticon implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyMicroChatEmoticonReqMsg reqMsg = null;
        protocol.Message.NotifyMicroChatEmoticonRspMsg.Builder rspMsg = Message.NotifyMicroChatEmoticonRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyMicroChatEmoticonReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            protocol.Message.NotifyChatMessageReqMsg chatMsgReq = protocol.Message.NotifyChatMessageReqMsg.newBuilder()
                    .setSrc(reqMsg.getSrcAddr())
                    .setMsg(protocol.Message.ChatMessage.newBuilder().setEmoticon(reqMsg.getEmoticonId()))
                    .build();
            eventListener.onNewMicroChatMessage(reqPkt, chatMsgReq);
        }
    }
}

/**
 * Pkt processor for NotifyMicroChatVoiceReqMsg
 */
class PktPNotifyMicroChatVoice implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyMicroChatVoiceReqMsg reqMsg = null;
        protocol.Message.NotifyMicroChatVoiceRspMsg.Builder rspMsg = Message.NotifyMicroChatVoiceRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyMicroChatVoiceReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            protocol.Message.NotifyChatMessageReqMsg chatMsgReq = protocol.Message.NotifyChatMessageReqMsg.newBuilder()
                    .setSrc(reqMsg.getSrcAddr())
                    .setMsg(protocol.Message.ChatMessage.newBuilder().setVoice(
                            protocol.Message.ChatMessage.Voice.newBuilder()
                            .setFileName(reqMsg.getFileName())
                            .setFileSize(reqMsg.getFileSize())
                            .setDuration(reqMsg.getDuration())
                    ))
                    .build();
            eventListener.onNewMicroChatMessage(reqPkt, chatMsgReq);
        }
    }
}

/**
 * Pkt processor for NotifyMicroChatTextReqMsg
 */
class PktPNotifyMicroChatText implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyMicroChatTextReqMsg reqMsg = null;
        protocol.Message.NotifyMicroChatTextRspMsg.Builder rspMsg = Message.NotifyMicroChatTextRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyMicroChatTextReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            protocol.Message.NotifyChatMessageReqMsg chatMsgReq = protocol.Message.NotifyChatMessageReqMsg.newBuilder()
                    .setSrc(reqMsg.getSrcAddr())
                    .setMsg(protocol.Message.ChatMessage.newBuilder().setText(reqMsg.getText()))
                    .build();
            eventListener.onNewMicroChatMessage(reqPkt, chatMsgReq);
        }
    }
}

/**
 * Pkt processor for NotifyChatMessageReqMsg
 */
class PktPNotifyChatMessage implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyChatMessageReqMsg reqMsg = null;
        protocol.Message.NotifyChatMessageRspMsg.Builder rspMsg = Message.NotifyChatMessageRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyChatMessageReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onNewMicroChatMessage(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyGroupChatMessageReqMsg
 */
class PktPNotifyGroupChatMessage implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyGroupChatMessageReqMsg reqMsg = null;
        protocol.Message.NotifyGroupChatMessageRspMsg.Builder rspMsg = Message.NotifyGroupChatMessageRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyGroupChatMessageReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onNewMicroChatGroupMessage(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyMicroChatVoiceReqMsg
 */
class PktPNotifySMSAgentNewSMS implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifySMSAgentNewSMSReqMsg reqMsg = null;
        protocol.Message.NotifySMSAgentNewSMSRspMsg.Builder rspMsg = Message.NotifySMSAgentNewSMSRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifySMSAgentNewSMSReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onNotifySMSAgentNewSMS(reqPkt, reqMsg);
        }
    }
}

/**
 * Pkt processor for NotifyChatGroupMemberChangedReqMsg
 */
class PktPNotifyChatGroupMemberChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifyChatGroupMemberChangedReqMsg reqMsg = null;
        protocol.Message.NotifyChatGroupMemberChangedRspMsg.Builder rspMsg = Message.NotifyChatGroupMemberChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifyChatGroupMemberChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onNotifyChatGroupMemberChanged(reqPkt, reqMsg);
        }
    }
}
/**
 * Pkt processor for NotifySosCallOrderChangedReqMsg
 */
class PktPNotifySosCallOrderChanged implements PktProcessor {
    @Override
    public void process(@Nullable TlcService tlcService, OnEventListener eventListener, @NonNull final Pkt reqPkt, @NonNull final OnResponse responseCallback) {
        protocol.Message.NotifySosCallOrderChangedReqMsg reqMsg = null;
        protocol.Message.NotifySosCallOrderChangedRspMsg.Builder rspMsg = Message.NotifySosCallOrderChangedRspMsg.newBuilder().setErrCode(Message.ErrorCode.FAILURE);
        try {
            reqMsg = reqPkt.getProtoBufMsg();
        } catch (Exception e) {
            L.e(getClass().getSimpleName(), "process() failure: request Pkt(" + reqPkt + ") value is not " + protocol.Message.NotifySosCallOrderChangedReqMsg.class.getSimpleName(), e);
        }
        if (reqMsg != null) {
            rspMsg.setErrCode(Message.ErrorCode.SUCCESS);
        }
        Pkt rspPkt = Pkt.newBuilder().setSeq(reqPkt.seq).setSrcAddr(reqPkt.dstAddr).setValue(rspMsg.build()).build();
        responseCallback.setResponse(rspPkt);
        if (reqMsg != null && eventListener != null) {
            eventListener.onNotifySosCallOrderChanged(reqPkt, reqMsg);
        }
    }
}
