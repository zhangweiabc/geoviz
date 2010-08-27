/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Masahiro Takatsuka*/

package geovista.readers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*====================================================================
 Implementation of class Reader
 ====================================================================*/
/**
 * Reader reads strings and numbers from a reader or an input stream.
 * 
 * 
 * @author Masahiro Takatsuka (masa@psu.edu)
 * @see java.io.Reader
 */

public final class Reader extends java.io.Reader {
	protected static final String STREAM_CLOSED = "Stream closed";
	private BufferedReader br = null;
	private String delimiters = null;
	private final boolean retTokens;
	private boolean eof = false; // has EOF been reached

	/**
	 * Construct a newly created Reader
	 * 
	 * @param in
	 *            An InputStream.
	 * @param delim
	 *            the delimiters.
	 * @param returnTokens
	 *            flag indicating whether to return the delimiters as tokens.
	 */
	public Reader(InputStream in, String delim, boolean returnTokens) {
		br = new BufferedReader(new InputStreamReader(in));
		delimiters = (delim == null) ? "" : delim; // 2000-Jan-12 0.1.1
		retTokens = returnTokens;
	}

	/**
	 * Construct a newly created Reader
	 * 
	 * @param in
	 *            An InputStream.
	 * @param delim
	 *            the delimiters.
	 */
	public Reader(InputStream in, String delim) {
		this(in, delim, false);
	}

	/**
	 * Construct a newly created Reader
	 * 
	 * @param in
	 *            An InputStream.
	 */
	public Reader(InputStream in) {
		this(in, " \t\n\r\f", false);
	}

	/**
	 * Construct a newly created Reader
	 * 
	 * @param in
	 *            A Reader.
	 * @param delim
	 *            the delimiters.
	 * @param returnTokens
	 *            flag indicating whether to return the delimiters as tokens.
	 */
	public Reader(java.io.Reader in, String delim, boolean returnTokens) {
		br = new BufferedReader(in);
		delimiters = (delim == null) ? "" : delim; // 2000-Jan-12 0.1.1
		retTokens = returnTokens;
	}

	/**
	 * Construct a newly created Reader
	 * 
	 * @param in
	 *            A Reader.
	 * @param delim
	 *            the delimiters.
	 */
	public Reader(java.io.Reader in, String delim) {
		this(in, delim, false);
	}

	/**
	 * Construct a newly created Reader
	 * 
	 * @param in
	 *            A Reader.
	 */
	public Reader(java.io.Reader in) {
		this(in, " \t\n\r\f", false);
	}

	/**
	 * Read characters into a portion of an array.
	 * 
	 * @param cbuf
	 *            Destination buffer
	 * @param off
	 *            Offset at which to start storing characters
	 * @param len
	 *            Maximum number of characters to read
	 * 
	 * @return The number of characters read, or -1 if the end of the stream has
	 *         been reached
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	@Override
	public final int read(char cbuf[], int off, int len) throws IOException {
		synchronized (lock) {
			ensureOpen();
			return br.read(cbuf, off, len);
		}
	}

	/**
	 * Reads the specified numbers of characters and returns as a String.
	 */
	public final String read(int len) throws IOException {
		StringBuffer sbuf = new StringBuffer();
		boolean flag = false;
		for (int i = 0; i < len; i++) {
			char c = (char) br.read();
			if (c == '\n') {
				return sbuf.toString();
			}
			if (c != ' ') {
				if (!flag) {
					flag = true;
				}
				sbuf.append(c);
			}
		}
		if (!flag) {
			return null;
		}
		return sbuf.toString();
	}

	/**
	 * Reads the specified numbers of characters from the specified Reader and
	 * returns as a String.
	 */
	public final static char[] readChars(java.io.Reader reader, int len)
			throws IOException {
		char[] chars = new char[len];
		readChars(chars, reader, len);
		return chars;
	}

