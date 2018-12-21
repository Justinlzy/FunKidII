package com.cqkct.FunKidII.glide;

import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.R;

public final class DeviceAvatar implements Avatar {
    @Nullable
    public final String mDeviceId;
    @Nullable
    public final String mAvatar;
    public final int mAvatarResId;
    public final int mDefaultResId;

    public DeviceAvatar(@Nullable String deviceId, @Nullable String avatar, @DrawableRes int avatarResId) {
        this(deviceId, avatar, avatarResId, 0);
    }

    public DeviceAvatar(@Nullable String deviceId, @Nullable String avatar, @DrawableRes int avatarResId, @DrawableRes int defaultResId) {
        mDeviceId = deviceId;
        mAvatar = avatar;
        mAvatarResId = avatarResId;
        mDefaultResId = defaultResId;
    }

    @Override
    public @Nullable
    String getFilename() {
        return mAvatar;
    }

    @Override
    public @Nullable
    String getAuthToken() {
        return mDeviceId;
    }

    @Override
    public @Nullable
    String getResourceToken() {
        return mDeviceId;
    }

    @Override
    public int getAlternate() {
        return mAvatarResId;
    }

    @Override
    public @DrawableRes
    int getDefault() {
        return R.drawable.mod_baby_male;
    }

    @Override
    public int hashCode() {
        int h = 0;
        if (mDeviceId != null)
            h = 31 * h + mDeviceId.hashCode();
        if (mAvatar != null)
            h = 31 * h + mAvatar.hashCode();
        h = 31 * h + getAlternate();
        h = 31 * h + getDefault();
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeviceAvatar) {
            DeviceAvatar oth = (DeviceAvatar) obj;
            if (mAvatar == null) {
                if (oth.mAvatar != null)
                    return false;
            } else if (!mAvatar.equals(oth.mAvatar)) {
                return false;
            }
            if (getAlternate() != oth.getAlternate())
                return false;
            if (getDefault() != oth.getDefault())
                return false;
            if (mDeviceId == null) {
                if (oth.mDeviceId != null)
                    return false;
            } else if (!mDeviceId.equals(oth.mDeviceId)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + mDeviceId + "," + mAvatar + "," + getAlternate() + "," + getDefault() + "}";
    }
}
