package com.chensd.zoomimageview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Created by chensd on 2016/10/21.
 */
public class ViewPagerImgActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
//            getActionBar().setBackgroundDrawable(new ColorDrawable(0x88000000));
//        }
        getSupportActionBar().hide();
        setContentView(R.layout.activity_viewpager);

        ViewPager mVp = (ViewPager) findViewById(R.id.viewpager);

        mVp.setAdapter(new ViewPagerAdapter());
    }

    private static class ViewPagerAdapter extends PagerAdapter{

        private static int[] drawables = {R.drawable.journey, R.mipmap.ic_launcher};
        private final Handler mHandler;

        public ViewPagerAdapter() {
            HandlerThread backgroundThread = new HandlerThread("backgroundThread");
            backgroundThread.start();

            mHandler = new Handler(backgroundThread.getLooper());
        }

        @Override
        public int getCount() {
            return drawables.length;
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            final ZoomImageView zoomImageView = new ZoomImageView(container.getContext());

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = BitmapFactory.decodeResource(zoomImageView.getResources(), drawables[position]);

                    zoomImageView.post(new Runnable() {
                        @Override
                        public void run() {
                            zoomImageView.setImageBitmap(bitmap);
                        }
                    });
                }
            });

            container.addView(zoomImageView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

            return zoomImageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {

            container.removeView((View) object);

            try {
                final ImageView imageView = (ImageView)object;
                Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                imageView.setImageBitmap(null);
                bitmap.recycle();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
