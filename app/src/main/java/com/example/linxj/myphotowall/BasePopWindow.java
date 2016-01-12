package com.example.linxj.myphotowall;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.DimenRes;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import java.util.List;

/**
 * Created by linxj on 2016/1/12.
 */
public abstract class BasePopWindow<T> extends PopupWindow {
    /**
     * 布局文件的最外层View
     */
    protected View mContentView;
    protected Context context;
    /**
     * ListView的数据集
     */
    protected List<T> mDatas;
    public BasePopWindow(View contentView, int width, int height,
                                      boolean focusable)
    {
        this(contentView, width, height, focusable, null);
    }

    public BasePopWindow(View contentView, int width, int height,
                                      boolean focusable, List<T> mDatas)
    {
        this(contentView, width, height, focusable, mDatas, new Object[0]);

    }
    public BasePopWindow(View contentView, int width, int height,
                                      boolean focusable, List<T> mDatas, Object... params) {
        super(contentView, width, height, focusable);
        this.mContentView = contentView;
        context = contentView.getContext();
        if (mDatas != null)
            this.mDatas = mDatas;

        if (params != null && params.length > 0) {
            beforeInitWeNeedSomeParams(params);
        }
        setBackgroundDrawable(new BitmapDrawable());
        setTouchable(true);
        setOutsideTouchable(true);
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initViews();
        initEvents();
        init();
    }

    protected abstract void initViews();
    protected abstract void initEvents();
    protected abstract void init();
    public abstract void beforeInitWeNeedSomeParams(Object[] params) ;

}
