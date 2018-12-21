package com.cqkct.FunKidII.glide;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

public class AvatarModelLoader implements ModelLoader<Avatar, InputStream> {
    /**
     * The default factory for {@link AvatarModelLoader}s.
     */
    public static class Factory implements ModelLoaderFactory<Avatar, InputStream> {
        private final ModelCache<Avatar, GlideUrl> modelCache = new ModelCache<>(500);

        @NonNull
        @Override
        public ModelLoader<Avatar, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new AvatarModelLoader(
                    multiFactory.build(GlideUrl.class, InputStream.class),
                    multiFactory.build(Integer.class, InputStream.class),
                    modelCache);
        }

        @Override
        public void teardown() {
        }
    }





    private static final String TAG = AvatarModelLoader.class.getSimpleName();

    @Nullable
    private final ModelLoader<GlideUrl, InputStream> urlLoader;
    @Nullable
    private final ModelLoader<Integer, InputStream> resourceLoader;
    @Nullable
    private final ModelCache<Avatar, GlideUrl> modelCache;

    private AvatarModelLoader(@Nullable ModelLoader<GlideUrl, InputStream> urlLoader, @Nullable ModelLoader<Integer, InputStream> resourceLoader, @Nullable ModelCache<Avatar, GlideUrl> modelCache) {
        this.urlLoader = urlLoader;
        this.resourceLoader = resourceLoader;
        this.modelCache = modelCache;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull Avatar model, int width, int height, @NonNull Options options) {
        GlideUrl result = null;
        if (modelCache != null) {
            result = modelCache.get(model, width, height);
        }

        if (result == null) {
            String stringURL = getUrl(model, width, height, options);
            if (TextUtils.isEmpty(stringURL)) {
                if (resourceLoader == null)
                    return null;
                int resId = model.getAlternate();
                if (resId <= 0) {
                    model.getDefault();
                }
                if (resId <= 0)
                    return null;
                return resourceLoader.buildLoadData(resId, width, height, options);
            }

            result = new GlideUrl(stringURL, getHeaders(model, width, height, options));

            if (modelCache != null) {
                modelCache.put(model, width, height, result);
            }
        }

        if (urlLoader == null)
            return null;

        return urlLoader.buildLoadData(result, width, height, options);
    }

    @Override
    public boolean handles(@NonNull Avatar model) {
        return true;
    }

    private String getUrl(Avatar model, int width, int height, Options options) {
        return Api.getURL(model, width, height);
    }

    private Headers getHeaders(Avatar model, int width, int height, Options options) {
        return Headers.DEFAULT;
    }
}
