/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Vladimir N. Molotkov, Alexander Y. Kleymenov
 */

package org.apache.harmony.security.x509;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.security.asn1.ASN1Implicit;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.internal.nls.Messages;

/**
 * The class encapsulates the ASN.1 DER encoding/decoding work with the
 * following structure which is a part of X.509 certificate (as specified in RFC
 * 3280 - Internet X.509 Public Key Infrastructure. Certificate and Certificate
 * Revocation List (CRL) Profile. http://www.ietf.org/rfc/rfc3280.txt):
 * 
 * <pre>
 * 
 *   NameConstraints ::= SEQUENCE {
 *        permittedSubtrees       [0]     GeneralSubtrees OPTIONAL,
 *        excludedSubtrees        [1]     GeneralSubtrees OPTIONAL }
 * 
 *   GeneralSubtrees ::= SEQUENCE SIZE (1..MAX) OF GeneralSubtree
 * 
 * </pre>
 * 
 * 
 * @see org.apache.harmony.security.x509.GeneralSubtree
 * @see org.apache.harmony.security.x509.GeneralName
 */
public class NameConstraints extends ExtensionValue {

	public static NameConstraints decode(byte[] encoding) throws IOException {
		return (NameConstraints) ASN1.decode(encoding);
	}

	// the value of permittedSubtrees field of the structure
	private final GeneralSubtrees permittedSubtrees;
	// the value of excludedSubtrees field of the structure
	private final GeneralSubtrees excludedSubtrees;

	// the ASN.1 encoded form of NameConstraints
	private byte[] encoding;
	// helper fields
	private ArrayList<GeneralName>[] permitted_names;

	private ArrayList<GeneralName>[] excluded_names;

	/**
	 * X.509 NameConstraints encoder/decoder.
	 */
	public static final ASN1Sequence ASN1 = new ASN1Sequence(new ASN1Type[] {
			new ASN1Implicit(0, GeneralSubtrees.ASN1),
			new ASN1Implicit(1, GeneralSubtrees.ASN1) }) {
		{
			setOptional(0);
			setOptional(1);
		}

		@Override
		protected Object getDecodedObject(BerInputStream in) {
			final Object[] values = (Object[]) in.content;
			return new NameConstraints((GeneralSubtrees) values[0],
					(GeneralSubtrees) values[1], in.getEncoded());
		}

		@Override
		protected void getValues(Object object, Object[] values) {

			final NameConstraints nc = (NameConstraints) object;

			values[0] = nc.permittedSubtrees;
			values[1] = nc.excludedSubtrees;
		}
	};

	/**
	 * Default ctor
	 */
	public NameConstraints() {
		this(null, null);
	}

	/**
	 * Constructs <code>NameConstrains</code> object
	 * 
	 * @param permittedSubtrees
	 *            : GeneralSubtrees
	 * @param excludedSubtrees
	 *            : GeneralSubtrees
	 */
	public NameConstraints(GeneralSubtrees permittedSubtrees,
			GeneralSubtrees excludedSubtrees) {
		if (permittedSubtrees != null) {
			final List<?> ps = permittedSubtrees.getSubtrees();
			if ((ps == null) || (ps.size() == 0)) {
				throw new IllegalArgumentException(
						Messages.getString("security.17D")); //$NON-NLS-1$
			}
		}
		if (excludedSubtrees != null) {
			final List<?> es = excludedSubtrees.getSubtrees();
			if ((es == null) || (es.size() == 0)) {
				throw new IllegalArgumentException(
						Messages.getString("security.17E")); //$NON-NLS-1$
			}
		}
		this.permittedSubtrees = permittedSubtrees;
		this.excludedSubtrees = excludedSubtrees;
	}

	//
	// Constructs NameConstrains object
	// @param permittedSubtrees: GeneralSubtrees
	// @param excludedSubtrees: GeneralSubtrees
	// @param encoding: byte[]
	//
	private NameConstraints(GeneralSubtrees permittedSubtrees,
			GeneralSubtrees excludedSubtrees, byte[] encoding) {
		this(permittedSubtrees, excludedSubtrees);
		this.encoding = encoding;
	}

	/**
	 * Places the string representation of extension value into the StringBuffer
	 * object.
	 */
	@Override
	public void dumpValue(StringBuffer buffer, String prefix) {
		buffer.append(prefix).append("Name Constraints: [\n"); //$NON-NLS-1$
		if (permittedSubtrees != null) {
			buffer.append(prefix).append("  Permitted: [\n"); //$NON-NLS-1$
			for (final Object name : permittedSubtrees.getSubtrees()) {
				((GeneralSubtree) name).dumpValue(buffer, prefix + "    "); //$NON-NLS-1$
			}
			buffer.append(prefix).append("  ]\n"); //$NON-NLS-1$
		}
		if (excludedSubtrees != null) {
			buffer.append(prefix).append("  Excluded: [\n"); //$NON-NLS-1$
			for (final Object name : excludedSubtrees.getSubtrees()) {
				((GeneralSubtree) name).dumpValue(buffer, prefix + "    "); //$NON-NLS-1$
			}
			buffer.append(prefix).append("  ]\n"); //$NON-NLS-1$
		}
		buffer.append('\n').append(prefix).append("]\n"); //$NON-NLS-1$
	}

