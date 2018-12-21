package com.cqkct.FunKidII.Ui.Adapter;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

/**
 * @param <V>
 * @author chendalin
 */
public class CalendarViewAdapter<V extends View> extends PagerAdapter {

    public static final String TAG = "CalendarViewAdapter";
    private V[] views;

    public CalendarViewAdapter(V[] views) {
        super();
        this.views = views;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        if (((ViewPager) container).getChildCount() == views.length) {
            ((ViewPager) container).removeView(views[position % views.length]);
        }

        ((ViewPager) container).addView(views[position % views.length], 0);
        return views[position % views.length];
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((View) object);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((View) container);
    }

    public V getItem(int position) {
        try {
            return views[position % views.length];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public V[] getAllItems() {
        return views;
    }

}
