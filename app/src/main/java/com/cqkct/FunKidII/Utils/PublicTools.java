package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.db.Entity.ClassDisableEntity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import protocol.Message;

/**
 * Created by justin on 2017/8/1.
 */

public class PublicTools {
    /**
     * 拨打电话
     *
     * @param phoneNum
     * @param mContext
     */
    public static void call(String phoneNum, Context mContext) {

        if (TextUtils.isEmpty(phoneNum)) { //电话号码为空
            L.e("手机号码为空");
            return;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL);
        Uri uri = Uri.parse("tel:" + phoneNum);   //设置要操作界面的具体内容  拨打电话固定格式： tel：
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    //是否是中文环境
    public static boolean isZh() {
        try {
            Locale locale = App.getInstance().getResources().getConfiguration().locale;
            String language = locale.getLanguage();
            if (language.endsWith("zh"))
                return true;
            else
                return false;
        } catch (Exception e) {

        }
        return false;
    }

    //中文返回数据中 去掉字符串中的邮编数据
    // 例如  中国上海市徐汇区虹梅街道虹星社区老年活动室 邮政编码: 200231
    //去掉后就变成 中国上海市徐汇区虹梅街道虹星社区老年活动室
    public static String getNoZipCodeStr(String source) {
        String nozipCodeStr = source;
        if (!StringUtils.isEmpty(nozipCodeStr)) {
            int lastEmptyStrIndex = nozipCodeStr.lastIndexOf(" ");
            if (lastEmptyStrIndex != -1) {
                nozipCodeStr = nozipCodeStr.substring(0, lastEmptyStrIndex);
                lastEmptyStrIndex = nozipCodeStr.lastIndexOf(" ");
                if (lastEmptyStrIndex != -1) {
                    nozipCodeStr = nozipCodeStr.substring(0, lastEmptyStrIndex);
                }
            }
        }
        return nozipCodeStr;
    }

    /**
     * dip2px：根据手机的分辨率从 dp 的单位 转成为 px(像素) (这里描述这个方法适用条件 – 可选)
     * <p>
     *
     * @param context
     * @param dpValue
     * @return int
     * @throws
     * @since 1.0.0
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int dp2px(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }


    /**
     * @deprecated use @{{@link #getDecoderWeak(Context, int)}}
     */
    public static String getDecoderWeak(int flag, Context context) {
        return getDecoderWeak(context, flag);
    }

    public static String getDecoderWeak(Context context, int flag) {
        if ((flag & Message.TimePoint.RepeatFlag.ALL_VALUE) == 0) {
            return context.getString(R.string.none);
        }
        if ((flag & Message.TimePoint.RepeatFlag.ALL_VALUE) == Message.TimePoint.RepeatFlag.ALL_VALUE) {
            return context.getString(R.string.week_everyday);
        }
        if ((flag & Message.TimePoint.RepeatFlag.ALL_VALUE) == (Message.TimePoint.RepeatFlag.MON_VALUE |
                Message.TimePoint.RepeatFlag.TUE_VALUE |
                Message.TimePoint.RepeatFlag.WED_VALUE |
                Message.TimePoint.RepeatFlag.THU_VALUE |
                Message.TimePoint.RepeatFlag.FRI_VALUE)) {
            return context.getString(R.string.work_day);
        }
        StringBuilder sb = new StringBuilder();
        String comma = context.getString(R.string.sep_char_comma);
        String sep = "";
        if ((flag & Message.TimePoint.RepeatFlag.MON_VALUE) != 0) {
            sb.append(sep).append(context.getString(R.string.week_short_text_monday));
            sep = comma;
        }
        if ((flag & Message.TimePoint.RepeatFlag.TUE_VALUE) != 0) {
            sb.append(sep).append(context.getString(R.string.week_short_text_tuesday));
            sep = comma;
        }
        if ((flag & Message.TimePoint.RepeatFlag.WED_VALUE) != 0) {
            sb.append(sep).append(context.getString(R.string.week_short_text_wednesday));
            sep = comma;
        }
        if ((flag & Message.TimePoint.RepeatFlag.THU_VALUE) != 0) {
            sb.append(sep).append(context.getString(R.string.week_short_text_thursday));
            sep = comma;
        }
        if ((flag & Message.TimePoint.RepeatFlag.FRI_VALUE) != 0) {
            sb.append(sep).append(context.getString(R.string.week_short_text_friday));
            sep = comma;
        }
        if ((flag & Message.TimePoint.RepeatFlag.SAT_VALUE) != 0) {
            sb.append(sep).append(context.getString(R.string.week_short_text_saturday));
            sep = comma;
        }
        if ((flag & Message.TimePoint.RepeatFlag.SUN_VALUE) != 0) {
            sb.append(sep).append(context.getString(R.string.week_short_text_sunday));
        }
        return sb.toString();
    }

    private static final SparseIntArray bitOfdayOfWeek = new SparseIntArray();

    static {
        bitOfdayOfWeek.put(Calendar.SUNDAY, Message.TimePoint.RepeatFlag.SUN_VALUE);
        bitOfdayOfWeek.put(Calendar.MONDAY, Message.TimePoint.RepeatFlag.MON_VALUE);
        bitOfdayOfWeek.put(Calendar.TUESDAY, Message.TimePoint.RepeatFlag.TUE_VALUE);
        bitOfdayOfWeek.put(Calendar.WEDNESDAY, Message.TimePoint.RepeatFlag.WED_VALUE);
        bitOfdayOfWeek.put(Calendar.THURSDAY, Message.TimePoint.RepeatFlag.THU_VALUE);
        bitOfdayOfWeek.put(Calendar.FRIDAY, Message.TimePoint.RepeatFlag.FRI_VALUE);
        bitOfdayOfWeek.put(Calendar.SATURDAY, Message.TimePoint.RepeatFlag.SAT_VALUE);
    }



    public static boolean isInClassDisable(List<ClassDisableEntity> classDisables) {
        Calendar calNow = Calendar.getInstance();

        Calendar begin = (Calendar) calNow.clone();
        begin.set(Calendar.SECOND, 0);
        begin.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) begin.clone();

        for (ClassDisableEntity one : classDisables) {
            if (one.getEnable()) {
                if ((one.getRepeat() & Message.TimePoint.RepeatFlag.ALL_VALUE) != 0) {
                    TimeZone tz = DateUtil.parseTimeZone(one.getTimezone());
                    calNow.setTimeZone(tz);
                    int dayOfWeek = calNow.get(Calendar.DAY_OF_WEEK);
                    if ((one.getRepeat() & bitOfdayOfWeek.get(dayOfWeek)) != 0) {
                        begin.setTimeZone(tz);
                        begin.set(Calendar.HOUR_OF_DAY, one.getBeginHour());
                        begin.set(Calendar.MINUTE, one.getBeginMinute());

                        end.setTimeZone(tz);
                        end.set(Calendar.HOUR_OF_DAY, one.getEndHour());
                        end.set(Calendar.MINUTE, one.getEndMinute());

                        if (calNow.after(begin) && calNow.before(end)) {
                            return true;
                        }
                    }
                } else {
                    begin.setTimeInMillis(one.getBeginTime() * 1000L);
                    end.setTimeInMillis(one.getEndTime() * 1000L);
                    if (calNow.after(begin) && calNow.before(end)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 隐藏软盘
     * @param context
     */
    public static void hideInputMethod(Context context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        assert inputMethodManager != null;
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static String calTimeDiff(String time1, String time2) throws ParseException {
        StringBuilder sb = new StringBuilder();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d1 = df.parse(time1);
        Date d2 = df.parse(time2);
        long diff = d1.getTime() - d2.getTime();//这样得到的差值是微秒级别

        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff - days * (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (diff - days * (1000 * 60 * 60 * 24) - hours * (1000 * 60 * 60)) / (1000 * 60);

        if (days > 0) sb.append(days).append("天");

        if (hours > 0) sb.append(hours).append("小时");

        if (minutes > 0) sb.append(minutes).append(minutes);

        return sb.toString();
    }

    public static String genTimeText(Context context, Calendar now, Calendar time) {
        if (time.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            if (time.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                return StringUtils.getStrDate(time.getTime(), "HH:mm");
            }
            if (time.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1) {
                return context.getString(R.string.message_time_yesterday) + StringUtils.getStrDate(time.getTime(), " HH:mm");
            }
            if (time.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                return StringUtils.getStrDate(time.getTime(), "E HH:mm");
            }
        }
        return StringUtils.getStrDate(time.getTime(), "yyyy-MM-dd HH:mm");
    }

    public static String genDayText(Context context, Calendar now, Calendar time) {
        if (time.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            if (time.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                return StringUtils.getStrDate(time.getTime(), "HH:mm");
            }
            if (time.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1) {
                return context.getString(R.string.message_time_yesterday) + StringUtils.getStrDate(time.getTime(), " HH:mm");
            }
            if (time.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                return StringUtils.getStrDate(time.getTime(), "E HH:mm");
            }
        }
        return StringUtils.getStrDate(time.getTime(), "yyyy-MM-dd");
    }
}
