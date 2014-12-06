package com.handpet.rtsp;

public interface INotify {

	void notify(String title, String text);

	void notify(String title, String text, boolean sendEmail);

	void shutdown();
}
