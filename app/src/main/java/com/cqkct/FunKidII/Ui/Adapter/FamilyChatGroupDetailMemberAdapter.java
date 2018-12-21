package com.cqkct.FunKidII.Ui.Adapter;

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
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;
import com.cqkct.FunKidII.glide.RelationAvatar;

import java.util.ArrayList;
import java.util.List;

import protocol.Message;

public class FamilyChatGroupDetailMemberAdapter extends RecyclerViewAdapter<FamilyChatGroupDetailMemberAdapter.ViewHolder, FamilyChatGroupDetailMemberAdapter.DataType> {

    class DataType {
        private String deviceId;
        private String userId;
        private String avatarFileName;
        private String name;
        private String mUserAvatar;

        DataType(String deviceId, String avatarFileName, String name) {
            this.deviceId = deviceId;
            this.avatarFileName = avatarFileName;
            this.name = name;
        }

        DataType(String deviceId, String userId, String relation, String userAvatar) {
            this.deviceId = deviceId;
            this.userId = userId;
            this.name = relation;
            this.mUserAvatar = userAvatar;
        }

        private boolean isDevice() {
            return TextUtils.isEmpty(userId);
        }
    }

    private List<DataType> mData = new ArrayList<>();

    public FamilyChatGroupDetailMemberAdapter(ConversationBean data) {
        initData(data);
    }

    public void setData(ConversationBean data) {
        initData(data);
        notifyDataSetChanged();
    }

    private void initData(ConversationBean data) {
        mData.clear();
        mData.add(new DataType(data.mDeviceId, data.mBabyAvatar, data.mBabyName));

        for (ConversationBean.Member member : data.mMembers) {
            mData.add(new DataType(data.mDeviceId, member.mUserId, member.mRelation, member.mUserAvatar));
        }
    }

    @Override
    public DataType getItem(int position) {
        if (mData != null) {
            return mData.get(position);
        } else {
            return null;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder2(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.family_chat_group_detail_member_item, null));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DataType data = getItem(position);
        if (data.isDevice()) {
            bindDevice(holder, data);
        } else {
            bindAppUser(holder, data);
        }
    }

    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        } else {
            return 0;
        }
    }

    private void bindDevice(ViewHolder holder, DataType data) {
        int alternateAvatarResId = DeviceInfo.getBabySex(data.deviceId) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
        Glide.with(holder.avatar)
                .load(new DeviceAvatar(data.deviceId, data.avatarFileName, alternateAvatarResId))
                .apply(RequestOptions.placeholderOf(holder.avatar.getDrawable()))
                .apply(RequestOptions.errorOf(alternateAvatarResId))
                .apply(DeviceInfo.isOnline(data.deviceId) ? GlideDefines.DEVICE_AVATAR_OPTIONS : GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS)
                .into(holder.avatar);

        holder.badge.setVisibility(View.VISIBLE);

        String babyName = data.name;
        if (TextUtils.isEmpty(babyName)) {
            holder.name.setText(R.string.baby);
        } else {
            holder.name.setText(babyName);
        }
    }

    private void bindAppUser(ViewHolder holder, DataType data) {
        RelationAvatar avatar;
        if (RelationUtils.isCustomRelation(data.name)) {
            avatar = new RelationAvatar(data.deviceId, data.userId, data.mUserAvatar);
        } else {
            avatar = new RelationAvatar(data.deviceId, data.userId, RelationUtils.getIconResId(data.name));
        }
        Glide.with(holder.avatar)
                .load(avatar)
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.placeholderOf(holder.avatar.getDrawable()))
                .apply(RequestOptions.skipMemoryCacheOf(false))
                .apply(RequestOptions.errorOf(R.drawable.head_relation))
                .into(holder.avatar);

        holder.badge.setVisibility(View.GONE);
        holder.name.setText(RelationUtils.decodeRelation(holder.name.getContext(), data.name));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView avatar;
        public ImageView badge;
        public TextView name;

        public ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            badge = itemView.findViewById(R.id.badge);
            name = itemView.findViewById(R.id.name);
        }
    }
}