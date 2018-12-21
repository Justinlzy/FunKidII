package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.db.Entity.BabyEntity;

import java.util.List;

/**
 * Created by justin on 2017/9/13.
 */

public class RelationUserAdapter extends BaseAdapter {
    private Context mContext;
    private List<BabyEntity> listBeen;

    public RelationUserAdapter(Context context, List<BabyEntity> list) {
        this.mContext = context;
        this.listBeen = list;
    }

    @Override
    public int getCount() {
        return listBeen.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        HolderView holderView = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.relation_user_item, null);
            holderView = new HolderView();
            holderView.number = (TextView) convertView.findViewById(R.id.relation_number);
            holderView.relation = (TextView) convertView.findViewById(R.id.relation_relation);
            convertView.setTag(holderView);
        } else {
            holderView = (HolderView) convertView.getTag();
        }
        holderView.number.setText(listBeen.get(position).getUserId());
        holderView.relation.setText(listBeen.get(position).getRelation());
        return convertView;
    }

    class HolderView {
        public TextView number;
        public TextView relation;
    }
}
