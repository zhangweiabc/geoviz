/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.readers.csv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;

public class CSVFileDataReader {

	public static final String COMMAND_DATA_SET_MADE = "dataMade";

	private DataSetForApps dataForApps;
	private String fileName;
	private final EventListenerList listenerList;
	protected final static Logger logger = Logger
			.getLogger(CSVFileDataReader.class.getName());

	public CSVFileDataReader() {
		listenerList = new EventListenerList();
	}

	private DataSetForApps makeDataSetForApps(String fileName) {

		String csvFileName = fileName + ".csv";
		// InputStream isCSV = cl.getResourceAsStream("resources/states48.csv");
		Object[] csvData = null;

		try {

			GeogCSVReader csv = new GeogCSVReader();
			FileInputStream inStream = new FileInputStream(csvFileName);
			csvData = csv.readFile(inStream);
			// shpData = new Object[dbData.length + 1];
			// for (int i = 0; i < dbData.length; i++) {
			// shpData[i] = dbData[i];
			// }
			// shpData[dbData.length] = new ShapeFile(fileName + ".shp");
		} catch (RuntimeException re) {
			// Add by Jin Chen: should always process excpetions after catching
			// it. If not, throw it again to let other program process it.
			throw re;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// this.fireActionPerformed(COMMAND_DATA_SET_MADE);
		dataForApps = new DataSetForApps(csvData);

		return dataForApps;
	}

	private String removeExtension(String fileName) {
		String removed = fileName;
		int index = fileName.lastIndexOf(".");
		if (index > -1) { // if it was found
			removed = fileName.substring(0, index);
		}
		return removed;
	}

	public void setDataForApps(DataSetForApps dataForApps) {
		this.dataForApps = dataForApps;
	}

	public DataSetForApps getDataForApps() {
		return dataForApps;
	}

	public Object[] getDataSet() {
		// System.out.print("get data set...");
		return dataForApps.getDataObjectOriginal();
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
		this.fileName = removeExtension(fileName);
		logger.finest(fileName);
		dataForApps = makeDataSetForApps(this.fileName);
		fireActionPerformed(COMMAND_DATA_SET_MADE);
		fireDataSetChanged(dataForApps);

	}

	public void setInputStream(InputStream stream) {
		Object[] csvData = null;
		try {

			GeogCSVReader csv = new GeogCSVReader();

			csvData = csv.readFile(stream);
			// shpData = new Object[dbData.length + 1];
			// for (int i = 0; i < dbData.length; i++) {
			// shpData[i] = dbData[i];
			// }
			// shpData[dbData.length] = new ShapeFile(fileName + ".shp");
		} catch (RuntimeException re) {
			// Add by Jin Chen: should always process excpetions after catching
			// it. If not, throw it again to let other program process it.
			throw re;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// this.fireActionPerformed(COMMAND_DATA_SET_MADE);
		dataForApps = new DataSetForApps(csvData);
		fireActionPerformed(COMMAND_DATA_SET_MADE);
		fireDataSetChanged(dataForApps);

	}

	public String getFileName() {
		return fileName;
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
	}

	/**
	 * implements ActionListener
	 */
	public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class, l);
	}

	/**
	 * removes an ActionListener from the button
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
	protected void fireActionPerformed(String command) {
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
		}
	}

	/**
	 * implements DataSetListener
	 */
	public void addDataSetListener(DataSetListener l) {
		listenerList.add(DataSetListener.class, l);
	}

	/**
	 * removes an DataSetListener from the button
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

}