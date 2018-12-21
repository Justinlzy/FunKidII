package com.cqkct.FunKidII.Ui.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.upgreade.Upgrade;

import org.greenrobot.eventbus.EventBus;

import protocol.Message;

public class ServerApiNotCompatDialogActivity extends BaseActivity {
    private static final String TAG = ServerApiNotCompatDialogActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    private Upgrade mUpgrade = new Upgrade(ServerApiNotCompatDialogActivity.this, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

    private int mCloseEnterAnimation;
    private int mCloseExitAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity_server_api_not_compat);

        initView();

        initAnim();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(mCloseEnterAnimation, mCloseExitAnimation);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mUpgrade.upgradeGuide("");
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    mUpgrade.upgradeGuide("");
                }
                break;
            default:
                break;
        }
    }

    private void initView() {
        ((TextView) findViewById(R.id.dialog_title)).setText(R.string.app_setting_version_too_low);
        ((TextView) findViewById(R.id.message)).setText(R.string.app_setting_version_too_low_update_now);
        ((Button) findViewById(R.id.button_positive)).setText(R.string.ok);
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
            case R.id.button_positive: {
                Event.ExtrudedLoggedOut stickyExtrudedLoggedOut = EventBus.getDefault().getStickyEvent(Event.ExtrudedLoggedOut.class);
                if(stickyExtrudedLoggedOut != null) {
                    EventBus.getDefault().removeStickyEvent(stickyExtrudedLoggedOut);
                }

                mUpgrade.upgradeGuide("");
            }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        }

        return super.onKeyDown(keyCode, event);
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