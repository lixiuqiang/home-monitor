package com.nbg.file;

import android.app.Notification.Builder;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import com.handpet.rtsp.INotify;

public abstract class NotificationService extends Service implements INotify {
	private Builder builder;
	private final int notification_id;
	protected final String config_name;

	public NotificationService(int notfication_id, String config_name) {
		this.notification_id = notfication_id;
		this.config_name = config_name;
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
	
	@Override
	public void onCreate() {
		super.onCreate();
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		builder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher).setTicker("NBG")
				.setContentTitle("Monitor").setContentIntent(pi);
	}
}