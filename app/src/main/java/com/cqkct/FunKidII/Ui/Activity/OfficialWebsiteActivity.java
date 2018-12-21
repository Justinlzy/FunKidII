package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.L;

/**
 * Created by Administrator on 2017/12/29.
 */

public class OfficialWebsiteActivity extends BaseActivity{
    public static final String TAG = OfficialWebsiteActivity.class.getSimpleName();
    private WebView webView;
    private String Url = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.official_website);
        setTitleBarTitle(R.string.kct);
        Intent intent = getIntent();
        Url = intent.getStringExtra(AboutUsActivity.URL);
        init();
    }

    private void init() {
        webView = (WebView) findViewById(R.id.web_view);
        WebSettings settings = webView.getSettings();
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            //  重写此方法表明点击网页里面的链接还是在当前的webview里跳转，不跳到浏览器那边
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }
        });
        webView.loadUrl(Url);
        L.v(TAG,"Url: "+Url);
    }

//    @Override
//    public void onBackPressed() {
//        if (webView.canGoBack()) {
//            webView.goBack(); // 返回上一页面
//        } else {
//            super.onBackPressed();
//        }
//    }
}
