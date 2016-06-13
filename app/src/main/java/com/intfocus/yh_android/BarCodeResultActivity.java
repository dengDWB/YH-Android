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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;

/**
 * Created by lijunjie on 16/6/10.
 */
public class BarCodeResultActivity extends BaseActivity {
  private String htmlPath, codeInfo, codeType, userNum;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_bar_code_result);

    mWebView = (WebView) findViewById(R.id.webview);
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


    htmlPath = sharedPath + "/bar_code_scan_result.html";
    if(!(new File(htmlPath).exists())) {
      FileUtil.copyAssetFile(mContext, "bar_code_scan_result.html", htmlPath);
    }

    try {
      Intent intent = getIntent();
      codeInfo = intent.getStringExtra("code_info");
      codeType = intent.getStringExtra("code_type");
      userNum = user.getString("user_num");
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
      @Override public void run() {
        ApiHelper.barCodeScan(mContext, userNum, codeInfo, codeType);

        runOnUiThread(new Runnable() {
          @Override public void run() {
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
  };
}
