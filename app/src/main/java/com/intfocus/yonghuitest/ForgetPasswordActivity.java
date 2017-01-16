package com.intfocus.yonghuitest;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.intfocus.yonghuitest.util.HttpUtil;
import com.intfocus.yonghuitest.util.K;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by dengwenbin on 17/1/13.
 */

public class ForgetPasswordActivity extends BaseActivity {

    public EditText idEt,iphoneEt;
    public TextView tipsTv;
    public String result = "", urlString = "";
    public boolean flag = false;
    public JSONObject jsonParams;
    public String userID = "", iphone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        // 组件初始化与监听
        viewInitAndListener();
    }

    public void viewInitAndListener() {
        idEt = (EditText) findViewById(R.id.etUserId);
        iphoneEt = (EditText) findViewById(R.id.etIphone);
        tipsTv = (TextView) findViewById(R.id.tipsTv);

        idEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                setUserID(s.toString());
            }
        });
        iphoneEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                setIphone(s.toString());
            }
        });

        findViewById(R.id.btn_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tipsTv.setText("");

                // 检测任意输入是否为空
                if (!checkInput(getUserID(), getIphone())) {
                    return;
                }

                //配置 Post 需要的信息
                postInfoConfigure(getUserID(), getIphone());

                //提交数据到服务器端
                submitData();
            }
        });
    }

    public boolean checkInput(String userNum, String mobile) {
        if (userNum.equals("")) {
            toast("用户编号不能为空");
            return false;
        }
        if (mobile.equals("")) {
            toast("联系方式不能为空");
            return false;
        }
        return true;
    }

    public void postInfoConfigure(String userNum, String mobile) {
        try {
            urlString = String.format(K.kUserForgetAPIPath, K.kBaseUrl);
            jsonParams = new JSONObject();
            jsonParams.put("user_num", userNum);
            jsonParams.put("mobile", mobile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void submitData() {

        tipsTv.setTextColor(Color.BLACK);
        tipsTv.setText("正在提交,请稍后。。。");

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
                        setTipsTv(result, flag);
                    }
                });
            }
        }).start();
    }

    public void setTipsTv(String info, boolean flag) {
        if (flag) {
            showAlertDialog(info);
            tipsTv.setText("");
            return;
        }

        tipsTv.setTextColor(Color.RED);
        tipsTv.setText(info);
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

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserID() {
        return userID;
    }

    public void setIphone(String iphone) {
        this.iphone = iphone;
    }

    public String getIphone() {
        return iphone;
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
