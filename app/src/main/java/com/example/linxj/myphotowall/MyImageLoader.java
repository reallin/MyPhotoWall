package com.example.linxj.myphotowall;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


/**
 * Created by linxj on 2016/1/10.
 */
public class MyImageLoader {
    private LruCache<String, Bitmap> lru = null;
    private static MyImageLoader myImageLoader = null;

    private ExecutorService mThreadPool;
    private volatile Semaphore mPoolSemphore = new Semaphore(1);
    private volatile Semaphore mSemphore;
    private Type mType;
    private int mNum;//定义线程数量

    private Handler mHandler;
    private Handler mUiHandler;
    private Thread mThread ;//论询线程

    private LinkedList<Runnable> mQueue;

    //定义缓存类型
    private enum Type{
        LIFO,LILO;
    }
    public static MyImageLoader getInstance(int num,Type type){
        if(myImageLoader == null){
            synchronized (MyImageLoader.class){
                if(myImageLoader == null){
                   myImageLoader = new MyImageLoader(num,type);
                }
            }
        }
        return myImageLoader;
    }
    public MyImageLoader(int num,Type type){
        this.mNum = num;
        mPoolSemphore = new Semaphore(mNum);
        mThreadPool = Executors.newFixedThreadPool(this.mNum);//开启线程池
        mQueue = new LinkedList<Runnable>();
        //this.mType = type;
        mType = type == null ? Type.LIFO : type;
        int mMax = (int)Runtime.getRuntime().maxMemory();
        int mMemory = mMax/8;//设置缓存大小
        lru = new LruCache<String,Bitmap>(mMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();//返回缓存的大小
            }
        };
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mUiHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        try {
                            mPoolSemphore.acquire();//防止mThreadPool没定义
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mThreadPool.execute(getTask());

                    }
                };
                mSemphore.release();
                Looper.loop();
            }
        });
        mThread.start();

    }
    private void loadImage(String url,ImageView view){
        view.setTag(url);//设置标志，防止图片错乱
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
               ViewHolder holder = (ViewHolder)msg.obj;
                ImageView view = holder.imageView;
                if(holder.mUrl.equals(view.getTag())) {

                    view.setImageBitmap(holder.bitmap);
                }

            }
        };
        Bitmap bm = lru.get(url);
        if(bm != null){//缓存里有，读缓存
            ViewHolder holder = new ViewHolder();
            holder.imageView = view;
            holder.bitmap = bm;
            holder.mUrl = url;
            Message message = Message.obtain();
            message.obj = holder;
            mHandler.sendMessage(message);
        }else{//缓存没有，加载并存入缓存
            addTask(new Runnable() {
                @Override
                public void run() {
                    Bitmap b = getBitmap();
                    ViewHolder holder = new ViewHolder();
                    holder.imageView = view;
                    holder.bitmap = b;
                    holder.mUrl = url;
                    Message message = Message.obtain();
                    message.obj = holder;
                    mHandler.sendMessage(message);
                }
            });

        }
    }

    private Bitmap getBitmap() {
        return null;
    }

    private synchronized Runnable getTask() {
        if(mQueue != null){
            if(mType == Type.LIFO){
                return mQueue.removeLast();
            }else if(mType == Type.LILO){
                return mQueue.removeFirst();
            }
        }
        return null;
    }
    private synchronized void addTask(Runnable r) {
        if(r != null) {
            mQueue.add(r);
        }
    }
    private class ViewHolder{
        ImageView imageView;
        String mUrl;
        Bitmap bitmap;
    }
}
