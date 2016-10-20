package com.chensd.zoomimageview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by chensd on 2016/10/18.
 */
public class ZoomImageView extends ImageView implements ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener, ViewTreeObserver.OnGlobalLayoutListener {

    private static final String TAG = ZoomImageView.class.getSimpleName();

    public static final float SCALE_MAX = 4.0F;
    public static final float SCALE_MID = 1.5F;

    private float initScale = 1.0F;

    private final float[] matrixValues = new float[9];

    private boolean once = true;

    private final Matrix mScaleMatrix = new Matrix();
    private ScaleGestureDetector mScaleGestureDetector;
    private boolean isCheckTopAndButtom = true;
    private boolean isCheckLeftAndRight = true;
    private int lastPointerCount;
    private boolean isCanDrag;
    private float mLastX, mLastY;
    private boolean isAutoScale;
    private GestureDetector mGestureDetector;

    public ZoomImageView(Context context) {
        super(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setScaleType(ScaleType.MATRIX);

        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isAutoScale == true)
                    return true;

                float x = e.getX();
                float y = e.getY();

                if(getScale() < SCALE_MID){
                    isAutoScale = true;
                    ZoomImageView.this.postDelayed(new AutoScaleRunnable(SCALE_MID, x, y), 16);
                }else if(getScale() >= SCALE_MID && getScale() < SCALE_MAX){
                    isAutoScale = true;
                    ZoomImageView.this.postDelayed(new AutoScaleRunnable(SCALE_MAX, x, y), 16);
                }else{
                    isAutoScale = true;
                    ZoomImageView.this.postDelayed(new AutoScaleRunnable(initScale, x, y), 16);
                }

                return true;
            }
        });
        this.setOnTouchListener(this);
    }

    private class AutoScaleRunnable implements Runnable{

        static final float BIGGER = 1.07F;
        static final float SMALLER = 0.93F;

        private float mTargetScale;
        private float tmpScale;

        /**
         * 缩放中心点
         */
        private float x;
        private float y;

        public AutoScaleRunnable(float targetScale, float x, float y){
            this.mTargetScale = targetScale;
            this.x = x;
            this.y = y;

            if(getScale() < targetScale){
                tmpScale = BIGGER;
            }else{
                tmpScale = SMALLER;
            }
        }

        @Override
        public void run() {

            //进行缩放
            mScaleMatrix.postScale(tmpScale, tmpScale, x, y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);

            final float currentScale = getScale();
            Log.e("currentScale:", currentScale + "");
            //如果在合法范围内，继续缩放
            if(((tmpScale > 1f) && (currentScale < mTargetScale) ||
                    ((tmpScale < 1f) && (currentScale > mTargetScale)))){

//                ZoomImageView.this.postDelayed(this, 10);
                postOnAnimation(ZoomImageView.this, this);
            }else{
                final float deltaScale = mTargetScale / currentScale;
                mScaleMatrix.postScale(deltaScale, deltaScale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);

                isAutoScale = false;
            }


        }
    }

    private void postOnAnimation(View v, Runnable autoScaleRunnable) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            v.postOnAnimation(autoScaleRunnable);
        }else{
            v.postDelayed(autoScaleRunnable, 16);
        }
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();
        Log.e("initScale:", initScale + "");

        if (getDrawable() == null)
            return true;

        //范围控制
        if ((scale < SCALE_MAX && scaleFactor > 1.0F) || (scale > (initScale/2) && scaleFactor < 1.0F)) {

            /**
             * scaleFactor 返回即时的两点之间的数据
             * scale 不一样，返回的最后的x的缩放的倍数
             */
//            if (scaleFactor * scale < initScale) {
//                scaleFactor = initScale / scale;  // scale越来越小 ，最终缩小到原值，与initScale 相同，结果相除为1
////                scaleFactor = 1.0F;
//            }
//
//            if (scaleFactor * scale > SCALE_MAX) {
//                scaleFactor = SCALE_MAX / scale;  //
//                Log.e("scaleFactor：", scaleFactor + "");
//            }

            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
        }
        Log.e("scale：", scale + "");
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    //获取当前x的缩放比例
    private float getScale() {
        mScaleMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event))
            return true;

        mScaleGestureDetector.onTouchEvent(event);

        //增加移动 只有放大 长或者宽大于屏幕的长宽时才可以移动
        float x = 0, y = 0;
        //拿到触摸的点的个数
        final int pointerCount = event.getPointerCount();

        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }

        x = x / pointerCount;
        y = y / pointerCount;

        if (pointerCount != lastPointerCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }

        lastPointerCount = pointerCount;

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag) {
                    isCanDrag = isCanDrag(dx, dy);

                }

                if(isCanDrag){
                    RectF rectF = getMatrixRectF();
                    if(getDrawable() != null){
                        isCheckLeftAndRight = isCheckTopAndButtom = true;
                        // 如果宽度小于屏幕宽度，则禁止左右移动
                        if(rectF.width() < getWidth()){
                            dx = 0;
                            isCheckLeftAndRight = false;
                        }
                        // 如果高度小于屏幕宽度，则禁止上下移动
                        if(rectF.height() < getHeight()){
                            dy = 0;
                            isCheckTopAndButtom = false;
                        }

                        mScaleMatrix.postTranslate(dx, dy);
                        checkMatrixBounds();
                        setImageMatrix(mScaleMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                //当缩放的值小于初始值时就还原
                if(getScale() < initScale){
                    isAutoScale = true;
                    ZoomImageView.this.postDelayed(new AutoScaleRunnable(initScale, getWidth()/2, getHeight()/2), 16);
                }
            case MotionEvent.ACTION_CANCEL:
                lastPointerCount = 0;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onGlobalLayout() {
        if (once) {
            Drawable d = getDrawable();
            if (d == null)
                return;

            int width = getWidth();
            int height = getHeight();

            //
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();

            float scale = 1.0F;

            //如果宽度大于屏幕 高度小于屏幕宽度 就按宽度缩放到屏幕宽度
            if (dw > width && dh <= height) {
                scale = width * 1.0F / dw;
            }

            //如果高度大于屏幕 宽度小于屏幕宽度 就按高度缩放到屏幕高度
            if (dh > height && dw <= width) {
                scale = height * 1.0F / dh;
            }

            //按照一定比例缩放 选择一个更小的进行缩放
            if (dw > height && dh > height) {
                scale = Math.min(width * 1.0F / dw, height * 1.0F / dh);
            }

            initScale = scale;

            //想移动到哪个点 就把终点坐标减去原点坐标
            mScaleMatrix.postTranslate((width - dw) / 2, (height - dh) / 2);
            //开始执行缩放
            mScaleMatrix.postScale(scale, scale, getWidth() / 2, getHeight() / 2);

            setImageMatrix(mScaleMatrix);
            once = false;

        }
    }

    private RectF getMatrixRectF() {
        RectF rectF = new RectF();
        Drawable d = getDrawable();
        if (d != null) {
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            mScaleMatrix.mapRect(rectF);
        }
        return rectF;
    }

    //检查缩放边界白边问题
    private void checkBorderAndCenterWhenScale() {
        RectF rect = getMatrixRectF();

        float deltaX = 0f;
        float deltaY = 0f;

        int width = getWidth();
        int height = getHeight();

        //
        if (rect.width() >= width) {
            if (rect.left > 0) {
                deltaX = -rect.left;
            }

            if (rect.right < width) {
                deltaX = width - rect.right;
            }

        }

        if (rect.height() >= height) {
            if (rect.top > 0) {
                deltaY = -rect.top;
            }

            if (rect.bottom < height) {
                deltaY = height - rect.bottom;
            }
        }

        if (rect.width() < width) {
            deltaX = width * 0.5f - rect.right + 0.5f * rect.width();
        }

        if (rect.height() < height) {
            deltaY = height * 0.5f - rect.bottom + 0.5f * rect.height();
        }

        mScaleMatrix.postTranslate(deltaX, deltaY);

    }

    /**
     * 判断移动时的边界问题
     */
    private void checkMatrixBounds() {
        RectF rect = getMatrixRectF();

        float deltaX = 0, deltaY = 0;
        final float viewHeight = getHeight();
        final float viewWidth = getWidth();

        if (rect.top > 0 && isCheckTopAndButtom) {
            deltaY = -rect.top;
        }

        if (rect.bottom < viewHeight && isCheckTopAndButtom) {
            deltaY = viewHeight - rect.bottom;
        }

        if (rect.left > 0 && isCheckLeftAndRight) {
            deltaX = -rect.left;
        }

        if (rect.right < viewWidth && isCheckLeftAndRight) {
            deltaX = viewWidth - rect.right;
        }

        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 判断是否在拖动
     */
    private boolean isCanDrag(float dx, float dy) {
        return Math.sqrt((dx * dx) + (dy * dy)) > 0;
    }
}
