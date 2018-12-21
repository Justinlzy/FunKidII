package com.cqkct.FunKidII.Utils;

import android.util.Log;

public class L {
    private static boolean isDebug = true; // 是否需要打印 bug，可以在 application 的 onCreate 函数里面初始化
    private static final String TAG = "FunKidII";

    public static void setIsDebug(Boolean isD) {
        isDebug = isD;
    }

    public static void v(String msg) {
        if (isDebug)
            Log.v(TAG, msg);
    }

    public static void d(String msg) {
        if (isDebug)
            Log.d(TAG, msg);
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void v(String tag, String msg) {
        if (isDebug)
            Log.v(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (isDebug)
            Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (isDebug)
            Log.v(tag, msg, tr);
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (isDebug)
            Log.d(tag, msg, tr);
    }

    public static void i(String tag, String msg, Throwable tr) {
        Log.i(tag, msg, tr);
    }

    public static void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }
}
