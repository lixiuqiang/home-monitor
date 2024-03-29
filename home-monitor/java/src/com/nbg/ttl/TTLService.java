package com.nbg.ttl;

import java.util.List;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.HandlerThread;

import com.monitor.ttl.driver.PL2303Exception;
import com.monitor.ttl.driver.PL2303callback;
import com.monitor.ttl.driver.PL2303driver;
import com.monitor.ttl.driver.PL2303driver.BaudRate;
import com.monitor.ttl.driver.PL2303driver.DataBits;
import com.monitor.ttl.driver.PL2303driver.FlowControl;
import com.monitor.ttl.driver.PL2303driver.Parity;
import com.monitor.ttl.driver.PL2303driver.StopBits;
import com.nbg.monitor.NotificationService;

public abstract class TTLService extends NotificationService {
	private PL2303driver driver;
	private Handler handler;

	public TTLService(int notfication_id, String config_name) {
		super(notfication_id, config_name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		HandlerThread thread = new HandlerThread("thread");
		thread.start();
		handler = new Handler(thread.getLooper());
		PL2303callback callback = new MyPL2303callback(this);
		driver = new PL2303driver(getApplicationContext(), callback);
		List<UsbDevice> devices = driver.getDeviceList();
		System.out.println("devices size:" + devices.size());
		UsbDevice usbDevice = devices.get(index());
		System.out.println(usbDevice.getDeviceId());
		System.out.println(usbDevice.getProductId());
		System.out.println(usbDevice.getVendorId());
		System.out.println(usbDevice.getDeviceName());
		try {
			driver.open(usbDevice);
			String notify = "connected DeviceId:" + usbDevice.getDeviceId();
			getMonitorConfig().edit().putString("device", usbDevice.getDeviceName())
					.commit();
			System.out.println(notify);
			notify("ttl", notify);
		} catch (PL2303Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.hasExtra("ttl_switch")) {
			final boolean value = intent.getBooleanExtra("ttl_switch", false);
			if (driver != null) {
				System.out.println("drive:" + driver.isConnected() + " value:"
						+ value);
			} else {
				System.out.println("drive is null");
			}
			if (driver != null && driver.isConnected()) {
				handler.post(new Runnable() {

					@Override
					public void run() {
						for (int i = 0; i < 5; i++) {
							try {
								driver.setup(BaudRate.B75, DataBits.D8,
										StopBits.S2, Parity.ODD,
										FlowControl.OFF);
								driver.setRTS(value);
								driver.setDTR(value);
								if (value) {
									TTLService.this.notify("开关", "动态开启");
								} else {
									TTLService.this.notify("开关", "动态关闭");
								}
								break;
							} catch (PL2303Exception e) {
								e.printStackTrace();
							}
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	public abstract int index();
}
