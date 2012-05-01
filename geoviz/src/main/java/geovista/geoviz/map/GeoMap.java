/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.map;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.colorbrewer.ColorBrewer;
import geovista.common.classification.ClassifierPicker;
import geovista.common.data.DataSetForApps;
import geovista.common.event.AuxiliaryDataSetEvent;
import geovista.common.event.AuxiliaryDataSetListener;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.ColorArrayListener;
import geovista.common.event.ConditioningEvent;
import geovista.common.event.ConditioningListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SpatialExtentEvent;
import geovista.common.event.SpatialExtentListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.event.VariableSelectionEvent;
import geovista.common.event.VariableSelectionListener;
import geovista.common.ui.Fisheyes;
import geovista.common.ui.MultiSlider;
import geovista.common.ui.RangeSlider;
import geovista.common.ui.ShapeReporter;
import geovista.common.ui.VisualSettingsPopupListener;
import geovista.coordination.CoordinationManager;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.readers.shapefile.ShapeFileToShape;
import geovista.symbolization.BivariateColorClassifierSimple;
import geovista.symbolization.BivariateColorSchemeVisualizer;
import geovista.symbolization.ColorClassifier;
import geovista.symbolization.ColorRampPicker;
import geovista.symbolization.event.ColorClassifierEvent;
import geovista.symbolization.event.ColorClassifierListener;

/**
 * This class handles the user state, like selection, pan, zoom, plus
 * symbolization options.
 * 
 * MapCanvas does most of the work.
 */
