/*
 * ParallelDisplay.java
 *
 * Created on 18. November 2001, 19:49
 *
 * Copyright 2001 Flo Ledermann flo@subnet.at
 *
 * Licensed under GNU General Public License (GPL).
 * See http://www.gnu.org/copyleft/gpl.html
 */

package geovista.geoviz.parvis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.jts.EmptyShape;
import geovista.common.ui.ExcentricLabelClient;
import geovista.common.ui.ShapeReporter;
import geovista.common.ui.VisualSettingsPopupAdapter;
import geovista.common.ui.VisualSettingsPopupListener;
import geovista.common.ui.VisualSettingsPopupMenu;

/**
 * The swing GUI Component for displaying a parallel coordinate visualisation.
 * Note that the actual rendering is done by the UI delegate, ParallelDisplayUI
 * (with its single subclass BasiParallelDisplayUI). This class is used to store
 * the state of the component and interact with the environment.
 * 
 * @author Flo Ledermann flo@subnet.at
 * 
 */
public class ParallelDisplay extends JComponent implements ChangeListener,
		IndicationListener, SelectionListener, ExcentricLabelClient,
		VisualSettingsPopupListener, ShapeReporter {

	protected final static Logger logger = Logger
			.getLogger(ParallelDisplay.class.getName());

	boolean useSelectionBlur = true;
	/** Scale values for the axes. */
	private float axisScale[] = null;
	/** Offset values for the axes. */
	private float axisOffset[] = null;

	/** axis -> dimension linking. */
	protected int axisOrder[] = null;

	/** brushed values of records */
	protected float brushValues[] = null;
	int[] savedSelection;

	protected Color recordColor = Color.black;
	protected Color brushedColor = Color.blue;

	/** Our model. */
	private ParallelSpaceModel model;

	/** The mode of interaction we are in. */
	protected int editMode = 0;

	public static final int REORDER = 0;
	public static final int SCALE = 1;
	public static final int TRANSLATE = 2;
	/** used to invert axis orientation */
	public static final int INVERT = 3;
	public static final int BRUSH = 4;

	public int indication = -1;

	public int[] indicationNeighbors;

	/**
	 * Whether we have to redraw the whole background. This is usually only
	 * needed if the model changes.
	 */
	public boolean deepRepaint = true;

	static {
		UIManager.put("geovista.geoviz.parvis.ParallelDisplayUI",
				"geovista.geoviz.parvis.BasicParallelDisplayUI");
	}

	/**
	 * Creates a new ParallelDisplay.
	 */
	public ParallelDisplay() {
		init(null);

	}

	/**
	 * Creates a new ParallelDisplay with the given model.
	 * 
	 * @param model
	 *            The model to display.
	 */
	public ParallelDisplay(ParallelSpaceModel model) {
		init(model);
	}

	/**
	 * Initializes the component with the given model.
	 * 
	 * @param model
	 *            The model to use.
	 */
	protected void init(ParallelSpaceModel model) {

		VisualSettingsPopupMenu popMenu = new VisualSettingsPopupMenu(this);
		MouseAdapter listener = new VisualSettingsPopupAdapter(popMenu);
		popMenu.addMouseListener(listener);
		addMouseListener(listener);

		logger.finest("Initializing ParallelDisplay Component");
		int[] emptyArray = {};
		indicationNeighbors = emptyArray;
		setModel(model);

		setMinimumSize(new Dimension(100, 100));
		setPreferredSize(new Dimension(700, 400));

		setBackground(Color.white);
		setDoubleBuffered(false);
		setOpaque(true);

		setDefaultPreferences();

		updateUI();
	}

	/**
	 * Returns the dimension associated with the given axis. Note that the
	 * assignment axisNumber (in the display) => dimensionNumber (in the model)
	 * is only the default setting, the axes might be reordered, or some
	 * dimensions may even be left out or displayed twice. So be sure to always
	 * call this function before querying the model to "convert" the axis number
	 * to the right deímension value.
	 * 
	 * @param order
	 *            The number of the axis to get the dimension number for.
	 * 
	 * @return The number of the dimnesion to display on the given axis.
	 */
	protected int getAxis(int order) {
		return axisOrder[order];
	}

	/**
	 * Returns the number of axes to display. Note that this is not necessarily
	 * equal to the number of dimensions in the model (see above: getAxis()).
	 * 
	 * @return The number of axes to display.
	 */
	public int getNumAxes() {
		if (axisOrder != null) {
			return axisOrder.length;
		}
		return 0;
	}

	/**
	 * Swaps two axes. This means the dimensions assigned to the two axes are
	 * swapped.
	 * 
	 * @param axis1
	 *            The first axis.
	 * @param axis2
	 *            The second axis.
	 */
	public void swapAxes(int axis1, int axis2) {
		int temp = axisOrder[axis1];

		axisOrder[axis1] = axisOrder[axis2];
		axisOrder[axis2] = temp;
	}

	/**
	 * Sets up the axis -> dimension assignment. See above: getAxis();
	 * 
	 * @param order
	 *            An array containing the int ids of the dimensions to be
	 *            displayed on the axes.
	 */
	public void setAxisOrder(int[] order) {
		axisOrder = order;
	}

	/**
	 * Sets the model to display.
	 * 
	 * @param model
	 *            The model to display.
	 */
	public void setModel(ParallelSpaceModel model) {
		if (this.model != null) {
			this.model.removeChangeListener(this);

			axisOffset = null;
			axisScale = null;

		}
		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();

		if (pui != null && pui.getRenderThread() != null
				&& pui.getBrushThread() != null) {

			pui.getRenderThread().doWork = false;
			pui.getBrushThread().doWork = false;
			// RenderThread.sleep(2000);

			// pui.getRenderThread().stopAxis = model.getNumDimensions() - 1;

		}
		this.model = model;

		if (model != null) {

			pui = (BasicParallelDisplayUI) getUI();
			if (pui.getRenderThread() != null) {
				pui.getRenderThread()
						.setRegion(0, model.getNumDimensions() - 1);
				pui.getBrushThread().setRegion(0, model.getNumDimensions() - 1);
				// pui.getRenderThread().stopAxis = model.getNumDimensions() -
				// 1;
				// pui.getBrushThread().stopAxis = model.getNumDimensions() - 1;

			}
			model.addChangeListener(this);

			axisOffset = new float[model.getNumDimensions()];
			axisScale = new float[model.getNumDimensions()];
			axisOrder = new int[model.getNumDimensions()];
			// fah -- trying this to fix the problem of selections breaking the
			// model

			if (brushValues == null
					|| brushValues.length != model.getNumRecords()) {
				brushValues = new float[model.getNumRecords()];
				for (int selVal = 0; selVal < brushValues.length; selVal++) {
					setBrushValue(selVal, 1f);
				}
				brushCount = brushValues.length;
			}
			for (int i = 0; i < model.getNumDimensions(); i++) {
				// initialize scaling of axis to show maximum detail
				axisOffset[i] = model.getMaxValue(i);
				axisScale[i] = model.getMinValue(i) - axisOffset[i];
				axisOrder[i] = i;
			}
		}

		deepRepaint = true;
		repaint();

	}

	/**
	 * Sets the mode for user interaction with the display.
	 * 
	 * @param mode
	 *            The interaction mode to use.
	 */
	public void setEditMode(int mode) {
		editMode = mode;

		resetCursor();
	}

	void resetCursor() {
		switch (editMode) {
		case BRUSH:
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			break;
		default:
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * Returns the currently active interaction mode.
	 * 
	 * @return The currently active interaction mode.
	 */
	public int getEditMode() {
		return editMode;
	}

	/**
	 * Returns the model. Actually the model should be hidden, because of the
	 * confusion that might occur by mistaking axes and dimensions (see above:
	 * getAxis()). This requires a rewrite of some parts of the code, so this is
	 * marked to do!
	 * 
	 * @return The model that is currently displayed ba the component.
	 */
	protected ParallelSpaceModel getModel() {
		return model;
	}

	/**
	 * Returns the number of Records in the model.
	 */
	public int getNumRecords() {
		if (model != null) {
			return model.getNumRecords();
		}
		return 0;
	}

	public String getRecordLabel(int num) {
		if (model != null) {
			return model.getRecordLabel(num);
		}
		return null;
	}

	public float getValue(int recordNum, int axisNum) {
		if (model != null && axisOrder.length > axisNum) {
			return model.getValue(recordNum, axisOrder[axisNum]);
		}
		return 0;
	}

	private int brushCount = 0;

	public int[] findSelection() {
		int numSelected = 0;
		for (float element : brushValues) {
			if (element > 0f) {
				numSelected++;
			}
		}
		int[] selection = new int[numSelected];
		int counter = 0;
		for (int i = 0; i < brushValues.length; i++) {
			if (brushValues[i] > 0f) {
				selection[counter] = i;
				counter++;
			}
		}
		return selection;
	}

	public float getBrushValue(int num) {
		if (brushValues != null) {
			return brushValues[num];
		}
		return 0.0f;
	}

	public void setBrushValue(int num, float val) {
		if (brushValues != null) {
			brushValues[num] = val;
		}
		if (val > 0.0f) {
			brushCount = 1;
		}
	}

	public int getBrushedCount() {
		return brushCount;
	}

	public int[] getRecordsByValueRange(int axisnum, float min, float max) {

		int ids[] = new int[getNumRecords()];
		int count = 0;

		for (int i = 0; i < getNumRecords(); i++) {
			float val = getValue(i, axisnum);
			if ((val >= min) && (val <= max)) {
				ids[count++] = i;
			}
		}

		if (count > 0) {
			int newids[] = new int[count];
			System.arraycopy(ids, 0, newids, 0, count);
			return newids;
		}
		return new int[0];
	}

	public Color getRecordColor() {
		return recordColor;
	}

	public Color getBrushedColor() {
		return brushedColor;
	}

	/**
	 * Sets the user interface delegate for the component.
	 */
	public void setUI(ParallelDisplayUI ui) {
		super.setUI(ui);
	}

	public ParallelDisplayUI getUI() {
		ParallelDisplayUI pUI = (ParallelDisplayUI) ui;
		return pUI;
	}

	/**
	 * Invalidates the component and causes a complete repaint.
	 */
	@Override
	public void invalidate() {
		super.invalidate();
		deepRepaint = true;
	}

	/**
	 * Swing method.
	 */
	@Override
	public void updateUI() {
		try {
			setUI((ParallelDisplayUI) UIManager.getUI(this));
		} catch (ClassCastException ccex) {
			ccex.printStackTrace();
		}
		invalidate();
	}

	/**
	 * Swing method.
	 */
	@Override
	public String getUIClassID() {
		logger.finest("retrieving classID");
		return "geovista.geoviz.parvis.ParallelDisplayUI";
	}

	/**
	 * Invoked when the model has changed its state.
	 * 
	 * @param e
	 *            A ChangeEvent object
	 */
	public void stateChanged(ChangeEvent e) {
		repaint();
	}

	/**
	 * Added by Frank Hardisty 19 July 2002
	 * 
	 */
	public void indicationChanged(IndicationEvent e) {
		int indication = e.getIndication();
		this.indication = indication;
		indicationNeighbors = e.getNeighbors();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("got indication, n bors = "
					+ indicationNeighbors.length);
		}
		// model.
		// BasicParallelDisplayUI bui =
		// (BasicParallelDisplayUI)UIManager.getUI(this);
		// can't use previous line b/c this makes a new UI when called
		// bui.setHoverRecord(indication,this);
		if (savedSelection != null && getUI().getRenderThread() != null
				&& savedSelection.length > 0) {
			getUI().getRenderThread().useSelectionBlur = useSelectionBlur;
		}
		repaint();

	}

	public void selectionChanged(SelectionEvent e) {
		getUI().createBrushImage(this);

		// first zero out old one
		for (int i = 0; i < brushValues.length; i++) {
			setBrushValue(i, 0f);
		}

		int[] selection = e.getSelection();

		if (selection.length <= 0) {

			for (int i = 0; i < model.getNumRecords(); i++) {
				setBrushValue(i, 1f);
			}
			brushCount = model.getNumRecords();
			deepRepaint = true;
			repaint();
			return;
		}
		// then put new one in
		for (int selVal : selection) {
			setBrushValue(selVal, 1f);
		}
		getUI().renderBrush();

		repaint();
		savedSelection = e.getSelection();
	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, savedSelection);
	}

	/**
	 * Returns the current offset (translation in axis units) for the axis.
	 * 
	 * @param num
	 *            The axis number.
	 * 
	 * @return The offset value.
	 */
	public float getAxisOffset(int num) {
		if (axisOffset != null && axisOrder.length > num) {
			return axisOffset[axisOrder[num]];
		}
		return 0;
	}

	/**
	 * Returns the current scale (visible region in axis units) for the axis.
	 * 
	 * @param num
	 *            The axis number.
	 * 
	 * @return The scale value.
	 */
	public float getAxisScale(int num) {
		if (axisScale != null && axisOrder.length > num) {
			return axisScale[axisOrder[num]];
		}
		return 0;
	}

	/**
	 * Returns a String label for a specific axis.
	 * 
	 * @param num
	 *            The axis number.
	 * 
	 * @return A Human-readable label for the axis.
	 */

	public String getAxisLabel(int num) {
		if (model != null) {
			String label = model.getAxisLabel(axisOrder[num]);
			if (label != null) {
				return label;
			}
			return ("X" + axisOrder[num]);
		}
		return "";
	}

	/**
	 * Sets the offset (translation in axis units) for the axis.
	 * 
	 * @param axis
	 *            The axis number.
	 * @param offset
	 *            The offset value.
	 */
	public void setAxisOffset(int axis, float offset) {
		if (axisOffset != null && axisOffset.length > axisOrder[axis]) {
			axisOffset[axisOrder[axis]] = offset;
		}

		repaint();
	}

	/**
	 * Sets the scale (visible region in axis units) for the axis.
	 * 
	 * @param axis
	 *            The axis number.
	 * @param scale
	 *            The scale value.
	 */
	public void setAxisScale(int axis, float scale) {
		if (axisScale != null && axisScale.length > axisOrder[axis]) {
			axisScale[axisOrder[axis]] = scale;
		}

		repaint();
	}

	/**
	 * Configures (scales, translates) all axes to show all values between its
	 * minimum and its maximum on a maximum scale.
	 */
	public void minMaxScale() {
		for (int i = 0; i < getNumAxes(); i++) {
			// initialize scaling of axis to show maximum detail
			axisOffset[i] = model.getMaxValue(i);
			axisScale[i] = model.getMinValue(i) - axisOffset[i];
		}

		deepRepaint = true;
		repaint();
	}

	/**
	 * Configures (scales, translates) all axes to show all values between the
	 * minimum and its maximum of the given variable.
	 */
	public void varMinMaxScale(int var) {
		float min = model.getMinValue(var);
		float max = model.getMaxValue(var);
		float range = max - min;
		// float range = model.getMinValue(var) - axisOffset[var];
		for (int i = 0; i < getNumAxes(); i++) {
			// here we have a problem if the scales are too wildly different
			// specifically, if thisScale is big and range is small
			// in such a case we just move everything off the screen range

			float thisMax = model.getMaxValue(i);
			float thisMin = model.getMinValue(i);
			float scaleFactor = getHeight() / range;
			float endMax = (thisMax - min) * scaleFactor;
			float endMin = (thisMin - min) * scaleFactor;

			logger.finest("i " + i);
			logger.finest("endMin" + endMin);
			logger.finest("endMax" + endMax);
			endMin = Math.abs(endMin);
			endMax = Math.abs(endMax);
			// endMax = Math.abs(endMax);
			if (endMin > 300000 || endMax > 300000) {
				logger.finest("fudge!");

				if (max > 0) {
					axisOffset[i] = Math.abs(thisMax) * 2f;
				} else {
					axisOffset[i] = Math.abs(thisMax) * -2f;
				}

				axisScale[i] = thisMax - thisMin;
			} else {
				axisOffset[i] = max;
				axisScale[i] = min - max;
			}
		}

		deepRepaint = true;
		repaint();
	}

	/**
	 * Configures (scales, translates) all axes to show all values between zero
	 * and its maximum on a maximum scale.
	 */
	public void zeroMaxScale() {
		for (int i = 0; i < getNumAxes(); i++) {
			// initialize scaling of axis to show maximum detail
			axisOffset[i] = model.getMaxValue(i);
			axisScale[i] = -1 * axisOffset[i];
		}

		deepRepaint = true;
		repaint();
	}

	/**
	 * Configures (scales, translates) all axes to show values between zero (or
	 * the nagative minimum of all axes) and the maximum value of all axes on a
	 * maximum scale.
	 */
	public void minMaxAbsScale() {
		int i;

		float absmax = Float.NEGATIVE_INFINITY;
		float absmin = 0.0f;

		for (i = 0; i < getNumAxes(); i++) {
			// initialize scaling of axis to show maximum detail
			float val = model.getMaxValue(i);
			if (val > absmax) {
				absmax = val;
			}
			val = model.getMinValue(i);
			if (val < absmin) {
				absmin = val;
			}
		}

		for (i = 0; i < getNumAxes(); i++) {
			axisOffset[i] = absmax;
			axisScale[i] = absmin - absmax;
		}

		deepRepaint = true;
		repaint();
	}

	private final Vector progressListeners = new Vector();

	public void addProgressListener(ProgressListener l) {
		progressListeners.add(l);
	}

	public void removeProgressListener(ProgressListener l) {
		progressListeners.remove(l);
	}

	public void fireProgressEvent(ProgressEvent e) {
		Vector list = (Vector) progressListeners.clone();
		for (int i = 0; i < list.size(); i++) {
			ProgressListener l = (ProgressListener) list.elementAt(i);
			l.processProgressEvent(e);
		}
	}

	Hashtable preferences = new Hashtable();

	public void setDefaultPreferences() {
		preferences.put("brushRadius", new Float(0.2f));
		preferences.put("softBrush", new Boolean(true));
		preferences.put("hoverText", new Boolean(true));
		preferences.put("hoverLine", new Boolean(true));
	}

	public void setFloatPreference(String key, float val) {
		Object obj = new Float(val);
		preferences.put(key, obj);
	}

	public void setBoolPreference(String key, boolean val) {
		Object obj = new Boolean(val);
		preferences.put(key, obj);
	}

	public boolean getBoolPreference(String key) {
		Object obj = preferences.get(key);
		if ((obj != null) && (obj instanceof Boolean)) {
			return ((Boolean) obj).booleanValue();
		}
		return false;
	}

	public float getFloatPreference(String key) {
		Object obj = preferences.get(key);
		if ((obj != null) && (obj instanceof Float)) {
			return ((Float) obj).floatValue();
		}
		return 0.0f;
	}

	public String getObservationLabel(int i) {
		String label = model.getRecordLabel(i);
		return label;
	}

	public Shape getShapeAt(int i) {

		Shape shp = new Rectangle2D.Float(0, 0, 0, 0);
		return shp;
	}

	public int[] pickAll(Rectangle2D hitBox) {

		int[] selObs = null;// ls.findSelection(hitBox);
		return selObs;
	}

	// end excentric labeling stuff
	/**
	 * adds an IndicationListener
	 */
	public void addIndicationListener(IndicationListener l) {
		listenerList.add(IndicationListener.class, l);
	}

	/**
	 * removes an IndicationListener from the component
	 */
	public void removeIndicationListener(IndicationListener l) {
		listenerList.remove(IndicationListener.class, l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireIndicationChanged(int newIndication) {

		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		IndicationEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == IndicationListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new IndicationEvent(this, newIndication);
				}
				((IndicationListener) listeners[i + 1]).indicationChanged(e);
			}
		}// next i

	}

	/**
	 * adds an SelectionListener
	 */
	public void addSelectionListener(SelectionListener l) {
		listenerList.add(SelectionListener.class, l);
	}

	/**
	 * removes an SelectionListener from the component
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
	public void fireSelectionChanged() {

		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		SelectionEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == SelectionListener.class) {
				// Lazily create the event:
				if (e == null) {
					int[] newSelection = findSelection();
					e = new SelectionEvent(this, newSelection);
				}
				((SelectionListener) listeners[i + 1]).selectionChanged(e);
			}
		}// next i

	}

	public Color getIndicationColor() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isSelectionBlur() {
		return useSelectionBlur;
	}

	public boolean isSelectionFade() {
		// BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();
		// return pui.useSelectionFade;
		return true;
	}

	public void setIndicationColor(Color indColor) {
		// TODO Auto-generated method stub

	}

	public void useMultiIndication(boolean useMultiIndic) {
		// TODO Auto-generated method stub

	}

	public void useSelectionBlur(boolean selBlur) {
		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();
		if (selBlur != pui.getRenderThread().useSelectionBlur) {

			pui.getRenderThread().useSelectionBlur = selBlur;
			deepRepaint = true;

			repaint();
		}
	}

	public void useSelectionFade(boolean selFade) {

		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();

		if (selFade != pui.useSelectionFade) {
			pui.useSelectionFade = selFade;
			deepRepaint = true;
			repaint();
		}

	}

	public Component renderingComponent() {
		return this;
	}

	public Shape reportShape() {
		if (indication < 0) {
			return EmptyShape.INSTANCE;
		}

		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();
		GeneralPath path = pui.assemblePath(indication, 0, 1, this);

		AffineTransform xForm = new AffineTransform();
		xForm.translate(0, 37);
		Shape transPath = path.createTransformedShape(xForm);
		return transPath;
	}

	public void processCustomCheckBox(boolean value, String text) {
		// TODO Auto-generated method stub

	}

	public boolean isSelectionOutline() {
		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();
		return pui.selectionOutline;
	}

	public void useSelectionOutline(boolean selOutline) {

		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();

		// if (selOutline != pui.selectionOutline) {
		pui.selectionOutline = selOutline;
		deepRepaint = true;
		repaint();
		// }
	}

	public int getSelectionLineWidth() {
		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();
		return pui.selectionWidth;
	}

	public void setSelectionLineWidth(int width) {
		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();
		if (width != pui.selectionWidth) {
			pui.setSelectionWidth(width);
			deepRepaint = true;
			repaint();
		}

	}

	public Color getSelectionColor() {
		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();
		return pui.selectionColor;
	}

	public void setSelectionColor(Color selColor) {
		BasicParallelDisplayUI pui = (BasicParallelDisplayUI) getUI();

		pui.selectionColor = selColor;
		deepRepaint = true;
		repaint();

	}
}
