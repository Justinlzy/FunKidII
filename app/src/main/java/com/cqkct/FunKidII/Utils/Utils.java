package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.TranslateAnimation;
import com.cqkct.FunKidII.R;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Locale;

import protocol.Message;

import static com.cqkct.FunKidII.Utils.PublicTools.dip2px;


public class Utils {


    public static void setNumberPickerDividerColor(NumberPicker numberPicker, Context context) {
        NumberPicker picker = numberPicker;
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    //设置分割线的颜色值
                    pf.set(picker, new ColorDrawable(context.getResources().getColor(R.color.transparent)));
                } catch (IllegalArgumentException | Resources.NotFoundException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public static void setNumberPickerTextColor(NumberPicker numberPicker, int color) {
        final int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText) {
                try {
                    Field selectorWheelPaintField = numberPicker.getClass()
                            .getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);
                    ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(color);
                    ((EditText) child).setTextColor(color);
                    numberPicker.invalidate();
                } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    public static String getLanguage() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        return language;
    }


    public static String getCountry() {
        Locale locale = Locale.getDefault();
        String country = locale.getCountry();
        return country;
    }


    public static boolean isPasswdLengthEnough(String passwd) {
        return !TextUtils.isEmpty(passwd) && passwd.length() >= 6;
    }

    public static boolean isPasswdLengthValid(String passwd) {
        return isPasswdLengthEnough(passwd) && passwd.length() <= 16;
    }

    public static boolean isPasswdValid(String passwd) {
        if (!isPasswdLengthValid(passwd)) {
            return false;
        }
        for (char c : passwd.toCharArray()) {
            if (!Character.isDigit(c) && !Character.isAlphabetic(c))
                return false;
        }
        return true;
    }

    public static long epochToMillis(int epochTime) {
        long millis = epochTime;
        millis &= 0xFFFFFFFFL;
        millis *= 1000;
        return millis;
    }

    public static Date epochToDate(int epochTime) {
        return new Date(epochToMillis(epochTime));
    }


    public static int charSequenceLength_zhCN(CharSequence c) {
        int len = 0;
        for (int i = 0; i < c.length(); i++) {
            int tmp = c.charAt(i);
            if (tmp >= 0 && tmp <= 127) {
                len += 1;
            } else {
                len += 2;
            }
        }
        return (len + 1) >>> 1;
    }

    public static String getLocateTypeString(Context context, protocol.Message.Position.LocateType type) {
        return getLocateTypeString(context, type.getNumber());
    }

    public static String getLocateTypeString(Context context, int type) {
        switch (type) {
            case Message.Position.LocateType.GPS_VALUE:
                return context.getString(R.string.locate_type_GPS);
            case protocol.Message.Position.LocateType.BDS_VALUE:
                return context.getString(R.string.locate_type_BDS);
            case protocol.Message.Position.LocateType.GALILEO_VALUE:
                return context.getString(R.string.locate_type_GALILEO);
            case protocol.Message.Position.LocateType.GLONASS_VALUE:
                return context.getString(R.string.locate_type_GLONASS);
            case protocol.Message.Position.LocateType.CELL_VALUE:
                return context.getString(R.string.locate_type_cell);
            case protocol.Message.Position.LocateType.WIFI_VALUE:
                return context.getString(R.string.locate_type_WiFi);
            case protocol.Message.Position.LocateType.HYBRID_VALUE:
                return context.getString(R.string.locate_type_HYBRID);
            default:
                return "";
        }
    }

    public static String bytesText(long bytes) {
        if (bytes > 1024 * 1024 * 1000) {
            return String.format(Locale.getDefault(), "%.2fGB", (bytes * 1.0) / (1024 * 1024 * 1024));
        }
        if (bytes > 1024 * 1000) {
            return String.format(Locale.getDefault(), "%.2fMB", (bytes * 1.0) / (1024 * 1024));
        }
        if (bytes > 1000) {
            return String.format(Locale.getDefault(), "%.2fKB", (bytes * 1.0) / 1024);
        }
        return String.format(Locale.getDefault(), "%dB", bytes);
    }


    /**
     * 屏幕中心marker 跳动
     */
    public static void startJumpAnimation(Marker marker, AMap aMap, Context context) {
        if (marker != null) {
            //根据屏幕距离计算需要移动的目标点
            final LatLng latLng = marker.getPosition();
            Point point = aMap.getProjection().toScreenLocation(latLng);
            point.y -= dip2px(context, 50);
            LatLng target = aMap.getProjection()
                    .fromScreenLocation(point);
            //使用TranslateAnimation,填写一个需要移动的目标点
            Animation animation = new TranslateAnimation(target);
            animation.setInterpolator(new Interpolator() {
                @Override
                public float getInterpolation(float input) {
                    // 模拟重加速度的interpolator
                    if (input <= 0.5) {
                        return (float) (0.5f - 2 * (0.5 - input) * (0.5 - input));
                    } else {
                        return (float) (0.5f - Math.sqrt((input - 0.5f) * (1.5f - input)));
                    }
                }
            });
            //整个移动所需要的时间
            animation.setDuration(600);
            //设置动画
            marker.setAnimation(animation);
            //开始动画
            marker.startAnimation();

        } else {
            Log.e("amap", "screenMarker is null");
        }
    }

}
