package com.intfocus.yh_android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;

import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lijunjie on 16/6/10.
 */
public class BarCodeResultActivity extends BaseActivity {
	private String htmlContent, barCodePath, htmlPath, codeInfo, codeType, groupID, roleID, userNum, storeID;
	private JSONObject barCodeInfo;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_bar_code_result);

		mWebView = (WebView) findViewById(R.id.browser);
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDefaultTextEncodingName("utf-8");
		webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

		List<ImageView> colorViews = new ArrayList<>();
		colorViews.add((ImageView) findViewById(R.id.colorView0));
		colorViews.add((ImageView) findViewById(R.id.colorView1));
		colorViews.add((ImageView) findViewById(R.id.colorView2));
		colorViews.add((ImageView) findViewById(R.id.colorView3));
		colorViews.add((ImageView) findViewById(R.id.colorView4));
		initColorView(colorViews);

		htmlPath = sharedPath + "/" + "BarCodeScan" + "/" + "scan_bar_code.html";
		barCodePath = FileUtil.basePath(mContext) + "/" + "Cached" + "/" + "barcode.json";
		htmlContent = FileUtil.readFile(htmlPath);

		try {
			barCodeInfo = new JSONObject(FileUtil.readFile(barCodePath));
			codeInfo = barCodeInfo.getJSONObject("barcode").getString("code_info");
			codeType = barCodeInfo.getJSONObject("barcode").getString("code_type");
			groupID = user.getString("group_id");
			roleID = user.getString("role_id");
			userNum = user.getString("user_num");
			storeID = barCodeInfo.getJSONObject("store").getString("id");
			Log.i("barcode", user.getString("store_ids"));
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		new Thread(new Runnable() {
			@Override
			public void run() {
				ApiHelper.barCodeScan(mContext, groupID, roleID, userNum, storeID, codeInfo, codeType);
				updateHtmlContentTimetamp();

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mWebView.loadUrl(String.format("file:///%s", htmlPath));
					}
				});
			}
		}).start();
	}

	/*
	 * 返回
	 */
	public void dismissActivity(View v) {
		BarCodeResultActivity.this.onBackPressed();
	}

	/*
	 * 更新时间戳
	 */
	public void updateHtmlContentTimetamp() {
		try {
			String newHtmlContent = htmlContent.replaceAll("TIMESTAMP", String.format("%d", new Date().getTime()));
			Log.i("HtmlContentTimetamp", newHtmlContent);
			FileUtil.writeFile(htmlPath, newHtmlContent);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 跳转门店列表选择页面
	 */
	public void launchStoreSelect(View v) {
		Intent intent = new Intent(BarCodeResultActivity.this, StoreSelectorActivity.class);
		mContext.startActivity(intent);
	}
}
