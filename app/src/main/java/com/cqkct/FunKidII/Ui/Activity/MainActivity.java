package com.cqkct.FunKidII.Ui.Activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.fragment.CallDialogFragment;
import com.cqkct.FunKidII.Ui.fragment.ConversationListFragment;
import com.cqkct.FunKidII.Ui.fragment.MapFragment;
import com.cqkct.FunKidII.Ui.fragment.MoreFragment;
import com.cqkct.FunKidII.Ui.view.CustomViewPager;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.gyf.barlibrary.ImmersionBar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;


public class MainActivity extends BaseMapActivity implements View.OnClickListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private CustomViewPager viewPager;

    private ImageView ivMap, ivChat, ivCall, ivMore;

    public static final String USER_ID = "user_Id";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化布局
        InitView();

        //初始化ViewPager
        InitViewPager();
    }

    @Override
    protected void initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.keyboardEnable(true)  //解决软键盘与底部输入框冲突问题
                .statusBarColor(R.color.main_background_color)
                .statusBarDarkFont(true)
                .init();
    }

    private long doubleClickQuitTime;

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - doubleClickQuitTime > 2000)) {
            toast(R.string.main_click_again_exit);
            doubleClickQuitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        doubleClickQuitTime = 0;
        switch (v.getId()) {
            case R.id.location:
                viewPager.setCurrentItem(0);
                break;
            case R.id.micro_chat:
                viewPager.setCurrentItem(1);
                break;
            case R.id.more_function:
                viewPager.setCurrentItem(2);
                break;
            case R.id.make_call:
                showFragment();
                break;

        }
    }

    private void showFragment() {
        CallDialogFragment callDialogFragment = new CallDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(USER_ID, mUserId);
        callDialogFragment.setArguments(bundle);
        callDialogFragment.show(getSupportFragmentManager(), "CallActivity");
        callDialogFragment.setCallDialogFragmentListener(entity -> callDialog(entity, callDialogFragment));
    }

    private void callDialog(BabyEntity entity, CallDialogFragment callDialogFragment) {
        if (TextUtils.isEmpty(entity.getDeviceId())) {
            return;
        }
        if (TextUtils.isEmpty(entity.getPhone())) {
            return;
        }

        // 第一步:上传配置信息 第二步:保存信息至本地
        Message.Baby baby = Message.Baby.newBuilder()
                .setPhone(entity.getPhone())
                .build();

        Message.DevConf devConf = Message.DevConf.newBuilder()
                .setBaby(baby)
                .build();

        final Message.PushDevConfReqMsg pushConfigReqMsg = Message.PushDevConfReqMsg.newBuilder()
                .setDeviceId(entity.getDeviceId())
                .setFlag(Message.DevConfFlag.DCF_BABY_VALUE)
                .setConf(devConf)
                .build();

        popWaitingDialog(R.string.please_wait);

        exec(
                pushConfigReqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.PushDevConfRspMsg rspMsg = response.getProtoBufMsg();
                            L.d(TAG, "pushBabyInfo() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                GreenUtils.saveConfigs(pushConfigReqMsg.getConf(), pushConfigReqMsg.getFlag(), pushConfigReqMsg.getDeviceId());
                                dismissDialog();
                                callDialogFragment.call(entity);
                            } else {
                                popErrorDialog(R.string.fail_to_save);
                            }
                        } catch (Exception e) {
                            L.e(TAG, "pushBabyInfo() -> exec() -> onResponse() process failure", e);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "pushBabyInfo() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.time_out_to_save);
                        } else {
                            popErrorDialog(R.string.fail_to_save);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }

    private void InitView() {
        ivMap = findViewById(R.id.iv_main_location);
        ivMap.setImageResource(R.drawable.location_choose);
        ivChat = findViewById(R.id.iv_main_chat);
        ivCall = findViewById(R.id.iv_main_call);
        ivMore = findViewById(R.id.iv_main_more);
    }

    private void InitViewPager() {
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(new PagerAdapter(getSupportFragmentManager(), new ArrayList<Fragment>() {{
            add(new MapFragment());
            add(new ConversationListFragment());
            add(new MoreFragment());
        }}));
        viewPager.setOffscreenPageLimit(3);
        viewPager.setPagingEnabled(false);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            //当ViewPager显示的Fragment发生改变时激发该方法
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        ivMap.setImageResource(R.drawable.location_choose);
                        ivChat.setImageResource(R.drawable.chat);
                        ivMore.setImageResource(R.drawable.more);
                        break;

                    case 1:
                        ivMap.setImageResource(R.drawable.location);
                        ivChat.setImageResource(R.drawable.chat_choose);
                        ivMore.setImageResource(R.drawable.more);
                        break;

//                    case 2:
//                        ivMap.setImageResource(R.drawable.location);
//                        ivChat.setImageResource(R.drawable.chat);
//                        ivMore.setImageResource(R.drawable.more);
//                        break;

                    case 2:
                        ivMap.setImageResource(R.drawable.location);
                        ivChat.setImageResource(R.drawable.chat);
                        ivMore.setImageResource(R.drawable.more_choose);
                        break;
                }
            }
        });
    }

    class PagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> mFragmentList;

        public PagerAdapter(FragmentManager fm, List<Fragment> fl) {
            super(fm);
            mFragmentList = fl;
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    @Override
    public boolean finishWhenCurrentBabySwitched(@android.support.annotation.Nullable BabyEntity oldBabyBean, @android.support.annotation.Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        boolean reallyNoMore = false;
        if (isSticky) {
            String userId = App.getInstance().getUserId();
            if (TextUtils.isEmpty(userId)) {
                L.w(TAG, "in finishWhenNoMoreBaby(isSticky) preferencesWrapper.getUserId() is empty");
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            }
            List<BabyEntity> babies = GreenUtils.getBabyEntityDao().queryBuilder().where(BabyEntityDao.Properties.UserId.eq(userId)).list();
            if (babies.isEmpty()) {
                L.w(TAG, "in finishWhenNoMoreBaby(isSticky) No Baby");
                reallyNoMore = true;
            }
        } else {
            L.i(TAG, "in finishWhenNoMoreBaby(notSticky)");
            reallyNoMore = true;
        }
        if (reallyNoMore) {
            startActivity(new Intent(this, BindDeviceActivity.class));
        }
        return reallyNoMore;
    }

    public void onAppAreaSwitched(Event.AppAreaSwitched ev) {
        super.onAppAreaSwitched(ev);
        if (isFinishing()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
}
