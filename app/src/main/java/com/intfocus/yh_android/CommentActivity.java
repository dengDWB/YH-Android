package com.intfocus.yh_android;

import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentActivity extends BaseActivity {

    private String bannerName;
    private int objectID;
    private int objectType;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        TextView mTitle = (TextView) findViewById(R.id.bannerTitle);
        pullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.browser);
        initWebView();
        setPullToRefreshWebView(true);

        mWebView.requestFocus();
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "AndroidJSBridge");
        mWebView.loadUrl(urlStringForLoading);

        Intent intent = getIntent();
        bannerName = intent.getStringExtra("bannerName");
        objectID   = intent.getIntExtra("objectID", -1);
        objectType = intent.getIntExtra("objectType", -1);

        mTitle.setText(bannerName);
        urlString = String.format(URLs.COMMENT_PATH, URLs.kBaseUrl, URLs.currentUIVersion(mContext), objectID, objectType);

        new Thread(mRunnableForDetecting).start();

        List<ImageView> colorViews = new ArrayList<>();
        colorViews.add((ImageView) findViewById(R.id.colorView0));
        colorViews.add((ImageView) findViewById(R.id.colorView1));
        colorViews.add((ImageView) findViewById(R.id.colorView2));
        colorViews.add((ImageView) findViewById(R.id.colorView3));
        colorViews.add((ImageView) findViewById(R.id.colorView4));
        initColorView(colorViews);
    }

    protected void onResume() {
        mMyApp.setCurrentActivity(this);
        super.onResume();
    }

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        CommentActivity.this.onBackPressed();
    };

    private class JavaScriptInterface extends JavaScriptBase  {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void writeComment(final String content) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Map<String, String> params = new HashMap<>();
                        params.put("object_title", bannerName);
                        params.put("user_name", user.getString("user_name"));
                        params.put("content", content);
                        Log.i("PARAMS", params.toString());
                        ApiHelper.writeComment(userID, objectType, objectID, params);

                        new Thread(mRunnableForDetecting).start();
                    } catch (JSONException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "发表/评论");
                logParams.put("obj_title", bannerName);
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void jsException(final String ex) {
            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "JS异常");
                logParams.put("obj_id", objectID);
                logParams.put("obj_type", objectType);
                logParams.put("obj_title", String.format("评论页面/%s/%s", bannerName, ex));
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
