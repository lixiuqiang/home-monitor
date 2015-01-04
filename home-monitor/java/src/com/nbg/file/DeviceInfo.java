package com.nbg.file;

public class DeviceInfo {
	private final String mac;
	private final String name;
	private String ip;
	private long last_time;
	private long change_time;
	private boolean line;

	public DeviceInfo(String mac, String name) {
		this.mac = mac;
		this.name = name;
	}

	public long getLast_time() {
		return last_time;
	}

	public void setLast_time(long last_time) {
		this.last_time = last_time;
	}

	public boolean isLine() {
		return line;
	}

	public void setLine(boolean line) {
		if (this.line != line) {
			this.change_time = System.currentTimeMillis();
		}
		this.line = line;
	}

	public long getChange_time() {
		return change_time;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMac() {
		return mac;
	}

	public String getName() {
		return name;
	}
}
