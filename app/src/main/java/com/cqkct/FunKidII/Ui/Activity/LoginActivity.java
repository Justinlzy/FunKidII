package com.cqkct.FunKidII.Ui.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;

import com.cqkct.FunKidII.Ui.view.Whlee.AnimationLoader;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PhoneNumberUtils;
import com.cqkct.FunKidII.Utils.ThirdLoginPlat;
import com.cqkct.FunKidII.Utils.UTIL;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;
import com.cqkct.FunKidII.service.tlc.TlcService;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

import cn.sharesdk.facebook.Facebook;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.google.GooglePlus;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.twitter.Twitter;
import cn.sharesdk.wechat.friends.Wechat;
import protocol.Message;


/**
 * Created by Justin on 2017/7/25.
 */

public class LoginActivity extends BaseActivity implements PlatformActionListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int ACTIVITY_REQUEST_LOGIN_FROM_COUNTRY_CODE = 0;
    private static final int ACTIVITY_REQUEST_REGISTER = 1;
    private static final int ACTIVITY_REQUEST_3RD_ASSOC = 2;
    private static final int PERMISSIONS_REQUEST_INTERNET = 1;
    public static final int RESULT_LOGIN_OK = RESULT_FIRST_USER + 1;

    private EditText et_phoneNumber;
    private EditText et_password;
    private boolean passwordVisible;
    private Button btn_login;
    private ImageView bt_cleanPhoneNumber;
    private ImageView bt_cleanPassWord;
    private ImageView iv_eye;
    private TextView tv_countryNumber;
    private View mThirdPartyPlatView;
    private ConstraintLayout firstLogin;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSIONS_REQUEST_INTERNET);
        }
    }

    private void init() {
        tv_countryNumber = findViewById(R.id.bt_select_country);
        btn_login = findViewById(R.id.bt_login);

        bt_cleanPhoneNumber = findViewById(R.id.phoneNumber_clean);
        bt_cleanPassWord = findViewById(R.id.psw_clean);

        iv_eye = findViewById(R.id.iv_look_psw);

        TextView tv_versions = findViewById(R.id.versions);
        tv_versions.setText(getString(R.string.version_number, UTIL.getVersion(this)));
        firstLogin = findViewById(R.id.first_login);
        et_phoneNumber = findViewById(R.id.et_username);
        et_phoneNumber.setOnFocusChangeListener((v, hasFocus) -> {

            if (hasFocus && !(TextUtils.isEmpty(et_phoneNumber.getText().toString()))) {
                bt_cleanPhoneNumber.setVisibility(View.VISIBLE);
            } else {
                bt_cleanPhoneNumber.setVisibility(View.GONE);
            }
        });
        et_phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(et_phoneNumber.getText().toString())) {
                    bt_cleanPhoneNumber.setVisibility(View.GONE);
                } else {
                    bt_cleanPhoneNumber.setVisibility(View.VISIBLE);
                }
                refreshConfirm();
            }
        });

        et_password = findViewById(R.id.et_psw);
        et_password.setOnFocusChangeListener((v, hasFocus) -> {

            if (hasFocus && !(TextUtils.isEmpty(et_password.getText().toString()))) {
                bt_cleanPassWord.setVisibility(View.VISIBLE);
            } else {
                bt_cleanPassWord.setVisibility(View.GONE);
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
                if (TextUtils.isEmpty(et_password.getText().toString())) {
                    bt_cleanPassWord.setVisibility(View.GONE);
                } else {
                    bt_cleanPassWord.setVisibility(View.VISIBLE);
                }
                refreshConfirm();
            }

        });


        PreferencesWrapper preferencesWrapper = getPreferencesWrapper();
        if (preferencesWrapper.getUsernameType().getNumber() < Message.UserNameType.USR_NAM_TYP_3RD_QQ_VALUE) {
            String username = preferencesWrapper.getUsername();
            if (preferencesWrapper.getUsernameType().getNumber() == Message.UserNameType.USR_NAM_TYP_PHONE_VALUE) {
                String countryCode = PhoneNumberUtils.pickCountryCodeFromNumber(username);
                if (!TextUtils.isEmpty(countryCode)) {
                    username = username.substring(countryCode.length());
                    tv_countryNumber.setText(countryCode);
                }
            }
            et_phoneNumber.setText(username);
            et_phoneNumber.setSelection(et_phoneNumber.length());   //末至光标
        }

        mThirdPartyPlatView = findViewById(R.id.third_party_plat);
    }

    private void refreshConfirm() {
        String phoneNumber = et_phoneNumber.getText().toString();
        String passWord = et_password.getText().toString();
        if (!TextUtils.isEmpty(phoneNumber) && Utils.isPasswdLengthEnough(passWord)) {
            btn_login.setBackground(getResources().getDrawable(R.drawable.login_button_bg));
            btn_login.setEnabled(true);
        } else {
            btn_login.setBackground(this.getResources().getDrawable(R.drawable.button_shape));
            btn_login.setEnabled(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_INTERNET:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ACTIVITY_REQUEST_LOGIN_FROM_COUNTRY_CODE: {
                    int countryName = data.getIntExtra("countryName", -1);
                    if (countryName > 0) {
                        tv_countryNumber.setText(PhoneNumberUtils.getCountryCode(countryName));
                    }
                }
                break;
//          case  ACTIVITY_REQUEST_3RD_ASSOC:
                default:
                    break;
            }
        } else if (resultCode == RESULT_LOGIN_OK) {
            if (data != null) {
                startActivity(data);
                finish();
            }
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        Platform platform = null;
        switch (viewId) {
            case R.id.bt_select_country:
                startActivityForResult(new Intent(this, SelectCountryActivity.class), ACTIVITY_REQUEST_LOGIN_FROM_COUNTRY_CODE);
                break;
            case R.id.tv_forget_pwd: {
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.putExtra(RegisterActivity.ACTIVITY_MODE_FIND_PWD, true);
                intent.putExtra(RegisterActivity.ACTIVITY_PARAM_COUNTRY_CODE, tv_countryNumber.getText().toString());
                intent.putExtra(RegisterActivity.ACTIVITY_PARAM_PHONE_NUMBER, et_phoneNumber.getText().toString());
                startActivity(intent);
            }
            break;
            case R.id.bt_login:
                popWaitingDialog(R.string.logging);
                login(null);
                hideSoftKeyBoard();
                break;
            case R.id.tv_register: {
                Intent intent = new Intent(this, RegisterActivity.class);
                intent.putExtra(RegisterActivity.ACTIVITY_PARAM_COUNTRY_CODE, tv_countryNumber.getText().toString());
                intent.putExtra(RegisterActivity.ACTIVITY_PARAM_PHONE_NUMBER, et_phoneNumber.getText().toString());
                startActivityForResult(intent, ACTIVITY_REQUEST_REGISTER);
            }
            break;

            case R.id.third_party:
                mThirdPartyPlatView.setVisibility(View.VISIBLE);
                mThirdPartyPlatView.startAnimation(AnimationLoader.getInAnimation(this));
                findViewById(R.id.first_login).setVisibility(View.GONE);
                break;
            case R.id.qq_login:
                platform = ShareSDK.getPlatform(QQ.NAME);
                break;
            case R.id.wx_login:
                platform = ShareSDK.getPlatform(Wechat.NAME);
                break;
            case R.id.twitter_login:
                platform = ShareSDK.getPlatform(Twitter.NAME);
                break;
            case R.id.facebook_login:
                platform = ShareSDK.getPlatform(Facebook.NAME);
                break;
            case R.id.google_login:
                platform = ShareSDK.getPlatform(GooglePlus.NAME);
                break;
            case R.id.tv_count_pwd_login:
                mThirdPartyPlatView.setVisibility(View.GONE);
                mThirdPartyPlatView.startAnimation(AnimationLoader.getOutAnimation(this));
                firstLogin.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
        if (platform != null) {
            authorize(platform);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.phoneNumber_clean:
                et_phoneNumber.setText("");
                break;

            case R.id.psw_clean:
                et_password.setText("");
                break;

            case R.id.iv_look_psw: {
                int editPosition = et_password.getSelectionEnd();
                if (passwordVisible) {
                    et_password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    iv_eye.setImageResource(R.drawable.psw_hide);
//                    tv_eye.setText(R.string.display_password);
                } else {
                    et_password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    iv_eye.setImageResource(R.drawable.psw_display);
//                    tv_eye.setText(R.string.hide_password);
                }
                passwordVisible = !passwordVisible;
                et_password.setSelection(editPosition);
            }
            break;
        }
    }

    private void authorize(Platform platform) {
        popWaitingDialog(R.string.please_wait);

        platform.removeAccount(true);
        platform.SSOSetting(false);
        platform.setPlatformActionListener(this);
        if (platform.isAuthValid()) {
            // 已授权
            login(platform);
            return;
        }
        platform.showUser(null);//授权并获取用户信息
    }

    @Override
    public void onComplete(final Platform platform, int i, HashMap<String, Object> hashMap) {
        L.i(TAG, "OAuth2 onComplete " + platform.getName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                login(platform);
            }
        });
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {
        L.e(TAG, "OAuth2 onError " + platform.getName(), throwable);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                popErrorDialog(R.string.other_login_fail);
            }
        });
    }

    @Override
    public void onCancel(Platform platform, int i) {
        L.i(TAG, "OAuth2 onCancel " + platform.getName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissDialog();
            }
        });
    }

    @Override
    public void onBackPressed() {
        L.v("Az", "onBackPressed ");
        if (mThirdPartyPlatView.getVisibility() == View.VISIBLE) {
            mThirdPartyPlatView.setVisibility(View.GONE);
            mThirdPartyPlatView.startAnimation(AnimationLoader.getOutAnimation(this));
            firstLogin.setVisibility(View.VISIBLE);
        } else {
            dismissDialog();
            moveTaskToBack(true);
        }
    }

    private void login(final Platform platform) {
        updateDialogText(R.string.logging);
        String phoneNumber = et_phoneNumber.getText().toString();
        String passWord = et_password.getText().toString();
        String username = tv_countryNumber.getText() + phoneNumber;
        mTlcService.login(platform == null ? username : platform.getDb().getUserId(),
                platform == null ? Message.UserNameType.USR_NAM_TYP_PHONE : ThirdLoginPlat.getUserNameType(platform),
                platform == null ? passWord : null, (rspMsg, status) -> {
                    L.v(TAG, "登录回响。。。:" + rspMsg + ", status： " + status);
                    switch (status) {
                        case TlcService.OnLoginListener.SUCCESS:
                            fetchDevices(rspMsg.getUserId());
                            App.getInstance().fetchUserInfo(rspMsg.getUserId());
                            break;
                        case TlcService.OnLoginListener.USER_OR_PASSWD_ERROR:
                            popErrorDialog(R.string.login_user_or_passwd_error);
                            break;
                        case TlcService.OnLoginListener.TIMEOUT:
                        case TlcService.OnLoginListener.NETWORK_ERROR:
                            popErrorDialog(R.string.third_plat_login_fail_check_network);
                            break;
                        case TlcService.OnLoginListener.NOT_EXISTS:
                            if (platform == null) {
                                popErrorDialog(R.string.login_user_or_passwd_error);
                            } else {
                                EventBus.getDefault().postSticky(new Event.ThirdLoginPlatform(platform));
                                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                                intent.putExtra("3RD_ASSOC_PLATFORM",platform.getName());
                                intent.putExtra(RegisterActivity.ACTIVITY_MODE_BIND_FHONE, true);
                                startActivityForResult(intent, ACTIVITY_REQUEST_3RD_ASSOC);
                                dismissDialog();
                            }
                            break;
                        case TlcService.OnLoginListener.NOT_COMPAT:
                            dismissDialog();
                            EventBus.getDefault().postSticky(new Event.ServerApiNotCompat());
                            break;
                        default:
                            popErrorDialog(R.string.login_failure);
                            break;
                    }
                });
    }

    private void fetchDevices(@NonNull String userId) {
        Message.FetchDeviceListReqMsg reqMsg = Message.FetchDeviceListReqMsg.newBuilder()
                .setUserId(userId).build();
        exec(reqMsg, new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    Message.FetchDeviceListRspMsg rspMsg = response.getProtoBufMsg();
                    L.d(TAG, "login() success -> exec(" + Message.FetchDeviceListReqMsg.class.getSimpleName() + ") -> rsp：" + rspMsg);
                    if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                        new SaveDevicesOfUserTask(LoginActivity.this).execute(rspMsg.getUsrDevAssocList());
                        return false;
                    }
                    L.e(TAG, "login() success -> exec(" + Message.FetchDeviceListReqMsg.class.getSimpleName() + ") -> onResponse(): " + rspMsg.getErrCode());
                } catch (Exception e) {
                    L.e(TAG, "login() success -> exec(" + Message.FetchDeviceListReqMsg.class.getSimpleName() + ") ->" +
                            " rsp is not " + Message.FetchDeviceListRspMsg.class.getSimpleName() + ": " + response, e);
                }
                popErrorDialog(R.string.login_failure);
                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                popErrorDialog(R.string.third_plat_login_fail_check_network);
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                // never to here for this exec
            }
        });
    }

    private static class SaveDevicesOfUserTask extends AsyncTask<Object, Object, Boolean> {
        WeakReference<LoginActivity> mA;

        SaveDevicesOfUserTask(LoginActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            List<Message.UsrDevAssoc> usrDevInfos = (List<Message.UsrDevAssoc>) objects[0];
            GreenUtils.saveDevicesOfUserFromFetch(usrDevInfos);
            return usrDevInfos.isEmpty();
        }

        @Override
        protected void onPostExecute(Boolean listIsEmpty) {
            LoginActivity a = mA.get();
            if (a == null)
                return;
            if (listIsEmpty) {
                a.startActivity(new Intent(a, BindDeviceActivity.class));
            } else {
                a.startActivity(new Intent(a, MainActivity.class));
            }
            a.finish();
        }
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


}
