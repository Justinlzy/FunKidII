package com.cqkct.FunKidII.Ui.Listener;

import android.os.SystemClock;
import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class DebouncedOnClickListener implements View.OnClickListener {

    private final long minimumInterval;
    private View lastClickView;
    private long lastClickTime;

    /**
     * Implement this in your subclass instead of onClick
     * @param view The view that was clicked
     */
    public abstract void onDebouncedClick(View view);

    public DebouncedOnClickListener() {
        this(800);
    }

    /**
     * constructor
     * @param minimumIntervalMillis The minimum allowed time between clicks - any click sooner than this after a previous click will be rejected
     */
    public DebouncedOnClickListener(long minimumIntervalMillis) {
        minimumInterval = minimumIntervalMillis;
    }

    @Override
    public final void onClick(View clickedView) {
        long previousClickTimestamp = 0;
        if (clickedView == lastClickView) {
            previousClickTimestamp = lastClickTime;
        }
        long currentTimestamp = SystemClock.elapsedRealtime();

        if(currentTimestamp - previousClickTimestamp > minimumInterval) {
            lastClickView = clickedView;
            lastClickTime = currentTimestamp;
            onDebouncedClick(clickedView);
        }
    }
}
