package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.DotPagerAdapter;
import com.cqkct.FunKidII.Ui.view.DotIndicator;
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
 * Created by justin on 2017/8/25.
 */

public class GuideActivity extends BaseActivity {
    private static final String TAG = GuideActivity.class.getSimpleName();

    private ViewPager viewPager;
    private TextView bt_startAPP;
    private DotIndicator dot_indicator;

    private EvHandler mEvHandler = new EvHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        init();
    }

    private void init() {
        bt_startAPP = findViewById(R.id.guide_start_app);

        dot_indicator = findViewById(R.id.dot_indicator);

        viewPager = findViewById(R.id.rl_guide);

        DotPagerAdapter adapter = new DotPagerAdapter(this);

        viewPager.setAdapter(adapter);
        dot_indicator.setViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 2) {
                    bt_startAPP.setVisibility(View.VISIBLE);
                    bt_startAPP.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mEvHandler.sendEmptyMessage(EvHandler.ON_START_APP_CLICK);
                        }
                    });
                }
                if (position == 0 || position == 1) {
                    bt_startAPP.setVisibility(View.GONE);
                }

            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        mEvHandler.sendEmptyMessage(EvHandler.ON_INIT);
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
        static final int ON_START_APP_CLICK = 0;
        static final int SET_START_ACTIVITY_INTENT = 1;
        static final int ON_LOGGED_IN = 2;
        static final int DO_QUERY_LOCAL_DB = 3;

        private boolean preparing = true;
        private Class<? extends AppCompatActivity> toActivity = null;
        boolean startAppClick = false;
        boolean loggedIn = false;

        private WeakReference<GuideActivity> mA;

        EvHandler(GuideActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            GuideActivity a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {
                case ON_INIT:
                    prepareActivityIntent(a);
                    break;

                case ON_START_APP_CLICK:
                    startAppClick = true;
                    if (toActivity != null) {
                        a.jumpActivity(toActivity);
                    } else if (!preparing) {
                        queryLocalDb(a, a.mUserId);
                    } else {
                        setStartActivity(LoginActivity.class);
                    }
                    break;

                case SET_START_ACTIVITY_INTENT: {
                    toActivity = (Class<? extends AppCompatActivity>) msg.obj;
                    if (startAppClick) {
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

        private void prepareActivityIntent(GuideActivity a) {
            if (!a.getPreferencesWrapper().canAutoLogin()) {
                setStartActivity(LoginActivity.class);
                return;
            }

            // 判断需要跳转到的 activity
            queryServer(a, null, a.mUserId);
        }

        private void queryServer(@NonNull final GuideActivity a, TlcService tlcService, String user) {
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
            a.exec(reqMsg, new TlcService.OnExecListener() {
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

        private void queryLocalDb(@NonNull GuideActivity a, String userId) {
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
