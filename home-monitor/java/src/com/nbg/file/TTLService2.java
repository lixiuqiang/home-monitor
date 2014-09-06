package com.nbg.file;

public class TTLService2 extends TTLService{

	public TTLService2() {
		super(4, "ttl_2");
	}

	@Override
	public String name() {
		return "/dev/bus/usb/001/005";
	}

}
