package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.ClassDisableAdapter;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.DateUtil;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.ClassDisableEntityDao;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Entity.ClassDisableEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/11/29.
 */

public class ClassDisableActivity extends BaseActivity {
    public static final int ACTIVITY_REQUEST_ADD_CLASS_DISABLE = 3;
    public static final int ACTIVITY_REQUEST_EDIT = 4;
    public static final String INTENT_ACTION = ClassDisableActivity.class.getName();
    public static final String INTENT_PARAM_LIST = "ClassDisableList";
    public static final int ACTION_ITEM_DEL = 0;
    private ImageView emptyView;

    private SwipeMenuRecyclerView recyclerView;
    private static final String TAG = ClassDisableActivity.class.getSimpleName();
    private ClassDisableAdapter classDisableAdapter;
    private List<ClassDisableEntity> list = new ArrayList<>();
    private String devId;
    private boolean hasEditPermission = false;
    private int limit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.class_disable);
        setTitleBarTitle(getString(R.string.class_disable_period));
        if (TextUtils.isEmpty(mDeviceId)) {
            L.e(TAG, "onCreate: mDeviceId is empty!!! finish()");
            finish();
            return;
        }
        devId = mDeviceId;
        hasEditPermission = hasEditPermission();
        initView();
        loadLimit();
        loadData();
    }

    private void initView() {
        classDisableAdapter = new ClassDisableAdapter(this, list, hasEditPermission ? cdClickListener : null, hasEditPermission);
        recyclerView = findViewById(R.id.sw_class_disable);
        emptyView = findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        if (hasEditPermission)
            recyclerView.setSwipeMenuCreator(getSwipeMenuCreator(this));
        //右菜单删除
        recyclerView.setSwipeMenuItemClickListener(menuBridge -> {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            ClassDisableActivity.this.showDelDialog(menuBridge.getPosition());
        });
        recyclerView.setAdapter(classDisableAdapter);


        View addBtn = findViewById(R.id.title_bar_right_icon);
        addBtn.setVisibility(View.VISIBLE);
    }


    private void showDelDialog(final int position) {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        dialogFragment.setTitle(getString(R.string.delete_class_disable))
                .setMessage(getString(R.string.ask_del_class_disable))
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> delClassDisable(list.get(position)));
        dialogFragment.show(getSupportFragmentManager(), "showDeleteClassDisableConfirmDialog");
    }

    //删除上课禁用时间段
    private void delClassDisable(final ClassDisableEntity disableEntity) {
        popWaitingDialog(R.string.please_wait);

        Message.DelClassDisableReqMsg reqMsg = Message.DelClassDisableReqMsg.newBuilder()
                .setDeviceId(devId)
                .addClassDisableId(disableEntity.getClassDisableId())
                .build();

        exec(reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.DelClassDisableRspMsg delClassDisableRspMsg = response.getProtoBufMsg();
                            switch (delClassDisableRspMsg.getErrCode()) {
                                case SUCCESS:
                                    list.remove(disableEntity);
                                    classDisableAdapter.notifyDataSetChanged();
                                    emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

                                    L.e(TAG, "SetClassDisableS1ReqMsg SUCCESS");
                                    GreenUtils.delClassDisable(devId, disableEntity);
                                    dismissDialog();
                                    return true;
                                default:
                                    L.w(TAG, "delete class disable failure: " + delClassDisableRspMsg.getErrCode());
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "delete class disable failure", e);
                        }
                        popErrorDialog(R.string.delete_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "setClassDisable Exception: " + cause);
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

    private ClassDisableAdapter.OnItemClickListener cdClickListener = new ClassDisableAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position) {
            if (hasEditPermission) {
                L.d(TAG, "onItemLongClick: " + position + ", idx: " + position);
                Intent intent = new Intent(ClassDisableActivity.this, ClassDisableItemActivity.class);
                intent.putExtra("position", position);
                intent.putExtra(INTENT_PARAM_LIST, (Serializable) list);
                startActivityForResult(intent, ACTIVITY_REQUEST_EDIT);
            }
        }

        @Override
        public void onCompoundButtonClick(int position, CompoundButton cb) {
            if (cb.getId() == R.id.ib_switch) {
                L.e(TAG, "is_open:" + list.get(position).getEnable());

                classDisableAdapter.notifyDataSetChanged();
                modifyClassDisable(list.get(position), position);
            }
        }
    };

    private void loadLimit() {
        List<DeviceEntity> deviceEntities = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(mDeviceId))
                .build().list();
        if (!deviceEntities.isEmpty()) {
            DeviceEntity deviceEntity = deviceEntities.get(0);
            Message.DeviceSysInfo sysInfo = deviceEntity.getSysInfo();
            int cnt = sysInfo.getLimit().getCountOfClassDisable();
            if (cnt > 0)
                limit = cnt;
        }
    }

    private void loadData() {
        loadDataFromDb();
        getClassDisableData();
    }

    private void loadDataFromDb() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "getClassDisableData: deviceId is empty");
            return;
        }
        List<ClassDisableEntity> listInDb = GreenUtils.getClassDisableEntityDao().queryBuilder()
                .where(ClassDisableEntityDao.Properties.DeviceId.eq(mDeviceId)).build().list();
        list.clear();
        list.addAll(listInDb);
        classDisableAdapter.notifyDataSetChanged();
        emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    //查询上课禁用时间段
    private void getClassDisableData() {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "getClassDisableData: deviceId is empty");
            return;
        }

        popWaitingDialog(R.string.loading);
        exec(
                Message.GetClassDisableReqMsg.newBuilder()
                        .setDeviceId(devId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetClassDisableRspMsg getClassDisableRspMsg = response.getProtoBufMsg();
                            L.v(TAG, "getClassDisableData() -> exec() -> onResponse(): " + getClassDisableRspMsg);
                            switch (getClassDisableRspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.e(TAG, "getClassDisableData SUCCESS ");
                                    limit = getClassDisableRspMsg.getCountLimit();
                                    List<Message.ClassDisable> classDisables = getClassDisableRspMsg.getClassDisableList();
                                    GreenUtils.saveClassDisable(deviceId, classDisables);
                                    loadDataFromDb();
                                    dismissDialog();
                                    break;
                                default:
                                    popErrorDialog(R.string.load_failure);
                                    break;
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "getClassDisableData onException: " + cause);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ACTIVITY_REQUEST_ADD_CLASS_DISABLE && resultCode == RESULT_OK)
                || (requestCode == ACTIVITY_REQUEST_EDIT && resultCode == RESULT_OK)) {
            Serializable serializable = data.getSerializableExtra("addClassDisableRspMsg");
            if (serializable == null) return;
            List<ClassDisableEntity> disableEntities = (List<ClassDisableEntity>) serializable;
            for (ClassDisableEntity classDisableEntity : disableEntities) {
                L.e(TAG, "classDisableEntity: " + classDisableEntity.toString());
            }
            list.clear();
            list.addAll(disableEntities);
            classDisableAdapter.notifyDataSetChanged();
            emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
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
            case R.id.title_bar_right_icon:
                addOne();
                break;
        }
    }

    private void addOne() {
        if (list.size() >= limit) {
            toast(R.string.number_of_class_disable_out_of_limit);
            return;
        }


        Intent intent = new Intent(this, ClassDisableItemActivity.class);
        intent.putExtra("isAdd", true);
        intent.putExtra(INTENT_PARAM_LIST, (Serializable) list);
        startActivityForResult(intent, ACTIVITY_REQUEST_ADD_CLASS_DISABLE);
    }

    //修改上课禁用时间段
    private void modifyClassDisable(final ClassDisableEntity disableEntity, final int position) {
        popWaitingDialog(R.string.please_wait);
        Message.ModifyClassDisableReqMsg modifyClassDisableReqMsg = Message.ModifyClassDisableReqMsg.newBuilder()
                .setDeviceId(devId)
                .addClassDisable(Message.ClassDisable.newBuilder()
                        .setId(disableEntity.getClassDisableId())
                        .setName(disableEntity.getName())
                        .setTimezone(Message.Timezone.newBuilder().setZone(DateUtil.timezoneISO8601()).build())
                        .setStartTime(Message.TimePoint.newBuilder().setTime(disableEntity.getBeginTime()))
                        .setEndTime(Message.TimePoint.newBuilder().setTime(disableEntity.getEndTime()))
                        .setRepeat(disableEntity.getRepeat())
                        .setEnable(!disableEntity.getEnable()))
                .build();
        L.d(TAG, "modify classDisable req: " + modifyClassDisableReqMsg);
        exec(
                modifyClassDisableReqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifyClassDisableRspMsg modifyClassDisableRspMsg = response.getProtoBufMsg();
                            switch (modifyClassDisableRspMsg.getErrCode()) {
                                case SUCCESS:
                                    list.get(position).setEnable(!list.get(position).getEnable());
                                    list.set(position, disableEntity);
                                    classDisableAdapter.notifyDataSetChanged();
                                    L.e(TAG, "SetClassDisableS1ReqMsg SUCCESS");
                                    GreenUtils.addOrModifyClassDisable(devId, disableEntity);
                                    dismissDialog();
                                    return false;
                                case OUT_OF_LIMIT:
                                    popInfoDialog(R.string.number_of_class_disable_out_of_limit);
                                    return false;
                                default:
                                    L.w(TAG, "modifyClassDisableRspMsg failure: " + modifyClassDisableRspMsg.getErrCode());
                                    break;
                            }
                        } catch (Exception e) {
                            L.e(TAG, "modifyClassDisableRspMsg failure", e);
                        }
                        popErrorDialog(R.string.setting_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "modifyClassDisable onException: " + cause);
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

    @Override
    public void onClassDisableChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifyClassDisableChangedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            loadDataFromDb();
        }
    }

    @Override
    public void onClassDisableSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifyClassDisableSyncedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            loadDataFromDb();
        }
    }
}
