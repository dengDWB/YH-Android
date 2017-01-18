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
import android.widget.Toast;

import com.intfocus.yonghuitest.R;
import com.intfocus.yonghuitest.YHApplication;
import com.intfocus.yonghuitest.util.ApiHelper;
import com.intfocus.yonghuitest.util.FileUtil;
import com.intfocus.yonghuitest.util.K;
import com.intfocus.yonghuitest.util.URLs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by lijunjie on 16/1/22.
 */
public class InitPassCodeActivity extends Activity {

    private Context mContext;
    protected YHApplication mMyApp;
    private final String TEXT_MAIN_CONFIRM = "确认密码";
    private final String TEXT_SUB_CONFIRM = "请再次输入密码";

    private byte counter = 0;
    private int password;
    private StringBuilder stringBuilder;
    private TextView text_main_pass;
    private TextView text_sub_pass;

    private ImageView circle1;
    private ImageView circle2;
    private ImageView circle3;
    private ImageView circle4;

    private Bitmap bitmapBlack = Bitmap.createBitmap(300, 300,
            Bitmap.Config.ARGB_8888);
    private Bitmap bitmapGlay = Bitmap.createBitmap(300, 300,
            Bitmap.Config.ARGB_8888);

    public static Intent createIntent(Context context) {
        return new Intent(context, InitPassCodeActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_passcode);

        mMyApp = (YHApplication)this.getApplicationContext();
        mContext = InitPassCodeActivity.this;
        stringBuilder = new StringBuilder();

        initViews();
        initCircleCanvas();
    }

    protected void onResume() {
        mMyApp.setCurrentActivity(this);
        super.onResume();
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
        if (length != 0) {
            stringBuilder.deleteCharAt(length - 1);
        }
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
                confirmPassword();
                break;
            default:
                break;
        }
    }

    private void confirmPassword() {
        // 如果确认失败了再从头再输入
        switch (counter) {
            case 0:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        counter++;
                        text_main_pass.setText(TEXT_MAIN_CONFIRM);
                        text_sub_pass.setText(TEXT_SUB_CONFIRM);
                        InitPassCodeActivity.this.password = Integer.parseInt(stringBuilder.toString());
                        initStringBuilder();
                        initCircleColor();
                    }
                }, 200);
                break;
            default:
                if (this.password == Integer.parseInt(stringBuilder.toString())) {

                    Toast.makeText(InitPassCodeActivity.this, "设置锁屏成功", Toast.LENGTH_SHORT).show();
                    // PrefUtil.setBool(getApplicationContext(), AppConfig.PREF_KEY_IS_LOCKED, true);
                    // PrefUtil.setInt(getApplicationContext(), AppConfig.PREF_KEY_PASSWORD, password);

                    try {
                        String userConfigPath = String.format("%s/%s", FileUtil.basePath(mContext), K.kUserConfigFileName);
                        JSONObject userJSON = new JSONObject();
                        if ((new File(userConfigPath)).exists()) {
                            userJSON = FileUtil.readConfigFile(userConfigPath);
                        }
                        userJSON.put(URLs.kUseGesturePassword, true);
                        userJSON.put(URLs.kGesturePassword, stringBuilder.toString());

                        FileUtil.writeFile(userConfigPath, userJSON.toString());
                        String settingsConfigPath = FileUtil.dirPath(mContext, K.kConfigDirName, K.kSettingConfigFileName);
                        FileUtil.writeFile(settingsConfigPath, userJSON.toString());

                        final JSONObject userInfo = userJSON;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ApiHelper.screenLock(userInfo.get("user_device_id").toString(), stringBuilder.toString(), true);

                                    JSONObject params = new JSONObject();
                                    params.put(URLs.kAction, "设置锁屏");
                                    ApiHelper.actionLog(mContext, params);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    finish();
                    this.onBackPressed();
                } else {
                    String TEXT_MAIN_MISTAKE = "密码有误";
                    text_main_pass.setText(TEXT_MAIN_MISTAKE);
                    String TEXT_SUB_MISTAKE = "两次密码不一致";
                    text_sub_pass.setText(TEXT_SUB_MISTAKE);
                    counter = 0;
                    password = 0;
                    initStringBuilder();
                    initCircleColor();

                    Log.i("confirmPassword2", "no");
                }
                break;
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

    public void onCancel(View view) {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
