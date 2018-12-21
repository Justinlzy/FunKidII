package com.cqkct.FunKidII.Utils;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberInputFilter implements InputFilter {

    private final int mMax;

    public PhoneNumberInputFilter(int max) {
        mMax = max;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        StringBuilder ret = new StringBuilder();
        boolean isFirstChar = dstart == 0;
        for (int i = start; i < end; ++i) {
            char c = source.charAt(i);
            if ((isFirstChar && c == '+') // 第一个字符可以为 '+'
                    || (c >= '0' && c <= '9')) {
                ret.append(c);
                isFirstChar = false;
            }
        }


        // 保证长度不超出 mMax

        if (ret.length() <= 0)
            return ret;

        int keep = mMax - (dest.length() - (dend - dstart));
        if (keep <= 0) {
            // 如果超出字数限制，就返回空字符串
            return "";
        } else if (keep >= end - start) {
            // 如果完全满足限制，就返回
            return ret;
        } else {
            if (keep > ret.length())
                keep = ret.length();
            if (Character.isHighSurrogate(ret.charAt(keep - 1))) {
                // 如果最后一位字符是 HighSurrogate（高编码，占2个字符位），就把kepp减1，保证不超出字数限制
                --keep;
                if (keep <= 0) {
                    return "";
                }
            }
            return ret.subSequence(0, keep);
        }
    }
}
