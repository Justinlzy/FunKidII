package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.SosContactRecyclerAdapter;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Ui.fragment.ContactsEditDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PhoneNumberUtils;
import com.cqkct.FunKidII.db.Dao.ContactEntityDao;
import com.cqkct.FunKidII.db.Dao.DeviceEntityDao;
import com.cqkct.FunKidII.db.Dao.SosEntityDao;
import com.cqkct.FunKidII.db.Entity.ContactEntity;
import com.cqkct.FunKidII.db.Entity.DeviceEntity;
import com.cqkct.FunKidII.db.Entity.SosEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.NotYetLoginException;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.cqkct.FunKidII.service.tlc.WaitThirdStageTimeoutException;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;
import com.yanzhenjie.recyclerview.swipe.touch.OnItemMoveListener;

import java.io.IOException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import protocol.Message;

/**
 * Created by justin on 2017/11/6.
 */

public class SosContactActivity extends BaseActivity {
    public static final String TAG = SosContactActivity.class.getSimpleName();

    private List<SosEntity> mOriginListData = new ArrayList<>(); // 原始数据，与显示数据对比，以判断是否重排了顺序
    private List<SosEntity> mListData = new ArrayList<>(); // 显示数据
    private List<ContactEntity> contactEntities = new ArrayList<>();
    private SwipeMenuRecyclerView recyclerView;
    private SosContactRecyclerAdapter sosContactRecyclerAdapter;
    private boolean hasEditPermission = false;
    private ImageView emptyView;
    private boolean itemOrderOfListChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_contact);
        setTitleBarTitle(R.string.sos_contact);
        hasEditPermission = hasEditPermission();
        initView();

        loadDbData();
        queryFromServer(true);
    }

    private void initView() {
        recyclerView = findViewById(R.id.list);
        emptyView = findViewById(R.id.empty_view);
        sosContactRecyclerAdapter = new SosContactRecyclerAdapter(mListData, hasEditPermission, contactEntities);
        if (hasEditPermission) {
            recyclerView.setSwipeItemClickListener((itemView, position) -> {
                L.v(TAG, "recyclerView.addOnItemTouchListener onItemClick" + position);
                SosEntity sosEntity = mListData.get(position);
                modifySos(sosEntity);
            });
        }
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(mDeviceId);
        if (deviceInfo != null){
            long func_module = deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule();
            if ((func_module & Message.FuncModule.FM_SOS_CALL_ORDER_VALUE) != 0) {
                recyclerView.setLongPressDragEnabled(hasEditPermission); // 长按拖拽，默认关闭。
            }
        }
        recyclerView.setOnItemMoveListener(new OnItemMoveListener() {
            @Override
            public boolean onItemMove(RecyclerView.ViewHolder srcHolder, RecyclerView.ViewHolder targetHolder) {
                // 不同的ViewType不能拖拽换位置。
                if (srcHolder.getItemViewType() != targetHolder.getItemViewType())
                    return false;

                // 真实的Position：通过ViewHolder拿到的position都需要减掉HeadView的数量。
                int fromPosition = srcHolder.getAdapterPosition() - recyclerView.getHeaderItemCount();
                int toPosition = targetHolder.getAdapterPosition() - recyclerView.getHeaderItemCount();

                Collections.swap(mListData, fromPosition, toPosition);
                sosContactRecyclerAdapter.notifyItemMoved(fromPosition, toPosition);

                boolean orderChanged = false;
                try {
                    for (int i = 0; i < mOriginListData.size(); ++i) {
                        if (!mOriginListData.get(i).getId().equals(mListData.get(i).getId())) {
                            orderChanged = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    L.e(TAG, "OnItemMoveListener.onItemMove", e);
                    orderChanged = true;
                }

                itemOrderOfListChanged = orderChanged;
                return true; // 返回 true 表示处理了并可以换位置，返回 false 表示你没有处理并不能换位置。
            }

            @Override
            public void onItemDismiss(RecyclerView.ViewHolder srcHolder) {
            }
        });
        recyclerView.setOnTouchListener((v, event) -> {
            if (itemOrderOfListChanged && event.getAction() == MotionEvent.ACTION_UP) {
                changeSosCallOrder();
                itemOrderOfListChanged = false;
            }
            return false;
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setSwipeMenuCreator((swipeLeftMenu, swipeRightMenu, viewType) -> {
            if (hasEditPermission) {
                int width = getResources().getDimensionPixelSize(R.dimen.dp_70);
                // 1. MATCH_PARENT 自适应高度，保持和Item一样高;
                // 2. 指定具体的高，比如80;
                // 3. WRAP_CONTENT，自身高度，不推荐;
                int height = ViewGroup.LayoutParams.MATCH_PARENT;
                // 添加右侧的，如果不添加，则右侧不会出现菜单。
                SwipeMenuItem deleteItem = new SwipeMenuItem(SosContactActivity.this)
                        .setImage(R.drawable.delete_left_slip)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(deleteItem);// 添加菜单到右侧。
            }
        });
        //右菜单删除
        recyclerView.setSwipeMenuItemClickListener(menuBridge -> {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            SosContactActivity.this.deleteSos(mListData.get(menuBridge.getAdapterPosition()));
        });
        recyclerView.setAdapter(sosContactRecyclerAdapter);

        if (hasEditPermission) {
            View addBtn = findViewById(R.id.title_bar_right_icon);
            addBtn.setVisibility(View.VISIBLE);
        }
    }

    private void loadDbData() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "loadDataInDb: deviceId is empty");
            return;
        }

        SosEntityDao dao = GreenUtils.getSosEntityDao();
        mOriginListData = dao.queryBuilder()
                .where(SosEntityDao.Properties.DeviceId.eq(deviceId))
                .orderAsc(SosEntityDao.Properties.CallOrder)
                .list();
        mListData.clear();
        mListData.addAll(mOriginListData);

        // 加载绑定用户
        loadBindUsers(deviceId);

        emptyView.setVisibility(!mListData.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerView.smoothCloseMenu();
        recyclerView.postDelayed(() -> recyclerView.getAdapter().notifyDataSetChanged(), 20);
    }

    private void loadBindUsers(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "loadBindUsers: deviceId is empty");
            return;
        }

        List<ContactEntity> assocList = GreenUtils.getContactEntityDao().queryBuilder().where(
                ContactEntityDao.Properties.DeviceId.eq(deviceId),
                ContactEntityDao.Properties.UserId.isNotNull(),
                ContactEntityDao.Properties.UserId.notEq("")
        ).list();

        contactEntities.clear();
        contactEntities.addAll(assocList);
    }

    private void queryFromServer(boolean showWaitingDialog) {
        String userId = mUserId;
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "queryFromServer(): userId: " + userId + ", deviceId: " + deviceId);
            return;
        }
        if (showWaitingDialog) {
            popWaitingDialog(R.string.loading);
        }
        Message.GetSosReqMsg reqMsg = Message.GetSosReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .build();
        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.GetSosRspMsg rspMsg = response.getProtoBufMsg();
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                L.v(TAG, "queryFromServer() -> exec() -> onResponse(): " + rspMsg);
                                GreenUtils.saveSos(deviceId, rspMsg.getSosList());
                                loadDbData();
                                if (showWaitingDialog) {
                                    dismissDialog();
                                }
                            }
                            L.w(TAG, "queryFromServer() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                            return false;
                        } catch (Exception e) {
                            L.e(TAG, "queryFromServer() -> exec() -> onResponse() process failure", e);
                        }
                        if (showWaitingDialog) {
                            popErrorDialog(R.string.load_failure);
                        }
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "queryFromServer() -> exec() -> onException()", cause);
                        if (showWaitingDialog) {
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
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
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
                addSos();
                break;
        }
    }

    public void addSos() {
        String userId = mUserId;
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "addSos(): userId: " + userId + " or deviceId: " + deviceId + " empty");
            return;
        }

        int maxCount = 5;
        List<DeviceEntity> deviceEntities = GreenUtils.getDeviceEntityDao().queryBuilder()
                .where(DeviceEntityDao.Properties.DeviceId.eq(mDeviceId))
                .list();
        if (!deviceEntities.isEmpty()) {
            DeviceEntity deviceEntity = deviceEntities.get(0);
            Message.DeviceSysInfo sysInfo = deviceEntity.getSysInfo();
            int cnt = sysInfo.getLimit().getCountOfSOS();
            if (cnt > 0)
                maxCount = cnt;
        }
        if (mListData.size() >= maxCount) {
            toast(R.string.number_of_sos_out_of_limit);
            return;
        }
        ContactsEditDialogFragment dialogFragment = new ContactsEditDialogFragment();
        dialogFragment.setTitle(getString(R.string.ios_57))
                .setNegativeButton(getString(R.string.cancel))
                .setPositiveButton(getString(R.string.ok), new OnPositiveButtonClickListener(userId, deviceId, "", -1));
        dialogFragment.show(getSupportFragmentManager(), "AddSosContactDialog");
    }

    private void modifySos(SosEntity entity) {
        String userId = mUserId;
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "modifySos(): userId: " + userId + " or deviceId: " + deviceId + " empty");
            return;
        }

        ContactsEditDialogFragment dialogFragment = new ContactsEditDialogFragment();
        dialogFragment.setTitle(getString(R.string.ios_59))
                .setNameText(entity.getName())
                .setNumberText(entity.getNumber())
                .setNegativeButton(getString(R.string.cancel))
                .setPositiveButton(getString(R.string.ok), new OnPositiveButtonClickListener(userId, deviceId, entity.getSosId(), entity.getCallOrder()));
        dialogFragment.show(getSupportFragmentManager(), "ModifySosContactDialog");
    }

    private class OnPositiveButtonClickListener implements ContactsEditDialogFragment.OnPositiveButtonClickListener {
        private String userId;
        private String deviceId;
        private String sosId;
        private int callOrder;

        OnPositiveButtonClickListener(String userId, String deviceId, String sosId, int callOrder) {
            this.userId = userId;
            this.deviceId = deviceId;
            this.sosId = sosId;
            this.callOrder = callOrder;
        }

        @Override
        public void onClick(@NonNull ContactsEditDialogFragment dialog, @Nullable String name, @Nullable String number) {
            if (TextUtils.isEmpty(name)) {
                toast(R.string.sos_contact_name_noting_null);
                return;
            }
            if (TextUtils.isEmpty(number)) {
                toast(R.string.phonenumber_can_not_be_null);
                return;
            }

//            if (!PublicTools.isValidMobileNo(number) && !PublicTools.IsSosPhone(number)) {
//                toast(R.string.sos_contact_number_formal_error);
//                return;
//            }

            // 检查号码重复
            // 登录用户的国家代码
            String userCountryCode = PhoneNumberUtils.pickCountryCodeFromNumber(GreenUtils.getUserPhone(userId));
            if (TextUtils.isEmpty(userCountryCode))
                userCountryCode = "";

            // 带国家代码的输入号码
            String fullNumber = number;
            if (TextUtils.isEmpty(PhoneNumberUtils.pickCountryCodeFromNumber(number))) {
                fullNumber = userCountryCode + number;
            }
            SosEntity self = null;
            for (SosEntity one : mListData) {
                if (one.getSosId().equals(sosId)) {
                    // 自己
                    self = one;
                    continue;
                }

                // 带国家代码的已有联系人号码
                String otherFullNum = one.getNumber();
                if (TextUtils.isEmpty(PhoneNumberUtils.pickCountryCodeFromNumber(otherFullNum))) {
                    otherFullNum = userCountryCode + otherFullNum;
                }

                if (fullNumber.equals(otherFullNum)) {
                    toast(R.string.number_already_exists);
                    return;
                }
            }

            if (TextUtils.isEmpty(sosId)) {
                doAddSos(deviceId, name, number);
            } else if (self != null && (!name.equals(self.getName()) || !number.equals(self.getNumber()))) {
                doModifySos(deviceId, sosId, name, number, callOrder);
            }
            emptyView.setVisibility(View.GONE);//添加后隐藏背景图片
            dialog.dismiss();
        }
    }

    private void doAddSos(String deviceId, String name, String number) {
        popWaitingDialog(R.string.please_wait);
        Message.SOS sos = Message.SOS.newBuilder()
                .setName(name)
                .setPhonenum(number)
                .build();
        exec(
                Message.AddSosReqMsg.newBuilder()
                        .addSos(sos)
                        .setDeviceId(deviceId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.AddSosRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.d(TAG, "doAddSos() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                                    if (rspMsg.getSosCount() > 0) {
                                        SosEntity entity = GreenUtils.addOrModifySos(deviceId, rspMsg.getSos(0));
                                        mListData.add(entity);
                                        sosContactRecyclerAdapter.notifyItemInserted(mListData.size() - 1);
                                    }
                                    dismissDialog();
                                    return false;
                                case OUT_OF_LIMIT:
                                    L.d(TAG, "doAddSos() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                                    popInfoDialog(R.string.number_of_sos_out_of_limit);
                                    return false;
                            }
                            L.w(TAG, "doAddSos() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "doAddSos() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "doAddSos() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.submit_timeout);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.submit_failure);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void doModifySos(String deviceId, String sosId, String name, String number, int callOrder) {
        final Message.SOS sos = Message.SOS.newBuilder()
                .setId(sosId)
                .setName(name)
                .setPhonenum(number)
                .build();
        exec(
                Message.ModifySosReqMsg.newBuilder()
                        .addSos(sos)
                        .setDeviceId(deviceId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ModifySosRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.d(TAG, "doModifySos() -> exec() -> onResponse() SUCCESS");
                                    GreenUtils.addOrModifySos(deviceId, sos.toBuilder().setCallOrder(callOrder).build());
                                    SosEntity old = null;
                                    for (int i = 0; i < mListData.size(); ++i) {
                                        SosEntity one = mListData.get(i);
                                        if (sosId.equals(one.getSosId())) {
                                            one.setName(name);
                                            one.setNumber(number);
                                            sosContactRecyclerAdapter.notifyItemChanged(i);
                                            old = one;
                                            break;
                                        }
                                    }
                                    if (old == null) {
                                        queryFromServer(false);
                                    }
                                    dismissDialog();
                                    return false;
                                case OUT_OF_LIMIT:
                                    L.d(TAG, "doModifySos() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                                    popInfoDialog(R.string.number_of_sos_out_of_limit);
                                    return false;
                                default:
                                    break;
                            }
                            L.d(TAG, "doModifySos() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "doModifySos() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.submit_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "doModifySos() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.submit_timeout);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.submit_failure);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    private void deleteSos(SosEntity sosEntity) {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "deleteSos() deviceId is empty");
            return;
        }
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        dialogFragment.setMessage(getString(R.string.sos_contact_sure_delete_number))
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    doDeleteSos(deviceId, sosEntity.getSosId());
                    dialog.dismiss();
                });
        dialogFragment.show(getSupportFragmentManager(), "DeleteSOSConfirmDialog");
    }

    private void doDeleteSos(String deviceId, String sosId) {
        popWaitingDialog(R.string.tip_deleting);
        exec(
                Message.DelSosReqMsg.newBuilder()
                        .addSosId(sosId)
                        .setDeviceId(deviceId)
                        .build(),
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.DelSosRspMsg delSosRspMsg = response.getProtoBufMsg();
                            switch (delSosRspMsg.getErrCode()) {
                                case SUCCESS:
                                    L.d(TAG, "commitData() -> exec() -> onResponse() SUCCESS");
                                    GreenUtils.deleteSos(deviceId, sosId);
                                    SosEntity old = null;
                                    for (int i = 0; i < mListData.size(); ++i) {
                                        SosEntity one = mListData.get(i);
                                        if (sosId.equals(one.getSosId())) {
                                            mListData.remove(i);
                                            recyclerView.smoothCloseMenu();
                                            sosContactRecyclerAdapter.notifyItemRemoved(i);
                                            old = one;
                                            break;
                                        }
                                    }
                                    if (mListData.isEmpty()) {
                                        emptyView.setVisibility(View.VISIBLE);//全部删除后显示背景图片
                                    }
                                    if (old == null) {
                                        loadDbData();
                                    }
                                    dismissDialog();
                                    return true;
                                default:
                                    break;
                            }
                            L.w(TAG, "commitData() -> exec() -> onResponse() " + delSosRspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "commitData() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.delete_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "commitData() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.request_timed_out);
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

    public void changeSosCallOrder() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "changeSosCallOrder() deviceId is empty");
            return;
        }
        popWaitingDialog(R.string.please_wait);
        List<String> list = new ArrayList<>();
        List<Message.SOS> sosList = new ArrayList<>();
        int order = 1;
        for (SosEntity entity : mListData) {
            list.add(entity.getSosId());
            sosList.add(entity.toSOS().toBuilder().setCallOrder(order).setDevSynced(entity.getSynced()).build());
            order++;
        }
        Message.ChangeSosCallOrderReqMsg reqMsg = Message.ChangeSosCallOrderReqMsg.newBuilder()
                .setDeviceId(deviceId)
                .addAllOrderedSosId(list)
                .build();
        exec(
                reqMsg,
                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.ChangeSosCallOrderRspMsg rspMsg = response.getProtoBufMsg();
                            switch (rspMsg.getErrCode()) {
                                case SUCCESS:
                                case DATA_CONFLICT:
                                    L.d(TAG, " changeSosCallOrder() -> exec() -> onResponse(): " + rspMsg.getErrCode());
                                    GreenUtils.saveSos(deviceId, sosList);
                                    if (rspMsg.getErrCode() == Message.ErrorCode.DATA_CONFLICT) {
                                        queryFromServer(false);
                                    } else {
                                        loadDbData();
                                    }
                                    dismissDialog();
                                    return false;
                            }
                            L.w(TAG, "changeSosCallOrder() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "changeSosCallOrder() -> exec() -> onResponse() process failure", e);
                        }
                        popSuccessDialog(R.string.app_setting_change_failed);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "changeSosCallOrder() -> exec() -> onException()", cause);
                        if (cause instanceof TimeoutException) {
                            popErrorDialog(R.string.app_setting_change_timeout);
                        } else if (cause instanceof NotYetConnectedException || cause instanceof IOException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof NotYetLoginException) {
                            popErrorDialog(R.string.network_quality_poor);
                        } else if (cause instanceof WaitThirdStageTimeoutException) {
                        } else {
                            popErrorDialog(R.string.app_setting_change_failed);
                        }
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
                    }
                }
        );
    }

    @Override
    public void onDevSosChanged(TlcService tlcService, @Nullable Pkt pkt, @NonNull Message.NotifySosChangedReqMsg reqMsg) {
        if (!TextUtils.isEmpty(mDeviceId) && mDeviceId.equals(reqMsg.getDeviceId())) {
            loadDbData();
        }
    }

    @Override
    public void onDevSosSynced(TlcService tlcService, @NonNull Pkt pkt, @NonNull Message.NotifySosSyncedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            if (TextUtils.isEmpty(mUserId)) {
                L.e(TAG, "onDevSosSynced userId is null");
                return;
            }
            loadDbData();
        }
    }

    @Override
    public void onDevConfChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyDevConfChangedReqMsg reqMsg) {
        if (reqMsg.getDeviceId().equals(mDeviceId)) {
            if (TextUtils.isEmpty(mUserId)) {
                L.e(TAG, "onDevSosSynced userId is null");
                return;
            }
            loadDbData();
        }
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId)) {
            loadBindUsers(mDeviceId);
            recyclerView.smoothCloseMenu();
            sosContactRecyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceId)) {
            loadBindUsers(mDeviceId);
            recyclerView.smoothCloseMenu();
            sosContactRecyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onNotifySosCallOrderChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifySosCallOrderChangedReqMsg reqMsg) {
        if (!TextUtils.isEmpty(mDeviceId) && mDeviceId.equals(reqMsg.getDeviceId())) {
            queryFromServer(false);
        }
    }
}
