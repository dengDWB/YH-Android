package com.intfocus.yh_android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.intfocus.yh_android.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

/**
 * Created by lijunjie on 16/6/10.
 */
public class BarCodeScannerActivity extends BaseActivity implements ZBarScannerView.ResultHandler {
	private ZBarScannerView mScannerView;
	private JSONObject barCode;//二维码扫描信息
	private JSONObject storeInfo;//门店信息
	private JSONObject barCodeInfo;//二维码扫描信息及门店信息
	private String barCodePath;//扫码信息保存路径

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_bar_code_scanner);

		barCodePath = FileUtil.basePath(mContext) + "/" + "Cached" + "/" + "barcode.json";

		ViewGroup contentFrame = (ViewGroup) findViewById(R.id.bar_code_scanner_frame);
		mScannerView = new ZBarScannerView(this);
		contentFrame.addView(mScannerView);

		List<ImageView> colorViews = new ArrayList<>();
		colorViews.add((ImageView) findViewById(R.id.colorView0));
		colorViews.add((ImageView) findViewById(R.id.colorView1));
		colorViews.add((ImageView) findViewById(R.id.colorView2));
		colorViews.add((ImageView) findViewById(R.id.colorView3));
		colorViews.add((ImageView) findViewById(R.id.colorView4));
		initColorView(colorViews);
	}

	@Override
	public void onResume() {
		super.onResume();
		mScannerView.setResultHandler(this);
		mScannerView.startCamera();
	}

	@Override
	public void onPause() {
		super.onPause();
		mScannerView.stopCamera();
	}

	@Override
	public void handleResult(Result rawResult) {
		if (rawResult.getContents() == null || rawResult.getContents().isEmpty()) {
		/*
         * Note:
         * Wait 2 seconds to resume the preview.
         *
         * @BUG:
         * On older devices continuously stopping and resuming camera preview can result in freezing the app.
         * I don't know why this is the case but I don't have the time to figure out.
         */
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScannerView.resumeCameraPreview(BarCodeScannerActivity.this);
				}
			}, 2000);
		}
		else {
			try {
				File file = new File(barCodePath);
				if (file.exists()) {
					barCodeInfo = new JSONObject(FileUtil.readFile(barCodePath));

					barCodeInfo.optJSONObject("barcode").put("code_info",rawResult.getContents());
					barCodeInfo.optJSONObject("barcode").put("code_type",rawResult.getBarcodeFormat().getName());
					FileUtil.writeFile(barCodePath,barCodeInfo.toString());
				}
				else {
					barCode = new JSONObject();
					storeInfo = new JSONObject();
					barCodeInfo = new JSONObject();

					barCode.put("code_info",rawResult.getContents());
					barCode.put("code_type",rawResult.getBarcodeFormat().getName());
					barCodeInfo.put("barcode",barCode);
					storeInfo.put("id","-1");
					storeInfo.put("name","默认门店");
					barCodeInfo.put("store",storeInfo);
					FileUtil.writeFile(barCodePath,barCodeInfo.toString());
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Intent intent = new Intent(mContext, BarCodeResultActivity.class);
			mContext.startActivity(intent);
			finish();
		}
	}

	/*
	 * 返回
	 */
	public void dismissActivity(View v) {
		BarCodeScannerActivity.this.onBackPressed();
	}
}
