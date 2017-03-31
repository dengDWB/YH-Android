package com.intfocus.yonghuitest;

import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

/**
 * Created by liuruilin on 2017/3/30.
 */

public class WebViewFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {
    public void initSwipeLayout(View view) {
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeLayout.setDistanceToTriggerSync(300);// 设置手指在屏幕下拉多少距离会触发下拉刷新
        mSwipeLayout.setSize(SwipeRefreshLayout.DEFAULT);
    }

    @Override
    public void onRefresh() {
        new Thread(mRunnableForDetecting).start();
    }
}