	/**
	 * Reads the specified numbers of characters from the specified Reader and
	 * returns as a String.
	 */
	public final static int readChars(char[] chars, java.io.Reader reader,
			int len) throws IOException {
		int i = 0;
		for (i = 0; i < len; i++) {
			char c = (char) reader.read();
			if (c == '\n') {
				break;
			}
			chars[i] = c;
		}

		return i;
	}

	/**
	 * Reads the specified numbers of characters from the specified Reader and
	 * returns as a String.
	 */
	public final static String read(java.io.Reader reader, int len)
			throws IOException {
		StringBuffer sbuf = new StringBuffer();
		boolean flag = false;
		for (int i = 0; i < len; i++) {
			char c = (char) reader.read();
			if (c == '\n') {
				return sbuf.toString();
			}
			if (c != ' ') {
				if (!flag) {
					flag = true;
				}
				sbuf.append(c);
			}
		}
		if (!flag) {
			return null;
		}
		return sbuf.toString();
	}

	/** Check to make sure that the stream has not been closed */
	private void ensureOpen() throws IOException {
		if (br == null) {
			throw new IOException(STREAM_CLOSED);
		}
	}

	/**
	 * Tell whether this stream is ready to be read. An InputReader is ready if
	 * its input buffer is not empty, or if bytes are available to be read from
	 * the underlying byte stream.
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	@Override
	public final boolean ready() throws IOException {
		synchronized (lock) {
			ensureOpen();
			return br.ready();
		}
	}

	/**
	 * Close the stream.
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	@Override
	public final void close() throws IOException {
		synchronized (lock) {
			try {
				ensureOpen();
				br.close();
				br = null;
			} catch (IOException ioe) {
				if (!ioe.getMessage().equals(STREAM_CLOSED)) {
					throw ioe;
				}
			}
		}
	}

	/**
	 * Skips delimiters.
	 */
	private void skipDelimiters() throws IOException {
		int ch;
		if (!retTokens) {
			br.mark(1); // set a mark.
			while ((ch = br.read()) != -1 && delimiters.indexOf((char) ch) >= 0) {
				br.mark(1);
			}
			if (ch == -1) { // has reached EOF.
				eof = true;
			} else {
				br.reset(); // go back one char.
			}
		}
	}

	/**
	 * Read a line of text. A line is considered to be terminated by any one of
	 * a line feed ('\n'), a carriage return ('\r'), or a carriage return
	 * followed immediately by a linefeed.
	 * 
	 * @return A String containing the contents of the line, not including any
	 *         line-termination characters, or null if the end of the stream has
	 *         been reached
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public final String readLine() throws IOException {
		String line = br.readLine();
		if (line == null) {
			eof = true;
		}
		return line;
	}

	/**
	 * Read a line of text. A line is considered to be terminated by any one of
	 * a line feed ('\n'), a carriage return ('\r'), or a carriage return
	 * followed immediately by a linefeed.
	 * 
	 * @return A String containing the contents of the line, not including any
	 *         line-termination characters, or null if the end of the stream has
	 *         been reached
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public final String readLine(String commentchars) throws IOException {
		String line = null;
		while ((line = br.readLine()) != null
				&& commentchars.indexOf(line.charAt(0)) >= 0) {

		}
		if (line == null) {
			eof = true;
		}
		return line;
	}

	/**
	 * Read a line of text. A line is considered to be terminated by any one of
	 * a line feed ('\n'), a carriage return ('\r'), or a carriage return
	 * followed immediately by a linefeed.
	 * 
	 * @return A String containing the contents of the line, not including any
	 *         line-termination characters, or null if the end of the stream has
	 *         been reached
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public final String readLine(char commentchar) throws IOException {
		String line = null;
		while ((line = br.readLine()) != null && commentchar == line.charAt(0)) {

		}
		if (line == null) {
			eof = true;
		}
		return line;
	}

	/**
	 * Read one token as a string.
	 * 
	 * @return A String containing the string, or null if the end of the stream
	 *         has been reached.
	 * 
	 * @exception IOException
	 *                If an I/O error occurs
	 */
	public final String readToken() throws IOException {
		skipDelimiters();
		if (eof) { // has reached EOF during skipDelimiters()
			return null;
		}

		StringBuffer buf = new StringBuffer();
		int ch;
		br.mark(1); // set a mark.
		while ((ch = br.read()) != -1 && delimiters.indexOf((char) ch) < 0) {
			br.mark(1);
			buf.append((char) ch);
		}
		if (ch == -1) {
			eof = true;
		}

		if (retTokens && (buf.length() == 0)
				&& (delimiters.indexOf((char) ch) >= 0)) {
			buf.append((char) ch);
		} else {
			br.reset(); // go back one char.
		}

		return buf.toString();
	}

