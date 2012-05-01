package geovista.geoviz.scatterplot;

/**
 * Title: SingleHistogram
 * @author Frank Hardisty
 * 
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.BitSet;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.ui.ShapeReporter;

public class SingleHistogram extends JPanel implements DataSetListener,
	SelectionListener, ActionListener, ChangeListener, TableModelListener,
	IndicationListener, SubspaceListener, ShapeReporter {
    Histogram histo;
    JSlider binSlider;
    JLabel nBins;
    JComboBox variableCombo;
    DataSetForApps dataSet;
    JPanel bottomPanel;
    protected final static Logger logger = Logger
	    .getLogger(SingleHistogram.class.getName());

    public SingleHistogram() {
	setPreferredSize(new Dimension(450, 300));
	histo = new Histogram();
	bottomPanel = new JPanel();
	binSlider = new JSlider();

	binSlider.setMinimum(3);
	binSlider.setMaximum(100);
	nBins = new JLabel(new Integer(histo.getHistNumber()).toString());
	binSlider.setValue(histo.getHistNumber());
	binSlider.addChangeListener(this);
	JLabel sliderLabel = new JLabel("Max N Bins:");
	variableCombo = new JComboBox();
	variableCombo.addActionListener(this);
	bottomPanel.add(variableCombo);
	bottomPanel.add(sliderLabel);
	bottomPanel.add(binSlider);
	bottomPanel.add(nBins);
	setLayout(new BorderLayout());
	this.add(histo, BorderLayout.CENTER);
	this.add(bottomPanel, BorderLayout.SOUTH);
	histo.addSelectionListener(this);

    }

    public void removeBottomPanel() {
	this.remove(bottomPanel);
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

    public void dataSetChanged(DataSetEvent e) {
	if (histo == null) {
	    return;
	}
	dataSet = e.getDataSetForApps();

	histo.dataSetChanged(e);
	variableCombo.removeActionListener(this);
	variableCombo.removeAllItems();
	for (int i = 0; i < dataSet.getNumberNumericAttributes(); i++) {
	    variableCombo.addItem(dataSet.getNumericArrayName(i));
	}
	variableCombo.addActionListener(this);
	histo.selectionChanged(new SelectionEvent());
    }

    public void selectionChanged(SelectionEvent e) {
	if (e.getSource() == histo) {
	    fireSelectionChanged(e.getSelection());
	} else {
	    histo.selectionChanged(e);
	}
    }

    public SelectionEvent getSelectionEvent() {
	return new SelectionEvent(this, histo.savedSelection);
    }

    public void setSelection(int[] sel) {
	SelectionEvent e = new SelectionEvent(this, sel);
	histo.selectionChanged(e);
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource().equals(variableCombo)) {
	    String name = (String) variableCombo.getSelectedItem();
	    histo.setVariableName(name);

	    double[] data = dataSet.getNumericDataAsDouble(variableCombo
		    .getSelectedIndex());
	    BitSet selections = (BitSet) histo.getSelections().clone();
	    histo.setData(data);
	    histo.setSelections(selections);
	}

    }

    public void setSelectedVariable(int var) {
	variableCombo.setSelectedIndex(var);
    }

    public void stateChanged(ChangeEvent e) {
	if (nBins == null || dataSet == null) {
	    return;
	}

	if (e.getSource().equals(binSlider)) {
	    BitSet selections = (BitSet) histo.getSelections().clone();
	    Integer nBins = binSlider.getValue();

	    this.nBins.setText(nBins.toString());
	    // if (!this.binSlider.getValueIsAdjusting()){
	    histo.setHistNumber(nBins);
	    histo.setSelections(selections);
	    histo.repaint();
	    // }
	}

    }

    public void tableChanged(TableModelEvent e) {
	variableCombo.removeActionListener(this);
	variableCombo.removeAllItems();
	for (int i = 0; i < dataSet.getNumberNumericAttributes(); i++) {
	    variableCombo.addItem(dataSet.getNumericArrayName(i));
	}
	variableCombo.addActionListener(this);

    }

    public void indicationChanged(IndicationEvent e) {
	histo.indicationChanged(e);

    }

    public void subspaceChanged(SubspaceEvent e) {
	int[] vars = e.getSubspace();
	setSelectedVariable(vars[0]);

    }

    public Component renderingComponent() {
	return histo;
    }

    public Shape reportShape() {

	return histo.reportShape();
    }

    public static void main2(String[] args) {
	int n = 1000;
	double[] bigData = new double[n];
	double[] bigData2 = new double[n];
	String[] names = { "one", "two" };

	for (int i = 0; i < n; i++) {
	    bigData[i] = Math.random() * 100;
	    bigData2[i] = Math.random() * 100;
	}

	int m = 10;
	long start = System.currentTimeMillis();

	for (int i = 0; i < m; i++) {
	    DescriptiveStatistics.mean(bigData);
	}

	long end = System.currentTimeMillis();

	logger.finest("mean: that took: " + (end - start) / (float) m);

	start = System.currentTimeMillis();

	for (int i = 0; i < m; i++) {
	    // DescriptiveStatistics.fineMean(bigData);
	}

	end = System.currentTimeMillis();

	logger.finest("fine mean: that took: " + (end - start) / (float) m);

	start = System.currentTimeMillis();

	for (int i = 0; i < m; i++) {
	    // DescriptiveStatistics.stdDev(bigData, false);
	}

	end = System.currentTimeMillis();

	logger.finest("stdDev: that took: " + (end - start) / (float) m);

	Object[] dataObject = { names, bigData, bigData2 };

	DataSetForApps data = new DataSetForApps(dataObject);

	JFrame app = new JFrame("test big data");

	SingleHistogram single = new SingleHistogram();
	DataSetEvent e = new DataSetEvent(data, app);
	single.dataSetChanged(e);

	app.add(single);
	app.pack();
	app.setVisible(true);
	// single.setVariableName("one");
	// single.setData(bigData);
	app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public static void main3(@SuppressWarnings("unused") String[] args) {
	// int N = Integer.parseInt(args[0]);

	int N = 5000;
	long start, stop, elapsed;
	double freq;

	logger.finest("Nanoseconds per operation");

	/***********************************************************************
	 * empty loop
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    for (int j = 1; j <= N; j++) {

	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Empty loop:" + "\t" + freq);

	/***********************************************************************
	 * addition
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Addition:" + "\t" + freq);

	/***********************************************************************
	 * multiplication
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Multiply:" + "\t" + freq);

	/***********************************************************************
	 * comparison
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Boolean:" + "\t" + freq);

	/***********************************************************************
	 * remainder
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Remainder:" + "\t" + freq);

	/***********************************************************************
	 * division
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Division:" + "\t" + freq);

	/***********************************************************************
	 * floating point add
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    @SuppressWarnings("unused")
	    double fi = i;
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Float Add:" + "\t" + freq);

	/***********************************************************************
	 * floating point division
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    @SuppressWarnings("unused")
	    double fi = i;
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Float Division:" + "\t" + freq);

	/***********************************************************************
	 * floating point multiply
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    @SuppressWarnings("unused")
	    double fi = i;
	    for (int j = 1; j <= N; j++) {
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Float Multiply:" + "\t" + freq);

	/***********************************************************************
	 * Empty integer function call
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    @SuppressWarnings("unused")
	    int k;
	    for (int j = 1; j <= N; j++) {
		k = 0;
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Function call:" + "\t" + freq);

	/***********************************************************************
	 * Math.sin
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    @SuppressWarnings("unused")
	    double k;
	    for (int j = 1; j <= N; j++) {
		k = Math.sin(i + j);
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Math.sin:" + "\t" + freq);

	/***********************************************************************
	 * Math.atan2
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    @SuppressWarnings("unused")
	    double k;
	    for (int j = 1; j <= N; j++) {
		k = Math.atan2(i, j);
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Math.atan2:" + "\t" + freq);

	/***********************************************************************
	 * Math.random
	 **********************************************************************/
	start = System.currentTimeMillis();
	for (int i = 1; i <= N; i++) {
	    @SuppressWarnings("unused")
	    double k;
	    for (int j = 1; j <= N; j++) {
		k = Math.random();
	    }
	}
	stop = System.currentTimeMillis();
	elapsed = stop - start;
	freq = 1.0E6 * elapsed / N / N;
	logger.finest("Math.random:" + "\t" + freq);

    }

    public static void main(String[] args) {
	JFrame app = new JFrame();
	app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	SingleHistogram single = new SingleHistogram();

	app.add(single);
	app.pack();
	app.setVisible(true);

    }

}
