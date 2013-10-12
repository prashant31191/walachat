package com.wala.chat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private ListView mListView;
	private Button sendButton;
	private Button sendFileButton;
	private Button addFriends;
	private Button recorderButton;
	private Button playerButton;
	private EditText mEditText;
	private String textContent;
	private List<Msg> listMsgs;
	private XmppUtils utils;
	private Connection connection;
	private MainHandler handler;
	private boolean isLogin = false;
	private String fromJid;
	private SendRecorder sendRecorder;
	MediaRecorder recorder;
//	private MainAdapter adapter;
	private final static int MSG_TEXT_RECEIVED_SUCCESS = 0x1;
	
	private class MainHandler extends Handler {

		@Override
		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_TEXT_RECEIVED_SUCCESS:
				System.out.println("handleMessage");
				Message message = (Message) msg.obj;
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				listMsgs.add(new Msg(message.getFrom(), message.getBody(), format.format(new Date()), "IN"));
//				adapter.notifyDataSetChanged();
				Toast.makeText(MainActivity.this, message.getFrom()+":"+message.getBody(), Toast.LENGTH_SHORT).show();
				break;
				
			case 12:
				System.out.println("login ok");
				Intent intent = new Intent(MainActivity.this, WalaChatService.class);
				MainActivity.this.startService(intent);
				break;

			default:
				break;
			}
		}
		
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		handler = new MainHandler();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//		adapter = new MainAdapter();
		listMsgs = new ArrayList<MainActivity.Msg>();
		mListView = (ListView) findViewById(R.id.formclient_listview);
