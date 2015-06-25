package com.eyingsoft.service;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite管理器，实现创建数据库和表，但版本变化时实现对表的数据库表的操作
 * 
 * @author .Fatty
 * 
 */
public class DBOpenHelper extends SQLiteOpenHelper {
	private static final String DBNAME = "downloader.db";
	private static final int VERSION = 1;

	/**
	 * 通过构造方法
	 * 
	 * @param context    应用程序的上下文对象
	 *          
	 */
	public DBOpenHelper(Context context) {
		super(context, DBNAME, null, VERSION);
	}

	/**
	 * 建立数据表 filedownlog 文件下载数据表
	 *  id 自动递增
	 *  downpath 文件存储的路径
	 *  threadid 线程的ID
	 * downlength 每个线程控制文件下载的长度
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS filedownlog (id integer primary key autoincrement, downpath varchar(100), threadid INTEGER, downlength INTEGER)");
	}

	/**
	 * 当版本变化时系统会调用该回调方法
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS filedownlog");
		onCreate(db);
	}

}
