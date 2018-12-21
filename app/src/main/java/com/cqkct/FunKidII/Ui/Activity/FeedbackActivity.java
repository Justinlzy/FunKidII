package com.cqkct.FunKidII.Ui.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.cqkct.FunKidII.Bean.UmengEvent;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.Rom;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by Administrator on 2017/8/9.
 */

public class FeedbackActivity extends BaseActivity {

    private WebView mWebView;
    private ProgressBar progressBar;
    private static final int PERMISSIONS_REQUEST_CALL_PHONE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_and_problems);
        setTitleBarTitle(R.string.problems_and_feedback);
        init();
    }

    private void init() {
        assert mCurrentBabyBean != null;
        String devId = mCurrentBabyBean.getDeviceId();
        String nickName = TextUtils.isEmpty(mCurrentBabyBean.getName()) ? getString(R.string.baby) : mCurrentBabyBean.getName();
        if (TextUtils.isEmpty(devId)) {
            finish();
            return;
        }
        StringBuilder url = new StringBuilder();
        url.append(Constants.FEEDBACK_URL).append(devId).append("&").append("nickname").append("=").append(nickName);
        L.e("FeedbackActivity", url.toString());


        mWebView = findViewById(R.id.feed_back_web);
        progressBar = findViewById(R.id.loading_pb);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        //android 版本 5.0之后加载https空白页：由于android5.0版本之前默认允许加载混合网络协议内容；
        //5.0之后默认不允许，设置webView允许加载混合网络协议即可
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webSettings.setAllowFileAccess(true);
        //设置支持缩放
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDefaultTextEncodingName("gb2312");
        webSettings.setSupportZoom(true);
        //设置Web视图
        mWebView.loadUrl(url.toString());
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {// 打电话
                    if (Rom.isFlyme()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        MobclickAgent.onEvent(FeedbackActivity.this, UmengEvent.TIMES_OF_CALL_TO_DEVICE);
                    } else if (ContextCompat.checkSelfPermission(FeedbackActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(FeedbackActivity.this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_PHONE);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                } else {
                    view.loadUrl(url);
                }
                return true;

            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //注意：super句话一定要删除，或者注释掉，否则又走handler.cancel()默认的不支持https的了。
                //super.onReceivedSslError(view, handler, error);
                //handler.cancel(); // Android默认的处理方式
                //handler.handleMessage(Message msg); // 进行其他处理
                handler.proceed(); // 接受所有网站的证书
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // TODO 自动生成的方法存根
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);//加载完网页进度条消失
                } else {
                    progressBar.setVisibility(View.VISIBLE);//开始加载网页时显示进度条
                    progressBar.setProgress(newProgress);//设置进度值
                }

            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                //拦截Alert 替换为dialog
                ConfirmDialogFragment confirmDialogFragment = new ConfirmDialogFragment();
                confirmDialogFragment
                        .setMessage(message)
                        .setTitle(getString(R.string.collect_hint))
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setBlur(false)
                        .setCancelable(false);//屏蔽返回键
                confirmDialogFragment.show(getSupportFragmentManager(), "Feedback");
                return true;
            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //这是一个监听用的按键的方法，keyCode 监听用户的动作，如果是按了返回键，同时Webview要返回的话，WebView执行回退操作，因为mWebView.canGoBack()返回的是一个Boolean类型，所以我们把它返回为true
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
