package com.cqkct.FunKidII.Ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewParent;

import com.cqkct.FunKidII.R;

import java.util.List;

public class TextMarkSeekBar extends AppCompatSeekBar {

    public TextMarkSeekBar(Context context) {
        super(context);
        initAttr(context, null);
        init(context);
    }

    public TextMarkSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
        init(context);
    }

    public TextMarkSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        init(context);
    }

    private boolean mShowMarkText;
    @Nullable
    private String[] mMarkTextArray;
    @ColorInt
    private int mProgressMarkTextColor;
    @ColorInt
    private int mUnprogressMarkTextColor;
    @ColorInt
    private int mThumbMarkTextColor;
    private int mProgressMarkTextSize;
    private int mUnprogressMarkTextSize;
    private int mThumbMarkTextSize;

    private void initAttr(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TextMarkSeekBar);

        mShowMarkText = typedArray.getBoolean(R.styleable.TextMarkSeekBar_showMarkText, true);

        TypedValue value = new TypedValue();
        if (typedArray.getValue(R.styleable.TextMarkSeekBar_markTextArray, value)) {
            if (value.type == TypedValue.TYPE_REFERENCE) {
                CharSequence[] charSequences = context.getResources().getTextArray(value.resourceId);
                if (charSequences.length != 0) {
                    mMarkTextArray = new String[charSequences.length];
                    for (int i = 0; i < charSequences.length; ++i) {
                        mMarkTextArray[i] = charSequences[i].toString();
                    }
                }
            } else {
                String str = typedArray.getString(R.styleable.TextMarkSeekBar_markTextArray);
                if (!TextUtils.isEmpty(str)) {
                    mMarkTextArray = str.split(",|;");
                }
            }
        }

        mProgressMarkTextColor = typedArray.getColor(R.styleable.TextMarkSeekBar_progressMarkTextColor, Color.WHITE);
        mUnprogressMarkTextColor = typedArray.getColor(R.styleable.TextMarkSeekBar_unprogressMarkTextColor, Color.WHITE);
        mThumbMarkTextColor = typedArray.getColor(R.styleable.TextMarkSeekBar_thumbMarkTextColor, Color.WHITE);

        mProgressMarkTextSize = typedArray.getDimensionPixelSize(R.styleable.TextMarkSeekBar_progressMarkTextSize, getDimensionPixelSize(TypedValue.COMPLEX_UNIT_SP, 12));
        mUnprogressMarkTextSize = typedArray.getDimensionPixelSize(R.styleable.TextMarkSeekBar_unprogressMarkTextSize, getDimensionPixelSize(TypedValue.COMPLEX_UNIT_SP, 12));
        mThumbMarkTextSize = typedArray.getDimensionPixelSize(R.styleable.TextMarkSeekBar_thumbMarkTextSize, getDimensionPixelSize(TypedValue.COMPLEX_UNIT_SP, 15));

        typedArray.recycle();
    }

    private int getDimensionPixelSize(int unit, int value) {
        float f = TypedValue.applyDimension(
                unit,
                value,
                getResources().getDisplayMetrics());
        int res = (int) ((f >= 0) ? (f + 0.5f) : (f - 0.5f));
        if (res != 0) return res;
        return Integer.compare(value, 0);
    }

    private Paint mProgressMarkTextPaint = new Paint();
    private Paint mUnprogressMarkTextPaint = new Paint();
    private Paint mThumbMarkTextPaint = new Paint();

    private void init(Context context) {
        mProgressMarkTextPaint.setAntiAlias(true);
        mProgressMarkTextPaint.setColor(mProgressMarkTextColor);
        mProgressMarkTextPaint.setTextSize(mProgressMarkTextSize);

        mUnprogressMarkTextPaint.setAntiAlias(true);
        mUnprogressMarkTextPaint.setColor(mUnprogressMarkTextColor);
        mUnprogressMarkTextPaint.setTextSize(mUnprogressMarkTextSize);

        mThumbMarkTextPaint.setAntiAlias(true);
        mThumbMarkTextPaint.setColor(mThumbMarkTextColor);
        mThumbMarkTextPaint.setTextSize(mThumbMarkTextSize);

        if (mMarkTextArray != null && mMarkTextArray.length != 0) {
            super.setMax(mMarkTextArray.length - 1);
        }

        // Api21及以上调用，去掉滑块后面的背景
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setSplitTrack(false);
        }
    }

    public void setShowMarkText(boolean showText) {
        mShowMarkText = showText;
        postInvalidate();
    }

    public boolean isShowMarkText() {
        return mShowMarkText;
    }

    public synchronized void setMarkTextArray(@Nullable String[] textArray) {
        mMarkTextArray = textArray;
        if (mMarkTextArray != null && mMarkTextArray.length != 0) {
            setMax(mMarkTextArray.length - 1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setMin(0);
            }
        }
        postInvalidate();
    }

    public void setMarkTextArray(@Nullable List<String> textList) {
        if (textList == null) {
            setMarkTextArray((String[]) null);
        } else {
            setMarkTextArray(textList.toArray(new String[]{}));
        }
    }

    @Override
    public synchronized void setMax(int max) {
        mMarkTextArray = null;
        super.setMax(max);
        postInvalidate();
    }

    public @Nullable String[] getMarkTextArray() {
        return mMarkTextArray;
    }

    public void setProgressMarkTextColor(@ColorInt int color) {
        mProgressMarkTextColor = color;
        mProgressMarkTextPaint.setColor(color);
        postInvalidate();
    }

    public @ColorInt int getProgressMarkTextColor() {
        return mProgressMarkTextColor;
    }

    public void setUnprogressMarkTextColor(@ColorInt int color) {
        mUnprogressMarkTextColor = color;
        mUnprogressMarkTextPaint.setColor(color);
        postInvalidate();
    }

    public @ColorInt int getUnprogressMarkTextColor() {
        return mUnprogressMarkTextColor;
    }

    public void setThumbMarkTextColor(@ColorInt int color) {
        mThumbMarkTextColor = color;
        mThumbMarkTextPaint.setColor(color);
        postInvalidate();
    }

    public @ColorInt int getThumbMarkTextColor() {
        return mThumbMarkTextColor;
    }

    public void setProgressMarkTextSize(int pixel) {
        mProgressMarkTextSize = pixel;
        mProgressMarkTextPaint.setTextSize(pixel);
        postInvalidate();
    }

    public void setProgressMarkTextSizeInSP(int sp) {
        setProgressMarkTextSize(getDimensionPixelSize(TypedValue.COMPLEX_UNIT_SP, sp));
    }

    public void setProgressMarkTextSizeInDP(int dip) {
        setProgressMarkTextSize(getDimensionPixelSize(TypedValue.COMPLEX_UNIT_DIP, dip));
    }

    public int getProgressMarkTextSize() {
        return mProgressMarkTextSize;
    }

    public void setUnprogressMarkTextSize(int pixel) {
        mUnprogressMarkTextSize = pixel;
        mUnprogressMarkTextPaint.setTextSize(pixel);
        postInvalidate();
    }

    public void setUnprogressMarkTextSizeInSP(int sp) {
        setUnprogressMarkTextSize(getDimensionPixelSize(TypedValue.COMPLEX_UNIT_SP, sp));
    }

    public void setUnprogressMarkTextSizeInDP(int dip) {
        setUnprogressMarkTextSize(getDimensionPixelSize(TypedValue.COMPLEX_UNIT_DIP, dip));
    }

    public int getUnprogressMarkTextSize() {
        return mUnprogressMarkTextSize;
    }

    public void setThumbMarkTextSize(int pixel) {
        mThumbMarkTextSize = pixel;
        mThumbMarkTextPaint.setTextSize(pixel);
        postInvalidate();
    }

    public void setThumbMarkTextSizeInSP(int sp) {
        setThumbMarkTextSize(getDimensionPixelSize(TypedValue.COMPLEX_UNIT_SP, sp));
    }

    public void setThumbMarkTextSizeInDP(int dip) {
        setThumbMarkTextSize(getDimensionPixelSize(TypedValue.COMPLEX_UNIT_DIP, dip));
    }

    public int getThumbMarkTextSize() {
        return mThumbMarkTextSize;
    }

    public synchronized String getProgressMarkText() {
        if (mMarkTextArray != null && mMarkTextArray.length != 0) {
            return mMarkTextArray[getProgress()];
        } else {
            return String.valueOf(getProgress());
        }
    }

    private void setProgressInternal(int progress, boolean fromUser, boolean animate) {
        if (getProgress() == progress) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setProgress(progress, animate);
        } else {
            setProgress(progress);
        }
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
        }
    }

    public void setProgress(@NonNull String text) {
        setProgress(text, true);
    }

    public synchronized void setProgress(@NonNull String text, boolean animate) {
        if (mMarkTextArray != null && mMarkTextArray.length != 0) {
            for (int i = 0; i < mMarkTextArray.length; ++i) {
                if (mMarkTextArray[i].equals(text)) {
                    setProgressInternal(i, false, animate);
                    break;
                }
            }
        } else {
            try {
                setProgressInternal(Integer.parseInt(text), false, animate);
            } catch (Exception e) {
                Log.d("TextMarkSeekBar", "setProgress(" + text + ")", e);
            }
        }
    }

    private int mPaddingLeft;
    private int mPaddingRight;
    private int mPaddingTop;
    private Rect mTrackBounds;
    private Rect mThumbBounds;

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        mPaddingTop = getPaddingTop();
        mTrackBounds = getProgressDrawable().getBounds();
        mThumbBounds = getThumb().getBounds();
    }

    private Rect textBounds = new Rect();

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!mShowMarkText)
            return;

        final int progress = getProgress();

        final int max = getMax();
        final int min = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ? getMin() : 0;
        final int range = max - min;

        final float pieceWidth = (mTrackBounds.width() * 1.0f - (mThumbBounds.width() - getThumbOffset() * 2)) / range;
        final float textXOffset = mPaddingLeft + mTrackBounds.left + mThumbBounds.width() * 0.5f - getThumbOffset();
        for (int i = 0; i <= range; i++) {
            final int textIdx = isLayoutRtl() && mMirrorForRtl ? range - i : i;
            final int progressIdx = textIdx + min;
            if (progressIdx == progress) {
                continue;
            }
            final Paint textPaint = progressIdx < progress ? mProgressMarkTextPaint : mUnprogressMarkTextPaint;
            final String text = mMarkTextArray != null && mMarkTextArray.length != 0 ? mMarkTextArray[textIdx] : String.valueOf(progressIdx);
            float textX = textXOffset + i * pieceWidth;
            float textY = mPaddingTop + mTrackBounds.top + mTrackBounds.height() * 0.5f;
            textPaint.getTextBounds(text, 0, text.length(), textBounds);
            textX -= textBounds.width() * 0.5f;
            textY += textBounds.height() * 0.5f;
            canvas.drawText(text, textX, textY, textPaint);
        }

        final float thumbX = mPaddingLeft + mThumbBounds.left - getThumbOffset();
        String text = mMarkTextArray != null && mMarkTextArray.length != 0 ? mMarkTextArray[progress] : String.valueOf(progress);
        float textX = thumbX + mThumbBounds.width() * 0.5f;
        float textY = getHeight() * 0.5f;
        mThumbMarkTextPaint.getTextBounds(text, 0, text.length(), textBounds);
        textX -= textBounds.width() * 0.5f;
        textY += textBounds.height() * 0.5f;
        canvas.drawText(text, textX, textY, mThumbMarkTextPaint);
    }

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    private boolean isInScrollingContainer;
    boolean mMirrorForRtl = false;
    float mTouchProgressOffset;
    private int mScaledTouchSlop;
    private float mTouchDownX;
    private boolean mIsDragging;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInScrollingContainer()) {
                    mTouchDownX = event.getX();
                } else {
                    startDrag(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        startDrag(event);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }
                // ProgressBar doesn't know to repaint the thumb drawable
                // in its inactive state when the touch stops (because the
                // value has not apparently changed)
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    public boolean isInScrollingContainer() {
        return isInScrollingContainer;
    }

    public void setInScrollingContainer(boolean isInScrollingContainer) {
        this.isInScrollingContainer = isInScrollingContainer;
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);

        Drawable mThumb = getThumb();
        if (mThumb != null) {
            // This may be within the padding region.
            invalidate(mThumb.getBounds());
        }

        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    private void onStartTrackingTouch() {
        mIsDragging = true;
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    private void onStopTrackingTouch() {
        mIsDragging = false;
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        final int x = Math.round(event.getX());
        final int y = Math.round(event.getY());
        setHotspot(x, y);
        setProgressInternal(calcProgress2(x, y),  true, false);
    }

    private int calcProgress(final int x, final int y) {
        final int width = getWidth();
        final int availableWidth = width - mPaddingLeft - mPaddingRight;

        final float scale;
        float progress = 0.0f;

        if (isLayoutRtl() && mMirrorForRtl) {
            if (x > width - mPaddingRight) {
                scale = 0.0f;
            } else if (x < mPaddingLeft) {
                scale = 1.0f;
            } else {
                scale = (availableWidth - x + mPaddingLeft) / (float) availableWidth;
                progress = mTouchProgressOffset;
            }
        } else {
            if (x < mPaddingLeft) {
                scale = 0.0f;
            } else if (x > width - mPaddingRight) {
                scale = 1.0f;
            } else {
                scale = (x - mPaddingLeft) / (float) availableWidth;
                progress = mTouchProgressOffset;
            }
        }

        int range = getMax();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            range -= getMin();
        }
        progress += scale * range;

        return Math.round(progress);
    }

    private int calcProgress2(final int x, final int y) {
        final int max = getMax();
        final int min = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ? getMin() : 0;
        final int range = max - min;

        final int width = getWidth();

        final float pieceWidth = (mTrackBounds.width() * 1.0f - (mThumbBounds.width() - getThumbOffset() * 2)) / range;
        final float xOffset = mPaddingLeft + mTrackBounds.left + mThumbBounds.width() * 0.5f - getThumbOffset();

        if (isLayoutRtl() && mMirrorForRtl) {
            if (x > width - mPaddingRight) {
                return max;
            } else if (x < mPaddingLeft) {
                return min;
            } else {
                // FIXME: 方向错误
                for (int i = 0; i <= range; i++) {
                    float X = xOffset + i * pieceWidth;
                    if (x > X - pieceWidth / 2 && x < X + pieceWidth / 2) {
                        return i + min;
                    }
                }
                return min;
            }
        } else {
            if (x < mPaddingLeft) {
                return min;
            } else if (x > width - mPaddingRight) {
                return max;
            } else {
                // FIXME: 更好的算法
                for (int i = 0; i <= range; i++) {
                    float X = xOffset + i * pieceWidth;
                    if (x > X - pieceWidth / 2 && x < X + pieceWidth / 2) {
                        return i + min;
                    }
                }
                return max;
            }
        }
    }

    private boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    private void setHotspot(float x, float y) {
        final Drawable bg = getBackground();
        if (bg != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bg.setHotspot(x, y);
            }
        }
    }

    private void attemptClaimDrag() {
        ViewParent mParent = getParent();
        if (mParent != null) {
            mParent.requestDisallowInterceptTouchEvent(true);
        }
    }
}