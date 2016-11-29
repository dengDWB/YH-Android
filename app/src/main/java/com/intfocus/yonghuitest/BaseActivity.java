package com.intfocus.yonghuitest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.LogUtil;
import com.intfocus.yonghuitest.util.URLs;
import com.pgyersdk.javabean.AppBean;
import com.pgyersdk.update.PgyUpdateManager;
import com.pgyersdk.update.UpdateManagerListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lijunjie on 16/1/14.
 */
public class BaseActivity extends Activity {

    public final static String kLoading = "loading";
    public final static String kPath = "path";
    public final static String kMessage = "message";
    public final static String kVersionCode = "versionCode";
    protected String sharedPath;
    protected String relativeAssetsPath;
    protected String urlStringForDetecting;
    protected ProgressDialog mProgressDialog;
    protected YHApplication mMyApp;
    protected PopupWindow popupWindow;
    protected DisplayMetrics displayMetrics;
    public boolean isWeiXinShared = false;
    PullToRefreshWebView pullToRefreshWebView;
    android.webkit.WebView mWebView;
    JSONObject user;
    RelativeLayout animLoading;
    int userID = 0;
    String urlString;
    String assetsPath;
    String urlStringForLoading;
    JSONObject logParams = new JSONObject();
    Context mAppContext;
    Toast toast;
    int displayDpi; //屏幕密度
    MediaPlayer mediaPlayer;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //获取当前设备屏幕密度
        displayMetrics = getResources().getDisplayMetrics();
        displayDpi = displayMetrics.densityDpi;

        mMyApp = (YHApplication)this.getApplication();
        mAppContext = mMyApp.getAppContext();

        sharedPath = FileUtil.sharedPath(mAppContext);
        assetsPath = sharedPath;
        urlStringForDetecting = K.kBaseUrl;
        relativeAssetsPath = "assets";
        urlStringForLoading = loadingPath(kLoading);

