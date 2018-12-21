package com.cqkct.FunKidII.Ui.Activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.service.tlc.PreferencesWrapper;

import org.greenrobot.eventbus.EventBus;

public class VersionSwitchActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.version_switch);
        setTitleBarTitle(R.string.select_areas);
        initView();
    }

    private void initView() {
        switch (getPreferencesWrapper().getAppArea()) {
            case PreferencesWrapper.APP_AREA_CHINA:
                findViewById(R.id.china).setEnabled(false);
                break;
            case PreferencesWrapper.APP_AREA_OVER_SEA:
                findViewById(R.id.other).setEnabled(false);
                break;
        }
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        int area = getPreferencesWrapper().getAppArea();
        switch (viewId) {
            case R.id.china:
                if (area != PreferencesWrapper.APP_AREA_CHINA) {
                    getPreferencesWrapper().setAppArea(PreferencesWrapper.APP_AREA_CHINA);
                    EventBus.getDefault().postSticky(new Event.AppAreaSwitched(PreferencesWrapper.APP_AREA_CHINA));
                }
                finish();
                break;
            case R.id.other:
                if (area != PreferencesWrapper.APP_AREA_OVER_SEA) {
                    getPreferencesWrapper().setAppArea(PreferencesWrapper.APP_AREA_OVER_SEA);
                    EventBus.getDefault().postSticky(new Event.AppAreaSwitched(PreferencesWrapper.APP_AREA_OVER_SEA));
                }
                finish();
                break;
        }
    }
}
