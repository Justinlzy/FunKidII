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

public class SmsAgentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<SmsEntity> data;
    private boolean checkable = false;
    private Map<Long, SmsEntity> mSelectedMap;
    private onItemClickListener listener;

    private boolean noMorePage;

    public SmsAgentAdapter(Context context, List<SmsEntity> list, @NonNull Map<Long, SmsEntity> selectedMap) {
        this.mContext = context;
        this.data = list;
        this.mSelectedMap = selectedMap;
    }

    public void setNoMorePage() {
        noMorePage = true;
        notifyDataSetChanged();
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
        View view = LayoutInflater.from(mContext).inflate(R.layout.sms_agent_list, parent, false);
        return new SmsAgentAdapter.SmsAdapterViewHolder(view);
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
        viewHolder.tvNumber.setText(smsEntity.getNumber());
        viewHolder.tvSmsContext.setText(smsEntity.getText());
        if (position == data.size() - 1) {
            viewHolder.line.setVisibility(View.GONE);
        }

        if (checkable) {
            viewHolder.tvNumber.setBackground(mSelectedMap.get(smsEntity.getId()) != null ?
                    mContext.getResources().getDrawable(R.drawable.text_red_background) :
                    mContext.getResources().getDrawable(R.drawable.text_gray_background));
            if (listener != null) {
                viewHolder.tvNumber.setOnClickListener(v -> listener.onNumberClick(position, smsEntity));
            }

        } else {
            viewHolder.tvNumber.setBackground(mContext.getResources().getDrawable(R.drawable.text_blue_background));
            viewHolder.cl.setOnClickListener(v -> listener.onItemClick(position, smsEntity));
        }

        long time = data.get(position).getTime();
        if (time > 0) {
            Date date = new Date(smsEntity.getTime() * 1000L);
            Calendar nowCalendar = Calendar.getInstance();
            Calendar msgCalendar = Calendar.getInstance();
            msgCalendar.setTime(date);
            ((SmsAdapterViewHolder) holder).tvSmsTime.setText(PublicTools.genDayText(mContext, nowCalendar, msgCalendar));
        }
        viewHolder.unreadMark.setVisibility(smsEntity.getUnreadMark() ? View.GONE : View.VISIBLE);
    }


    @Override
    public int getItemCount() {
        return data.size();
    }


    static class SmsAdapterViewHolder extends RecyclerView.ViewHolder {

        TextView tvNumber, tvSmsTime, tvSmsContext;
        ConstraintLayout cl;
        ImageView unreadMark;
        View line;
        SmsAdapterViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.sms_number);
            tvSmsTime = itemView.findViewById(R.id.sms_time);
            tvSmsContext = itemView.findViewById(R.id.sms_context);
            unreadMark = itemView.findViewById(R.id.unread_mark);
            cl = itemView.findViewById(R.id.sms_agent);
            line = itemView.findViewById(R.id.dividing_line);
        }
    }

    public interface onItemClickListener {
        void onNumberClick(int position, SmsEntity entity);
        void onItemClick(int position, SmsEntity entity);
    }
}
