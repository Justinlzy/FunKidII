package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.AlarmClockListViewAdapter;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.AlarmClockEntityDao;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.AlarmClockEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/11/28.
 */

public class AlarmClockActivity extends BaseActivity {
    public static final String TAG = AlarmClockActivity.class.getSimpleName();

    public static final int ACTIVITY_REQUEST_CODE_ALARM_CLOCK_EDIT = 1;

    private SwipeMenuRecyclerView recyclerView;
    private AlarmClockListViewAdapter alarmClockListViewAdapter;
    private List<AlarmClockEntity> mList = new ArrayList<>();
    private boolean hasEditPermission = false;
    private int limit;
    private boolean hasVibrationMotor;
    private ImageView emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_clock);
        setTitleBarTitle(getString(R.string.alarm_clock));
        hasEditPermission = hasEditPermission();

        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        if (deviceInfo != null) {
            hasVibrationMotor = (deviceInfo.getDeviceEntity().getSysInfo().getHwFeature() & Message.HwFeature.HWF_VIBRATION_MOTOR_VALUE) != 0;
        }

        initView();
        loadLimit();
        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_ALARM_CLOCK_EDIT && resultCode == RESULT_OK) {
            loadDataInDb();
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
            case R.id.title_bar_right_icon: {
                if (mList.size() >= limit) {
                    toast(R.string.number_of_alarm_clock_out_of_limit);
                    return;
                }
                Intent intent = new Intent(this, AlarmClockItemActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_ALARM_CLOCK_EDIT);
                break;
            }
        }
    }

    private void initView() {
        View addIconView = findViewById(R.id.title_bar_right_icon);
        if (hasEditPermission) {
            addIconView.setVisibility(View.VISIBLE);
        } else
            addIconView.setVisibility(View.INVISIBLE);

        alarmClockListViewAdapter = new AlarmClockListViewAdapter(this, mList, hasVibrationMotor, hasEditPermission ? alarmClickAdapterClickListener : null);
        recyclerView = findViewById(R.id.recycler_alarm_clock);
        recyclerView.setLongPressDragEnabled(false); // 长按拖拽，默认关闭。

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setSwipeMenuCreator(getSwipeMenuCreator(this));
        recyclerView.setSwipeMenuItemClickListener(menuBridge -> {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            AlarmClockActivity.this.showDelDialog(menuBridge.getAdapterPosition());
        });
        recyclerView.setAdapter(alarmClockListViewAdapter);

        emptyView = findViewById(R.id.empty_view);

    }

    private void loadLimit() {
        List<DeviceEntity> deviceEntities = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(mDeviceId))
                .build().list();
        if (!deviceEntities.isEmpty()) {
            DeviceEntity deviceEntity = deviceEntities.get(0);
            Message.DeviceSysInfo sysInfo = deviceEntity.getSysInfo();
            int cnt = sysInfo.getLimit().getCountOfAlarmClock();
            if (cnt > 0)
                limit = cnt;
        }
    }

    private void loadData() {
        loadDataInDb();
        queryAlarmClocksFromServer();
    }

    private void loadDataInDb() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "loadDataInDb: deviceId is empty");
            return;
        }

        AlarmClockEntityDao dao = GreenUtils.getAlarmClockEntityDao();
        List<AlarmClockEntity> list = dao.queryBuilder()
                .where(AlarmClockEntityDao.Properties.DeviceId.eq(deviceId))
                .list();

        mList.clear();
        mList.addAll(list);
        emptyView.setVisibility(mList.isEmpty() ? View.VISIBLE : View.GONE);
        alarmClockListViewAdapter.notifyDataSetChanged();
    }

    private void showDelDialog(final int position) {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setTitle(getString(R.string.delete_alarm_clock))
                .setMessage(getString(R.string.ask_del_alarm_clock))
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> deleteAlarm(position))
                .setNegativeButton(getString(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "unbindDialog");

    }

    private void modifyAlarmClock(final AlarmClockEntity entity, final int position) {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "modifyAlarmClock: deviceId is empty");
            return;
        }

        popWaitingDialog(R.string.please_wait);

        entity.setSynced(false);
        final Message.ModifyAlarmClockReqMsg reqMsg = Message.ModifyAlarmClockReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .addAlarmClock(entity.getAlarmClock())
                .build();
        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyAlarmClockRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    AlarmClockEntity entity = GreenUtils.saveAlarmClock(deviceId, reqMsg.getAlarmClock(0));
                                    if (entity != null) {
                                        mList.set(position, entity);
                                    }
                                    alarmClockListViewAdapter.notifyDataSetChanged();
                                    dismissDialog();
                                    return false;
                                case OUT_OF_LIMIT:
                                    alarmClockListViewAdapter.notifyDataSetChanged();
                                    popInfoDialog(R.string.number_of_alarm_clock_out_of_limit);
                                    return false;
                                default:
                                    L.w(TAG, "delete alarm clock failure: " + rspMsg.getErrCode());
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "delete alarm clock failure", e);
                        }
                        alarmClockListViewAdapter.notifyDataSetChanged();
                        popErrorDialog(R.string.setting_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "onException: " + cause);
                        alarmClockListViewAdapter.notifyDataSetChanged();
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.setup_timeout);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.setting_failure);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                }
        );
    }


    private void deleteAlarm(final int position) {
        if (position < 0 || position >= mList.size()) {
            L.e(TAG, "deleteAlarm: position (" + position + "): position < 0 || position >= mList.size()");
            return;
        }
        final AlarmClockEntity alarmClockEntity = mList.get(position);
        if (alarmClockEntity == null) {
            L.e(TAG, "deleteAlarm: position (" + position + ") AlarmClockEntity is null");
            return;
        }
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.e(TAG, "deleteAlarm: deviceId is empty");
            return;
        }

        popWaitingDialog(R.string.please_wait);
        final Message.DelAlarmClockReqMsg delAlarmClockReqMsg = Message.DelAlarmClockReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .addAlarmClockId(alarmClockEntity.getAlarmClockId())
                .build();

        exec(
                delAlarmClockReqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.DelAlarmClockRspMsg delAlarmClockRspMsg = response.getProtoBufMsg();
                            switch (delAlarmClockRspMsg.getErrCode()) {
                                case SUCCESS:
                                    GreenUtils.delAlarmClock(deviceId, alarmClockEntity);
                                    mList.remove(position);
                                    emptyView.setVisibility(mList.isEmpty() ? View.VISIBLE : View.GONE);
                                    alarmClockListViewAdapter.notifyDataSetChanged();
                                    dismissDialog();
                                    return true;
                                default:
                                    L.w(TAG, "delete alarm clock failure: " + delAlarmClockRspMsg.getErrCode());
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "delete alarm clock failure", e);
                        }
                        popErrorDialog(R.string.delete_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "onException: " + cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.delete_failure);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void queryAlarmClocksFromServer() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryAlarmClocksFromServer: deviceId is empty");
            return;
        }

        popWaitingDialog(R.string.loading);
        exec(
                Message.GetAlarmClockReqMsg.newBuilder()
                        .setDeviceId(deviceId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetAlarmClockRspMsg rspMsg = response.getProtoBufMsg();
                            L.v(TAG, "queryAlarmClocksFromServer() -> exec() -> onResponse(): " + rspMsg);
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                limit = rspMsg.getCountLimit();
                                GreenUtils.saveAlarmClock(deviceId, rspMsg.getAlarmClockList());
                                loadDataInDb();
                                dismissDialog();
                                return false;
                            }
                            L.e(TAG, "queryAlarmClocksFromServer onResponse failure: " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "queryAlarmClocksFromServer onResponse process", e);
                        }

                        popErrorDialog(R.string.load_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "queryAlarmClocksFromServer onException", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.load_timeout);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.load_failure);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );

    }

    private AlarmClockListViewAdapter.AlarmClickAdapterClickListener alarmClickAdapterClickListener = new AlarmClockListViewAdapter.AlarmClickAdapterClickListener() {
        @Override
        public void myOnClick(int position, View v) {
            if (v.getId() == R.id.imageview_setting_datetime_sys) {
                AlarmClockEntity old = mList.get(position);
                if (old == null) {
                    L.e(TAG, "invalid position: AlarmClockEntity entity = list.get(position) == null", new Exception("invalid position"));
                    return;
                }
                AlarmClockEntity entity = new AlarmClockEntity();
                entity.setId(old.getId());
                entity.setDeviceId(old.getDeviceId());
                entity.setAlarmClock(old.getAlarmClock());

                Message.AlarmClock.Builder alarmClockBuilder = old.getAlarmClock().toBuilder();
                if ((entity.getRepeat() & Message.TimePoint.RepeatFlag.ALL_VALUE) == 0) {
                    // 单次闹钟
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(entity.getTimePoint() * 1000L);
                    if (calendar.before(Calendar.getInstance()) || !entity.getEnable()) {
                        // 时间已过/当前为关闭状态，意味着该动作为开启闹钟，需要重设时间戳
                        calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, entity.getHour());
                        calendar.set(Calendar.MINUTE, entity.getMinute());
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);
                        if (calendar.before(Calendar.getInstance())) {
                            // 今天的“某时:某分”已过，设为明天
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                        }
                        entity.setTimePoint(calendar.getTimeInMillis() / 1000L);
                        entity.setEnable(true);
                    } else {
                        entity.setEnable(false);
                    }
                    entity.setRepeat(0);
                } else {
                    entity.setEnable(!entity.getEnable());
                }
                modifyAlarmClock(entity, position);
            }
        }

        @Override
        public void OnItemClick(int position, View v) {
            L.v(TAG, "AlarmClickAdapterClickListener position : " + position);
            if (hasEditPermission) {
                Intent intent = new Intent(AlarmClockActivity.this, AlarmClockItemActivity.class);
                intent.putExtra(AlarmClockItemActivity.INTENT_PARAM_ALARM_CLOCK_POSITION, position);
                intent.putExtra(AlarmClockItemActivity.INTENT_PARAM_ALARM_CLOCK_ENTITY, mList.get(position));
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_ALARM_CLOCK_EDIT);
            }
        }
    };

    @Override
    public void onAlarmClockChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyAlarmClockChangedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            loadDataInDb();
        }
    }

    @Override
    public void onAlarmClockSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyAlarmClockSyncedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            loadDataInDb();
        }
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        if (deviceInfo.getDeviceId().equals(mDeviceId)) {
            boolean has = (deviceInfo.getDeviceEntity().getSysInfo().getHwFeature() & Message.HwFeature.HWF_VIBRATION_MOTOR_VALUE) != 0;
            if (hasVibrationMotor != has) {
                hasVibrationMotor = has;
                alarmClockListViewAdapter.setHasVibrationMotor(hasVibrationMotor);
            }
        }
    }
}
