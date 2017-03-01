package com.intfocus.yonghuitest;

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

import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.LogUtil;
import com.intfocus.yonghuitest.util.URLs;
import com.intfocus.yonghuitest.view.RedPointView;
import com.intfocus.yonghuitest.view.TabView;
import com.pgyersdk.update.PgyUpdateManager;
import com.readystatesoftware.viewbadger.BadgeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.intfocus.yonghuitest.util.URLs.kGroupId;

public class DashboardActivity extends BaseActivity {
	private final static String kTab = "tab";
	private final static String kUserId = "user_id";

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
	private String currentUIVersion = "";

	private Context mContext;
	private int loadCount = 0;

	@Override
	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);

		mContext = this;

		initUrlStrings();
		initTab();
		readBehaviorFile();
		initUserIDColorView();
		loadWebView();
		displayAdOrNot(true);

		/*
		 * 检测版本更新
		 */
		checkPgyerVersionUpgrade(DashboardActivity.this,false);

		/*
         * 通过解屏进入界面后，进行用户验证
     	 */
		checkWhetherFromScreenLockActivity();

		/*
         * 检测服务器静态资源是否更新，并下载
     	 */
		checkAssetsUpdated(true);

        /*
         * 初始化本地通知
         */
		FileUtil.initLocalNotifications(mAppContext);

		/*
		 * 语音播报初始化
		 */
