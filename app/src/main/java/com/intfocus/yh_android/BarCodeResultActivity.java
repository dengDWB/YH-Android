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

/**
 * Created by lijunjie on 16/6/10.
 */
public class BarCodeResultActivity extends BaseActivity {
  private String htmlContent, htmlPath, codeInfo, codeType, groupID, roleID, userNum;

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

    htmlPath = sharedPath + "/barcode_scan_result.html";
    htmlContent = FileUtil.assetsFileContent(mContext, "barcode_scan_result.html");

    try {
      Intent intent = getIntent();
      codeInfo = intent.getStringExtra("code_info");
      codeType = intent.getStringExtra("code_type");
      groupID = user.getString("group_id");
      roleID = user.getString("role_id");
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

    String loadingString = String.format("{\"商品编号\": \"%s\",  \"状态\": \"处理中...\", \"order_keys\": [\"商品编号\",  \"状态\"]}", codeInfo);
    FileUtil.barCodeScanResult(mContext, loadingString);
    updateHtmlContentTimetamp();
    mWebView.loadUrl(String.format("file:///%s", htmlPath));

    new Thread(new Runnable() {
      @Override public void run() {
        ApiHelper.barCodeScan(mContext, groupID, roleID, userNum, codeInfo, codeType);
        updateHtmlContentTimetamp();

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

  public void updateHtmlContentTimetamp() {
    try {
      String newHtmlContent = htmlContent.replaceAll("TIMESTAMP", String.format("%d", new Date().getTime()));
      Log.i("HtmlContentTimetamp", newHtmlContent);
      FileUtil.writeFile(htmlPath, newHtmlContent);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
}
