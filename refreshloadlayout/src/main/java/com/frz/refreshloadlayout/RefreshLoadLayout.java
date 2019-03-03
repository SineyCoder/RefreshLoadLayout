package com.frz.refreshloadlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

/**
 * author: siney
 * Date: 2019/3/3
 * description: 使用外部拦截法解决滑动冲突
 */
public class RefreshLoadLayout extends LinearLayout {

    private static final String TAG = "RefreshLoadLayout";
    private static final String REFRESH = "refreshing";
    private static final String LOAD = "loading";
    private static final String BOTH = "both";
    public static final int MOVING = 1, UP = 2;

    public interface OnChangeListener{
        void headerChange(RefreshLoadLayout layout, int nowH, int maxH, int action);
        void footerChange(RefreshLoadLayout layout, int nowH, int maxH, int action);
    }

    public RefreshLoadLayout(Context context) {
        super(context);
    }

    public RefreshLoadLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshLoadLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initParameters(attrs);
    }

    private void initParameters(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RefreshLoadLayout);
        headerId = a.getResourceId(R.styleable.RefreshLoadLayout_header, -1);
        footerId = a.getResourceId(R.styleable.RefreshLoadLayout_footer, -1);
        mode = a.getString(R.styleable.RefreshLoadLayout_mode);
        duration = a.getInt(R.styleable.RefreshLoadLayout_duration, 500);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(!isFirst){
            isFirst = true;
            initChildView();
        }
    }

    private void initChildView() {
        if(headerId == -1 && footerId == -1){
            Log.e(TAG, "Your have to set at least one resource Id");
            return;
        }
        for(int i = 0;i < getChildCount();i++){
            View child = getChildAt(i);
            if(child.getId() == headerId){
                header = child;
                headerMaxH = child.getMeasuredHeight();
                headerNowH = 0;
                setViewHeight(header, 0);
            }else if(child.getId() == footerId){
                footer = child;
                footerMaxH = child.getMeasuredHeight();
                setViewHeight(footer, 0);
            }else if(child instanceof RecyclerView){
                recyclerView = (RecyclerView) child;
            }
        }
    }

    private void setViewHeight(View v, int height) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if(v == header){
            if(height > headerMaxH)headerNowH = headerMaxH;
            else if(height < 0)headerNowH = 0;
            else headerNowH = height;
            lp.height = headerNowH;
        }else if(v == footer){
            if(height > footerMaxH)footerNowH = footerMaxH;
            else if(height < 0)footerNowH = 0;
            else footerNowH = height;
            lp.height = footerNowH;
        }
        v.setLayoutParams(lp);

    }

    private void animClose(final View v, int nowH) {
        ValueAnimator animator = ValueAnimator.ofInt(nowH, 0);
        if(interpolator != null)
            animator.setInterpolator(interpolator);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int vv = (int) animation.getAnimatedValue();
                setViewHeight(v, vv);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimFinish = true;
            }
        });
        animator.setDuration(duration);
        animator.start();
    }

    private int headerId, footerId;

    private String mode;

    private boolean isFirst, isAnimFinish = true;

    private int headerNowH, headerMaxH, footerNowH, footerMaxH;

    private View header, footer;

    private RecyclerView recyclerView;

    private int lastY, duration;

    private OnChangeListener listener;

    private Interpolator interpolator;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(!isAnimFinish)
            return true;
        int y = (int) e.getRawY();
        switch (e.getAction()){
            case MotionEvent.ACTION_MOVE:
                handle(e);
                break;
            case MotionEvent.ACTION_UP:
                if(listener != null){
                    if(headerNowH > 0){
                        isAnimFinish = false;
                        listener.headerChange(this, headerNowH, headerMaxH, UP);
                    }else if(footerNowH > 0){
                        isAnimFinish = false;
                        listener.footerChange(this, footerNowH, footerMaxH, UP);
                    }
                }
                break;
        }
        lastY = y;
        return true;//如果本layout拦截了，那么处理结束就已经消费结束
    }

    private void handle(MotionEvent e) {
        int y = (int) e.getRawY();
        if(REFRESH.equals(mode)){
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if(manager instanceof LinearLayoutManager)
                ((LinearLayoutManager)manager).setStackFromEnd(false);
            if(headerNowH == 0 && (y - lastY) < 0 || (y - lastY) > 0 && recyclerView.canScrollVertically(-1))
                recyclerView.onTouchEvent(e);
            else if(!recyclerView.canScrollVertically(-1)){
                if(listener != null)
                    listener.headerChange(this, headerNowH, headerMaxH, MOVING);
                setViewHeight(header, headerNowH + y - lastY);
            }
        }else if(LOAD.equals(mode)){
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if(manager instanceof LinearLayoutManager)
                ((LinearLayoutManager)manager).setStackFromEnd(true);
            if(footerNowH == 0 && (y - lastY) > 0 || (y - lastY) < 0 && recyclerView.canScrollVertically(1))
                recyclerView.onTouchEvent(e);
            else if(!recyclerView.canScrollVertically(1)){
                if(listener != null)
                    listener.headerChange(this, footerNowH, footerMaxH, MOVING);
                setViewHeight(footer, footerNowH + lastY - y);
            }

        }else if(BOTH.equals(mode)){
            if((y - lastY) > 0 && !recyclerView.canScrollVertically(-1)){//如果下拉，并且不能下滑了，说明到头
                ((LinearLayoutManager)recyclerView.getLayoutManager()).setStackFromEnd(false);
                if(listener != null)
                    listener.headerChange(this, headerNowH, headerMaxH, MOVING);
                setViewHeight(header, headerNowH + y - lastY);
            }else if((y - lastY) < 0 && !recyclerView.canScrollVertically(1)){//如果上拉，并且不能上滑了，说明到底
                ((LinearLayoutManager)recyclerView.getLayoutManager()).setStackFromEnd(true);
                if(listener != null)
                    listener.headerChange(this, footerNowH, footerMaxH, MOVING);
                setViewHeight(footer, footerNowH + lastY - y);
            }else{//其余情况在这里处理
                if(headerNowH > 0){
                    if(listener != null)
                        listener.headerChange(this, headerNowH, headerMaxH, MOVING);
                    setViewHeight(header, headerNowH + y - lastY);
                }else if(footerNowH > 0){
                    if(listener != null)
                        listener.headerChange(this, footerNowH, footerMaxH, MOVING);
                    setViewHeight(footer, footerNowH + lastY - y);
                }else{
                    recyclerView.onTouchEvent(e);
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        boolean intercepted = false;
        int y = (int) e.getRawY();
        switch (e.getAction()){
            case MotionEvent.ACTION_DOWN:
                intercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if(!isAnimFinish){
                    intercepted = true;
                }else if(REFRESH.equals(mode)){
                    if(!recyclerView.canScrollVertically(-1) && (y - lastY) > 0)
                        intercepted = true;
                }else if(LOAD.equals(mode)){
                    if(!recyclerView.canScrollVertically(1) && (lastY - y) > 0)
                        intercepted = true;
                }else if(BOTH.equals(mode)){
                    if(!recyclerView.canScrollVertically(-1) && (y - lastY) > 0 || !recyclerView.canScrollVertically(1) && (lastY - y) > 0)
                        intercepted = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        lastY = y;
        return intercepted;
    }

    //完成加载后finish操作
    public void finish(){
        if(headerNowH >0){
            animClose(header, headerNowH);
        }else if(footerNowH > 0){
            animClose(footer, footerNowH);
        }
    }

    public OnChangeListener getListener() {
        return listener;
    }

    public void setListener(OnChangeListener listener) {
        this.listener = listener;
    }

    public Interpolator getInterpolator() {
        return interpolator;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }
}
