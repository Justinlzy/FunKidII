package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.ContactsActivity;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.db.Entity.ContactEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.RelationAvatar;

import java.util.List;

import protocol.Message;

public class ContactsAdapter extends RecyclerView.Adapter {
    public static final int SWIPE_MENU_NONE = 0;
    public static final int SWIPE_MENU_NORMAL = 1;
    public static final int SWIPE_MENU_ASSOC = 2;
    public static final int SWIPE_MENU_FRIEND = 3;

    private LayoutInflater mInflater;

    private List<ContactEntity> mData;
    @NonNull
    private final String mUserId;

    private int currentUserPermission;
    private boolean hasEditPermission;

    public ContactsAdapter(Context context, @NonNull List<ContactEntity> contactList, @NonNull String userId, int currentUserPermission, boolean hasEditPermission) {
        mInflater = LayoutInflater.from(context);

        this.mData = contactList;
        mUserId = userId;
        this.currentUserPermission = currentUserPermission;
        this.hasEditPermission = hasEditPermission;
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        ContactEntity contact = getItem(position);
        if (contact == null)
            return SWIPE_MENU_NONE;

        if (contact.getType() == ContactEntity.TYPE_ASSOC && contact.getUserId().equals(mUserId)) {
            // 自己可以解绑自己
            return SWIPE_MENU_ASSOC;
        }

        int permission = contact.getPermission();
        int type = SWIPE_MENU_NONE;
        if (hasEditPermission && (permission == 0 || currentUserPermission <= permission)) {
            switch (contact.getType()) {
                case ContactEntity.TYPE_NORMAL:
                    type = SWIPE_MENU_NORMAL;
                    break;
                case ContactEntity.TYPE_ASSOC:
                    type = SWIPE_MENU_ASSOC;
                    break;
                case ContactEntity.TYPE_FRIEND:
                    type = SWIPE_MENU_FRIEND;
                    break;
            }
        }
        return type;
    }

