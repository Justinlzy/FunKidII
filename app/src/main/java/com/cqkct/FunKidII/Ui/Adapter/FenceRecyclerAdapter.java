package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.GuardianAddrInfo;
import com.cqkct.FunKidII.R;

import java.util.List;


/**
 * Created by Justin on 2016/11/9.
 */

public class FenceRecyclerAdapter extends RecyclerView.Adapter<FenceRecyclerAdapter.FenceViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<GuardianAddrInfo> mList;
    private OnFenceItemClick mLocationItemClick;
    private RecyclerView mRecyclerView;


    public FenceRecyclerAdapter(Context context, List<GuardianAddrInfo> list) {
        mContext = context;
        mList = list;
    }

    public void setFenceItemClick(OnFenceItemClick locationItemClick) {
        mLocationItemClick = locationItemClick;
    }

    @Override
    public FenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.fence_sch_item, parent, false);
        view.setOnClickListener(this);
        return new FenceViewHolder(view);
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
    public void onBindViewHolder(FenceViewHolder holder, int position) {
        holder.tv_fenceName.setText(mList.get(position).name);
        holder.tv_fenceAddress.setText(mList.get(position).address) ;
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void onClick(View view) {
        int position = mRecyclerView.getChildAdapterPosition(view);
        mLocationItemClick.OnItemClick(mRecyclerView, view, position, mList.get(position));
    }

    public static class FenceViewHolder extends RecyclerView.ViewHolder {

        TextView tv_fenceName, tv_fenceAddress;

        public FenceViewHolder(View itemView) {
            super(itemView);
            tv_fenceName = itemView.findViewById(R.id.tv_fence_name);
            tv_fenceAddress = itemView.findViewById(R.id.tv_fence_address);
        }
    }

    public interface OnFenceItemClick {
        void OnItemClick(RecyclerView parent, View view, int position, GuardianAddrInfo addr);
    }
}
