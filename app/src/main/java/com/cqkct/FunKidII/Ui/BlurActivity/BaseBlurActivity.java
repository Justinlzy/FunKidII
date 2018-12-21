package com.cqkct.FunKidII.Ui.BlurActivity;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.umeng.commonsdk.debug.E;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;

import fr.tvbarthel.lib.blurdialogfragment.BlurDialogEngine;
import fr.tvbarthel.lib.blurdialogfragment.FastBlurHelper;
import fr.tvbarthel.lib.blurdialogfragment.RenderScriptBlurHelper;

public abstract class BaseBlurActivity extends BaseActivity {

    private static final String TAG = BaseBlurActivity.class.getSimpleName();

    private float mDownScaleFactor;
    private int mBlurRadius;
    private int mAnimationDuration;
    private boolean mUseRenderScript;
    private boolean mDebugEnable;

    private BlurAsyncTask mBluringTask;

    private Bitmap mBackgroundBitmapToBlur;
    private BitmapDrawable mBackgroundBlurredBitmap;

    public static class BackgroundBitmapToBlurEv {
        @NonNull
        public final Bitmap bitmap;
        public BackgroundBitmapToBlurEv(@NonNull Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BackgroundBitmapToBlurEv ev = EventBus.getDefault().getStickyEvent(BackgroundBitmapToBlurEv.class);
        if (ev != null) {
            EventBus.getDefault().removeStickyEvent(ev);
            mBackgroundBitmapToBlur = ev.bitmap;
        }

        mAnimationDuration = getResources().getInteger(fr.tvbarthel.lib.blurdialogfragment.R.integer.blur_dialog_animation_duration);

        mBlurRadius = getBlurRadius();
        if (mBlurRadius <= 0) {
            throw new IllegalArgumentException("Blur radius must be strictly positive. Found : " + mBlurRadius);
        }
        mDownScaleFactor = getDownScaleFactor();
        if (mDownScaleFactor <= 1.0) {
            throw new IllegalArgumentException("Down scale must be strictly greater than 1.0. Found : " + mDownScaleFactor);
        }
        mUseRenderScript = isRenderScriptEnable();
        mDebugEnable = isDebugEnable();
    }

    @Override
    public void onStart() {
        // add default fade to the dialog if no window animation has been set.
        int currentAnimation = getWindow().getAttributes().windowAnimations;
        if (currentAnimation == 0) {
            getWindow().getAttributes().windowAnimations = fr.tvbarthel.lib.blurdialogfragment.R.style.BlurDialogFragment_Default_Animation;
        }

        if (mBackgroundBlurredBitmap == null && mBluringTask == null) {
            if (mBackgroundBitmapToBlur == null) {
                BaseActivity previous = getPreviousActivity();
                if (previous == null) {
                    return;
                }
                View decorView = previous.getWindow().getDecorView();
                mBackgroundBitmapToBlur = capture(decorView);
            }

            mBluringTask = new BlurAsyncTask(this);
            mBluringTask.execute();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBluringTask != null) {
            mBluringTask.cancel(true);
        }
    }

    public static Bitmap capture(View view) {
        Rect rect = new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap background = view.getDrawingCache(true);

        /**
         * After rotation, the DecorView has no height and no width. Therefore
         * .getDrawingCache() return null. That's why we  have to force measure and layout.
         */
        if (background == null) {
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(rect.height(), View.MeasureSpec.EXACTLY)
            );
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            view.destroyDrawingCache();
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache(true);
            background = view.getDrawingCache(true);
        }

        background = Bitmap.createBitmap(background);

        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);

