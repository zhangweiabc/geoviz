/* -------------------------------------------------------------------
 Java source file for the class MapGenFile
  Original Author: Frank Hardisty
  $Author: hardistf $
  $Id: ComparableShapes.java,v 1.1 2005/12/05 20:17:05 hardistf Exp $
  $Date: 2005/12/05 20:17:05 $
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.
  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 -------------------------------------------------------------------   */

package geovista.cartogram;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JProgressBar;

import org.geotools.data.shapefile.Lock;
import org.geotools.data.shapefile.shp.ShapefileWriter;

import com.vividsolutions.jts.geom.Geometry;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.data.GeneralPathLine;
import geovista.common.jts.Java2DConverter;
import geovista.geoviz.scatterplot.LinearRegression;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;

/*
 * 
 */

public class MapGenFile {
	protected final static Logger logger = Logger.getLogger(MapGenFile.class
			.getName());
	public static final String CARTGENFILE = "./cartogram.gen"; // Cartogram

	// generate
	// file.

	public static GeneralPath[] readGenFile() throws IOException {
		return MapGenFile.readGenFile(MapGenFile.CARTGENFILE);
	}

	public static GeneralPath[] readGenFile(String fileName) throws IOException {
		String stringVal, line;
		BufferedReader infile;

		GeneralPath path;
		ArrayList pathList = new ArrayList();
		float xcoord, ycoord;
		infile = FileTools.openFileRead(fileName);

		path = new GeneralPath();
		int currPathId = 1;

		boolean isFirstPoint = true;
		String prevToken = "";
		while ((line = FileTools.readLine(infile)) != null) {
			StringTokenizer s = new StringTokenizer(line);
			String firstToken = s.nextToken();
			String secondToken = null, thirdToken = null;
			if (s.hasMoreTokens()) {
				secondToken = s.nextToken();
			}
			if (s.hasMoreTokens()) {
				thirdToken = s.nextToken();
			}

			boolean usedFlag = false;

			// if (sscanf(line,"%s %f %f",&stringVal,&xcoord,&ycoord)==3)
			if (secondToken != null && thirdToken != null) { // we have
				// stringVal, x
				// and y coords
				try {
					stringVal = firstToken;
					xcoord = Float.parseFloat(secondToken);
					ycoord = Float.parseFloat(thirdToken);
					if (isFirstPoint) {

						path.moveTo(xcoord, ycoord);
						isFirstPoint = false;
					} else {
						path.lineTo(xcoord, ycoord);
					}
					usedFlag = true;

				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			// else if (sscanf(line,"%f %f",&xcoord,&ycoord)==2)
			if (!usedFlag && secondToken != null && firstToken != null) { // we
				// have
				// just
				// x
				// and
				// y
				try {
					xcoord = Float.parseFloat(firstToken);
					ycoord = Float.parseFloat(secondToken);
					if (isFirstPoint) {
						path.moveTo(xcoord, ycoord);
						isFirstPoint = false;
					} else {
						path.lineTo(xcoord, ycoord);
					}
					usedFlag = true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			} else { // we just have stringVal. it's either an id or "END"

				stringVal = firstToken;
				isFirstPoint = true; // the next point should be a "moveTo"
				if (stringVal.compareToIgnoreCase("END") != 0) { // its not
					// end
					// close the current polygon (actually, don't, breaks
					// shapefile)
					// path.moveTo(firstXCoord, firstYCoord);
					// should be a number representing the current path
					int pathNum = Integer.parseInt(stringVal);
					if (pathNum != currPathId) {

						pathList.add(path.clone());
						path = new GeneralPath();
						currPathId = pathNum;
					}

				} else if (stringVal.compareToIgnoreCase("END") == 0
						&& prevToken.compareToIgnoreCase("END") == 0) {
					logger.finest("        we've come to the end end");
					// path.moveTo(firstXCoord, firstYCoord); removed by FAH,
					// don't do this, breaks shapefile
					pathList.add(path.clone());
				}

				prevToken = stringVal;

			}

		}
		infile.close();
		int numPaths = pathList.size();
		GeneralPath[] pathArray = new GeneralPath[numPaths];
		for (int i = 0; i < numPaths; i++) {
			pathArray[i] = (GeneralPath) pathList.get(i);
		}
		return pathArray;
	}

	static void writePathsToGen(Shape[] paths, String fileName) {
		BufferedWriter outFile = FileTools.openFileWrite(fileName);
		AffineTransform xForm = new AffineTransform();
		float[] currSeg = new float[6];
		try {
			boolean isFirst = true;
			for (int i = 0; i < paths.length; i++) {
				Shape path = paths[i];
				int pathName = i + 1;
				String pathNameString = String.valueOf(pathName);

				PathIterator it = path.getPathIterator(xForm);

				while (!it.isDone()) {
					int segType = it.currentSegment(currSeg);
					if (segType == PathIterator.SEG_MOVETO) {

						if (!isFirst) {
							outFile.write("END" + "\n");
						} else {
							isFirst = false;
						}
						outFile.write(pathNameString + "\n");
						outFile.write(currSeg[0] + " " + currSeg[1] + "\n");
					} else if (segType == PathIterator.SEG_LINETO) {
						outFile.write(currSeg[0] + " " + currSeg[1] + "\n");
					}
					it.next();
				}

			}
			outFile.write("END" + "\n");
			outFile.write("END" + "\n");
			outFile.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	static void writeCensusFile(int[] vals, String fileName) {
		BufferedWriter outFile = FileTools.openFileWrite(fileName);
		for (int i = 0; i < vals.length; i++) {
			try {
				outFile.write((i + 1) + " " + vals[i] + " states " + (i + 1)
						+ "\n");
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}
		try {
			outFile.close();
		} catch (IOException ex1) {
			ex1.printStackTrace();
		}
	}

	public static int[] makePositive(int[] arrayIn) {
		int[] arrayOut = new int[arrayIn.length];
		for (int i = 0; i < arrayIn.length; i++) {
			int anInt = arrayIn[i];
			if (anInt <= 0) {
				anInt = 1;
			}
			arrayOut[i] = anInt;
			logger.finest("anInt = " + anInt);
		}
		return arrayOut;
	}

	public static double[] makeDouble(int[] arrayIn) {
		double[] returnArray = new double[arrayIn.length];
		for (int i = 0; i < arrayIn.length; i++) {
			returnArray[i] = arrayIn[i];
		}
		return returnArray;

	}

	public static int[] makeInt(double[] arrayIn) {
		int[] arrayOut = new int[arrayIn.length];
		double[] playDouble = new double[arrayIn.length];
		for (int i = 0; i < arrayIn.length; i++) {
			playDouble[i] = Math.abs(arrayIn[i]);
		}

		double smallestDouble = DescriptiveStatistics.min(playDouble);
		double multiplier = 1d;
		if (smallestDouble < 1d) {
			multiplier = 10d;
		}
		if (smallestDouble < 0.1d) {
			multiplier = 100d;
		}
		if (smallestDouble < 0.01) {
			multiplier = 1000d;
		}
		if (smallestDouble < 0.001) {
			multiplier = 10000d;
		}

		for (int i = 0; i < arrayIn.length; i++) {
			double tempDouble = arrayIn[i];
			tempDouble = tempDouble * multiplier;

			if (tempDouble <= 1) {
				tempDouble = 1d;
			}
			long tempLong = Math.round(tempDouble);
			logger.finest("templong = " + tempLong);
			arrayOut[i] = (int) tempLong;
		}
		return arrayOut;
	}

	// Using the new Java IO, this becomes very fast and simple:

	/** Fast & simple file copy. */
	public static void fileCopy(File source, File dest) throws IOException {
		FileChannel in = null, out = null;
		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(dest).getChannel();

			long size = in.size();
			MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0,
					size);

			out.write(buf);

		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Takes a file name and a set of shapes, and writes out a shapefile of type
	 * polygon.
	 * 
	 * also see DBaseFile, ShapeFile
	 * 
	 */

	public static void writeShapefile(GeneralPath[] paths, String fileName) {

		writeShapefile(paths, fileName, 0);// XXX hack
	}

	public static void writeShapefile(Point2D[] points, String fileName) {

		String baseName = fileName.substring(0, fileName.length() - 4);

		/**
		 * XXX the following code uses the old shapefile writer, but we are
		 * moving to the geotools one. but, we might need some of the logic
		 * still header.setShapeType(ShapeFile.SHAPE_TYPE_POINT Rectangle2D
		 * unionRect = new Rectangle2D.Float( (float) points[0].getX(), (float)
		 * points[0].getY(), 0f, 0f); for (int i = 0; i < points.length; i++) {
		 * unionRect.add(points[i]); } double[] theBox = { unionRect.getMinX(),
		 * unionRect.getMinY(), unionRect.getMaxX(), unionRect.getMaxY(), 0, 0,
		 * 0, 0};
		 * 
		 * header.setBoundingBox(theBox); shapeFile.setFileHeader(header);
		 * 
		 * Vector theShapes = new Vector();
		 * 
		 * ShapeFileRecordPoint point;
		 * 
		 * for (int i = 0; i < points.length; i++) { point = new
		 * ShapeFileRecordPoint(); point.setX(points[i].getX());
		 * point.setY(points[i].getY()); theShapes.addElement(point); }
		 * 
		 * shapeFile.setData(theShapes);
		 */
		try {
			FileOutputStream shpFis = new FileOutputStream(baseName + ".shp");
			FileOutputStream shxFis = new FileOutputStream(baseName + ".shx");
			FileChannel shpChan = shpFis.getChannel();
			FileChannel shxChan = shxFis.getChannel();
			Lock lock = new Lock();
			@SuppressWarnings("unused")
			// XXX remove when writer works
			ShapefileWriter shpWriter = new ShapefileWriter(shpChan, shxChan,
					lock);

			// shpWriter.write(arg0, arg1)
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * Takes a file name and a set of shapes, and writes out a shapefile of the
	 * specified type.
	 * 
	 * also see DBaseFile, ShapeFile
	 * 
	 */
	public static void writeShapefile(GeneralPath[] paths, String fileName,
			int shapeType) {
		String baseName = fileName.substring(0, fileName.length() - 4);
		try {
			FileOutputStream shpFis = new FileOutputStream(baseName + ".shp");
			FileOutputStream shxFis = new FileOutputStream(baseName + ".shx");
			FileChannel shpChan = shpFis.getChannel();
			FileChannel shxChan = shxFis.getChannel();
			Lock lock = new Lock();
			ShapefileWriter shpWriter = new ShapefileWriter(shpChan, shxChan,
					lock);

			for (GeneralPath path : paths) {
				Geometry geom = Java2DConverter.toMultiGon(path);
				shpWriter.writeGeometry(geom);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		/**
		 * old writer, delete when new one works. /** see
		 * geovista.readers.geog.ShapeFileWriter
		 * 
		 * ShapeFile shapeFile = new ShapeFile(); ShapeFileHeader header = new
		 * ShapeFileHeader(); header.setShapeType(shapeType); Rectangle2D
		 * unionRect = paths[0].getBounds2D(); for (int i = 0; i < paths.length;
		 * i++) { GeneralPath path = paths[i]; Rectangle2D rect =
		 * path.getBounds2D(); unionRect = rect.createUnion(unionRect); }
		 * double[] theBox = { unionRect.getMinX(), unionRect.getMinY(),
		 * unionRect.getMaxX(), unionRect.getMaxY(), 0, 0, 0, 0};
		 * header.setBoundingBox(theBox); shapeFile.setFileHeader(header);
		 * 
		 * Vector theShapes = new Vector(); if (shapeType ==
		 * ShapeFile.SHAPE_TYPE_POLYGON) { makePolygonRecord(paths, theShapes);
		 * } else if (shapeType == ShapeFile.SHAPE_TYPE_POLYLINE) {
		 * makePolylineRecord(paths, theShapes); } shapeFile.setData(theShapes);
		 * try { shapeFile.write(fileName, baseName); } catch (Exception ex) {
		 * ex.printStackTrace(); }
		 */
	}

	/**
	 * old writer, delete when new one works. private static void
	 * makePolygonRecord(GeneralPath[] paths, Vector theShapes) {
	 * ShapeFileRecordPolygon polygon;
	 * 
	 * for (int i = 0; i < paths.length; i++) { polygon = new
	 * ShapeFileRecordPolygon();
	 * 
	 * double[] bbox = findBbox(paths, i); polygon.setBox(bbox);
	 * 
	 * Object[] returns = findPartsPoints(paths, i); int[] parts = (int[])
	 * returns[0]; double[][] thePoints = (double[][]) returns[1];
	 * 
	 * polygon.setParts(parts); polygon.setNumParts(parts.length);
	 * polygon.setPoints(thePoints); polygon.setNumPoints(thePoints.length);
	 * theShapes.add(polygon); } }
	 */
	@SuppressWarnings("unused")
	private static Object[] findPartsPoints(GeneralPath[] paths, int i) {
		PathIterator pit = paths[i].getPathIterator(new AffineTransform());

		// count parts and points
		int nPoints = 0;
		int nParts = 0;
		float[] coords = { 0, 1, 2, 3, 4, 5, 6 };
		while (!pit.isDone()) {
			int segType = pit.currentSegment(coords);
			if (segType == PathIterator.SEG_MOVETO) {
				nParts++;
			}
			nPoints++;
			pit.next();
		}

		int[] parts = new int[nParts];
		double[][] thePoints = new double[nPoints][2];
		pit = paths[i].getPathIterator(null); // fresh one
		nParts = 0;
		nPoints = 0;
		while (!pit.isDone()) {
			int segType = pit.currentSegment(coords);
			if (segType == PathIterator.SEG_MOVETO) {
				parts[nParts] = nPoints;
				nParts++;
			}
			thePoints[nPoints][0] = coords[0];
			thePoints[nPoints][1] = coords[1];
			nPoints++;
			pit.next();
		}
		Object[] returns = { parts, thePoints };
		return returns;
	}

	@SuppressWarnings("unused")
	private static double[] findBbox(GeneralPath[] paths, int i) {
		Rectangle2D bounds = paths[i].getBounds2D();
		double[] bbox = { bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(),
				bounds.getMaxY() };
		return bbox;
	}

	/**
	 * old writer, delete when new one works. private static void
	 * makePolylineRecord(GeneralPath[] paths, Vector theShapes) {
	 * ShapeFileRecordPolyLine polyline;
	 * 
	 * for (int i = 0; i < paths.length; i++) { polyline = new
	 * ShapeFileRecordPolyLine();
	 * 
	 * double[] bbox = findBbox(paths, i); polyline.setBox(bbox);
	 * 
	 * Object[] returns = findPartsPoints(paths, i); int[] parts = (int[])
	 * returns[0]; double[][] thePoints = (double[][]) returns[1];
	 * 
	 * polyline.setParts(parts); polyline.setNumParts(parts.length);
	 * polyline.setPoints(thePoints); polyline.setNumPoints(thePoints.length);
	 * theShapes.add(polyline); } }
	 */
	/*
	 * This method actually creates the temporary files and creates a
	 * TransformsMain to do the work.
	 */
	public static DataSetForApps createCartogram(JProgressBar progressBar,
			DataSetForApps dataSet, int currentVar, TransformsMain trans) {

		GeneralPath[] shapes = createTempFilesAndCartogram(progressBar,
				dataSet, currentVar, trans);
		DataSetForApps newDataSet = createNewDataSet(progressBar, dataSet,
				shapes);
		progressBar.setString("All Done");
		return newDataSet;

	}

	/*
	 * This method actually creates the temporary files and creates a
	 * TransformsMain to do the work.
	 */
	public static DataSetForApps createCartogram(JProgressBar progressBar,
			DataSetForApps dataSet, Preferences gvPrefs, String newShapeFile,
			String shapeFile, int currentVar, TransformsMain trans) {

		progressBar.setStringPainted(true);
		progressBar.setString("creating temp files...");
		logger.finest("starting cartogram creation...");
		progressBar.setMaximum(dataSet.getNumObservations());
		progressBar.setValue(dataSet.getNumObservations() / 10);
		gvPrefs.put("LastGoodOutputDirectory", newShapeFile);
		String baseFile = shapeFile.substring(0, shapeFile.lastIndexOf("."));
		String dbfFile = baseFile + ".dbf";
		int lastPoint = newShapeFile.lastIndexOf(".");
		String newBaseFile = "";
		if (lastPoint >= 0) {
			newBaseFile = newShapeFile.substring(0, lastPoint);

		} else {
			newBaseFile = newShapeFile;
			newShapeFile = newShapeFile + ".shp";
		}
		String newDbfFile = newBaseFile + ".dbf";
		try {
			MapGenFile.fileCopy(new File(dbfFile), new File(newDbfFile));
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		GeneralPath[] shapes = createTempFilesAndCartogram(progressBar,
				dataSet, currentVar, trans);

		DataSetForApps newDataSet = createNewDataSet(progressBar, dataSet,
				shapes);

		MapGenFile.writeShapefile(shapes, newShapeFile);

		progressBar.setString("All Done");
		return newDataSet;
	}

	static GeneralPath[] createTempFilesAndCartogram(JProgressBar progressBar,
			DataSetForApps dataSet, int currentVar, TransformsMain trans) {
		GeneralPath[] shapes = null;
		int[] vals = null;
		File genFile = null;
		File datFile = null;
		File polygonFile = null;
		String polygonFileName = null;
		String genFileName = null;
		String datFileName = null;
		try {
			genFile = File.createTempFile("map", ".gen");
			genFileName = genFile.getPath();
			datFile = File.createTempFile("census", ".dat");
			datFileName = datFile.getPath();
			polygonFile = File.createTempFile("polygon", ".gen"); // this will
			// be the
			// transformed
			// file
			polygonFileName = polygonFile.getPath();

		} catch (IOException ex2) {
			ex2.printStackTrace();
		}

		Shape[] paths = dataSet.getShapeData();
		MapGenFile.writePathsToGen(paths, polygonFileName);
		double[] numericArray = dataSet.getNumericDataAsDouble(currentVar);

		vals = MapGenFile.makeInt(numericArray);
		vals = MapGenFile.makePositive(vals);
		MapGenFile.writeCensusFile(vals, datFileName);
		progressBar.setString("Creating Cartogram");
		progressBar.setValue(dataSet.getNumObservations() / 5);
		try {
			if (trans == null) {
				trans = new TransformsMain(genFileName, datFileName,
						polygonFileName); // tranforms all the data, reading
				// from the temp files
				logger.finest(trans.toString());
			} else {
				trans.setGenFileName(genFileName);
				trans.setDataFileName(datFileName);
				trans.setPolygonFileName(polygonFileName);
				trans.makeCartogram();
				// the effect of using these methods is same as using the
				// constructor above
			}
			progressBar.setValue(dataSet.getNumObservations() / 2);
			shapes = MapGenFile.readGenFile(genFileName);
			// XXX GeoTools version, not currently used.
			// GeoESRIWriter.writeShapeFile(shapes,newBaseFile,GeoESRIWriter.POLYGON);
		} catch (Exception ex1) {
			ex1.printStackTrace();
		}
		cleanUpTempFiles(genFile, datFile, polygonFile);
		printCorrelation(vals, shapes);
		return shapes;
	}

	/*
	 * This method actually creates the temporary files and creates a
	 * TransformsMain to do the work.
	 */
	public static DataSetForApps createAuxCartogram(JProgressBar progressBar,
			DataSetForApps dataSet, Preferences gvPrefs, String newShapeFile,
			String shapeFile, @SuppressWarnings("unused") int currentVar,
			TransformsMain trans) {

		if (trans == null) {
			logger.finest("Cannot createAuxCartogram on a null TransformsMain");
			return null;
		}

		progressBar.setStringPainted(true);
		progressBar.setString("creating temp files...");
		logger.finest("starting aux cartogram creation...");
		progressBar.setMaximum(dataSet.getNumObservations());
		progressBar.setValue(dataSet.getNumObservations() / 10);
		gvPrefs.put("LastGoodOutputDirectory", newShapeFile);
		String baseFile = shapeFile.substring(0, shapeFile.lastIndexOf("."));
		String dbfFile = baseFile + ".dbf";
		int lastPoint = newShapeFile.lastIndexOf(".");
		String newBaseFile = "";
		if (lastPoint >= 0) {
			newBaseFile = newShapeFile.substring(0, lastPoint);

		} else {
			newBaseFile = newShapeFile;
			newShapeFile = newShapeFile + ".shp";
		}
		String newDbfFile = newBaseFile + ".dbf";

		try {

			MapGenFile.fileCopy(new File(dbfFile), new File(newDbfFile));
		} catch (IOException ex2) {
			ex2.printStackTrace();
		}

		progressBar.setString("Creating Aux Cartogram");
		progressBar.setValue(dataSet.getNumObservations() / 5);
		Shape[] paths = dataSet.getShapeData();
		Point2D[] oldPoints = dataSet.getPoint2DData();
		Point2D[] newPoints = null;
		GeneralPath[] shapes = null;
		if (paths != null) {
			shapes = new GeneralPath[paths.length];
			for (int i = 0; i < paths.length; i++) {
				shapes[i] = MapGenFile.transformPath(paths[i], trans);
			}
		} else if (oldPoints != null) {
			newPoints = new Point2D[oldPoints.length];
			for (int i = 0; i < oldPoints.length; i++) {
				Point nickPoint = new Point((float) oldPoints[i].getX(),
						(float) oldPoints[i].getY());
				nickPoint = trans.transf(nickPoint);
				newPoints[i] = new Point2D.Float(nickPoint.x, nickPoint.y);
			}

		}
		progressBar.setValue(dataSet.getNumObservations() / 2);
		// DataSetForApps.SPATIAL_TYPE_LINE
		if (dataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_POLYGON) {
			MapGenFile.writeShapefile(shapes, newShapeFile);
		} else if (dataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_LINE) {

			MapGenFile.writeShapefile(shapes, newShapeFile);// XXX will not work
		} else if (dataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_POINT) {
			MapGenFile.writeShapefile(newPoints, newShapeFile);
		}
		DataSetForApps newDataSet = null;
		if (paths != null) {
			newDataSet = createNewDataSet(progressBar, dataSet, shapes);
		} else if (dataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_POINT) {
			newDataSet = createNewDataSet(progressBar, dataSet, newPoints);

		}
		// cleanUpTempFiles(genFile, datFile, polygonFile);

		progressBar.setString("All Done Aux");
		return newDataSet;
	}

	static GeneralPath transformPath(Shape originalPath, TransformsMain trans) {
		GeneralPath newPath = new GeneralPath();
		AffineTransform xForm = new AffineTransform();
		PathIterator pi = originalPath.getPathIterator(xForm);
		Point p = new Point();
		Point newP = new Point();
		float[] coords = new float[6];
		while (!pi.isDone()) {
			int segType = pi.currentSegment(coords);
			p.x = coords[0];
			p.y = coords[1];
			newP = trans.transf(p);
			if (segType == PathIterator.SEG_LINETO) {
				newPath.lineTo(newP.x, newP.y);
			} else if (segType == PathIterator.SEG_MOVETO) {
				newPath.moveTo(newP.x, newP.y);
			}
			pi.next();
		}

		return newPath;
	}

	private static DataSetForApps createNewDataSet(JProgressBar progressBar,
			DataSetForApps oldDataSet, Point2D[] points) {
		progressBar.setValue(oldDataSet.getNumObservations());
		Object[] numericData = oldDataSet.getDataSetNumeric();
		String[] varNames = oldDataSet.getAttributeNamesNumeric();
		Object[] newArray = new Object[numericData.length + 2];
		newArray[0] = varNames;
		for (int i = 0; i < numericData.length; i++) {
			newArray[i + 1] = numericData[i];
		}

		newArray[numericData.length + 1] = points;
		return new DataSetForApps(newArray);

	}

	public static DataSetForApps createNewDataSet(DataSetForApps oldDataSet,
			Shape[] shapes) {

		Object[] numericData = oldDataSet.getDataSetNumeric();
		String[] varNames = oldDataSet.getAttributeNamesNumeric();
		Object[] newArray = new Object[numericData.length + 2];
		newArray[0] = varNames;
		for (int i = 0; i < numericData.length; i++) {
			newArray[i + 1] = numericData[i];
		}
		Shape[] newShapes = new Shape[shapes.length];
		for (int i = 0; i < newShapes.length; i++) {
			if (oldDataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_POLYGON) {
				newShapes[i] = shapes[i];
			} else if (oldDataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_LINE) {
				newShapes[i] = new GeneralPathLine(shapes[i]);
			}
		}
		newArray[numericData.length + 1] = newShapes;
		return new DataSetForApps(newArray);

	}

	private static DataSetForApps createNewDataSet(JProgressBar progressBar,
			DataSetForApps oldDataSet, GeneralPath[] shapes) {
		progressBar.setValue(oldDataSet.getNumObservations());

		return MapGenFile.createNewDataSet(oldDataSet, shapes);
	}

	private static void cleanUpTempFiles(File genFile, File datFile,
			File polygonFile) {
		datFile.delete();
		genFile.delete();
		polygonFile.delete();
	}

	private static void printCorrelation(int[] vals, GeneralPath[] shapes) {
		double[] newAreas = ShapeUtil.computeAreaArray(shapes);
		double[] mappedVals = MapGenFile.makeDouble(vals);
		Arrays.sort(newAreas);
		Arrays.sort(mappedVals);
		LinearRegression regression = new LinearRegression();
		double rSquared = regression.getRSquare(newAreas, mappedVals);
		logger.finest("rSquared = " + rSquared);
	}

	// for batch transforms
	public static void main(String[] args) {
		String basePath = "C:\\geovista_old\\cartogram\\";
		String baseFile = "./48states_new";
		baseFile = basePath + "48states_new";
		String shapeFile = baseFile + ".shp";
		String dbfFile = baseFile + ".dbf";
		String genFile = basePath + "/map.gen";
		String censusFile = basePath + "/census.dat";

		String newShapeFile = basePath + "/cartogram.shp";
		String newDbfFile = "";
		// genFile = "./map2.gen";
		// censusFile = "./census2.dat";

		// GeoDataGeneralizedStates statesData = new GeoDataGeneralizedStates();
		ShapeFileProjection shpProj = new ShapeFileProjection();
		ShapeFileDataReader reader = new ShapeFileDataReader();
		reader.setFileName(shapeFile);
		// reader.setFileName("./Export_Output.shp");
		shpProj.setInputDataSet(reader.getDataSet());
		// shpProj.setInputDataSet(reader.convertShpToShape(statesData.getDataSet()));
		Object[] dataSet = shpProj.getOutputDataSet();
		DataSetForApps dataSetApps = new DataSetForApps(dataSet);

		Shape[] paths = dataSetApps.getShapeData();

		MapGenFile.writePathsToGen(paths, genFile);

		Object[] numericArrays = dataSetApps.getDataSetNumeric();
		String[] numericArrayNames = dataSetApps.getAttributeNamesNumeric();
		String varName = null;
		Object numericArray = null;
		int[] vals = null;
		for (int i = 0; i < numericArrays.length; i++) {
			varName = numericArrayNames[i];
			newDbfFile = basePath + "/testing/" + varName + "_cartogram.dbf";
			try {
				File dbfF = new File(dbfFile);
				File newDbfF = new File(newDbfFile);
				MapGenFile.fileCopy(dbfF, newDbfF);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			newShapeFile = basePath + "/testing/" + varName + "_cartogram.shp";

			numericArray = numericArrays[i];
			if (numericArray instanceof int[]) {
				vals = MapGenFile.makePositive((int[]) numericArray);
			} else {
				vals = MapGenFile.makeInt((double[]) numericArray);
			}

			MapGenFile.writeCensusFile(vals, censusFile);
			try {
				@SuppressWarnings("unused")
				// side effect: does all the work
				TransformsMain trans = new TransformsMain(true); // tranforms
				// all the
				// data,
				// reading
				// from
				// files
			} catch (Exception ex1) {
				ex1.printStackTrace();
			}

			GeneralPath[] shapes = null;
			try {
				shapes = MapGenFile.readGenFile();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			MapGenFile.writeShapefile(shapes, newShapeFile);
		}
		// GeoMapUni map = new GeoMapUni();
		// //dataSet[dataSet.length - 1] = shapes;
		// JFrame frame = new JFrame();
		// frame.getContentPane().add(map);
		// map.setDataSet(dataSet);
		// frame.pack();
		// frame.setVisible(true);
	}
}
