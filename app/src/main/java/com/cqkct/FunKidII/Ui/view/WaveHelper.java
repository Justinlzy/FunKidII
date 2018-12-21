package com.cqkct.FunKidII.Ui.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

public class WaveHelper {
    private WaveView mWaveView;

    private AnimatorSet mAnimatorSet;

    private ObjectAnimator waterLevelAnim;

    public WaveHelper(WaveView waveView) {
        mWaveView = waveView;
        initAnimation();
    }

    public void start() {
        mWaveView.setShowWave(true);
        if (mAnimatorSet != null) {
            mAnimatorSet.start();
        }
    }

    private void initAnimation() {
        List<Animator> animators = new ArrayList<>();

        // horizontal animation.
        // wave waves infinitely.
        ObjectAnimator waveShiftAnim = ObjectAnimator.ofFloat(
                mWaveView, "waveShiftRatio", 0f, 1f);
        waveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
        waveShiftAnim.setDuration(1000);
        waveShiftAnim.setInterpolator(new LinearInterpolator());
        animators.add(waveShiftAnim);


        ObjectAnimator boatAnim = ObjectAnimator.ofFloat(
                mWaveView, "boatAmplitude", 0.0001f, 0.05f, 0.0001f);
        boatAnim.setRepeatCount(ValueAnimator.INFINITE);
        boatAnim.setDuration(3000);
        boatAnim.setInterpolator(new LinearInterpolator());
        animators.add(boatAnim);


        // vertical animation.
        // water level increases from 0 to center of WaveView
        waterLevelAnim = ObjectAnimator.ofFloat(
                mWaveView, "waterLevelRatio", 0f, mWaveView.getWaterLevelRatio());
        waterLevelAnim.setDuration(500);
        waterLevelAnim.setInterpolator(new DecelerateInterpolator());
        animators.add(waterLevelAnim);

        // amplitude animation.
        // wave grows big then grows small, repeatedly
        ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(
                mWaveView, "amplitudeRatio", 0.0001f, 0.05f);
        amplitudeAnim.setRepeatCount(ValueAnimator.INFINITE);
        amplitudeAnim.setRepeatMode(ValueAnimator.REVERSE);
        amplitudeAnim.setDuration(5000);
        amplitudeAnim.setInterpolator(new LinearInterpolator());
        animators.add(amplitudeAnim);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
    }

    public void cancel() {
        if (mAnimatorSet != null) {
//            mAnimatorSet.cancel();
            mAnimatorSet.end();
        }
    }

    public void setWaterLevelRatio(float waterLevelRatio) {
        mWaveView.setBoatBitmap(waterLevelRatio == 1 ? mWaveView.boatBitmap2 : mWaveView.boatBitmap1);
        waterLevelAnim.setFloatValues(mWaveView.getWaterLevelRatio(), 0.1f + 0.7f * waterLevelRatio);
        waterLevelAnim.start();
    }
}
