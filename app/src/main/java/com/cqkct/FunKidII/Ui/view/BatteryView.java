package com.cqkct.FunKidII.Ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.cqkct.FunKidII.R;

public class BatteryView extends View {
    private static final int WIDTH = 76;
    private static final int HEIGHT = 25;

    private static final int backgroundColor = 0xFFEEEEEE;
    private static final int[] energyColors = new int[]{0xFFFA4659, 0xFFEDA200, 0xFF11CBD7};

    private int mWidth = WIDTH;
    private int mHeight = HEIGHT;
    private float backgroundStroke = mHeight * 2.0f / HEIGHT;
    private RectF backgroundRectF = new RectF(backgroundStroke / 2, backgroundStroke / 2, mWidth - backgroundStroke, mHeight - backgroundStroke);
    private float roundRectRadius = backgroundRectF.height() / 2;

    private Bitmap originFlashBitmap, flashBitmap;

    private PaintFlagsDrawFilter paintFlagsDrawFilter;
    private Paint mPaint;
    Path clipPath;

    private float powerPercent = .8f;


    public double getPowerPercent() {
        return powerPercent;
    }

    public void setPowerPercent(float powerPercent) {
        this.powerPercent = powerPercent;
        if (this.powerPercent < 0)
            this.powerPercent = 0;
        postInvalidate();
    }


    public BatteryView(Context context) {
        super(context);
        init();
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        originFlashBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_electric);

        paintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mPaint = new Paint();

        if (clipPath == null) {
            clipPath = new Path();
            clipPath.addRoundRect(backgroundRectF, roundRectRadius, roundRectRadius, Path.Direction.CW);
        }
    }

    private static int getDefaultSize2(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(size, specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 计算控件尺寸
        int newWidth = getDefaultSize2(WIDTH, widthMeasureSpec);
        int newHeight = getDefaultSize2(HEIGHT, heightMeasureSpec);
        setMeasuredDimension(newWidth, newHeight);

        if (newWidth != mWidth || newHeight != mHeight) {
            mWidth = newWidth;
            mHeight = newHeight;
            backgroundRectF.set(backgroundStroke / 2, backgroundStroke / 2, mWidth - backgroundStroke, mHeight - backgroundStroke);
            roundRectRadius = backgroundRectF.height() / 2;
            clipPath.reset();
            clipPath.addRoundRect(backgroundRectF, roundRectRadius, roundRectRadius, Path.Direction.CW);

            flashBitmap = null;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.setDrawFilter(paintFlagsDrawFilter);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制界面

        // 电池电量
        int resourceIdx;
        if (powerPercent < 0.2f) {
            resourceIdx = 0;
        } else if (powerPercent < 0.5f) {
            resourceIdx = 1;
        } else {
            resourceIdx = 2;
        }

        // 电池背景
        mPaint.setStrokeWidth(backgroundStroke);
        // 填充
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(backgroundColor);
        canvas.drawRoundRect(backgroundRectF, roundRectRadius, roundRectRadius, mPaint);
        // 边框
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(energyColors[resourceIdx]);
        canvas.drawRoundRect(backgroundRectF, roundRectRadius, roundRectRadius, mPaint);

        // 电池电量
        float energyWidth = backgroundRectF.width() * (powerPercent > 1.0f ? 1.0f : powerPercent);
        canvas.save();
        canvas.clipPath(clipPath);
        canvas.translate(energyWidth - backgroundRectF.width(), 0);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(backgroundRectF, roundRectRadius, roundRectRadius, mPaint);
        canvas.restore();

        // 充电闪电
        if (powerPercent > 1.0f) {
            float flashHeight = mHeight - backgroundStroke * 2;
            if (flashBitmap == null) {
                if (((int) (flashHeight + 0.5f)) != originFlashBitmap.getHeight()) {
                    int flashW = (int) (originFlashBitmap.getWidth() / (originFlashBitmap.getHeight() * 1.0f / flashHeight) + 0.5f);
                    flashBitmap = Bitmap.createScaledBitmap(originFlashBitmap, flashW, (int) (flashHeight + 0.5f), false);
                } else {
                    flashBitmap = originFlashBitmap;
                }
            }
            float flashLeft = mWidth / 2.0f - flashBitmap.getWidth() * 1.0f / 2.0f;
            canvas.drawBitmap(flashBitmap, flashLeft, backgroundStroke, null);
        }
    }
}