public class GeoMap extends JPanel
	implements
	// MouseListener, MouseMotionListener,
	ActionListener, SelectionListener, IndicationListener, DataSetListener,
	AuxiliaryDataSetListener, ColorClassifierListener, ColorArrayListener,
	SpatialExtentListener, ComponentListener,
	ConditioningListener,
	TableModelListener,
	// the following line commented out until Glyph support improves
	// GlyphListener,
	VariableSelectionListener, SubspaceListener,
	VisualSettingsPopupListener, ShapeReporter {

    public static final int VARIABLE_CHOOSER_MODE_ACTIVE = 0;
    public static final int VARIABLE_CHOOSER_MODE_FIXED = 1;
    public static final int VARIABLE_CHOOSER_MODE_HIDDEN = 2;

    transient protected MapCanvas mapCan;

    transient protected VisualClassifier visClassOne;
    transient protected VisualClassifier visClassTwo;

    // transient protected JPanel legendPanel;

    transient protected JToolBar mapTools;

    transient protected JPanel topContent;

    transient protected Cursor[] customCursors;

    transient protected GeoCursors cursors;

    transient protected BivariateColorSchemeVisualizer biViz;
    // transient ScatterPlot biViz;

    // XXX scatterplot goes here
    transient protected Dimension currSize;

    transient protected Fisheyes fisheyes;

    transient protected final static Logger logger = Logger
	    .getLogger(GeoMap.class.getName());

    transient DataSetForApps dataSet;

    Color backgroundColor = Color.green;

    Preferences prefs;

    // Added by Diansheng. The reason for this is that for some applications we
    // need
    // to assign colors using other componenents instead of the visual
    // classifier.
    transient private final boolean visualClassifierNeeded = false;

    public GeoMap() {
	super();
	init();
    }

    public void setUseHistogram(boolean useHistogram) {
	mapCan.use_histograms = useHistogram;
    }

    private void init() {

	JPanel vcPanel = createVCPanel();
	JPanel legendPanel = createLegendPanel(vcPanel);

	makeToolbar();
	mapTools.setAlignmentX(Component.LEFT_ALIGNMENT);

	topContent = new JPanel();
	// topContent.setLayout(new BoxLayout(topContent, BoxLayout.Y_AXIS));
	topContent.add(legendPanel);
	topContent.add(mapTools);

	// topContent.setMinimumSize(vcSize);
	// topContent.setPreferredSize(vcSize);

	// note: uncomment the line below for animation panel stuff
	// vcPanel.add(this.makeAnimationPanel());
	cursors = new GeoCursors();
	setCursor(cursors.getCursor(GeoCursors.CURSOR_ARROW_SELECT));

	// setLayout(new BorderLayout());
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	if (visualClassifierNeeded) {
	    this.add(topContent);
	}

	// this.add(vcPanel, BorderLayout.NORTH);
	mapCan = new MapCanvas();
	// mapCan.setBackground(this.backgroundColor);
	this.add(mapCan);
	mapCan.addIndicationListener(this);
	visClassOne.addColorClassifierListener(this);
	visClassTwo.addColorClassifierListener(this);
	addIndicationListener(biViz);

	visClassTwo.setPalette(ColorBrewer.BrewerNames.Greens);
	visClassTwo.setHighColor(ColorRampPicker.DEFAULT_HIGH_COLOR_GREEN); // green
	currSize = new Dimension(this.getSize());
	fisheyes = new Fisheyes();
	fisheyes.setLensType(Fisheyes.LENS_HEMISPHERE);
	visClassOne.getClassPick().setNClasses(4);
	visClassTwo.getClassPick().setNClasses(4);
	// colorClassifierChanged(new
	// ColorClassifierEvent(visClassTwo,visClassTwo.getColorClasser()));
    }

    private JPanel createLegendPanel(JPanel vcPanel) {
	JPanel legendPanel = new JPanel();
	legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.X_AXIS));
	legendPanel.add(vcPanel);

	legendPanel.add(Box.createRigidArea(new Dimension(4, 2)));
	// biViz = new ScatterPlot();
	biViz = new BivariateColorSchemeVisualizer();
	// biViz.setPreferredSize(new Dimension(50, 50));
	// biViz.setAxisOn(false);
	legendPanel.add(biViz);
	legendPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	return legendPanel;
    }

    private JPanel createVCPanel() {
	JPanel vcPanel = new JPanel();
	vcPanel.setLayout(new BoxLayout(vcPanel, BoxLayout.Y_AXIS));
	// vcPanel.setLayout(new BorderLayout());
	visClassOne = new VisualClassifier();
	visClassTwo = new VisualClassifier();

	// visClassOne.setAlignmentX(Component.LEFT_ALIGNMENT);
	// visClassTwo.setAlignmentX(Component.LEFT_ALIGNMENT);

	Dimension vcSize = new Dimension(800, 800);

	// vcPanel.setMinimumSize(vcSize);
	// vcPanel.setMaximumSize(vcSize);
	// vcPanel.setPreferredSize(vcSize);

	// visClassOne.setMinimumSize(vcSize);
	// visClassTwo.setMinimumSize(vcSize);

	// visClassOne.setPreferredSize(vcSize);
	// visClassTwo.setPreferredSize(vcSize);
	//
	// visClassOne.setSize(vcSize);
	// visClassTwo.setSize(vcSize);

	visClassOne
		.setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
	visClassTwo
		.setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
	visClassOne.addActionListener(this);
	visClassOne.setOrientationInParentIsX(true);
	visClassTwo.addActionListener(this);
	visClassTwo.setOrientationInParentIsX(false);

	vcPanel.add(visClassTwo);
	vcPanel.add(visClassOne);

	return vcPanel;
    }

    @SuppressWarnings("unused")
    private JPanel makeAnimationPanel() {
	JPanel animationPanel = new JPanel();
	Dimension prefSize = new Dimension(100, 50);
	animationPanel.setSize(prefSize);
	animationPanel.setPreferredSize(prefSize);

	// animationPanel.setBorder(BorderFactory.createLineBorder(Color.blue));
	JLabel timeLabel = new JLabel("Time Step:");
	animationPanel.add(timeLabel);

	JSlider timeSlider = new JSlider(0, 10, 3);
	timeSlider.setPaintLabels(true);
	timeSlider.setPaintTicks(true);
	timeSlider.setPaintTrack(true);
	timeSlider.setMajorTickSpacing(1);
	timeSlider.createStandardLabels(2);

	animationPanel.add(timeSlider);

	return animationPanel;
    }

    private void makeToolbar() {
	mapTools = new JToolBar();

	// Dimension prefSize = new Dimension(100,10);
	// mapTools.setMinimumSize(prefSize);
	// mapTools.setPreferredSize(prefSize);
	JButton button = null;
	Class cl = GeoMap.class;
	URL urlGif = null;
	Dimension buttDim = new Dimension(24, 24);

	// first button
	try {
	    urlGif = cl.getResource("resources/select24.gif");

	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	button = new JButton(new ImageIcon(urlGif));
	button.setPreferredSize(buttDim);
	button.setToolTipText("Enter selection mode");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		GeoMap.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_SELECT));
		mapCan.setMode(MapCanvas.MapMode.Select);
	    }
	});
	mapTools.add(button);

	mapTools.addSeparator();

	// second button
	try {
	    urlGif = cl.getResource("resources/ZoomIn24.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Enter zoom in mode");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		GeoMap.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_ZOOM_IN));
		mapCan.setMode(MapCanvas.MapMode.ZoomIn);

		// GeoMap.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    }
	});
	mapTools.add(button);

	// third button
	try {
	    urlGif = cl.getResource("resources/ZoomOut24.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Enter zoom out mode");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		mapCan.setMode(MapCanvas.MapMode.ZoomOut);
		GeoMap.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_ZOOM_OUT));
	    }
	});
	mapTools.add(button);

	// fourth button
	try {
	    urlGif = cl.getResource("resources/Home24.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Zoom to full extent");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		mapCan.zoomFullExtent();
	    }
	});
	mapTools.add(button);

	// fifth button
	try {
	    urlGif = cl.getResource("resources/pan24.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Enter pan mode");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		GeoMap.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		mapCan.setMode(MapCanvas.MapMode.Pan);
	    }
	});
	mapTools.add(button);
	// sixth button
	try {
	    urlGif = cl.getResource("resources/excentric24.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Excentric Labels");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		GeoMap.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		mapCan.setMode(MapCanvas.MapMode.Excentric);
	    }
	});
	mapTools.add(button);

	// seventh button
	try {
	    urlGif = cl.getResource("resources/fisheye24.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Fisheye Lens");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		GeoMap.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		mapCan.setMode(MapCanvas.MapMode.Fisheye);
	    }
	});
	mapTools.add(button);
	// eighth button
	try {
	    urlGif = cl.getResource("resources/magnifying24.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Magnifiying Lens");
	button.addActionListener(new ActionListener() {

	    public void actionPerformed(ActionEvent e) {
		GeoMap.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		mapCan.setMode(MapCanvas.MapMode.Magnifying);
	    }
	});

	mapTools.add(button);

    }

    // jin: called by visualClassifier when change classification
    public void actionPerformed(ActionEvent e) {
	Object src = e.getSource();
	String command = e.getActionCommand();
	String varChangedCommand = ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED;

	if ((src == visClassOne) && command.equals(varChangedCommand)) {
	    int index = visClassOne.getCurrVariableIndex();

	    if (index >= 0) { // fix bug by jin,there could be a chance when
		// index=-1
		mapCan.setCurrColorColumnX(index);
	    } else {
		mapCan.setCurrColorColumnX(0);
	    }
	} else if ((src == visClassTwo) && command.equals(varChangedCommand)) {
	    int index = visClassTwo.getCurrVariableIndex();

	    if (index >= 0) { // fix bug by jin, there could be a chance when
		// index=-1, it =>
		// DataSetForApps.getNumericDataAsDouble()
		// throws exceptions
		mapCan.setCurrColorColumnY(index);
	    } else {
		mapCan.setCurrColorColumnY(0);
	    }
	}

    }

    public void selectionChanged(SelectionEvent e) {
	mapCan.selectionChanged(e);
	// biViz.setSelections(e.getSelection());
    }

    public void conditioningChanged(ConditioningEvent e) {
	mapCan.setConditionArray(e.getConditioning());
    }

    public void indicationChanged(IndicationEvent e) {
	Object source = e.getSource();

	if ((source == mapCan) && (e.getIndication() >= 0)) {
	    // jin: for debug purpose
	    int xClass = e.getXClass();
	    int yClass = e.getYClass();
	    visClassOne.setIndicatedClass(xClass);
	    visClassTwo.setIndicatedClass(yClass);
	    biViz.indicationChanged(e);

	} else if ((source == mapCan) && (e.getIndication() < 0)) {
	    visClassOne.setIndicatedClass(-1);
	    visClassTwo.setIndicatedClass(-1);
	    biViz.indicationChanged(new IndicationEvent(this, -1));

	    // this.fireIndicationChanged(e.getIndication());
	} else {
	    mapCan.indicationChanged(e);
	    biViz.indicationChanged(e);
	}
    }

    public void spatialExtentChanged(SpatialExtentEvent e) {
	mapCan.spatialExtentChanged(e);

	savedEvent = e;
    }

    SpatialExtentEvent savedEvent;

    public SpatialExtentEvent getSpatialExtentEvent() {
	return savedEvent;
    }

    public void dataSetChanged(DataSetEvent e) {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.finest("GeoMap, got a data set, id = "
		    + e.getDataSetForApps().hashCode());
	    logger.info(e.getDataSetForApps().getDataSourceName());
	}

	setDataSet(e.getDataSetForApps());
    }

    public void dataSetAdded(AuxiliaryDataSetEvent e) {
	if (e.getDataSetForApps().getShapeData() == null) {
	    return;
	}
	setAuxiliarySpatialData(e.getDataSetForApps());
    }

    public void setAuxiliarySpatialData(DataSetForApps dataSet) {
	mapCan.setAuxiliarySpatialData(dataSet);

    }

    /**
	 *
	 */
    public void setTextures(TexturePaint[] textures) {
	mapCan.setTextures(textures);
    }

    public void variableSelectionChanged(VariableSelectionEvent e) {
	visClassOne.setCurrVariableIndex(e.getVariableIndex() + 1);
	mapCan.setCurrColorColumnX(e.getVariableIndex() + 1);
    }

    public void colorArrayChanged(ColorArrayEvent e) {
	if (mapCan == null || e.getColors() == null) {
	    return;
	}
	mapCan.setObservationColors(e.getColors());
    }

    public void colorClassifierChanged(ColorClassifierEvent e) {
	if (e.getSource() == visClassOne) {
	    e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_X);
	}

	if (e.getSource() == visClassTwo) {
	    e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_Y);
	}

	biViz.colorClassifierChanged(e);

	if (mapCan == null) {
	    return;
	}

	ColorClassifier colorSymbolizerX = visClassOne.getColorClasser();
	ColorClassifier colorSymbolizerY = visClassTwo.getColorClasser();

	BivariateColorClassifierSimple biColorSymbolizer = new BivariateColorClassifierSimple();
	// turn these around to match what happens in the
	// scatterplot
	biColorSymbolizer.setClasserX(colorSymbolizerX.getClasser());
	biColorSymbolizer.setColorerX(colorSymbolizerX.getColorer());

	biColorSymbolizer.setClasserY(colorSymbolizerY.getClasser());
	biColorSymbolizer.setColorerY(colorSymbolizerY.getColorer());
	mapCan.setBivarColorClasser(biColorSymbolizer);
    }

    public int[] getSelectedObservations() {
	return mapCan.getSelectedObservationsInt();
    }

    public void setDataSet(DataSetForApps dataSet) {
	if (dataSet == null) {
	    logger.warning("null data sent to map");
	    return;
	}

	this.dataSet = dataSet;
	mapCan.setDataSet(dataSet);
	visClassOne.setDataSet(dataSet);
	visClassTwo.setDataSet(dataSet);

	// set default data to get color from
	int numNumeric = dataSet.getNumberNumericAttributes();

	// int currColorColumnX = mapCan.getCurrColorColumnX();
	// int currColorColumnY = mapCan.getCurrColorColumnY();
	//
	// if (currColorColumnX < 0) {
	// if (numNumeric > 0) {
	// int index = visClassOne.getCurrVariableIndex();
	// // this.setXVariable(1);
	// // this.mapCan.setCurrColorColumnX(1); // jin: set the initial
	// // variable index of visualclassifier
	// setXVariable(index);
	// mapCan.setCurrColorColumnX(index);
	// }
	// } else if (currColorColumnX < numNumeric) {
	// setXVariable(currColorColumnX);
	// mapCan.setCurrColorColumnX(currColorColumnX);
	// }
	//
	// if (currColorColumnY < 0) {
	// if (numNumeric > 1) {
	//
	// int index = visClassTwo.getCurrVariableIndex();
	// // this.setYVariable(2);
	// // this.mapCan.setCurrColorColumnY(2); // jin: set the initial
	// // variable index of visualclassifier
	// setYVariable(index);
	// mapCan.setCurrColorColumnY(index);
	// } else if (numNumeric == 1) {
	// setYVariable(1);
	// mapCan.setCurrColorColumnY(1);
	// }
	// } else if (currColorColumnY < numNumeric) {
	// setYVariable(currColorColumnY);
	// mapCan.setCurrColorColumnY(currColorColumnY);
	// }

	// Jin: fix the bug the initially the variable names in comboxes not
	// mataching the variable names in the indication

	if (numNumeric > 2) { // frank: fix the bug of the map crashing if
	    // there are not enough variables
	    visClassOne.setCurrVariableIndex(0); // Jin
	    visClassTwo.setCurrVariableIndex(1);
	    // frank here again... lets try something else....
	    ActionEvent e = new ActionEvent(this, 0,
		    ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED);
	    visClassOne.actionPerformed(e);
	    visClassTwo.actionPerformed(e);
	}
    }

    @Override
    public void setBackground(Color bg) {
	if ((mapCan != null) && (bg != null)) {
	    mapCan.setBackground(bg);
	}
    }

    // start component listener stuff
    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
	// todo: zoom in or out a bit

    }

    public void componentShown(ComponentEvent e) {
    }

    // end component listener stuff

    /**
     * adds an IndicationListener
     */
    public void addIndicationListener(IndicationListener l) {
	mapCan.addIndicationListener(l);
	// biViz.addIndicationListener(l);
    }

    /**
     * removes an IndicationListener from the component
     */
    public void removeIndicationListener(IndicationListener l) {
	mapCan.removeIndicationListener(l);
	// biViz.removeIndicationListener(l);
    }

    /**
     * adds an SelectionListener
     */
    public void addSelectionListener(SelectionListener l) {
	mapCan.addSelectionListener(l);
    }

    /**
     * removes an SelectionListener from the component
     */
    public void removeSelectionListener(SelectionListener l) {
	mapCan.removeSelectionListener(l);
    }

    /**
     * adds an SpatialExtentListener
     */
    public void addSpatialExtentListener(SpatialExtentListener l) {
	mapCan.addSpatialExtentListener(l);
    }

    /**
     * removes an SpatialExtentListener from the component
     */
    public void removeSpatialExtentListener(SpatialExtentListener l) {
	listenerList.remove(SpatialExtentListener.class, l);
	mapCan.removeSpatialExtentListener(l);
    }

    public void tableChanged(TableModelEvent e) {

	logger.finest("data changed, whooop!");
	int col = e.getColumn();
	logger.finest(col + " is the column...");
	// this.mapCan.setDataSet(dataSet);

	visClassOne.setDataSet(dataSet);
	visClassTwo.setDataSet(dataSet);

	// this.visClassOne.setUpdate(false);
	// this.visClassTwo.setUpdate(false);
	// //this.visClassOne.tableChanged(e);
	// // frank here again... lets try something else....
	// ActionEvent e2 = new ActionEvent(this, 0,
	// ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED);
	// visClassOne.actionPerformed(e2);
	// visClassTwo.actionPerformed(e2);
	// //this.visClassTwo.tableChanged(e);
	// this.visClassOne.setUpdate(true);
	// this.visClassTwo.setUpdate(true);
	//
	// so, what do we do?
	// add the new column to the picker, I suppose

    }

    public SelectionEvent getSelectionEvent() {
	return new SelectionEvent(this, getSelectedObservations());
    }

    public void subspaceChanged(SubspaceEvent e) {
	int[] vars = e.getSubspace();
	visClassOne.getClassPick().setCurrVariableIndex(vars[1]);
	visClassTwo.getClassPick().setCurrVariableIndex(vars[0]);
	mapCan.setCurrColorColumnX(vars[1]);
	mapCan.setCurrColorColumnY(vars[0]);

    }

    public Color getIndicationColor() {
	return mapCan.getIndicationColor();
    }

    public Color getSelectionColor() {
	return mapCan.getSelectionColor();
    }

    public boolean isSelectionBlur() {
	return mapCan.isSelectionBlur();
    }

    public boolean isSelectionFade() {
	return mapCan.isSelectionFade();
    }

    public void setIndicationColor(Color indColor) {
	mapCan.setIndicationColor(indColor);

    }

    public void setSelectionColor(Color selColor) {
	mapCan.setSelectionColor(selColor);

    }

    public void useMultiIndication(boolean useMultiIndic) {
	mapCan.useMultiIndication(useMultiIndic);

    }

    public void useSelectionBlur(boolean selBlur) {
	mapCan.useSelectionBlur(selBlur);

    }

    public void useSelectionFade(boolean selFade) {
	mapCan.useSelectionFade(selFade);

    }

    public Shape reportShape() {
	Shape s = mapCan.reportShape();
	return s;
    }

    public Component renderingComponent() {
	return mapCan;
    }

    public void processCustomCheckBox(boolean value, String text) {
	// TODO Auto-generated method stub

    }

    public boolean isSelectionOutline() {
	// TODO Auto-generated method stub
	return false;
    }

    public void useSelectionOutline(boolean selOutline) {
	mapCan.useSelectionOutline(selOutline);

    }

    public int getSelectionLineWidth() {
	return mapCan.getSelectionLineWidth();
    }

    public void setSelectionLineWidth(int width) {
	mapCan.setSelectionLineWidth(width);

    }

    public static void main(String[] args) {
	JFrame app = new JFrame("vcpanel, why?");
	GeoMap map = new GeoMap();
	// app.add(map.createLegendPanel(map.createVCPanel()));
	app.add(map.topContent);
	app.pack();
	app.setVisible(true);
    }

    public static void main2(String[] args) {

	boolean useProj = true;
	boolean useResource = true;
	JFrame app = new JFrame("MapBean Main Class: Why?");
	app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	MultiSlider mSlider = new MultiSlider();

	RangeSlider rSlider = new RangeSlider();
	rSlider.setVisible(true);
	mSlider.setNumberOfThumbs(5);
	mSlider.setVisible(true);
	// mSlider.setOrientation(mSlider.VERTICAL);
	app.getContentPane().add(mSlider);
	app.pack();
	app.setVisible(true);

	GeoMap map2 = new GeoMap();

	app.getContentPane().add(map2);
	app.pack();
	app.setVisible(true);

	String fileName = "C:\\arcgis\\arcexe81\\Bin\\TemplateData\\USA\\counties.shp";
	fileName = "C:\\temp\\shapefiles\\intrstat.shp";
	fileName = "C:\\data\\geovista_data\\shapefiles\\larger_cities.shp";
	fileName = "C:\\data\\geovista_data\\shapefiles\\jin\\CompanyProdLL2000Def.shp";
	fileName = "C:\\data\\geovista_data\\Historical-Demographic\\census\\census80_90_00.shp";

	ShapeFileDataReader shpRead = new ShapeFileDataReader();
	shpRead.setFileName(fileName);
	CoordinationManager coord = new CoordinationManager();
	ShapeFileToShape shpToShape = new ShapeFileToShape();
	ShapeFileProjection shpProj = new ShapeFileProjection();
	GeoDataGeneralizedStates stateData = new GeoDataGeneralizedStates();

	/*
	 * String fileName2 = "C:\\data\\grants\\nevac\\crimes\\cri.shp";
	 * ShapefileReader reader = new ShapefileReader(); DriverProperties dp =
	 * new DriverProperties(fileName2); FeatureCollection featColl = null;
	 * 
	 * try { featColl = reader.read(dp); } catch (IllegalParametersException
	 * e) { // TODO Auto-generated catch block e.printStackTrace(); } catch
	 * (Exception e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); }
	 * 
	 * List<Feature> featList = featColl.getFeatures();
	 * 
	 * for (Feature feat : featList) {
	 * 
	 * Geometry geom = (Geometry) feat.getAttribute(0);
	 * logger.info(geom.getClass().getName());
	 * 
	 * Java2DConverter converter = new Java2DConverter(new Viewport(app
	 * .getGlassPane())); try { Shape shp = converter.toShape(geom);
	 * logger.info(shp); } catch (NoninvertibleTransformException e) { //
	 * TODO Auto-generated catch block e.printStackTrace(); } }
	 */

	// coord.addBean(map2);
	coord.addBean(shpToShape);
	coord.addBean(map2);

	if (useResource) {

	    shpProj.setInputDataSetForApps(stateData.getDataForApps());
	} else {
	    if (useProj) {
		// stateData.addActionListener(shpProj);
		shpProj.setInputDataSet(shpRead.getDataSet());
	    }
	}
	Object[] dataSet = null;
	if (useProj) {
	    dataSet = shpProj.getOutputDataSet();
	} else {
	    dataSet = shpRead.getDataSet();
	}

	shpToShape.setInputDataSet(dataSet);

	map2.setBackground(Color.red);
	// shpToShape.setInputDataSet(dataSet);
	// Rectangle2D rect = new Rectangle2D.Float(-30f,-30f,600f,600f);
	// SpatialExtentEvent ext = new SpatialExtentEvent(map,rect);

	// ShapeFileProjection shpProj2 = new ShapeFileProjection();
	// OldProjection proj = shpProj.getProj();
	// shpProj2.setProj(proj);
	// shpProj2.setInputAuxiliaryData(stateData.getDataSet());

	// shpToShape2.setInputDataSet(shpProj2.getOutputAuxiliarySpatialData());
	// map2.setAuxiliarySpatialData(shpToShape2.getOutputDataSet());

	// map2.setAuxiliarySpatialData(shpToShape2.getOutputDataSet());

	// ShapeFileToShape shpToShape3 = new ShapeFileToShape();
	// fileName = "C:\\data\\geovista_data\\shapefiles\\jin\\States.shp";
	//
	// ShapeFileDataReader shpRead3 = new ShapeFileDataReader();
	// shpRead3.setFileName(fileName);
	// // shpToShape3.setInputDataSet(shpRead3.getDataSet());
	// shpToShape3.setInputDataSet(stateData.getDataSet());
	// map2.setAuxiliarySpatialData(shpToShape3.getOutputDataSetForApps());

	// map2.setDataSet(shpToShape2.getOutputDataSet());

    }

}
