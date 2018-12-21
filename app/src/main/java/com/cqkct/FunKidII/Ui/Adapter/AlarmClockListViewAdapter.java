package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.AlarmClockItemActivity;
import com.cqkct.FunKidII.Utils.L;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.db.Entity.AlarmClockEntity;

import java.util.Calendar;
import java.util.List;

import protocol.Message;

/**
 * Created by justin on 2017/11/28.
 */

public class AlarmClockListViewAdapter extends RecyclerView.Adapter {
    public static final String TAG = AlarmClockListViewAdapter.class.getSimpleName();

    private List<AlarmClockEntity> dataList;
    private AlarmClickAdapterClickListener mListener;
    private boolean hasVibrationMotor;
    private Context mContext;

    public AlarmClockListViewAdapter(Context context, List<AlarmClockEntity> data, boolean hasVibrationMotor, AlarmClickAdapterClickListener listener) {
        this.mContext = context;
        this.dataList = data;
        this.hasVibrationMotor = hasVibrationMotor;
        mListener = listener;
    }

    public void setHasVibrationMotor(boolean hasVibrationMotor) {
        this.hasVibrationMotor = hasVibrationMotor;
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_alarm_clock_item, parent, false);
        AlarmClockHolder holder = new AlarmClockHolder(itemView);
        if (mListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            itemView.setForeground(parent.getContext().getResources().getDrawable(R.drawable.foreground_shadow));
        if (mListener != null) {
            holder.rootRl.setOnClickListener(v -> mListener.OnItemClick(holder.getAdapterPosition(), v));
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        AlarmClockHolder holder = (AlarmClockHolder) viewHolder;
        AlarmClockEntity ac = dataList.get(position);
        Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(ac.getTimePoint() * 1000);
        holder.textAlarmTime.setText(String.format("%02d:%02d", instance.get(Calendar.HOUR_OF_DAY), instance.get(Calendar.MINUTE)));
        holder.textName.setText(ac.getName());
        String repeat = PublicTools.getDecoderWeak(mContext, ac.getRepeat());
        if (repeat.equals(mContext.getString(R.string.none))) {
            repeat = mContext.getString(R.string.single_alarm);
        }
        holder.textWeeksRepeat.setText(repeat);//周重复字符串

        holder.alarmEnableSwitch.setOnCheckedChangeListener(null);
        holder.alarmEnableSwitch.setChecked(ac.getEnable());
        if (ac.getEnable()) {
            if ((ac.getRepeat() & Message.TimePoint.RepeatFlag.ALL_VALUE) == 0) {
                // 这是单次闹钟，计算是否已过时
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(ac.getTimePoint() * 1000L);
                if (calendar.before(Calendar.getInstance())) {
                    // 已过时
                    holder.alarmEnableSwitch.setChecked(false);
                } else {
                    holder.alarmEnableSwitch.setChecked(true);
                }
            } else {
                holder.alarmEnableSwitch.setChecked(true);
            }
        } else {
            holder.alarmEnableSwitch.setChecked(false);
        }
        if (mListener != null) {
            holder.alarmEnableSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
            holder.alarmEnableSwitch.setEnabled(true);
        } else {
            holder.alarmEnableSwitch.setEnabled(false);
        }
        holder.alarmEnableSwitch.setTag(position);

        L.d(TAG, "Notice_type_:" + ac.getNoticeFlag());

        int noticeFlag = ac.getNoticeFlag();
        if (!hasVibrationMotor && (noticeFlag & Message.AlarmClock.NoticeFlag.VIBRATE_VALUE) != 0) {
            noticeFlag &= ~Message.AlarmClock.NoticeFlag.VIBRATE_VALUE;
        }
        holder.textNoticeType.setText(AlarmClockItemActivity.decodeNoticeToString(mContext, noticeFlag));

        holder.textAlarmTime.setOnClickListener(mListener);
        holder.textAlarmTime.setTag(position);

        holder.progressBar.setVisibility(ac.getSynced() ? View.GONE : View.VISIBLE);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    //TODO:设置剩余时间
//    private void setTextCountDown(TextView textCountdown, String weekRepeatStr) {
//
//    }

    public static abstract class AlarmClickAdapterClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            myOnClick((Integer) v.getTag(), v);
        }

        public abstract void myOnClick(int position, View v);
        public abstract void OnItemClick(int position, View v);
    }

    public final class AlarmClockHolder extends RecyclerView.ViewHolder {
        RelativeLayout rootRl;
        TextView textAlarmTime;
        TextView textNoticeType;
        TextView textWeeksRepeat;
        SwitchCompat alarmEnableSwitch;
        TextView textName;
        ProgressBar progressBar;


        public AlarmClockHolder(View itemView) {
            super(itemView);
            rootRl = itemView.findViewById(R.id.alarm_clock_item);
            textName = itemView.findViewById(R.id.alarm_clock_name);
            textAlarmTime = itemView.findViewById(R.id.alarm_clock_datetime);
            textNoticeType = itemView.findViewById(R.id.alarm_clock_notice);
            textWeeksRepeat = itemView.findViewById(R.id.text_weeks_repeat);
            alarmEnableSwitch = itemView.findViewById(R.id.imageview_setting_datetime_sys);
//          textCountdown = (TextView) convertView.findViewById(R.id.alarm_clock_countdown);
            progressBar = itemView.findViewById(R.id.alarm_clock_pb_syncing);
        }
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mListener != null) {
                buttonView.setOnCheckedChangeListener(null);
                buttonView.setEnabled(false);
                buttonView.setChecked(!isChecked);
                mListener.myOnClick((Integer) buttonView.getTag(), buttonView);
            }
        }
    };
}
