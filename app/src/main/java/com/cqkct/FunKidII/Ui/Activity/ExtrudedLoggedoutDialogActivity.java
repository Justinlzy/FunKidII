package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;

import protocol.Message;

public class ExtrudedLoggedoutDialogActivity extends BaseActivity {
    private static final String TAG = ExtrudedLoggedoutDialogActivity.class.getSimpleName();

    private int mCloseEnterAnimation;
    private int mCloseExitAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity_info);

        initView();

        initAnim();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(mCloseEnterAnimation, mCloseExitAnimation);
    }

    private void initView() {
        findViewById(R.id.title_layout).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.dialog_title)).setText(R.string.register_login_off_line_hint);
        ((TextView) findViewById(R.id.message)).setText(R.string.register_login_other_phone_login);
        Button button = findViewById(R.id.button_positive);
        button.setText(R.string.register_login_again_login);
        button.setOnClickListener(getDebouncedOnClickListener());
        button = findViewById(R.id.button_negative);
        button.setText(R.string.register_login_out);
        button.setOnClickListener(getDebouncedOnClickListener());
    }

    private void initAnim() {
        TypedArray activityStyle = getTheme().obtainStyledAttributes(new int[] {android.R.attr.windowAnimationStyle});
        int windowAnimationStyleResId = activityStyle.getResourceId(0, 0);
        activityStyle.recycle();
        activityStyle = getTheme().obtainStyledAttributes(windowAnimationStyleResId, new int[] {android.R.attr.activityCloseEnterAnimation, android.R.attr.activityCloseExitAnimation});
        mCloseEnterAnimation = activityStyle.getResourceId(0, 0);
        mCloseExitAnimation = activityStyle.getResourceId(1, 0);
        activityStyle.recycle();
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.button_positive:
                App.removeStickyExtrudedLoggedOutEvent();
                try {
                    if (!mTlcService.relogin()) {
                        mTlcService.logout();
                    }
                } catch (Exception e) {
                    L.e(TAG, "mTlcService.logout() error", e);
                }
                finish();
                break;
            case R.id.button_negative:
                App.removeStickyExtrudedLoggedOutEvent();
                try {
                    mTlcService.logout();
                } catch (Exception e) {
                    L.e(TAG, "mTlcService.logout() error", e);
                }
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);
    }

    @Override
    public void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky) {
    }

    @Override
    public void onBindRequest(@NonNull String toUser, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg, @NonNull Message.FetchUsrDevParticRspMsg usrDevPartic) {
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    public boolean shouldShowExtrudedLoggedOut() {
        return false;
    }

    protected boolean shouldShowServerApiNotCompat() {
        return false;
    }
}