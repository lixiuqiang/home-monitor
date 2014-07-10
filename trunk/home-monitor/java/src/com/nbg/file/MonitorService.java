package com.nbg.file;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import com.handpet.rtsp.INotify;
import com.handpet.rtsp.RTSPClient;

public class MonitorService extends Service implements INotify {
	private RTSPClient client;
	private Builder builder;
	private final int notification_id;
	private final String config_name;

	public MonitorService(int notfication_id, String config_name) {
		this.notification_id = notfication_id;
		this.config_name = config_name;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences config = getSharedPreferences(config_name,
				Context.MODE_PRIVATE);
		String dir = config.getString("dir", null);
		boolean check = config.getBoolean("check", false);
		if (check) {
			String url = config.getString("url", null);
			String mb = config.getString("mb", null);
			int m = 10240;
			try {
				m = Integer.parseInt(mb);
			} catch (Exception e) {
				e.printStackTrace();
			}
			client = new RTSPClient(url, dir, m, 30, this);

			Intent i = new Intent(this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
			builder = new Notification.Builder(this)
					.setSmallIcon(R.drawable.ic_launcher).setTicker("NBG")
					.setContentTitle("Monitor").setContentIntent(pi);
			notify("Monitor", "begin");

			client.start();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("===============onStartCommand==============");
		if (client == null || client.checkTimeout()) {
			shutdown();
		} else {
			String title = client.getTitle();
			String text = client.getText();
			notify(title, text);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		System.out.println("===============onDestroy==============");
		client.shutdown();
		super.onDestroy();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void notify(String title, String text) {
		builder.setContentTitle(title);
		builder.setContentText(text);
		startForeground(notification_id, builder.getNotification());
	}

	@Override
	public void shutdown() {
		System.exit(0);
		android.os.Process.killProcess(android.os.Process.myPid());
	}
}
