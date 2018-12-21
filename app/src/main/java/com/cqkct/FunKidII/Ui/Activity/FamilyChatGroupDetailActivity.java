package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.Bean.ConversationBean;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.FamilyChatGroupDetailMemberAdapter;
import com.cqkct.FunKidII.Ui.fragment.ConfirmDialogFragment;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.db.Dao.ChatEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.ChatEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.DeleteQuery;

import java.util.List;

import protocol.Message;


public class FamilyChatGroupDetailActivity extends BaseActivity {
    private static final String TAG = FamilyChatGroupDetailActivity.class.getSimpleName();


    private ConversationBean mConversation;

    private FamilyChatGroupDetailMemberAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_chat_group_detail);

        mConversation = EventBus.getDefault().getStickyEvent(ConversationBean.class);
        if (mConversation == null) {
            L.w(TAG, "mConversation is null, finish()");
            finish();
            return;
        }
        EventBus.getDefault().removeStickyEvent(mConversation);
        if (!mConversation.isGroup()) {
            L.w(TAG, "mConversation is not group, finish()");
            finish();
            return;
        }

        if (TextUtils.isEmpty(mUserId)) {
            L.w(TAG, "mUserId is empty, finish()");
            finish();
            return;
        }

        initView();
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.clean_chat_history:
                cleanChatHistory();
                break;
        }
    }

    /**
     * 初始化控件
     */
    private void initView() {
        setTitle();

        RecyclerView mRecyclerView = findViewById(R.id.members);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 5);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new FamilyChatGroupDetailMemberAdapter(mConversation);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void setTitle() {
        String babyName = mConversation.mBabyName;
        if (TextUtils.isEmpty(babyName)) {
            babyName = getString(R.string.baby);
        }
        if (mConversation.isGroup()) {
            setTitleBarTitle(getString(R.string.family_group_of_baby, babyName));
        } else {
            setTitleBarTitle(babyName);
        }
    }

    private void cleanChatHistory() {
        ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment()
                .setMessage(getString(R.string.ask_clean_chat_history))
                .setPositiveButton(getText(R.string.ok), (dialog, which) -> doCleanChatHistory())
                .setNegativeButton(getText(R.string.cancel), null);
        dialogFragment.show(getSupportFragmentManager(), "ConfirmDeleteDialog");
    }

    private void doCleanChatHistory() {
        ChatEntityDao dao = GreenUtils.getChatEntityDao();
        DeleteQuery<ChatEntity> deleteQuery = dao.queryBuilder()
                .where(ChatEntityDao.Properties.GroupId.eq(mConversation.mGroupId))
                .buildDelete();
        deleteQuery.executeDeleteWithoutDetachingEntities();
        dao.detachAll();

        setResult(RESULT_OK, new Intent());
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShouldUpdateFamilyGroupMember(Event.ShouldUpdateFamilyGroupMember ev) {
        if (ev == null || ev.usrDevAssoc == null || mConversation == null)
            return;
        Message.UsrDevAssoc uda = ev.usrDevAssoc;
        if (ev.usrDevAssoc.getUserId().equals(mUserId) && ev.usrDevAssoc.getDeviceId().equals(mConversation.mDeviceId)) {
            if (TextUtils.isEmpty(uda.getAvatar())) {
                mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getPermission());
            } else {
                mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getAvatar(), uda.getPermission());
            }
        }
    }


    @Override
    public void onChatGroupMemberChanged(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatGroupMemberChangedReqMsg reqMsg) {
        // 不是当前设备，不做任何动作
        if (reqMsg.getGroupId().equals(mConversation.mGroupId)) {
            List<Message.NotifyChatGroupMemberChangedReqMsg.Detail> list = reqMsg.getDetailList();
            if (list.isEmpty())
                return;
            for (Message.NotifyChatGroupMemberChangedReqMsg.Detail detail : list) {
                Message.NotifyChatGroupMemberChangedReqMsg.Detail.Action action = detail.getAction();
                Message.ChatGroup.Member member = detail.getMember();
                switch (action) {
                    case DEL:
                        if (mConversation != null) {
                            if (member.getInfoCase() == Message.ChatGroup.Member.InfoCase.USER) {
                                Message.UsrDevAssoc uda = member.getUser().getUsrDevAssoc();
                                if (uda.getUserId().equals(mUserId)) {
                                    // 解绑的是当前用户的当前设备，关闭页面。
                                    finish();
                                    return;
                                }
                                // 别人的相同设备号被解绑
                                if (!mConversation.isGroup()) {
                                    // 我们正处于单聊，不做任何动作
                                    return;
                                }
                                mConversation.removeMember(uda.getUserId());
                                mAdapter.setData(mConversation);
                            } else if (member.getInfoCase() == Message.ChatGroup.Member.InfoCase.DEVICE) {
                                // TODO: 多个设备 在同一个聊天组
//                                别人的相同设备号被解绑
//                                if (!mConversation.isGroup()) {
//                                    // 我们正处于单聊，不做任何动作
//                                    return;
//                                }
//
//                                mConversation.removeMember(uda.getUserId());
//                                mAdapter.setData(mConversation);
                            }
                        }
                        break;
                    case ADD:
                        if (mConversation != null) {
                            Message.UsrDevAssoc uda = member.getUser().getUsrDevAssoc();
                            if (uda.getDeviceId().equals(mConversation.mDeviceId)) {
                                if (TextUtils.isEmpty(uda.getAvatar())) {
                                    mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getPermission());
                                } else {
                                    mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getAvatar(), uda.getPermission());
                                }
                                mAdapter.setData(mConversation);
                            }
                        }
                        break;
                    case MODIFY:
                        if (mConversation != null) {
                            Message.UsrDevAssoc uda = member.getUser().getUsrDevAssoc();
                            if (uda.getDeviceId().equals(mConversation.mDeviceId)) {
                                mConversation.removeMember(uda.getUserId());
                                if (TextUtils.isEmpty(uda.getAvatar())) {
                                    mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getPermission());
                                } else {
                                    mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getAvatar(), uda.getPermission());
                                }
                                mAdapter.setData(mConversation);
                            }
                        }
                        break;
                    case NONE:
                        break;
                }

            }
        }
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }
}
