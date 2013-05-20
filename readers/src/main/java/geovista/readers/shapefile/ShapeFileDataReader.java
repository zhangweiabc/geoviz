/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.readers.shapefile;

import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import org.geotools.data.shapefile.Lock;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.geotools.data.shapefile.shp.ShapefileReader;

import com.vividsolutions.jts.awt.ShapeReader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

import geovista.common.data.DataSetForApps;
import geovista.common.data.SpatialWeights;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.jts.Java2DConverter;
import geovista.common.jts.LiteShape;
import geovista.readers.csv.GeogCSVReader;
import geovista.readers.geog.AttributeDescriptionFile;

/**
 * Takes a file name and returns an Object[] with: Object[0] = names of
 * variables 0bject[1] = data (double[], int[], or String[]) 0bject[1] = data
 * (double[], int[], or String[]) ... Object[n-1] = the shapefile data
 * 
 * also see DBaseFile, ShapeFile
 * 
 */
public class ShapeFileDataReader implements Serializable {
    protected final static Logger logger = Logger
	    .getLogger(ShapeFileDataReader.class.getName());
    public static final String COMMAND_DATA_SET_MADE = "dataMade";
    public static final int FILE_TYPE_DBF = 0;
    public static final int FILE_TYPE_CSV = 1;

    protected transient DataSetForApps dataForApps;
    protected transient String fileName;
    protected transient EventListenerList listenerList;

    public static double tolerance = .1;

    public ShapeFileDataReader() {
	super();
	listenerList = new EventListenerList();

    }

    /**
     * fah we are going to try to render geometries directly... or at least let
     * each map get appropriately simplified geometries and turn those into
     * shapes. private Shape[] makeShapes(ShapeFile shp) { Shape[] shapes =
     * (Shape[]) this.transform(shp);
     * 
     * return shapes; }
     */
    /**
     * fah commented out for now -- no references to this method //return the
     * data without attribute names public Object[] getDBData() { Object[]
     * dbData = null;
     * 
     * try { String dbFileName = fileName + ".dbf"; DbaseFileReader dBaseReader
     * = null;
     * 
     * DBaseFile dbf = new DBaseFile(dbFileName); dbData = new
     * Object[dbf.getDataSet().length]; for (int i = 0; i < dbData.length - 1;
     * i++) dbData[i] = dbf.getDataSet()[i + 1]; } catch (Exception ex) {
     * ex.printStackTrace(); } return dbData; }
     */
    // return attribute names of the DB Data
    @SuppressWarnings("unused")
    private static String[] getFieldNames(String fileName) {

	String dbFileName = fileName + ".dbf";
	FileInputStream fis = null;
	try {
	    fis = new FileInputStream(dbFileName);
	} catch (FileNotFoundException e) {
	    logger.severe("dbf file not found, file = " + dbFileName);
	    e.printStackTrace();
	}
	String[] fieldNames = getFieldNames(fis);
	return fieldNames;
    }

