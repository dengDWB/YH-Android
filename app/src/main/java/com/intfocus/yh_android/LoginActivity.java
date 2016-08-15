package com.intfocus.yh_android;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.intfocus.yh_android.screen_lock.ConfirmPassCodeActivity;
import com.intfocus.yh_android.util.ApiHelper;
import com.intfocus.yh_android.util.FileUtil;
import com.intfocus.yh_android.util.URLs;

public class LoginActivity extends BaseActivity {
	private TextView textSolgan;
	private EditText textUser;
	private EditText textPassWord;
	private Button btnLogin;
	private ProgressDialog mProgressDialog;

	private String userName;
	private String passWord;
	Context mContext = this;

	@Override
	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

        /*
		 *  如果是从触屏界面过来，则直接进入主界面
         *  不是的话，相当于直接启动应用，则检测是否有设置锁屏
         */
		Intent intent = getIntent();
		if (intent.hasExtra("from_activity") && intent.getStringExtra("from_activity").equals("ConfirmPassCodeActivity")) {
			Log.i("getIndent", intent.getStringExtra("from_activity"));
			intent = new Intent(LoginActivity.this, DashboardActivity.class);
			intent.putExtra("from_activity", intent.getStringExtra("from_activity"));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			LoginActivity.this.startActivity(intent);

			finish();
		}
		else if(FileUtil.checkIsLocked(mContext)){
			intent = new Intent(this, ConfirmPassCodeActivity.class);
			intent.putExtra("is_from_login", true);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(intent);

			finish();
		}
		else {
			Log.i("info","暂无动作");
		}

		init();
	}

	private void init() {
		textSolgan = (TextView) findViewById(R.id.text_slogan);
		textUser = (EditText) findViewById(R.id.edit_user);
		textPassWord = (EditText) findViewById(R.id.edit_password);
		btnLogin = (Button) findViewById(R.id.btn_login);

        /*
         * 设置Solgan文本内容，设置字体为“冬青黑体简体中文 W3”
         * 字体存放路径为"/assets/fonts/dqhtw3.otf"
         */
		textSolgan.setText("融合共享，成于至善");
		Typeface customFont = Typeface.createFromAsset(this.getAssets(), "fonts/dqhtW3.otf");
		textSolgan.setTypeface(customFont);

		btnLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				userName = textUser.getText().toString();
				passWord = textPassWord.getText().toString();

				if (userName.isEmpty() || passWord.isEmpty()) {
					Toast.makeText(LoginActivity.this, "请输入用户名或密码", Toast.LENGTH_SHORT).show();
					return;
				}

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
						mProgressDialog = ProgressDialog.show(LoginActivity.this, "稍等", "验证用户信息...");
						return;
					}
				});

				try {
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							String info = ApiHelper.authentication(mContext, userName, URLs.MD5(passWord));
							Log.i("info", info);
							if (info.compareTo("success") > 0) {
								if (mProgressDialog != null) mProgressDialog.dismiss();
								Toast.makeText(LoginActivity.this, info, Toast.LENGTH_SHORT).show();
								return;
							}

							Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);

							if (mProgressDialog != null) {
								mProgressDialog.dismiss();
							}
							finish();
						}
					});

					thread.start();
				} catch (Exception e) {
					e.printStackTrace();
					if (mProgressDialog != null) {
						mProgressDialog.dismiss();
					}
					Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	protected void onResume() {
		mMyApp.setCurrentActivity(this);
		if (mProgressDialog != null) mProgressDialog.dismiss();
		super.onResume();
	}

	protected void onDestroy() {
		mContext = null;
		mWebView = null;
		user = null;
		super.onDestroy();
	}
}
