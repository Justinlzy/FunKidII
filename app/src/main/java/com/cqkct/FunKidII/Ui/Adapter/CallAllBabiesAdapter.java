package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.Bean.DeviceInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;

import java.util.List;

import protocol.Message;


public class CallAllBabiesAdapter extends RecyclerView.Adapter {

    private RequestOptions mAvatarTransformSelectedCircleCrop;

    private List<BabyEntity> dataList;
    private Context mContext;
    private OnItemClickListener onItemClickListener;
    private int viewId;

    public CallAllBabiesAdapter(List<BabyEntity> dataList, OnItemClickListener onClickListener, int viewIdR, Context context) {
        this.dataList = dataList;
        this.viewId = viewIdR;
        this.mContext = context;
        onItemClickListener = onClickListener;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(viewId, null));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        CallAllBabiesAdapter.ViewHolder vh = (CallAllBabiesAdapter.ViewHolder) viewHolder;
        BabyEntity entity = dataList.get(position);

        if (entity.getId() == null) {
            vh.name.setText("");
            vh.icon.setClickable(false);
            vh.itemView.setTag(entity);
            return;
        }

        viewHolder.itemView.setOnClickListener(v -> onItemClickListener.onItemClickListener(position));
        vh.name.setTextColor(mContext.getResources().getColor(R.color.white));
        vh.name.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));

        int alternateAvatarResId = DeviceInfo.getBabySex(entity.getDeviceId()) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
        Glide.with(vh.icon)
                .load(new DeviceAvatar(entity.getDeviceId(), entity.getBabyAvatar(), alternateAvatarResId))
                .apply(RequestOptions.placeholderOf(vh.icon.getDrawable()))
                .apply(RequestOptions.errorOf(alternateAvatarResId))
                .apply(DeviceInfo.isOnline(entity.getDeviceId()) ? GlideDefines.DEVICE_AVATAR_OPTIONS : GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS)
                .into(vh.icon);

        String babyName = TextUtils.isEmpty(entity.getName()) ? mContext.getString(R.string.baby) : entity.getName();
        vh.name.setText(babyName);
        vh.itemView.setTag(entity);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView name;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
        }
    }

    public interface OnItemClickListener {
        void onItemClickListener(int pos);
    }

}