//		SpeechUtility.createUtility(mAppContext, "appid=581aa9e1");

		/*
         * 动态注册广播用于接收通知
		 */
		initNotificationService();

		dealSendMessage();
		if (urlStrings.get(3).equals(urlString)) {
			setWebViewLongListener(false);
		}

		new Thread(mRunnableForDetecting).start();

		checkUserModifiedInitPassword();
	}

	protected void onResume() {
		mMyApp.setCurrentActivity(this);
		/*
		 * 启动 Activity 时也需要判断小红点是否显示
		 */
		receiveNotification();

		/*
		 * 判断是否允许浏览器复制
		 */
		isAllowBrowerCopy();

		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (popupWindow != null) {
			popupWindow.dismiss();
		}
		super.onStop();
	}

	protected void onDestroy() {
		mWebView = null;
		user = null;
		PgyUpdateManager.unregister(); // 解除注册蒲公英版本更新检查
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
						mMyApp.setCurrentActivity(null);
						finish();
						System.exit(0);
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

	private void dealSendMessage() {
		currentUIVersion = URLs.currentUIVersion(mAppContext);
		String pushMessagePath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kPushMessageFileName);
		JSONObject pushMessageJSON = FileUtil.readConfigFile(pushMessagePath);
		try {
			if (pushMessageJSON.has("state") && pushMessageJSON.getBoolean("state")) {
				return;
			}
			if (pushMessageJSON.has("type")) {
				String type = pushMessageJSON.getString("type");
				switch (type) {
					case "report":
						Intent subjectIntent = new Intent(this, SubjectActivity.class);
						subjectIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
						subjectIntent.putExtra(URLs.kLink, pushMessageJSON.getString("url"));
						subjectIntent.putExtra(URLs.kBannerName, pushMessageJSON.getString("title"));
						subjectIntent.putExtra(URLs.kObjectId, pushMessageJSON.getInt("object_id"));
						subjectIntent.putExtra(URLs.kObjectType, pushMessageJSON.getInt("object_type"));
						startActivity(subjectIntent);
						break;
					case "analyse":
						jumpTab(mTabAnalyse);
						urlString = String.format(K.kAnalyseMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));
						break;
					case "app":
						jumpTab(mTabAPP);
						urlString = String.format(K.kAppMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));
						break;
					case "message":
						jumpTab(mTabMessage);
						urlString = String.format(K.kMessageMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId), user.getString(kGroupId), user.getString(kUserId));
						break;
					case "thursday_say":
						Intent blogLinkIntent = new Intent(DashboardActivity.this, ThursdaySayActivity.class);
						startActivity(blogLinkIntent);
				}
			}
			pushMessageJSON.put("state", true);
			FileUtil.writeFile(pushMessagePath, pushMessageJSON.toString());
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	public void jumpTab(TabView tabView) {
		mCurrentTab.setActive(false);
		mCurrentTab = tabView;
		mCurrentTab.setActive(true);
	}

	/*
	 * 初始化下拉菜单按钮
	 */
	private void initDropMenuItem() {
		listItem = new ArrayList<>();
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
						setAlertDialog(DashboardActivity.this, "相机权限获取失败，是否到本应用的设置界面设置权限");
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
		 * 打开通知服务, 用于发送通知
         */
		Intent intent = new Intent(this, LocalNotificationService.class);
		startService(intent);
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
			String noticePath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kLocalNotificationConfigFileName);
			notificationJSON = FileUtil.readConfigFile(noticePath);
			kpiNotifition = notificationJSON.getInt(URLs.kTabKpi);
			analyseNotifition = notificationJSON.getInt(URLs.kTabAnalyse);
			appNotifition = notificationJSON.getInt(URLs.kTabApp);
			messageNotifition = notificationJSON.getInt(URLs.kTabMessage);

			if (kpiNotifition > 0 && objectType != 1) {
				RedPointView.showRedPoint(mAppContext, kTab, bvKpi);
			}
			if (analyseNotifition > 0 && objectType != 2) {
				RedPointView.showRedPoint(mAppContext, kTab, bvAnalyse);
			}
			if (appNotifition > 0 && objectType != 3) {
				RedPointView.showRedPoint(mAppContext, kTab, bvApp);
			}
			if (messageNotifition > 0 && objectType != 5) {
				RedPointView.showRedPoint(mAppContext, kTab, bvMessage);
			}
			if (notificationJSON.getInt(URLs.kSetting) > 0) {
				RedPointView.showRedPoint(mAppContext, URLs.kSetting, bvBannerSetting);
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
		initPullWebView();
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

			new Thread(new Runnable() {
				@Override
				public synchronized void run() {
					try {
						String userConfigPath = String.format("%s/%s", FileUtil.basePath(mAppContext), K.kUserConfigFileName);
						JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);

						String info = ApiHelper.authentication(mAppContext, userJSON.getString("user_num"), userJSON.getString(URLs.kPassword));
						if (!info.isEmpty() && (info.contains("用户") || info.contains("密码"))) {
							// 解锁验证信息失败,也只变化登录状态,其余状况保持登录状态
							JSONObject configJSON = new JSONObject();
							configJSON.put("is_login", false);

							modifiedUserConfig(configJSON);
						}
					} catch (JSONException e) {
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
			/*
		     * 判断是否允许浏览器复制
		 	 */
			isAllowBrowerCopy();

			mCurrentTab.setActive(false);
			mCurrentTab = (TabView) v;
			mCurrentTab.setActive(true);

			animLoading.setVisibility(View.VISIBLE);
			String currentUIVersion = URLs.currentUIVersion(mAppContext);

			displayAdOrNot(false);
			try {
				switch (v.getId()) {
					case R.id.tabKPI:
						objectType = 1;
						urlString = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, user.getString(
								kGroupId), user.getString(URLs.kRoleId));

						bvKpi.setVisibility(View.GONE);
						notificationJSON.put(URLs.kTabKpi, 0);
						FileUtil.writeBehaviorFile(mAppContext,urlString,0);
						break;
					case R.id.tabAnalyse:
						objectType = 2;
						urlString = String.format(K.kAnalyseMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));

						bvAnalyse.setVisibility(View.GONE);
						notificationJSON.put(URLs.kTabAnalyse, 0);
						FileUtil.writeBehaviorFile(mAppContext,urlString,1);
						break;
					case R.id.tabApp:
						objectType = 3;
						urlString = String.format(K.kAppMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));

						bvApp.setVisibility(View.GONE);
						notificationJSON.put(URLs.kTabApp, 0);
						FileUtil.writeBehaviorFile(mAppContext,urlString,2);
						break;
					case R.id.tabMessage:
						objectType = 5;
						urlString = String.format(K.kMessageMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId), user.getString(kGroupId), user.getString(kUserId));

						bvMessage.setVisibility(View.GONE);
						notificationJSON.put(URLs.kTabMessage, 0);
						FileUtil.writeBehaviorFile(mAppContext,urlString,3);
						setWebViewLongListener(false);
						break;
					default:
						objectType = 1;
						urlString = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, user.getString(
								kGroupId), user.getString(URLs.kRoleId));

						bvKpi.setVisibility(View.GONE);
						notificationJSON.put(URLs.kTabKpi, 0);
						FileUtil.writeBehaviorFile(mAppContext,urlString,0);
						break;
				}

				String notificationPath = FileUtil.dirPath(mAppContext, K.kCachedDirName, K.kLocalNotificationConfigFileName);
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
				logParams.put(URLs.kAction, " 点击 / 主页面 / 标签栏 ");
				logParams.put(URLs.kObjType, objectType);
				new Thread(mRunnableForLogger).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	/*
	 * 读取用户习惯记录,即用户上次退出时所在 Tab 页面
	 */
	public void readBehaviorFile() {
		try {
			String behaviorPath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kBehaviorConfigFileName);
			if (new File(behaviorPath).exists()) {
				JSONObject dashboardJson = FileUtil.readConfigFile(behaviorPath);
				if (dashboardJson.has("dashboard")) {
					JSONObject behaviorJson = new JSONObject(dashboardJson.getString("dashboard"));
					int tabIndex = behaviorJson.getInt("tab_index");
					switch (tabIndex){
						case 0:
							mCurrentTab = mTabKPI;
							mCurrentTab.setActive(true);
							objectType = 1;
							break;
						case 1:
							mCurrentTab = mTabAnalyse;
							mCurrentTab.setActive(true);
							objectType = 2;
							break;
						case 2:
							mCurrentTab = mTabAPP;
							mCurrentTab.setActive(true);
							objectType = 3;
							break;
						case 3:
							mCurrentTab = mTabMessage;
							mCurrentTab.setActive(true);
							objectType = 5;
							break;
					}
					urlString = urlStrings.get(tabIndex);
				}else {
					mCurrentTab = mTabKPI;
					mCurrentTab.setActive(true);
					objectType = 1;
					urlString = urlStrings.get(0);
				}
			}
			else {
				mCurrentTab = mTabKPI;
				mCurrentTab.setActive(true);
				objectType = 1;
				urlString = urlStrings.get(0);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 标题栏点击设置按钮显示下拉菜单
	 */
	public void launchDropMenuActivity(View v) {
		initDropMenuItem();
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
						layoutParams.height = (int) animation.getAnimatedValue();
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
		urlStrings = new ArrayList<>();

		String currentUIVersion = URLs.currentUIVersion(mAppContext);
		String tmpString;
		try {
			tmpString = String.format(K.kKPIMobilePath, K.kBaseUrl, currentUIVersion, user.getString(
					kGroupId), user.getString(URLs.kRoleId));
			urlStrings.add(tmpString);
			tmpString = String.format(K.kAnalyseMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));
			urlStrings.add(tmpString);
			tmpString = String.format(K.kAppMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId));
			urlStrings.add(tmpString);
			tmpString = String.format(K.kMessageMobilePath, K.kBaseUrl, currentUIVersion, user.getString(URLs.kRoleId), user.getString(
					kGroupId), user.getString(kUserId));
			urlStrings.add(tmpString);
			tmpString = String.format(K.kThursdaySayMobilePath, K.kBaseUrl, currentUIVersion);
			urlStrings.add(tmpString);
		} catch (JSONException e) {
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
			if (null == link || link.equals("")) {
				toast("该功能正在开发中");
				return;
			}
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
					DashboardActivity.this.startActivity(intent);
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
				String filePath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kBehaviorConfigFileName);

				if ((new File(filePath).exists())) {
					String fileContent = FileUtil.readFile(filePath);
					JSONObject jsonObject = new JSONObject(fileContent);
					JSONObject config = new JSONObject(jsonObject.getString("dashboard"));
					config.put(pageName, tabIndex);
					jsonObject.put("dashboard",config.toString());

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
					if (config.has(pageName)){
						tabIndex = config.getInt(pageName);
					}
					Log.d("Tab", tabIndex+"");
				}

			} catch (JSONException e) {
				e.printStackTrace();
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

				//点击两次还是有异常 异常报出
				if (loadCount < 2) {
					showWebViewExceptionForWithoutNetwork();
					loadCount++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
