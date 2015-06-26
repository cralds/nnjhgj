package com.eyingsoft.service;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * 业务Bean，实现对数据的操作
 * 
 * @author .Fatty
 */
public class FileService {
	private DBOpenHelper openHelper; // 声明数据库管理器

	public FileService(Context context) {
		openHelper = new DBOpenHelper(context); // 根据上下文对象实例化数据库管理器
	}

	/**
	 * 获取特定URI的每条线程已经下载的文件长度
	 */
	public Map<Integer, Integer> getData(String path) {
		SQLiteDatabase db = openHelper.getReadableDatabase();
		// 根据下载路径查询所有线程下载数据，返回的Cursor指向第一条记录之前
		Cursor cursor = db.rawQuery("select threadid, downlength from"
				+ " filedownlog where downpath=?", new String[] { path });
		// 建立一个哈希表用于存放每条线程的已经下载的文件长度
		Map<Integer, Integer> data = new HashMap<Integer, Integer>();
		// 从第一条记录开始开始遍历Cursor对象
		while (cursor.moveToNext()) {
			// 把线程id和该线程已下载的长度设置进data哈希表中
			data.put(cursor.getInt(0), cursor.getInt(1));
			data.put(cursor.getInt(cursor.getColumnIndexOrThrow("threadid")),
					cursor.getInt(cursor.getColumnIndexOrThrow("downlength")));
		}
		cursor.close();
		db.close();
		return data;
	}

	/**
	 * 保存每条线程已经下载的文件长度
	 * 
	 * @param path
	 *            下载的路径
	 * @param map
	 *            现在的id和已经下载的长度的集合
	 */
	public void save(String path, Map<Integer, Integer> map) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		// 开始事务，因为此处要插入多批数据
		db.beginTransaction();
		try {
			for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
				// 插入特定下载路径特定线程ID已经下载的数据
				db.execSQL("insert into filedownlog"
						+ "(downpath, threadid, downlength) values(?,?,?)",
						new Object[] { path, entry.getKey(), entry.getValue() });
			}
			// 设置事务执行的标志为成功
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		db.close();
	}

	/**
	 * 实时更新每条线程已经下载的文件长度
	 * 
	 * @param path
	 * @param map
	 */
	public void update(String path, int threadId, int pos) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		// 更新特定下载路径下特定线程已经下载的文件长度
		db.execSQL("update filedownlog set downlength=? "
				+ "where downpath=? and threadid=?", new Object[] { pos, path,
				threadId });
		db.close();
	}

	/**
	 * 当文件下载完成后，删除对应的下载记录
	 * 
	 * @param path
	 */
	public void delete(String path) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		// 删除特定下载路径的所有线程记录
		db.execSQL("delete from filedownlog where downpath=?",
				new Object[] { path });
		db.close();
	}

}
