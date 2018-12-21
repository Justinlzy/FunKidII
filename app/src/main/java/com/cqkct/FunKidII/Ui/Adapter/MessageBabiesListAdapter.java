package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.Bean.MessageBabiesBean;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.FileUtils;
import com.cqkct.FunKidII.Utils.ImageCacheUtils;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;
import com.cqkct.FunKidII.service.MessageCenterUtils;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import protocol.Message;

public class MessageBabiesListAdapter extends RecyclerView.Adapter {

    private List<MessageBabiesBean> mData;
    private OnMessageBabyItemClickListener listener;
    public interface OnMessageBabyItemClickListener{
        void onItemOnClick(int position);
    }

    public MessageBabiesListAdapter(List<MessageBabiesBean> data) {
        mData = data;
    }

    public void setData(List<MessageBabiesBean> data) {
        mData = data;
        notifyDataSetChanged();
    }
    public void setOnMessageBabyItemClickListener(OnMessageBabyItemClickListener onMessageBabyItemClickListener){
        this.listener = onMessageBabyItemClickListener;
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

    public MessageBabiesBean getItem(int position) {
        if (mData == null) {
            return null;
        }
        if (position < 0 || position >= mData.size()) {
            return null;
        }
        return mData.get(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_babies_item, parent, false);
        return new MessageBabiesListHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        MessageBabiesListHolder holder = (MessageBabiesListHolder) viewHolder;
        Context context = holder.itemView.getContext();
        MessageBabiesBean data = mData.get(position);
        holder.linearLayout.setOnClickListener(v -> listener.onItemOnClick(position));

        holder.babyName.setText(TextUtils.isEmpty(data.getBabyName()) ? context.getString(R.string.baby) : data.getBabyName());

        int alternateAvatarResId = DeviceInfo.getBabySex(data.getDeviceId()) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
        Glide.with(holder.babyAvatar)
                .load(new DeviceAvatar(data.getDeviceId(), data.getBabyAvatar(), alternateAvatarResId))
                .apply(RequestOptions.placeholderOf(holder.babyAvatar.getDrawable()))
                .apply(RequestOptions.errorOf(alternateAvatarResId))
                .apply(DeviceInfo.isOnline(data.getDeviceId()) ? GlideDefines.DEVICE_AVATAR_OPTIONS : GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS)
                .into(holder.babyAvatar);

        holder.message.setText(MessageCenterUtils.getNotifyMessageBeanMsgContent(context, data.getLastMessage(), null));

        bindTime(context, holder, data);
        bindBadge(holder, data);
    }
    private void bindTime(Context context, MessageBabiesListHolder holder, MessageBabiesBean data) {
        if (data.getLastMessage() == null || data.getLastMessage().getTime() <= 0) {
            holder.date.setVisibility(View.INVISIBLE);
            return;
        }
        Calendar nowCalendar = Calendar.getInstance();
        Calendar msgCalendar = Calendar.getInstance();
        msgCalendar.setTimeInMillis(data.getLastMessage().getTime());
        holder.date.setText(PublicTools.genTimeText(context, nowCalendar, msgCalendar));
        holder.date.setVisibility(View.VISIBLE);
    }

    private void bindBadge(MessageBabiesListHolder holder, MessageBabiesBean data) {
        if (data.getUnreadCount() > 99) {
            holder.badge.setText(R.string.unread_message_count_overflow);
            holder.badge.setVisibility(View.VISIBLE);
        } else if (data.getUnreadCount() > 0) {
            holder.badge.setText(String.valueOf(data.getUnreadCount()));
            holder.badge.setVisibility(View.VISIBLE);
        } else {
            holder.badge.setVisibility(View.INVISIBLE);
        }
    }

    private Bitmap loadAvatar(String avatar) {
        if (TextUtils.isEmpty(avatar))
            return null;
        try {
            File f = new File(FileUtils.getExternalStorageImageCacheDirFile(), avatar);
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bitmap == null) {
                return null;
            }
            //处理头像（圆形遮罩）
            Bitmap bitmapEdit = ImageCacheUtils.createFramedPhoto(480, 480, bitmap, 1.6f);
            return ImageCacheUtils.toRoundBitmap(bitmapEdit);
        } catch (Exception e) {
            return null;
        }
    }

    class MessageBabiesListHolder extends RecyclerView.ViewHolder {
        TextView babyName, message, date, badge;
        ImageView babyAvatar;
        LinearLayout linearLayout;

        MessageBabiesListHolder(View itemView) {
            super(itemView);
            babyAvatar = itemView.findViewById(R.id.avatar);
            babyName = itemView.findViewById(R.id.baby_name);
            message = itemView.findViewById(R.id.last_message);
            date = itemView.findViewById(R.id.date);
            badge = itemView.findViewById(R.id.badge);
            linearLayout = itemView.findViewById(R.id.message_babies_item);
        }
    }
}
