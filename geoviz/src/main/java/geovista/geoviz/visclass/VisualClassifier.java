/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.visclass;

// import geovista.common.ui.panel.*;
import geovista.colorbrewer.ColorBrewer;
import geovista.colorbrewer.Palette;
import geovista.colorbrewer.UnivariatePalette;
import geovista.common.classification.Classifier;
import geovista.common.classification.ClassifierPicker;
import geovista.common.data.DataSetForApps;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.ColorArrayListener;
import geovista.common.event.ColumnAppendedEvent;
import geovista.common.event.ColumnAppendedListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.PaletteEvent;
import geovista.common.event.PaletteListener;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.symbolization.ColorBrewerPicker;
import geovista.symbolization.ColorRampPicker;
import geovista.symbolization.ColorSymbolClassificationSimple;
import geovista.symbolization.ColorSymbolizer;
import geovista.symbolization.event.ColorClassifierEvent;
import geovista.symbolization.event.ColorClassifierListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

// import javax.swing.colorchooser.*;

// public class VisualClassifier extends JPanel implements ActionListener {
public class VisualClassifier extends JPanel implements ActionListener,
	ComponentListener, PaletteListener, DataSetListener,
	ColumnAppendedListener, TableModelListener {

    public enum LayoutDebugStatus {
	Debug, Runtime
    }

    public static VisualClassifier.LayoutDebugStatus debugStatus = LayoutDebugStatus.Runtime;
    // Jin Chen: for extending purpose, change private to protected for
    // following fields:
    // symbolizationPanel,colors,dataColors,colorerLinear,colorClasser,classPick
    protected ColorBrewerPicker symbolizationPanel;

    // XXX in future, we want to support much beyond colors.
    protected transient Color[] colors;
    protected transient Color[] dataColors;
    public transient boolean[] anchored;

    private transient double[] data;
    private transient int[] classificationIndex;

    private int nClasses;
    private boolean update;
    private boolean interpolate;
    private boolean setupFinished;
    private boolean variableChooserActive = true;

    private transient JCheckBox updateBox;
    private transient JCheckBox interpolateBox;
    public static final String COMMAND_COLORS_CHANGED = "colors";
    public static final String COMMAND_BEAN_REGISTERED = "hi!";
    public static final String COMMAND_CLASSES_CHANGED = "classNumber";

    private int currOrientation = VisualClassifier.X_AXIS;
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    public transient boolean orientationInParentIsX = false;

    protected ColorSymbolClassificationSimple colorClasser;

    protected ClassifierPicker classPick;

    // private transient TexturePaint texPaint;
    // private transient int oldIndication = 0;
    final static Logger logger = Logger.getLogger(VisualClassifier.class
	    .getName());

    public VisualClassifier() {
	super();
	if (VisualClassifier.debugStatus
		.equals(VisualClassifier.LayoutDebugStatus.Debug)) {
	    setBorder(BorderFactory.createTitledBorder(this.getClass()
		    .getSimpleName()));

	}
	addComponentListener(this);
	colorClasser = new ColorSymbolClassificationSimple();

	// colorerLinear = new ColorSymbolizerLinear();
	setupFinished = false;
	nClasses = ColorRampPicker.DEFAULT_NUM_SWATCHES;
	update = true;

	classPick = new ClassifierPicker();
	classPick.addActionListener(this);
	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	makeSymbolizationPanel();

	this.add(classPick);
	if (variableChooserActive == true) {
	    setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
	} else {
	    setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_HIDDEN);
	}

	// this.add(classificationPanel);
	this.add(symbolizationPanel);
	setupFinished = true;
	makeColors();
	// Dimension size = new Dimension(800, 600);
	// setPreferredSize(size);
	// setMinimumSize(size);
	/*
	 * int prefHeight = (int)this.classPick.getPreferredSize().getHeight();
	 * prefHeight = prefHeight +
	 * (int)this.symbolizationPanel.getPreferredSize().getHeight(); int
	 * prefWidth = (int)this.classPick.getPreferredSize().getWidth(); if
	 * (prefWidth >
	 * (int)this.symbolizationPanel.getPreferredSize().getWidth()) {
	 * prefWidth =
	 * (int)this.symbolizationPanel.getPreferredSize().getWidth(); //take
	 * the smaller one } this.setPreferredSize(new Dimension(prefWidth,
	 * prefHeight));
	 */
	revalidate();
	makeTexPaint();
	// setMinimumSize(new Dimension(200, 60));
	// setMaximumSize(new Dimension(1000, 60));
    }

    public void setPalette(ColorBrewer.BrewerNames name) {
	symbolizationPanel.setPalette(name);
	makeColors();
    }

    private void makeTexPaint() {

	int texSize = 4;
	// Rectangle2D.Float rect = new Rectangle2D.Float(0, 0, texSize,
	// texSize);
	BufferedImage buff = new BufferedImage(texSize, texSize,
		BufferedImage.TYPE_INT_ARGB);
	Graphics2D g2 = buff.createGraphics();
	Color trans = new Color(0, 0, 0, 0); // clear black
	g2.setColor(trans);
	g2.fillRect(0, 0, texSize, texSize);
	g2.setColor(Color.blue);
	g2.drawLine(0, 0, 32, 32);

	// texPaint = new TexturePaint(buff, rect);

    }

    private void makeSymbolizationPanel() {
	symbolizationPanel = new ColorBrewerPicker();
	this.add(symbolizationPanel);
	symbolizationPanel.addActionListener(this);
    }

    private class PaletteColorer implements ColorSymbolizer {
	UnivariatePalette pal;
	int nClasses;

	PaletteColorer(UnivariatePalette pal, int nClasses) {
	    this.pal = pal;
	    this.nClasses = nClasses;
	}

	public Color[] getColors(int numClasses) {

	    return pal.getColors(nClasses);
	}

	public int getNumClasses() {

	    return nClasses;
	}

    }

    private void makeColors() {

	colors = symbolizationPanel.getColors();
	colorClasser = new ColorSymbolClassificationSimple();
	PaletteColorer palCol = new PaletteColorer(
		symbolizationPanel.getPalette(), nClasses);

	colorClasser.setColorer(palCol);

    }

    private void makeColorClasser() {
	colorClasser = new ColorSymbolClassificationSimple();
	// colorClasser.setColorer(this.symbolizationPanel.getPalette());
    }

    /*
     * private void makeColors() { if (logger.isLoggable(Level.FINEST)) {
     * logger.finest("VisClass, making colors..."); } Color[] pickerColors =
     * symbolizationPanel.getColors(); boolean[] pickerAnchors =
     * symbolizationPanel.getAnchored(); // now we interpolate...linearly int
     * nPicks = pickerColors.length; double picksPerColor = (double) nPicks /
     * (double) nClasses; if (colors == null) { colors = new Color[nClasses]; }
     * else if (colors.length != nClasses) { colors = new Color[nClasses]; }
     * 
     * for (int i = 0; i < nClasses; i++) { double whichColor = i *
     * picksPerColor; int index = (int) Math.floor(whichColor); if
     * (logger.isLoggable(Level.FINEST)) { logger.finest("i = " + i + "index = "
     * + index); } colors[i] = pickerColors[index];
     * 
     * } if (interpolate) { // for each lock in the picker, find the class that
     * is closest. double colorsPerPick = (double) (nClasses - 1) / (double)
     * (nPicks - 1); // int[] newAnchors = new int[pickerAnchors.length]; Vector
     * newAnchors = new Vector(); for (int i = 0; i < pickerAnchors.length; i++)
     * { double whichClass = i * colorsPerPick; int aClass = (int)
     * Math.round(whichClass); if (pickerAnchors[i]) { Integer Ind = new
     * Integer(aClass); newAnchors.add(Ind); } }
     * 
     * boolean[] colorAnchors = new boolean[nClasses]; if (newAnchors.size() >
     * 2) { for (Enumeration e = newAnchors.elements(); e.hasMoreElements();) {
     * Integer ind = (Integer) e.nextElement(); colorAnchors[ind.intValue()] =
     * true; }
     * 
     * colorAnchors[0] = true; colorAnchors[colorAnchors.length - 1] = true; }
     * else if (newAnchors.size() == 2) { colorAnchors[0] = true;
     * colorAnchors[colorAnchors.length - 1] = true; } else if
     * (newAnchors.size() == 1) { colorAnchors[0] = true; } // now find those
     * durn colors! symbolizationPanel.getRamp().rampColors(colors,
     * colorAnchors); colorerLinear.setRampingColors(colors);
     * colorerLinear.setAnchors(colorAnchors);
     * colorClasser.setColorer(colorerLinear); } // end if interpolate }
     */
    public Color[] findDataColors() {
	if (logger.isLoggable(Level.FINEST)) {
	    logger.finest("VisClass, finding data colors...");
	}
	if (classPick.getDataSet() == null) {
	    return null;
	}
	int currVar = classPick.getCurrVariableIndex();
	Classifier classer = classPick.getClasser();
	// bail if we don't have enough variables
	if (currVar > classPick.getDataSet().getNumberNumericAttributes()) {
	    return null;
	}
	double[] data = classPick.getDataSet().getNumericDataAsDouble(currVar);
	int[] classedData = classer.classify(data, classPick.getNClasses());
	if (dataColors == null || dataColors.length != classedData.length) {
	    dataColors = new Color[classedData.length];
	}
	colors = symbolizationPanel.getColors();
	for (int i = 0; i < classedData.length; i++) {
	    int aClass = classedData[i];
	    if (aClass < 0) {
		aClass = 0; // XXX egregious hack
	    }
	    Color c = colors[aClass];
	    dataColors[i] = c;
	}
	return dataColors;
    }

    /** Listens to the check boxen. */
    class CheckBoxListener implements ItemListener {
	public void itemStateChanged(ItemEvent e) {
	    if (e.getSource().equals(updateBox)) {
		if (e.getStateChange() == ItemEvent.SELECTED && setupFinished) {
		    update = true;
		    makeColors();
		    fireActionPerformed(VisualClassifier.COMMAND_COLORS_CHANGED);
		    fireColorClassifierPerformed();
		    fireColorArrayChanged();

		} else if (e.getStateChange() == ItemEvent.DESELECTED) {

		    update = false;
		}
	    } else if (e.getSource().equals(interpolateBox)) {
		if (e.getStateChange() == ItemEvent.SELECTED && setupFinished) {
		    interpolate = true;
		    makeColors();
		    fireActionPerformed(VisualClassifier.COMMAND_COLORS_CHANGED);
		    fireColorClassifierPerformed();
		    fireColorArrayChanged();

		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
		    interpolate = false;
		    makeColors();
		    fireActionPerformed(VisualClassifier.COMMAND_COLORS_CHANGED);
		    fireColorClassifierPerformed();
		    fireColorArrayChanged();
		}
	    }
	}
    }

    class SliderListener implements ChangeListener {
	public void stateChanged(ChangeEvent e) {
	    JSlider source = (JSlider) e.getSource();
	    // if (!source.getValueIsAdjusting()) {

	    int fps = source.getValue();
	    if (fps == 0) {
		// if (!frozen) stopAnimation();
	    } else {
		symbolizationPanel.setNSwatches(fps);
		nClasses = fps;
		makeColors();
		fireActionPerformed(VisualClassifier.COMMAND_COLORS_CHANGED);
		fireColorClassifierPerformed();
		fireColorArrayChanged();
		fireActionPerformed(VisualClassifier.COMMAND_CLASSES_CHANGED);
	    }
	    // }
	}
    }

    public ColorBrewerPicker getSymbolizationPanel() {
	return symbolizationPanel;
    }

    public void setSymbolizationPanel(ColorBrewerPicker symbolizationPanel) {
	this.symbolizationPanel = symbolizationPanel;
	makeColors();
	logger.finest("VC, settingSymbolizationPanel");
    }

    public ColorSymbolClassificationSimple getColorClasser() {
	return colorClasser;
    }

    public void setColorClasser(ColorSymbolClassificationSimple colorClasser) {
	this.colorClasser = colorClasser;
    }

    // public ColorSymbolizer getColorSymbolizer() {
    // logger.finest("VC, gettingColorSymbolizer");
    // return colorerLinear;
    // }

    // public void setColorSymbolizer(ColorSymbolizer colorerLinear) {
    // this.colorerLinear = (ColorSymbolizerLinear) colorerLinear;
    // }

    public Color[] getColorForObservations() {
	return findDataColors();
    }

    public Color[] getColors() {
	logger.finest("VC, gettingColors");
	if (data == null) {
	    logger.fine("Warning: VisualClassifier.getColors() called when VC had no data");
	    return null;
	}
	colors = symbolizationPanel.getColors();
	return colors;
    }

    public void setColors(Color[] colors) {
	this.colors = colors;
    }

    public int getClassNumber() {
	return nClasses;
    }

    public void paletteChanged(PaletteEvent e) {
	Palette pal = e.getPalette();
	UnivariatePalette uPal = (UnivariatePalette) pal;
	int maxColors = uPal.getMaxLength();
	logger.finest("max colors = " + maxColors);

	// we can't go over the max, or it blows null exceptions
	// so, we interpolate....

	int numClasses = classPick.getNClasses();
	Color[] cols = null;
	if (numClasses <= maxColors) {
	    cols = uPal.getColors(numClasses);
	} else {
	    // we have more colors wanted than the pallet can give us
	    // todo: make this work
	    // this.assignColors
	    cols = uPal.getColors(numClasses);
	}

	setColors(cols);
	symbolizationPanel.setColors(cols);
	revalidate();
	this.repaint();
    }

    public void actionPerformed(ActionEvent e) {
	String command = e.getActionCommand();
	logger.fine("actionPerformed: " + command);
	if (command == ColorRampPicker.COMMAND_SWATCH_COLOR_CHANGED) {
	    makeColors();
	    fireActionPerformed(VisualClassifier.COMMAND_COLORS_CHANGED);
	    fireColorClassifierPerformed();
	    VisualClassifier.this.fireColorArrayChanged();
	} else if (command == ClassifierPicker.COMMAND_CLASSES_CHANGED) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.finest("VisClass, n classes changed...");
	    }
	    int nClasses = classPick.getNClasses();
	    symbolizationPanel.setNSwatches(nClasses);
	    this.nClasses = nClasses;
	    fireActionPerformed(VisualClassifier.COMMAND_CLASSES_CHANGED);
	    makeColors();
	    fireActionPerformed(VisualClassifier.COMMAND_COLORS_CHANGED);
	    fireColorClassifierPerformed();
	    VisualClassifier.this.fireColorArrayChanged();
	    this.revalidate();
	} else if (command == ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED) {
	    if (logger.isLoggable(Level.FINEST)) {
		logger.finest("VisClass, selected variable changed...");
	    }

	    fireActionPerformed(ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED);
	    fireColorArrayChanged();
	} else if (command == ClassifierPicker.COMMAND_SELECTED_CLASSIFIER_CHANGED) {
	    // this.colorClasser.setColorer(this.colorerLinear);
	    colorClasser.setClasser(classPick.getClasser());
	    fireColorClassifierPerformed();
	    VisualClassifier.this.fireColorArrayChanged();
	}

	// need to pass this along, if we are a FoldupPanel
	// super.actionPerformed(e);
    }

    // start component event handling
    // note: this class only listens to itself
    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {

    }

    public void componentResized_old(ComponentEvent e) {
	double pickPrefWidth = classPick.getPreferredSize().getWidth();

	int prefWidth = (int) (pickPrefWidth * 1.5);
	if (getWidth() >= prefWidth) {
	    changeOrientation(VisualClassifier.X_AXIS);
	} else {
	    changeOrientation(VisualClassifier.Y_AXIS);
	}

    }

    // end component event handling

    private void changeOrientation(int orientation) {
	if (orientation == currOrientation) {
	    return;
	} else if (orientation == VisualClassifier.X_AXIS) {
	    Component[] comps = new Component[getComponentCount()];
	    for (int i = 0; i < getComponentCount(); i++) {
		comps[i] = getComponent(i);
	    }
	    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	    for (int i = 0; i < getComponentCount(); i++) {
		this.add(comps[i]);
	    }
	    currOrientation = VisualClassifier.X_AXIS;
	    setPreferredSize(new Dimension(300, 20));
	    revalidate();
	} else if (orientation == VisualClassifier.Y_AXIS) {
	    Component[] comps = new Component[getComponentCount()];
	    for (int i = 0; i < getComponentCount(); i++) {
		comps[i] = getComponent(i);
	    }
	    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	    for (int i = 0; i < getComponentCount(); i++) {
		this.add(comps[i]);
	    }
	    currOrientation = VisualClassifier.Y_AXIS;
	    setPreferredSize(new Dimension(300, 40));
	    revalidate();
	}

    }

    /**
     * implements ColorArrayListener
     */
    public void addColorArrayListener(ColorArrayListener l) {
	listenerList.add(ColorArrayListener.class, l);
	// this.fireColorArrayChanged(); //so that if any class registers

	// it gets an event
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
    protected void fireColorArrayChanged() { // change from private to
	// protected by Jin Chen to
	// enable fire the event
	// programmingly
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	ColorArrayEvent e = null;

	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == ColorArrayListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new ColorArrayEvent(this, findDataColors());
		}

		((ColorArrayListener) listeners[i + 1]).colorArrayChanged(e);
	    }
	} // next i
    }

    // }
    /**
     * implements ColorClassifierListener
     */
    public void addColorClassifierListener(ColorClassifierListener l) {
	listenerList.add(ColorClassifierListener.class, l);
	fireColorClassifierPerformed(); // so that if any class
	// registers
	// it gets an event
    }

    /**
     * removes an ColorClassifierListener from the component
     */
    public void removeColorClassifierListener(ColorClassifierListener l) {
	listenerList.remove(ColorClassifierListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    private void fireColorClassifierPerformed() {
	if (update) {
	    // Guaranteed to return a non-null array
	    Object[] listeners = listenerList.getListenerList();
	    ColorClassifierEvent e = null;
	    // Process the listeners last to first, notifying
	    // those that are interested in this event
	    for (int i = listeners.length - 2; i >= 0; i -= 2) {
		if (listeners[i] == ColorClassifierListener.class) {
		    // Lazily create the event:
		    if (e == null) {
			e = new ColorClassifierEvent(this, getColorClasser());
		    }
		    if (orientationInParentIsX == true) {
			e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_X);
		    } else {
			e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_Y);
		    }
		    ((ColorClassifierListener) listeners[i + 1])
			    .colorClassifierChanged(e);
		}
	    } // next i
	} // end if
    }

    /**
     * implements ActionListener
     */
    public void addActionListener(ActionListener l) {
	listenerList.add(ActionListener.class, l);
	fireActionPerformed(VisualClassifier.COMMAND_BEAN_REGISTERED);
    }

    /**
     * removes an ActionListener from the component
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
    private void fireActionPerformed(String command) {
	if (update) {
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
	    } // next i
	} // end if
    }

    public void setIndicatedClass(int indicClass) {
	logger.finest("class = " + indicClass);
	// clear old indication
	// if it still exists
	/*
	 * if (oldIndication < symbolizationPanel.getPanSet().length) {
	 * symbolizationPanel.getPanSet()[oldIndication].setTexPaint(null); }
	 * 
	 * if (indicClass < 0) { // used for null or out of range indication
	 * this.repaint(); // clear old indication return; } if (indicClass >=
	 * symbolizationPanel.getPanSet().length) { this.repaint(); // clear old
	 * indication return; }
	 * 
	 * symbolizationPanel.getPanSet()[indicClass].setTexPaint(texPaint);
	 * 
	 * oldIndication = indicClass; this.repaint();
	 */
    }

    public boolean[] getAnchored() {
	return anchored;
    }

    public void setAnchored(boolean[] anchored) {
	this.anchored = anchored;
    }

    public int[] getClassificationIndex() {
	return classificationIndex;
    }

    public void setClassificationIndex(int[] classificationIndex) {
	this.classificationIndex = classificationIndex;
    }

    public ClassifierPicker getClassPick() {
	return classPick;
    }

    public void setClassPick(ClassifierPicker classPick) {
	this.classPick = classPick;
    }

    public int getCurrOrientation() {
	return currOrientation;
    }

    public void setCurrOrientation(int currOrientation) {
	this.currOrientation = currOrientation;
    }

    public int getVariableChooserMode() {
	return classPick.getVariableChooserMode();
    }

    public void setVariableChooserMode(int variableChooserMode) {
	if (classPick.getVariableChooserMode() != variableChooserMode) {
	    classPick.setVariableChooserMode(variableChooserMode);

	}
    }

    public void setCurrVariableIndex(int index) {
	classPick.setCurrVariableIndex(index);
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

    public void setDataSet(DataSetForApps data) {
	classPick.setDataSet(data);
    }

    public void setAdditionalData(DataSetForApps data) {
	classPick.setAdditionalData(data);
    }

    public int getCurrVariableIndex() {
	return classPick.getCurrVariableIndex();
    }

    public void setHighColor(Color c) {
	logger.finest(c.toString());
	// symbolizationPanel.setHighColor(c);
    }

    public void setOrientationInParentIsX(boolean orientationInParentIsX) {
	this.orientationInParentIsX = orientationInParentIsX;
    }

    public boolean isVariableChooserActive() {
	return variableChooserActive;
    }

    public void setVariableChooserActive(boolean variableChooserActive) {
	this.variableChooserActive = variableChooserActive;
    }

    public void dataSetChanged(DataSetEvent e) {
	setData(e.getDataSet());
    }

    public void dataSetModified(ColumnAppendedEvent e) {
	if (e.getEventType() == ColumnAppendedEvent.ChangeType.TYPE_EXTENDED) {
	    // Object[] newData = e.getNewData();
	    // DataSetForApps newDataSet = new DataSetForApps(newData);
	    classPick.dataSetModified(e);
	}
    }

    public void tableChanged(TableModelEvent e) {

	classPick.tableChanged(e);
	classPick.setCurrVariableIndex(e.getColumn());

    }

    /**
     * @return the update
     */
    public boolean getUpdate() {
	return update;
    }

    /**
     * @param update
     *            the update to set
     */
    public void setUpdate(boolean update) {
	this.update = update;
    }

    public static void main(String[] args) {

	VisualClassifier.logger.setLevel(Level.WARNING);

	JFrame app = new JFrame();

	app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	VisualClassifier vc = new VisualClassifier();

	vc.setPalette(ColorBrewer.BrewerNames.Greens);

	GeoDataGeneralizedStates data = new GeoDataGeneralizedStates();
	vc.setDataSet(data.getDataForApps());
	app.add(vc);
	app.pack();
	app.setVisible(true);

    }

}
