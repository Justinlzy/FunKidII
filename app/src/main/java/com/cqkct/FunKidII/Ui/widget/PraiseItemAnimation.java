package com.cqkct.FunKidII.Ui.widget;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;


public class PraiseItemAnimation extends Animation {
    //缩放的中心点，设置为图片的中心即可
    //缩放的中心点，设置为图片的中心即可
    private int mCenterWidth;
    private int mCenterHeight;
    private Camera mCamera = new Camera();


    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        setDuration(800);// 设置默认时长
        setFillAfter(false);// 动画结束后保留状态
        setInterpolator(new AccelerateInterpolator());// 设置插值器

        mCenterWidth = width / 2;
        mCenterHeight = height / 2;
    }

    // 暴露接口-设置旋转角度
    public static void setRotateY(float rotateY) {
        float mRotateY = rotateY;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        final Matrix matrix = t.getMatrix();
        matrix.preTranslate(0, 1500 * interpolatedTime);
    }

/*    @Override
    protected void applyTransformation( float interpolatedTime, Transformation t) {
        final Matrix matrix = t.getMatrix();
        mCamera.save();
        mCamera.rotateZ(mRotateY * interpolatedTime);// 使用Camera设置旋转的角度
        Log.d("okc","time="+interpolatedTime);
        mCamera.getMatrix(matrix);// 将旋转变换作用到matrix上
        mCamera.restore();
        // 通过pre方法设置矩阵作用前的偏移量来改变旋转中心
        matrix.preTranslate(mCenterWidth, mCenterHeight);
        matrix.postTranslate(-mCenterWidth, -mCenterHeight);
    }*/

}