	/**
	 * Reads a <code>boolean</code> from this data input stream. This method
	 * reads a single byte from the underlying input stream. A value of
	 * <code>0</code> represents <code>false</code>. Any other value represents
	 * <code>true</code>. This method blocks until either the byte is read, the
	 * end of the stream is detected, or an exception is thrown.
	 * 
	 * @return the <code>boolean</code> value read.
	 * @exception EOFException
	 *                if this input stream has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final boolean readBoolean() throws IOException {
		return (Boolean.valueOf(readToken())).booleanValue();
	}

	/**
	 * Reads a signed 8-bit value from this data input stream. This method reads
	 * a byte from the underlying input stream. If the byte read is
	 * <code>b</code>, where 0&nbsp;&lt;=&nbsp;<code>b</code>
	 * &nbsp;&lt;=&nbsp;255, then the result is:
	 * <ul>
	 * <code>
	 *     (byte)(b)
	 * </code>
	 * </ul>
	 * <p>
	 * This method blocks until either the byte is read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next byte of this input stream as a signed 8-bit
	 *         <code>byte</code>.
	 * @exception EOFException
	 *                if this input stream has reached the end.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final byte readByte() throws IOException {
		return Byte.parseByte(readToken());
	}

	/**
	 * Reads a signed 16-bit number from this data input stream. The method
	 * reads two bytes from the underlying input stream. If the two bytes read,
	 * in order, are <code>b1</code> and <code>b2</code>, where each of the two
	 * values is between <code>0</code> and <code>255</code>, inclusive, then
	 * the result is equal to:
	 * <ul>
	 * <code> (short)((b1 &lt;&lt; 8) | b2) </code>
	 * </ul>
	 * <p>
	 * This method blocks until the two bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this input stream, interpreted as a signed
	 *         16-bit number.
	 * @exception EOFException
	 *                if this input stream reaches the end before reading two
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final short readShort() throws IOException {
		return Short.parseShort(readToken());
	}

	/**
	 * Reads a Unicode character from this data input stream. This method reads
	 * two bytes from the underlying input stream. If the bytes read, in order,
	 * are <code>b1</code> and <code>b2</code>, where 0&nbsp;&lt;=&nbsp;
	 * <code>b1</code>, <code>b1</code>&nbsp;&lt;=&nbsp;255, then the result is
	 * equal to:
	 * <ul>
	 * <code> (char)((b1 &lt;&lt; 8) | b2) </code>
	 * </ul>
	 * <p>
	 * This method blocks until either the two bytes are read, the end of the
	 * stream is detected, or an exception is thrown.
	 * 
	 * @return the next two bytes of this input stream as a Unicode character.
	 * @exception EOFException
	 *                if this input stream reaches the end before reading two
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final char readChar() throws IOException {
		skipDelimiters();
		if (eof) { // has reached EOF during skipDelimiters()
			return (char) -1;
		}

		int ch;
		br.mark(1); // set a mark.
		if ((ch = br.read()) != -1) {
			if (!retTokens && delimiters.indexOf((char) ch) >= 0) {
				br.reset();
			} else {
				br.mark(1);
			}
		} else {
			eof = true; // has reached EOF.
		}

		return (char) ch;
	}

	/**
	 * Reads a signed 32-bit integer from this data input stream. This method
	 * reads four bytes from the underlying input stream. If the bytes read, in
	 * order, are <code>b1</code>, <code>b2</code>, <code>b3</code>, and
	 * <code>b4</code>, where 0&nbsp;&lt;=&nbsp;<code>b1</code>, <code>b2</code>
	 * , <code>b3</code>, <code>b4</code>&nbsp;&lt;=&nbsp;255, then the result
	 * is equal to:
	 * <ul>
	 * <code> (b1 &lt;&lt; 24) | (b2 &lt;&lt; 16) + (b3 &lt;&lt; 8) +b4 </code>
	 * </ul>
	 * <p>
	 * This method blocks until the four bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next four bytes of this input stream, interpreted as an
	 *         <code>int</code>.
	 * @exception EOFException
	 *                if this input stream reaches the end before reading four
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final int readInt() throws IOException {
		return Integer.parseInt(readToken());
	}

	/**
	 * Reads a signed 64-bit integer from this data input stream. This method
	 * reads eight bytes from the underlying input stream. If the bytes read, in
	 * order, are <code>b1</code>, <code>b2</code>, <code>b3</code>,
	 * <code>b4</code>, <code>b5</code>, <code>b6</code>, <code>b7</code>, and
	 * <code>b8</code>, where
	 * <ul>
	 * <code> 0 &lt;= b1, b2, b3, b4, b5, b6, b7, b8 &lt;= 255, </code>
	 * </ul>
	 * <p>
	 * then the result is equal to:
	 * <p>
	 * <blockquote>
	 * 
	 * <pre>
	 * ((long) b1 &lt;&lt; 56) + ((long) b2 &lt;&lt; 48) + ((long) b3 &lt;&lt; 40) + ((long) b4 &lt;&lt; 32)
	 * 		+ ((long) b5 &lt;&lt; 24) + (b6 &lt;&lt; 16) + (b7 &lt;&lt; 8) + b8
	 * </pre>
	 * 
	 * </blockquote>
	 * <p>
	 * This method blocks until the eight bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next eight bytes of this input stream, interpreted as a
	 *         <code>long</code>.
	 * @exception EOFException
	 *                if this input stream reaches the end before reading eight
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final long readLong() throws IOException {
		return Long.parseLong(readToken());
	}

	/**
	 * Reads a <code>float</code> from this data input stream. This method reads
	 * an <code>int</code> value as if by the <code>readInt</code> method and
	 * then converts that <code>int</code> to a <code>float</code> using the
	 * <code>intBitsToFloat</code> method in class <code>Float</code>. This
	 * method blocks until the four bytes are read, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next four bytes of this input stream, interpreted as a
	 *         <code>float</code>.
	 * @exception EOFException
	 *                if this input stream reaches the end before reading four
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final float readFloat() throws IOException {
		return Float.parseFloat(readToken());
	}

	/**
	 * Reads a <code>double</code> from this data input stream. This method
	 * reads a <code>long</code> value as if by the <code>readLong</code> method
	 * and then converts that <code>long</code> to a <code>double</code> using
	 * the <code>longBitsToDouble</code> method in class <code>Double</code>.
	 * <p>
	 * This method blocks until the eight bytes are read, the end of the stream
	 * is detected, or an exception is thrown.
	 * 
	 * @return the next eight bytes of this input stream, interpreted as a
	 *         <code>double</code>.
	 * @exception EOFException
	 *                if this input stream reaches the end before reading eight
	 *                bytes.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public final double readDouble() throws IOException {
		return Double.parseDouble(readToken());
	}

	public boolean isEof() {
		return eof;
	}
}
