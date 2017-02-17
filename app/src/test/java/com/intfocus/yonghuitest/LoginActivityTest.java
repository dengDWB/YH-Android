package com.intfocus.yonghuitest;

import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import com.intfocus.yonghuitest.screen_lock.ConfirmPassCodeActivity;
import com.intfocus.yonghuitest.util.FileUtil;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by dengwenbin on 17/1/16.
 */

/* 1.键盘点击是否成功
 * 2.用户名密码输入错误提示
 * 3.验证用户名密码正确与错误是否登录成功
 */

//@RunWith(PowerMockRunner.class)
//@Config(manifest = "src/main/AndroidManifest.xml",resourceDir = "/res",assetDir = "/assets")
//@PowerMockIgnore({"org.mockito.*","org.robolectric.*","android.*" })
@PrepareForTest(FileUtil.class)

public class LoginActivityTest extends RobolectricTest{
    private EditText userNameEt;
    private EditText pwdEt;
    private Button loginBtn;
    private LoginActivity loginActivity;
    private Executor immediateExecutor;
    @Mock FileUtil fileUtil;

    @Before
    public void setUp() {
        loginActivity = Robolectric.setupActivity(LoginActivity.class);
        userNameEt = (EditText) loginActivity.findViewById(R.id.etUsername);
        pwdEt = (EditText) loginActivity.findViewById(R.id.etPassword);
        loginBtn = (Button) loginActivity.findViewById(R.id.btn_login);
        immediateExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    @Test
    public void clickLoginBtnSuccess() {
        loginActivity.setRunnable("admin","yh123");
        immediateExecutor.execute(loginActivity.getRunnable());
        Intent intent = new Intent(loginActivity, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent actualIntent = shadowOf(loginActivity).getNextStartedActivity();
        assertEquals(intent.toString(), actualIntent.toString());
    }

    @Test
    public void clickLoginBtnFail() {
        loginActivity.setRunnable("admin","123");
        immediateExecutor.execute(loginActivity.getRunnable());
        assertEquals("密码错误，找平台帮忙初始化密码",loginActivity.getInfo());

        loginActivity.setRunnable("admi","123");
        immediateExecutor.execute(loginActivity.getRunnable());
        assertEquals("用户不存在，找平台申请开通账号",loginActivity.getInfo());
    }

    @Test
    public void clickLoginShowToast() {
        userNameEt.setText("");
        pwdEt.setText("yh123");
        loginBtn.performClick();
        assertEquals("请输入用户名与密码", ShadowToast.getTextOfLatestToast());

        userNameEt.setText("admin");
        pwdEt.setText("");
        loginBtn.performClick();
        assertEquals("请输入用户名与密码", ShadowToast.getTextOfLatestToast());

        userNameEt.setText("");
        pwdEt.setText("");
        loginBtn.performClick();
        assertEquals("请输入用户名与密码", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void formConfirmPassCodeActivity() {
        Intent intent = new Intent();
        intent.putExtra("from_activity","ConfirmPassCodeActivity");
        loginActivity = Robolectric.buildActivity(LoginActivity.class).withIntent(intent).create().get();
        intent = Shadows.shadowOf(loginActivity).getNextStartedActivity();
        Intent mockIntent = new Intent(loginActivity, DashboardActivity.class);
        mockIntent.putExtra("from_activity","null");
        mockIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        assertEquals(mockIntent.toString(), intent.toString());
    }

    @Ignore
    public void checkIsLocked() {
//        mockStatic(FileUtil.class);
        fileUtil = mock(FileUtil.class);
        LoginActivity loginActivity1 = Mockito.mock(LoginActivity.class);
//        File mockFile = mock(File.class);
//        when(mockFile.exists()).thenReturn(true);
//        ShadowApplication apllication = ShadowApplication.getInstance();
//        JSONObject mockJson = mock(JSONObject.class);
//        try {
//            when(mockJson.getBoolean("is_login")).thenReturn(true);
//            when(mockJson.getBoolean("use_gesture_password")).thenReturn(true);
//            when(mockJson.getString("gesture_password")).thenReturn("123");
//            Mockito.verify(mockJson).getBoolean("is_login");
//            assertEquals(true,FileUtil.checkIsLocked(loginActivity));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        when(fileUtil.checkIsLocked(any(Context.class))).thenReturn(true);
        loginActivity = Robolectric.setupActivity(LoginActivity.class);
        Intent intent = new Intent(loginActivity, ConfirmPassCodeActivity.class);
        intent.putExtra("is_from_login", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        assertEquals(Shadows.shadowOf(loginActivity).getNextStartedActivity().toString(), intent.toString());

    }
}