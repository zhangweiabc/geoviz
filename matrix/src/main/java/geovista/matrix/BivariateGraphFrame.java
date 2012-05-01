/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.event.EventListenerList;

import geovista.common.classification.ClassifierPicker;
import geovista.common.data.DataSetForApps;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.ColorArrayListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.VariableSelectionEvent;
import geovista.common.event.VariableSelectionListener;
import geovista.common.ui.Fisheyes;
import geovista.geoviz.map.GeoCursors;
import geovista.geoviz.map.MapCanvas;
import geovista.geoviz.scatterplot.ScatterPlot;
import geovista.geoviz.scatterplot.ScatterPlotBasic;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.matrix.map.MapMatrixElement;
import geovista.symbolization.BivariateColorClassifier;
import geovista.symbolization.BivariateColorClassifierSimple;
import geovista.symbolization.BivariateColorSchemeVisualizer;
import geovista.symbolization.ColorClassifier;
import geovista.symbolization.event.ColorClassifierEvent;
import geovista.symbolization.event.ColorClassifierListener;

public class BivariateGraphFrame extends JPanel implements DataSetListener,
	ActionListener, ColorClassifierListener, SelectionListener,
	IndicationListener, VariableSelectionListener {
    protected final static Logger logger = Logger
	    .getLogger(BivariateGraphFrame.class.getName());
    public static final int VARIABLE_CHOOSER_MODE_ACTIVE = 0;
    public static final int VARIABLE_CHOOSER_MODE_FIXED = 1;
    public static final int VARIABLE_CHOOSER_MODE_HIDDEN = 2;
    transient private String elementClassName;
    transient private Class elementClass;
    transient private MatrixElement bivariateGraph;
    private final int[] displayIndices = new int[2];
    private final VisualClassifier visClassOne;
    private final VisualClassifier visClassTwo;
    transient private JToolBar mapTools;
    private final JPanel topContent;
    private final BivariateColorSchemeVisualizer biViz;
    transient private Color backgroundColor;
    transient private int[] selections;
    transient private Color[] multipleSelectionColors;
    transient private Color[] colorArrays;
    transient private Fisheyes fisheyes;
    transient private GeoCursors cursors;
    int[] savedSelection;

    public BivariateGraphFrame() {
	super();

	JPanel vcPanel = new JPanel();
	vcPanel.setLayout(new BoxLayout(vcPanel, BoxLayout.Y_AXIS));
	visClassOne = new VisualClassifier();
	visClassTwo = new VisualClassifier();
	visClassOne.setAlignmentX(Component.LEFT_ALIGNMENT);
	visClassTwo.setAlignmentX(Component.LEFT_ALIGNMENT);
	visClassOne
		.setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
	visClassTwo
		.setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
	visClassOne.addActionListener(this);
	visClassTwo.addActionListener(this);

	vcPanel.add(visClassTwo);
	vcPanel.add(visClassOne);

	JPanel legendPanel = new JPanel();
	legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.X_AXIS));
	biViz = new BivariateColorSchemeVisualizer();
	legendPanel.add(vcPanel);
	legendPanel.add(Box.createRigidArea(new Dimension(4, 2)));
	legendPanel.add(biViz);

	topContent = new JPanel();
	topContent.setLayout(new BoxLayout(topContent, BoxLayout.Y_AXIS));
	legendPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	topContent.add(legendPanel);
	// mapTools.setAlignmentX(Component.LEFT_ALIGNMENT);
	// topContent.add(mapTools);

	setLayout(new BorderLayout());
	this.add(topContent, BorderLayout.NORTH);
	// this.bivariateGraph.addActionListener(this);
	// this.add((Component)this.bivariateGraph, BorderLayout.CENTER);
	// this.scatterPlot.addIndicationListener(this);
	visClassOne.addColorClassifierListener(this);
	visClassTwo.addColorClassifierListener(this);
	addIndicationListener(biViz);

	visClassTwo.setHighColor(new Color(0, 150, 0));// Jin
	// visClassTwo.setHighColor(new Color(0, 255, 255)); //removed by jin
    }

    /**
     * Set up the name for element which will be displayed in matrix. Can be
     * defined in bean proporty.
     * 
     * @param classname
     */
    public void setElementClassName(String classname) {
	elementClassName = classname;
	try {
	    setElementClass((elementClassName != null) ? Class
		    .forName(elementClassName) : null);
	    bivariateGraph = (MatrixElement) elementClass.newInstance();
	    intialize();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Get the name for element which will be displayed in matrix.
     * 
     * @return
     */
    public String getElementClassName() {
	return elementClassName;
    }

    /**
     * Set the element which will be displayed in matrix by connecting to an
     * object.
     * 
     * @param obj
     */
    public void setElementClass(Object obj) {
	setElementClassName((obj != null) ? obj.getClass().getName() : null);
    }

    /**
     * Set up the element displayed in matrix. Called by setElementClass(Oject
     * obj).
     * 
     * @param clazz
     */
    public void setElementClass(Class clazz) {
	elementClass = clazz;
    }

    /**
     * @param data
     * 
     *            This method is deprecated becuase it wants to create its very
     *            own pet DataSetForApps. This is no longer allowed, to allow
     *            for a mutable, common data set. Use of this method may lead to
     *            unexpected program behavoir. Please use setDataSet instead.
     */
    @Deprecated
    public void setData(Object[] data) {
	setDataSet(new DataSetForApps(data));
    }

    public void setDataSet(DataSetForApps dataSet) {

	visClassOne.setDataSet(dataSet);
	visClassTwo.setDataSet(dataSet);

	// XXX this used to use the dataset numeric and spatial
	bivariateGraph.setDataSet(dataSet);

	selections = new int[dataSet.getNumObservations()];

	displayIndices[0] = 1;
	displayIndices[1] = 2;
	setXVariable(displayIndices[0]);
	setYVariable(displayIndices[1]);
	bivariateGraph.setAxisOn(true);
	bivariateGraph.setDataIndices(displayIndices);
    }

    private void intialize() {

	bivariateGraph.addActionListener(this);
	this.add((Component) bivariateGraph, BorderLayout.CENTER);
	bivariateGraph.setBackground(Color.white);
	// note the not (!) at the beginning of the next condition
	if (!bivariateGraph.getClass().getName()
		.equals("geovista.geoviz.map.MapMatrixElement")) {
	    makeToolbar();
	    mapTools.setAlignmentX(Component.LEFT_ALIGNMENT);
	    topContent.add(mapTools);
	    // note: uncomment the line below for animation panel stuff
	    // vcPanel.add(this.makeAnimationPanel());
	    cursors = new GeoCursors();
	    setCursor(cursors.getCursor(GeoCursors.CURSOR_ARROW_SELECT));
	    fisheyes = new Fisheyes();
	    fisheyes.setLensType(Fisheyes.LENS_HEMISPHERE);

	}

    }

    public void setXVariable(int var) {
	visClassOne.setCurrVariableIndex(var);
    }

    public void setYVariable(int var) {
	visClassTwo.setCurrVariableIndex(var);
    }

    public void setBivarColorClasser(BivariateColorClassifier bivarColorClasser) {
	bivariateGraph.setBivarColorClasser(bivarColorClasser, false);
    }

    public void setBackgroundColor(Color backgroundColor) {
	this.backgroundColor = backgroundColor;
    }

    public Color getBackgroundColor() {
	return backgroundColor;
    }

    public void setRegressionClass(Object obj) {
	setRegressionClassName((obj != null) ? obj.getClass().getName() : null);
    }

    public void setRegressionClassName(String classname) {
	((ScatterPlot) bivariateGraph).setRegressionClassName(classname);
    }

    public void makeToolbar() {
	mapTools = new JToolBar();

	// Dimension prefSize = new Dimension(100,10);
	// mapTools.setMinimumSize(prefSize);
	// mapTools.setPreferredSize(prefSize);
	JButton button = null;
	Class cl = this.getClass();
	URL urlGif = null;
	Dimension buttDim = new Dimension(20, 20);

	// first button
	try {
	    urlGif = cl.getResource("resources/select16.gif");
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	button = new JButton(new ImageIcon(urlGif));
	button.setPreferredSize(buttDim);
	button.setToolTipText("Enter selection mode");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		BivariateGraphFrame.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_SELECT));
		((MapMatrixElement) (bivariateGraph))
			.setMode(MapCanvas.MapMode.Select);
	    }
	});
	mapTools.add(button);

	mapTools.addSeparator();

	// second button
	try {
	    urlGif = cl.getResource("resources/ZoomIn16.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Enter zoom in mode");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		BivariateGraphFrame.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_ZOOM_IN));
		((MapMatrixElement) (bivariateGraph))
			.setMode(MapCanvas.MapMode.ZoomIn);

		// GeoMap.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
	    }
	});
	mapTools.add(button);

	// third button
	try {
	    urlGif = cl.getResource("resources/ZoomOut16.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Enter zoom out mode");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		((MapMatrixElement) (bivariateGraph))
			.setMode(MapCanvas.MapMode.ZoomOut);
		BivariateGraphFrame.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_ZOOM_OUT));
	    }
	});
	mapTools.add(button);

	// fourth button
	try {
	    urlGif = cl.getResource("resources/Home16.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Zoom to full extent");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		((MapMatrixElement) (bivariateGraph)).zoomFullExtent();
	    }
	});
	mapTools.add(button);

	// fifth button
	try {
	    urlGif = cl.getResource("resources/pan16.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Enter pan mode");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		BivariateGraphFrame.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		((MapMatrixElement) (bivariateGraph))
			.setMode(MapCanvas.MapMode.Pan);
	    }
	});
	mapTools.add(button);
	// sixth button
	try {
	    urlGif = cl.getResource("resources/excentric16.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Excentric Labels");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		BivariateGraphFrame.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		((MapMatrixElement) (bivariateGraph))
			.setMode(MapCanvas.MapMode.Excentric);
	    }
	});
	mapTools.add(button);

	// seventh button
	try {
	    urlGif = cl.getResource("resources/fisheye16.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Fisheye Lens");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		BivariateGraphFrame.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		((MapMatrixElement) (bivariateGraph))
			.setMode(MapCanvas.MapMode.Fisheye);
	    }
	});
	mapTools.add(button);
	// eighth button
	try {
	    urlGif = cl.getResource("resources/magnifying16.gif");
	    button = new JButton(new ImageIcon(urlGif));
	    button.setPreferredSize(buttDim);
	} catch (Exception ex) {
	    ex.printStackTrace();
	}

	button.setToolTipText("Magnifiying Lens");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		BivariateGraphFrame.this.setCursor(cursors
			.getCursor(GeoCursors.CURSOR_ARROW_PAN));
		((MapMatrixElement) (bivariateGraph))
			.setMode(MapCanvas.MapMode.Pan);
	    }
	});

	mapTools.add(button);

    }

    public void colorClassifierChanged(ColorClassifierEvent e) {
	if (e.getSource() == visClassOne) {
	    e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_X);
	} else if (e.getSource() == visClassTwo) {
	    e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_Y);
	}

	biViz.colorClassifierChanged(e);

	if (bivariateGraph == null) {
	    return;
	}

	// Object src = e.getSource();
	ColorClassifier colorSymbolizerX = visClassOne.getColorClasser();
	ColorClassifier colorSymbolizerY = visClassTwo.getColorClasser();

	BivariateColorClassifierSimple biColorSymbolizer = new BivariateColorClassifierSimple();

	biColorSymbolizer.setClasserX(colorSymbolizerX.getClasser());
	biColorSymbolizer.setColorerX(colorSymbolizerX.getColorer());

	biColorSymbolizer.setClasserY(colorSymbolizerY.getClasser());
	biColorSymbolizer.setColorerY(colorSymbolizerY.getColorer());
	bivariateGraph.setBivarColorClasser(biColorSymbolizer, false);

    }

    public void dataSetChanged(DataSetEvent e) {
	setDataSet(e.getDataSetForApps());
    }

    public void actionPerformed(ActionEvent e) {
	Object src = e.getSource();
	String command = e.getActionCommand();
	String varChangedCommand = ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED;
	String colorChangedCommand = VisualClassifier.COMMAND_COLORS_CHANGED;

	if ((src == visClassOne) && command.equals(varChangedCommand)) {
	    int index = visClassOne.getCurrVariableIndex();
	    index++;
	    displayIndices[0] = index;
	    bivariateGraph.setDataIndices(displayIndices);
	    // this.fireColorArrayChanged();
	} else if ((src == visClassTwo) && command.equals(varChangedCommand)) {
	    int index = visClassTwo.getCurrVariableIndex();
	    index++;
	    displayIndices[1] = index;
	    bivariateGraph.setDataIndices(displayIndices);
	    // this.fireColorArrayChanged();
	} else if ((src == bivariateGraph)
		&& command.compareTo(ScatterPlotBasic.COMMAND_POINT_SELECTED) == 0) {
	    // this.selectedObvs = this.scatterPlot.getSelectedObservations();
	    logger.finest("in actionformed");
	    selections = bivariateGraph.getSelections();
	    fireSelectionChanged(getSelectedObvs());
	} else if (command.equals(colorChangedCommand)) {
	    fireColorArrayChanged();
	}
    }

    public Color[] getColors() {
	return colorArrays;
    }

    /**
     * adds an IndicationListener to the button
     */
    public void addIndicationListener(IndicationListener l) {
	bivariateGraph.addIndicationListener(l);
    }

    /**
     * removes an IndicationListener from the button
     */
    public void removeIndicationListener(IndicationListener l) {
	bivariateGraph.removeIndicationListener(l);
    }

    /**
     * implements ColorArrayListener
     */
    public void addColorArrayListener(ColorArrayListener l) {
	listenerList.add(ColorArrayListener.class, l);
	fireColorArrayChanged(); // so that if any class registers
    }

    /**
     * removes an ColorArrayListener from the component
     */
    public void removeColorArrayListener(ColorArrayListener l) {
	listenerList.remove(ColorArrayListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    private void fireColorArrayChanged() {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	ColorArrayEvent e = null;

	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == ColorArrayListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new ColorArrayEvent(this, getColors());
		}
		((ColorArrayListener) listeners[i + 1]).colorArrayChanged(e);
	    }
	} // next i
    }

    public void setSelectedObvs(int[] selected) {

	if (selected == null) {
	    return;
	}
	for (int i = 0; i < selections.length; i++) {
	    selections[i] = 0;
	}
	for (int i = 0; i < selected.length; i++) {
	    selections[selected[i]] = 1;
	}
	multipleSelectionColors = null;
	bivariateGraph.setSelections(selections);
	repaint();
    }

    /**
     * Return index array for selected observations.
     * 
     * @return
     */
    public int[] getSelectedObvs() {
	// need to transform data length long int[] to short int[]
	Vector selectedObvs = new Vector();
	for (int i = 0; i < selections.length; i++) {
	    if (selections[i] == 1) {
		selectedObvs.add(new Integer(i));
	    }
	}
	int[] selectedShortArray = new int[selectedObvs.size()];
	for (int i = 0; i < selectedObvs.size(); i++) {
	    selectedShortArray[i] = ((Integer) selectedObvs.get(i)).intValue();
	}
	return selectedShortArray;
    }

    public void setMultipleSelectionColors(Color[] multipleSelectionColors) {
	this.multipleSelectionColors = multipleSelectionColors;
	bivariateGraph.setMultipleSelectionColors(this.multipleSelectionColors);
    }

    public void selectionChanged(SelectionEvent e) {
	logger.finest("sending to selectionchanged");
	if (e.getMultipleSlectionColors() != null) {
	    setMultipleSelectionColors(e.getMultipleSlectionColors());
	} else {
	    setSelectedObvs(e.getSelection());
	    // this.scatterPlot.setSelections(e.getSelection());
	    ((Component) bivariateGraph).repaint();
	}
	savedSelection = e.getSelection();
    }

    /**
     * adds an SelectionListener.
     * 
     * @see EventListenerList
     */
    public void addSelectionListener(SelectionListener l) {
	listenerList.add(SelectionListener.class, l);
    }

    /**
     * removes an SelectionListener from the component.
     * 
     * @see EventListenerList
     */
    public void removeSelectionListener(SelectionListener l) {
	listenerList.remove(SelectionListener.class, l);

    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    protected void fireSelectionChanged(int[] newSelection) {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	SelectionEvent e = null;
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == SelectionListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new SelectionEvent(this, newSelection);
		}
		((SelectionListener) listeners[i + 1]).selectionChanged(e);
	    }
	}// next i

    }

    /**
     * implements ColorClassifierListener
     */
    public void addColorClassifierListener(ColorClassifierListener l) {
	visClassOne.addColorClassifierListener(l);
	visClassTwo.addColorClassifierListener(l);

    }

    /**
     * removes an ColorClassifierListener from the component
     */
    public void removeColorClassifierListener(ColorClassifierListener l) {
	visClassOne.removeColorClassifierListener(l);
	visClassTwo.removeColorClassifierListener(l);
    }

    public void indicationChanged(IndicationEvent e) {
	bivariateGraph.setIndication(e.getIndication());
    }

    /*
     * public void colorArrayChanged(ColorArrayEvent e) { Color[] colors =
     * e.getColors(); bivariateGraph.setColorArrayForObs(colors); }
     */
    public void variableSelectionChanged(VariableSelectionEvent e) {
	visClassOne.setCurrVariableIndex(e.getVariableIndex() + 1);
	// scatterPlot.setCurrColorColumnX(e.getVariableIndex()+1);
    }

    public SelectionEvent getSelectionEvent() {
	return new SelectionEvent(this, savedSelection);
    }

}
