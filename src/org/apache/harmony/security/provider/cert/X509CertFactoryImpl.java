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
 * @author Alexander Y. Kleymenov
 */

package org.apache.harmony.security.provider.cert;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactorySpi;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.harmony.luni.util.Base64;
import org.apache.harmony.security.asn1.ASN1Constants;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.internal.nls.Messages;
import org.apache.harmony.security.pkcs7.ContentInfo;
import org.apache.harmony.security.pkcs7.SignedData;
import org.apache.harmony.security.x509.CertificateList;

/**
 * X509 Certificate Factory Service Provider Interface Implementation. It
 * supports CRLs and Certificates in (PEM) ASN.1 DER encoded form, and
 * Certification Paths in PkiPath and PKCS7 formats. For Certificates and CRLs
 * factory maintains the caching mechanisms allowing to speed up repeated
 * Certificate/CRL generation.
 * 
 * @see Cache
 */
public class X509CertFactoryImpl extends CertificateFactorySpi {

	/*
	 * This class extends any existing input stream with mark functionality. It
	 * acts as a wrapper over the stream and supports reset to the marked state
	 * with readlimit no more than BUFF_SIZE.
	 */
	private static class RestoringInputStream extends InputStream {

		// wrapped input stream
		private final InputStream inStream;
		// specifies how much of the read data is buffered
		// after the mark has been set up
		private static final int BUFF_SIZE = 32;
		// buffer to keep the bytes read after the mark has been set up
		private final int[] buff = new int[BUFF_SIZE * 2];
		// position of the next byte to read,
		// the value of -1 indicates that the buffer is not used
		// (mark was not set up or was invalidated, or reset to the marked
		// position has been done and all the buffered data was read out)
		private int pos = -1;
		// position of the last buffered byte
		private int bar = 0;
		// position in the buffer where the mark becomes invalidated
		private int end = 0;

		/**
		 * Creates the mark supporting wrapper over the stream.
		 */
		public RestoringInputStream(InputStream inStream) {
			this.inStream = inStream;
		}

		/**
		 * @see java.io.InputStream#available() method documentation for more
		 *      info
		 */
		@Override
		public int available() throws IOException {
			return (bar - pos) + inStream.available();
		}

		/**
		 * @see java.io.InputStream#close() method documentation for more info
		 */
		@Override
		public void close() throws IOException {
			inStream.close();
		}

		/**
		 * @see java.io.InputStream#mark(int readlimit) method documentation for
		 *      more info
		 */
		@Override
		public void mark(int readlimit) {
			if (pos < 0) {
				pos = 0;
				bar = 0;
				end = BUFF_SIZE - 1;
			} else {
				end = (pos + BUFF_SIZE - 1) % BUFF_SIZE;
			}
		}

		/**
		 * @see java.io.InputStream#markSupported() method documentation for
		 *      more info
		 */
		@Override
		public boolean markSupported() {
			return true;
		}

		/**
		 * Reads the byte from the stream. If mark has been set up and was not
		 * invalidated byte is read from the underlying stream and saved into
		 * the buffer. If the current read position has been reset to the marked
		 * position and there are remaining bytes in the buffer, the byte is
		 * taken from it. In the other cases (if mark has been invalidated, or
		 * there are no buffered bytes) the byte is taken directly from the
		 * underlying stream and it is returned without saving to the buffer.
		 * 
		 * @see java.io.InputStream#read() method documentation for more info
		 */
		@Override
		public int read() throws IOException {
			// if buffer is currently used
			if (pos >= 0) {
				// current position in the buffer
				final int cur = pos % BUFF_SIZE;
				// check whether the buffer contains the data to be read
				if (cur < bar) {
					// return the data from the buffer
					pos++;
					return buff[cur];
				}
				// check whether buffer has free space
				if (cur != end) {
					// it has, so read the data from the wrapped stream
					// and place it in the buffer
					buff[cur] = inStream.read();
					bar = cur + 1;
					pos++;
					return buff[cur];
				} else {
					// buffer if full and can not operate
					// any more, so invalidate the mark position
					// and turn off the using of buffer
					pos = -1;
				}
			}
			// buffer is not used, so return the data from the wrapped stream
			return inStream.read();
		}

