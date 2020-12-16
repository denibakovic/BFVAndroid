package com.bfv.BFVAndroid.fragments.dashboard;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;


public class RawDataRecyclerView extends RecyclerView implements View.OnLongClickListener {

    private RawDataRecyclerView.LongClickListener mLongClickListener;
    private GestureDetector mGestureDetector;


    public RawDataRecyclerView(@NonNull Context context) {
        super(context);
        init(context, this);
    }

    public RawDataRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, this);
    }

    public RawDataRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, this);
    }


    private void init(Context context, View view) {
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            final Handler handler = new Handler();
            final Runnable mLongPressed = () -> {if(mLongClickListener != null) {
                mLongClickListener.onRawDataLongClick(view);}};

            @Override
            public void onLongPress(MotionEvent e) {
                if(e.getAction() == MotionEvent.ACTION_DOWN)
                    handler.postDelayed(mLongPressed, ViewConfiguration.getLongPressTimeout());
                if((e.getAction() == MotionEvent.ACTION_MOVE)||(e.getAction() == MotionEvent.ACTION_UP))
                    handler.removeCallbacks(mLongPressed);
            }
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(mGestureDetector.onTouchEvent(e)) {
            return true;
        }
        return super.onTouchEvent(e);
    }

    /**
     * allows clicks events to be caught
     * @param rawDataLongClickListener click listener to set
     */
    public void setLongClickListener(RawDataRecyclerView.LongClickListener rawDataLongClickListener) {
        this.mLongClickListener = rawDataLongClickListener;
    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }


    /**
     * Parent activity/fragment will implement this interface to respond to click events
     */
    public interface LongClickListener {
        void onRawDataLongClick(View view);
    }
}
