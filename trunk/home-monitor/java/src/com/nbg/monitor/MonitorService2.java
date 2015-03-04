package com.nbg.monitor;

public class MonitorService2 extends MonitorService {

	public MonitorService2() {
		super(2, CONFIG_MONITOR_2);
	}
	
	@Override
	public int index() {
		return 2;
	}
}
