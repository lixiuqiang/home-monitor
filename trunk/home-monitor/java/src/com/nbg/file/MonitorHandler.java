package com.nbg.file;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.handpet.rtsp.INotify;

public class MonitorHandler implements HttpRequestHandler {

	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {
		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);
		String target = request.getRequestLine().getUri();
		Map<String, String> params = getParam(target);
		if (method.equals("GET")) {
			response.setStatusCode(HttpStatus.SC_OK);
			StringBuilder monitor = new StringBuilder();
			handlerDevices(monitor, params.get("scan") != null);
			boolean s = handlerService(monitor);
			monitor.append("\n\n视频监控状态：\n");
			boolean m1 = handlerPerference(INotify.CONFIG_MONITOR_1, monitor, 2);
			boolean m2 = handlerPerference(INotify.CONFIG_MONITOR_2, monitor, 2);
			monitor.append("\n\n红外监控状态：\n");
			boolean t1 = handlerPerference(INotify.CONFIG_TTL_1, monitor,
					72 * 60);
			boolean t2 = handlerPerference(INotify.CONFIG_TTL_2, monitor,
					72 * 60);
			if (s && m1 && m2 && t1 && t2) {
				monitor.insert(0, "监控状态：监控正常\n");
			} else {
				monitor.insert(0, "监控状态：监控异常\n");
			}
			StringEntity entity = new StringEntity(monitor.toString(), "utf-8");
			response.setEntity(entity);
		} else {
			throw new MethodNotSupportedException(method
					+ " method not supported");
		}
	}

	@SuppressLint("SimpleDateFormat")
	private boolean handlerService(StringBuilder monitor) {
		monitor.append("\n\n服务状态：\n");
		ActivityManager mActivityManager = (ActivityManager) MainApplication
				.getContext().getSystemService(Context.ACTIVITY_SERVICE);

		// 获得正在运行的Service信息
		List<ActivityManager.RunningServiceInfo> runServiceList = mActivityManager
				.getRunningServices(50);

		System.out.println(runServiceList.size());

		boolean result = true;
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
			String time = formatTime(activeSince);

			// 获得该Service的组件信息 可能是pkgname/servicename
			ComponentName serviceCMP = runServiceInfo.service;
			String serviceName = serviceCMP.getClassName(); // service
															// 的类名
			String pkgName = serviceCMP.getPackageName(); // 包名
			if (!pkgName.equals(MainApplication.getContext().getPackageName())) {
				continue;
			}

			// 打印Log
			monitor.append("进程名:" + processName + "\t进程id:" + pid + "\t启动时间："
					+ time + "\t组件信息:" + serviceName + "\n");

			if (pid <= 0) {
				result = false;
			}
		}
		return result;
	}

	private boolean handlerPerference(String config_name,
			StringBuilder monitor, long timeout_minute) {
		SharedPreferences config = MainApplication.getContext()
				.getSharedPreferences(config_name, 4);
		SharedPreferences enable_config = MainApplication.getContext()
				.getSharedPreferences(INotify.CONFIG_BASE, 4);
		boolean enable = enable_config.getBoolean(config_name, false);
		String detail = config.getString("monitor_detail", null);

		long time = config.getLong("monitor_time_detail", 0);
		long monitor_ago = System.currentTimeMillis() - time;

		long start_time = config.getLong("servie_start_time", 0);
		monitor.append("\n").append(config_name);
		if (enable) {
			monitor.append("(启动)");
			monitor.append(formatTimeAgo(start_time));
			monitor.append("  (刷新)");
			monitor.append(formatTimeAgo(monitor_ago)).append("\n");
			if (detail != null) {
				monitor.append(detail).append("\n");
			}
			return timeout_minute * 60000 >= monitor_ago;
		} else {
			monitor.append("(停止)");
			monitor.append(formatTimeAgo(start_time));
			monitor.append("  (刷新)");
			monitor.append(formatTimeAgo(monitor_ago)).append("\n");
			return true;
		}
	}

	@SuppressLint("SimpleDateFormat")
	public static String formatTime(long time) {
		if (time == 0) {
			return "未知";
		}
		Date d = new Date(time);
		DateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return format2.format(d);
	}

	public static String formatTimeAgo(long time) {
		time = System.currentTimeMillis() - time;
		StringBuilder builder = new StringBuilder();
		long second = time / 1000;
		long minute = second / 60;
		if (minute > 0) {
			long hour = minute / 60;
			if (hour > 0) {
				long day = hour / 24;
				if (day > 0) {
					if (day > 99) {
						return "未知";
					}
					builder.append(day).append("天");
				}
				builder.append(hour % 60).append("时");
			}
			builder.append(minute % 60).append("分");
		}
		builder.append(second % 60).append("秒").append("前");
		return builder.toString();
	}

	private static void handlerDevices(StringBuilder monitor, boolean scan) {
		if (scan) {
			DevicesMonitor.getInstance().scan();
		}
		DevicesMonitor.getInstance().monitor(monitor);
	}

	public static Map<String, String> getParam(String uri) {
		Map<String, String> params = new HashMap<String, String>();
		try {
			if (uri != null) {
				String subStr = uri.substring(uri.indexOf("?") + 1);
				String[] ary = subStr.split("&");
				for (int i = 0; i < ary.length; i++) {
					String[] temp = ary[i].split("=");
					if (temp.length < 2) {
						params.put(temp[0], "");
					} else {
						params.put(temp[0], temp[1]);
					}
				}
				return params;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
}
