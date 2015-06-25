package com.demo.demo24;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eyingsoft.service.downloader.DownloadProgressListener;
import com.eyingsoft.service.downloader.FileDownloader;

public class DownloadActivity extends Activity implements OnClickListener {

	private TextView tv;
	private Button start, stop;
	private ProgressBar progressBar;
	private static final int DPWNLOAD_FAIL = 0;
	private static final int DOWNLOAD_SUCCESS = 1;
	private static final int PROGRESSING = 2;
	private TextView resultView;
	private DownloadTask task;
	private int downloadThreads = 3;
	private Handler myhandler = new UIHandler();
	String fileName;
	String url;

	/**
	 * 创建UIHandler 内部类
	 * 
	 * @author .Fatty
	 */
	private final class UIHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// TODO 创建一个UI线程
			switch (msg.what) {
			case DOWNLOAD_SUCCESS:
				// 下载成功后
				break;

			case 2:
				int size = msg.getData().getInt("size");
				progressBar.setProgress(size);
				float num = (float) progressBar.getProgress()
						/ (float) progressBar.getMax();
				int result = (int) (num * 100);
				resultView.setText(result + "%");// 显示下载进度
				if (progressBar.getProgress() == progressBar.getMax()) {
					Toast.makeText(getApplicationContext(), "下载完成",
							Toast.LENGTH_SHORT).show();
					return;
				}
				break;

			case 0:
				Toast.makeText(getApplicationContext(), "下载失败",
						Toast.LENGTH_SHORT).show();
				break;

			default:
				break;
			}

			super.handleMessage(msg);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);

		initView();
		initDate();

	}

	private void initDate() {
		Intent intent = getIntent();
		fileName = intent.getStringExtra("fileName");
		url = intent.getStringExtra("url");
		if (fileName != null) {
			tv.setText(fileName);
		} else {
			tv.setText("文件名称无效!");
		}
	}

	private void initView() {
		start = (Button) findViewById(R.id.start);
		stop = (Button) findViewById(R.id.stop);
		resultView = (TextView) findViewById(R.id.resultView);
		tv = (TextView) findViewById(R.id.fileName);
		progressBar = (ProgressBar) findViewById(R.id.progress);
		start.setOnClickListener(this);
		stop.setOnClickListener(this);
	}

	/**
	 * 开始下载,停止下载
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start:
			start();
			break;
		case R.id.stop:
			stop();
			break;

		default:
			break;
		}
	}

	/**
	 * 暂停下载
	 */
	private void stop() {
		stopDownloading();
		start.setEnabled(true);
		stop.setEnabled(false);
		return;
	}

	private void stopDownloading() {
		if (task != null) {
			task.stopDownloading();
		}

	}

	/**
	 * 开始下载
	 */
	private void start() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			File saveDir = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
			getExternalFilesDir(Environment.DIRECTORY_MOVIES);
			Toast.makeText(getApplicationContext(), "开始下载...",
					Toast.LENGTH_SHORT).show();
			download(url, saveDir);
		} else {
			if (progressBar.getProgress() == progressBar.getMax()) {
				Toast.makeText(getApplicationContext(), "SDcard不存在",
						Toast.LENGTH_SHORT).show();
				return;
			}
		}
		start.setEnabled(false);
		stop.setEnabled(true);
		return;
	}
	/**
	 * 执行下载
	 * 
	 * @param url2
	 * @param saveDir
	 */
	private void download(String url2, File saveDir) {
		task = new DownloadTask(url2, saveDir);
		new Thread(task).start();

	}

	/**
	 * DownloadTask 下载任务
	 * 
	 * @author
	 */
	class DownloadTask implements Runnable {

		private String url;
		private File saveDir;
		private FileDownloader downloader;

		public DownloadTask(String url, File saveDir) {
			this.url = url;
			this.saveDir = saveDir;
		}

		public void stopDownloading() {
			if (downloader != null) {
				downloader.stopDownloading();
			}
		}

		@Override
		public void run() {
			try {
				downloader = new FileDownloader(getApplicationContext(), url,
						saveDir, downloadThreads);
				progressBar.setMax(downloader.getFileSize());
				downloader.download(dpListener);
			} catch (Exception e) {
				// TODO: 线程抛出的异常
				e.printStackTrace();
				myhandler.sendMessage(myhandler.obtainMessage(DPWNLOAD_FAIL));
			}

		}
	}

	/**
	 * 进度条更新UI线程
	 */
	DownloadProgressListener dpListener = new DownloadProgressListener() {

		@Override
		public void onDownloadSize(int size) {
			// TODO 下载进度条更新
			Message message = new Message();
			message.what = PROGRESSING;
			message.getData().putInt("size", size);// 发送下载的进度，告诉Handler，更新UI线程
			myhandler.sendMessage(message);
		}
	};

}
