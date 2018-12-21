package com.cqkct.FunKidII.Ui.Activity;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Ui.view.RoundProgressbarWithProgress;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.NetWorkUtils;
import com.cqkct.FunKidII.Utils.PhoneNumberUtils;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.Utils.ThirdLoginPlat;
import com.cqkct.FunKidII.db.Dao.UserEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.UserEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.svprogresshub.SVProgressHUB;
import com.cqkct.FunKidII.svprogresshub.listener.OnDismissListener;
import com.google.protobuf.GeneratedMessageV3;
import com.mob.MobSDK;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.sharesdk.framework.Platform;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

import static protocol.Message.Tag.QUERY_THIRD_ACCOUNT_BY_PHONE;

/**
 * Created by Justin on 2017/7/27.
 */

public class RegisterActivity extends BaseActivity {

    public static final String TAG = RegisterActivity.class.getSimpleName();

    private static final int ACTIVITY_REQUEST_COUNTRY_CODE = 0;
    private static final int ACTIVITY_REQUEST_SET_PWD_FOR_REGISTER = 1;
    private static final int ACTIVITY_REQUEST_FIND_PWD = 2;
    private static final int ACTIVITY_REQUEST_3RD_ASSOC = 3;

    private int currentRegisterState = REGISTER_STATE_FIRST; // 当前“next step”状态
    private final static int REGISTER_STATE_FIRST = 0;  //  注册界面
    private final static int REGISTER_STATE_SECOND = 1; //  校验验证码
    private static final int REGISTER_STATE_RESEND_SMS = 2; // 重新发送验证码
    private final static int START_COUNT_DOWN = 60;
    private static final int HORIZONTAL_WHAT = 0;

    //注册界面
    private ConstraintLayout cl_registerCode;
    private TextView bt_register_next;
    private EditText et_registerPhoneNumber;
    private ImageView bt_cleanPhoneNumber;
    private String phoneNumber;

    private int countryCode = 86;
    private TextView tv_selectCountry;
    private TextView tv_selectCountryCode;
    //验证码界面
    private ConstraintLayout cl_smsCode;
    private EditText et_input_verification_code;
    private RoundProgressbarWithProgress roundProgressBar;
    private String countryCodePhoneNumber;
    private TextView tv_smsCode_show_number;
    private boolean activityModeFindPwd = false;    //找回密码
    private boolean activityModeBindPhone = false;    //找回密码
    private TextView tv_resend_sms;
    /**
     * 找回密码时的传参
     */
    public static final String ACTIVITY_MODE_FIND_PWD = "findPwd";
    public static final String ACTIVITY_MODE_BIND_FHONE = "bindPhoneNumber";
    public static final String ACTIVITY_PARAM_COUNTRY_CODE = "country_code";
    public static final String ACTIVITY_PARAM_PHONE_NUMBER = "phone_number";

