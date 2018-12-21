package com.cqkct.FunKidII.Utils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Justin on 2017/7/27.
 */

public class StringUtils {
    public final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    public final static SimpleDateFormat TRACK_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd");

    public final static String TRACK_DATE_FORMAT_STRING = "yyyy-MM-dd";
    public final static String TRACK_HOUR_FORMAT_STRING = "HH:mm";


    public static ByteBuffer UTF8ToUnicode(String strChar) {//将UTF-8字符串变成Unicode字符串 返回值是ByteBuffer
        try {
            byte[] arrByUTF8 = strChar.getBytes("utf-8");

            ByteBuffer arrByUnicode = ByteBuffer.allocate(arrByUTF8.length * 2);

            int nBuf = 0;
            int nBufPos = 0;
            int nNeed = 0;
            int nCur = 0;
            int i = 0;
            int nLen = arrByUTF8.length;
            while (i < nLen) {
                nCur = arrByUTF8[i];
                nCur &= 0xff;
                i++;
                // -----单字节
                if ((nCur & 0x80) == 0) {
                    nNeed = 1;
                    nBuf = 0;
                    arrByUnicode.put((byte) nCur);
                    arrByUnicode.put((byte) 0x00);
                    continue;
                }

                // -----多字节
                else {
                    // 头一个字节
                    if ((nCur & 0x40) != 0) {
                        nBuf = 0;
                        nBufPos = 1;

                        // 单个字符需要2个utf-8字节
                        if ((nCur & 0x20) == 0) {
                            nNeed = 2;
                            nBuf = 0;

                            nCur &= 0x1f;
                            nCur <<= 6;
                            nBuf += nCur;
                        }
                        // 单个字符需要3个utf-8字节
                        else if ((nCur & 0x10) == 0) {
                            nNeed = 3;
                            nBuf = 0;

                            nCur &= 0x0f;
                            nCur <<= 12;
                            nBuf += nCur;
                        }
                        // 单个字符需要4个utf-8字节
                        else if ((nCur & 0x08) == 0) {
                            nNeed = 4;
                            nBuf = 0;

                            nCur &= 0x07;
                            nCur <<= 18;
                            nBuf += nCur;
                        }
                        // 单个字符需要5个utf-8字节
                        else if ((nCur & 0x04) == 0) {
                            nNeed = 5;
                            nBuf = 0;

                            nCur &= 0x03;
                            nCur <<= 24;
                            nBuf += nCur;
                        }
                        // 单个字符需要6个utf-8字节
                        else if ((nCur & 0x02) == 0) {
                            nNeed = 6;
                            nBuf = 0;

                            nCur &= 0x01;
                            nCur <<= 30;
                            nBuf += nCur;
                        }
                    }

                    // 多字节后面的字节
                    else {
                        nCur &= 0x3f;

                        nBufPos++;
                        nCur <<= (nNeed - nBufPos) * 6;
                        nBuf += nCur;

                        // 一个UNICODE字符接收完成
                        if (nBufPos >= nNeed) {
                            nBuf = InvertUintBit(nBuf);
                            if (nNeed <= 3) {
                                nBuf >>= 16;
                                arrByUnicode.putShort((short) nBuf);
                            } else {
                                arrByUnicode.putInt(nBuf);
                            }
                            nBuf = 0;
                        }
                    }
                }
            }
            return arrByUnicode;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getWheelRadius(int FENCE_RADIUS_MIN) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i <= 200; i++) {
            if (FENCE_RADIUS_MIN <= i * 50)
                list.add(String.valueOf(i * 50));
        }
        return list;
    }


