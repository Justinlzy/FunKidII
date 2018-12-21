package com.cqkct.FunKidII.Utils;

import android.support.annotation.StringRes;

import com.cqkct.FunKidII.R;

import java.util.HashMap;
import java.util.Map;

import cn.sharesdk.facebook.Facebook;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.google.GooglePlus;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.twitter.Twitter;
import cn.sharesdk.wechat.friends.Wechat;
import protocol.Message;

public class ThirdLoginPlat {
    private static final Map<String, Message.UserNameType> platMap = new HashMap<>();
    static {
        platMap.put(Wechat.NAME, Message.UserNameType.USR_NAM_TYP_3RD_WECHAT);
        platMap.put(QQ.NAME, Message.UserNameType.USR_NAM_TYP_3RD_QQ);
        platMap.put(SinaWeibo.NAME, Message.UserNameType.USR_NAM_TYP_3RD_WEIBO);
        platMap.put(Twitter.NAME, Message.UserNameType.USR_NAM_TYP_3RD_TWITTER);
        platMap.put(Facebook.NAME, Message.UserNameType.USR_NAM_TYP_3RD_FACEBOOK);
        platMap.put(GooglePlus.NAME, Message.UserNameType.USR_NAM_TYP_3RD_GOOGLEPLUS);
    }
    public static Message.UserNameType getUserNameType(Platform platform) {
        Message.UserNameType type = platMap.get(platform.getName());
        if (type == null) {
            type = Message.UserNameType.USR_NAM_TYP_UNKNOWN;
        }
        return type;
    }

    private static final Map<Message.UserNameType, Integer> thirdPlatNameMap = new HashMap<>();
    static {
        thirdPlatNameMap.put(protocol.Message.UserNameType.USR_NAM_TYP_3RD_QQ, R.string.third_plat_qq);
        thirdPlatNameMap.put(protocol.Message.UserNameType.USR_NAM_TYP_3RD_WECHAT, R.string.third_plat_wechat);
        thirdPlatNameMap.put(protocol.Message.UserNameType.USR_NAM_TYP_3RD_WEIBO, R.string.third_plat_weibo);
        thirdPlatNameMap.put(protocol.Message.UserNameType.USR_NAM_TYP_3RD_FACEBOOK, R.string.third_plat_facebook);
        thirdPlatNameMap.put(protocol.Message.UserNameType.USR_NAM_TYP_3RD_TWITTER, R.string.third_plat_twitter);
        thirdPlatNameMap.put(protocol.Message.UserNameType.USR_NAM_TYP_3RD_GOOGLEPLUS, R.string.third_plat_google);
    }
    public static @StringRes int getUserNameTypeNameStringRes(Message.UserNameType type) {
        Integer id = thirdPlatNameMap.get(type);
        if (id == null)
            return 0;
        return id;
    }

    private static final Map<String, Integer> platStrResMap = new HashMap<>();
    static {
        platStrResMap.put(Wechat.NAME, R.string.third_plat_wechat);
        platStrResMap.put(QQ.NAME, R.string.third_plat_qq);
        platStrResMap.put(SinaWeibo.NAME, R.string.third_plat_weibo);
        platStrResMap.put(Twitter.NAME, R.string.third_plat_twitter);
        platStrResMap.put(Facebook.NAME, R.string.third_plat_facebook);
        platStrResMap.put(GooglePlus.NAME, R.string.third_plat_google);
    }
    public static @StringRes int getUserNameTypeNameStringRes(String sharesdkPlatName) {
        Integer id  = platStrResMap.get(sharesdkPlatName);
        if (id == null)
            return 0;
        return id;
    }
}
