package com.nbg.file;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		MainActivity.handler(true, context, MonitorService1.class);
		MainActivity.handler(true, context, MonitorService2.class);
	}

}