        String userConfigPath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kUserConfigFileName);
        if ((new File(userConfigPath)).exists()) {
            try {
                user = FileUtil.readConfigFile(userConfigPath);
                if (user.has(URLs.kIsLogin) && user.getBoolean(URLs.kIsLogin)) {
                    userID = user.getInt("user_id");
                    assetsPath = FileUtil.dirPath(mAppContext, K.kHTMLDirName);
                    urlStringForDetecting = String.format(K.kDeviceStateAPIPath, K.kBaseUrl, user.getInt("user_device_id"));
                    relativeAssetsPath = "../../Shared/assets";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    // RefWatcher refWatcher = YHApplication.getRefWatcher(mContext);
    // refWatcher.watch(this);
    }

    protected void onDestroy() {
        clearReferences();
        fixInputMethodManager(BaseActivity.this);
        mMyApp = null;
        mAppContext = null;
        super.onDestroy();
    }

    public MediaPlayer getMediaPlayer() {
        if (mediaPlayer == null) {
            return new MediaPlayer();
        }
        else {
            return mediaPlayer;
        }
    }

    private void clearReferences(){
        String currActivity = mMyApp.getCurrentActivity();
        if (this.equals(currActivity)) {
            mMyApp.setCurrentActivity(null);
        }
    }

    private void fixInputMethodManager(Context context) {
        if (context == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }

        String [] arr = new String[]{"mCurRootView", "mServedView", "mNextServedView"};
        Field f = null;
        Object obj_get = null;
        for (String param : arr) {
            try {
                f = imm.getClass().getDeclaredField(param);
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }
                obj_get = f.get(imm);
                if (obj_get != null && obj_get instanceof View) {
                    View v_get = (View) obj_get;
                    if (v_get.getContext() == context) { // 被InputMethodManager持有引用的context是想要销毁的
                        f.set(imm, null);                // 置空
                    } else {
                        break;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    protected String loadingPath(String htmlName) {
        return String.format("file:///%s/loading/%s.html", sharedPath, htmlName);
    }

    android.webkit.WebView initPullWebView() {
        animLoading = (RelativeLayout) findViewById(R.id.anim_loading);
        pullToRefreshWebView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        mWebView = pullToRefreshWebView.getRefreshableView();
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.setWebChromeClient(new WebChromeClient());
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
                LogUtil.d("onPageStarted", String.format("%s - %s", URLs.timestamp(), url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                animLoading.setVisibility(View.GONE);
                LogUtil.d("onPageFinished", String.format("%s - %s", URLs.timestamp(), url));
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                LogUtil.d("onReceivedError",
                        String.format("errorCode: %d, description: %s, url: %s", errorCode, description,
                                failingUrl));
            }
        });

        mWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return false;
            }
        });
        setWebViewLongListener(true);

        initIndicator(pullToRefreshWebView);

        return mWebView;
    }

    public void setWebViewLongListener(final boolean flag) {
        mWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return flag;
            }
        });
    }

    android.webkit.WebView initSubWebView() {
        animLoading = (RelativeLayout) findViewById(R.id.anim_loading);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setDrawingCacheEnabled(true);
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
                LogUtil.d("onPageStarted", String.format("%s - %s", URLs.timestamp(), url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                animLoading.setVisibility(View.GONE);
                isWeiXinShared = true;
                LogUtil.d("onPageFinished", String.format("%s - %s", URLs.timestamp(), url));
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                LogUtil.d("onReceivedError",
                        String.format("errorCode: %d, description: %s, url: %s", errorCode, description,
                                failingUrl));
            }
        });

        mWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return false;
            }
        });
        setWebViewLongListener(true);
        return mWebView;
    }

    private void initIndicator(PullToRefreshWebView pullToRefreshWebView) {
        ILoadingLayout startLabels = pullToRefreshWebView
                .getLoadingLayoutProxy(true, false);
        startLabels.setPullLabel("请继续下拉...");// 刚下拉时，显示的提示
        startLabels.setRefreshingLabel("正在刷新...");// 刷新时
        startLabels.setReleaseLabel("放了我，我就刷新...");// 下来达到一定距离时，显示的提示

        ILoadingLayout endLabels = pullToRefreshWebView.getLoadingLayoutProxy(
                false, true);
        endLabels.setPullLabel("请继续下拉");// 刚下拉时，显示的提示
        endLabels.setRefreshingLabel("正在刷新");// 刷新时
        endLabels.setReleaseLabel("放了我，我就刷新");// 下来达到一定距离时，显示的提示
    }

    void setPullToRefreshWebView(boolean isAllow) {
        if (!isAllow) {
            pullToRefreshWebView.setMode(PullToRefreshBase.Mode.DISABLED);
            return;
        }

        // 刷新监听事件
        pullToRefreshWebView.setOnRefreshListener(
                new PullToRefreshBase.OnRefreshListener<android.webkit.WebView>() {
                    @Override
                    public void onRefresh(PullToRefreshBase<android.webkit.WebView> refreshView) {
                        new pullToRefreshTask().execute();

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String label = simpleDateFormat.format(System.currentTimeMillis());
                        refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(label);
                    }
                });
    }


    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private class pullToRefreshTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // 如果这个地方不使用线程休息的话，刷新就不会显示在那个 PullToRefreshListView 的 UpdatedLabel 上面

            /*
             *  下拉浏览器刷新时，删除响应头文件，相当于无缓存刷新
             */
            if (urlString != null && !urlString.isEmpty()) {
                String urlKey = urlString.contains("?") ? TextUtils.split(urlString, "?")[0] : urlString;
                ApiHelper.clearResponseHeader(urlKey, assetsPath);
            }
            new Thread(mRunnableForDetecting).start();

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams.put(URLs.kAction, "刷新/浏览器");
                logParams.put(URLs.kObjTitle, urlString);
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Call onRefreshComplete when the list has been refreshed. 如果没有下面的函数那么刷新将不会停
            pullToRefreshWebView.onRefreshComplete();
        }
    }

    protected final HandlerForDetecting mHandlerForDetecting = new HandlerForDetecting(BaseActivity.this);
    protected final HandlerWithAPI mHandlerWithAPI = new HandlerWithAPI(BaseActivity.this);

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
    public static class HandlerForDetecting extends Handler {
        private final WeakReference<BaseActivity> weakActivity;
        private final Context mContext;
        private WebView mWebView;
        private String mSharedPath;
        private String mUrlString;
        private String mAssetsPath;
        private String mRelativeAssetsPath;

        public HandlerForDetecting(BaseActivity activity) {
            weakActivity = new WeakReference<>(activity);
            mContext = weakActivity.get();
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
                @Override public void run() {
                    String urlStringForLoading = loadingPath("400");
                    mWebView.loadUrl(urlStringForLoading);
                }
            });
        }

        private void showDialogForDeviceForbided() {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(weakActivity.get());
            alertDialog.setTitle("温馨提示");
            alertDialog.setMessage("您被禁止在该设备使用本应用");

            alertDialog.setNegativeButton(
                    "知道了",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                JSONObject configJSON = new JSONObject();
                                configJSON.put(URLs.kIsLogin, false);

                                String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
                                JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

                                userJSON = ApiHelper.mergeJson(userJSON, configJSON);
                                FileUtil.writeFile(userConfigPath, userJSON.toString());

                                String settingsConfigPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kSettingConfigFileName);
                                FileUtil.writeFile(settingsConfigPath, userJSON.toString());
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }

                            Intent intent = new Intent();
                            intent.setClass(mContext, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            mContext.startActivity(intent);

                            dialog.dismiss();
                        }
                    }
            );
            alertDialog.show();
        }

        private final Runnable mRunnableWithAPI = new Runnable() {
            @Override
            public void run() {
                LogUtil.d("httpGetWithHeader", String.format("url: %s, assets: %s, relativeAssets: %s", mUrlString, mAssetsPath, mRelativeAssetsPath));
                Map<String, String> response = ApiHelper.httpGetWithHeader(mUrlString, mAssetsPath, mRelativeAssetsPath);

                Looper.prepare();
                HandlerWithAPI mHandlerWithAPI = new HandlerWithAPI(weakActivity.get());
                mHandlerWithAPI.setVariables(mWebView, mSharedPath);
                Message message = mHandlerWithAPI.obtainMessage();
                message.what = Integer.parseInt(response.get(URLs.kCode));
                message.obj = response.get(kPath);

                LogUtil.d("mRunnableWithAPI",
                        String.format("code: %s, path: %s", response.get(URLs.kCode), response.get(kPath)));
                mHandlerWithAPI.sendMessage(message);
                Looper.loop();
            }
        };

        @Override
        public void handleMessage(Message message) {
            BaseActivity activity = weakActivity.get();
            if (activity == null)  return;

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
                    showDialogForDeviceForbided();
                    break;
                default:
                    LogUtil.d("UnkownCode", String.format("%d", message.what));
                    break;
            }
        }

    }

    public static class HandlerWithAPI extends Handler {
        private final WeakReference<BaseActivity> weakActivity;
        private WebView mWebView;
        private String mSharedPath;

        public HandlerWithAPI(BaseActivity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        public void setVariables(WebView webView, String sharedPath) {
            mWebView = webView;
            mSharedPath = sharedPath;
        }

        protected String loadingPath(String htmlName) {
            return String.format("file:///%s/loading/%s.html", mSharedPath, htmlName);
        }

        private void showWebViewForWithoutNetwork() {
            mWebView.post(new Runnable() {
                @Override public void run() {
                    String urlStringForLoading = loadingPath("400");
                    mWebView.loadUrl(urlStringForLoading);
                }
            });
        }

        @Override
        public void handleMessage(Message message) {
            BaseActivity activity = weakActivity.get();
            if (activity == null || mWebView == null) {
                return;
            }

            switch (message.what) {
                case 200:
                case 304:
                    final String localHtmlPath = String.format("file:///%s", (String) message.obj);
                    LogUtil.d("localHtmlPath", localHtmlPath);
                    weakActivity.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.loadUrl(localHtmlPath);
                        }
                    });
                    break;
                case 400:
                case 408:
                    showWebViewForWithoutNetwork();
                    break;
                default:
                    String msg = String.format("访问服务器失败（%d)", message.what);
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    final Runnable  mRunnableForLogger = new Runnable() {
        @Override
        public void run() {
            try {
                String action = logParams.getString(URLs.kAction);
                if(action == null) {
                    return;
                }
                if (!action.contains("登录") && !action.equals("解屏") && !action.equals("点击/主页面/浏览器")) {
                    return;
                }

                ApiHelper.actionLog(mAppContext, logParams);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    void initColorView(List<ImageView> colorViews) {
        String[] colors = {"#00ffff", "#ffcd0a", "#fd9053", "#dd0929", "#016a43", "#9d203c", "#093db5", "#6a3906", "#192162", "#000000"};
        String userIDStr = String.format("%d", userID);
        int numDiff = colorViews.size() - userIDStr.length();
        numDiff = numDiff < 0 ? 0 : numDiff;

        for (int i = 0; i < colorViews.size(); i++) {
            int colorIndex = 0;
            if (i >= numDiff) {
                colorIndex = Character.getNumericValue(userIDStr.charAt(i - numDiff));
            }
            colorViews.get(i).setBackgroundColor(Color.parseColor(colors[colorIndex]));
        }
    }

    void modifiedUserConfig(JSONObject configJSON) {
        try {
            String userConfigPath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kUserConfigFileName);
            JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

            userJSON = ApiHelper.mergeJson(userJSON, configJSON);
            FileUtil.writeFile(userConfigPath, userJSON.toString());

            String settingsConfigPath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kSettingConfigFileName);
            FileUtil.writeFile(settingsConfigPath, userJSON.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * 检测版本更新
        {
          "code": 0,
          "message": "",
          "data": {
            "lastBuild": "10",
            "downloadURL": "",
            "versionCode": "15",
            "versionName": "0.1.5",
            "appUrl": "http://www.pgyer.com/yh-a",
            "build": "10",
            "releaseNote": "更新到版本: 0.1.5(build10)"
          }
        }
     */

    /*
     * 托管在蒲公英平台，对比版本号检测是否版本更新
     * 对比 build 值，只准正向安装提示
     * 奇数: 测试版本，仅提示
     * 偶数: 正式版本，点击安装更新
     */
    void checkPgyerVersionUpgrade(final Activity activity, final boolean isShowToast) {
        UpdateManagerListener updateManagerListener = new UpdateManagerListener() {
            @Override
            public void onUpdateAvailable(final String result) {
                try {
                    if(result == null || result.isEmpty()) {
                        return;
                    }

                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    int currentVersionCode = packageInfo.versionCode;

                    JSONObject response = new JSONObject(result);
                    String message = response.getString("message");

                    JSONObject responseVersionJSON = response.getJSONObject(URLs.kData);
                    int newVersionCode = responseVersionJSON.getInt(kVersionCode);
                    String newVersionName = responseVersionJSON.getString("versionName");

                    if (currentVersionCode >= newVersionCode) {
                        return;
                    }

                    String pgyerVersionPath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kPgyerVersionConfigFileName);
                    FileUtil.writeFile(pgyerVersionPath, result);

                    if (newVersionCode % 2 == 1) {
                        if (isShowToast) {
                            toast(String.format("有发布测试版本%s(%s)", newVersionName, newVersionCode));
                        }

                        return;
                    }

                    final AppBean appBean = getAppBeanFromString(result);
                    new AlertDialog.Builder(activity)
                            .setTitle("版本更新")
                            .setMessage(message.isEmpty() ? "无升级简介" : message)
                            .setPositiveButton(
                                    "确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startDownloadTask(activity, appBean.getDownloadURL());
                                        }
                                    })
                            .setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                            .show();

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNoUpdateAvailable() {
                if(isShowToast) {
                    toast("已是最新版本");
                }
            }
        };

        PgyUpdateManager.register(activity, updateManagerListener);
    }

    /*
	 * 标题栏设置按钮下拉菜单样式
	 */
    public void initDropMenu(SimpleAdapter adapter,AdapterView.OnItemClickListener itemClickListener) {
        View contentView = LayoutInflater.from(this).inflate(R.layout.menu_dialog, null);

        ListView listView = (ListView) contentView.findViewById(R.id.list_dropmenu);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(itemClickListener);

        popupWindow = new PopupWindow(this);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setContentView(contentView);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);
    }

    /**
     * app升级后，清除缓存头文件
     */
    void checkVersionUpgrade(String assetsPath) {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionConfigPath = String.format("%s/%s", assetsPath, K.kCurrentVersionFileName);

            String localVersion = "new-installer";
            boolean isUpgrade = true;
            if ((new File(versionConfigPath)).exists()) {
                localVersion = FileUtil.readFile(versionConfigPath);
                isUpgrade = !localVersion.equals(packageInfo.versionName);
            }

            if (isUpgrade) {
                LogUtil.d("VersionUpgrade",
                        String.format("%s => %s remove %s/%s", localVersion, packageInfo.versionName,
                                assetsPath, K.kCachedHeaderConfigFileName));

                /*
                 * 用户报表数据js文件存放在公共区域
                 */
                String headerPath = String.format("%s/%s", sharedPath, K.kCachedHeaderConfigFileName);
                File headerFile = new File(headerPath);
                if (headerFile.exists()) {
                    headerFile.delete();
                }

                FileUtil.writeFile(versionConfigPath, packageInfo.versionName);

                // 抢着消息配置，重新上传服务器
                String pushConfigPath = String.format("%s/%s", FileUtil.basePath(BaseActivity.this), K.kPushConfigFileName );
                JSONObject pushJSON = FileUtil.readConfigFile(pushConfigPath);
                pushJSON.put(K.kPushIsValid, false);
                FileUtil.writeFile(pushConfigPath, pushJSON.toString());
            }
        } catch (PackageManager.NameNotFoundException | IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检测服务器端静态文件是否更新
     * to do
     */
    void checkAssetsUpdated(boolean shouldReloadUIThread) {
        checkAssetUpdated(shouldReloadUIThread, kLoading, false);
        checkAssetUpdated(shouldReloadUIThread, URLs.kFonts, true);
        checkAssetUpdated(shouldReloadUIThread, URLs.kImages, true);
        checkAssetUpdated(shouldReloadUIThread, URLs.kStylesheets, true);
        checkAssetUpdated(shouldReloadUIThread, URLs.kJavaScripts, true);
        checkAssetUpdated(shouldReloadUIThread, URLs.kBarCodeScan, false);
        // checkAssetUpdated(shouldReloadUIThread, URLs.kAdvertisement, false);
    }

    private boolean checkAssetUpdated(boolean shouldReloadUIThread, String assetName, boolean isInAssets) {
        try {
            boolean isShouldUpdateAssets = false;
            String assetZipPath = String.format("%s/%s.zip", sharedPath, assetName);
            isShouldUpdateAssets = !(new File(assetZipPath)).exists();

            String userConfigPath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kUserConfigFileName);
            JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);
            String localKeyName = String.format("local_%s_md5", assetName);
            String keyName = String.format("%s_md5", assetName);
            isShouldUpdateAssets = !isShouldUpdateAssets && !userJSON.getString(localKeyName).equals(userJSON.getString(keyName));

            if (!isShouldUpdateAssets) {
                return false;
            }

            LogUtil.d("checkAssetUpdated", String.format("%s: %s != %s", assetZipPath, userJSON.getString(localKeyName), userJSON.getString(keyName)));
            // execute this when the downloader must be fired
            final DownloadAssetsTask downloadTask = new DownloadAssetsTask(mAppContext, shouldReloadUIThread, assetName, isInAssets);
            downloadTask.execute(String.format(K.kDownloadAssetsAPIPath, K.kBaseUrl, assetName), assetZipPath);

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected void toast(String info) {
        try {
            if (null == toast) {
                toast = Toast.makeText(mAppContext, info, Toast.LENGTH_SHORT);
            }
            else {
                toast.setText(info); //若当前已有 Toast 在显示,则直接修改当前 Toast 显示的内容
            }
            toast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class DownloadAssetsTask extends AsyncTask<String, Integer, String> {
        private final Context context;
        private PowerManager.WakeLock mWakeLock;
        private final boolean isReloadUIThread;
        private final String assetFilename;
        private final boolean isInAssets;

        public DownloadAssetsTask(Context context, boolean shouldReloadUIThread, String assetFilename, boolean isInAssets) {
            this.context = context;
            this.isReloadUIThread = shouldReloadUIThread;
            this.assetFilename = assetFilename;
            this.isInAssets = isInAssets;
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream(params[1]);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                LogUtil.d("Exception", e.toString());
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();

            if (result != null) {
                Toast.makeText(context, String.format("静态资源更新失败(%s)", result), Toast.LENGTH_LONG).show();
            } else {
                FileUtil.checkAssets(mAppContext, assetFilename, isInAssets);
                if (isReloadUIThread) {
                    new Thread(mRunnableForDetecting).start();
                }
            }
        }
    }

    class JavaScriptBase {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void refreshBrowser() {
            new Thread(mRunnableForDetecting).start();
        }

        @JavascriptInterface
        public void openURLWithSystemBrowser(final String url) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                        toast(String.format("无效链接: %s",  url));
                        return;
                    }
                    Intent browserIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });
        }
    }

    public void isAllowBrowerCopy() {
        try {
            String betaConfigPath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kBetaConfigFileName);
            JSONObject betaJSON = FileUtil.readConfigFile(betaConfigPath);
            if (betaJSON.has("allow_brower_copy") && betaJSON.getBoolean("allow_brower_copy")) {
                setWebViewLongListener(false);
            } else {
                setWebViewLongListener(true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
