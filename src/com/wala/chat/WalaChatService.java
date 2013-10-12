package com.wala.chat;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.pubsub.PresenceState;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class WalaChatService extends Service {

	private String fromJid;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		System.out.println("service oncreate");
	    final Connection connection =	XmppUtils.getInstnce().getConnection();
		
		ProviderManager pm = ProviderManager.getInstance();
		 pm.addIQProvider("si","http://jabber.org/protocol/si", new StreamInitiationProvider());
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
		if(sdm == null) {
			sdm = new ServiceDiscoveryManager(connection);
		}
		sdm.addFeature("http://jabber.org/protocol/disco#info");
		sdm.addFeature("jabber:iq:privacy");
		FileTransferManager manager = new FileTransferManager(connection);
		FileTransferNegotiator.setServiceEnabled(connection, true);
		
		manager.addFileTransferListener(new FileTransferListener() {
			
			@Override
			public void fileTransferRequest(FileTransferRequest arg0) {
				// TODO Auto-generated method stub
				System.out.println(arg0.getFileName());
				File file = new java.io.File("/storage/sdcard0/"+arg0.getFileName());
				IncomingFileTransfer transfer = arg0.accept();
				try {
					transfer.recieveFile(file);
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		
		
		
		
		
		
		Roster roster = connection.getRoster();
		roster.addRosterListener(new RosterListener() {
			
			@Override
			public void presenceChanged(Presence arg0) {
				// TODO Auto-generated method stub
				System.out.println("presenceChanged");
			}
			
			@Override
			public void entriesUpdated(Collection<String> arg0) {
				// TODO Auto-generated method stub
				System.out.println("entriesUpdated");
			}
			
			@Override
			public void entriesDeleted(Collection<String> arg0) {
				// TODO Auto-generated method stub
				System.out.println("entriesDeleted");
			}
			
			@Override
			public void entriesAdded(Collection<String> invites) {
				// TODO Auto-generated method stub
				System.out.println("好友   "+fromJid);
				Iterator<String > iterator = invites.iterator();
				while(iterator.hasNext()) {
					fromJid = iterator.next();
				}
//				
//				
//				PacketFilter filter = new AndFilter(new PacketTypeFilter(Presence.class), new PacketFilter() {
//					
//					@Override
//					public boolean accept(Packet arg0) {
//						// TODO Auto-generated method stub
//						System.out.println("accept   "+arg0.getFrom());
//						Presence presence = (Presence) arg0;
//						if(presence.getType().equals(Presence.Type.subscribe)) {
//							return true;
//						}else {
//							return false;
//						}
//					}
//				});
//				connection.addPacketListener(new PacketListener() {
//					
//					@Override
//					public void processPacket(Packet arg0) {
//						System.out.println("processPacket    "+arg0.getFrom());
//						String from = arg0.getFrom();
//						Presence presence = new Presence(Presence.Type.unsubscribed);
//						presence.setTo(from);
//						connection.sendPacket(presence);
//						
//					}
//				}, filter);
//				
//				
				
				if(fromJid != null) {
					System.out.println(fromJid +"  请求加好友");
					Presence presence = new Presence(Presence.Type.subscribe);
					presence.setTo(fromJid);
					connection.sendPacket(presence);
					Collection<RosterGroup> groups = connection.getRoster().getGroups();
					if (groups.size() != 0) {
						RosterEntry entry = connection.getRoster().getEntry(fromJid);
						Iterator<RosterGroup> gIterator = groups.iterator();
						try {
							RosterGroup group = gIterator.next();
							if (group != null && entry != null) {
								group.addEntry(entry);
							}else {
								RosterGroup group2 = connection.getRoster().createGroup("haha");
								if(entry != null)
								group2.addEntry(entry);
							}
						} catch (XMPPException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		});
		
		
		PacketFilter filter = new AndFilter(new PacketTypeFilter(Presence.class), new PacketFilter() {
			
			@Override
			public boolean accept(Packet arg0) {
				// TODO Auto-generated method stub
				System.out.println("accept   "+arg0.getFrom());
				Presence presence = (Presence) arg0;
				if(presence.getType().equals(Presence.Type.subscribe)) {
					return true;
				}else {
					return false;
				}
			}
		});
		connection.addPacketListener(new PacketListener() {
			
			@Override
			public void processPacket(Packet arg0) {
				System.out.println("processPacket    "+arg0.getFrom());
				String from = arg0.getFrom();
				Presence presence = new Presence(Presence.Type.unsubscribed);
				presence.setTo(from);
				connection.sendPacket(presence);
				
			}
		}, filter);
		
		
		
	
	}

}
