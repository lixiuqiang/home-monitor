package com.nbg.monitor;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.handpet.rtsp.INotify;
import com.nbg.file.R;
import com.nbg.ttl.TTLService1;
import com.nbg.ttl.TTLService2;
import com.nbg.web.WebService;

public class MainActivity extends Activity implements OnClickListener {
	private Button monitor_button1;
	private Button monitor_button2;
	private Button ttl_button1;
	private Button ttl_button2;
	private Button ttl_switch1;
	private Button ttl_switch2;
	private Button web_button;
	private EditText dirText;
	private EditText urlText1;
	private EditText urlText2;
	private EditText mbText1;
	private EditText mbText2;
	private TextView device1;
	private TextView device2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("===============================on create");
		setContentView(R.layout.activity_main);
		dirText = (EditText) findViewById(R.id.dirText);
		monitor_button1 = (Button) findViewById(R.id.monitor_button1);
		handler(monitor_button1, INotify.CONFIG_MONITOR_1, false, null);
		monitor_button2 = (Button) findViewById(R.id.monitor_button2);
		handler(monitor_button2, INotify.CONFIG_MONITOR_2, false, null);

		ttl_button1 = (Button) findViewById(R.id.ttl_button1);
		handler(ttl_button1, INotify.CONFIG_TTL_1, false, null);
		ttl_button2 = (Button) findViewById(R.id.ttl_button2);
		handler(ttl_button2, INotify.CONFIG_TTL_2, false, null);

		web_button = (Button) findViewById(R.id.web_button);
		handler(web_button, INotify.CONFIG_WEB, false, null);

		ttl_switch1 = (Button) findViewById(R.id.switch1);
		ttl_switch2 = (Button) findViewById(R.id.switch2);

		urlText1 = (EditText) findViewById(R.id.urlText1);
		urlText2 = (EditText) findViewById(R.id.urlText2);
		mbText1 = (EditText) findViewById(R.id.mbText1);
		mbText2 = (EditText) findViewById(R.id.mbText2);
		device1 = (TextView) findViewById(R.id.device1);
		device2 = (TextView) findViewById(R.id.device2);
		monitor_button1.setOnClickListener(this);
		monitor_button2.setOnClickListener(this);
		ttl_button1.setOnClickListener(this);
		ttl_button2.setOnClickListener(this);
		ttl_switch1.setOnClickListener(this);
		ttl_switch2.setOnClickListener(this);
		web_button.setOnClickListener(this);

		SharedPreferences config = getSharedPreferences(INotify.CONFIG_BASE);
		dirText.setText(config.getString("dir", "/mnt/sda1/HOME/"));
		mbText1.setText(config.getString("mb_1", "81920"));
		urlText1.setText(config.getString("url_1",
				"rtsp://xiaoni:dugudao3721@192.168.168.7:7001/mpeg4"));
		mbText2.setText(config.getString("mb_2", "40960"));
		urlText2.setText(config.getString("url_2",
				"rtsp://xiaoni:dugudao3721@192.168.168.8:8001/mpeg4"));

		SharedPreferences ttl_1 = getSharedPreferences(INotify.CONFIG_TTL_1);
		device1.setText(ttl_1.getString("device", null));

		SharedPreferences ttl_2 = getSharedPreferences(INotify.CONFIG_TTL_2);
		device2.setText(ttl_2.getString("device", null));

	}

	private SharedPreferences getSharedPreferences(String config_name) {
		return getSharedPreferences(config_name, 4);
	}

	@Override
	public void onClick(View v) {
		if (v == monitor_button1) {
			Editor editor = getSharedPreferences(INotify.CONFIG_BASE).edit();
			editor.putString("dir", dirText.getText().toString());
			editor.putString("url_1", urlText1.getText().toString());
			editor.putString("mb_1", mbText1.getText().toString());
			editor.commit();
			handler(monitor_button1, INotify.CONFIG_MONITOR_1, true,
					MonitorService1.class);
		} else if (v == monitor_button2) {
			Editor editor = getSharedPreferences(INotify.CONFIG_BASE).edit();
			editor.putString("dir", dirText.getText().toString());
			editor.putString("url_2", urlText2.getText().toString());
			editor.putString("mb_2", mbText2.getText().toString());
			editor.commit();
			handler(monitor_button2, INotify.CONFIG_MONITOR_2, true,
					MonitorService2.class);
		} else if (v == ttl_button1) {
			SharedPreferences ttl_1 = getSharedPreferences(INotify.CONFIG_TTL_1);
			device1.setText(ttl_1.getString("device", null));
			handler(ttl_button1, INotify.CONFIG_TTL_1, true, TTLService1.class);
		} else if (v == ttl_button2) {
			SharedPreferences ttl_2 = getSharedPreferences(INotify.CONFIG_TTL_2);
			device2.setText(ttl_2.getString("device", null));
			handler(ttl_button2, INotify.CONFIG_TTL_2, true, TTLService2.class);
		} else if (v == web_button) {
			handler(web_button, INotify.CONFIG_WEB, true, WebService.class);
		} else if (v == ttl_switch1) {
			SharedPreferences ttl_1 = getSharedPreferences(INotify.CONFIG_TTL_1);
			boolean value = !ttl_1.getBoolean("switch", false);
			Editor editor = ttl_1.edit();
			editor.putBoolean("switch", value);
			editor.commit();
			if (value) {
				ttl_switch1.setText(R.string.off);
			} else {
				ttl_switch1.setText(R.string.on);
			}
			Intent serviceIntent = new Intent(getApplicationContext(),
					TTLService1.class);
			serviceIntent.putExtra("ttl_switch", value);
			startService(serviceIntent);
		} else if (v == ttl_switch2) {
			SharedPreferences ttl_2 = getSharedPreferences(INotify.CONFIG_TTL_2);
			boolean value = !ttl_2.getBoolean("switch", false);
			Editor editor = ttl_2.edit();
			editor.putBoolean("switch", value);
			editor.commit();
			if (value) {
				ttl_switch2.setText(R.string.off);
			} else {
				ttl_switch2.setText(R.string.on);
			}
			Intent serviceIntent = new Intent(getApplicationContext(),
					TTLService2.class);
			serviceIntent.putExtra("ttl_switch", value);
			startService(serviceIntent);
		}
	}

	public void handler(Button button, String config_name, boolean click,
			Class<?> c) {
		SharedPreferences preference = getSharedPreferences(INotify.CONFIG_BASE);
		boolean enable = preference.getBoolean(config_name, false);
		if (click) {
			enable = !enable;
			Editor editor = preference.edit();
			editor.putBoolean(config_name, enable);
			editor.commit();
			System.out.println("====commit enable:" + enable + "====");
			BootReceiver.handler(enable, getApplicationContext(), c);
		}
		if (enable) {
			button.setText(R.string.end);
		} else {
			button.setText(R.string.start);
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
