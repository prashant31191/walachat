/**
 * $RCSfile: Jingle.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:08 $
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleActionEnum;

/**
 * An Jingle sub-packet, which is used by XMPP clients to exchange info like
 * descriptions and transports.
 * <p/>
 * The following link summarizes the requirements of Jingle IM: <a
 * href="http://www.jabber.org/jeps/jep-0166.html">Valid tags</a>.
 * <p/>
 * <p/>
 * Warning: this is an non-standard protocol documented by <a
 * href="http://www.jabber.org/jeps/jep-0166.html">JEP-166</a>. Because this is
 * a non-standard protocol, it is subject to change.
 * 
 * @author Alvaro Saurin
 */
public class Jingle extends IQ {

	// static

	public static final String NAMESPACE = "urn:xmpp:tmp:jingle";

	public static final String NODENAME = "jingle";

	// non-static

	/**
	 * Returns the XML element name of the extension sub-packet root element.
	 * Always returns "jingle"
	 * 
	 * @return the XML element name of the packet extension.
	 */
	public static String getElementName() {
		return NODENAME;
	}

	/**
	 * Returns the XML namespace of the extension sub-packet root element.
	 * 
	 * @return the XML namespace of the packet extension.
	 */
	public static String getNamespace() {
		return NAMESPACE;
	}

	/**
	 * Get a hash key for the session this packet belongs to.
	 * 
	 * @param sid
	 *            The session id
	 * @param initiator
	 *            The initiator
	 * @return A hash key
	 */
	public static int getSessionHash(final String sid, final String initiator) {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result
				+ (initiator == null ? 0 : initiator.hashCode());
		result = PRIME * result + (sid == null ? 0 : sid.hashCode());
		return result;
	}

	private String sid; // The session id

	// Sub-elements of a Jingle object.

	private JingleActionEnum action; // The action associated to the Jingle

	private String initiator; // The initiator as a "user@host/resource"

	private String responder; // The responder

	private final List<JingleContent> contents = new ArrayList<JingleContent>();

	private JingleContentInfo contentInfo;

	/**
	 * The default constructor
	 */
	public Jingle() {
		super();
	}

	/**
	 * A constructor where the action can be specified.
	 * 
	 * @param action
	 *            The action.
	 */
	public Jingle(final JingleActionEnum action) {
		this(null, null, null);
		this.action = action;

		// In general, a Jingle with an action is used in a SET packet...
		setType(IQ.Type.SET);
	}

	/**
	 * Constructor with a contents.
	 * 
	 * @param content
	 *            a content
	 */
	public Jingle(final JingleContent content) {
		super();

		addContent(content);

		// Set null all other fields in the packet
		initiator = null;
		responder = null;

		// Some default values for the most common situation...
		action = JingleActionEnum.UNKNOWN;
		setType(IQ.Type.SET);
	}

	/**
	 * Constructor with a content info.
	 * 
	 * @param info
	 *            The content info
	 */
	public Jingle(final JingleContentInfo info) {
		super();

		setContentInfo(info);

		// Set null all other fields in the packet
		initiator = null;
		responder = null;

		// Some default values for the most common situation...
		action = JingleActionEnum.UNKNOWN;
		setType(IQ.Type.SET);
	}

	/**
	 * A constructor where the main components can be initialized.
	 */
	public Jingle(final List<JingleContent> contents,
			final JingleContentInfo mi, final String sid) {
		super();

		if (contents != null) {
			contents.addAll(contents);
		}

		setContentInfo(mi);
		setSid(sid);

		// Set null all other fields in the packet
		initiator = null;
		responder = null;
		action = null;
	}

	/**
	 * A constructor where the session ID can be specified.
	 * 
	 * @param sid
	 *            The session ID related to the negotiation.
	 * @see #setSid(String)
	 */
	public Jingle(final String sid) {
		this(null, null, sid);
	}

	/**
	 * Add a new content.
	 * 
	 * @param content
	 *            the content to add
	 */
	public void addContent(final JingleContent content) {
		if (content != null) {
			synchronized (contents) {
				contents.add(content);
			}
		}
	}

