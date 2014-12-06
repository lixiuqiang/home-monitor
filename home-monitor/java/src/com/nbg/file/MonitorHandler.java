package com.nbg.file;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.handpet.rtsp.INotify;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

public class MonitorHandler implements HttpRequestHandler {

	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {

		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);
		if (method.equals("GET")) {
			response.setStatusCode(HttpStatus.SC_OK);
			StringBuilder monitor = new StringBuilder();
			monitor.append("倪宝刚家的监控状态：\n\n");
			monitor.append("服务状态：\n");
			handlerService(monitor);
			monitor.append("\n\n视频监控状态：\n");
			handlerPerference(INotify.CONFIG_MONITOR_1, monitor);
			handlerPerference(INotify.CONFIG_MONITOR_2, monitor);
			monitor.append("\n\n红外监控状态：\n");
			handlerPerference(INotify.CONFIG_TTL_1, monitor);
			handlerPerference(INotify.CONFIG_TTL_2, monitor);
			StringEntity entity = new StringEntity(monitor.toString(), "utf-8");
			response.setEntity(entity);
		} else {
			throw new MethodNotSupportedException(method
					+ " method not supported");
		}
	}

	@SuppressLint("SimpleDateFormat")
	private void handlerService(StringBuilder monitor) {
		ActivityManager mActivityManager = (ActivityManager) MainApplication
				.getContext().getSystemService(Context.ACTIVITY_SERVICE);

		// 获得正在运行的Service信息
		List<ActivityManager.RunningServiceInfo> runServiceList = mActivityManager
				.getRunningServices(20);

		System.out.println(runServiceList.size());

		// ServiceInfo Model类 用来保存所有进程信息
		for (ActivityManager.RunningServiceInfo runServiceInfo : runServiceList) {

			// 获得Service所在的进程的信息
			int pid = runServiceInfo.pid; // service所在的进程ID号
			// 进程名，默认是包名或者由属性android：process指定
			String processName = runServiceInfo.process;

			// 该Service启动时的时间值
			long activeSince = runServiceInfo.activeSince;
			activeSince = System.currentTimeMillis()
					- SystemClock.elapsedRealtime() + activeSince;
			Date d = new Date(activeSince);
			DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd|HH:mm:ss");
			String time = format2.format(d);

			// 获得该Service的组件信息 可能是pkgname/servicename
			ComponentName serviceCMP = runServiceInfo.service;
			String serviceName = serviceCMP.getShortClassName(); // service
																	// 的类名
			String pkgName = serviceCMP.getPackageName(); // 包名
			if (!pkgName.equals(MainApplication.getContext().getPackageName())) {
				continue;
			}

			// 打印Log
			monitor.append("进程名:" + processName + "\t进程id:" + pid + "\t启动时间："
					+ time + "\t组件信息:" + serviceName + "\n");
		}
	}

	private void handlerPerference(String config_name, StringBuilder monitor) {
		SharedPreferences config = MainApplication.getContext()
				.getSharedPreferences(config_name, 4);
		String detail = config.getString("monitor_detail", null);
		if (detail != null) {
			monitor.append(config_name).append("状态：").append(detail)
					.append("\n");
		}
	}
}
