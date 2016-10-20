package com.intfocus.yonghuitest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.intfocus.yonghuitest.screen_lock.ConfirmPassCodeActivity;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;
import com.pgyersdk.update.PgyUpdateManager;
import org.json.JSONObject;

public class LoginActivity extends BaseActivity {
    public final static String kFromActivity = "from_activity";
    public final static String kSuccess      = "success";
    private EditText usernameEditText, passwordEditText;
    private String usernameString, passwordString;
    private TextView versionTv;
    private final static int CODE_AUTHORITY_REQUEST = 0;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /*
         *  如果是从触屏界面过来，则直接进入主界面如
         *  不是的话，相当于直接启动应用，则检测是否有设置锁屏
         */
        Intent intent = getIntent();
        if (intent.hasExtra(kFromActivity) && intent.getStringExtra(kFromActivity).equals("ConfirmPassCodeActivity")) {
            Log.i("getIndent", intent.getStringExtra(kFromActivity));
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
            intent.putExtra(kFromActivity, intent.getStringExtra(kFromActivity));
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            LoginActivity.this.startActivity(intent);

            finish();
        }
        else if (FileUtil.checkIsLocked(mContext)) {
            intent = new Intent(this, ConfirmPassCodeActivity.class);
            intent.putExtra("is_from_login", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);

            finish();
        }
        else {
            /*
             *  检测版本更新
             *    1. 与锁屏界面互斥；取消解屏时，返回登录界面，则不再检测版本更新；
             *    2. 原因：如果解屏成功，直接进入MainActivity,会在BaseActivity#finishLoginActivityWhenInMainAcitivty中结束LoginActivity,若此时有AlertDialog，会报错误:Activity has leaked window com.android.internal.policy.impl.PhoneWindow$DecorView@44f72ff0 that was originally added here
             */
            checkPgyerVersionUpgrade(LoginActivity.this,false);
        }

        usernameEditText = (EditText) findViewById(R.id.etUsername);
        passwordEditText = (EditText) findViewById(R.id.etPassword);
        versionTv = (TextView) findViewById(R.id.versionTv);
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionInfo = String.format("a%s(%d)", packageInfo.versionName, packageInfo.versionCode);
            versionTv.setText(versionInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        /*
         * 检测登录界面，版本是否升级
         */
        checkVersionUpgrade(assetsPath);
    }

    private void getAuthority() {
            int writePermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(writePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(LoginActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_PHONE_STATE},CODE_AUTHORITY_REQUEST);
                return;
            }else{
            return;
        }
    }

    /*
     * 权限获取反馈
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CODE_AUTHORITY_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    break;
                } else {
                    Toast.makeText(LoginActivity.this, "文件权限获取失败，可能影响使用哦", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void onResume() {
        mMyApp.setCurrentActivity(this);
        if(mProgressDialog != null)  {
            mProgressDialog.dismiss();
        }
        getAuthority();
        super.onResume();
    }

    protected void onDestroy() {
        mContext = null;
        mWebView = null;
        user = null;
        PgyUpdateManager.unregister(); // 解除注册蒲公英版本更新检查
        super.onDestroy();
    }

    public void actionSubmit(View v) {
        try {
            usernameString = usernameEditText.getText().toString();
            passwordString = passwordEditText.getText().toString();
            if (usernameString.isEmpty() || passwordString.isEmpty()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toast("请输入用户名与密码");
                    }
                });

                return;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog = ProgressDialog.show(LoginActivity.this, "稍等", "验证用户信息...");
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String info = ApiHelper.authentication(mContext, usernameString, URLs.MD5(passwordString));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (info.compareTo(kSuccess) > 0 || info.compareTo(kSuccess) < 0) {
                                if (mProgressDialog != null) {
                                    mProgressDialog.dismiss();
                                }
                                toast(info);
                                return;
                            }

                            // 检测用户空间，版本是否升级
                            assetsPath = FileUtil.dirPath(mContext, K.kHTMLDirName);
                            checkVersionUpgrade(assetsPath);

                            // 跳转至主界面
                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            LoginActivity.this.startActivity(intent);

                            /*
                             * 用户行为记录, 单独异常处理，不可影响用户体验
                             */
                            try {
                                logParams = new JSONObject();
                                logParams.put("action", "登录");
                                new Thread(mRunnableForLogger).start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (mProgressDialog != null) {
                                mProgressDialog.dismiss();
                            }

                            finish();
                        }
                    });
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            if (mProgressDialog != null) mProgressDialog.dismiss();
            toast(e.getLocalizedMessage());
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
