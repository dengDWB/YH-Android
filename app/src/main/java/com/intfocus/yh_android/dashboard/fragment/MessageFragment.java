package com.intfocus.yh_android.dashboard.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.intfocus.yh_android.R;
import com.intfocus.yh_android.WebViewFragment;
import com.intfocus.yh_android.util.K;
import com.intfocus.yh_android.util.URLs;
import com.intfocus.yh_android.view.CustomWebView;

import org.json.JSONException;

import static com.intfocus.yh_android.util.K.kUserId;
import static com.intfocus.yh_android.util.URLs.kGroupId;

public class MessageFragment extends WebViewFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        mWebView = (CustomWebView) view.findViewById(R.id.browser);
        mAnimLoading = (RelativeLayout) view.findViewById(R.id.anim_loading);
        initWebView();
        initSwipeLayout(view);
        String currentUIVersion = URLs.currentUIVersion(mAppContext);
        try {
            urlString = String.format(K.kMessageMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId), user.getString(kGroupId), user.getString(kUserId));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new Thread(mRunnableForDetecting).start();
        return view;
    }
}
