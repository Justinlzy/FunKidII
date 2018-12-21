package com.cqkct.FunKidII.Ui.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.Utils.ThirdLoginPlat;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.Utils.WifiUtil;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.svprogresshub.SVProgressHUB;
import com.cqkct.FunKidII.svprogresshub.listener.OnDismissListener;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.List;

import cn.sharesdk.framework.Platform;
import protocol.Message;

/**
 * Created by Justin on 2017/8/7.
 */

public class RegisterConfirmPasswordActivity extends BaseActivity {
    public static final String TAG = RegisterConfirmPasswordActivity.class.getSimpleName();
    private String countryCodePhoneNumber = "";
    private EditText et_password;
    private boolean passwordVisible;
    private EditText et_confirmPassword;
    private boolean confirmPasswordVisible;
    private TextView bt_confirmPassword;
    private ImageView iv_passwordEye, iv_confirmPasswordEye;
    private ImageView iv_passwordClean, iv_confirmPasswordClean;
    private boolean activityModeFindPwd = false;
    private Platform mPlatform = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_password);

        Event.ThirdLoginPlatform thirdLoginPlatform = EventBus.getDefault().getStickyEvent(Event.ThirdLoginPlatform.class);
        if (thirdLoginPlatform != null) {
            EventBus.getDefault().removeStickyEvent(Event.ThirdLoginPlatform.class);
            mPlatform = thirdLoginPlatform.getmPlatform();
        }

        Intent intent = getIntent();
        activityModeFindPwd = intent.getBooleanExtra(RegisterActivity.ACTIVITY_MODE_FIND_PWD, false);
        setTitleBarTitle(activityModeFindPwd ? R.string.set_new_password : R.string.register_login_setting_password);
        countryCodePhoneNumber = intent.getStringExtra("countryCodePhoneNumber");
        L.i("手机号码：" + countryCodePhoneNumber);
        init();

    }


    private void init() {
        bt_confirmPassword = findViewById(R.id.bt_set_and_login);
        bt_confirmPassword.setBackground(getResources().getDrawable(R.drawable.button_shape));
        bt_confirmPassword.setEnabled(false);
        iv_passwordEye = (ImageView) findViewById(R.id.iv_look_psw);
        iv_confirmPasswordEye = (ImageView) findViewById(R.id.iv_look_psw2);
        et_password = (EditText) findViewById(R.id.et_psw);
        et_confirmPassword = findViewById(R.id.et_re_psw);
        iv_passwordClean = findViewById(R.id.btn_confirm_psw_clean);
        iv_confirmPasswordClean = findViewById(R.id.btn_confirm_psw_clean2);

        bt_confirmPassword.setText(activityModeFindPwd ? R.string.set_and_login : R.string.register_and_bind);
        et_password.setHint(activityModeFindPwd ? R.string.please_input_password : R.string.your_pwd);
        et_confirmPassword.setHint(activityModeFindPwd ? R.string.please_input_password : R.string.your_pwd);

        //        未获取焦点时不显示
        et_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (hasFocus && !(TextUtils.isEmpty(et_password.getText().toString()))) {
                    iv_passwordClean.setVisibility(View.VISIBLE);
                } else {
                    iv_passwordClean.setVisibility(View.GONE);
                }
            }
        });
        et_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    iv_passwordClean.setVisibility(View.VISIBLE);
                } else {
                    iv_passwordClean.setVisibility(View.GONE);
                }
                refreshConfirmButton();
            }
        });

        //        未获取焦点时不显示
        et_confirmPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (hasFocus && !(TextUtils.isEmpty(et_confirmPassword.getText().toString()))) {
                    iv_confirmPasswordClean.setVisibility(View.VISIBLE);
                } else {
                    iv_confirmPasswordClean.setVisibility(View.GONE);
                }
            }
        });
        et_confirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    iv_confirmPasswordClean.setVisibility(View.VISIBLE);
                } else {
                    iv_confirmPasswordClean.setVisibility(View.GONE);
                }
                refreshConfirmButton();
            }
        });

    }

    private void refreshConfirmButton() {
        String phoneNumber = et_password.getText().toString();
        String passWord = et_confirmPassword.getText().toString();
        if (!StringUtils.isEmpty(phoneNumber) && !StringUtils.isEmpty(passWord)) {
            bt_confirmPassword.setBackground(getResources().getDrawable(R.drawable.login_button_bg));
            bt_confirmPassword.setEnabled(true);

        } else {
            bt_confirmPassword.setBackground(getResources().getDrawable(R.drawable.button_shape));
            bt_confirmPassword.setEnabled(false);
        }
        // btn_login.setEnabled(!(StringUtils.isEmpty(phoneNumber) | (StringUtils.isEmpty(passWord))));
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.bt_set_and_login:
                registerUser();
                break;
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_confirm_psw_clean:
                et_password.setText("");
                break;
            case R.id.btn_confirm_psw_clean2:
                et_confirmPassword.setText("");
                break;
            case R.id.iv_look_psw: {
                int editPosition = et_password.getSelectionEnd();
                if (passwordVisible) {
                    et_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    iv_passwordEye.setImageResource(R.drawable.psw_hide);
                } else {
                    et_password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    iv_passwordEye.setImageResource(R.drawable.psw_display);
                }
                passwordVisible = !passwordVisible;
                et_password.setSelection(editPosition);
            }
            break;
            case R.id.iv_look_psw2: {
                int editPosition = et_confirmPassword.getSelectionEnd();
                if (confirmPasswordVisible) {
                    et_confirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    iv_confirmPasswordEye.setImageResource(R.drawable.psw_hide);
                } else {
                    et_confirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    iv_confirmPasswordEye.setImageResource(R.drawable.psw_display);
                }
                confirmPasswordVisible = !confirmPasswordVisible;
                et_confirmPassword.setSelection(editPosition);
            }
            break;

        }
    }

    private void registerUser() {
        String format = "^[a-zA-Z0-9]{6,16}$";
        final String password = et_password.getText().toString();
        String confirmPassword = et_confirmPassword.getText().toString();

        if (!WifiUtil.isNetConnected(this)) {
            toast(getString(R.string.net_error_tip));
            return;
        }
        if (StringUtils.isEmpty(password)) {
            toast(getResources().getString(R.string.check_empty_error,
                    getResources().getString(R.string.password)));
            return;
        }
        if (!password.equals(confirmPassword)) {
            toast(getResources().getString(
                    R.string.different_password));
            return;
        }
        if (StringUtils.isEmpty(confirmPassword)) {
            toast(getResources().getString(R.string.check_empty_error,
                    getResources().getString(R.string.confirm_password)));

        }
        if (!password.matches(format)) {
            toast(getResources().getString(R.string.check_password_error));
            return;
        }

        if (!confirmPassword.matches(format) || !Utils.isPasswdValid(password)) {
            toast(getResources().getString(R.string.check_password_error));
            return;
        }

        if (activityModeFindPwd) {
            findPwd(countryCodePhoneNumber, password);
            return;
        }
        if (mPlatform == null) {
            register(countryCodePhoneNumber, password);
        } else {
            associateThirdPlat(countryCodePhoneNumber, password);
        }
    }

    private void register(final String userName, final String password) {
        popWaitingDialog(R.string.ios_18);
        mTlcService.registerUser(userName, Message.UserNameType.USR_NAM_TYP_PHONE, password, null, new TlcService.OnRegisterUserListener() {
            @Override
            public void onStatus(Message.RegisterRspMsg rspMsg, int status) {
                switch (status) {
                    case SUCCESS:
                        setResult(RESULT_OK);
                        doLoginOnRegisterSucess(userName, password);
                        break;
                    case USER_ALREADY_EXISTS:
                        popErrorDialog(R.string.register_user_already_exists);
                        break;
                    case TIMEOUT:
                    case NETWORK_ERROR:
                        popErrorDialog(R.string.register_failed2);
                        break;
                    default:
                        popErrorDialog(R.string.register_failed);
                        break;
                }
            }
        });
    }

    private void associateThirdPlat(final String userName, String password) {
        popWaitingDialog(R.string.third_plat_connect_ing);
        mTlcService.registerUser(userName, Message.UserNameType.USR_NAM_TYP_PHONE, password,
                Message.OAuthAccountInfo.newBuilder()
                        .setAvatarUrl(mPlatform.getDb().getUserIcon())
                        .setPlat(ThirdLoginPlat.getUserNameType(mPlatform))
                        .setNickname(mPlatform.getDb().getUserName())
                        .setThirdAccId(mPlatform.getDb().getUserId())
                        .build(),
                new TlcService.OnRegisterUserListener() {
                    @Override
                    public void onStatus(Message.RegisterRspMsg rspMsg, int status) {
                        switch (status) {
                            case SUCCESS:
                            case USER_ALREADY_EXISTS:
                                setResult(RESULT_OK);
                                showAssociateSuccess(userName);
                                break;
                            case TIMEOUT:
                            case NETWORK_ERROR:
                                popErrorDialog(R.string.third_plat_connect_fail_check_network);
                                break;
                            default:
                                popErrorDialog(R.string.third_plat_connect_fail);
                                break;
                        }
                    }
                });
    }

    private void doLoginOnRegisterSucess(String username, String passwd) {
        String name = mPlatform == null ? username : mPlatform.getDb().getUserId();
        Message.UserNameType type = mPlatform == null ? Message.UserNameType.USR_NAM_TYP_PHONE : ThirdLoginPlat.getUserNameType(mPlatform);
        if (mPlatform != null) {
            passwd = null;
        }
        mTlcService.login(name, type, passwd, new TlcService.OnLoginListener() {
            @Override
            public void onStatus(Message.LoginRspMsg rspMsg, int status) {
                switch (status) {
                    case SUCCESS: {
                        dismissDialog();
                        App.getInstance().fetchUserInfo(rspMsg.getUserId());
                        Intent intent = new Intent(RegisterConfirmPasswordActivity.this, BindDeviceActivity.class);
                        setResult(LoginActivity.RESULT_LOGIN_OK, intent);
                        finish();
                    }
                    break;
                    default:
                        // 注册成功，但登录失败
                        popSuccessDialog(R.string.register_login_register_suc, true);
                        break;
                }
            }
        });
    }

    private void showAssociateSuccess(String name) {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setTitle(getString(R.string.third_plat_connect_suc))
                .setMessage(name)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    doLoginOnAssociateSuccess();
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "showAssociateSuccess");
    }

    private void doLoginOnAssociateSuccess() {
        updateDialogText(R.string.logging);
        String name = mPlatform.getDb().getUserId();
        Message.UserNameType type = ThirdLoginPlat.getUserNameType(mPlatform);
        mTlcService.login(name, type, null, new TlcService.OnLoginListener() {
            @Override
            public void onStatus(Message.LoginRspMsg rspMsg, int status) {
                switch (status) {
                    case SUCCESS:
                        fetchDevices(rspMsg.getUserId());
                        App.getInstance().fetchUserInfo(rspMsg.getUserId());
                        break;
                    case TIMEOUT:
                    case NETWORK_ERROR:
                        popErrorDialog(R.string.third_plat_login_fail_check_network, new OnDismissListener() {
                            @Override
                            public void onDismiss(SVProgressHUB hud) {
                                finish();
                            }
                        });
                        break;
                    default:
                        popErrorDialog(R.string.login_failure);
                        break;
                }
            }
        });
    }

    private void fetchDevices(@NonNull String userId) {
        protocol.Message.FetchDeviceListReqMsg reqMsg = protocol.Message.FetchDeviceListReqMsg.newBuilder()
                .setUserId(userId).build();
        exec(reqMsg, new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    protocol.Message.FetchDeviceListRspMsg rspMsg = response.getProtoBufMsg();
                    L.d(TAG, "fetchDevices() success -> exec(" + protocol.Message.FetchDeviceListReqMsg.class.getSimpleName() + ") -> rsp：" + rspMsg);
                    if (rspMsg.getErrCode() == protocol.Message.ErrorCode.SUCCESS) {
                        dismissDialog();
                        new SaveDevicesOfUserTask(RegisterConfirmPasswordActivity.this).execute(rspMsg.getUsrDevAssocList());
                        return false;
                    }
                    L.e(TAG, "fetchDevices() success -> exec(" + protocol.Message.FetchDeviceListReqMsg.class.getSimpleName() + ") -> onResponse(): " + rspMsg.getErrCode());
                } catch (Exception e) {
                    L.e(TAG, "fetchDevices() success -> exec(" + protocol.Message.FetchDeviceListReqMsg.class.getSimpleName() + ") ->" +
                            " rsp is not " + protocol.Message.FetchDeviceListRspMsg.class.getSimpleName() + ": " + response, e);
                }
                popErrorDialog(R.string.login_failure, new OnDismissListener() {
                    @Override
                    public void onDismiss(SVProgressHUB hud) {
                        finish();
                    }
                });
                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                popErrorDialog(R.string.third_plat_login_fail_check_network, new OnDismissListener() {
                    @Override
                    public void onDismiss(SVProgressHUB hud) {
                        finish();
                    }
                });
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                // never to here for this exec
            }
        });
    }

    private static class SaveDevicesOfUserTask extends AsyncTask<Object, Object, Boolean> {
        WeakReference<RegisterConfirmPasswordActivity> mA;

        SaveDevicesOfUserTask(RegisterConfirmPasswordActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            List<protocol.Message.UsrDevAssoc> usrDevInfos = (List<protocol.Message.UsrDevAssoc>) objects[0];
            GreenUtils.saveDevicesOfUserFromFetch(usrDevInfos);
            return usrDevInfos.isEmpty();
        }

        @Override
        protected void onPostExecute(Boolean listIsEmpty) {
            RegisterConfirmPasswordActivity a = mA.get();
            if (a == null)
                return;
            Intent intent;
            if (listIsEmpty) {
                intent = new Intent(a, BindDeviceActivity.class);
            } else {
                intent = new Intent(a, MainActivity.class);
            }
            a.setResult(LoginActivity.RESULT_LOGIN_OK, intent);
            a.finish();
        }
    }

    private void findPwd(String username, String password) {
        popWaitingDialog(R.string.please_wait);
        mTlcService.setPasswd(username, Message.UserNameType.USR_NAM_TYP_PHONE, password, new TlcService.OnSetPasswdListener() {
            @Override
            public void onStatus(Message.SetPwdRspMsg rspMsg, int status) {
                switch (status) {
                    case SUCCESS:
                        // 找回密码成功
                        setResult(RESULT_OK);
                        popSuccessDialog(R.string.register_login_setting_password_suc, true);
                        break;
                    case USER_NOT_EXISTS:
                        popErrorDialog(getString(R.string.user_not_exists));
                    default:
                        popErrorDialog(R.string.find_psw_failure);
                        break;
                }
            }
        });
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
