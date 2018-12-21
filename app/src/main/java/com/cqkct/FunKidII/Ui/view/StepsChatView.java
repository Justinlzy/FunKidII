package com.cqkct.FunKidII.Ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;


/**
 * Created by 路很长~ on 2017/2/26.
 */

public class StepsChatView extends View {
    public int mWidth;
    public int mHeight;
    private PaintFlagsDrawFilter drawFilter;
    private Paint paint;
    private Paint textPaint;
    private Paint dashPaint;
    private float[] xValues;
    private float[] yValues;
    private float xStart;
    private long yStart;
    private float xEnd, yEnd;
    private boolean isFillDownLineColor;
    private float scaleLen;
    private float perLengthX;
    private float perLengthY;
    private Paint linePaint;
    private boolean stop;
    private int numCount;
    private float fraction;

    public StepsChatView(Context context) {
        super(context);
        init(context);
    }

    public StepsChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StepsChatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        //在画布上去除锯齿
        drawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        DisplayMetrics dm = getResources().getDisplayMetrics();

        // 屏幕高
        mHeight = (int) (dm.heightPixels * 0.3);
        // 屏幕宽
        mWidth = (int) (dm.widthPixels * 0.8);

        paint = new Paint();
        paint.setColor(Color.BLACK);
        textPaint = new Paint();
        dashPaint = new Paint();

