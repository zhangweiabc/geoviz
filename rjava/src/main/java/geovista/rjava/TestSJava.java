/* -------------------------------------------------------------------
 Java source file for the class TestSJava
 Copyright (c), 2004, Frank Hardisty
 */
package geovista.rjava;

import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.coordination.CoordinationManager;
import geovista.geoviz.map.GeoMap;
import geovista.geoviz.parvis.ParallelPlot;

/**
 * A class for testing the SJava link between R and Java. See:
 * http://www.omegahat.org/RSJava/
 */

public class TestSJava {

	JPanel pan = null;
	private transient final EventListenerList listenerList;

	// ListenerList listener;
	CoordinationManager cm;
	GeoMap map;
	ParallelPlot pcp;

	// DataSetForApps dataSet;
	Object[] rawData;
	String[] varNames;
	boolean showGUI = true;
	int[] selection;
	protected final static Logger logger = Logger.getLogger(TestSJava.class
			.getName());

	public TestSJava() {
		listenerList = new EventListenerList();
		cm = new CoordinationManager();
		logger.finest("made cm");

		if (showGUI) {
			pcp = new ParallelPlot();
			cm.addBean(this);
			cm.addBean(pcp);
			// map = new GeoMap();
			JFrame app = new JFrame();

			app.getContentPane().add(pcp);
			app.pack();
			app.setVisible(true);
		}
	}

	public String sayHi() {
		return "Hi World!";

	}

	public String echoStringArray(Object stringObj) {
		String[] strings = null;
		try {
			strings = (String[]) stringObj;
		} catch (ClassCastException cce) {
			return "ClassCastException thrown " + cce.getMessage();
		}
		String returnString = "";
		for (String string : strings) {
			returnString = returnString + "," + string;
		}
		return returnString;
	}

	public String echoInt(int aNum) {
		return "" + aNum;
	}

	public String echoDouble(double aDouble) {
		return "" + aDouble;
	}

	public void toStdOut(String message) {
		// let's see if we can thrrow an exception first
		// int[] myStuff = {1,2};
		// myStuff[78] = 45;//throw it!

		logger.finest(message);
	}

	public String echoObjectArray(Object obj) {
		Object[] objs = null;
		try {
			objs = (Object[]) obj;
		} catch (ClassCastException cce) {
			return "ClassCastException thrown " + cce.getMessage();
		}

		return "krunk is in a bad mood" + objs.length;
	}

	public String echoIntArray(int[] ints) {
		String returnString = "";
		for (int j : ints) {
			returnString = returnString + j;
		}
		return returnString;
	}

	public int[] getRandomIntArray() {
		int[] ints = { 1, 2, 3, 4, 5 };
		return ints;

	}

	public int[] getNewSelection() {
		return selection;
	}

	public String getIntArrayLen(int[] ints) {
		String returnString = "";
		returnString = returnString + ints.length;
		return returnString;
	}

	public String dataFrameToDataSet(Object dataFrame) {
		// can't do the following line, crashes jvm when called from SJava
		// return dataFrame.getClass().getName();
		logger.info(dataFrame.getClass().getName());
		return "wha happen?";
	}

	public String coordinateObject(Object obj) {
		cm.addBean(obj);
		return "TRUE";
	}

	public String initData(int nColumns) {
		rawData = new Object[nColumns + 1];
		logger.finest("setting size of data = " + nColumns);
		return "" + nColumns;
	}

	public String setVariableNames(String[] varNames) {
		if (rawData == null) {
			rawData = new Object[varNames.length];
		}
		this.varNames = varNames;
		String message = "TestSJava, setVariableNames, number of names = "
				+ varNames.length;
		logger.finest(message);
		rawData[0] = varNames;
		return "TRUE";
	}

	// public String setRowNames(String[] rowNames) {
	// this.varNames = varNames;
	// String message = "TestSJava, setRowNames, number of rows = " +
	// rowNames.length;

	// rawData[0] = varNames;
	// return "TRUE";
	//
	// }

	public String addData(Object data, int index) {

		try {
			rawData[index] = data;
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			return "index out of bounds thrown";
		} catch (NullPointerException nulle) {
			nulle.printStackTrace();
			return "null pointer exception thrown";
		}
		String message = "TestSJava, addData, index = " + index;
		logger.finest(message);
		return "TRUE";
	}

	public String fireDataSetChanged() {
		logger.finest("entering fireDataSetChanged()");
		if (rawData == null) {
			String message = "TestSJava, fireDataSetChanged called while data is null";
			logger.finest(message);
			return message;
		}
		DataSetForApps dataSet = new DataSetForApps(rawData);
		this.fireDataSetChanged(dataSet);
		String message = "TestSJava, fireDataSetChanged called, ok";
		logger.finest(message);
		return "TRUE";
	}

	/**
	 * adds an DataSetListener
	 */
	public void addDataSetListener(DataSetListener l) {
		listenerList.add(DataSetListener.class, l);
	}

	/**
	 * removes an DataSetListener from the component
	 */
	public void removeDataSetListener(DataSetListener l) {
		listenerList.remove(DataSetListener.class, l);

	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	protected void fireDataSetChanged(DataSetForApps dataSet) {
		logger.finest("ShpToShp.fireDataSetChanged, Hi!!");
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		DataSetEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == DataSetListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new DataSetEvent(dataSet, this);

				}
				((DataSetListener) listeners[i + 1]).dataSetChanged(e);
			}
		}
	}

	/**
	 * Main method for testing.
	 */
	public static void main(String[] args) {
		Integer[] someIntegers = new Integer[3];
		logger.finest(someIntegers.getClass().getName());

		int[] someInts = { 1, 2, 3 };
		logger.finest(someInts.getClass().getName());

		TestSJava sjav = new TestSJava();

		logger.finest(sjav.sayHi());

	}

	public int[] getSelection() {
		return selection;
	}

	public void setSelection(int[] selection) {
		this.selection = selection;
	}

}
