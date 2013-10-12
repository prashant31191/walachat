package com.wala.chat.conn;

public interface XmppConnection {

	void initConnection();
	
	void login(String username, String password);
	void sendTextMessage();
}
