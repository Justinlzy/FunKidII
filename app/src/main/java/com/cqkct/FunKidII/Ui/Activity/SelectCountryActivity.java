package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.PersonBean;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.CountryCodeAdapter;
import com.cqkct.FunKidII.Ui.view.SideBar;
import com.cqkct.FunKidII.Utils.PingYin;
import com.cqkct.FunKidII.Utils.PinyinComparator;
import com.cqkct.FunKidII.db.Entity.BabyEntity;
import com.cqkct.FunKidII.service.Pkt;
import com.cqkct.FunKidII.service.tlc.TlcService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import protocol.Message;

/**
 * Created by Justin on 2017/7/26.
 */

public class SelectCountryActivity extends BaseActivity {
    private ListView listView;
    private CountryCodeAdapter countryCodeAdapter;
    private List<PersonBean> data;
    //    private static final int[] rawData = new int[]{
//            R.string.register_login_macao_district,
//            R.string.register_login_china,
//            R.string.register_login_vietnam,
//            R.string.register_login_indonesia,
//            R.string.register_login_india,
//            R.string.register_login_iran,
//            R.string.register_login_singapore,
//            R.string.register_login_hk_district,
//            R.string.register_login_taiwan_district,
//            R.string.register_login_thailand,
//            R.string.register_login_burma,
//            R.string.register_login_american,
//            R.string.register_login_malaysia,
//            R.string.register_login_laos,
//            R.string.register_login_cambodia,
//            R.string.register_login_philippines,
//    };
    private static final int[] rawData = new int[]{
            R.string.register_login_macao_district,
            R.string.register_login_china,
            R.string.register_login_hk_district,
            R.string.register_login_taiwan_district,
            R.string.register_login_american,
            R.string.register_login_canada,
            R.string.register_login_england,

    };

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.select_country);
        setTitleBarTitle(R.string.register_login_select_county_or_district);
        init();
    }

    private List<PersonBean> getData() {
        List<PersonBean> personBeans = new ArrayList<PersonBean>();
        for (int strResId : rawData) {
            String name = getString(strResId);
            String pinyin = PingYin.getPingYin(name);
            String Fpinyin = pinyin.substring(0, 1).toUpperCase();

            PersonBean person = new PersonBean();
            person.setId(strResId);
            person.setName(name);
            person.setPinYin(pinyin);
            //正则表达式，判断首字母是否是英文字母
            if (Fpinyin.matches("[A-Z]")) {
                person.setFirstPinYin(Fpinyin);
            } else {
                person.setFirstPinYin("#");
            }
            personBeans.add(person);
        }
        return personBeans;

    }

    private void init() {
        SideBar sidebar = (SideBar) findViewById(R.id.sidebar);
        listView = (ListView) findViewById(R.id.listview);
        sidebar.setTextView((TextView) findViewById(R.id.dialog));
//        隐藏普通滚动条
        listView.setVerticalScrollBarEnabled(false);
//         隐藏快速滚动条
        listView.setFastScrollEnabled(false);
        // 设置字母导航触摸监听
        sidebar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                // TODO Auto-generated method stub
                // 该字母首次出现的位子
                int position = countryCodeAdapter.getPositionForSelection(s.charAt(0));

                if (position != -1) {
                    listView.setSelection(position);
                }
            }
        });
        data = getData();
        //数据放在adapter之前需要排序
        Collections.sort(data, new PinyinComparator());
        countryCodeAdapter = new CountryCodeAdapter(this, data);
        listView.setAdapter(countryCodeAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("countryName", data.get(position).getId());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public void onTitleBarClick(View v) {
        switch (v.getId()) {
            case R.id.title_bar_left_icon:
                setResult(RESULT_CANCELED);
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoggedout(TlcService tlcService, @Nullable String userId, boolean isSticky) {
    }

    @Override
    protected boolean finishWhenCurrentBabySwitched(@Nullable BabyEntity oldBabyBean, @Nullable BabyEntity newBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    protected boolean finishWhenNoMoreBaby(@Nullable BabyEntity oldBabyBean, boolean isSticky) {
        return false;
    }

    @Override
    public void onBindRequest(@NonNull String toUser, @NonNull Pkt reqPkt, @NonNull Message.NotifyAdminBindDevReqMsg reqMsg, @NonNull Message.FetchUsrDevParticRspMsg usrDevPartic) {
    }

    @Override
    public boolean shouldShowExtrudedLoggedOut() {
        return false;
    }

    protected boolean shouldShowServerApiNotCompat() {
        return false;
    }
}
