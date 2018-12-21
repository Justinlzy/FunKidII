package com.cqkct.FunKidII.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.App.TlcServiceEventCallback;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Activity.BindRequestActivity;
import com.cqkct.FunKidII.Ui.Activity.MainActivity;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.ChatEntityDao;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.ChatEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.protobuf.GeneratedMessageV3;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Random;

import protocol.Message;


/**
 * Created by justin on 2017/10/24.
 */

public class MainService extends Service implements TlcServiceEventCallback {
    private static final String TAG = MainService.class.getSimpleName();

    public interface OnEventListener {
        /**
         * 有用户请求绑定设备
         *
         * @param toUser       消息接收者
         * @param reqPkt       消息数据
         * @param reqMsg       解码后的消息
         * @param usrDevPartic 请求人的用户信息
         */
        void onBindRequest(@NonNull String toUser, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg, @NonNull Message.FetchUsrDevParticRspMsg usrDevPartic);
    }

    static final String CHANNEL_ID_BIND_REQUEST = "CHANNEL_ID_BIND_REQUEST";
    static final String CHANNEL_ID_CHAT = "CHANNEL_ID_CHAT";
    static final String CHANNEL_ID_MSG_CENTER = "CHANNEL_ID_MSG_CENTER";

    private BroadcastReceiver deviceStateReceiver;

    NotificationManagerCompat mNotificationManagerCompat;
    private static int notificationID = -1;

    synchronized int getNotificationID() {
        if (notificationID++ < 0) {
            notificationID = 1000;
        }
        return notificationID;
    }

    private String mUserId;

    private OnEventListener mOnEventListener;

    public void setOnTlcEventListener(OnEventListener listener) {
        mOnEventListener = listener;
    }

    private class BindRequestNotificationData {
        int count;
        protocol.Message.NotifyAdminBindDevReqMsg reqMsg;

        BindRequestNotificationData(protocol.Message.NotifyAdminBindDevReqMsg reqMsg) {
            count = 1;
            this.reqMsg = reqMsg;
        }
    }

//    private Map<String, Integer> bindReqNotificationIdMap = new HashMap<>();
//    private SparseArray<BindRequestNotificationData> bindReqProtobufMap = new SparseArray<>();


    public class MainServiceBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    private final MainServiceBinder mBinder = new MainServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        L.e(TAG, "onCreate");

