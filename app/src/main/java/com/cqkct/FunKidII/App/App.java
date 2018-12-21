package com.cqkct.FunKidII.App;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.BuildConfig;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.DBOpenHelper;
import com.cqkct.FunKidII.db.Dao.DaoMaster;
import com.cqkct.FunKidII.db.Dao.DaoSession;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.MainService;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnEventListener;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.facebook.stetho.Stetho;
import com.mob.MobSDK;
import com.tencent.bugly.crashreport.CrashReport;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.greendao.database.Database;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.jpush.android.api.JPushInterface;
import protocol.Message;

/**
 * Created by Justin on 2017/7/25.
 */

public class App extends MultiDexApplication {
    private static final String TAG = App.class.getSimpleName();

    private static App instance;

    private DaoSession daoSession;

    /** BabyEntity 可能随时会变 */
    private BabyEntity mCurrentBabyEntity;

    public void setUserId(String userId) {
        synchronized (this) {
            mUserId = userId;
        }
    }

    @Nullable
    public String getUserId() {
        synchronized (this) {
            return mUserId;
        }
    }

    @Nullable
    public String getDeviceId() {
        synchronized (mOnDeviceChangeListeners) {
            return mCurrentBabyEntity == null ? null : mCurrentBabyEntity.getDeviceId();
        }
    }

    public interface OnDeviceChangeListener {
        void onCurrentBabyChanged(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky);
        void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo);
    }

    @Nullable
    public BabyEntity getCurrentBabyBean() {
        return mCurrentBabyEntity;
    }

    public void setCurrentBaby(@Nullable BabyEntity babyEntity) {
        mEvHandler.obtainMessage(EvHandler.ON_DEVICE_CHANGED, babyEntity).sendToTarget();
    }

    private final List<OnDeviceChangeListener> mOnDeviceChangeListeners = new ArrayList<>();

    public void regOnCurrentBabyBeanChangeListener(@NonNull OnDeviceChangeListener listener) {
        synchronized (mOnDeviceChangeListeners) {
            mOnDeviceChangeListeners.add(listener);
            mEvHandler.obtainMessage(EvHandler.STICKY_ON_DEVICE_CHANGED, listener).sendToTarget();
        }
    }

    public void unregOnCurrentBabyBeanChangeListener(@NonNull OnDeviceChangeListener listener) {
        synchronized (mOnDeviceChangeListeners) {
            mOnDeviceChangeListeners.remove(listener);
        }
    }

    public void notifyDeviceEntity(DeviceEntity deviceEntity) {
        mEvHandler.obtainMessage(EvHandler.ON_NOTIFY_DEVICE_ENTIRY, deviceEntity).sendToTarget();
    }

    private EvHandler mEvHandler = new EvHandler(this);
    private TlcService mTlcService = null;
    private Boolean mTlcServiceBound = null;


    public boolean isMainProcess() {
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null)
            return false;
        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
        if (infos == null)
            return false;
        for (ActivityManager.RunningAppProcessInfo processInfo : infos) {
            if (processInfo.pid == pid) {
                L.d(TAG, "processName: " + processInfo.processName);
                if (getPackageName().equals(processInfo.processName))
                    return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        L.setIsDebug(BuildConfig.DEBUG);

        if (!isMainProcess())
            return;

        // Bugly SDK初始化
        CrashReport.initCrashReport(getApplicationContext());
        //Third Party Login
        MobSDK.init(this);
        // 友盟
        UMConfigure.init(this, UMConfigure.DEVICE_TYPE_PHONE, "" /* FIXME: Push推送业务的secret */);
        UMConfigure.setLogEnabled(BuildConfig.DEBUG);
        UMConfigure.setEncryptEnabled (true);
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
        MobclickAgent.setCatchUncaughtExceptions(false);
        MobclickAgent.openActivityDurationTrack(false);

        setupPreferencesWrapper();

        setupDatabase();

        // MainService
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(new Intent(this, MainService.class));
        }
        bindMainService();

        // TCP 长连接服务
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(new Intent(this, TlcService.class));
        }
        bindTlcService();

        instance = this;

        if (BuildConfig.DEBUG) {
            // Debug模式下才初始化
            Stetho.initializeWithDefaults(this);
        }

