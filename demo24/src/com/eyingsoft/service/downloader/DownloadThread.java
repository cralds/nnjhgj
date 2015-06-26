package com.eyingsoft.service.downloader;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

/**
 * 下载线程的实现
 * 
 * 下载线程，根据具体下载地址、保持到的文件、下载块的大小、已经下载的数据大小等信息进行下载
 * 
 * @author .Fatty
 */
public class DownloadThread extends Thread {

	private static final String TAG = "ThreadDownloader......";

	private FileDownloader downloader;
	private URL downUrl;
	private File saveFile;
	private int block;
	private int downloadedLength;
	private int threadId = -1;

	private boolean finished = false;

	/**
	 * 构造方法
	 * 
	 * @param downloader
	 *            文件下载的FileDownloader对象
	 * @param downUrl
	 *            所下载文件的Url
	 * @param saveFile
	 *            将文件保存的路径
	 * @param block
	 *            给该线程分配的下载的长度
	 * @param downloadedLength
	 *            该线程正在的下载的长度 if(downloadLength<block) 说明未下载完成 else 下载完成
	 * @param threadId
	 *            该线程的ID标识
	 */
	public DownloadThread(FileDownloader downloader, URL downUrl,
			File saveFile, int block, int downloadedLength, int threadId) {
		this.downUrl = downUrl;
		this.saveFile = saveFile;
		this.block = block;
		this.downloader = downloader;
		this.threadId = threadId;
		this.downloadedLength = downloadedLength;
	}

	@Override
	public void run() {
		if (downloadedLength < block) {// 如果该条线程为下载完成
			try {
				HttpURLConnection http = (HttpURLConnection) downUrl
						.openConnection();
				http.setConnectTimeout(5 * 1000);
				http.setRequestMethod("GET");
				http.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg,application/x-shockwave-flash, application/xaml+xml,application/vnd.ms-xpsdocument, application/x-ms-xbap,application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword");
			//	http.setRequestProperty(, null); // 设置客户端可以接受的返回数据类型
				
				  
				 
				http.setRequestProperty("Accept-Language", "zh-CN");
				http.setRequestProperty("Referer", downUrl.toString());// 设置请求的来源，便于对访问来源进行统计
				http.setRequestProperty("Charset", "UTF-8");

				// 计算该线程下载的开始位置
				int startPos = block * (threadId - 1) + downloadedLength;
				// 计算该线程下载的结束位置
				int endPos = block * threadId - 1;

				// 设置获取实体数据的范围,如果超过了实体数据的大小会自动返回实际的数据大小
				http.setRequestProperty("Range", "bytes=" + startPos + "-"
						+ endPos);

				// 客户端用户代理
				http.setRequestProperty("User-Agent",
						"Mozilla/4.0 (compatible; MSIE 8.0;"
								+ " Windows NT 5.2; Trident/4.0;"
								+ " .NET CLR 1.1.4322;"
								+ " .NET CLR 2.0.50727;"
								+ " .NET CLR 3.0.04506.30;"
								+ " .NET CLR 3.0.4506.2152;"
								+ " .NET CLR 3.5.30729)");

				// 使用长连接
				http.setRequestProperty("Connection", "Keep-Alive");
				// 获取远程连接的输入流
				InputStream inStream = http.getInputStream();
				// 设置本地数据缓存的大小为1M
				byte[] buffer = new byte[1024];
				// 设置每次读取的数据量
				int offset = 0;
				// 打印该线程开始下载的位置
				print("Thread " + this.threadId
						+ " starts to download from position " + startPos);
				RandomAccessFile threadFile = new RandomAccessFile(
						this.saveFile, "rwd");
				// 文件指针指向开始下载的位置
				threadFile.seek(startPos);
				// 但用户没有要求停止下载，同时没有到达请求数据的末尾时候会一直循环读取数据
				// 直接把数据写到文件中
				while (!downloader.getStopDownloading()
						&& (offset = inStream.read(buffer, 0, 1024)) != -1) {
					threadFile.write(buffer, 0, offset);
					downloadedLength += offset;
					// 把该线程已经下载的数据长度更新到数据库和内存哈希表中
					downloader.update(this.threadId, downloadedLength);
					// 把新下载的数据长度加入到已经下载的数据总长度中
					downloader.append(offset);
				}
				threadFile.close();
				inStream.close();

				if (downloader.getStopDownloading()) {
					print("Thread " + this.threadId + " has been paused");
				} else {
					print("Thread " + this.threadId + " download finish");
				}

				this.finished = true; // 设置完成标志为true，无论是下载完成还是用户主动中断下载
			} catch (Exception e) {
				// 设置该线程已经下载的长度为-1
				this.downloadedLength = -1;
				print("Thread " + this.threadId + ":" + e);
			}
		}
	}

	private static void print(String msg) {
		Log.i(TAG, msg);
	}

	/**
	 * 下载是否完成
	 * 
	 * @return true or false
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * 已经下载的内容大小
	 * 
	 * @return 如果返回值为-1,代表下载失败
	 */
	public long getDownloadedLength() {
		return downloadedLength;
	}
}
