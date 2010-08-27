/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.largedata;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Logger;

import cern.colt.Arrays;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.data.GeoDataSource;
import geovista.common.data.SpatialWeights;
import geovista.readers.csv.ExcelCSVParser;
import geovista.readers.example.GoogleFluDataReader;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;

@SuppressWarnings("unused")
public class DCCrimeDataReader implements GeoDataSource {

	final static Logger logger = Logger.getLogger(DCCrimeDataReader.class
			.getName());

	HashMap<Integer, Report> reports = new HashMap<Integer, Report>();

	ArrayList<Date> date = new ArrayList();
	ArrayList<Integer> day = new ArrayList();
	ArrayList<String> name = new ArrayList();
	ArrayList<Integer> nCases = new ArrayList();
	ArrayList<Integer> nRespCases = new ArrayList();
	HashMap<String, Integer> idName = new HashMap<String, Integer>();
	HashMap<Integer, String> nameID = new HashMap();
	HashSet<String> observers = new HashSet();
	HashSet<String> species = new HashSet();
	ArrayList<Coordinate> locations = new ArrayList();
	Geometry[] theGeoms;
	Shape[] shapes;
	SpatialWeights weights;

	Rectangle2D[] bounds;

	HashMap<Integer, Coordinate> coordinateMap;
	HashMap<Integer, String> descMap;

	HashMap<String, Integer> nameIDs;
	HashMap<Geometry, Integer> geomIDs;
	HashMap<Geometry, String> geomNames;
	HashMap<Geometry, String> nameGeoms;
	private static String resource_shapefile = "resources/tr11_d00.shp";
	private static String resource_fips = "resources/tr11_d00_fips.csv";
	private static String resource_crimes = "resources/clean_crimes.csv";

	DataSetForApps dataSet;

	public DCCrimeDataReader() {
		coordinateMap = new HashMap<Integer, Coordinate>();
		descMap = new HashMap<Integer, String>();
	}

