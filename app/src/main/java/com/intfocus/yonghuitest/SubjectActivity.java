package com.intfocus.yonghuitest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.LogUtil;
import com.intfocus.yonghuitest.util.URLs;
import com.intfocus.yonghuitest.util.WidgetUtil;
import com.joanzapata.pdfview.PDFView;
import com.joanzapata.pdfview.listener.OnErrorOccurredListener;
import com.joanzapata.pdfview.listener.OnLoadCompleteListener;
import com.joanzapata.pdfview.listener.OnPageChangeListener;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static android.webkit.WebView.enableSlowWholeDocumentDraw;
import static java.lang.String.format;

public class SubjectActivity extends BaseActivity implements OnPageChangeListener, OnLoadCompleteListener, OnErrorOccurredListener {
	private Boolean isInnerLink, isSupportSearch;
	private String templateID, reportID;
	private PDFView mPDFView;
	private File pdfFile;
	private String bannerName, link;
	private int groupID, objectID, objectType;
	private String userNum;
	private RelativeLayout bannerView;
	private ArrayList<HashMap<String, Object>> listItem;
	private Context mContext;
	private int loadCount = 0;
	private TextView mTitle;
	private boolean reportDataState;

	@Override
	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/*
		 * 判断当前设备版本，5.0 以上 Android 系统使用才 enableSlowWholeDocumentDraw();
		 */
        if (Build.VERSION.SDK_INT > 20) {
            enableSlowWholeDocumentDraw();
        }
        setContentView(R.layout.activity_subject);

        mContext = SubjectActivity.this;

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

		mWebView = (WebView) findViewById(R.id.browser);
		initSubWebView();

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

				// 报表缓存列表:是否把报表标题存储
				if (reportDataState && url.contains("report_"+reportID)){
					try {
						SharedPreferences sp = getSharedPreferences("subjectCache", MODE_PRIVATE);
						SharedPreferences.Editor editor = sp.edit();
						String cache = sp.getString("cache","");
						JSONObject json;
						if (cache.equals("")){
							json = new JSONObject();
							json.put("0", bannerName);
						}else {
							boolean isAdd = true;
							json = new JSONObject(cache);
							Iterator<String> it = json.keys();
							while(it.hasNext()){
								String key = it.next();
								if (json.getString(key).equals(bannerName)){
									isAdd = false;
								}
							}
							if (isAdd){
								json.put("" + json.length(), bannerName);
							}
						}
						editor.putString("cache", json.toString());
						editor.commit();
						Log.d("cache111", sp.getString("cache",""));
					}catch (JSONException e){
						e.printStackTrace();
					}
				}
			}

			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				LogUtil.d("onReceivedError",
						String.format("errorCode: %d, description: %s, url: %s", errorCode, description,
								failingUrl));
			}
		});

		mWebView.requestFocus();
		mWebView.setVisibility(View.VISIBLE);
		mWebView.addJavascriptInterface(new JavaScriptInterface(), URLs.kJSInterfaceName);
		animLoading.setVisibility(View.VISIBLE);
		initActiongBar();

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
		ImageView mBannerSetting = (ImageView) findViewById(R.id.bannerSetting);
		mTitle = (TextView) findViewById(R.id.bannerTitle);

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
            mPDFView = (PDFView) findViewById(R.id.pdfview);
            mPDFView.setVisibility(View.INVISIBLE);
        }
        mBannerSetting.setVisibility(View.VISIBLE);
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
		listItem = new ArrayList<>();
		String[] itemName = {"分享", "评论", "刷新"};
		int[] itemImage = {R.drawable.banner_share,
					R.drawable.banner_comment,
					R.drawable.btn_refresh};
