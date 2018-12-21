package com.cqkct.FunKidII.service.tlc;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseArray;

import com.cqkct.FunKidII.BuildConfig;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.Utils.Installation;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.Rom;
import com.cqkct.FunKidII.service.Pkt;
import com.google.protobuf.GeneratedMessageV3;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import cn.jpush.android.api.JPushInterface;

public class TlcService extends Service {
    private static final String TAG = TlcService.class.getSimpleName();

    private final TlcServiceBinder mBinder = new TlcServiceBinder();
    private BroadcastReceiver deviceStateReceiver;

    private PreferencesWrapper mPreferencesWrapper;
    private PowerManager mPowerManager;
    private TlcWakeLock mTlcWakeLock;
    ServiceHandler mHandler;
    private TLC mTLC;
    private OnEventListener mOnEventListener;

    private int TLC_EXEC_TIMEOUT_DEF = 1000 * 10;

    private boolean loggedin = false;
    private String jPushRegistrationID;

    @Override
    public void onCreate() {
        L.i(TAG, "onCreate");
        super.onCreate();

        mTlcWakeLock = new TlcWakeLock(getPowerManager());
        mHandler = new ServiceHandler(this);
        mTLC = new TLC(this);
//         mTLC = new TLC(this, "kid.cqkct.top", false);//开发环境

        registerConnectivityBroadcasts();
    }

    @Override
    public void onDestroy() {
        L.i(TAG, "onDestroy");

        unregisterConnectivityBroadcasts();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        L.i(TAG, "onBind");
        return mBinder;
    }

    private void registerConnectivityBroadcasts() {
        if (deviceStateReceiver != null)
            return;

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        deviceStateReceiver = new BroadcastReceiver() {
            private String mNetworkType;
            private boolean mConnected = false;
            private String mRoutes = "";

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

                    boolean connected = (info != null && info.isConnected() && isConnectivityValid());
                    String networkType = connected ? info.getTypeName() : "null";

                    String currentRoutes = dumpRoutes();
                    // Ignore the event if the current active network is not changed.
                    if (connected == mConnected && networkType.equals(mNetworkType) && currentRoutes.equals(mRoutes)) {
                        return;
                    }
                    if (!networkType.equals(mNetworkType)) {
                        L.d(TAG, "onConnectivityChanged(): " + mNetworkType + " -> " + networkType);
                        mNetworkType = networkType;
                    } else {
                        L.d(TAG, "Route changed : " + mRoutes + " -> " + currentRoutes);
                    }
                    mRoutes = currentRoutes;
                    mConnected = connected;

                    if (connected) {
                        L.i(TAG, "connectivity valid");
                        checkGoogleAccessibility();
                        resetReconnectDelay(0);
                        reconnectTlc(0);
                    } else {
                        L.i(TAG, "connectivity invalid");
                        mHandler.sendEmptyMessage(ServiceHandler.ON_CONNECTIVITY_INVALID);
                        mTLC.disconnect();
                    }
                }
            }

