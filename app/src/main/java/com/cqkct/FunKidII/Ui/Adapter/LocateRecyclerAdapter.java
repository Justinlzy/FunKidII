package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.GuardianAddrInfo;
import com.cqkct.FunKidII.R;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * Created by Justin on 2016/11/9.
 */

public class LocateRecyclerAdapter extends RecyclerView.Adapter<LocateRecyclerAdapter.LocateViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<GuardianAddrInfo> mList;
    private OnLocationItemClick mLocationItemClick;
    private RecyclerView mRecyclerView;


    public LocateRecyclerAdapter(Context context, List<GuardianAddrInfo> list) {
        mContext = context;
        mList = list;
    }

    public void setLocationItemClick(OnLocationItemClick locationItemClick) {
        mLocationItemClick = locationItemClick;
    }

    @Override
    public LocateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.guardian_sch_item, parent, false);
        view.setOnClickListener(this);
        return new LocateViewHolder(view);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;

    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecyclerView = null;
    }

    @Override
    public void onBindViewHolder(LocateViewHolder holder, int position) {
        GuardianAddrInfo info = mList.get(position);
        holder.nameTextView.setText(info.name);
        holder.addressTextView.setText(info.address);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private Map<View, Long> lastClickMap = new WeakHashMap<>();

    @Override
    public void onClick(View view) {
        Long previousClickTimestamp = lastClickMap.get(view);
        long currentTimestamp = SystemClock.elapsedRealtime();

        if(previousClickTimestamp == null || currentTimestamp - previousClickTimestamp > 800) {
            lastClickMap.put(view, currentTimestamp);
            int position = mRecyclerView.getChildAdapterPosition(view);
            mLocationItemClick.OnItemClick(mRecyclerView, view, position, mList.get(position));
        }
    }

    public static class LocateViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView, addressTextView;

        public LocateViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.name);
            addressTextView = itemView.findViewById(R.id.address);
        }
    }

    public interface OnLocationItemClick {
        void OnItemClick(RecyclerView parent, View view, int position, GuardianAddrInfo addr);
    }
}
