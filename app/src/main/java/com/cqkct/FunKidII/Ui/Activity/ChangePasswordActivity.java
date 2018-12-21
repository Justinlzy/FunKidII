package com.cqkct.FunKidII.Ui.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.service.tlc.TlcService;


/**
 * Created by justin on 2017/8/8.
 */

public class ChangePasswordActivity extends BaseActivity implements View.OnClickListener {
    public static final String TAG = ChangePasswordActivity.class.getSimpleName();

    private EditText ed_password1, ed_password2, ed_password3;
    private ImageView iv_passwordEye1, iv_passwordEye2, iv_passwordEye3;
    private boolean passwordVisible1, passwordVisible2, passwordVisible3;
    private String password1, password2, password3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password);
        init();
    }

    private void init() {
        TextView title = findViewById(R.id.dialog_title);
        title.setText(R.string.change_password);

        iv_passwordEye1 = findViewById(R.id.iv_look_psw);
        iv_passwordEye2 = findViewById(R.id.iv_look_psw2);
        iv_passwordEye3 = findViewById(R.id.iv_look_psw3);
        findViewById(R.id.ok).setOnClickListener(v -> changePassword());
        findViewById(R.id.cancel).setOnClickListener(v -> this.finish());
        ed_password1 = findViewById(R.id.et_psw);
        ed_password1.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        ed_password1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                password1 = ed_password1.getText().toString();
            }
        });


        ed_password2 = findViewById(R.id.et_psw2);
        ed_password2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                password2 = ed_password2.getText().toString();
            }
        });
        ed_password2.setOnFocusChangeListener((v, hasFocus) -> {
        });
        ed_password3 = findViewById(R.id.et_psw3);
        ed_password3.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                password3 = ed_password3.getText().toString();
            }
        });

    }


    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.iv_look_psw: {
                int editPosition = ed_password1.getSelectionEnd();
                if (passwordVisible1) {
                    ed_password1.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    iv_passwordEye1.setImageResource(R.drawable.psw_hide);
                } else {
                    ed_password1.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    iv_passwordEye1.setImageResource(R.drawable.psw_display);
                }
                passwordVisible1 = !passwordVisible1;
                ed_password1.setSelection(editPosition);
            }
            break;
            case R.id.iv_look_psw2: {
                int editPosition = ed_password2.getSelectionEnd();
                if (passwordVisible2) {
                    ed_password2.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    iv_passwordEye2.setImageResource(R.drawable.psw_hide);
                } else {
                    ed_password2.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    iv_passwordEye2.setImageResource(R.drawable.psw_display);
                }
                passwordVisible2 = !passwordVisible2;
                ed_password2.setSelection(editPosition);
            }
            break;
            case R.id.iv_look_psw3: {
                int editPosition = ed_password3.getSelectionEnd();
                if (passwordVisible3) {
                    ed_password3.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    iv_passwordEye3.setImageResource(R.drawable.psw_hide);
                } else {
                    ed_password3.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    iv_passwordEye3.setImageResource(R.drawable.psw_display);
                }
                passwordVisible3 = !passwordVisible3;
                ed_password3.setSelection(editPosition);
            }
            break;
        }
    }

    private void changePassword() {
        if (StringUtils.isEmpty(password1) || StringUtils.isEmpty(password2) || StringUtils.isEmpty(password3)) {
            toast(R.string.please_input_all_password);
            return;
        }

        if (!Utils.isPasswdValid(password1) || !Utils.isPasswdValid(password2)) {
            toast(R.string.passwd_invalid_tip);
            return;
        }

        if (!password2.equals(password3)) {
            toast(R.string.twice_password_diffent);
            return;
        }

        if (password2.equals(password1)) {
            toast(getString(R.string.newpassword_oldpassword_same));
            return;
        }

        popWaitingDialog(R.string.app_setting_changing);

        mTlcService.changePasswd(password1, password3, (rspMsg, status) -> {
            switch (status) {
                case TlcService.OnChangePasswdListener.SUCCESS:
                    popSuccessDialog(R.string.app_setting_change_ok, true, hud -> {
                        try {
                            mTlcService.logout();
                        } catch (Exception e) {
                            L.e(TAG, "mTlcService.logout() error", e);
                        }
                    });
                    break;
                case TlcService.OnChangePasswdListener.NETWORK_ERROR:
                    popErrorDialog(R.string.network_quality_poor);
                    break;
                case TlcService.OnChangePasswdListener.TIMEOUT:
                    popErrorDialog(R.string.app_setting_change_timeout);
                    break;
                case TlcService.OnChangePasswdListener.EAGIN:
                    popErrorDialog(R.string.network_quality_poor);
                    break;
                case TlcService.OnChangePasswdListener.OLD_PASSWD_ERROR:
                    popErrorDialog(R.string.app_setting_original_password_error);
                    break;
                default:
                    popErrorDialog(R.string.app_setting_change_failed);
                    break;
            }
        });

    }

}
