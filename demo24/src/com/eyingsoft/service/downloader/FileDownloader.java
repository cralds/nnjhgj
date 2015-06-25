package com.eyingsoft.service.downloader;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.eyingsoft.service.FileService;

public class FileDownloader {
	private static final String TAG = "FileDownloader......";
	private static final int RESPONSE_OK = 200;
	private Context context;
	private FileService fileService;
	private boolean stopDownloaded; // 停止下载标志
	private int downloadedSize = 0; // 已下载文件长度
	private int fileSize = 0; // 原始文件长度
	private DownloadThread[] threads; // 根据线程数设置下载线程池
	private File saveFile; // 数据保存到的本地文件
	// 缓存各线程下载的长度
	private Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();
	private int block; // 每条线程下载的长度
	private String downloadUrl; // 下载路径

	/**
	 * 构建文件下载器
	 * 
	 * @param downloadUrl
	 *            下载路径
	 * @param fileSaveDir
	 *            文件保存目录
	 * @param threadNum
	 *            下载线程数
	 */
	public FileDownloader(Context context, String downloadUrl,
			File fileSaveDir, int threadNum) {
		try {
			this.context = context;
			this.downloadUrl = downloadUrl;
			fileService = new FileService(this.context);
			URL url = new URL(this.downloadUrl);
			if (!fileSaveDir.exists())
				fileSaveDir.mkdirs();
			this.threads = new DownloadThread[threadNum];

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg,application/x-shockwave-flash, application/xaml+xml,application/vnd.ms-xpsdocument, application/x-ms-xbap,application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword");
			//conn.setRequestProperty("Accept", "application/pdf");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty("Referer", downloadUrl);
			conn.setRequestProperty("Charset", "UTF-8");
			// 设置用户代理
			conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; "
					+ "MSIE 8.0; Windows NT 5.2;"
					+ " Trident/4.0; .NET CLR 1.1.4322;"
					+ ".NET CLR 2.0.50727; " + ".NET CLR 3.0.04506.30;"
					+ " .NET CLR 3.0.4506.2152; " + ".NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.connect();	
			printResponseHeader(conn);
			if (conn.getResponseCode() == RESPONSE_OK) {
				this.fileSize = conn.getContentLength();
				// 当文件大小为小于等于零时抛出运行时异常
				if (this.fileSize <= 0)
					throw new RuntimeException("Unkown file size ");

				
				String filename = getFileName(conn);
				// 根据文件保存目录和文件名构建保存文件
				this.saveFile = new File(fileSaveDir, filename);
				Map<Integer, Integer> logdata = fileService
						.getData(downloadUrl);

				// 如果存在下载记录
				if (logdata.size() > 0) {
					for (Map.Entry<Integer, Integer> entry : logdata.entrySet())
						// 把各条线程已经下载的数据长度放入data中
						data.put(entry.getKey(), entry.getValue());
				}

				// 如果已经下载的数据的线程数和现在设置的线程数相同时则计算所有线程已经下载的数据总长度
				if (this.data.size() == this.threads.length) {
					// 遍历每条线程已经下载的数据
					for (int i = 0; i < this.threads.length; i++) {
						// 计算已经下载的数据之和
						this.downloadedSize += this.data.get(i + 1);
					}
					// 打印出已经下载的数据总和
					print("已经下载的长度" + this.downloadedSize + "个字节");
				}
				// 计算每条线程下载的数据长度
				this.block = (this.fileSize % this.threads.length) == 0 ? this.fileSize
						/ this.threads.length
						: this.fileSize / this.threads.length + 1;
			} else {
				print("服务器响应错误:" + conn.getResponseCode()
						+ conn.getResponseMessage());
				throw new RuntimeException("server response error ");
			}
		} catch (Exception e) {
			print(e.toString());
			throw new RuntimeException("Can't connection this url");
		}
	}

	private boolean isFinish = false;

	public boolean isFinished() {
		return isFinish;
	}

	/**
	 * 获取线程数
	 */
	public int getThreadSize() {
		return threads.length;
	}

	/**
	 * 退出下载
	 */
	public void stopDownloading() {
		this.stopDownloaded = true;
	}

	public boolean getStopDownloading() {
		return this.stopDownloaded;
	}

	/**
	 * 获取文件大小
	 * 
	 * @return
	 */
	public int getFileSize() {
		return fileSize;
	}

	/**
	 * 累计已下载大小
	 * 
	 * @param size
	 */
	protected synchronized void append(int size) { // 使用同步关键字解决并发访问问题
		downloadedSize += size; // 把实时下载的长度加入到总下载长度中
	}

	/**
	 * 更新指定线程最后下载的位置
	 * 
	 * @param threadId
	 *            线程id
	 * @param pos
	 *            最后下载的位置
	 */
	protected synchronized void update(int threadId, int pos) {
		this.data.put(threadId, pos); // 把制定线程ID的线程赋予最新的下载长度，以前的值会被覆盖掉
		this.fileService.update(this.downloadUrl, threadId, pos); // 更新数据库中指定线程的下载长度
	}

	/**
	 * 获取文件名
	 */
	private String getFileName(HttpURLConnection conn) {
		String filename = this.downloadUrl.substring(this.downloadUrl
				.lastIndexOf('/') + 1);

		if (filename == null || "".equals(filename.trim())) {// 如果获取不到文件名称
			for (int i = 0;; i++) { // 无限循环遍历
				String mine = conn.getHeaderField(i); // 从返回的流中获取特定索引的头字段值
				if (mine == null)
					break; // 如果遍历到了返回头末尾这退出循环
				if ("content-disposition".equals(conn.getHeaderFieldKey(i)
						.toLowerCase())) { // 获取content-disposition返回头字段，里面可能会包含文件名
					Matcher m = Pattern.compile(".*filename=(.*)").matcher(
							mine.toLowerCase()); // 使用正则表达式查询文件名
					if (m.find())
						return m.group(1); // 如果有符合正则表达规则的字符串
				}
			}
			filename = UUID.randomUUID() + ".tmp";// 由网卡上的标识数字(每个网卡都有唯一的标识号)以及
													// CPU 时钟的唯一数字生成的的一个 16
													// 字节的二进制作为文件名
		}
		return filename;
	}