        mNotificationManagerCompat = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_BIND_REQUEST, getString(R.string.notification_channel_name_bind_request), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            channel = new NotificationChannel(CHANNEL_ID_CHAT, getString(R.string.notification_channel_name_micro_chat), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            channel = new NotificationChannel(CHANNEL_ID_MSG_CENTER, getString(R.string.notification_channel_name_message_center), NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        App.getInstance().regTlcServiceEventCallback(this);
//        registerConnectivityBroadcasts();

        failureAllPendingSendChatMsg();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
//        unregisterConnectivityBroadcasts();
        App.getInstance().unregTlcServiceEventCallback(this);
        super.onDestroy();
    }

    private void registerConnectivityBroadcasts() {
        if (deviceStateReceiver != null)
            return;

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        deviceStateReceiver = new BroadcastReceiver() {
            private String mNetworkType;
            private boolean mConnected = false;

            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                if (action == null)
                    return;
                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (cm == null)
                        return;
                    NetworkInfo info = cm.getActiveNetworkInfo();

                    boolean connected = (info != null && info.isConnected());
                    String networkType = connected ? info.getTypeName() : "null";

                    // Ignore the event if the current active network is not changed.
                    if (connected == mConnected && networkType.equals(mNetworkType)) {
                        return;
                    }

                    if (!networkType.equals(mNetworkType)) {
                        L.d(TAG, "onConnectivityChanged(): " + mNetworkType + " -> " + networkType);
                        mNetworkType = networkType;
                    }

                    mConnected = connected;

                    if (connected) {
                        checkGoogleAccessibility();
                    }
                }
            }
        };
        registerReceiver(deviceStateReceiver, intentfilter);
    }

    private void unregisterConnectivityBroadcasts() {
        if (deviceStateReceiver != null) {
            unregisterReceiver(deviceStateReceiver);
            deviceStateReceiver = null;
        }
    }

    private void checkGoogleAccessibility() {
        try {
            CheckGoogleAccessibilityThread thread = new CheckGoogleAccessibilityThread();
            thread.start();
        } catch (Exception e) {
            L.e(TAG, "checkGoogleAccessibility", e);
            EventBus.getDefault().postSticky(Event.GoogleAccessibility.UNKNOWN);
        }
    }

    private class CheckGoogleAccessibilityThread extends Thread {
        @Override
        public void run() {
            try {
                URL url = new URL("https://www.google.com/ncr");
                URLConnection urlConn = url.openConnection();
                urlConn.setConnectTimeout(3000);
                urlConn.connect();
                L.i(TAG, "CheckGoogleAccessibilityThread.run OK: ACCESSIBLE");
                EventBus.getDefault().postSticky(Event.GoogleAccessibility.ACCESSIBLE);
            } catch (IOException e) {
                L.i(TAG, "CheckGoogleAccessibilityThread.run: " + e.getMessage());
                EventBus.getDefault().postSticky(Event.GoogleAccessibility.INACCESSIBLE);
            } catch (Exception e) {
                L.i(TAG, "CheckGoogleAccessibilityThread.run: " + e.getMessage());
                EventBus.getDefault().postSticky(Event.GoogleAccessibility.UNKNOWN);
            }
        }
    }


    private void failureAllPendingSendChatMsg() {
        ChatEntityDao dao = GreenUtils.getChatEntityDao();
        List<ChatEntity> list = dao.queryBuilder()
                .where(ChatEntityDao.Properties.SendStatus.eq(ChatEntity.SEND_STAT_QUEUE))
                .list();
        if (!list.isEmpty()) {
            for (ChatEntity one : list) {
                one.setSendStatus(ChatEntity.SEND_STAT_FAILED);
            }
            dao.updateInTx(list);
        }
    }

    private void onNotifyMicroChat(GeneratedMessageV3 generatedMessageV3) {
        if (TextUtils.isEmpty(mUserId)) {
            L.e(TAG, "onMicroChatVoiceFile: mUserId is empty");
            return;
        }
        GreenUtils.saveChatMessage(mUserId, generatedMessageV3);
        PreferencesWrapper preferencesWrapper = PreferencesWrapper.getInstance(this);
        int notifyStatus = preferencesWrapper.getNotificationConf();
        try {
            if (!BaseActivity.isAppRunOnForeground()) {
                String tipMsg = getString(R.string.you_have_new_micro_message);
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this, CHANNEL_ID_CHAT)
                                .setSmallIcon(R.drawable.app_icon)
                                .setContentTitle(getString(R.string.new_micro_chat_message))
                                .setDefaults(notifyStatus) //使用默认的声音、振动、闪光
                                .setContentText(tipMsg);
                // Creates an explicit intent for an Activity in your app
                Intent intent = new Intent(MainService.this, MainActivity.class);
                intent.putExtra(BindRequestActivity.PARAM_KEY_TO_USER_ID, mUserId);
                mBuilder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), new Random().nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT));
                mBuilder.setAutoCancel(true);
                mNotificationManagerCompat.notify(getNotificationID(), mBuilder.build());
            } else {
                if ((notifyStatus & Notification.DEFAULT_SOUND) != 0) {
                    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone rt = RingtoneManager.getRingtone(this, uri);
                    rt.play();
                }
                if ((notifyStatus & Notification.DEFAULT_VIBRATE) != 0) {
                    Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {100, 200, 0, 0}; // 停止 开启 停止 开启
                    if (vibrator != null) {
                        vibrator.vibrate(pattern, -1); //重复两次上面的pattern 如果只想震动一次，index设为-1
                    }
                }
            }
        } catch (Exception e) {
            L.e(TAG, "process TAG_ON_NEW_FILE error", e);
        }
    }


    @Override
    public void onConnected(TlcService tlcService, boolean isSticky) {
    }

    @Override
    public void onDisconnected(TlcService tlcService, boolean isSticky) {
    }

    @Override
    public void onLoggedin(TlcService tlcService, @NonNull String userId, boolean isSticky) {
        mUserId = userId;
    }

    @Override
    public void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky) {
    }

    @Override
    public void onBindDeviceRequest(TlcService tlcService, @NonNull final Pkt reqPkt, @NonNull final Message.NotifyAdminBindDevReqMsg reqMsg) {
         final Message.UsrDevAssoc uda = reqMsg.getUsrDevAssoc();
        if (uda == null || TextUtils.isEmpty(uda.getUserId()) || TextUtils.isEmpty(uda.getDeviceId()))
            return;
        //获取设备信息
        final String currentUserId = mUserId;
        if (TextUtils.isEmpty(currentUserId)) {
            L.e(TAG, "onBindDeviceRequest: currentUserId = mUserId is empty");
            return;
        }
        getUserInfo(uda.getUserId(), reqMsg.getUsrDevAssoc().getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {

                PreferencesWrapper preferencesWrapper = PreferencesWrapper.getInstance(MainService.this);
                int notifyStatus = preferencesWrapper.getNotificationConf();
                //保存消息
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, currentUserId, reqPkt);
                Message.UserInfo userInfo = fetchUsrDevParticRspMsg.getUserInfo();
                if (!BaseActivity.isAppRunOnForeground() /*isBackground(MainService.this)*/) {
                    String tipMsg = getString(R.string.notify_who_request_bind_which_device,
                            RelationUtils.decodeRelation(MainService.this, uda.getRelation()),
                            (TextUtils.isEmpty(fetchUsrDevParticRspMsg.getBaby().getName())
                                    ? getString(R.string.baby)
                                    : fetchUsrDevParticRspMsg.getBaby().getName()));
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(MainService.this, CHANNEL_ID_BIND_REQUEST)
                                    .setSmallIcon(R.drawable.contact_device_bound_icon)
                                    .setContentTitle(getText(R.string.contact_Request_binding_device))
                                    .setDefaults(notifyStatus)
                                    .setContentText(tipMsg);
                    // Creates an explicit intent for an Activity in your app
                    Intent intent = new Intent(MainService.this, BindRequestActivity.class);
                    intent.putExtra(BindRequestActivity.PARAM_KEY_BINDREQ_SEQ, reqPkt.seq.toString());

                    intent.putExtra(BindRequestActivity.PARAM_KEY_BINDREQ_PROTOBUF_MSG, reqMsg);
                    intent.putExtra(BindRequestActivity.PARAM_KEY_FROM_USER_INFO_PROTOBUF_MSG, fetchUsrDevParticRspMsg);
                    intent.putExtra(BindRequestActivity.PARAM_KEY_TO_USER_ID, currentUserId);
                    mBuilder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), new Random().nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT));
                    mBuilder.setAutoCancel(true);
                    mNotificationManagerCompat.notify(getNotificationID(), mBuilder.build());
                } else if (mOnEventListener != null) {
                    mOnEventListener.onBindRequest(currentUserId, reqPkt, reqMsg, fetchUsrDevParticRspMsg);

                    if ((notifyStatus & Notification.DEFAULT_SOUND) != 0) {
                        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone rt = RingtoneManager.getRingtone(MainService.this, uri);
                        rt.play();
                    }
                    if ((notifyStatus & Notification.DEFAULT_VIBRATE) != 0) {
                        Vibrator vibrator = (Vibrator) MainService.this.getSystemService(Context.VIBRATOR_SERVICE);
                        long[] pattern = {100, 200, 0, 0}; // 停止 开启 停止 开启
                        if (vibrator != null) {
                            vibrator.vibrate(pattern, -1); //重复两次上面的pattern 如果只想震动一次，index设为-1
                        }
                    }

                }
            }

            @Override
            public void onFailure() {

            }
        });
    }

    interface OnGetUserInfoListener {
        void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg);

        void onFailure();
    }

    private void getUserInfo(String userId, String devId, final TlcService tlcService, final OnGetUserInfoListener listener) {
        if (tlcService == null) {
            L.w(TAG, "getUserInfo: tlcService == null");
            return;
        }
        if (userId == null)
            userId = "";
        if (devId == null)
            devId = "";
        final Message.FetchUsrDevParticReqMsg fetchUserInfoReqMsg = Message.FetchUsrDevParticReqMsg.newBuilder()
                .setUserId(userId)
                .setDeviceId(devId)
                .build();
        L.d(TAG, "getUserInfo: " + fetchUserInfoReqMsg);

        tlcService.exec(
                fetchUserInfoReqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.FetchUsrDevParticRspMsg rspMsg = response.getProtoBufMsg();
                            if (rspMsg.getErrCode() != Message.ErrorCode.SUCCESS) {
                                L.w(TAG, "onBindDeviceRequest() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                                if (listener != null) {
                                    listener.onFailure();
                                }
                            } else {
                                if (listener != null) {
                                    listener.onSuccess(rspMsg);
                                }
                            }
                        } catch (Exception e) {
                            L.e(TAG, "onBindDeviceRequest() -> exec() -> onResponse() process failure", e);
                        }
                        if (listener != null) {
                            listener.onFailure();
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "onBindDeviceRequest() -> exec() -> onException()", cause);
                        if (listener != null) {
                            listener.onFailure();
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }

        );
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull final Message.NotifyUserBindDevReqMsg reqMsg) {
        try {
            protocol.Message.UsrDevAssoc usrDevAssoc = reqMsg.getUsrDevAssoc();
            // 绑定
            GreenUtils.saveUsrDevAssoc(usrDevAssoc);

            // 缓存设备信息
            DeviceInfo.cache(usrDevAssoc.getDeviceId());

            //添加绑定消息
            final String userId = mUserId;
            getUserInfo(reqMsg.getBinder(), reqMsg.getUsrDevAssoc().getDeviceId(), tlcService, new OnGetUserInfoListener() {
                @Override
                public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                    MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, reqPkt);
                }

                @Override
                public void onFailure() {

                }
            });
            if (TextUtils.isEmpty(usrDevAssoc.getUserId()) || TextUtils.isEmpty(usrDevAssoc.getDeviceId())) {
                L.w(TAG, "onDeviceBind: NotifyUsrBindDevResultReqMsg: .getUsrDevAssoc() data invalid");
            }
        } catch (Exception e) {
            L.e(TAG, "onDeviceBind() process failure", e);
        }
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull final Message.NotifyUserUnbindDevReqMsg reqMsg) {
        boolean record = true;
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId)) {
            // 被解绑的是当前用户的设备，清除设备相关的信息
            GreenUtils.clearDeviceWhenUnbind(reqMsg.getUsrDevAssoc().getDeviceId(), reqMsg.getUsrDevAssoc().getUserId(), reqMsg.getClearLevel());
            if ((reqMsg.getClearLevel() & Message.UnbindClearFlag.UBCF_NOTIFI_MESSAGE_VALUE) != 0) {
                record = false;
            }
        } else {
            // 被解绑的不是当前用户的设备，只删除联系人
            GreenUtils.deleteContact(reqMsg.getUsrDevAssoc().getDeviceId(), reqMsg.getUsrDevAssoc().getUserId());
        }

        if (record) {
            try {
                //添加设备解绑通知
                final String userId = mUserId;
                getUserInfo(reqMsg.getUnbinder(), reqMsg.getUsrDevAssoc().getDeviceId(), tlcService, new OnGetUserInfoListener() {
                    @Override
                    public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticReqMsg) {
                        MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticReqMsg, reqMsg, userId, reqPkt);
                    }

                    @Override
                    public void onFailure() {
                    }
                });
            } catch (Exception e) {
                L.e(TAG, "onDeviceUnbind() process failure", e);
            }
        }
    }

    @Override
    public void onDevConfChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull final Message.NotifyDevConfChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveConfigs(reqMsg.getConf(), reqMsg.getFlag(), reqMsg.getDeviceId());

        // 写入通知中心
        final String userId = mUserId;
        getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, reqPkt);
            }

            @Override
            public void onFailure() {

            }
        });
    }

    @Override
    public void onDevConfSynced(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfSyncedReqMsg reqMsg) {
    }

    /* 围栏触发消息 */
    @Override
    public void onDevFenceChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull final Message.NotifyFenceChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveFence(reqMsg.getDeviceId(), reqMsg.getDetail());

        try {
            final String userId = mUserId;
            getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
                @Override
                public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                    MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, pkt);
                }

                @Override
                public void onFailure() {

                }
            });
            if (TextUtils.isEmpty(mUserId) || TextUtils.isEmpty(reqMsg.getDeviceId())) {
                L.w(TAG, "onDevFenceChanged() getUserInfo(): NotifyUsrBindDevResultReqMsg: .getUsrDevAssoc() data invalid");
            }
        } catch (Exception e) {
            L.e(TAG, "onDevFenceChanged() getUserInfo(): process failure", e);
        }
    }

    @Override
    public void onDevSosChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull final Message.NotifySosChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveSos(reqMsg.getDeviceId(), reqMsg.getDetail());

        // 写入通知中心
        final String userId = mUserId;
        getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, pkt);
            }

            @Override
            public void onFailure() {
            }
        });
    }

    @Override
    public void onNotifySosCallOrderChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySosCallOrderChangedReqMsg reqMsg) {

    }

    @Override
    public void onDevSosSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifySosSyncedReqMsg reqMsg) {
        L.v(TAG, "onDevSosSynced ");
        List<String> list = reqMsg.getSosIdList();
        String devId = reqMsg.getDeviceId();
        GreenUtils.updateSosSyncState(devId, list);
    }

    @Override
    public void onDevContactChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull final Message.NotifyContactChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveContact(reqMsg.getDeviceId(), reqMsg.getDetail());

        // 写入通知中心
        final String userId = mUserId;
        getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, pkt);
            }

            @Override
            public void onFailure() {
            }
        });
    }

    @Override
    public void onDevContactSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyContactSyncedReqMsg reqMsg) {
    }

    @Override
    public void onAlarmClockChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull final Message.NotifyAlarmClockChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveAlarmClock(reqMsg.getDeviceId(), reqMsg.getDetail());

        // 写入通知中心
        final String userId = mUserId;
        getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, pkt);
            }

            @Override
            public void onFailure() {
            }
        });
    }

    @Override
    public void onAlarmClockSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyAlarmClockSyncedReqMsg reqMsg) {
        List<String> list = reqMsg.getAlarmClockIdList();
        String devId = reqMsg.getDeviceId();
        GreenUtils.updateAlarmClockSyncState(devId, list);
    }

    @Override
    public void onClassDisableChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull final Message.NotifyClassDisableChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveClassDisable(reqMsg.getDeviceId(), reqMsg.getDetail());

        // 写入通知中心
        final String userId = mUserId;
        getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, pkt);
            }

            @Override
            public void onFailure() {
            }
        });
    }

    @Override
    public void onClassDisableSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyClassDisableSyncedReqMsg reqMsg) {
        List<String> list = reqMsg.getClassDisableIdList();
        String devId = reqMsg.getDeviceId();
        GreenUtils.updateClassDisableSyncState(devId, list);
    }

    @Override
    public void onSchoolGuardChanged(TlcService tlcService, @NonNull Pkt pkt, @NonNull final Message.NotifySchoolGuardChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveSchoolGuard(reqMsg.getDeviceId(), reqMsg.getDetail().getGuard());

        // 写入通知中心
        final String userId = mUserId;
        getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, pkt);
            }

            @Override
            public void onFailure() {
            }
        });
    }

    @Override
    public void onPraiseChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull final Message.NotifyPraiseChangedReqMsg reqMsg) {
        // 保存信息
        GreenUtils.saveCollectPraise(reqMsg.getDeviceId(), reqMsg.getDetail());

        // 写入通知中心
        final String userId = mUserId;
        getUserInfo(reqMsg.getChanger(), reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, pkt);
            }

            @Override
            public void onFailure() {
            }
        });
    }

    @Override
    public void onLocateS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.LocateS3ReqMsg reqMsg) {
        List<DeviceEntity> list = GreenUtils.getDeviceEntityDao().queryBuilder().where(DeviceEntityDao.Properties.DeviceId.eq(reqMsg.getDeviceId())).list();
        if (list.isEmpty() || list.get(0).getLastPosition().getTime() < reqMsg.getPosition().getTime()) {
            GreenUtils.saveDeviceLastPosition(reqMsg.getDeviceId(), reqMsg.getPosition());
        }
    }

    @Override
    public void onDevicePosition(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevicePositionReqMsg reqMsg) {
        GreenUtils.saveDeviceLastPosition(reqMsg.getDeviceId(), reqMsg.getPosition());
    }

    @Override
    public void onLocationModeChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyLocationModeChangedReqMsg reqMsg) {
        GreenUtils.upDataLocationMode(reqMsg.getLocationMode(), reqMsg.getDeviceId());

        String userId = mUserId;
        if (TextUtils.isEmpty(userId))
            return;
        getUserInfo(userId, reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
            @Override
            public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, reqPkt);
            }

            @Override
            public void onFailure() {

            }
        });
    }

    @Override
    public void onDeviceIncident(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull final Message.NotifyIncidentReqMsg reqMsg) {
        try {
            L.v(TAG, "onDeviceIncident: " + reqMsg);
            final String userId = mUserId;
            if (TextUtils.isEmpty(mUserId)) {
                L.w(TAG, "onDeviceIncident: mUserId is empty");
            } else {
                getUserInfo(userId, reqMsg.getDeviceId(), tlcService, new OnGetUserInfoListener() {
                    @Override
                    public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                        MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, userId, reqPkt);
                    }

                    @Override
                    public void onFailure() {
                    }
                });
            }
        } catch (Exception e) {
            L.e(TAG, "onDeviceIncident() process failure", e);
        }
    }

    @Override
    public void onUsrDevAssocModified(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull final Message.NotifyUsrDevAssocModifiedReqMsg reqMsg) {
        final Message.UsrDevAssoc uda = reqMsg.getUsrDevAssoc();

        // 保存信息
        BabyEntityDao dao = GreenUtils.getBabyEntityDao();
        List<BabyEntity> list = dao.queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(uda.getUserId()))
                .where(BabyEntityDao.Properties.DeviceId.eq(uda.getDeviceId()))
                .build().list();
        if (!list.isEmpty()) {
            for (BabyEntity one : list) {
                one.setPermission(uda.getPermissionValue());
                one.setRelation(uda.getRelation());

                if (!TextUtils.isEmpty(uda.getAvatar())) {
                    one.setUserAvatar(uda.getAvatar());
                }
            }
            dao.updateInTx(list);
        }

        if (!TextUtils.isEmpty(reqMsg.getChanger())) {
            // 写入通知中心
            getUserInfo(reqMsg.getChanger(), uda.getDeviceId(), tlcService, new OnGetUserInfoListener() {
                @Override
                public void onSuccess(Message.FetchUsrDevParticRspMsg fetchUsrDevParticRspMsg) {
                    MessageCenterUtils.saveNotifyMessage(MainService.this, fetchUsrDevParticRspMsg, reqMsg, uda.getUserId(), reqPkt);
                }

                @Override
                public void onFailure() {

                }
            });
        }
    }

    @Override
    public void onFindDeviceS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.FindDeviceS3ReqMsg reqMsg) {
    }

    @Override
    public void onTakePhotoS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.TakePhotoS3ReqMsg reqMsg) {
        // FIXME: 保存图片
    }

    @Override
    public void onSimplexCallS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.SimplexCallS3ReqMsg reqMsg) {
    }

    @Override
    public void onFetchDevStatusInfoS3(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.FetchDeviceSensorDataS3ReqMsg reqMsg) {
    }

    @Override
    public void onDeviceSensorData(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDeviceSensorDataReqMsg reqMsg) {
        GreenUtils.saveDeviceLastSensorData(reqMsg.getDeviceId(), reqMsg.getData(), reqMsg.getTime());
    }

    @Override
    public void onNewMicroChatMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatMessageReqMsg reqMsg) {
        onNotifyMicroChat(reqMsg);
    }

    @Override
    public void onNewMicroChatGroupMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyGroupChatMessageReqMsg reqMsg) {
        onNotifyMicroChat(reqMsg);
    }

    @Override
    public void onNotifySMSAgentNewSMS(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySMSAgentNewSMSReqMsg reqMsg) {
//        GreenUtils.upDataSmsAgentStatus(reqMsg.getEnabled(), reqMsg.getDeviceId());
        String deiceId = reqMsg.getDeviceId();
        GreenUtils.saveSmsEntity(reqMsg.getSms(), deiceId, mUserId);
    }

    @Override
    public void onDeviceFriendChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyFriendChangedReqMsg reqMsg) {
        GreenUtils.saveFriendContact(reqMsg.getDeviceId(), reqMsg.getDetail());
    }

    @Override
    public void onChatGroupMemberChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatGroupMemberChangedReqMsg reqMsg) {
        String group_id = reqMsg.getGroupId();
        for (Message.NotifyChatGroupMemberChangedReqMsg.Detail detail : reqMsg.getDetailList()) {
            Message.ChatGroup.Member member = detail.getMember();
            switch (detail.getAction()) {
                case ADD:
                    GreenUtils.addFamilyChatGroupMember(member, group_id);
                    break;
                case DEL:
                    GreenUtils.deleteFamilyChatGroupMember(member, group_id);
                    break;
                case MODIFY:
                    GreenUtils.modifyFamilyChatGroupMember(member, group_id);
                    break;
                case NONE:
                    break;
            }
        }
    }

    @Override
    public void onNotProcessedPkt(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull OnResponseSetter responseSetter) {
        responseSetter.setResponse(null);
    }

    public static boolean isBackground(Context context) {
        String packageName = context.getPackageName();
        boolean foreground = getRunningTask(context, packageName);
        L.v(TAG, "isBackground: " + packageName + "处于" + (foreground ? "前台" : "后台"));
        return !foreground;
    }

    /**
     * 通过getRunningTasks判断App是否位于前台，此方法在5.0以上失效。不过判断自身是没问题的
     *
     * @param context     上下文参数
     * @param packageName 需要检查是否位于栈顶的App的包名
     * @return
     */
    public static boolean getRunningTask(Context context, String packageName) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
            return !TextUtils.isEmpty(packageName) && packageName.equals(cn.getPackageName());
        } catch (Exception e) {
            L.d(TAG, "getRunningTask", e);
            return false;
        }
    }
}
