package com.cqkct.FunKidII.Ui.Adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.MoreFunction.CollectPraiseEditActivity;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import protocol.Message;

/**
 * Created by justin on 2017/11/8.
 */

public class PraiseEditViewAdapter extends RecyclerView.Adapter {

    private OnPraiseEditNameClickListener listener;

    public interface OnPraiseEditNameClickListener {
        void onPraiseEditNameClick(View view, int position);
        void nPraiseEditItemClick(View view, int position);
    }

    public class Item {
        public static final int ITEM_HEAD = 0;
        public static final int ITEM_ITEM = 1;
//        public static final int PRIZE_HEAD = 2;
//        public static final int PRIZE_EDITOR = 3;
//        public static final int BUTTON = 4;
//        public static final int HISTORY_HEAD = 5;
        public static final int HISTORY_ITEM = 6;

        public int type;
        public Object dat;

        public Item(int type) {
            this.type = type;
        }

        public Item(int type, Object data) {
            this.type = type;
            this.dat = data;
        }
    }

    private boolean itemEditable;

    @NonNull
    private final List<Item> datalist;

    public PraiseEditViewAdapter(@NonNull List<Item> dataList) {
        this.datalist = dataList;
    }

    public void setItemEditable(boolean editable) {
        itemEditable = editable;
    }

    public void addPraiseEditNameClickListener(OnPraiseEditNameClickListener onPraiseEditNameClickListener) {
        this.listener = onPraiseEditNameClickListener;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return datalist.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case Item.ITEM_HEAD: {
                PraiseItemHead holder = createItemViewHolder(parent);
                if (itemEditable)
                    holder.llItem.setOnClickListener(v -> listener.onPraiseEditNameClick(holder.llItem, holder.getAdapterPosition()));
                return holder;
            }
            case Item.ITEM_ITEM: {
                PraiseItemViewHolder holder = createPraiseItemViewHolder(parent);
                holder.itemView.setOnClickListener(v -> listener.nPraiseEditItemClick(holder.itemView, holder.getAdapterPosition()));
                return holder;
            }
//            case Item.PRIZE_HEAD:
//                return createEditPraiseItemHeadViewHolder(parent);
//            case Item.PRIZE_EDITOR:
//                return getPrizeEditorView(item, convertView, parent);
//            case Item.BUTTON:
//                return getButtonView(item, convertView, parent);
//            case Item.HISTORY_HEAD:
//                return getHistoryHeadView(item, convertView, parent);
//            case Item.HISTORY_ITEM:
//                return getHistoryItemView(item, convertView, parent);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = datalist.get(position);
        switch (item.type) {
            case Item.ITEM_HEAD:
                bindItemHeadView((PraiseItemHead) holder);
                break;
            case Item.ITEM_ITEM:
                bindPraiseItemViewHolder((PraiseItemViewHolder) holder, item);
                break;
//            case Item.PRIZE_HEAD:
//                  bindEditPraiseItem((EditPraiseItemHeadViewHolder) holder,item);
//                  break;
//            case Item.PRIZE_EDITOR:
//                return getPrizeEditorView(item, convertView, parent);
//                break;
//            case Item.BUTTON:
//                return getButtonView(item, convertView, parent);
//                break;
//            case Item.HISTORY_HEAD:
//                return getHistoryHeadView(item, convertView, parent);
//                break;
//            case Item.HISTORY_ITEM:
//                return getHistoryItemView(item, convertView, parent);
//                break;
        }
    }

//    @Override
//    public int getItemViewType(int position) {
//        switch (datalist.get(position).type) {
//            case Item.ITEM_ITEM:
//                return 1;
//            case Item.HISTORY_ITEM:
//                return 2;
//            default:
//                return 0;
//        }
//    }


    @Override
    public int getItemViewType(int position) {
        return datalist.get(position).type;
    }


