package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.zxing.capture.CaptureBindNumberActivity;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/9/1.
 */

public class InputBindNumberActivity extends BaseActivity {
    private static final String TAG = InputBindNumberActivity.class.getSimpleName();

    public static final String RESULT_ACTION = InputBindNumberActivity.class.getName();
    public static final String RESULT_KEY_BIND_NUM = "bind_num";

    private static final int ACTIVITY_REQUEST_BIND_REQUEST = 1;

    /**
     * 工作模式：扫描得到绑定号
     */
    public static final int PARAM_VALUE_MODE_GET_BIND_NUM = 0;
    /**
     * 绑定状态：绑定请求已成功发送到管理员
     */
    public static final int RESULT_VALUE_BIND_STATUS_WAIT = 0;
    /**
     * 绑定状态：绑定成功
     */
    public static final int RESULT_VALUE_BIND_STATUS_OK = 1;
    /**
     * 绑定状态：该设备已经绑定
     */
    public static final int RESULT_VALUE_BIND_STATUS_ALREADY_BOUND = 2;
    /**
     * 绑定设备状态：mode 为 PARAM_VALUE_MODE_BIND_DEVICE 时有效
     */
    public static final String RESULT_KEY_BIND_STATUS = "bind_status";
    private static final int ACTIVITY_REQUEST_MANUAL_INPUT_BIND_NUM = 0;
    private static final int ACTIVITY_REQUEST_SELECT_RELATION_AND_DO_BIND = 1;
    private static final int ACTIVITY_REQUEST_WAIT_BIND_RESULT = 2;

    /**
     * 工作模式
     */
    public static final String PARAM_KEY_MODE = "mode";

    /**
     * 工作模式：扫描绑定号并绑定设备
     */
    public static final int PARAM_VALUE_MODE_BIND_DEVICE = 1;
    /**
     * 与 PARAM_KEY_MODE 取值一致
     */
    public static final String RESULT_KEY_MODE = "mode";
    /**
     * 想要显示的 Activity title
     */
    public static final String PARAM_KEY_WINDOW_TITLE = "title";
    private int workMode = PARAM_VALUE_MODE_GET_BIND_NUM;

