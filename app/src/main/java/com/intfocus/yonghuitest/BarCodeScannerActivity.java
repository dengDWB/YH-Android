package com.intfocus.yonghuitest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

/**
 * Created by lijunjie on 16/6/10.
 */
public class BarCodeScannerActivity extends BaseActivity implements ZBarScannerView.ResultHandler {
    private ZBarScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
      super.onCreate(state);
      setContentView(R.layout.activity_bar_code_scanner);

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
        Intent intent = new Intent(mContext, BarCodeResultActivity.class);
        intent.putExtra("code_info", rawResult.getContents());
        intent.putExtra("code_type", rawResult.getBarcodeFormat().getName());
        mContext.startActivity(intent);
      }
    }

    /*
     * 返回
     */
    public void dismissActivity(View v) {
      BarCodeScannerActivity.this.onBackPressed();
    };
}
