package com.nbg.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MonitorDB extends SQLiteOpenHelper {
	private static MonitorDB db;
	private static final String TABLE_FINISH = "monitor_finish";
	private static final String COLUMN_DAY = "_day";
	private static final String COLUMN_CLIENT_IP = "_client_ip";
	private static final String COLUMN_NUM = "_num";
	private static final String COLUMN_PRODUCT = "_product";
	private static final String COLUMN_SOFT_VERSION = "_soft_version";
	private static final String COLUMN_PACKAGE = "_package";
	private static final String COLUMN_IMEI = "_imei";
	private static final String COLUMN_TIME = "_time";
	private static final String WHERE = COLUMN_DAY + "=? and "
			+ COLUMN_CLIENT_IP + "=?";
	private static final String CREATE_MONITOR = "create table if not exists "
			+ TABLE_FINISH + " (" + COLUMN_DAY + " int," + COLUMN_CLIENT_IP
			+ " varchar(20)," + COLUMN_NUM + " int," + COLUMN_PRODUCT
			+ " varchar(64)," + COLUMN_SOFT_VERSION + " varchar(20),"
			+ COLUMN_PACKAGE + " varchar(32)," + COLUMN_IMEI + " varchar(20),"
			+ COLUMN_TIME + " int,primary key (" + COLUMN_DAY + ","
			+ COLUMN_CLIENT_IP + "))";

	public MonitorDB(Context context) {
		super(context, "monitor", null, 1);
		MonitorDB.db = this;
	}

	public static MonitorDB getInstance() {
		return db;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		System.out.println(CREATE_MONITOR);
		db.execSQL(CREATE_MONITOR);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	private void insert(FinishBean finishBean) {
		try {
			ContentValues values = new ContentValues();
			values.put(COLUMN_DAY, finishBean.getDay());
			values.put(COLUMN_CLIENT_IP, finishBean.getClient_ip());
			values.put(COLUMN_NUM, finishBean.getNum());
			values.put(COLUMN_PRODUCT, finishBean.getProduct());
			values.put(COLUMN_SOFT_VERSION, finishBean.getSoft_version());
			values.put(COLUMN_PACKAGE, finishBean.getPkg());
			values.put(COLUMN_IMEI, finishBean.getImei());
			values.put(COLUMN_TIME, finishBean.getTime());
			getWritableDatabase().insert(TABLE_FINISH, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean queryOrInsert(FinishBean finishBean) {
		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().query(
					TABLE_FINISH,
					new String[] { COLUMN_NUM, COLUMN_PRODUCT,
							COLUMN_SOFT_VERSION, COLUMN_PACKAGE, COLUMN_IMEI,
							COLUMN_TIME },
					WHERE,
					new String[] { finishBean.getDay() + "",
							finishBean.getClient_ip() }, null, null, null);
			if (cursor.moveToFirst()) {
				finishBean.setNum(cursor.getInt(0));
				finishBean.setProduct(cursor.getString(1));
				finishBean.setSoft_version(cursor.getString(2));
				finishBean.setPkg(cursor.getString(3));
				finishBean.setImei(cursor.getString(4));
				finishBean.setTime(cursor.getLong(5));
			} else {
				insert(finishBean);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public List<FinishBean> load() {
		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().query(
					TABLE_FINISH,
					new String[] { COLUMN_DAY, COLUMN_CLIENT_IP, COLUMN_NUM,
							COLUMN_PRODUCT, COLUMN_SOFT_VERSION,
							COLUMN_PACKAGE, COLUMN_IMEI, COLUMN_TIME }, null,
					null, null, null, COLUMN_DAY + " desc," + COLUMN_CLIENT_IP);
			List<FinishBean> finishBeans = new ArrayList<FinishBean>();
			while (cursor.moveToNext()) {
				FinishBean finishBean = new FinishBean();
				finishBean.setDay(cursor.getInt(0));
				finishBean.setClient_ip(cursor.getString(1));
				finishBean.setNum(cursor.getInt(2));
				finishBean.setProduct(cursor.getString(3));
				finishBean.setSoft_version(cursor.getString(4));
				finishBean.setPkg(cursor.getString(5));
				finishBean.setImei(cursor.getString(6));
				finishBean.setTime(cursor.getLong(7));
				finishBeans.add(finishBean);
			}
			return finishBeans;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void update(FinishBean finishBean) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NUM, finishBean.getNum());
		values.put(COLUMN_PRODUCT, finishBean.getProduct());
		values.put(COLUMN_SOFT_VERSION, finishBean.getSoft_version());
		values.put(COLUMN_PACKAGE, finishBean.getPkg());
		values.put(COLUMN_IMEI, finishBean.getImei());
		values.put(COLUMN_TIME, finishBean.getTime());
		getWritableDatabase().update(
				TABLE_FINISH,
				values,
				WHERE,
				new String[] { finishBean.getDay() + "",
						finishBean.getClient_ip() });
	}
}
