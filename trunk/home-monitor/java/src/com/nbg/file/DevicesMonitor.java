package com.nbg.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.conn.util.InetAddressUtils;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class DevicesMonitor {
	private static final DevicesMonitor monitor = new DevicesMonitor();
	private final HandlerThread handlerThread;
	private final Handler handler;
	private final String ping = "ping -c 1 -w 2.0 ";
	private final Map<String, DeviceInfo> deviceMap = new HashMap<String, DeviceInfo>();
	private Runnable oldRunnable;
	private long last_check_time;

	private final String MAC_ZTE = "4c:cb:f5:e2:38:4b";
	private final String MAC_T50 = "9c:a9:e4:18:5c:98";
	private final String MAC_MI2 = "ac:f7:f3:16:1c:47";

	public static DevicesMonitor getInstance() {
		return monitor;
	}

	private DevicesMonitor() {
		handlerThread = new HandlerThread("ping");
		handlerThread.start();
		handler = new Handler(handlerThread.getLooper());
		initDevice(MAC_ZTE, "星星一号手机");
		initDevice(MAC_T50, "中兴T50手机");
		initDevice(MAC_MI2, "米2电信版手机");
		initDevice("08:bd:43:86:24:fe", "网件从路由器");
		initDevice("70:54:f5:51:82:f3", "华为主路由器");
		initDevice("00:b9:f4:6c:00:93", "车位高清监控");
		initDevice("3c:97:0e:57:1c:b8", "联想笔记本  ");
		initDevice("00:07:a8:8c:e5:9a", "海尔水盒子  ");
		initDevice("74:e1:b6:74:78:e5", "苹果平板电脑");
		initDevice("8c:be:be:4e:82:32", "小米空气净化器");
		initDevice("10:48:b1:34:4f:bd", "小米4K电视2");
		initDevice("00:ec:4c:dd:f0:18", "客厅高清监控");
		initDevice("c8:93:46:46:80:82", "海尔醛知道  ");
		initDevice("00:16:96:04:a5:07", "联想台式机  ");
	}

	private void initDevice(String mac, String name) {
		DeviceInfo deviceInfo = new DeviceInfo(mac, name);
		deviceInfo.setIp("192.168.168.*");
		deviceMap.put(mac, deviceInfo);
	}

	public String getLocAddress() {
		String ipaddress = "";
		try {
			Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces();
			// 遍历所用的网络接口
			while (en.hasMoreElements()) {
				NetworkInterface networks = en.nextElement();
				// 得到每一个网络接口绑定的所有ip
				Enumeration<InetAddress> address = networks.getInetAddresses();
				// 遍历每一个接口绑定的所有ip
				while (address.hasMoreElements()) {
					InetAddress ip = address.nextElement();
					if (!ip.isLoopbackAddress()
							&& InetAddressUtils.isIPv4Address(ip
									.getHostAddress())) {
						ipaddress = ip.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			Log.e("", "获取本地ip地址失败");
			e.printStackTrace();
		}
		System.out.println("本机IP:" + ipaddress);
		return ipaddress;

	}

	public synchronized void scan() {
		if (oldRunnable != null) {
			System.out.println("清除历史扫描任务");
			handler.removeCallbacks(oldRunnable);
		}
		oldRunnable = new Runnable() {
			public void run() {
				final String address = getLocAddress();
				String scan_addrees = address.substring(0,
						address.lastIndexOf(".") + 1);
				pingIP(scan_addrees + 168);
				for (int i = 1; i < 11; i++) {
					pingIP(scan_addrees + i);
				}
				for (int i = 20; i < 50; i++) {
					pingIP(scan_addrees + i);
				}
			}
		};
		handler.post(oldRunnable);
		System.out.println("提交扫描任务");
	}

	private void pingIP(String current_ip) {
		try {
			System.out.println("尝试连接IP：" + current_ip);
			Process proc = Runtime.getRuntime().exec(ping + current_ip);
			int result = proc.waitFor();
			if (result == 0) {
				System.out.println("连接成功" + current_ip);
				getMacFromFile(current_ip, true);
			} else {
				System.out.println("连接失败" + current_ip);
				getMacFromFile(current_ip, false);
			}
			last_check_time = System.currentTimeMillis();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private void getMacFromFile(String current_ip, boolean line) {
		List<String> mResult = readFileLines("/proc/net/arp");
		Log.d("nbg", "=======  /proc/net/arp  =========");
		for (int i = 0; i < mResult.size(); ++i) {
			Log.d("line", mResult.get(i));
		}

		if (mResult != null && mResult.size() > 1) {
			for (int j = 1; j < mResult.size(); ++j) {
				List<String> mList = new ArrayList<String>();
				String[] mType = mResult.get(j).split(" ");
				for (int i = 0; i < mType.length; ++i) {
					if (mType[i] != null && mType[i].length() > 0)
						mList.add(mType[i]);
				}

				if (mList != null && mList.size() > 4) {
					String ip = mList.get(0);
					String mac = mList.get(3);
					if (!"00:00:00:00:00:00".equals(mac)) {
						DeviceInfo deviceInfo = deviceMap.get(mac);
						if (deviceInfo == null) {
							deviceInfo = new DeviceInfo(mac, "未知网络设备");
							deviceMap.put(mac, deviceInfo);
						}
						deviceInfo.setIp(ip);
						if (ip.equals(current_ip)) {
							deviceInfo.setLast_time(System.currentTimeMillis());
							deviceInfo.setLine(line);
						}
						Log.i("nbg", "ip :" + ip + " mac:" + mac);
					}
				}
			}
		}
	}

	private List<String> readFileLines(String fileName) {
		File file = new File(fileName);
		BufferedReader reader = null;
		String tempString = "";
		List<String> mResult = new ArrayList<String>();
		try {
			Log.i("result", "以行为单位读取文件内容，一次读一整行：");
			reader = new BufferedReader(new FileReader(file));
			while ((tempString = reader.readLine()) != null) {
				mResult.add(tempString);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}

		return mResult;
	}

	public void monitor(StringBuilder monitor) {
		isBodyOnline(monitor, "男主人", MAC_T50, MAC_ZTE);
		isBodyOnline(monitor, "女主人", MAC_MI2);

		monitor.append("\n\n设备状态：\n");
		String address = getLocAddress();
		String check_time = MonitorHandler.formatTimeAgo(last_check_time);
		monitor.append("\n本机IP:" + address).append("\t最后扫描时间:")
				.append(check_time);
		int online = monitorStatus(null, true);
		int offline = monitorStatus(null, false);
		monitor.append("\t在线个数:").append(online);
		monitor.append("\t离线个数:").append(offline).append("\n");

		monitorStatus(monitor, true);
		monitor.append("\n");
		monitorStatus(monitor, false);

	}

	private boolean isBodyOnline(StringBuilder monitor, String name,
			String... macs) {
		monitor.append("\n").append(name).append("   ");
		DeviceInfo onlineDevice = null;
		// 搜寻在线设备
		for (String mac : macs) {
			DeviceInfo deviceInfo = deviceMap.get(mac);
			if (deviceInfo != null && deviceInfo.isLine()) {
				if (onlineDevice == null
						|| onlineDevice.getChange_time() > deviceInfo
								.getChange_time()) {
					onlineDevice = deviceInfo;
				}
			}
		}
		if (onlineDevice == null) {
			// 搜寻不在线设备
			for (String mac : macs) {
				DeviceInfo deviceInfo = deviceMap.get(mac);
				if (deviceInfo != null && !deviceInfo.isLine()) {
					if (onlineDevice == null
							|| onlineDevice.getChange_time() < deviceInfo
									.getChange_time()) {
						onlineDevice = deviceInfo;
					}
				}
			}
		}
		if (onlineDevice != null && onlineDevice.getChange_time() > 0) {
			String time = MonitorHandler.formatTime(onlineDevice
					.getChange_time());
			if (onlineDevice.isLine()) {
				monitor.append(time).append("回到家中").append("  判断设备:")
						.append(onlineDevice.getName());
				return true;
			} else {
				monitor.append(time).append("离开家里").append("  判断设备:")
						.append(onlineDevice.getName());
				return false;
			}
		} else {
			monitor.append("情况不明");
			return false;
		}
	}

	private int monitorStatus(StringBuilder monitor, boolean line) {
		int num = 0;
		for (DeviceInfo deviceInfo : deviceMap.values()) {
			String value = MonitorHandler.formatTimeAgo(deviceInfo
					.getLast_time());
			String change_value = MonitorHandler.formatTimeAgo(deviceInfo
					.getChange_time());
			if (deviceInfo.isLine()) {
				if (line) {
					if (monitor != null) {
						monitor.append("\nip:").append(deviceInfo.getIp());
						monitor.append("\tmac:").append(deviceInfo.getMac());
						monitor.append("\t设备:").append(deviceInfo.getName());
						monitor.append("\t状态:在线\t更新时间:").append(value);
						monitor.append("\t状态切换:").append(change_value);
					}
					num++;
				}
			} else {
				if (!line) {
					if (monitor != null) {
						monitor.append("\nip:").append(deviceInfo.getIp());
						monitor.append("\tmac:").append(deviceInfo.getMac());
						monitor.append("\t设备:").append(deviceInfo.getName());
						monitor.append("\t状态:离线\t更新时间:").append(value);
						monitor.append("\t状态切换:").append(change_value);
					}
					num++;
				}
			}
		}
		return num;
	}

	public void checkIP() {
		if (System.currentTimeMillis() - last_check_time > 600 * 1000) {
			scan();
		}
	}
}
