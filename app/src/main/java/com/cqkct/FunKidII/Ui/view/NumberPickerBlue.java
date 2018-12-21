package com.cqkct.FunKidII.Ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

import com.cqkct.FunKidII.R;

import java.lang.reflect.Field;

/**
 * 由于在换字体颜色时会出现 无选择字体颜色  和 选择回滚问题
 */

public class NumberPickerBlue extends NumberPicker {
    private Context mContext;
    private NumberPicker picker;


    public NumberPickerBlue(Context context) {
        super(context);
        picker = this;
        mContext = context;
    }

    public NumberPickerBlue(Context context, AttributeSet attrs) {
        super(context, attrs);
        picker = this;
        mContext = context;
    }

    public NumberPickerBlue(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        picker = this;
        mContext = context;
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        updateView(child);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        updateView(child);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);
        updateView(child);
    }

    private void updateView(View view) {
        if (view instanceof EditText) {
            ((EditText) view).setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
            ((EditText) view).setTextColor(Color.parseColor("#11CBD8"));
        }
    }

    private int mRight;
    private int mLeft;
    private int[] mSelectorIndices;
    private SparseArray<String> mSelectorIndexToStringCache;
    private EditText mInputText;
    private Paint mSelectorWheelPaint;
    private int mSelectorElementHeight;
    private int mCurrentScrollOffset;
    private boolean mHasSelectorWheel;
    private boolean mHideWheelUntilFocused;

    /**
     * 通过反射获取值
     */
    private void getMyValue() {
        mLeft = super.getLeft();
        mRight = super.getRight();
        Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorWheelPaint")) {
                try {
                    mSelectorWheelPaint = (Paint) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorElementHeight")) {
                try {
                    mSelectorElementHeight = (int) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mCurrentScrollOffset")) {
                try {
                    mCurrentScrollOffset = (int) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mInputText")) {
                try {
                    mInputText = (EditText) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorIndexToStringCache")) {
                try {
                    mSelectorIndexToStringCache = (SparseArray<String>) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mSelectorIndices")) {
                try {
                    mSelectorIndices = (int[]) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mHasSelectorWheel")) {
                try {
                    mHasSelectorWheel = (boolean) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        for (Field field : pickerFields) {
            field.setAccessible(true);
            if (field.getName().equals("mHideWheelUntilFocused")) {
                try {
                    mHideWheelUntilFocused = (boolean) field.get(picker);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        getMyValue();
        mSelectorWheelPaint.setColor(Color.BLUE);

        if (!mHasSelectorWheel) {
            super.onDraw(canvas);
            return;
        }
        final boolean showSelectorWheel = !mHideWheelUntilFocused || hasFocus();
        float x = (mRight - mLeft) / 2;
        float y = mCurrentScrollOffset;


        int[] selectorIndices = mSelectorIndices;
        for (int i = 0; i < selectorIndices.length; i++) {
            int selectorIndex = selectorIndices[i];
            String scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex);
            if (i != 1) {
                //其他
                mSelectorWheelPaint.setColor(getResources().getColor(R.color.text_color_four));
                mSelectorWheelPaint.setTextSize(sp2px(20));
            } else {
                //选中
                mSelectorWheelPaint.setColor(Color.parseColor("#11CBD8"));
                mSelectorWheelPaint.setTextSize(sp2px(30));
            }

            if ((showSelectorWheel && i != 1) ||
                    (i == 1 && mInputText.getVisibility() != VISIBLE)) {
                Rect mRect = new Rect();
                mSelectorWheelPaint.getTextBounds(scrollSelectorValue, 0, scrollSelectorValue.length(), mRect);
                canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint);
            }
            y += mSelectorElementHeight;
        }

    }

    private int sp2px(int sp) {
        float scale = mContext.getResources().getDisplayMetrics().scaledDensity;
        return (int) (scale * sp + 0.5f);
    }
}
