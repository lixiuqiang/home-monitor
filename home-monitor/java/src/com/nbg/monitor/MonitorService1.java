package com.nbg.monitor;

public class MonitorService1 extends MonitorService {

	public MonitorService1() {
		super(1, CONFIG_MONITOR_1);
	}

	@Override
	public int index() {
		return 1;
	}
}
