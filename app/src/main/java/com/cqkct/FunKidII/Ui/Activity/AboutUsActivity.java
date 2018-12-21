package com.cqkct.FunKidII.Ui.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.BuildConfig;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.UTIL;


/**
 * Created by Administrator on 2017/8/9.
 */

public class AboutUsActivity extends BaseActivity {

    public static final String URL = "URL_OFFICIAL_WEBSITE";
    private TextView tv_appVersion, tv_url;
    private ImageView rl__user_agreement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_us);
        setTitleBarTitle(R.string.about_us);

        tv_appVersion = findViewById(R.id.app_version);
        tv_appVersion.setText(getString(R.string.current_version, UTIL.getVersion(this)));
        rl__user_agreement = findViewById(R.id.ioc_about_us);
        rl__user_agreement.setOnLongClickListener(v -> {
            showTestVersion();
            return false;
        });
        tv_url = findViewById(R.id.url_web_view);

    }

    private void showTestVersion() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.app_setting_in_version, BuildConfig.versionNameInternal))
                .setCancelable(true)
                .create()
                .show();
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.user_agreement:
                startActivity(new Intent(this, UserInstructionActivity.class));
                break;
            case R.id.rl_about_us:
                Intent intent = new Intent(this,OfficialWebsiteActivity.class);
                intent.putExtra(URL,tv_url.getText().toString());
                startActivity(intent);
                break;
        }
    }
}
