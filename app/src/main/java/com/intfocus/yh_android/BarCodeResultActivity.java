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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by lijunjie on 16/6/10.
 */
public class BarCodeResultActivity extends BaseActivity {
  public final static String kId = "id";
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
      codeInfo = intent.getStringExtra(URLs.kCodeInfo);
      codeType = intent.getStringExtra(URLs.kCodeType);
      groupID = user.getString(URLs.kGroupId);
      roleID = user.getString(URLs.kRoleId);
      userNum = user.getString("user_num");

      /*
       * 初始化默认选中门店（第一家）
       */
      JSONObject cachedJSON = FileUtil.readConfigFile(cachedPath);
      if((!cachedJSON.has(URLs.kStore) || !cachedJSON.getJSONObject(URLs.kStore).has(kId)) &&
          user.has(URLs.kStoreIds) && user.getJSONArray(URLs.kStoreIds).length() > 0) {
        cachedJSON.put(URLs.kStore, user.getJSONArray(URLs.kStoreIds).get(0));
        FileUtil.writeFile(cachedPath, cachedJSON.toString());
      }

      /*
       * 商品条形码写入缓存
       */
      JSONObject cachedCodeJSON = new JSONObject();
      cachedCodeJSON.put(URLs.kCodeInfo, codeInfo);
      cachedCodeJSON.put(URLs.kCodeType, codeType);
      cachedJSON.put("barcode", cachedCodeJSON);
      FileUtil.writeFile(cachedPath, cachedJSON.toString());

      storeID = cachedJSON.getJSONObject(URLs.kStore).getString(kId);
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    mWebView.loadUrl(loadingPath("loading"));
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
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    mContext.startActivity(intent);
  }
}
