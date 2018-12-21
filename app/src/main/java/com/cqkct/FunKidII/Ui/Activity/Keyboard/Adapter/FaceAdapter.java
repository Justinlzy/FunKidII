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

/**
 * 表情区域GridView的适配器
 *
 * @author kymjs (http://www.kymjs.com/)
 */
public class FaceAdapter extends BaseAdapter implements AbsListView.OnScrollListener {
    private List<String> emotionNames;
    private AbsListView mList;
    private boolean isScrolling;
    private AbsListView.OnScrollListener listener;
    protected LayoutInflater mInflater;

    public FaceAdapter(AbsListView view, List<String> list) {
        this.emotionNames = list;
        this.mList = view;
        mList.setOnScrollListener(this);
        this.mInflater = LayoutInflater.from(view.getContext());
    }

    @Override
    public int getCount() {
        return emotionNames.size();
    }

    @Override
    public Object getItem(int position) {
        return emotionNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_face, parent, false);
        }
        ImageView imageView = convertView.findViewById(R.id.itemImage);
        imageView.setImageResource(EmotionUtils.EMOTION_CLASSIC_MAP.get(emotionNames.get(position)));
        return convertView;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == 0) {
            this.isScrolling = false;
            this.notifyDataSetChanged();
        } else {
            this.isScrolling = true;
        }

        if (this.listener != null) {
            this.listener.onScrollStateChanged(view, scrollState);
        }
    }

    public void addOnScrollListener(AbsListView.OnScrollListener l) {
        this.listener = l;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (this.listener != null) {
            this.listener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }
}