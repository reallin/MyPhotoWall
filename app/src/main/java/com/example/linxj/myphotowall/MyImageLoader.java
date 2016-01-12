package com.example.linxj.myphotowall;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.view.ViewGroup.LayoutParams;

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
    public static final String TAG = "MyImageLoader";

    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 10L;

    //定义缓存类型
    public enum Type{
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
        //mPoolSemphore.release();
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
                         mThreadPool.execute(getTask());
                        try {
                            mPoolSemphore.acquire();//防止mThreadPool没定义
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    }
                };
                mSemphore.release();
                Looper.loop();
            }
        });
        mThread.start();

    }
    public void loadImage(final String url,final ImageView view){
        view.setTag(url);//设置标志，防止图片错乱
        mHandler = new Handler(){


            @Override
            public void handleMessage(Message msg) {
               ViewHolder holder = (ViewHolder)msg.obj;
                ImageView view = holder.imageView;
                if(holder.mUrl.equals(view.getTag())) {

                    view.setImageBitmap(holder.bitmap);
                }else{
                    Log.w(TAG, "set image bitmap,but url has changed, ignored!");
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
                    ImageSize mSize = getImageViewSize(view);

                    int reqWidth = mSize.width;
                    int reqHeight = mSize.height;
                    Bitmap b = decodeSampledBitmapFromResource(url, reqWidth,
                            reqHeight);
                    addBitmapToLruCache(url, b);

                    ViewHolder holder = new ViewHolder();
                    holder.imageView = view;
                    holder.bitmap = b;
                    holder.mUrl = url;
                    Message message = Message.obtain();
                    message.obj = holder;
                    mHandler.sendMessage(message);
                    mPoolSemphore.release();
                }
            });

        }
    }
/**
 * 压缩图片
 */
    public Bitmap decodeSampledBitmapFromResource(String url,int width,int height) {
       final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        BitmapFactory.decodeFile(url,options);
        options.inSampleSize = calculateInSampleSize(options, width,
                height);
        options.inJustDecodeBounds = true;
        Bitmap bitmap =BitmapFactory.decodeFile(url, options);
        return bitmap;
    }

    /**
     * 压缩图片
     * @param options
     * @param width
     * @param height
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int width, int height) {
        int mWidth = options.outWidth;
        int mHeight = options.outHeight;
        int sampleSize = 1;
        int sampleSizeWidth = Math.round((float)mWidth/width);
        int sampleSizeHeight = Math.round((float) mHeight / height);
         sampleSize = Math.max(sampleSizeWidth,sampleSizeHeight)         ;
        return sampleSize;

    }

    /**
     * 获取ImageView的最大大小
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public ImageSize getImageViewSize(ImageView imageView){
        ImageSize mSize = new ImageSize();
        DisplayMetrics metrics = imageView.getContext().getResources().getDisplayMetrics();
       final LayoutParams lp = imageView.getLayoutParams();
        int width;
        width = imageView.getWidth();
        if(width <= 0){
            width = lp.width;
        }if(width <= 0){//wrapcontent
            width = imageView.getMaxWidth();
        }
        if(width <= 0){
            width = metrics.widthPixels;
        }
        int height;
        height = imageView.getHeight();
        if(height <= 0){
            height = lp.height;
        }if(height <= 0){//wrapcontent
            height = imageView.getMaxHeight();
        }
        if(height <= 0){
            height = metrics.heightPixels;
        }
        mSize.width = width;
        mSize.height = height;
        return mSize;
    }
    private Bitmap getBitmap() {
        return null;
    }
    /**
     * 存入缓存
     */
    private void addBitmapToLruCache(String url,Bitmap bm){
        if(lru.get(url) == null) {
            if(bm !=null) {
                lru.put(url, bm);
            }
        }
    }
    /**
     * 添加任务到队列
     * @return
     */

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
        if(mUiHandler != null){
            mSemphore.release();
        }
        mUiHandler.sendEmptyMessage(0x110);
    }
    private class ViewHolder{
        ImageView imageView;
        String mUrl;
        Bitmap bitmap;
    }
    private class ImageSize{
        int width;
        int height;

    }
}
