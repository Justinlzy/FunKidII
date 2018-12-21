package com.cqkct.FunKidII.Ui.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.MoreHeadAdapter;
import com.cqkct.FunKidII.Ui.BlurActivity.BaseBlurActivity;
import com.cqkct.FunKidII.Ui.view.PullBackLayout;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.gyf.barlibrary.ImmersionBar;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import protocol.Message;

public class AllBabiesActivity extends BaseBlurActivity {
    public static final String TAG = AllBabiesActivity.class.getSimpleName();

    private RecyclerView recyclerView;
    private String mUserIdForShown;
    private List<BabyEntity> mData = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.more_baby_list_activity);

        mUserIdForShown = mUserId;
        if (TextUtils.isEmpty(mUserIdForShown)) {
            finish();
            return;
        }

        initView();

        loadData();
    }

    @Override
    protected void initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.init();
    }

    private void initView() {
        ((PullBackLayout) findViewById(R.id.pull_back_layout)).setCallback(new PullBackLayout.Callback() {
            @Override public void onPullStart() {}
            @Override public void onPullDown(float progress) {}
            @Override public void onPullCancel() {}
            @Override public void onPullUp() {}
            @Override
            public void onPullComplete() {
                L.e(TAG, "onPullComplete");
                setResult(RESULT_CANCELED);
                finish();
                overridePendingTransition(0, R.anim.out_to_bottom);
            }
        });
        findViewById(R.id.slide_down_icon).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.out_to_bottom);
        });

        recyclerView = findViewById(R.id.all_baby_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 5);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(new MoreHeadAdapter(mData, pos -> {
            BabyEntity entity = mData.get(pos);
            GreenUtils.selectBaby(entity.getUserId(), entity.getDeviceId());
            setResult(RESULT_OK);
            finish();
        }, R.layout.all_baby_list));
    }

    private void loadData() {
        new LoadDataTask(this).execute(mUserIdForShown);
    }

    private static class LoadDataTask extends AsyncTask<String, String, List<BabyEntity>> {
        private WeakReference<AllBabiesActivity> mA;

        LoadDataTask(AllBabiesActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected List<BabyEntity> doInBackground(String... strings) {
            String userId = strings[0];
            return GreenUtils.getBabyEntityDao().queryBuilder().where(BabyEntityDao.Properties.UserId.eq(userId)).list();
        }

        @Override
        protected void onPostExecute(List<BabyEntity> babyEntities) {
            AllBabiesActivity a = mA.get();
            if (a == null) {
                return;
            }
            a.mData.clear();
            a.mData.addAll(babyEntities);
            a.recyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        for (int i = 0; i < mData.size(); ++i) {
            BabyEntity entity = mData.get(i);
            if (deviceInfo.getDeviceId().equals(entity.getDeviceId())) {
                List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder().where(
                        BabyEntityDao.Properties.UserId.eq(mUserIdForShown),
                        BabyEntityDao.Properties.DeviceId.eq(deviceInfo.getDeviceId())).list();
                if (!list.isEmpty()) {
                    mData.set(i, list.get(0));
                    recyclerView.getAdapter().notifyItemChanged(i);
                }
                break;
            }
        }
    }

    @Override
    public void onCurrentBabyChanged(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        super.onCurrentBabyChanged(oldBabyBean, newBabyBean, isSticky);
        if (newBabyBean != null) {
            for (int i = 0; i < mData.size(); ++i) {
                BabyEntity entity = mData.get(i);
                if (newBabyBean.getDeviceId().equals(entity.getDeviceId())) {
                    mData.set(i, newBabyBean);
                    recyclerView.getAdapter().notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserIdForShown)) {
            String deviceId = reqMsg.getUsrDevAssoc().getDeviceId();
            List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder().where(
                    BabyEntityDao.Properties.UserId.eq(mUserIdForShown),
                    BabyEntityDao.Properties.DeviceId.eq(deviceId)).list();
            if (!list.isEmpty()) {
                mData.add(list.get(0));
                recyclerView.getAdapter().notifyItemInserted(mData.size() - 1);
            }
        }
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserIdForShown)) {
            String deviceId = reqMsg.getUsrDevAssoc().getDeviceId();
            for (int i = 0; i < mData.size(); ++i) {
                BabyEntity entity = mData.get(i);
                if (deviceId.equals(entity.getDeviceId())) {
                    mData.remove(i);
                    recyclerView.getAdapter().notifyItemRemoved(i);
                    break;
                }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDeviceOnline(Event.DeviceOnline ev) {
        recyclerView.getAdapter().notifyDataSetChanged();
    }
}
