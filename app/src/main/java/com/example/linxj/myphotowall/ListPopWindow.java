package com.example.linxj.myphotowall;


import android.view.View;


import java.util.List;

/**
 * Created by linxj on 2016/1/12.
 */
public class ListPopWindow<T> extends BasePopWindow<ImageFloder> {


    public ListPopWindow(int width, int height,
                                   List<ImageFloder> datas, View convertView)
    {
        super(convertView, width, height, true, datas);
    }


    @Override
    protected void initViews() {

    }

    @Override
    protected void initEvents() {

    }

    @Override
    protected void init() {

    }

    @Override
    public void beforeInitWeNeedSomeParams(Object[] params) {

    }
}
