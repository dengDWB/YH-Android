package com.intfocus.yonghuitest;

import android.content.Intent;
import android.widget.ImageView;

import com.intfocus.yonghuitest.view.TabView;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dengwenbin on 17/2/8.
 */
public class DashboardActivityTest extends RobolectricTest {

    private LoginActivity loginActivity;
    private DashboardActivity dashboardActivity;
    private Executor immediateExecutor;
    private TabView mTabKPI, mTabAnalyse, mTabAPP, mTabMessage;
    private ImageView mBannerSetting;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

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
        mTabKPI = (TabView) dashboardActivity.findViewById(R.id.tabKPI);
        mTabAnalyse = (TabView) dashboardActivity.findViewById(R.id.tabAnalyse);
        mTabAPP = (TabView) dashboardActivity.findViewById(R.id.tabApp);
        mTabMessage = (TabView) dashboardActivity.findViewById(R.id.tabMessage);
        mBannerSetting = (ImageView) dashboardActivity.findViewById(R.id.bannerSetting);
    }

    @Test
    public void createDashboardActivitySuccess() throws JSONException {
        immediateExecutor.execute(dashboardActivity.mRunnableForDetecting);
        immediateExecutor.execute(dashboardActivity.mHandlerForDetecting.mRunnableWithAPI);
        assertEquals("200", dashboardActivity.getdetectingResponse().get("code"));
        String code = dashboardActivity.getApiResponse().get("code");
        if (code.equals("304") || code.equals("200") && dashboardActivity.getApiResponse().toString().contains("kpi")) {
            assertTrue(true);
        }else {
            assertTrue(false);
        }
    }

    @Test
    public void clickTabLogic() throws JSONException {
        mTabAnalyse.performClick();
        immediateExecutor.execute(dashboardActivity.mRunnableForDetecting);
        immediateExecutor.execute(dashboardActivity.mHandlerForDetecting.mRunnableWithAPI);
        assertEquals("200", dashboardActivity.getdetectingResponse().get("code"));
        String code = dashboardActivity.getApiResponse().get("code");
        if ((code.equals("304") || code.equals("200")) && dashboardActivity.getApiResponse().toString().contains("analyse")) {
            assertTrue(true);
        }else {
            assertTrue(false);
        }

        mTabAPP.performClick();
        immediateExecutor.execute(dashboardActivity.mRunnableForDetecting);
        immediateExecutor.execute(dashboardActivity.mHandlerForDetecting.mRunnableWithAPI);
        assertEquals("200", dashboardActivity.getdetectingResponse().get("code"));
        code = dashboardActivity.getApiResponse().get("code");
        if ((code.equals("304") || code.equals("200")) && dashboardActivity.getApiResponse().toString().contains("app")) {
            assertTrue(true);
        }else {
            assertTrue(false);
        }

        mTabMessage.performClick();
        immediateExecutor.execute(dashboardActivity.mRunnableForDetecting);
        immediateExecutor.execute(dashboardActivity.mHandlerForDetecting.mRunnableWithAPI);
        assertEquals("200", dashboardActivity.getdetectingResponse().get("code"));
        code = dashboardActivity.getApiResponse().get("code");
        if ((code.equals("304") || code.equals("200")) && dashboardActivity.getApiResponse().toString().contains("message")) {
            assertTrue(true);
        }else {
            assertTrue(false);
        }

        mTabKPI.performClick();
        immediateExecutor.execute(dashboardActivity.mRunnableForDetecting);
        immediateExecutor.execute(dashboardActivity.mHandlerForDetecting.mRunnableWithAPI);
        assertEquals("200", dashboardActivity.getdetectingResponse().get("code"));
        code = dashboardActivity.getApiResponse().get("code");
        if ((code.equals("304") || code.equals("200")) && dashboardActivity.getApiResponse().toString().contains("kpi")) {
            assertTrue(true);
        }else {
            assertTrue(false);
        }
    }

    @Test
    public void clickSettingBtn() {
//        YHApplication mMyApp = (YHApplication)dashboardActivity.getApplication();
        mBannerSetting.performClick();
//        assertTrue(dashboardActivity.popupWindow.isShowing());
        ShadowListView listView = Shadows.shadowOf(dashboardActivity.getListView());
        listView.populateItems();

        listView.performItemClick(3);
        Intent intent  = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
        Intent barCodeScannerIntent = new Intent(dashboardActivity, BarCodeScannerActivity.class);
        assertEquals(barCodeScannerIntent.toString(), intent.toString());

//        mBannerSetting.performClick();
//        listView.performItemClick(3);
//        assertEquals("1", mMyApp.getCurrentActivity());
//        Intent intent  = Shadows.shadowOf(dashboardActivity).getNextStartedActivity();
//        Intent settingIntent = new Intent(dashboardActivity, SettingActivity.class);
//        assertEquals("1", intent.toString());

//        mBannerSetting.performClick();
//        listView.performItemClick(1);
//        assertEquals("功能开发中，敬请期待", ShadowToast.getTextOfLatestToast());
//
//        mBannerSetting.performClick();
//        listView.performItemClick(2);
//        assertEquals("功能开发中，敬请期待", ShadowToast.getTextOfLatestToast());
    }

    @After
    public void tearDown() throws Exception {

    }

}