	/**
	 * 开始下载文件
	 * 
	 * @param listener
	 *            监听下载数量的变化,如果不需要了解实时下载的数量,可以设置为null
	 * @return 已下载文件大小
	 * @throws Exception
	 */
	public int download(DownloadProgressListener listener) throws Exception { // 进行下载，并抛出异常给调用者，如果有异常的话
		try {
			RandomAccessFile randOut = new RandomAccessFile(this.saveFile,
					"rwd");
			if (this.fileSize > 0)
				randOut.setLength(this.fileSize); // 设置文件的大小
			randOut.close(); // 关闭该文件，使设置生效
			URL url = new URL(this.downloadUrl);
			if (this.data.size() != this.threads.length) { // 如果原先未曾下载或者原先的下载线程数与现在的线程数不一致
				this.data.clear();
				for (int i = 0; i < this.threads.length; i++) { // 遍历线程池
					this.data.put(i + 1, 0);// 初始化每条线程已经下载的数据长度为0
				}
				this.downloadedSize = 0; // 设置已经下载的长度为0
			}
			for (int i = 0; i < this.threads.length; i++) {// 开启线程进行下载
				int downloadedLength = this.data.get(i + 1); // 通过特定的线程ID获取该线程已经下载的数据长度
				if (downloadedLength < this.block
						&& this.downloadedSize < this.fileSize) {// 判断线程是否已经完成下载,否则继续下载
					this.threads[i] = new DownloadThread(this, url,
							this.saveFile, this.block, this.data.get(i + 1),
							i + 1); // 初始化特定id的线程
					this.threads[i].setPriority(7); // 设置线程的优先级，Thread.NORM_PRIORITY
													// = 5 Thread.MIN_PRIORITY =
													// 1 Thread.MAX_PRIORITY =
													// 10
					this.threads[i].start(); // 启动线程
					isFinish = false;
				} else {
					this.threads[i] = null; // 表明在线程已经完成下载任务
					isFinish = true;
				}
			}
			fileService.delete(this.downloadUrl); // 如果存在下载记录，删除它们，然后重新添加
			fileService.save(this.downloadUrl, this.data); // 把已经下载的实时数据写入数据库

			boolean notFinished = true;// 下载未完成
			while (notFinished) {// 循环判断所有线程是否完成下载
				Thread.sleep(900);
				notFinished = false;// 假定全部线程下载完成
				for (int i = 0; i < this.threads.length; i++) {
					if (this.threads[i] != null
							&& !this.threads[i].isFinished()) {// 如果发现线程未完成下载
						notFinished = true;// 设置标志为下载没有完成
						if (this.threads[i].getDownloadedLength() == -1) {// 如果下载失败,再重新在已经下载的数据长度的基础上下载
							this.threads[i] = new DownloadThread(this, url,
									this.saveFile, this.block,
									this.data.get(i + 1), i + 1); // 重新开辟下载线程
							this.threads[i].setPriority(7); // 设置下载的优先级
							this.threads[i].start(); // 开始下载线程
						}
					}
				}
				isFinish = true;
				if (listener != null)
					listener.onDownloadSize(this.downloadedSize);// 通知目前已经下载完成的数据长度
			}
			if (downloadedSize == this.fileSize)
				fileService.delete(this.downloadUrl);// 下载完成删除记录
		} catch (Exception e) {
			print(e.toString()); // 打印错误
			throw new Exception("File downloads error"); // 抛出文件下载异常
		}
		return this.downloadedSize;
	}

	/**
	 * 获取Http响应头字段
	 * 
	 * @param http
	 *            HttpURLConnection对象
	 * @return 返回头字段的LinkedHashMap
	 */
	public static Map<String, String> getHttpResponseHeader(
			HttpURLConnection http) {
		Map<String, String> header = new LinkedHashMap<String, String>(); // 使用LinkedHashMap保证写入和遍历的时候的顺序相同，而且允许空值存在
		for (int i = 0;; i++) { // 此处为无限循环，因为不知道头字段的数量
			String fieldValue = http.getHeaderField(i); // getHeaderField(int
														// n)用于返回 第n个头字段的值。

			if (fieldValue == null)
				break; // 如果第i个字段没有值了，则表明头字段部分已经循环完毕，此处使用Break退出循环
			header.put(http.getHeaderFieldKey(i), fieldValue); // getHeaderFieldKey(int
																// n)用于返回
																// 第n个头字段的键。
		}
		return header;
	}

	/**
	 * 打印Http头字段
	 * 
	 * @param http
	 *            HttpURLConnection对象
	 */
	public static void printResponseHeader(HttpURLConnection http) {
		Map<String, String> header = getHttpResponseHeader(http);
		for (Map.Entry<String, String> entry : header.entrySet()) {
			String key = entry.getKey() != null ? entry.getKey() + ":" : "";
			print(key + entry.getValue()); // 答应键和值的组合
		}
	}

	/**
	 * 打印信息
	 * 
	 * @param msg
	 *            信息字符串
	 */
	private static void print(String msg) {
		Log.i(TAG, msg); // 使用LogCat的Information方式打印信息
	}
}
