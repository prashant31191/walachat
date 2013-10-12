package com.wala.chat;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.bytestreams.ibb.provider.CloseIQProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.DataPacketProvider;
import org.jivesoftware.smackx.bytestreams.ibb.provider.OpenIQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
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
import org.jivesoftware.smack.XMPPException;

public class XmppUtils {

	private static  XmppUtils utils;
	private static Connection connection;
	
	private XmppUtils(){
		
		ConnectionConfiguration config = new ConnectionConfiguration("210.14.68.164", 5222);
		config.setSecurityMode(SecurityMode.disabled);
		config.setCompressionEnabled(true);
		config.setSASLAuthenticationEnabled(false);
		connection = new XMPPConnection(config);
		configureConnection();
	}
	
	public static XmppUtils getInstnce() {
		if(utils == null) {
			synchronized (XmppUtils.class) {
				if(utils == null) {
					utils = new XmppUtils();
				}
			}
		}
		return utils;
	}
	
	public Connection getConnection() {
		if(connection != null) {
			return connection;
		}
		return null;
	}
	
	public boolean login(String userName, String password) {
		try {
			connection.connect();
			connection.login(userName, password);
			return true;
		} catch (XMPPException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void configureConnection() {
		ProviderManager pm = ProviderManager.getInstance();
		try {
			pm.addIQProvider("query","jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
       
 
        //  Roster Exchange
        pm.addExtensionProvider("x","jabber:x:roster", new RosterExchangeProvider());
 
        //  Message Events
        pm.addExtensionProvider("x","jabber:x:event", new MessageEventProvider());
 
        //  Chat State
        pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
 
        //  XHTML
        pm.addExtensionProvider("html","http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());
 
        //  Group Chat Invitations
        pm.addExtensionProvider("x","jabber:x:conference", new GroupChatInvitation.Provider());
 
        //  Service Discovery # Items    
        pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
 
        //  Service Discovery # Info
        pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
 
        //  Data Forms
        pm.addExtensionProvider("x","jabber:x:data", new DataFormProvider());
 
        //  MUC User
        pm.addExtensionProvider("x","http://jabber.org/protocol/muc#user", new MUCUserProvider());
 
        //  MUC Admin    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
 
 
        //  MUC Owner    
        pm.addIQProvider("query","http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
 
        //  Delayed Delivery
        pm.addExtensionProvider("x","jabber:x:delay", new DelayInformationProvider());
 
        //  Version
        try {
            pm.addIQProvider("query","jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
            //  Not sure what's happening here.
        }
 
        //  VCard
        pm.addIQProvider("vCard","vcard-temp", new VCardProvider());
 
        //  Offline Message Requests
        pm.addIQProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
 
        //  Offline Message Indicator
        pm.addExtensionProvider("offline","http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
 
        //  Last Activity
        pm.addIQProvider("query","jabber:iq:last", new LastActivity.Provider());
 
        //  User Search
        pm.addIQProvider("query","jabber:iq:search", new UserSearch.Provider());
 
        //  SharedGroupsInfo
        pm.addIQProvider("sharedgroup","http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
 
        //  JEP-33: Extended Stanza Addressing
        pm.addExtensionProvider("addresses","http://jabber.org/protocol/address", new MultipleAddressesProvider());
 
        //   FileTransfer
        pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
 
        pm.addIQProvider("query","http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
 
        pm.addIQProvider("open","http://jabber.org/protocol/ibb", new OpenIQProvider());
 
        pm.addIQProvider("close","http://jabber.org/protocol/ibb", new CloseIQProvider());
 
        pm.addExtensionProvider("data","http://jabber.org/protocol/ibb",  new DataPacketProvider());
 
        //  Privacy
        pm.addIQProvider("query","jabber:iq:privacy", new PrivacyProvider());
 
        pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
        pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
	}
}
