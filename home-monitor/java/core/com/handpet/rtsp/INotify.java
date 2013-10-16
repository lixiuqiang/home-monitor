package com.handpet.rtsp;

public interface INotify {

	void notify(String title, String text);
	
	void shutdown();
}
