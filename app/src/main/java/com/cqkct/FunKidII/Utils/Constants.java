package com.cqkct.FunKidII.Utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.WindowManager;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.service.MessageCenterUtils;

/**
 * Created by Justin on 2017/7/31.
 * 资源类
 */

public class Constants {
    //数据库名称
    public static final String DB_NAME = "FunKidII";

    public static final String MOD_SMS_APP_KEY = "1faf0053f3652";
    public static final String MOD_SMS_APP_SECRET = "2a2695f746133ebc4dbdd21ab923c93a";

    //https://app.cqkct.top/feedback/faq?devid=358688006124851&nickname=宝贝2
    public static final String FEEDBACK_URL = "https://app.cqkct.top/feedback/faq?devid=";

    public static final String AMAP_KEY = "eb6831e17f738422149a6102db0a0433";

    public final static String ISNEEDLOG = "isneedlog";
    //监听Home按键的Action
    public final static String HOME_WATCHER_ACTION = "android.intent.action.CLOSE_SYSTEM_DIALOGS";

    public static final int MAP_TYPE_AMAP = 0;//高德地图
    public static final int MAP_TYPE_GOOGLE = 1;//谷歌地图
    public static final double LAT_LON_EPSILON = 0.0000005;

    // 定位精度 颜色
    public static final int FENCE_FILL_COLOR = Color.argb(25, 17, 203, 215);
    public static final int FENCE_STROKE_COLOR = Color.argb(255, 17, 203, 215);
    // 定位图标颜色
    public static final int MAP_MARK_COLOR = Color.argb(100, 29, 161, 242);

    private static SparseArray<Integer> greadMap = new SparseArray<>();

    static {
        greadMap.put(0x00010000, R.string.grade_not_attending_school);
        greadMap.put(0x00010001, R.string.grade_kindergarten);
        greadMap.put(0x00010002, R.string.grade_kindergarten_middle_school);
        greadMap.put(0x00010003, R.string.grade_kindergarten_class);
        greadMap.put(0x00020000, R.string.grade_preschool);
        greadMap.put(0x00030001, R.string.grade_first_grade);
        greadMap.put(0x00030002, R.string.grade_second_grade);
        greadMap.put(0x00030003, R.string.grade_third_grade);
        greadMap.put(0x00030004, R.string.grade_fourth_grade);
        greadMap.put(0x00030005, R.string.grade_fifth_grade);
        greadMap.put(0x00030006, R.string.grade_grade_6);
        greadMap.put(0x00040001, R.string.grade_junior_high_school);
        greadMap.put(0x00040002, R.string.grade_second_grade_junior_high_school);
        greadMap.put(0x00040003, R.string.junior_high_school);
        greadMap.put(0x00050001, R.string.grade_high_school_first_grade);
        greadMap.put(0x00050002, R.string.grade_high_school_second_grade);
        greadMap.put(0x00050003, R.string.grade_high_school_senior);
        greadMap.put(0x00060000, R.string.other);
    }


