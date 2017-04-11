package com.intfocus.yonghuitest.dashboard.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.WebViewFragment;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;
import com.intfocus.yonghuitest.view.CustomWebView;

import org.json.JSONException;

public class AppFragment extends WebViewFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        mWebView = (CustomWebView) view.findViewById(R.id.browser);
        mAnimLoading = (RelativeLayout) view.findViewById(R.id.anim_loading);
        initWebView();
        initSwipeLayout(view);
        String currentUIVersion = URLs.currentUIVersion(mAppContext);
        try {
            urlString = String.format(K.kAppMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new Thread(mRunnableForDetecting).start();
        return view;
    }
}
