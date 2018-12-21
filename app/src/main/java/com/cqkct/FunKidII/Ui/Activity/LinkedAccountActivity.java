package com.cqkct.FunKidII.Ui.Activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.BlurActivity.BaseBlurActivity;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Ui.view.PullBackLayout;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.Utils.ThirdLoginPlat;
import com.cqkct.FunKidII.Utils.Utils;
import com.cqkct.FunKidII.db.Dao.UserEntityDao;
import com.cqkct.FunKidII.db.Entity.UserEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


public class LinkedAccountActivity extends BaseBlurActivity implements PlatformActionListener {
    private static final String TAG = LinkedAccountActivity.class.getSimpleName();

    private boolean qqAssociate = false, wechatAssociate = false, googleAssociate = false, facebookAssociate = false, twitterAssociate = false;
    private TextView tvQQAssociateState, tvWechatAssociateState, tvGoogleAssociateState, tvFacebookAssociateState, tvTwitterAssociateState;
    private Map<Message.UserNameType, Message.OAuthAccountInfo> map = new HashMap<>();
    private TextView phoneView;
    private String phone;
    private TextView dialog_title;
    private int StartIndexZh = 7;
    private int StartIndexEn = 29;
    private int EndIndexEnLogin = 24;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linked_account);
        initView();


        popWaitingDialog(R.string.please_wait);
        loadData();
    }

    private void initView() {
        ((PullBackLayout) findViewById(R.id.pull_back_layout)).setCallback(new PullBackLayout.Callback() {
            @Override
            public void onPullStart() {
            }

            @Override
            public void onPullDown(float progress) {
            }

            @Override
            public void onPullUp() {
            }

            @Override
            public void onPullCancel() {
            }

            @Override
            public void onPullComplete() {
                finish();
                overridePendingTransition(0, R.anim.out_to_bottom);
            }
        });
        //设置标题
        dialog_title = findViewById(R.id.dialog_title);
        dialog_title.setText(R.string.other_login_connected_account);
        phoneView = findViewById(R.id.tv_phone_bind);
        tvQQAssociateState = findViewById(R.id.tv_qq_bind);
        tvWechatAssociateState = findViewById(R.id.tv_wechat_bind);
        tvGoogleAssociateState = findViewById(R.id.tv_google_bind);
        tvFacebookAssociateState = findViewById(R.id.tv_facebook_bind);
        tvTwitterAssociateState = findViewById(R.id.tv_twitter_bind);
    }

    private void initViewData() {
        phoneView.setText(phone);
        for (Message.UserNameType userNameType : map.keySet()) {
            if (userNameType == protocol.Message.UserNameType.USR_NAM_TYP_3RD_QQ) {
                qqAssociate = true;
            } else if (userNameType == protocol.Message.UserNameType.USR_NAM_TYP_3RD_WECHAT) {
                wechatAssociate = true;
            } else if (userNameType == Message.UserNameType.USR_NAM_TYP_3RD_GOOGLEPLUS) {
                googleAssociate = true;
            } else if (userNameType == Message.UserNameType.USR_NAM_TYP_3RD_FACEBOOK) {
                facebookAssociate = true;
            } else if (userNameType == Message.UserNameType.USR_NAM_TYP_3RD_TWITTER) {
                twitterAssociate = true;
            }
        }
        tvQQAssociateState.setText(qqAssociate ? getString(R.string.other_login_connected) : getString(R.string.other_login_not_connected));
        tvWechatAssociateState.setText(wechatAssociate ? getString(R.string.other_login_connected) : getString(R.string.other_login_not_connected));
        tvGoogleAssociateState.setText(googleAssociate ? getString(R.string.other_login_connected) : getString(R.string.other_login_not_connected));
        tvFacebookAssociateState.setText(facebookAssociate ? getString(R.string.other_login_connected) : getString(R.string.other_login_not_connected));
        tvTwitterAssociateState.setText(twitterAssociate ? getString(R.string.other_login_connected) : getString(R.string.other_login_not_connected));
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        Platform platform = null;
        switch (viewId) {
            case R.id.wechat:
                if (wechatAssociate)
                    showUnAssociateDialog(Message.UserNameType.USR_NAM_TYP_3RD_WECHAT);
                else
                    platform = ShareSDK.getPlatform(Wechat.NAME);
                break;
            case R.id.qq:
                if (qqAssociate)
                    showUnAssociateDialog(Message.UserNameType.USR_NAM_TYP_3RD_QQ);
                else
                    platform = ShareSDK.getPlatform(QQ.NAME);
                break;
            case R.id.google:
                if (googleAssociate)
                    showUnAssociateDialog(Message.UserNameType.USR_NAM_TYP_3RD_GOOGLEPLUS);
                else
                    platform = ShareSDK.getPlatform(GooglePlus.NAME);
                break;
            case R.id.facebook:
                if (facebookAssociate)
                    showUnAssociateDialog(Message.UserNameType.USR_NAM_TYP_3RD_FACEBOOK);
                else
                    platform = ShareSDK.getPlatform(Facebook.NAME);
                break;
            case R.id.twitter:
                if (twitterAssociate)
                    showUnAssociateDialog(Message.UserNameType.USR_NAM_TYP_3RD_TWITTER);
                else
                    platform = ShareSDK.getPlatform(Twitter.NAME);
                break;
            case R.id.slide_down_icon:
                finish();
                overridePendingTransition(0, R.anim.out_to_bottom);
                return;
            default:
                break;
        }
        if (platform != null) {
            authorize(platform);
        }
    }

    private void loadData() {
        String userId = mUserId;
        if (TextUtils.isEmpty(userId)) {
            L.e(TAG, "loadData userId is empty");
            finish();
            return;
        }
        List<UserEntity> list = GreenUtils.getUserEntityDao().queryBuilder().where(UserEntityDao.Properties.UserId.eq(userId)).list();
        if (!list.isEmpty()) {
            Message.UserInfo userInfo = list.get(0).getUserInfo();
            phone = userInfo.getPhone();
            map.clear();
            for (Message.OAuthAccountInfo oAuthAccountInfo : userInfo.getOAuthInfoList()) {
                map.put(oAuthAccountInfo.getPlat(), oAuthAccountInfo);
            }
            initViewData();
        }

        getUserInfo(userId);
    }

    private void getUserInfo(String userId) {
        exec(Message.FetchUserInfoReqMsg.newBuilder()
                .setUserId(userId)
                .build(), new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    Message.FetchUserInfoRspMsg rspMsg = response.getProtoBufMsg();
                    if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                        GreenUtils.saveUserInfo(rspMsg.getUserInfo());
                        Message.UserInfo userInfo = rspMsg.getUserInfo();
                        phone = userInfo.getPhone();
                        map.clear();
                        for (Message.OAuthAccountInfo oAuthAccountInfo : userInfo.getOAuthInfoList()) {
                            map.put(oAuthAccountInfo.getPlat(), oAuthAccountInfo);
                        }
                        initViewData();
                        dismissDialog();
                        return false;
                    }
                } catch (Exception e) {
                    L.e(TAG, "getUserInfo onResponse", e);
                }
                popErrorDialog(R.string.load_failure);
                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.e(TAG, "getUserInfo onException", cause);
                popErrorDialog(R.string.load_failure);
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

            }
        });
    }

    private void showUnAssociateDialog(final Message.UserNameType userNameType) {
        //改变userNameType颜色 Html.fromHtml();
        String tipMsg = getString(R.string.other_login_remove_connected,
                        getString(ThirdLoginPlat.getUserNameTypeNameStringRes(userNameType)));
        if (getPreferencesWrapper().getUsernameType() == userNameType) {
            tipMsg += "\n" + getString(R.string.other_login_now_you_use_which_login_will_logout,
                    getString(ThirdLoginPlat.getUserNameTypeNameStringRes(userNameType))
                            );
        }
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setMessage(tipMsg, true)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LinkedAccountActivity.this.unAssociateThirdPlat(userNameType);
                        LinkedAccountActivity.this.popWaitingDialog(R.string.please_wait);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "UnLinkedAccountDialog");
    }

    private void unAssociateThirdPlat(final Message.UserNameType userNameType) {

        Message.DisassociateThirdAccountReqMsg disassociateThirdAccountReqMsg =
                Message.DisassociateThirdAccountReqMsg.newBuilder()
                        .setUserId(mUserId)
                        .setPlat(userNameType)
                        .setThirdAccId(map.get(userNameType).getThirdAccId())
                        .build();

        exec(disassociateThirdAccountReqMsg, new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    Message.DisassociateThirdAccountRspMsg rspMsg = response.getProtoBufMsg();
                    if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                        map.remove(userNameType);
                        Platform platform = null;
                        switch (userNameType) {
                            case USR_NAM_TYP_3RD_QQ:
                                platform = ShareSDK.getPlatform(QQ.NAME);
                                qqAssociate = false;
                                break;
                            case USR_NAM_TYP_3RD_WECHAT:
                                platform = ShareSDK.getPlatform(Wechat.NAME);
                                wechatAssociate = false;
                                break;
                            case USR_NAM_TYP_3RD_GOOGLEPLUS:
                                platform = ShareSDK.getPlatform(GooglePlus.NAME);
                                googleAssociate = false;
                                break;
                            case USR_NAM_TYP_3RD_FACEBOOK:
                                platform = ShareSDK.getPlatform(Facebook.NAME);
                                facebookAssociate = false;
                                break;
                            case USR_NAM_TYP_3RD_TWITTER:
                                platform = ShareSDK.getPlatform(Twitter.NAME);
                                twitterAssociate = false;
                                break;
                            default:
                                break;
                        }
                        if (platform != null) {
                            platform.removeAccount(true);
                        }
                        dismissDialog();
                        initViewData();
                        if (getPreferencesWrapper().getUsernameType() == userNameType) {
                            try {
                                mTlcService.logout();
                            } catch (Exception e) {
                                L.e(TAG, "mTlcService.logout() error", e);
                            }
                            return false;
                        }
                    } else {
                        popErrorDialog(R.string.submit_failure);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                if (cause instanceof TimeoutException)
                    popErrorDialog(R.string.net_timeout);
                else
                    popErrorDialog(R.string.submit_failure);
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

            }
        });
    }


    //关联第三方账号
    private void associateThirdPlat(final Platform sharesdkPlat) {
        final String userId = mUserId;
        if (TextUtils.isEmpty(userId)) {
            L.e(TAG, "associateThirdPlat userId is empty");
            finish();
            return;
        }
        updateDialogText(R.string.third_plat_connect_ing);
        final protocol.Message.UserNameType plat = ThirdLoginPlat.getUserNameType(sharesdkPlat);
        final Message.OAuthAccountInfo oAuthAccountInfo = Message.OAuthAccountInfo.newBuilder()
                .setPlat(plat)
                .setThirdAccId(sharesdkPlat.getDb().getUserId())
                .setNickname(sharesdkPlat.getDb().getUserName())
                .setAvatarUrl(sharesdkPlat.getDb().getUserIcon())
                .build();
        Message.AssociateThirdAccountReqMsg reqMsg = Message.AssociateThirdAccountReqMsg.newBuilder()
                .setUserId(userId)
                .setOAuthInfo(oAuthAccountInfo)
                .build();
        exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.AssociateThirdAccountRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    map.put(plat, oAuthAccountInfo);
                                    switch (plat) {
                                        case USR_NAM_TYP_3RD_QQ:
                                            qqAssociate = true;
                                            break;
                                        case USR_NAM_TYP_3RD_WECHAT:
                                            wechatAssociate = true;
                                            break;
                                        case USR_NAM_TYP_3RD_GOOGLEPLUS:
                                            googleAssociate = true;
                                            break;
                                        case USR_NAM_TYP_3RD_FACEBOOK:
                                            facebookAssociate = true;
                                            break;
                                        case USR_NAM_TYP_3RD_TWITTER:
                                            twitterAssociate = true;
                                            break;
                                        default:
                                            break;
                                    }
                                    initViewData();
                                    dismissDialog();
                                    return false;
                                case ALREADY_ASSOC_OTHER:
                                    //对应的平台账号已经存在
                                    getUserInfo(userId);
                                    return false;
                                case ALREADY_ASSOC_OTHER_USER:
                                    showThisThirdPlatAssociatedOtherUser(sharesdkPlat);
                                    dismissDialog();
                                    return false;
                                case NO_USER:
                                    //未找到对应的 user
                                default:
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "associateThirdPlat onResponse", e);
                        }
                        popErrorDialog(R.string.third_plat_connect_fail);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "associateThirdPlat onException", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.third_plat_connect_fail_check_network);
                        } else {
                            popErrorDialog(R.string.third_plat_connect_fail);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void showThisThirdPlatAssociatedOtherUser(final Platform sharesdkPlat) {
        Message.UserNameType plat = ThirdLoginPlat.getUserNameType(sharesdkPlat);
        String platStr = getString(ThirdLoginPlat.getUserNameTypeNameStringRes(plat));

        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setTitle(getString(R.string.collect_hint))
                .setMessage(getString(R.string.already_assoc_other_user, platStr, platStr))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> sharesdkPlat.removeAccount(true))
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "showThisThirdPlatAssociatedOtherUser");

    }

    private void authorize(Platform plat) {
        plat.removeAccount(true);
        plat.SSOSetting(false);
        plat.setPlatformActionListener(this);
        if (plat.isAuthValid()) {
            //判断是否已经存在授权状态，可以根据自己的登录逻辑设置
            //已授权
            popWaitingDialog(R.string.please_wait);
            associateThirdPlat(plat);
            return;
        }
        popWaitingDialog(getString(R.string.please_wait));
        plat.showUser(null);//授权并获取用户信息
    }

    @Override
    public void onComplete(final Platform platform, int i, HashMap<String, Object> hashMap) {
        L.i(TAG, "OAuth2 onComplete " + platform.getName());
        runOnUiThread(() -> associateThirdPlat(platform));
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {
        L.e(TAG, "OAuth2 onError " + platform.getName(), throwable);
        runOnUiThread(() -> popErrorDialog(R.string.other_login_fail));
    }

    @Override
    public void onCancel(Platform platform, int i) {
        L.i(TAG, "OAuth2 onCancel " + platform.getName());
        runOnUiThread(this::dismissDialog);
    }
}