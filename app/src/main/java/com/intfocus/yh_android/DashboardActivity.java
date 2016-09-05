package com.intfocus.yh_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.LogUtil;
import com.intfocus.yh_android.util.URLs;
import com.readystatesoftware.viewbadger.BadgeView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class DashboardActivity extends BaseActivity implements View.OnClickListener {
    public static final String ACTION_UPDATENOTIFITION = "action.updateNotifition";
    private static final int ZBAR_CAMERA_PERMISSION = 1;
    private TabView mCurrentTab;
    private PopupWindow popupWindow;
    private BadgeView bvUser, bvVoice;
    private LinearLayout linearUserInfo,linearScan,linearVoice,linearSearch;
    private ArrayList<String> urlStrings;
    private JSONObject notificationJSON;
    private BadgeView bvKpi, bvAnalyse, bvApp, bvMessage, bvBannerSetting;
    private int objectType, kpiNotifition, analyseNotifition, appNotifition, messageNotifition;
    private NotificationBroadcastReceiver notificationBroadcastReceiver;
    private TabView mTabKPI, mTabAnalyse, mTabAPP, mTabMessage;
    private WebView browserAd;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initUrlStrings();

        initDropMenu();
        initTab();
        initUserIDColorView();
        loadWebView();
        displayAdOrNot(true);

        /*
         * 通过解屏进入界面后，进行用户验证
         */
        checkWhetherFromScreenLockActivity();

        /*
         * 检测服务器静态资源是否更新，并下载
         */
        checkAssetsUpdated(true);

        /*
         * 动态注册广播用于接收通知
         */
        initLocalNotifications();
        initNotificationService();

        new Thread(mRunnableForDetecting).start();


        checkUserModifiedInitPassword();
    }

    protected void onResume() {
        mMyApp.setCurrentActivity(this);
        /*
         * 启动 Activity 时也需要判断小红点是否显示
         */
        receiveNotification();
        super.onResume();
    }

    @Override
    protected void onStop() {
        popupWindow.dismiss();

        super.onStop();
    }

    protected void onDestroy() {
        mContext = null;
        mWebView = null;
        user = null;
        popupWindow.dismiss();
        unregisterReceiver(notificationBroadcastReceiver);

        super.onDestroy();
    }

    /*
     * 仪表盘界面可以显示广告
     */
    private void displayAdOrNot(boolean isShouldLoadHtml) {
        String adIndexBasePath = FileUtil.sharedPath(this) + "/advertisement/index_android";
        String adIndexPath = adIndexBasePath + ".html";
        String adIndexWithTimestampPath = adIndexBasePath + ".timestamp.html";
        if(new File(adIndexPath).exists()) {
            String htmlContent = FileUtil.readFile(adIndexPath);
            htmlContent = htmlContent.replaceAll("TIMESTAMP", String.format("%d", new Date().getTime()));

            try {
                FileUtil.writeFile(adIndexWithTimestampPath, htmlContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            isShouldLoadHtml = false;
        }

        if(isShouldLoadHtml) {
            browserAd.loadUrl(String.format("file:///%s", adIndexWithTimestampPath));
        }

        boolean isShouldDisplayAd = mCurrentTab == mTabKPI && new File(adIndexPath).exists();
        browserAd.setVisibility(isShouldDisplayAd ? View.VISIBLE : View.GONE);
    }

    /*
     * 动态注册广播用于接收通知
     */
    private void initNotificationService() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATENOTIFITION);
        notificationBroadcastReceiver = new NotificationBroadcastReceiver();
        registerReceiver(notificationBroadcastReceiver, filter);
        /*
         * 打开通知服务,用于发送通知
         */
        Intent startService = new Intent(this, LocalNotificationService.class);
        startService(startService);
    }

    /*
     * 定义广播接收器（内部类），接收到后调用是否显示通知的判断逻辑
     */
    private class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            receiveNotification();
        }
    }

    /*
     * 通知显示判断逻辑，在 Activity 显示和接收到广播时都会调用
     */
    private void receiveNotification() {
        try {
            String noticePath = FileUtil.dirPath(mContext, "Cached", URLs.LOCAL_NOTIFICATION_FILENAME);
            notificationJSON = FileUtil.readConfigFile(noticePath);
            kpiNotifition = notificationJSON.getInt("tab_kpi");
            analyseNotifition = notificationJSON.getInt("tab_analyse");
            appNotifition = notificationJSON.getInt("tab_app");
            messageNotifition = notificationJSON.getInt("tab_message");

            if (kpiNotifition > 0 && objectType != 1) {
                setBadgeView("tab", bvKpi);
            }
            if (analyseNotifition > 0 && objectType != 2) {
                setBadgeView("tab", bvAnalyse);
            }
            if (appNotifition > 0 && objectType != 3) {
                setBadgeView("tab" ,bvApp);
            }
            if (messageNotifition > 0 && objectType != 5) {
                setBadgeView("tab", bvMessage);
            }
            if (notificationJSON.getInt("setting_pgyer") == 1 || notificationJSON.getInt("setting_password") == 1) {
                setBadgeView("setting", bvBannerSetting);
                setBadgeView("user",bvUser);
            }
            else {
                bvBannerSetting.setVisibility(View.GONE);
                bvUser.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * 配置 mWebView
     */
    public void loadWebView() {
        pullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.browser);
        initWebView();
        setPullToRefreshWebView(true);

        mWebView.requestFocus();
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "AndroidJSBridge");
        mWebView.loadUrl(urlStringForLoading);

        browserAd = (WebView) findViewById(R.id.browserAd);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dip2px(this, 100));
        browserAd.setLayoutParams(layoutParams);
        browserAd.getSettings().setJavaScriptEnabled(true);
        browserAd.getSettings().setDefaultTextEncodingName("utf-8");
        browserAd.requestFocus();
        browserAd.addJavascriptInterface(new JavaScriptInterface(), "AndroidJSBridge");
        browserAd.setWebViewClient(new WebViewClient());
        browserAd.setWebChromeClient(new WebChromeClient() {
        });
        browserAd.getSettings().setUseWideViewPort(true);
        browserAd.getSettings().setLoadWithOverviewMode(true);
    }

    /*
     * 通过解屏进入界面后，进行用户验证
     */
    public void checkWhetherFromScreenLockActivity() {
        Intent intent = getIntent();
        if (intent.hasExtra("from_activity")) {
            checkVersionUpgrade(assetsPath);
            checkPgyerVersionUpgrade(false);

            new Thread(new Runnable() {
                @Override
                public synchronized void run() {
                    try {
                        String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), URLs.USER_CONFIG_FILENAME);
                        JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

                        String info = ApiHelper.authentication(mContext, userJSON.getString("user_num"), userJSON.getString("password"));
                        if (!info.isEmpty() && (info.contains("用户") || info.contains("密码"))) {
                            userJSON.put("is_login", false);
                            FileUtil.writeFile(userConfigPath, userJSON.toString());
                        }
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            mWebView.clearCache(true);
        }
    }

    /*
     * 用户编号
     */
    public void checkUserModifiedInitPassword() {
        try {
            if(!user.getString("password").equals(URLs.MD5(URLs.kInitPassword))) {
                return;
            }

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(DashboardActivity.this);
            alertDialog.setTitle("温馨提示");
            alertDialog.setMessage("安全起见，请在【设置】-【个人信息】-【修改登录密码】页面修改初始密码");

            alertDialog.setNegativeButton("知道了", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }
            );
            alertDialog.show();
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    /*
     * 用户编号
     */
    public void initUserIDColorView() {
        List<ImageView> colorViews = new ArrayList<>();
        colorViews.add((ImageView) findViewById(R.id.colorView0));
        colorViews.add((ImageView) findViewById(R.id.colorView1));
        colorViews.add((ImageView) findViewById(R.id.colorView2));
        colorViews.add((ImageView) findViewById(R.id.colorView3));
        colorViews.add((ImageView) findViewById(R.id.colorView4));
        initColorView(colorViews);
    }

    /*
     * 标题栏设置按钮下拉菜单样式
     */
    public void initDropMenu() {
        View contentView = LayoutInflater.from(this).inflate(R.layout.activity_dashboard_dialog, null);

        linearScan = (LinearLayout) contentView.findViewById(R.id.linearScan);
        linearSearch = (LinearLayout) contentView.findViewById(R.id.linearSearch);
        linearVoice = (LinearLayout) contentView.findViewById(R.id.linearVoice);
        linearUserInfo = (LinearLayout) contentView.findViewById(R.id.linearUserInfo);

        popupWindow = new PopupWindow(this);
        popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setContentView(contentView);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);

        /*
         * 根据配置动态设置显示下拉菜单选项
         */
        View viewSeparator;
        if(!URLs.kDropMenuScan) {
            viewSeparator = contentView.findViewById(R.id.linearScanSeparator);
            viewSeparator.setVisibility(URLs.kDropMenuScan ? View.VISIBLE : View.GONE);

            linearScan.setVisibility(URLs.kDropMenuScan ? View.VISIBLE : View.GONE);
        }
        else {
            linearScan.setOnClickListener(this);
        }

        if(!URLs.kDropMenuVoice) {
            viewSeparator = contentView.findViewById(R.id.linearVoiceSeparator);
            viewSeparator.setVisibility(URLs.kDropMenuVoice ? View.VISIBLE : View.GONE);

            linearVoice.setVisibility(URLs.kDropMenuVoice ? View.VISIBLE : View.GONE);
        }
        else {
            linearVoice.setOnClickListener(this);
        }

        if(!URLs.kDropMenuSearch) {
            viewSeparator = contentView.findViewById(R.id.linearSearchSeparator);
            viewSeparator.setVisibility(URLs.kDropMenuSearch ? View.VISIBLE : View.GONE);

            linearSearch.setVisibility(URLs.kDropMenuSearch ? View.VISIBLE : View.GONE);
        }
        else {
            linearSearch.setOnClickListener(this);
        }

        if(!URLs.kDropMenuUserInfo) {
            linearUserInfo.setVisibility(URLs.kDropMenuUserInfo ? View.VISIBLE : View.GONE);
            linearUserInfo.setOnClickListener(this);
        }
        else {
            linearUserInfo.setOnClickListener(this);
        }
    }

    /*
     * 标题栏设置按钮下拉菜单点击响应事件
     */
    @Override
    public void onClick(View v) {
        popupWindow.dismiss();

        switch (v.getId()) {
            case R.id.linearUserInfo:
                Intent settingIntent = new Intent(mContext, SettingActivity.class);
                mContext.startActivity(settingIntent);
                break;

            case R.id.linearScan:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, ZBAR_CAMERA_PERMISSION);
                } else {
                    Intent barCodeScannerIntent = new Intent(mContext, BarCodeScannerActivity.class);
                    mContext.startActivity(barCodeScannerIntent);
                }
                break;

            case R.id.linearSearch:
                toast("功能开发中，敬请期待");
                break;

            case R.id.linearVoice:
                toast("功能开发中，敬请期待");
                break;
            default:
                break;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @JavascriptInterface
    private void initTab() {
        mTabKPI = (TabView) findViewById(R.id.tabKPI);
        mTabAnalyse = (TabView) findViewById(R.id.tabAnalyse);
        mTabAPP = (TabView) findViewById(R.id.tabApp);
        mTabMessage = (TabView) findViewById(R.id.tabMessage);
        ImageView mBannerSetting = (ImageView) findViewById(R.id.bannerSetting);

        if(URLs.kTabBar) {
            mTabKPI.setVisibility(URLs.kTabBarKPI ? View.VISIBLE : View.GONE);
            mTabAnalyse.setVisibility( URLs.kTabBarAnalyse ? View.VISIBLE : View.GONE);
            mTabAPP.setVisibility(URLs.kTabBarApp ? View.VISIBLE : View.GONE);
            mTabMessage.setVisibility(URLs.kTabBarMessage ? View.VISIBLE : View.GONE);
        }
        else {
           findViewById(R.id.toolBar).setVisibility(View.GONE);
        }

        mTabKPI.setOnClickListener(mTabChangeListener);
        mTabAnalyse.setOnClickListener(mTabChangeListener);
        mTabAPP.setOnClickListener(mTabChangeListener);
        mTabMessage.setOnClickListener(mTabChangeListener);

        mCurrentTab = mTabKPI;
        mCurrentTab.setActive(true);

        bvKpi = new BadgeView(this, mTabKPI);
        bvAnalyse = new BadgeView(this, mTabAnalyse);
        bvApp = new BadgeView(this, mTabAPP);
        bvMessage = new BadgeView(this, mTabMessage);
        bvBannerSetting = new BadgeView(this, mBannerSetting);
        bvUser = new BadgeView(this,linearUserInfo);
    }

    /*
     * 标签栏点击响应事件
     *
     * OBJ_TYPE_KPI = 1
     * OBJ_TYPE_ANALYSE = 2
     * OBJ_TYPE_APP = 3
     * OBJ_TYPE_REPORT = 4
     * OBJ_TYPE_MESSAGE = 5
     */
    @SuppressLint("SetJavaScriptEnabled")
    private final View.OnClickListener mTabChangeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mCurrentTab) {
                return;
            }

            mCurrentTab.setActive(false);
            mCurrentTab = (TabView) v;
            mCurrentTab.setActive(true);

            mWebView.loadUrl(loadingPath("loading"));
            String currentUIVersion = URLs.currentUIVersion(mContext);

            displayAdOrNot(false);

            try {
                switch (v.getId()) {
                    case R.id.tabKPI:
                        objectType = 1;
                        urlString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));

                        bvKpi.setVisibility(View.GONE);
                        notificationJSON.put("tab_kpi", 0);
                        break;
                    case R.id.tabAnalyse:
                        objectType = 2;
                        urlString = String.format(URLs.ANALYSE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));

                        bvAnalyse.setVisibility(View.GONE);
                        notificationJSON.put("tab_analyse", 0);
                        break;
                    case R.id.tabApp:
                        objectType = 3;
                        urlString = String.format(URLs.APPLICATION_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));

                        bvApp.setVisibility(View.GONE);
                        notificationJSON.put("tab_app", 0);
                        break;
                    case R.id.tabMessage:
                        objectType = 5;
                        urlString = String.format(URLs.MESSAGE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"), user.getString("group_id"), user.getString("user_id"));

                        bvMessage.setVisibility(View.GONE);
                        notificationJSON.put("tab_message", 0);
                        break;
                    default:
                        objectType = 1;
                        urlString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));

                        bvKpi.setVisibility(View.GONE);
                        notificationJSON.put("tab_kpi", 0);
                        break;
                }

                String notificationPath = FileUtil.dirPath(mContext, "Cached", URLs.LOCAL_NOTIFICATION_FILENAME);
                FileUtil.writeFile(notificationPath, notificationJSON.toString());

                new Thread(mRunnableForDetecting).start();
            } catch (Exception e) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * 标题栏点击设置按钮显示下拉菜单
     */
    public void launchSettingActivity(View v) {
        ImageView mBannerSetting = (ImageView) findViewById(R.id.bannerSetting);
        popupWindow.showAsDropDown(mBannerSetting, dip2px(this, -87), dip2px(this, 10));

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

    /*
     * javascript & native 交互
     */
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
                    LogUtil.d("JSClick", message);

                    Intent intent = new Intent(DashboardActivity.this, SubjectActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void adLink(final String openType, final String openLink, final String ObjeckID, final String objectType
                                ,final String objectTitle) {
            runOnUiThread(new Runnable() {
                @Override
                    public void run() {
                        switch (openType) {
                            case "browser":
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                Uri content_url = Uri.parse(openLink);
                                intent.setData(content_url);
                                startActivity(intent);
                                break;
                            case "tab_kpi":
                                break;
                            case "tab_analyse":
                                mTabAnalyse.performClick();
                                break;
                            case "tab_app":
                                mTabAPP.performClick();
                                break;
                            case "tab_message":
                                mTabMessage.performClick();
                                break;
                            case "report":
                                Intent subjectIntent = new Intent(DashboardActivity.this, SubjectActivity.class);
                                subjectIntent.putExtra("link", openLink);
                                subjectIntent.putExtra("bannerName", openType);
                                startActivity(subjectIntent);
                                break;
                        }
                }
            });
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
            } catch (JSONException | IOException e) {
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
        public void setDashboardDataCount(final String tabType, final int dataCount) {
           Log.i("setDashboardDataCount", String.format("type: %s, count: %d", tabType, dataCount));
        }

        @JavascriptInterface
        public void jsException(final String ex) {
            Log.i("jsException", ex);

            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put("action", "JS异常");
                logParams.put("obj_type", objectType);
                logParams.put("obj_title", String.format("主页面/%s", ex));
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initUrlStrings() {
        urlStrings = new ArrayList<String>();

        String currentUIVersion = URLs.currentUIVersion(mContext);
        String tmpString;
        try {
            tmpString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
            urlStrings.add(tmpString);
            tmpString = String.format(URLs.ANALYSE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
            urlStrings.add(tmpString);
            tmpString = String.format(URLs.APPLICATION_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
            urlStrings.add(tmpString);
            tmpString = String.format(URLs.MESSAGE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"), user.getString("group_id"), user.getString("user_id"));
            urlStrings.add(tmpString);
            tmpString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
            urlStrings.add(tmpString);
        } catch(JSONException e) {
            e.printStackTrace();
        }

        /*
         * 默认标签栏选中【仪表盘】
         */
        objectType = 1;
        urlString = urlStrings.get(0);
    }
    /**
     *  初始化本地通知
     */
    private void initLocalNotifications() {
        try {
            String noticePath = FileUtil.dirPath(mContext, "Cached", URLs.LOCAL_NOTIFICATION_FILENAME);
            if((new File(noticePath)).exists()) {
                return;
            }

            JSONObject noticeJSON = new JSONObject();
            noticeJSON.put("app", -1);
            noticeJSON.put("tab_kpi", -1);
            noticeJSON.put("tab_kpi_last", -1);
            noticeJSON.put("tab_analyse", -1);
            noticeJSON.put("tab_analyse_last", -1);
            noticeJSON.put("tab_app", "-1");
            noticeJSON.put("tab_app_last", -1);
            noticeJSON.put("tab_message", -1);
            noticeJSON.put("tab_message_last", -1);
            noticeJSON.put("setting", -1);
            noticeJSON.put("setting_pgyer", -1);
            noticeJSON.put("setting_password", -1);

            FileUtil.writeFile(noticePath, noticeJSON.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}