            private String dumpRoutes() {
                String routes = "";
                FileReader fr = null;
                try {
                    fr = new FileReader("/proc/net/route");
                    StringBuilder contentBuf = new StringBuilder();
                    BufferedReader buf = new BufferedReader(fr);
                    String line;
                    while ((line = buf.readLine()) != null) {
                        contentBuf.append(line).append("\n");
                    }
                    routes = contentBuf.toString();
                    buf.close();
                } catch (FileNotFoundException e) {
                    L.e(TAG, "No route file found routes", e);
                } catch (IOException e) {
                    L.e(TAG, "Unable to read route file", e);
                } finally {
                    if (fr != null) {
                        try {
                            fr.close();
                        } catch (IOException e) {
                            L.e(TAG, "Unable to close route file", e);
                        }
                    }
                }

                String finalRoutes = routes;
                if (!TextUtils.isEmpty(routes)) {
                    String[] items = routes.split("\n");
                    List<String> finalItems = new ArrayList<>();
                    int line = 0;
                    for (String item : items) {
                        boolean addItem = true;
                        if (line > 0) {
                            String[] ent = item.split("\t");
                            if (ent.length > 8) {
                                String maskStr = ent[7];
                                if (maskStr.matches("^[0-9A-Fa-f]{8}$")) {
                                    int lastMaskPart = Integer.parseInt(maskStr.substring(0, 2), 16);
                                    if (lastMaskPart > 192) {
                                        // if more than 255.255.255.192 : ignore this line
                                        addItem = false;
                                    }
                                } else {
                                    L.w(TAG, "The route mask does not looks like a mask" + maskStr);
                                }
                            }
                        }

                        if (addItem) {
                            finalItems.add(item);
                        }
                        line++;
                    }
                    finalRoutes = TextUtils.join("\n", finalItems);
                }

                return finalRoutes;
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
            synchronized (CheckGoogleAccessibilityThread.class) {
                mCheckGoogleAccessibilityThread = thread;
            }
            thread.start();
        } catch (Exception e) {
            L.e(TAG, "checkGoogleAccessibility", e);
            EventBus.getDefault().postSticky(Event.GoogleAccessibility.UNKNOWN);
        }
    }
    private CheckGoogleAccessibilityThread mCheckGoogleAccessibilityThread;
    private class CheckGoogleAccessibilityThread extends Thread {
        @Override
        public void run() {
            try {
                URL url = new URL("https://www.google.com/");
                URLConnection urlConn = url.openConnection();
                urlConn.setConnectTimeout(3000);
                urlConn.connect();
                L.i(TAG, "CheckGoogleAccessibilityThread.run OK: ACCESSIBLE");
                synchronized (CheckGoogleAccessibilityThread.class) {
                    if (CheckGoogleAccessibilityThread.this == mCheckGoogleAccessibilityThread) {
                        EventBus.getDefault().postSticky(Event.GoogleAccessibility.ACCESSIBLE);
                    }
                }
            } catch (IOException e) {
                L.i(TAG, "CheckGoogleAccessibilityThread.run: " + e.getMessage());
                synchronized (CheckGoogleAccessibilityThread.class) {
                    if (CheckGoogleAccessibilityThread.this == mCheckGoogleAccessibilityThread) {
                        EventBus.getDefault().postSticky(Event.GoogleAccessibility.INACCESSIBLE);
                    }
                }
            } catch (Exception e) {
                L.i(TAG, "CheckGoogleAccessibilityThread.run: " + e.getMessage());
                synchronized (CheckGoogleAccessibilityThread.class) {
                    if (CheckGoogleAccessibilityThread.this == mCheckGoogleAccessibilityThread) {
                        EventBus.getDefault().postSticky(Event.GoogleAccessibility.UNKNOWN);
                    }
                }
            }
        }
    }

    public void setOnTlcEventListener(OnEventListener listener) {
        mOnEventListener = listener;
    }


    // 用户注册

    /**
     * 查询用户是否已注册
     */
    public interface OnCheckUserCanRegisterListener {
        int TIMEOUT = -2;
        int NETWORK_ERROR = -1;
        int EXISTS = 0;
        int NOT_EXISTS = 1;
        int FAILURE = 2;

        void onStatus(protocol.Message.CheckUserRspMsg rspMsg, int status);
    }

    /**
     * 查询用户是否已注册
     *
     * @param username     用户名
     * @param userNameType 用户类型（手机号或邮箱）
     * @param listener     OnCheckUserCanRegisterListener
     * @return 执行实体
     */
    public ExecEntity checkUserExists(@NonNull String username, @NonNull protocol.Message.UserNameType userNameType, @Nullable final OnCheckUserCanRegisterListener listener) {
        String pktSrcAddr = getPreferencesWrapper().getUserId();
        if (TextUtils.isEmpty(pktSrcAddr))
            pktSrcAddr = username;
        protocol.Message.CheckUserReqMsg reqMsg = protocol.Message.CheckUserReqMsg.newBuilder()
                .setType(userNameType)
                .setName(username)
                .build();
        return exec(pktSrcAddr, reqMsg, TLC_EXEC_TIMEOUT_DEF, true,
                new OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        L.d(TAG, "checkUserExists() onResponse() Pkt: " + response);
                        try {
                            protocol.Message.CheckUserRspMsg rspMsg = response.getProtoBufMsg();
                            if (listener != null) {
                                switch (rspMsg.getErrCode()) {
                                    case ALREADY_EXISTS: {
//                                        String userId = rspMsg.getUserId();
//                                        if (TextUtils.isEmpty(mTLC.getLocalPktAddr())) {
//                                            mTLC.setLocalPktAddr(userId);
//                                        }
                                        listener.onStatus(rspMsg, listener.EXISTS);
                                    }
                                    break;
                                    case NOT_EXISTS:
                                        listener.onStatus(rspMsg, listener.NOT_EXISTS);
                                        break;
                                    default:
                                        listener.onStatus(rspMsg, listener.FAILURE);
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            L.e(TAG, "checkUserExists() onResponse() is not " + protocol.Message.CheckUserRspMsg.class.getSimpleName(), e);
                            if (listener != null)
                                listener.onStatus(null, listener.FAILURE);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "checkUserExists() onException()", cause);
                        if (cause instanceof TimeoutException) {
                            if (listener != null)
                                listener.onStatus(null, listener.TIMEOUT);
                        } else {
                            if (listener != null)
                                listener.onStatus(null, listener.NETWORK_ERROR);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                });
    }

    public interface OnQueryThirdAccountByPhoneListener {
        int TIMEOUT = -2;
        int NETWORK_ERROR = -1;
        int SUCCESS = 0;
        int NO_USER = 1;
        int FAILURE = 2;

        void onStatus(protocol.Message.QueryThirdAccountByPhoneRspMsg rspMsg, int status);
    }

    public ExecEntity queryThirdAccountByPhone(@NonNull String phone, @NonNull protocol.Message.UserNameType thridPlat, @Nullable final OnQueryThirdAccountByPhoneListener listener) {
        String pktSrcAddr =getPreferencesWrapper().getUserId();
        if (TextUtils.isEmpty(pktSrcAddr))
            pktSrcAddr = phone;
        protocol.Message.QueryThirdAccountByPhoneReqMsg reqMsg = protocol.Message.QueryThirdAccountByPhoneReqMsg.newBuilder()
                .setPhone(phone)
                .setPlat(thridPlat)
                .build();
        return exec(pktSrcAddr, reqMsg, TLC_EXEC_TIMEOUT_DEF, true,
                new OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        L.d(TAG, "queryThirdAccountByPhone() onResponse() Pkt: " + response);
                        try {
                            protocol.Message.QueryThirdAccountByPhoneRspMsg rspMsg = response.getProtoBufMsg();
                            if (listener != null) {
                                switch (rspMsg.getErrCode()) {
                                    case SUCCESS:
                                        listener.onStatus(rspMsg, listener.SUCCESS);

                                    break;
                                    case NO_USER:
                                        listener.onStatus(rspMsg, listener.NO_USER);
                                        break;
                                    default:
                                        listener.onStatus(rspMsg, listener.FAILURE);
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            L.e(TAG, "queryThirdAccountByPhone() onResponse() is not " + protocol.Message.CheckUserRspMsg.class.getSimpleName(), e);
                            if (listener != null)
                                listener.onStatus(null, listener.FAILURE);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "queryThirdAccountByPhone() onException()", cause);
                        if (cause instanceof TimeoutException) {
                            if (listener != null)
                                listener.onStatus(null, listener.TIMEOUT);
                        } else {
                            if (listener != null)
                                listener.onStatus(null, listener.NETWORK_ERROR);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                });
    }

    /**
     * 注册事件Listener
     */
    public interface OnRegisterUserListener {
        int TIMEOUT = -2;
        int NETWORK_ERROR = -1;
        int SUCCESS = 0;
        int FAILURE = 1;
        int USER_ALREADY_EXISTS = 2;
        int ALREADY_ASSOC_OTHER = 3;

        void onStatus(protocol.Message.RegisterRspMsg rspMsg, int status);
    }

    /**
     * 注册用户
     *
     * @param username     用户名
     * @param userNameType 用户类型（手机号或邮箱）
     * @param passwd       密码
     * @param listener     OnRegisterUserListener
     * @return 执行实体
     */
    public ExecEntity registerUser(@NonNull String username, @NonNull protocol.Message.UserNameType userNameType, @Nullable String passwd,
                                   @Nullable protocol.Message.OAuthAccountInfo oAuthAccountInfo,
                                   @NonNull final OnRegisterUserListener listener) {
        if (passwd == null)
            passwd = "";
        if (oAuthAccountInfo == null) {
            oAuthAccountInfo = protocol.Message.OAuthAccountInfo.newBuilder().build();
        }
        protocol.Message.RegisterReqMsg reqMsg = protocol.Message.RegisterReqMsg.newBuilder()
                .setType(userNameType)
                .setLoginSign(username)
                .setPasswd(passwd)
                .setOAuthInfo(oAuthAccountInfo)
                .build();

        String pktSrcAddr = getPreferencesWrapper().getUserId();
        if (TextUtils.isEmpty(pktSrcAddr))
            pktSrcAddr = username;

        return exec(pktSrcAddr, reqMsg, TLC_EXEC_TIMEOUT_DEF, true,
                new OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        L.d(TAG, "registerUser() onResponse() Pkt: " + response);
                        try {
                            protocol.Message.RegisterRspMsg rspMsg = response.getProtoBufMsg();
                            if (listener != null) {
                                switch (rspMsg.getErrCode()) {
                                    case SUCCESS:
                                        listener.onStatus(rspMsg, listener.SUCCESS);
                                        break;
                                    case FAILURE:
                                        listener.onStatus(rspMsg, listener.FAILURE);
                                        break;
                                    case ALREADY_EXISTS:
                                        listener.onStatus(rspMsg, listener.USER_ALREADY_EXISTS);
                                        break;
                                    case ALREADY_ASSOC_OTHER:
                                        listener.onStatus(rspMsg, listener.ALREADY_ASSOC_OTHER);
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            L.e(TAG, "registerUser() onResponse() is not " + protocol.Message.RegisterRspMsg.class.getSimpleName(), e);
                            if (listener != null)
                                listener.onStatus(null, listener.FAILURE);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "registerUser() onException()", cause);
                        if (cause instanceof TimeoutException) {
                            if (listener != null)
                                listener.onStatus(null, listener.TIMEOUT);
                        } else {
                            if (listener != null)
                                listener.onStatus(null, listener.NETWORK_ERROR);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                });
    }


    // 用户登录

    /**
     * 登录事件Listener
     */
    public interface OnLoginListener {
        int TIMEOUT = -2;
        int NETWORK_ERROR = -1;
        int SUCCESS = 0;
        int FAILURE = 1;
        int USER_OR_PASSWD_ERROR = 2;
        /** app 与服务器接口不兼容 */
        int NOT_COMPAT = 3;
        int NOT_EXISTS = 4;

        void onStatus(protocol.Message.LoginRspMsg rspMsg, int status);
    }

    private protocol.Message.LoginReqMsg makeLoginReqMsg(@NonNull String username, @NonNull protocol.Message.UserNameType usernameType, @Nullable String passwd, @Nullable String sessionId) {
        String jPushAppKey = null;
        PackageInfo packageInfo = null;
        protocol.Message.PushConf.Type pushConfType = protocol.Message.PushConf.Type.JPush;
        try {
            ApplicationInfo appInfo = this.getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            jPushAppKey = appInfo.metaData.getString("JPUSH_APPKEY");
            packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            L.e(TAG, "makeLoginReqMsg GET_META_DATA", e);
        }
        if (TextUtils.isEmpty(jPushAppKey))
            jPushAppKey = "";
        String jPushRegistrationID = JPushInterface.getRegistrationID(this);
        if (TextUtils.isEmpty(jPushRegistrationID)) {
            jPushRegistrationID = "";
        }
        if (TextUtils.isEmpty(jPushAppKey) || TextUtils.isEmpty(jPushRegistrationID)) {
            pushConfType = protocol.Message.PushConf.Type.NONE;
        }
        this.jPushRegistrationID = jPushRegistrationID;

        if (passwd == null)
            passwd = "";

        return protocol.Message.LoginReqMsg.newBuilder()
                .setType(usernameType)
                .setName(username)
                .setPasswd(passwd)
                .setExpire(60 * 3 + 20)
                .setPushConf(protocol.Message.PushConf.newBuilder()
                        .setType(pushConfType)
                        .setApp(jPushAppKey)
                        .setToken(jPushRegistrationID)
                        .build())
                .setPhoneInfo(protocol.Message.PhoneInfo.newBuilder()
                        .setVender(android.os.Build.BRAND)
                        .setModel(android.os.Build.MODEL)
                        .setOsType(protocol.Message.PhoneInfo.OsType.Android)
                        .setOsName("Android")
                        .setOsNameX(Rom.getName())
                        .setOsVer(android.os.Build.VERSION.RELEASE)
                        .setOsVerX(Rom.getVersion())
                        .setId(Installation.id(this))
                        .build())
                .setVerInfo(protocol.Message.LoginReqMsg.VersionInfo.newBuilder()
                        .setVerCode(packageInfo == null ? BuildConfig.VERSION_CODE : packageInfo.versionCode)
                        .setVerName(packageInfo == null ? BuildConfig.VERSION_NAME : packageInfo.versionName)
                        .setVerNameInternal(BuildConfig.versionNameInternal))
                .setSessionId(TextUtils.isEmpty(sessionId) ? "" : sessionId)
                .build();
    }

    /**
     * 用户登录
     *
     * @param username     用户名
     * @param usernameType 用户名类型
     * @param passwd       密码
     * @param listener     OnLoginListener
     * @return 执行实体
     */
    public void login(@NonNull String username, @NonNull protocol.Message.UserNameType usernameType, @Nullable String passwd, @Nullable final OnLoginListener listener) {
        synchronized (mBinder) {
            if (TextUtils.isEmpty(getPreferencesWrapper().getUsername()) || !getPreferencesWrapper().getUsername().equals(username)) {
                getPreferencesWrapper().setUsername(username);
            }
            if (getPreferencesWrapper().getUsernameType() == null || getPreferencesWrapper().getUsernameType() != usernameType) {
                getPreferencesWrapper().setUsernameType(usernameType);
            }
            if (usernameType.getNumber() < protocol.Message.UserNameType.USR_NAM_TYP_3RD_QQ_VALUE) {
                // 需要密码登录
                String oldPwd = getPreferencesWrapper().getPasswd();
                if (oldPwd == null || !oldPwd.equals(passwd)) {
                    getPreferencesWrapper().setPasswd(passwd);
                }
            }
            getPreferencesWrapper().setUserId("");
            getPreferencesWrapper().setLoginSessionId("");
            getPreferencesWrapper().setCanAutoLogin(false);
        }

        if (!isConnectivityValid()) {
            L.w(TAG, "login failure: !isConnectivityValid()");
            if (listener != null) {
                listener.onStatus(null, OnLoginListener.NETWORK_ERROR);
            }
            return;
        }

        long timeoutMillis = TLC_EXEC_TIMEOUT_DEF;
        String jPushRegistrationID = JPushInterface.getRegistrationID(this);
        if (TextUtils.isEmpty(jPushRegistrationID)) {
            L.i(TAG, "login warn: jPushRegistrationID is empty");
        }
        login(timeoutMillis, username, usernameType, passwd, listener);
    }

    private void login(long timeoutMillis, @NonNull String username, @NonNull protocol.Message.UserNameType usernameType, @Nullable String passwd, @Nullable final OnLoginListener listener) {
        if (timeoutMillis <= 0) {
            L.w(TAG, "login failure: timeoutMillis <= 0");
            if (listener != null) {
                listener.onStatus(null, OnLoginListener.TIMEOUT);
            }
            return;
        }
        protocol.Message.LoginReqMsg reqMsg = makeLoginReqMsg(username, usernameType, passwd, null);
        if (TextUtils.isEmpty(reqMsg.getPushConf().getApp()) || TextUtils.isEmpty(reqMsg.getPushConf().getToken())) {
            L.i(TAG, "login failure: JPUSH_APPKEY: " + reqMsg.getPushConf().getApp() + ", JPushRegistrationID: " + reqMsg.getPushConf().getToken());
        }

        if (true) {
            exec(username, reqMsg, TLC_EXEC_TIMEOUT_DEF, true, new OnExecListener() {
                @Override
                public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                    L.d(TAG, "login() onResponse() Pkt: " + response);
                    try {
                        protocol.Message.LoginRspMsg rspMsg = response.getProtoBufMsg();
                        if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                            String userId = rspMsg.getUserId();
                            mTLC.setLocalPktAddr(userId);
                            mTLC.setIsLoggedIn(true);
                            // 启动心跳定时器
                            mTLC.enableHeartbeat(rspMsg.getExpire());
                            // 下次启动app时，可以自动登录
                            synchronized (mBinder) {
                                getPreferencesWrapper().setUserId(userId);
                                getPreferencesWrapper().setLoginSessionId(rspMsg.getSessionId());
                                getPreferencesWrapper().setCanAutoLogin(true);
                            }
                            mHandler.obtainMessage(ServiceHandler.ON_LOGGEDIN, userId).sendToTarget();
                            JPushInterface.resumePush(TlcService.this);
                        }
                        if (listener != null) {
                            switch (rspMsg.getErrCode()) {
                                case USER_OR_PASSWD_WRONG:
                                    listener.onStatus(rspMsg, OnLoginListener.USER_OR_PASSWD_ERROR);
                                    break;
                                case SUCCESS:
                                    listener.onStatus(rspMsg, OnLoginListener.SUCCESS);
                                    break;
                                case NO_USER:
                                    listener.onStatus(rspMsg, OnLoginListener.USER_OR_PASSWD_ERROR);
                                    break;
                                case NOT_COMPAT:
                                    listener.onStatus(rspMsg, OnLoginListener.NOT_COMPAT);
                                    break;
                                case NOT_EXISTS:
                                    listener.onStatus(rspMsg, OnLoginListener.NOT_EXISTS);
                                    break;
                                default:
                                    listener.onStatus(rspMsg, OnLoginListener.FAILURE);
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        L.e(TAG, "login() onResponse() is not " + protocol.Message.LoginRspMsg.class.getSimpleName(), e);
                        if (listener != null)
                            listener.onStatus(null, OnLoginListener.FAILURE);
                    }

                    return false;
                }

                @Override
                public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                    L.e(TAG, "login() onException()", cause);
                    if (cause instanceof TimeoutException) {
                        if (listener != null)
                            listener.onStatus(null, OnLoginListener.TIMEOUT);
                    } else {
                        if (listener != null)
                            listener.onStatus(null, OnLoginListener.NETWORK_ERROR);
                    }
                }

                @Override
                public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    // never to here for this exec
                }
            });

        } else {
            Pkt reqPkt = Pkt.newBuilder()
                    .setSrcAddr(username)
                    .setValue(reqMsg)
                    .build();
            ExecEntity execEntity = new ExecEntity(reqPkt, timeoutMillis,
                    new OnExecListener() {
                        @Override
                        public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                            L.d(TAG, "login() onResponse() Pkt: " + response);
                            try {
                                protocol.Message.LoginRspMsg rspMsg = response.getProtoBufMsg();
                                if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                                    String userId = rspMsg.getUserId();
                                    mTLC.setLocalPktAddr(userId);
                                    mTLC.setIsLoggedIn(true);
                                    // 启动心跳定时器
                                    mTLC.enableHeartbeat(rspMsg.getExpire());
                                    // 下次启动app时，可以自动登录
                                    synchronized (mBinder) {
                                        getPreferencesWrapper().setUserId(userId);
                                        getPreferencesWrapper().setLoginSessionId(rspMsg.getSessionId());
                                        getPreferencesWrapper().setCanAutoLogin(true);
                                    }
                                    mHandler.obtainMessage(ServiceHandler.ON_LOGGEDIN, userId).sendToTarget();
                                    JPushInterface.resumePush(TlcService.this);
                                }
                                if (listener != null) {
                                    switch (rspMsg.getErrCode()) {
                                        case USER_OR_PASSWD_WRONG:
                                            listener.onStatus(rspMsg, OnLoginListener.USER_OR_PASSWD_ERROR);
                                            break;
                                        case SUCCESS:
                                            listener.onStatus(rspMsg, OnLoginListener.SUCCESS);
                                            break;
                                        case NO_USER:
                                            listener.onStatus(rspMsg, OnLoginListener.USER_OR_PASSWD_ERROR);
                                            break;
                                        case NOT_COMPAT:
                                            listener.onStatus(rspMsg, OnLoginListener.NOT_COMPAT);
                                            break;
                                        case NOT_EXISTS:
                                            listener.onStatus(rspMsg, OnLoginListener.NOT_EXISTS);
                                            break;
                                        default:
                                            listener.onStatus(rspMsg, OnLoginListener.FAILURE);
                                            break;
                                    }
                                }
                            } catch (Exception e) {
                                L.e(TAG, "login() onResponse() is not " + protocol.Message.LoginRspMsg.class.getSimpleName(), e);
                                if (listener != null)
                                    listener.onStatus(null, OnLoginListener.FAILURE);
                            }

                            return false;
                        }

                        @Override
                        public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                            L.e(TAG, "login() onException()", cause);
                            if (cause instanceof TimeoutException) {
                                if (listener != null)
                                    listener.onStatus(null, OnLoginListener.TIMEOUT);
                            } else {
                                if (listener != null)
                                    listener.onStatus(null, OnLoginListener.NETWORK_ERROR);
                            }
                        }

                        @Override
                        public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                            // never to here for this exec
                        }
                    }, null);

            mTLC.exec(execEntity, true, username);
        }
    }

    private void uploadPushConf() {
        String id = JPushInterface.getRegistrationID(this);
        jPushRegistrationID = id;
        if (TextUtils.isEmpty(id))
            return;
        String userId = getPreferencesWrapper().getUserId();
        if (TextUtils.isEmpty(userId))
            return;
        String jPushAppKey;
        try {
            ApplicationInfo appInfo = this.getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            jPushAppKey = appInfo.metaData.getString("JPUSH_APPKEY");
        } catch (PackageManager.NameNotFoundException e) {
            L.e(TAG, "makeLoginReqMsg GET_META_DATA", e);
            return;
        }
        if (TextUtils.isEmpty(jPushAppKey))
            return;
        protocol.Message.SetAppPushConfReqMsg reqMsg = protocol.Message.SetAppPushConfReqMsg.newBuilder()
                .setUserId(userId)
                .setPushConf(protocol.Message.PushConf.newBuilder()
                        .setType(protocol.Message.PushConf.Type.JPush)
                        .setApp(jPushAppKey)
                        .setToken(id)
                        .build())
                .build();
        exec(reqMsg, new OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    protocol.Message.SetAppPushConfRspMsg rspMsg = response.getProtoBufMsg();
                    L.i(TAG, "SetAppPushConfReqMsg onResponse: " + rspMsg.getErrCode());
                } catch (Exception e) {
                    L.e(TAG, "SetAppPushConfReqMsg onResponse", e);
                }
                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.e(TAG, "SetAppPushConfReqMsg onException", cause);
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

            }
        });
    }

    public void onJPushRegistrationID(String jPushRegistrationID) {
        mHandler.obtainMessage(ServiceHandler.ON_GOT_JPushRegistrationID, jPushRegistrationID).sendToTarget();
    }


    public boolean relogin() {
        synchronized (mBinder) {
            String username = getPreferencesWrapper().getUsername();
            String userid = getPreferencesWrapper().getUserId();
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(userid)) {
                return false;
            }
            if (!getPreferencesWrapper().canAutoLogin()) {
                getPreferencesWrapper().setCanAutoLogin(true);
                reconnectTlc(0);
            }
            return true;
        }
    }

    // 用户登出

    /**
     * 用户退出
     */
    public void logout() {
        L.i(TAG, "logout()");
        synchronized (mBinder) {
            String user = getPreferencesWrapper().getUsername();
            if (TextUtils.isEmpty(user)) {
                return;
            } else {
                JPushInterface.stopPush(this);
//                if (getPreferencesWrapper().canAutoLogin())
                mHandler.obtainMessage(ServiceHandler.ON_LOGGEDOUT, user).sendToTarget();
                getPreferencesWrapper().setCanAutoLogin(false);
                getPreferencesWrapper().setUserId("");
                getPreferencesWrapper().setPasswd("");
                getPreferencesWrapper().setLoginSessionId("");
            }
        }

        mTLC.disconnect();
    }


    // 修改密码

    /**
     * 修改密码事件Listener
     */
    public interface OnChangePasswdListener {
        int TIMEOUT = -2;
        int NETWORK_ERROR = -1;
        int SUCCESS = 0;
        int FAILURE = 1;
        int OLD_PASSWD_ERROR = 2;
        int EAGIN = 3;

        void onStatus(protocol.Message.ChangePwdRspMsg rspMsg, int status);
    }

    /**
     * 修改密码
     *
     * @param oldPwd   旧密码
     * @param newPwd   新密码
     * @param listener 回调
     * @return 执行实体
     */
    public ExecEntity changePasswd(@NonNull String oldPwd, @NonNull final String newPwd, final OnChangePasswdListener listener) {
        if (!loggedin) {
            if (listener != null)
                listener.onStatus(null, OnChangePasswdListener.EAGIN);
            return null; // FIXME: 返回执行实体
        }

        protocol.Message.ChangePwdReqMsg reqMsg = protocol.Message.ChangePwdReqMsg.newBuilder()
                .setUserId(getPreferencesWrapper().getUserId())
                .setOldPwd(oldPwd)
                .setNewPwd(newPwd)
                .build();

        return exec(reqMsg, TLC_EXEC_TIMEOUT_DEF, new OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    protocol.Message.ChangePwdRspMsg rspMsg = response.getProtoBufMsg();
                    L.v(TAG, "changePasswd() onResponse() rspMsg: " + rspMsg);
                    if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                        // 修改成功, 登出再登录
                        synchronized (mBinder) {
                            getPreferencesWrapper().setPasswd(newPwd);
                        }
                        mTLC.disconnect();
                    }
                    if (listener != null) {
                        switch (rspMsg.getErrCode()) {
                            case SUCCESS:
                                listener.onStatus(rspMsg, listener.SUCCESS);
                                break;
                            case OLD_PASSWD_WRONG:
                                listener.onStatus(rspMsg, listener.OLD_PASSWD_ERROR);
                                break;
                            case NO_USER:
                                listener.onStatus(rspMsg, listener.OLD_PASSWD_ERROR);
                                break;
                            default:
                                listener.onStatus(rspMsg, listener.FAILURE);
                                break;
                        }
                    }
                } catch (Exception e) {
                    L.e(TAG, "changePasswd() onResponse() is not " + protocol.Message.LoginRspMsg.class.getSimpleName(), e);
                    if (listener != null)
                        listener.onStatus(null, listener.FAILURE);
                }

                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.e(TAG, "changePasswd() onException()", cause);
                if (cause instanceof TimeoutException) {
                    if (listener != null)
                        listener.onStatus(null, listener.TIMEOUT);
                } else {
                    if (listener != null)
                        listener.onStatus(null, listener.NETWORK_ERROR);
                }
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                // never to here for this exec
            }
        });
    }

    public interface OnSetPasswdListener {
        int TIMEOUT = -2;
        int NETWORK_ERROR = -1;
        int SUCCESS = 0;
        int FAILURE = 1;
        int USER_NOT_EXISTS = 2;

        void onStatus(protocol.Message.SetPwdRspMsg rspMsg, int status);
    }

    /**
     * 找回密码
     *
     * @return 执行实体
     */
    public ExecEntity setPasswd(@NonNull String username, @NonNull protocol.Message.UserNameType userNameType, @NonNull String passwd, @Nullable final OnSetPasswdListener listener) {
        protocol.Message.SetPwdReqMsg reqMsg = protocol.Message.SetPwdReqMsg.newBuilder()
                .setName(username)
                .setType(userNameType)
                .setNewPwd(passwd)
                .build();
        return exec(username, reqMsg, TLC_EXEC_TIMEOUT_DEF, true,
                new OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            protocol.Message.SetPwdRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "setPasswd() onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                                // 设置成功, 断开连接
                                mTLC.disconnect();
                            }
                            if (listener != null) {
                                switch (rspMsg.getErrCode()) {
                                    case SUCCESS:
                                        listener.onStatus(rspMsg, listener.SUCCESS);
                                        break;
                                    case FAILURE:
                                        listener.onStatus(rspMsg, listener.FAILURE);
                                        break;
                                    case NO_USER:
                                        listener.onStatus(rspMsg, listener.USER_NOT_EXISTS);
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            L.e(TAG, "setPasswd() onResponse() is not " + protocol.Message.LoginRspMsg.class.getSimpleName(), e);
                            if (listener != null)
                                listener.onStatus(null, listener.FAILURE);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "setPasswd() onException()", cause);
                        if (cause instanceof TimeoutException) {
                            if (listener != null)
                                listener.onStatus(null, listener.TIMEOUT);
                        } else {
                            if (listener != null)
                                listener.onStatus(null, listener.NETWORK_ERROR);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                });
    }

    void onExtrudedLoggedout() {
        synchronized (mBinder) {
            getPreferencesWrapper().setLoginSessionId("");
            getPreferencesWrapper().setCanAutoLogin(false);
        }
        JPushInterface.stopPush(this);
        mTLC.disconnect();

        EventBus.getDefault().postSticky(new Event.ExtrudedLoggedOut());
    }

    void onServerApiNotCompat() {
        synchronized (mBinder) {
            getPreferencesWrapper().setLoginSessionId("");
            getPreferencesWrapper().setCanAutoLogin(false);
        }
        JPushInterface.stopPush(this);
        mTLC.disconnect();

        EventBus.getDefault().postSticky(new Event.ServerApiNotCompat());
    }


    // 执行网络请求

    /**
     * 网络请求的回调 Listener
     */
    public interface OnExecListener {
        /**
         * 得到服务器回响时回调
         *
         * @param request  请求包
         * @param response 回响包
         * @return 该返回值主要用于有第3阶段的 Exec, 返回 true 表示一切正常, false 表示需要终断第3阶段的 timer
         */
        boolean onResponse(@NonNull Pkt request, @NonNull Pkt response);

        /**
         * 无法得到服务器回响时回调
         *
         * @param request 请求包
         * @param cause   具体异常
         */
        void onException(@NonNull Pkt request, @NonNull Throwable cause);

        /**
         * 服务器发过来的第3阶段请求
         *
         * @param firstStageRequest  app 第1阶段的请求的数据包
         * @param firstStageResponse server 回响 app 第1阶段请求的数据包
         * @param thirdStageRequest  server 第3阶段的请求包（一般为第1阶段请求的结果数据）
         * @param responseSetter     回响设置器
         */
        void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter);
    }

    /**
     * 执行网络请求
     *
     * @param protoBufMsg 请求数据的 protoBuf
     * @param listener    回响、超时以及其他失败事件的回调 Listener
     */
    public ExecEntity exec(@NonNull GeneratedMessageV3 protoBufMsg, @Nullable OnExecListener listener) {
        return exec(protoBufMsg, listener, null);
    }

    /**
     * 执行网络请求
     *
     * @param protoBufMsg             请求数据的 protoBuf
     * @param listener                回响、超时以及其他失败事件的回调 Listener
     * @param thirdStageTimeoutMillis 第3阶段回响超时时间 （如果为 null, 则不等待第3阶段）
     */
    public ExecEntity exec(@NonNull GeneratedMessageV3 protoBufMsg, @Nullable OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        return exec(protoBufMsg, TLC_EXEC_TIMEOUT_DEF, listener, thirdStageTimeoutMillis);
    }

    /**
     * 执行网络请求
     *
     * @param protoBufMsg   请求数据的 protoBuf
     * @param timeoutMillis 等待回响的超时时间 (毫秒)
     * @param listener      回响、超时以及其他失败事件的回调 Listener
     */
    public ExecEntity exec(@NonNull GeneratedMessageV3 protoBufMsg, long timeoutMillis, @Nullable OnExecListener listener) {
        return exec(protoBufMsg, timeoutMillis, listener, null);
    }

    /**
     * 执行网络请求
     *
     * @param protoBufMsg             请求数据的 protoBuf
     * @param timeoutMillis           等待回响的超时时间 (毫秒)
     * @param listener                回响、超时以及其他失败事件的回调 Listener
     * @param thirdStageTimeoutMillis 第3阶段回响超时时间 （如果为 null, 则不等待第3阶段）
     */
    public ExecEntity exec(@NonNull GeneratedMessageV3 protoBufMsg, long timeoutMillis, @Nullable OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        return exec(protoBufMsg, timeoutMillis, false, listener, thirdStageTimeoutMillis);
    }

    /**
     * 执行网络请求
     *
     * @param protoBufMsg             请求数据的 protoBuf
     * @param timeoutMillis           等待回响的超时时间 (毫秒)
     * @param forceReconnect          未连接到服务器卡时，是否执行连接操作
     * @param listener                回响、超时以及其他失败事件的回调 Listener
     * @param thirdStageTimeoutMillis 第3阶段回响超时时间 （如果为 null, 则不等待第3阶段）
     */
    public ExecEntity exec(@NonNull GeneratedMessageV3 protoBufMsg, long timeoutMillis, boolean forceReconnect, @Nullable OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        return exec(null, protoBufMsg, timeoutMillis, forceReconnect, listener, thirdStageTimeoutMillis);
    }

    /**
     * 执行网络请求
     *
     * @param srcAddr        请求包的源地址
     * @param protoBufMsg    请求数据的 protoBuf
     * @param timeoutMillis  等待回响的超时时间 (毫秒)
     * @param forceReconnect 未连接到服务器卡时，是否执行连接操作
     * @param listener       回响、超时以及其他失败事件的回调 Listener
     */
    private ExecEntity exec(@Nullable String srcAddr, @NonNull GeneratedMessageV3 protoBufMsg, long timeoutMillis, boolean forceReconnect, @Nullable OnExecListener listener) {
        return exec(srcAddr, protoBufMsg, timeoutMillis, forceReconnect, listener, null);
    }

    /**
     * 执行网络请求
     *
     * @param srcAddr                 请求包的源地址
     * @param protoBufMsg             请求数据的 protoBuf
     * @param timeoutMillis           等待回响的超时时间 (毫秒)
     * @param forceReconnect          未连接到服务器卡时，是否执行连接操作
     * @param listener                回响、超时以及其他失败事件的回调 Listener
     * @param thirdStageTimeoutMillis 第3阶段回响超时时间 （如果为 null, 则不等待第3阶段）
     */
    private ExecEntity exec(@Nullable String srcAddr, @NonNull GeneratedMessageV3 protoBufMsg, long timeoutMillis, boolean forceReconnect, @Nullable OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        Pkt pkt = Pkt.newBuilder()
                .setSrcAddr(TextUtils.isEmpty(srcAddr) ? getPreferencesWrapper().getUserId() : srcAddr)
                .setValue(protoBufMsg)
                .build();
        return exec(pkt, timeoutMillis, forceReconnect, listener, thirdStageTimeoutMillis);
    }

    /**
     * 执行网络请求
     *
     * @param pkt                     请求数据包
     * @param timeoutMillis           等待回响的超时时间 (毫秒)
     * @param forceReconnect          未连接到服务器卡时，是否执行连接操作
     * @param listener                回响、超时以及其他失败事件的回调 Listener
     * @param thirdStageTimeoutMillis 第3阶段回响超时时间 （如果为 null, 则不等待第3阶段）
     * @return 执行实体
     */
    private ExecEntity exec(@NonNull Pkt pkt, long timeoutMillis, boolean forceReconnect, @Nullable OnExecListener listener, @Nullable Long thirdStageTimeoutMillis) {
        return mTLC.exec(pkt, timeoutMillis, forceReconnect, listener, thirdStageTimeoutMillis);
    }

    /**
     * 发送数据包的回调 Listener
     */
    public interface OnSendListener {
        void onSuccess(@NonNull Pkt request);

        void onException(@NonNull Pkt request, @NonNull Throwable cause);
    }

    /**
     * @param protoBufMsg
     * @param dir            Pkt.DIR_REQUEST or Pkt.DIR_RESPONSE
     * @param seq            流水号
     * @param onSendListener
     */
    public void send(GeneratedMessageV3 protoBufMsg, Pkt.Seq seq, OnSendListener onSendListener) {
        Pkt pkt = Pkt.newBuilder()
                .setSeq(seq)
                .setSrcAddr(getPreferencesWrapper().getUserId())
                .setValue(protoBufMsg)
                .build();
        send(pkt, false, onSendListener);
    }

    /**
     * 向服务器发送数据
     *
     * @param pkt 数据包
     */
    private void send(@NonNull Pkt pkt, boolean needIsLoggedIn, OnSendListener onSendListener) {
        mTLC.send(pkt, needIsLoggedIn, onSendListener);
    }


    private static SparseArray<PktProcessor> mPktProcessors = new SparseArray<>();

    static {
        mPktProcessors.put(protocol.Message.Tag.ON_EXTRUDED_LOGIN_VALUE, new PktPForceExit());
        mPktProcessors.put(protocol.Message.Tag.ON_BIND_DEVICE_REQUEST_VALUE, new PktPNotifyAdminBindDev());
        mPktProcessors.put(protocol.Message.Tag.ON_DEVICE_BIND_VALUE, new PktPNotifyUserBindDev());
        mPktProcessors.put(protocol.Message.Tag.ON_DEVICE_UNBIND_VALUE, new PktPNotifyUserUnbindDev());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_CONF_CHANGED_VALUE, new PktPNotifyDevConfChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_CONF_SYNCED_VALUE, new PktPNotifyDevConfSynced());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_FENCE_CHANGED_VALUE, new PktPNotifyFenceChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_SOS_CHANGED_VALUE, new PktPNotifySosChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_SOS_SYNCED_VALUE, new PktPNotifySosSynced());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_CONTACT_CHANGED_VALUE, new PktPNotifyContactChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_CONTACT_SYNCED_VALUE, new PktPNotifyContactSynced());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_ALARM_CLOCK_CHANGED_VALUE, new PktPNotifyAlarmClockChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_ALARM_CLOCK_SYNCED_VALUE, new PktPNotifyAlarmClockSynced());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_CLASS_DISABLE_CHANGED_VALUE, new PktPNotifyClassDisableChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_DEV_CLASS_DISABLE_SYNCED_VALUE, new PktPNotifyClassDisableSynced());
        mPktProcessors.put(protocol.Message.Tag.ON_SCHOOL_GUARD_CHANGED_VALUE, new PktPNotifySchoolGuardChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_PRAISE_CHANGED_VALUE, new PktPNotifyPraiseChanged());
        mPktProcessors.put(protocol.Message.Tag.LOCATE_S3_VALUE, new PktPLocateS3());
        mPktProcessors.put(protocol.Message.Tag.ON_DEVICE_ONLINE_STATUS_VALUE, new PktPNotifyOnlineStatusOfDev());
        mPktProcessors.put(protocol.Message.Tag.ON_DEVICE_POSITION_VALUE, new PktPNotifyDevicePosition());
        mPktProcessors.put(protocol.Message.Tag.ON_LOCATION_MODE_CHANGED_VALUE, new PktPNotifyLocationModeChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_DEVICE_INCIDENT_VALUE, new PktPNotifyIncident());
        mPktProcessors.put(protocol.Message.Tag.ON_FRIEND_CHANGED_VALUE, new PktNotifyFriendChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_USR_DEV_ASSOC_MODIFIED_VALUE, new PktPNotifyUsrDevAssocModified());
        mPktProcessors.put(protocol.Message.Tag.FIND_DEVICE_S3_VALUE, new PktPFindDeviceS3());
        mPktProcessors.put(protocol.Message.Tag.TAKE_PHOTO_S3_VALUE, new PktPTakePhotoS3());
        mPktProcessors.put(protocol.Message.Tag.SIMPLEX_CALL_S3_VALUE, new PktPSimplexCallS3());
        mPktProcessors.put(protocol.Message.Tag.FETCH_DEVICE_SENSOR_DATA_S3_VALUE, new PktPFetchDeviceSensorDataS3());
        mPktProcessors.put(protocol.Message.Tag.ON_DEVICE_SENSOR_DATA_VALUE, new PktPNotifyDeviceSensorData());
        mPktProcessors.put(protocol.Message.Tag.ON_NEW_MICRO_CHAT_EMOTICON_VALUE, new PktPNotifyMicroChatEmoticon());
        mPktProcessors.put(protocol.Message.Tag.ON_NEW_MICRO_CHAT_VOICE_VALUE, new PktPNotifyMicroChatVoice());
        mPktProcessors.put(protocol.Message.Tag.ON_NEW_MICRO_CHAT_TEXT_VALUE, new PktPNotifyMicroChatText());
        mPktProcessors.put(protocol.Message.Tag.ON_NEW_CHAT_MESSAGE_VALUE, new PktPNotifyChatMessage());
        mPktProcessors.put(protocol.Message.Tag.ON_NEW_GROUP_CHAT_MESSAGE_VALUE, new PktPNotifyGroupChatMessage());
        mPktProcessors.put(protocol.Message.Tag.SMS_AGENT_ON_NEW_SMS_VALUE, new PktPNotifySMSAgentNewSMS());
        mPktProcessors.put(protocol.Message.Tag.ON_CHAT_GROUP_MEMBER_CHANGED_VALUE, new PktPNotifyChatGroupMemberChanged());
        mPktProcessors.put(protocol.Message.Tag.ON_SOS_CALL_ORDER_CHANGED_VALUE, new PktPNotifySosCallOrderChanged());
        /* mPktProcessors.put... TODO: 增加相关指令后，这里需要添加 */
    }

    private void processRequestPkt(@NonNull final Pkt reqPkt, @NonNull final PktProcessor.OnResponse responseCallback) {
        PktProcessor processor = mPktProcessors.get(reqPkt.tag);
        if (processor != null) {
            processor.process(this, mOnEventListener, reqPkt, responseCallback);
        } else {
            L.w(TAG, "request Pkt " + reqPkt + " not found processor");
            if (mOnEventListener != null)
                mOnEventListener.onNotProcessedPkt(reqPkt, new OnResponseSetter() {
                    @Override
                    public void setResponse(GeneratedMessageV3 rspMsg) {
                        Pkt.Builder rspPktBuilder = Pkt.newBuilder()
                                .setSeq(reqPkt.seq)
                                .setSrcAddr(reqPkt.dstAddr);
                        if (rspMsg != null) {
                            rspPktBuilder.setValue(rspMsg);
                        } else {
                            rspPktBuilder.setDir(Pkt.DIR_RESPONSE).setTag(reqPkt.tag);
                        }
                        responseCallback.setResponse(rspPktBuilder.build());
                    }
                });
        }
    }


    private long reconnectDelay; // 毫秒

    private void resetReconnectDelay(long delayMillis) {
        reconnectDelay = delayMillis;
    }

    private void reconnectTlc(long lastConnTimeSpentSinceConnect) {
        mHandler.removeMessages(ServiceHandler.RECONNECT_REQ);
        if (isConnectivityValid() && getPreferencesWrapper().canAutoLogin()) {

            // 检查 JPush 的 RegistrationID
            String jPushRegistrationID = JPushInterface.getRegistrationID(this);
            if (TextUtils.isEmpty(jPushRegistrationID)) {
                // 等待 JPush 的 RegistrationID 就绪
                mHandler.sendEmptyMessageDelayed(ServiceHandler.RECONNECT_REQ, 1000L);
                return;
            }

            if (lastConnTimeSpentSinceConnect >= reconnectDelay) {
                resetReconnectDelay(0);
            }
            mTLC.connect(getPreferencesWrapper().getUserId(), reconnectDelay - lastConnTimeSpentSinceConnect);
            synchronized (this) {
                if (reconnectDelay < 1000) {
                    reconnectDelay = 1000;
                } else {
                    reconnectDelay <<= 1;
                }
                if (reconnectDelay > 8000) {
                    reconnectDelay = 8000;
                }
            }
        }
    }

    private void autoLogin() {
        final String username = getPreferencesWrapper().getUsername();
        protocol.Message.UserNameType userNameType = getPreferencesWrapper().getUsernameType();
        if (TextUtils.isEmpty(username) || !getPreferencesWrapper().canAutoLogin()) {
            L.d(TAG, "in autoLogin(): username isEmpty || canAutoLogin: " + getPreferencesWrapper().canAutoLogin());
            return;
        }
        String passwd = getPreferencesWrapper().getPasswd();
        if (userNameType.getNumber() < protocol.Message.UserNameType.USR_NAM_TYP_3RD_QQ_VALUE) {
            // 需要密码登录
            if (TextUtils.isEmpty(passwd)) {
                L.d(TAG, "in autoLogin(): passwd isEmpty");
                return;
            }
        }

        protocol.Message.LoginReqMsg reqMsg = makeLoginReqMsg(username, userNameType, passwd, getPreferencesWrapper().getLoginSessionId());
        if (TextUtils.isEmpty(reqMsg.getPushConf().getToken())) {
            // JPush 的 RegistrationID 还未就绪，重连。
            L.w(TAG, "autoLogin failure: JPushRegistrationID: " + reqMsg.getPushConf().getToken() + ", is empty");
            mTLC.disconnect();
        }
        String userId = getPreferencesWrapper().getUserId();
        exec(userId, reqMsg, 1000 * 8, true, new OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    protocol.Message.LoginRspMsg rspMsg = response.getProtoBufMsg();
                    L.v(TAG, "mTLC onConnected() autoLogin onResponse(): " + rspMsg);
                    if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                        String userId = rspMsg.getUserId();
                        mTLC.setLocalPktAddr(userId);
                        mTLC.setIsLoggedIn(true);
                        // 登陆成功，启动心跳定时器
                        mTLC.enableHeartbeat(rspMsg.getExpire());

                        L.i(TAG, "autoLogin success");
                        mHandler.obtainMessage(ServiceHandler.ON_LOGGEDIN, userId).sendToTarget();

                        synchronized (mBinder) {
                            if (!rspMsg.getSessionId().equals(getPreferencesWrapper().getLoginSessionId())) {
                                getPreferencesWrapper().setLoginSessionId(rspMsg.getSessionId());
                            }
                        }

                        JPushInterface.resumePush(TlcService.this);
                    } else {
                        L.e(TAG, "autoLogin onResponse() login failure: " + rspMsg.getErrCode());
                        switch (rspMsg.getErrCode()) {
                            case INVALID_PARAM:
                            case NOT_IMPLEMENTED:
                            case USER_OR_PASSWD_WRONG:
                            case NO_USER:
                            case NOT_EXISTS:
                                logout();
                                break;

                            case INVALID_SESSION:
                                // session ID 已无效，应该是被其他 app 挤出了。
                                onExtrudedLoggedout();
                                break;

                            case NOT_COMPAT:
                                // 与服务器交互的接口已不兼容，需要升级 APP
                                onServerApiNotCompat();
                                break;

                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    L.e(TAG, "mTLC onConnected() autoLogin onResponse() is not " + protocol.Message.LoginRspMsg.class.getSimpleName(), e);
                }

                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.e(TAG, "mTLC onConnected() autoLogin onException()", cause);
                mTLC.disconnect();
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                // never to here for this exec
            }
        });
    }

    public class TlcServiceBinder extends Binder {
        public TlcService getService() {
            return TlcService.this;
        }
    }

    private ConnectivityManager connectivityManager;

    public NetworkInfo getNetworkInfo() {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        return connectivityManager == null ? null : connectivityManager.getActiveNetworkInfo();
    }

    public boolean networkIsAvailable() {
        NetworkInfo ni = getNetworkInfo();
        return ni != null && ni.isConnected();
    }

    public boolean isConnectivityValid() {
        return getPreferencesWrapper().isValidConnection(getNetworkInfo());
    }

    public boolean isValidMobileConnection() {
        return getPreferencesWrapper().isValidMobileConnection(getNetworkInfo());
    }

    public synchronized PreferencesWrapper getPreferencesWrapper() {
        if (mPreferencesWrapper == null) {
            mPreferencesWrapper = PreferencesWrapper.getInstance(this);
        }
        return mPreferencesWrapper;
    }

    private PowerManager getPowerManager() {
        if (mPowerManager == null) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
        return mPowerManager;
    }


    static class ServiceHandler extends Handler {
        public static final int ON_TLC_CONNECTED = 0;
        public static final int ON_TLC_DISCONNECTED = 1;
        public static final int ON_EXEC_RESPONSE = 2;
        public static final int ON_EXEC_EXCEPTION = 3;
        public static final int ON_THIRD_STAGE_EVENT = 4;

        public static final int ON_EVENT_PKT = 1000;

        static final int ON_LOGGEDIN = 2000;
        static final int ON_LOGGEDOUT = 2001;

        static final int RECONNECT_REQ = 3000;

        static final int ON_CONNECTIVITY_INVALID = 4000;

        static final int ON_GOT_JPushRegistrationID = 1000002;

        private WeakReference<TlcService> mS;

        ServiceHandler(TlcService s) {
            mS = new WeakReference<>(s);
        }

        @Override
        public void handleMessage(Message msg) {
            TlcService s = mS.get();
            if (s == null)
                return;

            switch (msg.what) {
                case ON_TLC_CONNECTED:
                    L.i(TAG, "mTLC connected");
                    if (s.mOnEventListener != null)
                        s.mOnEventListener.onConnected();
                    s.autoLogin();
                    break;
                case ON_TLC_DISCONNECTED: {
                    long timeSpent = msg.obj == null ? 0 : (long) msg.obj;
                    L.e(TAG, "mTLC disconnected. time spent since connect: " + timeSpent);
                    s.jPushRegistrationID = "";
                    s.loggedin = false;
                    if (s.mOnEventListener != null)
                        s.mOnEventListener.onDisconnected();
                    s.reconnectTlc(timeSpent);
                }
                    break;

                case ON_EXEC_RESPONSE: {
                    ExecEntity execEntity = (ExecEntity) msg.obj;
                    if (execEntity.listener != null) {
                        if (!execEntity.listener.onResponse(execEntity.request, execEntity.response)) {
                            if (execEntity.thirdStageTimeoutMillis != null) {
                                s.mTLC.stopThirdStageTimer(execEntity);
                            }
                        }
                    }
                }
                    break;

                case ON_EXEC_EXCEPTION: {
                    ExecEntity execEntity = (ExecEntity) msg.obj;
                    if (execEntity.listener != null)
                        execEntity.listener.onException(execEntity.request, execEntity.cause);

                    if (execEntity.cause instanceof NotYetConnectedException) {
                        s.resetReconnectDelay(200);
                        s.reconnectTlc(0);
                    }
                }
                    break;

                case ON_THIRD_STAGE_EVENT: {
                    final ExecEntity execEntity = (ExecEntity) msg.obj;
                    if (execEntity.listener != null)
                        execEntity.listener.onThirdStage(execEntity.request, execEntity.response, execEntity.thirdStageEvent, new OnResponseSetter() {
                            @Override
                            public void setResponse(@Nullable GeneratedMessageV3 rspMsg) {
                                Pkt.Builder rspPktBuilder = Pkt.newBuilder()
                                        .setSeq(execEntity.thirdStageEvent.seq)
                                        .setSrcAddr(execEntity.thirdStageEvent.dstAddr);
                                if (rspMsg != null) {
                                    rspPktBuilder.setValue(rspMsg);
                                } else {
                                    rspPktBuilder.setDir(Pkt.DIR_RESPONSE)
                                            .setTag(execEntity.thirdStageEvent.tag);
                                }
                                Pkt rspPkt = rspPktBuilder.build();
                                L.d(TAG, "third stage Pkt(" + execEntity.thirdStageEvent + ") -> OnResponseSetter.setResponse(" + rspMsg + ")");
                                TlcService s = mS.get();
                                if (s == null)
                                    return;
                                s.mTLC.send(rspPkt, false, null);
                            }
                        });
                }
                    break;

                case ON_EVENT_PKT: {
                    final Pkt reqPkt = (Pkt) msg.obj;
                    L.d(TAG, "mTLC receive request Pkt: " + reqPkt);
                    s.processRequestPkt(reqPkt, new PktProcessor.OnResponse() {
                        @Override
                        public void setResponse(@NonNull Pkt rspPkt) {
                            L.d(TAG, "event Pkt(" + reqPkt + ") -> OnResponse.setResponse(" + rspPkt + ")");
                            TlcService s = mS.get();
                            if (s == null)
                                return;
                            s.mTLC.send(rspPkt, false, null);
                        }
                    });
                }
                break;

                case ON_LOGGEDIN:
                    s.loggedin = true;
                    s.resetReconnectDelay(0);
                    if (s.mOnEventListener != null)
                        s.mOnEventListener.onLoggedin((String) msg.obj);
                    if (TextUtils.isEmpty(s.jPushRegistrationID)) {
                        s.uploadPushConf();
                    }
                    break;

                case ON_LOGGEDOUT:
                    if (s.mOnEventListener != null)
                        s.mOnEventListener.onLoggedout((String) msg.obj);
                    break;

                case RECONNECT_REQ:
                    s.reconnectTlc(0);
                    break;

                case ON_CONNECTIVITY_INVALID:
                    removeMessages(ServiceHandler.RECONNECT_REQ);
                    break;

                case ON_GOT_JPushRegistrationID:
                    L.i(TAG, "got JPushInterface.ACTION_REGISTRATION_ID");
                    if (s.loggedin) {
                        s.uploadPushConf();
                    }
                    break;

                default:
                    break;
            }
        }
    }
}