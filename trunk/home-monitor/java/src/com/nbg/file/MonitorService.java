package com.nbg.file;

import android.content.Intent;
import android.content.SharedPreferences;

import com.handpet.rtsp.RTSPClient;

public abstract class MonitorService extends NotificationService {
	private RTSPClient client;

	public MonitorService(int notfication_id, String config_name) {
		super(notfication_id, config_name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences config = getConfig();
		String dir = config.getString("dir", null);
		String url = config.getString("url_" + index(), null);
		String mb = config.getString("mb_" + index(), null);
		int m = 10240;
		try {
			m = Integer.parseInt(mb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		client = new RTSPClient(url, dir, m, 30, this);

		notify("Monitor", "begin");

		client.start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
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

	public abstract int index();
}
