package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
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
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;

import java.util.List;

import protocol.Message;

public class BabyListAdapter extends RecyclerView.Adapter {

    private List<BabyEntity> dataList;
    private OnItemClickListener onItemClickListener;
    private Context mContext;
    private RecyclerView mBabyRecyclerView;

    public BabyListAdapter(List<BabyEntity> dataList, OnItemClickListener onClickListener, RecyclerView recyclerView, Context context) {
        this.dataList = dataList;
        this.onItemClickListener = onClickListener;
        this.mBabyRecyclerView = recyclerView;
        this.mContext = context;
    }

    public void onNotifyDataSetChanged(List<BabyEntity> list) {
        if (!dataList.isEmpty()) {
            dataList.clear();
        }
        dataList.addAll(list);
        setBabiesLayoutParams();
        notifyDataSetChanged();
    }

    private void setBabiesLayoutParams(){
        ViewGroup.LayoutParams layoutParams = mBabyRecyclerView.getLayoutParams();
        if (dataList.size() >= 5) {
            layoutParams.height = PublicTools.dip2px(mContext, 320);
        }else {
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
        mBabyRecyclerView.setLayoutParams(layoutParams);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.popupwindow_baby_list_adapter, parent, false);
        return new BabyListHolder(convertView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BabyListHolder vh = (BabyListHolder) holder;
        BabyEntity entity = dataList.get(position);

        vh.itemView.setOnClickListener(v -> onItemClickListener.onItemClickListener(position));

        RequestOptions glideOptions;

        vh.babyChosenIcon.setVisibility(entity.getIs_select() ? View.VISIBLE : View.GONE);

        if (DeviceInfo.isOnline(entity.getDeviceId())) {
            glideOptions = GlideDefines.DEVICE_AVATAR_OPTIONS;
            vh.babyName.setTextColor(vh.babyName.getResources().getColor(R.color.text_color_two));
        } else {
            glideOptions = GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS;
            vh.babyName.setTextColor(vh.babyName.getResources().getColor(R.color.text_color_four));
        }

        int alternateAvatarResId = DeviceInfo.getBabySex(entity.getDeviceId()) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
        Glide.with(vh.babyIcon)
                .load(new DeviceAvatar(entity.getDeviceId(), entity.getBabyAvatar(), alternateAvatarResId))
                .apply(RequestOptions.placeholderOf(vh.babyIcon.getDrawable()))
                .apply(RequestOptions.errorOf(alternateAvatarResId))
                .apply(glideOptions)
                .into(vh.babyIcon);

        String babyName = TextUtils.isEmpty(entity.getName()) ? vh.babyName.getResources().getString(R.string.baby) : entity.getName();
        vh.babyName.setText(babyName);
        vh.itemView.setTag(entity);
//        if (dataList.size() == position + 1) {
//            vh.line.setVisibility(View.GONE);
//        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class BabyListHolder extends RecyclerView.ViewHolder {
        ImageView babyIcon;
        ImageView babyChosenIcon;
        TextView babyName;
//        View line;

        BabyListHolder(View itemView) {
            super(itemView);
            babyIcon = itemView.findViewById(R.id.baby_head_icon);
            babyChosenIcon = itemView.findViewById(R.id.baby_chosen_badge);
            babyName = itemView.findViewById(R.id.baby_name);
//            line = itemView.findViewById(R.id.line);
        }
    }

    public interface OnItemClickListener {
        void onItemClickListener(int pos);
    }

}
