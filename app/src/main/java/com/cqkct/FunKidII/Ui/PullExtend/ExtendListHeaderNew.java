package com.cqkct.FunKidII.Ui.PullExtend;


import android.app.Service;
import android.content.Context;
import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.DensityUtils;


/**
 * 这个类封装了下拉刷新的布局
 */
public class ExtendListHeaderNew extends ExtendLayout {


    float containerHeight = DensityUtils.dip2px(50);
    float listHeight = DensityUtils.dip2px(100);

    boolean arrivedListHeight = false;
    private RecyclerView mRecyclerView;

    /**
     * 圆点
     */
    private ExpendPoint mExpendPoint;

    /**
     * 构造方法
     *
     * @param context context
     */
    public ExtendListHeaderNew(Context context) {
        super(context);
    }


    /**
     * 构造方法
     *
     * @param context context
     * @param attrs   attrs
     */
    public ExtendListHeaderNew(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void bindView(View container) {
        mRecyclerView = findViewById(R.id.list);
        mExpendPoint = findViewById(R.id.expend_point);
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    protected View createLoadingView(Context context, AttributeSet attrs) {
        return LayoutInflater.from(context).inflate(R.layout.extend_header, null);
    }

    @Override
    public int getContentHeight() {
        return (int) (containerHeight);
    }

    @Override
    public int getListSize() {
        return (int) (listHeight);
    }


    @Override
    public void onReset() {
        mExpendPoint.setVisibility(VISIBLE);
        mExpendPoint.setAlpha(1);
        mExpendPoint.setTranslationY(0);
        mRecyclerView.setTranslationY(0);
        arrivedListHeight = false;
    }

    @Override
    protected void onReleaseToRefresh() {
    }

    @Override
    protected void onPullToRefresh() {

    }

    @Override
    public void onArrivedListHeight() {
        arrivedListHeight = true;
    }

    @Override
    protected void onRefreshing() {
    }

    @Override
    public void onPull(int offset) {
        if (!arrivedListHeight) {
            mExpendPoint.setVisibility(VISIBLE);
            float percent = Math.abs(offset) / containerHeight;
            int moreOffset = Math.abs(offset) - (int) containerHeight;
            if (percent <= 1.0f) {
                mExpendPoint.setPercent(percent);
                mExpendPoint.setTranslationY(-Math.abs(offset) / 2 + mExpendPoint.getHeight() / 2);
                mRecyclerView.setTranslationY(-containerHeight);
            } else {
                float subPercent = (moreOffset) / (listHeight - containerHeight);
                subPercent = Math.min(1.0f, subPercent);
                mExpendPoint.setTranslationY(-(int) containerHeight / 2 + mExpendPoint.getHeight() / 2 + (int) containerHeight * subPercent / 2);
                mExpendPoint.setPercent(1.0f);
                float alpha = (1 - subPercent * 2);
                mExpendPoint.setAlpha(Math.max(alpha, 0));
                mRecyclerView.setTranslationY(-(1 - subPercent) * containerHeight);
            }
        }
        if (Math.abs(offset) >= listHeight) {
            mExpendPoint.setVisibility(INVISIBLE);
            mRecyclerView.setTranslationY(-(Math.abs(offset) - listHeight) / 2);
        }
    }


}
