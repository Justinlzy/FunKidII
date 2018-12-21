package com.cqkct.FunKidII.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import java.security.MessageDigest;
import java.util.concurrent.locks.Lock;

public class CircleCropTransform extends BitmapTransformation {
    private static final int VERSION = 1;
    private static final String ID = CircleCropTransform.class.getName() + "." + VERSION;

    private Paint mBorderPaint;
    private int mBorderWidthDip;
    private int mBorderColor;
    private Paint mForegroundPaint;
    private int mForegroundColor;

    public CircleCropTransform() {
        init(null, null, null);
    }

    public CircleCropTransform(int borderWidthDip, @ColorInt int borderColor) {
        init(borderWidthDip, borderColor, null);
    }

    public CircleCropTransform(@ColorInt int foregroundColor) {
        init(null, null, foregroundColor);
    }

    public CircleCropTransform(int borderWidthPix, @ColorInt int borderColor, @ColorInt int foregroundColor) {
        init(borderWidthPix, borderColor, foregroundColor);
    }

    private void init(@Nullable Integer borderWidthPix, @Nullable @ColorInt Integer borderColor, @Nullable @ColorInt Integer foregroundColor) {
        if (borderWidthPix != null && borderColor != null) {
            mBorderWidthDip = borderWidthPix;
            mBorderColor = borderColor;
            mBorderPaint = new Paint(TransformUtils.CIRCLE_CROP_PAINT_FLAGS);
            mBorderPaint.setColor(borderColor);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mBorderPaint.setStrokeWidth(borderWidthPix);
        }
        if (foregroundColor != null) {
            mForegroundColor = foregroundColor;
            mForegroundPaint = new Paint(TransformUtils.CIRCLE_CROP_PAINT_FLAGS);
            mForegroundPaint.setColor(foregroundColor);
            mForegroundPaint.setStyle(Paint.Style.FILL);
            if (mBorderPaint != null) {
                mForegroundPaint.setStrokeWidth(mBorderPaint.getStrokeWidth());
            }
        }
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        return circleCrop(pool, toTransform, outWidth, outHeight);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CircleCropTransform) {
            CircleCropTransform oth = (CircleCropTransform) o;
            if (mBorderPaint == null) {
                if (oth.mBorderPaint != null)
                    return false;
            } else if (oth.mBorderPaint == null) {
                return false;
            } else {
                if (mBorderWidthDip != oth.mBorderWidthDip || mBorderColor != oth.mBorderColor)
                    return false;
            }
            if (mForegroundPaint == null) {
                if (oth.mForegroundPaint != null)
                    return false;
            } else if (oth.mForegroundPaint == null) {
                return false;
            } else {
                if (mForegroundColor != oth.mForegroundColor)
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = ID.hashCode();
        if (mBorderPaint != null) {
            h = 31 * h + mBorderWidthDip;
            h = 31 * h + mBorderColor;
        }
        if (mForegroundPaint != null) {
            h = 31 * h + mForegroundColor;
        }
        return h;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        String key = ID;
        if (mBorderPaint != null) {
            key += mBorderWidthDip;
            key += mBorderColor;
        }
        if (mForegroundPaint != null) {
            key += mForegroundColor;
        }
        messageDigest.update(key.getBytes(CHARSET));
    }

    public Bitmap circleCrop(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int destWidth, int destHeight) {
        int destMinEdge = Math.min(destWidth, destHeight);
        float radius = destMinEdge / 2f;

        int srcWidth = inBitmap.getWidth();
        int srcHeight = inBitmap.getHeight();

        float scaleX = destMinEdge / (float) srcWidth;
        float scaleY = destMinEdge / (float) srcHeight;
        float maxScale = Math.max(scaleX, scaleY);

        float scaledWidth = maxScale * srcWidth;
        float scaledHeight = maxScale * srcHeight;
        float left = (destMinEdge - scaledWidth) / 2f;
        float top = (destMinEdge - scaledHeight) / 2f;

        RectF destRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Alpha is required for this transformation.
        Bitmap toTransform = TransformUtils.getAlphaSafeBitmap(pool, inBitmap);

        Bitmap.Config outConfig = TransformUtils.getAlphaSafeConfig(inBitmap);
        Bitmap result = pool.get(destMinEdge, destMinEdge, outConfig);
        result.setHasAlpha(true);

        Lock lock = TransformationUtils.getBitmapDrawableLock();
        lock.lock();
        try {
            Canvas canvas = new Canvas(result);
            // Draw a circle
            canvas.drawCircle(radius, radius, radius, TransformUtils.CIRCLE_CROP_SHAPE_PAINT);
            // Draw the bitmap in the circle
            canvas.drawBitmap(toTransform, null, destRect, TransformUtils.CIRCLE_CROP_BITMAP_PAINT);
            // Draw the ring
            if (mBorderPaint != null) {
                canvas.drawCircle(radius, radius, radius - mBorderPaint.getStrokeWidth() / 2, mBorderPaint);
            }
            if (mForegroundPaint != null) {
                canvas.drawCircle(radius, radius, radius - mForegroundPaint.getStrokeWidth() / 2, mForegroundPaint);
            }
            TransformUtils.clear(canvas);
        } finally {
            lock.unlock();
        }

        if (toTransform != inBitmap) {
            pool.put(toTransform);
        }

        return result;
    }
}
