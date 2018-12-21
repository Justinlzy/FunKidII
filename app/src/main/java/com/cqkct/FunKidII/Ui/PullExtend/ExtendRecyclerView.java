package com.cqkct.FunKidII.Ui.PullExtend;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public class ExtendRecyclerView extends RecyclerView {
    ExtendListHeaderNew mExtendListHeader;

    public ExtendRecyclerView(Context context) {
        super(context);
        init();
    }

    public ExtendRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
    }

    public void setExtendListHeader(ExtendListHeaderNew extendListHeader) {
        mExtendListHeader = extendListHeader;
        setup();
    }

    private void setup() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewGroup.LayoutParams lp = mExtendListHeader.getLayoutParams();
                lp.height = getHeight();
                mExtendListHeader.setLayoutParams(lp);
                LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                layoutManager.scrollToPositionWithOffset(1, 0);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                    int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisiblePosition == 0) {
                        View firstVisibleItemView = recyclerView.getChildAt(0);
                        if (firstVisibleItemView != null) {
                            int scrollY = firstVisibleItemView.getBottom();
                            int headerListHeight = mExtendListHeader.getListSize();
                            if (scrollY < headerListHeight / 2) {
                                recyclerView.post(() -> recyclerView.smoothScrollBy(0, scrollY));
                            } else if (scrollY < headerListHeight || scrollY > headerListHeight) {
                                recyclerView.post(() -> recyclerView.smoothScrollBy(0, scrollY - headerListHeight));
                            }
                        }
                    }
                }
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
                int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                View firstVisibleItemView = recyclerView.getChildAt(1);
                if (firstVisiblePosition != 0) {
                    mExtendListHeader.onReset();
                }
                if (firstVisiblePosition == 0 && firstVisibleItemView != null) {
                    if (firstVisibleItemView.getTop() >= 0) {
                        mExtendListHeader.onPull(firstVisibleItemView.getTop());
                    }
                    if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING &&
                            firstVisibleItemView.getTop() > mExtendListHeader.getListSize() / 5) {
                        recyclerView.stopScroll();
                    }
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
            if (layoutManager.findFirstVisibleItemPosition() == 0) {
                View firstVisibleItemView = layoutManager.getChildAt(0);
                if (firstVisibleItemView != null) {
                    final int scrollY = firstVisibleItemView.getBottom();
                    final int headerListHeight = mExtendListHeader.getListSize();
                    if (scrollY < headerListHeight / 2) {
                        post(() -> smoothScrollBy(0, scrollY));
                    } else if (scrollY < headerListHeight || scrollY > headerListHeight) {
                        post(() -> smoothScrollBy(0, scrollY - headerListHeight));
                    }
                }
            }
        }
        return super.onTouchEvent(ev);
    }
}