    public ContactEntity getItem(int position) {
        if (position < 0 || position > getItemCount() - 1)
            return null;
        return mData.get(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SubViewHolder(mInflater.inflate(R.layout.contacts_adapter_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        SubViewHolder holder = (SubViewHolder) viewHolder;
        ContactEntity data = getItem(position);
        if (ContactsActivity.isSeparatorData(data)) {
            bindCategorySeparator(holder, data);
        } else {
            bindContact(holder, data);
        }

        holder.divider.setVisibility(View.GONE);
        ContactEntity nextData = getItem(position + 1);
        if (nextData == null || ContactsActivity.isSeparatorData(nextData)) {
            if (!ContactsActivity.isSeparatorData(data) || ContactsActivity.isExpanded(data)) {
                holder.divider.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (!ContactsActivity.isSeparatorData(data)) {
            holder.divider.setVisibility(View.VISIBLE);
        }
    }

    private void bindCategorySeparator(SubViewHolder holder, ContactEntity data) {
        holder.headIcon.setVisibility(View.GONE);
        holder.nameTextView.setVisibility(View.GONE);
        holder.numberTextView.setVisibility(View.GONE);
        holder.adminLabel.setVisibility(View.GONE);
        holder.isYouLabel.setVisibility(View.GONE);
        holder.categorySeparatorView.setVisibility(View.VISIBLE);

        switch (ContactsActivity.getSeparatorCategory(data)) {
            case ContactEntity.TYPE_ASSOC:
                holder.categoryName.setText(R.string.contacts_category_admin);
                break;
            case ContactEntity.TYPE_NORMAL:
                holder.categoryName.setText(R.string.contacts_category_normal);
                break;
            case ContactEntity.TYPE_FRIEND:
                holder.categoryName.setText(R.string.contacts_category_friends);
                break;
            default:
                holder.categoryName.setText("?");
                break;
        }

        holder.categoryExpandIndicator.setRotation(ContactsActivity.isExpanded(data) ? 0 : 180);
    }

    private void bindContact(SubViewHolder holder, ContactEntity data) {
        holder.headIcon.setVisibility(View.VISIBLE);
        holder.nameTextView.setVisibility(View.VISIBLE);
        holder.numberTextView.setVisibility(View.VISIBLE);
        holder.adminLabel.setVisibility(View.GONE);
        holder.isYouLabel.setVisibility(View.GONE);
        holder.categorySeparatorView.setVisibility(View.GONE);

        String name = null;
        if (data.getType() == ContactEntity.TYPE_ASSOC) {
            name = data.getRelation();
        }
        if (TextUtils.isEmpty(name)) {
            name = data.getName();
        }
        name = RelationUtils.decodeRelation(holder.itemView.getContext(), name);
        holder.nameTextView.setText(name);
        holder.numberTextView.setText(data.getNumber());
        if (data.getType() == ContactEntity.TYPE_NORMAL) {
            // 普通联系人
            holder.headIcon.setImageResource(R.drawable.hard_contacts);
        } else if (data.getType() == ContactEntity.TYPE_ASSOC) {
            // 绑定用户
            if (data.getPermission() == Message.UsrDevAssoc.Permission.OWNER_VALUE) {
                holder.adminLabel.setVisibility(View.VISIBLE);
            }
            if (data.getUserId().equals(mUserId)) {
                holder.isYouLabel.setVisibility(View.VISIBLE);
            }

            RelationAvatar avatar;
            if (RelationUtils.isCustomRelation(data.getRelation())) {
                avatar = new RelationAvatar(data.getDeviceId(), data.getUserId(), data.getUserAvatar());
            } else {
                avatar = new RelationAvatar(data.getDeviceId(), data.getUserId(), RelationUtils.getIconResId(data.getRelation()));
            }
            Glide.with(holder.headIcon)
                    .load(avatar)
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.placeholderOf(holder.headIcon.getDrawable()))
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.errorOf(R.drawable.head_relation))
                    .into(holder.headIcon);
        } else if (data.getType() == ContactEntity.TYPE_FRIEND) {
            // 手表好友
            if (TextUtils.isEmpty(data.getName())) {
                if (TextUtils.isEmpty(data.getFriendNickname())) {
                    holder.nameTextView.setText(R.string.friend_of_baby);
                } else {
                    holder.nameTextView.setText(data.getFriendNickname());
                }
            } else if (TextUtils.isEmpty(data.getFriendNickname())) {
                holder.nameTextView.setText(data.getName());
            } else {
                holder.nameTextView.setText(holder.nameTextView.getResources().getString(R.string.friend_display_name_combination, data.getFriendNickname(), data.getName()));
            }

            Glide.with(holder.headIcon)
                    .load(new DeviceAvatar(data.getFriendDeviceId(), data.getFriendBabyAvatar(), R.drawable.ic_head_relation_friend))
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.placeholderOf(holder.headIcon.getDrawable()))
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.errorOf(R.drawable.ic_head_relation_friend))
                    .into(holder.headIcon);
        }
    }

    class SubViewHolder extends RecyclerView.ViewHolder {

        ImageView headIcon, categoryExpandIndicator;
        TextView nameTextView, numberTextView, categoryName;
        View isYouLabel, adminLabel;
        View categorySeparatorView;
        View divider;

        SubViewHolder(View itemView) {
            super(itemView);
            headIcon = itemView.findViewById(R.id.head_icon);
            nameTextView = itemView.findViewById(R.id.name);
            numberTextView = itemView.findViewById(R.id.number);
            adminLabel = itemView.findViewById(R.id.admin);
            isYouLabel = itemView.findViewById(R.id.is_you);
            categorySeparatorView = itemView.findViewById(R.id.category_separator);
            categoryExpandIndicator = itemView.findViewById(R.id.category_expand_indicator);
            categoryName = itemView.findViewById(R.id.category_name);

            divider = itemView.findViewById(R.id.divider);
        }
    }
}