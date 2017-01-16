package com.intfocus.yonghuitest;

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

/**
 * Created by dengwenbin on 17/1/16.
 */

/* 1.键盘点击是否成功
 * 2.用户名密码输入错误提示
 * 3.验证用户名密码正确与错误是否登录成功
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class LoginActivityTest {
    private EditText userNameEt;
    private EditText pwdEt;
    private Button loginBtn;

    @Before
    public void setUp() {
        Activity loginActivity = Robolectric.setupActivity(LoginActivity.class);
        userNameEt = (EditText) loginActivity.findViewById(R.id.etUsername);
        pwdEt = (EditText) loginActivity.findViewById(R.id.etPassword);
        loginBtn = (Button) loginActivity.findViewById(R.id.btn_login);
    }

    @Test
    public void testOnclickLoginBtn() {
        userNameEt.setText("admin");
        pwdEt.setText("yh123");
        loginBtn.performClick();
        ShadowApplication application = ShadowApplication.getInstance();
        assertThat("Next activity has started", application.getNextStartedActivity(), is(notNullValue()));
    }

    @Test
    public void ClickLoginBtnFail() {
        userNameEt.setText("admin");
        pwdEt.setText("123");
        loginBtn.performClick();
        ShadowApplication application = ShadowApplication.getInstance();
        assertThat("Next activity has started", application.getNextStartedActivity(), is(nullValue()));
    }

}