package geovista.geoviz.condition;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company: GeoVISTA
 * @author Xiping
 * 
 */

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import geovista.common.data.DataSetForApps;
import geovista.common.event.ConditioningEvent;
import geovista.common.event.ConditioningListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;

public class ConditionManager extends JPanel implements ChangeListener,
		ListSelectionListener, DataSetListener {

	protected final static Logger logger = Logger
			.getLogger(ConditionManager.class.getName());
	private Object[] dataObject;
	private double[][] doubleDataArrays = null;
	private double[] dataConditionedOn;
	private Object[] datasConditioned;
	private String[] attributesArray = null;
	private String[] attributesDisplay;
	private int condVarIndex;
	private double[] conditionRanges;
	private int[] conditionResults;
	private JList attList;
	private JScrollPane attSelectPane;
	private JComboBox conditionMethod;
	private String[] method;
	private String methodName;
	private JLabel conditionSetLabel;
	private static String conditionSetStr = "Set Conditions:";
	private JTextField[] conditionSetField;
	private OneVarCondition oneVarCond;
	private OneVarCondExtremes oneVarCondEx;
	private MultiVarCondition multiVarCond;
	private MultiVarConditionEx multiVarCondEx;
	private double[] minMaxValues;
	private final Object[] rangesTitle = new Object[2];
	private Vector rangesTitleVector = new Vector();
	private final JLabel varMin = new JLabel();
	private final JLabel varMax = new JLabel();
	private final EventListenerList listenerListAction = new EventListenerList();
	private geovista.common.data.DataSetForApps dataObjTransfer;

	public ConditionManager() {
		super();
		setLayout(new BorderLayout());
		this.setSize(300, 300);
		init();
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
	public void setDataObject(Object[] data) {
		setDataSet(new DataSetForApps(data));

	}

	public void setDataSet(DataSetForApps data) {

		dataObjTransfer = data;
		dataObject = dataObjTransfer.getDataSetNumericAndSpatial();// XXX bad?
		attributesDisplay = dataObjTransfer.getAttributeNamesNumeric();

		initList();
		rangesTitle[0] = attributesDisplay[0];
		fireChangeEvent();
	}

	public void setDoubleDataArrays(double[][] dataArrays) {
		doubleDataArrays = dataArrays.clone();
		if (attributesArray != null
				&& doubleDataArrays.length == attributesArray.length) {
			initList();
		}
	}

	public void setAttributesArray(String[] attributesArray) {
		this.attributesArray = attributesArray.clone();
		if (doubleDataArrays != null
				&& doubleDataArrays.length == this.attributesArray.length) {
			initList();
		}
	}

	public void setConditionRanges(double[] ranges) {
		conditionRanges = ranges;
		if (methodName.equals(method[0])) {
			oneVarCond = new OneVarCondition();
			oneVarCond.setDataArray(dataConditionedOn);
			oneVarCond.setConditionRanges(conditionRanges);
			conditionSetField[0].setText(Double.toString(conditionRanges[0]));
			conditionSetField[1].setText(Double.toString(conditionRanges[1]));
			conditionResults = (oneVarCond.getConditionResults()).clone();

			fireActionPerformed();
			fireConditioningChanged(conditionResults);
		} else if (methodName == method[1]) {
			oneVarCondEx = new OneVarCondExtremes();
			oneVarCondEx.setDataArray(dataConditionedOn);
			oneVarCondEx.setConditionRanges(conditionRanges);
			conditionSetField[0].setText(Double.toString(conditionRanges[0]));
			conditionSetField[1].setText(Double.toString(conditionRanges[1]));
			conditionResults = (oneVarCondEx.getConditionResults()).clone();

			fireActionPerformed();
			fireConditioningChanged(conditionResults);

			// } else {
		}
	}

	public void setConditionRanges(Object[] ranges) {
		if (methodName.equals(method[0])) {

			multiVarCond = new MultiVarCondition();
			multiVarCond.setDataArray(datasConditioned);
			multiVarCond.setConditionRanges(ranges);
			conditionSetField[0].setText(Double
					.toString(((double[]) ranges[0])[0]));
			conditionSetField[1].setText(Double
					.toString(((double[]) ranges[0])[1]));
			conditionResults = (multiVarCond.getConditionResults()).clone();

			fireActionPerformed();
			fireConditioningChanged(conditionResults);
		} else if (methodName == method[1]) {
			multiVarCondEx = new MultiVarConditionEx();
			multiVarCondEx.setDataArray(datasConditioned);
			multiVarCondEx.setConditionRanges(ranges);
			conditionSetField[0].setText(Double
					.toString(((double[]) ranges[0])[0]));
			conditionSetField[1].setText(Double
					.toString(((double[]) ranges[0])[1]));
			conditionResults = (multiVarCondEx.getConditionResults()).clone();

			fireActionPerformed();
			fireConditioningChanged(conditionResults);

			// } else {
		}
	}

	public double getMinimumRanges() {
		return minMaxValues[0];
	}

	public double getMaximumRanges() {
		return minMaxValues[1];
	}

	public double[] getSliderRanges() {
		logger.finest("get ranges for slider...");
		return minMaxValues;
	}

	public String getAttributeName() {
		// return this.attributesArray[condVarIndex];
		return attributesDisplay[condVarIndex - 1];
	}

	public Object[] getRangesTitle() {
		logger.finest("get rangesTitle for slider...");
		return rangesTitle;
	}

	public int[] getConditionResults() {
		logger.finest("get conditioning in conditionManager");
		return conditionResults;
	}

	public void setConditionResults(int[] conditionResults) {
		this.conditionResults = conditionResults;
	}

	void init() {
		attList = new JList();
		attList
				.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		attList.addListSelectionListener(this);

		attSelectPane = new JScrollPane(attList);
		attSelectPane.setViewportView(attList);

		method = new String[] { "Focus", "Extremes", "Display All" };
		conditionMethod = new JComboBox(method);
		conditionMethod.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				methodName = (String) cb.getSelectedItem();
				// if (attList.getSelectedIndices().length == 1){
				// setConditionRanges(conditionRanges);
				// }else{
				// setConditionRangesVector(ranges);
				// }
			}
		});
		conditionMethod.setSelectedIndex(0);

		JButton okButton;
		JButton sliderButton;
		okButton = new JButton("OK");

		sliderButton = new JButton("Sliders"); // bring up sliders for adjust
		// conditioning ranage.

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(okButton);
		buttonPanel.add(sliderButton);

		conditionSetLabel = new JLabel(conditionSetStr);

		varMin.setText("unknown");
		varMax.setText("unknown");

		conditionSetField = new JTextField[2];
		conditionSetField[0] = new JTextField(8);
		conditionSetField[1] = new JTextField(8);
		// end loop

		JPanel attPanel = new JPanel(new BorderLayout());
		attPanel.add(new JLabel("Attribute Names:"), BorderLayout.NORTH);
		attPanel.add(attSelectPane, BorderLayout.CENTER);

		JPanel comboPanel = new JPanel(new BorderLayout());
		comboPanel.add(new JLabel("ConditionMethods:"), BorderLayout.NORTH);
		JPanel tmpComboPanel = new JPanel(new BorderLayout());
		tmpComboPanel.add(conditionMethod, BorderLayout.NORTH);
		comboPanel.add(tmpComboPanel, BorderLayout.CENTER);

		JPanel conditionPanel = new JPanel(new BorderLayout());
		JPanel conditionField = new JPanel(new GridLayout(1, 4));
		conditionField.add(varMin); // loop
		conditionField.add(conditionSetField[0]);
		conditionField.add(conditionSetField[1]);
		conditionField.add(varMax);

		conditionPanel.add(conditionSetLabel, BorderLayout.NORTH);
		conditionPanel.add(conditionField, BorderLayout.CENTER);
		conditionPanel.add(buttonPanel, BorderLayout.SOUTH);

		this.add(attPanel, BorderLayout.CENTER);
		this.add(comboPanel, BorderLayout.EAST);
		this.add(conditionPanel, BorderLayout.SOUTH);

		validate();
	}

	void initList() {
		int observLen = 0;
		observLen = dataObjTransfer.getNumObservations();
		conditionResults = new int[observLen];

		// this.conditionResults = new int[this.attributesArray.length];
		// //mistake. should be observations' #.
		if (attList == null) {
			// this.attList = new JList(attributesArray);
			attList = new JList(attributesDisplay);
		} else {
			// this.attList.setListData(attributesArray);
			attList.setListData(attributesDisplay);
		}
		// this.condVarIndex = 0;
		condVarIndex = 1; // first object in dataset is the variable names. So,
		// data begins with 1.
		// this.attList.setSelectedIndex(condVarIndex);
		attList.setSelectedIndex(condVarIndex - 1);
		setMinMaxValues(condVarIndex);
		validate();
	}

	void setMinMaxValues(int index) {
		boolean[] dataBoolean;
		int[] dataInt;
		int len;
		if (dataObject[index] instanceof double[]) {
			dataConditionedOn = (double[]) (dataObject[index]);
		} else if (dataObject[index] instanceof int[]) {
			dataInt = (int[]) dataObject[index];
			len = dataInt.length;
			dataConditionedOn = new double[len];
			for (int i = 0; i < len; i++) {
				dataConditionedOn[i] = dataInt[i];
			}
		} else if (dataObject[index] instanceof boolean[]) {
			dataBoolean = (boolean[]) dataObject[index];
			len = dataBoolean.length;
			dataConditionedOn = new double[len];
			for (int i = 0; i < len; i++) {
				if (dataBoolean[i] == true) {
					dataConditionedOn[i] = 1;
				} else {
					dataConditionedOn[i] = 0;
				}
			}
		}
		minMaxValues = getMinMaxValues(dataConditionedOn);
		rangesTitle[1] = minMaxValues;
		varMin.setText(Double.toString(minMaxValues[0]));
		varMax.setText(Double.toString(minMaxValues[1]));
		conditionSetField[0].setText(Double.toString(minMaxValues[0]));
		conditionSetField[1].setText(Double.toString(minMaxValues[1]));
	}

	@SuppressWarnings("unused")
	private void okButton_actionPerformed() {
		if (methodName.equals(method[0])) {
			oneVarCond = new OneVarCondition();
			oneVarCond.setDataArray(dataConditionedOn);

			// set conditionRanges
			if (conditionRanges == null) {
				conditionRanges = new double[2];
			}
			conditionRanges[0] = Double.parseDouble(conditionSetField[0]
					.getText());
			conditionRanges[1] = Double.parseDouble(conditionSetField[1]
					.getText());
			oneVarCond.setConditionRanges(conditionRanges);
			conditionResults = (oneVarCond.getConditionResults()).clone();
			minMaxValues = conditionRanges;
			rangesTitle[1] = minMaxValues;
			fireChangeEvent();
			fireConditioningChanged(conditionResults);
			fireActionPerformed();
		} else if (methodName == method[1]) {
			oneVarCondEx = new OneVarCondExtremes();
			oneVarCondEx.setDataArray(dataConditionedOn);
			// set conditionRanges
			if (conditionRanges == null) {
				conditionRanges = new double[2];
			}
			conditionRanges[0] = Double.parseDouble(conditionSetField[0]
					.getText());
			conditionRanges[1] = Double.parseDouble(conditionSetField[1]
					.getText());
			oneVarCondEx.setConditionRanges(conditionRanges);
			conditionResults = (oneVarCondEx.getConditionResults()).clone();
			minMaxValues = conditionRanges;
			rangesTitle[1] = minMaxValues;
			fireChangeEvent();
			fireConditioningChanged(conditionResults);
			fireActionPerformed();
		}
		// } else {
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}
		JList theList = (JList) e.getSource();
		if (theList.isSelectionEmpty()) {
			return;
		}
		rangesTitleVector.clear();
		datasConditioned = new Object[theList.getSelectedIndices().length];
		logger.finest("number of var: " + datasConditioned.length);
		// if (theList.getSelectedIndices().length == 1){
		// condVarIndex = theList.getSelectedIndex() + 1; //dataObject[0] is
		// attribute name array.
		// setMinMaxValues(condVarIndex);
		// this.rangesTitle[0] = this.attributesDisplay[condVarIndex - 1];
		// this.rangesTitle[1] = this.minMaxValues;
		// fireChangeEvent ();
		// }else{
		for (int i = 0; i < theList.getSelectedIndices().length; i++) {
			condVarIndex = theList.getSelectedIndices()[i] + 1; // dataObject[0]
			// is attribute
			// name array.
			setMinMaxValues(condVarIndex);
			rangesTitle[0] = attributesDisplay[condVarIndex - 1];
			rangesTitle[1] = minMaxValues;
			datasConditioned[i] = dataConditionedOn.clone();
			rangesTitleVector.add(i, rangesTitle.clone());
		}
		rangesTitleVector.trimToSize();
		fireChangeEvent();
		// }
	}

	private double[] getMinMaxValues(double[] dataArray) {
		double[] minMaxValues = new double[2];
		if (!Double.isNaN(dataArray[0])) {
			minMaxValues[0] = dataArray[0];
			minMaxValues[1] = dataArray[0];
		} else {
			minMaxValues[0] = 0;
			minMaxValues[1] = 0;
		}
		for (int i = 1; i < dataArray.length; i++) {
			if (minMaxValues[0] > dataArray[i] && !Double.isNaN(dataArray[i])) {
				minMaxValues[0] = dataArray[i];
			}
			if (minMaxValues[1] < dataArray[i] && !Double.isNaN(dataArray[i])) {
				minMaxValues[1] = dataArray[i];
			}
		}
		return minMaxValues;
	}

	public void stateChanged(ChangeEvent e) {
	}

	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param l
	 */
	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}

	/**
	 * put your documentation comment here
	 */
	private void fireChangeEvent() {
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ChangeListener.class) {
				((ChangeListener) listeners[i + 1])
						.stateChanged(new ChangeEvent(this));
			}
		} // end for
	}

	// Work with coordinator.
	public void dataSetChanged(DataSetEvent e) {
		setDataObject(e.getDataSet());
	}

	/**
	 * adds an SelectionListener.
	 * 
	 * @see EventListenerList
	 */
	public void addConditioningListener(ConditioningListener l) {

		listenerList.add(ConditioningListener.class, l);
	}

	/**
	 * removes an SelectionListener from the component.
	 * 
	 * @see EventListenerList
	 */
	public void removeConditioningListener(ConditioningListener l) {
		listenerList.remove(ConditioningListener.class, l);

	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireConditioningChanged(int[] condition) {

		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		ConditioningEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ConditioningListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new ConditioningEvent(this, condition);
				}
				((ConditioningListener) listeners[i + 1])
						.conditioningChanged(e);
			}
		}// next i

	}

	/**
	 * adds an ActionListener to the button
	 */
	public void addActionListener(ActionListener l) {
		listenerListAction.add(ActionListener.class, l);
	}

	/**
	 * removes an ActionListener from the button
	 */
	public void removeActionListener(ActionListener l) {
		listenerListAction.remove(ActionListener.class, l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireActionPerformed() {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerListAction.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		ActionEvent e2 = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				"OK");
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				// Lazily create the event:
				((ActionListener) listeners[i + 1]).actionPerformed(e2);
			}
		}
	}

	public Vector getRangesTitleVector() {
		return rangesTitleVector;
	}

	public void setRangesTitleVector(Vector rangesTitleVector) {
		this.rangesTitleVector = rangesTitleVector;
	}
}