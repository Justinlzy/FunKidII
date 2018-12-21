package com.cqkct.FunKidII.Ui.Activity;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.zxing.capture.CaptureBindNumberActivity;

import java.lang.ref.WeakReference;

import protocol.Message;

public class WaitDeviceBindSuccessActivity extends BaseActivity {
    private static final String TAG = WaitDeviceBindSuccessActivity.class.getSimpleName();

    public static final String PARAM_KEY_WAIT_DEVIDE = "wait_device";
    public static final String RESULT_KEY_BIND_NUM = "bind_num";

    private String mWaitDeviceId;
    private Intent resultIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_device_bind_success);
        setTitleBarTitle(R.string.bind_device);
//        闪烁动画
        initAnimation();
        mWaitDeviceId = getIntent().getStringExtra(PARAM_KEY_WAIT_DEVIDE);
        resultIntent = new Intent(WaitDeviceBindSuccessActivity.class.getSimpleName());
        resultIntent.putExtra(RESULT_KEY_BIND_NUM, mWaitDeviceId);

        try {
            if (!GreenUtils.getBabyEntityDao().queryBuilder()
                    .where(BabyEntityDao.Properties.UserId.eq(mUserId))
                    .where(BabyEntityDao.Properties.DeviceId.eq(mWaitDeviceId))
                    .build().list().isEmpty()) {
                // 已经绑定成功
                L.i(TAG, "device (" + mWaitDeviceId + " already bound, we finish()");
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }
        } catch (Exception e) {
            L.w(TAG, "query whether already bound error, we finish()", e);
            finish();
            return;
        }

        setResult(RESULT_CANCELED, resultIntent);
    }

    public void initAnimation() {
        LinearLayout ll_bot = findViewById(R.id.ll_bot);
        AlphaAnimation alphaAnimation1 = new AlphaAnimation(0.1f, 1.0f);
        alphaAnimation1.setDuration(500);
        alphaAnimation1.setRepeatCount(Animation.INFINITE);
        alphaAnimation1.setRepeatMode(Animation.REVERSE);
        ll_bot.setAnimation(alphaAnimation1);
        alphaAnimation1.start();
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        protocol.Message.UsrDevAssoc usrDevAssoc = reqMsg.getUsrDevAssoc();
        if (TextUtils.isEmpty(usrDevAssoc.getUserId()) || TextUtils.isEmpty(usrDevAssoc.getDeviceId())) {
            L.w(TAG, "onDeviceBind: NotifyUsrBindDevResultReqMsg: .getUdi() data invalid");
            return;
        }

        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId)
                && (TextUtils.isEmpty(mWaitDeviceId) || usrDevAssoc.getDeviceId().equals(mWaitDeviceId))) {
            try {
                mTaskHandler.sendEmptyMessageDelayed(TaskHandler.DELAY_FINISH, 100);
            } catch (Exception e) {
                L.e(TAG, "onDeviceBind() process failure", e);
            }
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.tv_switchUser:
                try {
                    assert mTlcService != null;
                    mTlcService.logout();
                } catch (Exception e) {
                    L.e(TAG, "mTlcService.logout() error", e);
                    getPreferencesWrapper().setCanAutoLogin(false);
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishBindActivity();
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return false;
    }

    private TaskHandler mTaskHandler = new TaskHandler(this);

    private static class TaskHandler extends Handler {
        static final int DELAY_FINISH = 0;

        private WeakReference<WaitDeviceBindSuccessActivity> mA;

        TaskHandler(WaitDeviceBindSuccessActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            WaitDeviceBindSuccessActivity a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {
                case DELAY_FINISH:
                    a.setResult(RESULT_OK, a.resultIntent);
                    a.finish();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        finishBindActivity();
        super.onDestroy();
    }

    private void finishBindActivity() {
        if (null != InputBindNumberActivity.ActivityInputBindNumber)
            InputBindNumberActivity.ActivityInputBindNumber.finish();
        if (null != CaptureBindNumberActivity.ActivityCapture)
            CaptureBindNumberActivity.ActivityCapture.finish();
    }
}