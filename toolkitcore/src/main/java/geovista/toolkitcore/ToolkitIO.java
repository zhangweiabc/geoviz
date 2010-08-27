/* -------------------------------------------------------------------
 Java source file for the class ToolkitIO
 Copyright (c), 2005 Ke Liao, Frank Hardisty
 $Author: hardisty $
 $Id: ToolkitIO.java,v 1.6 2005/04/11 17:52:14 hardisty Exp $
 $Date: 2005/04/11 17:52:14 $
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

package geovista.toolkitcore;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Geometry;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.geoviz.map.GeoMap;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileDataWriter;
import geovista.readers.util.MyFileFilter;
import geovista.toolkitcore.marshal.Marshaler;

public class ToolkitIO {
	protected final static Logger logger = Logger.getLogger(ToolkitIO.class
			.getName());

	public enum Action {
		OPEN, SAVE
	}

	public enum FileType {
		LAYOUT, SHAPEFILE, SEERSTAT, CSV, XLS, GVT
	}

	// we need to keep these as strings to avoid breaking existing clients
	private static String LAYOUT_DIR = "LastGoodLayoutDirectory";
	private static String SHAPEFILE_DIR = "LastGoodFileDirectory";
	private static String IMAGE_DIR = "LastGoodImageDirectory";
	private static String SEERSTAT_DIR = "LastGoodSeerStatDirectory";
	private static String CSV_DIR = "LastGoodCSVDirectory";
	public static String dataSetPathFromXML = " ";

	public static void saveVizStateToFile(VizState state) {
		Marshaler marsh = Marshaler.INSTANCE;
		String xml = marsh.toXML(state);
		logger.info(xml);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("state.gvz"));
			out.write(xml);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void saveImageToFile(Component c) {
		BufferedImage buff = new BufferedImage(c.getWidth(), c.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = buff.getGraphics();
		c.paint(g);
		Preferences gvPrefs = Preferences
				.userNodeForPackage(ToolkitBeanSet.class);
		String defaultDir = gvPrefs.get(ToolkitIO.IMAGE_DIR, "");
		JFileChooser chooser = new JFileChooser(defaultDir);
		MyFileFilter myff = new MyFileFilter("png");
		chooser.setFileFilter(myff);

		int returnVal = chooser.showSaveDialog(c);
		File fi = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			chooser.getSelectedFile();
			String fileName = chooser.getSelectedFile().getPath();

			if (!fileName.contains(".png")) {
				fileName = fileName + ".png";
			}
			fi = new File(fileName);
			try {
				ImageIO.write(buff, "png", fi);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			gvPrefs.put(ToolkitIO.IMAGE_DIR, fi.getPath());

		}

	}

	public static String getFileName(Component parent, Action action,
			FileType fileType) {
		logger.info("in get file name method");
		Preferences gvPrefs = Preferences
				.userNodeForPackage(ToolkitBeanSet.class);
		String fullFileName = null;

		try {
			// LookAndFeel laf = UIManager.getLookAndFeel();
			// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			String defaultDir = "";
			MyFileFilter fileFilter = null;
			// we want to get the last good directory of the type we are looking
			// for
			logger.info("getting defaults");
			if (fileType == ToolkitIO.FileType.LAYOUT) {
				defaultDir = gvPrefs.get(ToolkitIO.LAYOUT_DIR, "");
				fileFilter = new MyFileFilter("xml");
			} else if (fileType == ToolkitIO.FileType.SHAPEFILE) {
				defaultDir = gvPrefs.get(ToolkitIO.SHAPEFILE_DIR, "");
				fileFilter = new MyFileFilter(new String[] { "shp", "dbf",
						"csv" });
			} else if (fileType == ToolkitIO.FileType.SEERSTAT) {
				defaultDir = gvPrefs.get(ToolkitIO.SEERSTAT_DIR, "");
				fileFilter = new MyFileFilter(new String[] { "dic" });
			} else if (fileType == FileType.CSV) {
				defaultDir = gvPrefs.get(ToolkitIO.CSV_DIR, "");
				fileFilter = new MyFileFilter(new String[] { "csv" });
			}

			JFileChooser fileChooser = new JFileChooser(defaultDir);
			fileChooser.setFileFilter(fileFilter);
			int returnVal = JFileChooser.CANCEL_OPTION;
			if (action == ToolkitIO.Action.OPEN) {
				logger.info("about to show open dialog");
				returnVal = fileChooser.showOpenDialog(parent);

			} else if (action == ToolkitIO.Action.SAVE) {
				logger.info("about to show save dialog");
				returnVal = fileChooser.showSaveDialog(parent);
				if (returnVal == JFileChooser.CANCEL_OPTION) {
					return null;
				}
				File tempFile = fileChooser.getSelectedFile();
				if (tempFile.exists()) {
					logger.finest("this files exists!");

					int erase;
					String[] choices = { "Replace existing one",
							"Choose another name", "Return without saving" };
					erase = JOptionPane.showOptionDialog(parent,
							"This fils exists, you want to:",
							"Warning: name conflicts",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE, null, choices,
							"Choose another name");

					if (erase == JOptionPane.NO_OPTION) {
						GeoVizToolkit gvt = (GeoVizToolkit) parent;
						String xml = Marshaler.INSTANCE.toXML(gvt);
						ToolkitIO.writeLayout(xml, parent);
					} else if (erase == JOptionPane.CANCEL_OPTION) {
						return null;
					}
				}
			}
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = null;
				file = fileChooser.getSelectedFile();
				fullFileName = file.getAbsolutePath();
				String path = file.getPath();
				logger.finest("path = " + path);
				logger.finest("absolutePath = " + file.getAbsolutePath());

				if (fileType == ToolkitIO.FileType.LAYOUT) {
					gvPrefs.put(ToolkitIO.LAYOUT_DIR, path);
				} else if (fileType == ToolkitIO.FileType.SHAPEFILE) {
					gvPrefs.put(ToolkitIO.SHAPEFILE_DIR, path);
				} else if (fileType == ToolkitIO.FileType.SEERSTAT) {
					gvPrefs.put(ToolkitIO.SEERSTAT_DIR, path);
				}
			}

		}

		catch (Exception ex) {
			ex.printStackTrace();
		}
		return fullFileName;

	}

	public static void writeLayout(String xml, Component parent) {
		String xmlFullName = ToolkitIO.getFileName(parent,
				ToolkitIO.Action.SAVE, ToolkitIO.FileType.LAYOUT);
		if (xmlFullName == null) {
			return;
		}
		int periodPlace = xmlFullName.lastIndexOf(".");
		String extension = null;
		if (periodPlace == -1) {
			extension = "";
		} else {
			extension = xmlFullName
					.substring(periodPlace, xmlFullName.length());
		}
		logger.finest("extension = " + extension);
		if (extension.compareToIgnoreCase(".xml") != 0) {
			xmlFullName = xmlFullName + ".xml";
		}
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(xmlFullName));
			out.write(xml);
			out.close();
			Preferences gvPrefs = Preferences
					.userNodeForPackage(ToolkitBeanSet.class);

			File fi = new File(xmlFullName);
			String path = fi.getAbsolutePath();
			gvPrefs.put("LastGoodLayoutDirectory", path);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String getVizStateXML(String fileName) {

		InputStreamReader inReader = null;
		try {
			inReader = new FileReader(fileName);
		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		}

		return readCharStream(inReader);
	}

	private static String readCharStream(InputStreamReader inReader) {
		StringBuffer strBuff = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(inReader);
			String str;
			while ((str = in.readLine()) != null) {
				strBuff.append(str);
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("built string " + strBuff.toString());
		}
		return strBuff.toString();
	}

	public static VizState openDefaultLayout() {
		String xmlName = "anthony";

		return getVizStateFromResource(xmlName);
	}

	private static VizState getVizStateFromResource(String xmlName) {
		InputStream inStream = null;
		try {
			Class cl = ToolkitIO.class;

			inStream = cl.getResourceAsStream("resources/" + xmlName + ".xml");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		String xml = readCharStream(new InputStreamReader(inStream));
		if (logger.isLoggable(Level.FINEST)) {
			logger.info("getting mashaller");
		}
		Marshaler marsh = Marshaler.INSTANCE;
		if (logger.isLoggable(Level.FINEST)) {
			logger.info("about to instantiate VizState");
		}
		VizState state = (VizState) marsh.fromXML(xml);
		if (logger.isLoggable(Level.FINEST)) {
			logger.info("instantiated VizState");
		}
		return state;
	}

	public static VizState openStarPlotMapLayout() {
		String xmlName = "starmap";
		return getVizStateFromResource(xmlName);
	}

	public static VizState openAllComponentsLayout() {
		String xmlName = "new_all";
		return getVizStateFromResource(xmlName);
	}

	public static VizState getVizState(Component parent) {
		String xml = ToolkitIO.makeVizStateXML(parent);
		if (xml == null) {
			return null;
		}
		if (logger.isLoggable(Level.FINEST)) {
			logger.info("getting mashaller");
		}
		Marshaler marsh = Marshaler.INSTANCE;
		if (logger.isLoggable(Level.FINEST)) {
			logger.info("about to instantiate VizState");
		}
		VizState state = (VizState) marsh.fromXML(xml);
		if (logger.isLoggable(Level.FINEST)) {
			logger.info("instantiated VizState");
		}
		return state;

	}

	public static String makeVizStateXML(Component parent) {

		String xmlFullName = ToolkitIO.getFileName(parent,
				ToolkitIO.Action.OPEN, ToolkitIO.FileType.LAYOUT);
		if (xmlFullName == null) {
			return null;
		}

		logger.info("xmlFullName = " + xmlFullName);
		return ToolkitIO.getVizStateXML(xmlFullName);
	}

	public static void copyComponentImageToClipboard(Component c) {
		BufferedImage buff = new BufferedImage(c.getWidth(), c.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = buff.getGraphics();
		c.paint(g);
		ToolkitIO.sendImageToClipboard(buff);
	}

	/*
	 * public static void saveCommentedImage(Component c) { float
	 * COMPRESSION_QUALITY = 0.95F; BufferedImage buff = new
	 * BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
	 * Graphics g = buff.getGraphics(); c.paint(g);
	 * 
	 * String outputFileName = "C:\\test.jpg"; InputStream inStream = null; try
	 * { Class cl = ToolkitIO.class;
	 * 
	 * inStream = cl.getResourceAsStream("resources/default.xml"); } catch
	 * (Exception ex) { ex.printStackTrace(); } Document doc =
	 * ToolkitIO.readDocument(inStream); // Get core JPEG writer. Iterator
	 * writers = ImageIO.getImageWritersByFormatName("jpeg"); ImageWriter writer
	 * = ImageIO.getImageWritersByFormatName("jpeg").next(); while
	 * (writers.hasNext()) { writer = (ImageWriter) writers.next(); if
	 * (writer.getClass().getName().startsWith( "javax_imageio_jpeg_image_1.0"))
	 * { // Break on finding the core compProvider. break; } } if (writer ==
	 * null) { System.err.println("Cannot find core JPEG writer!"); } // Set the
	 * compression level. ImageWriteParam writeParam =
	 * writer.getDefaultWriteParam();
	 * writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	 * writeParam.setCompressionQuality(COMPRESSION_QUALITY); RenderedImage
	 * image = buff; ImageTypeSpecifier spec = ImageTypeSpecifier
	 * .createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB); // construct
	 * the image metadata. // IIOMetadata imageMetadata =
	 * writer.getDefaultImageMetadata(imageType, // param) IIOMetadata
	 * imageMetadata = writer.getDefaultImageMetadata(spec, null); //
	 * IIOMetadata imageMetadata = new JPEGMetadata(); // add something...
	 * IIOMetadataNode commentNode = new IIOMetadataNode("com");
	 * commentNode.setAttribute("comment", doc.toString());
	 * logger.finest(""+imageMetadata.isStandardMetadataFormatSupported()); try
	 * { IIOMetadataNode root = (IIOMetadataNode) imageMetadata
	 * .getAsTree("javax_imageio_jpeg_image_1.0"); //
	 * root.setAttribute("comment", "Anthony is in trouble"); //
	 * root.setTextContent("this is text content");
	 * 
	 * Vector vec = new Vector(); Element el = new Element("AString");
	 * vec.add(el); // Element el2 = new Element("anotherstring"); //
	 * vec.add(el2); // Document doc = new Document(vec); //
	 * root.setUserObject("Anthony.... this had better be good"); //
	 * root.setUserObject(doc); // root.setUserObject(someBytes); NodeList
	 * rootnl = root.getChildNodes(); //
	 * root.getLastChild().appendChild(commentNode); for (int i = 0; i <
	 * rootnl.getLength(); i++) { Node nod = rootnl.item(i);
	 * logger.finest("***"); logger.finest("nod " + i);
	 * logger.finest("nod name " + nod.getNodeName());
	 * logger.finest("nod value " + nod.getNodeValue()); IIOMetadataNode iioNod
	 * = (IIOMetadataNode) nod; logger.finest("iiomnod comment
	 * " + iioNod.getAttribute("comment")); } logger.finest("***");
	 * logger.finest("root comment " + root.getAttribute("Comment"));
	 * logger.finest("***"); logger.finest("root text content " +
	 * root.getTextContent()); logger.finest("***"); logger.finest("root
	 * userObject " + root.getUserObject()); //
	 * imageMetadata.setFromTree("javax_imageio_jpeg_image_1.0", root); //
	 * imageMetadata.mergeTree("javax_imageio_jpeg_image_1.0", // commentNode);
	 * 
	 * IIOMetadataNode newRoot = (IIOMetadataNode) imageMetadata
	 * .getAsTree("javax_imageio_jpeg_image_1.0"); NodeList nl =
	 * newRoot.getChildNodes();
	 * 
	 * for (int i = 0; i < nl.getLength(); i++) { logger.finest("***");
	 * logger.finest("new nod " + i); Node nod = nl.item(i); logger.finest("new
	 * nod name " + nod.getNodeName()); logger.finest("new nod value " +
	 * nod.getNodeValue()); IIOMetadataNode iioNod = (IIOMetadataNode) nod;
	 * logger.finest("new iiomnod comment " + iioNod.getAttribute("Comment")); }
	 * logger.finest("***"); logger.finest("new root comment " +
	 * newRoot.getAttribute("Comment")); logger.finest("***");
	 * logger.finest("***"); logger.finest("new root userObject " +
	 * root.getUserObject()); } catch (Exception e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } // Set the output stream, write the
	 * image try {
	 * 
	 * writer .setOutput(new FileImageOutputStream(new File( outputFileName)));
	 * // writer.replaceImageMetadata(0,imageMetadata); writer.write(null, new
	 * IIOImage(image, null, imageMetadata), writeParam); writer.dispose(); }
	 * catch (FileNotFoundException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } }
	 */
	public static String openCommentedImage(String fileName) {

		fileName = "C:\\test.jpg";

		// Get core JPEG reader.
		Iterator readers = ImageIO.getImageReadersByFormatName("jpeg");
		ImageReader reader = ImageIO.getImageReadersByFormatName("jpeg").next();
		while (readers.hasNext()) {
			reader = (ImageReader) readers.next();
			if (reader.getClass().getName().startsWith("com.sun.imageio")) {
				// Break on finding the core compProvider.
				break;
			}
		}
		if (reader == null) {
			System.err.println("Cannot find core JPEG reader!");
		}

		String comment = null;
		// Use Java Image I/O directly
		// Set the input stream.
		try {
			reader.setInput(new FileImageInputStream(new File(fileName)));
			// Get the image metadata.
			IIOMetadata imageMetadata = reader.getImageMetadata(0);
			String[] jpegFormats = imageMetadata.getMetadataFormatNames();
			for (String s : jpegFormats) {
				logger.finest(s);
			}
			// get that **** comment

			IIOMetadataNode nod = (IIOMetadataNode) imageMetadata
					.getAsTree("javax_imageio_jpeg_image_1.0");
			IIOMetadataNode chNod = (IIOMetadataNode) nod.getLastChild();
			NodeList nodList = chNod.getChildNodes();
			for (int i = 0; i < nodList.getLength(); i++) {
				IIOMetadataNode child = (IIOMetadataNode) nodList.item(i);
				logger.finest("child " + i + " value = " + child.getNodeName());
				if (child.getNodeName().equals("com")) {
					logger.finest("this is the one");
					logger.finest(child.getAttribute("comment"));
				}
			}

			logger.finest("user object from file");
			logger.finest("object " + nod.getUserObject());
			logger.finest("text content from file");
			logger.finest(nod.getTextContent());
			// comment = (String) nod.getUserObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return comment;
	}

	// I got this method and the next class from an online article in devx.
	// the code was available for download with no copyright
	// notice, so I judge the risk of problems to be small - Frank
	public static void sendImageToClipboard(Image image) {
		ImageSelection imageSelection = new ImageSelection(image);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				imageSelection, null);
	}

	// Inner class is used to hold an image while on the clipboard.
	public static class ImageSelection implements Transferable {
		// the Image object which will be housed by the ImageSelection
		private final Image image;

		public ImageSelection(Image image) {
			this.image = image;
		}

		// Returns the supported flavors of our implementation
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}

		// Returns true if flavor is supported
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}

		// Returns Image object housed by Transferable object
		public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (!DataFlavor.imageFlavor.equals(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			// else return the payload
			return image;
		}

	}

	public static void main(String[] args) {

	}

	public static DataSetForApps getClevelandData() {
		String bgLocation = "C:\\data\\grants\\NIJ_ESDA\\cleveland_bgs\\cleveland bgs";
		ShapeFileDataReader reader = new ShapeFileDataReader();
		reader.setFileName(bgLocation + ".shp");
		DataSetForApps dsaBGs = reader.getDataForApps();

		HashMap<String, Integer> counts = new HashMap<String, Integer>();

		String crimeLocation = "C:\\data\\grants\\NIJ_ESDA\\part1\\part1data";
		ShapeFileDataReader reader2 = new ShapeFileDataReader();
		reader2.setFileName(crimeLocation + ".shp");
		DataSetForApps dsaCrime = reader2.getDataForApps();

		Geometry[] bgs = ShapeFileDataReader.getGeoms(bgLocation);
		Geometry[] pts = ShapeFileDataReader.getGeoms(crimeLocation);
		int hits = 0;
		for (int i = 0; i < dsaCrime.getNumObservations(); i++) {
			// find the bg

			Geometry point = pts[i];

			for (int bgNum = 0; bgNum < dsaBGs.getNumObservations(); bgNum++) {

				Geometry bgGeom = bgs[bgNum];
				if (bgGeom.contains(point)) {
					// find the year
					int year = (Integer) dsaCrime.getValueAt(i, 2);
					// get the crime
					String crimeType = (String) dsaCrime.getValueAt(i, 10);
					String crimeCode = crimeType.substring(0, 1);
					// here's our encoding: YYYYTypeID
					// Type = "R"robbery, "A"assault, "H"homicide
					String id = year + crimeCode + bgNum;
					Integer count = counts.get(id);
					if (count == null) {
						count = 0;
					}
					count++;
					counts.put(id, count);
					hits++;
				}

			}

		}

		// assemble dsa
		int nVars = Crime.values().length * Year.values().length * 2;
		Object[] dataSetNums = new Object[nVars];
		String[] varNames = new String[nVars];
		for (int i = 0; i < nVars; i++) {
			dataSetNums[i] = new double[dsaBGs.getNumObservations()];
		}
		int i = 0;
		HashMap<String, Integer> varIDs = new HashMap<String, Integer>();
		for (Crime cri : Crime.values()) {
			for (Year year : Year.values()) {
				String varName = cri.name() + year.val;
				varNames[i] = varName;
				varIDs.put(varName, i);
				i++;
			}
		}

		for (Crime cri : Crime.values()) {
			for (Year year : Year.values()) {
				String varName = cri.name() + year.val;
				varName = "Ch" + varName;
				varNames[i] = varName;
				varIDs.put(varName, i);
				i++;
			}
		}

		for (String key : counts.keySet()) {
			int count = counts.get(key);
			// here's our encoding: YYYYTypeID
			String year = key.substring(0, 4);
			String type = key.substring(4, 5);
			String ID = key.substring(5);
			String typeName = "";
			if (type.equals(Crime.Robbery.initial)) {
				typeName = Crime.Robbery.name();
			} else if (type.equals(Crime.Assault.initial)) {
				typeName = Crime.Assault.name();
			} else if (type.equals(Crime.Homicide.initial)) {
				typeName = Crime.Homicide.name();
			}
			int place = varIDs.get(typeName + year);
			double[] data = (double[]) dataSetNums[place];
			int id = Integer.valueOf(ID);
			data[id] = count;

		}

		i = 0;
		int var = 0;

		// robbery
		for (int obs = 0; obs < dsaBGs.getNumObservations(); obs++) {
			double[] threeData = new double[3];
			double mean = 0;
			for (i = 0; i < 3; i++) {
				double[] data = (double[]) dataSetNums[i];
				mean = mean + data[obs];
				threeData[i] = data[obs];
			}
			mean = mean / 3;
			double[] zScores = DescriptiveStatistics
					.calculateZScores(threeData);
			double diff = 0;
			for (i = 0; i < 3; i++) {
				double[] data = (double[]) dataSetNums[i];
				diff = data[obs] - mean;
				double[] diffData = (double[]) dataSetNums[nVars / 2 + i];
				diffData[obs] = zScores[i];
			}
		}

		// robbery
		for (int obs = 0; obs < dsaBGs.getNumObservations(); obs++) {
			double mean = 0;
			for (i = 3; i < 6; i++) {
				double[] data = (double[]) dataSetNums[i];
				mean = mean + data[obs];
			}
			mean = mean / 3;
			double diff = 0;
			for (i = 3; i < 6; i++) {
				double[] data = (double[]) dataSetNums[i];
				diff = data[obs] - mean;
				double[] diffData = (double[]) dataSetNums[nVars / 2 + i];
				diffData[obs] = diff;
			}
		}

		// homicide
		for (int obs = 0; obs < dsaBGs.getNumObservations(); obs++) {

			double mean = 0;
			for (i = 6; i < 9; i++) {
				double[] data = (double[]) dataSetNums[i];
				mean = mean + data[obs];
			}
			mean = mean / 3;
			double diff = 0;
			for (i = 6; i < 9; i++) {
				double[] data = (double[]) dataSetNums[i];
				diff = data[obs] - mean;
				double[] diffData = (double[]) dataSetNums[nVars / 2 + i];
				diffData[obs] = diff;
			}
		}

		Object[] dataObject = new Object[dataSetNums.length + 3];
		dataObject[0] = varNames;
		for (int j = 0; j < dataSetNums.length; j++) {
			dataObject[j + 1] = dataSetNums[j];
		}
		dataObject[dataObject.length - 2] = dsaBGs.getShapeData();
		dataObject[dataObject.length - 1] = dsaBGs.getSpatialWeights();
		DataSetForApps dsa = new DataSetForApps(dataObject);
		System.out.println("hit " + hits + " out of "
				+ dsaCrime.getNumObservations());
		return dsa;

	}

	private enum Crime {
		Robbery("R"), Assault("A"), Homicide("H");
		String initial;

		Crime(String initial) {
			this.initial = initial;
		}
	}

	private enum Year {
		Six(2006), Seven(2007), Eight(2008);
		int val;

		Year(int val) {
			this.val = val;
		}
	}

	private static void testShapefileWriter() {
		GeoDataGeneralizedStates states = new GeoDataGeneralizedStates();
		String fileRoot = "C://temp//shp//shpfile";
		ToolkitIO.saveShapeFile(new File(fileRoot), states.getDataForApps());

		ShapeFileDataReader reader = new ShapeFileDataReader();
		reader.setFileName(fileRoot + ".shp");
		DataSetForApps dsa = reader.getDataForApps();
		GeoMap map = new GeoMap();
		map.setDataSet(dsa);
		JFrame fram = new JFrame();
		fram.add(map);
		fram.pack();
		fram.setVisible(true);
	}

	private static void testImageWrite() {
		JFrame app = new JFrame("image test");
		JPanel pan = new JPanel();
		pan.setBackground(Color.pink);
		// gvt.setVisible(true);
		pan.setMinimumSize(new Dimension(300, 300));
		pan.setPreferredSize(new Dimension(300, 300));
		app.add(pan);
		app.pack();
		app.setVisible(true);
		// ToolkitIO.saveCommentedImage(app);
		logger.finest(ToolkitIO.openCommentedImage(""));
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public static void saveShapeFile(File location, DataSetForApps dataSet) {
		Geometry[] geoms = dataSet.getGeomData();
		if (geoms == null) {
			Shape[] shps = dataSet.getShapeData();
			geoms = ShapeFileDataReader.shapesToGeoms(shps);
		}
		ShapeFileDataWriter.writeShapefile(geoms, location.getPath());
		ShapeFileDataWriter.writeDBFile(dataSet.getAttributeNamesOriginal(),
				dataSet.getNamedArrays(), dataSet.getNumObservations(),
				location.getPath());
	}

	public static void saveShapeFile(Component parent, DataSetForApps dataSet) {
		logger.info("start");
		String shapeFileName = ToolkitIO.getFileName(parent,
				ToolkitIO.Action.SAVE, ToolkitIO.FileType.SHAPEFILE);
		ShapeFileDataWriter
				.writeShapefile(dataSet.getGeomData(), shapeFileName);
		ShapeFileDataWriter.writeDBFile(dataSet.getAttributeNamesOriginal(),
				dataSet.getNamedArrays(), dataSet.getNumObservations(),
				shapeFileName);
		logger.info("finish!");

	}
}