    private Platform mPlatform = null;
    private TextView bt_sms_next;
    private TextView tv_sms_countryCode;
    private TextView other_login_hint1;
    public Boolean isSendVerificationCode = false;
    private TextView tv_register_userprotocol;
    private TextView tv_register_userprotocol_tips;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Intent intent = getIntent();
        activityModeFindPwd = intent.getBooleanExtra(ACTIVITY_MODE_FIND_PWD, false);
        activityModeBindPhone = intent.getBooleanExtra(ACTIVITY_MODE_BIND_FHONE, false);
        setTitleBarTitle(activityModeFindPwd ? R.string.forget_password : R.string.new_user_register);
        findViewById(R.id.ll_register_user_agreement).setVisibility(activityModeFindPwd ? View.GONE : View.VISIBLE);//找回密码时不显示用户协议
        Event.ThirdLoginPlatform thirdLoginPlatform = EventBus.getDefault().getStickyEvent(Event.ThirdLoginPlatform.class);
        if (thirdLoginPlatform != null) {
            EventBus.getDefault().removeStickyEvent(Event.ThirdLoginPlatform.class);
            mPlatform = thirdLoginPlatform.getmPlatform();
        }
        init();
        //注册SMS
        MobSDK.init(this, Constants.MOD_SMS_APP_KEY, Constants.MOD_SMS_APP_SECRET);
        //注册短信回调
        SMSSDK.registerEventHandler(eh);

    }

    private void init() {
        tv_register_userprotocol_tips = findViewById(R.id.tv_register_userprotocol_tips);
        tv_register_userprotocol_tips.setText(Html.fromHtml(getString(R.string.register_tips_user_agreement)));
        //第三方登录关联手机号
        other_login_hint1 = findViewById(R.id.other_login_hint1);
        if (activityModeBindPhone) {
            setTitleBarTitle(R.string.associate_phone_number);
            other_login_hint1.setVisibility(View.VISIBLE);
        }
        //验证码界面
        cl_smsCode = findViewById(R.id.cl_sms_code);
        et_input_verification_code = findViewById(R.id.et_input_Verification_code);
        bt_sms_next = findViewById(R.id.bt_sms_next);
        roundProgressBar = findViewById(R.id.sms_round_progressBar);
        tv_resend_sms = findViewById(R.id.tv_resend_sms);

        //注册界面
        currentRegisterState = REGISTER_STATE_FIRST;
        cl_registerCode = findViewById(R.id.cl_register);
        tv_selectCountry = findViewById(R.id.tv_country_code);
//        tv_selectCountryCode = findViewById(R.id.tv_country);

        tv_sms_countryCode = findViewById(R.id.tv_sms_countryCode);//验证码界面 +86
        bt_register_next = findViewById(R.id.bt_register_next);

        bt_cleanPhoneNumber = findViewById(R.id.bt_register_phoneNumber_clean);
        et_registerPhoneNumber = findViewById(R.id.et_register_phonenumber);
        //注册界面 下一步按钮是否可点击
        et_registerPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (StringUtils.isEmpty(et_registerPhoneNumber.getText().toString())) {
                    bt_cleanPhoneNumber.setVisibility(View.GONE);
                    bt_register_next.setBackground(getResources().getDrawable(R.drawable.button_shape));
                    bt_register_next.setEnabled(false);
                } else {
                    bt_register_next.setBackground(getResources().getDrawable(R.drawable.login_button_bg));
                    bt_register_next.setEnabled(true);
                    bt_cleanPhoneNumber.setVisibility(View.VISIBLE);
                }

            }
        });
        et_input_verification_code.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String codeSms = et_input_verification_code.getText().toString().trim();
                if (StringUtils.isEmpty(codeSms) && codeSms.length() < 4) {
                    bt_sms_next.setBackground(getResources().getDrawable(R.drawable.button_shape));
                    bt_sms_next.setEnabled(false);
                } else {
                    bt_sms_next.setBackground(getResources().getDrawable(R.drawable.login_button_bg));
                    bt_sms_next.setEnabled(true);
                }
            }
        });

        String countryCode_temp = getIntent().getStringExtra(ACTIVITY_PARAM_COUNTRY_CODE);

        if (!TextUtils.isEmpty(countryCode_temp) && !countryCode_temp.equals(getString(R.string.country))) {

            tv_sms_countryCode.setText(countryCode_temp);
            countryCode = Integer.parseInt(countryCode_temp.replace("+", ""));
//            tv_selectCountry.setText(PhoneNumberUtils.getCountryName(this, countryCode_temp));//中国
            tv_selectCountry.setText(countryCode_temp);//+86
        }
        String number = getIntent().getStringExtra(ACTIVITY_PARAM_PHONE_NUMBER);
        et_registerPhoneNumber.setText(number);
        et_registerPhoneNumber.setSelection(et_registerPhoneNumber.length());   //末至光标

        tv_smsCode_show_number = findViewById(R.id.smsCode_show_number);
    }

    private EventHandler eh = new EventHandler() {

        @Override
        public void afterEvent(int event, int result, Object data) {
            L.d(TAG, "EventHandler: afterEvent() " + event + ", " + result + ", " + data + ". currentRegisterState: " + currentRegisterState);
            if (result == SMSSDK.RESULT_COMPLETE) {
                //回调完成
                if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                    // 校验验证码
                    if (currentRegisterState == REGISTER_STATE_SECOND) {
                        // 是处于输入验证码阶段
                        mUIHandler.obtainMessage(UIHandler.MESSAGE_CHECK_VERIFICATION_CODE_SUCCESS).sendToTarget();
                    }
                } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                    // 发送验证码
                    if (currentRegisterState == REGISTER_STATE_FIRST || currentRegisterState == REGISTER_STATE_RESEND_SMS) {
                        // 是处于发送验证码阶段
                        mUIHandler.obtainMessage(UIHandler.MESSAGE_GET_VERIFICATION_CODE_SUCCESS).sendToTarget();
                    }
                } else if (event == SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES) {
                    L.d(TAG, " //返回支持发送验证码的国家列表");
                }
            } else {
                switch (event) {
                    case SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE:
                        // 校验验证码
                        if (currentRegisterState == REGISTER_STATE_SECOND) {
                            // 是处于输入验证码阶段
                            mUIHandler.obtainMessage(UIHandler.MESSAGE_CHECK_VERIFICATION_CODE_FAILTH, data).sendToTarget();
                        }
                        break;
                    case SMSSDK.EVENT_GET_VERIFICATION_CODE:
                        // 发送验证码
                        try {
                            L.e(TAG, "data:  " + data.toString());
                            if (currentRegisterState == REGISTER_STATE_FIRST || currentRegisterState == REGISTER_STATE_RESEND_SMS) {
                                // 是处于发送验证码阶段

                                JSONObject jsonObject = new JSONObject(((Throwable) data).getMessage());
                                int status = jsonObject.getInt("status");
                                switch (status) {
                                    case 462: // 每分钟发送次数超限	每分钟发送短信的数量超过限制。
                                        mUIHandler.obtainMessage(UIHandler.MESSAGE_SEND_LIMIT_PER_MINUTE, data).sendToTarget();
                                        break;

                                    case 463: // 手机号码每天发送次数超限	手机号码在当前APP内每天发送短信的次数超出限制。
                                    case 464: // 每台手机每天发送次数超限	每台手机每天发送短信的次数超限。
                                    case 465: // 号码在App中每天发送短信的次数超限	手机号码在APP中每天发送短信的数量超限。
                                    case 477: // 当前手机号发送短信的数量超过限额	当前手机号码在SMSSDK平台内每天最多可发送短信10条，包括客户端发送和WebApi发送
                                        mUIHandler.obtainMessage(UIHandler.MESSAGE_SEND_BEYOND_TIMES, data).sendToTarget();
                                        break;
                                    default:
                                        mUIHandler.obtainMessage(UIHandler.MESSAGE_SEND_VERIFICATION_CODE_FAILTH, data).sendToTarget();
                                        break;
                                    // 可能会出现 java.net.ProtocolException: unexpected end of stream，这种情况应该是网络不稳定
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            mUIHandler.obtainMessage(UIHandler.MESSAGE_SEND_VERIFICATION_CODE_FAILTH, data).sendToTarget();
                        }
                        break;
                    default:

                        break;
                }
            }
        }
    };

    private UIHandler mUIHandler = new UIHandler(this);

    private static class UIHandler extends Handler {
        static final int MESSAGE_VERIFICATION_CODE_COUNT_DOWN = 1;
        static final int MESSAGE_GET_VERIFICATION_CODE_SUCCESS = 2;
        static final int MESSAGE_GET_VERIFICATION_CODE_FAILTH = 3;
        static final int MESSAGE_CHECK_VERIFICATION_CODE_SUCCESS = 4;
        static final int MESSAGE_CHECK_VERIFICATION_CODE_FAILTH = 5;
        static final int MESSAGE_SEND_VERIFICATION_CODE_FAILTH = 6;
        static final int MESSAGE_SEND_BEYOND_TIMES = 7;
        static final int MESSAGE_SEND_LIMIT_PER_MINUTE = 8;
        static final int MESSAGE_PHONENUMBER_NOT_VALID = 9;
        static final int MESSAGE_PHONENUMBER_ALREADY_ASSOCIATION = 10;

        private WeakReference<RegisterActivity> mA;

        UIHandler(RegisterActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(Message msg) {
            RegisterActivity a = mA.get();
            if (a == null)
                return;
            switch (msg.what) {
                case MESSAGE_VERIFICATION_CODE_COUNT_DOWN:
                    L.i(TAG, "MESSAGE_VERIFICATION_CODE_COUNT_DOWN");
                    break;
                case MESSAGE_GET_VERIFICATION_CODE_SUCCESS:
                    a.dismissDialog();
                    a.cl_smsCode.setVisibility(View.VISIBLE); //验证码界面
//                    a.tv_user_agreement.setVisibility(View.GONE); //隐藏用户协议
                    a.cl_registerCode.setVisibility(View.GONE);//注册界面
                    a.roundProgressBar.setVisibility(View.VISIBLE);
                    a.roundProgressBar.setProgress(0);
                    a.mTimerDownHandler.sendEmptyMessage(HORIZONTAL_WHAT);
                    a.tv_smsCode_show_number.setText(a.phoneNumber);
                    a.setTitleBarTitle(R.string.please_enter_verify_code);
                    a.currentRegisterState = REGISTER_STATE_SECOND;
                    a.tv_resend_sms.setVisibility(View.INVISIBLE);
                    break;
                case MESSAGE_GET_VERIFICATION_CODE_FAILTH:
                    L.w(TAG, "获取验证码错误");
                    a.toast(R.string.get_sms_code_failure);
                    break;
                case MESSAGE_CHECK_VERIFICATION_CODE_SUCCESS: {
                    //短息效验成功
                    L.i(TAG, "验证码效验成功");
                    if (a.mPlatform == null) {
                        a.dismissDialog();
                        Intent intent = new Intent(a, RegisterConfirmPasswordActivity.class);
                        L.d(TAG, "countryCodePhoneNumber" + a.countryCodePhoneNumber);
                        intent.putExtra("countryCodePhoneNumber", a.countryCodePhoneNumber);
                        intent.putExtra(ACTIVITY_MODE_FIND_PWD, a.activityModeFindPwd);
                        a.startActivityForResult(intent, a.activityModeFindPwd ? ACTIVITY_REQUEST_FIND_PWD : ACTIVITY_REQUEST_SET_PWD_FOR_REGISTER);
                    } else {
                        a.checkUserOfPhoneFor3rdAssoc(a.mPlatform);
                    }
                }
                break;
                case MESSAGE_CHECK_VERIFICATION_CODE_FAILTH:
                    L.e(TAG, "验证码输入错误");
                    a.popErrorDialog(R.string.register_login_code_disable);
                    break;
                case MESSAGE_SEND_VERIFICATION_CODE_FAILTH:
                    L.e(TAG, "获取验证码失败");
                    a.popErrorDialog(R.string.register_login_send_fail_plz_anew_send);
                    break;
                case MESSAGE_SEND_BEYOND_TIMES:
                    L.e(TAG, "超出每日次数");
                    a.popInfoDialog(R.string.register_login_tomorrow);
                    break;
                case MESSAGE_SEND_LIMIT_PER_MINUTE:
                    L.w(TAG, "每分钟发送次数超限");
                    a.popInfoDialog(R.string.register_login_too_often);
                    break;
                case MESSAGE_PHONENUMBER_ALREADY_ASSOCIATION:
                    L.w(TAG, "手机号已关联第三方账户");
                    return;
                default:
                    break;
            }
        }
    }

    //圆形计时器
    private TimerHandler mTimerDownHandler = new TimerHandler(this);

    private static class TimerHandler extends Handler {
        private WeakReference<RegisterActivity> mA;

        TimerHandler(RegisterActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(Message msg) {
            RegisterActivity a = mA.get();
            if (a == null)
                return;

            int progress = a.roundProgressBar.getProgress();
            int nextProgress = ++progress;
            a.roundProgressBar.setProgress(nextProgress);
            sendEmptyMessageDelayed(HORIZONTAL_WHAT, 1000L);
            if (progress >= 60) {
                a.roundProgressBar.setVisibility(View.INVISIBLE);
                a.tv_resend_sms.setVisibility(View.VISIBLE);
                removeMessages(HORIZONTAL_WHAT);
            }
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.bt_register_next:
                isSendVerificationCode = false;
                registerUser();
                break;
            case R.id.tv_country_code:
                startActivityForResult(new Intent(this, SelectCountryActivity.class), ACTIVITY_REQUEST_COUNTRY_CODE);
                break;
            case R.id.tv_resend_sms:
                currentRegisterState = REGISTER_STATE_RESEND_SMS;
                getVerificationCode(true);
                break;
            //  验证码界面的下一步
            case R.id.bt_sms_next:
                isSendVerificationCode = true;
                registerUser();
                break;
            default:
                break;
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.bt_register_phoneNumber_clean:
                et_registerPhoneNumber.setText("");
                break;
            case R.id.tv_register_userprotocol:
                startActivity(new Intent(this, UserInstructionActivity.class));
            default:
                break;
        }
    }

    private void registerUser() {
        switch (currentRegisterState) {
            case REGISTER_STATE_FIRST:
                registerUserOne();
                break;
            case REGISTER_STATE_SECOND:
            case REGISTER_STATE_RESEND_SMS:
                registerUserSecond();
                break;
        }

    }

    @Override
    public void onBackPressed() {
        if (currentRegisterState == REGISTER_STATE_FIRST) {
            super.onBackPressed();
        } else {
            currentRegisterState = REGISTER_STATE_FIRST;
            dismissDialog();
            cl_smsCode.setVisibility(View.GONE); // 隐藏验证码界面
//            tv_user_agreement.setVisibility(View.VISIBLE); //显示用户协议控件
            cl_registerCode.setVisibility(View.VISIBLE);// 显示注册界面
            mTimerDownHandler.removeMessages(HORIZONTAL_WHAT); // 停止计时
            roundProgressBar.setVisibility(View.INVISIBLE); // 隐藏倒计时
            tv_smsCode_show_number.setText("");
            // 设置标题
            setTitleBarTitle(activityModeFindPwd ? R.string.forget_password : R.string.new_user_register);
        }
    }

    private void registerUserOne() {
        phoneNumber = et_registerPhoneNumber.getText().toString();
        countryCodePhoneNumber = "+" + countryCode + phoneNumber;
        L.d(TAG, "registerUserOne:" + countryCodePhoneNumber);
        //手机号校验
        if (!(PhoneNumberUtils.isValidChineseMobileNumber(countryCodePhoneNumber))) {
            popErrorDialog(R.string.register_login_phonenumber_invalid);
        } else {
            // 第三方平台登录时的注册，略过手机号是否已注册的检查
            // 先验证是否绑定其他账号
            // 应该直接验证手机号是否时他自己的
            if (mPlatform != null) {
                //检测是否已关联
                isAssociate();
            } else {
                checkPhoneRegistered();
            }
        }
    }


    private void isAssociate() {
        protocol.Message.UserNameType thirdPlatId = ThirdLoginPlat.getUserNameType(mPlatform);
        if (mTlcService != null) {
            mTlcService.queryThirdAccountByPhone(countryCodePhoneNumber, thirdPlatId, new TlcService.OnQueryThirdAccountByPhoneListener() {
                @Override
                public void onStatus(protocol.Message.QueryThirdAccountByPhoneRspMsg rspMsg, int status) {
                    switch (status) {
                        case SUCCESS:
                            // 手机已注册
                            for (protocol.Message.OAuthAccountInfo oAuthInfo : rspMsg.getOAuthInfoList()) {
                                if (oAuthInfo.getPlat() == thirdPlatId && !TextUtils.isEmpty(oAuthInfo.getThirdAccId())) {
                                    // 这个手机号已关联了其他账号
                                    showTips(thirdPlatId);
                                    return;
                                }
                            }
                            // 未关联其他账号，可以继续 获取验证码
                            popWaitingDialog(R.string.register_login_ver_code_ing);
                            getVerificationCode(false);
                            break;
                        case NO_USER:
                            // 手机号未注册 去注册
                            checkPhoneRegistered();
                            break;
                        case NETWORK_ERROR:
                            // 网络问题
                            popErrorDialog(R.string.network_quality_poor);
                            break;
                        case TIMEOUT:
                            // 请求超时
                            popErrorDialog(R.string.net_timeout);
                            break;
                        default:
                            // 失败
                            popErrorDialog(R.string.request_failed);
                            break;
                    }
                }
            });
        }
    }

    /**
     * UserNameType    type    = 1; // 用户名类型。可能的取值：USR_NAM_TYP_PHONE USR_NAM_TYP_EMAIL USR_NAM_TYP_DEVICE
     */
    private void checkPhoneRegistered() {
        popWaitingDialog(R.string.register_login_plz_wait);

        mTlcService.checkUserExists(countryCodePhoneNumber, protocol.Message.UserNameType.USR_NAM_TYP_PHONE, new TlcService.OnCheckUserCanRegisterListener() {
            @Override
            public void onStatus(protocol.Message.CheckUserRspMsg rspMsg, int status) {
                switch (status) {
                    case EXISTS:
                        if (activityModeFindPwd) {
                            getVerificationCode(false);
                        } else {
                            popErrorDialog(R.string.register_login_user_have);
//                            getVerificationCode(false);
                        }
                        break;
                    case NOT_EXISTS:
                        if (activityModeFindPwd) {
                            popErrorDialog(R.string.register_login_user_not_have);
                        } else {
                            getVerificationCode(false);
                        }
                        break;
                    case TIMEOUT:
                        popErrorDialog(R.string.request_timed_out);
                        break;
                    case NETWORK_ERROR:
                        popErrorDialog(R.string.net_error_tip);
                        break;
                    default:
                        popErrorDialog(R.string.register_failed);
                        break;
                }
            }
        });
    }

    private void checkUserOfPhoneFor3rdAssoc(final Platform platform) {
        updateDialogText(R.string.third_plat_connect_ing);
        mTlcService.checkUserExists(countryCodePhoneNumber, protocol.Message.UserNameType.USR_NAM_TYP_PHONE, new TlcService.OnCheckUserCanRegisterListener() {
            @Override
            public void onStatus(protocol.Message.CheckUserRspMsg rspMsg, int status) {
                switch (status) {
                    case EXISTS:
                        //已存在当前手机账号
                        associateThirdPlat(platform);
                        break;
                    case NOT_EXISTS:
                        dismissDialog();
                        // 到填密码界面
                        EventBus.getDefault().postSticky(new Event.ThirdLoginPlatform(platform));
                        Intent intent = new Intent(RegisterActivity.this, RegisterConfirmPasswordActivity.class);
                        intent.putExtra("activityModeFindPwd", activityModeFindPwd);
                        intent.putExtra("countryCodePhoneNumber", countryCodePhoneNumber);
                        startActivityForResult(intent, ACTIVITY_REQUEST_3RD_ASSOC);
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


    private void associateThirdPlat(final Platform platform) {

        final protocol.Message.UserNameType plat = ThirdLoginPlat.getUserNameType(platform);
        mTlcService.registerUser(countryCodePhoneNumber, protocol.Message.UserNameType.USR_NAM_TYP_PHONE, null,
                protocol.Message.OAuthAccountInfo.newBuilder()
                        .setAvatarUrl(mPlatform.getDb().getUserIcon())
                        .setPlat(plat)
                        .setNickname(mPlatform.getDb().getUserName())
                        .setThirdAccId(mPlatform.getDb().getUserId())
                        .build(),
                new TlcService.OnRegisterUserListener() {
                    @Override
                    public void onStatus(protocol.Message.RegisterRspMsg rspMsg, int status) {
                        switch (status) {
                            case TlcService.OnRegisterUserListener.SUCCESS:
                            case TlcService.OnRegisterUserListener.USER_ALREADY_EXISTS:
                                // 注册（关联）成功，登录
                                RegisterActivity.this.dismissDialog();
                                RegisterActivity.this.setResult(RESULT_OK);
                                RegisterActivity.this.showAssociateSuccess(platform);
                                break;
                            case TlcService.OnRegisterUserListener.ALREADY_ASSOC_OTHER:
                                // 手机号对应的用户已经关联了这个平台的其他账号
                                RegisterActivity.this.dismissDialog();
                                RegisterActivity.this.showTips(plat);
                                break;
                            case TlcService.OnRegisterUserListener.TIMEOUT:
                            case TlcService.OnRegisterUserListener.NETWORK_ERROR:
                                RegisterActivity.this.popErrorDialog(R.string.third_plat_connect_fail_check_network);
                                break;
                            default:
                                RegisterActivity.this.popErrorDialog(R.string.third_plat_connect_fail);
                                break;
                        }

                    }
                });
    }

    private void showAssociateSuccess(final Platform platform) {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setTitle(getString(R.string.third_plat_connect_suc))
                .setMessage(countryCodePhoneNumber)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    doLoginOnAssociateThirdPlatSuccess(platform);
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "showAssociateSuccess");

    }

    private void showTips(protocol.Message.UserNameType plat) {
        String platName = getString(ThirdLoginPlat.getUserNameTypeNameStringRes(plat));
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setTitle(getString(R.string.collect_hint))
                .setMessage(getString(R.string.third_plat_the_number_connect) + platName + getString(R.string.third_plat_ID) + "," + getString(R.string.third_plat_plz_phone_or_last) + platName + getString(R.string.third_plat_plz_ID_login_unbind))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> finish())
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "showTips");
    }


    private void doLoginOnAssociateThirdPlatSuccess(Platform platform) {
        popWaitingDialog(R.string.logging);
        mTlcService.login(platform.getDb().getUserId(), ThirdLoginPlat.getUserNameType(platform), null, new TlcService.OnLoginListener() {
            @Override
            public void onStatus(protocol.Message.LoginRspMsg rspMsg, int status) {
                switch (status) {
                    case SUCCESS:
                        fetchDevices(rspMsg.getUserId());
                        App.getInstance().fetchUserInfo(rspMsg.getUserId());
                        break;
                    case TIMEOUT:
                    case NETWORK_ERROR:
                        popErrorDialog(R.string.third_plat_login_fail_check_network, hud -> finish());
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
                        new SaveDevicesOfUserTask(RegisterActivity.this).execute(rspMsg.getUsrDevAssocList());
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
                popErrorDialog(R.string.third_plat_login_fail_check_network, hud -> finish());
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                // never to here for this exec
            }
        });
    }

    private static class SaveDevicesOfUserTask extends AsyncTask<Object, Object, Boolean> {
        WeakReference<RegisterActivity> mA;

        SaveDevicesOfUserTask(RegisterActivity a) {
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
            RegisterActivity a = mA.get();
            if (a == null)
                return;
            Intent intent = new Intent();
//            如果listIsEmpty 即没有绑定过手表就跳转到绑定界面 否则进入主界面
            if (listIsEmpty) {
                intent.setClass(a, BindDeviceActivity.class);
            } else {
                intent.setClass(a, MainActivity.class);
//                intent.setClass(a, BindDeviceActivity.class);
            }
            a.setResult(LoginActivity.RESULT_LOGIN_OK, intent);
            a.finish();
        }
    }

    private void registerUserSecond() {
        String verificationCode = et_input_verification_code.getText().toString().trim();
        if (StringUtils.isEmpty(verificationCode)) {
            toast(getString(R.string.sms_code_error));
        } else {
            popWaitingDialog(getString(R.string.checking_sms_code));
            SMSSDK.submitVerificationCode(countryCode + "", phoneNumber, verificationCode);
        }
    }


    //sendText sms code
    private void getVerificationCode(boolean isResend) {
        if (isResend) {
            popWaitingDialog(R.string.register_login_ver_code_ing);
        } else {
            updateDialogText(R.string.register_login_ver_code_ing);
        }
        if (NetWorkUtils.isConnect(this)) {
            if (!StringUtils.isEmpty(countryCode + "") && !StringUtils.isEmpty(phoneNumber)) {
                try {
                    L.e(TAG, "country code  :" + countryCode + "    phone number :" + phoneNumber);
                    cn.smssdk.SMSSDK.getVerificationCode(countryCode + "", phoneNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            popErrorDialog(R.string.net_error_tip);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_REQUEST_COUNTRY_CODE:
                if (resultCode == RESULT_OK) {
                    int countryName = data.getIntExtra("countryName", -1);
                    if (countryName > 0) {
//                        tv_selectCountryCode.setText(PhoneNumberUtils.getCountryCode(countryName));
                        tv_sms_countryCode.setText(PhoneNumberUtils.getCountryCode(countryName));
                        countryCode = Integer.parseInt(PhoneNumberUtils.getCountryCode(countryName));
//                        tv_selectCountry.setText(countryName);
                        tv_selectCountry.setText(PhoneNumberUtils.getCountryCode(countryName));
                    }
                }
                break;
            case ACTIVITY_REQUEST_SET_PWD_FOR_REGISTER:
            case ACTIVITY_REQUEST_FIND_PWD:
            case ACTIVITY_REQUEST_3RD_ASSOC:
                if (resultCode == RESULT_OK || resultCode > RESULT_FIRST_USER) {
                    setResult(resultCode, data);
                    finish();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SMSSDK.unregisterEventHandler(eh);
    }

    @Override
    public void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky) {
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity
                                                            oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    public void onBindRequest(@NonNull String toUser, @NonNull Pkt
            reqPkt, @NonNull protocol.Message.NotifyAdminBindDevReqMsg
                                      reqMsg, @NonNull protocol.Message.FetchUsrDevParticRspMsg usrDevPartic) {
    }

    @Override
    public boolean shouldShowExtrudedLoggedOut() {
        return false;
    }

    protected boolean shouldShowServerApiNotCompat() {
        return false;
    }
}