//        // 设置未捕获异常的处理器
//        Thread.setDefaultUncaughtExceptionHandler(new GlobarCatchException()); 已经使用腾讯 bugly

        if (BuildConfig.DEBUG) {
            JPushInterface.setDebugMode(true); // 设置开启日志
        }
        JPushInterface.init(this); // 初始化 JPush
    }

    @Override
    public void onTerminate() {
        L.i(TAG, "onTerminate");
        unbindTlcService();
        unbindMainService();
        super.onTerminate();
    }

    private void setupPreferencesWrapper() {
        PreferencesWrapper pref = PreferencesWrapper.getInstance(this);
        int preferencesVersion = pref.getPreferenceVersion();
        if (preferencesVersion < 0) {
            L.i(TAG, "RESET PreferencesWrapper SETTINGS !");
            pref.resetAllDefaultValues();
        }
        synchronized (this) {
            if (TextUtils.isEmpty(mUserId)) {
                String userId = pref.getUserId();
                if (!TextUtils.isEmpty(userId)) {
                    mUserId = userId;
                }
            }
        }
    }

    /**
     * 数据库操作
     */
    private void setupDatabase() {
        DBOpenHelper helper = new DBOpenHelper(this, Constants.DB_NAME + (BuildConfig.DEBUG ? "" : ".encrypted"), null);
        Database db = BuildConfig.DEBUG ? helper.getWritableDb() : helper.getEncryptedWritableDb("funkidii-super-secret-db");
        daoSession = new DaoMaster(db).newSession();
        GreenUtils.init(this);
        L.i("DataBase setting :","database haven Setting Up");
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public static App getInstance(){
        return instance;
    }


    public static void removeStickyExtrudedLoggedOutEvent() {
        Event.ExtrudedLoggedOut stickyExtrudedLoggedOut = EventBus.getDefault().getStickyEvent(Event.ExtrudedLoggedOut.class);
        if(stickyExtrudedLoggedOut != null) {
            EventBus.getDefault().removeStickyEvent(stickyExtrudedLoggedOut);
            DeviceInfo.invalidate();
        }
    }


    public interface OnTlcServiceBindListener {
        void onTlcServiceBound(TlcService service, boolean isSticky);
        void onTlcServiceLost(boolean isSticky);
    }

    private final List<OnTlcServiceBindListener> mOnTlcServiceBindListeners = new ArrayList<>();

    public void regOnTlcServiceBindListener(OnTlcServiceBindListener listener) {
        if (listener == null)
            return;
        synchronized (mOnTlcServiceBindListeners) {
            mOnTlcServiceBindListeners.add(listener);
            if (mTlcServiceBound != null) {
                if (mTlcServiceBound) {
                    Object[] objs = new Object[]{listener, mTlcService};
                    mEvHandler.obtainMessage(EvHandler.STICKY_ON_TLCSERVICE_BOUND, objs).sendToTarget();
                } else {
                    mEvHandler.obtainMessage(EvHandler.STICKY_ON_TLCSERVICE_LOST, listener).sendToTarget();
                }
            }
        }
    }

    public void unregOnTlcServiceBindListener(OnTlcServiceBindListener listener) {
        synchronized (mOnTlcServiceBindListeners) {
            mOnTlcServiceBindListeners.remove(listener);
        }
    }

    private final List<TlcServiceEventCallback> mTlcServiceEventCallbacks = new ArrayList<>();
    @Nullable
    private Boolean mTlcIsConnected;
    private Long mUserOfflineTime;
    @Nullable
    private Boolean mIsLoggedOut;
    @Nullable
    private String mUserId;
    public void regTlcServiceEventCallback(TlcServiceEventCallback cb) {
        if (cb == null)
            return;
        synchronized (mTlcServiceEventCallbacks) {
            mTlcServiceEventCallbacks.add(cb);
            if (mTlcServiceBound != null) {
                if (mTlcIsConnected != null) {
                    Object[] objs = new Object[]{cb, mTlcService};
                    if (mTlcIsConnected) {
                        mEvHandler.obtainMessage(EvHandler.STICKY_ON_CONNECTED, objs).sendToTarget();
                    } else {
                        mEvHandler.obtainMessage(EvHandler.STICKY_ON_DISCONNECTED, objs).sendToTarget();
                    }
                }
                if (mIsLoggedOut != null && mIsLoggedOut) {
                    mEvHandler.obtainMessage(EvHandler.STICKY_ON_LOGGED_OUT, new Object[]{cb, mTlcService, mUserId}).sendToTarget();
                }
            }
        }
    }

    public void unregTlcServiceEventCallback(TlcServiceEventCallback cb) {
        synchronized (mTlcServiceEventCallbacks) {
            mTlcServiceEventCallbacks.remove(cb);
        }
    }

    @Nullable
    public TlcService getTlcService() {
        return mTlcService;
    }

    private void bindTlcService() {
        mTlcServiceConnection = MTlcServiceConnection;
        bindService(new Intent(this, TlcService.class), mTlcServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindTlcService() {
        if (mTlcServiceConnection != null)
            unbindService(mTlcServiceConnection);
    }

    private boolean tlcServiceRebindOnDisconnected = true;
    private ServiceConnection mTlcServiceConnection;
    private ServiceConnection MTlcServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            L.i(TAG, "TlcService connected");
            try {
                TlcService.TlcServiceBinder tlcServiceBinder = (TlcService.TlcServiceBinder) binder;
                mTlcService = tlcServiceBinder.getService();
                mTlcServiceBound = true;
                mTlcService.setOnTlcEventListener(mOnEventListener);
                synchronized (mOnTlcServiceBindListeners) {
                    for (OnTlcServiceBindListener one : mOnTlcServiceBindListeners)
                        one.onTlcServiceBound(mTlcService, false);
                }
            } catch (Exception e) {
                L.w(TAG, "mTlcServiceConnection.onServiceConnected()", e);
                tlcServiceRebindOnDisconnected = false;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            L.i(TAG, "TlcService disconnected");
            if (!tlcServiceRebindOnDisconnected)
                return;
            mTlcServiceBound = false;
            mTlcService = null;
            synchronized (mOnTlcServiceBindListeners) {
                for (OnTlcServiceBindListener one : mOnTlcServiceBindListeners)
                    one.onTlcServiceLost(false);
            }
            bindTlcService();
        }
    };

    private static class EvHandler extends Handler {
        static final int STICKY_ON_LOGGED_IN = -1;
        static final int STICKY_ON_LOGGED_OUT = -2;
        static final int STICKY_ON_TLCSERVICE_BOUND = -3;
        static final int STICKY_ON_TLCSERVICE_LOST = -4;
        static final int STICKY_ON_CONNECTED = -6;
        static final int STICKY_ON_DISCONNECTED = -7;
        static final int STICKY_ON_DEVICE_CHANGED = -8;

        static final int ON_DEVICE_CHANGED = 10000;
        static final int ON_NOTIFY_DEVICE_ENTIRY = 10001;

        static final int ON_CONNECTED = 1;
        static final int ON_DISCONNECTED = 2;
        static final int ON_LOGGED_IN = 3;
        static final int ON_LOGGED_OUT = 4;
        static final int ON_BIND_DEVICE_REQUEST = 6;
        static final int ON_DEVICE_BIND = 7;
        static final int ON_DEVICE_UNBIND = 8;
        static final int ON_DEV_CONF_CHANGED = 9;
        static final int ON_DEV_CONF_SYNCED = 10;
        static final int ON_DEV_FENCE_CHANGED = 11;
        static final int ON_DEV_SOS_CHANGED = 12;
        static final int ON_DEV_SOS_SYNCED = 13;
        static final int ON_DEV_CONTACT_CHANGED = 14;
        static final int ON_DEV_CONTACT_SYNCED = 15;
        static final int ON_DEV_ALARM_CLOCK_CHANGED = 16;
        static final int ON_DEV_ALARM_CLOCK_SYNCED = 17;
        static final int ON_DEV_CLASS_DISABLE_CHANGED = 18;
        static final int ON_DEV_CLASS_DISABLE_SYNCED = 19;
        static final int ON_SCHOOL_GUARD_CHANGED = 20;
        static final int ON_PRAISE_CHANGED = 21;
        static final int ON_LOCATE_S3 = 22;
        static final int ON_DEVICE_ONLINE_STATUS = 23;
        static final int ON_DEVICE_POSITION = 24;
        static final int ON_DEVICE_INCIDENT = 25;
        static final int ON_USR_DEV_ASSOC_MODIFIED = 26;
        static final int ON_FIND_DEVICE_S3 = 27;
        static final int ON_TAKE_PHOTO_S3 = 28;
        static final int ON_SIMPLEX_CALL_S3 = 29;
        static final int ON_FETCH_DEVICE_SENSOR_DATA_S3 = 30;
        static final int ON_DEVICE_SENSOR_DATA = 31;
        static final int ON_NEW_MICRO_CHAT_MSG = 34;
        static final int ON_LOCATION_MODE_CHANGED = 35;
        static final int ON_NEW_MICRO_CHAT_GROUP_MSG = 36;
        static final int ON_NEW_SMS_AGENT = 37;
        static final int ON_DEVICE_FRIEND_CHANGED = 38;
        static final int ON_CHAT_GROUP_MEMBER_CHANGED = 39;
        static final int ON_NOTIFY_SOS_CALL_ORDER_CHANGED = 40;
        static final int ON_NOT_PROCESSED_PKT = 1000;

        static final int UPDATE_DEVICE_ONLINE_STATUS = 2000;

        // mainService 事件
        static final int ON_BIND_DEVICE_REQUEST_FROM_MAIN_SERVICE = 100001;

        private WeakReference<App> mA;

        EvHandler(App a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            App a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {

                case STICKY_ON_TLCSERVICE_BOUND: {
                    Object[] objs = (Object[]) msg.obj;
                    OnTlcServiceBindListener listener = (OnTlcServiceBindListener) objs[0];
                    TlcService tlcService = (TlcService) objs[1];
                    synchronized (a.mOnTlcServiceBindListeners) {
                        for (OnTlcServiceBindListener one : a.mOnTlcServiceBindListeners) {
                            if (one == listener)
                                listener.onTlcServiceBound(tlcService, true);
                        }
                    }
                }
                break;

                case STICKY_ON_TLCSERVICE_LOST: {
                    OnTlcServiceBindListener listener = (OnTlcServiceBindListener) msg.obj;
                    synchronized (a.mOnTlcServiceBindListeners) {
                        for (OnTlcServiceBindListener one : a.mOnTlcServiceBindListeners) {
                            if (one == listener)
                                listener.onTlcServiceLost(true);
                        }
                    }
                }
                break;


                case STICKY_ON_CONNECTED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcServiceEventCallback cb = (TlcServiceEventCallback) objs[0];
                    TlcService tlcService = (TlcService) objs[1];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            if (one == cb)
                                cb.onConnected(tlcService, true);
                    }
                }
                break;

                case STICKY_ON_DISCONNECTED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcServiceEventCallback cb = (TlcServiceEventCallback) objs[0];
                    TlcService tlcService = (TlcService) objs[1];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            if (one == cb)
                                cb.onDisconnected(tlcService, true);
                    }
                }
                break;

                case STICKY_ON_DEVICE_CHANGED: {
                    OnDeviceChangeListener listener = (OnDeviceChangeListener) msg.obj;
                    if (listener != null) {
                        BabyEntity babyBean = a.mCurrentBabyEntity;
                        listener.onCurrentBabyChanged(null, babyBean, true);
                        if (babyBean != null) {
                            DeviceInfo di = DeviceInfo.getDeviceInfo(babyBean.getDeviceId(), false);
                            if (di != null) {
                                listener.onDeviceInfoChanged(di);
                            }
                        }
                    }
                }
                    break;

                case ON_DEVICE_CHANGED: {
                    BabyEntity babyEntity = (BabyEntity) msg.obj;
                    synchronized (a.mOnDeviceChangeListeners) {
                        if (!BabyEntity.equals(a.mCurrentBabyEntity, babyEntity)) {
                            BabyEntity old = a.mCurrentBabyEntity;

                            DeviceInfo deviceInfo = null;
                            if (babyEntity != null) {
                                if (old == null || !old.getDeviceId().equals(babyEntity.getDeviceId())) {
                                    deviceInfo = DeviceInfo.getDeviceInfo(babyEntity.getDeviceId(), true);
                                }
                            }

                            a.mCurrentBabyEntity = babyEntity;
                            for (OnDeviceChangeListener one : a.mOnDeviceChangeListeners)
                                one.onCurrentBabyChanged(old, babyEntity, false);

                            if (deviceInfo != null) {
                                for (OnDeviceChangeListener one : a.mOnDeviceChangeListeners)
                                    one.onDeviceInfoChanged(deviceInfo);
                            }
                        }
                    }
                }
                    break;

                case ON_NOTIFY_DEVICE_ENTIRY: {
                    DeviceEntity deviceEntity = (DeviceEntity) msg.obj;
                    if (deviceEntity != null && !TextUtils.isEmpty(deviceEntity.getDeviceId())) {
                        DeviceInfo.cache(deviceEntity);
                        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceEntity.getDeviceId(), true);
                        if (deviceInfo != null) {
                            synchronized (a.mOnDeviceChangeListeners) {
                                for (OnDeviceChangeListener one : a.mOnDeviceChangeListeners)
                                    one.onDeviceInfoChanged(deviceInfo);
                            }
                        }
                    }
                }
                    break;


                case STICKY_ON_LOGGED_IN: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcServiceEventCallback cb = (TlcServiceEventCallback) objs[0];
                    TlcService tlcService = (TlcService) objs[1];
                    String user = (String) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            if (one == cb)
                                one.onLoggedin(tlcService, user, true);
                    }
                }
                    break;

                case STICKY_ON_LOGGED_OUT: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcServiceEventCallback cb = (TlcServiceEventCallback) objs[0];
                    TlcService tlcService = (TlcService) objs[1];
                    String user = (String) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            if (one == cb)
                                one.onLoggedout(tlcService, user, true);
                    }
                }
                    break;


                //*********************** TlcService 事件 *******************************/

                case ON_CONNECTED: {
                    TlcService tlcService = (TlcService) msg.obj;

                    a.mTlcIsConnected = true;
                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            one.onConnected(tlcService, false);
                    }
                }
                    break;

                case ON_DISCONNECTED: {
                    TlcService tlcService = (TlcService) msg.obj;
                    a.mUserOfflineTime = System.currentTimeMillis();
                    a.mTlcIsConnected = false;
                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            one.onDisconnected(tlcService, false);
                    }
                }
                    break;

                case ON_LOGGED_IN: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    String userId = (String) objs[1];

                    removeStickyExtrudedLoggedOutEvent();

                    boolean queryFromServer = a.mUserOfflineTime != null
                            && Math.abs(System.currentTimeMillis() - a.mUserOfflineTime) > 1000L * 60 * 60;
                    DeviceInfo.fillDeviceOfUserCache(userId,  queryFromServer);
                    a.mUserOfflineTime = null;

                    a.setUserId(userId);
                    a.mIsLoggedOut = false;
                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            one.onLoggedin(tlcService, userId, false);
                    }
                }
                    break;

                case ON_LOGGED_OUT: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    String userId = (String) objs[1];

                    a.mIsLoggedOut = true;
                    removeStickyExtrudedLoggedOutEvent();
                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks)
                            one.onLoggedout(tlcService, userId, false);
                    }

                    DeviceInfo.clearCache();
                }
                    break;

                case ON_BIND_DEVICE_REQUEST: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyAdminBindDevReqMsg reqMsg = (Message.NotifyAdminBindDevReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onBindDeviceRequest(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEVICE_BIND: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyUserBindDevReqMsg reqMsg = (Message.NotifyUserBindDevReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDeviceBind(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEVICE_UNBIND: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyUserUnbindDevReqMsg reqMsg = (Message.NotifyUserUnbindDevReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDeviceUnbind(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_CONF_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyDevConfChangedReqMsg reqMsg = (Message.NotifyDevConfChangedReqMsg) objs[2];
                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevConfChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_CONF_SYNCED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyDevConfSyncedReqMsg reqMsg = (Message.NotifyDevConfSyncedReqMsg) objs[2];
                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevConfSynced(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_FENCE_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyFenceChangedReqMsg reqMsg = (Message.NotifyFenceChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevFenceChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_SOS_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifySosChangedReqMsg reqMsg = (Message.NotifySosChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevSosChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_SOS_SYNCED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifySosSyncedReqMsg reqMsg = (Message.NotifySosSyncedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevSosSynced(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_CONTACT_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyContactChangedReqMsg reqMsg = (Message.NotifyContactChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevContactChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_CONTACT_SYNCED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyContactSyncedReqMsg reqMsg = (Message.NotifyContactSyncedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevContactSynced(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_ALARM_CLOCK_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyAlarmClockChangedReqMsg reqMsg = (Message.NotifyAlarmClockChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onAlarmClockChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_ALARM_CLOCK_SYNCED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyAlarmClockSyncedReqMsg reqMsg = (Message.NotifyAlarmClockSyncedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onAlarmClockSynced(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_CLASS_DISABLE_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyClassDisableChangedReqMsg reqMsg = (Message.NotifyClassDisableChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onClassDisableChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEV_CLASS_DISABLE_SYNCED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyClassDisableSyncedReqMsg reqMsg = (Message.NotifyClassDisableSyncedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onClassDisableSynced(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_SCHOOL_GUARD_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifySchoolGuardChangedReqMsg reqMsg = (Message.NotifySchoolGuardChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onSchoolGuardChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_PRAISE_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyPraiseChangedReqMsg reqMsg = (Message.NotifyPraiseChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onPraiseChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_LOCATE_S3: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.LocateS3ReqMsg reqMsg = (Message.LocateS3ReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onLocateS3(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEVICE_ONLINE_STATUS: {
                    Object[] objs = (Object[]) msg.obj;
                    Message.NotifyOnlineStatusOfDevReqMsg reqMsg = (Message.NotifyOnlineStatusOfDevReqMsg) objs[2];
                    objs = new Object[] { reqMsg.getDeviceId(), reqMsg.getOnline() };
                    obtainMessage(UPDATE_DEVICE_ONLINE_STATUS, objs).sendToTarget();
                }
                    break;

                case ON_DEVICE_POSITION: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyDevicePositionReqMsg reqMsg = (Message.NotifyDevicePositionReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDevicePosition(tlcService, reqPkt, reqMsg);
                        }
                    }
                    break;
                }

                case ON_LOCATION_MODE_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyLocationModeChangedReqMsg reqMsg = (Message.NotifyLocationModeChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onLocationModeChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                    break;
                }

                case ON_NEW_SMS_AGENT: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifySMSAgentNewSMSReqMsg reqMsg = (Message.NotifySMSAgentNewSMSReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onNotifySMSAgentNewSMS(tlcService, reqPkt, reqMsg);
                        }
                    }
                    break;
                }


                case ON_DEVICE_INCIDENT: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyIncidentReqMsg reqMsg = (Message.NotifyIncidentReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDeviceIncident(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_USR_DEV_ASSOC_MODIFIED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyUsrDevAssocModifiedReqMsg reqMsg = (Message.NotifyUsrDevAssocModifiedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onUsrDevAssocModified(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_FIND_DEVICE_S3: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.FindDeviceS3ReqMsg reqMsg = (Message.FindDeviceS3ReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onFindDeviceS3(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_TAKE_PHOTO_S3: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.TakePhotoS3ReqMsg reqMsg = (Message.TakePhotoS3ReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onTakePhotoS3(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_SIMPLEX_CALL_S3: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.SimplexCallS3ReqMsg reqMsg = (Message.SimplexCallS3ReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onSimplexCallS3(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_FETCH_DEVICE_SENSOR_DATA_S3: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.FetchDeviceSensorDataS3ReqMsg reqMsg = (Message.FetchDeviceSensorDataS3ReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onFetchDevStatusInfoS3(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEVICE_SENSOR_DATA: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyDeviceSensorDataReqMsg reqMsg = (Message.NotifyDeviceSensorDataReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDeviceSensorData(tlcService, reqPkt, reqMsg);
                        }
                    }
                    break;
                }

                case ON_NEW_MICRO_CHAT_MSG: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyChatMessageReqMsg reqMsg = (Message.NotifyChatMessageReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onNewMicroChatMessage(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_NEW_MICRO_CHAT_GROUP_MSG: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyGroupChatMessageReqMsg reqMsg = (Message.NotifyGroupChatMessageReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onNewMicroChatGroupMessage(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;

                case ON_DEVICE_FRIEND_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyFriendChangedReqMsg reqMsg = (Message.NotifyFriendChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onDeviceFriendChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                    break;
                case ON_CHAT_GROUP_MEMBER_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyChatGroupMemberChangedReqMsg reqMsg = (Message.NotifyChatGroupMemberChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onChatGroupMemberChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                break;

                case ON_NOTIFY_SOS_CALL_ORDER_CHANGED: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifySosCallOrderChangedReqMsg reqMsg = (Message.NotifySosCallOrderChangedReqMsg) objs[2];

                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onNotifySosCallOrderChanged(tlcService, reqPkt, reqMsg);
                        }
                    }
                }
                break;


                case ON_NOT_PROCESSED_PKT: {
                    Object[] objs = (Object[]) msg.obj;
                    TlcService tlcService = (TlcService) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    OnResponseSetter responseSetter = (OnResponseSetter) objs[2];
                    synchronized (a.mTlcServiceEventCallbacks) {
                        for (TlcServiceEventCallback one : a.mTlcServiceEventCallbacks) {
                            one.onNotProcessedPkt(tlcService, reqPkt, responseSetter);
                        }
                    }
                }
                    break;


                case UPDATE_DEVICE_ONLINE_STATUS: {
                    Object[] objs = (Object[]) msg.obj;
                    String deviceId = (String) objs[0];
                    boolean online = (boolean) objs[1];
                    if (!TextUtils.isEmpty(deviceId)) {
                        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId, true);
                        if (deviceInfo != null) {
                            deviceInfo.setOnline(online);
                        }
                        EventBus.getDefault().postSticky(new Event.DeviceOnline(deviceId, online));
                    }
                    break;
                }


                //*********************** MainService 事件 *******************************/

                case ON_BIND_DEVICE_REQUEST_FROM_MAIN_SERVICE: {
                    Object[] objs = (Object[]) msg.obj;
                    String toUser = (String) objs[0];
                    Pkt reqPkt = (Pkt) objs[1];
                    Message.NotifyAdminBindDevReqMsg reqMsg = (Message.NotifyAdminBindDevReqMsg) objs[2];
                    Message.FetchUsrDevParticRspMsg usrDevPartic = (Message.FetchUsrDevParticRspMsg) objs[3];
                    synchronized (a.mMainServiceEventCallbacks) {
                        for (MainServiceEventCallback one : a.mMainServiceEventCallbacks) {
                            one.onBindRequest(toUser, reqPkt, reqMsg, usrDevPartic);
                        }
                    }
                }
                    break;

                default:
                    break;
            }
        }
    }

    OnEventListener mOnEventListener = new OnEventListener() {

        @Override
        public void onConnected() {
            mEvHandler.obtainMessage(EvHandler.ON_CONNECTED, mTlcService).sendToTarget();
        }

        @Override
        public void onDisconnected() {
            mEvHandler.obtainMessage(EvHandler.ON_DISCONNECTED, mTlcService).sendToTarget();
        }

        @Override
        public void onLoggedin(@NonNull String userId) {
            Object objs[] = new Object[]{mTlcService, userId};
            mEvHandler.obtainMessage(EvHandler.ON_LOGGED_IN, objs).sendToTarget();
        }

        @Override
        public void onLoggedout(@NonNull String userId) {
            Object objs[] = new Object[]{mTlcService, userId};
            mEvHandler.obtainMessage(EvHandler.ON_LOGGED_OUT, objs).sendToTarget();
        }

        @Override
        public void onBindDeviceRequest(@NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_BIND_DEVICE_REQUEST, objs).sendToTarget();
        }

        @Override
        public void onDeviceBind(@NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEVICE_BIND, objs).sendToTarget();
        }

        @Override
        public void onDeviceUnbind(@Nullable Pkt reqPkt, @Nullable Message.NotifyUserUnbindDevReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEVICE_UNBIND, objs).sendToTarget();
        }

        @Override
        public void onDevConfChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_CONF_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onDevConfSynced(@NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfSyncedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_CONF_SYNCED, objs).sendToTarget();
        }

        @Override
        public void onDevFenceChanged(@Nullable Pkt reqPkt, @Nullable Message.NotifyFenceChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_FENCE_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onDevSosChanged(@Nullable Pkt reqPkt, @Nullable Message.NotifySosChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_SOS_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onDevSosSynced(@Nullable Pkt reqPkt, @Nullable Message.NotifySosSyncedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_SOS_SYNCED, objs).sendToTarget();
        }

        @Override
        public void onDevContactChanged(@Nullable Pkt reqPkt, @Nullable Message.NotifyContactChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_CONTACT_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onDevContactSynced(@Nullable Pkt reqPkt, @Nullable Message.NotifyContactSyncedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_CONTACT_SYNCED, objs).sendToTarget();
        }

        @Override
        public void onAlarmClockChanged(@Nullable Pkt reqPkt, @Nullable Message.NotifyAlarmClockChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_ALARM_CLOCK_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onAlarmClockSynced(@Nullable Pkt reqPkt, @Nullable Message.NotifyAlarmClockSyncedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_ALARM_CLOCK_SYNCED, objs).sendToTarget();
        }

        @Override
        public void onClassDisableChanged(@Nullable Pkt reqPkt, @Nullable Message.NotifyClassDisableChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_CLASS_DISABLE_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onClassDisableSynced(@Nullable Pkt reqPkt, @Nullable Message.NotifyClassDisableSyncedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEV_CLASS_DISABLE_SYNCED, objs).sendToTarget();
        }

        @Override
        public void onSchoolGuardChanged(@Nullable Pkt reqPkt, @Nullable Message.NotifySchoolGuardChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_SCHOOL_GUARD_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onPraiseChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyPraiseChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_PRAISE_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onLocateS3(@NonNull Pkt reqPkt, @NonNull Message.LocateS3ReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_LOCATE_S3, objs).sendToTarget();
        }

        @Override
        public void onDeviceOnline(@Nullable Pkt reqPkt, @Nullable Message.NotifyOnlineStatusOfDevReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEVICE_ONLINE_STATUS, objs).sendToTarget();
        }

        @Override
        public void onDevicePosition(@NonNull Pkt reqPkt, @NonNull Message.NotifyDevicePositionReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEVICE_POSITION, objs).sendToTarget();
        }

        @Override
        public void onLocationModeChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyLocationModeChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_LOCATION_MODE_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onDeviceIncident(@Nullable Pkt reqPkt, @Nullable Message.NotifyIncidentReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEVICE_INCIDENT, objs).sendToTarget();
        }

        @Override
        public void onDeviceFriendChanged(@Nullable Pkt reqPkt, @Nullable Message.NotifyFriendChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEVICE_FRIEND_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onUsrDevAssocModified(@Nullable Pkt reqPkt, @Nullable Message.NotifyUsrDevAssocModifiedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_USR_DEV_ASSOC_MODIFIED, objs).sendToTarget();
        }

        @Override
        public void onFindDeviceS3(@NonNull Pkt reqPkt, @NonNull Message.FindDeviceS3ReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_FIND_DEVICE_S3, objs).sendToTarget();
        }

        @Override
        public void onTakePhotoS3(@NonNull Pkt reqPkt, @NonNull Message.TakePhotoS3ReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_TAKE_PHOTO_S3, objs).sendToTarget();
        }

        @Override
        public void onSimplexCallS3(@NonNull Pkt reqPkt, @NonNull Message.SimplexCallS3ReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_SIMPLEX_CALL_S3, objs).sendToTarget();
        }

        @Override
        public void onFetchDeviceSensorDataS3(@NonNull Pkt reqPkt, @NonNull Message.FetchDeviceSensorDataS3ReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_FETCH_DEVICE_SENSOR_DATA_S3, objs).sendToTarget();
        }

        @Override
        public void onDeviceSensorData(@NonNull Pkt reqPkt, @NonNull Message.NotifyDeviceSensorDataReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_DEVICE_SENSOR_DATA, objs).sendToTarget();
        }

        @Override
        public void onNewMicroChatMessage(@NonNull Pkt reqPkt, @NonNull Message.NotifyChatMessageReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_NEW_MICRO_CHAT_MSG, objs).sendToTarget();
        }

        @Override
        public void onNewMicroChatGroupMessage(@NonNull Pkt reqPkt, @NonNull Message.NotifyGroupChatMessageReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_NEW_MICRO_CHAT_GROUP_MSG, objs).sendToTarget();
        }

        @Override
        public void onNotifySMSAgentNewSMS(@NonNull Pkt reqPkt, @NonNull Message.NotifySMSAgentNewSMSReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_NEW_SMS_AGENT, objs).sendToTarget();
        }

        @Override
        public void onNotifyChatGroupMemberChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifyChatGroupMemberChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_CHAT_GROUP_MEMBER_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onNotifySosCallOrderChanged(@NonNull Pkt reqPkt, @NonNull Message.NotifySosCallOrderChangedReqMsg reqMsg) {
            Object objs[] = new Object[]{mTlcService, reqPkt, reqMsg};
            mEvHandler.obtainMessage(EvHandler.ON_NOTIFY_SOS_CALL_ORDER_CHANGED, objs).sendToTarget();
        }

        @Override
        public void onNotProcessedPkt(@NonNull Pkt reqPkt, @NonNull OnResponseSetter responseSetter) {
            Object objs[] = new Object[]{mTlcService, reqPkt, responseSetter};
            mEvHandler.obtainMessage(EvHandler.ON_NOT_PROCESSED_PKT, objs).sendToTarget();
        }
    };

    public void updateDeviceOnlineStatus(String deviceId, boolean online) {
        Object[] objs = new Object[] { deviceId, online };
        mEvHandler.obtainMessage(EvHandler.UPDATE_DEVICE_ONLINE_STATUS, objs).sendToTarget();
    }


    boolean mainServiceRebindOnDisconnected = true;
    private ServiceConnection mMainServiceConnection;
    private ServiceConnection MMainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            L.i(TAG, "MainService connected");
            try {
                MainService.MainServiceBinder mainServiceBinder = (MainService.MainServiceBinder) binder;
                MainService mainService = mainServiceBinder.getService();
                mainService.setOnTlcEventListener(mMainServiceEventListener);
            } catch (Exception e) {
                L.w(TAG, "mMainServiceConnection.onServiceConnected()", e);
                mainServiceRebindOnDisconnected = false;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            L.i(TAG, "MainService disconnected");
            if (mainServiceRebindOnDisconnected) {
                bindMainService();
            }
        }
    };

    private void bindMainService() {
        mMainServiceConnection = MMainServiceConnection;
        bindService(new Intent(this, MainService.class), mMainServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindMainService() {
        if (mMainServiceConnection != null)
            unbindService(mMainServiceConnection);
    }

    MainService.OnEventListener mMainServiceEventListener = new MainService.OnEventListener() {
        @Override
        public void onBindRequest(@NonNull String toUser, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg, @NonNull Message.FetchUsrDevParticRspMsg usrDevPartic) {
            Object objs[] = new Object[]{toUser, reqPkt, reqMsg, usrDevPartic};
            mEvHandler.obtainMessage(EvHandler.ON_BIND_DEVICE_REQUEST_FROM_MAIN_SERVICE, objs).sendToTarget();
        }
    };

    private final List<MainServiceEventCallback> mMainServiceEventCallbacks = new ArrayList<>();

    public void regMainServiceEventCallback(MainServiceEventCallback cb) {
        if (cb == null)
            return;
        synchronized (mMainServiceEventCallbacks) {
            mMainServiceEventCallbacks.add(cb);
        }
    }

    public void unregMainServiceEventCallback(MainServiceEventCallback cb) {
        synchronized (mMainServiceEventCallbacks) {
            mMainServiceEventCallbacks.remove(cb);
        }
    }


    public void fetchUserInfo(String userId) {
        if (mTlcService == null)
            return;
        mTlcService.exec(protocol.Message.FetchUserInfoReqMsg.newBuilder().setUserId(userId).build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.FetchUserInfoRspMsg rspMsg = response.getProtoBufMsg();
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                GreenUtils.saveUserInfo(rspMsg.getUserInfo());
                            } else {
                                L.w(TAG, "fetchUserInfo onResponse: " + rspMsg.getErrCode());
                            }
                        } catch (Exception e) {
                            L.e(TAG, "fetchUserInfo onResponse", e);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "fetchUserInfo onException", cause);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }

    public Event.GoogleAccessibility getGoogleAccessibility() {
        Event.GoogleAccessibility accessibility = EventBus.getDefault().getStickyEvent(Event.GoogleAccessibility.class);
        if (accessibility == null)
            return Event.GoogleAccessibility.UNKNOWN;
        return accessibility;
    }
}
