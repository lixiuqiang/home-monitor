package com.nbg.monitor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.handpet.rtsp.INotify;
import com.nbg.file.R;
import com.nbg.ttl.MailSender;

@SuppressLint("SimpleDateFormat")
public abstract class NotificationService extends Service implements INotify {
	private final MailSender mailSender = new MailSender();
	private Builder builder;
	private final int notification_id;
	protected final String config_name;

	public NotificationService(int notfication_id, String config_name) {
		this.notification_id = notfication_id;
		this.config_name = config_name;
	}

	protected SharedPreferences getMonitorConfig() {
		return getSharedPreferences(this.config_name, 4);
	}

	protected SharedPreferences getConfig() {
		return getSharedPreferences(INotify.CONFIG_BASE, 4);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void notify(String title, String text) {
		builder.setContentTitle(title);
		builder.setContentText(text);
		Log.i("nbg", title + " " + text);
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		monitor("detail", format.format(new Date()) + " " + title + "\n" + text);
		startForeground(notification_id, builder.getNotification());
	}

	@Override
	public void notify(String title, String text, boolean sendEmail) {
		notify(title, text);
		if (sendEmail) {
			mailSender.sendMail(title, text);
		}
	}

	@Override
	public void monitor(String key, String value) {
		try {
			Editor editor = getMonitorConfig().edit();
			editor.putString("monitor_" + key, value);
			editor.putLong("monitor_time_" + key, System.currentTimeMillis());
			editor.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown() {
		stopSelf();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("nbg", "service " + getClass().getName() + " start");
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
		builder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher).setTicker("NBG")
				.setContentTitle("Monitor").setContentIntent(pi);
		Editor editor = getMonitorConfig().edit();
		editor.putLong("servie_start_time", System.currentTimeMillis());
		editor.commit();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

			@Override
			public void run() {
				Process.killProcess(Process.myPid());
			}
		}, 1000);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}
