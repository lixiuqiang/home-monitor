package com.nbg.file;

public class TTLService1 extends TTLService{

	public TTLService1() {
		super(3, "ttl_1");
	}

	@Override
	public String name() {
		return "/dev/bus/usb/001/003";
	}

}
