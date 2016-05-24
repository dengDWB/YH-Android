package com.intfocus.yh_android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.ImageView;

import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    private int objectType;
    private TabView mCurrentTab;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMyApp.setCurrentActivity(this);

        findViewById(R.id.setting).setOnClickListener(mSettingListener);
        pullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.webview);
        initRefreshWebView();
        setPullToRefreshWebView(true);

        mWebView.requestFocus();
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "AndroidJSBridge");
        mWebView.loadUrl(urlStringForLoading);

        try {
            objectType = 1;
            urlString = String.format(URLs.KPI_PATH, URLs.HOST, currentUIVersion(), user.getString("group_id"), user.getString("role_id"));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        initTab();

        List<ImageView> colorViews = new ArrayList<>();
        colorViews.add((ImageView) findViewById(R.id.colorView0));
        colorViews.add((ImageView) findViewById(R.id.colorView1));
        colorViews.add((ImageView) findViewById(R.id.colorView2));
        colorViews.add((ImageView) findViewById(R.id.colorView3));
        colorViews.add((ImageView) findViewById(R.id.colorView4));
        initColorView(colorViews);

        Intent intent = getIntent();
        if (intent.hasExtra("from_activity") && intent.getStringExtra("from_activity").contains("ConfirmPassCodeActivity")) {
            Log.i("from_activity", intent.getStringExtra("from_activity"));

            checkVersionUpgrade(assetsPath);
            checkUpgrade(false);


            new Thread(new Runnable() {
                @Override
                public synchronized void run() {
                    try {
                        String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.USER_CONFIG_FILENAME);
                        JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

                        String info = ApiHelper.authentication(mContext, userJSON.getString("user_num"), userJSON.getString("password"));
                        if (info.equals("用户名或密码不正确")) {
                            userJSON.put("is_login", false);
                            FileUtil.writeFile(userConfigPath, userJSON.toString());
                        }
                    }
                    catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        else {
            mWebView.clearCache(true);
        }

        /*
         * 检测服务器静态资源是否更新，并下载
         */
        checkAssetsUpdated(true);
        new Thread(mRunnableForDetecting).start();
    }

    protected void onResume() {
        mMyApp.setCurrentActivity(this);
        super.onResume();
    }

    protected void onDestroy() {
        mContext = null;
        mWebView = null;
        user = null;
        super.onDestroy();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @JavascriptInterface
    private void initTab() {
        TabView mTabKPI = (TabView) findViewById(R.id.tab_kpi);
        TabView mTabAnalysis = (TabView) findViewById(R.id.tab_analysis);
        TabView mTabAPP = (TabView) findViewById(R.id.tab_app);
        TabView mTabMessage = (TabView) findViewById(R.id.tab_message);

        mTabKPI.setOnClickListener(mTabChangeListener);
        mTabAnalysis.setOnClickListener(mTabChangeListener);
        mTabAPP.setOnClickListener(mTabChangeListener);
        mTabMessage.setOnClickListener(mTabChangeListener);

        mCurrentTab = mTabKPI;
        mCurrentTab.setActive(true);
    }

    private final View.OnClickListener mSettingListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mContext, SettingActivity.class);
            mContext.startActivity(intent);

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "点击/主页面/设置");
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    private final View.OnClickListener mTabChangeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mCurrentTab) return;

            mCurrentTab.setActive(false);
            mCurrentTab = (TabView) v;
            mCurrentTab.setActive(true);

            mWebView.loadUrl(loadingPath("loading"));
            String currentUIVersion = currentUIVersion();
            try {
                switch (v.getId()) {
                    case R.id.tab_kpi:
                        objectType = 1;
                        urlString = String.format(URLs.KPI_PATH, URLs.HOST, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
                        break;
                    case R.id.tab_analysis:
                        objectType = 2;
                        urlString = String.format(URLs.ANALYSE_PATH, URLs.HOST, currentUIVersion, user.getString("role_id"));
                        break;
                    case R.id.tab_app:
                        objectType = 3;
                        urlString = String.format(URLs.APPLICATION_PATH, URLs.HOST, currentUIVersion, user.getString("role_id"));
                        break;
                    case R.id.tab_message:
                        objectType = 5;
                        urlString = String.format(URLs.MESSAGE_PATH, URLs.HOST, currentUIVersion, user.getString("role_id"), user.getString("group_id"), user.getString("user_id"));
                        break;
                    default:
                        objectType = 1;
                        urlString = String.format(URLs.KPI_PATH, URLs.HOST, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
                        break;
                }

                new Thread(mRunnableForDetecting).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "点击/主页面/标签栏");
                logParams.put("obj_type", objectType);
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private class JavaScriptInterface extends JavaScriptBase {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void pageLink(final String bannerName, final String link, final int objectID) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message = String.format("%s\n%s\n%d", bannerName, link, objectID);
                    longLog("JSClick", message);

                    Intent intent = new Intent(MainActivity.this, SubjectActivity.class);
                    intent.putExtra("bannerName", bannerName);
                    intent.putExtra("link", link);
                    intent.putExtra("objectID", objectID);
                    intent.putExtra("objectType", objectType);
                    mContext.startActivity(intent);
                }
            });

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "点击/主页面/浏览器");
                logParams.put("obj_id", objectID);
                logParams.put("obj_type", objectType);
                logParams.put("obj_title", bannerName);
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

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
                // e.printStackTrace();
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
                logParams.put("obj_type", objectType);
                logParams.put("obj_title", String.format("主页面/%s", ex));
                new Thread(mRunnableForLogger).start();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
