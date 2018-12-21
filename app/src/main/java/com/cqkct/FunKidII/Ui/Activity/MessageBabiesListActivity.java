package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;

import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.MessageBabiesBean;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.MessageBabiesListAdapter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.NotifyMessageEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.NotifyMessageEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import protocol.Message;

public class MessageBabiesListActivity extends BaseActivity {
    public static final String TAG = MessageBabiesListActivity.class.getSimpleName();
    public static final String MESSAGEBABIESDATA = "MessageBabiesBean";

    private MessageBabiesListAdapter adapter;
    private List<MessageBabiesBean> babiesBeanList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_babies_list_activity);
        setTitleBarTitle(R.string.notification_channel_name_message_center);
        initView();
        getBabiesData();
    }

    private void initView() {
        adapter = new MessageBabiesListAdapter(babiesBeanList);
        SwipeMenuRecyclerView recyclerView = findViewById(R.id.message_babies_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setSwipeMenuCreator(getSwipeMenuCreator(this));
        //右菜单删除
        recyclerView.setSwipeMenuItemClickListener(menuBridge -> {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            int position = menuBridge.getAdapterPosition();
            MessageBabiesBean bean = babiesBeanList.get(position);
            GreenUtils.deleteNotifyMessage(bean.getDeviceId(), mUserId);
            babiesBeanList.remove(position);
            adapter.notifyItemRemoved(position);
        });
        adapter.setOnMessageBabyItemClickListener(position -> {
            Intent intent = new Intent(this, MessageListsActivity.class);
            intent.putExtra(MESSAGEBABIESDATA, babiesBeanList.get(position));
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getBabiesData();
    }

    private void getBabiesData() {
        babiesBeanList.clear();
        String userId = mUserId;
        if (TextUtils.isEmpty(userId)) {
            L.w(TAG, "refreshClipViewPagerData userId is isEmpty");
            return;
        }

        List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .list();

        for (BabyEntity babyEntity : list) {
            // 最新的一条消息
            List<NotifyMessageEntity> entities = GreenUtils.getNotifyMessageEntityDao().queryBuilder()
                    .where(NotifyMessageEntityDao.Properties.UserId.eq(userId))
                    .where(NotifyMessageEntityDao.Properties.DeviceId.eq(babyEntity.getDeviceId()))
                    .orderDesc(NotifyMessageEntityDao.Properties.Id)
                    .limit(1)
                    .list();
            if (entities.isEmpty()) {
                // 没有消息
                continue;
            }

            MessageBabiesBean bean = new MessageBabiesBean();
            bean.setLastMessage(entities.get(0));

            // 未读数
            long unreadCount = GreenUtils.getNotifyMessageEntityDao().queryBuilder()
                    .where(NotifyMessageEntityDao.Properties.UserId.eq(userId))
                    .where(NotifyMessageEntityDao.Properties.DeviceId.eq(babyEntity.getDeviceId()))
                    .where(NotifyMessageEntityDao.Properties.IsRead.eq(false))
                    .count();
            bean.setUnreadCount(unreadCount);
            bean.setBabySex(babyEntity.getSex());
            bean.setBabyName(babyEntity.getName());
            bean.setDeviceId(babyEntity.getDeviceId());
            bean.setBabyAvatar(babyEntity.getBabyAvatar());

            babiesBeanList.add(bean);
        }

        adapter.notifyDataSetChanged();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onHasNewMessageOfMessageCenter(Event.HasNewMessageOfMessageCenter ev) {
        if (!TextUtils.isEmpty(ev.getDeviceId())){
            getBabiesData();
        }
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    public void onCurrentBabyChanged(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        super.onCurrentBabyChanged(oldBabyBean, newBabyBean, isSticky);
        getBabiesData();
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        getBabiesData();
    }

    @Override
    public void onUsrDevAssocModified(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUsrDevAssocModifiedReqMsg reqMsg) {
        getBabiesData();
    }
}
