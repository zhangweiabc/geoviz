/*
 * Read files in comma separated value format.
 * Copyright (C) 2001,2002 Stephen Ostermiller <utils@Ostermiller.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See COPYING.TXT for details.
 */

package geovista.readers.csv;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Read files in comma separated value format.
 * More information about this class is available from <a href=
 * "http://ostermiller.org/utils/CSVLexer.html">ostermiller.org</a>.
 *
 * CSV is a file format used as a portable representation of a database.
 * Each line is one entry or record and the fields in a record are separated by commas.
 * Commas may be preceded or followed by arbitrary space and/or tab characters which are
 * ignored.
 * <P>
 * If field includes a comma or a new line, the whole field must be surrounded with double quotes.
 * When the field is in quotes, any quote literals must be escaped by \" Backslash
 * literals must be escaped by \\.	Otherwise a backslash an the character following it
 * will be treated as the following character, ie."\n" is equivelent to "n".  Other escape
 * sequences may be set using the setEscapes() method.	Text that comes after quotes that have
 * been closed but come before the next comma will be ignored.
 * <P>
 * Empty fields are returned as as String of length zero: "".  The following line has three empty
 * fields and three non-empty fields in it.  There is an empty field on each end, and one in the
 * middle.  One token is returned as a space.<br>
 * <pre>,second,, ,fifth,</pre>
 * <P>
 * Blank lines are always ignored.	Other lines will be ignored if they start with a
 * comment character as set by the setCommentStart() method.
 * <P>
 * An example of how CVSLexer might be used:
 * <pre>
 * CSVParser shredder = new CSVParser(System.in);
 * shredder.setCommentStart("#;!");
 * shredder.setEscapes("nrtf", "\n\r\t\f");
 * String t;
 * while ((t = shredder.nextValue()) != null) {
 *	   logger.finest("" + shredder.lastLineNumber() + " " + t);
 * }
 * </pre>
 * <P>
 * Some applications do not output CSV according to the generally accepted standards and this parse may
 * not be able to handle it.  One such application is the Microsoft Excel spreadsheet.  A
 * separate class must be use to read
 * <a href="http://ostermiller.org/utils/ExcelCSV.html">Excel CSV</a>.
 *
 * @see geovista.io.csv.ExcelCSVParser
 */
public class CSVParser implements CSVParse {
	protected final static Logger logger = Logger.getLogger(CSVParser.class.getName());
	/**
	 * Does all the dirty work.
	 * Calls for new tokens are routed through
	 * this object.
	 */
	private transient CSVLexer lexer;

	/**
	 * Token cache.  Used for when we request a token
	 * from the lexer but can't return it because its
	 * on the next line.
	 */
	private transient String tokenCache;

	/**
	 * Line cache.	The line number that goes along with
	 * the tokenCache.	Not valid if the tokenCache is
	 * null.
	 */
	private transient int lineCache;

	/**
	 * The line number the last token came from, or -1 if
	 * no tokens have been returned.
	 */
	private transient int lastLine = -1;

	/**
	 * Create a parser to parse comma separated values from
	 * an InputStream.
     * <p>
     * Byte to character conversion is done using the platform
     * default locale.
	 *
	 * @param in stream that contains comma separated values.
	 */
	public CSVParser(java.io.InputStream in){
		lexer = new CSVLexer(in);
	}

	/**
	 * Create a parser to parse comma separated values from
	 * a Reader.
	 *
	 * @param in reader that contains comma separated values.
	 */
	public CSVParser(java.io.Reader in){
		lexer = new CSVLexer(in);
	}

    /**
	 * Create a parser to parse comma separated values from
	 * an InputStream.
     * <p>
     * Byte to character conversion is done using the platform
     * default locale.
	 *
	 * @param in stream that contains comma separated values.
     * @param escapes a list of characters that will represent escape sequences.
	 * @param replacements the list of repacement characters for those escape sequences.
	 * @param commentDelims list of characters a comment line may start with.
	 */
	public CSVParser(java.io.InputStream in, String escapes, String replacements, String commentDelims){
		lexer = new CSVLexer(in);
        setEscapes(escapes, replacements);
        setCommentStart(commentDelims);
	}

	/**
	 * Create a parser to parse comma separated values from
	 * a Reader.
	 *
	 * @param in reader that contains comma separated values.
     * @param escapes a list of characters that will represent escape sequences.
	 * @param replacements the list of repacement characters for those escape sequences.
	 * @param commentDelims list of characters a comment line may start with.
	 */
	public CSVParser(java.io.Reader in, String escapes, String replacements, String commentDelims){
		lexer = new CSVLexer(in);
        setEscapes(escapes, replacements);
        setCommentStart(commentDelims);
	}

	/**
	 * get the next value.
	 *
	 * @return the next value or null if there are no more values.
     * @throws IOException if an error occurs while reading
	 */
	public String nextValue() throws IOException {
		if (tokenCache == null){
			tokenCache = lexer.getNextToken();
			lineCache = lexer.getLineNumber();
		}
		lastLine = lineCache;
		String result = tokenCache;
		tokenCache = null;
		return result;
	}

	/**
	 * Get the line number that the last token came from.
     * <p>
     * New line breaks that occur in the middle of a token are not
     * counted in the line number count.
	 *
	 * @return line number or -1 if no tokens have been returned yet.
	 */
	public int lastLineNumber(){
		return lastLine;
	}

