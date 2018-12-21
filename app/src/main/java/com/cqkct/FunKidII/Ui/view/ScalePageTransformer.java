package com.cqkct.FunKidII.Ui.view;

import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

/**
 * Created by HanHailong on 15/9/27.
 */
public class ScalePageTransformer implements ViewPager.PageTransformer {

    public static final float MAX_SCALE = 1.0f;
    public static final float MIN_SCALE = 0.8f;

    @Override
    public void transformPage(View page, float position) {
        if (true) {

            float scale = 1f - ((float) (0.2 * Math.abs(position)));
            // 不写下面的if，得到的是多级缩放，写了之后，是单级缩放
            if (false) {
                if (scale < 0.8f) {
                    scale = 0.8f;
                }
            }
            page.setScaleY(scale);
            page.setScaleX(scale);

        } else {

            float normalizedPosition = position;
            if (position < -1) {
                normalizedPosition = -1;
            } else if (position > 1) {
                normalizedPosition = 1;
            }

            float tempScale = normalizedPosition < 0 ? 1 + normalizedPosition : 1 - normalizedPosition;

            float slope = (MAX_SCALE - MIN_SCALE) / 1;
            //一个公式
            float scaleValue = MIN_SCALE + tempScale * slope;

            if (false) {
                Object object = page.getTag();
                if (object instanceof ClipViewTagData) {
                    ClipViewTagData data = (ClipViewTagData) object;
                    Log.e("ScalePageTransformer", "---------- [" + data.position + "]: scaleValue: " + scaleValue + ", position: " + position);
                    if (data.entity.getIs_select()) {
                        scaleValue = MAX_SCALE;
                    } else {
                        scaleValue = MIN_SCALE;
                    }
                }
            }

            page.setScaleX(scaleValue);
            page.setScaleY(scaleValue);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            page.getParent().requestLayout();
        }
    }
}