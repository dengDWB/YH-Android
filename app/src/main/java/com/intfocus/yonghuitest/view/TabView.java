package com.intfocus.yonghuitest.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.util.K;

/**
 * Created by wiky on 1/10/16.
 */
public class TabView extends LinearLayout {
    private boolean mActive;
    private Drawable mDrawable;
    private Drawable mActiveDrawable;
    private ImageView mImageView;
    private TextView mTextView;

    public TabView(Context context) {
        this(context, null);
    }

    public TabView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabView);
        mActive = a.getBoolean(R.styleable.TabView_active, false);
        mDrawable = a.getDrawable(R.styleable.TabView_src);
        mActiveDrawable = a.getDrawable(R.styleable.TabView_active_src);
        String mText = a.getString(R.styleable.TabView_text);
        int srcHeight = a.getDimensionPixelOffset(R.styleable.TabView_src_height, 20);
        a.recycle();

        setOrientation(VERTICAL);
        mImageView = new ImageView(context);
        mImageView.setPadding(0 - 8, 0 - 8, 0 - 8, 0 - 8);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, srcHeight);
        addView(mImageView, layoutParams);
        mTextView = new TextView(context);
        mTextView.setText(mText);
        mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 6;
        addView(mTextView, layoutParams);

        update();
    }

    private void update() {
        if (mActive) {
            mImageView.setImageDrawable(mActiveDrawable);
            mTextView.setTextColor(Color.parseColor(K.kThemeColor));
        }
        else {
            mImageView.setImageDrawable(mDrawable);
            mTextView.setTextColor(Color.BLACK);
        }
    }

    public void setActive(boolean active) {
        mActive = active;
        update();
    }
}
