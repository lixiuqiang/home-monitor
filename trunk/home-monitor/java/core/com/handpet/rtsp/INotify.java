package com.handpet.rtsp;

public interface INotify {
	String CONFIG_WEB = "web";
	String CONFIG_MONITOR_1 = "monitor_1";
	String CONFIG_MONITOR_2 = "monitor_2";
	String CONFIG_TTL_1 = "ttl_1";
	String CONFIG_TTL_2 = "ttl_2";

	void notify(String title, String text);

	void notify(String title, String text, boolean sendEmail);

	void monitor(String key, String value);

	void shutdown();
}
