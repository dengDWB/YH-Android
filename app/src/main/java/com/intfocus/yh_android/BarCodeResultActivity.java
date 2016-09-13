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
import com.intfocus.yh_android.util.URLs;
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
  private String htmlContent, htmlPath, cachedPath;
  private String codeInfo, codeType, groupID, roleID, userNum;
  private String storeID;

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

    String htmlOriginPath = String.format("%s/BarCodeScan/%s",sharedPath,URLs.SCAN_BARCODE_FILENAME);
    htmlContent = FileUtil.readFile(htmlOriginPath);
    cachedPath = FileUtil.dirPath(mContext, URLs.CACHED_DIRNAME, URLs.BARCODE_RESULT_FILENAME);
    htmlPath = String.format("%s.tmp", htmlOriginPath);

    try {
      Intent intent = getIntent();
      codeInfo = intent.getStringExtra("code_info");
      codeType = intent.getStringExtra("code_type");
      groupID = user.getString("group_id");
      roleID = user.getString("role_id");
      userNum = user.getString("user_num");

      /*
       * 初始化默认选中门店（第一家）
       */
      JSONObject cachedJSON = FileUtil.readConfigFile(cachedPath);
      if((!cachedJSON.has("store") || !cachedJSON.getJSONObject("store").has("id")) &&
          user.has("store_ids") && user.getJSONArray("store_ids").length() > 0) {
        cachedJSON.put("store", user.getJSONArray("store_ids").get(0));
        FileUtil.writeFile(cachedPath, cachedJSON.toString());
      }

      /*
       * 商品条形码写入缓存
       */
      JSONObject cachedCodeJSON = new JSONObject();
      cachedCodeJSON.put("code_info", codeInfo);
      cachedCodeJSON.put("code_type", codeType);
      cachedJSON.put("barcode", cachedCodeJSON);
      FileUtil.writeFile(cachedPath, cachedJSON.toString());

      storeID = cachedJSON.getJSONObject("store").getString("id");
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    String loadingString = "{\"chart\": \"[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]\", \"tabs\": [{ title: \"提示\", table: { length: 1, \"1\": [\"加载中...\"]}}]}";
    FileUtil.barCodeScanResult(mContext, loadingString);
    updateHtmlContentTimetamp();
    mWebView.loadUrl(String.format("file:///%s", htmlPath));

    new Thread(new Runnable() {
      @Override public void run() {
        ApiHelper.barCodeScan(mContext, groupID, roleID, userNum, storeID, codeInfo, codeType);
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
  }

  public void updateHtmlContentTimetamp() {
    try {
      String newHtmlContent = htmlContent.replaceAll("TIMESTAMP", String.format("%d", new Date().getTime()));
      Log.i("HtmlContentTimetamp", newHtmlContent);
      FileUtil.writeFile(htmlPath, newHtmlContent);
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  public void actionLaunchStoreSelectorActivity(View v) throws InterruptedException {
    Intent intent = new Intent(mContext, StoreSelectorActivity.class);
    mContext.startActivity(intent);
  }
}
