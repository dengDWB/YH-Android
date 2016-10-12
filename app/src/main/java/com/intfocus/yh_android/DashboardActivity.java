package com.intfocus.yh_android;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.K;
import com.intfocus.yh_android.util.LogUtil;
import com.intfocus.yh_android.util.URLs;
import com.intfocus.yh_android.view.RedPointView;
import com.intfocus.yh_android.view.TabView;
import com.readystatesoftware.viewbadger.BadgeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DashboardActivity extends BaseActivity {
    public final static String kTab = "tab";
    public final static String kUserId = "user_id";

    public static final String ACTION_UPDATENOTIFITION = "action.updateNotifition";
    private static final int ZBAR_CAMERA_PERMISSION = 1;
    private TabView mCurrentTab;
    private ArrayList<String> urlStrings;
    private ArrayList<HashMap<String, Object>> listItem;
    private JSONObject notificationJSON;
    private BadgeView bvKpi, bvAnalyse, bvApp, bvMessage, bvBannerSetting;
    private int objectType, kpiNotifition, analyseNotifition, appNotifition, messageNotifition;
    private NotificationBroadcastReceiver notificationBroadcastReceiver;
    private TabView mTabKPI, mTabAnalyse, mTabAPP, mTabMessage;
    private WebView browserAd;
    private int mAnimationTime;
    private MenuAdapter mSimpleAdapter;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initUrlStrings();
        initDropMenuItem();
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


    private void initDropMenuItem() {
        listItem = new ArrayList<HashMap<String, Object>>();
        int[] imgID = {R.drawable.icon_scan, R.drawable.icon_voice, R.drawable.icon_search, R.drawable.icon_user};
        String[] itemName = {"扫一扫", "语音播报", "搜索", "个人信息"};
        for (int i = 0; i < itemName.length; i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("ItemImage", imgID[i]);
            map.put("ItemText", itemName[i]);
            listItem.add(map);
        }

        mSimpleAdapter = new MenuAdapter(this, listItem, R.layout.menu_list_items, new String[]{"ItemImage", "ItemText"}, new int[]{R.id.img_menu_item, R.id.text_menu_item});
        initDropMenu(mSimpleAdapter, mDropMenuListener);
    }

    /*
      * 标题栏设置按钮下拉菜单点击响应事件
      */
    private final AdapterView.OnItemClickListener mDropMenuListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3) {
            popupWindow.dismiss();
            switch (listItem.get(arg2).get("ItemText").toString()) {
                case "个人信息":
                    Intent settingIntent = new Intent(mContext, SettingActivity.class);
                    mContext.startActivity(settingIntent);
                    break;

                case "扫一扫":
                    if (ContextCompat.checkSelfPermission(DashboardActivity.this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(DashboardActivity.this, new String[]{Manifest.permission.CAMERA}, ZBAR_CAMERA_PERMISSION);
                    } else {
                        Intent barCodeScannerIntent = new Intent(mContext, BarCodeScannerActivity.class);
                        mContext.startActivity(barCodeScannerIntent);
                    }
                    break;

                case "语音播报":
                    toast("功能开发中，敬请期待");
                    break;

                case "搜索":
                    toast("功能开发中，敬请期待");
                    break;
                default:
                    break;
            }
        }
    };

    /*
     * 权限获取反馈
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ZBAR_CAMERA_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent barCodeScannerIntent = new Intent(mContext, BarCodeScannerActivity.class);
                    mContext.startActivity(barCodeScannerIntent);
                } else {
                    Toast.makeText(DashboardActivity.this, "相机权限获取失败，请重试", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("温馨提示")
                .setMessage(String.format("确认退出【%s】？", getResources().getString(R.string.app_name)))
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
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

    /*
     * 仪表盘界面可以显示广告
     */
    private void displayAdOrNot(boolean isShouldLoadHtml) {
		/*
		 * 隐藏广告位
		 */
        if (!K.kDashboardAd) {
            return;
        }
        String adIndexBasePath = FileUtil.sharedPath(this) + "/advertisement/index_android";
        String adIndexPath = adIndexBasePath + ".html";
        String adIndexWithTimestampPath = adIndexBasePath + ".timestamp.html";
        if (new File(adIndexPath).exists()) {
            String htmlContent = FileUtil.readFile(adIndexPath);
            htmlContent = htmlContent.replaceAll("TIMESTAMP", String.format("%d", new Date().getTime()));

            try {
                FileUtil.writeFile(adIndexWithTimestampPath, htmlContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            isShouldLoadHtml = false;
        }

        if (isShouldLoadHtml) {
            browserAd.loadUrl(String.format("file:///%s", adIndexWithTimestampPath));
        }

        boolean isShouldDisplayAd = mCurrentTab == mTabKPI && new File(adIndexPath).exists();
        if (isShouldDisplayAd) {
            viewAnimation(browserAd, true, 0, dip2px(DashboardActivity.this, 140));
        } else {
            viewAnimation(browserAd, false, dip2px(DashboardActivity.this, 140), 0);
        }
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
            String noticePath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kLocalNotificationConfigFileName);
            notificationJSON = FileUtil.readConfigFile(noticePath);
            kpiNotifition = notificationJSON.getInt(URLs.kTabKpi);
            analyseNotifition = notificationJSON.getInt(URLs.kTabAnalyse);
            appNotifition = notificationJSON.getInt(URLs.kTabApp);
            messageNotifition = notificationJSON.getInt(URLs.kTabMessage);

            if (kpiNotifition > 0 && objectType != 1) {
                RedPointView.showRedPoint(mContext, kTab, bvKpi);
            }
            if (analyseNotifition > 0 && objectType != 2) {
                RedPointView.showRedPoint(mContext, kTab, bvAnalyse);
            }
            if (appNotifition > 0 && objectType != 3) {
                RedPointView.showRedPoint(mContext, kTab, bvApp);
            }
            if (messageNotifition > 0 && objectType != 5) {
                RedPointView.showRedPoint(mContext, kTab, bvMessage);
            }
            if (notificationJSON.getInt(URLs.kSetting) > 0) {
                RedPointView.showRedPoint(mContext, URLs.kSetting, bvBannerSetting);
            } else {
                bvBannerSetting.setVisibility(View.GONE);
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
        mWebView.addJavascriptInterface(new JavaScriptInterface(), URLs.kJSInterfaceName);
        animLoading.setVisibility(View.VISIBLE);


        browserAd = (WebView) findViewById(R.id.browserAd);
        browserAd.getSettings().setUseWideViewPort(true);
        browserAd.getSettings().setLoadWithOverviewMode(true);
        browserAd.getSettings().setJavaScriptEnabled(true);
        browserAd.setOverScrollMode(View.OVER_SCROLL_NEVER);
        browserAd.getSettings().setDefaultTextEncodingName("utf-8");
        browserAd.requestFocus();
        browserAd.addJavascriptInterface(new JavaScriptInterface(), URLs.kJSInterfaceName);
        browserAd.setWebViewClient(new WebViewClient());
        browserAd.setWebChromeClient(new WebChromeClient() {
        });
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
                        String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
                        JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

                        String info = ApiHelper.authentication(mContext, userJSON.getString("user_num"), userJSON.getString(URLs.kPassword));
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
            if (!user.getString(URLs.kPassword).equals(URLs.MD5(K.kInitPassword))) {
                return;
            }

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(DashboardActivity.this);
            alertDialog.setTitle("温馨提示");
            alertDialog.setMessage("安全起见，请在【设置】-【个人信息】-【修改登录密码】页面修改初始密码");

            alertDialog.setNegativeButton("知道了", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
            );
            alertDialog.show();
        } catch (JSONException e) {
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

    @SuppressLint("SetJavaScriptEnabled")
    @JavascriptInterface
    private void initTab() {
        mTabKPI = (TabView) findViewById(R.id.tabKPI);
        mTabAnalyse = (TabView) findViewById(R.id.tabAnalyse);
        mTabAPP = (TabView) findViewById(R.id.tabApp);
        mTabMessage = (TabView) findViewById(R.id.tabMessage);
        ImageView mBannerSetting = (ImageView) findViewById(R.id.bannerSetting);

        if (K.kTabBar) {
            mTabKPI.setVisibility(K.kTabBarKPI ? View.VISIBLE : View.GONE);
            mTabAnalyse.setVisibility(K.kTabBarAnalyse ? View.VISIBLE : View.GONE);
            mTabAPP.setVisibility(K.kTabBarApp ? View.VISIBLE : View.GONE);
            mTabMessage.setVisibility(K.kTabBarMessage ? View.VISIBLE : View.GONE);
        } else {

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

            animLoading.setVisibility(View.VISIBLE);
            String currentUIVersion = URLs.currentUIVersion(mContext);

            displayAdOrNot(false);
            try {
                switch (v.getId()) {
                    case R.id.tabKPI:
                        objectType = 1;
                        urlString = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kGroupId), user.getString(URLs.kRoleId));

                        bvKpi.setVisibility(View.GONE);
                        notificationJSON.put(URLs.kTabKpi, 0);
                        break;
                    case R.id.tabAnalyse:
                        objectType = 2;
                        urlString = String.format(K.kAnalyseMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));

                        bvAnalyse.setVisibility(View.GONE);
                        notificationJSON.put(URLs.kTabAnalyse, 0);
                        break;
                    case R.id.tabApp:
                        objectType = 3;
                        urlString = String.format(K.kAppMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));

                        bvApp.setVisibility(View.GONE);
                        notificationJSON.put(URLs.kTabApp, 0);
                        break;
                    case R.id.tabMessage:
                        objectType = 5;
                        urlString = String.format(K.kMessageMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId), user.getString(URLs.kGroupId), kUserId);

                        bvMessage.setVisibility(View.GONE);
                        notificationJSON.put(URLs.kTabMessage, 0);
                        break;
                    default:
                        objectType = 1;
                        urlString = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kGroupId), user.getString(URLs.kRoleId));

                        bvKpi.setVisibility(View.GONE);
                        notificationJSON.put(URLs.kTabKpi, 0);
                        break;
                }

                String notificationPath = FileUtil.dirPath(mContext, K.kCachedDirName, K.kLocalNotificationConfigFileName);
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
                logParams.put(URLs.kAction, "点击/主页面/标签栏");
                logParams.put(URLs.kObjType, objectType);
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /*
     * 标题栏点击设置按钮显示下拉菜单
     */
    public void launchDropMenuActivity(View v) {
        ImageView mBannerSetting = (ImageView) findViewById(R.id.bannerSetting);
        popupWindow.showAsDropDown(mBannerSetting, dip2px(this, -47), dip2px(this, 10));

		/*
		 * 用户行为记录, 单独异常处理，不可影响用户体验
		 */
        try {
            logParams = new JSONObject();
            logParams.put("action", "点击/主页面/下拉菜单");
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
                    intent.putExtra(URLs.kBannerName, bannerName);
                    intent.putExtra(URLs.kLink, link);
                    intent.putExtra(URLs.kObjectId, objectID);
                    intent.putExtra(URLs.kObjectType, objectType);
                    mContext.startActivity(intent);
                }
            });

			/*
			 * 用户行为记录, 单独异常处理，不可影响用户体验
			 */
            try {
                logParams = new JSONObject();
                logParams.put(URLs.kAction, "点击/主页面/浏览器");
                logParams.put("obj_id", objectID);
                logParams.put(URLs.kObjType, objectType);
                logParams.put(URLs.kObjTitle, bannerName);
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void adLink(final String openType, final String openLink, final String ObjeckID, final String objectType
                , final String objectTitle) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (openType) {
                        case "browser":
                            if (openLink == null) {
                                toast("无效链接");
                                break;
                            }
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri content_url = Uri.parse(openLink);
                            intent.setData(content_url);
                            startActivity(intent);
                            break;
                        case URLs.kTabKpi:
                            break;
                        case URLs.kTabAnalyse:
                            mTabAnalyse.performClick();
                            break;
                        case URLs.kTabApp:
                            mTabAPP.performClick();
                            break;
                        case URLs.kTabMessage:
                            if (openLink.equals("0") || openLink.equals("1") || openLink.equals("2")) {

                                storeTabIndex("message", Integer.parseInt(openLink));
                            }
                            mTabMessage.performClick();
                            break;
                        case "report":
                            String[] reportValue = {openLink, ObjeckID, objectType, objectTitle};
                            for (String value : reportValue) {
                                if (value == null || value.equals("")) {
                                    toast("页面跳转失败");
                                    return;
                                }
                            }
                            Intent subjectIntent = new Intent(DashboardActivity.this, SubjectActivity.class);
                            subjectIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            subjectIntent.putExtra(URLs.kLink, openLink);
                            subjectIntent.putExtra(URLs.kBannerName, objectTitle);
                            subjectIntent.putExtra(URLs.kObjectId, ObjeckID);
                            subjectIntent.putExtra(URLs.kObjectType, objectType);
                            startActivity(subjectIntent);
                            break;
                        default:
                            break;
                    }
                }
            });
        }

        @JavascriptInterface
        public void hideAd() {
            viewAnimation(browserAd, false, dip2px(DashboardActivity.this, 140), 0);
        }

        @JavascriptInterface
        public void storeTabIndex(final String pageName, final int tabIndex) {
            try {
                String filePath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kTabIndexConfigFileName);

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
                String filePath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kTabIndexConfigFileName);

                JSONObject config = new JSONObject();
                if ((new File(filePath).exists())) {
                    String fileContent = FileUtil.readFile(filePath);
                    config = new JSONObject(fileContent);
                }
                tabIndex = config.getInt(pageName);

            } catch (JSONException e) {
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
                logParams.put(URLs.kAction, "JS异常");
                logParams.put(URLs.kObjType, objectType);
                logParams.put(URLs.kObjTitle, String.format("主页面/%s", ex));
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * view 缩放动画
     */
    public void viewAnimation(final View view, final Boolean isShow, final int startHeight, final int endHeight) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAnimationTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);//动画效果时间
                final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                ValueAnimator valueAnimator = ValueAnimator.ofInt(startHeight, endHeight);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int adHeight = (int) animation.getAnimatedValue();
                        layoutParams.height = adHeight;
                        view.setLayoutParams(layoutParams);
                        view.requestLayout();
                    }
                });

                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        if (isShow) {
                            view.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (!isShow) {
                            view.setVisibility(View.GONE);
                        }
                    }
                });
                valueAnimator.setDuration(mAnimationTime);
                valueAnimator.start();
            }
        });
    }

    private void initUrlStrings() {
        urlStrings = new ArrayList<String>();

        String currentUIVersion = URLs.currentUIVersion(mContext);
        String tmpString;
        try {
            tmpString = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kGroupId), user.getString(URLs.kRoleId));
            urlStrings.add(tmpString);
            tmpString = String.format(K.kAnalyseMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));
            urlStrings.add(tmpString);
            tmpString = String.format(K.kAppMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));
            urlStrings.add(tmpString);
            tmpString = String.format(K.kMessageMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId), user.getString(URLs.kGroupId), kUserId);
            urlStrings.add(tmpString);
            tmpString = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kGroupId), user.getString(URLs.kRoleId));
            urlStrings.add(tmpString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

		/*
		 * 默认标签栏选中【仪表盘】
		 */
        objectType = 1;
        urlString = urlStrings.get(0);
    }

    /**
     * 初始化本地通知
     */
    private void initLocalNotifications() {
        try {
            String noticePath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kLocalNotificationConfigFileName);
            notificationJSON = FileUtil.readConfigFile(noticePath);
			/*
			 * 版本迭代的问题：
			 * 1. 动态添加新字段
			 * 2. 不可影响已存在字段存放的数据
			 */
            if (!notificationJSON.has("app")) {
                notificationJSON.put("app", -1);
            }
            if (!notificationJSON.has(URLs.kTabKpi)) {
                notificationJSON.put(URLs.kTabKpi, -1);
            }
            if (!notificationJSON.has("tab_kpi_last")) {
                notificationJSON.put("tab_kpi_last", -1);
            }
            if (!notificationJSON.has(URLs.kTabAnalyse)) {
                notificationJSON.put(URLs.kTabAnalyse, -1);
            }
            if (!notificationJSON.has("tab_analyse_last")) {
                notificationJSON.put("tab_analyse_last", -1);
            }
            if (!notificationJSON.has(URLs.kTabApp)) {
                notificationJSON.put(URLs.kTabApp, -1);
            }
            if (!notificationJSON.has("tab_app_last")) {
                notificationJSON.put("tab_app_last", -1);
            }
            if (!notificationJSON.has(URLs.kTabMessage)) {
                notificationJSON.put(URLs.kTabMessage, -1);
            }
            if (!notificationJSON.has("tab_message_last")) {
                notificationJSON.put("tab_message_last", -1);
            }
            if (!notificationJSON.has(URLs.kSetting)) {
                notificationJSON.put(URLs.kSetting, -1);
            }
            if (!notificationJSON.has(URLs.kSettingPgyer)) {
                notificationJSON.put(URLs.kSettingPgyer, -1);
            }
            if (!notificationJSON.has(URLs.kSettingPassword)) {
                notificationJSON.put(URLs.kSettingPassword, -1);
            }
            if (!notificationJSON.has(URLs.kSettingThursdaySay)) {
                notificationJSON.put(URLs.kSettingThursdaySay, -1);
            }
            if (!notificationJSON.has("setting_thursday_say_last")) {
                notificationJSON.put("setting_thursday_say_last", -1);
            }

            FileUtil.writeFile(noticePath, notificationJSON.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}
