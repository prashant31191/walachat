/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.javax.naming;

/**
 * An <code>NoPermissionException</code> is the
 * <code>NamingSecurityException</code> used when a server refuses permission to
 * the client.
 * <p>
 * Multithreaded access to an <code>NoPermissionException</code> instance is
 * only safe when client code locks the object first.
 * </p>
 */
public class NoPermissionException extends NamingSecurityException {

	private static final long serialVersionUID = 8395332708699751775L;

	/**
	 * Constructs a <code>NoPermissionException</code> instance with all data
	 * initialized to null.
	 */
	public NoPermissionException() {
		super();
	}

	/**
	 * Constructs a <code>NoPermissionException</code> instance with the
	 * specified message.
	 * 
	 * @param s
	 *            The detail message for this exception. It may be null.
	 */
	public NoPermissionException(String s) {
		super(s);
	}

}
