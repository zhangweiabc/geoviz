/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Nick */
package geovista.cartogram;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/*
 * Created on Dec 10, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

/**
 * @author Nick
 * 
 */
public class FileTools {
	final static Logger logger = Logger.getLogger(FileTools.class.getName());
	public static final String WHITE_SPACE = "\r \t";

	/**
	 * @param in
	 *            - BufferedReader to read from
	 * @return the line read, or null if error/eof occured better print a stack
	 *         trace first-frank
	 */
	public static String readLine(BufferedReader in) {
		String line = null;
		try {
			line = in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return line;
	}

	public static int readInt(String line) {
		StringTokenizer st = new StringTokenizer(line);
		return Integer.parseInt(st.nextToken());
	}

	/**
	 * @param in
	 *            - BufferedReader to close
	 * @return true if successfull, false if not
	 */
	public static boolean closeFile(BufferedReader in) {
		try {
			in.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param fileName
	 *            - name of the file to be open for read
	 * @return the BufferedReader handle
	 */
	public static BufferedReader openFileRead(String fileName) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();

		}
		return in;
	}

	public static BufferedWriter openFileWrite(String fileName) {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}

	public static void main(String[] args) {
		File f = new File("C:\\geovista_old\\cartogram\\test7.shx");
		FileInputStream fio = null;
		try {
			fio = new FileInputStream(f);

			for (int i = 0; i <= 125; i++) {
				int input = fio.read();
				logger.info("" + input);

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
