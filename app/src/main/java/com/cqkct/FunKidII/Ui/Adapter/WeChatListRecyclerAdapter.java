package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.WeChatMemberBean;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.JoinBitmaps;
import com.cqkct.FunKidII.Utils.PublicTools;
import com.cqkct.FunKidII.Utils.RelationUtils;
import com.cqkct.FunKidII.db.Entity.ContactEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WeChatListRecyclerAdapter extends RecyclerView.Adapter<WeChatListRecyclerAdapter.WeChatViewHolder> implements View.OnClickListener {

    private Context mContext;
    private List<WeChatMemberBean> mList;
    private WeChatListRecyclerAdapter.OnWeChatItemClick mWeChatItemClick;
    private RecyclerView mRecyclerView;
    private String mUserId;


    public WeChatListRecyclerAdapter(Context context, List<WeChatMemberBean> list, String userId) {
        mContext = context;
        mList = list;
        mUserId = userId;
    }

    public void setWeChatItemClick(WeChatListRecyclerAdapter.OnWeChatItemClick locationItemClick) {
        mWeChatItemClick = locationItemClick;
    }

    @Override
    public WeChatListRecyclerAdapter.WeChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.wechat_list, parent, false);
        view.setOnClickListener(this);
        return new WeChatListRecyclerAdapter.WeChatViewHolder(view);
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
    public void onBindViewHolder(WeChatListRecyclerAdapter.WeChatViewHolder holder, int position) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(mList.get(position).getBabyName());
        if (TextUtils.isEmpty(stringBuffer))
            stringBuffer.append(mContext.getString(R.string.baby));
        stringBuffer.append("的家庭群聊");
        holder.tvWeChatItemName.setText(stringBuffer.toString());

        holder.tvWeChatLastText.setText(mList.get(position).getLastText());

        long time = mList.get(position).getLastTextTime();
        if (time > 0) {
            Date date = new Date(mList.get(position).getLastTextTime());
            Calendar nowCalendar = Calendar.getInstance();
            Calendar msgCalendar = Calendar.getInstance();
            msgCalendar.setTime(date);
            holder.tvWeChatItemTime.setText(PublicTools.genTimeText(mContext, nowCalendar, msgCalendar));
        }
        ArrayList<Bitmap> mBmps = new ArrayList<Bitmap>();
        for (ContactEntity contactEntity : mList.get(position).getContactEntityList()) {
            if (mBmps.size() >= 5)
                break;
            //过滤掉自己
            if (contactEntity.getUserId().equals(mUserId))
                continue;
            mBmps.add(BitmapFactory.decodeResource(mContext.getResources(), RelationUtils.getIconResId(contactEntity.getRelation())));
        }
        if (mBmps.size() > 0)
            holder.ivWeChatItemIcon.setImageBitmap(JoinBitmaps.createBitmap(150, 150, mBmps));
        mBmps.clear();
    }


    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void onClick(View view) {
        int position = mRecyclerView.getChildAdapterPosition(view);
        mWeChatItemClick.OnItemClick(mRecyclerView, view, position, mList.get(position));
    }

    static class WeChatViewHolder extends RecyclerView.ViewHolder {

        TextView tvWeChatItemName, tvWeChatLastText, tvWeChatItemTime;
        ImageView ivWeChatItemIcon;

        WeChatViewHolder(View itemView) {
            super(itemView);
            tvWeChatItemName = itemView.findViewById(R.id.wechat_list_name);
            tvWeChatLastText = itemView.findViewById(R.id.wechat_list_last_text);
            tvWeChatItemTime = itemView.findViewById(R.id.wechat_list_time);
            ivWeChatItemIcon = itemView.findViewById(R.id.wechat_icon);

        }
    }

    public interface OnWeChatItemClick {
        void OnItemClick(RecyclerView parent, View view, int position, WeChatMemberBean weChatMemberBean);
    }
}
