package com.monitor.ttl;

import java.util.HashMap;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;

import com.monitor.ttl.R;

public class MainActivity extends Activity {
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private UsbManager manager;
	private byte[] outBytes = "adfasdf".getBytes();
	private byte[] inBytes = new byte[outBytes.length];
	private static int TIMEOUT = 3000;

	private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			System.out.println("action:" + action);
			if (ACTION_USB_PERMISSION.equals(action)) {
				System.out.println("get permission");
				final UsbDevice device = (UsbDevice) intent
						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				new Thread() {
					public void run() {
						UsbInterface intf = device.getInterface(0);
						System.out.println("nbg:" + intf.getInterfaceClass()
								+ "  " + intf.getInterfaceProtocol() + " "
								+ intf.getEndpointCount());
						UsbEndpoint inEndpoint = intf.getEndpoint(2);
						UsbEndpoint outEndpoint = intf.getEndpoint(1);
						System.out.println(inEndpoint.getDirection() + "  "
								+ outEndpoint.getDirection());
						UsbDeviceConnection connection = manager
								.openDevice(device);
						connection.claimInterface(intf, true);
						int a = connection.bulkTransfer(outEndpoint, outBytes,
								outBytes.length, TIMEOUT);
						System.out.println("write length:" + a);
						int b = connection.bulkTransfer(inEndpoint, inBytes,
								inBytes.length, TIMEOUT);
						System.out.println("read length:" + b);
						System.out.println("read:" + new String(inBytes, 0, b));
					};
				}.start();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		UsbDevice ttlDevice = null;
		for (UsbDevice device : deviceList.values()) {
			int pid = device.getProductId();
			int vid = device.getVendorId();
			if (vid == 1659 && pid == 8963) {
				ttlDevice = device;
				System.out.println("found devices:{}" + device);
			}
		}
		if (ttlDevice != null) {
			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			registerReceiver(mUsbReceiver, filter);
			PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,
					0, new Intent(ACTION_USB_PERMISSION), 0);
			manager.requestPermission(ttlDevice, mPermissionIntent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
