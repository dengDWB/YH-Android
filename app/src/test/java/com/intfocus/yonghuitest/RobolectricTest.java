package com.intfocus.yonghuitest;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by dengwenbin on 17/1/20.
 */

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(RobolectricGradleTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", resourceDir = "/res", assetDir = "/assets",
        constants = BuildConfig.class, sdk = 21)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})

public class RobolectricTest {
}
