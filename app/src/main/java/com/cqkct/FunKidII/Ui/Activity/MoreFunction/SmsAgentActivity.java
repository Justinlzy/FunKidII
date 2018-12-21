package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.SmsAgentAdapter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.SmsEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.SmsEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import protocol.Message;

public class SmsAgentActivity extends BaseActivity {

    public static final String TAG = SmsAgentActivity.class.getSimpleName();
    public static final String ON_DETAIL_DATA_FLAG = "ON_DETAIL_DATA_FLAG";
    public static final int ON_DETAIL_FLAG = 1;


    private LinearLayout mEditStatusTop;
    private SwitchCompat mSwitchTop;

    private RelativeLayout mEditStatusBottom;
    private SwitchCompat mSwitchBottom;

    private RelativeLayout mEmptyView;
    private TextView mEmptyText;

    private RecyclerView recyclerView;

    private TextView edit, cancel;
    private ImageView back;


    private SmsAgentAdapter adapter;
    private Map<Long, SmsEntity> mSelectedMap = new ConcurrentHashMap<>();
    private List<SmsEntity> mList = new ArrayList<>();
    boolean hasEditPermission;
    private boolean mSmsAgentEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_agent);
        setTitleBarTitle(R.string.sms_agent);
        initView();
        initData();
    }

    private void initView() {
        hasEditPermission = hasEditPermission();

        edit = findViewById(R.id.title_bar_right_text);
        edit.setText(R.string.edit);
        cancel = findViewById(R.id.title_bar_left_text);
        cancel.setText(R.string.cancel);
        back = findViewById(R.id.title_bar_left_icon);

        mEmptyView = findViewById(R.id.empty_view);
        mEmptyText = findViewById(R.id.empty_text);

        mEditStatusTop = findViewById(R.id.edit_sms_top);
        mSwitchTop = findViewById(R.id.switch_sms_agent_top);

        mEditStatusBottom = findViewById(R.id.edit_sms_bottom);
        mSwitchBottom = findViewById(R.id.switch_sms_agent_bottom);
        findViewById(R.id.delete).setOnClickListener(v -> {
            for (SmsEntity entity : mList) {
                SmsEntity selectEntity = mSelectedMap.get(entity.getId());
                if (selectEntity != null) {
                    SmsEntityDao dao = GreenUtils.getSmsEntityDao();
                    List<SmsEntity> entities = GreenUtils.getSmsEntityDao().queryBuilder()
                            .where(SmsEntityDao.Properties.DeviceId.eq(selectEntity.getDeviceId()))
                            .where(SmsEntityDao.Properties.UserId.eq(selectEntity.getUserId()))
                            .where(SmsEntityDao.Properties.Number.eq(selectEntity.getNumber()))
                            .list();
                    dao.deleteInTx(entities);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        mSwitchTop.setClickable(hasEditPermission);
        mSwitchBottom.setClickable(hasEditPermission);

        mSwitchTop.setOnCheckedChangeListener(smsAgentTopSwitchListener);
        mSwitchBottom.setOnCheckedChangeListener(smsAgentBottomSwitchListener);

        recyclerView = findViewById(R.id.recycler_sms);
        adapter = new SmsAgentAdapter(this, mList, mSelectedMap);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setItemClickListener(new SmsAgentAdapter.onItemClickListener() {
            @Override
            public void onNumberClick(int position, SmsEntity entity) {
                if (mSelectedMap.get(entity.getId()) == null) {
                    mSelectedMap.put(entity.getId(), entity);
                } else {
                    mSelectedMap.remove(entity.getId());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onItemClick(int position, SmsEntity entity) {
                Intent intent = new Intent(SmsAgentActivity.this, SmsDetailActivity.class);
                intent.putExtra(ON_DETAIL_DATA_FLAG, entity);
                startActivity(intent);
            }
        });
    }

    CompoundButton.OnCheckedChangeListener smsAgentTopSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (!mSmsAgentEnabled) {
                mSwitchTop.setOnCheckedChangeListener(null);
                mSwitchTop.setChecked(!b);
                mSwitchTop.setEnabled(false);
                if (!modifySmsAgentSwitch(!mSmsAgentEnabled)) {
                    mSwitchTop.setOnCheckedChangeListener(smsAgentTopSwitchListener);
                    mSwitchTop.setEnabled(true);
                }
            }
        }
    };
    CompoundButton.OnCheckedChangeListener smsAgentBottomSwitchListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (mSmsAgentEnabled) {
                mSwitchBottom.setOnCheckedChangeListener(null);
                mSwitchBottom.setChecked(!b);
                mSwitchBottom.setEnabled(false);
                if (!modifySmsAgentSwitch(!mSmsAgentEnabled)) {
                    mSwitchBottom.setOnCheckedChangeListener(smsAgentBottomSwitchListener);
                    mSwitchBottom.setEnabled(true);
                }
            }
        }
    };


    public void initData() {
        getDataFromDB();
        getSmsAgentSwitchFromService();
//        getSmsDataFromService(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDataFromDB();
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        switch (v.getId()) {
            case R.id.title_bar_right_text:
                if (edit.getText().toString().equals(getString(R.string.edit))) {
                    edit.setText(R.string.select_all);
                    back.setVisibility(View.GONE);
                    cancel.setVisibility(View.VISIBLE);
                    mEditStatusBottom.setVisibility(View.VISIBLE);
                    adapter.notifyCheckable(true);
                } else if (edit.getText().toString().equals(getString(R.string.select_all))) {
                    for (SmsEntity entity : mList) {
                        if (mSelectedMap.get(entity.getId()) == null) {
                            mSelectedMap.put(entity.getId(), entity);
                        }
                    }
                    if (mSelectedMap.isEmpty())
                        adapter.notifyDataSetChanged();
                }
                break;
            case R.id.title_bar_left_text:
                if (!TextUtils.isEmpty(cancel.getText().toString())) {
                    mSelectedMap.clear();
                    edit.setText(R.string.edit);
                    cancel.setVisibility(View.GONE);
                    back.setVisibility(View.VISIBLE);
                    mEditStatusBottom.setVisibility(View.GONE);
                    adapter.notifyCheckable(false);
                }
                break;
        }
    }

    private void getDataFromDB() {
        //get sms status switch;
        List<BabyEntity> babyEntities = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.DeviceId.eq(mDeviceId))
                .where(BabyEntityDao.Properties.UserId.eq(mUserId))
                .list();
        if (!babyEntities.isEmpty()) {
            BabyEntity babyEntity = babyEntities.get(0);
            mSmsAgentEnabled = babyEntity.getSmsAgentEnabled();
            if (mSmsAgentEnabled) {
                mEditStatusTop.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
            } else {
                mEditStatusTop.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            }
            mSwitchTop.setOnCheckedChangeListener(null);
            mSwitchTop.setChecked(mSmsAgentEnabled);
            mSwitchTop.setOnCheckedChangeListener(smsAgentTopSwitchListener);

            mSwitchBottom.setOnCheckedChangeListener(null);
            mSwitchBottom.setChecked(mSmsAgentEnabled);
            mSwitchBottom.setOnCheckedChangeListener(smsAgentBottomSwitchListener);
        }

        mList.clear();
        mList.addAll(filterData(GreenUtils.getSmsEntityDao().queryBuilder()
                .where(SmsEntityDao.Properties.DeviceId.eq(mDeviceId), SmsEntityDao.Properties.UserId.eq(mUserId))
                .orderDesc(SmsEntityDao.Properties.Time)
                .list()));
        if (mList.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }
        edit.setVisibility(mSmsAgentEnabled ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }


    public List<SmsEntity> filterData(List<SmsEntity> entities) {
        String lastNumber = "";
        for (Iterator<SmsEntity> it = entities.iterator(); it.hasNext(); ) {
            SmsEntity entity = it.next();
            if (entity.getNumber().equals(lastNumber)) {
                it.remove();
            } else {
                lastNumber = entity.getNumber();
            }
        }
        return entities;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private boolean modifySmsAgentSwitch(boolean isOpen) {
        String deviceId = mDeviceId;
        String userId = mUserId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "modifySmsAgentSwitch ERROR: deviceId is empty");
            return false;
        }
        if (TextUtils.isEmpty(userId)) {
            L.e(TAG, "modifySmsAgentSwitch ERROR: userId is empty");
            return false;
        }
        popWaitingDialog(R.string.please_wait);
        exec(Message.SMSAgentSwitchReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .setEnable(isOpen)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        Message.SMSAgentSwitchRspMsg rspMsg = response.getProtoBufMsg();
                        L.e(TAG, "modifySmsAgentSwitch ErrCode: " + rspMsg.getErrCode());

                        switch (rspMsg.getErrCode()) {
                            case SUCCESS:
                                dismissDialog();
                                if (isOpen) {
                                    mSwitchTop.setChecked(true);
                                    mSwitchBottom.setChecked(true);
                                    mSmsAgentEnabled = true;
                                    mSwitchTop.setOnCheckedChangeListener(smsAgentTopSwitchListener);
                                    mSwitchTop.setEnabled(true);
                                } else {
                                    mSwitchTop.setChecked(false);
                                    mSwitchBottom.setChecked(false);
                                    mSmsAgentEnabled = false;
                                    mSwitchBottom.setOnCheckedChangeListener(smsAgentBottomSwitchListener);
                                    mSwitchBottom.setEnabled(true);
                                    mEditStatusBottom.setVisibility(View.GONE);
                                }
                                setView();
                                GreenUtils.upDataSmsAgentStatus(isOpen, deviceId, userId);
                                return false;
                            default:
                                break;

                        }
                        popErrorDialog(R.string.setup_failed);
                        if (isOpen) {
                            mSwitchTop.setOnCheckedChangeListener(smsAgentTopSwitchListener);
                            mSwitchTop.setEnabled(true);
                        } else {
                            mSwitchBottom.setOnCheckedChangeListener(smsAgentBottomSwitchListener);
                            mSwitchBottom.setEnabled(true);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        popErrorDialog(cause instanceof TimeoutException ? R.string.setup_timeout : R.string.setting_failure);

                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );

        return true;
    }

    private void getSmsAgentSwitchFromService() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "getSmsAgentSwitchFromService ERROR: deviceId is empty");
            return;
        }
        String userId = mUserId;
        if (TextUtils.isEmpty(userId)) {
            L.e(TAG, "getSmsAgentSwitchFromService ERROR: userId is empty");
            return;
        }
        popWaitingDialog(R.string.loading);
        exec(Message.SMSAgentGetSwitchStatusReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        Message.SMSAgentGetSwitchStatusRspMsg rspMsg = response.getProtoBufMsg();

                        if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                            L.e(TAG, "getSmsAgentSwitchFromService: " + rspMsg);
                            mSmsAgentEnabled = rspMsg.getEnabled();
                            mSwitchTop.setOnCheckedChangeListener(null);
                            mSwitchTop.setChecked(mSmsAgentEnabled);
                            mSwitchTop.setOnCheckedChangeListener(smsAgentTopSwitchListener);

                            mSwitchBottom.setOnCheckedChangeListener(null);
                            mSwitchBottom.setChecked(mSmsAgentEnabled);
                            mSwitchBottom.setOnCheckedChangeListener(smsAgentBottomSwitchListener);
                            dismissDialog();
                            setView();
                            GreenUtils.upDataSmsAgentStatus(mSmsAgentEnabled, deviceId, userId);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        popErrorDialog(cause instanceof TimeoutException ? R.string.setup_timeout : R.string.setting_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                });
    }

    private void setView() {
        if (mSmsAgentEnabled) {
            edit.setVisibility(View.VISIBLE);
            edit.setText(R.string.edit);
            back.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.GONE);
            mEditStatusTop.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
            if (mList.isEmpty()) {
                mEmptyView.setVisibility(View.VISIBLE);
            }else {
                mEmptyView.setVisibility(View.GONE);
            }
        } else {
            back.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.GONE);
            edit.setVisibility(View.GONE);
            mEditStatusTop.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNotifySMSAgentNewSMS(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySMSAgentNewSMSReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            String deiceId = reqMsg.getDeviceId();
            GreenUtils.saveSmsEntity(reqMsg.getSms(), deiceId, mUserId);
            getDataFromDB();
        }
    }

    /// 不获取SMS数据（靠通知）
