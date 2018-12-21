package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.Bean.ConversationBean;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.DensityUtils;
import com.cqkct.FunKidII.Utils.EmotionUtils;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.MediaManager;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.db.Entity.ChatEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;
import com.cqkct.FunKidII.glide.RelationAvatar;
import com.cqkct.FunKidII.service.OkHttpRequestManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.greendao.query.LazyList;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;

import protocol.Message;

public class ChatListAdapter extends RecyclerViewAdapter<ChatListAdapter.ChatViewHolder, ChatEntity> {
    private static final int DIR_SEND = 0;
    private static final int DIR_RECV = 1;

    private String mUserId;
    private LazyList<ChatEntity> mData;
    @NonNull
    private ConversationBean mConversation;

    private Integer paddingRight;

    private OnItemLongClickListener mOnItemBubbleLongClickListener;
    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mOnItemBubbleLongClickListener != null && mRecyclerView != null) {
                try {
                    int position = (int) v.getTag();
                    if (position >= 0 && position < getItemCount()) {
                        return mOnItemBubbleLongClickListener.onItemLongClick(mRecyclerView, ChatListAdapter.this, v, position);
                    }
                } catch (Exception ignored) {
                }
            }
            return false;
        }
    };

    public ChatListAdapter(@NonNull String userId, LazyList<ChatEntity> data, @NonNull ConversationBean conversation) {
        mUserId = userId;
        mData = data;
        mConversation = conversation;
    }

    public void setLazyList(LazyList<ChatEntity> list) {
        if (list != mData) {
            if (mData != null) {
                mData.close();
            }
            mData = list;
            notifyDataSetChanged();
        }
    }

    private int getPaddingRight(View v) {
        if (paddingRight == null) {
            paddingRight = v.getContext().getResources().getDimensionPixelOffset(R.dimen.dimen_10);
        }
        return paddingRight;
    }

    public void setBubbleLongClickListener(OnItemLongClickListener listener) {
        mOnItemBubbleLongClickListener = listener;
    }

    @Override
    public ChatEntity getItem(int position) {
        if (mData != null) {
            return mData.get(position);
        } else {
            return null;
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatEntity data = getItem(position);
        if (data.getIsSendDir())
            return DIR_SEND;
        return DIR_RECV;
    }

    @Override
    public ChatViewHolder onCreateViewHolder2(ViewGroup parent, int viewType) {
        int res;
        if (viewType == DIR_RECV) {
            res = R.layout.chat_list_left_item;
        } else {
            res = R.layout.chat_list_right_item;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(res, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ChatEntity data = getItem(position);

        textSetTime(holder, position, data);
        bindAvatar(holder, data);
        bindName(holder, data);

        holder.notPlayMark.setVisibility(View.GONE);
        holder.unreadMark.setVisibility(View.GONE);

        holder.bubble.setOnClickListener(null);
        holder.bubble.setOnLongClickListener(null);
        holder.emoticon.setOnLongClickListener(null);
        switch (data.getMessageType()) {
            case ChatEntity.TYPE_TEXT:
                bindTextMessage(holder, data, position);
                break;
            case ChatEntity.TYPE_VOICE:
                bindVoiceMessage(holder, data, position);
                break;
            case ChatEntity.TYPE_EMOTICON:
                bindEmoticonMessage(holder, data, position);
                break;
        }

        if (data.getIsSendDir() && data.getSendStatus() == ChatEntity.SEND_STAT_QUEUE) {
            holder.progressing.setVisibility(View.VISIBLE);
        } else {
            holder.progressing.setVisibility(View.GONE);
        }

        holder.failure.setOnClickListener(null);
        if (data.getIsSendDir() && data.getSendStatus() == ChatEntity.SEND_STAT_FAILED) {
            holder.failure.setVisibility(View.VISIBLE);
            holder.failure.setOnClickListener(v -> EventBus.getDefault().post(new Event.ResendChatMessage(data)));
        } else {
            holder.failure.setVisibility(View.GONE);
        }
    }

    private void bindTextMessage(ChatViewHolder holder, ChatEntity data, int position) {
        holder.emoticon.setVisibility(View.GONE);
        holder.bubble.setVisibility(View.VISIBLE);
        holder.bubble.setTag(position);
        holder.bubble.setOnLongClickListener(mOnLongClickListener);
        holder.voiceIcon.setVisibility(View.GONE);
        holder.text.setText(data.getText());
        holder.text.setVisibility(View.VISIBLE);
    }

    private void bindVoiceMessage(ChatViewHolder holder, ChatEntity data, int position) {
        holder.emoticon.setVisibility(View.GONE);
        holder.bubble.setVisibility(View.VISIBLE);
        if (data.getIsSendDir()) {
            holder.voiceIcon.setImageResource(R.drawable.play_gray_3);
        } else {
            holder.voiceIcon.setImageResource(R.drawable.play_white_3);
            if (!data.getVoiceIsPlayed()) {
                holder.notPlayMark.setVisibility(View.VISIBLE);
                holder.unreadMark.setVisibility(View.VISIBLE);
            }
        }
        holder.voiceIcon.setVisibility(View.VISIBLE);

        holder.text.setText(String.format("%d 's", data.getVoiceDuration()));
        holder.text.setVisibility(View.VISIBLE);

        holder.bubble.setOnClickListener(new OnVoiceChatMessageClickListener(mUserId, holder, data, this, position));
        holder.bubble.setTag(position);
        holder.bubble.setOnLongClickListener(mOnLongClickListener);
    }

    private void bindEmoticonMessage(ChatViewHolder holder, ChatEntity data, int position) {
        holder.bubble.setVisibility(View.GONE);
        Integer emotionIconResId = EmotionUtils.EMOTION_CLASSIC_MAP.get(data.getEmoticon());
        if (emotionIconResId == null)
            emotionIconResId = R.drawable.emotion_error;
        holder.emoticon.setImageResource(emotionIconResId);
        holder.emoticon.setTag(position);
        holder.emoticon.setOnLongClickListener(mOnLongClickListener);
        int padding = getPaddingRight(holder.emoticon);
        holder.emoticon.setPadding(padding / 2, 0, padding, 0);
        holder.emoticon.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        /**
         * 时间
         */
        TextView date;
        /**
         * 头像
         */
        ImageView avatar;
        /**
         * 姓名
         */
        TextView name;
        /**
         * 表情图片消息
         */
        ImageView emoticon;
        /**
         * 语音图标
         */
        ImageView voiceIcon;
        /**
         * 文本
         */
        TextView text;
        /**
         * 正在处理
         */
        View progressing;
        /**
         * 发送失败
         */
        View failure;
        /**
         * 语音未播放标识
         */
        View notPlayMark;
        /**
         * 未读标识
         */
        View unreadMark;
        /**
         * 消息气泡 layout
         */
        View bubble;

        ChatViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.date);
            avatar = itemView.findViewById(R.id.avatar);
            name = itemView.findViewById(R.id.name);
            emoticon = itemView.findViewById(R.id.emoticon_image);
            voiceIcon = itemView.findViewById(R.id.voice);
            text = itemView.findViewById(R.id.text);
            notPlayMark = itemView.findViewById(R.id.not_play_mark);
            unreadMark = itemView.findViewById(R.id.unread_mark);
            progressing = itemView.findViewById(R.id.progressing);
            failure = itemView.findViewById(R.id.failure);
            bubble = itemView.findViewById(R.id.bubble);
        }
    }

    private void textSetTime(ChatViewHolder holder, int position, ChatEntity itemData) {
        boolean shouldShowDate = false;
        if (position == 0) {
            shouldShowDate = true;
        } else if (itemData.getTimestamp() - getItem(position - 1).getTimestamp() > 60 * 1000L) {
            shouldShowDate = true;
        }
        if (shouldShowDate) {
            Calendar msgCalendar = Calendar.getInstance();
            msgCalendar.setTimeInMillis(itemData.getTimestamp());
            String timeStr = PublicTools.genTimeText(holder.itemView.getContext(), Calendar.getInstance(), msgCalendar);
            holder.date.setText(timeStr);
            holder.date.setVisibility(View.VISIBLE);
        } else {
            holder.date.setVisibility(View.GONE);
        }
    }

    private void bindAvatar(ChatViewHolder holder, ChatEntity data) {
        if (data.getSenderType() == Message.TermAddr.Type.DEVICE_VALUE) {
            // 设备
            int alternateAvatarResId = DeviceInfo.getBabySex(data.getDeviceId()) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
            Glide.with(holder.avatar)
                    .load(new DeviceAvatar(data.getDeviceId(), mConversation.mBabyAvatar, alternateAvatarResId))
                    .apply(RequestOptions.placeholderOf(holder.avatar.getDrawable()))
                    .apply(RequestOptions.errorOf(alternateAvatarResId))
                    .apply(DeviceInfo.isOnline(data.getDeviceId()) ? GlideDefines.DEVICE_AVATAR_OPTIONS : GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS)
                    .into(holder.avatar);
        } else {
            // APP 用户
            RelationAvatar avatar;
            ConversationBean.Member member = mConversation.getMember(data.getSenderId());
            if (member != null) {
                if (RelationUtils.isCustomRelation(member.mRelation)) {
                    avatar = new RelationAvatar(data.getDeviceId(), member.mUserId, member.mUserAvatar);
                } else {
                    avatar = new RelationAvatar(data.getDeviceId(), member.mUserId, RelationUtils.getIconResId(member.mRelation));
                }
            } else {
                avatar = new RelationAvatar(data.getDeviceId(), null, R.drawable.head_relation);
            }

            Glide.with(holder.avatar)
                    .load(avatar)
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.placeholderOf(holder.avatar.getDrawable()))
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.errorOf(R.drawable.head_relation))
                    .into(holder.avatar);
        }
    }

    private void bindName(ChatViewHolder holder, ChatEntity data) {
        if (data.getIsSendDir() || !data.isGroup()) {
            holder.name.setVisibility(View.GONE);
            return;
        }
        if (data.getSenderType() == Message.TermAddr.Type.DEVICE_VALUE) {
            // 设备
            if (TextUtils.isEmpty(mConversation.mBabyName)) {
                holder.name.setText(R.string.baby);
            } else {
                holder.name.setText(mConversation.mBabyName);
            }
        } else {
            // APP 用户
            String name = null;
            ConversationBean.Member member = mConversation.getMember(data.getSenderId());
            if (member != null) {
                name = RelationUtils.decodeRelation(holder.name.getContext(), member.mRelation);
            }
            holder.name.setText(name);
        }
        holder.name.setVisibility(View.VISIBLE);
    }



    private static class OnVoiceChatMessageClickListener implements View.OnClickListener {
        private static final String TAG = OnVoiceChatMessageClickListener.class.getSimpleName();

        private static volatile OnVoiceChatMessageClickListener sCurrentListener;
        private static volatile boolean isPlaying;

        private String mUserId;
        private ChatViewHolder mHolder;
        private ChatEntity mData;
        private WeakReference<ChatListAdapter> mAdapter;
        private int mPosition;

        private AnimationDrawable mPlayingAnimation;
        private boolean isDownloading;
        private boolean playWhenDownload;
        private Animation mNotPlayingMarkHiddenAction;

        OnVoiceChatMessageClickListener(@NonNull String userId, @NonNull ChatViewHolder holder, @NonNull ChatEntity data, ChatListAdapter adapter, int position) {
            mUserId = userId;
            mHolder = holder;
            mData = data;
            mAdapter = new WeakReference<>(adapter);
            mPosition = position;
        }

        @Override
        public void onClick(View v) {
            if (isDownloading) {
                // 正在下载
                // 下载完成后是否播放
                playWhenDownload = !playWhenDownload;
                return;
            }

            OnVoiceChatMessageClickListener prevListener;
            boolean playing;
            synchronized (OnVoiceChatMessageClickListener.class) {
                prevListener = sCurrentListener;
                sCurrentListener = this;
                playing = isPlaying;
            }
            if (prevListener != null && playing) {
                // 之前正在播放，这次点击后，应该停止播放
                prevListener.stop();
                if (this == prevListener) {
                    // 这次点击的就是上次点击的那个语音
                    // 直接返回，不再做其他事情
                    return;
                }
            }

            if (TextUtils.isEmpty(mData.getFilename()) || mData.getFileSize() <= 0) {
                L.i(TAG, "onClick: " + mData.getId() + ": filename is empty, or file size <= 0! this maybe not voice message");
                return;
            }

            File file = new File(FileUtils.getExternalStorageVoiceChatCacheDirFile(), mData.getFilename());
            if (file.exists()) {
                play(file);
            } else {
                // 文件不存在，下载后播放
                playWhenDownload = true;
                download(v.getContext(), file, this);
            }
        }

        private void play(File file) {
            L.d(TAG, "play " + mData.getId() + " " + mData.getFilename());

            synchronized (OnVoiceChatMessageClickListener.class) {
                isPlaying = true;
            }

            // 如果播放的是接收的语音 且没有读过该语音
            if (!mData.getIsSendDir() && !mData.getVoiceIsPlayed()) {
                mData.setVoiceIsPlayed(true);
                GreenUtils.updateVoiceMessagePlayed(mData.getId());

                ChatListAdapter adapter = mAdapter.get();
                if (adapter != null && adapter.getViewHolder(mPosition) == mHolder) {
                    mHolder.unreadMark.setVisibility(View.GONE); // 不显示状态 已读了就不显示状态

                    if (mNotPlayingMarkHiddenAction == null) {
                        mNotPlayingMarkHiddenAction = new TranslateAnimation(Animation.ABSOLUTE,
                                0, Animation.ABSOLUTE, DensityUtils.dp2px(mHolder.notPlayMark.getContext(), 7),
                                Animation.ABSOLUTE, 0, Animation.ABSOLUTE,
                                0);
                    }
                    mNotPlayingMarkHiddenAction.setDuration(500);
                    mHolder.notPlayMark.setAnimation(mNotPlayingMarkHiddenAction);
                    mNotPlayingMarkHiddenAction.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mHolder.unreadMark.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    mHolder.notPlayMark.setVisibility(View.GONE);
                }
            }

            MediaManager.playSound(file.getAbsolutePath(), new MediaManager.PlaySoundListener() {
                @Override
                public void playBefore() {
                    showPlayingAnimation();
                }

                @Override
                public void playCompletion() {
                    stop();
                }
            });
        }

        private void stop() {
            L.d(TAG, "stop " + mData.getId() + " " + mData.getFilename());
            synchronized (OnVoiceChatMessageClickListener.class) {
                MediaManager.release();
                isPlaying = false;
            }
            hidePlayingAnimation();
        }

        private void showPlayingAnimation() {
            ChatListAdapter adapter = mAdapter.get();
            if (adapter == null)
                return;
            if (adapter.getViewHolder(mPosition) != mHolder)
                return;

            if (mData.getIsSendDir()) {
                mHolder.voiceIcon.setImageResource(R.drawable.voice_to_icon);
            } else {
                mHolder.voiceIcon.setImageResource(R.drawable.voice_from_icon);
            }
            if (mPlayingAnimation != null)
                mPlayingAnimation.stop();
            mPlayingAnimation = (AnimationDrawable) mHolder.voiceIcon.getDrawable();
            mPlayingAnimation.start();
        }

        private void hidePlayingAnimation() {
            if (mPlayingAnimation != null) {
                mPlayingAnimation.stop();
                mPlayingAnimation = null;
            }

            ChatListAdapter adapter = mAdapter.get();
            if (adapter == null)
                return;
            if (adapter.getViewHolder(mPosition) != mHolder)
                return;

            if (mData.getIsSendDir()) {
                mHolder.voiceIcon.setImageResource(R.drawable.play_gray_3);
            } else {
                mHolder.voiceIcon.setImageResource(R.drawable.play_white_3);
            }
        }

        private void download(Context context, File file, OnVoiceChatMessageClickListener onClickListener) {
            isDownloading = true;
            mHolder.progressing.setVisibility(View.VISIBLE);
            OkHttpRequestManager.getInstance(context).downloadChatVoiceFile(file, mUserId, mData.getSenderId(), new OkHttpRequestManager.ReqProgressCallBack<File>() {

                @Override
                public void onProgress(long total, long current) {
                }

                @Override
                public void onReqSuccess(File result) {
                    new Handler().post(() -> {
                        isDownloading = false;
                        ChatListAdapter adapter = mAdapter.get();
                        if (adapter == null)
                            return;
                        if (adapter.getViewHolder(mPosition) == mHolder) {
                            mHolder.progressing.setVisibility(View.GONE);
                        }
                        if (onClickListener == sCurrentListener && playWhenDownload) {
                            play(file);
                        }
                    });
                }

                @Override
                public void onReqFailed(String errorMsg) {
                    new Handler().post(() -> {
                        isDownloading = false;
                        ChatListAdapter adapter = mAdapter.get();
                        if (adapter == null)
                            return;
                        if (adapter.getViewHolder(mPosition) == mHolder) {
                            mHolder.progressing.setVisibility(View.GONE);
                        }
                    });
                }
            });
        }
    }
}