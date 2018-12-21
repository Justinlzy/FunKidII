package com.cqkct.FunKidII.Utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.View;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Created by KCT on 2016/3/31.
 */
public class ImageCacheUtils {
    private static final String TAG = ImageCacheUtils.class.getSimpleName();

    /**
     * 根据图片路径 获取图片对象
     *
     * @param url
     * @return
     */
    public static Bitmap getLocalBitmap(String url) {
        File f = new File(url);
        if (!f.exists()) {
            L.e("文件不存在");
            return null;
        }
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis); // 把File流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getLocalBitmap(File file) {
        if (file == null || !file.exists()) {
            L.e(TAG, "getLocalBitmap(" + file + "): 文件不存在");
            return null;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            return BitmapFactory.decodeStream(fis); // 把File流转化为Bitmap图片
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static Bitmap loadAvatar(String avatar) {
        if (TextUtils.isEmpty(avatar))
            return null;
        try {
            File f = new File(FileUtils.getExternalStorageImageCacheDirFile(), avatar);
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bitmap == null) {
                return null;
            }
            //处理头像（圆形遮罩）
            Bitmap bitmapEdit = ImageCacheUtils.createFramedPhoto(480, 480, bitmap, 1.6f);
            return ImageCacheUtils.toRoundBitmap(bitmapEdit);
        } catch (Exception e) {
            return null;
        }
    }




    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        return getRoundedCornerBitmap(bitmap, true);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, String colorString) {
        return getRoundedCornerBitmap(bitmap, true, colorString);
    }


    /**
     * @param bitmap                   need rounded bitmap
     * @param hasRoundBitmapWithWhite: use't whitRound
     * @return rounded bitmap
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, boolean hasRoundBitmapWithWhite) {
        return getRoundedCornerBitmap(bitmap, hasRoundBitmapWithWhite, "");
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, boolean hasRoundBitmapWithWhite, String colorString) {
        int size = bitmap.getWidth() < bitmap.getHeight() ? bitmap.getWidth()
                : bitmap.getHeight();
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);
        final float roundPx = size / 2;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(roundPx, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        if (hasRoundBitmapWithWhite)
            return getRoundBitmapWithWhite(output, colorString);
        else
            return output;
    }

    public static Bitmap rsBlur(Context context, Bitmap source, int radius) {

        Bitmap inputBmp = source;
        //(1)
        RenderScript renderScript = RenderScript.create(context);

        Log.i(TAG, "scale size:" + inputBmp.getWidth() + "*" + inputBmp.getHeight());

        // Allocate memory for Renderscript to work with
        //(2)
        final Allocation input = Allocation.createFromBitmap(renderScript, inputBmp);
        final Allocation output = Allocation.createTyped(renderScript, input.getType());
        //(3)
        // Load up an instance of the specific script that we want to use.
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        //(4)
        scriptIntrinsicBlur.setInput(input);
        //(5)
        // Set the blur radius
        scriptIntrinsicBlur.setRadius(radius);
        //(6)
        // Start the ScriptIntrinisicBlur
        scriptIntrinsicBlur.forEach(output);
        //(7)
        // Copy the output to the blurred bitmap
        output.copyTo(inputBmp);
        //(8)
        renderScript.destroy();

        return inputBmp;
    }

    public static int getScreenWidth(Activity context) {
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    /**
     * 把一个bitmap压缩，压缩到指定大小
     *
     * @param bm
     * @param width
     * @param height
     * @return
     */
    public static Bitmap scaleBitmap(Bitmap bm, float width, float height) {
        if (bm == null) {
            return null;
        }
        int bmWidth = bm.getWidth();
        int bmHeight = bm.getHeight();
        float scaleWidth = width / bmWidth;
        float scaleHeight = height / bmHeight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        if (scaleWidth == 1 && scaleHeight == 1) {
            return bm;
        } else {
            Bitmap resizeBitmap = Bitmap.createBitmap(bm, 0, 0, bmWidth, bmHeight, matrix, false);
            bm.recycle();//回收图片内存
            bm.setDensity(4);
            return resizeBitmap;
        }
    }


    /**
     * 获取白色边框
     *
     * @param bitmap
     * @return
     */
    public static Bitmap getRoundBitmapWithWhite(Bitmap bitmap, String colorString) {
        int size = bitmap.getWidth() < bitmap.getHeight() ? bitmap.getWidth()
                : bitmap.getHeight();
        int num = 20;
        int sizeBig = size + num;
        Bitmap output = Bitmap.createBitmap(sizeBig, sizeBig, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = Color.parseColor(TextUtils.isEmpty(colorString) ? "#FFFFFF" : colorString);
        final Paint paint = new Paint();
        final float roundPx = sizeBig / 2;

        paint.setAntiAlias(false);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawBitmap(bitmap, num / 2, num / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));

        RadialGradient gradient = new RadialGradient(roundPx, roundPx, roundPx,
                new int[]{0xFF11CBD8, 0xFF11CBD8, Color.parseColor("#AAAAAAAA")}, new float[]{0.f, 0.97f, 1.0f}, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawCircle(roundPx, roundPx, roundPx, paint);

        return output;
    }

    /**
     * 转换图片成圆形
     *
     * @param bitmap 传入Bitmap对象
     * @return
     */
    public static Bitmap toRoundBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float roundPx;
        float left, top, right, bottom, dst_left, dst_top, dst_right, dst_bottom;
        if (width <= height) {
            roundPx = width / 2;

            left = 0;
            top = 0;
            right = width;
            bottom = width;

            height = width;

            dst_left = 0;
            dst_top = 0;
            dst_right = width;
            dst_bottom = width;
        } else {
            roundPx = height / 2;

            float clip = (width - height) / 2;

            left = clip;
            right = width - clip;
            top = 0;
            bottom = height;
            width = height;

            dst_left = 0;
            dst_top = 0;
            dst_right = height;
            dst_bottom = height;
        }

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect src = new Rect((int) left, (int) top, (int) right, (int) bottom);
        final Rect dst = new Rect((int) dst_left, (int) dst_top, (int) dst_right, (int) dst_bottom);
        final RectF rectF = new RectF(dst);
        paint.setAntiAlias(true);// 设置画笔无锯齿

        canvas.drawARGB(0, 0, 0, 0); // 填充整个Canvas

        // 以下有两种方法画圆,drawRounRect和drawCircle
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);// 画圆角矩形，第一个参数为图形显示区域，第二个参数和第三个参数分别是水平圆角半径和垂直圆角半径。
        // canvas.drawCircle(roundPx, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint); // 以Mode.SRC_IN模式合并bitmap和已经draw了的Circle

        return output;
    }

    /**
     * 图片合成
     *
     * @param background 背景图
     * @param foreground 前景图
     * @return 合成后的图
     */
    public static Bitmap toConformBitmap(Bitmap background, Bitmap foreground) {
        if (background == null) {
            return null;
        }
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        int fgWidth = foreground.getWidth();

        //create the new blank bitmap 创建一个新的和SRC长度宽度一样的位图
        Bitmap newbmp = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newbmp);
        //draw bg into
        cv.drawBitmap(background, 1, 1, null);//在 0，0坐标开始画入bg
        //draw fg into
        cv.drawBitmap(foreground, (bgWidth - fgWidth) / 2, (bgWidth - fgWidth) / 2, null);//在 0，0坐标开始画入fg ，可以从任意位置画入
        //save all clip
        cv.save(Canvas.ALL_SAVE_FLAG);//保存
        //store
        cv.restore();//存储
        return newbmp;
    }

    /**
     * view 转bitmap
     *
     * @param view：目标view left
     * @return ： result Bitmap
     */
    public static Bitmap getBitmaByView(View view) {
        view.setDrawingCacheEnabled(true);
        view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0,
                view.getMeasuredWidth(),
                view.getMeasuredHeight());

        view.buildDrawingCache();
        Bitmap cacheBitmap = view.getDrawingCache();

        return Bitmap.createBitmap(cacheBitmap);
    }


    /**
     * @param x              图像的宽度
     * @param y              图像的高度
     * @param image          源图片
     * @param outerRadiusRat 圆角的大小
     * @return 圆角图片
     */
    public static Bitmap createFramedPhoto(int x, int y, Bitmap image, float outerRadiusRat) {
        // 根据源文件新建一个darwable对象
        Drawable imageDrawable = new BitmapDrawable(image);

        // 新建一个新的输出图片
        Bitmap output = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 新建一个矩形
        RectF outerRect = new RectF(0, 0, x, y);

        // 产生一个红色的圆角矩形
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        canvas.drawRoundRect(outerRect, outerRadiusRat, outerRadiusRat, paint);

        // 将源图片绘制到这个圆角矩形上
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        imageDrawable.setBounds(0, 0, x, y);
        canvas.saveLayer(outerRect, paint, Canvas.ALL_SAVE_FLAG);
        imageDrawable.draw(canvas);
        canvas.restore();

        return output;
    }


    //图片按比例大小压缩方法（根据路径获取图片并压缩）：
    public static Bitmap getimage(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);//此时返回bm为空

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return compressImage(bitmap);//压缩好比例大小后再进行质量压缩
    }

    //第一：质量压缩法：
    public static Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {    //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            options -= 10;//每次都减少10
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中

        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }


    public static Bitmap mixBitmap(Bitmap background, Bitmap icon) {
        if (background == null) {
            return null;
        }
        if (icon == null) {
            return null;
        }
        int bgW = background.getWidth();
        int bgH = background.getHeight();
//        int fgW = icon.getWidth();
//        int fwH = icon.getHeight();
//        float xScale = bgW * 0.85f / fgW;
//        float yScale = bgH * 0.85f / fwH;
//        float scale = Math.min(xScale, yScale);
//        Matrix matrix = new Matrix();
//        matrix.postScale(scale, scale);
//        Bitmap newIconBitmap = toRoundBitmap(icon);

        Bitmap overlay = Bitmap.createBitmap(bgW, bgH, background.getConfig());
        Canvas cv = new Canvas(overlay);
        cv.drawBitmap(background, new Matrix(), null);

        // cv.save();
//        cv.translate((bgW - fgW * scale) / 2, (bgH - fwH * scale) / 5);
//        cv.drawBitmap(newIconBitmap, matrix, null);
//
//        cv.save(Canvas.ALL_SAVE_FLAG);
//        cv.restore();
        return overlay;
    }

    public static Bitmap mixBitmap(Context context, int backgroundResId, int iconResId) {
        Bitmap bg = BitmapFactory.decodeResource(context.getResources(), backgroundResId);
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), iconResId);
        return mixBitmap(bg, icon);
    }

    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }


    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (Exception e) {
            L.e(TAG, "readPictureDegree", e);
        }
        return degree;
    }

    public static Bitmap rotate(Bitmap img, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        int width = img.getWidth();
        int height = img.getHeight();
        img = Bitmap.createBitmap(img, 0, 0, width, height, matrix, true);
        return img;
    }
}
