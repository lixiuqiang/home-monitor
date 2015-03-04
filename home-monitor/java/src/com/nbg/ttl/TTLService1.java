package com.nbg.ttl;

public class TTLService1 extends TTLService {

	public TTLService1() {
		super(3, CONFIG_TTL_1);
	}

	@Override
	public int index() {
		return 0;
	}

}
