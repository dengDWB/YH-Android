package com.intfocus.yonghuitest;


import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;

import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by dengwenbin on 17/1/4.
 */
public class LoginActivityTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<>(
            LoginActivity.class);

    @Test
    public void test_Login () {
        onView(withId(R.id.etUsername)).perform(typeText("admin"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("yh123"), closeSoftKeyboard());
        onView(withId(R.id.btn_login)).perform(click());
    }
}