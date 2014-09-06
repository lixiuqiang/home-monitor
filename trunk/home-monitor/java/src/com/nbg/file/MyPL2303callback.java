package com.nbg.file;

import com.handpet.rtsp.INotify;
import com.monitor.ttl.driver.PL2303callback;

public class MyPL2303callback implements PL2303callback {
	private MailSender mailSender=new MailSender();
	private long time1 = 0;
	private long num1 = 0;
	private long time2 = 0;
	private long num2 = 0;
	private final INotify notify;
	
	public MyPL2303callback(INotify notify) {
		this.notify=notify;
	}

	@Override
	public void onRI(boolean state) {
		if (state) {
			notify.notify("报警","1号位检测到有人离开");
		} else {
			notify.notify("报警","1号位检测到有人闯入");
		}
		num1++;
		if (System.currentTimeMillis() - time1 < 60000) {
			return;
		}
		time1 = System.currentTimeMillis();
		try {
			mailSender.sendMail("1号位有人闯入", "赶紧回家看看吧，有人闯入 一分钟内报警次数" + num1,
					"nibaogang@163.com", "13920294304@139.com");
			num1 = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onInitSuccess(String devicename) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onInitFailed(String reason) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeviceDetached(String devicename) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDSR(boolean state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDCD(boolean state) {
		if (state) {
			notify.notify("报警","2号位检测到有人离开");
		} else {
			notify.notify("报警","2号位检测到有人闯入");
		}
		num2++;
		if (System.currentTimeMillis() - time2 < 60000) {
			return;
		}
		time2 = System.currentTimeMillis();
		try {
			mailSender.sendMail("2号位有人闯入", "赶紧回家看看吧，有人闯入 一分钟内报警次数" + num2,
					"nibaogang@163.com", "13920294304@139.com");
			num2 = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCTS(boolean state) {
		// TODO Auto-generated method stub

	}
}
