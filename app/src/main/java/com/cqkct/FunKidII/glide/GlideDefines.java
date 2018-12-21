package com.cqkct.FunKidII.glide;

import android.content.Context;

import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.request.RequestOptions;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.DensityUtils;

public class GlideDefines {
    public static final RequestOptions DEVICE_AVATAR_OPTIONS = new RequestOptions()
            .skipMemoryCache(false)
            .circleCrop()
            .autoClone();

    public static final RequestOptions DEVICE_OFFLINE_AVATAR_OPTIONS = new RequestOptions()
            .skipMemoryCache(false)
            .transform(new MultiTransformation<>(new GrayscaleTransformation(), new CircleCropTransform()))
            .autoClone();

    private static RequestOptions mCurrentDeviceAvatarOptions;
    public synchronized static RequestOptions currentDeviceAvatarOptions(Context context) {
        if (mCurrentDeviceAvatarOptions == null) {
            mCurrentDeviceAvatarOptions = new RequestOptions()
                    .skipMemoryCache(false)
                    .transform(new CircleCropTransform(
                            DensityUtils.dip2pix(context, 1.5f),
                            context.getResources().getColor(R.color.blue_tone),
                            context.getResources().getColor(R.color.blue_tone_alpha_20))
                    )
                    .autoClone();
        }
        return mCurrentDeviceAvatarOptions;
    }

    private static RequestOptions mCurrentDeviceOfflineAvatarOptions;
    public synchronized static RequestOptions currentDeviceOfflineAvatarOptions(Context context) {
        if (mCurrentDeviceOfflineAvatarOptions == null) {
            mCurrentDeviceOfflineAvatarOptions = new RequestOptions()
                    .skipMemoryCache(false)
                    .transform(new MultiTransformation<>(
                                    new GrayscaleTransformation(),
                                    new CircleCropTransform(
                                            DensityUtils.dip2pix(context, 1.5f),
                                            context.getResources().getColor(R.color.blue_tone),
                                            context.getResources().getColor(R.color.blue_tone_alpha_20))
                            )
                    )
                    .autoClone();
        }
        return mCurrentDeviceOfflineAvatarOptions;
    }
}
