package com.intfocus.yonghuitest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by lijunjie on 16/1/18.
 */
public class ResetPasswordActivity extends BaseActivity {

    private int loadCount = 0;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        mMyApp.setCurrentActivity(this);

        mWebView = (WebView) findViewById(R.id.browser);
        initSubWebView();

        mWebView.requestFocus();
        mWebView.addJavascriptInterface(new JavaScriptInterface(), URLs.kJSInterfaceName);
        mWebView.loadUrl(urlStringForLoading);

        urlString = String.format(K.kResetPwdMobilePath, K.kBaseUrl, URLs.currentUIVersion(mAppContext));
        new Thread(mRunnableForDetecting).start();
    }

    protected void onResume() {
        mMyApp.setCurrentActivity(this);
        super.onResume();
    }

    /*
     * 返回
     */
    public void dismissActivity(View v) {
        ResetPasswordActivity.this.onBackPressed();
    }

    private class JavaScriptInterface extends JavaScriptBase  {
        /*
         * JS 接口，暴露给JS的方法使用@JavascriptInterface装饰
         */
        @JavascriptInterface
        public void resetPassword(final String oldPassword, final String newPassword) {
            try {
                if (URLs.MD5(oldPassword).equals(user.get(URLs.kPassword))) {
                    Map<String, String> response = ApiHelper.resetPassword(user.get("user_id").toString(), URLs.MD5(newPassword));

                    JSONObject responseInfo = new JSONObject(response.get(URLs.kBody));

                    Builder alertDialog = new AlertDialog.Builder(ResetPasswordActivity.this);
                    alertDialog.setTitle("温馨提示");
                    alertDialog.setMessage(responseInfo.getString("info"));

                    if (response.get(URLs.kCode).equals("200") || response.get(URLs.kCode).equals("201")) {
                        alertDialog.setPositiveButton(
                                "重新登录",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        modifiedUserConfig(false);

                                        Intent intent = new Intent();
                                        intent.setClass(ResetPasswordActivity.this, LoginActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }
                                }
                        );
                        alertDialog.show();

                        /*
                         * 用户行为记录, 单独异常处理，不可影响用户体验
                         */
                        try {
                            logParams = new JSONObject();
                            logParams.put(URLs.kAction, "重置密码");
                            new Thread(mRunnableForLogger).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        alertDialog.setNegativeButton(
                                "好的",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }
                        );
                        alertDialog.show();
                    }

                } else {
                    Toast.makeText(ResetPasswordActivity.this, "原始密码输入有误", Toast.LENGTH_SHORT).show();
                    new Thread(mRunnableForDetecting).start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(ResetPasswordActivity.this, "请退出，重新登录，再尝试", Toast.LENGTH_SHORT).show();
            }

        }

        @JavascriptInterface
        public void jsException(final String ex) {
            /*
             * 用户行为记录, 单独异常处理，不可影响用户体验
             */
            try {
                logParams = new JSONObject();
                logParams.put(URLs.kAction, "JS异常");
                logParams.put(URLs.kObjTitle, String.format("重置密码页面/%s", ex));
                new Thread(mRunnableForLogger).start();

                //点击两次还是有异常 异常报出
                if (loadCount < 2) {
                    showWebViewExceptionForWithoutNetwork();
                    loadCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