    private static String[] getFieldNames(FileInputStream fis) {
	String[] fieldNames = null;
	try {

	    ReadableByteChannel dChan = Channels.newChannel(fis);
	    DbaseFileReader dBaseReader = new DbaseFileReader(dChan, true);
	    DbaseFileHeader dBaseHeader = dBaseReader.getHeader();

	    fieldNames = new String[dBaseHeader.getNumFields()];
	    for (int i = 0; i < fieldNames.length; i++) {
		fieldNames[i] = dBaseHeader.getFieldName(i);
	    }

	    // dBaseReader.close();
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
	return fieldNames;
    }

    public static Geometry[] getGeoms(String baseFileName) {

	try {
	    FileInputStream shpStream = new FileInputStream(baseFileName
		    + ".shp");
	    return getGeoms(shpStream);
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
	return null;

    }

    public static Geometry[] getGeoms(InputStream shpStream) {
	try {
	    ShapefileReader shpReader;
	    ReadableByteChannel shpChan = Channels.newChannel(shpStream);
	    Lock lock = new Lock();
	    shpReader = new ShapefileReader(shpChan, lock);
	    Vector<Geometry> shapes = new Vector<Geometry>();
	    while (shpReader.hasNext()) {
		Geometry geom = (Geometry) shpReader.nextRecord().shape();
		// this helps ensure valid topology
		// geom = geom.buffer(0);
		shapes.add(geom);
	    }
	    Geometry[] geomArray = new Geometry[shapes.size()];
	    for (int i = 0; i < geomArray.length; i++) {
		geomArray[i] = shapes.elementAt(i);
	    }
	    shpReader.close();

	    return geomArray;
	} catch (IOException ex) {
	    System.err.println("couldn't read shapestream ");
	    ex.printStackTrace();
	}
	return null;
    }

    public static Geometry[] getGeoms(InputStream shpStream, double tolerance) {
	ShapeFileDataReader.tolerance = tolerance;
	Geometry[] simplerGeoms = makeSimplerGeoms(
		ShapeFileDataReader.getGeoms(shpStream), tolerance);

	return simplerGeoms;
    }

    public static Shape[] getShapes(InputStream shpStream) {
	Geometry[] geoms = getGeoms(shpStream);

	// SpatialWeights weights = findSpatialWeights(geoms);

	Geometry[] simplerGeoms = makeSimplerGeoms(geoms, tolerance);

	Shape[] shapes = geomsToShapes(simplerGeoms);
	// Shape[] shapes = getLiteShapes(simplerGeoms);

	return shapes;
    }

    @SuppressWarnings("unused")
    private static LiteShape[] getLiteShapes(Geometry[] geoms) {
	LiteShape[] liteShapes = new LiteShape[geoms.length];
	AffineTransform xForm = new AffineTransform();
	for (int i = 0; i < geoms.length; i++) {
	    LiteShape shp = new LiteShape(geoms[i], xForm, false);
	    liteShapes[i] = shp;
	}
	return liteShapes;
    }

    public static SpatialWeights getWeights(InputStream shpStream) {
	Geometry[] geoms = getGeoms(shpStream);

	SpatialWeights weights = findSpatialWeights(geoms);
	return weights;
    }

    protected static DataSetForApps makeDataSetForApps(String fileName) {
	logger.fine("making dataset from shp and dbf in local file, fileName = "
		+ fileName);
	FileInputStream shpStream = null;
	FileInputStream dbfStream = null;
	try {
	    shpStream = new FileInputStream(fileName + ".shp");
	    dbfStream = new FileInputStream(fileName + ".dbf");

	} catch (IOException ex) {
	    logger.severe("Error reading file, file name = " + fileName);
	    ex.printStackTrace();
	}

	DataSetForApps dataForApps = makeDataSetForApps(shpStream, dbfStream);
	return dataForApps;

    }

    public static DataSetForApps makeDataSetForAppsCsv(Class clazz, String name) {
	DataSetForApps shpData = null;
	logger.fine("making dataset from shp and cvs class resource, name = "
		+ name);
	try {

	    InputStream isCSV = clazz.getResourceAsStream("resources/" + name
		    + ".csv");
	    InputStream isSHP = clazz.getResourceAsStream("resources/" + name
		    + ".shp");

	    if (isCSV == null) {
		logger.severe("Could not find " + clazz.getName()
			+ "/resources/" + name + ".csv");
		return null;
	    }
	    if (isSHP == null) {
		logger.severe("Could not find " + clazz.getName()
			+ "/resources/" + name + ".shp");
		return null;
	    }

	    shpData = ShapeFileDataReader.makeDataSetForAppsCsv(isSHP, isCSV);

	    isCSV.close();
	    isSHP.close();

	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	return shpData;
    }

    public static DataSetForApps makeDataSetForApps(Class clazz, String name) {
	DataSetForApps shpData = null;
	logger.fine("making dataset from class dbf and shp resource, name = "
		+ name);
	try {

	    InputStream isCSV = clazz.getResourceAsStream("resources/" + name
		    + ".dbf");
	    if (isCSV == null) {
		logger.severe("cannot find " + clazz.getName() + "/resources/"
			+ name + ".dbf");
		return null;
	    }
	    InputStream isSHP = clazz.getResourceAsStream("resources/" + name
		    + ".shp");
	    if (isSHP == null) {
		logger.severe("cannot find " + clazz.getName() + "/resources/"
			+ name + ".shp");
		return null;
	    }

	    shpData = ShapeFileDataReader.makeDataSetForApps(isSHP, isCSV);

	    isCSV.close();
	    isSHP.close();

	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	return shpData;
    }

    public static DataSetForApps makeDataSetForAppsCsv(InputStream shpStream,
	    InputStream isCSV) {
	Object[] allData;

	GeogCSVReader csv = new GeogCSVReader();
	Object[] dbColumnData = csv.readFile(isCSV);
	// isCSV.close();

	allData = new Object[dbColumnData.length + 2];
	for (int i = 0; i < dbColumnData.length; i++) {
	    allData[i] = dbColumnData[i];
	}

	Geometry[] geoms = getGeoms(shpStream);
	SpatialWeights weights = findSpatialWeights(geoms);

	Geometry[] simplerGeoms = makeSimplerGeoms(geoms, tolerance);

	Shape[] shapes = geomsToShapes(simplerGeoms);

	allData[dbColumnData.length] = shapes;
	allData[dbColumnData.length + 1] = weights;
	// AttributeDescriptionFile desc = null;

	// desc = new AttributeDescriptionFile(fileName + ".desc");// XXX what
	// if this
	// does not
	// exist?

	// allData[dbColumnData.length + 1] = desc.getAttributeDescriptions();

	// this.fireActionPerformed(COMMAND_DATA_SET_MADE);
	// int type =
	// this.getDataSetForAppsSpatialType(shpFile.getFileHeader().getShapeType
	// ());
	DataSetForApps dataForApps = new DataSetForApps(allData);

	// this.dataForApps.setSpatialType(type);
	return dataForApps;
    }

    private static DataSetForApps makeDataSetForApps(InputStream shpStream,
	    InputStream dbfStream) {
	Object[] allData;

	Object[] dbColumnData = getDbfData(dbfStream);
	// String[] fieldNames = getFieldNames(dbfStream);

	allData = new Object[dbColumnData.length + 2];
	for (int i = 0; i < dbColumnData.length; i++) {
	    allData[i] = dbColumnData[i];
	}

	Geometry[] geoms = getGeoms(shpStream);

	// for carly
	// int[] fips = (int[]) allData[1];
	// for (int i = 0; i < geoms.length; i++) {
	// int oneCode = fips[i];
	// String stringFips = String.valueOf(oneCode);
	// stringFips = "00" + stringFips;
	// int len = stringFips.length();
	// stringFips = stringFips.substring(len - 5, len);
	// logger.fine(stringFips + "," + geoms[i].getCoordinate().x
	// + "," + geoms[i].getCoordinate().y);
	// }

	try {
	    if ((geoms[0] instanceof com.vividsolutions.jts.geom.Point) == false) {
		logger.fine(geoms[0].getClass().toString());
		// XXX should do by distance or similar
		SpatialWeights weights = findSpatialWeights(geoms);

		allData[dbColumnData.length + 1] = weights;
	    }
	} catch (Exception ex) {
	    logger.severe("ack ack topology problem!");
	    ex.printStackTrace();
	}

	Geometry[] simplerGeoms = makeSimplerGeoms(geoms, tolerance);

	Shape[] shapes = geomsToShapes(simplerGeoms);

	allData[dbColumnData.length] = shapes;

	// AttributeDescriptionFile desc = null;

	// desc = new AttributeDescriptionFile(fileName + ".desc");// XXX what
	// if this
	// does not
	// exist?

	// allData[dbColumnData.length + 1] = desc.getAttributeDescriptions();

	// this.fireActionPerformed(COMMAND_DATA_SET_MADE);
	// int type =
	// this.getDataSetForAppsSpatialType(shpFile.getFileHeader().getShapeType
	// ());
	DataSetForApps dataForApps = new DataSetForApps(allData);
	// this.dataForApps.setSpatialType(type);
	return dataForApps;
    }

    public static Shape[] geomsToShapes(Geometry[] simplerGeoms) {
	Java2DConverter converter = new Java2DConverter(new AffineTransform());
	Shape[] shapes = new Shape[simplerGeoms.length];
	long pointCount = 0;
	for (int i = 0; i < shapes.length; i++) {
	    Geometry geometry = simplerGeoms[i];
	    pointCount = pointCount + geometry.getNumPoints();
	    try {
		Shape g = converter.toShape(geometry);
		shapes[i] = g;
	    } catch (NoninvertibleTransformException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	logger.fine("n points = " + pointCount);
	return shapes;
    }

    public static Geometry[] shapesToGeoms(Shape[] simplerGeoms) {
	GeometryFactory fact = new GeometryFactory();
	ShapeReader reader = new ShapeReader(fact);
	Geometry[] geoms = new Geometry[simplerGeoms.length];
	long pointCount = 0;
	for (int i = 0; i < geoms.length; i++) {
	    Shape shp = simplerGeoms[i];

	    Geometry g = reader.read(shp.getPathIterator(null));
	    geoms[i] = g;

	}
	logger.fine("n points = " + pointCount);
	return geoms;
    }

    public static Geometry[] makeSimplerGeoms(Geometry[] geoms,
	    double distanceTolerance) {
	logger.fine("simplifying geoms");
	Geometry[] simplerGeoms = new Geometry[geoms.length];

	for (int i = 0; i < geoms.length; i++) {
	    Geometry geom = geoms[i];
	    Geometry simplerGeom = DouglasPeuckerSimplifier.simplify(geom,
		    distanceTolerance);
	    simplerGeoms[i] = simplerGeom;
	}
	return simplerGeoms;
    }

    public static SpatialWeights findSpatialWeights(Geometry[] geoms) {
	SpatialWeights weights = new SpatialWeights(geoms.length);
	ArrayList<Geometry> geomList = new ArrayList();
	for (Geometry g : geoms) {
	    geomList.add(g);
	}
	weights.findNeighbors(geomList);
	return weights;
    }

    @SuppressWarnings("unused")
    private static Object[] getDbfColumns(String fileName) {

	String dbFileName = fileName + ".dbf";
	FileInputStream fis = null;
	try {
	    fis = new FileInputStream(dbFileName);
	} catch (FileNotFoundException e) {
	    logger.severe("couldn't find file " + dbFileName);
	    e.printStackTrace();
	}

	Object[] dbColumnData = getDbfColumns(fis);
	return dbColumnData;
    }

    private static Object[] getDbfData(InputStream fis) {
	Object[] dbColumnData = null;
	try {

	    ReadableByteChannel dChan = Channels.newChannel(fis);
	    DbaseFileReader dBaseReader = new DbaseFileReader(dChan, true);
	    DbaseFileHeader dBaseHeader = dBaseReader.getHeader();

	    int nFields = dBaseHeader.getNumFields();
	    int nRows = dBaseHeader.getNumRecords();
	    dbColumnData = new Object[nFields + 1];// +1 for the variable names
	    String[] fieldNames = new String[dBaseHeader.getNumFields()];
	    for (int i = 0; i < fieldNames.length; i++) {
		fieldNames[i] = dBaseHeader.getFieldName(i);
	    }
	    dbColumnData[0] = fieldNames;
	    // set up each array with it's proper type
	    for (int i = 0; i < nFields; i++) {
		if (dBaseHeader.getFieldClass(i).equals(java.lang.String.class)) {
		    dbColumnData[i + 1] = new String[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(
			java.lang.Double.class)) {
		    dbColumnData[i + 1] = new double[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(
			java.lang.Integer.class)) {
		    dbColumnData[i + 1] = new int[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(
			java.lang.Boolean.class)) {
		    dbColumnData[i + 1] = new boolean[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(Date.class)) {
		    dbColumnData[i + 1] = new Date[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(Long.class)) { // undocumented
		    // in
		    // geotools
		    dbColumnData[i + 1] = new int[nRows];
		}

		else {
		    logger.severe("hit unknown type while reading dbf, class = "
			    + dBaseHeader.getFieldClass(i).getName());
		}
	    }
	    int recordNum = 0;
	    while (dBaseReader.hasNext()) {

		Object[] rowData = dBaseReader.readEntry();
		for (int fieldNum = 0; fieldNum < nFields; fieldNum++) {
		    if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.String.class)) {
			String[] stringCol = (String[]) dbColumnData[fieldNum + 1];
			stringCol[recordNum] = (String) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.Double.class)) {
			double[] doubleCol = (double[]) dbColumnData[fieldNum + 1];
			doubleCol[recordNum] = (Double) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.Integer.class)) {
			int[] intCol = (int[]) dbColumnData[fieldNum + 1];
			if (rowData[fieldNum] instanceof Double) {
			    Double dNum = (Double) rowData[fieldNum];
			    intCol[recordNum] = (int) Math.round(dNum);
			} else {

			    intCol[recordNum] = (Integer) rowData[fieldNum];
			}
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.Boolean.class)) {
			boolean[] boolCol = (boolean[]) dbColumnData[fieldNum + 1];
			boolCol[recordNum] = (Boolean) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    Date.class)) {
			Date[] dateCol = (Date[]) dbColumnData[fieldNum + 1];
			dateCol[recordNum] = (Date) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    Long.class)) {
			int[] intCol = (int[]) dbColumnData[fieldNum + 1];
			Object datum = rowData[fieldNum];
			Long longDatum = null;
			if (datum instanceof Double) {
			    longDatum = ((Double) datum).longValue();
			} else {
			    longDatum = (Long) rowData[fieldNum];
			}

			intCol[recordNum] = longDatum.intValue();
		    }

		}
		recordNum++;
	    }
	    dBaseReader.close(); // always do this
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
	return dbColumnData;
    }

    private static Object[] getDbfColumns(InputStream fis) {
	Object[] dbColumnData = null;
	try {

	    ReadableByteChannel dChan = Channels.newChannel(fis);
	    DbaseFileReader dBaseReader = new DbaseFileReader(dChan, true);
	    DbaseFileHeader dBaseHeader = dBaseReader.getHeader();

	    int nFields = dBaseHeader.getNumFields();
	    int nRows = dBaseHeader.getNumRecords();
	    dbColumnData = new Object[nFields];
	    // set up each array with it's proper type
	    for (int i = 0; i < nFields; i++) {
		if (dBaseHeader.getFieldClass(i).equals(java.lang.String.class)) {
		    dbColumnData[i] = new String[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(
			java.lang.Double.class)) {
		    dbColumnData[i] = new double[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(
			java.lang.Integer.class)) {
		    dbColumnData[i] = new int[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(
			java.lang.Boolean.class)) {
		    dbColumnData[i] = new boolean[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(Date.class)) {
		    dbColumnData[i] = new Date[nRows];
		} else if (dBaseHeader.getFieldClass(i).equals(Long.class)) { // undocumented
		    // in
		    // geotools
		    dbColumnData[i] = new int[nRows];
		}

		else {
		    logger.severe("hit unknown type while reading dbf, class = "
			    + dBaseHeader.getFieldClass(i).getName());
		}
	    }
	    int recordNum = 0;
	    while (dBaseReader.hasNext()) {

		Object[] rowData = dBaseReader.readEntry();
		for (int fieldNum = 0; fieldNum < nFields; fieldNum++) {
		    if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.String.class)) {
			String[] stringCol = (String[]) dbColumnData[fieldNum];
			stringCol[recordNum] = (String) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.Double.class)) {
			double[] doubleCol = (double[]) dbColumnData[fieldNum];
			doubleCol[recordNum] = (Double) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.Integer.class)) {
			int[] intCol = (int[]) dbColumnData[fieldNum];
			intCol[recordNum] = (Integer) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    java.lang.Boolean.class)) {
			boolean[] boolCol = (boolean[]) dbColumnData[fieldNum];
			boolCol[recordNum] = (Boolean) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    Date.class)) {
			Date[] dateCol = (Date[]) dbColumnData[fieldNum];
			dateCol[recordNum] = (Date) rowData[fieldNum];
		    } else if (dBaseHeader.getFieldClass(fieldNum).equals(
			    Long.class)) {
			int[] intCol = (int[]) dbColumnData[fieldNum];
			Long lng = (Long) rowData[fieldNum];
			intCol[recordNum] = lng.intValue();
		    }

		}
		recordNum++;
	    }
	    // dBaseReader.close(); // always do this
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
	return dbColumnData;
    }

    private DataSetForApps makeDataSetForAppsCSV(String fileName) {
	Object[] shpData = null;

	try {
	    String dbFileName = fileName + ".csv";

	    GeogCSVReader csv = new GeogCSVReader();
	    FileInputStream inStream = new FileInputStream(dbFileName);
	    Object[] dbData = csv.readFile(inStream);

	    String shapeFileName = fileName + ".shp";
	    File shpFile = new File(shapeFileName);
	    if (shpFile.exists()) {

		shpData = new Object[dbData.length + 2];
		for (int i = 0; i < dbData.length; i++) {
		    shpData[i] = dbData[i];
		}

		shpData[dbData.length] = ShapeFileDataReader.getGeoms(fileName);

		AttributeDescriptionFile desc = new AttributeDescriptionFile(
			fileName + ".desc");
		shpData[dbData.length + 1] = desc.getAttributeDescriptions();
	    } else {
		shpData = new Object[dbData.length + 1];
		for (int i = 0; i < dbData.length; i++) {
		    shpData[i] = dbData[i];
		}
		if (dataForApps == null) {
		    dataForApps = new DataSetForApps(shpData);
		}
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	return dataForApps;

    }

    protected String removeExtension(String fileName) {
	String removed = fileName;
	int index = fileName.lastIndexOf(".");
	if (index > -1) { // if it was found
	    removed = fileName.substring(0, index);
	}
	return removed;
    }

    private String getExtension(String fileName) {
	String extension = fileName;
	int index = fileName.lastIndexOf(".");
	if (index > -1) { // if it was found
	    // int len = fileName.length();
	    extension = fileName.substring(index + 1);
	}
	return extension;
    }

    public void setDataForApps(DataSetForApps dataForApps) {
	this.dataForApps = dataForApps;
    }

    public DataSetForApps getDataForApps() {
	return dataForApps;
    }

    public Object[] getDataSet() {
	if (dataForApps == null) {
	    logger.severe("returning null data");
	    return null;
	}
	return dataForApps.getDataObjectOriginal();
    }

    public void setFileName(String fileName) {
	// this.fileName = this.removeExtension(fileName);
	if ((getExtension(fileName).toLowerCase()).equals("dbf")) {
	    this.fileName = removeExtension(fileName);
	    dataForApps = makeDataSetForApps(this.fileName);
	} else if ((getExtension(fileName).toLowerCase()).equals("csv")) {
	    this.fileName = removeExtension(fileName);
	    dataForApps = makeDataSetForAppsCSV(this.fileName);
	} else if ((getExtension(fileName).toLowerCase()).equals("shp")) {
	    this.fileName = removeExtension(fileName);
	    String fileDbf = this.fileName + ".dbf";
	    String fileCsv = this.fileName + ".csv";
	    if ((new File(fileDbf)).exists()) {
		dataForApps = makeDataSetForApps(this.fileName);
	    } else if ((new File(fileCsv)).exists()) {
		dataForApps = makeDataSetForAppsCSV(this.fileName);
	    } else {
		try {

		    throw new FileNotFoundException("Couldn't find dbf "
			    + fileDbf + " or csv for shapefile " + fileName);
		} catch (FileNotFoundException ex) {
		    ex.printStackTrace();
		}
	    }
	}
	fireActionPerformed(COMMAND_DATA_SET_MADE);
	fireDataSetChanged(dataForApps);
    }

    public void setFileNameCSV(String fileName) {
	if ((getExtension(fileName).toLowerCase()).equals("dbf")) {
	    this.fileName = removeExtension(fileName);
	    dataForApps = makeDataSetForApps(this.fileName);
	} else if ((getExtension(fileName).toLowerCase()).equals("csv")) {
	    this.fileName = removeExtension(fileName);
	    dataForApps = makeDataSetForAppsCSV(this.fileName);
	} else {
	    this.fileName = removeExtension(fileName);
	    dataForApps = makeDataSetForAppsCSV(this.fileName);
	}
	fireActionPerformed(COMMAND_DATA_SET_MADE);
	fireDataSetChanged(dataForApps);
    }

    public void setFileName(String fileName, int fileType) {
	if (fileType == ShapeFileDataReader.FILE_TYPE_DBF) {
	    this.setFileName(fileName);
	} else if (fileType == ShapeFileDataReader.FILE_TYPE_CSV) {
	    setFileNameCSV(fileName);
	} else {
	    throw new IllegalArgumentException(
		    "ShapeFileDataReader, unexpected file type");
	}
    }

    public String getFileName() {
	return fileName;
    }

    public String getShortFileName() {
	int idx = fileName.lastIndexOf("\\");
	return fileName.substring(idx);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
	oos.defaultWriteObject();
    }

    private void readObject(ObjectInputStream ois)
	    throws ClassNotFoundException, IOException {
	ois.defaultReadObject();
    }

    /**
     * we now allow jts geometries in datasetforapps public Object[]
     * convertShpToShape(Object[] dataIn) { if (dataIn[dataIn.length - 1]
     * instanceof ShapeFile) { ShapeFile shp = (ShapeFile) dataIn[dataIn.length
     * - 1]; dataIn[dataIn.length - 1] = this.makeShapes(shp); } return dataIn;
     * }
     */

    /**
     * we now allow jts geometries in datasetforapps public Object[]
     * transform(ShapefileReader shpFile) { if (shpFile == null) { throw new
     * IllegalArgumentException( "null arg passed in to
     * ShapeFileToShape.transform"); } // Shape[] shpNew = new
     * Shape[shpFile.getData().size()]; Vector oldData = shpFile.getData();
     * shpFile. Object[] newData = null;
     * 
     * switch (shpFile.getFileHeader().getShapeType()) { case
     * ShapeFile.SHAPE_TYPE_POINT: newData = transformPoints(oldData); //
     * outData.setSpatialType(outData.SPATIAL_TYPE_POINT); break; case
     * ShapeFile.SHAPE_TYPE_POLYLINE: newData = transformPolylines(oldData);
     * break; case ShapeFile.SHAPE_TYPE_POLYGON: newData =
     * transformPolygons(oldData); //
     * outData.setSpatialType(outData.SPATIAL_TYPE_POLYGON); break; default:
     * throw new IllegalArgumentException("ShapeFileToShape.transform," + "
     * unknown file type or missing shapefile"); } return newData; }
     */
    /**
     * private Point2D[] transformPoints(Vector shapeFileData) {
     * logger.finest("transformPoints(Vector shapeFileData)...");
     * 
     * Point2D[] newShapes = new Point2D[shapeFileData.size()]; int currShape =
     * 0; double x = 0; double y = 0; for (Enumeration e =
     * shapeFileData.elements(); e.hasMoreElements();) { ShapeFileRecordPoint
     * pointOld = (ShapeFileRecordPoint) e .nextElement(); x = pointOld.getX();
     * y = pointOld.getY();
     * 
     * Point2D newShape = new Point2D.Double(x, y);
     * 
     * newShapes[currShape] = newShape; currShape++; }
     * 
     * return newShapes; }
     */
    /**
     * Frank here.... this null in the array business didn't seem like a good
     * idea I'm going to change this to creating GeneralPathLine objects.
     */
    /**
     * private GeneralPathLine[] transformPolylines(Vector shapeFileData) {
     * logger.finest("transformPolylines(Vector shapeFileData)...");
     * 
     * int len = shapeFileData.size(); GeneralPathLine[] newShapes = new
     * GeneralPathLine[len]; int currShape = 0; for (Enumeration e =
     * shapeFileData.elements(); e.hasMoreElements();) { ShapeFileRecordPolyLine
     * polyOld = (ShapeFileRecordPolyLine) e .nextElement();
     * 
     * double[][] dataOld = polyOld.getPoints(); // XXX next line pointCount =
     * pointCount + dataOld.length; int[] parts = polyOld.getParts(); int part =
     * 0; GeneralPathLine newShape = new GeneralPathLine(); for (int counter =
     * 0; counter < dataOld.length; counter++) { if (part < parts.length &&
     * counter == parts[part]) { newShape.moveTo((float) dataOld[counter][0],
     * (float) dataOld[counter][1]); part++; } else { newShape.lineTo((float)
     * dataOld[counter][0], (float) dataOld[counter][1]); } }
     * newShapes[currShape] = newShape; currShape++; }
     * 
     * logger.finest("len = " + len + ", final total = " + currShape); //
     * newShapes[len] = null; return newShapes; }
     */

    /**
     * private Shape[] transformPolygons(Vector shapeFileData) {
     * logger.finest("transformPolygons(Vector shapeFileData)...");
     * 
     * GeneralPath[] newShapes = new GeneralPath[shapeFileData.size()]; int
     * currShape = 0; for (Enumeration e = shapeFileData.elements();
     * e.hasMoreElements();) { ShapeFileRecordPolygon polyOld =
     * (ShapeFileRecordPolygon) e .nextElement();
     * 
     * double[][] dataOld = polyOld.getPoints(); // XXX next line pointCount =
     * pointCount + dataOld.length; int[] parts = polyOld.getParts(); int part =
     * 0; GeneralPath newShape = new GeneralPath(); for (int counter = 0;
     * counter < dataOld.length; counter++) { if (part < parts.length && counter
     * == parts[part]) { newShape.moveTo((float) dataOld[counter][0], (float)
     * dataOld[counter][1]); part++; } else { newShape.lineTo((float)
     * dataOld[counter][0], (float) dataOld[counter][1]); } }
     * newShapes[currShape] = newShape; currShape++; }
     * 
     * return newShapes; }
     */
    /**
     * implements ActionListener
     */
    public void addActionListener(ActionListener l) {
	listenerList.add(ActionListener.class, l);
    }

    /**
     * removes an ActionListener from the button
     */
    public void removeActionListener(ActionListener l) {
	listenerList.remove(ActionListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    protected void fireActionPerformed(String command) {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	ActionEvent e = null;
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == ActionListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
			    command);
		}
		((ActionListener) listeners[i + 1]).actionPerformed(e);
	    }
	}
    }

    /**
     * implements DataSetListener
     */
    public void addDataSetListener(DataSetListener l) {
	listenerList.add(DataSetListener.class, l);
    }

    /**
     * removes an DataSetListener from the button
     */
    public void removeDataSetListener(DataSetListener l) {
	listenerList.remove(DataSetListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    protected void fireDataSetChanged(DataSetForApps dataSet) {
	logger.finest("ShpToShp.fireDataSetChanged, Hi!!");
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	DataSetEvent e = null;
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == DataSetListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new DataSetEvent(dataSet, this);

		}
		((DataSetListener) listeners[i + 1]).dataSetChanged(e);
	    }
	}
    }

}