//    private void getSmsDataFromService(int pageIndex) {
//        String devId = mDeviceId;
//        if (TextUtils.isEmpty(devId)) {
//            L.e(TAG, " ERROR: decId is null");
//            return;
//        }
//        exec(
//                Message.SMSAgentGetReqMsg.newBuilder()
//                        .setPageSize(50)
//                        .setPageIdx(pageIndex)
//                        .setDeviceId(devId)
//                        .build(),
//                new TlcService.OnExecListener() {
//                    @Override
//                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
//                        Message.SMSAgentGetRspMsg rspMsg = response.getProtoBufMsg();
//
//                        if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
//                            L.e(TAG, "ModifyLocationModeReqMsg: " + rspMsg);
//                            dismissDialog();
//
//                            if (rspMsg.getSmsList().size() <= 0) {
//                                adapter.setNoMorePage();
//                            }
//                            if (pageIndex == 0) {
//                                mList.clear();
//                                GreenUtils.getSmsEntityDao().deleteAll();
//                            }
//                            for (Message.SMS sms : rspMsg.getSmsList()) {
//                                SmsEntity smsEntity = new SmsEntity();
//                                smsEntity.setSmsId(sms.getId());
//                                smsEntity.setDeviceId(devId);
//                                smsEntity.setUserId(mUserId);
//                                smsEntity.setTime(sms.getRecvTime());
//                                smsEntity.setNumber(sms.getPeerNumber());
//                                smsEntity.setText(sms.getContent());
//                                smsEntity.setSynced(false);
//                                mList.add(smsEntity);
//
//                                //只存第一页
//                                SmsEntityDao dao = GreenUtils.getSmsEntityDao();
//                                dao.insert(smsEntity);
//                            }
//                            filterData(mList);
//                            adapter.notifyDataSetChanged();
//                            adapter.notifyItemRemoved(adapter.getItemCount());
//                        }
//                        return false;
//                    }
//
//                    @Override
//                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
//                        popErrorDialog(cause instanceof TimeoutException ? R.string.load_timeout : R.string.load_failure);
//                    }
//
//                    @Override
//                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
//
//                    }
//                }
//        );
//    }
}
