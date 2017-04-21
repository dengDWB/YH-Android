package com.intfocus.yonghuitest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.LogUtil;
import com.intfocus.yonghuitest.util.URLs;
import com.intfocus.yonghuitest.util.WidgetUtil;
import com.intfocus.yonghuitest.view.CustomWebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by liuruilin on 2017/3/22.
 */

public class BaseFragment extends Fragment {
    public SwipeRefreshLayout mSwipeLayout;
    public RelativeLayout mAnimLoading;
    public Context mContext;
    public YHApplication mMyApp;
    public Context mAppContext;
    public CustomWebView mWebView;
    public String urlString, sharedPath, assetsPath, urlStringForDetecting, relativeAssetsPath, urlStringForLoading;
    public JSONObject user;
    public int userID;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mMyApp = (YHApplication) getActivity().getApplication();
        mAppContext = mMyApp.getAppContext();
        sharedPath = FileUtil.sharedPath(mContext);
        assetsPath = sharedPath;
        urlStringForDetecting = K.kBaseUrl;
        relativeAssetsPath = "assets";
        urlStringForLoading = loadingPath("loading");

        String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
        if ((new File(userConfigPath)).exists()) {
            try {
                user = FileUtil.readConfigFile(userConfigPath);
                if (user.has(URLs.kIsLogin) && user.getBoolean(URLs.kIsLogin)) {
                    userID = user.getInt("user_id");
                    assetsPath = FileUtil.dirPath(mContext, K.kHTMLDirName);
                    urlStringForDetecting = String.format(K.kDeviceStateAPIPath, K.kBaseUrl, user.getInt("user_device_id"));
                    relativeAssetsPath = "../../Shared/assets";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    protected String loadingPath(String htmlName) {
        return String.format("file:///%s/loading/%s.html", sharedPath, htmlName);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    public void initWebView() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setDrawingCacheEnabled(true);
        mWebView.addJavascriptInterface(new JavaScriptBase(), URLs.kJSInterfaceName);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mAnimLoading.setVisibility(View.VISIBLE);
                LogUtil.d("onPageStarted", String.format("%s - %s", URLs.timestamp(), url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mAnimLoading.setVisibility(View.GONE);
                LogUtil.d("onPageFinished", String.format("%s - %s", URLs.timestamp(), url));
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                LogUtil.d("onReceivedError",
                        String.format("errorCode: %d, description: %s, url: %s", errorCode, description,
                                failingUrl));
            }
        });

        mWebView.setOnScrollChangedCallback(new CustomWebView.OnScrollChangedCallback() {
            public void onScroll(int horizontal, int vertical) {
                System.out.println("==" + horizontal + "---" + vertical);
                //this is to check webview scroll
                if (vertical < 50) {
                    mSwipeLayout.setEnabled(true);
                } else {
                    mSwipeLayout.setEnabled(false);
                }
            }
        });

        mWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return false;
            }
        });
    }

    protected final BaseFragment.HandlerForDetecting mHandlerForDetecting = new BaseFragment.HandlerForDetecting();
    protected final BaseFragment.HandlerWithAPI mHandlerWithAPI = new BaseFragment.HandlerWithAPI();

    protected final Runnable mRunnableForDetecting = new Runnable() {
        @Override
        public void run() {
            Map<String, String> response = HttpUtil.httpGet(urlStringForDetecting,
                    new HashMap<String, String>());

            int statusCode = Integer.parseInt(response.get(URLs.kCode));
            if (statusCode == 200 && !urlStringForDetecting.equals(K.kBaseUrl)) {
                try {
                    JSONObject json = new JSONObject(response.get("body"));
                    statusCode = json.getBoolean("device_state") ? 200 : 401;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            mHandlerForDetecting.setVariables(mWebView, urlString, sharedPath, assetsPath, relativeAssetsPath);
            Message message = mHandlerForDetecting.obtainMessage();
            message.what = statusCode;
            mHandlerForDetecting.sendMessage(message);
        }
    };

    /**
     * Instances of static inner classes do not hold an implicit reference to their outer class.
     */
    public class HandlerForDetecting extends Handler {
        private WebView mWebView;
        private String mSharedPath;
        private String mUrlString;
        private String mAssetsPath;
        private String mRelativeAssetsPath;

        public HandlerForDetecting() {

        }

        public void setVariables(WebView webView, String urlString, String sharedPath, String assetsPath, String relativeAssetsPath) {
            mWebView = webView;
            mUrlString = urlString;
            mSharedPath = sharedPath;
            mUrlString = urlString;
            mAssetsPath = assetsPath;
            mRelativeAssetsPath = relativeAssetsPath;
        }

        protected String loadingPath(String htmlName) {
            return String.format("file:///%s/loading/%s.html", mSharedPath, htmlName);
        }

        private void showWebViewForWithoutNetwork() {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    String urlStringForLoading = loadingPath("400");
                    mWebView.loadUrl(urlStringForLoading);
                }
            });
        }

        private final Runnable mRunnableWithAPI = new Runnable() {
            @Override
            public void run() {
                LogUtil.d("httpGetWithHeader", String.format("url: %s, assets: %s, relativeAssets: %s", mUrlString, mAssetsPath, mRelativeAssetsPath));
                final Map<String, String> response = ApiHelper.httpGetWithHeader(mUrlString, mAssetsPath, mRelativeAssetsPath);
                Looper.prepare();
                BaseFragment.HandlerWithAPI mHandlerWithAPI = new BaseFragment.HandlerWithAPI();
                mHandlerWithAPI.setVariables(mWebView, mSharedPath, mAssetsPath);
                Message message = mHandlerWithAPI.obtainMessage();
                message.what = Integer.parseInt(response.get(URLs.kCode));
                message.obj = response.get("path");

                LogUtil.d("mRunnableWithAPI",
                        String.format("code: %s, path: %s", response.get(URLs.kCode), response.get("path")));
                mHandlerWithAPI.sendMessage(message);
                Looper.loop();
            }
        };

        @Override
        public void handleMessage(Message message) {

            switch (message.what) {
                case 200:
                case 201:
                case 304:
                    new Thread(mRunnableWithAPI).start();
                    break;
                case 400:
                case 408:
                    showWebViewForWithoutNetwork();
                    break;
                case 401:
                    break;
                default:
                    showWebViewForWithoutNetwork();
                    LogUtil.d("UnkownCode", String.format("%d", message.what));
                    break;
            }
        }

    }

    public class HandlerWithAPI extends Handler {
        private WebView mWebView;
        private String mSharedPath;
        private String mAssetsPath;

        public void setVariables(WebView webView, String sharedPath, String assetsPath) {
            mWebView = webView;
            mSharedPath = sharedPath;
            mAssetsPath = assetsPath;
        }

        protected String loadingPath(String htmlName) {
            return String.format("file:///%s/loading/%s.html", mSharedPath, htmlName);
        }

        private void showWebViewForWithoutNetwork() {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    String urlStringForLoading = loadingPath("400");
                    mWebView.loadUrl(urlStringForLoading);
                }
            });
        }

        private void deleteHeadersFile() {
            String headersFilePath = String.format("%s/%s", mAssetsPath, K.kCachedHeaderConfigFileName);
            if ((new File(headersFilePath)).exists()) {
                new File(headersFilePath).delete();
            }
        }

        @Override
        public void handleMessage(Message message) {

            switch (message.what) {
                case 200:
                case 304:
                    final String localHtmlPath = String.format("file:///%s", (String) message.obj);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.loadUrl(localHtmlPath);
                        }
                    });
                    break;
                case 400:
                case 401:
                case 408:
                    showWebViewForWithoutNetwork();
                    deleteHeadersFile();
                    break;
                default:
                    String msg = String.format("访问服务器失败（%d)", message.what);
                    showWebViewForWithoutNetwork();
                    deleteHeadersFile();
                    break;
            }

            if (mSwipeLayout.isRefreshing()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeLayout.setRefreshing(false);
                    }
                });
            }
        }
    }


    public class JavaScriptBase {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void refreshBrowser() {
            new Thread(mRunnableForDetecting).start();
        }

        @JavascriptInterface
        public void openURLWithSystemBrowser(final String url) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                        return;
                    }
                    Intent browserIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });
        }

        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void pageLink(final String bannerName, final String link, final int objectID) {
            if (null == link || link.equals("")) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String message = String.format("%s\n%s\n%d", bannerName, link, objectID);
                    LogUtil.d("JSClick", message);

                    if (link.indexOf("template") > 0) {
                        String templateID = TextUtils.split(link, "/")[7];

                        if (templateID.equals("3")) {
                            Intent homeTricsIntent = new Intent(mContext, HomeTricsActivity.class);
                            homeTricsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            homeTricsIntent.putExtra("urlString", link);
                            mContext.startActivity(homeTricsIntent);
                        }
                        else if (templateID.equals("5")) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("温馨提示")
                                    .setMessage("当前版本暂不支持该模板, 请升级应用后查看")
                                    .setPositiveButton("前去升级", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent browserIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(K.kPgyerUrl));
                                            startActivity(browserIntent);
                                        }
                                    })
                                    .setNegativeButton("稍后升级", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // 返回 LoginActivity
                                        }
                                    });
                            builder.show();
                        }
                        else {
                            Intent intent = new Intent(getActivity(), SubjectActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            intent.putExtra(URLs.kBannerName, bannerName);
                            intent.putExtra(URLs.kLink, link);
                            intent.putExtra(URLs.kObjectId, objectID);
                            intent.putExtra(URLs.kObjectType, 1);
                            startActivity(intent);
                        }
                    }
                    else {
                        Intent intent = new Intent(getActivity(), SubjectActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.putExtra(URLs.kBannerName, bannerName);
                        intent.putExtra(URLs.kLink, link);
                        intent.putExtra(URLs.kObjectId, objectID);
                        intent.putExtra(URLs.kObjectType, 1);
                        startActivity(intent);
                    }
                }
            });
        }

        @JavascriptInterface
        public void storeTabIndex(final String pageName, final int tabIndex) {
            try {
                String filePath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kBehaviorConfigFileName);

                if ((new File(filePath).exists())) {
                    String fileContent = FileUtil.readFile(filePath);
                    JSONObject jsonObject = new JSONObject(fileContent);
                    JSONObject config = new JSONObject(jsonObject.getString("dashboard"));
                    config.put(pageName, tabIndex);
                    jsonObject.put("dashboard", config.toString());

                    FileUtil.writeFile(filePath, jsonObject.toString());
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public int restoreTabIndex(final String pageName) {
            int tabIndex = 0;
            try {
                String filePath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kBehaviorConfigFileName);

                if ((new File(filePath).exists())) {
                    String fileContent = FileUtil.readFile(filePath);
                    JSONObject jsonObject = new JSONObject(fileContent);
                    JSONObject config = new JSONObject(jsonObject.getString("dashboard"));
                    if (config.has(pageName)) {
                        tabIndex = config.getInt(pageName);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return tabIndex < 0 ? 0 : tabIndex;
        }
    }
}
