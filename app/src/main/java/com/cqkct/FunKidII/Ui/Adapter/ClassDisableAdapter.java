package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.DateUtil;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.db.Entity.ClassDisableEntity;

import java.util.Calendar;
import java.util.List;

/**
 * Created by justin on 2017/11/29.
 */

public class ClassDisableAdapter extends RecyclerView.Adapter {
    private Context mContext;
    private List<ClassDisableEntity> list;
    public OnItemClickListener mListener;
    private boolean hasEditPermission;

    public ClassDisableAdapter(Context context, List<ClassDisableEntity> contactBeanList, OnItemClickListener listener, boolean hasEditPermission) {
        this.mContext = context;
        this.list = contactBeanList;
        this.mListener = listener;
        this.hasEditPermission = hasEditPermission;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.adapter_class_disable, null);
        ClassDisableHolder holder = new ClassDisableHolder(itemView);
        if (mListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            itemView.setForeground(parent.getContext().getResources().getDrawable(R.drawable.foreground_shadow));
        if (mListener != null && hasEditPermission) {
            holder.item.setOnClickListener(v -> mListener.onItemClick(holder.getAdapterPosition()));

        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        ClassDisableHolder holder = (ClassDisableHolder) viewHolder;
        ClassDisableEntity entity = list.get(position);
        Calendar begin = Calendar.getInstance();
        begin.setTimeZone(DateUtil.parseTimeZone(entity.getTimezone()));
        begin.setTimeInMillis(entity.getBeginTime() * 1000L);
        String am = String.format("%02d:%02d", begin.get(Calendar.HOUR_OF_DAY), begin.get(Calendar.MINUTE));
        begin.setTimeInMillis(entity.getEndTime() * 1000L);
        String pm = String.format("%02d:%02d", begin.get(Calendar.HOUR_OF_DAY), begin.get(Calendar.MINUTE));

        holder.tv_name.setText(entity.getName());
        holder.tv_start.setText(am);
        holder.tv_end.setText(pm);
        holder.tv_repeat.setText(PublicTools.getDecoderWeak(mContext, entity.getRepeat()));
        holder.progressBar.setVisibility(entity.getSynced() ? View.GONE : View.VISIBLE);

        holder.ib_switch.setOnCheckedChangeListener(null);
        holder.ib_switch.setChecked(entity.getEnable());
        if (mListener != null && hasEditPermission) {
            holder.ib_switch.setOnCheckedChangeListener(mOnCheckedChangeListener);
            holder.ib_switch.setEnabled(true);
        } else {
            holder.ib_switch.setEnabled(false);
        }
        holder.ib_switch.setTag(position);

        if (mListener == null) {
            holder.detailIcon.setVisibility(View.GONE);
        } else {
            holder.detailIcon.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public interface OnItemClickListener<T> {
        void onItemClick(int position);
        void onCompoundButtonClick(int position, CompoundButton cb);
    }

    class ClassDisableHolder extends RecyclerView.ViewHolder {
        SwitchCompat ib_switch;
        TextView tv_start;
        TextView tv_end;
        TextView tv_repeat;
        TextView tv_name;
        ProgressBar progressBar;
        ImageView detailIcon;
        RelativeLayout item;

        public ClassDisableHolder(View itemView) {
            super(itemView);
            ib_switch = itemView.findViewById(R.id.ib_switch);
            tv_start = itemView.findViewById(R.id.start_text);
            tv_end = itemView.findViewById(R.id.end_text);
            tv_repeat = itemView.findViewById(R.id.repeat_text);
            tv_name = itemView.findViewById(R.id.name);
            progressBar = itemView.findViewById(R.id.class_disable_pb_syncing);
            detailIcon = itemView.findViewById(R.id.detail_icon);
            item = itemView.findViewById(R.id.more_imp_refuse_stranger_);
        }
    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mListener != null) {
                buttonView.setOnCheckedChangeListener(null);
                buttonView.setEnabled(false);
                buttonView.setChecked(!isChecked);
                mListener.onCompoundButtonClick((Integer) buttonView.getTag(), buttonView);
            }
        }
    };

}
