package com.cqkct.FunKidII.Ui.Activity;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.cqkct.FunKidII.App.App;
import com.cqkct.FunKidII.Bean.ConversationBean;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.Keyboard.ChatKeyboard;
import com.cqkct.FunKidII.Ui.Adapter.ChatListAdapter;
import com.cqkct.FunKidII.Ui.Adapter.RecyclerViewAdapter;
import com.cqkct.FunKidII.Ui.view.PopupList;
import com.cqkct.FunKidII.Ui.view.RecordButton;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.MediaManager;
import com.cqkct.FunKidII.db.Dao.ChatEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.db.Entity.ChatEntity;
import com.cqkct.FunKidII.service.OkHttpRequestManager;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.OnResponseSetter;
import com.cqkct.FunKidII.service.tlc.TlcService;
import com.google.protobuf.GeneratedMessageV3;
import com.gyf.barlibrary.ImmersionBar;
import com.gyf.barlibrary.OnKeyboardListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.LazyList;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import protocol.Message;

public class ChatActivity extends BaseActivity implements OnKeyboardListener {
    private static final String TAG = ChatActivity.class.getSimpleName();

    private static final int ACTIVITY_REQUEST_CODE_GROUP_DETAIL = 1;

    private ConversationBean mConversation;


    private RecyclerView mRecyclerView;
    private ChatListAdapter mAdapter;
    private LazyList<ChatEntity> mData;
    private ChatKeyboard chatKeyboard;

    private Boolean mDeviceSupportedTextMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mConversation = EventBus.getDefault().getStickyEvent(ConversationBean.class);
        if (mConversation == null) {
            L.w(TAG, "mConversation is null, finish()");
            finish();
            return;
        }
        EventBus.getDefault().removeStickyEvent(mConversation);

