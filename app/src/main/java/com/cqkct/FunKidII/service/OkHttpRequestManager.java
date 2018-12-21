package com.cqkct.FunKidII.service;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.cqkct.FunKidII.Ui.Activity.AppSettingActivity;
import com.cqkct.FunKidII.Utils.Digest;
import com.cqkct.FunKidII.Utils.L;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class OkHttpRequestManager {
    private static final String TAG = OkHttpRequestManager.class.getSimpleName();

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");//mdiatype 这个需要和服务端保持一致
    private static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");//mdiatype 这个需要和服务端保持一致
    private static final MediaType MEDIA_OBJECT_STREAM = MediaType.parse("application/octet-stream");//mdiatype 这个需要和服务端保持一致
    private static final MediaType MEDIA_FORM_DATA = MediaType.parse("multipart/form-data");//mdiatype 这个需要和服务端保持一致

    private static final String BASE_URL = "https://cqkct.com/resource"; // 请求接口根地址
    public static final String HEAD_ICON_UPLOAD_URL = "https://kid.cqkct.com/resource/upload.json"; // 请求接口根地址
    public static final String HEAD_ICON_DOWNLOAD_URL = "https://kid.cqkct.com/resource/download";

    //google 搜索
    public static final String GOOGLEMAP_SEARCH_PLACE_BY_KEY_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    public static final String GOOGLEMAP_ANDROID_KEY = "AIzaSyC--ogpte6S2GqFZ9ZrIqtOGdE87_U0JTg";
    public static final String GOOGLEMAP_WEB_KEY = "AIzaSyBnRnEduldbbMjMQs9eS4zsxOszVhtyzWA";


    public static final String APP_CHECK_VERSION = "https://app.cqkct.com/package/getLastVersion?os=android";
    public static final String APP_UPDATE_DOWNLOAD = "https://app.cqkct.com/resource/android/dis/apk/FunKidII.apk";

    private static volatile OkHttpRequestManager mInstance;//单利引用
    public static final int TYPE_GET = 0;//get请求
    public static final int TYPE_POST_JSON = 1;//post请求参数为json
    public static final int TYPE_POST_FORM = 2;//post请求参数为表单
    private OkHttpClient mOkHttpClient;//okHttpClient 实例
    private Handler okHttpHandler;//全局处理子线程和M主线程通信

    /**
     * 初始化RequestManager
     */
    public OkHttpRequestManager(Context context) {
        //初始化OkHttpClient
        mOkHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)//设置超时时间
                .readTimeout(10, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(10, TimeUnit.SECONDS)//设置写入超时时间
                .build();

        //初始化Handler
        okHttpHandler = new Handler(context.getMainLooper());
    }

    /**
     * 获取单例引用
     *
     * @return
     */
    public static OkHttpRequestManager getInstance(Context context) {
        OkHttpRequestManager inst = mInstance;
        if (inst == null) {
            synchronized (OkHttpRequestManager.class) {
                inst = mInstance;
                if (inst == null) {
                    inst = new OkHttpRequestManager(context.getApplicationContext());
                    mInstance = inst;
                }
            }
        }
        return inst;
    }

    /**
     * okHttp同步请求统一入口
     *
     * @param actionUrl   接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     */
    public void requestSyn(String actionUrl, int requestType, HashMap<String, String> paramsMap) {
        switch (requestType) {
            case TYPE_GET:
                requestGetBySyn(actionUrl, paramsMap);
                break;
            case TYPE_POST_JSON:
                requestPostBySyn(actionUrl, paramsMap);
                break;
            case TYPE_POST_FORM:
                requestPostBySynWithForm(actionUrl, paramsMap);
                break;
        }
    }

    /**
     * okHttp get同步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     */
    private void requestGetBySyn(String actionUrl, HashMap<String, String> paramsMap) {
        StringBuilder tempParams = new StringBuilder();
        try {
            //处理参数
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                //对参数进行URLEncoder
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            //补全请求地址
            String requestUrl = String.format("%s/%s?%s", BASE_URL, actionUrl, tempParams.toString());
            //创建一个请求
            Request request = addHeaders().url(requestUrl).build();
            //创建一个Call
            final Call call = mOkHttpClient.newCall(request);
            //执行请求
            final Response response = call.execute();
            response.body().string();
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
    }

    /**
     * okHttp post同步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     */
    private void requestPostBySyn(String actionUrl, HashMap<String, String> paramsMap) {
        try {
            //处理参数
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            //补全请求地址
            String requestUrl = String.format("%s/%s", BASE_URL, actionUrl);
            //生成参数
            String params = tempParams.toString();
            //创建一个请求实体对象 RequestBody
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params);
            //创建一个请求
            final Request request = addHeaders().url(requestUrl).post(body).build();
            //创建一个Call
            final Call call = mOkHttpClient.newCall(request);
            //执行请求
            Response response = call.execute();
            //请求执行成功
            if (response.isSuccessful()) {
                //获取返回数据 可以是String，bytes ,byteStream
                L.e(TAG, "response ----->" + response.body().string());
            }
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
    }

    /**
     * okHttp post同步请求表单提交
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     */
    private void requestPostBySynWithForm(String actionUrl, HashMap<String, String> paramsMap) {
        try {
            //创建一个FormBody.Builder
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : paramsMap.keySet()) {
                //追加表单信息
                builder.add(key, paramsMap.get(key));
            }
            //生成表单实体对象
            RequestBody formBody = builder.build();
            //补全请求地址
            String requestUrl = String.format("%s/%s", BASE_URL, actionUrl);
            //创建一个请求
            final Request request = addHeaders().url(requestUrl).post(formBody).build();
            //创建一个Call
            final Call call = mOkHttpClient.newCall(request);
            //执行请求
            Response response = call.execute();
            if (response.isSuccessful()) {
                L.e(TAG, "response ----->" + response.body().string());
            }
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
    }

    /**
     * okHttp异步请求统一入口
     *
     * @param actionUrl   接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     * @param callBack    请求返回数据回调
     * @param <T>         数据泛型
     **/
    public <T> Call requestAsyn(String actionUrl, int requestType, HashMap<String, String> paramsMap, ReqCallBack<T> callBack) {
        Call call = null;
        switch (requestType) {
            case TYPE_GET:
                call = requestGetByAsyn(actionUrl, paramsMap, callBack);
                break;
            case TYPE_POST_JSON:
                call = requestPostByAsyn(actionUrl, paramsMap, callBack);
                break;
            case TYPE_POST_FORM:
                call = requestPostByAsynWithForm(actionUrl, paramsMap, callBack);
                break;
        }
        return call;
    }

    /**
     * okHttp get异步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @param <T>       数据泛型
     * @return
     */
    private <T> Call requestGetByAsyn(String actionUrl, HashMap<String, String> paramsMap, final ReqCallBack<T> callBack) {
        StringBuilder tempParams = new StringBuilder();
        try {
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            String requestUrl = String.format("%s/%s?%s", BASE_URL, actionUrl, tempParams.toString());
            final Request request = addHeaders().url(requestUrl).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedCallBack("访问失败", callBack);
                    L.e(TAG, e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        L.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("服务器错误", callBack);
                    }
                }
            });
            return call;
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * okHttp post异步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @param <T>       数据泛型
     * @return
     */
    private <T> Call requestPostByAsyn(String actionUrl, HashMap<String, String> paramsMap, final ReqCallBack<T> callBack) {
        try {
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            String params = tempParams.toString();
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params);
            String requestUrl = String.format("%s/%s", BASE_URL, actionUrl);
            final Request request = addHeaders().url(requestUrl).post(body).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedCallBack("访问失败", callBack);
                    L.e(TAG, e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        L.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("服务器错误", callBack);
                    }
                }
            });
            return call;
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * okHttp post异步请求表单提交
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @param <T>       数据泛型
     * @return
     */
    private <T> Call requestPostByAsynWithForm(String actionUrl, HashMap<String, String> paramsMap, final ReqCallBack<T> callBack) {
        try {
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : paramsMap.keySet()) {
                builder.add(key, paramsMap.get(key));
            }
            RequestBody formBody = builder.build();
            String requestUrl = String.format("%s/%s", BASE_URL, actionUrl);
            final Request request = addHeaders().url(requestUrl).post(formBody).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    failedCallBack("访问失败", callBack);
                    L.e(TAG, e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        L.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("服务器错误", callBack);
                    }
                }
            });
            return call;
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
        return null;
    }

    public <T> void updateAPK(String url, final ReqCallBack<T> callBack) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e(TAG, "updateAPK onFailure...");
                callBack.onReqFailed(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                AppSettingActivity.UpdateInfo updateInfo = new AppSettingActivity.UpdateInfo();
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    if (!jsonObject.getBoolean("success")) {
                        callBack.onReqFailed(responseBody);
                    } else {
                        JSONObject verJson = new JSONObject(jsonObject.getString("data"));
                        updateInfo.setVersion(verJson.getString("verStr"));
                        updateInfo.setVersionCode(verJson.getInt("verNum"));
                        updateInfo.setLength(verJson.getLong("length"));
                        L.v(TAG, "code: " + updateInfo.getVersion() +
                                "version_code: " + updateInfo.getVersionCode());
                        callBack.onReqSuccess((T) updateInfo);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    callBack.onReqFailed(e.getMessage());
                }
            }
        });
    }

    public <T> void getPlaceIdBySearchKey(final String searchKey, final LatLng latLng, final int radius, final ReqCallBack<T> callBack) {

        String urlStr = OkHttpRequestManager.GOOGLEMAP_SEARCH_PLACE_BY_KEY_URL +
                "?" + "query=" + searchKey +
                "&" + "location=" + latLng.latitude + "," + latLng.longitude +
                "&" + "radius=" + radius +
                "&" + "key=" + OkHttpRequestManager.GOOGLEMAP_ANDROID_KEY;
        L.e(TAG, "getPlaceIdBySearchKey, urlStr: " + urlStr);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(urlStr)
                .build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e(TAG, "googleMap search place onFailure...");
                callBack.onReqFailed(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    L.e(TAG, "getPlaceIdBySearchKey JSONObject: " + jsonObject.toString());
                    callBack.onReqSuccess((T) jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    callBack.onReqFailed(e.getMessage());
                }
            }
        });


    }


    public interface ReqCallBack<T> {
        /**
         * 响应成功
         */
        void onReqSuccess(T result);

        /**
         * 响应失败
         */
        void onReqFailed(String errorMsg);
    }

    /**
     * 统一为请求添加头信息
     *
     * @return
     */
    private Request.Builder addHeaders() {
        return new Request.Builder()
                .addHeader("Connection", "keep-alive");
    }

    /**
     * 统一同意处理成功信息
     *
     * @param result
     * @param callBack
     * @param <T>
     */
    private <T> void successCallBack(final T result, final ReqCallBack<T> callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onReqSuccess(result);
                }
            }
        });
    }

    /**
     * 统一处理失败信息
     *
     * @param errorMsg
     * @param callBack
     * @param <T>
     */
    private <T> void failedCallBack(final String errorMsg, final ReqCallBack<T> callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onReqFailed(errorMsg);
                }
            }
        });
    }

    /**
     * 上传文件
     *
     * @param url      地址
     * @param filePath 本地文件地址
     */
    public <T> void upLoadFile(String url, String filePath, final ReqCallBack<T> callBack) {
        //补全请求地址
        //创建File
        File file = new File(filePath);
        //创建RequestBody
        RequestBody body = RequestBody.create(MEDIA_OBJECT_STREAM, file);
        //创建Request
        final Request request = new Request.Builder().url(url).post(body).build();
        final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e(TAG, e.toString());
                failedCallBack("上传失败", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String string = response.body().string();
                    L.e(TAG, "response ----->" + string);
                    successCallBack((T) string, callBack);
                } else {
                    failedCallBack("上传失败", callBack);
                }
            }
        });
    }

    /**
     * 上传文件
     *
     * @param url       地址
     * @param paramsMap 参数
     * @param callBack  回调
     * @param <T>
     */
    public <T> void upLoadFile(String url, HashMap<String, Object> paramsMap, final ReqCallBack<T> callBack) {
        try {
            //补全请求地址
            String requestUrl = url;
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //设置类型
            builder.setType(MultipartBody.FORM);
            //追加参数
            for (String key : paramsMap.keySet()) {
                Object object = paramsMap.get(key);
                if (!(object instanceof File)) {
                    builder.addFormDataPart(key, object.toString());
                } else {
                    File file = (File) object;
                    builder.addFormDataPart(key, file.getName(), RequestBody.create(null, file));
                }
            }
            //创建RequestBody
            RequestBody body = builder.build();
            //创建Request
            final Request request = new Request.Builder().url(requestUrl).post(body).build();
            //单独设置参数 比如读取超时时间
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    L.e(TAG, e.toString());
                    failedCallBack("上传失败", callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        L.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("上传失败", callBack);
                    }
                }
            });
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
    }

    /**
     * 上传文件
     *
     * @param url       地址
     * @param paramsMap 参数
     * @param callBack  回调
     * @param <T>
     */
    public <T> void upLoadFile(String url, HashMap<String, Object> paramsMap, final ReqProgressCallBack<T> callBack) {
        try {
            //补全请求地址
            String requestUrl = url;
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //设置类型
            builder.setType(MultipartBody.FORM);
            //追加参数
            for (String key : paramsMap.keySet()) {
                Object object = paramsMap.get(key);
                if (!(object instanceof File)) {
                    builder.addFormDataPart(key, object.toString());
                } else {
                    File file = (File) object;
                    builder.addFormDataPart(key, file.getName(), createProgressRequestBody(MEDIA_OBJECT_STREAM, file, callBack));
                }
            }
            //创建RequestBody
            RequestBody body = builder.build();
            //创建Request
            final Request request = new Request.Builder().url(requestUrl).post(body).build();
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    L.e(TAG, e.toString());
                    failedCallBack("上传失败", callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        L.e(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        failedCallBack("上传失败", callBack);
                    }
                }
            });
        } catch (Exception e) {
            L.e(TAG, e.toString());
        }
    }

    public <T> void uploadDeviceHeadIcon(File file, String userId, String deviceId, final ReqProgressCallBack<T> callBack) {
        if (userId == null || deviceId == null) {
            L.e(TAG, "uploadDeviceHeadIcon userId: " + userId + ", deviceId: " + deviceId);
            return;
        }
        try {
            UploadDownloadFileParam uploadParam = new UploadDownloadFileParam(file, userId, deviceId /* 头像在服务器上是以设备Id为目录存储的 */);
            L.d(TAG, "uploadParam: " + uploadParam);
            Map<String, Object> paramsMap = uploadParam.toMap();
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue().toString(), "utf-8")));
                pos++;
            }
            String requestUrl = String.format("%s?%s", HEAD_ICON_UPLOAD_URL, tempParams.toString());

            L.v(TAG, "uploadDeviceHeadIcon URL: " + requestUrl);

            //创建RequestBody
            String fileName = file.getName();
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/" + fileName.substring(fileName.lastIndexOf(".") + 1)), file);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .build();

            //创建Request
            final Request request = new Request.Builder().url(requestUrl).post(requestBody).build();
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    L.e(TAG, "uploadDeviceHeadIcon() -> onFailure()", e);
                    failedCallBack("上传失败", callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        L.d(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        L.e(TAG, "response ----->" + response);
                        failedCallBack(response.toString(), callBack);
                    }
                }
            });
        } catch (Exception e) {
            L.e(TAG, "uploadDeviceHeadIcon() Exception", e);
            failedCallBack("上传失败", callBack);
        }
    }

    /**
     * @param filename 文件名
     * @param authId   鉴权ID
     * @param id       文件上传者ID
     */
    public static String makeFileDownloadUrl(@NonNull String filename, @NonNull String authId, @NonNull String id) {
        UploadDownloadFileParam deviceHeadIconParam = new UploadDownloadFileParam(filename, authId, id);
        Map<String, Object> paramsMap = deviceHeadIconParam.toMap();
        StringBuilder tempParams = new StringBuilder();
        int pos = 0;
        for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
            if (pos > 0) {
                tempParams.append("&");
            }
            try {
                tempParams.append(String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue().toString(), "utf-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            pos++;
        }
        return String.format("%s?%s", HEAD_ICON_DOWNLOAD_URL, tempParams.toString());
    }

    public <T> void downloadDeviceHeadIcon(final File file, String userId, String deviceId, final ReqProgressCallBack<T> callBack) {
        String requestUrl = makeFileDownloadUrl(file.getName(), userId, deviceId);

        L.v(TAG, "downloadDeviceHeadIcon URL: " + requestUrl);

        final Request request = new Request.Builder().url(requestUrl).build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e(TAG, e.toString());
                failedCallBack("下载失败", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[1024 * 8];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    if (response.header("Content-Type").contains("json")) {
                        String string = response.body().string();
                        L.e(TAG, string);
                        file.delete();
                        failedCallBack(string, callBack);
                        return;
                    }
                    long total = response.body().contentLength();
                    L.v(TAG, "total------>" + total);
                    long current = 0;
                    String path = file.getParent();
                    if (path != null) {
                        File pathFile = new File(path);
                        if (!pathFile.exists()) {
                            pathFile.mkdirs();
                        }
                    }
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        L.v(TAG, "current------>" + current);
                        progressCallBack(total, current, callBack);
                    }
                    fos.flush();
                    successCallBack((T) file, callBack);
                } catch (Exception e) {
                    file.delete();
                    L.e(TAG, "下载失败", e);
                    failedCallBack(e.getMessage(), callBack);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        L.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    public <T> void uploadChatVoiceFile(File file, String userId, final ReqProgressCallBack<T> callBack) {
        try {
            UploadDownloadFileParam uploadParam = new UploadDownloadFileParam(file, userId, userId);
            L.d(TAG, "uploadParam: " + uploadParam);
            Map<String, Object> paramsMap = uploadParam.toMap();
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue().toString(), "utf-8")));
                pos++;
            }
            String requestUrl = String.format("%s?%s", HEAD_ICON_UPLOAD_URL, tempParams.toString());

            L.v(TAG, "uploadChatVoiceFile URL: " + requestUrl);

            //创建RequestBody
            String fileName = file.getName();
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/" + fileName.substring(fileName.lastIndexOf(".") + 1)), file);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileBody)
                    .build();

            //创建Request
            final Request request = new Request.Builder().url(requestUrl).post(requestBody).build();
            final Call call = mOkHttpClient.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    L.e(TAG, "uploadChatVoiceFile() -> onFailure()", e);
                    failedCallBack("上传失败", callBack);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        L.d(TAG, "response ----->" + string);
                        successCallBack((T) string, callBack);
                    } else {
                        L.e(TAG, "response ----->" + response);
                        failedCallBack(response.toString(), callBack);
                    }
                }
            });
        } catch (Exception e) {
            L.e(TAG, "uploadChatVoiceFile() Exception", e);
            failedCallBack("上传失败", callBack);
        }
    }

    public <T> void downloadChatVoiceFile(final File file, String userId, String deviceId, final ReqProgressCallBack<T> callBack) {

        UploadDownloadFileParam deviceHeadIconParam = new UploadDownloadFileParam(file, userId, deviceId);
        Map<String, Object> paramsMap = deviceHeadIconParam.toMap();
        StringBuilder tempParams = new StringBuilder();
        int pos = 0;
        for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
            if (pos > 0) {
                tempParams.append("&");
            }
            try {
                tempParams.append(String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue().toString(), "utf-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            pos++;
        }
        String requestUrl = String.format("%s?%s", HEAD_ICON_DOWNLOAD_URL, tempParams.toString());

        L.v(TAG, "downloadChatVoiceFile URL: " + requestUrl);

        final Request request = new Request.Builder().url(requestUrl).build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e(TAG, e.toString());
                failedCallBack("下载失败", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[1024 * 8];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    if (response.header("Content-Type").contains("json")) {
                        String string = response.body().string();
                        L.e(TAG, string);
                        failedCallBack(string, callBack);
                    }
                    long total = response.body().contentLength();

                    long current = 0;
                    String path = file.getParent();
                    if (path != null) {
                        File pathFile = new File(path);
                        if (!pathFile.exists()) {
                            pathFile.mkdirs();
                        }
                    }
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);

                        progressCallBack(total, current, callBack);
                    }
                    fos.flush();
                    successCallBack((T) file, callBack);
                } catch (IOException e) {
                    file.delete();
                    L.e(TAG, e.toString());
                    failedCallBack("下载失败", callBack);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        L.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    /**
     * 创建带进度的RequestBody
     *
     * @param contentType MediaType
     * @param file        准备上传的文件
     * @param callBack    回调
     * @param <T>
     * @return
     */
    public <T> RequestBody createProgressRequestBody(final MediaType contentType, final File file, final ReqProgressCallBack<T> callBack) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source;
                try {
                    source = Okio.source(file);
                    Buffer buf = new Buffer();
                    long remaining = contentLength();
                    long current = 0;
                    for (long readCount; (readCount = source.read(buf, 2048)) != -1; ) {
                        sink.write(buf, readCount);
                        current += readCount;
                        L.e(TAG, "current------>" + current);
                        progressCallBack(remaining, current, callBack);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 下载文件
     *
     * @param fileUrl     文件url
     * @param destFileDir 存储目标目录
     */
    public <T> void downLoadFile(String fileUrl, final String destFileDir, final ReqCallBack<T> callBack) {
        final String fileName = Digest.md5(fileUrl);
        final File file = new File(destFileDir, fileName);
        if (file.exists()) {
            successCallBack((T) file, callBack);
            return;
        }
        final Request request = new Request.Builder().url(fileUrl).build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e(TAG, e.toString());
                failedCallBack("下载失败", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    long total = response.body().contentLength();
                    L.e(TAG, "total------>" + total);
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        L.e(TAG, "current------>" + current);
                    }
                    fos.flush();
                    successCallBack((T) file, callBack);
                } catch (IOException e) {
                    L.e(TAG, e.toString());
                    failedCallBack("下载失败", callBack);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        L.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    /**
     * 下载文件
     *
     * @param length
     * @param fileUrl 文件url
     * @param file    存储目标目录
     */
    public <T> void downLoadFile(final long length, String fileUrl, final File file, final ReqProgressCallBack<T> callBack) {

        if (file.exists()) {
            file.delete();
        }
        final Request request = new Request.Builder().url(fileUrl).build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                L.e(TAG, e.toString());
                failedCallBack("下载失败", callBack);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    L.e(TAG, "length------>" + length);
                    long current = 0;
                    is = response.body().byteStream();
                    fos = new FileOutputStream(file);
                    while ((len = is.read(buf)) != -1) {
                        current += len;
                        fos.write(buf, 0, len);
                        L.e(TAG, "current------>" + current);
                        progressCallBack(length, current, callBack);
                    }
                    fos.flush();
                    successCallBack((T) file, callBack);
                } catch (IOException e) {
                    L.e(TAG, e.toString());
                    failedCallBack("下载失败", callBack);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                        L.e(TAG, e.toString());
                    }
                }
            }
        });
    }

    public interface ReqProgressCallBack<T> extends ReqCallBack<T> {
        /**
         * 响应进度更新
         */
        void onProgress(long total, long current);
    }

    /**
     * 统一处理进度信息
     *
     * @param total    总计大小
     * @param current  当前进度
     * @param callBack
     * @param <T>
     */
    private <T> void progressCallBack(final long total, final long current, final ReqProgressCallBack<T> callBack) {
        okHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                if (callBack != null) {
                    callBack.onProgress(total, current);
                }
            }
        });
    }

    public static class UploadDownloadFileParam {
        final private String authId; // userId
        /**
         * 文件上传者Id
         */
        final private String id; // deviceId
        final private String filename;
        private String tostr;

        /**
         * @param filename 文件名
         * @param authId   鉴权ID
         * @param id       文件上传者ID
         */
        public UploadDownloadFileParam(String filename, String authId, String id) {
            this.authId = authId;
            this.id = id;
            this.filename = filename;
        }

        /**
         * @param file   文件
         * @param authId 鉴权ID
         * @param id     文件上传者ID
         */
        public UploadDownloadFileParam(File file, String authId, String id) {
            this(file.getName(), authId, id);
        }

        public HashMap<String, Object> toMap() {
            HashMap<String, Object> map = new HashMap<>();
            map.put("authId", authId);
            map.put("id", id);
            map.put("filename", filename);
            return map;
        }
    }
}