    public static String UnicodeToUTF8(ByteBuffer arrByUnicode) {//将Unicode编码的ByteBuffer变为UTF-8编码的字符串
        ByteBuffer arrByUTF8 = null;
        if (arrByUnicode.capacity() < 2) {
            return "";
        }
        // arrByUnicode.position(0);
        arrByUTF8 = ByteBuffer.allocate(arrByUnicode.capacity()
                - arrByUnicode.position());

        int byHeigh = 0;
        int byLow = 0;
        int u1 = 0;
        int u2 = 0;
        int u3 = 0;
        int nSum = 0;

        int nLen = 0;
        try {
            while (true && (arrByUnicode.position() < arrByUnicode.capacity())) {
                // 服务器返回的数据，低位在前
                byLow = arrByUnicode.get();
                byLow &= 0xff;
                byHeigh = arrByUnicode.get();
                byHeigh &= 0xff;

                nLen++;

                // 结尾
                if (byLow == 0 && byHeigh == 0) {
                    break;
                }

                nSum = 0;
                nSum = (byHeigh << 8) & 0xff00;
                nSum += byLow;

                // 1位
                if (nSum <= 0x7f) {
                    arrByUTF8.put((byte) nSum);
                }

                // 2位
                else if (nSum >= 0x80 && nSum <= 0x07ff) {
                    u1 = 0xc0;
                    u1 += ((nSum >> 6) & 0x1f);

                    u2 = 0x80;
                    u2 += (nSum & 0x3f);

                    arrByUTF8.put((byte) u1);
                    arrByUTF8.put((byte) u2);
                }

                // 3位
                else if (nSum >= 0x0800 && nSum <= 0xffff) {
                    u1 = 0xe0;
                    u1 += ((nSum >> 12) & 0x0f);

                    u2 = 0x80;
                    u2 += ((nSum >> 6) & 0x3f);

                    u3 = 0x80;
                    u3 += (nSum & 0x3f);

                    arrByUTF8.put((byte) u1);
                    arrByUTF8.put((byte) u2);
                    arrByUTF8.put((byte) u3);
                }

            }

            String str = new String(arrByUTF8.array());
            return str;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 判断字符串是否为null或者为空.
     *
     * @param str the str
     * @return true, if is empty
     */
    public static boolean isEmpty(String str) {
        if (str == null || str == "" || str.trim().equals("") || str.trim().equals("null")) {
            return true;
        }
        return false;
    }

    /**
     * 返回当前日期的格式化表示.
     *
     * @param date    指定格式化的日期
     * @param formate 格式化参数
     * @return the str date
     */
    public static String getStrDate(Date date, String formate) {
        SimpleDateFormat dd = new SimpleDateFormat(formate);
        return dd.format(date);
    }

    // 把无符号整形4个字节低高位倒置, 0x01 02 03 04 ---> 0x04 03 02 01
    public static int InvertUintBit(int nNum) {
        int nResult = 0;
        int nTemp = 0;

        // 0x00 00 00 04 ---> 0x04 00 00 00
        nTemp = nNum;
        nTemp <<= 24;
        nTemp &= 0xff000000;
        nResult += nTemp;

        // 0x00 00 03 00 ---> 0x00 03 00 00
        nTemp = nNum;
        nTemp <<= 8;
        nTemp &= 0xff0000;
        nResult += nTemp;

        // 0x00 02 00 00 ---> 0x00 00 02 00
        nTemp = nNum;
        nTemp >>= 8;
        nTemp &= 0xff00;
        nResult += nTemp;

        // 0x01 00 00 00 ---> 0x00 00 00 01
        nTemp = nNum;
        nTemp >>= 24;
        nTemp &= 0xff;
        nResult += nTemp;

        return nResult;
    }

    /**
     * 格式一个日期.
     *
     * @param time   the time
     * @param format 格式化参数
     * @return 格式化后的日期
     */
    public static String getStrDate(long time, String format) {
        Date date = new Date(time);
        return getStrDate(date, format);
    }

    public static String getStrDateTime(long time) {
        Date date = new Date(time);
        return SIMPLE_DATE_FORMAT.format(date);
    }

    public static Calendar parseStrToCalendar(String dateStr) { //转换成日历格式
        Calendar calendar = null;
        Date parseDate = parseStrToDate(dateStr);
        if (parseDate != null) {
            calendar = Calendar.getInstance();
            calendar.setTime(parseDate);
            return calendar;
        }
        return calendar;
    }

    public static Date parseStrToDate(String dateStr) {
        try {
            Date date = SIMPLE_DATE_FORMAT.parse(dateStr);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date parseStrToDate(String dateStr, SimpleDateFormat format) { //转换成日期格式
        try {
            Date date = format.parse(dateStr);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
