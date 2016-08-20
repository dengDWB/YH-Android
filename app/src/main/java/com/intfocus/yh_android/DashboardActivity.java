package com.intfocus.yh_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
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
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class DashboardActivity extends BaseActivity implements View.OnClickListener {
    private static final int ZBAR_CAMERA_PERMISSION = 1;
    private int objectType;
    private ImageView imgSetting;
    private TabView mCurrentTab;
    private PopupWindow popupWindow;
    private BadgeView bvUser, bvVoice;
    private BadgeView bvKpi,bvAnalyse,bvApp,bvMessage,bvSetting;
    private LinearLayout linearUserInfo,linearScan,linearVoice,linearSearch;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        /*
         * 默认标签栏选中【仪表盘】
         */
        try {
            objectType = 1;
            urlString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion(), user.getString("group_id"), user.getString("role_id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        imgSetting = (ImageView) findViewById(R.id.btnSetting);
        bvSetting = new BadgeView(DashboardActivity.this, imgSetting);
        bvSetting.setId(7);

        loadWebView();
        initTab();
        initUserIDColorView();
        initDropMenu();

        /*
         * 通过解屏进入界面后，进行用户验证
         */
        checkWhetherFromScreenLockActivity();

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
        super.onDestroy();
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
        } else {
            linearScan.setOnClickListener(this);
        }

        if(!URLs.kDropMenuVoice) {
            viewSeparator = contentView.findViewById(R.id.linearVoiceSeparator);
            viewSeparator.setVisibility(URLs.kDropMenuVoice ? View.VISIBLE : View.GONE);

            linearVoice.setVisibility(URLs.kDropMenuVoice ? View.VISIBLE : View.GONE);
        } else {
            linearVoice.setOnClickListener(this);

            // bvVoice = new BadgeView(DashboardActivity.this, linearVoice);
            // setRedDot(bvVoice, false);
        }

        if(!URLs.kDropMenuSearch) {
            viewSeparator = contentView.findViewById(R.id.linearSearchSeparator);
            viewSeparator.setVisibility(URLs.kDropMenuSearch ? View.VISIBLE : View.GONE);

            linearSearch.setVisibility(URLs.kDropMenuSearch ? View.VISIBLE : View.GONE);
        } else {
            linearSearch.setOnClickListener(this);
        }

        if(!URLs.kDropMenuUserInfo) {
            linearUserInfo.setVisibility(URLs.kDropMenuUserInfo ? View.VISIBLE : View.GONE);
            linearUserInfo.setOnClickListener(this);
        } else {
            linearUserInfo.setOnClickListener(this);

            // bvUser = new BadgeView(DashboardActivity.this, linearUserInfo);
            // setRedDot(bvUser, false);
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
                }
                else {
                    Intent barCodeScannerIntent = new Intent(mContext, BarCodeScannerActivity.class);
                    mContext.startActivity(barCodeScannerIntent);
                }
                break;

            case R.id.linearSearch:
                toast("【搜索】功能开发中，敬请期待");
                break;

            case R.id.linearVoice:
                toast("【语音播报】功能开发中，敬请期待");
                break;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @JavascriptInterface
    private void initTab() {
        TabView mTabKPI = (TabView) findViewById(R.id.tabKPI);
        TabView mTabAnalyse = (TabView) findViewById(R.id.tabAnalyse);
        TabView mTabAPP = (TabView) findViewById(R.id.tabApp);
        TabView mTabMessage = (TabView) findViewById(R.id.tabMessage);

        if(URLs.kTabBar) {
            mTabKPI.setVisibility(URLs.kTabBarKPI ? View.VISIBLE : View.GONE);
            mTabAnalyse.setVisibility( URLs.kTabBarAnalyse ? View.VISIBLE : View.GONE);
            mTabAPP.setVisibility(URLs.kTabBarApp ? View.VISIBLE : View.GONE);
            mTabMessage.setVisibility(URLs.kTabBarMessage ? View.VISIBLE : View.GONE);
        } else {
           findViewById(R.id.toolBar).setVisibility(View.GONE);
        }

        mTabKPI.setOnClickListener(mTabChangeListener);
        mTabAnalyse.setOnClickListener(mTabChangeListener);
        mTabAPP.setOnClickListener(mTabChangeListener);
        mTabMessage.setOnClickListener(mTabChangeListener);

        mCurrentTab = mTabKPI;
        mCurrentTab.setActive(true);
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
            String currentUIVersion = currentUIVersion();
            try {
                switch (v.getId()) {
                    case R.id.tabKPI:
                        objectType = 1;
                        urlString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
                        break;
                    case R.id.tabAnalyse:
                        objectType = 2;
                        urlString = String.format(URLs.ANALYSE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
                        break;
                    case R.id.tabApp:
                        objectType = 3;
                        urlString = String.format(URLs.APPLICATION_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
                        break;
                    case R.id.tabMessage:
                        objectType = 5;
                        urlString = String.format(URLs.MESSAGE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"), user.getString("group_id"), user.getString("user_id"));
                        break;
                    default:
                        objectType = 1;
                        urlString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
                        break;
                }

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
        popupWindow.showAsDropDown(imgSetting, dip2px(this, -87), dip2px(this, 10));

        // Intent intent = new Intent(mContext, SettingActivity.class);
        // mContext.startActivity(intent);

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setRedDot(BadgeView badgeView, boolean flag) {
        badgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
        badgeView.setWidth(dip2px(this, 7));
        badgeView.setHeight(dip2px(this, 7));
        //是否为最右上角
        if(flag){
            badgeView.setBadgeMargin(0, 0);
        }

        badgeView.show();
    }
}
