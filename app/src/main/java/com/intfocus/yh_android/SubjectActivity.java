package com.intfocus.yh_android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshWebView;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.K;
import com.intfocus.yh_android.util.URLs;
import com.joanzapata.pdfview.PDFView;
import com.joanzapata.pdfview.listener.OnErrorOccurredListener;
import com.joanzapata.pdfview.listener.OnLoadCompleteListener;
import com.joanzapata.pdfview.listener.OnPageChangeListener;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

import static android.webkit.WebView.enableSlowWholeDocumentDraw;
import static java.lang.String.format;

public class SubjectActivity extends BaseActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorOccurredListener {
	private Boolean isInnerLink, isSupportSearch;
	private String templateID, reportID;
	private PDFView mPDFView;
	private ImageView mBannerComment,mBannerSetting;
	private File pdfFile;
	private String bannerName, link;
	private int groupID, objectID, objectType;
	private String userNum;
	private RelativeLayout bannerView;
	private ArrayList<HashMap<String, Object>> listItem = new ArrayList<>();
	private Boolean isShowSearchButton = false;

	@Override
	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
         * 判断当前设备版本，5.0 以上 Android 系统使用才 enableSlowWholeDocumentDraw();
         */
		int sysVersion = Build.VERSION.SDK_INT;
		if (sysVersion > 20) {
			enableSlowWholeDocumentDraw();
		}
		setContentView(R.layout.activity_subject);
		mMyApp.setCurrentActivity(this);

        /*
         * JSON Data
         */
		try {
			groupID = user.getInt(URLs.kGroupId);
			userNum = user.getString(URLs.kUserNum);
		} catch (JSONException e) {
			e.printStackTrace();
			groupID = -2;
			userNum = "not-set";
		}

		pullToRefreshWebView = (PullToRefreshWebView) findViewById(R.id.browser);
		initWebView();

		mWebView.requestFocus();
		pullToRefreshWebView.setVisibility(View.VISIBLE);
		mWebView.addJavascriptInterface(new JavaScriptInterface(), URLs.kJSInterfaceName);
		mWebView.loadUrl(urlStringForLoading);
		initActiongBar();
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

