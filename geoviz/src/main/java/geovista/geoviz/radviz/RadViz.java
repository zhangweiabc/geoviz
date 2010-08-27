package geovista.geoviz.radviz;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.ItemSelectable;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.data.DataSetForApps;
import geovista.common.event.ClassificationResultEvent;
import geovista.common.event.ClassificationResultListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.readers.csv.CSVFileDataReader;

/**
 * RadViz is a GUI component that provides an interactive RadViz plot for
 * exploring multivariate datasets.
 * <p>
 * <p>
 * Copyright: Copyright (c) 2004
 * <p>
 * Company: GeoVISTA
 * 
 * @author Gary L.
 * 
 */
public class RadViz extends JPanel implements ActionListener,
		ClassificationResultListener, DataSetListener, MouseListener,
		MouseMotionListener, IndicationListener, ItemListener,
		SelectionListener, SubspaceListener, TableModelListener

{
	public static final boolean DEBUG = false;
	public static final int NUM_CLASS_DEFAULT = 1;
	public static final int ANIMATION_ND = 0;
	public static final int ANIMATION_GT2D = 1;
	public static final int ANIMATION_GTND = 2;
	public static final String CLR_CODE_ATT_DEFAULT = "Classification";
	public static final String ANIMATION_CMD = "Animation";
	public static final String SET_CLR_ATT_CMD = "Assign Color By Attribute";
	public static final String CLR_SETTINGS_CMD = "Style";
	public static final String GT_ANIMATION_CMD = "Grand Tour Animation";
	public static final String TOGGLE_INDICATION_CMD = "Indication On";
	public static final String SET_VISIBLE_ATT_CMD = "Select Data Attributes";
	public static final String TOGGLE_LEGEND_CMD = "Show Legend";
	public static final String RECENTER_CMD = "Recenter";
	public static final String PAN_CMD = "Pan";
	public static final String NO_ZOOM_CMD = "Normal 1:1";
	public static final String ZOOM_IN_CMD = "Zoom In";
	public static final String ZOOM_OUT_CMD = "Zoom Out";

	final static Logger logger = Logger.getLogger(RadViz.class.getName());

	private Color backgroundColor = Color.white,
			selectionColor = Color.magenta;
	private Color[] colors = new Color[0];
	private DataSetForApps datasetForApp = new DataSetForApps();
	private double[] attWeights = new double[0];
	private int alpha = 255, indicationId = -1, numClass = NUM_CLASS_DEFAULT;
	private int pointSize = 5;
	private int[] classification = new int[0], selection = new int[0];
	private int[] visibleAttIdx = new int[0];
	private JCheckBoxMenuItem indicationOnItem = null, showLegendItem = null;
	private final JPopupMenu popup = new JPopupMenu();
	private List classLabels = null;
	// fah 2007
	private String colorCodeAtt = CLR_CODE_ATT_DEFAULT, colorScheme = "ble";// OriginalColor.SET1;
	// protected static final OriginalColor origClr = new OriginalColor();

	protected static transient AffineTransform radvizTransform;
	protected transient AttSelectPanel attSelPanel = null;
	protected transient AttSetWeightPanel attSetWtPanel = null;
	protected transient AttWeightsAnimationPanel animationPanel = null;
	protected transient boolean draggingLeftBtn = false, drawLegendTip = false,
			indicationOn = false;
	protected transient boolean leftBtnPressed = false,
			leftBtnPressedOnAttLbl = false;
	protected transient boolean panModeOn = false, showClassLegend = false;
	protected transient BufferedImage ri = null;
	protected transient ButtonGroup zoomBtnGroup = null;
	protected Cursor defaultCursor = getCursor();

	protected Cursor handCursor = Cursor
			.getPredefinedCursor(Cursor.HAND_CURSOR);
	protected Dimension toolTipSize = new Dimension();
	protected double lastSetWtOldWt, zoomScale = 1.0;
	protected transient double offsetX = 0.0, offsetY = 0.0;
	protected transient double startX, startY;
	protected double[] attWeightsBkup = new double[0], diff = new double[0];
	protected double[] endWts = new double[0];
	protected double[] sx = new double[0], sy = new double[0];
	protected double[] ux = new double[0], uy = new double[0];
	protected double[][] data = new double[0][0];
	protected EventListenerList listenerList = new EventListenerList();
	protected GeneralPath draggingPath = new GeneralPath();

	protected Ellipse2D radvizBox = new Ellipse2D.Double();
	protected transient GrandTourPanel gtPanel = null;
	protected int animationType = -1, radius = 0;
	protected transient int currFrameId, dstAttIdx, srcAttIdx, gtCurrAtt,
			gtCurrRound;
	protected transient int draggingLastX, draggingLastY;
	protected transient int draggingFirstX, draggingFirstY;
	protected transient int lastSetWtAttIdx, numFrames, currX, currY;
	protected transient JDialog animationDialog = null, attSelDialog = null;
	protected transient JDialog attSetWtDialog = null,
			colorSettingsDialog = null;
	protected transient JDialog gtDialog = null;
	protected transient JFrame dummyFrame = null;
	protected transient JMenu zoomInMenu = null, zoomOutMenu = null;
	protected transient JMenuItem gtAnimationItem = null, panItem = null,
			recenterItem = null;
	protected transient JRadioButtonMenuItem noZoomItem = null;
	protected transient RadvizColorSettingsGUI colorSettingsPanel = null;
	protected Rectangle2D attDraggingIcon = new Rectangle2D.Double();
	protected Rectangle2D legendBox = new Rectangle2D.Double();
	protected Rectangle2D[] visibleAttLblBnds = new Rectangle2D[0];
	protected transient String indication_text;
	protected Timer timer = new Timer(1000, this);
	protected Vector missingData = new Vector();

	/**
	 * Default constructor.
	 */
	public RadViz() {
		// try {
		// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		// }
		// catch (Exception e) {
		// e.printStackTrace();
		// }
		setPreferredSize(new Dimension(500, 500));
		// setMinimumSize(getPreferredSize());
		addMouseListener(this);
		addMouseMotionListener(this);
		initPopupMenu();

		// fah 2007
		// setColors(getDistinctColors(getColorScheme(), getNumClass()));
	}

	/**
	 * Initializes the popup menu for the RadViz GUI.
	 */
	protected void initPopupMenu() {
		JMenuItem menuItem = new JMenuItem(ANIMATION_CMD);
		menuItem.addActionListener(this);
		popup.add(menuItem);
		menuItem = new JMenuItem(SET_CLR_ATT_CMD);
		menuItem.addActionListener(this);
		popup.add(menuItem);
		gtAnimationItem = new JMenuItem(GT_ANIMATION_CMD);
		gtAnimationItem.addActionListener(this);
		popup.add(gtAnimationItem);
		indicationOnItem = new JCheckBoxMenuItem(TOGGLE_INDICATION_CMD);
		indicationOnItem.addItemListener(this);
		popup.add(indicationOnItem);
		menuItem = new JMenuItem(SET_VISIBLE_ATT_CMD);
		menuItem.addActionListener(this);
		popup.add(menuItem);
		showLegendItem = new JCheckBoxMenuItem(TOGGLE_LEGEND_CMD);
		showLegendItem.addItemListener(this);
		popup.add(showLegendItem);
		menuItem = new JMenuItem(CLR_SETTINGS_CMD);
		menuItem.addActionListener(this);
		popup.add(menuItem);
		popup.addSeparator();
		recenterItem = new JMenuItem(RECENTER_CMD, new ImageIcon(this
				.getClass().getResource("resources/home16.gif")));
		recenterItem.addActionListener(this);
		recenterItem.setVisible(false);
		popup.add(recenterItem);
		panItem = new JMenuItem(PAN_CMD, new ImageIcon(this.getClass()
				.getResource("resources/pan16.gif")));
		panItem.addActionListener(this);
		popup.add(panItem);
		zoomBtnGroup = new ButtonGroup();
		noZoomItem = new JRadioButtonMenuItem(NO_ZOOM_CMD);
		noZoomItem.addActionListener(this);
		noZoomItem.setVisible(false);
		zoomBtnGroup.add(noZoomItem);
		popup.add(noZoomItem);
		zoomInMenu = new JMenu(ZOOM_IN_CMD);
		zoomOutMenu = new JMenu(ZOOM_OUT_CMD);
		JRadioButtonMenuItem rbMenuItem;
		for (int i = 2; i < 25; i++) {
			rbMenuItem = new JRadioButtonMenuItem(i + ":1");
			rbMenuItem.addItemListener(this);
			zoomBtnGroup.add(rbMenuItem);
			zoomInMenu.add(rbMenuItem);
			rbMenuItem = new JRadioButtonMenuItem("1:" + i);
			rbMenuItem.addItemListener(this);
			zoomBtnGroup.add(rbMenuItem);
			zoomOutMenu.add(rbMenuItem);
		}
		zoomInMenu.setEnabled(false);
		zoomOutMenu.setEnabled(false);
		popup.add(zoomInMenu);
		popup.add(zoomOutMenu);
	}

	/**
	 * Creates the dataset to be displayed in RadViz GUI from a flat csv file.
	 * 
	 * @param path
	 *            absolute path to the csv file
	 */
	public void setAbsolutePath(String path) {
		CSVFileDataReader csvReader = new CSVFileDataReader();
		csvReader.setFileName(path);
		DataSetForApps da = csvReader.getDataForApps();
		setDataSet(da);
	}

	// /**
	// * Creates the dataset to be displayed in RadViz GUI from the result of a
	// * supervised classification task.
	// *
	// * @param result supervised classification result
	// */
	// public void setApplyResult(JDMApplyResult result) {
	// //Get the dataset without the classification results
	// Object[] data_set = result.getData();
	//        
	// //Get the class labels;
	// List classLabels = result.getClassLabels();
	//        
	// //Get the classification indices
	// int[] class_indices = result.getClassIndices();
	//        
	// //Create a new String array to store the class label for each data record
	// String[] classification_str = new String[class_indices.length];
	// for (int i = 0; i < classification_str.length; i++) {
	// classification_str[i] = (String)classLabels.get(class_indices[i]);
	// }
	//        
	// //Append the new array of class labels as a new attribute to the dataset
	// String[] old_att = (String[])data_set[0];
	// String[] new_att = new String[old_att.length + 1];
	// for (int i = 0; i < old_att.length; i++) {
	// new_att[i] = old_att[i];
	// }
	// new_att[new_att.length - 1] = new String("Classification");
	// setColorCodeAtt("Classification");
	// Object[] new_data = new Object[data_set.length + 1];
	// new_data[0] = new_att;
	// for (int i = 1; i < data_set.length; i++) {
	// new_data[i] = data_set[i];
	// }
	// new_data[new_data.length - 1] = classification_str;
	//        
	// //Convert the dataset to a DataSetForApp to pass to the program
	// DataSetForApps da = new DataSetForApps();
	// da.setDataObject(new_data);
	// try {
	// setDatasetForApp(da);
	// } catch (Exception e) {
	// System.err.println("RadViz.setApplyResult():" + e.getMessage());
	// e.printStackTrace();
	// }
	// }
	//    

	// /**
	// * Sets the result of an unsupervised classification task to be displayed
	// * in RadViz GUI.
	// *
	// * @param result unsupervised classification result
	// */
	// public void setUnsupervisedResult(JDMUnsupervisedResult result) {
	// //Get the list of categories
	// CategoryItf[] categories = result.getCategories();
	//        
	// //Set the classification indices, class labels and colors
	// if (datasetForApp != null &&
	// datasetForApp.getNumObservations() == result.getNumberOfInstances())
	// {
	// int[] indices = new int[datasetForApp.getNumObservations()];
	// Vector labels = new Vector();
	// for (int i = 0; i < categories.length; i++) {
	// labels.add(categories[i].getName());
	// java.util.Iterator itr = categories[i].getMemberIds().iterator();
	// while (itr.hasNext()) {
	// Integer help = (Integer)itr.next();
	// indices[help.intValue()] = i;
	// }
	// }
	// setClassification(indices);
	// setClassLabels(labels);
	// if (getClassLabels().size() != getNumClass()) {
	// setNumClass(getClassLabels().size());
	// setColors(getDistinctColors(getColorScheme(), getNumClass()));
	// }
	// makePlotLayerImg();
	// repaint();
	// }
	// }
	//    
	/**
	 * Sets the dataset to be displayed in RadViz GUI.
	 * 
	 * @param datasetForApp
	 *            the dataset to be displayed
	 */
	public void setDataSet(DataSetForApps datasetForApp) {
		this.datasetForApp = datasetForApp;
		// fireDataSetChanged(datasetForApp.getDataSetFull());

		// Set atttribute weights and indices of visible attributes
		int[] attIdx = new int[datasetForApp.getNumberNumericAttributes()];
		double[] attWts = new double[attIdx.length];
		for (int i = 0; i < attIdx.length; i++) {
			attIdx[i] = i;
			attWts[i] = 1.0;
		}
		setAttWeights(attWts);
		setVisibleAttIdx(attIdx);
		resetAttLabelCoordinates();

		// Set color code attribute and update classification indices and class
		// labels
		// if necessary
		if (getColorCodeAtt() == null) {
			setColorCodeAtt(CLR_CODE_ATT_DEFAULT);
		}
		Vector labels = new Vector();
		labels.add(new String("All"));
		setClassLabels(labels);
		int[] default_classification = new int[datasetForApp
				.getNumObservations()];
		java.util.Arrays.fill(default_classification, 0);
		setClassification(default_classification);
		if (1 != getNumClass()) {
			setNumClass(1);
			// fah 2007
			// setColors(getDistinctColors(getColorScheme(), getNumClass()));
		}

		// updateClassificationAndClassLabels(getColorCodeAtt());

		// normalize the data values as required by RadViz method
		data = new double[datasetForApp.getNumberNumericAttributes()][];
		for (int i = 0; i < data.length; i++) {
			double[] help = datasetForApp.getNumericDataAsDouble(i);
			data[i] = help.clone();// new double[help.length];
			// System.arraycopy(help, 0, data[i], 0, help.length);
			/*
			 * if (attNames[i].equalsIgnoreCase("RCALLAWhF9901")) { for (int j =
			 * 0; j < data[i].length; j++) { logger.finest(data[i][j]); } }
			 */
		}
		for (int i = 0; i < data.length; i++) {
			normalize(data[i], i);
			/*
			 * if (attNames[i].equalsIgnoreCase("RCALLAWhF9901")) { for (int j =
			 * 0; j < data[i].length; j++) { logger.finest(data[i][j]); } }
			 */
		}
		resetDataLabelCoordinates();

		makePlotLayerImg();
		repaint();
	}

	/**
	 * Returns the dataset displayed in RadViz GUI.
	 * 
	 * @return returns the dataset displayed in RadViz GUI
	 */
	public DataSetForApps getDatasetForApp() {
		return datasetForApp;
	}

	/**
	 * Returns names of numeric attributes of the dataset displayed in RadViz
	 * GUI.
	 * 
	 * @return returns the names of all numeric attributes
	 */
	public String[] getAttNamesNumeric() {
		return getDatasetForApp().getAttributeNamesNumeric();
	}

	/**
	 * Returns names of all attributes of the dataset displayed in RadViz GUI.
	 * 
	 * @return returns the names of all attributes
	 */
	public String[] getAttNamesOriginal() {
		return getDatasetForApp().getAttributeNamesOriginal();
	}

	/**
	 * Returns the attribute weights of the dataset.
	 * 
	 * @return returns the attribute weights
	 */
	public double[] getAttWeights() {
		return attWeights;
	}

	/**
	 * Sets the attribute weights of the dataset.
	 * 
	 * @param attWeights
	 *            the attribute weights
	 */
	public void setAttWeights(double[] attWeights) {
		this.attWeights = attWeights;
	}

	/**
	 * Returns the background color of RadViz GUI.
	 * 
	 * @return returns the background color of RadViz GUI
	 */
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * Sets the background color of RadViz GUI.
	 * 
	 * @param backgroundColor
	 *            the background color of RadViz GUI
	 */
	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	/**
	 * Returns the classification indices.
	 * 
	 * @return returns the classification indices
	 */
	public int[] getClassification() {
		return classification;
	}

	/**
	 * Sets the classification indices.
	 * 
	 * @param classification
	 *            the classification indices
	 */
	public void setClassification(int[] classification) {
		this.classification = classification;
	}

	/**
	 * Returns the class labels.
	 * 
	 * @return returns the class labels
	 */
	public List getClassLabels() {
		return classLabels;
	}

	/**
	 * Sets the class labels.
	 * 
	 * @param classLabels
	 *            the class labels
	 */
	public void setClassLabels(List classLabels) {
		this.classLabels = classLabels;
	}

	/**
	 * Returns the color code attribute.
	 * 
	 * @return returns the color code attribute
	 */
	public String getColorCodeAtt() {
		return colorCodeAtt;
	}

	/**
	 * Sets the color code attribute.
	 * 
	 * @param colorCodeAtt
	 *            the color code attribute
	 */
	public void setColorCodeAtt(String colorCodeAtt) {
		this.colorCodeAtt = colorCodeAtt;
	}

	/**
	 * Returns the array of distinct colors used to color code the data.
	 * 
	 * @return returns the array of distinct colors
	 */
	public Color[] getColors() {
		return colors;
	}

	/**
	 * Sets the array of distinct colors used to color code the data.
	 * 
	 * @param colors
	 *            the array of distinct colors
	 */
	public void setColors(Color[] colors) {
		this.colors = colors;
	}

	/**
	 * Returns the color scheme of the array of distinct colors used to color
	 * code the data.
	 * 
	 * @return returns the color scheme of the array of distinct colors
	 */
	public String getColorScheme() {
		return colorScheme;
	}

	/**
	 * Sets the color scheme of the array of distinct colors used to color code
	 * the data.
	 * 
	 * @param colorScheme
	 *            the color scheme of the array of distinct colors
	 */
	public void setColorScheme(String colorScheme) {
		this.colorScheme = colorScheme;
	}

	/**
	 * Returns the index of the indicated data point.
	 * 
	 * @return returns the index of the indicated data point
	 */
	public int getIndicationId() {
		return indicationId;
	}

	/**
	 * Sets the index of the indicated data point.
	 * 
	 * @param indicationId
	 *            the index of the indicated data point
	 */
	public void setIndicationId(int indicationId) {
		this.indicationId = indicationId;
	}

	/**
	 * Sets up the content of the tooltip for the indicated data point.
	 * 
	 * @param indicationId
	 *            the index of the indicated data point
	 */
	public void setIndicationToolTip(int indicationId) {
		if (indicationId >= 0) {
			String[] attNames = getAttNamesNumeric();
			indication_text = "Index = " + (indicationId + 1) + ";";
			FontMetrics metrics = getGraphics().getFontMetrics();
			toolTipSize.height = (visibleAttIdx.length + 1)
					* metrics.getHeight() + 8;
			for (int element : visibleAttIdx) {
				String help_str = (attNames[element] + " = "
						+ data[element][indicationId] + ";");
				// datasetForApp.getNumericDataAsDouble(
				// visibleAttIdx[i])[indicationId] + ";");
				int help_width = metrics.stringWidth(help_str) + 5;
				if (toolTipSize.width < help_width) {
					toolTipSize.width = help_width;
				}
				indication_text += help_str;
			}
			// setToolTipText(indication_text);
		} else {
			indication_text = null;
			// setToolTipText(null);
		}
	}

	/**
	 * Returns the number of classes.
	 * 
	 * @return returns the number of classes
	 */
	public int getNumClass() {
		return numClass;
	}

	/**
	 * Sets the number of classes.
	 * 
	 * @param numClass
	 *            the number of classes
	 */
	public void setNumClass(int numClass) {
		this.numClass = numClass;
	}

	/**
	 * Returns the indices of the selected data points.
	 * 
	 * @return returns the indices of the selected data points
	 */
	public int[] getSelection() {
		return selection;
	}

	/**
	 * Sets the indices of the selected data points.
	 * 
	 * @param selection
	 *            the indices of the selected data points
	 */
	public void setSelection(int[] selection) {
		this.selection = selection;
	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, selection);
	}

	/**
	 * Returns the color used to highlight the selected data points in RadViz
	 * GUI.
	 * 
	 * @return returns the selection color
	 */
	public Color getSelectionColor() {
		return selectionColor;
	}

	/**
	 * Sets the color used to highlight the selected data points in RadViz GUI.
	 * 
	 * @param selectionColor
	 *            the selection color
	 */
	public void setSelectionColor(Color selectionColor) {
		this.selectionColor = selectionColor;
	}

	/**
	 * Sets the menu items in the popup menu to enabled or disabled status.
	 * 
	 * @param enabled
	 *            a boolean representing the desired status
	 */
	private void setPopupItems(boolean enabled) {
		MenuElement[] items = popup.getSubElements();
		for (MenuElement element : items) {
			if (element == panItem || element == zoomInMenu
					|| element == zoomOutMenu) {
				if (!enabled || getVisibleAttIdx().length > 2) {
					element.getComponent().setEnabled(enabled);
				}
			} else {
				element.getComponent().setEnabled(enabled);
			}
		}
	}

	/**
	 * Returns the indices of those attributes that will be displayed in RadViz
	 * GUI.
	 * 
	 * @return returns the indices of the visible attributes
	 */
	public int[] getVisibleAttIdx() {
		return visibleAttIdx;
	}

	/**
	 * Sets the indices of those attributes that will be displayed in RadViz
	 * GUI.
	 * 
	 * @param visibleAttIdx
	 *            the indices of the visible attributes
	 */
	public void setVisibleAttIdx(int[] visibleAttIdx) {
		this.visibleAttIdx = visibleAttIdx;
		if (visibleAttIdx == null || visibleAttIdx.length < 3) {
			zoomInMenu.setEnabled(false);
			zoomOutMenu.setEnabled(false);
			panItem.setEnabled(false);
		} else {
			zoomInMenu.setEnabled(true);
			zoomOutMenu.setEnabled(true);
			panItem.setEnabled(true);
		}
	}

	/**
	 * Resets the screen coordinates of attribute labels displayed in RadViz
	 * GUI.
	 */
	protected void resetAttLabelCoordinates() {
		int numAtt = visibleAttIdx.length;
		// x, y coordiantes of labels for visible attributes
		sx = new double[numAtt];
		sy = new double[numAtt];
		// rectangular boxes outlining the labels for visible attributes
		visibleAttLblBnds = new Rectangle2D[numAtt];
	}

	/**
	 * Resets the screen coordinates of data points displayed in RadViz GUI.
	 */
	protected void resetDataLabelCoordinates() {
		int numData = datasetForApp.getNumObservations();
		ux = new double[numData];
		uy = new double[numData];
	}

	/**
	 * Updates the classification indices and class labels based on the
	 * colorCodeAtt, an attribute by whose values we will assign the data
	 * records into groups represented by distinct colors and class labels. Thus
	 * it is possible for us to color code the data points not only by the
	 * results of supervised or unsupervised classification tasks but also by
	 * any categorical attribute.
	 * 
	 * @param colorCodeAtt
	 *            the attribute used to color code the dataset
	 */
	protected void updateClassificationAndClassLabels(String colorCodeAtt) {
		Object[] objs = datasetForApp.getDataSetFull();
		String[] names = (String[]) objs[0];
		int[] indices;
		Vector labels;
		Hashtable classHash = new Hashtable();
		int index = 0;
		for (int i = 0; i < names.length; i++) {
			if (names[i].equalsIgnoreCase(colorCodeAtt)) {
				index = i + 1;
			}
		}
		if (index < 1) {
			return;
		}
		int class_count = 0;
		if (objs[index] instanceof String[]) {
			String[] s = (String[]) objs[index];
			indices = new int[s.length];
			labels = new Vector();
			for (int i = 0; i < s.length; i++) {
				if (classHash.get(s[i]) == null) {
					classHash.put(s[i], Integer.toString(class_count));
					indices[i] = class_count;
					labels.add(s[i]);
					++class_count;
				} else {
					indices[i] = Integer.parseInt((String) classHash.get(s[i]));
				}
			}
		} else if (objs[index] instanceof int[]) {
			int[] ia = (int[]) objs[index];
			indices = new int[ia.length];
			labels = new Vector();
			for (int i = 0; i < ia.length; i++) {
				String tempClassLbl = Integer.toString(ia[i]);
				if (classHash.get(tempClassLbl) == null) {
					classHash.put(tempClassLbl, Integer.toString(class_count));
					indices[i] = class_count;
					labels.add(tempClassLbl);
					++class_count;
				} else {
					indices[i] = Integer.parseInt((String) classHash
							.get(tempClassLbl));
				}
			}
		} else if (objs[index] instanceof boolean[]) {
			labels = new Vector();
			labels.add(new String("True"));
			labels.add(new String("False"));
			boolean[] b = (boolean[]) objs[index];
			indices = new int[b.length];
			for (int i = 0; i < indices.length; i++) {
				if (b[i]) {
					indices[i] = 0;
				} else {
					indices[i] = 1;
				}
			}
		} else {
			return;
			/*
			 * labels = new Vector(); labels.add(new String("All")); indices =
			 * new int[datasetForApp.getNumObservations()];
			 * java.util.Arrays.fill(indices, 0); if
			 * (!getColorCodeAtt().equalsIgnoreCase(CLR_CODE_ATT_DEFAULT)) {
			 * setColorCodeAtt(CLR_CODE_ATT_DEFAULT); }
			 */
		}
		setClassification(indices);
		setClassLabels(labels);
		if (getClassLabels().size() != getNumClass()) {
			setNumClass(getClassLabels().size());
			// fah 2007
			// setColors(getDistinctColors(getColorScheme(), getNumClass()));
		}
	}

	// /**
	// * Generates an user specified number of distinct colors using the user
	// specified
	// * color scheme.
	// *
	// * @param colorScheme the desired color scheme for generating distinct
	// colors
	// * @param numClass the desired number of distinct colors to be generated
	// * @return returns an array of distinct colors
	// */
	// public static Color[] getDistinctColors(String colorScheme, int numClass)
	// {
	// int[][] color_scheme = origClr.makescheme(colorScheme, numClass);
	// Color[] distinctColors = origClr.transform(color_scheme, numClass);
	// return distinctColors;
	// }

	/**
	 * Finds out the index of the data point that is currently pointed to by the
	 * mouse so that RadViz GUI can indicate the pointed data point.
	 * 
	 * @param xpos
	 *            the x screen coordinate of the current mouse position
	 * @param ypos
	 *            the y screen coordinate of the current mouse position
	 * @return returns the index of the pointed data point or -1 if not found
	 */
	protected int getIndexOfIndicated(int xpos, int ypos) {
		int index = -1;
		Ellipse2D e2d;

		if (visibleAttIdx != null) {
			if (visibleAttIdx.length == 2) {
				for (int i = 0; i < ux.length; i++) {
					e2d = new Ellipse2D.Double(ux[i] - pointSize, uy[i]
							- pointSize, 2 * pointSize + 1, 2 * pointSize + 1);
					if (e2d.contains(xpos, ypos)) {
						index = i;
						break;
					}
				}
			} else if (visibleAttIdx.length > 2) {
				for (int i = 0; i < ux.length; i++) {
					e2d = new Ellipse2D.Double(ux[i] - pointSize, uy[i]
							- pointSize, 2 * pointSize + 1, 2 * pointSize + 1);
					Shape s = radvizTransform.createTransformedShape(e2d);
					if (s.contains(xpos, ypos)) {
						index = i;
						break;
					}
				}
			}
		}
		return index;
	}

	/**
	 * Selects all data points that are within the user dragged selection box.
	 */
	protected void makeSelection() {
		int[] helpArray = new int[ux.length];
		int count = 0;
		if (visibleAttIdx != null) {
			if (visibleAttIdx.length == 2) {
				for (int i = 0; i < ux.length; i++) {
					if (draggingPath.contains(ux[i], uy[i])) {
						helpArray[count++] = i;
					}
				}
			} else if (visibleAttIdx.length > 2) {
				for (int i = 0; i < ux.length; i++) {
					Point2D src = new Point2D.Double(ux[i], uy[i]);
					Point2D dst = new Point2D.Double();
					radvizTransform.transform(src, dst);
					// if (draggingPath.contains(dst)) {
					logger
							.finest("d-selected" + dst.getX() + "//"
									+ dst.getY());
					// if (radvizBox.contains(dst)) {
					logger.finest("r-selected");
					// }
					// }
					if (draggingPath.contains(dst)) {
						helpArray[count++] = i;
					}
				}
			}
		}
		logger.finest("count " + count);
		int[] selected = new int[count];
		System.arraycopy(helpArray, 0, selected, 0, count);
		setSelection(selected);
	}

	/**
	 * Does in-place normalization of all values in a double array.
	 * 
	 * @param dstr
	 *            the array of double values to be normalized
	 */
	public void normalize(double[] dstr, int idx) {
		double[] copy = new double[dstr.length];
		System.arraycopy(dstr, 0, copy, 0, dstr.length);
		java.util.Arrays.sort(copy);
		double min = copy[0];
		// double max = copy[copy.length-1];
		int help = copy.length - 1;
		while (Double.isNaN(copy[help])) {
			logger.finest("" + help);
			--help;
		}
		double max = copy[help];
		if (max <= min) {
			return;
		}
		for (int i = 0; i < dstr.length; i++) {
			dstr[i] = (dstr[i] - min) / (max - min);
		}
	}

	/**
	 * Helper function to draw the indication "tooltip" on the given graphics
	 * object.
	 * 
	 * @param g2
	 *            The Graphics2D Object to draw on
	 * @tipText the content of the indication tooltip
	 * @param x
	 *            The x coordinate of the upper left corner
	 * @param y
	 *            The y coordinate of the upper left corner
	 */
	private void drawIndicationTip(Graphics2D g2, int x, int y, String tipText) {
		int offX = 10, offY = 20;
		int topX = x + offX, topY = y + offY;
		// Font f = g2.getFont();
		// g2.setFont(f.deriveFont(9f));

		// Make sure the legend tip doesn't extend beyond the window bounds
		if (getWidth() < (topX + toolTipSize.width)) {
			topX = getWidth() - toolTipSize.width - 1;
		}
		if (getHeight() < (topY + toolTipSize.height)) {
			topY = getHeight() - toolTipSize.height - 1;
		}
		if (topX < 1) {
			topX = 1;
		}
		if (topY < 1) {
			topY = 1;
		}

		g2.drawRect(topX - 1, topY - 1, toolTipSize.width + 2,
				toolTipSize.height + 2);
		Color bkup_clr = g2.getColor();
		Color back = new Color(255, 255, 204);
		g2.setColor(back);
		g2.fillRect(topX, topY, toolTipSize.width, toolTipSize.height);
		g2.setColor(bkup_clr);
		if (tipText != null) {
			StringTokenizer st = new StringTokenizer(tipText, ";");
			int textHeight = g2.getFontMetrics().getHeight();
			int nRows = st.countTokens();
			for (int i = 0; i < nRows; i++) {
				g2.drawString(st.nextToken(), topX + 5, topY + (i + 1)
						* textHeight);
			}
		}
	}

	/**
	 * Helper function to draw a "tooltip" like legend on the given graphics
	 * object.
	 * 
	 * @param g2
	 *            The Graphics2D Object to draw on
	 * @text the textual labels for the classes in the legend
	 * @param x
	 *            The x coordinate of the upper left corner
	 * @param y
	 *            The y coordinate of the upper left corner
	 * @param colors
	 *            The color representing the classes in the legend
	 */
	private void drawLegendTip(Graphics2D g2, int x, int y, List text,
			Color[] colors) {
		int numClass = text.size(), offX = 1, offY = 20;
		int topX = x + offX, topY = y + offY;
		Font f = g2.getFont();
		g2.setFont(f.deriveFont(9f));
		FontMetrics metrics = g2.getFontMetrics();
		int lineHeight = metrics.getHeight() * 2, pad = 5;
		int ovalW = 30, ovalH = lineHeight - 2 * pad;
		int width = 0, height = (numClass * (lineHeight + pad) + pad);
		boolean isBivariate = false;
		String[] lines = new String[0];

		int swidth;
		for (int i = 0; i < numClass; i++) {
			String help = (String) text.get(i);
			StringTokenizer tok = new StringTokenizer(help, ";");
			if (i == 0) {
				if (tok.countTokens() > 1) {
					isBivariate = true;
					lines = new String[numClass * 2];
				} else {
					lines = new String[numClass];
				}
			}
			if (isBivariate) {
				lines[i * 2] = tok.nextToken();
				swidth = metrics.stringWidth(lines[i * 2]);
				if (width < swidth) {
					width = swidth;
				}
				lines[i * 2 + 1] = tok.nextToken();
				swidth = metrics.stringWidth(lines[i * 2 + 1]);
				if (width < swidth) {
					width = swidth;
				}
			} else {
				lines[i] = tok.nextToken();
				swidth = metrics.stringWidth(lines[i]);
				if (width < swidth) {
					width = swidth;
				}
			}
		}
		width += (ovalW + 4 * pad);

		// Make sure the legend tip doesn't extend beyond the window bounds
		if (getWidth() < (topX + width)) {
			topX = getWidth() - width - 1;
		}
		if (getHeight() < (topY + height)) {
			topY = getHeight() - height - 1;
		}
		if (topX < 1) {
			topX = 1;
		}
		if (topY < 1) {
			topY = 1;
		}

		g2.drawRect(topX - 1, topY - 1, width + 2, height + 2);
		Color bkup_clr = g2.getColor();
		Color back = new Color(255, 255, 204);
		g2.setColor(back);
		g2.fillRect(topX, topY, width, height);

		for (int i = 0; i < numClass; i++) {
			g2.setColor(bkup_clr);
			if (isBivariate) {
				g2.drawString(lines[i * 2], topX + 3 * pad + ovalW, topY
						+ (i + 1) * pad + (2 * i + 1) * lineHeight / 2);
				g2.drawString(lines[i * 2 + 1], topX + 3 * pad + ovalW, topY
						+ (i + 1) * (pad + lineHeight));
			} else {
				g2.drawString(lines[i], topX + 3 * pad + ovalW, topY + (i + 1)
						* pad + (3 * i + 2) * lineHeight / 3);
			}
			g2.setColor(colors[i]);
			g2.fillOval(topX + pad, topY + (i + 1) * pad + i * lineHeight + 5,
					ovalW, ovalH);
		}
	}

	/**
	 * Draws the multidimenional RadViz plot.
	 * 
	 * @param g
	 *            the Graphics2D reference to be used for drawing
	 * @param w
	 *            the width of this RadViz panel
	 * @param h
	 *            the height of this RadViz panel
	 */
	protected void drawRadviz(Graphics2D g, int w, int h) {
		if (data == null || data.length == 0) {
			return;
		}
		int numAtt = visibleAttIdx.length;
		int numData = data[0].length;
		String[] attNames = getAttNamesNumeric();

		FontMetrics metrics = g.getFontMetrics();
		int textHeight = metrics.getHeight();
		int pad = textHeight + 10;
		int textPad = 3;
		int xmin = 50, xmax = w, ymin = 0, ymax = h;
		radius = Math.min((xmax - xmin), (ymax - ymin)) / 2 - pad;
		double unitAngle = (Math.PI * 2.0) / numAtt;

		AffineTransform defaultTransform = (AffineTransform) g.getTransform()
				.clone();

		g.translate((xmin + xmax) / 2, (ymin + ymax) / 2);
		if (logger.isLoggable(Level.FINEST)) {
			g.rotate(Math.PI / 2);
			g.drawString("test", 0, 0);
			logger.finest("sy=" + g.getTransform().getScaleY() + "sx="
					+ g.getTransform().getScaleX());
		}
		// Draws the labels of visible attributes
		for (int i = 0; i < numAtt; i++) {
			sx[i] = radius * Math.cos(i * unitAngle);
			sy[i] = radius * (-1) * Math.sin(i * unitAngle);
			float weight = (float) Math
					.round((float) attWeights[visibleAttIdx[i]] * 100) / 100;
			String attLabelStr = attNames[visibleAttIdx[i]] + " (" + weight
					+ ")";
			visibleAttLblBnds[i] = metrics.getStringBounds(attLabelStr, g);
			double x, y;
			double rotationAngle = i * unitAngle;

			// Compute the drawing positions in the rotated coordinate system
			x = -(visibleAttLblBnds[i].getWidth() / 2);
			if (rotationAngle > Math.PI) {
				y = radius + textHeight;
				rotationAngle = -1.0 * Math.PI / 2 - rotationAngle;
			} else {
				y = -radius - textPad;
				rotationAngle = Math.PI / 2 - rotationAngle;
			}
			// remember the drawing positions
			double[] srcPts = new double[8];
			srcPts[0] = x;
			srcPts[1] = y;
			srcPts[2] = x + visibleAttLblBnds[i].getWidth();
			srcPts[3] = y;
			srcPts[4] = x + visibleAttLblBnds[i].getWidth();
			srcPts[5] = y - visibleAttLblBnds[i].getHeight();
			srcPts[6] = x;
			srcPts[7] = y - visibleAttLblBnds[i].getHeight();

			// Rotate the coordinate system and draw labels
			g.rotate(rotationAngle);
			g.drawString(attLabelStr, (float) x, (float) y);
			// Compute the screen coordinates of the drawing positions
			double[] dstPts = new double[8];
			g.getTransform().transform(srcPts, 0, dstPts, 0, 4);
			// Make a bounding box of the drawn label and remember it
			GeneralPath attLblBnd = new GeneralPath();
			attLblBnd.moveTo((float) dstPts[0], (float) dstPts[1]);
			for (int j = 2; j < 8; j += 2) {
				attLblBnd.lineTo((float) dstPts[j], (float) dstPts[j + 1]);
			}
			attLblBnd.lineTo((float) dstPts[0], (float) dstPts[1]);
			attLblBnd.closePath();
			visibleAttLblBnds[i].setRect(attLblBnd.getBounds2D());
			// Recover from the rotation
			g.rotate(-1.0 * rotationAngle);
		}

		// Draws the RadViz box
		/*
		 * if (radvizBox.getCurrentPoint() != null) { radvizBox.reset(); }
		 * radvizBox.moveTo((float)sx[0], (float)sy[0]); for (int i = 1; i <
		 * numAtt; i++) { radvizBox.lineTo((float)sx[i], (float)sy[i]); }
		 * radvizBox.lineTo((float)sx[0], (float)sy[0]); radvizBox.closePath();
		 * radvizBox.transform(g.getTransform());
		 */
		Point2D ul_src = new Point2D.Double(-radius, -radius);
		Point2D ul_dst = new Point2D.Double();
		g.getTransform().transform(ul_src, ul_dst);
		radvizBox
				.setFrame(ul_dst.getX(), ul_dst.getY(), 2 * radius, 2 * radius);

		// Draws the data points
		g.translate(offsetX, offsetY);
		g.scale(zoomScale, zoomScale);
		try {
			// g.setClip(radvizBox.createTransformedShape(g.getTransform().createInverse()));
			g.setClip(g.getTransform().createInverse().createTransformedShape(
					radvizBox));
		} catch (NoninvertibleTransformException nte) {
			System.err.println("RadViz.drawRadviz(): " + nte.getMessage());
			nte.printStackTrace();
		}
		Color clr_bkup = g.getColor();
		Color clr_bkup_alpha = new Color(clr_bkup.getRed(),
				clr_bkup.getGreen(), clr_bkup.getBlue(), alpha);
		if (missingData.size() > 0) {
			missingData.clear();
		}
		for (int i = 0; i < numData; i++) {
			double sumj = 0.0;
			String attValStr = "Index = " + (i + 1);
			for (int j = 0; j < numAtt; j++) {
				sumj += data[visibleAttIdx[j]][i]
						* attWeights[visibleAttIdx[j]];
				attValStr += (" " + attNames[visibleAttIdx[j]] + " = " + data[visibleAttIdx[j]][i]);
			}
			ux[i] = uy[i] = 0;
			if (Double.isNaN(sumj)) {
				missingData.add(attValStr);
				continue;
			}
			for (int j = 0; j < numAtt; j++) {
				double wij = 0.0;
				if (sumj > 0) {
					wij = data[visibleAttIdx[j]][i]
							* attWeights[visibleAttIdx[j]] / sumj;
				}
				ux[i] += wij * sx[j];
				uy[i] += wij * sy[j];
			}
			if (classification != null && classification.length > 0
					&& colors != null && colors.length > 0) {
				g.setColor(colors[classification[i]]);
			} else {
				g.setColor(clr_bkup_alpha);
			}
			g.fill(new Ellipse2D.Double(ux[i] - pointSize, uy[i] - pointSize,
					2 * pointSize + 1, 2 * pointSize + 1));
		}
		radvizTransform = (AffineTransform) g.getTransform().clone();
		g.setClip(null);
		g.setTransform(defaultTransform);
		g.setColor(Color.lightGray);
		Stroke stroke_bkup = g.getStroke();
		float[] dash = { 3.0f };
		g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND, 1.0f, dash, 0.0f));
		for (Rectangle2D element : visibleAttLblBnds) {
			g.draw(element);
		}
		g.setStroke(stroke_bkup);
		g.setColor(clr_bkup);
		g.draw(radvizBox);
		if (logger.isLoggable(Level.FINEST)) {
			for (int i = 0; i < missingData.size(); i++) {
				logger.finest("missing " + missingData.get(i));
			}
		}
	}

	/**
	 * Draws the 2-dimenional scatter plot.
	 * 
	 * @param g
	 *            the Graphics2D reference to be used for drawing
	 * @param w
	 *            the width of this RadViz panel
	 * @param h
	 *            the height of this RadViz panel
	 */
	protected void drawScatter2D(Graphics2D g, int w, int h) {
		double[] xVal = datasetForApp.getNumericDataAsDouble(visibleAttIdx[0]);
		double[] yVal = datasetForApp.getNumericDataAsDouble(visibleAttIdx[1]);
		String[] attNames = getAttNamesNumeric();
		String xName = attNames[visibleAttIdx[0]];
		String yName = attNames[visibleAttIdx[1]];
		double[] xValCopy = xVal.clone();
		Arrays.sort(xValCopy);
		double xMinVal = xValCopy[0];
		int help = xValCopy.length - 1;
		while (Double.isNaN(xValCopy[help])) {
			--help;
		}
		double xMaxVal = xValCopy[help];
		int xFloor = (int) Math.floor(xMinVal);
		int xCeil = (int) Math.ceil(xMaxVal);

		double[] yValCopy = yVal.clone();
		Arrays.sort(yValCopy);
		double yMinVal = yValCopy[0];
		help = yValCopy.length - 1;
		while (Double.isNaN(yValCopy[help])) {
			--help;
		}
		double yMaxVal = yValCopy[help];
		int yFloor = (int) Math.floor(yMinVal);
		int yCeil = (int) Math.ceil(yMaxVal);

		FontMetrics metrics = g.getFontMetrics();
		int textHeight = metrics.getHeight();

		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(xName + ": xmin=" + xMinVal + "xmax=" + xMaxVal);
			logger.finest(yName + ": ymin=" + yMinVal + "ymax=" + yMaxVal);
			Object[] obj = datasetForApp.getDataSetFull();
			if (obj[visibleAttIdx[1] + 1] instanceof double[]) {
				double[] d1 = (double[]) obj[visibleAttIdx[1] + 1];
				for (double element : d1) {
					logger.finest("y=" + element);
				}
			}
		}

		String[] xTicks = new String[6];
		xTicks[0] = String.valueOf((float) xFloor);
		xTicks[5] = String.valueOf((float) xCeil);
		String[] yTicks = new String[6];
		yTicks[0] = String.valueOf((float) yFloor);
		int yTextWidth = metrics.stringWidth(yTicks[0]);
		yTicks[5] = String.valueOf((float) yCeil);
		if (metrics.stringWidth(yTicks[5]) > yTextWidth) {
			yTextWidth = metrics.stringWidth(yTicks[5]);
		}
		float xtdiff = (float) (xCeil - xFloor) / 5;
		float ytdiff = (float) (yCeil - yFloor) / 5;
		for (int i = 1; i < 5; i++) {
			xTicks[i] = String.valueOf((float) Math
					.round((xFloor + i * xtdiff) * 100) / 100);
			yTicks[i] = String.valueOf((float) Math
					.round((yFloor + i * ytdiff) * 100) / 100);
			if (metrics.stringWidth(yTicks[i]) > yTextWidth) {
				yTextWidth = metrics.stringWidth(yTicks[i]);
			}
		}

		int spad = 5;
		int lpad = 20;

		int xmin = textHeight + spad + yTextWidth + spad + spad + 50;
		int xmax = w - lpad;
		int edgeLength = xmax - xmin;
		int ymax = h - (2 * textHeight + 4 * spad);
		int ymin = lpad;
		if (edgeLength > (ymax - ymin)) {
			edgeLength = ymax - ymin;
		}
		xmax = xmin + edgeLength;
		ymin = ymax - edgeLength;

		// Draws labels of x axis and y axis
		g.drawString(xName, (xmin + xmax - metrics.stringWidth(xName)) / 2,
				ymax + 2 * (textHeight + spad));
		visibleAttLblBnds[0] = new Rectangle2D.Double(((xmin + xmax - metrics
				.stringWidth(xName)) / 2), (ymax + textHeight + 2 * spad),
				metrics.stringWidth(xName), textHeight);
		g.rotate(Math.PI / (-2));
		g.drawString(yName, -(ymin + ymax + metrics.stringWidth(yName)) / 2,
				xmin - yTextWidth - 3 * spad);
		g.rotate(Math.PI / 2);
		visibleAttLblBnds[1] = new Rectangle2D.Double((xmin - textHeight
				- yTextWidth - 3 * spad), ((ymin + ymax - metrics
				.stringWidth(yName)) / 2), textHeight, metrics
				.stringWidth(yName));

		// Draws x axis and y axis
		/*
		 * if (radvizBox.getCurrentPoint() != null) { radvizBox.reset(); }
		 * radvizBox.moveTo((float)xmin, (float)ymin);
		 * radvizBox.lineTo((float)xmax, (float)ymin);
		 * radvizBox.lineTo((float)xmax, (float)ymax);
		 * radvizBox.lineTo((float)xmin, (float)ymax);
		 * radvizBox.lineTo((float)xmin, (float)ymin); radvizBox.closePath();
		 * g.draw(radvizBox);
		 */
		radvizBox.setFrame(xmin, ymin, xmax - xmin, ymax - ymin);
		g.drawRect(xmin, ymin, xmax - xmin, ymax - ymin);

		// Draws 6 x ticks and 6 y ticks
		for (int i = 0; i < 6; i++) {
			int ytemp = ymax - i * edgeLength / 5;
			g.drawLine(xmin - spad, ytemp, xmin, ytemp);
			g.drawString(yTicks[i], xmin - 2 * spad
					- metrics.stringWidth(yTicks[i]), ytemp + textHeight / 3);
			int xtemp = xmin + i * edgeLength / 5;
			g.drawLine(xtemp, ymax, xtemp, ymax + spad);
			g.drawString(xTicks[i], xtemp - metrics.stringWidth(xTicks[i]) / 2,
					ymax + spad + textHeight);
		}

		// Draws the data points
		double xValDiff = (xCeil - xFloor);
		double yValDiff = (yCeil - yFloor);
		Color clr_bkup = g.getColor();
		Color clr_bkup_alpha = new Color(clr_bkup.getRed(),
				clr_bkup.getGreen(), clr_bkup.getBlue(), alpha);
		if (missingData.size() > 0) {
			missingData.clear();
		}
		for (int i = 0; i < xVal.length; i++) {
			ux[i] = xmin + edgeLength * (xVal[i] - xFloor) / xValDiff;
			uy[i] = ymax - edgeLength * (yVal[i] - yFloor) / yValDiff;
			String xyValStr = "Index = " + (i + 1) + " ";
			if (Double.isNaN(ux[i]) || Double.isNaN(uy[i])) {
				xyValStr += (xName + " = " + xVal[i] + " " + yName + " = " + yVal[i]);
				missingData.add(xyValStr);
			}
			if (classification != null && classification.length > 0
					&& colors != null && colors.length > 0) {
				g.setColor(colors[classification[i]]);
			} else {
				g.setColor(clr_bkup_alpha);
			}
			g.fill(new Ellipse2D.Double(ux[i] - pointSize, uy[i] - pointSize,
					2 * pointSize + 1, 2 * pointSize + 1));
		}
		g.setColor(Color.lightGray);
		Stroke stroke_bkup = g.getStroke();
		float[] dash = { 3.0f };
		g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND, 1.0f, dash, 0.0f));
		for (Rectangle2D element : visibleAttLblBnds) {
			g.draw(element);
		}
		g.setStroke(stroke_bkup);
		g.setColor(clr_bkup);
		for (int i = 0; i < missingData.size(); i++) {
			logger.finest("missing " + missingData.get(i));
		}
	}

	/**
	 * Draws the plot layer which contains either a multidimenional RadViz plot
	 * or a 2-dimensional scatter plot. This layer is implemented as a
	 * BufferedImage with the same size as the RadViz GUI. On top of this layer
	 * is the selection layer where all selections and indications are
	 * displayed.
	 */
	protected void makePlotLayerImg() {
		int w = getWidth();
		int h = getHeight();
		ri = (BufferedImage) createImage(w, h);
		Graphics2D rig = ri.createGraphics();
		rig.setBackground(getBackgroundColor());
		rig.clearRect(0, 0, w, h);
		if (getVisibleAttIdx() != null && getVisibleAttIdx().length > 1) {
			if (visibleAttIdx.length == 2) {
				drawScatter2D(rig, w, h);
			} else if (visibleAttIdx.length > 2) {
				drawRadviz(rig, w, h);
			}
		}
		rig.dispose();
	}

	/**
	 * Updates the plot layer display. If desired also updates classification
	 * indices and class labels, the selection list and the indication index.
	 * 
	 * @param updateCC
	 *            a boolean value deciding if an update is desired
	 * @param updateIndication
	 *            a boolean value deciding if an update is desired
	 * @param updateSelection
	 *            a boolean value deciding if an update is desired
	 */
	protected void updatePlotLayer(final boolean updateCC,
			final boolean updateIndication, final boolean updateSelection) {
		// final SwingWorker worker = new SwingWorker() {
		// @Override
		// protected Object doInBackground() throws Exception {
		// // TODO Auto-generated method stub
		// return null;
		// }
		// };
		// worker.execute();
	}

	/**
	 * Overrides the default component rendering method. This method first
	 * renders the BufferedImage containing the plot layer, and then renders the
	 * selection layer above it.
	 * 
	 * @param g
	 *            the Graphics reference to be used for rendering this RadViz
	 *            GUI
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		// Draws the plot layer
		if (ri == null || ri.getWidth() != getWidth()
				|| ri.getHeight() != getHeight()) {
			makePlotLayerImg();
		}
		Graphics2D g2d = (Graphics2D) g;
		g2d.drawImage(ri, null, 0, 0);

		if (getVisibleAttIdx() != null && getVisibleAttIdx().length > 2) {
			g2d.setClip(radvizBox);
		}

		Color clr_bkup;

		// Draws selection highlights
		int[] selected = getSelection();
		if (selected != null) {
			clr_bkup = g2d.getColor();
			g2d.setColor(getSelectionColor());
			if (visibleAttIdx != null) {
				if (visibleAttIdx.length == 2) {
					for (int element : selected) {
						g2d.fill(new Ellipse2D.Double(ux[element] - pointSize,
								uy[element] - pointSize, 2 * pointSize + 1,
								2 * pointSize + 1));
					}
				} else if (visibleAttIdx.length > 2) {
					for (int element : selected) {
						Ellipse2D e2d = new Ellipse2D.Double(ux[element]
								- pointSize, uy[element] - pointSize,
								2 * pointSize + 1, 2 * pointSize + 1);
						Shape s = radvizTransform.createTransformedShape(e2d);
						g2d.fill(s);
					}
				}
			}
			g2d.setColor(clr_bkup);
		}

		// Draws indication highlights
		int indicated = getIndicationId();
		if (indicated >= 0) {
			clr_bkup = g2d.getColor();
			g2d.setColor(getSelectionColor());
			if (visibleAttIdx != null) {
				if (visibleAttIdx.length == 2) {
					g2d.fill(new Ellipse2D.Double(ux[indicated] - pointSize,
							uy[indicated] - pointSize, 2 * pointSize + 1,
							2 * pointSize + 1));
				} else if (visibleAttIdx.length > 2) {
					Ellipse2D e2d = new Ellipse2D.Double(ux[indicated]
							- pointSize, uy[indicated] - pointSize,
							2 * pointSize + 1, 2 * pointSize + 1);
					Shape s = radvizTransform.createTransformedShape(e2d);
					g2d.fill(s);
				}
			}
			g2d.setColor(clr_bkup);
		}
		g2d.setClip(null);

		// Draws class legend if desired
		if (showClassLegend && classLabels != null && classLabels.size() > 0) {
			int size = classLabels.size();
			clr_bkup = g2d.getColor();
			Font f = g2d.getFont();
			g2d.setFont(f.deriveFont(9f));
			FontMetrics metrics = g2d.getFontMetrics();
			int topX, w;
			double topY, h;
			if (getVisibleAttIdx() != null && getVisibleAttIdx().length > 1) {
				topX = 10;
				w = 30;
				topY = radvizBox.getBounds2D().getMinY() + metrics.getHeight()
						+ 10;
				h = radvizBox.getBounds2D().getMaxY() - topY;
				h = (h < (10 * size)) ? h : (10 * size);
				legendBox.setRect(topX, topY, w, h);
				g2d.drawString("Legend", (topX + w / 2 - metrics
						.stringWidth("Legend") / 2), (float) (topY - 10));
				double y, unitH = h / size;
				for (int i = 0; i < size; i++) {
					y = topY + i * unitH;
					g2d.setColor(colors[i]);
					g2d.fill(new Rectangle2D.Double(topX, y, w, unitH));
				}
			}
			g2d.setColor(clr_bkup);
			g2d.setFont(f);
		}

		// Draws a "ToolTip" like class legend
		if (drawLegendTip) {
			drawLegendTip(g2d, currX, currY, getClassLabels(), getColors());
		}

		// Draws an indication "ToolTip"
		if (indicationOn && getIndicationId() >= 0) {
			drawIndicationTip(g2d, currX, currY, indication_text);
		}

		// Draws stuff being dragged by the user
		if (draggingLeftBtn) {
			if (leftBtnPressedOnAttLbl) {
				// Draws an attribute label being dragged
				float dash[] = { 3.0f };
				clr_bkup = g2d.getColor();
				g2d.setColor(Color.black);
				Stroke stroke_bkup = g2d.getStroke();
				g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND,
						BasicStroke.JOIN_ROUND, 1.0f, dash, 0.0f));
				g2d.draw(attDraggingIcon);
				g2d.setStroke(stroke_bkup);
				g2d.setColor(clr_bkup);
			} else {
				// Draws a selection box of arbitrary shape being dragged
				clr_bkup = g2d.getColor();
				g2d.setColor(selectionColor);
				g2d.draw(draggingPath);
				g2d.drawLine(draggingLastX, draggingLastY, draggingFirstX,
						draggingFirstY);
				g2d.setColor(clr_bkup);
			}
		}
	}

	/**
	 * Adds the listener object to the list of listeners for actions.
	 * 
	 * @param al
	 *            listener
	 */
	public void addActionListener(ActionListener al) {
		listenerList.add(ActionListener.class, al);
	}

	/**
	 * Removes the listener object from the list.
	 * 
	 * @param al
	 *            listener
	 */
	public void removeActionListener(ActionListener al) {
		listenerList.remove(ActionListener.class, al);
	}

	/**
	 * Fires the actionPerformed event on all the registered listeners.
	 */
	public void fireAction() {
		Object[] listeners = listenerList.getListenerList();
		int numListeners = listeners.length;
		ActionEvent se = new ActionEvent(this, 42, "a string");
		for (int i = 0; i < numListeners; i++) {
			if (listeners[i] == ActionListener.class) {
				// pass the event to the listeners event dispatch method
				((ActionListener) listeners[i + 1]).actionPerformed(se);
			}
		}
	}

	/*
	 * Commented by Gary - seems unnecessary
	 * 
	 * /** Adds the listener object to the list of listeners for datasets.
	 * 
	 * @param dl listener
	 */
	// public void addDataSetListener(DataSetListener dl) {
	// listenerList.add(DataSetListener.class, dl);
	// }
	/**
	 * Removes the listener object from the list.
	 * 
	 * @param dl
	 *            listener
	 */
	// public void removeDataSetListener(DataSetListener dl) {
	// listenerList.remove(DataSetListener.class, dl);
	// }
	/**
	 * Fires the DataSetEvent on all registered listeners for this event type.
	 * 
	 * @param dataSet
	 *            the new dataset
	 */
	/*
	 * Commented by Gary - seems unnecessary public void
	 * fireDataSetChanged(Object[] dataSet) { Object[] listeners =
	 * listenerList.getListenerList(); DataSetEvent e = null;
	 * 
	 * //Process the listeners last to first, notifying //those that are
	 * interested in this event for (int i = listeners.length - 2; i >= 0; i -=
	 * 2) { if (listeners[i] == DataSetListener.class) { // Lazily create the
	 * event if (e == null) { e = new DataSetEvent(this, dataSet); }
	 * ((DataSetListener)listeners[i + 1]).dataSetChanged(e); } } }
	 */

	/**
	 * Adds the listener object to the list of listeners for indications.
	 * 
	 * @param il
	 *            listener
	 */
	public void addIndicationListener(IndicationListener il) {
		listenerList.add(IndicationListener.class, il);
	}

	/**
	 * Removes the listener object from the list.
	 * 
	 * @param il
	 *            listener
	 */
	public void removeIndicationListener(IndicationListener il) {
		listenerList.remove(IndicationListener.class, il);
	}

	/**
	 * Fires the indicationChanged event on all the registered listeners.
	 * 
	 * @param indicator
	 *            the new indication index
	 */
	public void fireIndicationChanged(int indicator) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		IndicationEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == IndicationListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new IndicationEvent(this, indicator);
				}
				((IndicationListener) listeners[i + 1]).indicationChanged(e);
			}
		}
	}

	/**
	 * Adds the listener object to the list of listeners for selections.
	 * 
	 * @param sl
	 *            listener
	 */
	public void addSelectionListener(SelectionListener sl) {
		listenerList.add(SelectionListener.class, sl);
	}

	/**
	 * Removes the listener object from the list.
	 * 
	 * @param sl
	 *            listener
	 */
	public void removeSelectionListener(SelectionListener sl) {
		listenerList.remove(SelectionListener.class, sl);
	}

	/**
	 * Fires the selectionChanged event on all the registered listeners.
	 * 
	 * @param newSelection
	 *            the new list of selected indices
	 */
	private void fireSelectionChanged(int[] newSelection) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		SelectionEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == SelectionListener.class) {
				if (e == null) {
					e = new SelectionEvent(this, newSelection);
				}
				((SelectionListener) listeners[i + 1]).selectionChanged(e);
			}
		}
	}

	/**
	 * Adds the listener object to the list of listeners for supspaces.
	 * 
	 * @param sl
	 *            listener
	 */
	public void addSubspaceListener(SubspaceListener sl) {
		listenerList.add(SubspaceListener.class, sl);
	}

	/**
	 * Removes the listener object from the list.
	 * 
	 * @param sl
	 *            listener
	 */
	public void removeSubspaceListener(SubspaceListener sl) {
		listenerList.remove(SubspaceListener.class, sl);
	}

	/**
	 * Fires the subspaceChanged event on all the registered listeners.
	 * 
	 * @param selection
	 *            the new list of attributes for a new subspace
	 */
	public void fireSubspaceChanged(int[] selection) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		SubspaceEvent e = null;

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == SubspaceListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new SubspaceEvent(this, selection);
				}
				((SubspaceListener) listeners[i + 1]).subspaceChanged(e);
			}
		}
	}

	/**
	 * Implements the MouseListener interface.
	 * 
	 * @param e
	 *            a mouse event
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
			// left button double clicked, opens the attSetWtDialog for changing
			// the
			// weight of the clicked attribute or animating the effects of
			// changing
			// its weight
			if (!panModeOn && visibleAttLblBnds != null
					&& visibleAttLblBnds.length > 2) {
				String[] attNames = getAttNamesNumeric();
				for (int i = 0; i < visibleAttLblBnds.length; i++) {
					if (visibleAttLblBnds[i].contains(e.getX(), e.getY())) {
						lastSetWtAttIdx = visibleAttIdx[i];
						String title = "Set " + attNames[lastSetWtAttIdx]
								+ "'s Weight";
						if (attSetWtDialog == null) {
							if (dummyFrame == null) {
								dummyFrame = new JFrame();
							}
							attSetWtDialog = new JDialog(dummyFrame, title,
									true);
							attSetWtDialog.setLocation(200, 200);
							attSetWtDialog.setSize(300, 180);
							attSetWtDialog.getContentPane().setLayout(
									new BorderLayout());
							if (attSetWtPanel == null) {
								attSetWtPanel = new AttSetWeightPanel();
								attSetWtPanel.addActionListener(this);
							}
							attSetWtDialog.getContentPane().add(attSetWtPanel,
									BorderLayout.CENTER);
						}
						attSetWtPanel.setWeight(attWeights[lastSetWtAttIdx]);
						lastSetWtOldWt = attWeights[lastSetWtAttIdx];
						attSetWtDialog.setTitle(title);
						attSetWtDialog.setVisible(true);
						break;
					}
				}
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	/**
	 * Implements the MouseListener interface.
	 * 
	 * @param e
	 *            a mouse event
	 */
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (panModeOn) {
				// prepare for the use to pan the RadViz image
				startX = e.getPoint().getX();
				startY = e.getPoint().getY();
			} else {
				// Left button down, be prepared for the user to drag either a
				// selection
				// box or an attribute label. Meanwhile, clear the current
				// selection
				if (!leftBtnPressed) {
					leftBtnPressed = true;
				}

				if (visibleAttLblBnds != null) {
					for (int i = 0; i < visibleAttLblBnds.length; i++) {
						if (visibleAttLblBnds[i].contains(e.getX(), e.getY())) {
							// User may drag an attribute label
							if (!leftBtnPressedOnAttLbl) {
								leftBtnPressedOnAttLbl = true;
							}
							srcAttIdx = i;
							attDraggingIcon.setRect(e.getX()
									- visibleAttLblBnds[i].getWidth() / 2.0, e
									.getY()
									- visibleAttLblBnds[i].getHeight() / 2.0,
									visibleAttLblBnds[i].getWidth(),
									visibleAttLblBnds[i].getHeight());
							break;
						}
					}
					if (!leftBtnPressedOnAttLbl) {
						// User may drag a selection box
						if (logger.isLoggable(Level.FINEST)) {
							logger.finest("left button NOT on att");
						}
						if (getSelection() != null) { // Clear current
							// selection
							setSelection(null);
							repaint();
							fireSelectionChanged(getSelection());
						}
						draggingPath.reset();
						draggingFirstX = e.getX();
						draggingFirstY = e.getY();
						draggingPath.moveTo(draggingFirstX, draggingFirstY);
					}
				}
			}
		}
	}

	/**
	 * Implements the MouseListener interface.
	 * 
	 * @param e
	 *            a mouse event
	 */
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (panModeOn) {
				if (radvizBox != null && radvizBox.contains(startX, startY)) {
					offsetX += (e.getPoint().getX() - startX);
					offsetY += (e.getPoint().getY() - startY);
					if (offsetX != 0 || offsetY != 0) {
						recenterItem.setVisible(true);
						updatePlotLayer(false, false, false);
					}
				}
			} else {
				if (leftBtnPressedOnAttLbl) {
					if (draggingLeftBtn) {
						// User drops an attribute label he has been dragging
						draggingLeftBtn = false;
						dstAttIdx = -1;
						for (int i = 0; i < visibleAttLblBnds.length; i++) {
							if (visibleAttLblBnds[i].contains(e.getX(), e
									.getY())) {
								dstAttIdx = i;
								if (srcAttIdx != dstAttIdx) {
									int help = visibleAttIdx[srcAttIdx];
									visibleAttIdx[srcAttIdx] = visibleAttIdx[dstAttIdx];
									visibleAttIdx[dstAttIdx] = help;
									updatePlotLayer(false, false, false);
								} else {
									repaint();
								}
								break;
							}
						}
						if (dstAttIdx < 0) {
							repaint();
						}
					}
					leftBtnPressedOnAttLbl = false;
					attDraggingIcon.setRect(0, 0, 0, 0);
				} else {
					if (draggingPath.getCurrentPoint() != null) {
						// User finishes dragging a selection box
						draggingPath.closePath();
						if (draggingLeftBtn) {
							draggingLeftBtn = false;
							updatePlotLayer(false, false, true);
						}
					}
				}
				if (leftBtnPressed) {
					leftBtnPressed = false;
				}
			}
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			// Right click, bring up the popup menu
			if (panModeOn) {
				setCursor(defaultCursor);
				panModeOn = false;
			} else {
				if (e.isPopupTrigger()) {
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
	}

	/**
	 * Implements the MouseMotionListener interface.
	 * 
	 * @param e
	 *            a mouse event
	 */
	public void mouseDragged(MouseEvent e) {
		if (!panModeOn) {
			if (leftBtnPressed) {
				// Dragging with left button down
				if (leftBtnPressedOnAttLbl) {
					// Dragging an attribute label
					attDraggingIcon.setRect(e.getX()
							- attDraggingIcon.getWidth() / 2.0, e.getY()
							- attDraggingIcon.getHeight() / 2.0,
							attDraggingIcon.getWidth(), attDraggingIcon
									.getHeight());
				} else {
					// Dragging a selection box
					if (draggingPath.getCurrentPoint() == null) {
						draggingFirstX = e.getX();
						draggingFirstY = e.getY();
						draggingPath.moveTo(draggingFirstX, draggingFirstY);
					} else {
						draggingLastX = e.getX();
						draggingLastY = e.getY();
						draggingPath.lineTo(draggingLastX, draggingLastY);
					}
				}
				if (!draggingLeftBtn) {
					draggingLeftBtn = true;
				}
				repaint();
			}
		}
	}

	/**
	 * Implements the MouseMotionListener interface.
	 * 
	 * @param e
	 *            a mouse event
	 */
	public void mouseMoved(MouseEvent e) {
		currX = e.getX();
		currY = e.getY();
		boolean showHandCursor = false;
		if (!panModeOn) {
			if (visibleAttLblBnds != null && visibleAttLblBnds.length > 0) {
				for (Rectangle2D element : visibleAttLblBnds) {
					if (element != null && element.contains(currX, currY)) {
						showHandCursor = true;
						break;
					}
				}
			}
			if (showHandCursor) {
				setCursor(handCursor);
			} else {
				setCursor(defaultCursor);
			}
		}
		/*
		 * else { if (radvizBox.getCurrentPoint() != null &&
		 * radvizBox.contains(e.getPoint().getX(), e.getPoint().getY())) {
		 * showHandCursor = true; } if (showHandCursor) {
		 * this.setCursor(panCursor); } else { this.setCursor(noPanCursor); } }
		 */
		if (indicationOn) {
			// Indicates the data point pointed to by the mouse
			updatePlotLayer(false, true, false);
		}
		if (showClassLegend) {
			if (legendBox.contains(currX, currY)) {
				drawLegendTip = true;
				repaint();
			} else {
				if (drawLegendTip) {
					drawLegendTip = false;
					repaint();
				}
			}
		}
	}

	/**
	 * Implements the ActionListener interface.
	 * 
	 * @param e
	 *            an ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			// The ActionListener part of the timer controling the animation
			switch (animationType) {
			case ANIMATION_ND: // Animating the effects of changing
				// multiple weights
				++currFrameId;
				for (int i = 0; i < attWeights.length; i++) {
					attWeights[i] = endWts[i]
							- ((numFrames - currFrameId) * diff[i] / numFrames);
				}
				makePlotLayerImg();
				repaint();
				if (currFrameId == numFrames) {
					timer.stop();
					animationType = -1;
				}
				break;
			case ANIMATION_GT2D: // 2-D grand tour animation
				++currFrameId;
				if (currFrameId == numFrames) {
					++gtCurrAtt;
					if (gtCurrAtt == (numFrames - 1)) {
						timer.stop();
						animationType = -1;
						setPopupItems(true);
						gtAnimationItem.setText(GT_ANIMATION_CMD);
					} else {
						currFrameId = gtCurrAtt + 1;
						visibleAttIdx[0] = gtCurrAtt;
						visibleAttIdx[1] = currFrameId;
						makePlotLayerImg();
						repaint();
					}
				} else {
					visibleAttIdx[0] = gtCurrAtt;
					visibleAttIdx[1] = currFrameId;
					makePlotLayerImg();
					repaint();
				}
				break;
			case ANIMATION_GTND: // Multi-D grand tour animation
				++currFrameId;
				if (gtCurrRound == 1) {
					attWeights[visibleAttIdx[gtCurrAtt]] = 0.0
							+ (double) (numFrames - currFrameId)
							/ (numFrames - 1);
					makePlotLayerImg();
					repaint();
					if (currFrameId == numFrames) {
						++gtCurrRound;
						currFrameId = 0;
					}
				} else if (gtCurrRound == 2) {
					attWeights[visibleAttIdx[gtCurrAtt]] = 1.0
							- (double) (numFrames - currFrameId)
							/ (numFrames - 1);
					makePlotLayerImg();
					repaint();
					if (currFrameId == numFrames) {
						++gtCurrAtt;
						if (gtCurrAtt == visibleAttIdx.length) {
							timer.stop();
							animationType = -1;
							setPopupItems(true);
							gtAnimationItem.setText(GT_ANIMATION_CMD);
						} else {
							gtCurrRound = 1;
							currFrameId = 0;
						}
					}
				}
				break;
			default:
				break;
			}
		} else if (e.getActionCommand().equalsIgnoreCase(CLR_SETTINGS_CMD)) {
			// User selected the CLR_SETTINGS_CMD from the popup menu
			if (colorSettingsDialog == null) {
				if (dummyFrame == null) {
					dummyFrame = new JFrame();
				}
				colorSettingsDialog = new JDialog(dummyFrame, CLR_SETTINGS_CMD,
						true);
				colorSettingsDialog.setLocation(300, 300);
				colorSettingsDialog.setSize(570, 270);
				colorSettingsDialog.getContentPane().setLayout(
						new BorderLayout());
				if (colorSettingsPanel == null) {
					colorSettingsPanel = new RadvizColorSettingsGUI();
					colorSettingsPanel.setBackgroundColor(getBackgroundColor());
					colorSettingsPanel.setSelectionColor(getSelectionColor());
					colorSettingsPanel.setClassificationColors(
							getColorScheme(), getNumClass());
					colorSettingsPanel.setDataLabel(pointSize, alpha);
					colorSettingsPanel.addActionListener(this);
				}
				colorSettingsDialog.getContentPane().add(colorSettingsPanel,
						BorderLayout.CENTER);
			}
			colorSettingsPanel.setBackgroundColor(getBackgroundColor());
			colorSettingsPanel.setSelectionColor(getSelectionColor());
			colorSettingsPanel.setClassificationColors(getColorScheme(),
					getNumClass());
			colorSettingsPanel.setDataLabel(pointSize, alpha);
			colorSettingsDialog.setVisible(true);
		} else if (e.getActionCommand().equalsIgnoreCase(SET_VISIBLE_ATT_CMD)) {
			// User selected the SET_VISIBLE_ATT_CMD from the popup menu
			if (attSelDialog == null) {
				if (dummyFrame == null) {
					dummyFrame = new JFrame();
				}
				attSelDialog = new JDialog(dummyFrame, SET_VISIBLE_ATT_CMD,
						true);
				attSelDialog.setLocation(300, 300);
				attSelDialog.setSize(325, 400);
				attSelDialog.getContentPane().setLayout(new BorderLayout());
				if (attSelPanel == null) {
					attSelPanel = new AttSelectPanel(true);
					attSelPanel.addActionListener(this);
				}
				attSelDialog.getContentPane().add(attSelPanel,
						BorderLayout.CENTER);
			}
			String[] attNames = getAttNamesNumeric();
			attSelPanel.setAttList(attNames, getVisibleAttIdx());
			attSelDialog.setVisible(true);
		} else if (e.getActionCommand().equalsIgnoreCase(ANIMATION_CMD)) {
			// User selected the ANIMATION_CMD from the popup menu
			attWeightsBkup = attWeights.clone();
			String[] attNames = getAttNamesNumeric();
			if (animationDialog == null) {
				if (dummyFrame == null) {
					dummyFrame = new JFrame();
				}
				animationDialog = new JDialog(dummyFrame, ANIMATION_CMD, true);
				animationDialog.setLocation(300, 300);
				animationDialog.setSize(400, 430);
				animationDialog.getContentPane().setLayout(new BorderLayout());
				if (animationPanel == null) {
					animationPanel = new AttWeightsAnimationPanel(attNames,
							attWeights);
					animationPanel.addActionListener(this);
				}
				animationDialog.getContentPane().add(animationPanel,
						BorderLayout.CENTER);
			} else {
				animationPanel.setAttributeNames(attNames);
				animationPanel.setStartWeights(attWeights);
				animationPanel.setEndWeights(attWeights);
			}
			animationDialog.setVisible(true);
		} else if (e.getActionCommand().equalsIgnoreCase(SET_CLR_ATT_CMD)) {
			// User selected the SET_CLR_ATT_CMD from the popup menu
			String[] all = datasetForApp.getAttributeNamesOriginal();
			int[] types = datasetForApp.getDataTypeArray();
			String[] help = new String[all.length];
			int count = 0;
			for (int i = 0; i < all.length; i++) {
				logger.finest(all[i] + " = " + types[i]);
				if (types[i] != DataSetForApps.TYPE_DOUBLE) {
					help[count] = all[i];
					++count;
				}
			}
			String[] selected = new String[count];
			System.arraycopy(help, 0, selected, 0, count);
			if (dummyFrame == null) {
				dummyFrame = new JFrame();
			}
			String s = (String) JOptionPane.showInputDialog(dummyFrame,
					"Attribute", SET_CLR_ATT_CMD, JOptionPane.PLAIN_MESSAGE,
					null, selected, colorCodeAtt);
			if (s != null) {
				if (getColorCodeAtt() == null
						|| getColorCodeAtt().compareToIgnoreCase(s) != 0) {
					setColorCodeAtt(s);
					updatePlotLayer(true, false, false);
				}
			}
		} else if (e.getActionCommand().equalsIgnoreCase(GT_ANIMATION_CMD)) {
			// User selected the GT_ANIMATION_CMD from the popup menu
			if (gtDialog == null) {
				if (dummyFrame == null) {
					dummyFrame = new JFrame();
				}
				gtDialog = new JDialog(dummyFrame, GT_ANIMATION_CMD, true);
				gtDialog.setLocation(300, 300);
				gtDialog.setSize(360, 200);
				gtDialog.getContentPane().setLayout(new BorderLayout());
				if (gtPanel == null) {
					gtPanel = new GrandTourPanel();
					gtPanel.addActionListener(this);
				}
				gtDialog.getContentPane().add(gtPanel, BorderLayout.CENTER);
			}
			gtDialog.setVisible(true);
		} else if (e.getActionCommand().equalsIgnoreCase(NO_ZOOM_CMD)) {
			zoomScale = 1.0;
			updatePlotLayer(false, false, false);
			noZoomItem.setVisible(false);
		} else if (e.getActionCommand().equalsIgnoreCase(RECENTER_CMD)) {
			offsetX = offsetY = 0.0;
			recenterItem.setVisible(false);
			updatePlotLayer(false, false, false);
		} else if (e.getActionCommand().equalsIgnoreCase(PAN_CMD)) {
			if (!panModeOn) {
				panModeOn = true;
			}
			setCursor(handCursor);
		} else if (e.getActionCommand().equalsIgnoreCase("AttSelectPanel.OK")) {
			// User accepted the change of the selection of visible attributes
			AttSelectPanel attPanel = (AttSelectPanel) e.getSource();
			setVisibleAttIdx(attPanel.getSelection());
			resetAttLabelCoordinates();
			attSelDialog.setVisible(false);
			fireSubspaceChanged(getVisibleAttIdx());
			updatePlotLayer(false, false, false);
		} else if (e.getActionCommand().equalsIgnoreCase(
				"AttSelectPanel.CANCEL")) {
			// User discarded the change of the selection of visible attributes
			attSelDialog.setVisible(false);
		} else if (e.getActionCommand()
				.equalsIgnoreCase("AttSetWeightPanel.OK")) {
			// User accepted the attribute weight change
			if (lastSetWtAttIdx >= 0) {
				attWeights[lastSetWtAttIdx] = attSetWtPanel.getWeight();
			}
			attSetWtDialog.setVisible(false);
			if (attWeights[lastSetWtAttIdx] != lastSetWtOldWt) {
				updatePlotLayer(false, false, false);
			}
		} else if (e.getActionCommand().equalsIgnoreCase(
				"AttSetWeightPanel.CANCEL")) {
			// User discarded the attribute weight change
			attSetWtDialog.setVisible(false);
			if (attWeights[lastSetWtAttIdx] != lastSetWtOldWt) {
				attWeights[lastSetWtAttIdx] = lastSetWtOldWt;
				updatePlotLayer(false, false, false);
			}
		} else if (e.getActionCommand().equalsIgnoreCase(
				"AttSetWeightPanel.WeightIsChanging")) {
			// User is constantly changing the weight of the selected attribute
			// to
			// animate the effects of changing it
			if (lastSetWtAttIdx >= 0) {
				attWeights[lastSetWtAttIdx] = attSetWtPanel.getWeight();
				makePlotLayerImg();
				repaint();
			}
		} else if (e.getActionCommand().equalsIgnoreCase(
				"AttWeightsAnimationPanel.OK")) {
			// User chose to replace current attribute weights with the set of
			// end
			// attribute weights as defined in the dialog
			animationDialog.setVisible(false);
			double[] newWeights = animationPanel.getEndWeights().clone();
			boolean nochange = true;
			for (int i = 0; i < newWeights.length; i++) {
				if (newWeights[i] != attWeights[i]) {
					nochange = false;
					break;
				}
			}
			attWeights = newWeights;
			if (!nochange) {
				makePlotLayerImg();
			}
			repaint();
		} else if (e.getActionCommand().equalsIgnoreCase(
				"AttWeightsAnimationPanel.CANCEL")) {
			// User chose to leave the dialog without doing anything
			animationDialog.setVisible(false);
			boolean nochange = true;
			for (int i = 0; i < attWeights.length; i++) {
				if (attWeights[i] != attWeightsBkup[i]) {
					nochange = false;
					break;
				}
			}
			if (!nochange) {
				attWeights = attWeightsBkup.clone();
				makePlotLayerImg();
			}
			repaint();
		} else if (e.getActionCommand().equalsIgnoreCase(
				"AttWeightsAnimationPanel.ANIMATE")) {
			// User chose to animate changes between 2 sets of attribute weights
			int fps = animationPanel.getFPS();
			int numSeconds = animationPanel.getNumberOfSeconds();
			numFrames = fps * numSeconds;
			if (numFrames > 0) {
				double[] startWts = animationPanel.getStartWeights();
				endWts = animationPanel.getEndWeights();
				diff = new double[startWts.length];
				boolean nodiff = true;
				for (int i = 0; i < diff.length; i++) {
					diff[i] = endWts[i] - startWts[i];
					if (diff[i] != 0.0) {
						if (nodiff) {
							nodiff = false;
						}
					}
				}
				if (!nodiff) {
					attWeights = startWts.clone();
					makePlotLayerImg();
					repaint();
					int delay = (1000 * numSeconds) / numFrames;
					currFrameId = 0;
					animationType = ANIMATION_ND;
					timer.setDelay(delay);
					timer.setRepeats(true);
					timer.start();
				}
			}
		} else if (e.getActionCommand().equalsIgnoreCase(
				"AttWeightsAnimationPanel.STOP")) {
			if (timer.isRunning()) {
				timer.stop();
			}
			animationType = -1;
			animationPanel.setStartWeights(attWeights);
		} else if (e.getActionCommand().equalsIgnoreCase("GrandTourPanel.OK")) {
			// User chose grand tour animation
			gtDialog.setVisible(false);
			setPopupItems(false);
			gtAnimationItem.setText("Stop");
			gtAnimationItem.setEnabled(true);

			int fps = gtPanel.getFPS();
			int numSeconds = gtPanel.getNumberOfSeconds();
			int delay;

			if (visibleAttIdx.length == 2) {
				// 2-dimensional grand tour
				animationType = RadViz.ANIMATION_GT2D;
				numFrames = datasetForApp.getNumberNumericAttributes();
				delay = 1000 * numSeconds / numFrames;
				gtCurrAtt = 0;
				currFrameId = 0;
				timer.setDelay(delay);
				timer.setRepeats(true);
				timer.start();
			} else if (visibleAttIdx.length > 2) {
				// multidimensional grand tour
				animationType = ANIMATION_GTND;
				numSeconds = Math.round((float) numSeconds
						/ (visibleAttIdx.length * 2));
				numFrames = fps * numSeconds;
				delay = (1000 * numSeconds) / numFrames;
				gtCurrAtt = 0;
				gtCurrRound = 1;
				currFrameId = 0;
				for (int i = 0; i < visibleAttIdx.length; i++) {
					attWeights[visibleAttIdx[i]] = 1.0;
				}
				timer.setDelay(delay);
				timer.setRepeats(true);
				timer.start();
			}
		} else if (e.getActionCommand().equalsIgnoreCase(
				"GrandTourPanel.CANCEL")) {
			// User gave up the grand tour animation
			gtDialog.setVisible(false);
		} else if (e.getActionCommand().equalsIgnoreCase("Stop")) {
			// User stopped the grand tour animation
			setPopupItems(true);
			gtAnimationItem.setText(GT_ANIMATION_CMD);
			if (timer.isRunning()) {
				timer.stop();
			}
			animationType = -1;
		} else if (e.getActionCommand().equalsIgnoreCase(
				"RadvizColorSettingsGUI.OK")) {
			// User accepted the color settings changes
			RadvizColorSettingsGUI csPanel = (RadvizColorSettingsGUI) e
					.getSource();
			boolean updatePlot = false;
			boolean updateSC = false;
			if (csPanel.getBackgroundColor() != getBackgroundColor()) {
				if (!updatePlot) {
					updatePlot = true;
				}
				setBackgroundColor(csPanel.getBackgroundColor());
			}
			if (csPanel.getSelectionColor() != getSelectionColor()) {
				if (!updateSC) {
					updateSC = true;
				}
				setSelectionColor(csPanel.getSelectionColor());
			}
			if (!csPanel.getColorScheme().equalsIgnoreCase(getColorScheme())) {
				if (!updatePlot) {
					updatePlot = true;
				}
				setColorScheme(csPanel.getColorScheme());
				setColors(csPanel.getClassificationColors());
			}
			if (csPanel.getAlpha() != alpha
					|| csPanel.getPointSize() != pointSize) {
				if (!updatePlot) {
					updatePlot = true;
				}
				if (csPanel.getAlpha() != alpha) {
					alpha = csPanel.getAlpha();
					setColors(csPanel.getClassificationColors());
				}
				pointSize = csPanel.getPointSize();
			}
			colorSettingsDialog.setVisible(false);
			if (updatePlot) {
				updatePlotLayer(false, false, false);
			}
			if (updateSC && getSelection() != null && getSelection().length > 0) {
				repaint();
			}
		} else if (e.getActionCommand().equalsIgnoreCase(
				"RadvizColorSettingsGUI.CANCEL")) {
			// User discarded color settings changes
			colorSettingsDialog.setVisible(false);
		}

	}

	/**
	 * Implements the ItemListener interface.
	 * 
	 * @param e
	 *            an ItemEvent
	 */
	public void itemStateChanged(ItemEvent e) {
		ItemSelectable source = e.getItemSelectable();
		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (source == indicationOnItem) {
				indicationOn = true;
			} else if (source == showLegendItem) {
				if (!showClassLegend) {
					showClassLegend = true;
					repaint();
				}
			} else {
				noZoomItem.setVisible(true);
				String zoomScaleText = source.getSelectedObjects()[0]
						.toString();
				int help = zoomScaleText.indexOf(":");
				if (zoomScaleText.startsWith("1:")) {
					zoomScaleText = zoomScaleText.substring(help + 1);
					logger.finest(zoomScaleText);
					zoomScale = 1.0 / Double.parseDouble(zoomScaleText);
				} else {
					zoomScaleText = zoomScaleText.substring(0, help);
					logger.finest(zoomScaleText);
					zoomScale = Double.parseDouble(zoomScaleText);
				}
				updatePlotLayer(false, false, false);
			}
		} else {
			if (source == indicationOnItem) {
				indicationOn = false;
				if (getIndicationId() >= 0) {
					repaint();
				}
			} else if (source == showLegendItem) {
				if (showClassLegend) {
					showClassLegend = false;
					repaint();
				}
			}
		}
	}

	/**
	 * Implements the ClassificationResultListener interface.
	 * 
	 * @param e
	 *            a ClassificationResultEvent
	 */
	public void classificationResultChanged(ClassificationResultEvent e) {
	}

	/**
	 * Implements the DataSetListener interface.
	 * 
	 * @param e
	 *            a DataSetEvent
	 */
	public void dataSetChanged(DataSetEvent e) {
		setDataSet(e.getDataSetForApps());
	}

	/**
	 * Implements the IndicationListener interface.
	 * 
	 * @param e
	 *            an IndicationEvent
	 */
	public void indicationChanged(IndicationEvent e) {
		if (this == e.getSource()) {
			return;
		}
		setIndicationId(e.getIndication());
		setIndicationToolTip(e.getIndication());
		repaint();
	}

	/**
	 * Implements the SelectionListener interface.
	 * 
	 * @param e
	 *            a SelectionEvent
	 */
	public void selectionChanged(SelectionEvent e) {
		setSelection(e.getSelection());
		repaint();
	}

	/**
	 * Implements the SubspaceListener interface.
	 * 
	 * @param e
	 *            a SubspaceEvent
	 */
	public void subspaceChanged(SubspaceEvent e) {
		setVisibleAttIdx(e.getSubspace());
		resetAttLabelCoordinates();
		makePlotLayerImg();
		repaint();
	}

	/**
	 * Creates and shows the RadViz GUI for testing.
	 */
	protected static void createAndShowGUI() {
		RadViz rv = new RadViz();
		JFrame f = new JFrame("Test");
		f.getContentPane().add(rv);
		f.setSize(510, 510);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
		rv.setAbsolutePath("e:\\temp\\USA_Cancer_Ob.csv");
	}

	/**
	 * Tests the RadViz GUI.
	 * 
	 * @param args
	 *            arguments passed to the main() function
	 */
	public static void main(String args[]) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	public void tableChanged(TableModelEvent e) {
		setDataSet(datasetForApp);

	}

}