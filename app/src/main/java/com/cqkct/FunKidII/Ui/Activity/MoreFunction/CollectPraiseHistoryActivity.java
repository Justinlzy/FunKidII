package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.CollectPraiseEntityDao;
import com.cqkct.FunKidII.db.Entity.CollectPraiseEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import java.util.ArrayList;
import java.util.List;

import protocol.Message;

public class CollectPraiseHistoryActivity extends BaseActivity {
    public static final String TAG = CollectPraiseHistoryActivity.class.getSimpleName();
    private List<Message.Praise> mHistoryPraiseList = new ArrayList<>();
    private CollectPraiseHistoryAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collect_praise_history);
        setTitleBarTitle(R.string.collect_history);

        intiView();
        loadData();
    }

    private void intiView() {
        adapter = new CollectPraiseHistoryAdapter(mHistoryPraiseList);
        SwipeMenuRecyclerView recyclerView = findViewById(R.id.collect_praise_history);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setSwipeMenuCreator(getSwipeMenuCreator(this));

        //右菜单删除
        recyclerView.setSwipeMenuItemClickListener(menuBridge -> {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            int position = menuBridge.getAdapterPosition();
            if (mHistoryPraiseList.get(position) != null)
                CollectPraiseHistoryActivity.this.showDeletePraiseDialog(mHistoryPraiseList.get(position));
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "loadData: deviceId is empty");
            return;
        }

        List<CollectPraiseEntity> listInDb = App.getInstance().getDaoSession().getCollectPraiseEntityDao()
                .queryBuilder()
                .where(CollectPraiseEntityDao.Properties.DeviceId.eq(deviceId))
                .orderDesc(CollectPraiseEntityDao.Properties.StartTime)
                .list();


        mHistoryPraiseList.clear();
        for (CollectPraiseEntity entity : listInDb) {
            Message.Praise praise = entity.getPraise();
            if (praise.getFinishTime() == 0 && praise.getStartTime() != 0) {
                continue;
            }
            mHistoryPraiseList.add(entity.getPraise());
        }
        adapter.notifyDataSetChanged();
    }


    private void showDeletePraiseDialog(final Message.Praise praise) {
        if (praise == null) {
            L.w(TAG, "showDeletePraiseDialog:  Message.Praise is null");
            return;
        }

        if (TextUtils.isEmpty(praise.getId())) {
            L.w(TAG, "showDeletePraiseDialog: praise.id is empty");
            return;
        }

        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
        dialogFragment.setTitle(getString(R.string.collect_hint))
                .setMessage(getString(R.string.collect_sure_delete_context))
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    deletePraise(praise);
                });
        dialogFragment.show(getSupportFragmentManager(), "showDeletePraiseDialog");
    }

    private void deletePraise(final Message.Praise praise) {
        final String deviceId = mDeviceId;
        if (TextUtils.isEmpty(deviceId)) {
            L.w(TAG, "deletePraise: deviceId is empty");
            return;
        }
        if (praise == null) {
            L.w(TAG, "deletePraise: praise is null");
            return;
        }
        if (TextUtils.isEmpty(praise.getId())) {
            L.w(TAG, "deletePraise: praise.id is empty");
            return;
        }

        popWaitingDialog(R.string.tip_deleting);

        exec(Message.DelPraiseReqMsg.newBuilder().setDeviceId(mDeviceId)
                        .setPraiseId(praise.getId()).build(),

                new TlcService.OnExecListener() {
                    @Override
                    public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                        try {
                            Message.DelPraiseRspMsg rspMsg = response.getProtoBufMsg();
                            if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                                mHistoryPraiseList.remove(praise);
                                GreenUtils.deletePraise(deviceId, praise);
                                adapter.notifyDataSetChanged();
                                popSuccessDialog(R.string.delete_success);
                                return false;
                            }
                            L.w(TAG, "deletePraise() -> exec() -> onResponse() " + rspMsg.getErrCode());
                        } catch (Exception e) {
                            L.e(TAG, "deletePraise() -> exec() -> onResponse() process failure", e);
                        }
                        popErrorDialog(R.string.delete_failure);
                        return false;
                    }

                    @Override
                    public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                        L.e(TAG, "deletePraise() -> exec() -> onException()", cause);
                        popErrorDialog(R.string.delete_failure);
                    }

                    @Override
                    public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

                    }
                });
    }

}






