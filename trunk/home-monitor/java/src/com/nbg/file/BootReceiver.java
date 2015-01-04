package com.nbg.file;

import java.util.List;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;

import com.handpet.rtsp.INotify;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		System.out.println("nbg:BootReceiver");
		checkService(context, INotify.CONFIG_MONITOR_1, MonitorService1.class);
		checkService(context, INotify.CONFIG_MONITOR_2, MonitorService2.class);
		checkService(context, INotify.CONFIG_TTL_1, TTLService1.class);
		checkService(context, INotify.CONFIG_TTL_2, TTLService2.class);
		checkService(context, INotify.CONFIG_WEB, WebService.class);
	}

	private void checkService(Context context, String config_name,
			Class<? extends Service> c) {
		SharedPreferences preferences = context.getSharedPreferences(INotify.CONFIG_BASE,
				4);
		boolean enable = preferences.getBoolean(config_name, false);
		handler(enable, context, c);
	}

	public static void handler(boolean enable, Context context, Class<?> c) {
		Intent serviceIntent = new Intent(context, c);
		if (enable) {
			context.startService(serviceIntent);
		} else {
			context.stopService(serviceIntent);
		}
	}

	@SuppressWarnings("unused")
	private void checkPid(final Context context) {
		HandlerThread handlerThread = new HandlerThread("refresh_file");
		handlerThread.start();

		Handler handler = new Handler(handlerThread.getLooper());
		ActivityManager mActivityManager = (ActivityManager) MainApplication
				.getContext().getSystemService(Context.ACTIVITY_SERVICE);

		// 获得正在运行的Service信息
		List<ActivityManager.RunningServiceInfo> runServiceList = mActivityManager
				.getRunningServices(50);

		for (ActivityManager.RunningServiceInfo runServiceInfo : runServiceList) {
			int pid = runServiceInfo.pid;
			ComponentName serviceCMP = runServiceInfo.service;
			String serviceName = serviceCMP.getClassName(); // service

			String pkgName = serviceCMP.getPackageName(); // 包名
			if (!pkgName.equals(MainApplication.getContext().getPackageName())) {
				continue;
			}
			if (pid == 0) {
				System.out.println("pid 0 serviceName:" + serviceName);
				try {
					final Class<?> c = Class.forName(serviceName);
					handler(false, context, c);
					handler.postDelayed(new Runnable() {

						@Override
						public void run() {
							handler(true, context, c);
						}
					}, 2000);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
