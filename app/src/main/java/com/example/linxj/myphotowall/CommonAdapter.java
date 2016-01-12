package com.example.linxj.myphotowall;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Created by linxj on 2016/1/12.
 */
public abstract class CommonAdapter<T> extends BaseAdapter {
    @Override
    public int getCount() {
        return 0;
    }
    public abstract void convert( ImageFloder item);
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

        return null;
    }
}
