package com.cqkct.FunKidII.Ui.Adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.cqkct.FunKidII.Bean.BaseBean;

import org.greenrobot.greendao.query.LazyList;


public abstract class LazyListAdapter<T extends BaseBean> extends BaseAdapter {

    protected boolean dataValid;
    protected LazyList<T> lazyList;
    protected Context context;

    public LazyListAdapter(Context context, LazyList<T> lazyList) {
        this.lazyList = lazyList;
        this.dataValid = lazyList != null;
        this.context = context;
    }

    public void setLazyList(LazyList<T> list) {
        if (list != lazyList) {
            if (lazyList != null) {
                lazyList.close();
            }
            lazyList = list;
            this.dataValid = lazyList != null;
            notifyDataSetChanged();
        }
    }

    /**
     * Returns the list.
     *
     * @return the list.
     */
    public LazyList<T> getLazyList() {
        return lazyList;
    }

    /**
     * @see android.widget.ListAdapter#getCount()
     */
    @Override
    public int getCount() {
        if (dataValid && lazyList != null) {
            return lazyList.size();
        } else {
            return 0;
        }
    }

    /**
     * @see android.widget.ListAdapter#getItem(int)
     */
    @Override
    public T getItem(int position) {
        if (dataValid && lazyList != null) {
            return lazyList.get(position);
        } else {
            return null;
        }
    }

    /**
     * @see android.widget.ListAdapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        if (dataValid && lazyList != null) {
            T item = lazyList.get(position);
            if (item != null) {
                return item.getId();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * @see android.widget.ListAdapter#getView(int, View, ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {

        if (!dataValid) {
            throw new IllegalStateException("this should only be called when lazylist is populated");
        }

        T item = lazyList.get(position);
        if (item == null) {
            throw new IllegalStateException("Item at position " + position + " is null");
        }

        View v;
        if (convertView == null) {
            v = newView(position, context, item, parent);
        } else {
            v = convertView;
        }
        bindView(v, position, context, item);
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        if (dataValid) {
            T item = lazyList.get(position);
            View v;
            if (convertView == null) {
                v = newDropDownView(position, context, item, parent);
            } else {
                v = convertView;
            }
            bindView(v, position, context, item);
            return v;
        } else {
            return null;
        }
    }

    /**
     * Makes a new view to hold the data contained in the item.
     *
     * @param context Interface to application's global information
     * @param item    The object that contains the data
     * @param parent  The parent to which the new view is attached to
     * @return the newly created view.
     */
    public abstract View newView(int position, Context context, T item, ViewGroup parent);

    /**
     * Makes a new drop down view to hold the data contained in the item.
     *
     * @param context Interface to application's global information
     * @param item    The object that contains the data
     * @param parent  The parent to which the new view is attached to
     * @return the newly created view.
     */
    public View newDropDownView(int position, Context context, T item, ViewGroup parent) {
        return newView(position, context, item, parent);
    }

    /**
     * Bind an existing view to the data data contained in the item.
     *
     * @param view    Existing view, returned earlier by newView
     * @param context Interface to application's global information
     * @param cursor  The object that contains the data
     */
    public abstract void bindView(View view, int position, Context context, T item);

}