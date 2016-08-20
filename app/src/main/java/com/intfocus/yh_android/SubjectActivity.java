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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

import static java.lang.String.format;

public class SubjectActivity extends BaseActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorOccurredListener {
    private Boolean isInnerLink, isSupportSearch;
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
        mMyApp.setCurrentActivity(this);

        /*
         * JSON Data
         */
        try {
            groupID = user.getInt("group_id");
            userNum = user.getString("user_num");
        } catch (JSONException e) {
            e.printStackTrace();
            groupID = -2;
            userNum = "not-set";
        }

        ImageView bannerComment = (ImageView) findViewById(R.id.bannerComment);
        bannerComment.setVisibility(URLs.kSubjectComment ? View.VISIBLE : View.GONE);
        ImageView bannerShare = (ImageView) findViewById(R.id.bannerShare);
        bannerShare.setVisibility(URLs.kSubjectShare ? View.VISIBLE : View.GONE);
        ImageView bannerSearch = (ImageView) findViewById(R.id.bannerSearch);
        bannerSearch.setVisibility(View.GONE);

        bannerView = (RelativeLayout) findViewById(R.id.actionBar);
        TextView mTitle = (TextView) findViewById(R.id.bannerTitle);
        mPDFView = (PDFView) findViewById(R.id.pdfview);
        mPDFView.setVisibility(View.INVISIBLE);

        pullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.browser);
        initWebView();

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

        List<ImageView> colorViews = new ArrayList<>();
        colorViews.add((ImageView) findViewById(R.id.colorView0));
        colorViews.add((ImageView) findViewById(R.id.colorView1));
        colorViews.add((ImageView) findViewById(R.id.colorView2));
        colorViews.add((ImageView) findViewById(R.id.colorView3));
        colorViews.add((ImageView) findViewById(R.id.colorView4));
        initColorView(colorViews);
    }

    protected void onResume() {
        checkInterfaceOrientation(this.getResources().getConfiguration());

        mMyApp.setCurrentActivity(this);
        super.onResume();
    }

    protected void displayBannerTitleAndSearchIcon() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                ImageView bannerSearch = (ImageView) findViewById(R.id.bannerSearch);
                if (!URLs.kSubjectComment && !URLs.kSubjectShare) {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(dip2px(50), RelativeLayout.LayoutParams.MATCH_PARENT);
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                    bannerSearch.setLayoutParams(params);
                }
                bannerSearch.setVisibility(View.VISIBLE);

                String selectedItem = FileUtil.reportSelectedItem(mContext, String.format("%d", groupID), templateID, reportID);
                if (selectedItem == null || selectedItem.length() == 0) {
                    ArrayList<String> items = FileUtil.reportSearchItems(mContext, String.format("%d", groupID), templateID, reportID);
                    if (items.size() > 0) {
                        selectedItem = items.get(0);
                    } else {
                        selectedItem = String.format("%s(NONE)", bannerName);
                    }
                }
                TextView mTitle = (TextView) findViewById(R.id.bannerTitle);
                mTitle.setText(selectedItem);
            }
        });
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
        Log.d("loadComplete", "load pdf done");
    }

    public void errorOccured(String errorType, String errorMessage) {
        String htmlPath = String.format("%s/loading/%s.html", sharedPath, "500"),
               outputPath = String.format("%s/loading/%s.html", sharedPath, "500.output");

        if(!(new File(htmlPath)).exists()) {
            toast(String.format("链接打开失败: %s", link));
            return;
        }

        mWebView.setVisibility(View.VISIBLE);
        mPDFView.setVisibility(View.INVISIBLE);

        String htmlContent = FileUtil.readFile(htmlPath);
        htmlContent = htmlContent.replace("$exception_type$", errorType);
        htmlContent = htmlContent.replace("$exception_message$", errorMessage);
        htmlContent = htmlContent.replace("$visit_url$", link);

        try {
            FileUtil.writeFile(outputPath, htmlContent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Message message = mHandlerWithAPI.obtainMessage();
        message.what = 200;
        message.obj = outputPath;

        mHandlerWithAPI.sendMessage(message);
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

        mWebView.post(new Runnable() {
            @Override public void run() {
                loadHtml();
            }
        });
    }

    private void loadHtml() {
        WebSettings webSettings = mWebView.getSettings();
        if (isInnerLink) {
            // format: /mobile/v1/group/:group_id/template/:template_id/report/:report_id
            // deprecated
            // format: /mobile/report/:report_id/group/:group_id
            templateID = TextUtils.split(link, "/")[6];
            reportID = TextUtils.split(link, "/")[8];
            String urlPath = format(link.replace("%@", "%d"), groupID);
            urlString = String.format("%s%s", URLs.kBaseUrl, urlPath);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

            /**
             * 内部报表具有筛选功能时
             *   - 如果用户已选择，则 banner 显示该选项名称
             *   - 未设置时，默认显示筛选项列表中第一个
             *
             *  初次加载时，判断筛选功能的条件还未生效
             *  此处仅在第二次及以后才会生效
             */
            isSupportSearch = FileUtil.reportIsSupportSearch(mContext, String.format("%d", groupID), templateID, reportID);
            if(isSupportSearch) {
                displayBannerTitleAndSearchIcon();;
            }

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
                         * 外部链接传参: user_num, timestamp
                         */
                        String appendParams = String.format("?user_num=%s&timestamp=%s", userNum, URLs.timestamp());
                        urlString = urlString.contains("?") ? urlString.replace("?", appendParams) : String.format("%s%s", urlString, appendParams);
                        mWebView.loadUrl(urlString);
                        Log.i("OutLink", urlString);
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
            } else {
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

    /*
     * 内部报表具有筛选功能时，调用筛选项界面
     */
    public void actionLaunchReportSelectorActivity(View v) {
        Intent intent = new Intent(mContext, ReportSelectorAcitity.class);
        intent.putExtra("bannerName", bannerName);
        intent.putExtra("groupID", groupID);
        intent.putExtra("reportID", reportID);
        intent.putExtra("templateID", templateID);
        mContext.startActivity(intent);
    }

    /*
     * 分享截图至微信
     */
    public void actionShare2Weixin(View v) {
        Log.i("actionShare2Weixin", "pending");
    }

    /*
     * 评论
     */
    public void actionLaunchCommentActivity(View v) {
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
    };

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        SubjectActivity.this.onBackPressed();
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
                urlKey = String.format(URLs.API_DATA_PATH, URLs.kBaseUrl, groupID, templateID, reportID);
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

            loadHtml();

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

        @JavascriptInterface
        public void reportSearchItems(final String arrayString) {
            try {
                String searchItemsPath = String.format("%s.search_items", FileUtil.reportJavaScriptDataPath(mContext, String.format("%d", groupID), templateID, reportID));
                if(!new File(searchItemsPath).exists()) {
                    FileUtil.writeFile(searchItemsPath, arrayString);

                    /**
                     *  判断筛选的条件: arrayString 数组不为空
                     *  报表第一次加载时，此处为判断筛选功能的关键点
                     */
                    isSupportSearch = FileUtil.reportIsSupportSearch(mContext, String.format("%d", groupID), templateID, reportID);
                    if(isSupportSearch) {
                        displayBannerTitleAndSearchIcon();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String reportSelectedItem() {
            String item = null;
            String selectedItemPath = String.format("%s.selected_item", FileUtil.reportJavaScriptDataPath(mContext, String.format("%d", groupID), templateID, reportID));
            if(new File(selectedItemPath).exists()) {
                item = FileUtil.readFile(selectedItemPath);
            }
            return item;
        }
    }

    public int dip2px( float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
