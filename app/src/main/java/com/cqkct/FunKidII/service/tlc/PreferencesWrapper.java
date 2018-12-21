package com.cqkct.FunKidII.service.tlc;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.Utils.L;

import java.util.HashMap;

public class PreferencesWrapper {
    private static final String TAG = "PreferencesWrapper";

    private static final String PREFERENCES_FILE = "com.cqkct.FunKidII.service.tlc.PreferencesWrapper";

    private static final String PREFERENCE_VERSION = "preference_version";

    private static final String CFG_USER_NAME = "username";
    private static final String CFG_USER_NAME_TYPE = "username_type";
    private static final String CFG_PASSWD = "passwd";
    private static final String USER_ID = "userId";
    private static final String LOGIN_SESSION_ID = "loginSessionId";

    /** 登陆状态，用于判断是否能自动登陆
     *  boolean
     */
    private static final String CFG_CAN_AUTO_LOGIN = "can_auto_login";


    /** 是否在 Wi-Fi 环境下使用 */
    public static final String USE_WIFI_NET = "use_wifi_net";
    /** 是否在 移动网络 环境下使用 */
    public static final String USE_MOBILE_NET = "use_mobile_net";
    /** 是否在 其他网络 环境下使用 */
    public static final String USE_OTHER_NET = "use_other_net";
    /** 是否在 所有网络 环境下使用 */
    public static final String USE_ANYWAY_NET = "use_anyway_net";
    /** 消息通知开关 */
    public static final String USER_NOTIFICATION_CONF = "user_default_notification_options_conf";
    public static final String LISTEN_NUMBER = "LISTEN_NUMBER";

    /** MAP SDK */
    public static final String APP_AREA = "APP_AREA";
    /** 未知地图 */
    public static final int APP_AREA_UNKNOWN = 0;
    /** 高德地图 */
    public static final int APP_AREA_CHINA = 1;
    /** Google 地图 */
    public static final int APP_AREA_OVER_SEA = 2;


    private SharedPreferences mPreference;

