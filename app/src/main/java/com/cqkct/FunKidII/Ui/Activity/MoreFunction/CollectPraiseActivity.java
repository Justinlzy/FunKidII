package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.view.WaveHelper;
import com.cqkct.FunKidII.Ui.view.WaveView;
import com.cqkct.FunKidII.Ui.widget.PraiseItemAnimation;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.CollectPraiseEntityDao;
import com.cqkct.FunKidII.db.Entity.CollectPraiseEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class CollectPraiseActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = CollectPraiseActivity.class.getSimpleName();

    private WaveHelper mWaveHelper;
    private static final int MAX_ENERGY = 6;

    static final String PARAM_PRAISE = "praise";
    private static final int ACTIVITY_CODE_EDIT_PRAISE = 1;
    private TextView editViewOnTitle;
    private ImageView addView;
    private TextView collectPraiseName, prizeTextTips;
    private LinearLayout scheduleLl;
    private WaveView waveView;

    private View[] praiseEnergyViews = new View[MAX_ENERGY];
    private TextView[] praiseEnergyTextViews = new TextView[MAX_ENERGY];
    private TextView durationTextView;
    private TextView percentTextView;
    private Message.Praise mPraise;
    private RelativeLayout noPermissionRelativeLayout;
    private ImageView itemIcon0, itemIcon1, itemIcon2, itemIcon3, itemIcon4, itemIcon5;
    private int mBorderColor = Color.parseColor("#44FFFFFF");
    private int mBorderWidth = 0;

    private boolean hasEditPermission = false;
    private TextView tip_text, noCollectPraiseTip;

    private LinearLayout llCollectPraiseFinishTip1;
    private ImageView ivCollectPraiseFinishTip2;
    private TextView tvCollectPraiseFinishTip3;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_praise);
        hasEditPermission = hasEditPermission();
        setTitleBarTitle(R.string.collect_praise);
        initView();
        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (requestCode == ACTIVITY_CODE_EDIT_PRAISE) {
            Serializable ret = data.getSerializableExtra(PARAM_PRAISE);
            if (ret == null) {
                setCurrentPraise(null);
            } else {
                setCurrentPraise((Message.Praise) ret);
            }
        }
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        super.onDebouncedClick(v);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.add_collect_praise: {
                startCollectPraiseEditActivity();
            }
            break;
            case R.id.title_bar_right_text: {
                if (editViewOnTitle.getText().equals(getString(R.string.collect_history))) {
                    startActivity(new Intent(this, CollectPraiseHistoryActivity.class));
                } else {
                    startCollectPraiseEditActivity();
                }
            }
            break;
        }
    }

    public void startCollectPraiseEditActivity() {
        Intent intent = new Intent(this, CollectPraiseEditActivity.class);
        intent.putExtra(PARAM_PRAISE, mPraise);
        startActivityForResult(intent, ACTIVITY_CODE_EDIT_PRAISE);
    }

    public void onClick(View v) {
        if (hasEditPermission) {
            int itemN = 0;
            int viewId = v.getId();
            switch (viewId) {
                case R.id.item_5_icon:
                    ++itemN;
                case R.id.item_4_icon:
                    ++itemN;
                case R.id.item_3_icon:
                    ++itemN;
                case R.id.item_2_icon:
                    ++itemN;
                case R.id.item_1_icon:
                    ++itemN;
                case R.id.item_0_icon:
                    doPraise(itemN, viewId);
                    break;
                default:
                    break;
            }
        }
    }

    private void initView() {
        waveView = findViewById(R.id.wave);
        waveView.setBorder(mBorderWidth, mBorderColor);
        mWaveHelper = new WaveHelper(waveView);
        mWaveHelper.setWaterLevelRatio(0);
        waveView.setShowBoat(false);

        tip_text = findViewById(R.id.tip_text);
        tip_text.setVisibility(View.GONE);
        noCollectPraiseTip = findViewById(R.id.no_collect_praise_tip_text);
        addView = findViewById(R.id.add_collect_praise);
        collectPraiseName = findViewById(R.id.collect_praise_name);
        prizeTextTips = findViewById(R.id.prize_text_tips);
        scheduleLl = findViewById(R.id.rl_schedule);
        scheduleLl.setVisibility(View.GONE);
        llCollectPraiseFinishTip1 = findViewById(R.id.collection_complete_tips1);
        llCollectPraiseFinishTip1.setVisibility(View.GONE);

        ivCollectPraiseFinishTip2 = findViewById(R.id.collection_complete_tips2);
        ivCollectPraiseFinishTip2.setVisibility(View.GONE);

        ivCollectPraiseFinishTip2.setBackground(getCurrentLanguage().toLowerCase().contains("zh") ?
                getResources().getDrawable(R.drawable.collect_praise_success_zh) :
                getResources().getDrawable(R.drawable.collect_praise_success_en));

        tvCollectPraiseFinishTip3 = findViewById(R.id.collection_complete_tips3);
        tvCollectPraiseFinishTip3.setVisibility(View.GONE);

        editViewOnTitle = findViewById(R.id.title_bar_right_text);
        editViewOnTitle.setVisibility(View.VISIBLE);
        editViewOnTitle.setText(R.string.collect_history);

        View titleBarLayout = findViewById(R.id.title_bar);
        titleBarLayout.setBackgroundColor(0xffffff);

        praiseEnergyViews[0] = findViewById(R.id.item_0);
        praiseEnergyTextViews[0] = findViewById(R.id.item_0_text);
        praiseEnergyViews[1] = findViewById(R.id.item_1);
        praiseEnergyTextViews[1] = findViewById(R.id.item_1_text);
        praiseEnergyViews[2] = findViewById(R.id.item_2);
        praiseEnergyTextViews[2] = findViewById(R.id.item_2_text);
        praiseEnergyViews[3] = findViewById(R.id.item_3);
        praiseEnergyTextViews[3] = findViewById(R.id.item_3_text);
        praiseEnergyViews[4] = findViewById(R.id.item_4);
        praiseEnergyTextViews[4] = findViewById(R.id.item_4_text);
        praiseEnergyViews[5] = findViewById(R.id.item_5);
        praiseEnergyTextViews[5] = findViewById(R.id.item_5_text);

        durationTextView = findViewById(R.id.duration);
        percentTextView = findViewById(R.id.schedule);

        noPermissionRelativeLayout = findViewById(R.id.rl_no_permission);

        if (hasEditPermission) {
            addView.setVisibility(View.VISIBLE);
            //无权限没有点击效果
            itemIcon0 = findViewById(R.id.item_0_icon);
            itemIcon1 = findViewById(R.id.item_1_icon);
            itemIcon2 = findViewById(R.id.item_2_icon);
            itemIcon3 = findViewById(R.id.item_3_icon);
            itemIcon4 = findViewById(R.id.item_4_icon);
            itemIcon5 = findViewById(R.id.item_5_icon);
            itemIcon0.setOnClickListener(this);
            itemIcon1.setOnClickListener(this);
            itemIcon2.setOnClickListener(this);
            itemIcon3.setOnClickListener(this);
            itemIcon4.setOnClickListener(this);
            itemIcon5.setOnClickListener(this);
        } else {
            addView.setVisibility(View.GONE);
        }

    }

    private void updateView() {
        Message.Praise praise = mPraise;
        if (praise == null) {
            editViewOnTitle.setText(R.string.collect_history);
            waveView.setShowBoat(false);
            tip_text.setVisibility(View.GONE);
            noCollectPraiseTip.setVisibility(View.VISIBLE);
            setTitleBarTitle(R.string.collect_praise);
            scheduleLl.setVisibility(View.GONE);
            collectPraiseName.setVisibility(View.GONE);
            if (hasEditPermission) {
                addView.setVisibility(View.VISIBLE);
                prizeTextTips.setVisibility(View.VISIBLE);
            } else {
                addView.setVisibility(View.GONE);
                prizeTextTips.setVisibility(View.GONE);
            }
            praise = Message.Praise.newBuilder().build();
        } else {
            editViewOnTitle.setText(R.string.edit);
            waveView.setShowBoat(true);
            tip_text.setVisibility(View.VISIBLE);
            noCollectPraiseTip.setVisibility(View.GONE);
            scheduleLl.setVisibility(View.VISIBLE);
            setTitleBarTitle(R.string.collect_praise);
            addView.setVisibility(View.GONE);
            prizeTextTips.setVisibility(View.GONE);
            collectPraiseName.setVisibility(View.VISIBLE);
            collectPraiseName.setText(praise.getPrize());
            if (hasEditPermission) {
                noPermissionRelativeLayout.setVisibility(View.GONE);
                editViewOnTitle.setVisibility(View.VISIBLE);
            } else {
                editViewOnTitle.setVisibility(View.GONE);
                noPermissionRelativeLayout.setVisibility(View.VISIBLE);
            }
        }


        int i = 0;
        for (Message.Praise.Item item : praise.getItemList()) {
            praiseEnergyViews[i].setVisibility(item.getPraised() ? View.GONE : View.VISIBLE);
            praiseEnergyTextViews[i].setText(item.getName());
            ++i;
        }
        while (i < MAX_ENERGY) {
            praiseEnergyViews[i].setVisibility(View.GONE);
            ++i;
        }

        if (praise.getCompleteTime() != 0) {
            // 集赞完成
            llCollectPraiseFinishTip1.setVisibility(View.VISIBLE);
            ivCollectPraiseFinishTip2.setVisibility(View.VISIBLE);
            tvCollectPraiseFinishTip3.setVisibility(View.VISIBLE);
            noPermissionRelativeLayout.setVisibility(View.GONE);
            tip_text.setVisibility(View.GONE);
            //隐藏集赞任务
            setItemIconVisibility(View.GONE);

        } else {
            llCollectPraiseFinishTip1.setVisibility(View.GONE);
            ivCollectPraiseFinishTip2.setVisibility(View.GONE);
            tvCollectPraiseFinishTip3.setVisibility(View.GONE);
            setItemIconVisibility(View.VISIBLE);
        }
        long days = 0;
        if (praise.getStartTime() != 0) {
            Calendar begin = Calendar.getInstance(TimeZone.getTimeZone(praise.getTimezone().getZone()));
            begin.setTimeInMillis(praise.getStartTime() * 1000L);
            Calendar end = Calendar.getInstance(TimeZone.getTimeZone(praise.getTimezone().getZone()));
            if (praise.getCompleteTime() != 0) {
                end.setTimeInMillis(praise.getCompleteTime() * 1000L);
            }
            days = (end.getTimeInMillis() - begin.getTimeInMillis()) / 1000 / 60 / 60 / 24;
            if (days == 0) {
//                days = end.get(Calendar.DAY_OF_YEAR) - begin.get(Calendar.DAY_OF_YEAR);
                days = 1;//不足一天算一天
            }
        }
        durationTextView.setText(String.valueOf(days));
        float percent = 0;
        if (praise.getTotalGoal() != 0) {
            percent = (float) praise.getTotalReached() / (float) praise.getTotalGoal();
        }
        percentTextView.setText(String.format("%d/%d", praise.getTotalReached(), praise.getTotalGoal()));
        mWaveHelper.setWaterLevelRatio(percent);
    }

    private void setItemIconVisibility(int visibility) {
        ImageView[] imageViews = new ImageView[]{itemIcon0, itemIcon1, itemIcon2, itemIcon3, itemIcon4, itemIcon5};
        for (ImageView imageView : imageViews) {
            if (imageView.getVisibility() != visibility)
                imageView.setVisibility(visibility);
        }
    }

    private void setCurrentPraise(Message.Praise praise) {
        if (praise == mPraise || (praise != null && praise.equals(mPraise))) {
            return;
        }
        mPraise = praise;
        updateView();
    }

    private void loadData() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "queryServer deviceId is empty");
            return;
        }
        List<CollectPraiseEntity> list = App.getInstance().getDaoSession().getCollectPraiseEntityDao()
                .queryBuilder()
                .where(CollectPraiseEntityDao.Properties.DeviceId.eq(deviceId))
                .where(CollectPraiseEntityDao.Properties.IsCancel.eq(false))
                .where(CollectPraiseEntityDao.Properties.FinishTime.eq(0))
                .build()
                .list();

        if (!list.isEmpty()) {
            setCurrentPraise(list.get(0).getPraise());
        }

        queryServer();
    }

    private void queryServer() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "queryServer deviceId is empty");
            return;
        }

        popWaitingDialog(R.string.please_wait);

        exec(
                Message.GetPraiseReqMsg.newBuilder().setDeviceId(deviceId).setPageSize(20).setPageIdx(0).build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetPraiseRspMsg rspMsg = response.getProtoBufMsg();
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                List<Message.Praise> praises = rspMsg.getPraiseList();
                                GreenUtils.savePraise(deviceId, praises);
                                if (praises.isEmpty()) {
                                    setCurrentPraise(null);
                                } else {
                                    Message.Praise praise = praises.get(0);
                                    if (praise.getFinishTime() == 0) {
                                        setCurrentPraise(praise);
                                    } else {
                                        setCurrentPraise(null);
                                    }
                                }
                                dismissDialog();
                                return false;
                            }
                            L.w(TAG, "queryServer() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "queryServer() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.load_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "queryServer() -> exec() -> onException()", cause);
                        popErrorDialog(cause instanceof TimeoutException ? R.string.load_timeout : R.string.load_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }

    private void doPraise(int n, @IdRes int energyView) {
        Message.Praise praise = mPraise;
        if (praise == null) {
            L.e(TAG, "doPraise: praise is null");
            return;
        }
        final ImageView imageView = findViewById(energyView);
        imageView.setClickable(false);
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "doPraise: deviceId is empty");
            return;
        }
        exec(Message.DoPraiseReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .setPraise(
                                Message.Praise.newBuilder()
                                        .setId(praise.getId())
                                        .addItem(
                                                Message.Praise.Item.newBuilder()
                                                        .setId(praise.getItem(n).getId())
                                        )
                        )
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.DoPraiseRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                case DATA_CONFLICT:
                                    startAnimation(praiseEnergyViews[n], rspMsg);
                                    GreenUtils.savePraise(deviceId, rspMsg.getPraise());
                                    imageView.setClickable(true);
                                    return false;
                            }
                            L.w(TAG, "doPraise() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "doPraise() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        imageView.setClickable(true);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "doPraise() -> exec() -> onException()", cause);
                        popErrorDialog(R.string.submit_failure);
                        imageView.setClickable(true);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }

    private void startAnimation(View view, Message.DoPraiseRspMsg rspMsg) {
        PraiseItemAnimation  animator1 = new PraiseItemAnimation();
        Animation scaleAnim = new ScaleAnimation(1.0f,0.4f,1.0f,0.4f,Animation.RELATIVE_TO_SELF,1.0f,Animation.RELATIVE_TO_SELF,1.0f);
        scaleAnim.setDuration(1000);
        scaleAnim.setFillAfter(false);// 动画结束后保留状态

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(animator1);
        animationSet.addAnimation(scaleAnim);
        view.startAnimation(animationSet);
        animator1.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {
            }
            @Override public void onAnimationEnd(Animation animation) {
                setCurrentPraise(rspMsg.getPraise());
                view.clearAnimation();
            }
            @Override public void onAnimationRepeat(Animation animation) {
            }
        });

    }



    @Override
    public void onPraiseChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyPraiseChangedReqMsg reqMsg) {
        Message.Praise praise = reqMsg.getDetail().getPraise();
        if (praise == null) {
            return;
        }

        if (mPraise != null && !praise.getId().equals(mPraise.getId())) {
            // 不是当前集赞
            return;
        }

        // 当前集赞为空，我们认为通知里的集赞就是当前集赞


        if (praise.getFinishTime() != 0) {
            // 已经结束了
            setCurrentPraise(null);
            return;
        }

        switch (reqMsg.getDetail().getAction()) {
            case ADD:
                setCurrentPraise(praise);
                break;
            case DEL:
            case FINISH:
            case CANCELED:
                setCurrentPraise(null);
                break;
            case MODIFY:
            case COMPLETE:
            case PRAISE:
                setCurrentPraise(praise);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWaveHelper.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWaveHelper.start();
        loadData();
    }
}