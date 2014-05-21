package com.nbg.file;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MainService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		System.out.println("=============onCreate=============");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			System.out.println("=============onStart=============");
			zhengli("/mnt/sda1/HOME/");
			zhengli("/mnt/sda2/HOME/");
			zhengli("/mnt/sdb1/HOME/");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void zhengli(String path) {
		File dir = new File(path);
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				if (file.isFile()) {
					long time = file.lastModified();
					if (System.currentTimeMillis() - time < 5000) {
						continue;
					}
					String dirPath = new SimpleDateFormat("MMdd/HH/mm/",
							Locale.CHINA).format(new Date(time));
					File desc = new File(path + dirPath + file.getName());
					System.out.println(desc);
					desc.getParentFile().mkdirs();
					file.renameTo(desc);
				}
			}
		}
	}
}
