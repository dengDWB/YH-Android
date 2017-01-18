package com.intfocus.yonghuitest;

import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by dengwenbin on 17/1/17.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml",resourceDir = "/res",assetDir = "/assets")
//@Config(constants = BuildConfig.class)
public class SettingActivityTest {
    private SettingActivity settingActivity;
    private TextView developerTv;

    @Before
    public void setUp() throws Exception {

        settingActivity = Robolectric.setupActivity(SettingActivity.class);
        developerTv = (TextView) settingActivity.findViewById(R.id.developerTv);
    }

    @Test
    public void clickDevloperTv() {
        developerTv.performClick();
        assertThat("adfa",shadowOf(settingActivity).getNextStartedActivity(), is(notNullValue()));
    }
}