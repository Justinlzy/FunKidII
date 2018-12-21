package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.Bean.ConversationBean;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.EmotionUtils;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.db.Entity.ChatEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;
import com.cqkct.FunKidII.glide.GroupAvatar;
import com.cqkct.FunKidII.glide.RelationAvatar;

import java.util.Calendar;
import java.util.List;

import protocol.Message;

public class ConversationListAdapter extends RecyclerViewAdapter<ConversationListAdapter.ConversationViewHolder, ConversationBean> {

    private List<ConversationBean> mData;

    public ConversationListAdapter(List<ConversationBean> data) {
        mData = data;
    }

    public void setData(List<ConversationBean> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public long getItemId(int position) {
        if (mData == null)
            return RecyclerView.NO_ID;
        return RecyclerView.NO_ID;
    }

    public ConversationBean getItem(int position) {
        if (mData == null) {
            return null;
        }
        if (position < 0 || position >= mData.size()) {
            return null;
        }
        return mData.get(position);
    }

    @Override
    public ConversationViewHolder onCreateViewHolder2(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversation_list_item, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        ConversationBean data = mData.get(position);
        bindAvatar(context, holder, data);
        bindTitle(context, holder, data);
        bindMessageDesc(context, holder, data);
        bindTime(context, holder, data);
        bindBadge(context, holder, data);
        holder.sepLine.setVisibility(position < getItemCount() - 1 ? View.VISIBLE : View.GONE);
    }

    private void bindAvatar(Context context, ConversationViewHolder holder, ConversationBean data) {
        int alternateAvatarResId = DeviceInfo.getBabySex(data.mDeviceId) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
        if (data.isGroup()) {
            GroupAvatar icons = new GroupAvatar();
            // 设备头像
            icons.add(new DeviceAvatar(data.mDeviceId, data.mBabyAvatar, alternateAvatarResId, alternateAvatarResId));
            // APP 用户头像
            for (ConversationBean.Member member : data.mMembers) {
                RelationAvatar avatar;
                if (RelationUtils.isCustomRelation(member.mRelation)) {
                    avatar = new RelationAvatar(data.mDeviceId, member.mUserId, member.mUserAvatar);
                } else {
                    avatar = new RelationAvatar(data.mDeviceId, member.mUserId, RelationUtils.getIconResId(member.mRelation));
                }
                icons.add(avatar);
                if (icons.size() >= 5) {
                    break;
                }
            }
            Glide.with(holder.avatar)
                    .load(icons)
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.placeholderOf(holder.avatar.getDrawable()))
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.errorOf(R.drawable.group_head))
                    .into(holder.avatar);
        } else {
            Glide.with(holder.avatar)
                    .load(new DeviceAvatar(data.mDeviceId, data.mBabyAvatar, alternateAvatarResId))
                    .apply(RequestOptions.placeholderOf(holder.avatar.getDrawable()))
                    .apply(RequestOptions.errorOf(alternateAvatarResId))
                    .apply(DeviceInfo.isOnline(data.mDeviceId) ? GlideDefines.DEVICE_AVATAR_OPTIONS : GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS)
                    .into(holder.avatar);
        }
    }

    private void bindTitle(Context context, ConversationViewHolder holder, ConversationBean data) {
        String babyName = data.mBabyName;
        if (TextUtils.isEmpty(babyName)) {
            babyName = context.getString(R.string.baby);
        }
        if (data.isGroup()) {
            holder.title.setText(context.getString(R.string.family_group_chat_of_baby, babyName));
        } else {
            holder.title.setText(babyName);
        }
    }

    private void bindMessageDesc(Context context, ConversationViewHolder holder, ConversationBean data) {
        if (data.lastMessage == null) {
            if (data.isGroup()) {
                holder.text.setText(R.string.try_to_chat_with_family_members);
            } else {
                holder.text.setText(R.string.try_to_chat_with_baby);
            }
            return;
        }

        ChatEntity message = data.lastMessage;

        String messageDesc = genMessageDesc(context, message);

        if (message.getIsSendDir() || !message.isGroup()) {
            holder.text.setText(messageDesc);
            return;
        }

        if (message.getSenderType() == Message.TermAddr.Type.DEVICE_VALUE) {
            // 设备发来的
            String babyName = data.mBabyName;
            if (TextUtils.isEmpty(babyName)) {
                babyName = context.getString(R.string.baby);
            }
            messageDesc = babyName + ":" + messageDesc;
        } else {
            // APP 发来的
            ConversationBean.Member member = data.getMember(message.getSenderId());
            if (member != null) {
                String relation = RelationUtils.decodeRelation(context, member.mRelation);
                if (!TextUtils.isEmpty(relation)) {
                    messageDesc = relation + ":" + messageDesc;
                }
            }
        }
        holder.text.setText(messageDesc);
    }

    private String genMessageDesc(Context context, ChatEntity message) {
        switch (message.getMessageType()) {
            case ChatEntity.TYPE_VOICE:
                return context.getString(R.string.chat_conversation_message_voice);
            case ChatEntity.TYPE_EMOTICON: {
                String emotion = context.getString(EmotionUtils.EMOTION_DESC_STRING_MAP.get(message.getEmoticon()));
                if (TextUtils.isEmpty(emotion)) {
                    emotion = context.getString(R.string.emoticon);
                }
                return context.getString(R.string.chat_conversation_message_emotion, emotion);
            }
            case ChatEntity.TYPE_TEXT:
                return message.getText();
            default:
                return "";
        }
    }

    private void bindTime(Context context, ConversationViewHolder holder, ConversationBean data) {
        if (data.lastMessage == null || data.lastMessage.getTimestamp() <= 0) {
            holder.date.setVisibility(View.INVISIBLE);
            return;
        }
        Calendar nowCalendar = Calendar.getInstance();
        Calendar msgCalendar = Calendar.getInstance();
        msgCalendar.setTimeInMillis(data.lastMessage.getTimestamp());
        holder.date.setText(PublicTools.genTimeText(context, nowCalendar, msgCalendar));
        holder.date.setVisibility(View.VISIBLE);
    }

    private void bindBadge(Context context, ConversationViewHolder holder, ConversationBean data) {
        if (data.unreadCount > 99) {
            holder.badge.setText(R.string.unread_message_count_overflow);
            holder.badge.setVisibility(View.VISIBLE);
        } else if (data.unreadCount > 0) {
            holder.badge.setText(String.valueOf(data.unreadCount));
            holder.badge.setVisibility(View.VISIBLE);
        } else {
            holder.badge.setVisibility(View.INVISIBLE);
        }
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        ImageView avatar;
        TextView title, text, date, badge;
        View sepLine;

        ConversationViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            title = itemView.findViewById(R.id.title);
            text = itemView.findViewById(R.id.text);
            date = itemView.findViewById(R.id.date);
            badge = itemView.findViewById(R.id.badge);
            sepLine = itemView.findViewById(R.id.sep_line);
        }
    }

}