//					mTts.isSpeaking() ? R.drawable.btn_stop : R.drawable.btn_play};
		for (int i = 0; i < itemName.length; i++) {
			HashMap<String, Object> map = new HashMap<>();
			map.put("ItemImage", itemImage[i]);
			map.put("ItemText", itemName[i]);
			listItem.add(map);
		}

		if (FileUtil.reportIsSupportSearch(mAppContext, String.format("%d", groupID), templateID, reportID)) {
			HashMap<String, Object> map = new HashMap<>();
			map.put("ItemImage",R.drawable.banner_search);
			map.put("ItemText","筛选");
			listItem.add(map);
		}

		if (!isInnerLink) {
			HashMap<String, Object> map = new HashMap<>();
			map.put("ItemImage",R.drawable.banner_copy);
			map.put("ItemText","拷贝链接");
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

				case "拷贝链接":
					actionCopyLink(arg1);
					break;

				case "分享":
					actionShare2Weixin(arg1);
					break;

				case "评论":
					actionLaunchCommentActivity(arg1);
					break;

				case "刷新":
					refresh(arg1);
					break;

				default:
					break;
			}
		}
	};

	public void onResume() {

		checkInterfaceOrientation(this.getResources().getConfiguration());
		mMyApp.setCurrentActivity(this);
		isWeiXinShared = false;
		/*
		 * 判断是否允许浏览器复制
		 */
		isAllowBrowerCopy();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (isOffline){
							mTitle.setText(bannerName + "(离线)");
						}
					}
				});
			}
		}).start();
		super.onResume();
	}

	protected void displayBannerTitleAndSearchIcon() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String selectedItem = FileUtil.reportSelectedItem(SubjectActivity.this, String.format("%d", groupID), templateID, reportID);
				if (selectedItem == null || selectedItem.length() == 0) {
					ArrayList<String> items = FileUtil.reportSearchItems(SubjectActivity.this, String.format("%d", groupID), templateID, reportID);
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

	/*
	 * 横屏 or 竖屏
	 */
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
			isSupportSearch = FileUtil.reportIsSupportSearch(mAppContext, String.format("%d", groupID), templateID, reportID);
			if (isSupportSearch) {
				displayBannerTitleAndSearchIcon();
			}

			new Thread(new Runnable() {
				@Override
				public void run() {
				reportDataState = ApiHelper.reportData(mAppContext, String.format("%d", groupID), templateID, reportID);
				String jsFileName = "";

				// 模板 4 的 groupID 为 0
				if(Integer.valueOf(templateID) == 4){
					jsFileName = String.format("group_%s_template_%s_report_%s.js", "0", templateID, reportID);
				}else{
					jsFileName = String.format("group_%s_template_%s_report_%s.js", String.format("%d", groupID), templateID, reportID);
				}
				String javascriptPath = String.format("%s/assets/javascripts/%s", sharedPath, jsFileName);
				if(new File(javascriptPath).exists()){
					new Thread(mRunnableForDetecting).start();
				}else{
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							AlertDialog.Builder builder = new AlertDialog.Builder(SubjectActivity.this);
							builder.setTitle("温馨提示")
									.setMessage("报表数据下载失败,不再加载网页")
									.setPositiveButton("确认", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
											SubjectActivity.this.finish();
										}
									});
							builder.show();
						}
					});
				}
				}
			}).start();
		} else {
			urlString = link;

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (urlString.toLowerCase().endsWith(".pdf")) {
						new Thread(mRunnableForPDF).start();
					} else {
                        /*
                         * 外部链接传参: user_num, timestamp
                         */
                        String appendParams = String.format("user_num=%s&timestamp=%s", userNum, URLs.timestamp());
                        String splitString = urlString.contains("?") ? "&" : "?";
                        urlString = String.format("%s%s%s", urlString, splitString, appendParams);
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
            String outputPath = String.format("%s/%s/%s.pdf", FileUtil.basePath(mAppContext), K.kCachedDirName, URLs.MD5(urlString));
            pdfFile = new File(outputPath);
            ApiHelper.downloadFile(mAppContext, urlString, pdfFile);

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
	 * 拷贝链接
	 */
	public void actionCopyLink(View v) {
		ClipboardManager clipboardManager= (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
		clipboardManager.setText(link);
		WidgetUtil.showToastShort(mContext, "链接已拷贝");
	}

    /*
     * 分享截图至微信
     */
    public void actionShare2Weixin(View v) {
        if (link.toLowerCase().endsWith(".pdf")) {
            toast("暂不支持 PDF 分享");
            return;
        }
        if (!isWeiXinShared) {
            toast("网页加载完成,才能使用分享功能");
            return;
        }
        Bitmap imgBmp;
        String filePath = FileUtil.basePath(mAppContext) + "/" + K.kCachedDirName + "/" + "timestmap.png";

        String betaConfigPath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kBetaConfigFileName);
        JSONObject betaJSON = FileUtil.readConfigFile(betaConfigPath);

        try {
            mWebView.setDrawingCacheEnabled(true);
            if (!betaJSON.has("image_within_screen") || betaJSON.getBoolean("image_within_screen")) {
                mWebView.measure(View.MeasureSpec.makeMeasureSpec(
                        View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                int imgMaxHight = displayMetrics.heightPixels * 5;
                if (mWebView.getMeasuredHeight() <= 0 || mWebView.getMeasuredWidth() <= 0 || mWebView.getMeasuredHeight() > imgMaxHight) {
                    toast("截图失败,请尝试系统截图!");
                    return;
                }
                mWebView.buildDrawingCache();
                imgBmp = Bitmap.createBitmap(mWebView.getMeasuredWidth(),
                        mWebView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                if (imgBmp == null && imgBmp.getWidth() <= 0 && imgBmp.getHeight() <= 0) {
                    toast("截图失败");
                    return;
                }
                Canvas canvas = new Canvas(imgBmp);
                Paint paint = new Paint();
                int iHeight = imgBmp.getHeight();
                canvas.drawBitmap(imgBmp, 0, iHeight, paint);
                mWebView.draw(canvas);
            } else {
                imgBmp = mWebView.getDrawingCache();
            }
            FileUtil.saveImage(filePath, imgBmp);
            mWebView.setDrawingCacheEnabled(false);
            imgBmp.recycle(); // 回收 bitmap 资源，避免内存浪费
        } catch (JSONException e) {
            e.printStackTrace();
        }

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

    private final UMShareListener umShareListener = new UMShareListener() {
        @Override
        public void onResult(SHARE_MEDIA platform) {
            Log.d("plat", "platform" + platform);
        }

        @Override
        public void onError(SHARE_MEDIA platform, Throwable t) {
            toast("分享失败啦");
            try {
                logParams = new JSONObject();
                logParams.put("action", "扫码/截图");
                logParams.put("obj_title", "功能: \"扫码/截图\",报错:" + t.getMessage());
                new Thread(mRunnableForLogger).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    @Override
    public void onBackPressed() {
        finish();
    }

    public void refresh(View v) {
        animLoading.setVisibility(View.VISIBLE);
        new refreshTask().execute();
    }

    private class refreshTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            /*
             *  浏览器刷新时，删除响应头文件，相当于无缓存刷新
             */
            if (isInnerLink) {
                String urlKey;
                if (urlString != null && !urlString.isEmpty()) {
                    urlKey = urlString.contains("?") ? TextUtils.split(urlString, "?")[0] : urlString;
                    ApiHelper.clearResponseHeader(urlKey, assetsPath);
                }
                urlKey = String.format(K.kReportDataAPIPath, K.kBaseUrl, groupID, templateID, reportID);
                ApiHelper.clearResponseHeader(urlKey, FileUtil.sharedPath(mAppContext));

                boolean reportDataState = ApiHelper.reportData(mAppContext, String.format("%d", groupID), templateID, reportID);

                if (reportDataState) {
                    new Thread(mRunnableForDetecting).start();
                } else {
                    showWebViewExceptionForWithoutNetwork();
                }

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
        }
    }

    private class JavaScriptInterface extends JavaScriptBase {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void storeTabIndex(final String pageName, final int tabIndex) {
            try {
                String filePath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kTabIndexConfigFileName);

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
                String filePath = FileUtil.dirPath(mAppContext, K.kConfigDirName, K.kTabIndexConfigFileName);

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

                //点击两次还是有异常 异常报出
                if (loadCount < 2) {
                    showWebViewExceptionForWithoutNetwork();
                    loadCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void reportSearchItems(final String arrayString) {
            try {
                String searchItemsPath = String.format("%s.search_items", FileUtil.reportJavaScriptDataPath(SubjectActivity.this, String.format("%d", groupID), templateID, reportID));
                FileUtil.writeFile(searchItemsPath, arrayString);

                /**
                 *  判断筛选的条件: arrayString 数组不为空
                 *  报表第一次加载时，此处为判断筛选功能的关键点
                 */
                isSupportSearch = FileUtil.reportIsSupportSearch(SubjectActivity.this, String.format("%d", groupID), templateID, reportID);
                if (isSupportSearch) {
                    displayBannerTitleAndSearchIcon();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String reportSelectedItem() {
            String item = null;
            String selectedItemPath = String.format("%s.selected_item", FileUtil.reportJavaScriptDataPath(SubjectActivity.this, String.format("%d", groupID), templateID, reportID));
            if (new File(selectedItemPath).exists()) {
                item = FileUtil.readFile(selectedItemPath);
            }
            return item;
        }

        @JavascriptInterface
        public void refreshBrowser() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    animLoading.setVisibility(View.VISIBLE);
                    loadHtml();
                }
            });
        }
    }
}
