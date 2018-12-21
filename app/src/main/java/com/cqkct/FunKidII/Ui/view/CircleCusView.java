package com.cqkct.FunKidII.Ui.view;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import com.cqkct.FunKidII.R;


/**
 * Created by Administrator on 2018/1/8.
 */

public class CircleCusView extends View {
    private Paint fillArcPaint;
    // 设置光源的方向
    private float[] direction = new float[]{1, 1, 1};

    // 设置环境光亮度
    private float light = 0.4f;

    // 选择要应用的反射等级
    private float specular = 6;
    private RectF oval;
    private BlurMaskFilter mBlur;
    // view重绘的标记
    private boolean reset = false;
    // 向 mask应用一定级别的模糊
    private float blur = 3.5f;
    private int arcradus = 30;
    //初始化进度
    private int progress = 0;
    //设置进度最大值
    private int max = 100;
    private Paint mDefaultWheelPaint;// 绘制底部灰色圆圈的画笔
    private Paint stepPaint;
    private Paint goalPaint;

    private int stepSize;
    private int goalSize;
    int[] arcColors = new int[]{
            Color.parseColor("#E059D3FF"),
            Color.parseColor("#59D3FF"),
            Color.parseColor("#99ccff"),
    };


    public CircleCusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
        oval = new RectF();
        mBlur = new BlurMaskFilter(20, BlurMaskFilter.Blur.NORMAL);
    }

    //初始化画笔操作
    private void initPaint() {
        stepSize = dip2px(getContext(), 20);
        goalSize = dip2px(getContext(), 15);
        fillArcPaint = new Paint();
        // 设置是否抗锯齿
        fillArcPaint.setAntiAlias(true);
        // 帮助消除锯齿
        fillArcPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        // 设置中空的样式
        fillArcPaint.setStyle(Paint.Style.STROKE);
        fillArcPaint.setDither(true);
        fillArcPaint.setStrokeJoin(Paint.Join.ROUND);

        stepPaint = new Paint();
        stepPaint.setColor(Color.BLACK);
        stepPaint.setAntiAlias(true);
        stepPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        stepPaint.setTextAlign(Paint.Align.LEFT);
        stepPaint.setTextSize(stepSize);

        goalPaint = new Paint();
        goalPaint.setColor(Color.BLACK);
        goalPaint.setAntiAlias(true);
        goalPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        goalPaint.setTextAlign(Paint.Align.LEFT);
        goalPaint.setTextSize(goalSize);

        // 绘制底部灰色圆圈的画笔
        mDefaultWheelPaint = new Paint();
        mDefaultWheelPaint.setAntiAlias(true);
        mDefaultWheelPaint.setColor(getResources().getColor(R.color.common_Background));
        mDefaultWheelPaint.setStyle(Paint.Style.STROKE);
        mDefaultWheelPaint.setStrokeWidth(arcradus);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (reset) {
            canvas.drawColor(Color.TRANSPARENT);
            reset = false;
        }
        drawCircle(canvas);
    }

    private void drawCircle(Canvas canvas) {
        int height = getMeasuredWidth();
        int width = getMeasuredWidth();
        //半径 = 宽/2-圆环的宽度
        int radius = width / 5 - arcradus;
        canvas.drawCircle(width / 2, height / 5, radius,
                mDefaultWheelPaint);
        // 环形颜色填充
        SweepGradient sweepGradient =
                new SweepGradient(width / 2, height / 5, arcColors, null);
        fillArcPaint.setShader(sweepGradient);
        // 设置画笔为白色

        // 模糊效果
        fillArcPaint.setMaskFilter(mBlur);
        // 设置线的类型,边是圆的
        fillArcPaint.setStrokeCap(Paint.Cap.ROUND);

        //设置圆弧的宽度
        fillArcPaint.setStrokeWidth(arcradus + 1);
        // 确定圆弧的绘制位置，也就是里面圆弧坐标和外面圆弧坐标
        oval.set(width / 2 - radius, height / 5 - radius, width
                / 2 + radius, height / 5 + radius);
        // 画圆弧，第二个参数为：起始角度，第三个为跨的角度，第四个为true的时候是实心，false的时候为空心
        canvas.drawArc(oval,
                0,
                ((float) progress / max) * 360,
                false,
                fillArcPaint);

        canvas.drawText("2000步", (width / 2) - (radius/2) , height / 6, stepPaint);
        canvas.drawText("目标步数：2000", (width / 2) - radius + arcradus + 5, height / 4, goalPaint);
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        this.invalidate();
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
