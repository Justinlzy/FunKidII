package com.cqkct.FunKidII.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.StringRes;
import android.util.SparseArray;

import com.cqkct.FunKidII.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberUtils {


    private static final Map<Integer, String> countryCodeNameMap = new HashMap<>();
//    static {
//        countryCodeNameMap.put("+853", R.string.register_login_macao_district);
//        countryCodeNameMap.put("+86", R.string.register_login_china);
//        countryCodeNameMap.put("+84", R.string.register_login_vietnam);
//        countryCodeNameMap.put("+62", R.string.register_login_indonesia);
//        countryCodeNameMap.put("+91", R.string.register_login_india);
//        countryCodeNameMap.put("+98", R.string.register_login_iran);
//        countryCodeNameMap.put("+65", R.string.register_login_singapore);
//        countryCodeNameMap.put("+852", R.string.register_login_hk_district);
//        countryCodeNameMap.put("+886", R.string.register_login_taiwan_district);
//        countryCodeNameMap.put("+66", R.string.register_login_thailand);
//        countryCodeNameMap.put("+95", R.string.register_login_burma);
//        countryCodeNameMap.put("+1", R.string.register_login_american);
//        countryCodeNameMap.put("+60", R.string.register_login_malaysia);
//        countryCodeNameMap.put("+856", R.string.register_login_laos);
//        countryCodeNameMap.put("+855", R.string.register_login_cambodia);
//        countryCodeNameMap.put("+63", R.string.register_login_philippines);
//    }

//    static {
//        countryCodeNameMap.put("+853", R.string.register_login_macao_district);
//        countryCodeNameMap.put("+86", R.string.register_login_china);
//        countryCodeNameMap.put("+886", R.string.register_login_taiwan_district);
//        countryCodeNameMap.put("+1", R.string.register_login_american);
//        countryCodeNameMap.put("+1", R.string.register_login_canada);
//        countryCodeNameMap.put("+852", R.string.register_login_hk_district);
//        countryCodeNameMap.put("+44", R.string.register_login_england);
//    }

    static {
        countryCodeNameMap.put(R.string.register_login_macao_district, "+853");
        countryCodeNameMap.put(R.string.register_login_china, "+86");
        countryCodeNameMap.put(R.string.register_login_taiwan_district, "+886");
        countryCodeNameMap.put(R.string.register_login_american, "+1");
        countryCodeNameMap.put(R.string.register_login_canada, "+1");
        countryCodeNameMap.put(R.string.register_login_hk_district, "+852");
        countryCodeNameMap.put(R.string.register_login_england, "+44");
    }

    private final SparseArray<String> countryNameCodeMap = new SparseArray<>();
    private final List<String> countryNameCodeList = new ArrayList<>();

    private PhoneNumberUtils() {
        init();
    }

    private void init() {
        for (Map.Entry<Integer, String> one : countryCodeNameMap.entrySet()) {
            countryNameCodeMap.put(one.getKey(), one.getValue());
            countryNameCodeList.add(one.getValue());
        }
        Collections.sort(countryNameCodeList);
    }


    private static volatile PhoneNumberUtils instance;

    private static PhoneNumberUtils getInstance() {
        if (instance == null) {
            synchronized (Constants.class) {
                if (instance == null) {
                    instance = new PhoneNumberUtils();
                }
            }
        }
        return instance;
    }

    public static String getCountryCode(@StringRes int countryNameStrResId) {
        String countrycode = getInstance().countryNameCodeMap.get(countryNameStrResId);
        if (countrycode == null)
            return "";
        return countrycode;
    }

    public static @StringRes
    int getCountryName(String countryCode) {
        Integer stringResId = getKeyByValue(countryCode);
        if (stringResId == null)
            return 0;
        return stringResId;
    }

    private static Integer getKeyByValue(String value) {
        Set set = ((Map) PhoneNumberUtils.countryCodeNameMap).entrySet();
        Integer s = null;
        for (Object aSet : set) {
            Map.Entry entry = (Map.Entry) aSet;
            if (entry.getValue().equals(value)) {
                s = (int) entry.getKey();
            }
        }
        return s;
    }

    public static String getCountryName(Context context, String countryCode) {
        int stringResId = getCountryName(countryCode);
        if (stringResId != 0)
            return context.getString(stringResId);
        return "";
    }

    public static String pickCountryCodeFromNumber(String num) {
        if (num == null)
            return null;
        if (num.isEmpty() || num.charAt(0) != '+') {
            return "";
        }
        List<String> list = getInstance().countryNameCodeList;
        int idx = Collections.binarySearch(list, num, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o2.startsWith(o1))
                    return 0;
                return o1.compareTo(o2);
            }
        });
        if (idx < 0)
            return "+";
        return list.get(idx);
    }


    public boolean isValidMobileNumber(String number) {
        Pattern p = Pattern.compile("^\\+?\\d+$");
        Matcher m = p.matcher(number);
        return m.matches();
    }

    /**
     * 检验手机号
     *
     * @param number 号码
     * @return boolean
     */
    public static boolean isValidChineseMobileNumber(String number) {
        String regPattern = "^(\\+86)?(13[0-9]|14[579]|15[0-3,5-9]|16[6]|17[0135678]|18[0-9]|19[89])\\d{8}$";
        return number.matches(regPattern);
    }

    /**
     * Sos 适配 110 119 120 122
     *
     * @param mobileNo
     * @return
     */
    public static boolean isSosPhone(String mobileNo) {
        return mobileNo.equals("110") || mobileNo.equals("119") || mobileNo.equals("120") || mobileNo.equals("122");
    }
}