package com.cqkct.FunKidII.db.Entity;

import com.cqkct.FunKidII.Bean.BaseBean;
import com.google.protobuf.InvalidProtocolBufferException;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Keep;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

import protocol.Message;

@Entity
public class UserEntity implements BaseBean, Serializable {

    private static final long serialVersionUID = 7833862381553484656L;
    @Id(autoincrement = true)
    private Long id;

    @NotNull
    private String userId;
    @NotNull
    private String phone;
    private String email;

    private String qq;
    private String wechat;
    private String sinaWeibo;
    private String facebook;
    private String twitter;
    private String googlePlus;

    private byte[] userInfoData;

    @Transient
    Message.UserInfo userInfo;


    @Generated(hash = 1069255689)
    public UserEntity(Long id, @NotNull String userId, @NotNull String phone, String email,
            String qq, String wechat, String sinaWeibo, String facebook, String twitter,
            String googlePlus, byte[] userInfoData) {
        this.id = id;
        this.userId = userId;
        this.phone = phone;
        this.email = email;
        this.qq = qq;
        this.wechat = wechat;
        this.sinaWeibo = sinaWeibo;
        this.facebook = facebook;
        this.twitter = twitter;
        this.googlePlus = googlePlus;
        this.userInfoData = userInfoData;
    }

    @Generated(hash = 1433178141)
    public UserEntity() {
    }


    @Override
    @Keep
    public Long getId() {
        return id;
    }

    @Keep
    public Message.UserInfo getUserInfo() {
        if (userInfo == null) {
            byte[] data = getUserInfoData();
            if (data == null) {
                data = new byte[0];
            }
            try {
                userInfo = Message.UserInfo.parseFrom(data);
            } catch (InvalidProtocolBufferException e) {
                userInfo = Message.UserInfo.newBuilder().build();
            }
        }
        return userInfo;
    }

    @Keep
    public void setUserInfo(Message.UserInfo userInfo) {
        this.userInfo = userInfo;
        setPhone(userInfo.getPhone());
        setEmail(userInfo.getEmail());
        setQq("");
        setWechat("");
        setSinaWeibo("");
        setFacebook("");
        setTwitter("");
        setGooglePlus("");
        for (Message.OAuthAccountInfo oAuthAccountInfo : userInfo.getOAuthInfoList()) {
            switch (oAuthAccountInfo.getPlat()) {
                case USR_NAM_TYP_3RD_QQ:
                    setQq(oAuthAccountInfo.getThirdAccId());
                    break;
                case USR_NAM_TYP_3RD_WECHAT:
                    setWechat(oAuthAccountInfo.getThirdAccId());
                    break;
                case USR_NAM_TYP_3RD_WEIBO:
                    setSinaWeibo(oAuthAccountInfo.getThirdAccId());
                    break;
                case USR_NAM_TYP_3RD_FACEBOOK:
                    setFacebook(oAuthAccountInfo.getThirdAccId());
                    break;
                case USR_NAM_TYP_3RD_TWITTER:
                    setTwitter(oAuthAccountInfo.getThirdAccId());
                    break;
                case USR_NAM_TYP_3RD_GOOGLEPLUS:
                    setGooglePlus(oAuthAccountInfo.getThirdAccId());
                    break;
                default:
                    break;
            }
        }
        setUserInfoData(userInfo.toByteArray());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getQq() {
        return this.qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }

    public String getWechat() {
        return this.wechat;
    }

    public void setWechat(String wechat) {
        this.wechat = wechat;
    }

    public String getSinaWeibo() {
        return this.sinaWeibo;
    }

    public void setSinaWeibo(String sinaWeibo) {
        this.sinaWeibo = sinaWeibo;
    }

    public String getFacebook() {
        return this.facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getTwitter() {
        return this.twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getGooglePlus() {
        return this.googlePlus;
    }

    public void setGooglePlus(String googlePlus) {
        this.googlePlus = googlePlus;
    }

    public byte[] getUserInfoData() {
        return this.userInfoData;
    }

    public void setUserInfoData(byte[] userInfoData) {
        this.userInfoData = userInfoData;
    }
}
