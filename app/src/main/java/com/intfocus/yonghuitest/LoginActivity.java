package com.intfocus.yonghuitest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.intfocus.yonghuitest.dashboard.DashboardActivity;
import com.intfocus.yonghuitest.screen_lock.ConfirmPassCodeActivity;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;
import com.pgyersdk.update.PgyUpdateManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends BaseActivity{
    public  String kFromActivity = "from_activity";         // APP 启动标识
    public  String kSuccess      = "success";               // 用户登录验证结果
    private EditText usernameEditText, passwordEditText;
    private String usernameString, passwordString;
    private final static int CODE_AUTHORITY_REQUEST = 0;
    private static final String[] permissionsArray = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA };

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 使背景填满整个屏幕,包括状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        /*
         *  如果是从触屏界面过来，则直接进入主界面如
         *  不是的话，相当于直接启动应用，则检测是否有设置锁屏
         */
        Intent intent = getIntent();
        if (intent.hasExtra(kFromActivity) && intent.getStringExtra(kFromActivity).equals("ConfirmPassCodeActivity")) {
            intent = new Intent(LoginActivity.this, DashboardActivity.class);
            intent.putExtra(kFromActivity, intent.getStringExtra(kFromActivity));
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            LoginActivity.this.startActivity(intent);

            finish();
        }
        else if (FileUtil.checkIsLocked(mAppContext)) {
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
        TextView versionTv = (TextView) findViewById(R.id.versionTv);
        findViewById(R.id.forgetPasswordTv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent (LoginActivity.this, ForgetPasswordActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.applyRegistTv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("温馨提示")
                        .setMessage("请到数据化运营平台申请开通账号")
                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
        });

        /*
         * 显示记住用户名称
         */
        try {
            if (user.has(URLs.kUserNum)) {
                usernameEditText.setText(user.getString(URLs.kUserNum));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
         * 显示当前应用版本号
         */
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionInfo = String.format("a%s(%d)", packageInfo.versionName, packageInfo.versionCode);
            versionTv.setText(versionInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        /*
         *  当用户系统不在我们支持范围内时,发出警告。
         */
        if (Build.VERSION.SDK_INT > K.kMaxSdkVersion || Build.VERSION.SDK_INT < K.kMinSdkVersion) {
            showVersionWarring();
        }

        RelativeLayout loginLayout = (RelativeLayout) findViewById(R.id.login_layout);
        Button mSubmit = (Button) findViewById(R.id.btn_login);
        controlKeyboardLayout(loginLayout, mSubmit);

        /*
         * 检测登录界面，版本是否升级
         */
        checkVersionUpgrade(assetsPath);
        getAuthority();
    }

    protected void onResume() {
        mMyApp.setCurrentActivity(this);
        if(mProgressDialog != null)  {
            mProgressDialog.dismiss();
        }
        super.onResume();
    }

    protected void onDestroy() {
        mWebView = null;
        user = null;
        PgyUpdateManager.unregister();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        mMyApp.setCurrentActivity(null);
        finish();
        System.exit(0);
    }

    /*
     * 系统版本警告
     */
    private void showVersionWarring() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("温馨提示")
                .setMessage(String.format("本应用不支持当前系统版本【Android %s】,强制使用可能会出现异常喔,给您带来的不便深表歉意,我们会尽快适配的!", Build.VERSION.RELEASE))
                .setPositiveButton("退出应用", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mMyApp.setCurrentActivity(null);
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton("继续运行", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 返回 LoginActivity
                    }
                });
        builder.show();
    }

    /*
     * 获取权限 : 文件读写 (WRITE_EXTERNAL_STORAGE),读取设备信息 (READ_PHONE_STATE)
     */
    private void getAuthority() {
        List<String> permissionsList = new ArrayList<>();
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(LoginActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        if (!permissionsList.isEmpty() && permissionsList != null){
            ActivityCompat.requestPermissions(LoginActivity.this, permissionsList.toArray(new String[permissionsList.size()]), CODE_AUTHORITY_REQUEST);
        }
    }

    /*
     * 权限获取反馈
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case CODE_AUTHORITY_REQUEST:
                boolean flag = false;
                if (grantResults.length > 0){
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        } else {
                            flag = true;
                        }
                    }
                }

                if (flag) {
                    setAlertDialog(LoginActivity.this, "某些权限获取失败，是否到本应用的设置界面设置权限");
                }

                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*
     * 键盘弹出监听,使用键盘时,整体布局上移
     */
    private void controlKeyboardLayout(final View view, final View scrollToView) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        Rect rect = new Rect();
                        view.getWindowVisibleDisplayFrame(rect);// 获取完整布局在窗体的可视区域
                        int rootInvisibleHeight = view.getRootView().getHeight() - rect.bottom; //完整布局高度 减去 可视区域高度
                        if (rootInvisibleHeight > 0) {
                            //软键盘弹出来的时候
                            int[] location = new int[2];
                            // 获取 scrollToView 在窗体的坐标
                            scrollToView.getLocationInWindow(location);
                            // 计算完整布局滚动高度，使 scrollToView 在可见区域的底部
                            int srollHeight = (location[1] + scrollToView.getHeight()) - rect.bottom;
                            view.scrollTo(0, srollHeight + 20);
                        } else {
                            // 软键盘没有弹出来的时候
                            view.scrollTo(0, 0);
                        }
                    }
                });
    }

    /*
     * 登录按钮点击事件
     */
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
                    final String info = ApiHelper.authentication(mAppContext, usernameString, URLs.MD5(passwordString));
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
                            assetsPath = FileUtil.dirPath(mAppContext, K.kHTMLDirName);
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
}
