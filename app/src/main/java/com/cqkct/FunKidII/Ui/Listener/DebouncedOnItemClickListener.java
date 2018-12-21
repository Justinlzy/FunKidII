package com.cqkct.FunKidII.Ui.Listener;

import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class DebouncedOnItemClickListener implements AdapterView.OnItemClickListener {

    private final long minimumInterval;
    private Map<View, Long> lastClickMap;

    /**
     * Implement this in your subclass instead of onClick
     */
    public abstract void onDebouncedItemClick(AdapterView<?> parent, View view, int position, long id);

    public DebouncedOnItemClickListener() {
        this(800);
    }

    /**
     * constructor
     * @param minimumIntervalMillis The minimum allowed time between clicks - any click sooner than this after a previous click will be rejected
     */
    public DebouncedOnItemClickListener(long minimumIntervalMillis) {
        minimumInterval = minimumIntervalMillis;
        lastClickMap = new WeakHashMap<>();
    }

    @Override
    public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Long previousClickTimestamp = lastClickMap.get(view);
        long currentTimestamp = SystemClock.elapsedRealtime();

        if(previousClickTimestamp == null || currentTimestamp - previousClickTimestamp > minimumInterval) {
            lastClickMap.put(view, currentTimestamp);
            onDebouncedItemClick(parent, view, position, id);
        }
    }
}
