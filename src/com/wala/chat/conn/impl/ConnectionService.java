package com.wala.chat.conn.impl;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ConnectionService extends IntentService {
	public ConnectionService() {
		super("");
		// TODO Auto-generated constructor stub
	}

	@Override
	public IBinder onBind(Intent intent) {

		System.out.println("on bind");
		return new MyBinder();
	}

	
	public class MyBinder extends Binder implements IService{

		@Override
		public void initConnection() {
			// TODO Auto-generated method stub
			sayHelloInService();
		}

		
	}
	
	
	
	/**
	 * 服务里面的一个方法 
	 */
	public void sayHelloInService(){
		System.out.println("hello in service");
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		System.out.println("on  unbind");
		return super.onUnbind(intent);
	}




	@Override
	public void onCreate() {
		System.out.println("oncreate");
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		System.out.println("onstart");
		
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		System.out.println("ondestroy");
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		
	}
}
