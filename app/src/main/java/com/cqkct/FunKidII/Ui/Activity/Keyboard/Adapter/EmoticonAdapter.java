package com.cqkct.FunKidII.Ui.Activity.Keyboard.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.EmotionUtils;

import java.util.List;


public class EmoticonAdapter extends BaseAdapter {
    private List<String> faceNames;
    private int itemWidth;

    public EmoticonAdapter(View view, List<String> listData, int itemWidth) {
        this.faceNames = listData;
        this.itemWidth = itemWidth;
    }

    @Override
    public int getCount() {
        return faceNames.size();
    }

    @Override
    public String getItem(int position) {
        return faceNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_emoji, parent, false);
        }
        ImageView itemTvEmoji = convertView.findViewById(R.id.itemEmoji);
        // 设置内边距
        itemTvEmoji.setPadding(itemWidth / 4, itemWidth / 4, itemWidth / 4, itemWidth / 4);
        ViewGroup.LayoutParams params = new AbsListView.LayoutParams(itemWidth, itemWidth);
        itemTvEmoji.setLayoutParams(params);

        itemTvEmoji.setImageResource(EmotionUtils.EMOTION_CLASSIC_MAP.get(faceNames.get(position)));

        return convertView;
    }
}