        scaleLen = dip2px(context, scaleLen);
        isFillDownLineColor = true;
        xStart = 0;
        yStart = 0;
        xEnd = 40;
        yEnd = 100;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpectMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpectSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpectMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpectSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpectMode == MeasureSpec.AT_MOST
                && heightSpectMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mWidth, mHeight);
        } else if (widthSpectMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mWidth, heightSpectSize);
        } else if (heightSpectMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpectSize, mHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.setDrawFilter(drawFilter);
        drawCoordinate(canvas);
        drawPoint(canvas);
    }

    /**
     * 画坐标系
     *
     * @param canvas
     */
    private void drawCoordinate(Canvas canvas) {
        Rect rect = new Rect();
        textPaint.getTextBounds("300", 1, 3, rect);
        // 所画的坐标系的原点位置
        int startX = getPaddingLeft() + rect.width();
        int startY = mHeight - rect.height() - getPaddingBottom();
        // X轴的长度
        int lengthX = mWidth - getPaddingRight() - startX;
        // Y轴的长度
        int lengthY = startY - getPaddingTop();
        float countX, countY;
        countX = xEnd - xStart;
        countY = yEnd - yStart;
        // x轴每个刻度的长度
        perLengthX = 1.0f * lengthX / countX - 1;
        // y轴每个刻度的长度
        perLengthY = 1.0f * lengthY / countY;
        paint.setColor(Color.parseColor("#000000"));
        // 画x轴的刻度
        textPaint.setTextSize(18);
    }

    private void drawPoint(Canvas canvas) {
        // 用于保存y值大于compareValue的值
        if (xValues != null) {
            float[] storageX = new float[xValues.length];
            float[] storageY = new float[xValues.length];
            Rect rect = new Rect();
            textPaint.getTextBounds("300", 0, 3, rect);
            int startX = getPaddingLeft() + rect.width();
            int startY = mHeight - rect.height() - getPaddingBottom();
            linePaint = new Paint();
            linePaint.setColor(Color.parseColor("#59D3FF"));
            if (!isFillDownLineColor) {
                linePaint.setStyle(Paint.Style.STROKE);
            }
            Path path = new Path();
            Path path2 = new Path();
            path.moveTo(startX + (xValues[0] - xStart) * perLengthX, startY - (yValues[0] - yStart) * perLengthY * fraction);

            int count = xValues.length;
            for (int i = 0; i < count - 1; i++) {
                float x, y, x2, y2, x3, y3, x4, y4;
                x = startX + (xValues[i] - xStart) * perLengthX;
                x4 = (startX + (xValues[i + 1] - xStart) * perLengthX);
                x2 = x3 = (x + x4) / 2;
                // 乘以这个fraction是为了添加动画特效
                y = startY - (yValues[i] - yStart) * perLengthY * fraction;
                y4 = startY - (yValues[i + 1] - yStart) * perLengthY * fraction;
                y2 = y;
                y3 = y4;
//                if (yValues[i] > compareValue) {
//                    storageX[i] = x;
//                    storageY[i] = y;
//                }
                if (!isFillDownLineColor && i == 0) {
                    path2.moveTo(x, y);
                    path.moveTo(x, y);
                    continue;
                }
                // 填充颜色
                if (isFillDownLineColor && i == 0) {
                    // 形成封闭的图形
                    path2.moveTo(x, y);
                    path.moveTo(x, startY);
                    path.lineTo(x, y);
                }

                // 填充颜色
                if (isFillDownLineColor && i == count - 1) {
                    path.lineTo(x, startY);
                }
                path.cubicTo(x2, y2, x3, y3, x4, y4);
                path2.cubicTo(x2, y2, x3, y3, x4, y4);
            }
            if (isFillDownLineColor) {
                // 形成封闭的图形
                path.lineTo(startX + (xValues[count - 1] - xStart) * perLengthX, startY);
            }
            Paint rectPaint = new Paint();
            rectPaint.setColor(Color.BLUE);
            float left = startX + (xValues[0] - xStart) * perLengthX;
            float top = getPaddingTop();
            float right = startX + (xValues[count - 1] - xStart) * perLengthX;
            float bottom = startY;
            // 渐变的颜色
            LinearGradient lg = new LinearGradient(left, top, left, bottom, Color.parseColor("#00ffffff"),
                    Color.parseColor("#FFffffff"), Shader.TileMode.MIRROR);// CLAMP重复最后一个颜色至最后
            rectPaint.setShader(lg);
            rectPaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_ATOP));
            if (isFillDownLineColor) {
                canvas.drawPath(path, linePaint);
            }
            canvas.drawRect(left, top, right, bottom, rectPaint);
            // canvas.restoreToCount(layerId);
            rectPaint.setXfermode(null);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth((float) 2.3);
            linePaint.setColor(Color.parseColor("#E059D3FF"));//设置曲线的颜色的
            canvas.drawPath(path2, linePaint);
            linePaint.setPathEffect(null);
            drawDashAndPoint(storageX, storageY, startY, canvas);
            if (!stop)
                performAnimator();
            if (fraction > 0.99) {
                performAnimator();
            }
        }
    }


    private void drawDashAndPoint(float[] x, float[] y, float startY, Canvas canvas) {
        PathEffect pe = new DashPathEffect(new float[]{10, 10}, 1);
        // 要设置不是填充的，不然画一条虚线是没显示出来的
        dashPaint.setStyle(Paint.Style.STROKE);
        dashPaint.setPathEffect(pe);
        dashPaint.setColor(Color.BLACK);
        Paint pointPaint = new Paint();
        pointPaint.setColor(Color.BLACK);
        for (int i = 0; i < x.length; i++) {
            if (y[i] > 1) {
                canvas.drawCircle(x[i], y[i], 11, pointPaint);
                Path path = new Path();
                path.moveTo(x[i], startY);
                path.lineTo(x[i], y[i]);
                canvas.drawPath(path, dashPaint);
            }
        }

    }

    public void performAnimator() {
        if (numCount >= 1)
            return;
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        numCount++;
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fraction = (Float) animation.getAnimatedValue();
                stop = true;
                postInvalidate();
            }
        });
        va.setDuration(1000);
        va.start();
    }


    public static class StepsChartBuilder {
        private static StepsChatView curveChart;
        private static StepsChartBuilder cBuilder;

        private StepsChartBuilder() {
        }

        public static StepsChartBuilder createBuilder(StepsChatView curve) {
            curveChart = curve;
            //第一个参数决定了位置第一个起始位置显示的数字
            curveChart.setxStart(1.06f, 8.5f);
            curveChart.setyStart(0, .80f);
            synchronized (StepsChartBuilder.class) {
                if (cBuilder == null) {
                    cBuilder = new StepsChartBuilder();
                }
            }
            return cBuilder;
        }

        public static StepsChartBuilder setXYValues(float[] xValues, float[] yValues) {
            curveChart.setxValues(xValues);
            curveChart.setyValues(yValues);
            return cBuilder;
        }


    }

    public void setxValues(float[] xValues) {
        this.xValues = xValues;
    }

    public void setyValues(float[] yValues) {
        this.yValues = yValues;
    }

    private void setyStart(float yStart, float yEnd) {

    }

    private void setxStart(float xStart, float xEnd) {
        this.xStart = xStart;
        this.xEnd = xEnd;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
