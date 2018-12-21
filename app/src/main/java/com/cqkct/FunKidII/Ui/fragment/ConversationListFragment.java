package com.cqkct.FunKidII.Ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cqkct.FunKidII.Bean.ConversationBean;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.ChatActivity;
import com.cqkct.FunKidII.Ui.Adapter.ConversationListAdapter;
import com.cqkct.FunKidII.Ui.Adapter.RecyclerViewAdapter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Dao.ChatEntityDao;
import com.cqkct.FunKidII.db.Dao.FamilyChatGroupMemberEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.ChatEntity;
import com.cqkct.FunKidII.db.Entity.FamilyChatGroupMemberEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import protocol.Message;

public class ConversationListFragment extends BaseFragment {
    public static final String TAG = ConversationListFragment.class.getSimpleName();
    private Context mContext;
    private static final int ACTIVITY_REQUEST_CODE_TO_CHAT = 1;

    private ConversationListAdapter mAdapter;
    private List<ConversationBean> mData;
    private XHandler mXHandler;

    @Override
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_conversation_list, container, false);
        mContext = getContext();
        initTitleBar(R.string.notification_channel_name_micro_chat, rootView, true);
        initView(rootView);
        mXHandler = new XHandler(this);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mData == null) {
            queryGroup();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mData == null) {
            mXHandler.refreshConversationList();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_CODE_TO_CHAT) {
            mXHandler.refreshConversationList();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFamilyChatGroupMemberUpdated(Event.FamilyChatGroupMemberUpdated ev) {
        mXHandler.refreshConversationList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShouldUpdateFamilyGroupMember(Event.ShouldUpdateFamilyGroupMember ev) {
        queryGroup();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onConversationListShouldRefresh(ConversationListShouldRefresh ev) {
        mXHandler.refreshConversationList();
    }


    private void initView(View rootView) {
        RecyclerView recyclerView = rootView.findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new ConversationListAdapter(mData);
        mAdapter.setOnItemClickListener(mAdapter.new DebouncedOnItemClickListener() {
            @Override
            public void onDebouncedItemClick(RecyclerView recyclerView, RecyclerViewAdapter adapter, View view, int position) {
                EventBus.getDefault().postSticky(mAdapter.getItem(position));
                startActivityForResult(new Intent(mContext, ChatActivity.class), ACTIVITY_REQUEST_CODE_TO_CHAT);
            }
        });
        recyclerView.setAdapter(mAdapter);
    }

    private void loadDataFromDb() {
        mData = loadData(mContext, mUserId);
        mAdapter.setData(mData);
    }

    private static List<ConversationBean> loadData(Context context, String userId) {
        if (TextUtils.isEmpty(userId)) {
            L.w(TAG, "loadData: userId is empty");
            return null;
        }

        List<BabyEntity> list = GreenUtils.getBabyEntityDao().queryBuilder()
                .where(BabyEntityDao.Properties.UserId.eq(userId))
                .build().list();

        List<ConversationBean> data = new ArrayList<>();

        ConversationBean conversationBean;
        for (BabyEntity babyEntity : list) {
            // 设备单聊
            conversationBean = getDeviceConversation(context, babyEntity);
            if (conversationBean != null) {
                data.add(conversationBean);
            }

            // 设备家庭群聊
            conversationBean = getFamilyGroupConversation(context, babyEntity);
            if (conversationBean != null) {
                data.add(conversationBean);
            }
        }

        return data;
    }

    private static ConversationBean getDeviceConversation(Context context, BabyEntity babyEntity) {
        Message.UsrDevAssoc.Permission permission = Message.UsrDevAssoc.Permission.forNumber(babyEntity.getPermission());
        if (permission == null)
            permission = Message.UsrDevAssoc.Permission.MINI;
        ConversationBean bean;
        if (!TextUtils.isEmpty(babyEntity.getUserAvatar())) {
            bean = new ConversationBean(babyEntity.getDeviceId(), babyEntity.getName(), babyEntity.getBabyAvatar(), babyEntity.getUserAvatar(), babyEntity.getUserId(), babyEntity.getRelation(), permission);
        } else {
            bean = new ConversationBean(babyEntity.getDeviceId(), babyEntity.getName(), babyEntity.getBabyAvatar(), babyEntity.getUserId(), babyEntity.getRelation(), permission);
        }
        ChatEntityDao dao = GreenUtils.getChatEntityDao();
        QueryBuilder<ChatEntity> queryBuilder = dao.queryBuilder();
        WhereCondition notGroup = queryBuilder.or(ChatEntityDao.Properties.GroupId.isNull(), ChatEntityDao.Properties.GroupId.eq(""));
        long unreadCount = queryBuilder.where(ChatEntityDao.Properties.DeviceId.eq(babyEntity.getDeviceId()), notGroup, ChatEntityDao.Properties.IsSeen.eq(false)).count();
        bean.setUnreadCount(unreadCount);

        queryBuilder = dao.queryBuilder();
        notGroup = queryBuilder.or(ChatEntityDao.Properties.GroupId.isNull(), ChatEntityDao.Properties.GroupId.eq(""));
        List<ChatEntity> last = queryBuilder.where(ChatEntityDao.Properties.DeviceId.eq(babyEntity.getDeviceId()), notGroup).orderDesc(ChatEntityDao.Properties.Timestamp).limit(1).list();
        if (!last.isEmpty()) {
            bean.setLastMessage(last.get(0));
        }

        return bean;
    }

    private static ConversationBean getFamilyGroupConversation(Context context, BabyEntity babyEntity) {
        if (TextUtils.isEmpty(babyEntity.getFamilyGroup())) {
            return null;
        }

        List<FamilyChatGroupMemberEntity> memberInDb = GreenUtils.getFamilyChatGroupMemberEntityDao()
                .queryBuilder().where(FamilyChatGroupMemberEntityDao.Properties.GroupId.eq(babyEntity.getFamilyGroup()))
                .list();

        List<ConversationBean.Member> members = new ArrayList<>();

        for (FamilyChatGroupMemberEntity memberEntity : memberInDb) {
            if (TextUtils.isEmpty(memberEntity.getUserId())) {
                continue;
            }
            if (!TextUtils.isEmpty(memberEntity.getUserId())) {
                Message.UsrDevAssoc.Permission permission = Message.UsrDevAssoc.Permission.forNumber(memberEntity.getPermission());
                if (permission == null)
                    permission = Message.UsrDevAssoc.Permission.MINI;
                if (TextUtils.isEmpty(memberEntity.getUserAvatar())) {
                    members.add(new ConversationBean.Member(memberEntity.getUserId(), memberEntity.getRelation(), permission));
                } else {
                    members.add(new ConversationBean.Member(memberEntity.getUserId(), memberEntity.getRelation(), memberEntity.getUserAvatar(), permission));
                }
            }
        }

        if (members.isEmpty()) {
            return null;
        }
        ConversationBean bean;
        if (!TextUtils.isEmpty(babyEntity.getUserAvatar())) {
            bean = new ConversationBean(babyEntity.getDeviceId(), babyEntity.getName(), babyEntity.getBabyAvatar(), babyEntity.getUserAvatar(),
                    ConversationBean.Type.FAMILY_GROUP, babyEntity.getFamilyGroup(), members);
        } else {
            bean = new ConversationBean(babyEntity.getDeviceId(), babyEntity.getName(), babyEntity.getBabyAvatar(),
                    ConversationBean.Type.FAMILY_GROUP, babyEntity.getFamilyGroup(), members);
        }

        ChatEntityDao dao = GreenUtils.getChatEntityDao();
        bean.unreadCount = dao.queryBuilder().where(ChatEntityDao.Properties.GroupId.eq(babyEntity.getFamilyGroup()),
                ChatEntityDao.Properties.IsSeen.eq(false)).count();

        List<ChatEntity> last = dao.queryBuilder().where(ChatEntityDao.Properties.GroupId.eq(babyEntity.getFamilyGroup()))
                .orderDesc(ChatEntityDao.Properties.Timestamp).limit(1).list();
        if (!last.isEmpty()) {
            bean.setLastMessage(last.get(0));
        }

        return bean;
    }

    private void queryGroup() {
        Message.GetFamilyGroupOfChatReqMsg reqMsg = Message.GetFamilyGroupOfChatReqMsg.newBuilder().setShouldContainMember(true).build();
        exec(reqMsg, new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                try {
                    Message.GetFamilyGroupOfChatRspMsg rspMsg = response.getProtoBufMsg();
                    if (rspMsg.getErrCode() == Message.ErrorCode.SUCCESS) {
                        for (Message.GetFamilyGroupOfChatRspMsg.Group g : rspMsg.getGroupList()) {
                            GreenUtils.saveFamilyGroup(g.getDeviceId(), g.getGroup());
                            mXHandler.refreshConversationList();
                        }
                    }
                } catch (Exception e) {
                    L.w(TAG, "queryGroup exec onResponse", e);
                }
                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.w(TAG, "queryGroup exec onException", cause);
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {

            }
        });
    }


    public static class ConversationListShouldRefresh {
        ConversationListShouldRefresh() {
        }
    }

    public static class ShouldQueryFamilyGroupMember {
        ShouldQueryFamilyGroupMember() {
        }
    }

    private static class XHandler extends Handler {
        static final int REFRESH_CONVERSATION_LIST = 0;

        private WeakReference<ConversationListFragment> mF;

        XHandler(ConversationListFragment a) {
            mF = new WeakReference<>(a);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            ConversationListFragment f = mF.get();
            if (f == null)
                return;

            switch (msg.what) {
                case REFRESH_CONVERSATION_LIST:
                    f.loadDataFromDb();
                    break;
            }
        }

        void refreshConversationList() {
            removeMessages(REFRESH_CONVERSATION_LIST);
            sendEmptyMessageDelayed(REFRESH_CONVERSATION_LIST, 100);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDeviceOnline(Event.DeviceOnline ev) {
        EventBus.getDefault().postSticky(new ConversationListShouldRefresh());
    }


    @Override
    public void onNewMicroChatMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatMessageReqMsg reqMsg) {
        EventBus.getDefault().postSticky(new ConversationListShouldRefresh());
    }

    @Override
    public void onNewMicroChatGroupMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyGroupChatMessageReqMsg reqMsg) {
        EventBus.getDefault().postSticky(new ConversationListShouldRefresh());
    }


    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        EventBus.getDefault().postSticky(new ConversationListShouldRefresh());
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        if (reqMsg.getUsrDevAssoc().getUserId().equals(mUserId)) {
            queryGroup();
        }
    }

    @Override
    public void onChatGroupMemberChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatGroupMemberChangedReqMsg reqMsg) {
        mXHandler.refreshConversationList();
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@android.support.annotation.Nullable BabyEntity oldBabyBean, @android.support.annotation.Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

}

