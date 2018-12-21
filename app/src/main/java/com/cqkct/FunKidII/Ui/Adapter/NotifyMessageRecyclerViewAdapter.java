package com.cqkct.FunKidII.Ui.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.Constants;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.Utils.StringUtils;
import com.cqkct.FunKidII.db.Entity.NotifyMessageEntity;
import com.cqkct.FunKidII.service.MessageCenterUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by justin on 2017/8/11.
 */

public class NotifyMessageRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private boolean checkable = false;

    private Map<Long, NotifyMessageEntity> mSelectedMap;
    private List<NotifyMessageEntity> notifyMessageEntities;
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_BIND_ITEM = 1;
    private Context mContext;

    public NotifyMessageRecyclerViewAdapter(@NonNull Context context, @NonNull List<NotifyMessageEntity> list, @NonNull Map<Long, NotifyMessageEntity> selectedMap) {
        this.mContext = context;
        this.notifyMessageEntities = list;
        this.mSelectedMap = selectedMap;
    }


    public void setNoMorePage() {
        notifyDataSetChanged();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.sms_notification_listview_item, parent, false);
            return new NotifyMessageViewHolder(view);
        } else if (viewType == TYPE_BIND_ITEM) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.sms_request_bind_notification_listview_item, parent, false);
            return new BindItemViewHolder(view);
        }
        return null;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        NotifyMessageEntity entity = notifyMessageEntities.get(position);

        if (holder instanceof NotifyMessageViewHolder) {
            NotifyMessageViewHolder h = (NotifyMessageViewHolder) holder;

            h.iv_icon.setImageResource(Constants.getNotifyTypeIconResId(entity.getContentType()));
            h.tv_content.setText(MessageCenterUtils.getNotifyMessageBeanMsgContent(mContext, entity, null));
            setTextTime(entity, h.tv_time, h.tv_date, position);

            h.linearLayout.setOnLongClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onLongClickListener(position, entity);
                }
                return false;
            });

            if (checkable) {
                h.tv_time.setVisibility(View.GONE);
                h.checkbox.setVisibility(View.VISIBLE);
                h.checkbox.setBackground(mSelectedMap.get(entity.getId()) != null ? mContext.getResources().getDrawable(R.drawable.text_red_background_two) : null);

                if (h.tv_date.getVisibility() == View.VISIBLE) {
                    if (isAllSameDayItemSelected(position, entity.getTime())) {
                        h.tv_date.setBackground(mContext.getResources().getDrawable(R.drawable.text_red_background));
                    } else {
                        h.tv_date.setBackground(mContext.getResources().getDrawable(R.drawable.text_gray_background));
                    }
                    h.tv_date.setOnClickListener(v -> {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onDateClickListener(position, entity);
                        }
                    });
                }
                h.linearLayout.setOnClickListener(v -> {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onClickListener(position, entity);
                    }
                });
            } else {
                if (h.tv_date.getVisibility() == View.VISIBLE) {
                    h.tv_date.setBackground(mContext.getResources().getDrawable(R.drawable.text_blue_background));
                }
                h.checkbox.setVisibility(View.GONE);
                h.tv_time.setVisibility(View.VISIBLE);
            }
        } else if (holder instanceof BindItemViewHolder) {
            BindItemViewHolder h = (BindItemViewHolder) holder;

            h.iv_icon.setImageResource(Constants.getNotifyTypeIconResId(entity.getContentType()));
            h.relation.setText(RelationUtils.decodeRelation(mContext, entity.getOriginator_relation()));
            h.bind_requester.setText(String.format("(%1$s)", entity.getOriginator_phone()));
            h.bind_baby_device.setText(String.format("(%1$s)", entity.getDeviceId()));

            h.bind_baby_name.setText(MessageCenterUtils.getNotifyMessageBeanMsgContent(mContext, entity, null));
            setTextTime(entity, h.tvTime, h.tvDate, position);

            h.linearLayout.setOnLongClickListener(v -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onLongClickListener(position, entity);
                }
                return false;
            });
            h.onBindItemLl.setBackground(mContext.getResources().getDrawable(!TextUtils.isEmpty(entity.getSeq()) ? R.drawable.text_red_two_background : R.drawable.message_bind_reqed_bg));

            if (checkable) {
                h.tvTime.setVisibility(View.GONE);
                h.checkbox.setVisibility(View.VISIBLE);
                h.checkbox.setBackground(mSelectedMap.get(entity.getId()) != null ? mContext.getResources().getDrawable(R.drawable.text_red_background_two) : null);

                if (h.tvDate.getVisibility() == View.VISIBLE) {
                    if (isAllSameDayItemSelected(position, entity.getTime())) {
                        h.tvDate.setBackground(mContext.getResources().getDrawable(R.drawable.text_red_background));
                    } else {
                        h.tvDate.setBackground(mContext.getResources().getDrawable(R.drawable.text_gray_background));
                    }
                    h.tvDate.setOnClickListener(v -> {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onDateClickListener(position, entity);
                        }
                    });
                }
                h.linearLayout.setOnClickListener(v -> {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onClickListener(position, entity);
                    }
                });
            } else {
                if (!TextUtils.isEmpty(entity.getSeq())) {
                    h.linearLayout.setOnClickListener(v -> mOnItemClickListener.onBindItemClickListener(position, entity));
                }

                if (h.tvDate.getVisibility() == View.VISIBLE) {
                    h.tvDate.setBackground(mContext.getResources().getDrawable(R.drawable.text_blue_background));
                }
                h.checkbox.setVisibility(View.GONE);
                h.tvTime.setVisibility(View.VISIBLE);
            }
        }
    }


    private void setTextTime(NotifyMessageEntity notifyMessageEntity, TextView tv_time, TextView tv_date, int position) {
        Calendar thisMsgCal = Calendar.getInstance();
        thisMsgCal.setTimeInMillis(notifyMessageEntity.getTime());
        boolean showDate = false;
        if (position == 0) {
            showDate = true;
        } else {
            NotifyMessageEntity pre = notifyMessageEntities.get(position - 1);
            Calendar preMsgCal = Calendar.getInstance();
            preMsgCal.setTimeInMillis(pre.getTime());
            if (thisMsgCal.get(Calendar.DAY_OF_YEAR) != preMsgCal.get(Calendar.DAY_OF_YEAR) || thisMsgCal.get(Calendar.YEAR) != preMsgCal.get(Calendar.YEAR)) {
                showDate = true;
            }
        }

        if (showDate) {
            tv_date.setVisibility(View.VISIBLE);
            tv_date.setText(StringUtils.getStrDate(thisMsgCal.getTime(), "yyyy-MM-dd"));
        } else {
            tv_date.setVisibility(View.GONE);
        }
        tv_time.setText(StringUtils.getStrDate(thisMsgCal.getTime(), "HH:mm"));
    }

    @Override
    public int getItemCount() {
        return getDataCount();
    }

    public int getDataCount() {
        return notifyMessageEntities == null ? 0 : notifyMessageEntities.size();
    }

    public NotifyMessageEntity getData(int position) {
        if (position < 0)
            return null;
        int dataCount = getDataCount();
        if (position >= dataCount)
            return null;
        return notifyMessageEntities.get(position);
    }

    public boolean isSameDay(long aMillis, long bMillis) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(aMillis);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(bMillis);
        return a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) && a.get(Calendar.YEAR) == b.get(Calendar.YEAR);
    }

    public Map<Long, NotifyMessageEntity> getSameDayItems(int position, long millis) {
        @SuppressLint("UseSparseArrays") Map<Long, NotifyMessageEntity> map = new HashMap<>();
        if (notifyMessageEntities == null || notifyMessageEntities.isEmpty())
            return map;

        for (int i = position; ; i--) {
            NotifyMessageEntity data = getData(i);
            if (data == null)
                break;
            if (isSameDay(data.getTime(), millis))
                map.put(data.getId(), data);
        }
        for (int i = position; ; i++) {
            NotifyMessageEntity data = getData(i);
            if (data == null)
                break;
            if (isSameDay(data.getTime(), millis))
                map.put(data.getId(), data);
        }
        return map;
    }

    public boolean isAllSameDayItemSelected(int position, long millis) {
        Map<Long, NotifyMessageEntity> sameDateMap = getSameDayItems(position, millis);
        for (Long key : sameDateMap.keySet()) {
            if (mSelectedMap.get(key) == null)
                return false;
        }
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        if (notifyMessageEntities.get(position).getContentType() == MessageCenterUtils.ON_REQUEST_BIND) {
            return TYPE_BIND_ITEM;
        } else {
            return TYPE_ITEM;
        }
    }


    class NotifyMessageViewHolder extends RecyclerView.ViewHolder {
        View itemView;
        TextView tv_time;
        TextView tv_date;
        TextView tv_content;
        ImageView iv_icon;
        ImageView checkbox;
        LinearLayout linearLayout;

        NotifyMessageViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;

            iv_icon = itemView.findViewById(R.id.notification_type);
            tv_time = itemView.findViewById(R.id.time);
            tv_date = itemView.findViewById(R.id.date);
            tv_content = itemView.findViewById(R.id.notification_text);
            checkbox = itemView.findViewById(R.id.select_item);
            linearLayout = itemView.findViewById(R.id.message_item);
        }
    }

    static class BindItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, relation, bind_requester, bind_baby_name, bind_baby_device;
        ImageView iv_icon, checkbox;
        LinearLayout linearLayout, onBindItemLl;

        BindItemViewHolder(View view) {
            super(view);
            iv_icon = itemView.findViewById(R.id.notification_type);
            tvTime = itemView.findViewById(R.id.time);
            tvDate = itemView.findViewById(R.id.date);
            relation = itemView.findViewById(R.id.relation);
            checkbox = itemView.findViewById(R.id.select_item);
            bind_requester = itemView.findViewById(R.id.bind_requester);
            bind_baby_name = itemView.findViewById(R.id.bind_baby_name);
            bind_baby_device = itemView.findViewById(R.id.bind_baby_device);
            linearLayout = itemView.findViewById(R.id.message_item);
            onBindItemLl = itemView.findViewById(R.id.onBindItemBg);
        }
    }


    public interface OnItemClickListener {
        void onLongClickListener(int position, NotifyMessageEntity bean);

        void onDateClickListener(int position, NotifyMessageEntity bean);

        void onClickListener(int position, NotifyMessageEntity bean);

        void onBindItemClickListener(int position, NotifyMessageEntity entity);
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnShowItemClickListener(OnItemClickListener onShowItemClickListener) {
        mOnItemClickListener = onShowItemClickListener;
    }

    public boolean notifyCheckable(boolean checkable) {
        this.checkable = checkable;
        notifyDataSetChanged();
        return this.checkable;
    }
    public boolean getNotifyCheckable(){
        return this.checkable;
    }
}
