package com.intfocus.yh_android.setting;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import com.intfocus.yh_android.BaseActivity;
import com.intfocus.yh_android.R;
import com.intfocus.yh_android.util.K;
import com.intfocus.yh_android.util.PrivateURLs;
import com.intfocus.yh_android.util.URLs;

/**
 * Created by 40284 on 2016/9/10.
            */
    public class ThursdaySayActivity extends BaseActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thursday_say);

        mWebView = (WebView) findViewById(R.id.browser);
        initSubWebView();

        animLoading.setVisibility(View.VISIBLE);
        setWebViewLongListener(false);
        urlString = String.format(K.kThursdaySayMobilePath, PrivateURLs.kBaseUrl, URLs.currentUIVersion(ThursdaySayActivity.this));
        new Thread(mRunnableForDetecting).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMyApp.setCurrentActivity(this);
    }

    public void dismissActivity(View v) {
        ThursdaySayActivity.this.onBackPressed();
    }
}