    public PreferencesWrapper(Context context) {
        mPreference = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    private static volatile PreferencesWrapper instance;
    public static PreferencesWrapper getInstance(Context context) {
        if (instance == null) {
            synchronized (PreferencesWrapper.class) {
                if (instance == null) {
                    instance = new PreferencesWrapper(context);
                }
            }
        }
        return instance;
    }

    public int getPreferenceVersion() {
        return getPreferenceIntegerValue(PREFERENCE_VERSION, -1);
    }

    public void updatePreferenceVersion(int version) {
        setPreferenceIntegerValue(PREFERENCE_VERSION, version);
    }


    public void setNotificationConf(int notificationConf) {
        setPreferenceIntegerValue(USER_NOTIFICATION_CONF, notificationConf);
    }

    public int getNotificationConf() {
        return getPreferenceIntegerValue(USER_NOTIFICATION_CONF, Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
    }


    public void setListenNumber(String number) {
        setPreferenceStringValue(LISTEN_NUMBER, number);
    }

    public String getListenNumber() {
        return getPreferenceStringValue(LISTEN_NUMBER, "");
    }


    public void setUsername(String username) {
        setPreferenceStringValue(CFG_USER_NAME, username);
    }

    public String getUsername() {
        return getPreferenceStringValue(CFG_USER_NAME, "");
    }

    public void setUsernameType(@NonNull protocol.Message.UserNameType usernameType) {
        setPreferenceIntegerValue(CFG_USER_NAME_TYPE, usernameType.getNumber());
    }

    public protocol.Message.UserNameType getUsernameType() {
        return protocol.Message.UserNameType.valueOf(getPreferenceIntegerValue(CFG_USER_NAME_TYPE, 0));
    }

    public void setPasswd(String passwd) {
        setPreferenceStringValue(CFG_PASSWD, passwd);
    }

    public String getPasswd() {
        return getPreferenceStringValue(CFG_PASSWD, "");
    }


    void setUserId(String userId) {
        setPreferenceStringValue(USER_ID, userId);
    }

    public String getUserId() {
        return getPreferenceStringValue(USER_ID, "");
    }

    void setLoginSessionId(String sessionId) {
        setPreferenceStringValue(LOGIN_SESSION_ID, sessionId);
    }

    String getLoginSessionId() {
        return getPreferenceStringValue(LOGIN_SESSION_ID, "");
    }

    public void setCanAutoLogin(boolean canAutoLogin) {
        setPreferenceBooleanValue(CFG_CAN_AUTO_LOGIN, canAutoLogin);
    }

    public boolean canAutoLogin() {
        return getPreferenceBooleanValue(CFG_CAN_AUTO_LOGIN, false);
    }

    public void setAppArea(int area) {
        setPreferenceIntegerValue(APP_AREA, area);
    }

    public int getAppArea() {
        return getPreferenceIntegerValue(APP_AREA, APP_AREA_UNKNOWN);
    }





    public @Nullable String getPreferenceStringValue(String key) {
        return getPreferenceStringValue(key, null);
    }

    public String getPreferenceStringValue(String key, String defaultValue) {
        return gPrefStringValue(mPreference, key, defaultValue);
    }

    public void setPreferenceStringValue(String key, String newValue) {
        mPreference.edit().putString(key, newValue).apply();
    }

    public boolean getPreferenceBooleanValue(String key) {
        return getPreferenceBooleanValue(key, false);
    }

    public boolean getPreferenceBooleanValue(String key, boolean defaultValue) {
        return gPrefBooleanValue(mPreference, key, defaultValue);
    }

    public void setPreferenceBooleanValue(String key, boolean value) {
        mPreference.edit().putBoolean(key, value).apply();
    }

    public int getPreferenceIntegerValue(String key, int defaultValue) {
        return gPrefIntegerValue(mPreference, key, defaultValue);
    }

    public void setPreferenceIntegerValue(String key, int value) {
        mPreference.edit().putInt(key, value).apply();
    }

    private static String gPrefStringValue(SharedPreferences aPrefs, String key, String def) {
        if (aPrefs == null) {
            return STRING_PREFS.get(key);
        }
        if (STRING_PREFS.containsKey(key)) {
            return aPrefs.getString(key, STRING_PREFS.get(key));
        }
        return aPrefs.getString(key, def);
    }

    private static boolean gPrefBooleanValue(SharedPreferences aPrefs, String key, boolean def) {
        if (aPrefs == null) {
            return BOOLEAN_PREFS.get(key);
        }
        if (BOOLEAN_PREFS.containsKey(key)) {
            return aPrefs.getBoolean(key, BOOLEAN_PREFS.get(key));
        }
        if (aPrefs.contains(key)) {
            return aPrefs.getBoolean(key, def);
        }
        return def;
    }

    private static int gPrefIntegerValue(SharedPreferences aPrefs, String key, int def) {
        if (aPrefs == null) {
            return INTEGER_PREFS.get(key);
        }
        if (INTEGER_PREFS.containsKey(key)) {
            return aPrefs.getInt(key, INTEGER_PREFS.get(key));
        }
        if (aPrefs.contains(key)) {
            return aPrefs.getInt(key, def);
        }
        return def;
    }

    private final static HashMap<String, String> STRING_PREFS = new HashMap<String, String>() {
        {
//            put(SOME, "x");
        }
    };
    private final static HashMap<String, Boolean> BOOLEAN_PREFS = new HashMap<String, Boolean>(){
        {
            put(USE_WIFI_NET, true);
            put(USE_MOBILE_NET, true);
            put(USE_OTHER_NET, true);
            put(USE_ANYWAY_NET, false);
        }
    };
    private final static HashMap<String, Integer>  INTEGER_PREFS = new HashMap<String, Integer>() {
        {
            put(USER_NOTIFICATION_CONF, Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        }
    };

    public void resetAllDefaultValues() {
        SharedPreferences.Editor editor = mPreference.edit();
        for (String key : STRING_PREFS.keySet()) {
            editor.putString(key, STRING_PREFS.get(key));
        }
        for (String key : BOOLEAN_PREFS.keySet()) {
            editor.putBoolean(key, BOOLEAN_PREFS.get(key));
        }
        for (String key : INTEGER_PREFS.keySet()) {
            editor.putInt(key, INTEGER_PREFS.get(key));
        }
        editor.apply();
        updatePreferenceVersion(0);
    }

    public boolean isValidConnection(NetworkInfo ni) {
        if (isValidWifiConnection(ni)) {
            L.d(TAG, "We are valid for WIFI");
            return true;
        }
        if (isValidMobileConnection(ni)) {
            L.d(TAG, "We are valid for MOBILE");
            return true;
        }
        if (isValidOtherConnection(ni)) {
            L.d(TAG, "We are valid for OTHER");
            return true;
        }
        if (isValidAnywayConnection(ni)) {
            L.d(TAG, "We are valid ANYWAY");
            return true;
        }
        return false;
    }

    // Check for wifi
    public boolean isValidWifiConnection(NetworkInfo ni) {
        boolean valid_for_wifi = getPreferenceBooleanValue(USE_WIFI_NET, true);
        // We consider ethernet as wifi
        if (valid_for_wifi && ni != null) {
            int type = ni.getType();
            // Wifi connected
            if (ni.isConnected() &&
                    (type == ConnectivityManager.TYPE_WIFI ||
                            type == ConnectivityManager.TYPE_ETHERNET)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidMobileConnection(NetworkInfo ni) {

        boolean valid_for_cell = getPreferenceBooleanValue(USE_MOBILE_NET, false);

        if(!valid_for_cell && ni != null) {
            if(ni.isRoaming()) {
                return false;
            }
        }

        if (valid_for_cell && ni != null) {
            int type = ni.getType();

            // Any mobile network connected
            if (ni.isConnected() &&
                    // Type 3,4,5 are other mobile data ways
                    (type == ConnectivityManager.TYPE_MOBILE || (type <= 5 && type >= 3))) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidOtherConnection(NetworkInfo ni) {
        boolean valid_for_other = getPreferenceBooleanValue(USE_OTHER_NET, true);
        // boolean valid_for_other = true;
        //noinspection SimplifiableIfStatement
        if (valid_for_other &&
                ni != null &&
                ni.getType() != ConnectivityManager.TYPE_MOBILE
                && ni.getType() != ConnectivityManager.TYPE_WIFI) {
            return ni.isConnected();
        }
        return false;
    }

    public boolean isValidAnywayConnection(NetworkInfo ni) {
        return getPreferenceBooleanValue(USE_ANYWAY_NET, false);
    }
}