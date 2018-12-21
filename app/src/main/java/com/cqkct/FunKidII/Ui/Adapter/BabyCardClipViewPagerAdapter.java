package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
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
import com.cqkct.FunKidII.Ui.view.ClipViewTagData;
import com.cqkct.FunKidII.Ui.view.RecyclingPagerAdapter;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.glide.DeviceAvatar;
import com.cqkct.FunKidII.glide.GlideDefines;

import java.util.List;

import protocol.Message;

public class BabyCardClipViewPagerAdapter extends  RecyclingPagerAdapter {

    private final List<BabyEntity> mData;
    private Context mContext;
    private OnClickBabyItemListener onClickListener;

    public BabyCardClipViewPagerAdapter(Context context, List<BabyEntity> data) {
        this.mData = data;
        this.mContext = context;
    }
    public BabyCardClipViewPagerAdapter(Context context, List<BabyEntity> data, OnClickBabyItemListener listener) {
        this.mData = data;
        this.mContext = context;
        this.onClickListener = listener;
    }

    public interface OnClickBabyItemListener{
        void onBabyItemClick(int pos);
    }

    @Override
    public int getItemPosition(Object object) {
        if (true) {
            if (object == null)
                return POSITION_UNCHANGED;

            View view = (View) object;
            ClipViewTagData tagData = view.getTag() == null ? null : (ClipViewTagData) view.getTag();
            if (tagData == null)
                return POSITION_NONE;

            if (tagData.entity == null)
                return POSITION_NONE;

            if (mData == null || mData.isEmpty())
                return POSITION_NONE;

            if (tagData.position < 0 || tagData.position >= mData.size())
                return POSITION_NONE;

            BabyEntity babyEntity = mData.get(tagData.position);
            if (!BabyEntity.equals(tagData.entity, babyEntity) || tagData.entity.getIs_select() != babyEntity.getIs_select())
                return POSITION_NONE;

            return POSITION_UNCHANGED;
        } else {
            return POSITION_NONE;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {
            convertView = LayoutInflater.from(container.getContext()).inflate(R.layout.side_baby, container, false);
            ClipViewTagData tagData = new ClipViewTagData();
            convertView.setTag(tagData);
        }
        ClipViewTagData tagData = (ClipViewTagData) convertView.getTag();
        BabyEntity entity = mData.get(position);
        tagData.entity = entity;
        tagData.position = position;

        ImageView icon = convertView.findViewById(R.id.side_baby_icon);
        TextView name = convertView.findViewById(R.id.side_baby_name);

        RequestOptions glideOptions;
        if (entity.getIs_select()) {
            if (DeviceInfo.isOnline(entity.getDeviceId())) {
                glideOptions = GlideDefines.currentDeviceAvatarOptions(icon.getContext());
            } else {
                glideOptions = GlideDefines.currentDeviceOfflineAvatarOptions(icon.getContext());
            }
            name.setTextColor(mContext.getResources().getColor(R.color.blue_tone));
        } else {
            if (DeviceInfo.isOnline(entity.getDeviceId())) {
                glideOptions = GlideDefines.DEVICE_AVATAR_OPTIONS;
            } else {
                glideOptions = GlideDefines.DEVICE_OFFLINE_AVATAR_OPTIONS;
            }
            name.setTextColor(mContext.getResources().getColor(R.color.text_color_two));
        }

        int alternateAvatarResId = DeviceInfo.getBabySex(entity.getDeviceId()) == Message.Baby.Sex.FEMALE_VALUE ? R.drawable.mod_baby_female : R.drawable.mod_baby_male;
        Glide.with(icon)
                .load(new DeviceAvatar(entity.getDeviceId(), entity.getBabyAvatar(), alternateAvatarResId))
                .apply(RequestOptions.placeholderOf(icon.getDrawable()))
                .apply(RequestOptions.errorOf(alternateAvatarResId))
                .apply(glideOptions)
                .into(icon);

        String babyName = TextUtils.isEmpty(entity.getName()) ? mContext.getString(R.string.baby) : entity.getName();
        name.setText(babyName);

        return convertView;
    }

    private View mLastView = null;

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        if (mLastView != null) {
            mLastView.setOnClickListener(null);
            mLastView = null;
        }

        View rootView = (View) object;
        if (rootView == null)
            return;

        Object viewTag = rootView.getTag();
        final BabyEntity entity = viewTag == null ? null : ((ClipViewTagData) viewTag).entity;
        if (entity == null)
            return;

        View view = rootView.findViewById(R.id.side_baby_icon);

        View.OnClickListener l;
        if (TextUtils.isEmpty(entity.getDeviceId())) {
            l = v -> {
//                Intent intent = new Intent(mContext, CaptureBindNumberActivity.class);
//                intent.putExtra(CaptureBindNumberActivity.PARAM_KEY_MODE, CaptureBindNumberActivity.PARAM_VALUE_MODE_BIND_DEVICE);
//                mContext.startActivity(intent);
            };
        } else {
            l = v -> {
//                Intent intent = new Intent(mContext, BabyCardActivity.class);
//                intent.putExtra(BabyCardActivity.ACTIVITY_PARAM_DEVICE_ID, entity.getDeviceId());
//                mContext.startActivity(intent);
            };
        }
        if (onClickListener != null) {
            view.setOnClickListener(v -> onClickListener.onBabyItemClick(position));
        }
        mLastView = view;

    }

    @Override
    public int getCount() {
        return mData.size();
    }
}