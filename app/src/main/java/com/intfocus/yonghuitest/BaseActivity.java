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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
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

import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.LogUtil;
import com.intfocus.yonghuitest.util.URLs;
import com.pgyersdk.javabean.AppBean;
import com.pgyersdk.update.PgyUpdateManager;
import com.pgyersdk.update.UpdateManagerListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lijunjie on 16/1/14.
 */
public class BaseActivity extends Activity {

    public final static String kLoading = "loading";
    public final static String kPath = "path";
    public final static String kMessage = "message";
    public final static String kVersionCode = "versionCode";
    public String sharedPath;
    public String relativeAssetsPath;
    public String urlStringForDetecting;
    public ProgressDialog mProgressDialog;
    public YHApplication mMyApp;
    public PopupWindow popupWindow;
    public DisplayMetrics displayMetrics;
    public boolean isWeiXinShared = false;
    public android.webkit.WebView mWebView;
    public JSONObject user;
    public RelativeLayout animLoading;
    public int userID = 0;
    public String urlString;
    public String assetsPath;
    public String urlStringForLoading;
    public JSONObject logParams = new JSONObject();
    public Context mAppContext;
    public Toast toast;
    int displayDpi; //屏幕密度
    boolean isOffline = false;

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

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        super.onBackPressed();
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config=new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config,res.getDisplayMetrics());
        return res;
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

    public void setWebViewLongListener(final boolean flag) {
        mWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return flag;
            }
        });
    }

    public android.webkit.WebView initSubWebView() {
        animLoading = (RelativeLayout) findViewById(R.id.anim_loading);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

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

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    protected final HandlerForDetecting mHandlerForDetecting = new HandlerForDetecting(BaseActivity.this);
    protected final HandlerWithAPI mHandlerWithAPI = new HandlerWithAPI(BaseActivity.this);

    public final Runnable mRunnableForDetecting = new Runnable() {
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

        public String getLoadLocalHtmlUrl(){
            String htmlName = HttpUtil.UrlToFileName(mUrlString);
            String htmlPath = String.format("%s/%s", mAssetsPath, htmlName);
            return htmlPath;
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
                final Map<String, String> response = ApiHelper.httpGetWithHeader(mUrlString, mAssetsPath, mRelativeAssetsPath);
                Looper.prepare();
                HandlerWithAPI mHandlerWithAPI = new HandlerWithAPI(weakActivity.get());
                mHandlerWithAPI.setVariables(mWebView, mSharedPath, mAssetsPath);
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
                    if (new File(getLoadLocalHtmlUrl()).exists()){
                        mWebView.loadUrl("file:///" + getLoadLocalHtmlUrl());
                        isOffline = true;
                    }else {
                        showWebViewForWithoutNetwork();
                    }
                    break;
                case 401:
                    if (new File(getLoadLocalHtmlUrl()).exists()){
                        mWebView.loadUrl("file:///" + getLoadLocalHtmlUrl());
                        isOffline = true;
                    }else {
                        showDialogForDeviceForbided();
                    }
                    break;
                default:
                    if (new File(getLoadLocalHtmlUrl()).exists()){
                        mWebView.loadUrl("file:///" + getLoadLocalHtmlUrl());
                        isOffline = true;
                    }else {
                        showWebViewForWithoutNetwork();
                    }
                    LogUtil.d("UnkownCode", String.format("%d", message.what));
                    break;
            }
        }

    }

    public class HandlerWithAPI extends Handler {
        private final WeakReference<BaseActivity> weakActivity;
        private WebView mWebView;
        private String mSharedPath;
        private String mAssetsPath;

        public HandlerWithAPI(BaseActivity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        public void setVariables(WebView webView, String sharedPath, String assetsPath ) {
            mWebView = webView;
            mSharedPath = sharedPath;
            mAssetsPath = assetsPath;
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

        private void deleteHeadersFile() {
            String headersFilePath = String.format("%s/%s", mAssetsPath, K.kCachedHeaderConfigFileName);
            if ((new File(headersFilePath)).exists()) {
                new File(headersFilePath).delete();
            }
        }

        @Override
        public void handleMessage(final Message message) {
            BaseActivity activity = weakActivity.get();
            if (activity == null || mWebView == null) {
                return;
            }

            switch (message.what) {
                case 200:
                case 304:
                    final String localHtmlPath = String.format("file:///%s", (String) message.obj);
                    LogUtil.d("localHtmlPath111", localHtmlPath);
                    weakActivity.get().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.loadUrl(localHtmlPath);
                        }
                    });
                    isOffline = false;
                    break;
                case 400:
                case 401:
                case 408:
                    if (new File((String)message.obj).exists()){
                        weakActivity.get().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mWebView.loadUrl("file:///" + message.obj);
                            }
                        });
                        isOffline = true;
                    }else {
                        showWebViewForWithoutNetwork();
                    }
                    deleteHeadersFile();
                    break;
                default:
                    if (new File((String)message.obj).exists()){
                        weakActivity.get().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mWebView.loadUrl("file:///" + message.obj);
                            }
                        });
                        isOffline = true;
                    }else {
                        showWebViewForWithoutNetwork();
                    }
                    String msg = String.format("访问服务器失败（%d)", message.what);
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                    deleteHeadersFile();
                    break;
            }
        }
    }

    public final Runnable  mRunnableForLogger = new Runnable() {
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

    public void modifiedUserConfig(boolean isLogin) {
        try {
            JSONObject configJSON = new JSONObject();
            configJSON.put("is_login", isLogin);
            String userConfigPath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kUserConfigFileName);
            JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

            userJSON = ApiHelper.mergeJson(userJSON, configJSON);
            FileUtil.writeFile(userConfigPath, userJSON.toString());

            String settingsConfigPath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kSettingConfigFileName);
            FileUtil.writeFile(settingsConfigPath, userJSON.toString());
        } catch (IOException | JSONException e) {
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
    public void checkPgyerVersionUpgrade(final Activity activity, final boolean isShowToast) {
        UpdateManagerListener updateManagerListener = new UpdateManagerListener() {
            @Override
            public void onUpdateAvailable(final String result) {
                try {
                    final AppBean appBean = getAppBeanFromString(result);

                    if(result == null || result.isEmpty()) {
                        return;
                    }

                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    int currentVersionCode = packageInfo.versionCode;

                    JSONObject response = new JSONObject(result);
                    String message = response.getString("message");

                    JSONObject responseVersionJSON = response.getJSONObject(URLs.kData);
                    int newVersionCode = responseVersionJSON.getInt(kVersionCode);
                    Log.i("1111", newVersionCode+"");
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
                    } else if (HttpUtil.isWifi(activity) && newVersionCode % 10 == 8) {

                        startDownloadTask(activity, appBean.getDownloadURL());

                        return;
                    }
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
                            .setNegativeButton("下一次",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                            .setCancelable(false)
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

                // 强制消息配置，重新上传服务器
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

    public void showWebViewExceptionForWithoutNetwork() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String urlStringForLoading = loadingPath("400");
                mWebView.loadUrl(urlStringForLoading);
            }
        });
    }

    public void setAlertDialog(Context context, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("温馨提示")
                .setMessage(message)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToAppSetting();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 返回DashboardActivity
                    }
                });
        builder.show();
    }

    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    /*
     * 判断有无网络
     */
    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            // 获取手机所有连接管理对象(包括对wi-fi,net等连接的管理)
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            // 获取NetworkInfo对象
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            //判断NetworkInfo对象是否为空
            if (networkInfo != null)
                return networkInfo.isAvailable();
        }
        return false;
    }
}
