package org.jivesoftware.smack.proxy;

import java.io.IOException;

/**
 * An exception class to handle exceptions caused by proxy.
 * 
 * @author Atul Aggarwal
 */
public class ProxyException extends IOException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7576501142332855300L;

	public ProxyException(ProxyInfo.ProxyType type) {
		super("Proxy Exception " + type.toString() + " : " + "Unknown Error");
	}

	public ProxyException(ProxyInfo.ProxyType type, String ex) {
		super("Proxy Exception " + type.toString() + " : " + ex);
	}

	public ProxyException(ProxyInfo.ProxyType type, String ex, Throwable cause) {
		super("Proxy Exception " + type.toString() + " : " + ex + ", " + cause);
	}
}
