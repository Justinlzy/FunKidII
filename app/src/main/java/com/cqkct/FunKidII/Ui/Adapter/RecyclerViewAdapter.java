package com.cqkct.FunKidII.Ui.Adapter;

import android.os.SystemClock;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;
import java.util.WeakHashMap;


public abstract class RecyclerViewAdapter<VH extends RecyclerView.ViewHolder, ItemData> extends RecyclerView.Adapter<VH> implements View.OnClickListener, View.OnLongClickListener {
    protected RecyclerView mRecyclerView;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(RecyclerView recyclerView, RecyclerViewAdapter adapter, View view, int position);
    }

    public abstract class DebouncedOnItemClickListener implements OnItemClickListener {

        private final long minimumInterval;
        private Map<View, Long> lastClickMap;

        /**
         * Implement this in your subclass instead of onClick
         */
        public abstract void onDebouncedItemClick(RecyclerView recyclerView, RecyclerViewAdapter adapter, View view, int position);

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
        public void onItemClick(RecyclerView recyclerView, RecyclerViewAdapter adapter, View view, int position) {
            Long previousClickTimestamp = lastClickMap.get(view);
            long currentTimestamp = SystemClock.elapsedRealtime();

            if(previousClickTimestamp == null || currentTimestamp - previousClickTimestamp > minimumInterval) {
                lastClickMap.put(view, currentTimestamp);
                onDebouncedItemClick(recyclerView, adapter, view, position);
            }
        }
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(RecyclerView recyclerView, RecyclerViewAdapter adapter, View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
    }

    public abstract ItemData getItem(int position);

    public boolean positionIsVisible(int position) {
        if (mRecyclerView == null)
            return false;
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null)
            return false;
        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            int firstVisiblePosition = linearLayoutManager.findFirstVisibleItemPosition();
            if (firstVisiblePosition == RecyclerView.NO_POSITION) {
                return false;
            }
            int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
            if (lastVisiblePosition == RecyclerView.NO_POSITION) {
                return false;
            }
            return position >= firstVisiblePosition && position <= lastVisiblePosition;
        }
        return false;
    }

    public VH getViewHolder(int position) {
        if (mRecyclerView == null)
            return null;
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(position);
        return (VH) vh;
    }

    public View getView(int position) {
        if (mRecyclerView == null)
            return null;
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager == null)
            return null;
        return layoutManager.findViewByPosition(position);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
        VH vh = onCreateViewHolder2(parent, viewType);
        vh.itemView.setOnClickListener(this);
        return vh;
    }

    public abstract VH onCreateViewHolder2(ViewGroup parent, int viewType);

    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null && mRecyclerView != null) {
            int position = mRecyclerView.getChildAdapterPosition(v);
            mOnItemClickListener.onItemClick(mRecyclerView, this, v, position);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnItemLongClickListener != null && mRecyclerView != null) {
            int position = mRecyclerView.getChildAdapterPosition(v);
            return mOnItemLongClickListener.onItemLongClick(mRecyclerView, this, v, position);
        }
        return false;
    }
}