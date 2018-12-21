package com.cqkct.FunKidII.glide;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

/**
 * An implementation of ModelStreamLoader that leverages the StreamOpener class and the
 * ExecutorService backing the Engine to download the image and resize it in memory before saving
 * the resized version directly to the disk cache.
 */
public final class GroupIconModelLoader implements ModelLoader<GroupAvatar, InputStream> {
    public static class Factory implements ModelLoaderFactory<GroupAvatar, InputStream> {
        private Context mContext;

        public Factory(Context context) {
            mContext = context;
        }

        @NonNull
        @Override
        public ModelLoader<GroupAvatar, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new GroupIconModelLoader(mContext, multiFactory.build(Avatar.class, InputStream.class), multiFactory.build(Integer.class, InputStream.class));
        }

        @Override
        public void teardown() {
        }
    }




    private Context mContext;
    @NonNull
    private final ModelLoader<Avatar, InputStream> avatarLoader;
    @NonNull
    private final ModelLoader<Integer, InputStream> resourceLoader;

    public GroupIconModelLoader(Context context, @NonNull ModelLoader<Avatar, InputStream> avatarLoader, @NonNull ModelLoader<Integer, InputStream> resourceLoader) {
        mContext = context;
        this.avatarLoader = avatarLoader;
        this.resourceLoader = resourceLoader;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull GroupAvatar model, int width, int height, @NonNull Options options) {
        DataFetcher<InputStream> dataFetcher = new GroupIconStreamFetcher(mContext, avatarLoader, resourceLoader, model, width, height, options);
        return new LoadData<>(new ObjectKey(model), dataFetcher);
    }

    @Override
    public boolean handles(@NonNull GroupAvatar strings) {
        return true;
    }
}