    public static int getNotifyTypeIconResId(int contentType) {
//        switch (contentType) {
//            case MessageCenterUtils.CONF_BABY:
//            case MessageCenterUtils.CONF_FENCE:
//            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED:
//            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED_RELATION:
//            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED_PERMISSION:
//                return R.drawable.news_phone_set;
//
//            case MessageCenterUtils.CONF_FUNC:
//            case MessageCenterUtils.CONF_FUNC_REFUSE_STRANGER_CALL_IN:
//            case MessageCenterUtils.CONF_FUNC_REFUSE_STRANGER_CALL_OUT:
//            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_ON_OFF:
//            case MessageCenterUtils.CONF_FUNC_INUNDATE_REMIND:
//            case MessageCenterUtils.CONF_FUNC_SAVE_POWER_MODE:
//            case MessageCenterUtils.CONF_FUNC_CALL_POSITION:
//            case MessageCenterUtils.CONF_FUNC_WATCH_SET_LIGHT:
//            case MessageCenterUtils.CONF_FUNC_WATCH_REPORT_LOST:
//            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_ON:
//            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_OFF:
//            case MessageCenterUtils.CONF_FUNC_LOCATION_MODE:
//            case MessageCenterUtils.CONF_FUNC_ENABLE_SMS_AGENT:
//            case MessageCenterUtils.CONF_SOS:
//            case MessageCenterUtils.CONF_CONTACTS:
//            case MessageCenterUtils.CONF_DEV_SYS_INFO:
//            case MessageCenterUtils.CONF_COMPANY_INFO:
//            case MessageCenterUtils.CONF_ALARM_CLOCK:
//            case MessageCenterUtils.CONF_CLASS_DISABLE:
//            case MessageCenterUtils.CONF_COLLECT_PRAISE:
//            case MessageCenterUtils.CONF_SCHOOL_GUARD:
//                return R.drawable.news_watch_set;
//
//            case MessageCenterUtils.BIND_OR_UNBIND:
//            case MessageCenterUtils.ON_REQUEST_BIND:
//            case MessageCenterUtils.ON_BIND:
//            case MessageCenterUtils.ON_UNBIND:
//                return R.drawable.news_information;
//
//            case MessageCenterUtils.INCIDENT_IN_CALL:
//            case MessageCenterUtils.INCIDENT_OUT_CALL:
//            case MessageCenterUtils.INCIDENT_POWER_ON:
//            case MessageCenterUtils.INCIDENT_POWER_OFF:
//            case MessageCenterUtils.INCIDENT_OFF_WRIST:
//            case MessageCenterUtils.INCIDENT_SOAK_WATER:
//                return R.drawable.news_incident;
//
//            case MessageCenterUtils.INCIDENT_REPORT_LOSS:
//            case MessageCenterUtils.INCIDENT_SOS:
//            case MessageCenterUtils.INCIDENT_FENCE:
//            case MessageCenterUtils.INCIDENT_SCHOOL_GUARD:
//            case MessageCenterUtils.INCIDENT_LOW_BATTERY:
//                return R.drawable.news_warning;
//            default:
//                return 0;
//        }

        switch (contentType) {
            case MessageCenterUtils.BIND_OR_UNBIND:
            case MessageCenterUtils.ON_REQUEST_BIND:
            case MessageCenterUtils.ON_BIND:
            case MessageCenterUtils.ON_UNBIND:
                return R.drawable.news_binding;
            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED:
            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED_RELATION:
            case MessageCenterUtils.ON_USR_DEV_ASSOC_MODIFIED_PERMISSION:
                return R.drawable.news_watch_set;


            case MessageCenterUtils.INCIDENT_IN_CALL:
            case MessageCenterUtils.INCIDENT_OUT_CALL:
            case MessageCenterUtils.INCIDENT_SOS:
            case MessageCenterUtils.INCIDENT_LOW_BATTERY:
            case MessageCenterUtils.INCIDENT_POWER_ON:
            case MessageCenterUtils.INCIDENT_POWER_OFF:

            case MessageCenterUtils.INCIDENT_OFF_WRIST:
            case MessageCenterUtils.INCIDENT_SOAK_WATER:
            case MessageCenterUtils.INCIDENT_REPORT_LOSS:
                return R.drawable.news_warning;

            case MessageCenterUtils.INCIDENT_FENCE:
                return R.drawable.news_incident;
            case MessageCenterUtils.INCIDENT_SCHOOL_GUARD:
                return R.drawable.news_incident;


            case MessageCenterUtils.CONF_DEV_SYS_INFO:

            case MessageCenterUtils.CONF_FUNC:
            case MessageCenterUtils.CONF_FUNC_LOCATION_MODE:
            case MessageCenterUtils.CONF_FUNC_REFUSE_STRANGER_CALL_IN:
            case MessageCenterUtils.CONF_FUNC_REFUSE_STRANGER_CALL_OUT:
            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_ON_OFF:
            case MessageCenterUtils.CONF_FUNC_INUNDATE_REMIND:
            case MessageCenterUtils.CONF_FUNC_SAVE_POWER_MODE:
            case MessageCenterUtils.CONF_FUNC_CALL_POSITION:
            case MessageCenterUtils.CONF_FUNC_WATCH_SET_LIGHT:
            case MessageCenterUtils.CONF_FUNC_WATCH_REPORT_LOST:
            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_ON:
            case MessageCenterUtils.CONF_FUNC_TIMER_POWER_OFF:
            case MessageCenterUtils.CONF_FUNC_ENABLE_CALCULATOR:
            case MessageCenterUtils.CONF_FUNC_ENABLE_SMS_AGENT:
                return R.drawable.news_set_message;


            case MessageCenterUtils.CONF_BABY:
                return R.drawable.news_watch_set;

            case MessageCenterUtils.CONF_COMPANY_INFO:
                return R.drawable.news_set_message;

            case MessageCenterUtils.CONF_FENCE:
                return R.drawable.news_incident;

            case MessageCenterUtils.CONF_SOS:
                return R.drawable.news_watch_set;

            case MessageCenterUtils.CONF_CONTACTS:
                return R.drawable.news_watch_set;

            case MessageCenterUtils.CONF_ALARM_CLOCK:
                return R.drawable.news_set_message;

            case MessageCenterUtils.CONF_CLASS_DISABLE:
                return R.drawable.news_set_message;

            case MessageCenterUtils.CONF_SCHOOL_GUARD:
                return R.drawable.news_incident;

            case MessageCenterUtils.CONF_COLLECT_PRAISE:
                return R.drawable.news_set_message;

            default:
                return 0;
        }
    }


    /**
     * 读取联系人信息
     *
     * @param uri
     */
    public static String[] getPhoneContacts(Uri uri, Context context) {
        String[] contact = new String[2];
        //得到ContentResolver对象
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            //取得联系人姓名
            int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            contact[0] = cursor.getString(nameFieldColumnIndex);
            contact[1] = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            L.i("contacts", contact[0]);
            L.i("contactsUsername", contact[1]);
            if (!TextUtils.isEmpty(contact[1])) {
                contact[1] = contact[1].replace(" ", "");
                contact[1] = contact[1].replace("-", "");
            }
            cursor.close();
        } else {
            return null;
        }
        return contact;
    }

    /**
     * 改变背景颜色
     */
    private void darkenBackground(Float bgColor, Activity activity) {
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.alpha = bgColor;
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        activity.getWindow().setAttributes(lp);
    }

}
