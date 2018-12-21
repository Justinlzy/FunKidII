package com.cqkct.FunKidII.glide;

import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.R;

public final class RelationAvatar implements Avatar {
    @Nullable
    public final String mDeviceId;
    @Nullable
    public final String mUserId;
    @Nullable
    public final String mAvatar;
    public final int mAvatarResId;

    public RelationAvatar(@Nullable String deviceId, @Nullable String userId, @Nullable String avatar) {
        this(deviceId, userId, avatar, 0);
    }

    public RelationAvatar(@Nullable String deviceId, @Nullable String userId, @DrawableRes int avatarResId) {
        this(deviceId, userId, null, avatarResId);
    }

    public RelationAvatar(@Nullable String deviceId, @Nullable String userId, @Nullable String avatar, @DrawableRes int avatarResId) {
        mDeviceId = deviceId;
        mUserId = userId;
        mAvatar = avatar;
        mAvatarResId = avatarResId;
    }

    @Override
    public @Nullable
    String getFilename() {
        return mAvatar;
    }

    @Override
    public @Nullable
    String getAuthToken() {
        return mUserId;
    }

    @Override
    public @Nullable
    String getResourceToken() {
        return mUserId;
    }

    @Override
    public int getAlternate() {
        return mAvatarResId;
    }

    @Override
    public @DrawableRes
    int getDefault() {
        return R.drawable.head_relation;
    }

    @Override
    public int hashCode() {
        int h = 0;
        if (mDeviceId != null)
            h = 31 * h + mDeviceId.hashCode();
        if (mUserId != null)
            h = 31 * h + mUserId.hashCode();
        if (mAvatar != null)
            h = 31 * h + mAvatar.hashCode();
        h = 31 * h + getAlternate();
        h = 31 * h + getDefault();
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RelationAvatar) {
            RelationAvatar oth = (RelationAvatar) obj;
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
            if (mUserId == null) {
                if (oth.mUserId != null)
                    return false;
            } else if (!mUserId.equals(oth.mUserId)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + mDeviceId + "," + mUserId + "," + mAvatar + "," + getAlternate() + "," + getDefault() + "}";
    }
}