		/**
		 * @see java.io.InputStream#read(byte[] b) method documentation for more
		 *      info
		 */
		@Override
		public int read(byte[] b) throws IOException {
			return read(b, 0, b.length);
		}

		/**
		 * @see java.io.InputStream#read(byte[] b, int off, int len) method
		 *      documentation for more info
		 */
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read_b;
			int i;
			for (i = 0; i < len; i++) {
				if ((read_b = read()) == -1) {
					return (i == 0) ? -1 : i;
				}
				b[off + i] = (byte) read_b;
			}
			return i;
		}

		/**
		 * @see java.io.InputStream#reset() method documentation for more info
		 */
		@Override
		public void reset() throws IOException {
			if (pos >= 0) {
				pos = (end + 1) % BUFF_SIZE;
			} else {
				throw new IOException(Messages.getString("security.15A")); //$NON-NLS-1$
			}
		}

		/**
		 * @see java.io.InputStream#skip(long n) method documentation for more
		 *      info
		 */
		@Override
		public long skip(long n) throws IOException {
			if (pos >= 0) {
				long i = 0;
				final int av = available();
				if (av < n) {
					n = av;
				}
				while ((i < n) && (read() != -1)) {
					i++;
				}
				return i;
			} else {
				return inStream.skip(n);
			}
		}
	}

	// number of leading/trailing bytes used for cert hash computation
	private static int CERT_CACHE_SEED_LENGTH = 28;
	// certificate cache
	private static Cache CERT_CACHE = new Cache(CERT_CACHE_SEED_LENGTH);
	// number of leading/trailing bytes used for crl hash computation
	private static int CRL_CACHE_SEED_LENGTH = 24;

	// crl cache
	private static Cache CRL_CACHE = new Cache(CRL_CACHE_SEED_LENGTH);

	private static byte[] pemBegin;

	private static byte[] pemClose;

	/**
	 * Code describing free format for PEM boundary suffix: "^-----BEGIN.*\n" at
	 * the beginning, and<br>
	 * "\n-----END.*(EOF|\n)$" at the end.
	 */
	private static byte[] FREE_BOUND_SUFFIX = null;

	/**
	 * Code describing PEM boundary suffix for X.509 certificate:
	 * "^-----BEGIN CERTIFICATE-----\n" at the beginning, and<br>
	 * "\n-----END CERTIFICATE-----" at the end.
	 */
	private static byte[] CERT_BOUND_SUFFIX;

	static {
		// Initialise statics
		try {
			pemBegin = "-----BEGIN".getBytes("UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
			pemClose = "-----END".getBytes("UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
			CERT_BOUND_SUFFIX = " CERTIFICATE-----".getBytes("UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Returns the Certificate object corresponding to the provided encoding.
	 * Resulting object is retrieved from the cache if it contains such
	 * correspondence and is constructed on the base of encoding and stored in
	 * the cache otherwise.
	 * 
	 * @throws IOException
	 *             if some decoding errors occur (in the case of cache miss).
	 */
	private static Certificate getCertificate(byte[] encoding)
			throws CertificateException, IOException {
		if (encoding.length < CERT_CACHE_SEED_LENGTH) {
			throw new CertificateException(Messages.getString("security.152")); //$NON-NLS-1$
		}
		synchronized (CERT_CACHE) {
			final long hash = CERT_CACHE.getHash(encoding);
			if (CERT_CACHE.contains(hash)) {
				final Certificate res = (Certificate) CERT_CACHE.get(hash,
						encoding);
				if (res != null) {
					return res;
				}
			}
			final Certificate res = new X509CertImpl(encoding);
			CERT_CACHE.put(hash, encoding, res);
			return res;
		}
	}

	/**
	 * Returns the Certificate object corresponding to the encoding provided by
	 * the stream. Resulting object is retrieved from the cache if it contains
	 * such correspondence and is constructed on the base of encoding and stored
	 * in the cache otherwise.
	 * 
	 * @throws IOException
	 *             if some decoding errors occur (in the case of cache miss).
	 */
	private static Certificate getCertificate(InputStream inStream)
			throws CertificateException, IOException {
		synchronized (CERT_CACHE) {
			inStream.mark(CERT_CACHE_SEED_LENGTH);
			// read the prefix of the encoding
			final byte[] buff = readBytes(inStream, CERT_CACHE_SEED_LENGTH);
			inStream.reset();
			if (buff == null) {
				throw new CertificateException(
						Messages.getString("security.152")); //$NON-NLS-1$
			}
			final long hash = CERT_CACHE.getHash(buff);
			if (CERT_CACHE.contains(hash)) {
				final byte[] encoding = new byte[BerInputStream.getLength(buff)];
				if (encoding.length < CERT_CACHE_SEED_LENGTH) {
					throw new CertificateException(
							Messages.getString("security.15B3")); //$NON-NLS-1$
				}
				inStream.read(encoding);
				Certificate res = (Certificate) CERT_CACHE.get(hash, encoding);
				if (res != null) {
					return res;
				}
				res = new X509CertImpl(encoding);
				CERT_CACHE.put(hash, encoding, res);
				return res;
			} else {
				inStream.reset();
				final Certificate res = new X509CertImpl(inStream);
				CERT_CACHE.put(hash, res.getEncoded(), res);
				return res;
			}
		}
	}

	/**
	 * Returns the CRL object corresponding to the provided encoding. Resulting
	 * object is retrieved from the cache if it contains such correspondence and
	 * is constructed on the base of encoding and stored in the cache otherwise.
	 * 
	 * @throws IOException
	 *             if some decoding errors occur (in the case of cache miss).
	 */
	private static CRL getCRL(byte[] encoding) throws CRLException, IOException {
		if (encoding.length < CRL_CACHE_SEED_LENGTH) {
			throw new CRLException(Messages.getString("security.152")); //$NON-NLS-1$
		}
		synchronized (CRL_CACHE) {
			final long hash = CRL_CACHE.getHash(encoding);
			if (CRL_CACHE.contains(hash)) {
				final X509CRL res = (X509CRL) CRL_CACHE.get(hash, encoding);
				if (res != null) {
					return res;
				}
			}
			final X509CRL res = new X509CRLImpl(encoding);
			CRL_CACHE.put(hash, encoding, res);
			return res;
		}
	}

	// ---------------------------------------------------------------------
	// ------------------------ Staff methods ------------------------------
	// ---------------------------------------------------------------------

	/**
	 * Returns the CRL object corresponding to the encoding provided by the
	 * stream. Resulting object is retrieved from the cache if it contains such
	 * correspondence and is constructed on the base of encoding and stored in
	 * the cache otherwise.
	 * 
	 * @throws IOException
	 *             if some decoding errors occur (in the case of cache miss).
	 */
	private static CRL getCRL(InputStream inStream) throws CRLException,
			IOException {
		synchronized (CRL_CACHE) {
			inStream.mark(CRL_CACHE_SEED_LENGTH);
			final byte[] buff = readBytes(inStream, CRL_CACHE_SEED_LENGTH);
			// read the prefix of the encoding
			inStream.reset();
			if (buff == null) {
				throw new CRLException(Messages.getString("security.152")); //$NON-NLS-1$
			}
			final long hash = CRL_CACHE.getHash(buff);
			if (CRL_CACHE.contains(hash)) {
				final byte[] encoding = new byte[BerInputStream.getLength(buff)];
				if (encoding.length < CRL_CACHE_SEED_LENGTH) {
					throw new CRLException(Messages.getString("security.15B4")); //$NON-NLS-1$
				}
				inStream.read(encoding);
				CRL res = (CRL) CRL_CACHE.get(hash, encoding);
				if (res != null) {
					return res;
				}
				res = new X509CRLImpl(encoding);
				CRL_CACHE.put(hash, encoding, res);
				return res;
			} else {
				final X509CRL res = new X509CRLImpl(inStream);
				CRL_CACHE.put(hash, res.getEncoded(), res);
				return res;
			}
		}
	}

	/**
	 * Reads the data of specified length from source and returns it as an
	 * array.
	 * 
	 * @return the byte array contained read data or null if the stream contains
	 *         not enough data
	 * @throws IOException
	 *             if some I/O error has been occurred.
	 */
	private static byte[] readBytes(InputStream source, int length)
			throws IOException {
		final byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			final int bytik = source.read();
			if (bytik == -1) {
				return null;
			}
			result[i] = (byte) bytik;
		}
		return result;
	}

	/**
	 * Default constructor. Creates the instance of Certificate Factory SPI
	 * ready for use.
	 */
	public X509CertFactoryImpl() {
	}

	/**
	 * Method retrieves the PEM encoded data from the stream and returns its
	 * decoded representation. Method checks correctness of PEM boundaries. It
	 * supposes that the first '-' of the opening boundary has already been read
	 * from the stream. So first of all it checks that the leading bytes are
	 * equal to "-----BEGIN" boundary prefix. Than if boundary_suffix is not
	 * null, it checks that next bytes equal to boundary_suffix + new line
	 * char[s] ([CR]LF). If boundary_suffix parameter is null, method supposes
	 * free suffix format and skips any bytes until the new line.<br>
	 * After the opening boundary has been read and checked, the method read
	 * Base64 encoded data until closing PEM boundary is not reached.<br>
	 * Than it checks closing boundary - it should start with new line +
	 * "-----END" + boundary_suffix. If boundary_suffix is null, any characters
	 * are skipped until the new line.<br>
	 * After this any trailing new line characters are skipped from the stream,
	 * Base64 encoding is decoded and returned.
	 * 
	 * @param inStream
	 *            the stream containing the PEM encoding.
	 * @param boundary_suffix
	 *            the suffix of expected PEM multipart boundary delimiter.<br>
	 *            If it is null, that any character sequences are accepted.
	 * @throws IOException
	 *             If PEM boundary delimiter does not comply with expected or
	 *             some I/O or decoding problems occur.
	 */
	private byte[] decodePEM(InputStream inStream, byte[] boundary_suffix)
			throws IOException {
		int ch; // the char to be read
		// check and skip opening boundary delimiter
		// (first '-' is supposed as already read)
		for (int i = 1; i < pemBegin.length; i++) {
			if (pemBegin[i] != (ch = inStream.read())) {
				throw new IOException("Incorrect PEM encoding: '-----BEGIN"
						+ ((boundary_suffix == null) ? "" : new String(
								boundary_suffix))
						+ "' is expected as opening delimiter boundary.");
			}
		}
		if (boundary_suffix == null) {
			// read (skip) the trailing characters of
			// the beginning PEM boundary delimiter
			while ((ch = inStream.read()) != '\n') {
				if (ch == -1) {
					throw new IOException(Messages.getString("security.156")); //$NON-NLS-1$
				}
			}
		} else {
			for (final byte element : boundary_suffix) {
				if (element != inStream.read()) {
					throw new IOException(Messages.getString("security.15B", //$NON-NLS-1$
							new String(boundary_suffix)));
				}
			}
			// read new line characters
			if ((ch = inStream.read()) == '\r') {
				// CR has been read, now read LF character
				ch = inStream.read();
			}
			if (ch != '\n') {
				throw new IOException(Messages.getString("security.15B2")); //$NON-NLS-1$
			}
		}
		int size = 1024; // the size of the buffer containing Base64 data
		byte[] buff = new byte[size];
		int index = 0;
		// read bytes while ending boundary delimiter is not reached
		while ((ch = inStream.read()) != '-') {
			if (ch == -1) {
				throw new IOException(Messages.getString("security.157")); //$NON-NLS-1$
			}
			buff[index++] = (byte) ch;
			if (index == size) {
				// enlarge the buffer
				final byte[] newbuff = new byte[size + 1024];
				System.arraycopy(buff, 0, newbuff, 0, size);
				buff = newbuff;
				size += 1024;
			}
		}
		if (buff[index - 1] != '\n') {
			throw new IOException(Messages.getString("security.158")); //$NON-NLS-1$
		}
		// check and skip closing boundary delimiter prefix
		// (first '-' was read)
		for (int i = 1; i < pemClose.length; i++) {
			if (pemClose[i] != inStream.read()) {
				throw new IOException(Messages.getString("security.15B1", //$NON-NLS-1$
						((boundary_suffix == null) ? "" : new String(
								boundary_suffix))));
			}
		}
		if (boundary_suffix == null) {
			// read (skip) the trailing characters of
			// the closing PEM boundary delimiter
			while (((ch = inStream.read()) != -1) && (ch != '\n')
					&& (ch != '\r')) {
			}
		} else {
			for (final byte element : boundary_suffix) {
				if (element != inStream.read()) {
					throw new IOException(Messages.getString("security.15B1", //$NON-NLS-1$
							new String(boundary_suffix)));
				}
			}
		}
		// skip trailing line breaks
		inStream.mark(1);
		while (((ch = inStream.read()) != -1) && (ch == '\n' || ch == '\r')) {
			inStream.mark(1);
		}
		inStream.reset();
		buff = Base64.decode(buff, index);
		if (buff == null) {
			throw new IOException(Messages.getString("security.159")); //$NON-NLS-1$
		}
		return buff;
	}

	/**
	 * Generates the X.509 certificate from the data in the stream. The data in
	 * the stream can be either in ASN.1 DER encoded X.509 certificate, or PEM
	 * (Base64 encoding bounded by <code>"-----BEGIN CERTIFICATE-----"</code> at
	 * the beginning and <code>"-----END CERTIFICATE-----"</code> at the end)
	 * representation of the former encoded form.
	 * 
	 * Before the generation the encoded form is looked up in the cache. If the
	 * cache contains the certificate with requested encoded form it is returned
	 * from it, otherwise it is generated by ASN.1 decoder.
	 * 
	 * @see java.security.cert.CertificateFactorySpi#engineGenerateCertificate(InputStream)
	 *      method documentation for more info
	 */
	@Override
	public Certificate engineGenerateCertificate(InputStream inStream)
			throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Messages.getString("security.153")); //$NON-NLS-1$
		}
		try {
			if (!inStream.markSupported()) {
				// create the mark supporting wrapper
				inStream = new RestoringInputStream(inStream);
			}
			// mark is needed to recognize the format of the provided encoding
			// (ASN.1 or PEM)
			inStream.mark(1);
			// check whether the provided certificate is in PEM encoded form
			if (inStream.read() == '-') {
				// decode PEM, retrieve CRL
				return getCertificate(decodePEM(inStream, CERT_BOUND_SUFFIX));
			} else {
				inStream.reset();
				// retrieve CRL
				return getCertificate(inStream);
			}
		} catch (final IOException e) {
			throw new CertificateException(e);
		}
	}

	/**
	 * Generates the collection of the certificates on the base of provided via
	 * input stream encodings.
	 * 
	 * @see java.security.cert.CertificateFactorySpi#engineGenerateCertificates(InputStream)
	 *      method documentation for more info
	 */
	@Override
	public Collection<? extends Certificate> engineGenerateCertificates(
			InputStream inStream) throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Messages.getString("security.153")); //$NON-NLS-1$
		}
		final ArrayList<Certificate> result = new ArrayList<Certificate>();
		try {
			if (!inStream.markSupported()) {
				// create the mark supporting wrapper
				inStream = new RestoringInputStream(inStream);
			}
			// if it is PEM encoded form this array will contain the encoding
			// so ((it is PEM) <-> (encoding != null))
			byte[] encoding = null;
			// The following by SEQUENCE ASN.1 tag, used for
			// recognizing the data format
			// (is it PKCS7 ContentInfo structure, X.509 Certificate, or
			// unsupported encoding)
			int second_asn1_tag = -1;
			inStream.mark(1);
			int ch;
			while ((ch = inStream.read()) != -1) {
				// check if it is PEM encoded form
				if (ch == '-') { // beginning of PEM encoding ('-' char)
					// decode PEM chunk and store its content (ASN.1 encoding)
					encoding = decodePEM(inStream, FREE_BOUND_SUFFIX);
				} else if (ch == 0x30) { // beginning of ASN.1 sequence (0x30)
					encoding = null;
					inStream.reset();
					// prepare for data format determination
					inStream.mark(CERT_CACHE_SEED_LENGTH);
				} else { // unsupported data
					if (result.size() == 0) {
						throw new CertificateException(
								Messages.getString("security.15F")); //$NON-NLS-1$
					} else {
						// it can be trailing user data,
						// so keep it in the stream
						inStream.reset();
						return result;
					}
				}
				// Check the data format
				final BerInputStream in = (encoding == null) ? new BerInputStream(
						inStream) : new BerInputStream(encoding);
				// read the next ASN.1 tag
				second_asn1_tag = in.next(); // inStream position changed
				if (encoding == null) {
					// keep whole structure in the stream
					inStream.reset();
				}
				// check if it is a TBSCertificate structure
				if (second_asn1_tag != ASN1Constants.TAG_C_SEQUENCE) {
					if (result.size() == 0) {
						// there were not read X.509 Certificates, so
						// break the cycle and check
						// whether it is PKCS7 structure
						break;
					} else {
						// it can be trailing user data,
						// so return what we already read
						return result;
					}
				} else {
					if (encoding == null) {
						result.add(getCertificate(inStream));
					} else {
						result.add(getCertificate(encoding));
					}
				}
				// mark for the next iteration
				inStream.mark(1);
			}
			if (result.size() != 0) {
				// some Certificates have been read
				return result;
			} else if (ch == -1) {
				throw new CertificateException(
						Messages.getString("security.155")); //$NON-NLS-1$
			}
			// else: check if it is PKCS7
			if (second_asn1_tag == ASN1Constants.TAG_OID) {
				// it is PKCS7 ContentInfo structure, so decode it
				final ContentInfo info = (ContentInfo) ((encoding != null) ? ContentInfo.ASN1
						.decode(encoding) : ContentInfo.ASN1.decode(inStream));
				// retrieve SignedData
				final SignedData data = info.getSignedData();
				if (data == null) {
					throw new CertificateException(
							Messages.getString("security.154")); //$NON-NLS-1$
				}
				final List certs = data.getCertificates();
				if (certs != null) {
					for (int i = 0; i < certs.size(); i++) {
						result.add(new X509CertImpl(
								(org.apache.harmony.security.x509.Certificate) certs
										.get(i)));
					}
				}
				return result;
			}
			// else: Unknown data format
			throw new CertificateException(Messages.getString("security.15F")); //$NON-NLS-1$
		} catch (final IOException e) {
			throw new CertificateException(e);
		}
	};

	/**
	 * @see java.security.cert.CertificateFactorySpi#engineGenerateCertPath(InputStream)
	 *      method documentation for more info
	 */
	@Override
	public CertPath engineGenerateCertPath(InputStream inStream)
			throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Messages.getString("security.153")); //$NON-NLS-1$
		}
		return engineGenerateCertPath(inStream, "PkiPath"); //$NON-NLS-1$
	}

	/**
	 * @see java.security.cert.CertificateFactorySpi#engineGenerateCertPath(InputStream,String)
	 *      method documentation for more info
	 */
	@Override
	public CertPath engineGenerateCertPath(InputStream inStream, String encoding)
			throws CertificateException {
		if (inStream == null) {
			throw new CertificateException(Messages.getString("security.153")); //$NON-NLS-1$
		}
		if (!inStream.markSupported()) {
			inStream = new RestoringInputStream(inStream);
		}
		try {
			inStream.mark(1);
			int ch;

			// check if it is PEM encoded form
			if ((ch = inStream.read()) == '-') {
				// decode PEM chunk into ASN.1 form and decode CertPath object
				return X509CertPathImpl.getInstance(
						decodePEM(inStream, FREE_BOUND_SUFFIX), encoding);
			} else if (ch == 0x30) { // ASN.1 Sequence
				inStream.reset();
				// decode ASN.1 form
				return X509CertPathImpl.getInstance(inStream, encoding);
			} else {
				throw new CertificateException(
						Messages.getString("security.15F")); //$NON-NLS-1$
			}
		} catch (final IOException e) {
			throw new CertificateException(e);
		}
	}

	/**
	 * @see java.security.cert.CertificateFactorySpi#engineGenerateCertPath(List)
	 *      method documentation for more info
	 */
	@Override
	public CertPath engineGenerateCertPath(List certificates)
			throws CertificateException {
		return new X509CertPathImpl(certificates);
	}

	/**
	 * @see java.security.cert.CertificateFactorySpi#engineGenerateCRL(InputStream)
	 *      method documentation for more info
	 */
	@Override
	public CRL engineGenerateCRL(InputStream inStream) throws CRLException {
		if (inStream == null) {
			throw new CRLException(Messages.getString("security.153")); //$NON-NLS-1$
		}
		try {
			if (!inStream.markSupported()) {
				// Create the mark supporting wrapper
				// Mark is needed to recognize the format
				// of provided encoding form (ASN.1 or PEM)
				inStream = new RestoringInputStream(inStream);
			}
			inStream.mark(1);
			// check whether the provided crl is in PEM encoded form
			if (inStream.read() == '-') {
				// decode PEM, retrieve CRL
				return getCRL(decodePEM(inStream, FREE_BOUND_SUFFIX));
			} else {
				inStream.reset();
				// retrieve CRL
				return getCRL(inStream);
			}
		} catch (final IOException e) {
			throw new CRLException(e);
		}
	}

	/**
	 * @see java.security.cert.CertificateFactorySpi#engineGenerateCRLs(InputStream)
	 *      method documentation for more info
	 */
	@Override
	public Collection<? extends CRL> engineGenerateCRLs(InputStream inStream)
			throws CRLException {
		if (inStream == null) {
			throw new CRLException(Messages.getString("security.153")); //$NON-NLS-1$
		}
		final ArrayList<CRL> result = new ArrayList<CRL>();
		try {
			if (!inStream.markSupported()) {
				inStream = new RestoringInputStream(inStream);
			}
			// if it is PEM encoded form this array will contain the encoding
			// so ((it is PEM) <-> (encoding != null))
			byte[] encoding = null;
			// The following by SEQUENCE ASN.1 tag, used for
			// recognizing the data format
			// (is it PKCS7 ContentInfo structure, X.509 CRL, or
			// unsupported encoding)
			int second_asn1_tag = -1;
			inStream.mark(1);
			int ch;
			while ((ch = inStream.read()) != -1) {
				// check if it is PEM encoded form
				if (ch == '-') { // beginning of PEM encoding ('-' char)
					// decode PEM chunk and store its content (ASN.1 encoding)
					encoding = decodePEM(inStream, FREE_BOUND_SUFFIX);
				} else if (ch == 0x30) { // beginning of ASN.1 sequence (0x30)
					encoding = null;
					inStream.reset();
					// prepare for data format determination
					inStream.mark(CRL_CACHE_SEED_LENGTH);
				} else { // unsupported data
					if (result.size() == 0) {
						throw new CRLException(
								Messages.getString("security.15F")); //$NON-NLS-1$
					} else {
						// it can be trailing user data,
						// so keep it in the stream
						inStream.reset();
						return result;
					}
				}
				// Check the data format
				final BerInputStream in = (encoding == null) ? new BerInputStream(
						inStream) : new BerInputStream(encoding);
				// read the next ASN.1 tag
				second_asn1_tag = in.next();
				if (encoding == null) {
					// keep whole structure in the stream
					inStream.reset();
				}
				// check if it is a TBSCertList structure
				if (second_asn1_tag != ASN1Constants.TAG_C_SEQUENCE) {
					if (result.size() == 0) {
						// there were not read X.509 CRLs, so
						// break the cycle and check
						// whether it is PKCS7 structure
						break;
					} else {
						// it can be trailing user data,
						// so return what we already read
						return result;
					}
				} else {
					if (encoding == null) {
						result.add(getCRL(inStream));
					} else {
						result.add(getCRL(encoding));
					}
				}
				inStream.mark(1);
			}
			if (result.size() != 0) {
				// the stream was read out
				return result;
			} else if (ch == -1) {
				throw new CRLException(Messages.getString("security.155")); //$NON-NLS-1$
			}
			// else: check if it is PKCS7
			if (second_asn1_tag == ASN1Constants.TAG_OID) {
				// it is PKCS7 ContentInfo structure, so decode it
				final ContentInfo info = (ContentInfo) ((encoding != null) ? ContentInfo.ASN1
						.decode(encoding) : ContentInfo.ASN1.decode(inStream));
				// retrieve SignedData
				final SignedData data = info.getSignedData();
				if (data == null) {
					throw new CRLException(Messages.getString("security.154")); //$NON-NLS-1$
				}
				final List crls = data.getCRLs();
				if (crls != null) {
					for (int i = 0; i < crls.size(); i++) {
						result.add(new X509CRLImpl((CertificateList) crls
								.get(i)));
					}
				}
				return result;
			}
			// else: Unknown data format
			throw new CRLException(Messages.getString("security.15F")); //$NON-NLS-1$
		} catch (final IOException e) {
			throw new CRLException(e);
		}
	}

	/**
	 * @see java.security.cert.CertificateFactorySpi#engineGetCertPathEncodings()
	 *      method documentation for more info
	 */
	@Override
	public Iterator<String> engineGetCertPathEncodings() {
		return X509CertPathImpl.encodings.iterator();
	}
}
