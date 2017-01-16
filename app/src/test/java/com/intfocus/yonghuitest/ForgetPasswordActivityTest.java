package com.intfocus.yonghuitest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.*;

/**
 * Created by dengwenbin on 17/1/16.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class ForgetPasswordActivityTest {

    @Test
    public void checkInput() throws Exception {
        ForgetPasswordActivity fpActivity = Robolectric.setupActivity(ForgetPasswordActivity.class);
        fpActivity.setUserID("1");
        fpActivity.setIphone("");
        fpActivity.findViewById(R.id.btn_submit).performClick();

        Toast toast = ShadowToast.getLatestToast();
        assertNotNull(toast);

        assertEquals(ShadowToast.getTextOfLatestToast(),"联系方式不能为空");

    }

}