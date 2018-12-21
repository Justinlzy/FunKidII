package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.Bean.MessageBabiesBean;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.NotifyMessageRecyclerViewAdapter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.NotifyMessageEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.NotifyMessageEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.protobuf.InvalidProtocolBufferException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import protocol.Message;


/**
 * Created by justin on 2017/8/11.
 */

public class MessageListsActivity extends BaseActivity {
    public static final String TAG = MessageListsActivity.class.getSimpleName();

    public static final int BIND_REQ_FLAG  = 1;
    private Map<Long, NotifyMessageEntity> selectedMap = new ConcurrentHashMap<>();
    private NotifyMessageRecyclerViewAdapter messageRvAdapter;
    private List<NotifyMessageEntity> notifyMessageEntities = new ArrayList<>();

    private TextView left_text, right_text, title_text;
    private ImageView left_icon, right_icon;
    private String titleString;
    private LinearLayout deleteLl;

    private String mDeviceIdOfShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notify_message_list);

        MessageBabiesBean babiesBean = (MessageBabiesBean) getIntent().getSerializableExtra(MessageBabiesListActivity.MESSAGEBABIESDATA);
        if (babiesBean == null) {
            L.w(TAG, "no MessageBabiesBean! finish");
            finish();
            return;
        }
        mDeviceIdOfShow = babiesBean.getDeviceId();

        initView(babiesBean.getBabyName());

        setMessageHasBeenRead(mDeviceIdOfShow);
        getMessageDataFromDb();
    }

    private void initView(String babyNameOfShow) {
        titleString = String.valueOf((TextUtils.isEmpty(babyNameOfShow) ? getString(R.string.baby) : babyNameOfShow) + getString(R.string.who_message));

        title_text = findViewById(R.id.title_bar_title_text);
        title_text.setText(titleString);

        left_icon = findViewById(R.id.title_bar_left_icon);
        left_text = findViewById(R.id.title_bar_left_text);
        right_icon = findViewById(R.id.title_bar_right_icon);
        right_icon.setImageResource(R.drawable.set_buton);
        right_icon.setVisibility(View.VISIBLE);
        right_text = findViewById(R.id.title_bar_right_text);

        deleteLl = findViewById(R.id.delete_ll);

        if (TextUtils.isEmpty(mUserId)) {
            L.e(TAG, "initView: userId is empty finish activity!!!");
            finish();
            return;
        }
        RecyclerView mRecyclerView = findViewById(R.id.ls_sms_list);
        messageRvAdapter = new NotifyMessageRecyclerViewAdapter(this, notifyMessageEntities, selectedMap);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(messageRvAdapter);

        messageRvAdapter.setOnShowItemClickListener(new NotifyMessageRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onLongClickListener(int position, NotifyMessageEntity bean) {
                messageRvAdapter.notifyCheckable(true);
                right_icon.setVisibility(View.GONE);
                right_text.setVisibility(View.VISIBLE);
                right_text.setText(R.string.select_all);
                left_icon.setVisibility(View.GONE);
                left_text.setVisibility(View.VISIBLE);
                left_text.setText(R.string.cancel);
                deleteLl.setVisibility(View.VISIBLE);
                title_text.setText(getString(R.string.selected_items, selectedMap.size()));
            }

            @Override
            public void onDateClickListener(int position, NotifyMessageEntity bean) {
                if (!messageRvAdapter.getNotifyCheckable())
                    return;
                long timePosition = bean.getTime();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timePosition);

                for (int i = position; i < notifyMessageEntities.size(); i++) {
                    NotifyMessageEntity entity = notifyMessageEntities.get(i);
                    Calendar calendarPro = Calendar.getInstance();
                    calendarPro.setTimeInMillis(entity.getTime());
                    if (calendar.get(Calendar.DAY_OF_YEAR) == calendarPro.get(Calendar.DAY_OF_YEAR) && calendar.get(Calendar.YEAR) == calendarPro.get(Calendar.YEAR)) {
                        if (selectedMap.get(entity.getId()) == null) {
                            selectedMap.put(entity.getId(), bean);
                        } else {
                            selectedMap.remove(entity.getId());
                        }
                    } else {
                        break;
                    }
                }
                messageRvAdapter.notifyDataSetChanged();
                title_text.setText(getString(R.string.selected_items, selectedMap.size()));
            }

            @Override
            public void onClickListener(int position, NotifyMessageEntity bean) {
                if (selectedMap.get(bean.getId()) == null) {
                    selectedMap.put(bean.getId(), bean);
                } else {
                    selectedMap.remove(bean.getId());
                }
                messageRvAdapter.notifyDataSetChanged();
                title_text.setText(getString(R.string.selected_items, selectedMap.size()));
            }

            @Override
            public void onBindItemClickListener(int position, NotifyMessageEntity entity) {
                Intent intent = new Intent(MessageListsActivity.this, BindRequestActivity.class);
                intent.putExtra(BindRequestActivity.PARAM_KEY_BINDREQ_SEQ, entity.getSeq());
                Message.NotifyAdminBindDevReqMsg reqMsg = null;
                try {
                    reqMsg = Message.NotifyAdminBindDevReqMsg.parseFrom(entity.getData());
                    Message.FetchUsrDevParticRspMsg rspMsg = Message.FetchUsrDevParticRspMsg.newBuilder()
                            .setBaby(Message.Baby.newBuilder().setName(entity.getDeviceName()).build())
                            .setUserInfo(Message.UserInfo.newBuilder()
                                    .setPhone(entity.getOriginator_phone()).build())
                            .build();

                    intent.putExtra(BindRequestActivity.PARAM_KEY_BINDREQ_PROTOBUF_MSG, reqMsg);
                    intent.putExtra(BindRequestActivity.PARAM_KEY_FROM_USER_INFO_PROTOBUF_MSG, rspMsg);
                    intent.putExtra(BindRequestActivity.PARAM_KEY_TO_USER_ID, mUserId);
                    intent.putExtra(BindRequestActivity.PARAM_KEY_FROM_MESSAGE_CENTER, true);
                    startActivityForResult(intent, BIND_REQ_FLAG);

                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BIND_REQ_FLAG && resultCode == RESULT_OK) {
            messageRvAdapter.notifyDataSetChanged();
        }
    }

    private void getMessageDataFromDb() {
        List<NotifyMessageEntity> list = GreenUtils.getNotifyMessageEntityDao().queryBuilder()
                .where(NotifyMessageEntityDao.Properties.UserId.eq(mUserId))
                .where(NotifyMessageEntityDao.Properties.DeviceId.eq(mDeviceIdOfShow))
                .orderDesc(NotifyMessageEntityDao.Properties.Time)
                .orderDesc(NotifyMessageEntityDao.Properties.Id)
                .build().list();
        notifyMessageEntities.clear();
        notifyMessageEntities.addAll(list);
        messageRvAdapter.notifyDataSetChanged();
    }


    @Override
    public void onTitleBarClick(View v) {
        super.onTitleBarClick(v);
        switch (v.getId()) {
            case R.id.title_bar_right_icon: {
                Intent intent = new Intent(this, NotifyMessageSettingActivity.class);
                intent.putExtra(NotifyMessageSettingActivity.DEVICE_ID_OF_SHOW, mDeviceIdOfShow);
                startActivity(intent);
            }
            break;
            case R.id.title_bar_right_text:
                if (right_text.getText().equals(getString(R.string.select_all))) {
                    for (NotifyMessageEntity entity : notifyMessageEntities) {
                        if (selectedMap.get(entity.getId()) == null) {
                            selectedMap.put(entity.getId(), entity);
                        }
                    }
                    messageRvAdapter.notifyDataSetChanged();
                    right_text.setText(R.string.clear_all_message);
                    title_text.setText(getString(R.string.selected_items, selectedMap.size()));
                } else if (right_text.getText().equals(getString(R.string.clear_all_message))) {
                    selectedMap.clear();
                    messageRvAdapter.notifyDataSetChanged();
                    right_text.setText(R.string.select_all);
                }
                break;
            case R.id.title_bar_left_text:
                    onBackPressed();
                break;
        }
    }

    private void resetLayout() {
        selectedMap.clear();
        messageRvAdapter.notifyCheckable(false);
        title_text.setText(titleString);
        right_icon.setVisibility(View.VISIBLE);
        right_text.setVisibility(View.GONE);
        left_icon.setVisibility(View.VISIBLE);
        left_text.setVisibility(View.GONE);
        deleteLl.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (left_text.getVisibility()==View.VISIBLE) {
            resetLayout();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        super.onDebouncedClick(view, viewId);
        switch (viewId) {
            case R.id.delete_btn: {
                deleteMessage();
            }
            break;
        }
    }

    private void deleteMessage() {
        if (selectedMap.isEmpty())
            return;
        NotifyMessageEntityDao dao = GreenUtils.getNotifyMessageEntityDao();
        dao.deleteByKeyInTx(selectedMap.keySet());
        selectedMap.clear();
        getMessageDataFromDb();
        resetLayout();
    }

    private void setMessageHasBeenRead(String deviceId) {
        if (TextUtils.isEmpty(deviceId))
            return;
        App.getInstance().getDaoSession().getDatabase().execSQL("UPDATE " + NotifyMessageEntityDao.TABLENAME +
                        " SET " + NotifyMessageEntityDao.Properties.IsRead.columnName + "=?" +
                        " WHERE " +
                        NotifyMessageEntityDao.Properties.DeviceId.columnName + "=?" +
                        " AND " +
                        NotifyMessageEntityDao.Properties.IsRead.columnName + "=?"
                ,

                new Object[]{true, deviceId, false}
        );
        GreenUtils.getNotifyMessageEntityDao().detachAll();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onHasNewMessageOfMessageCenter(Event.HasNewMessageOfMessageCenter ev) {
        if (!TextUtils.isEmpty(ev.getDeviceId()) && ev.getDeviceId().equals(mDeviceIdOfShow)) {
            setMessageHasBeenRead(ev.getDeviceId());
            getMessageDataFromDb();
        }
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getDeviceId().equals(mDeviceIdOfShow)) {
            // 当前正在查看的宝贝被解绑，关闭这个页面
            finish();
        }
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }
}
