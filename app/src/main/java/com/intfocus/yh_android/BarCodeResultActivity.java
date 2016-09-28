package com.intfocus.yh_android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.K;
import com.intfocus.yh_android.util.URLs;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static android.webkit.WebView.enableSlowWholeDocumentDraw;

/**
 * Created by lijunjie on 16/6/10.
 */
public class BarCodeResultActivity extends BaseActivity {
  public final static String kId = "id";
  private String htmlContent, htmlPath, cachedPath;
  private String codeInfo, codeType, groupID, roleID, userNum;
  private String storeID;
  private ArrayList<HashMap<String, Object>> listItem = new ArrayList<>();

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    /*
     * 判断当前设备版本，5.0 以上 Android 系统使用才 enableSlowWholeDocumentDraw();
     */
    int sysVersion = Build.VERSION.SDK_INT;
    if (sysVersion > 20) {
      enableSlowWholeDocumentDraw();
    }
    setContentView(R.layout.activity_bar_code_result);

    mWebView = (WebView) findViewById(R.id.barcode_browser);
    WebSettings webSettings = mWebView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDefaultTextEncodingName("utf-8");
    webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

    initDropMenuItem();

    List<ImageView> colorViews = new ArrayList<>();
    colorViews.add((ImageView) findViewById(R.id.colorView0));
    colorViews.add((ImageView) findViewById(R.id.colorView1));
    colorViews.add((ImageView) findViewById(R.id.colorView2));
    colorViews.add((ImageView) findViewById(R.id.colorView3));
    colorViews.add((ImageView) findViewById(R.id.colorView4));
    initColorView(colorViews);

    String htmlOriginPath = String.format("%s/BarCodeScan/%s",sharedPath, K.kScanBarCodeHTMLName);
    htmlContent = FileUtil.readFile(htmlOriginPath);
    cachedPath = FileUtil.dirPath(mContext, K.kCachedDirName, K.kBarCodeResultFileName);
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

  public void actionLaunchStoreSelectorActivity(){
    Intent intent = new Intent(mContext, StoreSelectorActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    mContext.startActivity(intent);
  }

  /*
 * 初始化标题栏下拉菜单
 */
  private void initDropMenuItem() {
    String[] itemName = {"筛选","分享"};
    int[] itemImage = {R.drawable.banner_search,R.drawable.banner_share};
    for (int i = 0;i < itemName.length; i++) {
      HashMap<String, Object> map = new HashMap<String, Object>();
      map.put("ItemImage",itemImage[i]);
      map.put("ItemText", itemName[i]);
      listItem.add(map);
    }

    SimpleAdapter mSimpleAdapter = new SimpleAdapter(this, listItem, R.layout.menu_list_items, new String[]{"ItemImage", "ItemText"}, new int[]{R.id.img_menu_item, R.id.text_menu_item});
    initDropMenu(mSimpleAdapter,mDropMenuListener);
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
          actionLaunchStoreSelectorActivity();
          break;

        case "分享":
          actionShare2Weixin();
          break;

        default:
          break;
      }
    }
  };

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
   * 分享截图至微信
   */
  public void actionShare2Weixin() {
    String filePath = FileUtil.basePath(mContext) + "/" + K.kCachedDirName + "/" + "timestmap.png";

    mWebView.measure(View.MeasureSpec.makeMeasureSpec(
            View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    mWebView.setDrawingCacheEnabled(true);
    mWebView.buildDrawingCache();
    int imgMaxHight = displayMetrics.heightPixels * 3;
    if (mWebView.getMeasuredHeight() > 0) {
      toast("截图失败,请尝试系统截图!");
      return;
    }
    Bitmap imgBmp = Bitmap.createBitmap(mWebView.getMeasuredWidth(),
            mWebView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
    if (imgBmp == null) { toast("截图失败"); }
    Canvas canvas = new Canvas(imgBmp);
    Paint paint = new Paint();
    int iHeight = imgBmp.getHeight();
    canvas.drawBitmap(imgBmp, 0, iHeight, paint);
    mWebView.draw(canvas);
    FileUtil.saveImage(filePath,imgBmp);
    imgBmp.recycle(); // 回收 bitmap 资源，避免内存浪费

    File file = new File(filePath);
    if (file.exists() && file.length() > 0) {
      UMImage image = new UMImage(BarCodeResultActivity.this, file);

      new ShareAction(this)
              .withTitle("分享截图")
              .setPlatform(SHARE_MEDIA.WEIXIN)
              .setDisplayList(SHARE_MEDIA.WEIXIN)
              .setCallback(umShareListener)
              .withMedia(image)
              .open();
    }
    else {
      toast("截图失败,请尝试系统截图");
    }
  }

  private UMShareListener umShareListener  = new UMShareListener() {
    @Override
    public void onResult(SHARE_MEDIA platform) {
      Log.d("plat","platform"+platform);
    }

    @Override
    public void onError(SHARE_MEDIA platform, Throwable t) {
      Toast.makeText(BarCodeResultActivity.this,platform + " 分享失败啦", Toast.LENGTH_SHORT).show();
      if(t!=null){
        Log.d("throw","throw:"+t.getMessage());
      }
    }

    @Override
    public void onCancel(SHARE_MEDIA platform) {
      Toast.makeText(BarCodeResultActivity.this,platform + " 分享取消了", Toast.LENGTH_SHORT).show();
    }
  };

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    UMShareAPI.get( this ).onActivityResult( requestCode, resultCode, data);
  }
}
