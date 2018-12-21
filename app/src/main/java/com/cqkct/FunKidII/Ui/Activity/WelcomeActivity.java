package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.lang.ref.WeakReference;
import java.util.List;

import protocol.Message;

/**
 * Created by Justin on 2017/7/25.
 */

public class WelcomeActivity extends BaseActivity {
    private static final String TAG = WelcomeActivity.class.getSimpleName();

    private EvHandler mEvHandler = new EvHandler(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //hit the toolbar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_welcome);
        init();
        mEvHandler.sendEmptyMessage(EvHandler.ON_INIT);
        //判断启动页是否为当前任务栈的根Activity，
        // 如果不是根Activity则finish掉。语意上来讲，一个任务栈的根Activity应是启动页，这样也符合正常的应用场景；
        // 如果不是根Activity，那么说明出现了异常启动，应当将该启动页销毁掉，显示原有任务栈。
        if (!isTaskRoot())
            finish();

        // 选择 APP 使用地区
        selectAppArea();
    }

    @Override
    protected boolean isImmersionBarEnabled() {
        return false;
    }

    public void init() {
        ImageView iv_welcome = findViewById(R.id.iv_welcome);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.01f, 1.0f);//透明度从1%~1
        alphaAnimation.setDuration(1000L * 3);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mEvHandler.sendEmptyMessage(EvHandler.ON_ANIMATION_END);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        iv_welcome.setAnimation(alphaAnimation);
    }


    private void jumpActivity(Class<? extends AppCompatActivity> a) {
        if (isFinishing())
            return;
        if (a == null) {
            a = LoginActivity.class;
        }
        startActivity(new Intent(this, a));
        finish();
    }

    private static class EvHandler extends Handler {
        static final int ON_INIT = -1;
        static final int ON_ANIMATION_END = 0;
        static final int SET_START_ACTIVITY_INTENT = 1;
        static final int ON_LOGGED_IN = 2;
        static final int DO_QUERY_LOCAL_DB = 3;

        private boolean preparing = true;
        private Class<? extends AppCompatActivity> toActivity = null;
        boolean animationEnded = false;
        boolean loggedIn = false;

        private WeakReference<WelcomeActivity> mA;

        EvHandler(WelcomeActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            WelcomeActivity a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {
                case ON_INIT:
                    prepareActivityIntent(a);
                    break;

                case ON_ANIMATION_END:
                    animationEnded = true;
                    if (toActivity != null) {
                        a.jumpActivity(toActivity);
                    } else {
                        if (!preparing)
                            queryLocalDb(a, a.mUserId);
                    }
                    break;

                case SET_START_ACTIVITY_INTENT: {
                    toActivity = (Class<? extends AppCompatActivity>) msg.obj;
                    if (animationEnded) {
                        a.jumpActivity(toActivity);
                    }
                }
                break;

                case ON_LOGGED_IN:
                    loggedIn = true;
                    if (toActivity == null && !preparing) {
                        Object[] objs = (Object[]) msg.obj;
                        TlcService tlcService = (TlcService) objs[0];
                        String user = (String) objs[1];
                        queryServer(a, tlcService, user);
                    }
                    break;

                case DO_QUERY_LOCAL_DB: {
                    String user = (String) msg.obj;
                    queryLocalDb(a, user);
                }
                break;

                default:
                    break;
            }
        }

        private void setStartActivity(@NonNull Class<? extends AppCompatActivity> aClass) {
            preparing = false;
            obtainMessage(SET_START_ACTIVITY_INTENT, aClass).sendToTarget();
        }

        private void prepareActivityIntent(WelcomeActivity a) {
            try {
                PackageInfo info = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
                int currentVersion = info.versionCode;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
                String VERSION_KEY = "VERSION";
                int lastVersion = prefs.getInt(VERSION_KEY, 0);
                if (currentVersion > lastVersion) {
                    L.i(TAG, "First time user");
                    //如果当前版本大于上次版本，该版本属于第一次启动
                    //将当前版本写入preference中，则下次启动的时候，据此判断，不再为首次启动
                    prefs.edit().putInt(VERSION_KEY, currentVersion).apply();
                    //启动帮助Activity
                    setStartActivity(GuideActivity.class);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!a.getPreferencesWrapper().canAutoLogin()) {
                setStartActivity(LoginActivity.class);
                return;
            }

            // 判断需要跳转到的 activity
            queryServer(a, null, a.mUserId);
        }

        private void queryServer(@NonNull final WelcomeActivity a, TlcService tlcService, String user) {
            if (TextUtils.isEmpty(user)) {
                user = a.mUserId;
                if (TextUtils.isEmpty(user)) {
                    // 等待登录成功
                    preparing = false;
                    return;
                }
            }

            if (tlcService == null) {
                tlcService = a.mTlcService;
                if (tlcService == null) {
                    preparing = false;
                    return;
                }
            }

            if (!a.getPreferencesWrapper().canAutoLogin()) {
                preparing = false;
                return;
            }

            if (!tlcService.isConnectivityValid()) {
                // 无网络，查看本地数据库
                queryLocalDb(a, user);
                preparing = false;
                return;
            }

            final String userId = user;

            Message.FetchDeviceListReqMsg reqMsg = Message.FetchDeviceListReqMsg.newBuilder()
                    .setUserId(userId)
                    .build();
            tlcService.exec(reqMsg, new TlcService.OnExecListener() {
                @Override
                public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                    try {
                        Message.FetchDeviceListRspMsg rspMsg = response.getProtoBufMsg();
                        L.d(TAG, "exec(" + Message.FetchDeviceListReqMsg.class.getSimpleName() + ") -> rsp：" + rspMsg);
                        if (rspMsg.getErrCode() != Message.ErrorCode.SUCCESS) {
                            L.e(TAG, "exec(" + Message.FetchDeviceListReqMsg.class.getSimpleName() + ") -> rsp failure");
                            // 获取设备列表失败，从本地数据库判断
                            obtainMessage(DO_QUERY_LOCAL_DB, userId).sendToTarget();
                        }
                        List<Message.UsrDevAssoc> usrDevInfos = rspMsg.getUsrDevAssocList();
                        synchronized (this) {
                            if (usrDevInfos.isEmpty()) {  //设备信息为空
                                setStartActivity(BindDeviceActivity.class);
                            } else {
                                GreenUtils.saveDevicesOfUserFromFetchAsync(usrDevInfos);
                                setStartActivity(MainActivity.class);
                            }
                        }
                    } catch (Exception e) {
                        L.e(TAG, "exec(" + Message.FetchDeviceListReqMsg.class.getSimpleName() + ") ->" +
                                " rsp is not " + Message.FetchDeviceListRspMsg.class.getSimpleName() + ": " + response, e);
                        // 获取设备列表失败，从本地数据库判断
                        obtainMessage(DO_QUERY_LOCAL_DB, userId).sendToTarget();
                    }

                    return false;
                }

                @Override
                public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                    L.e(TAG, "exec(" + Message.FetchDeviceListReqMsg.class.getSimpleName() + ") -> onException", cause);
                    // 获取设备列表失败，从本地数据库判断
                    obtainMessage(DO_QUERY_LOCAL_DB, userId).sendToTarget();
                }

                @Override
                public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    // never to here for this exec
                }
            });

        }

        private void queryLocalDb(@NonNull WelcomeActivity a, String userId) {
            if (TextUtils.isEmpty(userId)) {
                setStartActivity(LoginActivity.class);
                return;
            }
            List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder()
                    .where(BabyEntityDao.Properties.UserId.eq(userId))
                    .build().list();
            if (!list.isEmpty()) {
                BabyEntity isSelected = null;
                for (BabyEntity one : list) {
                    if (one.getIs_select()) {
                        isSelected = one;
                        break;
                    }
                }
                if (isSelected == null) {
                    isSelected = list.get(0);
                    isSelected.setIs_select(true);
                    GreenUtils.getBabyEntityDao().update(isSelected);
                }
                App.getInstance().setCurrentBaby(isSelected);

                setStartActivity(MainActivity.class);
            } else if (loggedIn) {
                setStartActivity(BindDeviceActivity.class);
            } else {
                setStartActivity(LoginActivity.class);
            }
        }
    }

    @Override
    public void onLoggedin(TlcService tlcService, @NonNull String userId, boolean isSticky) {
        super.onLoggedin(tlcService, userId, isSticky);
        Object[] objs = new Object[]{tlcService, userId};
        mEvHandler.obtainMessage(EvHandler.ON_LOGGED_IN, objs).sendToTarget();
    }

    @Override
    public void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky) {
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    public void onBindRequest(@NonNull String toUser, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg, @NonNull Message.FetchUsrDevParticRspMsg usrDevPartic) {
    }

    @Override
    public boolean shouldShowExtrudedLoggedOut() {
        return false;
    }

    protected boolean shouldShowServerApiNotCompat() {
        return false;
    }
}
