package geovista.geoviz.scatterplot;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: </p>
 * @author unascribed
 * @author
 *
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.classification.ClassifierPicker;
import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.event.VariableSelectionEvent;
import geovista.common.event.VariableSelectionListener;
import geovista.common.ui.ShapeReporter;
import geovista.common.ui.VisualSettingsPopupListener;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.symbolization.BivariateColorSchemeVisualizer;
import geovista.symbolization.BivariateColorSymbolClassification;
import geovista.symbolization.BivariateColorSymbolClassificationSimple;
import geovista.symbolization.ColorRampPicker;
import geovista.symbolization.ColorSymbolClassification;
import geovista.symbolization.event.ColorClassifierEvent;
import geovista.symbolization.event.ColorClassifierListener;

public class SingleScatterPlot extends JPanel implements DataSetListener,
		ActionListener, ColorClassifierListener, SelectionListener,
		IndicationListener, VariableSelectionListener, TableModelListener,
		SubspaceListener, VisualSettingsPopupListener, ShapeReporter {

	public static final int VARIABLE_CHOOSER_MODE_ACTIVE = 0;
	public static final int VARIABLE_CHOOSER_MODE_FIXED = 1;
	public static final int VARIABLE_CHOOSER_MODE_HIDDEN = 2;
	private final ScatterPlot scatterPlot;
	private final int[] displayIndices = new int[2];
	private final VisualClassifier visClassX;
	private final VisualClassifier visClassY;
	private final JPanel topContent;
	private final BivariateColorSchemeVisualizer biViz;
	transient private Color backgroundColor;
	transient private int[] selections;
	int[] savedSelection;
	transient private Color[] multipleSelectionColors;
	transient private Color[] colorArrays;

	Logger logger = Logger.getLogger(SingleScatterPlot.class.getName());
	DataSetForApps dataSet;

	public SingleScatterPlot() {
		super();

		JPanel vcPanel = new JPanel();
		vcPanel.setLayout(new BoxLayout(vcPanel, BoxLayout.Y_AXIS));
		visClassX = new VisualClassifier();
		visClassY = new VisualClassifier();
		visClassX.setAlignmentX(Component.LEFT_ALIGNMENT);
		visClassY.setAlignmentX(Component.LEFT_ALIGNMENT);
		visClassX
				.setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
		visClassY
				.setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
		visClassX.addActionListener(this);
		visClassY.addActionListener(this);

		vcPanel.add(visClassY);
		vcPanel.add(visClassX);
		visClassY.setPalette("Greens");
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
		setLayout(new BorderLayout());
		this.add(topContent, BorderLayout.NORTH);
		scatterPlot = new ScatterPlot();
		scatterPlot.setRegressionClass(LinearRegression.class);
		scatterPlot.addActionListener(this);
		this.add(scatterPlot, BorderLayout.CENTER);
		visClassX.addColorClassifierListener(this);
		visClassY.addColorClassifierListener(this);

		visClassY.setHighColor(ColorRampPicker.DEFAULT_HIGH_COLOR_GREEN); // green
		visClassX.getClassPick().setNClasses(4);
		visClassY.getClassPick().setNClasses(4);
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

		visClassX.setDataSet(data);
		visClassY.setDataSet(data);

		// set default data to get color from

		scatterPlot.setBackground(Color.white);
		// XXX is this OK?
		scatterPlot.setRegressionClass(LinearRegression.class);

		scatterPlot.setDataSet(data);

		selections = new int[data.getNumObservations()];

		if (data.getNumberNumericAttributes() == 1) {
			displayIndices[0] = 0;
			setXVariable(displayIndices[0]);

			displayIndices[1] = 0;
			setYVariable(displayIndices[0]);
		}
		if (data.getNumberNumericAttributes() >= 2) {
			displayIndices[0] = 0;
			setXVariable(displayIndices[0]);

			displayIndices[1] = 1;
			setYVariable(displayIndices[1]);
		}

		scatterPlot.setAxisOn(true);
		scatterPlot.setDataIndices(displayIndices);
	}

	public void setXVariable(int var) {
		visClassX.setCurrVariableIndex(var);
	}

	public void setYVariable(int var) {
		visClassY.setCurrVariableIndex(var);
	}

	public void setBivarColorClasser(
			BivariateColorSymbolClassification bivarColorClasser) {
		scatterPlot.setBivarColorClasser(bivarColorClasser, false);
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
		scatterPlot.setRegressionClassName(classname);
	}

	public void colorClassifierChanged(ColorClassifierEvent e) {
		if (e.getSource() == visClassX) {
			e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_X);
		} else if (e.getSource() == visClassY) {
			e.setOrientation(ColorClassifierEvent.SOURCE_ORIENTATION_Y);
		}

		biViz.colorClassifierChanged(e);

		if (scatterPlot == null) {
			return;
		}

		ColorSymbolClassification colorSymbolizerX = visClassX
				.getColorClasser();
		ColorSymbolClassification colorSymbolizerY = visClassY
				.getColorClasser();

		BivariateColorSymbolClassificationSimple biColorSymbolizer = new BivariateColorSymbolClassificationSimple();

		biColorSymbolizer.setClasserX(colorSymbolizerX.getClasser());
		biColorSymbolizer.setColorerX(colorSymbolizerX.getColorer());

		biColorSymbolizer.setClasserY(colorSymbolizerY.getClasser());
		biColorSymbolizer.setColorerY(colorSymbolizerY.getColorer());
		scatterPlot.setBivarColorClasser(biColorSymbolizer, false);
	}

	public void dataSetChanged(DataSetEvent e) {
		dataSet = e.getDataSetForApps();
		setDataSet(dataSet);

	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		String command = e.getActionCommand();
		String varChangedCommand = ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED;
		// String colorChangedCommand = VisualClassifier.COMMAND_COLORS_CHANGED;

		if ((src == visClassX) && command.equals(varChangedCommand)) {
			int index = visClassX.getCurrVariableIndex();

			displayIndices[0] = index;
			scatterPlot.setDataIndices(displayIndices);
		} else if ((src == visClassY) && command.equals(varChangedCommand)) {
			int index = visClassY.getCurrVariableIndex();

			displayIndices[1] = index;
			scatterPlot.setDataIndices(displayIndices);
		} else if ((src == scatterPlot)
				&& command.compareTo(ScatterPlotBasic.COMMAND_POINT_SELECTED) == 0) {
			selections = scatterPlot.getSelections();
			int[] selObs = getSelectedObvs();
			if (selObs.length == dataSet.getNumObservations()) {
				selObs = new int[0];
			}
			fireSelectionChanged(selObs);
		} else {
			logger.info("unsupported command " + command);
		}
	}

	public Color[] getColors() {
		return colorArrays;
	}

	/**
	 * adds an IndicationListener to the button
	 */
	public void addIndicationListener(IndicationListener l) {
		scatterPlot.addIndicationListener(l);
	}

	/**
	 * removes an IndicationListener from the button
	 */
	public void removeIndicationListener(IndicationListener l) {
		scatterPlot.removeIndicationListener(l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	/*
	 * private void fireColorArrayChanged() { // Guaranteed to return a non-null
	 * array Object[] listeners = listenerList.getListenerList();
	 * ColorArrayEvent e = null;
	 * 
	 * // Process the listeners last to first, notifying // those that are
	 * interested in this event for (int i = listeners.length - 2; i >= 0; i -=
	 * 2) { if (listeners[i] == ColorArrayListener.class) { // Lazily create the
	 * event: if (e == null) { e = new ColorArrayEvent(this, getColors()); }
	 * ((ColorArrayListener) listeners[i + 1]).colorArrayChanged(e); } } // next
	 * i }
	 */
	public void setSelectedObvs(int[] selected) {

		if (selected == null || selections == null) {
			return;
		}
		for (int i = 0; i < selections.length; i++) {
			selections[i] = 0;
		}
		for (int i = 0; i < selected.length; i++) {
			selections[selected[i]] = 1;
		}
		if (selected.length == 0) {
			for (int i = 0; i < selections.length; i++) {
				selections[i] = 1;
			}
		}
		multipleSelectionColors = null;
		scatterPlot.setSelections(selections);
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
		scatterPlot.setMultipleSelectionColors(this.multipleSelectionColors);
	}

	public void selectionChanged(SelectionEvent e) {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("sending to selectionchanged");
		}
		if (e.getMultipleSlectionColors() != null) {
			setMultipleSelectionColors(e.getMultipleSlectionColors());
		} else {
			setSelectedObvs(e.getSelection());

			scatterPlot.repaint();
		}
		savedSelection = e.getSelection();
	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, savedSelection);
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

	// /**
	// * implements ColorClassifierListener
	// */
	// public void addColorClassifierListener(ColorClassifierListener l) {
	// this.visClassOne.addColorClassifierListener(l);
	// this.visClassTwo.addColorClassifierListener(l);
	//
	// }

	// /**
	// * removes an ColorClassifierListener from the component
	// */
	// public void removeColorClassifierListener(ColorClassifierListener l) {
	// this.visClassOne.removeColorClassifierListener(l);
	// this.visClassTwo.removeColorClassifierListener(l);
	// }

	public void indicationChanged(IndicationEvent e) {
		scatterPlot.indicationChanged(e);
	}

	/*
	 * public void colorArrayChanged(ColorArrayEvent e) { Color[] colors =
	 * e.getColors(); scatterPlot.setColorArrayForObs(colors); }
	 */
	public void variableSelectionChanged(VariableSelectionEvent e) {
		visClassX.setCurrVariableIndex(e.getVariableIndex() + 1);
	}

	public void tableChanged(TableModelEvent e) {
		visClassX.setDataSet(dataSet);
		visClassY.setDataSet(dataSet);

	}

	public void subspaceChanged(SubspaceEvent e) {
		int[] vars = e.getSubspace();
		visClassX.getClassPick().setCurrVariableIndex(vars[1]);
		visClassY.getClassPick().setCurrVariableIndex(vars[0]);
		displayIndices[0] = vars[1];
		displayIndices[1] = vars[0];
		scatterPlot.setDataIndices(displayIndices);

	}

	public Color getIndicationColor() {
		return scatterPlot.getIndicationColor();
	}

	public Color getSelectionColor() {
		return scatterPlot.getSelectionColor();
	}

	public boolean isSelectionBlur() {
		return scatterPlot.isSelectionBlur();
	}

	public boolean isSelectionFade() {
		return scatterPlot.isSelectionFade();
	}

	public void setIndicationColor(Color indColor) {
		scatterPlot.setIndicationColor(indColor);

	}

	public void setSelectionColor(Color selColor) {
		scatterPlot.setSelectionColor(selColor);

	}

	public void useMultiIndication(boolean useMultiIndic) {
		scatterPlot.useMultiIndication(useMultiIndic);

	}

	public void useSelectionBlur(boolean selBlur) {
		scatterPlot.useSelectionBlur(selBlur);

	}

	public void useSelectionFade(boolean selFade) {
		scatterPlot.useSelectionFade(selFade);

	}

	public Component renderingComponent() {

		return scatterPlot;
	}

	public Shape reportShape() {

		return scatterPlot.reportShape();
	}

	public void processCustomCheckBox(boolean value, String text) {
		// TODO Auto-generated method stub

	}

	public boolean isSelectionOutline() {
		return scatterPlot.isSelectionOutline();
	}

	public void useSelectionOutline(boolean selOutline) {
		scatterPlot.useSelectionOutline(selOutline);

	}

	public int getSelectionLineWidth() {
		return scatterPlot.getSelectionLineWidth();
	}

	public void setSelectionLineWidth(int width) {
		scatterPlot.setSelectionLineWidth(width);

	}

}