		List<ImageView> colorViews = new ArrayList<>();
		colorViews.add((ImageView) findViewById(R.id.colorView0));
		colorViews.add((ImageView) findViewById(R.id.colorView1));
		colorViews.add((ImageView) findViewById(R.id.colorView2));
		colorViews.add((ImageView) findViewById(R.id.colorView3));
		colorViews.add((ImageView) findViewById(R.id.colorView4));
		initColorView(colorViews);
	}

	private void initActiongBar(){
		bannerView = (RelativeLayout) findViewById(R.id.actionBar);
		mBannerComment = (ImageView) findViewById(R.id.bannerComment);
		mBannerSetting = (ImageView) findViewById(R.id.bannerSetting);
		TextView mTitle = (TextView) findViewById(R.id.bannerTitle);

		/*
         * Intent Data || JSON Data
         */
		Intent intent = getIntent();
		link = intent.getStringExtra(URLs.kLink);
		bannerName = intent.getStringExtra(URLs.kBannerName);
		objectID = intent.getIntExtra(URLs.kObjectId, -1);
		objectType = intent.getIntExtra(URLs.kObjectType, -1);
		isInnerLink = !(link.startsWith("http://") || link.startsWith("https://"));
		mTitle.setText(bannerName);

		if (link.toLowerCase().endsWith(".pdf")) {
			mBannerComment.setVisibility(View.VISIBLE);
			mPDFView = (PDFView) findViewById(R.id.pdfview);
			mPDFView.setVisibility(View.INVISIBLE);
			return;
		}
		mBannerSetting.setVisibility(View.VISIBLE);
		initDropMenuItem();
	}

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
			logParams.put("action", "点击/报表/下拉菜单");
			new Thread(mRunnableForLogger).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 初始化标题栏下拉菜单
	 */
	private void initDropMenuItem() {
		String[] itemName = {"分享", "评论"};
		int[] itemImage = {R.drawable.banner_share, R.drawable.banner_comment};
		for (int i = 0; i < itemName.length; i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("ItemImage", itemImage[i]);
			map.put("ItemText", itemName[i]);
			listItem.add(map);
		}

		SimpleAdapter mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.menu_list_items, new String[]{"ItemImage", "ItemText"}, new int[]{R.id.img_menu_item, R.id.text_menu_item});
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
				case "筛选":
					actionLaunchReportSelectorActivity(arg1);
					break;

				case "分享":
					actionShare2Weixin(arg1);
					break;

				case "评论":
					actionLaunchCommentActivity(arg1);
					break;

				default:
					break;
			}
		}
	};

	protected void onResume() {
		checkInterfaceOrientation(this.getResources().getConfiguration());

		mMyApp.setCurrentActivity(this);
		super.onResume();
	}

	protected void displayBannerTitleAndSearchIcon() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isShowSearchButton) {
					HashMap<String, Object> map = new HashMap<>();
					map.put("ItemImage", R.drawable.banner_search);
					map.put("ItemText", "筛选");
					listItem.add(map);
					SimpleAdapter mSimpleAdapter = new SimpleAdapter(mContext, listItem, R.layout.menu_list_items, new String[]{"ItemImage", "ItemText"}, new int[]{R.id.img_menu_item, R.id.text_menu_item});
					initDropMenu(mSimpleAdapter, mDropMenuListener);
					isShowSearchButton = true;
				}

				String selectedItem = FileUtil.reportSelectedItem(mContext, String.format("%d", groupID), templateID, reportID);
				if (selectedItem == null || selectedItem.length() == 0) {
					ArrayList<String> items = FileUtil.reportSearchItems(mContext, String.format("%d", groupID), templateID, reportID);
					if (items.size() > 0) {
						selectedItem = items.get(0);
					}
					else {
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

		if (!(new File(htmlPath)).exists()) {
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
		}
		else {
			WindowManager.LayoutParams attr = getWindow().getAttributes();
			attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().setAttributes(attr);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		}

		mWebView.post(new Runnable() {
			@Override
			public void run() {
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
			urlString = String.format("%s%s", K.kBaseUrl, urlPath);
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
			if (isSupportSearch) {
				displayBannerTitleAndSearchIcon();
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
			String outputPath = String.format("%s/%s/%s.pdf", FileUtil.basePath(mContext), K.kCachedDirName, URLs.MD5(urlString));
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
		intent.putExtra(URLs.kBannerName, bannerName);
		intent.putExtra(URLs.kGroupId, groupID);
		intent.putExtra("reportID", reportID);
		intent.putExtra("templateID", templateID);
		mContext.startActivity(intent);
	}

	/*
	 * 分享截图至微信
	 */
	public void actionShare2Weixin(View v) {
		String filePath = FileUtil.basePath(mContext) + "/" + K.kCachedDirName + "/" + "timestmap.png";
		mWebView.measure(View.MeasureSpec.makeMeasureSpec(
				View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		mWebView.setDrawingCacheEnabled(true);
		mWebView.buildDrawingCache();
		int imgMaxHight = displayMetrics.heightPixels * 5;
		if (mWebView.getMeasuredHeight() > imgMaxHight) {
			toast("截图失败,请尝试系统截图!");
			return;
		}
		Bitmap imgBmp = Bitmap.createBitmap(mWebView.getMeasuredWidth(),
				mWebView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		if (imgBmp == null) {
			toast("截图失败");
		}
		Canvas canvas = new Canvas(imgBmp);
		Paint paint = new Paint();
		int iHeight = imgBmp.getHeight();
		canvas.drawBitmap(imgBmp, 0, iHeight, paint);
		mWebView.draw(canvas);
		FileUtil.saveImage(filePath, imgBmp);
		imgBmp.recycle(); // 回收 bitmap 资源，避免内存浪费

		File file = new File(filePath);
		if (file.exists() && file.length() > 0) {
			UMImage image = new UMImage(SubjectActivity.this, file);

			new ShareAction(this)
					.withTitle("分享截图")
					.setPlatform(SHARE_MEDIA.WEIXIN)
					.setDisplayList(SHARE_MEDIA.WEIXIN)
					.setCallback(umShareListener)
					.withMedia(image)
					.open();
		} else {
			toast("截图失败,请尝试系统截图");
		}
	}

	private UMShareListener umShareListener = new UMShareListener() {
		@Override
		public void onResult(SHARE_MEDIA platform) {
			Log.d("plat", "platform" + platform);
		}

		@Override
		public void onError(SHARE_MEDIA platform, Throwable t) {
			toast("分享失败啦");
			if (t != null) {
				Log.d("throw", "throw:" + t.getMessage());
			}
		}

		@Override
		public void onCancel(SHARE_MEDIA platform) {
			// 取消分享
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
	}

	/*
	 * 评论
	 */
	public void actionLaunchCommentActivity(View v) {
		Intent intent = new Intent(mContext, CommentActivity.class);
		intent.putExtra(URLs.kBannerName, bannerName);
		intent.putExtra(URLs.kObjectId, objectID);
		intent.putExtra(URLs.kObjectType, objectType);
		mContext.startActivity(intent);

        /*
         * 用户行为记录, 单独异常处理，不可影响用户体验
         */
		try {
			logParams = new JSONObject();
			logParams.put(URLs.kAction, "点击/主题页面/评论");
			new Thread(mRunnableForLogger).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * 返回
	 */
	public void dismissActivity(View v) {
		SubjectActivity.this.onBackPressed();
	}

	private class pullToRefreshTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// 如果这个地方不使用线程休息的话，刷新就不会显示在那个 PullToRefreshListView 的 UpdatedLabel 上面

            /*
             *  下拉浏览器刷新时，删除响应头文件，相当于无缓存刷新
             */
			if (isInnerLink) {
				String urlKey;
				if (urlString != null && !urlString.isEmpty()) {
					urlKey = urlString.contains("?") ? TextUtils.split(urlString, "?")[0] : urlString;
					ApiHelper.clearResponseHeader(urlKey, assetsPath);
				}
				urlKey = String.format(K.kReportDataAPIPath, K.kBaseUrl, groupID, templateID, reportID);
				ApiHelper.clearResponseHeader(urlKey, FileUtil.sharedPath(mContext));

				ApiHelper.reportData(mContext, String.format("%d", groupID), templateID, reportID);
				new Thread(mRunnableForDetecting).start();
                /*
                 * 用户行为记录, 单独异常处理，不可影响用户体验
                 */
				try {
					logParams = new JSONObject();
					logParams.put(URLs.kAction, "刷新/浏览器");
					logParams.put(URLs.kObjTitle, urlString);
					new Thread(mRunnableForLogger).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			loadHtml();
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
				logParams.put(URLs.kAction, "JS异常");
				logParams.put("obj_id", objectID);
				logParams.put(URLs.kObjType, objectType);
				logParams.put(URLs.kObjTitle, String.format("主题页面/%s/%s", bannerName, ex));
				new Thread(mRunnableForLogger).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@JavascriptInterface
		public void reportSearchItems(final String arrayString) {
			try {
				String searchItemsPath = String.format("%s.search_items", FileUtil.reportJavaScriptDataPath(mContext, String.format("%d", groupID), templateID, reportID));
				if (!new File(searchItemsPath).exists()) {
					FileUtil.writeFile(searchItemsPath, arrayString);

					/**
					 *  判断筛选的条件: arrayString 数组不为空
					 *  报表第一次加载时，此处为判断筛选功能的关键点
					 */
					isSupportSearch = FileUtil.reportIsSupportSearch(mContext, String.format("%d", groupID), templateID, reportID);
					if (isSupportSearch) {
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
			if (new File(selectedItemPath).exists()) {
				item = FileUtil.readFile(selectedItemPath);
			}
			return item;
		}
	}
}