	/**
	 * Add a list of JingleContent elements
	 * 
	 * @param contentList
	 *            the list of contents to add
	 */
	public void addContents(final List<JingleContent> contentList) {
		if (contentList != null) {
			synchronized (contents) {
				contents.addAll(contentList);
			}
		}
	}

	/**
	 * Get the action specified in the packet
	 * 
	 * @return the action
	 */
	public JingleActionEnum getAction() {
		return action;
	}

	/**
	 * Return the XML representation of the packet.
	 * 
	 * @return the XML string
	 */
	@Override
	public String getChildElementXML() {
		final StringBuilder buf = new StringBuilder();

		buf.append("<").append(getElementName());
		buf.append(" xmlns=\"").append(getNamespace()).append("\"");
		if (getInitiator() != null) {
			buf.append(" initiator=\"").append(getInitiator()).append("\"");
		}
		if (getResponder() != null) {
			buf.append(" responder=\"").append(getResponder()).append("\"");
		}
		if (getAction() != null) {
			buf.append(" action=\"").append(getAction()).append("\"");
		}
		if (getSid() != null) {
			buf.append(" sid=\"").append(getSid()).append("\"");
		}
		buf.append(">");

		synchronized (contents) {
			for (final JingleContent content : contents) {
				buf.append(content.toXML());
			}
		}

		// and the same for audio jmf info
		if (contentInfo != null) {
			buf.append(contentInfo.toXML());
		}

		buf.append("</").append(getElementName()).append(">");
		return buf.toString();
	}

	/**
	 * @return the audioInfo
	 */
	public JingleContentInfo getContentInfo() {
		return contentInfo;
	}

	/**
	 * Get an iterator for the contents
	 * 
	 * @return the contents
	 */
	public Iterator<JingleContent> getContents() {
		synchronized (contents) {
			return Collections.unmodifiableList(new ArrayList(contents))
					.iterator();
		}
	}

	/**
	 * Get an iterator for the content
	 * 
	 * @return the contents
	 */
	public List<JingleContent> getContentsList() {
		synchronized (contents) {
			return new ArrayList<JingleContent>(contents);
		}
	}

	/**
	 * Get the initiator. The initiator will be the full JID of the entity that
	 * has initiated the flow (which may be different to the "from" address in
	 * the IQ)
	 * 
	 * @return the initiator
	 */
	public String getInitiator() {
		return initiator;
	}

	/**
	 * Get the responder. The responder is the full JID of the entity that has
	 * replied to the initiation (which may be different to the "to" addresss in
	 * the IQ).
	 * 
	 * @return the responder
	 */
	public String getResponder() {
		return responder;
	}

	/**
	 * Returns the session ID related to the session. The session ID is a unique
	 * identifier generated by the initiator. This should match the XML Nmtoken
	 * production so that XML character escaping is not needed for characters
	 * such as &.
	 * 
	 * @return Returns the session ID related to the session.
	 * @see #setSid(String)
	 */
	public String getSid() {

		return sid;
	}

	/**
	 * Set the action in the packet
	 * 
	 * @param action
	 *            the action to set
	 */
	public void setAction(final JingleActionEnum action) {
		this.action = action;
	}

	/**
	 * @param contentInfo
	 *            the audioInfo to set
	 */
	public void setContentInfo(final JingleContentInfo contentInfo) {
		this.contentInfo = contentInfo;
	}

	/**
	 * Set the initiator. The initiator must be the full JID of the entity that
	 * has initiated the flow (which may be different to the "from" address in
	 * the IQ)
	 * 
	 * @param initiator
	 *            the initiator to set
	 */
	public void setInitiator(final String initiator) {
		this.initiator = initiator;
	}

	/**
	 * Set the responder. The responder must be the full JID of the entity that
	 * has replied to the initiation (which may be different to the "to"
	 * addresss in the IQ).
	 * 
	 * @param resp
	 *            the responder to set
	 */
	public void setResponder(final String resp) {
		responder = resp;
	}

	/**
	 * Set the session ID related to this session. The session ID is a unique
	 * identifier generated by the initiator. This should match the XML Nmtoken
	 * production so that XML character escaping is not needed for characters
	 * such as &.
	 * 
	 * @param sid
	 *            the session ID
	 */
	public final void setSid(final String sid) {
		this.sid = sid;
	}
}
