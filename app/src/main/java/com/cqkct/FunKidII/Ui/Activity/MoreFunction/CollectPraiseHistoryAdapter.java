package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.PraiseEditViewAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import protocol.Message;

public class CollectPraiseHistoryAdapter extends RecyclerView.Adapter {
    private List<Message.Praise> list;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyyy/MM/dd");

    public CollectPraiseHistoryAdapter(List<Message.Praise> mHistoryPraiseList) {
        this.list = mHistoryPraiseList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_praise_adapter_history_item, parent, false);
        return new HistoryViewHolder(convertView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        bindHistoryItemValue((HistoryViewHolder) holder, list.get(position));
    }

    private void bindHistoryItemValue(HistoryViewHolder holder, Message.Praise praise) {

        holder.date.setText(TIME_FMT.format(new Date(praise.getStartTime() * 1000L)));
        holder.prizeName.setText(praise.getPrize());
        long diffDay = (praise.getFinishTime() - praise.getStartTime()) / 60 / 60 / 24;
        if (diffDay == 0) {
            Calendar begin = Calendar.getInstance(TimeZone.getTimeZone(praise.getTimezone().getZone()));
            begin.setTimeInMillis(praise.getStartTime() * 1000L);
            Calendar finish = Calendar.getInstance(TimeZone.getTimeZone(praise.getTimezone().getZone()));
            finish.setTimeInMillis(praise.getFinishTime() * 1000L);
            if (begin.get(Calendar.DAY_OF_YEAR) != finish.get(Calendar.DAY_OF_YEAR)) {
                diffDay = 1;
            }
        }
        holder.consumedDates.setText(String.valueOf(diffDay));
        if (praise.getTotalReached() == praise.getTotalGoal()) {
            holder.bar.setProgress(0);
            holder.bar.setSecondaryProgress(100);
        } else {
            int percentageInt = praise.getTotalReached() * 100 / praise.getTotalGoal();
            holder.bar.setProgress(percentageInt);
            holder.bar.setSecondaryProgress(0);
        }
        holder.percentage.setText(String.format("%d/%d", praise.getTotalReached(), praise.getTotalGoal()));

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView date, prizeName, consumedDates, percentage;
        ProgressBar bar;

        HistoryViewHolder(View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.time);
            prizeName = itemView.findViewById(R.id.prize);
            consumedDates = itemView.findViewById(R.id.consumed);
            bar = itemView.findViewById(R.id.progress_bar);
            percentage = itemView.findViewById(R.id.percentage_unit);
        }
    }
}
