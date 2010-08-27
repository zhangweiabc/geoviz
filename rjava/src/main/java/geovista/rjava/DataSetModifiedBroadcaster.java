/* -------------------------------------------------------------------
 Java source file for the class ColumnAppendedBroadcaster
 Copyright (c), 2004, Frank Hardisty
 All Rights Reserved.
 Original Author: Frank Hardisty
 $Author: hardisty $
 $Id: ColumnAppendedBroadcaster.java,v 1.1 2005/02/13 03:26:27 hardisty Exp $
 $Date: 2005/02/13 03:26:27 $
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

package geovista.rjava;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import geovista.common.event.ColumnAppendedEvent;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;

/**
 * This class is able to accept modified data for rebroadcast.
 */
public class DataSetModifiedBroadcaster implements DataSetListener {

	private transient EventListenerList listenerList;

	private transient Object[] newData;
	final static Logger logger = Logger
			.getLogger(DataSetModifiedBroadcaster.class.getName());

	public DataSetModifiedBroadcaster() {
		super();
		listenerList = new EventListenerList();
	}

	public void setListenerList(EventListenerList listenerList) {
		this.listenerList = listenerList;
	}

	public EventListenerList getListenerList() {
		return listenerList;
	}

	public String fireDataSetModified() {
		logger.finest("entering fireDataSetModified()");
		if (newData == null) {
			String message = "ColumnAppendedBroadcaster, fireDataSetModified called without new data";
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest(message);
			}

			return message;
		}
		this.fireDataSetModified(newData);
		String message = "ColumnAppendedBroadcaster, fireDataSetModified called, ok";
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(message);
		}
		return "TRUE";
	}

	public String initData(int nColumns) {
		newData = new Object[nColumns + 1];
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("setting size of data = " + nColumns);
		}

		return "" + nColumns;
	}

	public String setVariableNames(String[] varNames) {
		if (newData == null) {
			newData = new Object[varNames.length];
		}
		String message = "DataSetBroadcaster, setVariableNames, number of names = "
				+ varNames.length;
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(message);
		}
		newData[0] = varNames;
		return "TRUE";
	}

	public String addData(Object data, int index) {

		try {
			newData[index] = data;
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			return "index out of bounds thrown";
		} catch (NullPointerException nulle) {
			nulle.printStackTrace();
			return "null pointer exception thrown";
		}
		String message = "DataSetBroadcaster, addData, index = " + index;
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(message);
		}
		return "TRUE";
	}

	/**
	 * echoStringArray is useful when calling from SJava or the like. It is
	 * expected that an array of strings is the object passed in.
	 * 
	 * @param stringObj
	 *            Object
	 * @return String
	 */
	public String echoStringArray(Object stringObj) {
		String[] strings = null;
		try {
			strings = (String[]) stringObj;
		} catch (ClassCastException cce) {
			return "ClassCastException thrown " + cce.getMessage();
		}
		String returnString = "";
		for (String element : strings) {
			returnString = returnString + "," + element;
		}
		return returnString;
	}

	/**
	 * implements ColumnAppendedListener
	 */
	public void addDataSetModifiedListener(DataSetModifiedListener l) {
		listenerList.add(DataSetModifiedListener.class, l);
		if (logger.isLoggable(Level.FINEST)) {
			System.out
					.println("listenerList.add(ColumnAppendedListener.class, l);");
		}

	}

	/**
	 * removes an ColumnAppendedListener from the button
	 */
	public void removeDataSetModifiedListener(DataSetModifiedListener l) {
		listenerList.remove(DataSetModifiedListener.class, l);
		if (logger.isLoggable(Level.FINEST)) {
			System.out
					.println("listenerList.remove(ColumnAppendedListener.class, l);");
		}

	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	protected void fireDataSetModified(Object[] newData) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		ColumnAppendedEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == DataSetModifiedListener.class) {
				// Lazily create the event:
				((DataSetModifiedListener) listeners[i + 1]).dataSetModified(e);
			}
		}
	}

	public void setNewData(Object[] newData) {
		this.newData = newData;
		this.fireDataSetModified();
	}

	public void dataSetChanged(DataSetEvent arg0) {
		// TODO Auto-generated method stub

	}
}
