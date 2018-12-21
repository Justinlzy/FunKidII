package com.cqkct.FunKidII.Ui.Activity;

import android.os.Bundle;
import android.widget.ListView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.RelationUserAdapter;
import com.cqkct.FunKidII.Utils.GreenUtils;
import com.cqkct.FunKidII.db.Dao.BabyEntityDao;
import com.cqkct.FunKidII.db.Entity.BabyEntity;

import org.greenrobot.greendao.query.Query;

import java.util.List;


/**
 * Created by justin on 2017/9/13.
 */

public class RelationUserActivity extends BaseActivity {
    private ListView listView;
    private RelationUserAdapter relationUserAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relation_user);
        setTitleBarTitle(R.string.title_associate_user);
        init();
        getRelationData();
    }

    private void getRelationData() {
        BabyEntityDao babyBeanDao = GreenUtils.getBabyEntityDao();
        Query query = babyBeanDao.queryBuilder().build();
        List<BabyEntity> list = query.list();
        if (list.size() > 0) {
            relationUserAdapter = new RelationUserAdapter(this, list);
            listView.setAdapter(relationUserAdapter);
        }
    }

    private void init() {
        listView = (ListView) findViewById(R.id.relation_baby_list);
    }

}