        return background;
    }

    private static class BlurAsyncTask extends AsyncTask<Void, Void, Bitmap> {
        private WeakReference<BaseBlurActivity> mA;

        BlurAsyncTask(BaseBlurActivity a) {
            mA = new WeakReference<>(a);
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            BaseBlurActivity a = mA.get();
            if (a == null) {
                return null;
            }
            if (a.mBackgroundBitmapToBlur == null)
                return null;

            //process to the blue
            if (!isCancelled()) {
                Bitmap out = a.blur(a.mBackgroundBitmapToBlur);
                if (out != null) {
                    a.mBackgroundBitmapToBlur.recycle();
                }
                return out;
            }
            return null;
        }

        protected void onPostExecute(Bitmap bitmap) {
            BaseBlurActivity a = mA.get();
            if (a == null) {
                return;
            }
            a.mBluringTask = null;

            if (bitmap == null)
                return;

            a.mBackgroundBlurredBitmap = new BitmapDrawable(a.getResources(), bitmap);

            View decorView = a.getWindow().getDecorView();
            decorView.setBackground(a.mBackgroundBlurredBitmap);

            a.mBluringTask = null;
        }
    }

    private Bitmap blur(Bitmap bkg) {
        long startMs = System.currentTimeMillis();

        //overlay used to build scaled preview and blur background
        Bitmap overlay = null;

        // evaluate bottom or right offset due to navigation bar.
        int bottomOffset = 0;
        int rightOffset = 0;
        final int navBarSize = getNavigationBarOffset();

        View contentView = findViewById(android.R.id.content);
        if (bkg.getHeight() - contentView.getHeight() > 2) {
            bottomOffset = navBarSize;
        } else if (bkg.getWidth() - contentView.getWidth() > 2) {
            rightOffset = navBarSize;
        }

        //add offset to the source boundaries since we don't want to blur actionBar pixels
        Rect srcRect = new Rect(
                0,
                0,
                bkg.getWidth() - rightOffset,
                bkg.getHeight() - bottomOffset
        );

        //in order to keep the same ratio as the one which will be used for rendering, also
        //add the offset to the overlay.
        double height = Math.ceil((bkg.getHeight() - bottomOffset) / mDownScaleFactor);
        double width = Math.ceil(((bkg.getWidth() - rightOffset) * height / (bkg.getHeight() - bottomOffset)));

        // Render script doesn't work with RGB_565
        if (mUseRenderScript) {
            overlay = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
        } else {
            overlay = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.RGB_565);
        }
        //scale and draw background view on the canvas overlay
        Canvas canvas = new Canvas(overlay);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);

        //build drawing destination boundaries
        final RectF destRect = new RectF(0, 0, overlay.getWidth(), overlay.getHeight());

        //draw background from source area in source background to the destination area on the overlay
        canvas.drawBitmap(bkg, srcRect, destRect, paint);

        //apply fast blur on overlay
        if (mUseRenderScript) {
            overlay = RenderScriptBlurHelper.doBlur(overlay, mBlurRadius, true, this);
        } else {
            overlay = FastBlurHelper.doBlur(overlay, mBlurRadius, true);
        }
        if (mDebugEnable) {
            String blurTime = (System.currentTimeMillis() - startMs) + " ms";
            Log.d(TAG, "Blur method : " + (mUseRenderScript ? "RenderScript" : "FastBlur"));
            Log.d(TAG, "Radius : " + mBlurRadius);
            Log.d(TAG, "Down Scale Factor : " + mDownScaleFactor);
            Log.d(TAG, "Blurred achieved in : " + blurTime);
            Log.d(TAG, "Allocation : " + bkg.getRowBytes() + "ko (screen capture) + "
                    + overlay.getRowBytes() + "ko (blurred bitmap)"
                    + (!mUseRenderScript ? " + temp buff " + overlay.getRowBytes() + "ko." : "."));
            Rect bounds = new Rect();
            Canvas canvas1 = new Canvas(overlay);
            paint.setColor(Color.BLACK);
            paint.setAntiAlias(true);
            paint.setTextSize(20.0f);
            paint.getTextBounds(blurTime, 0, blurTime.length(), bounds);
            canvas1.drawText(blurTime, 2, bounds.height(), paint);
        }

        return overlay;
    }

    private int getNavigationBarOffset() {
        int result = 0;
        Resources resources = getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    /**
     * For inheritance purpose.
     * <p/>
     * Enable or disable debug mode.
     *
     * @return true if debug mode should be enabled.
     */
    protected boolean isDebugEnable() {
        return BlurDialogEngine.DEFAULT_DEBUG_POLICY;
    }

    /**
     * For inheritance purpose.
     * <p/>
     * Allow to customize the down scale factor.
     * <p/>
     * The factor down scaled factor used to reduce the size of the source image.
     * Range :  ]1.0,infinity)
     *
     * @return customized down scaled factor.
     */
    protected float getDownScaleFactor() {
        return BlurDialogEngine.DEFAULT_BLUR_DOWN_SCALE_FACTOR;
    }

    /**
     * For inheritance purpose.
     * <p/>
     * Allow to customize the blur radius factor.
     * <p/>
     * radius down scaled factor used to reduce the size of the source image.
     * Range :  [1,infinity)
     *
     * @return customized blur radius.
     */
    protected int getBlurRadius() {
        return BlurDialogEngine.DEFAULT_BLUR_RADIUS;
    }

    /**
     * For inheritance purpose.
     * <p/>
     * Enable or disable RenderScript.
     * <p/>
     * Disable by default.
     * <p/>
     * Don't forget to add those lines to your build.gradle if your are using Renderscript
     * <pre>
     *  defaultConfig {
     *  ...
     *  renderscriptTargetApi 22
     *  renderscriptSupportModeEnabled true
     *  ...
     *  }
     * </pre>
     *
     * @return true to enable RenderScript.
     */
    protected boolean isRenderScriptEnable() {
        return BlurDialogEngine.DEFAULT_USE_RENDERSCRIPT;
    }
}
