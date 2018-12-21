package com.cqkct.FunKidII.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.cqkct.FunKidII.Utils.JoinBitmaps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GroupIconStreamFetcher implements DataFetcher<InputStream> {
    private final Context context;
    @NonNull
    private final ModelLoader<Avatar, InputStream> avatarLoader;
    @NonNull
    private final ModelLoader<Integer, InputStream> resourceLoader;
    private final List<Avatar> model;
    private final int width;
    private final int height;
    @NonNull
    Options options;

    public GroupIconStreamFetcher(Context context, @NonNull ModelLoader<Avatar, InputStream> avatarLoader, @NonNull ModelLoader<Integer, InputStream> resourceLoader, List<Avatar> model, int width, int height, @NonNull Options options) {
        this.context = context;
        this.avatarLoader = avatarLoader;
        this.resourceLoader = resourceLoader;
        this.model = model;
        this.width = width;
        this.height = height;
        this.options = options;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        try {
            List<Bitmap> bitmaps = new ArrayList<>();
            for (Avatar avatar : model) {
                Bitmap bitmap = null;
                try {
                    bitmap = Glide.with(context).asBitmap().load(avatar).submit(width, height).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (bitmap == null) {
                    int drawableRes = avatar.getAlternate();
                    if (drawableRes <= 0) {
                        drawableRes = avatar.getDefault();
                    }
                    bitmap = BitmapFactory.decodeResource(context.getResources(), drawableRes);
                }
                bitmaps.add(bitmap);
            }

            callback.onDataReady(bitmap2InputStream(JoinBitmaps.createBitmap(width, height, bitmaps)));
        } catch (Exception e) {
            callback.onLoadFailed(e);
        }
    }

    @Override
    public void cleanup() {

    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.REMOTE;
    }


    public InputStream bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