	public void readDCShapefile() {

		logger.info("reading DC shapefile");
		InputStream shpStream = this.getClass().getResourceAsStream(
				resource_shapefile);

		InputStream fipsStream = this.getClass().getResourceAsStream(
				resource_shapefile);
		BufferedReader fipsReader = new BufferedReader(new InputStreamReader(
				fipsStream));
		theGeoms = ShapeFileDataReader.getGeoms(shpStream, 0);

		weights = ShapeFileDataReader.findSpatialWeights(theGeoms);
		// theGeoms = ShapeFileDataReader.makeSimplerGeoms(theGeoms, 1);
		shapes = ShapeFileDataReader.geomsToShapes(theGeoms);

		bounds = new Rectangle2D[theGeoms.length];
		for (int i = 0; i < theGeoms.length; i++) {

			bounds[i] = shapes[i].getBounds2D();
		}

		int nPolys = 0;
		geomNames = new HashMap<Geometry, String>();
		nameGeoms = new HashMap<Geometry, String>();
		geomIDs = new HashMap<Geometry, Integer>();
		nameIDs = new HashMap<String, Integer>();
		try {
			fipsReader.readLine();

			for (int j = 0; j < theGeoms.length; j++) {
				Geometry geom = theGeoms[j];
				geomIDs.put(geom, j);
				String fips = fipsReader.readLine();
				geomNames.put(geom, fips);
				nameGeoms.put(geom, fips);
				nameIDs.put(fips, j);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Geometry[] geoms = new Geometry[theGeoms.length];
		// for (int i=0;i)

		// ShapeFileDataWriter.writeShapefile()

	}

	public void readStatesShapefile() {
		logger.info("getting states info");
		GoogleFluDataReader states = null;
		try {
			states = new GoogleFluDataReader();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Shape[] theShapes = states.getDataForApps().getShapeData();
		theGeoms = ShapeFileDataReader.shapesToGeoms(theShapes);
		geomIDs = new HashMap<Geometry, Integer>();
		for (int j = 0; j < theGeoms.length; j++) {
			Geometry geom = theGeoms[j];
			geomIDs.put(geom, j);

		}
		ShapeFileProjection proj = new ShapeFileProjection();
		proj.setInputDataSetForApps(states.getDataForApps());
		dataSet = proj.getOutputDataSetForApps();

	}

	private static double isDouble(String str) {
		try {
			return Double.parseDouble(str);

		} catch (NumberFormatException nfe) {
			return Double.NaN;
		}
	}

	private String extractRest(BufferedReader input) {
		char quote = (char) 34;
		StringBuilder builder = new StringBuilder();
		try {
			int val = input.read();
			if (val == -1) {
				return "";
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return "";
	}

	private static final Calendar now = Calendar.getInstance();

	public static Calendar parseTimestamp(String timestamp) {
		/*
		 * * we specify Locale.US since months are in english
		 */
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
		Date d = null;
		try {
			d = sdf.parse(timestamp);

		} catch (ParseException e) {
			return now;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		return cal;
	}

	/*
	 * 
	 * Data notes
	 * 
	 * 0:nid 1:ccn 2:epoch 3:year 4:month 5:week 6:compositeyear
	 * 7:compositemonth 8:compositeweek 9:shift 10:offense 11:method
	 * 12:blocksiteaddress 13:narrative 14:latitude 15:longitude 16:district
	 * 17:psa 18:the_geom 19:grid
	 */

	private class Report {
		int id;

		Date timeStamp;
		String description;
		Point location;
		String tractName;
		String offence;
		String method;
		int year;

	}

	private final Report emptyReport = new Report();

	private Report parseReport(String[] report, int id, GeometryFactory fact) {
		Report rep = new Report();

		rep.id = id;

		// Calendar when = parseTimestamp(report[2]);
		// if (when == now) {
		// logger.info("no date for " + id);
		// return emptyReport;
		// }
		try {
			Long longTime = Long.parseLong(report[2]);
			longTime = longTime * 1000;
			rep.timeStamp = new Date(longTime);

		} catch (Exception ex) {

			logger.info("cant parse timestamp " + id);

		}
		String desc = report[4];
		double lat = Double.valueOf(report[14]);
		double longit = Double.valueOf(report[15]);

		Coordinate coord = new Coordinate(longit, lat);
		coordinateMap.put(id, coord);
		descMap.put(id, desc);

		rep.location = fact.createPoint(coord);
		rep.offence = report[10];
		rep.year = Integer.valueOf(report[3]);
		rep.method = report[11];

		// Geometry geom = findGeom(rep.location);//too slow with 150,000
		// rep.tractName = nameGeoms.get(geom);
		return rep;
	}

	public void readCrimeContents() {
		long startTime = System.currentTimeMillis();
		logger.info("readingCrimeContents");
		try {
			InputStream fis = this.getClass().getResourceAsStream(
					resource_crimes);
			BufferedReader input = new BufferedReader(
					new InputStreamReader(fis));
			// input.readLine();
			ExcelCSVParser parser = new ExcelCSVParser(fis);
			String[][] vals = parser.getAllValues();

			String[] firstLine = vals[1];
			// logger.info(Arrays.toString(firstLine));
			int id = 0;
			GeometryFactory fact = new GeometryFactory();
			for (id = 1; id < vals.length; id++) {
				// logger.info("parsing report " + id);
				String[] report = vals[id];
				Report rep = parseReport(report, id, fact);
				if (rep != emptyReport) {
					reports.put(id, rep);
				}
			}

			long endTime = System.currentTimeMillis();
			logger.info((endTime - startTime) / 1000 + " seconds");
			logger.info(id + " reports ");
			logger.info(reports.size() + " valid reports ");

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void countHits(int[] hits, Report element) {

		for (int i = 0; i < theGeoms.length; i++) {
			Geometry theGeom = theGeoms[i];

			if (theGeom.contains(element.location)) {
				element.tractName = geomNames.get(theGeom);
				hits[i]++;
				break;
			}

		}

	}

	private Geometry findGeom(Point coord) {
		for (Geometry theGeom : theGeoms) {
			if (theGeom.contains(coord)) {
				return theGeom;
			}

		}
		return null;
	}

	public int[] countAllHits() {
		return countCertainHits(reports.values());

	}

	int[] countCertainHits(Collection<Report> reps) {
		int[] hitCount = new int[theGeoms.length];

		for (Report element : reps) {

			countHits(hitCount, element);
		}
		return hitCount;
	}

	private void readIDCodes() {
		try {
			FileInputStream isoGtdStream = new FileInputStream(
					"C:\\data\\grants\\nevac\\purdue\\id_name.csv");
			// use buffering, reading one line at a time
			// FileReader always assumes default encoding is OK!
			BufferedReader input = new BufferedReader(new InputStreamReader(
					isoGtdStream));
			try {
				String line = null; // not declared within while loop
				/*
				 * readLine is a bit quirky : it returns the content of a line
				 * MINUS the newline. it returns null only for the END of the
				 * stream. it returns an empty String if two newlines appear in
				 * a row.
				 */
				String name = "";
				String ID = "";
				while ((line = input.readLine()) != null) {
					Scanner scan = new Scanner(line).useDelimiter(",");

					ID = scan.next();

					name = scan.next();

					if (idName.containsKey(name)) {
						logger.severe("hit duplicate name, bad bad");
					}

					idName.put(name, Integer.valueOf(ID));
					nameID.put(Integer.valueOf(ID), name);
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	private DataSetForApps makeDataSetForApps() {
		logger.info("making dsa");
		int nTracts = geomIDs.keySet().size();
		if (nTracts == 0) {
			logger.severe("data not loaded");
			return null;
		}
		int numYears = 5;
		Object[] numericalArrays = new Object[(numYears * 4) + 1];
		for (int i = 0; i < numericalArrays.length; i++) {
			double[] data = new double[nTracts];
			numericalArrays[i] = data;
		}
		ArrayList<String> yearHeadings = makeYearHeadings(2006, 2010);

		for (Report rep : reports.values()) {
			int year = rep.year;
			if (year > 2010 || year < 2006) {
				logger.info("wrong year " + year);
			}
			if (rep.tractName == null) {
				continue;
			}
			int id = nameIDs.get(rep.tractName);
			if (year == 2006) {
				double[] data = (double[]) numericalArrays[0];
				data[id] = data[id] + 1;
				if (rep.offence.equals("ARSON")) {
					double[] arsonData = (double[]) numericalArrays[5];
					arsonData[id] = arsonData[id] + 1;
				}
				if (rep.offence.equals("HOMICIDE")) {
					double[] homicideData = (double[]) numericalArrays[10];
					homicideData[id] = homicideData[id] + 1;
				}
				if (rep.method.equals("GUN")) {
					double[] gunData = (double[]) numericalArrays[15];
					gunData[id] = gunData[id] + 1;
				}

			}
			if (year == 2007) {
				double[] data = (double[]) numericalArrays[1];
				data[id] = data[id] + 1;
				if (rep.offence.equals("ARSON")) {
					double[] arsonData = (double[]) numericalArrays[6];
					arsonData[id] = arsonData[id] + 1;
				}
				if (rep.offence.equals("HOMICIDE")) {
					double[] homicideData = (double[]) numericalArrays[11];
					homicideData[id] = homicideData[id] + 1;
				}
				if (rep.method.equals("GUN")) {
					double[] gunData = (double[]) numericalArrays[16];
					gunData[id] = gunData[id] + 1;
				}
			}
			if (year == 2008) {
				double[] data = (double[]) numericalArrays[2];
				data[id] = data[id] + 1;
				if (rep.offence.equals("ARSON")) {
					double[] arsonData = (double[]) numericalArrays[7];
					arsonData[id] = arsonData[id] + 1;
				}
				if (rep.offence.equals("HOMICIDE")) {
					double[] homicideData = (double[]) numericalArrays[12];
					homicideData[id] = homicideData[id] + 1;
				}
				if (rep.method.equals("GUN")) {
					double[] gunData = (double[]) numericalArrays[17];
					gunData[id] = gunData[id] + 1;
				}
			}
			if (year == 2009) {
				double[] data = (double[]) numericalArrays[3];
				data[id] = data[id] + 1;
				if (rep.offence.equals("ARSON")) {
					double[] arsonData = (double[]) numericalArrays[8];
					arsonData[id] = arsonData[id] + 1;
				}
				if (rep.offence.equals("HOMICIDE")) {
					double[] homicideData = (double[]) numericalArrays[13];
					homicideData[id] = homicideData[id] + 1;
				}
				if (rep.method.equals("GUN")) {
					double[] gunData = (double[]) numericalArrays[18];
					gunData[id] = gunData[id] + 1;
				}
			}
			if (year == 2010) {
				double[] data = (double[]) numericalArrays[4];
				data[id] = data[id] + 1;
				if (rep.offence.equals("ARSON")) {
					double[] arsonData = (double[]) numericalArrays[9];
					arsonData[id] = arsonData[id] + 1;
				}
				if (rep.offence.equals("HOMICIDE")) {
					double[] homicideData = (double[]) numericalArrays[14];
					homicideData[id] = homicideData[id] + 1;
				}
				if (rep.method.equals("GUN")) {
					double[] gunData = (double[]) numericalArrays[19];
					gunData[id] = gunData[id] + 1;
				}
			}

		}

		String[] varNames = new String[numericalArrays.length];

		String[] obsNames = makeObsNames(nTracts);
		numericalArrays[numericalArrays.length - 1] = obsNames;
		for (int i = 0; i < yearHeadings.size(); i++) {
			varNames[i] = yearHeadings.get(i);

		}
		varNames[varNames.length - 1] = "name";

		DataSetForApps dataSet = new DataSetForApps(varNames, numericalArrays,
				shapes, weights);

		return dataSet;
	}

	private void transformStdDev(DataSetForApps data) {
		double[] rowData = new double[data.getNumberNumericAttributes()];

		for (int obs = 0; obs < data.getNumObservations(); obs++) {
			for (int var = 0; var < data.getNumberNumericAttributes(); var++) {
				rowData[var] = data.getNumericValueAsDouble(var, obs);
			}
			double[] zScores = DescriptiveStatistics.calculateZScores(rowData);
			for (int var = 0; var < data.getNumberNumericAttributes(); var++) {
				double[] numericVar = data.getNumericDataAsDouble(var);
				numericVar[obs] = zScores[var];
			}
		}

	}

	private ArrayList<String> makeYearHeadings(int minVal, int maxVal) {
		int numBins = (maxVal - minVal) + 1;
		ArrayList<String> headings = new ArrayList<String>();

		for (int i = 0; i < numBins; i++) {
			String label = Integer.toString(minVal + i);
			label = "All_" + label;
			headings.add(label);

		}

		for (int i = 0; i < numBins; i++) {
			String label = Integer.toString(minVal + i);
			label = "Arson_" + label;
			headings.add(label);

		}
		for (int i = 0; i < numBins; i++) {
			String label = Integer.toString(minVal + i);
			label = "Homic_" + label;
			headings.add(label);

		}
		for (int i = 0; i < numBins; i++) {
			String label = Integer.toString(minVal + i);
			label = "Gun_" + label;
			headings.add(label);

		}
		return headings;

	}

	private String[] makeObsNames(int nCountries) {
		String[] obsNames = new String[nCountries];
		for (int i = 0; i < nCountries; i++) {

			String name = nameID.get(i);
			obsNames[i] = name;
		}
		return obsNames;
	}

	public DataSetForApps getDataForApps() {
		logger.info("getDataForApps");
		readDCShapefile();
		readCrimeContents();
		countAllHits();
		return makeDataSetForApps();

	}

	public static void printMemory() {
		Runtime runtime = Runtime.getRuntime();

		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		logger.info("free memory: " + freeMemory / 1024);
		logger.info("allocated memory: " + allocatedMemory / 1024);
		logger.info("max memory: " + maxMemory / 1024);
		logger.info("total free memory: "
				+ (freeMemory + (maxMemory - allocatedMemory)) / 1024);
	}

	public Collection<Report> getReports(ArrayList<Integer> ids) {
		Collection<Report> returnedReports = new ArrayList<Report>();
		for (Integer i : ids) {
			Report rep = reports.get(i);
			returnedReports.add(rep);
		}

		return returnedReports;

	}

	public static void main(String[] args) {

		DCCrimeDataReader reader = new DCCrimeDataReader();

		reader.getDataForApps();
		logger.info("all done");
		System.exit(0);
		reader.readDCShapefile();
		reader.readCrimeContents();

		logger.info("starting hit count");

		int[] hits = reader.countAllHits();
		logger.info(Arrays.toString(hits));

		HashMap<Integer, String> descs = reader.descMap;
		ArrayList<Integer> childHits = new ArrayList<Integer>();
		for (Integer i : descs.keySet()) {
			String desc = descs.get(i);
			if (desc.contains("children") || desc.contains("child")) {
				childHits.add(i);
			}
		}

		int[] childHitCounts = reader.countCertainHits(reader
				.getReports(childHits));

		// logger.info(Arrays.toString(childHitCounts));

		float[] proportions = new float[childHitCounts.length];
		for (int i = 0; i < proportions.length; i++) {
			proportions[i] = (float) childHitCounts[i] / (float) hits[i];
		}

		// logger.info(Arrays.toString(proportions));

		logger.info("All done!");

	}
}
