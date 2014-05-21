package com.nbg.file;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		System.out.println("nbg:BootReceiver");
		SharedPreferences config_1 = context.getSharedPreferences("config_1", Context.MODE_PRIVATE);
		if(config_1.getBoolean("enable", true)){			
			MainActivity.handler(true, context, MonitorService1.class);
		}
		SharedPreferences config_2 = context.getSharedPreferences("config_2", Context.MODE_PRIVATE);
		if(config_2.getBoolean("enable", true)){			
			MainActivity.handler(true, context, MonitorService2.class);
		}
	}

}
