package com.cqkct.FunKidII.Ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.cqkct.FunKidII.R;

public class DialogTitle extends LinearLayout {

    private Paint mPaint;
    private Path mPath;
    private String title;
    private int titleSize = 15;
    private int titleColor = 0xEEEEEE;
    private Paint mTextPaint;


    private float radianGrade = 0.8f; //弧度

    public DialogTitle(Context context) {
        super(context);
    }

    public DialogTitle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DialogTitle(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init(Context context, AttributeSet attrs) {


        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DialogTitle);
        title = typedArray.getString(R.styleable.DialogTitle_title);
        titleSize = (int) typedArray.getDimension(R.styleable.DialogTitle_titleSize, titleSize);
        titleColor = typedArray.getInteger(R.styleable.DialogTitle_titleColor, titleColor);
        radianGrade = typedArray.getFloat(R.styleable.DialogTitle_radian, 1);
        typedArray.recycle();
        mPaint = new Paint();
        mPaint.setColor(0xEEEEEE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(2);
        mPaint.setAntiAlias(true);
        mPath = new Path();

        mTextPaint = new Paint();
        mTextPaint.setColor(titleColor);
        mTextPaint.setStrokeWidth(1);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(titleSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        int width = getWidth();
        int height = getHeight();

        // 移动起点至[100,100]
        mPath.moveTo(0, height);

        // 连接路径到点
        mPath.quadTo(width * 0.5f, height * radianGrade, width, height);
        canvas.drawPath(mPath, mPaint);
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        if (!TextUtils.isEmpty(title))
            canvas.drawText(title, width / 2, height * radianGrade * 0.5f - (fm.descent - (-fm.ascent + fm.descent) / 2), mTextPaint);
    }
}
