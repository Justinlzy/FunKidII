package com.cqkct.FunKidII.glide;

import android.text.TextUtils;

import com.cqkct.FunKidII.service.OkHttpRequestManager;

import java.util.Collections;
import java.util.List;

public class Api {

    public static String getURL(Avatar model, int width, int height) {
        if (TextUtils.isEmpty(model.getFilename()) || TextUtils.isEmpty(model.getAuthToken()) || TextUtils.isEmpty(model.getResourceToken()))
            return "";
        return OkHttpRequestManager.makeFileDownloadUrl(model.getFilename(), model.getAuthToken(), model.getResourceToken());
    }

    public static List<String> getAlternateUrls(Avatar model, int width, int height) {
        return Collections.emptyList();
    }
}
