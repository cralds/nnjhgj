package com.demo.demo24;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	private Button btn;
	String fileName = "qq聊天软件";
	String url = "http://221.236.21.155/down.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk?mkey=543bb6c0665c5c83&f=d087&p=.apk";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initView();
		initDate();
	}

	private void initDate() {
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						DownloadActivity.class);
				intent.putExtra("fileName", fileName);
				intent.putExtra("url", url);
				startActivity(intent);

			}
		});
	}

	private void initView() {
		btn = (Button) findViewById(R.id.btn);
	}

}
