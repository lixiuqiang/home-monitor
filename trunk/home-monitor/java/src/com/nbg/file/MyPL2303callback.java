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
	private int jiange1=1;
	private int jiange2=1;
	
	public MyPL2303callback(INotify notify) {
		this.notify=notify;
	}

	@Override
	public void onRI(boolean state) {
		if (state) {
			notify.notify("报警","卧室检测到有人离开");
		} else {
			notify.notify("报警","卧室检测到有人闯入");
		}
		num1++;
		long ago = System.currentTimeMillis() - time1;
		if (ago < jiange1 * 60000) {
			return;
		}
		if (ago > (jiange1 + 1) * 60000) {
			jiange1 = 1;
		}else{			
			jiange1++;
		}
		time1 = System.currentTimeMillis();
		try {
			mailSender.sendMail("卧室有人闯入,间隔"+jiange1+"分钟", "赶紧回家看看吧，有人闯入 一分钟内报警次数" + num1);
			num1 = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onInitSuccess(String devicename) {
		try {
			mailSender.sendMail("设备"+devicename+"启动成功", devicename);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onInitFailed(String reason) {
		try {
			mailSender.sendMail("设备"+reason+"启动失败", reason);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDeviceDetached(String devicename) {
		try {
			mailSender.sendMail("设备"+devicename+"断开", devicename);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDSR(boolean state) {
	}

	@Override
	public void onDCD(boolean state) {
		if (state) {
			notify.notify("报警","客厅检测到有人离开");
		} else {
			notify.notify("报警","客厅检测到有人闯入");
		}
		num2++;
		long ago = System.currentTimeMillis() - time2;
		if (ago < jiange2 * 60000) {
			return;
		}
		if (ago > (jiange2 + 1) * 60000) {
			jiange2 = 1;
		}else{			
			jiange2++;
		}
		time2 = System.currentTimeMillis();
		try {
			mailSender.sendMail("客厅有人闯入,间隔"+jiange2+"分钟", "赶紧回家看看吧，有人闯入 一分钟内报警次数" + num2);
			num2 = 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCTS(boolean state) {
	}
}
