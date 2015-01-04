package com.nbg.file;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class MainApplication extends Application {

	private static Context context;

	public static Context getContext() {
		return context;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		context = getApplicationContext();
		Intent serviceIntent = new Intent(context, BootReceiver.class);
		PendingIntent mAlarmSender = PendingIntent.getBroadcast(context, 0,
				serviceIntent, 0);
		AlarmManager am = (AlarmManager) context
				.getSystemService(Activity.ALARM_SERVICE);
		int round = 10 * 1000;
		am.cancel(mAlarmSender);
		am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + round,
				round, mAlarmSender);
	}
}
