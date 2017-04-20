package com.intfocus.yonghuitest.screen_lock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.intfocus.yonghuitest.LoginActivity;
import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.YHApplication;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lijunjie on 16/1/22.
 */
public class ConfirmPassCodeActivity extends Activity {

    private boolean is_from_login;
    private Context mContext;
    private YHApplication mMyApp;

    private StringBuilder stringBuilder;
    private TextView text_main_pass;
    private TextView text_sub_pass;
    private ImageView circle1;
    private ImageView circle2;
    private ImageView circle3;
    private ImageView circle4;

    private Bitmap bitmapBlack = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
    private Bitmap bitmapGlay = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);

    public static Intent createIntent(Context context) {
        return new Intent(context, ConfirmPassCodeActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_passcode);

        mMyApp = (YHApplication)this.getApplicationContext();
        mContext = ConfirmPassCodeActivity.this;
        stringBuilder = new StringBuilder();
        initViews();
        initCircleCanvas();

        is_from_login = getIntent().getBooleanExtra("is_from_login", false);
    }

    protected void onResume() {
        super.onResume();
            mMyApp.setCurrentActivity(this);
    }

    private void initViews() {
        text_main_pass = (TextView) findViewById(R.id.text_main_pass);
        text_sub_pass = (TextView) findViewById(R.id.text_sub_pass);

        circle1 = (ImageView) findViewById(R.id.circle1);
        circle2 = (ImageView) findViewById(R.id.circle2);
        circle3 = (ImageView) findViewById(R.id.circle3);
        circle4 = (ImageView) findViewById(R.id.circle4);

        circle1.setImageBitmap(bitmapGlay);
        circle2.setImageBitmap(bitmapGlay);
        circle3.setImageBitmap(bitmapGlay);
        circle4.setImageBitmap(bitmapGlay);
    }

    private void initCircleCanvas() {

        Canvas canvas;
        canvas = new Canvas(bitmapBlack);

        Paint paint;
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(150, 150, 148, paint);

        Canvas canvas2;
        canvas2 = new Canvas(bitmapGlay);
        Paint paint2;
        paint2 = new Paint();
        paint2.setColor(Color.parseColor("#f5f5f5"));
        paint2.setStyle(Paint.Style.FILL);
        paint2.setAntiAlias(true);
        canvas2.drawCircle(150, 150, 148, paint2);
    }

    private void initStringBuilder() {
        stringBuilder.setLength(0);
        stringBuilder.trimToSize();
    }

    private void initCircleColor() {
        circle1.setImageBitmap(bitmapGlay);
        circle2.setImageBitmap(bitmapGlay);
        circle3.setImageBitmap(bitmapGlay);
        circle4.setImageBitmap(bitmapGlay);
    }

    public void put0(View view) {
        inputPassword("0");
    }

    public void put1(View view) {
        inputPassword("1");
    }

    public void put2(View view) {
        inputPassword("2");
    }

    public void put3(View view) {
        inputPassword("3");
    }

    public void put4(View view) {
        inputPassword("4");
    }

    public void put5(View view) {
        inputPassword("5");
    }

    public void put6(View view) {
        inputPassword("6");
    }

    public void put7(View view) {
        inputPassword("7");
    }

    public void put8(View view) {
        inputPassword("8");
    }

    public void put9(View view) {
        inputPassword("9");
    }

    public void onDelete(View view) {
        int length = stringBuilder.length();
        deleteCircleColor(length);
        if (length != 0)
            stringBuilder.deleteCharAt(length - 1);
    }

    private void deleteCircleColor(int length) {
        switch (length) {
            case 1:
                circle1.setImageBitmap(bitmapGlay);
                break;
            case 2:
                circle2.setImageBitmap(bitmapGlay);
                break;
            case 3:
                circle3.setImageBitmap(bitmapGlay);
                break;
            case 4:
                circle4.setImageBitmap(bitmapGlay);
                break;
            default:
                break;
        }
    }

    private void inputPassword(String password) {
        switch (stringBuilder.length()) {
            case 0:
                circle1.setImageBitmap(bitmapBlack);
                stringBuilder.append(password);
                break;
            case 1:
                circle2.setImageBitmap(bitmapBlack);
                stringBuilder.append(password);
                break;
            case 2:
                circle3.setImageBitmap(bitmapBlack);
                stringBuilder.append(password);
                break;
            case 3:
                circle4.setImageBitmap(bitmapBlack);
                stringBuilder.append(password);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        confirmPassword();
                    }
                }, 200);
                break;
            default:
                break;
        }
    }

    private void confirmPassword() {
        try {
            Log.i("confirmPassword", "confirmPassword");

            String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
            JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);
            if (stringBuilder.toString().equals(userJSON.getString(URLs.kGesturePassword))) {
                /*
                 * 出现验证界面，是由于两种原因
                 * 1. 打开app时，之前已登录用户设置了锁屏功能;验证成功，直接跳转至主界面
                 * 2. 已打开app, 手机待机后再激活时，进入app；验证成功，不作任何处理
                 *
                 */
                if (is_from_login) {
                    Intent intent = new Intent(mContext, LoginActivity.class);
                    intent.putExtra("from_activity", this.getClass().getSimpleName());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    mContext.startActivity(intent);
                }

                new Thread(new Runnable() {
                    @Override
                    public synchronized void run() {
                        try {
                            JSONObject params = new JSONObject();
                            params.put(URLs.kAction, "解屏");
                            ApiHelper.actionLog(mContext, params);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                finish();
            } else {
                Log.i("confirmPassword", "no");

                String TEXT_MAIN_MISTAKE = "请输入密码";
                text_main_pass.setText(TEXT_MAIN_MISTAKE);
                String TEXT_SUB_MISTAKE = "密码有误";
                text_sub_pass.setText(TEXT_SUB_MISTAKE);
                initStringBuilder();
                initCircleColor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.bitmapBlack = null;
        this.bitmapGlay = null;
        this.circle1 = null;
        this.circle2 = null;
        this.circle3 = null;
        this.circle4 = null;
        this.text_main_pass = null;
        this.text_sub_pass = null;
        if (this.stringBuilder != null) {
            initStringBuilder();
            this.stringBuilder = null;
        }
    }

    /*
     * 验证界面，取消进入登录界面
     */
    public void onCancel(View view) {
        //moveTaskToBack(true);
        try {
            String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
            JSONObject userJSON = FileUtil.readConfigFile(userConfigPath);
            userJSON.put("is_login", false);
            FileUtil.writeFile(userConfigPath, userJSON.toString());

            String settingsConfigPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kSettingConfigFileName);
            FileUtil.writeFile(settingsConfigPath, userJSON.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(ConfirmPassCodeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        ConfirmPassCodeActivity.this.startActivity(intent);

        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        mMyApp.setCurrentActivity(null);
        finish();
    }
}