    private ImageView btn_bind_number_clean;
    private TextView bt_confirm;
    private EditText et_inputBindNumber;
    public static InputBindNumberActivity ActivityInputBindNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bind_hand_input);
        setTitleBarTitle(R.string.bind_by_bindnumber);
        ActivityInputBindNumber = this;
        init();
        processParams();
    }

    private void init() {
        btn_bind_number_clean = findViewById(R.id.btn_bind_number_clean);

        bt_confirm = findViewById(R.id.bt_bind);

        et_inputBindNumber = (EditText) findViewById(R.id.et_bindNumber);

        et_inputBindNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!StringUtils.isEmpty(et_inputBindNumber.getText().toString())) {
                    btn_bind_number_clean.setVisibility(View.VISIBLE);
                    bt_confirm.setEnabled(true);
                } else {
                    btn_bind_number_clean.setVisibility(View.INVISIBLE);
                    bt_confirm.setEnabled(false);
                }
            }
        });
    }

    private void processParams() {
        Intent intent = getIntent();
        if (intent == null)
            return;
        workMode = intent.getIntExtra(PARAM_KEY_MODE, PARAM_VALUE_MODE_GET_BIND_NUM);
        String title = intent.getStringExtra(PARAM_KEY_WINDOW_TITLE);
        if (!TextUtils.isEmpty(title)) {
            setTitleBarTitle(title);
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
//               绑定号绑定
            case R.id.bt_bind:
                checkBindDevice(et_inputBindNumber.getText().toString());
                break;
//                扫描二维码绑定
            case R.id.btn_bind_device_scan: {
                Intent intent = new Intent(InputBindNumberActivity.this, CaptureBindNumberActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(CaptureBindNumberActivity.PARAM_KEY_MODE, CaptureBindNumberActivity.PARAM_VALUE_MODE_BIND_DEVICE);
                startActivityForResult(intent, ACTIVITY_REQUEST_BIND_REQUEST);

            }
            break;
        }

    }

    @Override
    protected void onDestroy() {
        ActivityInputBindNumber = null;
        super.onDestroy();
    }

    private boolean isLoginBind() {
        Intent intent1 = getIntent();
        Bundle extras = intent1.getExtras();
        String activity_mode = null;
        if (extras != null)
            activity_mode = extras.getString("ACTIVITY_MODE");

        if (activity_mode != null) {
            return activity_mode.equals("LOGIN_BIND");
        }
        return false;
    }

    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_bind_number_clean:
                et_inputBindNumber.setText("");
                break;
        }
    }

    private void checkBindDevice(final String bindNum) {
        L.v(TAG, "checkBindDevice: bindNum:" + bindNum);
        if (StringUtils.isEmpty(bindNum)) {
            L.e(TAG, "checkBindDevice: bindNum is null");
            return;
        }
        popWaitingDialog(R.string.please_wait);
        Message.CheckDeviceReqMsg checkDeviceRspMsg = Message.CheckDeviceReqMsg.newBuilder()
                .setDeviceId(bindNum)
                .build();
        exec(
                checkDeviceRspMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.CheckDeviceRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "checkBindDevice() -> exec() -> onResponse() " + rspMsg);
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    checkIsAlreadyBound(bindNum);
                                    break;
                                    //设备未激活
                                case INACTIVATED:
                                    popInfoDialog(R.string.bind_device_inactivated);
                                    break;
                                case TIMEOUT:
                                    popErrorDialog(R.string.request_timed_out);
                                    break;
                                    //设备无效
                                case NO_DEVICE:
                                    popErrorDialog(R.string.invalid_device);
                                    break;

                            }
                        } catch (Exception e) {
                            L.e(TAG, "checkBindDevice() -> exec() -> onResponse() process failure", e);
                            popErrorDialog(R.string.unknown_error);
                        }

                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "checkBindDevice() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.request_timed_out);
                        } else {
                            popErrorDialog(R.string.request_failed);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                        // never to here for this exec
                    }
                }
        );
    }

    private void checkIsAlreadyBound(final String bindNum) {
        String userId = mUserId;
        if (TextUtils.isEmpty(mUserId) || TextUtils.isEmpty(bindNum)) {
            L.w(TAG, "checkIsAlreadyBound: userId or bindNum is empty");
            return;
        }
        Message.FetchDeviceListReqMsg reqMsg = Message.FetchDeviceListReqMsg.newBuilder()
                .setUserId(userId)
                .build();
        exec(reqMsg, new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    Message.FetchDeviceListRspMsg rspMsg = response.getProtoBufMsg();
                    for (Message.UsrDevAssoc one : rspMsg.getUsrDevAssocList()) {
                        if (one.getDeviceId().equals(bindNum)) {
                            popInfoDialog(R.string.device_already_bound);
                            return true;
                        }
                    }
                    dismissDialog();
//                    result(bindNum); 不返回 在当前页面执行后续方法
                    onBindNumIsvalid(bindNum, false);
//                    doBind(bindNum);
                } catch (Exception e) {
                    L.e(TAG, "checkIsAlreadyBound() -> exec() -> onResponse() process failure", e);
                    popErrorDialog(R.string.unknown_error);
                }
                return true;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.e(TAG, "checkIsAlreadyBound() -> exec() -> onException()", cause);
                if (cause instanceof TimeoutException) {
                    popErrorDialog(R.string.request_timed_out);
                } else {
                    popErrorDialog(R.string.request_failed);
                }
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

            }
        });
    }

    private void onBindNumIsvalid(String bindNum, boolean checkWhetherBound) {

        switch (workMode) {
            case PARAM_VALUE_MODE_BIND_DEVICE:
                if (checkWhetherBound) {
                    checkIsAlreadyBound(bindNum);
                } else {
                    doBind(bindNum);
                }
                break;
            default:
                doBind(bindNum);
//                result(bindNum, RESULT_VALUE_BIND_STATUS_WAIT);
                break;
        }
    }

    private void doBind(String bindNum) {
        Intent intent = new Intent(InputBindNumberActivity.this, SelectRelationActivity.class);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_MODE, SelectRelationActivity.PARAM_MODE_BIND);
        intent.putExtra(SelectRelationActivity.PARAM_KEY_BIND_NUM, bindNum);
        if (!isLoginBind())
            intent.putExtra("ACTIVITY_MODE", "BABYCARD_BIND");
        startActivityForResult(intent, ACTIVITY_REQUEST_SELECT_RELATION_AND_DO_BIND);
    }


    private void result(String bindNum, int bindStatus) {
        Intent intent = new Intent(RESULT_ACTION);
        intent.putExtra(RESULT_KEY_MODE, workMode);
        intent.putExtra(RESULT_KEY_BIND_NUM, bindNum);
        switch (workMode) {
            case PARAM_VALUE_MODE_BIND_DEVICE:
                intent.putExtra(RESULT_KEY_BIND_STATUS, bindStatus);
                break;
            default:
                intent.putExtra(RESULT_KEY_BIND_STATUS, bindStatus);
                break;
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    private TaskHandler mTaskHandler = new TaskHandler(this);

    private static class TaskHandler extends Handler {
        static final int DELAY_FINISH = 0;

        private WeakReference<InputBindNumberActivity> mA;

        TaskHandler(InputBindNumberActivity a) {
            mA = new WeakReference<>(a);
        }

        private boolean finished = false;

        @Override
        public void handleMessage(android.os.Message msg) {
            InputBindNumberActivity a = mA.get();
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

    //接收 CaptureBindNumberActivity和SelectRelactionActivity传回的数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
//                绑定号绑定
                case ACTIVITY_REQUEST_MANUAL_INPUT_BIND_NUM: {
                    String bindNum = data.getStringExtra(InputBindNumberActivity.RESULT_KEY_BIND_NUM);
                    onBindNumIsvalid(bindNum, false);
                }
                break;
//                选择关系后 判断是否已经绑定
                case ACTIVITY_REQUEST_SELECT_RELATION_AND_DO_BIND: {
                    boolean alreadyBind = data.getBooleanExtra(SelectRelationActivity.RESULT_KEY_ALREADY_BIND, false);
                    String bindNum = data.getStringExtra(SelectRelationActivity.RESULT_KEY_BIND_NUM);
                    if (alreadyBind) {
                        // 已经绑定，不用从服务器取数据
                        // 直接先将相关信息插入本地数据库
                        protocol.Message.UsrDevAssoc uda = (Message.UsrDevAssoc) data.getSerializableExtra(SelectRelationActivity.RESULT_KEY_USER_OF_DEV);
                        GreenUtils.saveUsrDevAssoc(uda);
//                        mTaskHandler.sendEmptyMessage(BindDeviceActivity.TaskHandler.DELAY_FINISH);
                        result(bindNum, RESULT_VALUE_BIND_STATUS_ALREADY_BOUND);
                    } else {
                        // 等待绑定成功
                        Intent intent = new Intent(InputBindNumberActivity.this, WaitDeviceBindSuccessActivity.class);
                        intent.putExtra(WaitDeviceBindSuccessActivity.PARAM_KEY_WAIT_DEVIDE, bindNum);
                        startActivityForResult(intent, ACTIVITY_REQUEST_WAIT_BIND_RESULT);
                    }
                }
                break;
//
                case ACTIVITY_REQUEST_WAIT_BIND_RESULT:
                    result(data.getStringExtra(WaitDeviceBindSuccessActivity.RESULT_KEY_BIND_NUM), RESULT_VALUE_BIND_STATUS_OK);
                    break;
            }
        } else {
            if (requestCode == ACTIVITY_REQUEST_WAIT_BIND_RESULT) {
                result(data.getStringExtra(WaitDeviceBindSuccessActivity.RESULT_KEY_BIND_NUM), RESULT_VALUE_BIND_STATUS_WAIT);
            }

        }
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return false;
    }
}
