package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.Adapter.SmsAgentDetailAdapter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.db.Dao.SmsEntityDao;
import com.cqkct.FunKidII.db.Entity.SmsEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SmsDetailActivity extends BaseActivity {

    public static final String TAG = SmsDetailActivity.class.getSimpleName();
    private LinearLayout mDeleteLl;

    private RecyclerView recyclerView;

    private TextView edit, cancel;
    private ImageView back;


    private SmsAgentDetailAdapter adapter;
    private Map<Long, SmsEntity> mSelectedMap = new ConcurrentHashMap<>();
    private List<SmsEntity> mList = new ArrayList<>();
    boolean hasEditPermission;
    private SmsEntity entity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_detail_agent);
        setTitleBarTitle(R.string.sms_agent);
        Intent intent = getIntent();
        entity = (SmsEntity) intent.getSerializableExtra(SmsAgentActivity.ON_DETAIL_DATA_FLAG);
        if (entity == null) {
            this.finish();
        }
        initView();
        initData();

    }

    private void initView() {
        hasEditPermission = hasEditPermission();

        edit = findViewById(R.id.title_bar_right_text);
        cancel = findViewById(R.id.title_bar_left_text);
        cancel.setText(R.string.cancel);
        back = findViewById(R.id.title_bar_left_icon);


        findViewById(R.id.delete).setOnClickListener(v -> {
            if (mSelectedMap.isEmpty())
                return;
            SmsEntityDao dao = GreenUtils.getSmsEntityDao();
            dao.deleteByKeyInTx(mSelectedMap.keySet());
            mSelectedMap.clear();
            getDataFromDB();
            resetLayout();
        });
        mDeleteLl = findViewById(R.id.delete_ll);
        recyclerView = findViewById(R.id.recycler_sms);
        adapter = new SmsAgentDetailAdapter(this, mList, mSelectedMap);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        adapter.setItemClickListener(new SmsAgentDetailAdapter.onItemClickListener() {
            @Override
            public void onNumberClick(int position, SmsEntity entity) {
                if (mSelectedMap.get(entity.getId()) == null) {
                    mSelectedMap.put(entity.getId(), entity);
                } else {
                    mSelectedMap.remove(entity.getId());
                }
                if (mSelectedMap.size() > 0) {
                    mDeleteLl.setVisibility(View.VISIBLE);
                } else {
                    mDeleteLl.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onItemLongClick(int position, SmsEntity entity) {
                mSelectedMap.clear();
                adapter.notifyCheckable(true);
                edit.setText(R.string.select_all);
                edit.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                back.setVisibility(View.GONE);
                if (mSelectedMap.size() > 0) {
                    mDeleteLl.setVisibility(View.VISIBLE);
                } else {
                    mDeleteLl.setVisibility(View.GONE);
                }
            }
        });
    }

    public void initData() {
        getDataFromDB();
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        switch (v.getId()) {
            case R.id.title_bar_right_text:
                if (edit.getText().toString().equals(getString(R.string.select_all))) {
                    back.setVisibility(View.GONE);
                    cancel.setVisibility(View.VISIBLE);

                    for (SmsEntity entity : mList) {
                        if (mSelectedMap.get(entity.getId()) == null) {
                            mSelectedMap.put(entity.getId(), entity);
                        }
                    }
                    if (mSelectedMap.size() > 0) {
                        mDeleteLl.setVisibility(View.VISIBLE);
                    } else {
                        mDeleteLl.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                }
                break;
            case R.id.title_bar_left_text:
                if (!TextUtils.isEmpty(cancel.getText().toString())) {
                    resetLayout();
                    cancel.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void resetLayout() {
        adapter.notifyCheckable(false);
        edit.setVisibility(View.GONE);
        back.setVisibility(View.VISIBLE);
        mDeleteLl.setVisibility(View.GONE);
    }

    private void getDataFromDB() {
        mList.clear();
        List<SmsEntity> entities = filterData(GreenUtils.getSmsEntityDao().queryBuilder()
                .where(SmsEntityDao.Properties.DeviceId.eq(mDeviceId), SmsEntityDao.Properties.UserId.eq(mUserId))
                .orderDesc(SmsEntityDao.Properties.Time)
                .list());
        GreenUtils.updateSmsUnreadMark(entities);
        mList.addAll(entities);
        adapter.notifyDataSetChanged();
    }


    public List<SmsEntity> filterData(List<SmsEntity> entities) {
        for (Iterator<SmsEntity> it = entities.iterator(); it.hasNext(); ) {
            SmsEntity sms = it.next();
            if (!sms.getNumber().equals(entity.getNumber())) {
                it.remove();
            }
        }
        return entities;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