    private PraiseItemViewHolder createPraiseItemViewHolder(ViewGroup parent) {
        View convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_praise_adapter_praise_item, parent, false);
        return new PraiseItemViewHolder(convertView);
    }

    class PraiseItemViewHolder extends RecyclerView.ViewHolder {
        TextView praiseName;
        public PraiseItemViewHolder(View itemView) {
            super(itemView);
            praiseName = itemView.findViewById(R.id.item_name);
        }
    }

    private void bindPraiseItemViewHolder(PraiseItemViewHolder holder, Item item) {
        holder.praiseName.setText(((Message.Praise.Item.Builder) item.dat).getName());
    }



    private PraiseItemHead createItemViewHolder(ViewGroup parent) {
        View convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_praise_adapter_praise_item_head, parent, false);
        return new PraiseItemHead(convertView);
    }

    class PraiseItemHead extends RecyclerView.ViewHolder {
        View addIcon;
        LinearLayout llItem;

        public PraiseItemHead(View itemView) {
            super(itemView);
            llItem = itemView.findViewById(R.id.edit_praise_item);
            addIcon = itemView.findViewById(R.id.title_bar_right_icon);
        }
    }

    private void bindItemHeadView(PraiseItemHead holder) {
        holder.addIcon.setVisibility(itemEditable ? View.VISIBLE : View.GONE);
    }


    private RecyclerView.ViewHolder createEditPraiseItemHeadViewHolder(ViewGroup parent) {
        View convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_praise_adapter_praise_item, parent, false);
        return new EditPraiseItemHeadViewHolder(convertView);
    }

    class EditPraiseItemHeadViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageButton imageButton;

        public EditPraiseItemHeadViewHolder(View itemView) {
            super(itemView);
//            text = itemView.findViewById(R.id.item_text);
//            imageButton = itemView.findViewById(R.id.item_detail);
        }
    }

    private void bindEditPraiseItem(EditPraiseItemHeadViewHolder holder, Item item) {
        holder.text.setText(((Message.Praise.Item.Builder) item.dat).getName());
        holder.imageButton.setVisibility(itemEditable ? View.VISIBLE : View.GONE);
    }


    private View getPrizeHeadView(Item item, View convertView, ViewGroup parent) {
        if (convertView == null || convertView.getTag() == null || item.type != (Integer) convertView.getTag()) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_praise_adapter_prize_head, parent, false);
            convertView.setTag(item.type);
        }
        return convertView;
    }
    /*-----------------------------*/

    private RecyclerView.ViewHolder createPrizeItemHeadViewHolder(ViewGroup parent) {
        View convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_praise_adapter_prize_editor_container, parent, false);
        return new PraiseItemViewHolder(convertView);
    }

    class PrizeItemHeadViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageButton imageButton;

        public PrizeItemHeadViewHolder(View itemView) {
            super(itemView);
//            text = itemView.findViewById(R.id.item_text);
//            imageButton = itemView.findViewById(R.id.item_detail);
        }
    }

    private void bindPrizeItemHeadViewHolder(PrizeItemHeadViewHolder holder, Item item) {
        holder.text.setText(((Message.Praise.Item.Builder) item.dat).getName());
        holder.imageButton.setVisibility(itemEditable ? View.VISIBLE : View.GONE);
    }


    private View getPrizeEditorView(Item item, View convertView, ViewGroup parent) {
        if (convertView == null || convertView.getTag() == null || item.type != (Integer) convertView.getTag()) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edit_praise_adapter_prize_editor_container, parent, false);
            CollectPraiseEditActivity.PrizeEditorViewHolder editViewHolder = (CollectPraiseEditActivity.PrizeEditorViewHolder) item.dat;
            if (editViewHolder.parent != null) {
                editViewHolder.parent.removeView(editViewHolder.view);
            }
            editViewHolder.parent = (ViewGroup) convertView;
            editViewHolder.parent.addView(editViewHolder.view);
            convertView.setTag(item.type);
        }
        return convertView;
    }


}
