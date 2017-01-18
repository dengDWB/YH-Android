package com.intfocus.yonghuitest;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivityThread;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import java.util.concurrent.Executor;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by dengwenbin on 17/1/16.
 */

/* 1.键盘点击是否成功
 * 2.用户名密码输入错误提示
 * 3.验证用户名密码正确与错误是否登录成功
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")
//@Config(constants = BuildConfig.class)
public class LoginActivityTest {
    private EditText userNameEt;
    private EditText pwdEt;
    private Button loginBtn;
    private LoginActivity loginActivity;

    @Before
    public void setUp() {
        loginActivity = Robolectric.setupActivity(LoginActivity.class);
        userNameEt = (EditText) loginActivity.findViewById(R.id.etUsername);
        pwdEt = (EditText) loginActivity.findViewById(R.id.etPassword);
        loginBtn = (Button) loginActivity.findViewById(R.id.btn_login);
    }

    @Test
    public void clickLoginBtnSuccess() {
//        userNameEt.setText("admin");
//        pwdEt.setText("yh123");
        loginActivity.setRunnable("admin","yh123");
        Executor immediateExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        immediateExecutor.execute(loginActivity.getRunnable());

//        loginActivity.onResume();
//        assertNotNull(pwdEt);
//        assertNotNull(userNameEt);
//        assertNotNull(loginBtn);
//        assertEquals("admin",userNameEt.getText().toString());
//        assertEquals("yh123",pwdEt.getText().toString());
//        Intent intent = new Intent(loginActivity, DashboardActivity.class);
//        ShadowApplication application = ShadowApplication.getInstance();
        Intent actualIntent = shadowOf(loginActivity).getNextStartedActivity();;
        assertNull(actualIntent);
////        assertEquals(intent, actualIntent);
////        assertNotNull(intent);
////        assertThat("Next activity has started", application.getNextStartedActivity(), is(nullValue()));
    }

//    @Test
//    public void clickLoginBtnFail() {
//        userNameEt.setText("admin");
//        pwdEt.setText("yh123");
//        loginBtn.performClick();
////        ShadowApplication application = ShadowApplication.getInstance();
//        ShadowActivity activity = shadowOf(loginActivity);
//        assertNotNull(activity.getNextStartedActivity());
////        assertThat("Next activity has started", application.getNextStartedActivity(), is(notNullValue()));
//
//    }

//    @Test
//    public void clickingLogin_shouldStartLoginActivity() {
//        userNameEt.setText("admin");
//        pwdEt.setText("yh123");
//        loginBtn.performClick();
//        Robolectric.flushBackgroundThreadScheduler();
//        Robolectric.flushBackgroundThreadScheduler();
//        Robolectric.flushForegroundThreadScheduler();
//        assertEquals("123",Robolectric.getBackgroundThreadScheduler().toString());
//        assertNotNull(shadowOf(loginActivity).getNextStartedActivity());

//        ShadowActivity toast = shadowOf(loginActivity);
//        assertNotNull(toast.getNextStartedActivity());
//        ShadowApplication application = ShadowApplication.getInstance();
//        assertThat(loginActivity.toString(), is(notNullValue()));
//        assertNotNull(toast.getNextStartedActivity());
//        assertEquals(loginActivity.actionSubmit(loginBtn), "123");
//        assertEquals("123", toast.getTextOfLatestToast());
//    }

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

//    @Test
//    public void verifySuccess() {
//        LoginActivity mocklLoginActivity = Mockito.mock(LoginActivity.class);
//        mocklLoginActivity.postData("admin","yh123");
//        assertEquals("123",mocklLoginActivity.getInfo());
//    }

}