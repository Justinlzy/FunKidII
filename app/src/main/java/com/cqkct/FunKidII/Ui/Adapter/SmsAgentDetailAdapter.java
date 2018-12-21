package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.db.Entity.SmsEntity;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SmsAgentDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<SmsEntity> data;
    private boolean checkable = false;
    private Map<Long, SmsEntity> mSelectedMap;
    private onItemClickListener listener;


    public SmsAgentDetailAdapter(Context context, List<SmsEntity> list, @NonNull Map<Long, SmsEntity> selectedMap) {
        this.mContext = context;
        this.data = list;
        this.mSelectedMap = selectedMap;
    }

    public boolean notifyCheckable(boolean checkable) {
        this.checkable = checkable;
        notifyDataSetChanged();
        return this.checkable;
    }

    public void setItemClickListener(onItemClickListener itemClickListener) {
        listener = itemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.sms_detail_agent_list, parent, false);
        return new SmsAgentDetailAdapter.SmsAdapterViewHolder(view);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        SmsAdapterViewHolder viewHolder = (SmsAdapterViewHolder) holder;
        SmsEntity smsEntity = data.get(position);
        viewHolder.tvSmsContext.setText(smsEntity.getText());



        if (checkable) {
            if (mSelectedMap.get(smsEntity.getId()) == null) {
                viewHolder.tvSmsTime.setBackground(mContext.getResources().getDrawable(R.drawable.text_gray_background));
            } else {
                viewHolder.tvSmsTime.setBackground(mContext.getResources().getDrawable(R.drawable.text_red_background));
            }
            if (listener != null) {
                viewHolder.cl.setOnClickListener(v -> listener.onNumberClick(position, smsEntity));
            }

        } else {
            viewHolder.tvSmsTime.setBackground(mContext.getResources().getDrawable(R.drawable.text_blue_background));
            viewHolder.cl.setOnLongClickListener(v -> {
                listener.onItemLongClick(position, smsEntity);
                return false;
            });
        }

        long time = data.get(position).getTime();
        if (time > 0) {
            Date date = new Date(smsEntity.getTime() * 1000L);
            Calendar nowCalendar = Calendar.getInstance();
            Calendar msgCalendar = Calendar.getInstance();
            msgCalendar.setTime(date);
            viewHolder.tvSmsTime.setText(PublicTools.genTimeText(mContext, nowCalendar, msgCalendar));
        }

    }


    @Override
    public int getItemCount() {
        return data.size();
    }


    static class SmsAdapterViewHolder extends RecyclerView.ViewHolder {

        TextView  tvSmsTime, tvSmsContext;
        ConstraintLayout cl;
        SmsAdapterViewHolder(View itemView) {
            super(itemView);
            tvSmsTime = itemView.findViewById(R.id.sms_time);
            tvSmsContext = itemView.findViewById(R.id.sms_context);
            cl = itemView.findViewById(R.id.sms_agent);
        }
    }

    public interface onItemClickListener {
        void onNumberClick(int position, SmsEntity entity);
        void onItemLongClick(int position, SmsEntity entity);
    }
}