	/**
	 * Returns ASN.1 encoded form of this X.509 NameConstraints value.
	 * 
	 * @return a byte array containing ASN.1 encode form.
	 */
	@Override
	public byte[] getEncoded() {
		if (encoding == null) {
			encoding = ASN1.encode(this);
		}
		return encoding;
	}

	//
	// Returns the value of certificate extension
	//
	private byte[] getExtensionValue(X509Certificate cert, String OID) {
		try {
			final byte[] bytes = cert.getExtensionValue(OID);
			if (bytes == null) {
				return null;
			}
			return (byte[]) ASN1OctetString.getInstance().decode(bytes);
		} catch (final IOException e) {
			return null;
		}
	}

	/**
	 * Check if this list of names is acceptable accoring to this
	 * NameConstraints object.
	 * 
	 * @param names
	 *            : List
	 * @return
	 */
	public boolean isAcceptable(List<GeneralName> names) {
		if (permitted_names == null) {
			prepareNames();
		}

		final Iterator<GeneralName> it = names.iterator();
		// check map: shows which types of permitted alternative names are
		// presented in the certificate
		final boolean[] types_presented = new boolean[9];
		// check map: shows if permitted name of presented type is found
		// among the certificate's alternative names
		final boolean[] permitted_found = new boolean[9];
		while (it.hasNext()) {
			final GeneralName name = it.next();
			final int type = name.getTag();
			// search the name in excluded names
			if (excluded_names[type] != null) {
				for (int i = 0; i < excluded_names[type].size(); i++) {
					if (excluded_names[type].get(i).isAcceptable(name)) {
						return false;
					}
				}
			}
			// Search the name in permitted names
			// (if we already found the name of such type between the alt
			// names - we do not need to check others)
			if ((permitted_names[type] != null) && (!permitted_found[type])) {
				types_presented[type] = true;
				for (int i = 0; i < permitted_names[type].size(); i++) {
					if (permitted_names[type].get(i).isAcceptable(name)) {
						// found one permitted name of such type
						permitted_found[type] = true;
					}
				}
			}
		}
		for (int type = 0; type < 9; type++) {
			if (types_presented[type] && !permitted_found[type]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Apply the name restrictions specified by this NameConstraints instance to
	 * the subject distinguished name and subject alternative names of specified
	 * X509Certificate. Restrictions apply only if specified name form is
	 * present in the certificate. The restrictions are applied according the
	 * RFC 3280 (see 4.2.1.11 Name Constraints), excepting that restrictions are
	 * applied and to CA certificates, and to certificates which issuer and
	 * subject names the same (i.e. method does not check if it CA's certificate
	 * or not, or if the names differ or not. This check if it is needed should
	 * be done by caller before calling this method).
	 * 
	 * @param X509Certificate
	 *            : X.509 Certificate to be checked.
	 * @return true, if the certificate is acceptable according these
	 *         NameConstraints restrictions, and false otherwise.
	 */
	public boolean isAcceptable(X509Certificate cert) {
		if (permitted_names == null) {
			prepareNames();
		}

		final byte[] bytes = getExtensionValue(cert, "2.5.29.17"); //$NON-NLS-1$
		List<GeneralName> names;
		try {
			names = (bytes == null) ? new ArrayList<GeneralName>(1) // will
																	// check the
																	// subject
					// field only
					: ((GeneralNames) GeneralNames.ASN1.decode(bytes))
							.getNames();
		} catch (final IOException e) {
			// the certificate is broken;
			e.printStackTrace();
			return false;
		}
		if ((excluded_names[4] != null) || (permitted_names[4] != null)) {
			try {
				names.add(new GeneralName(4, cert.getSubjectX500Principal()
						.getName()));
			} catch (final IOException e) {
				// should never be happened
			}
		}
		return isAcceptable(names);
	}

	//
	// Prepare the data structure to speed up the checking process.
	//
	private void prepareNames() {
		// array of lists with permitted General Names divided by type
		permitted_names = new ArrayList[9];
		if (permittedSubtrees != null) {
			final Iterator<?> it = permittedSubtrees.getSubtrees().iterator();
			while (it.hasNext()) {
				final GeneralName name = ((GeneralSubtree) it.next()).getBase();
				// System.out.println("PERMITTED: "+name);
				final int tag = name.getTag();
				if (permitted_names[tag] == null) {
					permitted_names[tag] = new ArrayList<GeneralName>();
				}
				permitted_names[tag].add(name);
			}
		}
		// array of lists with excluded General Names divided by type
		excluded_names = new ArrayList[9];
		if (excludedSubtrees != null) {
			final Iterator<?> it = excludedSubtrees.getSubtrees().iterator();
			while (it.hasNext()) {
				final GeneralName name = ((GeneralSubtree) it.next()).getBase();
				// System.out.println("EXCLUDED: "+name);
				final int tag = name.getTag();
				if (excluded_names[tag] == null) {
					excluded_names[tag] = new ArrayList<GeneralName>();
				}
				excluded_names[tag].add(name);
			}
		}
	}
}