//		mListView.setAdapter(adapter);
		sendButton = (Button) findViewById(R.id.formclient_btsend);
		sendFileButton = (Button) findViewById(R.id.sendfile);
		addFriends = (Button) findViewById(R.id.addfriends);
		mEditText = (EditText) findViewById(R.id.formclient_text);
		recorderButton = (Button) findViewById(R.id.audio);
		playerButton = (Button) findViewById(R.id.player_audio);
		sendRecorder = new SendRecorder(this, recorderButton);
		mListView.setAdapter(new MainAdapter());
		textContent = mEditText.getText().toString().trim();
		
		utils = XmppUtils.getInstnce();
		connection = utils.getConnection();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				boolean login = utils.login("wanglianjie", "123456");
				if(login) {
					isLogin = true;
					System.out.println("login");
					handler.sendEmptyMessage(12);
					Looper.prepare();
					Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
					Looper.loop();
				
				}else {
					isLogin = false;
					Looper.prepare();
					Toast.makeText(MainActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
					Looper.loop();
				}
				
			}
		}).start();
		setAddFriendsListener();
		setSendButtonListener();
		setSendFileListener();
		receivedFile();
		connection.getChatManager().addChatListener(new MainChatManagerListener());
		
		setRecorderButtonListener();

		setPlayerButtonListener();
	}
	/**
	 * 播放语音
	 */
	public void setPlayerButtonListener() {
		playerButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MediaPlayer player = new MediaPlayer();
				try {
					System.out.println(sendRecorder.getAudioPath());
					player.setDataSource(sendRecorder.getAudioPath());
					player.prepare();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				player.start();
				
			}
		});
	}
	/**
	 * 发送语音
	 */
	private void setRecorderButtonListener() {
		sendRecorder.setButtonListener();
	}

	/**
	 * 发送文件
	 */
	private void setSendFileListener() {
		sendFileButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new Thread(runnable).start();
			}
		});
	}
	/**
	 * 接收文件
	 */
	public void receivedFile() {
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
		SystemClock.sleep(1000);
		if(sdm == null) {
			sdm = new ServiceDiscoveryManager(connection);
		}
		sdm.addFeature("http://jabber.org/protocol/disco#info");
		sdm.addFeature("jabber:iq:privacy");
		FileTransferNegotiator.setServiceEnabled(connection, true);
 		FileTransferManager manager = new FileTransferManager(connection);
		manager.addFileTransferListener(new FileTransferListener() {
			
			@Override
			public void fileTransferRequest(FileTransferRequest request) {
				System.out.println(request.getFileSize());
				System.out.println(request.getMimeType());
				IncomingFileTransfer transfer = request.accept();
				String fileName = transfer.getFileName();
				System.out.println(fileName);
				String fileSavePath = Environment.getExternalStorageDirectory().getPath()+"/walachat/file";
				
				File file = new File(fileSavePath);
				if(!file.exists()) {
					file.mkdirs();
				}
//				File receivedFile = new File(file, fileName);
//				if(!receivedFile.exists()) {
//					try {
//						receivedFile.createNewFile();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
				try {
					System.out.println("接收中...................");
					transfer.recieveFile(new File(Environment.getExternalStorageDirectory().getPath()+"/walachat/file/"+fileName));
				} catch (XMPPException e) {
					e.printStackTrace();
					transfer.getError();
					transfer.getException();
				}
				while (!transfer.isDone()) {
					System.out.println(transfer.getProgress());
				}
				if(transfer.isDone()) {
					Toast.makeText(MainActivity.this, "文件接收完成",Toast.LENGTH_SHORT).show();
				}
				//  拒绝接收的方法 request.reject();
				
			}
		});
	}
	
	private Runnable runnable = new Runnable() {
		
		@Override
		public void run() {
			ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
			if(sdm == null) {
				sdm = new ServiceDiscoveryManager(connection);
			}
			sdm.addFeature("http://jabber.org/protocol/disco#info");
			sdm.addFeature("jabber:iq:privacy");
			FileTransferManager manager = new FileTransferManager(connection);
			FileTransferNegotiator.setServiceEnabled(connection, true);
			String fileTo = connection.getRoster().getPresence("wanglianjie2@127.0.0.1").getFrom();
			OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(fileTo);
			try {
				transfer.sendFile(new File("/storage/sdcard0/梦里水乡.mp3"), "weijian");
			} catch (XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			while(!transfer.isDone()) {
//	            if(transfer.getStatus().equals(Status.error)) {
//	                  System.out.println("ERROR!!! " + transfer.getError());
//	            } else {
//	                  System.out.println(transfer.getStatus());
//	                  System.out.println(transfer.getProgress());
//	            }
//	      }
			
		}
	};
	
	private void setAddFriendsListener() {
		addFriends.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					Roster roster = connection.getRoster();
					roster.createEntry("wanglianjie2@127.0.0.1", "wanglianjie2", null);
				} catch (XMPPException e) {
					e.printStackTrace();
				}
				
			}
		});
	}
	
	private void setSendButtonListener() {
		sendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ChatManager chatManager = connection.getChatManager();
				Chat chat = chatManager.createChat("wanglianjie2@127.0.0.1", new MessageListener() {
					
					@Override
					public void processMessage(Chat chat, Message message) {
						System.out.println(message.getBody());
						System.out.println(message.getFrom());
					}
				});
				try {
					Message msg = new Message();
					msg.setBody(mEditText.getText().toString());
					chat.sendMessage(msg);
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public class MainChatManagerListener implements ChatManagerListener {

		@Override
		public void chatCreated(Chat chat, boolean createdLocally) {
			System.out.println("chat manager listener");
			if(!createdLocally) {
				chat.addMessageListener(new MessageListener() {
					
					@Override
					public void processMessage(Chat chat, Message message) {
						System.out.println(message.getBody());
						System.out.println(message.getFrom());
						System.out.println(Thread.currentThread().getName());
						android.os.Message msg = handler.obtainMessage();
						msg.obj = message;
						msg.what = MSG_TEXT_RECEIVED_SUCCESS;
//						msg.sendToTarget();
						handler.sendMessage(msg);
					}
				});
			}
		}
		
	}
	
	public class Msg {
		public String userName;
		public String message;
		public String data;
		public String form;
		public Msg(String userName, String message, String data, String form) {
			this.userName = userName;
			this.message = message;
			this.data = data;
			this.form = form;
		}
	}
	
	private class MainAdapter extends BaseAdapter {

		private LayoutInflater inflater;

		@Override
		public int getCount() {
			return listMsgs.size();
		}

		@Override
		public Object getItem(int position) {
			return listMsgs.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			System.out.println("getview");
			this.inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if(listMsgs.get(position).form.equals("IN")){
				convertView = this.inflater.inflate(R.layout.formclient_chat_in, null);
			}else{
				convertView = this.inflater.inflate(R.layout.formclient_chat_out, null);
			}
			TextView useridView = (TextView) convertView.findViewById(R.id.formclient_row_userid);
			TextView dateView = (TextView) convertView.findViewById(R.id.formclient_row_date);
			TextView msgView = (TextView) convertView.findViewById(R.id.formclient_row_msg);
			useridView.setText(listMsgs.get(position).userName);
			dateView.setText(listMsgs.get(position).data);
			msgView.setText(listMsgs.get(position).message);
			return convertView;
		}
		
	}

}
