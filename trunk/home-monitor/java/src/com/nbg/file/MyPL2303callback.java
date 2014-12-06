package com.nbg.file;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;

import com.handpet.rtsp.INotify;
import com.monitor.ttl.driver.PL2303callback;

@SuppressLint("SimpleDateFormat")
public class MyPL2303callback implements PL2303callback {
	private long time = 0;
	private Map<String, Integer> numMap = new HashMap<String, Integer>();
	private int jiange = 1;
	private final INotify notify;
	private boolean sendMail = true;

	public MyPL2303callback(INotify notify) {
		this.notify = notify;
	}

	private void notify(boolean state, String location) {
		Integer num = numMap.get(location);
		if (num == null) {
			num = 0;
		}
		num++;
		numMap.put(location, num);
		StringBuilder builder = new StringBuilder();
		builder.append("当前状态:");
		if (sendMail) {
			builder.append("设防");
		} else {
			builder.append("撤防");
		}
		builder.append("，间隔").append(jiange);
		builder.append("分钟，触发次数为");
		for (String l : numMap.keySet()) {
			int n = numMap.get(l);
			builder.append("(").append(l).append("触发").append(n).append("次)");
		}
		String detail = null;
		if (state) {
			detail = location + "检测到有人离开";
		} else {
			detail = location + "检测到有人闯入";
		}
		notify.notify(detail, builder.toString());
		long ago = System.currentTimeMillis() - time;
		if (ago < jiange * 60000) {
			return;
		}
		if (ago > (jiange * 2) * 60000) {
			jiange /= 2;
		} else {
			jiange *= 2;
		}
		if (jiange < 1) {
			jiange = 1;
		}
		if (jiange > 64) {
			jiange = 64;
		}
		time = System.currentTimeMillis();
		try {
			notify.notify(detail + ",间隔调整为" + jiange + "分钟",
					builder.toString(), sendMail);
			numMap.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRI(boolean state) {
		notify(state, "卧室");
	}

	@Override
	public void onInitSuccess(String devicename) {
		notify.notify("维护信息", "设备" + devicename + "启动成功", true);
	}

	@Override
	public void onInitFailed(String reason) {
		notify.notify("维护信息", "设备" + reason + "启动失败", true);
	}

	@Override
	public void onDeviceDetached(String devicename) {
		notify.notify("维护信息", "设备" + devicename + "断开连接", true);
	}

	@Override
	public void onDSR(boolean state) {
	}

	@Override
	public void onDCD(boolean state) {
		notify(state, "客厅");
	}

	@Override
	public void onCTS(boolean state) {
	}
}
