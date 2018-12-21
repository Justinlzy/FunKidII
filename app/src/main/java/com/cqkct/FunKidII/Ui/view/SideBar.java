package com.cqkct.FunKidII.Ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.DensityUtils;
import com.cqkct.FunKidII.Utils.Digest;

public class SideBar extends View {


    // 触摸事件
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
    // 26个字母
    public static String[] A_Z = {"A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z", "#"};
    private int choose = -1;// 选中
    private Paint paint = new Paint();
    private TextView mTextDialog;

    int height = getHeight();// 获取对应高度
    int width = getWidth(); // 获取对应宽度
    int singleHeight = height / A_Z.length - 2;// 获取每一个字母的高度  (这里-2仅仅是为了好看而已)
    private Paint bg_paint = new Paint();
    Resources resources = this.getResources();

    /**
     * 为SideBar设置显示字母的TextView
     *
     * @param mTextDialog
     */
    public void setTextView(TextView mTextDialog) {
        this.mTextDialog = mTextDialog;
    }


    public SideBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SideBar(Context context) {
        super(context);
    }

    /**
     * 重写这个方法
     */
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 获取焦点改变背景颜色.
        int height = getHeight();// 获取对应高度
        int width = getWidth(); // 获取对应宽度
        int singleHeight = height / A_Z.length - 2;// 获取每一个字母的高度  (这里-2仅仅是为了好看而已)

//        RectF oval = new RectF(xPos, height, screenWidth, height + singleHeight);

        for (int i = 0; i < A_Z.length; i++) {
            paint.setColor(resources.getColor(R.color.text_color_three));  //设置字体颜色
            paint.setTypeface(Typeface.DEFAULT);  //设置字体
            paint.setAntiAlias(true);  //设置抗锯齿
            paint.setTextSize(30);  //设置字母字体大小
            // x坐标等于中间-字符串宽度的一半.
            float xPos = width / 2 - paint.measureText(A_Z[i]) / 2;
            float yPos = singleHeight * i + singleHeight;

            // 选中的状态
            if (i == choose) {
                paint.setColor(resources.getColor(R.color.white));  //选中的字母改变颜色
                paint.setFakeBoldText(true);  //设置字体为粗体

                //椭圆背景 paint
                bg_paint.setColor(resources.getColor(R.color.main_color_tone));  //颜色
                bg_paint.setAntiAlias(true);

                float top = yPos - singleHeight + DensityUtils.dp2px(getContext(), 2);
                float bottom = yPos + singleHeight / 2;
                float left = 0 + DensityUtils.dp2px(getContext(), 3);
                float right = getWidth() - DensityUtils.dp2px(getContext(), 3);

                canvas.drawRoundRect(new RectF(left, top, right, bottom), 30, 30, bg_paint);
            }

            canvas.drawText(A_Z[i], xPos, yPos, paint);  //绘制所有的字母


            paint.reset();// 重置画笔
            bg_paint.reset();
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float y = event.getY();// 点击y坐标
        final int oldChoose = choose;
        final OnTouchingLetterChangedListener listener = onTouchingLetterChangedListener;
        final int c = (int) (y / getHeight() * A_Z.length);// 点击y坐标所占总高度的比例*b数组的长度就等于点击b中的个数.

        switch (action) {
            case MotionEvent.ACTION_UP:
                setBackgroundDrawable(new ColorDrawable(0x00000000));
                choose = -1;//
                invalidate();
                if (mTextDialog != null) {
                    mTextDialog.setVisibility(View.INVISIBLE);
                }
                break;

            default:
                setBackgroundResource(R.drawable.sidebar_background);
                if (oldChoose != c) {  //判断选中字母是否发生改变
                    if (c >= 0 && c < A_Z.length) {
                        if (listener != null) {
                            listener.onTouchingLetterChanged(A_Z[c]);
                        }
                        if (mTextDialog != null) {
                            mTextDialog.setText(A_Z[c]);
                            mTextDialog.setVisibility(View.VISIBLE);
                        }

                        choose = c;
                        invalidate();
                    }
                }

                break;
        }
        return true;
    }

    /**
     * 向外公开的方法
     *
     * @param onTouchingLetterChangedListener
     */
    public void setOnTouchingLetterChangedListener(
            OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    /**
     * 接口
     *
     * @author coder
     */
    public interface OnTouchingLetterChangedListener {
        void onTouchingLetterChanged(String s);
    }

}