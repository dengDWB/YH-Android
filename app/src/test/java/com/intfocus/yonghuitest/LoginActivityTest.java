package com.intfocus.yonghuitest;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Button;
import android.widget.EditText;

import com.intfocus.yonghuitest.util.FileUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

import java.util.concurrent.Executor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by dengwenbin on 17/1/16.
 */

/* 1.键盘点击是否成功
 * 2.用户名密码输入错误提示
 * 3.验证用户名密码正确与错误是否登录成功
 */

@RunWith(RobolectricTestRunner.class)
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
        mockIntent.putExtra("from_activity","ConfirmPassCodeActivity");
        mockIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        assertEquals(mockIntent.toString(), Shadows.shadowOf(loginActivity).getNextStartedActivity().toString());
    }

    @Test
    public void checkIsLocked() {
        mockStatic(FileUtil.class);
        fileUtil = mock(FileUtil.class);
        Context context = Mockito.mock(Context.class);
//        ShadowApplication apllication = ShadowApplication.getInstance();
//        when(fileUtil.checkIsLocked(context)).thenReturn(true);
//        doReturn(true).when(fileUtil.checkIsLocked(context));
        assertEquals(false,fileUtil.checkIsLocked(context));

    }
}