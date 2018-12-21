package com.cqkct.FunKidII.Ui.Adapter;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.R;

import java.util.List;

public class MoreActivityAdapter extends RecyclerView.Adapter {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_ITEM_TYPE = 1;

    public static class DataPrototype {
        public @DrawableRes
        int imgResId;
        @StringRes
        int nameStrResId;
        String itemType;

        public DataPrototype(String type) {
            this.itemType = type;
        }

        public DataPrototype(@DrawableRes int imgResId, @StringRes int nameStrResId) {
            this.imgResId = imgResId;
            this.nameStrResId = nameStrResId;
        }
    }

    private List<DataPrototype> dataList;
    private View.OnClickListener mOnClickListener;

    public MoreActivityAdapter(List<DataPrototype> dataList, View.OnClickListener onClickListener) {
        this.dataList = dataList;
        mOnClickListener = onClickListener;
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public int getItemViewType(int position) {
        DataPrototype data = dataList.get(position);
        if (data.nameStrResId == 0) {
            return TYPE_ITEM_TYPE;
        }
        return TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_ITEM_TYPE) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.contact_item_type_recycler, null, false);
            return new ItemTypeHolder(view);
        }
        ItemHolder itemHolder = new ItemHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.more_activit_item, null));
        itemHolder.itemView.setOnClickListener(mOnClickListener);
        return itemHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ItemHolder) {
            ItemHolder vh = (ItemHolder) viewHolder;
            DataPrototype data = dataList.get(position);
            if (data.imgResId != 0)
                vh.icon.setImageResource(data.imgResId);
            if (data.nameStrResId != 0)
                vh.name.setText(data.nameStrResId);
            vh.itemView.setTag(data);
        } else if (viewHolder instanceof ItemTypeHolder) {
            ItemTypeHolder vh = (ItemTypeHolder) viewHolder;
            DataPrototype data = dataList.get(position);
            vh.itemType.setText(data.itemType);
            vh.contactMore.setVisibility(View.INVISIBLE);
        }

    }


    public class ItemHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        ItemHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
        }
    }

    public class ItemTypeHolder extends RecyclerView.ViewHolder {
        TextView itemType;
        ImageView contactMore;

        ItemTypeHolder(View itemView) {
            super(itemView);
            itemType = itemView.findViewById(R.id.contact_type);
            contactMore = itemView.findViewById(R.id.contact_type_more);
        }
    }
}
