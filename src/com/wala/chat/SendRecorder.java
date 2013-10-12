package com.wala.chat;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SendRecorder {
	private Context context;
	private Button button;
	/**录音的状态*/
	private int recode_state = 0;
	/**不在录音的状态*/
	private static final int RECORD_NO = 0;
	/**正在录音 */
	private static final int RECORD_ING = 1;
	/**录音完成*/
	private static final int RECORD_END = 2;
	/**录音的时间*/
	private static float recodeTime=0.0f;    
	/**麦克风获取的音量值*/
	private static double voiceValue=0.0; 
	/**最长录制时间，单位秒，0为无时间限制*/
	private static int MAX_TIME = 15;
	/**最短录制时间，单位秒，0为无时间限制，建议设为1*/
	private static int MIX_TIME = 1;
	/**录音达到了最大状态的消息*/
	private static final int RECORD_STOP = 0x1;
	/**改变录音时的图片的消息*/
	private static final int RECORD_IMAGE_STATE = 0x2;
	/**录音存放的位置*/
	private String path;
	
	private ImageView dialogImage; 
	
	private Dialog dialog;
	
	private Thread recodeThread = null;
	
	private MediaRecorder recorder = null;
	public SendRecorder(Context _context, Button _button) {
		context = _context;
		button = _button;
	}
	
	public void setButtonListener() {
		button.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if(recode_state != RECORD_ING) {
						showVoiceDialog();
						recode_state = RECORD_ING;
						try {
							path = Environment.getExternalStorageDirectory().getPath() + "/walachat/";
							startRecode(path);
							startThread();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					break;

				case MotionEvent.ACTION_UP:
					if(recode_state == RECORD_ING) {
						recode_state = RECORD_END;
						if(dialog != null && dialog.isShowing()) {
							dialog.dismiss();
						}
						stopRecode();
						voiceValue = 0.0f;
						if(recodeTime < MIX_TIME) {
							showWarnToast();
							recode_state = RECORD_NO;
						}
						/**这里录音完成*/
					}
					break;
				}
				return false;
			}
		});
	}
	
	/**
	 * 开始录音
	 * @param path 音频文件存放的位置
	 * @throws IOException
	 */
	public void startRecode(String path) throws IOException {
		path = path + UUID.randomUUID().toString() + ".amr";
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			File directory = new File(path).getParentFile();
			if (!directory.exists() && !directory.mkdirs()) { 
				throw new IOException("Path to file could not be created");
			}
			this.path = path;
			recorder = new MediaRecorder();
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			recorder.setAudioSamplingRate(8000);
			recorder.setOutputFile(path);
			recorder.prepare();
			recorder.start();
		}else {
			Toast.makeText(context, "sd卡不可用", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * 停止录音
	 */
	public void stopRecode() {
		if(recorder != null) {
			recorder.stop();
			recorder.release();
			recorder = null;
		}
	}
	
	/**
	 * 录音计时线程
	 */
	private void startThread() {
		recodeThread = new Thread(new RecodeRunnable());
		recodeThread.start();
	}
	
	public void showVoiceDialog() {
		dialog = new Dialog(context, R.style.DialogStyle);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		dialog.setContentView(R.layout.my_dialog);
		dialogImage = (ImageView) dialog.findViewById(R.id.dialog_img);
		dialog.show();
	}
	
	/***
	 * 获取录音的时间总长
	 * @return
	 */
	public int getRecoderTime() {
		return (int) recodeTime; 
	}
	/***
	 * 获取录音的存储位置
	 * @return
	 */
	public String getAudioPath() {
		return path;
	}
	
	private class RecodeRunnable implements Runnable {

		@Override
		public void run() {
			recodeTime = 0.0f;
			while(recode_state == RECORD_ING) {
				if(recodeTime >= MAX_TIME && MAX_TIME != 0) {
					/**录音的时间到达了最大,停止录音*/
					recodeHandler.sendEmptyMessage(RECORD_STOP);
				}else {
					try {
						Thread.sleep(200);
						recodeTime += 0.2;
						if(recode_state == RECORD_ING) {
							if(recorder != null) {
								voiceValue = recorder.getMaxAmplitude();
							}
							/**发送消息更新录音时的图片状态*/
							recodeHandler.sendEmptyMessage(RECORD_IMAGE_STATE);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	//录音时间太短时Toast显示
	public void showWarnToast(){
		Toast toast = new Toast(context);
		 LinearLayout linearLayout = new LinearLayout(context);
		 linearLayout.setOrientation(LinearLayout.VERTICAL); 
		 linearLayout.setPadding(20, 20, 20, 20);
		
		// 定义一个ImageView
		 ImageView imageView = new ImageView(context);
		 imageView.setImageResource(R.drawable.voice_to_short); // 图标
		 
		 TextView mTv = new TextView(context);
		 mTv.setText("时间太短   录音失败");
		 mTv.setTextSize(14);
		 mTv.setTextColor(Color.WHITE);//字体颜色
		 //mTv.setPadding(0, 10, 0, 0);
		 
		// 将ImageView和ToastView合并到Layout中
		 linearLayout.addView(imageView);
		 linearLayout.addView(mTv);
		 linearLayout.setGravity(Gravity.CENTER);//内容居中
		 linearLayout.setBackgroundResource(R.drawable.record_bg);//设置自定义toast的背景
		 
		 toast.setView(linearLayout); 
		 toast.setGravity(Gravity.CENTER, 0,0);//起点位置为中间     100为向下移100dp
		 toast.show();				
	}
	
	//录音Dialog图片随声音大小切换
	public void setDialogImage(){
		if (voiceValue < 200.0) {
			dialogImage.setImageResource(R.drawable.record_animate_01);
		}else if (voiceValue > 200.0 && voiceValue < 400) {
			dialogImage.setImageResource(R.drawable.record_animate_02);
		}else if (voiceValue > 400.0 && voiceValue < 800) {
			dialogImage.setImageResource(R.drawable.record_animate_03);
		}else if (voiceValue > 800.0 && voiceValue < 1600) {
			dialogImage.setImageResource(R.drawable.record_animate_04);
		}else if (voiceValue > 1600.0 && voiceValue < 3200) {
			dialogImage.setImageResource(R.drawable.record_animate_05);
		}else if (voiceValue > 3200.0 && voiceValue < 5000) {
			dialogImage.setImageResource(R.drawable.record_animate_06);
		}else if (voiceValue > 5000.0 && voiceValue < 7000) {
			dialogImage.setImageResource(R.drawable.record_animate_07);
		}else if (voiceValue > 7000.0 && voiceValue < 10000.0) {
			dialogImage.setImageResource(R.drawable.record_animate_08);
		}else if (voiceValue > 10000.0 && voiceValue < 14000.0) {
			dialogImage.setImageResource(R.drawable.record_animate_09);
		}else if (voiceValue > 14000.0 && voiceValue < 17000.0) {
			dialogImage.setImageResource(R.drawable.record_animate_10);
		}else if (voiceValue > 17000.0 && voiceValue < 20000.0) {
			dialogImage.setImageResource(R.drawable.record_animate_11);
		}else if (voiceValue > 20000.0 && voiceValue < 24000.0) {
			dialogImage.setImageResource(R.drawable.record_animate_12);
		}else if (voiceValue > 24000.0 && voiceValue < 28000.0) {
			dialogImage.setImageResource(R.drawable.record_animate_13);
		}else if (voiceValue > 28000.0) {
			dialogImage.setImageResource(R.drawable.record_animate_14);
		}
	}
	
	private Handler recodeHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case RECORD_STOP:
				if(recode_state == RECORD_ING) {
					recode_state = RECORD_END;
					if(dialog != null && dialog.isShowing()) {
						dialog.dismiss();
					}
					stopRecode();
					voiceValue = 0.0f;
					if (recodeTime < 1.0) {
						/**录音时间太短,提示*/
						showWarnToast();
					}else {
						/**录音完成*/
					}
				}
				break;

			case RECORD_IMAGE_STATE:
				setDialogImage();
				break;
			}
			super.handleMessage(msg);
		}
		
	};
}