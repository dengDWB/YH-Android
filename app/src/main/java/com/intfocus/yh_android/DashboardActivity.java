package com.intfocus.yh_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.HttpUtil;
import com.intfocus.yh_android.util.LogUtil;
import com.intfocus.yh_android.util.URLs;
import com.readystatesoftware.viewbadger.BadgeView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;


public class DashboardActivity extends BaseActivity {
	private static final int ZBAR_CAMERA_PERMISSION = 1;
	private int objectType;
	private int displayDpi;
	private TabView mCurrentTab;
	private BadgeView bvKpi;
	private BadgeView bvAnalyse;
	private BadgeView bvApp;
	private BadgeView bvMessage;
	private BadgeView bvBannerSetting;
	private PopupWindow popupWindow;
	private View popupView;
	private JSONObject notifition = new JSONObject();

	private String notifitionGetUrl = "http://iamjay.name/api/notifications/liurl/list";
	private String notifitionPostUrl = "http://iamjay.name/api/notifications/liurl/read/%s";

	@Override
	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);

		ImageView bannerCodeScan = (ImageView) findViewById(R.id.bannerCodeScan);
		bannerCodeScan.setVisibility(URLs.kDashboardDisplayScanCode ? View.VISIBLE : View.INVISIBLE);

		pullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.browser);
		initWebView();
		setPullToRefreshWebView(true);

		mWebView.requestFocus();
		mWebView.addJavascriptInterface(new JavaScriptInterface(), "AndroidJSBridge");
		mWebView.loadUrl(urlStringForLoading);

		//获取当前设备屏幕密度
		DisplayMetrics dm = getResources().getDisplayMetrics();
		displayDpi = dm.densityDpi;
		Log.i("display","屏幕密度为：" + displayDpi);

		try {
			objectType = 1;
			urlString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion(), user.getString("group_id"), user.getString("role_id"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		initTab();
		new initNotifition().execute();

		List<ImageView> colorViews = new ArrayList<>();
		colorViews.add((ImageView) findViewById(R.id.colorView0));
		colorViews.add((ImageView) findViewById(R.id.colorView1));
		colorViews.add((ImageView) findViewById(R.id.colorView2));
		colorViews.add((ImageView) findViewById(R.id.colorView3));
		colorViews.add((ImageView) findViewById(R.id.colorView4));
		initColorView(colorViews);

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
						if (!info.isEmpty() && info.contains("用户") || info.contains("密码")) {
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
		TabView mTabKPI = (TabView) findViewById(R.id.tabKPI);
		TabView mTabAnalyse = (TabView) findViewById(R.id.tabAnalyse);
		TabView mTabAPP = (TabView) findViewById(R.id.tabApp);
		TabView mTabMessage = (TabView) findViewById(R.id.tabMessage);
		ImageView mBannerSetting = (ImageView) findViewById(R.id.banner_setting);

		if (URLs.kDashboardTabBarDisplay) {
			mTabKPI.setVisibility(URLs.kDashboardTabBarDisplayKPI ? View.VISIBLE : View.GONE);
			mTabAnalyse.setVisibility(URLs.kDashboardTabBarDisplayAnalyse ? View.VISIBLE : View.GONE);
			mTabAPP.setVisibility(URLs.kDashboardTabBarDisplayApp ? View.VISIBLE : View.GONE);
			mTabMessage.setVisibility(URLs.kDashboardTabBarDisplayMessage ? View.VISIBLE : View.GONE);
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

		mCurrentTab = mTabKPI;
		mCurrentTab.setActive(true);
	}

	/*
	 * 读取服务器通知信息,若有通知，添加小红点
	 */
	private class initNotifition extends AsyncTask<Void, Void, Map<String, String>> {
		@Override
		protected Map<String, String> doInBackground(Void... voids) {
			Map<String, String> response = HttpUtil.httpGet(notifitionGetUrl,
					new HashMap<String, String>());
			Log.i("notifition",response.get("body").toString());
			return response;
		}

		@Override
		protected void onPostExecute(Map<String, String> response) {
			super.onPostExecute(response);

			int statusCode = Integer.parseInt(response.get("code"));
			if (statusCode == 200) {
				try {
					notifition = new JSONObject(response.get("body").toString());

					if (notifition.getInt("tab_kpi") > 0) {
						setBadgeView(bvKpi);
					}
					if (notifition.getInt("tab_analyse") > 0) {
						setBadgeView(bvAnalyse);
					}
					if (notifition.getInt("tab_app") > 0) {
						setBadgeView(bvApp);
					}
					if (notifition.getInt("tab_message") > 0) {
						setBadgeView(bvMessage);
					}
					if (notifition.getInt("setting") > 0) {
						setBadgeView(bvBannerSetting);
					}
				} catch (JSONException e) {
					Log.i("notifition", "notifition is wrong");
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * 设置应用内通知小红点参数
	 */
	private void setBadgeView(BadgeView badgeView) {
		//根据不同屏幕显示密度设置小红点大小
		if (displayDpi < 320){
			badgeView.setWidth(10);
			badgeView.setHeight(10);
		}
		else if (displayDpi >= 320 && displayDpi < 480){
			badgeView.setWidth(20);
			badgeView.setHeight(20);
		}
		else if (displayDpi >= 480){
			badgeView.setWidth(25);
			badgeView.setHeight(25);
		}

		//badgeView.setText(badgerCount);  //暂不需要计数
		badgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
		if (badgeView == bvBannerSetting) {
			badgeView.setBadgeMargin(20, 15);
		} else {
			badgeView.setBadgeMargin(45, 0);
		}
		badgeView.show();
	}

	/*
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
			if (v == mCurrentTab) return;

			mCurrentTab.setActive(false);
			mCurrentTab = (TabView) v;
			mCurrentTab.setActive(true);

			mWebView.loadUrl(loadingPath("loading"));
			String currentUIVersion = currentUIVersion();
			try {
				switch (v.getId()) {
					case R.id.tabKPI:
						objectType = 1;
						bvKpi.setVisibility(View.GONE);
						notifition.put("tab_kpi", 0);
						notifitionPost("tab_kpi");
						urlString = String.format(URLs.KPI_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("group_id"), user.getString("role_id"));
						break;

					case R.id.tabAnalyse:
						objectType = 2;
						bvAnalyse.setVisibility(View.GONE);
						notifition.put("tab_analyse", 0);
						notifitionPost("tab_analyse");
						urlString = String.format(URLs.ANALYSE_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
						break;

					case R.id.tabApp:
						objectType = 3;
						bvApp.setVisibility(View.GONE);
						notifition.put("tab_app", 0);
						notifitionPost("tab_app");
						urlString = String.format(URLs.APPLICATION_PATH, URLs.kBaseUrl, currentUIVersion, user.getString("role_id"));
						break;

					case R.id.tabMessage:
						objectType = 5;
						bvMessage.setVisibility(View.GONE);
						notifition.put("tab_message", 0);
						notifitionPost("tab_message");
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

	private void notifitionPost(final String notifitionType) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				String urlString = String.format(notifitionPostUrl, notifitionType);
				Map<String, String> response = HttpUtil.httpPost(urlString, notifition);
				Log.i("info", response.get("code").toString());
			}
		});
		thread.start();
	}

	public void launchSettingActivity(View v) {
		try {
			popupView = getLayoutInflater().inflate(R.layout.menu_list,null);
			popupView.getBackground().setAlpha(100);
			popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,true);
			popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			popupWindow.showAsDropDown(v,-150,10);

			bvBannerSetting.setVisibility(View.GONE);
			notifition.put("setting", 0);
			notifitionPost("setting");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void showToast(View v){
		Toast.makeText(DashboardActivity.this,"点击有效",Toast.LENGTH_SHORT).show();
	}

	public void launchBarCodeScannerActivity(View v) {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ZBAR_CAMERA_PERMISSION);
		} else {
			Intent intent = new Intent(mContext, BarCodeScannerActivity.class);
			mContext.startActivity(intent);
		}
	}

	//@Override
	//public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
	//    switch (requestCode) {
	//        case ZBAR_CAMERA_PERMISSION:
	//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
	//
	//                Intent intent = new Intent(this, BarCodeScannerActivity.class);
	//                startActivity(intent);
	//            } else {
	//                toast("Please grant camera permission to use the QR Scanner");
	//            }
	//            return;
	//    }
	//}

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

			} catch (JSONException e) {
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
}
