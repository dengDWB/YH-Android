package com.intfocus.yh_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    setContentView(R.layout.activity_bar_code_result);

    List<ImageView> colorViews = new ArrayList<>();
    colorViews.add((ImageView) findViewById(R.id.colorView0));
    colorViews.add((ImageView) findViewById(R.id.colorView1));
    colorViews.add((ImageView) findViewById(R.id.colorView2));
    colorViews.add((ImageView) findViewById(R.id.colorView3));
    colorViews.add((ImageView) findViewById(R.id.colorView4));
    initColorView(colorViews);


    String htmlPath = FileUtil.sharedPath(mContext) + "/bar_code_scan_result.html";
    if(!(new File(htmlPath).exists())) {
      FileUtil.copyAssetFile(mContext, "bar_code_scan_result.html", htmlPath);
    }

    try {
      Intent intent = getIntent();
      String codeInfo = intent.getStringExtra("code_info");
      String codeType = intent.getStringExtra("code_type");
      ApiHelper.barCodeScan(mContext, user.getString("user_num"), codeInfo, codeType);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    mWebView.loadUrl("file:///" + htmlPath);
  }

  /*
   * 返回
   */
  public void dismissActivity(View v) {
    BarCodeResultActivity.this.onBackPressed();
  };
}
