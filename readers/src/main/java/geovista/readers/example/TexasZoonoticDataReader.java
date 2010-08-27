/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.readers.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import geovista.common.data.DataSetForApps;
import geovista.common.data.GeoDataSource;
import geovista.common.data.SpatialWeights;
import geovista.readers.join.JoinPoint;
import geovista.readers.join.Joiner;
import geovista.readers.shapefile.ShapeFileDataReader;

public class TexasZoonoticDataReader implements GeoDataSource {

	final static Logger logger = Logger.getLogger(TexasZoonoticDataReader.class
			.getName());

	ArrayList<Integer> lymeYears = new ArrayList<Integer>();
	private final ArrayList<String> lymeVarNames = new ArrayList<String>();

	private final ArrayList<String> lymeCountyNames = new ArrayList<String>();
	private final ArrayList<Double> lymeAges = new ArrayList<Double>();
	private final ArrayList<String> lymeRaces = new ArrayList<String>();
	private final ArrayList<String> lymeHispanics = new ArrayList<String>();

	ArrayList<Integer> wnYears = new ArrayList<Integer>();

	private final ArrayList<String> wnCountyNames = new ArrayList<String>();
	private final ArrayList<Double> wnAges = new ArrayList<Double>();
	private final ArrayList<String> wnRaces = new ArrayList<String>();
	private final ArrayList<String> wnHispanics = new ArrayList<String>();

	public TexasZoonoticDataReader() throws IOException {
		// basePath = "C:\\temp\\data\\";
		// basePath = System.getProperty("user.dir");
		readLymeContents();
		readWestNileContents();
	}

	String basePath = "C:\\temp\\zoonotic\\";

	@SuppressWarnings("unused")
	public void readLymeContents() throws IOException {

		Scanner sc = null;
		String fileName = basePath + "lyme_2000_7.csv";
		// InputStream is = this.getClass().getResourceAsStream(
		// "resources/historic1.csv");
		FileInputStream fis = new FileInputStream(fileName);
		// use buffering, reading one line at a time
		// FileReader always assumes default encoding is OK!
		BufferedReader input = new BufferedReader(new InputStreamReader(fis));
		String namesLine = input.readLine();
		extractNames(namesLine);

		String subString = "";
		String line = "";
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		while ((line = input.readLine()) != null) {

			sc = new Scanner(line).useDelimiter(",");
			sc.next();// count
			String yearString = sc.next();
			Integer year = Integer.valueOf(yearString);
			lymeYears.add(year);

			String condition = sc.next();
			String onset = sc.next();
			String city = sc.next();
			String zip = sc.next();
			String county = sc.next();
			county = county.trim();
			lymeCountyNames.add(county);
			sc.next();// region
			sc.next();// birthdate
			String ageString = sc.next(); // age

			Double age = getDoubleVal(ageString);
			lymeAges.add(age);
			sc.next();// sex
			String race = sc.next();
			lymeRaces.add(race);
			if (sc.hasNext()) {
				lymeHispanics.add(sc.next());
			}

		}

	}

	public void readWestNileContents() throws IOException {

		Scanner sc = null;
		String fileName = basePath + "west_nile_2002_7.csv";
		// InputStream is = this.getClass().getResourceAsStream(
		// "resources/historic1.csv");
		FileInputStream fis = new FileInputStream(fileName);
		// use buffering, reading one line at a time
		// FileReader always assumes default encoding is OK!
		BufferedReader input = new BufferedReader(new InputStreamReader(fis));
		String namesLine = input.readLine();
		extractNames(namesLine);

		String line = "";

		while ((line = input.readLine()) != null) {

			sc = new Scanner(line).useDelimiter(",");

			String yearString = sc.next();
			Integer year = Integer.valueOf(yearString);
			wnYears.add(year);
			sc.next();// condition
			sc.next();// WN death

			String county = sc.next();
			county = county.trim();
			wnCountyNames.add(county);

			String ageString = sc.next(); // age
			Double age = getDoubleVal(ageString);
			wnAges.add(age);
			sc.next();// dob
			if (sc.hasNext()) {
				sc.next();// sex
			}
			if (sc.hasNext()) {
				String race = sc.next();
				wnRaces.add(race);
			}
			if (sc.hasNext()) {

				wnHispanics.add(sc.next());
			}

		}

	}

	private Double getDoubleVal(String ageString) {
		Double age = Double.NaN;
		try {
			age = Double.valueOf(ageString);
		} catch (NumberFormatException e) {
			logger.fine(e.getMessage());
		}
		return age;
	}

	private void extractNames(String namesLine) {
		Scanner scan = new Scanner(namesLine);
		scan.useDelimiter(",");
		scan.next();// skip "date"
		while (scan.hasNext()) {
			String aName = scan.next();
			logger.fine("names = " + aName);

			lymeVarNames.add(aName);
		}

	}

