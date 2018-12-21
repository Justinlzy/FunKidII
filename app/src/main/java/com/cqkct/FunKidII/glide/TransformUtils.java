package com.cqkct.FunKidII.glide;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

public class TransformUtils {
    public static final int CIRCLE_CROP_PAINT_FLAGS = TransformationUtils.PAINT_FLAGS | Paint.ANTI_ALIAS_FLAG;
    public static final Paint CIRCLE_CROP_SHAPE_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
    public static final Paint CIRCLE_CROP_BITMAP_PAINT;

    static {
        CIRCLE_CROP_BITMAP_PAINT = new Paint(CIRCLE_CROP_PAINT_FLAGS);
        CIRCLE_CROP_BITMAP_PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    public static Bitmap getAlphaSafeBitmap(@NonNull BitmapPool pool, @NonNull Bitmap maybeAlphaSafe) {
        Bitmap.Config safeConfig = getAlphaSafeConfig(maybeAlphaSafe);
        if (safeConfig.equals(maybeAlphaSafe.getConfig())) {
            return maybeAlphaSafe;
        }

        Bitmap argbBitmap = pool.get(maybeAlphaSafe.getWidth(), maybeAlphaSafe.getHeight(), safeConfig);
        new Canvas(argbBitmap).drawBitmap(maybeAlphaSafe, 0 /*left*/, 0 /*top*/, null /*paint*/);

        // We now own this Bitmap. It's our responsibility to replace it in the pool outside this method
        // when we're finished with it.
        return argbBitmap;
    }

    @NonNull
    public static Bitmap.Config getAlphaSafeConfig(@NonNull Bitmap inBitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Avoid short circuiting the sdk check.
            if (Bitmap.Config.RGBA_F16.equals(inBitmap.getConfig())) { // NOPMD
                return Bitmap.Config.RGBA_F16;
            }
        }

        return Bitmap.Config.ARGB_8888;
    }

    // Avoids warnings in M+.
    public static void clear(Canvas canvas) {
        canvas.setBitmap(null);
    }
}
