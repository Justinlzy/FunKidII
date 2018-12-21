package com.cqkct.FunKidII.Ui.Activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.tlc.TlcService;


public class UserInstructionActivity extends BaseActivity {

    public static final String TAG = UserInstructionActivity.class.getName();
    private WebView mWebView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerdealdetail);
        setTitleBarTitle(R.string.user_agreement);
        mWebView = findViewById(R.id.webwiew_useragreement);
        mWebView.getSettings().setJavaScriptEnabled(true);
        if (getCurrentLanguageUseResources().toLowerCase().contains("zh")) {
            mWebView.loadUrl("file:///android_asset/about_ZH.html");
        } else {
            mWebView.loadUrl("file:///android_asset/about_EN.html");
        }
        findViewById(R.id.title_bar_left_icon).setOnClickListener(v -> finish());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky) {
    }

    @Override
    public void onCurrentBabyChanged(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
    }
}
