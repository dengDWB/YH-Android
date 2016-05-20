package com.intfocus.yh_android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.URLs;
import com.joanzapata.pdfview.PDFView;
import com.joanzapata.pdfview.listener.OnErrorOccurredListener;
import com.joanzapata.pdfview.listener.OnLoadCompleteListener;
import com.joanzapata.pdfview.listener.OnPageChangeListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class SubjectActivity extends BaseActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorOccurredListener {
    private Boolean isInnerLink;
    private String templateID, reportID;
    private PDFView mPDFView;
    private File pdfFile;
    private String bannerName, link;
    private int groupID, objectID, objectType;
    private String userNum;
    private RelativeLayout bannerView;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject);

        findViewById(R.id.back).setOnClickListener(mOnBackListener);
        findViewById(R.id.back_text).setOnClickListener(mOnBackListener);

        /*
         * JSON Data
         */
        try {
            groupID = user.getInt("group_id");
            userNum = user.getString("user_num");
        } catch (JSONException e) {
            e.printStackTrace();
            groupID = -2;
            userNum = "mobile-not-get";
        }

        bannerView = (RelativeLayout) findViewById(R.id.actionBar);
        TextView mTitle = (TextView) findViewById(R.id.title);
        mPDFView = (PDFView) findViewById(R.id.pdfview);
        ImageView mComment = (ImageView) findViewById(R.id.comment);
        mComment.setOnClickListener(mOnCommentLister);
        mPDFView.setVisibility(View.INVISIBLE);

        pullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.webview);
        initRefreshWebView();

        mWebView.requestFocus();
        pullToRefreshWebView.setVisibility(View.VISIBLE);
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "AndroidJSBridge");
        mWebView.loadUrl(urlStringForLoading);

        // 刷新监听事件
        pullToRefreshWebView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<android.webkit.WebView>() {
            @Override
            public void onRefresh(PullToRefreshBase<android.webkit.WebView> refreshView) {
                // 模拟加载任务
                new pullToRefreshTask().execute();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String label = simpleDateFormat.format(System.currentTimeMillis());
                // 显示最后更新的时间
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
            }
        });

        /*
         * Intent Data || JSON Data
         */
        Intent intent = getIntent();
        link = intent.getStringExtra("link");

        bannerName = intent.getStringExtra("bannerName");
        objectID = intent.getIntExtra("objectID", -1);
        objectType = intent.getIntExtra("objectType", -1);
        isInnerLink = !(link.startsWith("http://") || link.startsWith("https://"));

        mTitle.setText(bannerName);
        checkInterfaceOrientation(this.getResources().getConfiguration());


        List<ImageView> colorViews = new ArrayList<>();
        colorViews.add((ImageView) findViewById(R.id.colorView0));
        colorViews.add((ImageView) findViewById(R.id.colorView1));
        colorViews.add((ImageView) findViewById(R.id.colorView2));
        colorViews.add((ImageView) findViewById(R.id.colorView3));
        colorViews.add((ImageView) findViewById(R.id.colorView4));
        initColorView(colorViews);
    }

    /**
     * PDFView OnPageChangeListener CallBack
     *
     * @param page      the new page displayed, starting from 1
     * @param pageCount the total page count, starting from 1
     */
    public void onPageChanged(int page, int pageCount) {
        Log.i("onPageChanged", format("%s %d / %d", bannerName, page, pageCount));
    }

    public void loadComplete(int nbPages) {
        Log.d("loadComplete", "f");
    }

    public void errorOccured(String errorType, String errorMessage) {
        Log.d("errorOccured", format("type: %s, message: %s", errorType, errorMessage));
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // 横屏时隐藏标题栏、导航栏
        checkInterfaceOrientation(newConfig);
    }

    private void checkInterfaceOrientation(Configuration config) {
        Boolean isLandscape = (config.orientation == Configuration.ORIENTATION_LANDSCAPE);

        bannerView.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        if (isLandscape) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            getWindow().setAttributes(lp);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } else {
            WindowManager.LayoutParams attr = getWindow().getAttributes();
            attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attr);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        dealWithURL();
    }

    private void dealWithURL() {
        WebSettings webSettings = mWebView.getSettings();
        if (isInnerLink) {
            // format: /mobile/v1/group/:group_id/template/:template_id/report/:report_id
            // deprecated
            // format: /mobile/report/:report_id/group/:group_id
            templateID = TextUtils.split(link, "/")[6];
            reportID = TextUtils.split(link, "/")[8];
            String urlPath = format(link.replace("%@", "%d"), groupID);
            urlString = String.format("%s%s", URLs.HOST, urlPath);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ApiHelper.reportData(mContext, String.format("%d", groupID), templateID, reportID);

                    new Thread(mRunnableForDetecting).start();
                }
            }).start();
        } else {
            urlString = link;
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (urlString.toLowerCase().endsWith(".pdf")) {
                        new Thread(mRunnableForPDF).start();
                    } else {
                        /*
                         * 外部链接传参: userNum, timestamp
                         */
                        String appendParams = String.format("?user_num=%s&timestamp=%s", userNum, URLs.TimeStamp);

                        if (!urlString.contains("?")) {
                            urlString = String.format("%s%s", urlString, appendParams);
                        } else {
                            urlString = urlString.replace("?", appendParams);
                        }
                        Log.i("OutLink", urlString);
                        mWebView.loadUrl(urlString);
                    }
                }
            });
        }
    }
    private final Handler mHandlerForPDF = new Handler() {
        public void handleMessage(Message message) {

            //Log.i("PDF", pdfFile.getAbsolutePath());
            if (pdfFile.exists()) {
                mPDFView.fromFile(pdfFile)
                        .defaultPage(1)
                        .showMinimap(true)
                        .enableSwipe(true)
                        .swipeVertical(true)
                        .onLoad(SubjectActivity.this)
                        .onPageChange(SubjectActivity.this)
                        .onErrorOccured(SubjectActivity.this)
                        .load();
                mWebView.setVisibility(View.INVISIBLE);
                mPDFView.setVisibility(View.VISIBLE);
            }
            else {
                toast("加载PDF失败");
            }

        }
    };

    private final Runnable mRunnableForPDF = new Runnable() {
        @Override
        public void run() {
            String outputPath = String.format("%s/%s/%s.pdf", FileUtil.basePath(mContext), URLs.CACHED_DIRNAME, URLs.MD5(urlString));
            pdfFile = new File(outputPath);
            ApiHelper.downloadFile(mContext, urlString, pdfFile);

            Message message = mHandlerForPDF.obtainMessage();
            mHandlerForPDF.sendMessage(message);
        }
    };

    private final View.OnClickListener mOnCommentLister = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mContext, CommentActivity.class);
            intent.putExtra("bannerName", bannerName);
            intent.putExtra("objectID", objectID);
            intent.putExtra("objectType", objectType);

            mContext.startActivity(intent);

                /*
                 * 用户行为记录, 单独异常处理，不可影响用户体验
                 */
            try {
                logParams = new JSONObject();
                logParams.put("action", "点击/主题页面/评论");
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private final View.OnClickListener mOnBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SubjectActivity.this.onBackPressed();
        }
    };

    private class pullToRefreshTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // 如果这个地方不使用线程休息的话，刷新就不会显示在那个 PullToRefreshListView 的 UpdatedLabel 上面

            /*
             *  下拉浏览器刷新时，删除响应头文件，相当于无缓存刷新
             */
            if(isInnerLink) {
                String urlKey;
                if (urlString != null && !urlString.isEmpty()) {
                    urlKey = urlString.contains("?") ? TextUtils.split(urlString, "?")[0] : urlString;
                    ApiHelper.clearResponseHeader(urlKey, assetsPath);
                }
                urlKey = String.format(URLs.API_DATA_PATH, URLs.HOST, groupID, templateID, reportID);
                ApiHelper.clearResponseHeader(urlKey, FileUtil.sharedPath(mContext));

                ApiHelper.reportData(mContext, String.format("%d", groupID), templateID, reportID);
                new Thread(mRunnableForDetecting).start();


                /*
                 * 用户行为记录, 单独异常处理，不可影响用户体验
                 */
                try {
                    logParams = new JSONObject();
                    logParams.put("action", "刷新/浏览器");
                    logParams.put("obj_title", urlString);
                    new Thread(mRunnableForLogger).start();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dealWithURL();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Call onRefreshComplete when the list has been refreshed. 如果没有下面的函数那么刷新将不会停
            pullToRefreshWebView.onRefreshComplete();
        }
    }

    private class JavaScriptInterface extends JavaScriptBase {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void storeTabIndex(final String pageName, final int tabIndex) {
            try {
                String filePath = FileUtil.dirPath(mContext, URLs.CONFIG_DIRNAME, URLs.TABINDEX_CONFIG_FILENAME);

                JSONObject config = new JSONObject();
                if ((new File(filePath).exists())) {
                    String fileContent = FileUtil.readFile(filePath);
                    config = new JSONObject(fileContent);
                }
                config.put(pageName, tabIndex);

                FileUtil.writeFile(filePath, config.toString());
            }
            catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public int restoreTabIndex(final String pageName) {
            int tabIndex = 0;
            try {
                String filePath = FileUtil.dirPath(mContext, URLs.CONFIG_DIRNAME, URLs.TABINDEX_CONFIG_FILENAME);

                JSONObject config = new JSONObject();
                if ((new File(filePath).exists())) {
                    String fileContent = FileUtil.readFile(filePath);
                    config = new JSONObject(fileContent);
                }
                tabIndex = config.getInt(pageName);
            }
            catch (JSONException e) {
                //e.printStackTrace();
            }

            return tabIndex < 0 ? 0 : tabIndex;
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
                logParams.put("obj_title", String.format("主题页面/%s/%s", bannerName, ex));
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
