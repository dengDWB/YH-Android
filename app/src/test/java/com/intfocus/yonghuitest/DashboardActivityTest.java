package com.intfocus.yonghuitest;

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.concurrent.Executor;

import static com.intfocus.yonghuitest.util.URLs.kGroupId;
import static org.junit.Assert.*;

/**
 * Created by dengwenbin on 17/2/8.
 */
public class DashboardActivityTest extends RobolectricTest {

    private LoginActivity loginActivity;
    private DashboardActivity dashboardActivity;
    private Executor immediateExecutor;

    @Before
    public void setUp() throws Exception {
        loginActivity = Robolectric.setupActivity(LoginActivity.class);
        immediateExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        loginActivity.setRunnable("admin","yh123");
        immediateExecutor.execute(loginActivity.getRunnable());
        dashboardActivity = Robolectric.setupActivity(DashboardActivity.class);
    }

    @Test
    public void createDashboardActivitySuccess() throws JSONException {
        assertEquals("200", dashboardActivity.getdetectingResponse().get("code"));
//        assertNull(dashboardActivity.getApiResponse());
//        assertEquals("200", dashboardActivity.getApiResponse().toString());
        assertEquals("DashboardActivity", dashboardActivity.getYHApplication().getCurrentActivity());
    }

    @Test
    public void clickTabLogic() throws JSONException {

    }

    @After
    public void tearDown() throws Exception {

    }

}