package com.intfocus.yonghuitest;import android.os.Bundle;import android.view.View;import android.widget.TextView;import com.intfocus.yonghuitest.util.FileUtil;import com.intfocus.yonghuitest.util.K;public class DeveloperActivity extends BaseActivity {    private TextView behaviorTv,settingTv,notificationTv,barcodeTv;    @Override    protected void onCreate(Bundle savedInstanceState) {        super.onCreate(savedInstanceState);        setContentView(R.layout.activity_developer);        findViewById();        initData();    }    public void findViewById() {        behaviorTv = (TextView) findViewById(R.id.behaviorTv);        settingTv = (TextView) findViewById(R.id.settingTv);        notificationTv = (TextView) findViewById(R.id.notificationTv);        barcodeTv = (TextView) findViewById(R.id.barcodeTv);    }    public void initData() {        String pushMessagePath = String.format("%s/%s", FileUtil.basePath(this), K.kPushMessageFileName);        if (FileUtil.readFile(pushMessagePath) != null){            behaviorTv.setText(FileUtil.readFile(pushMessagePath));        }        String noticePath = FileUtil.dirPath(this, K.kConfigDirName, K.kLocalNotificationConfigFileName);        if (FileUtil.readFile(noticePath) != null){            notificationTv.setText(FileUtil.readFile(noticePath));        }        String settingsConfigPath = FileUtil.dirPath(this, K.kConfigDirName, K.kSettingConfigFileName);        if (FileUtil.readFile(settingsConfigPath) != null){            settingTv.setText(FileUtil.readFile(settingsConfigPath));        }        String javascriptPath = FileUtil.sharedPath(mAppContext) + "/BarCodeScan/assets/javascripts/bar_code_data.js";        if (FileUtil.readFile(javascriptPath) != null){            barcodeTv.setText(FileUtil.readFile(javascriptPath));        }    }    public void dismissActivity(View v) {        DeveloperActivity.this.onBackPressed();    }}