        if (TextUtils.isEmpty(mUserId)) {
            L.w(TAG, "mUserId is empty, finish()");
            finish();
            return;
        }

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mData == null) {
            loadData();
        }

        if (mDeviceSupportedTextMessage == null) {
            if (mConversation.isGroup()) {
                mDeviceSupportedTextMessage = true;
            } else {
                mDeviceSupportedTextMessage = deviceSupportedTextMessage(mConversation.mDeviceId);
                chatKeyboard.setTextEditorEnable(mDeviceSupportedTextMessage);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RecordButton.PERMISSIONS_REQUEST_RECORD_AUDIO: // 该请求码在按下RecordButton后触发，详情请查看RecordButton里的代码
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_record_audio_permission);
                }
                break;
            case RecordButton.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: // 该请求码在按下RecordButton后触发，详情请查看RecordButton里的代码
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    toast(R.string.no_external_storage_permission);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RecordButton.ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_RECORD_AUDIO_PERMISSION:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // 用户已经在设置页面授权
                }
                break;
            case RecordButton.ACTIVITY_REQUEST_CODE_GOTO_SETTING_TO_SET_WRITE_EXTERNAL_STORAGE:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 用户已经在设置页面授权
                }
                break;

            case ACTIVITY_REQUEST_CODE_GROUP_DETAIL:
                if (resultCode == RESULT_OK) {
                        mData = null;
                        mAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        MediaManager.release();
        ImmersionBar.with(this).destroy(); //不调用该方法，如果界面bar发生改变，在不关闭app的情况下，退出此界面再进入将记忆最后一次bar改变的状态
        super.onDestroy();
    }

    @Override
    protected void initImmersionBar() {
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.statusBarDarkFont(true).keyboardEnable(true).setOnKeyboardListener(this);
        mImmersionBar.init();
    }

    @Override
    public void onTitleBarClick(View v) {
        super.onDebouncedClick(v);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.title_bar_left_icon:
                finish();
                break;
            case R.id.title_bar_right_icon: {
                EventBus.getDefault().postSticky(mConversation);
                Intent intent = new Intent(this, FamilyChatGroupDetailActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_GROUP_DETAIL);
            }
            break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!chatKeyboard.isInterceptBackPress()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onKeyboardChange(boolean isPopup, int keyboardHeight) {
        L.e(TAG, "onKeyboardChange(" + isPopup + ", " + keyboardHeight + ")");
        if (chatKeyboard != null) {
            chatKeyboard.onKeyboardChange(isPopup, keyboardHeight);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSendTextChatMessage(Event.SendTextChatMessage ev) {
        if (TextUtils.isEmpty(mUserId) || mConversation == null) {
            L.d(TAG, "onSendTextChatMessage: mUserId is empty or mConversation is null");
            return;
        }
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(mUserId);
        chatEntity.setDeviceId(mConversation.mDeviceId);
        chatEntity.setGroupId(mConversation.mGroupId);
        chatEntity.setSenderId(mUserId);
        chatEntity.setSenderType(Message.TermAddr.Type.USER_VALUE);
        chatEntity.setTimestamp(ev.millis);
        chatEntity.setIsSendDir(true);
        chatEntity.setSendStatus(ChatEntity.SEND_STAT_QUEUE);
        chatEntity.setIsSeen(true);
        chatEntity.setMessageType(ChatEntity.TYPE_TEXT);
        chatEntity.setText(ev.text);
        chatEntity.setId(GreenUtils.getChatEntityDao().insert(chatEntity));
        loadData();
        scrollToBottom();
        sendChatEntity(chatEntity, null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSendVoiceChatMessage(Event.SendVoiceChatMessage ev) {
        if (TextUtils.isEmpty(mUserId) || mConversation == null) {
            L.d(TAG, "onSendVoiceChatMessage: mUserId is empty or mConversation is null");
            return;
        }
        if (!ev.file.exists()) {
            L.w(TAG, "onSendVoiceChatMessage: file (" + ev.file.getAbsolutePath() + ") not exists");
            return;
        }
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(mUserId);
        chatEntity.setDeviceId(mConversation.mDeviceId);
        chatEntity.setGroupId(mConversation.mGroupId);
        chatEntity.setSenderId(mUserId);
        chatEntity.setSenderType(Message.TermAddr.Type.USER_VALUE);
        chatEntity.setTimestamp(ev.millis);
        chatEntity.setIsSendDir(true);
        chatEntity.setSendStatus(ChatEntity.SEND_STAT_QUEUE);
        chatEntity.setIsSeen(true);
        chatEntity.setMessageType(ChatEntity.TYPE_VOICE);
        chatEntity.setFilename(ev.file.getName());
        chatEntity.setFileSize(ev.file.length());
        chatEntity.setVoiceDuration(ev.duration);
        chatEntity.setVoiceIsPlayed(true);
        chatEntity.setId(GreenUtils.getChatEntityDao().insert(chatEntity));
        loadData();
        scrollToBottom();
        sendChatEntity(chatEntity, ev.file);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSendEmoticonChatMessage(Event.SendEmoticonChatMessage ev) {
        if (TextUtils.isEmpty(mUserId) || mConversation == null) {
            L.d(TAG, "onSendTextChatMessage: mUserId is empty or mConversation is null");
            return;
        }
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(mUserId);
        chatEntity.setDeviceId(mConversation.mDeviceId);
        chatEntity.setGroupId(mConversation.mGroupId);
        chatEntity.setSenderId(mUserId);
        chatEntity.setSenderType(Message.TermAddr.Type.USER_VALUE);
        chatEntity.setTimestamp(ev.millis);
        chatEntity.setIsSendDir(true);
        chatEntity.setSendStatus(ChatEntity.SEND_STAT_QUEUE);
        chatEntity.setIsSeen(true);
        chatEntity.setMessageType(ChatEntity.TYPE_EMOTICON);
        chatEntity.setEmoticon(ev.name);
        chatEntity.setId(GreenUtils.getChatEntityDao().insert(chatEntity));
        loadData();
        scrollToBottom();
        sendChatEntity(chatEntity, null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResendChatMessage(Event.ResendChatMessage ev) {
        ChatEntity chatEntity = ev.getChatEntity();
        if (chatEntity == null)
            return;
        File voiceFile = null;
        if (chatEntity.getMessageType() == ChatEntity.TYPE_VOICE) {
            if (TextUtils.isEmpty(chatEntity.getFilename()))
                return;
            voiceFile = new File(FileUtils.getExternalStorageVoiceChatCacheDirFile(), chatEntity.getFilename());
            if (!voiceFile.exists()) {
                L.w(TAG, "onSendVoiceChatMessage: file (" + voiceFile.getAbsolutePath() + ") not exists");
                return;
            }
        }
        sendChatEntity(chatEntity, voiceFile);
    }

    private void sendChatEntity(ChatEntity chatEntity, File voiceFile) {
        if (chatEntity.getMessageType() == ChatEntity.TYPE_VOICE && chatEntity.getFileUploadStatus() != ChatEntity.FILE_UPLOAD_SUCCESS) {
            if (voiceFile == null) {
                L.w(TAG, "sendChatEntity: type is voice, but voiceFile is null");
                ChatActivity.updateChatMessageSendStatus(this, chatEntity, ChatEntity.SEND_STAT_FAILED);
                return;
            }
            if (!voiceFile.exists()) {
                L.w(TAG, "sendChatEntity: type is voice, but voiceFile not exists");
                ChatActivity.updateChatMessageSendStatus(this, chatEntity, ChatEntity.SEND_STAT_FAILED);
                return;
            }
            if (chatEntity.getFileUploadStatus() == ChatEntity.FILE_UPLOAD_PROGRESS) {
                L.d(TAG, "sendChatEntity: voice file is FILE_UPLOAD_PROGRESS");
                return;
            }
            GreenUtils.updateVoiceChatMessageFileUploadStatus(chatEntity, ChatEntity.FILE_UPLOAD_PROGRESS);
            OkHttpRequestManager.getInstance(this).uploadChatVoiceFile(voiceFile, chatEntity.getUserId(), new VoiceFileUploadCallBack(this, chatEntity));
            return;
        }
        switch (chatEntity.getMessageType()) {
            case ChatEntity.TYPE_TEXT:
            case ChatEntity.TYPE_EMOTICON:
            case ChatEntity.TYPE_VOICE:
                ChatActivity.sendChatEntityAsProtoBuf(this, chatEntity);
                break;
            default:
                L.w(TAG, "sendChatEntity: unsupported message type: " + chatEntity.getMessageType());
                ChatActivity.updateChatMessageSendStatus(this, chatEntity, ChatEntity.SEND_STAT_FAILED);
                break;
        }
    }

    private static class VoiceFileUploadCallBack implements OkHttpRequestManager.ReqProgressCallBack<String> {
        private WeakReference<ChatActivity> mA;
        private final @NonNull ChatEntity chatEntity;

        VoiceFileUploadCallBack(ChatActivity a, @NonNull ChatEntity chatEntity) {
            mA = new WeakReference<>(a);
            this.chatEntity = chatEntity;
        }

        @Override
        public void onReqSuccess(String result) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.getBoolean("success")) {
                    GreenUtils.updateVoiceChatMessageFileUploadStatus(chatEntity, ChatEntity.FILE_UPLOAD_SUCCESS);
                    ChatActivity.sendChatEntityAsProtoBuf(mA.get(), chatEntity);
                    return;
                } else {
                    L.w(TAG, "VoiceFileUploadCallBack onReqSuccess process failure: " + result);
                }
            } catch (JSONException e) {
                L.e(TAG, "VoiceFileUploadCallBack onReqSuccess process failure", e);
            }

            GreenUtils.updateVoiceChatMessageFileUploadStatus(chatEntity, ChatEntity.FILE_UPLOAD_NONE);
            ChatActivity.updateChatMessageSendStatus(mA.get(), chatEntity, ChatEntity.SEND_STAT_FAILED);
        }

        @Override
        public void onReqFailed(String errorMsg) {
            GreenUtils.updateVoiceChatMessageFileUploadStatus(chatEntity, ChatEntity.FILE_UPLOAD_NONE);
            ChatActivity.updateChatMessageSendStatus(mA.get(), chatEntity, ChatEntity.SEND_STAT_FAILED);
        }

        @Override
        public void onProgress(long total, long current) {
        }
    }

    private static void sendChatEntityAsProtoBuf(@Nullable ChatActivity activity, ChatEntity chatEntity) {
        GeneratedMessageV3 reqMsg = chatEntityToProtoBuf(chatEntity);
        if (reqMsg == null) {
            GreenUtils.updateChatMessageSendStatus(chatEntity, ChatEntity.SEND_STAT_FAILED);
            return;
        }

        App app = App.getInstance();
        if (app == null) {
            GreenUtils.updateChatMessageSendStatus(chatEntity, ChatEntity.SEND_STAT_FAILED);
            return;
        }
        TlcService tlcService = app.getTlcService();
        if (tlcService == null) {
            GreenUtils.updateChatMessageSendStatus(chatEntity, ChatEntity.SEND_STAT_FAILED);
            return;
        }
        tlcService.exec(reqMsg, new TlcService.OnExecListener() {
            @Override
            public boolean onResponse(@NonNull Pkt request, @NonNull Pkt response) {
                GeneratedMessageV3 rspMsg = response.getProtoBufMsg();
                if (rspMsg == null) {
                    L.w(TAG, "tlcService.exec for " + reqMsg.getClass().getSimpleName() + " onResponse: response.getProtoBufMsg() is null");
                    ChatActivity.updateChatMessageSendStatus(activity, chatEntity, ChatEntity.SEND_STAT_FAILED);
                    return false;
                }
                protocol.Message.ErrorCode errorCode;
                long serverMessageTime;
                if (rspMsg instanceof protocol.Message.SendGroupChatMessageRspMsg) {
                    protocol.Message.SendGroupChatMessageRspMsg msg = (protocol.Message.SendGroupChatMessageRspMsg) rspMsg;
                    errorCode = msg.getErrCode();
                    serverMessageTime = msg.getTimestamp();
                } else if (rspMsg instanceof protocol.Message.SendChatMessageRspMsg) {
                    protocol.Message.SendChatMessageRspMsg msg = (protocol.Message.SendChatMessageRspMsg) rspMsg;
                    errorCode = msg.getErrCode();
                    serverMessageTime = msg.getTimestamp();
                } else {
                    L.e(TAG, "tlcService.exec for " + reqMsg.getClass().getSimpleName() + " onResponse: rspMsg invalid");
                    ChatActivity.updateChatMessageSendStatus(activity, chatEntity, ChatEntity.SEND_STAT_FAILED);
                    return false;
                }

                switch (errorCode) {
                    case SUCCESS:
                        ChatActivity.updateChatMessageSendSuccess(activity, chatEntity, serverMessageTime / 1000000L);
                        break;
                    default:
                        L.w(TAG, "tlcService.exec for " + reqMsg.getClass().getSimpleName() + " onResponse: " + errorCode);
                        ChatActivity.updateChatMessageSendStatus(activity, chatEntity, ChatEntity.SEND_STAT_FAILED);
                        break;
                }

                return false;
            }

            @Override
            public void onException(@NonNull Pkt request, @NonNull Throwable cause) {
                L.w(TAG, "tlcService.exec for " + reqMsg.getClass().getSimpleName() + " onException", cause);
                ChatActivity.updateChatMessageSendStatus(activity, chatEntity, ChatEntity.SEND_STAT_FAILED);
            }

            @Override
            public void onThirdStage(@NonNull Pkt firstStageRequest, @NonNull Pkt firstStageResponse, @NonNull Pkt thirdStageRequest, @NonNull OnResponseSetter responseSetter) {
            }
        });
    }

    private static GeneratedMessageV3 chatEntityToProtoBuf(ChatEntity chatEntity) {
        protocol.Message.ChatMessage.Builder chatMessageBuilder = protocol.Message.ChatMessage.newBuilder();
        switch (chatEntity.getMessageType()) {
            case ChatEntity.TYPE_TEXT:
                chatMessageBuilder.setText(chatEntity.getText());
                break;
            case ChatEntity.TYPE_EMOTICON:
                chatMessageBuilder.setEmoticon(chatEntity.getEmoticon());
                break;
            case ChatEntity.TYPE_VOICE:
                chatMessageBuilder.setVoice(protocol.Message.ChatMessage.Voice.newBuilder()
                        .setFileName(chatEntity.getFilename())
                        .setFileSize(chatEntity.getFileSize())
                        .setDuration(chatEntity.getVoiceDuration()));
                break;
            default:
                return null;
        }
        if (chatEntity.isGroup()) {
            return protocol.Message.SendGroupChatMessageReqMsg.newBuilder()
                    .setGroupId(chatEntity.getGroupId())
                    .setMsg(chatMessageBuilder)
                    .build();
        } else {
            return protocol.Message.SendChatMessageReqMsg.newBuilder()
                    .setDst(protocol.Message.TermAddr.newBuilder()
                            .setType(protocol.Message.TermAddr.Type.DEVICE)
                            .setAddr(chatEntity.getDeviceId()))
                    .setMsg(chatMessageBuilder)
                    .build();
        }
    }

    private static void updateChatMessageSendStatus(@Nullable ChatActivity activity, @NonNull ChatEntity chatEntity, @ChatEntity.SendStatus int status) {
        if (GreenUtils.updateChatMessageSendStatus(chatEntity, status)) {
            if (activity != null) {
                activity.mRecyclerView.post(() -> activity.mAdapter.notifyDataSetChanged());
            }
        }
    }

    private static void updateChatMessageSendSuccess(@Nullable ChatActivity activity, @NonNull ChatEntity chatEntity, long serverTimestampMills) {
        if (GreenUtils.updateChatMessageSendSuccess(chatEntity, serverTimestampMills)) {
            if (activity != null) {
                activity.mRecyclerView.post(() -> activity.mAdapter.notifyDataSetChanged());
            }
        }
    }

    /**
     * 初始化控件
     */
    private void initView() {
        setTitle();
        if (mConversation.isGroup()) {
            findViewById(R.id.title_bar_right_icon).setVisibility(View.VISIBLE);
            ((ImageView) findViewById(R.id.title_bar_right_icon)).setImageResource(R.drawable.group_button);
        }

        mRecyclerView = findViewById(R.id.list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        layoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setOnTouchListener((v, event) -> {
            hideKeyBoardAndEmoticonBoard();
            return false;
        });

        mAdapter = new ChatListAdapter(mUserId, mData, mConversation);
        mAdapter.setOnItemClickListener((recyclerView, adapter, view, position) -> hideKeyBoardAndEmoticonBoard());
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setBubbleLongClickListener(new RecyclerViewAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, RecyclerViewAdapter adapter, View view, int position) {
                ChatEntity data = (ChatEntity) adapter.getItem(position);
                List<String> popupMenuItemList = new ArrayList<>();
                if (data.getMessageType() == ChatEntity.TYPE_TEXT) {
                    popupMenuItemList.add(getString(R.string.copy));
                }
                popupMenuItemList.add(getString(R.string.delete));
                PopupList popupList = new PopupList(view.getContext());
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                popupList.showPopupListWindow(view, position, location[0] + view.getWidth() / 2, location[1] + view.getHeight() / 4, popupMenuItemList, new PopupList.PopupListListener() {
                    @Override
                    public boolean showPopupList(View adapterView, View contextView, int contextPosition) {
                        return true;
                    }

                    @Override
                    public void onPopupListClick(View contextView, int contextPosition, int position) {
                        if (popupMenuItemList.size() > 1) {
                            // 菜单: 拷贝、删除
                            if (position == 0) {
                                // 拷贝
                                ClipboardManager cmb = (ClipboardManager) contextView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                if (cmb != null) {
                                    cmb.setPrimaryClip(ClipData.newPlainText(null, data.getText()));
                                }
                            } else {
                                // 删除
                                GreenUtils.getChatEntityDao().delete(data);
                                loadData();
                            }
                        } else {
                            // 菜单: 删除
                            GreenUtils.getChatEntityDao().delete(data);
                            loadData();
                        }
                    }
                });
                return false;
            }
        });

        initKeyboard();
    }

    private void hideKeyBoardAndEmoticonBoard() {
        chatKeyboard.hideEmoticonLayout(false);
        chatKeyboard.hideKeyboard();
    }

    private void setTitle() {
        String babyName = mConversation.mBabyName;
        if (TextUtils.isEmpty(babyName)) {
            babyName = getString(R.string.baby);
        }
        if (mConversation.isGroup()) {
            setTitleBarTitle(getString(R.string.family_group_chat_of_baby, babyName));
        } else {
            setTitleBarTitle(babyName);
        }
    }

    private void loadData() {
        if (mConversation.isGroup()) {
            mData = GreenUtils.getChatEntityDao().queryBuilder()
                    .where(ChatEntityDao.Properties.UserId.eq(mUserId), ChatEntityDao.Properties.GroupId.eq(mConversation.mGroupId))
                    .orderAsc(ChatEntityDao.Properties.Timestamp).listLazy();
        } else {
            QueryBuilder<ChatEntity> queryBuilder = GreenUtils.getChatEntityDao().queryBuilder();
            WhereCondition notGroup = queryBuilder.or(ChatEntityDao.Properties.GroupId.isNull(), ChatEntityDao.Properties.GroupId.eq(""));
            mData = queryBuilder
                    .where(ChatEntityDao.Properties.UserId.eq(mUserId), notGroup, ChatEntityDao.Properties.DeviceId.eq(mConversation.mDeviceId))
                    .orderAsc(ChatEntityDao.Properties.Timestamp).listLazy();
        }
        mAdapter.setLazyList(mData);

        markMessageIsSeen();
    }

    private void markMessageIsSeen() {
        if (mConversation.isGroup()) {
            App.getInstance().getDaoSession().getDatabase().execSQL("UPDATE " + ChatEntityDao.TABLENAME +
                            " SET " + ChatEntityDao.Properties.IsSeen.columnName + "=? WHERE " +
                            ChatEntityDao.Properties.GroupId.columnName + "=? AND " +
                            ChatEntityDao.Properties.IsSeen.columnName + "=?",
                    new Object[]{true, mConversation.mGroupId, false});
        } else {
            App.getInstance().getDaoSession().getDatabase().execSQL("UPDATE " + ChatEntityDao.TABLENAME +
                            " SET " + ChatEntityDao.Properties.IsSeen.columnName + "=? WHERE " +
                            ChatEntityDao.Properties.DeviceId.columnName + "=? AND " +
                            "(" + ChatEntityDao.Properties.GroupId.columnName + " IS NULL OR " + ChatEntityDao.Properties.GroupId.columnName + "='') AND " +
                            ChatEntityDao.Properties.IsSeen.columnName + "=?",
                    new Object[]{true, mConversation.mDeviceId, false});
        }
    }

    /**
     * 初始化表情面板
     */
    public void initKeyboard() {
        chatKeyboard = findViewById(R.id.chat_keyboard);

        // 为录音 Dialog 准备高度以及位置
        chatKeyboard.setRecordIndicatorDialogDepView(mRecyclerView);
    }

    private boolean deviceSupportedTextMessage(String deviceId) {
        DeviceInfo deviceInfo = DeviceInfo.getDeviceInfo(deviceId);
        if (deviceInfo != null) {
            return  (deviceInfo.getDeviceEntity().getFuncModuleInfo().getFuncModule() & Message.FuncModule.FM_MICRO_CHAT_TEXT_VALUE) != 0;
        }
        return false;
    }

    private void scrollToBottom() {
        mRecyclerView.postDelayed(() -> mRecyclerView.smoothScrollToPosition(mRecyclerView.getAdapter().getItemCount()), 50);
    }

    private void tryScrollToBottom() {
        if (!mRecyclerView.canScrollVertically(1)) {
            scrollToBottom();
        }
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
    public void onNewMicroChatMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyChatMessageReqMsg reqMsg) {
        if (mConversation != null && !mConversation.isGroup() &&
                reqMsg.getSrc().getType() == protocol.Message.TermAddr.Type.DEVICE && reqMsg.getSrc().getAddr().equals(mConversation.mDeviceId)) {
            loadData();
            tryScrollToBottom();
        }
    }

    @Override
    public void onNewMicroChatGroupMessage(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyGroupChatMessageReqMsg reqMsg) {
        if (mConversation != null && mConversation.isGroup() && reqMsg.getGroupId().equals(mConversation.mGroupId)) {
            loadData();
            tryScrollToBottom();
        }
    }

    @Override
    public void onDeviceUnbind(TlcService tlcService, @Nullable Pkt reqPkt, @NonNull Message.NotifyUserUnbindDevReqMsg reqMsg) {
        if (mConversation != null) {
            Message.UsrDevAssoc uda = reqMsg.getUsrDevAssoc();
            if (!uda.getDeviceId().equals(mConversation.mDeviceId)) {
                // 不是当前设备，不做任何动作
                return;
            }
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

//            mConversation.removeMember(uda.getUserId()); 不做处理
        }
    }

    @Override
    public void onDeviceBind(TlcService tlcService, @NonNull Pkt reqPkt, @NonNull Message.NotifyUserBindDevReqMsg reqMsg) {
        if (mConversation != null) {
            Message.UsrDevAssoc uda = reqMsg.getUsrDevAssoc();
            if (uda.getUserId().equals(mUserId) && uda.getDeviceId().equals(mConversation.mDeviceId)) {
                if (TextUtils.isEmpty(uda.getAvatar())) {
                    mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getPermission());
                } else {
                    mConversation.addMember(uda.getUserId(), uda.getRelation(), uda.getAvatar(), uda.getPermission());
                }
            }
        }
    }

    @Override
    public void onDeviceInfoChanged(@NonNull DeviceInfo deviceInfo) {
        if (mConversation == null || mConversation.isGroup())
            return;
        if (mDeviceSupportedTextMessage != null && mConversation.mDeviceId.equals(deviceInfo.getDeviceId())) {
            boolean supported = deviceSupportedTextMessage(mConversation.mDeviceId);
            if (supported != mDeviceSupportedTextMessage) {
                mDeviceSupportedTextMessage = supported;
                chatKeyboard.setTextEditorEnable(supported);
            }
        }
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }
}
