package com.cqkct.FunKidII.Ui.Adapter;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by justin on 2017/8/7.
 */
public class DotPagerAdapter extends PagerAdapter {
    private List<View> datas = new ArrayList<>();

    public DotPagerAdapter(Activity c) {
        Typeface typeFace = Typeface.createFromAsset(c.getAssets(), "font/simsun.ttf");

        int[] views = new int[]{R.layout.banner_one, R.layout.banner_two, R.layout.banner_three};
        for (int i = 0; i < views.length; i++) {

            View view = LayoutInflater.from(c).inflate(views[i], null);
            if (i < views.length - 1) {
                TextView textViewZH1 = view.findViewById(R.id.guide_text_zh_one);
                if (((BaseActivity) c).getCurrentLanguageUseResources().toLowerCase().contains("zh")) {
//                    中文状态 则显示 textViewZH
                    textViewZH1.setVisibility(View.VISIBLE);
                } else {
//                    否则显示英文
//                    textViewEN.setVisibility(View.VISIBLE);
//                    textViewEN1.setVisibility(View.VISIBLE);
//                    textViewEN2.setVisibility(View.VISIBLE);
//                    textViewEN3.setVisibility(View.VISIBLE);
//                    if(i == 1){ textViewEN4.setVisibility(View.VISIBLE);}
                }
//                if (Build.VERSION.SDK_INT > 19) {
//                    textViewZH1.setTypeface(typeFace);
//                    textViewZH2.setTypeface(typeFace);
//                    if (i == 2) {
//                        textViewZH3.setTypeface(typeFace);
//                    }

//                    textViewEN.setTypeface(typeFace);
//                    textViewEN1.setTypeface(typeFace);
//                    textViewEN2.setTypeface(typeFace);
//                    textViewEN3.setTypeface(typeFace);
//                    if(i == 1){textViewEN4.setTypeface(typeFace);}
//                }
            }
//            else {
//                TextView textViewZH = (TextView) view.findViewById(R.id.guide_text);
//                TextView textView1 = (TextView) view.findViewById(R.id.guide_text2);
//                if (Build.VERSION.SDK_INT > 19) {
//                    textViewZH.setTypeface(typeFace);
//                    textView1.setTypeface(typeFace);
//                }
//            }

            datas.add(view);
        }

    }

    @Override
    public int getCount() {
        return datas.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(datas.get(position));
//        super.destroyItem(container, position, object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(datas.get(position));
        return datas.get(position);
    }
}