	public DataSetForApps getDataForApps() {
		String fileName = basePath + "tx.shp";
		ShapeFileDataReader reader = new ShapeFileDataReader();
		reader.setFileName(fileName);

		DataSetForApps dataSet = reader.getDataForApps();
		String[] countyNames = dataSet.getObservationNames();

		ArrayList<String> countyNameList = new ArrayList<String>();
		ArrayList<String> lowerCountyNameList = new ArrayList<String>();
		for (String element : countyNames) {
			countyNameList.add(element);
			lowerCountyNameList.add(element.toLowerCase());
		}

		HashSet<String> nameHash = new HashSet<String>();
		for (String nam : countyNameList) {
			nameHash.add(nam);
		}

		HashSet<String> lowerNameHash = new HashSet<String>();
		for (String nam : countyNameList) {
			lowerNameHash.add(nam.toLowerCase());
		}

		Iterator it = nameHash.iterator();
		ArrayList<String> uniqueNames = new ArrayList<String>();
		while (it.hasNext()) {
			uniqueNames.add((String) it.next());

		}

		Iterator lit = lowerNameHash.iterator();
		ArrayList<String> lowerUniqueNames = new ArrayList<String>();
		while (lit.hasNext()) {
			lowerUniqueNames.add((String) lit.next());

		}

		List<JoinPoint> lymeJoin = Joiner.leftOuterJoin(uniqueNames,
				countyNameList);

		for (JoinPoint pt : lymeJoin) {
			if (pt.getRight() == JoinPoint.NO_COORDINATE) {
				logger.info("ark ark, missed " + pt.getValue());
			}
		}

		List<JoinPoint> wnJoin = Joiner.leftOuterJoin(lowerUniqueNames,
				lowerCountyNameList);

		for (JoinPoint pt : wnJoin) {
			if (pt.getRight() == JoinPoint.NO_COORDINATE) {
				logger.info("ark ark, missed " + pt.getValue());
			}
		}

		HashMap<String, Integer> nameToIdHash = new HashMap<String, Integer>();
		Iterator<JoinPoint> jit = lymeJoin.iterator();
		while (jit.hasNext()) {
			JoinPoint jp = jit.next();
			nameToIdHash.put(jp.getValue(), jp.getRight());
		}
		HashMap<String, Integer> lowerNameToIdHash = new HashMap<String, Integer>();
		Iterator<JoinPoint> wnit = wnJoin.iterator();
		while (wnit.hasNext()) {
			JoinPoint jp = wnit.next();
			lowerNameToIdHash.put(jp.getValue(), jp.getRight());
		}

		String[] yearNames = { "Name", "Lyme 2000", "Lyme 2001", "Lyme 2002",
				"Lyme 2003", "Lyme 2004", "Lyme 2005", "Lyme 2006",
				"Lyme 2007", "W. Nile 2002", "W. Nile 2003", "W. Nile 2004",
				"W. Nile 2005", "W. Nile 2006", "W. Nile 2007", "Hisp - Lat",
				"Pop" };
		Object[] lymeDoubleData = new Object[8];// 2000-2007
		for (int i = 0; i < lymeDoubleData.length; i++) {
			double[] dat = new double[dataSet.getNumObservations()];
			lymeDoubleData[i] = dat;
		}

		compileLymeCases(nameToIdHash, lymeDoubleData);

		Object[] wnDoubleData = new Object[8];// 2000-2007
		for (int i = 0; i < wnDoubleData.length; i++) {
			double[] dat = new double[dataSet.getNumObservations()];
			wnDoubleData[i] = dat;
		}

		compileWnCases(lowerNameToIdHash, wnDoubleData);

		Object[] objectData = new Object[lymeDoubleData.length
				+ wnDoubleData.length + 4];
		objectData[0] = yearNames;
		objectData[1] = dataSet.getObservationNames();
		for (int i = 0; i < lymeDoubleData.length; i++) {
			objectData[i + 2] = lymeDoubleData[i];
		}
		for (int i = 0; i < wnDoubleData.length; i++) {
			objectData[i + 2 + lymeDoubleData.length] = wnDoubleData[i];
		}

		double[] hisp = dataSet.getNumericDataAsDouble(88);
		double[] pop = dataSet.getNumericDataAsDouble(11);
		SpatialWeights sw = dataSet.getSpatialWeights();

		objectData[objectData.length - 4] = hisp;
		objectData[objectData.length - 3] = pop;
		objectData[objectData.length - 2] = dataSet.getShapeData();
		objectData[objectData.length - 1] = sw;

		DataSetForApps newDataSet = new DataSetForApps(objectData);

		newDataSet.setSpatialWeights(sw);

		return newDataSet;
	}

	private void compileLymeCases(HashMap<String, Integer> nameToIdHash,
			Object[] doubleData) {
		for (int i = 0; i < lymeCountyNames.size(); i++) {
			Integer year = lymeYears.get(i);
			int columnIndex = year - 2000;
			double[] dat = (double[]) doubleData[columnIndex];
			int rowIndex = nameToIdHash.get(lymeCountyNames.get(i));
			dat[rowIndex]++;

		}
	}

	private void compileWnCases(HashMap<String, Integer> nameToIdHash,
			Object[] doubleData) {
		for (int i = 0; i < wnCountyNames.size(); i++) {
			Integer year = wnYears.get(i);
			int columnIndex = year - 2002;
			double[] dat = (double[]) doubleData[columnIndex];
			String name = wnCountyNames.get(i);
			logger.fine(name);

			if (name.equals("")) {
				continue;
			}
			try {
				int rowIndex = nameToIdHash.get(name.toLowerCase());
				dat[rowIndex]++;
			} catch (NullPointerException ex) {
				logger.fine("bad name:" + name);
				// ex.printStackTrace();
			}

		}
	}

	public static void main(String[] args) {

		TexasZoonoticDataReader reader = null;
		try {
			reader = new TexasZoonoticDataReader();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// reader.readIsoCodes();
		// reader.findCountryCodes();

		DataSetForApps dataSet = reader.getDataForApps();

		logger.info("All done! we have this many obs:"
				+ dataSet.getNumObservations());

	}
}
