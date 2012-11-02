/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.readers.text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

import geovista.common.data.DataSetForApps;

public class TextParser {

    String sep;
    InputStream is;

    private enum types {
	INTEGER, DOUBLE, STRING
    };

    final static Logger logger = Logger.getLogger(TextParser.class.getName());

    /**
     * 
     * First line is expected to contain variable names
     * 
     * @param separator
     * @param inStream
     * @return
     * 
     * 
     */

    public DataSetForApps readContents(String separator, InputStream inStream) {

	ArrayList<ArrayList<String>> contents;
	sep = separator;
	is = inStream;
	// each ArrayList<String> is a row
	// first one is the column names
	contents = new ArrayList<ArrayList<String>>();

	readAllLines(contents);

	ArrayList<String> varNamesArr = contents.get(0);

	ArrayList<TextParser.types> typesArr = findTypes(contents);

	String[] varNames = varNamesArr.toArray(new String[varNamesArr.size()]);

	Object[] dsaObjArray = new Object[varNames.length + 1];

	dsaObjArray[0] = varNames;

	for (int col = 0; col < varNames.length; col++) {
	    if (typesArr.get(col) == types.DOUBLE) {

		double[] doubleArray = findDoubleData(contents, col);

		dsaObjArray[col + 1] = doubleArray;
	    } else if (typesArr.get(col) == types.INTEGER) {
		int[] intArray = findIntData(contents, col);

		dsaObjArray[col + 1] = intArray;
	    } else if (typesArr.get(col) == types.STRING) {
		String[] stringArray = findStringData(contents, col);
		// fetch actual contents here
		dsaObjArray[col + 1] = stringArray;
	    }
	}
	DataSetForApps dsa = new DataSetForApps(dsaObjArray);
	return dsa;
    }

    private double[] findDoubleData(ArrayList<ArrayList<String>> contents,
	    int col) {
	double[] doubleArray = new double[contents.size() - 1];
	for (int i = 1; i < contents.size(); i++) {
	    ArrayList<String> row = contents.get(i);
	    doubleArray[i - 1] = Double.parseDouble(row.get(col));

	}
	return doubleArray;
    }

    private int[] findIntData(ArrayList<ArrayList<String>> contents, int col) {
	int[] intArray = new int[contents.size() - 1];
	for (int i = 1; i < contents.size(); i++) {
	    ArrayList<String> row = contents.get(i);
	    intArray[i - 1] = Integer.parseInt(row.get(col));

	}
	return intArray;
    }

    private String[] findStringData(ArrayList<ArrayList<String>> contents,
	    int col) {
	String[] stringArray = new String[contents.size() - 1];
	for (int i = 1; i < contents.size(); i++) {
	    ArrayList<String> row = contents.get(i);
	    stringArray[i - 1] = row.get(col);

	}
	return stringArray;
    }

    private ArrayList<geovista.readers.text.TextParser.types> findTypes(
	    ArrayList<ArrayList<String>> contents) {

	ArrayList<TextParser.types> typesArr = new ArrayList<TextParser.types>();

	int numColumns = contents.get(0).size();

	for (int i = 0; i < numColumns; i++) {
	    typesArr.add(types.INTEGER);
	}

	for (int row = 1; row < contents.size(); row++) {
	    ArrayList<String> lineArr = contents.get(row);
	    for (int col = 0; col < numColumns; col++) {
		String token = lineArr.get(col);
		types tokenType = getType(token);
		types currType = typesArr.get(col);
		if (token.length() > 0 && tokenType == types.STRING) {
		    typesArr.set(col, types.STRING);
		} else if (token.length() > 0 && tokenType == types.DOUBLE) {
		    if (currType == types.INTEGER) {
			typesArr.set(col, types.DOUBLE);
		    }
		}

	    }

	}

	return typesArr;
    }

    public types getType(String input) {
	if (isInteger(input)) {
	    return types.INTEGER;
	} else if (isDouble(input)) {
	    return types.DOUBLE;
	}
	return types.STRING;
    }

    public boolean isInteger(String input) {
	if (input.length() == 0) {
	    return false;
	}
	if (input.charAt(0) == "0".charAt(0)) {
	    return false;
	}

	try {
	    Integer.parseInt(input);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    public boolean isDouble(String input) {
	if (input.length() == 0) {
	    return false;
	}
	if (input.charAt(0) == "0".charAt(0)) {
	    return false;
	}

	try {
	    Double.parseDouble(input);
	    return true;
	} catch (NumberFormatException e) {
	    return false;
	}
    }

    private void readAllLines(ArrayList<ArrayList<String>> contents) {
	BufferedReader buffReader = new BufferedReader(
		new InputStreamReader(is));
	String line;
	Scanner sc;

	try {
	    // logger.info(buffReader.readLine());
	    while ((line = buffReader.readLine()) != null) {
		sc = new Scanner(line).useDelimiter(sep);
		ArrayList<String> lineArr = new ArrayList<String>();
		while (sc.hasNext()) {
		    lineArr.add(sc.next());
		}
		contents.add(lineArr);

	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    public static void main(String[] args) {

	TextParser parser = new TextParser();
	String fileName = "resources/event_data_with_geolocation_SAMPLEv12_v2.txt";

	InputStream classInputStream = TextParser.class
		.getResourceAsStream(fileName);
	DataSetForApps dsa = parser.readContents("\t", classInputStream);

	logger.info("" + dsa.getNumObservations());

    }

}