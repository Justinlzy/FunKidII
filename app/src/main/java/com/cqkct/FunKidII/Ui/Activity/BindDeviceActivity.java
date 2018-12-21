package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.zxing.capture.CaptureBindNumberActivity;

import java.lang.ref.WeakReference;

import protocol.Message;


/**
 * Created by justin on 2017/8/14.
 */

public class BindDeviceActivity extends BaseActivity {
    private static final String TAG = BindDeviceActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;
    private static final int ACTIVITY_REQUEST_BIND_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bound_watch);
        setTitleBarTitle(R.string.title_bind_device);
        init();
    }

    private void init() {
    }

    @Override
    public void onTitleBarClick(View v) {
        switch (v.getId()) {
            case R.id.title_bar_left_icon:
                switchUser();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
//            切换账户
            case R.id.title_bar_left_icon:
                switchUser();
                break;
            case R.id.bt_switch_user:
                switchUser();
                break;
            case R.id.bt_to_bind_device:
                Intent intent = new Intent(BindDeviceActivity.this, InputBindNumberActivity.class);
                intent.putExtra("ACTIVITY_MODE", "LOGIN_BIND");//从登陆界面跳转去绑定
                intent.putExtra(InputBindNumberActivity.PARAM_KEY_MODE, InputBindNumberActivity.PARAM_VALUE_MODE_BIND_DEVICE);
                startActivityForResult(intent, ACTIVITY_REQUEST_BIND_REQUEST);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:
                // TODO: 摄像头权限
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_REQUEST_BIND_REQUEST:
                if (resultCode == RESULT_OK) {
                    int bindStatus = data.getIntExtra(CaptureBindNumberActivity.RESULT_KEY_BIND_STATUS, CaptureBindNumberActivity.RESULT_VALUE_BIND_STATUS_WAIT);
                    switch (bindStatus) {
                        case InputBindNumberActivity.RESULT_VALUE_BIND_STATUS_OK:
                        case InputBindNumberActivity.RESULT_VALUE_BIND_STATUS_ALREADY_BOUND:

                            // 绑定成功，进入 Main activity
                            mTaskHandler.sendEmptyMessage(TaskHandler.DELAY_FINISH);
                            break;
                        default:
                            break;
                    }
                }
        }
    }

    private void switchUser() {
        if (isTaskRoot()) {
            try {
                if (mTlcService != null) {
                    mTlcService.logout();
                }
            } catch (Exception e) {
                L.e(TAG, "mTlcService.logout() error", e);
                getPreferencesWrapper().setCanAutoLogin(false);
            }
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
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
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        try {
            mTaskHandler.sendEmptyMessageDelayed(TaskHandler.DELAY_FINISH, 100);
        } catch (Exception e) {
            L.d(TAG, "onDeviceBind() process failure", e);
        }
    }

    private TaskHandler mTaskHandler = new TaskHandler(this);
    private static class TaskHandler extends Handler {
        static final int DELAY_FINISH = 0;

        private WeakReference<BindDeviceActivity> mA;

        TaskHandler(BindDeviceActivity a) {
            mA = new WeakReference<>(a);
        }

        private boolean finished = false;

        @Override
        public void handleMessage(android.os.Message msg) {
            BindDeviceActivity a = mA.get();
            if (a == null)
                return;

            switch (msg.what) {
                case DELAY_FINISH:
                    if (finished)
                        break;
                    finished = true;
                    a.startActivity(new Intent(a, MainActivity.class));
                    a.setResult(RESULT_OK);
                    a.finish();
                    break;
                default:
                    break;
            }
        }
    }
}