	/**
	 * Get all the values from a line.
     * <p>
     * If the line has already been partially read, only the
     * values that have not already been read will be included.
	 *
	 * @return all the values from the line or null if there are no more values.
     * @throws IOException if an error occurs while reading
	 */
	public String[] getLine() throws IOException{
		int lineNumber = -1;
		Vector v = new Vector();
		if (tokenCache != null){
			v.add(tokenCache);
			lineNumber = lineCache;
		}
		while ((tokenCache = lexer.getNextToken()) != null
			&& (lineNumber == -1 || lexer.getLineNumber() == lineNumber)) {
			v.add(tokenCache);
			lineNumber = lexer.getLineNumber();
		}
		if (v.size() == 0){
			return null;
		}
		lastLine = lineNumber;
		lineCache = lexer.getLineNumber();
		String[] result = new String[v.size()];
		return ((String[])v.toArray(result));
	}

    /**
	 * Get all the values from the file.
     * <p>
     * If the file has already been partially read, only the
     * values that have not already been read will be included.
     * <p>
     * Each line of the file that has at least one value will be
     * represented.  Comments and empty lines are ignored.
     * <p>
     * The resulting double array may be jagged.
	 *
	 * @return all the values from the file or null if there are no more values.
     * @throws IOException if an error occurs while reading
	 */
    public String[][] getAllValues() throws IOException {
		Vector v = new Vector();
        String[] line;
        while((line = getLine()) != null){
            v.add(line);
        }
		if (v.size() == 0){
			return null;
		}
        String[][] result = new String[v.size()][];
		return ((String[][])v.toArray(result));
    }

	/**
	 * Specify escape sequences and their replacements.
	 * Escape sequences set here are in addition to \\ and \".
	 * \\ and \" are always valid escape sequences.  This method
	 * allows standard escape sequenced to be used.  For example
	 * "\n" can be set to be a newline rather than an 'n'.
	 * A common way to call this method might be:<br>
	 * <code>setEscapes("nrtf", "\n\r\t\f");</code><br>
	 * which would set the escape sequences to be the Java escape
	 * sequences.  Characters that follow a \ that are not escape
	 * sequences will still be interpreted as that character.<br>
	 * The two arguemnts to this method must be the same length.  If
	 * they are not, the longer of the two will be truncated.
	 *
	 * @param escapes a list of characters that will represent escape sequences.
	 * @param replacements the list of repacement characters for those escape sequences.
	 */
	public void setEscapes(String escapes, String replacements){
		lexer.setEscapes(escapes, replacements);
	}

	/**
	 * Set the characters that indicate a comment at the beginning of the line.
	 * For example if the string "#;!" were passed in, all of the following lines
	 * would be comments:<br>
	 * <pre> # Comment
	 * ; Another Comment
	 * ! Yet another comment</pre>
	 * By default there are no comments in CVS files.  Commas and quotes may not be
	 * used to indicate comment lines.
	 *
	 * @param commentDelims list of characters a comment line may start with.
	 */
	public void setCommentStart(String commentDelims){
		lexer.setCommentStart(commentDelims);
	}

	/**
	 * Get the number of the line from which the last value was retreived.
	 *
	 * @return line number or -1 if no tokens have been returned.
	 */
	public int getLastLineNumber(){
		return lastLine;
	}

	/**
	 * Parse the given file for comma separatad values and print the results
	 * to System.out.
	 *
	 * @param args First argument is the file name.  System.in used if no filename given.
	 */
	public static void main(String[] args) {
		InputStream in;
		try {
			if (args.length > 0){
				File f = new File(args[0]);
				if (f.exists()){
					if (f.canRead()){
						in = new FileInputStream(f);
					} else {
						throw new IOException("Could not open " + args[0]);
					}
				} else {
					throw new IOException("Could not find " + args[0]);
				}
			} else {
				in = System.in;
			}
			CSVParser p  = new CSVParser(in);
			p.setCommentStart("#;!");
			p.setEscapes("nrtf", "\n\r\t\f");
			String[] t;
			while ((t = p.getLine()) != null) {
				for (int i=0; i<t.length; i++){
					System.out.print('"' + t[i] + '"');
					if (i<t.length-1){
						System.out.print(", ");
					}
				}
				
			}
		} catch (IOException e){
			logger.finest(e.getMessage());
		}
	}

    /**
	 * Parse the comma delimited data from a string.
     * <p>
     * Only escaped backslashes and quotes will be recognized as escape sequences.
     * The data will be treated as having no comments.
	 *
	 * @param s string with comma delimited data to parse.
	 */
	public static String[][] parse(String s){
        try {
		    return (new CSVParser(new StringReader(s))).getAllValues();
        } catch (IOException x){
            return null;
        }
    }

    /**
	 * Parse the comma delimited data from a string.
     * Escaped backslashes and quotes will always recognized as escape sequences.
	 *
	 * @param s string with comma delimited data to parse.
     * @param escapes a list of additional characters that will represent escape sequences.
	 * @param replacements the list of repacement characters for those escape sequences.
	 * @param commentDelims list of characters a comment line may start with.
	 */
	public static String[][] parse(String s, String escapes, String replacements, String commentDelims){
		try {
            return (new CSVParser(new StringReader(s), escapes, replacements, commentDelims)).getAllValues();
        } catch (IOException x){
            return null;
        }
    }
}
