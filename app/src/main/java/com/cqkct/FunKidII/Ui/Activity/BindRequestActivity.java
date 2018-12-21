package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class BindRequestActivity extends BaseActivity {
    private static final String TAG = BindRequestActivity.class.getSimpleName();

    public static final String PARAM_KEY_BINDREQ_SEQ = "req_pkt";
    public static final String PARAM_KEY_BINDREQ_PROTOBUF_MSG = "req_protobuf_msg";
    public static final String PARAM_KEY_FROM_USER_INFO_PROTOBUF_MSG = "from_user_info_protobuf_msg";
    public static final String PARAM_KEY_TO_USER_ID = "to_user_id";
    public static final String PARAM_KEY_FROM_MESSAGE_CENTER = "param_key_from_message_center";

    private String seq;
    private Message.NotifyAdminBindDevReqMsg bindDevReqMsg;
    private Message.FetchUsrDevParticRspMsg usrDevParticRspMsg;
    private boolean isMessageCenter = false;


    private TextView textView;

    private int mCloseEnterAnimation;
    private int mCloseExitAnimation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity_info);

        if (!getParams())
            return;

        initView();

        initAnim();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(mCloseEnterAnimation, mCloseExitAnimation);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.button_positive:
                agree();
                break;
            case R.id.button_negative:
                finish();
                break;
            default:
                break;
        }
    }

    private boolean getParams() {
        Intent intent = getIntent();
        seq = intent.getStringExtra(PARAM_KEY_BINDREQ_SEQ);
        bindDevReqMsg = (Message.NotifyAdminBindDevReqMsg) intent.getSerializableExtra(PARAM_KEY_BINDREQ_PROTOBUF_MSG);
        usrDevParticRspMsg = (Message.FetchUsrDevParticRspMsg) intent.getSerializableExtra(PARAM_KEY_FROM_USER_INFO_PROTOBUF_MSG);
        isMessageCenter = intent.getBooleanExtra(PARAM_KEY_FROM_MESSAGE_CENTER, false);
        String toUserId = intent.getStringExtra(PARAM_KEY_TO_USER_ID);
        if (TextUtils.isEmpty(seq) || bindDevReqMsg == null || usrDevParticRspMsg == null || TextUtils.isEmpty(toUserId)) {
            L.e(TAG, "Invalid params");
            finish();
            return false;
        }

        if (!toUserId.equals(mUserId)) {
            L.w(TAG, "User already logged out!!!");
            finish();
            return false;
        }

        return true;
    }

    private void initView() {
        findViewById(R.id.title_layout).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.dialog_title)).setText(R.string.bind_device_request_title);
        textView = findViewById(R.id.message);
        String babyName = usrDevParticRspMsg.getBaby().getName();
        if (TextUtils.isEmpty(babyName))
            babyName = getString(R.string.baby);
        String bindDeviceRequestContent = getString(
                R.string.bind_device_request_content,
                RelationUtils.decodeRelation(this, bindDevReqMsg.getUsrDevAssoc().getRelation()),
                usrDevParticRspMsg.getUserInfo().getPhone(),
                babyName,
                bindDevReqMsg.getUsrDevAssoc().getDeviceId()
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(bindDeviceRequestContent, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textView.setText(Html.fromHtml(bindDeviceRequestContent));
        }

        Button button = findViewById(R.id.button_positive);
        button.setText(R.string.bind_device_request_agree);
        button.setOnClickListener(getDebouncedOnClickListener());
        button = findViewById(R.id.button_negative);
        button.setText(R.string.bind_device_request_ignore);
        button.setOnClickListener(getDebouncedOnClickListener());
    }

    private void initAnim() {
        TypedArray activityStyle = getTheme().obtainStyledAttributes(new int[]{android.R.attr.windowAnimationStyle});
        int windowAnimationStyleResId = activityStyle.getResourceId(0, 0);
        activityStyle.recycle();
        activityStyle = getTheme().obtainStyledAttributes(windowAnimationStyleResId, new int[]{android.R.attr.activityCloseEnterAnimation, android.R.attr.activityCloseExitAnimation});
        mCloseEnterAnimation = activityStyle.getResourceId(0, 0);
        mCloseExitAnimation = activityStyle.getResourceId(1, 0);
        activityStyle.recycle();
    }

    private void agree() {
        String userId = mUserId;
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            popErrorDialog(R.string.submit_temporarily_unable);
            return;
        }
        if (bindDevReqMsg == null)
            return;
        Message.UsrDevAssoc.Permission permission = Message.UsrDevAssoc.Permission.forNumber(Message.UsrDevAssoc.Permission.ADMIN_VALUE);
        Message.UsrDevAssoc usrDevAssoc = bindDevReqMsg.getUsrDevAssoc().toBuilder().setPermission(permission).build();
        Message.NotifyAdminBindDevRspMsg rspMsg = Message.NotifyAdminBindDevRspMsg.newBuilder()
                .setUsrDevAssoc(usrDevAssoc)
                .setErrCode(Message.ErrorCode.SUCCESS)
                .build();
        send(rspMsg, new Pkt.Seq(seq), new TlcService.OnSendListener() {
            @Override
            public void onSuccess(@NonNull Pkt request) {

                EventBus.getDefault().post(new Event.ShouldUpdateFamilyGroupMember(usrDevAssoc));
                GreenUtils.upDateNotifyMessageEntity(seq);
                if (isMessageCenter) {
                    setResult(RESULT_OK);
                }
                finish();
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.e(TAG, "ChoicePermissionDialogFragment.onClick() -> send()", cause);
                if (cause instanceof TimeoutException) {
                    popErrorDialog(R.string.submit_timeout);
                } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                    popErrorDialog(R.string.network_quality_poor);
                } else if (cause instanceof NotYetLoginException) {
                    popErrorDialog(R.string.network_quality_poor);
                } else if (cause instanceof WaitThirdStageTimeoutException) {
                } else {
                    popErrorDialog(R.string.submit_failure);
                }
            }
        });
    }
}