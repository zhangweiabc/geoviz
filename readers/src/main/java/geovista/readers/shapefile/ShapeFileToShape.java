/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.readers.shapefile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.readers.example.GeoData48States;

/**
 * This class does nothing now and is slated for removal. All its functionality
 * has been moved to ShapeFileDataReader
 * 
 * This bean (used to) takes a shapefile and turns it into GeneralPath objects.
 * Note that there is a loss of accuracy from double to float upon doing so.
 */
public class ShapeFileToShape implements ActionListener {
    protected final static Logger logger = Logger
	    .getLogger(ShapeFileToShape.class.getName());
    private transient DataSetForApps inputDataSetForApps;
    private transient DataSetForApps outputDataSetForApps;
    private transient EventListenerList listenerList;

    public ShapeFileToShape() {
	super();
	listenerList = new EventListenerList();
    }

    public void setInputDataSetForApps(DataSetForApps inputDataSetForApps) {
	if (inputDataSetForApps != null) {
	    this.inputDataSetForApps = inputDataSetForApps;
	    outputDataSetForApps = inputDataSetForApps;
	    fireActionPerformed("made shapes");
	    fireDataSetChanged(outputDataSetForApps);
	}

    }

    public DataSetForApps getInputDataSetForApps() {
	return inputDataSetForApps;
    }

    public void setOutputDataSetForApps(DataSetForApps outputDataSetForApps) {
	this.outputDataSetForApps = outputDataSetForApps;
    }

    public DataSetForApps getOutputDataSetForApps() {
	return outputDataSetForApps;
    }

    public void setInputDataSet(Object[] inputDataSet) {

	if (inputDataSet != null) {
	    inputDataSetForApps = new DataSetForApps(inputDataSet);
	    outputDataSetForApps = inputDataSetForApps;

	    fireActionPerformed("made shapes");
	    fireDataSetChanged(outputDataSetForApps);
	}

    }

    public Object[] getInputDataSet() {
	return inputDataSetForApps.getDataObjectOriginal();
    }

    public void setOutputDataSet(Object[] outputDataSet) {
	// this.outputDataSet = outputDataSet;
    }

    public Object[] getOutputDataSet() {
	return outputDataSetForApps.getDataObjectOriginal();
    }

    public void setListenerList(EventListenerList listenerList) {
	this.listenerList = listenerList;
    }

    public EventListenerList getListenerList() {
	return listenerList;
    }

    public void actionPerformed(ActionEvent e) {

	if (e.getSource() instanceof GeoData48States) {
	    GeoData48States data = (GeoData48States) e.getSource();
	    setInputDataSet(data.getDataSet());
	}
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
	logger.fine("ShpToShp.fireDataSetChanged, not supposed to use me :(");
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