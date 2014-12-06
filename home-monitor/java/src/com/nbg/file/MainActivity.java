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
import android.widget.EditText;

public class MainActivity extends Activity implements OnClickListener {
	private Button monitor_button1;
	private Button monitor_button2;
	private Button ttl_button1;
	private Button ttl_button2;
	private Button ttl_switch1;
	private Button ttl_switch2;
	private EditText dirText;
	private EditText urlText1;
	private EditText urlText2;
	private EditText mbText1;
	private EditText mbText2;
	private SharedPreferences config_1;
	private SharedPreferences config_2;
	private SharedPreferences ttl_1;
	private SharedPreferences ttl_2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("===============================on create");
		setContentView(R.layout.activity_main);
		dirText = (EditText) findViewById(R.id.dirText);
		monitor_button1 = (Button) findViewById(R.id.monitor_button1);
		config_1 = getSharedPreferences("config_1", Context.MODE_PRIVATE);
		refreshButton(monitor_button1, config_1, false);
		monitor_button2 = (Button) findViewById(R.id.monitor_button2);
		config_2 = getSharedPreferences("config_2", Context.MODE_PRIVATE);
		refreshButton(monitor_button2, config_2, false);

		ttl_button1 = (Button) findViewById(R.id.ttl_button1);
		ttl_1 = getSharedPreferences("ttl_1", Context.MODE_PRIVATE);
		refreshButton(ttl_button1, ttl_1, false);
		ttl_button2 = (Button) findViewById(R.id.ttl_button2);
		ttl_2 = getSharedPreferences("ttl_2", Context.MODE_PRIVATE);
		refreshButton(ttl_button2, ttl_2, false);

		ttl_switch1 = (Button) findViewById(R.id.switch1);
		ttl_switch2 = (Button) findViewById(R.id.switch2);

		urlText1 = (EditText) findViewById(R.id.urlText1);
		urlText2 = (EditText) findViewById(R.id.urlText2);
		mbText1 = (EditText) findViewById(R.id.mbText1);
		mbText2 = (EditText) findViewById(R.id.mbText2);
		monitor_button1.setOnClickListener(this);
		monitor_button2.setOnClickListener(this);
		ttl_button1.setOnClickListener(this);
		ttl_button2.setOnClickListener(this);
		ttl_switch1.setOnClickListener(this);
		ttl_switch2.setOnClickListener(this);

		dirText.setText(config_1.getString("dir", "/mnt/sda1/HOME/"));
		mbText1.setText(config_1.getString("mb", "81920"));
		urlText1.setText(config_1.getString("url",
				"rtsp://xiaoni:dugudao3721@192.168.168.7:7001/mpeg4"));

		mbText2.setText(config_2.getString("mb", "40960"));
		urlText2.setText(config_2.getString("url",
				"rtsp://xiaoni:dugudao3721@192.168.168.8:8001/mpeg4"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v == monitor_button1) {
			Editor editor = config_1.edit();
			editor.putString("dir", dirText.getText().toString());
			editor.putString("url", urlText1.getText().toString());
			editor.putString("mb", mbText1.getText().toString());
			editor.commit();
			handler(monitor_button1, config_1, getApplicationContext(),
					MonitorService1.class);
		} else if (v == monitor_button2) {
			Editor editor = config_2.edit();
			editor.putString("dir", dirText.getText().toString());
			editor.putString("url", urlText2.getText().toString());
			editor.putString("mb", mbText2.getText().toString());
			editor.commit();
			handler(monitor_button2, config_2, getApplicationContext(),
					MonitorService2.class);
		} else if (v == ttl_button1) {
			handler(ttl_button1, ttl_1, getApplicationContext(),
					TTLService1.class);
		} else if (v == ttl_button2) {
			handler(ttl_button2, ttl_2, getApplicationContext(),
					TTLService2.class);
		} else if (v == ttl_switch1) {
			boolean value = !ttl_1.getBoolean("switch", false);
			Editor editor = ttl_1.edit();
			editor.putBoolean("switch", value);
			editor.commit();
			if (value) {
				ttl_switch1.setText(R.string.off);
			} else {
				ttl_switch1.setText(R.string.on);
			}
			Intent serviceIntent = new Intent(getApplicationContext(), TTLService1.class);
			serviceIntent.putExtra("ttl_switch", value);
			startService(serviceIntent);
		} else if (v == ttl_switch2) {
			boolean value = !ttl_2.getBoolean("switch", false);
			Editor editor = ttl_2.edit();
			editor.putBoolean("switch", value);
			editor.commit();
			if (value) {
				ttl_switch2.setText(R.string.off);
			} else {
				ttl_switch2.setText(R.string.on);
			}
			Intent serviceIntent = new Intent(getApplicationContext(), TTLService2.class);
			serviceIntent.putExtra("ttl_switch", value);
			startService(serviceIntent);
		}
	}

	public static void handler(Button button, SharedPreferences preference,
			Context context, Class<?> c) {
		boolean enable = refreshButton(button, preference, true);
		handler(enable, context, c);
	}

	public static void handler(boolean enable, Context context, Class<?> c) {
		Intent serviceIntent = new Intent(context, c);
		PendingIntent mAlarmSender = PendingIntent.getService(context, 0,
				serviceIntent, 0);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Activity.ALARM_SERVICE);
		if (enable) {
			context.startService(serviceIntent);
			int round = 10 * 1000;
			am.cancel(mAlarmSender);
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis()
					+ round, round, mAlarmSender);
		} else {
			context.stopService(serviceIntent);
			am.cancel(mAlarmSender);
		}
	}

	private static boolean refreshButton(Button button,
			SharedPreferences preference, boolean click) {
		boolean enable = preference.getBoolean("enable", false);
		if (click) {
			enable = !enable;
			Editor editor = preference.edit();
			editor.putBoolean("enable", enable);
			editor.commit();
		}
		if (enable) {
			button.setText(R.string.end);
		} else {
			button.setText(R.string.start);
		}
		return enable;
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
