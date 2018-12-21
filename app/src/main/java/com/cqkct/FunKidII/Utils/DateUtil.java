package com.cqkct.FunKidII.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.CustomDate;
import com.cqkct.FunKidII.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

//import com.mtk.data.CustomDate;

public class DateUtil {

    public static long getTimeAtHM(int timeHour, int timeMin) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, timeHour);
        calendar.set(Calendar.MINUTE, timeMin);
        return calendar.getTimeInMillis() / 1000;
    }

    /**
     * 流水号 ：时分秒+四位自增数字
     */
    private static int sequence = 0;

    private static int length = 4;

    public static synchronized String getBusinessNumber() {

        sequence = sequence >= 9999 ? 1 : sequence + 1;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = simpleDateFormat.format(new Date());
        String s = Integer.toString(sequence);
        String[] string = time.split(":");
        String times = string[0] + string[1] + string[2];
        return times + addLeftZero(s, length);
    }

    public static String addLeftZero(String s, int length) {
        // StringBuilder sb=new StringBuilder();
        int old = s.length();
        if (length > old) {
            char[] c = new char[length];
            char[] x = s.toCharArray();
            if (x.length > length) {
                throw new IllegalArgumentException(
                        "Numeric value is larger than intended length: " + s
                                + " LEN " + length);
            }
            int lim = c.length - x.length;
            for (int i = 0; i < lim; i++) {
                c[i] = '0';
            }
            System.arraycopy(x, 0, c, lim, x.length);
            return new String(c);
        }
        return s.substring(0, length);

    }

    public static String getTimeFormatOfDay(int hourOfDay, Context context) {
        String dateForamt = "";
        if (hourOfDay < 5) {
            dateForamt = context.getString(R.string.before_dawn);
        } else if (hourOfDay < 12) {
            dateForamt = context.getString(R.string.morning);
        } else if (hourOfDay <= 13) {
            dateForamt = context.getString(R.string.noon);
        } else if (hourOfDay <= 20) {
            dateForamt = context.getString(R.string.afternoon);
        } else {
            dateForamt = context.getString(R.string.night);
        }
        return dateForamt;
    }

    public static boolean isOnTheWeek(Date date) {

        Date currentDate = new Date();
        int timeBegin, timeEnd;
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        timeEnd = (int) (cal.getTimeInMillis() / 1000);

        cal.add(Calendar.DAY_OF_MONTH, -7);
        cal.add(Calendar.SECOND, 1);
        timeBegin = (int) (cal.getTimeInMillis() / 1000);
        int currentDt = (int) (date.getTime() / 1000);

        return timeBegin <= currentDt && timeEnd >= currentDt;
    }

    public static boolean inSameDay(Date date1, Date Date2) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        int year1 = calendar.get(Calendar.YEAR);
        int day1 = calendar.get(Calendar.DAY_OF_YEAR);

        calendar.setTime(Date2);
        int year2 = calendar.get(Calendar.YEAR);
        int day2 = calendar.get(Calendar.DAY_OF_YEAR);

        if ((year1 == year2) && (day1 == day2)) {
            return true;
        }
        return false;
    }

    public static int getMonthDays(int year, int month) {
        if (month > 12) {
            month = 1;
            year += 1;
        } else if (month < 1) {
            month = 12;
            year -= 1;
        }
        int[] arr = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int days = 0;

        if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
            arr[1] = 29; //
        }

        try {
            days = arr[month - 1];
        } catch (Exception e) {
            e.getStackTrace();
        }

        return days;
    }

    public static int getYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static int getMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    public static int getCurrentMonthDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    public static int getWeekDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
    }

    public static int getHour() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinute() {
        return Calendar.getInstance().get(Calendar.MINUTE);
    }

    public static CustomDate getNextSunday() {

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 7 - getWeekDay() + 1);
        CustomDate date = new CustomDate(c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return date;
    }

    public static int[] getWeekSunday(int year, int month, int day, int pervious) {
        int[] time = new int[3];
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.add(Calendar.DAY_OF_MONTH, pervious);
        time[0] = c.get(Calendar.YEAR);
        time[1] = c.get(Calendar.MONTH) + 1;
        time[2] = c.get(Calendar.DAY_OF_MONTH);
        return time;

    }

    public static int getWeekDayFromDate(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (week_index < 0) {
            week_index = 0;
        }
        return week_index;
    }

    @SuppressLint("SimpleDateFormat")
    public static Date getDateFromString(int year, int month) {
        String dateString = year + "-" + (month > 9 ? month : ("0" + month))
                + "-01";
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
        return date;
    }

    public static boolean isToday(CustomDate date) {
        return (date.year == DateUtil.getYear()
                && date.month == DateUtil.getMonth() && date.day == DateUtil
                .getCurrentMonthDay());
    }

    public static boolean isCurrentMonth(CustomDate date) {
        return (date.year == DateUtil.getYear() && date.month == DateUtil
                .getMonth());
    }

    public static String timezoneISO8601() {
        int offset = TimeZone.getDefault().getRawOffset();
        char ch = offset < 0 ? '-' : '+';
        offset = Math.abs(offset);
        offset /= 1000;
        int hour = offset / 60 / 60;
        int minute = (offset % 60) / 60;
        int sec = (offset % (60 * 60));
        return String.format("%c%02d:%02d:%02d", ch, hour, minute, sec);
    }

    public static TimeZone parseTimeZone(String zoneString) {
        if (TextUtils.isEmpty(zoneString) || zoneString.equals("Z")) {
            return TimeZone.getTimeZone("UTC");
        }
        if (zoneString.charAt(0) == '-' || zoneString.charAt(0) == '+') {
            if (zoneString.length() > 1) {
                boolean hasColon = false;
                boolean isISO8601 = true;
                for (char ch : zoneString.toCharArray()) {
                    if (Character.isDigit(ch) || ch == '-' || ch == '+') {
                        continue;
                    }
                    if (ch == ':') {
                        hasColon = true;
                        continue;
                    }
                    isISO8601 = false;
                    break;
                }
                if (isISO8601) {
                    if (!hasColon) {
                        // +08
                        // +0800
                        // +080000
                        StringBuilder sb = new StringBuilder();
                        sb.append(zoneString.substring(0, 3));
                        sb.append(':');
                        if (zoneString.length() >= 5) {
                            sb.append(zoneString.substring(3, 5));
                        } else {
                            sb.append("00");
                        }
                        zoneString = sb.toString();
                    } else if (zoneString.length() > 6) {
                        // +08:00:00
                        zoneString = zoneString.substring(0, 6);
                    }
                    zoneString = "GMT" + zoneString;
                }
            }
        }
        return TimeZone.getTimeZone(zoneString);
    }
}
