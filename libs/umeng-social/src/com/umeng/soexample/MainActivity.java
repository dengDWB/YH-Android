package com.umeng.soexample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.umeng.socialize.Config;
import com.umeng.socialize.utils.Log;
import com.umeng.soexample.share_auth.ShareandAuthActivity;
import com.umeng.soexample.R;



/**
 * Created by umeng on 15/9/14.
 */
public class MainActivity extends Activity {
    private Button shareButton, shakebutton, commentButton;
    private static final int REQUEST_PERM = 151;


    
    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.umeng_share) {
                Intent intent = new Intent(MainActivity.this, ShareandAuthActivity.class);
                startActivity(intent);
            } else if (view.getId() == R.id.umeng_shake) {
                Toast.makeText(MainActivity.this, "This function will come soon", Toast.LENGTH_LONG).show();
//                Intent intent = new Intent(MainActivity.this, ShakeActivity.class);
//                startActivity(intent);
            }

        else if(view.getId()==R.id.umeng_comment)
        {
            Toast.makeText(MainActivity.this, "This function will come soon", Toast.LENGTH_LONG).show();
//                Intent intent = new Intent(MainActivity.this, NetCenterActivity.class);
//                startActivity(intent);
        }
    }
};

    protected void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.app_main);
            Config.dialogSwitch=true;
//          如想让你的app在android 6.0系统上也能运行的话，需要动态获取权限，没有权限的话分享sdk会出错，参考一下代码做动态获取权限,适配安卓6.0系统
//          你需要最新的android.support.v4包，或者v13的包可也以
//            if(Build.VERSION.SDK_INT>=23){
//                String[] mPermissionList = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CALL_PHONE,Manifest.permission.READ_LOGS,Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.SET_DEBUG_APP,Manifest.permission.SYSTEM_ALERT_WINDOW,Manifest.permission.GET_ACCOUNTS,Manifest.permission.WRITE_APN_SETTINGS};
//                ActivityCompat.requestPermissions(this,mPermissionList,REQUEST_PERM);
//            }
            shakebutton=(Button)findViewById(R.id.umeng_shake);
            shareButton=(Button)findViewById(R.id.umeng_share);
            commentButton=(Button)findViewById(R.id.umeng_comment);
            shakebutton.setOnClickListener(clickListener);
            shareButton.setOnClickListener(clickListener);
            commentButton.setOnClickListener(clickListener);
        //

    }

}


