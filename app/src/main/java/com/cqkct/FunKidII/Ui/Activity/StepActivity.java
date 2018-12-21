package com.cqkct.FunKidII.Ui.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.view.CircleCusView;
import com.cqkct.FunKidII.Ui.view.StepsChatView;

/**
 * Created by Administrator on 2018/1/8.
 */

public class StepActivity extends BaseActivity {
    float[] xValues = new float[]{1 , 2, 3 , 4 , 5, 6, 7,  8,  9,10,11,12,13,14,15,16,17,18,19};
    float[] yValues = new float[]{13, 11, 22, 45, 44, 56, 78, 67, 55, 50, 46, 38, 32, 34, 24, 34, 15, 11, 7};
    private int myProgress = 10;
    private CircleCusView c;
    private StepsChatView stepsChatView;
    private StepsChatView.StepsChartBuilder stepsChartBuilder;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.step);
        setTitleBarTitle(R.string.main_step);
        c = (CircleCusView) findViewById(R.id.c);
        stepsChatView = (StepsChatView) findViewById(R.id.line_curvechart_colu);
        stepsChartBuilder = StepsChatView.StepsChartBuilder.createBuilder(stepsChatView);
        stepsChartBuilder.setXYValues(xValues, yValues);
        startAddProgress();
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    startAddProgress();
                    break;
                default:
                    break;
            }
        }
    };
    private void startAddProgress() {
        if (myProgress > 90)  return;
        myProgress = myProgress + 1;
        c.setProgress(myProgress);
        c.setMax(360);
        handler.sendEmptyMessageDelayed(1, 100);
    }

}
