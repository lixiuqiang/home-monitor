package com.monitor.ttl;

import java.util.List;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.Menu;

import com.monitor.ttl.driver.PL2303Exception;
import com.monitor.ttl.driver.PL2303callback;
import com.monitor.ttl.driver.PL2303driver;
import com.monitor.ttl.driver.PL2303driver.BaudRate;
import com.monitor.ttl.driver.PL2303driver.DataBits;
import com.monitor.ttl.driver.PL2303driver.FlowControl;
import com.monitor.ttl.driver.PL2303driver.Parity;
import com.monitor.ttl.driver.PL2303driver.StopBits;

public class MainActivity extends Activity {
	
	MailSender mailSender=new MailSender();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PL2303callback callback = new PL2303callback() {

			@Override
			public void onRI(boolean state) {
				if(state){
					System.out.println("1号位检测到有人离开");
				}else{
					System.out.println("1号位检测到有人闯入");					
				}
				try {
					mailSender.sendMail("有人闯入", "有人闯入", "nibaogang@163.com" , "13920294304@139.com");
				} catch (Exception e) {
					// TODO Auto-generated catch block
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
				if(state){
					System.out.println("2号位检测到有人离开");
				}else{
					System.out.println("2号位检测到有人闯入");					
				}
			}

			@Override
			public void onCTS(boolean state) {
				// TODO Auto-generated method stub

			}
		};
		final PL2303driver driver = new PL2303driver(getApplicationContext(),
				callback);
		List<UsbDevice> devices = driver.getDeviceList();
		System.out.println("devices size:" + devices.size());
		try {
			driver.open(devices.get(0));
			new Thread() {
				public void run() {
					try {
						while (!driver.isConnected()) {
							Thread.sleep(1000);
							System.out.println("wait for connect");
						}
						try{
							driver.setup(BaudRate.B75, DataBits.D8, StopBits.S2,
									Parity.ODD, FlowControl.OFF);
						}catch(Exception e){
							e.printStackTrace();
						}
						for (int i = 0; i < 3; i++) {							
							try{								
								driver.setRTS(true);
								driver.setDTR(true);
							}catch(Exception e){
								e.printStackTrace();
							}
							System.out.println("rts true");
							Thread.sleep(5000);
							try{								
								driver.setRTS(false);
								driver.setDTR(false);
							}catch(Exception e){
								e.printStackTrace();
							}							
							System.out.println("rts false");
							Thread.sleep(5000);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
			}.start();
		} catch (PL2303Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
