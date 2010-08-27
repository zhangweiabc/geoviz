/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.readers.shapefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import org.geotools.data.shapefile.Lock;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileWriter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

import geovista.common.data.DataSetForApps;
import geovista.readers.example.GeoDataGeneralizedStates;

/**
 * Takes a file name and a set of shapes, and writes out a shapefile.
 * 
 * also see DBaseFile, ShapeFile
 * 
 */
public class ShapeFileDataWriter {
	protected final static Logger logger = Logger
			.getLogger(ShapeFileDataWriter.class.getName());

	public static void writeShapefile(Geometry[] paths, String fileNameRoot) {
		if (paths == null) {
			logger.severe("can't write null paths!");
			return;
		}
		try {
			GeometryFactory geomFact = new GeometryFactory();
			GeometryCollection geoms = null;
			geoms = new GeometryCollection(paths, geomFact);
			File shp = new File(fileNameRoot + ".shp");
			File shx = new File(fileNameRoot + ".shx");
			FileOutputStream shpStream = new FileOutputStream(shp);
			FileOutputStream shxStream = new FileOutputStream(shx);

			FileChannel shpChan = shpStream.getChannel();
			FileChannel shxChan = shxStream.getChannel();

			ShapefileWriter writer = new ShapefileWriter(shpChan, shxChan,
					new Lock());
			ShapeType bestType = JTSUtilities.getShapeType(paths[0], 2);
			writer.write(geoms, bestType);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static int findMaxLeftSide(double[] array) {
		int len = 0;
		for (double num : array) {
			double leftNum = Math.floor(num);

			String numString = String.valueOf(leftNum);
			if (numString.length() > len) {
				len = numString.length();
			}
		}
		return len;
	}

	public static int findMaxStringLen(String[] array) {
		int len = 0;
		for (String str : array) {
			if (str.length() > len) {
				len = str.length();
			}
		}

		return len;
	}

	public static void writeDBFile(String[] columnNames, Object[] data,
			int nRecords, String fileNameRoot) {

		try {
			DbaseFileHeader header = new DbaseFileHeader();
			header.setNumRecords(nRecords);
			for (int i = 0; i < columnNames.length; i++) {
				Object array = data[i];
				String name = columnNames[i];
				if (array instanceof double[]) {
					header.addColumn(name, 'N', 20, 4);
				} else if (array instanceof String[]) {
					header.addColumn(name, 'C',
							findMaxStringLen((String[]) array), 0);
				} else if (array instanceof int[]) {
					header.addColumn(name, 'N', 20, 0);
				} else {
					logger.severe("hit unknown array type, "
							+ array.getClass().getName());
				}
			}
			File dbf = new File(fileNameRoot + ".dbf");
			FileOutputStream dbfStream = new FileOutputStream(dbf);
			FileChannel dbfChan = dbfStream.getChannel();
			DbaseFileWriter writer = new DbaseFileWriter(header, dbfChan);
			Object[] record = new Object[columnNames.length];
			for (int i = 0; i < nRecords; i++) {
				for (int j = 0; j < record.length; j++) {
					Object array = data[j];

					if (array instanceof double[]) {
						record[j] = new Double(((double[]) array)[i]);
					} else if (array instanceof String[]) {
						record[j] = ((String[]) array)[i];
					} else if (array instanceof int[]) {
						record[j] = new Integer(((int[]) array)[i]);
					} else {
						logger.severe("hit unknown array type, "
								+ array.getClass().getName());
					}

				}
				writer.write(record);
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}

	}

	public static void writeDataSetForApps(DataSetForApps dsa, String fileName) {
		String[] varNames = dsa.getAttributeNamesOriginal();
		Object[] data = dsa.getNamedArrays();
		writeDBFile(varNames, data, dsa.getNumObservations(), fileName);
		ShapeFileDataWriter.writeShapefile(dsa.getGeomData(), fileName);
	}

	public static void main(String[] args) {
		GeoDataGeneralizedStates stateData = new GeoDataGeneralizedStates();
		String fileName = "C:\\temp\\test2";
		writeDataSetForApps(stateData.getDataForApps(), fileName);

	}
	/**
	 * uses old shapefile writer... public static void
	 * writeShapefile(GeneralPath[] paths, String fileName) {
	 * 
	 * String baseName = fileName.substring(0, fileName.length() - 4); ShapeFile
	 * shapeFile = new ShapeFile(); ShapeFileHeader header = new
	 * ShapeFileHeader(); header.setShapeType(ShapeFile.SHAPE_TYPE_POLYGON);
	 * Rectangle2D unionRect = paths[0].getBounds2D(); for (int i = 0; i <
	 * paths.length; i++) { GeneralPath path = paths[i]; Rectangle2D rect =
	 * path.getBounds2D(); unionRect = rect.createUnion(unionRect); } double[]
	 * theBox = {unionRect.getMinX(), unionRect.getMinY(), unionRect.getMaxX(),
	 * unionRect.getMaxY(), -1, 1, -1, 1}; header.setBoundingBox(theBox);
	 * shapeFile.setFileHeader(header);
	 * 
	 * Vector theShapes = new Vector(); ShapeFileRecordPolygon polygon;
	 * 
	 * for (int i = 0; i < paths.length; i++) { polygon = new
	 * ShapeFileRecordPolygon(); Rectangle2D bounds = paths[i].getBounds2D();
	 * double[] bbox = {bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(),
	 * bounds.getMaxY()}; polygon.setBox(bbox); PathIterator pit =
	 * paths[i].getPathIterator(new AffineTransform()); int nPoints = 0; int
	 * nParts = 0; float[] coords = {0, 1, 2, 3, 4, 5, 6}; while (!pit.isDone())
	 * { int segType = pit.currentSegment(coords); if (segType ==
	 * PathIterator.SEG_MOVETO) { nParts++; } nPoints++; pit.next(); }
	 * logger.finest("nParts = " + nParts); logger.finest("nPoints = " +
	 * nPoints); int[] parts = new int[nParts]; double[][] thePoints = new
	 * double[nPoints][2]; pit = paths[i].getPathIterator(null); //fresh one
	 * nParts = 0; nPoints = 0; while (!pit.isDone()) { int segType =
	 * pit.currentSegment(coords); if (segType == PathIterator.SEG_MOVETO) {
	 * parts[nParts] = nPoints; nParts++; } thePoints[nPoints][0] = coords[0];
	 * thePoints[nPoints][1] = coords[1]; nPoints++; pit.next(); }
	 * polygon.setParts(parts); polygon.setNumParts(parts.length);
	 * polygon.setPoints(thePoints); polygon.setNumPoints(thePoints.length);
	 * theShapes.add(polygon); } shapeFile.setData(theShapes); try {
	 * shapeFile.write(fileName, baseName); } catch (Exception ex) {
	 * ex.printStackTrace(); } }
	 * 
	 * public static void main(String[] args) { String shapeFile =
	 * "C:/geovista_old/cartogram/testing/48states.shp"; String newShapeFile =
	 * "C:/geovista_old/cartogram/testing/cartogram.shp"; //genFile =
	 * "./map2.gen"; //censusFile = "./census2.dat";
	 * 
	 * //GeoData48States statesData = new GeoData48States(); ShapeFileProjection
	 * shpProj = new ShapeFileProjection(); ShapeFileDataReader reader = new
	 * ShapeFileDataReader(); reader.setFileName(shapeFile);
	 * //reader.setFileName("./Export_Output.shp");
	 * shpProj.setInputDataSet(reader.getDataSet());
	 * //shpProj.setInputDataSet(reader
	 * .convertShpToShape(statesData.getDataSet())); Object[] dataSet =
	 * shpProj.getOutputDataSet(); DataSetForApps dataSetApps = new
	 * DataSetForApps(dataSet);
	 * 
	 * Shape[] paths = dataSetApps.getShapeData(); GeneralPath[] newPaths = new
	 * GeneralPath[paths.length]; for (int i = 0; i < paths.length; i++) {
	 * newPaths[i] = (GeneralPath) paths[i]; }
	 * ShapeFileDataWriter.writeShapefile(newPaths, newShapeFile); }
	 */

}
