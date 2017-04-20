package com.intfocus.yonghuitest;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.intfocus.yonghuitest.BaseActivity;
import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.PrivateURLs;
import com.intfocus.yonghuitest.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by dengwenbin on 17/1/13.
 */

public class ForgetPasswordActivity extends BaseActivity {

    public String result = "";
    public boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        mWebView = (WebView) findViewById(R.id.browser);
        initSubWebView();
        mWebView.requestFocus();
        mWebView.addJavascriptInterface(new JavaScriptInterface(), URLs.kJSInterfaceName);

        animLoading.setVisibility(View.VISIBLE);
        setWebViewLongListener(false);
        urlString = String.format(K.kForgetPwdMobilePath, PrivateURLs.kBaseUrl, URLs.currentUIVersion(mAppContext));
        new Thread(mRunnableForDetecting).start();
    }

    public void submitData(final String urlString, final JSONObject jsonParams) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> response = HttpUtil.httpPost(urlString, jsonParams);

                try {
                    JSONObject jsonResponse = new JSONObject(response.get("body").toString());
                    result = jsonResponse.getString("info");
                    if (response.get("code").equals("201")) {
                        flag = true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isSuccess(result, flag);
                    }
                });
            }
        }).start();
    }

    public void isSuccess(String info, boolean flag) {
        if (flag) {
            showAlertDialog(info);
            return;
        }

        toast(info);
    }

    public void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("温馨提示")
                .setMessage(message)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).setCancelable(false);
        builder.show();

    }

    private class JavaScriptInterface extends JavaScriptBase  {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void forgetPassword(final String userNum, final String mobile) {
            try {
                JSONObject jsonParams = new JSONObject();
                jsonParams.put("user_num", userNum);
                jsonParams.put("mobile", mobile);
                String urlString = String.format(K.kUserForgetAPIPath, K.kBaseUrl);
                submitData(urlString, jsonParams);


                /*
                 * 用户行为记录, 单独异常处理，不可影响用户体验
                 */
                try {
                    logParams = new JSONObject();
                    logParams.put(URLs.kAction, "找回密码");
                    new Thread(mRunnableForLogger).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (JSONException e1) {
                e1.toString();
            }
        }
    }

    public void dismissActivity(View view) {
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
