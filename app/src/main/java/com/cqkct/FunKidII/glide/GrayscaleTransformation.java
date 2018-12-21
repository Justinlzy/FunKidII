package com.cqkct.FunKidII.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import java.security.MessageDigest;
import java.util.concurrent.locks.Lock;

public class GrayscaleTransformation extends BitmapTransformation {

    private static final int VERSION = 1;
    private static final String ID = GrayscaleTransformation.class.getName() + "." + VERSION;
    private static final byte[] ID_BYTES = ID.getBytes(CHARSET);

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int outWidth, int outHeight) {
        int width = inBitmap.getWidth();
        int height = inBitmap.getHeight();

        // Alpha is required for this transformation.
        Bitmap toTransform = TransformUtils.getAlphaSafeBitmap(pool, inBitmap);

        Bitmap.Config outConfig = TransformUtils.getAlphaSafeConfig(inBitmap);
        Bitmap result = pool.get(width, height, outConfig);
        result.setHasAlpha(true);

        Lock lock = TransformationUtils.getBitmapDrawableLock();
        lock.lock();
        try {
            Canvas canvas = new Canvas(result);
            ColorMatrix saturation = new ColorMatrix();
            saturation.setSaturation(0f);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(saturation));
            canvas.drawBitmap(toTransform, 0, 0, paint);
            TransformUtils.clear(canvas);
        } finally {
            lock.unlock();
        }

        if (toTransform != inBitmap) {
            pool.put(toTransform);
        }

        return result;
    }

    @Override
    public String toString() {
        return GrayscaleTransformation.class.getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GrayscaleTransformation;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
    }
}