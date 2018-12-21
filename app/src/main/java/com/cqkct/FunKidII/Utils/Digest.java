package com.cqkct.FunKidII.Utils;

import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {
    public static String md5(String string) {
        if (TextUtils.isEmpty(string))
            return "";

        try {
            return ByteUtils.rawToHexStr(MessageDigest.getInstance("MD5").digest(string.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }
}