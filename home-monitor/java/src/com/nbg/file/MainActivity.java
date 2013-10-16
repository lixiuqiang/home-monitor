package com.nbg.file;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class MainActivity extends Activity implements OnClickListener {
	private Button start_button1;
	private Button end_button1;
	private Button start_button2;
	private Button end_button2;
	private EditText dirText;
	private EditText urlText1;
	private EditText urlText2;
	private CheckBox checkBox1;
	private CheckBox checkBox2;
	private EditText mbText1;
	private EditText mbText2;
	private SharedPreferences config_1;
	private SharedPreferences config_2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("===============================on create");
		setContentView(R.layout.activity_main);
		dirText = (EditText) findViewById(R.id.dirText);
		start_button1 = (Button) findViewById(R.id.start_button1);
		end_button1 = (Button) findViewById(R.id.end_button1);
		start_button2 = (Button) findViewById(R.id.start_button2);
		end_button2 = (Button) findViewById(R.id.end_button2);
		urlText1 = (EditText) findViewById(R.id.urlText1);
		urlText2 = (EditText) findViewById(R.id.urlText2);
		mbText1 = (EditText) findViewById(R.id.mbText1);
		mbText2 = (EditText) findViewById(R.id.mbText2);
		checkBox1 = (CheckBox) findViewById(R.id.checkBox1);
		checkBox2 = (CheckBox) findViewById(R.id.checkBox2);
		start_button1.setOnClickListener(this);
		end_button1.setOnClickListener(this);
		start_button2.setOnClickListener(this);
		end_button2.setOnClickListener(this);
		
		
		config_1 = getSharedPreferences("config_1", Context.MODE_PRIVATE);
		dirText.setText(config_1.getString("dir", "/mnt/sda1/HOME/"));
		mbText1.setText(config_1.getString("mb", "1024"));
		checkBox1.setChecked(config_1.getBoolean("check", true));
		urlText1.setText(config_1.getString("url",
				"rtsp://xiaoni:dugudao3721@192.168.168.7:7001/mpeg4"));

		config_2 = getSharedPreferences("config_2", Context.MODE_PRIVATE);
		mbText2.setText(config_2.getString("mb", "1024"));
		urlText2.setText(config_2.getString("url",
				"rtsp://xiaoni:dugudao3721@192.168.168.8:8001/mpeg4"));
		checkBox2.setChecked(config_2.getBoolean("check", true));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v == start_button2 || v == end_button2) {
			Editor editor_2 = config_2.edit();
			editor_2.putString("dir", dirText.getText().toString());
			editor_2.putString("url", urlText2.getText().toString());
			editor_2.putBoolean("check", checkBox2.isChecked());
			editor_2.putString("mb", mbText2.getText().toString());
			editor_2.commit();
			handler(v == start_button2, MonitorService2.class);
		} else if (v == start_button1 || v == end_button1) {
			Editor editor_1 = config_1.edit();
			editor_1.putString("dir", dirText.getText().toString());
			editor_1.putString("url", urlText1.getText().toString());
			editor_1.putBoolean("check", checkBox1.isChecked());
			editor_1.putString("mb", mbText1.getText().toString());
			editor_1.commit();
			handler(v == start_button1, MonitorService1.class);
		}
	}

	public void handler(boolean start, Class<?> c) {
		Context context = getApplicationContext();
		Intent serviceIntent = new Intent(context, c);
		PendingIntent mAlarmSender = PendingIntent.getService(context, 0,
				serviceIntent, 0);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Activity.ALARM_SERVICE);
		if (start) {
			startService(serviceIntent);
			int round = 20 * 1000;
			am.cancel(mAlarmSender);
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis()
					+ round, round, mAlarmSender);
		} else {
			stopService(serviceIntent);
			am.cancel(mAlarmSender);
		}
	}

	public boolean isServiceRun() {
		ActivityManager activityManager = (ActivityManager) getApplication()
				.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager
				.getRunningServices(Integer.MAX_VALUE);
		for (int i = 0; i < serviceList.size(); i++) {
			String c = serviceList.get(i).service.getClassName();
			if (c.equals(MonitorService1.class.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onStart() {
		System.out.println("===============================on start");
		super.onStart();
	}

	@Override
	protected void onStop() {
		System.out.println("===============================on stop");
		super.onStop();
	}

	@Override
	protected void onRestart() {
		System.out.println("===============================on restart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		System.out.println("===============================on resume");
		super.onResume();
	}

	@Override
	protected void onPause() {
		System.out.println("===============================on pause");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		System.out.println("===============================on destroy");
		super.onDestroy();
	}
}
