/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.common.event;

import geovista.common.data.DataSetForApps;

import java.util.EventObject;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * An DataSetEvent signals that a new data set is available.
 * 
 */
public class DataSetEvent extends EventObject {

    private final Object[] dataSet;
    private transient DataSetForApps dataSetForApps;
    private HashMap<String, String> metaData;
    private Object sTempoEventAccessor;

    final static Logger logger = Logger.getLogger(DataSetEvent.class.getName());

    /**
     * The constructor is the same as that for EventObject, except that the
     * dataSet values are indicated.
     */
    @Deprecated
    public DataSetEvent(Object source, Object[] dataSet) {
	super(source);
	this.dataSet = dataSet;
    }

    /**
     * Note that the DataSetForApps is first. This allows us to pass in a null
     * DataSetForApps if desired, without being confused with the previous cntr.
     */
    public DataSetEvent(DataSetForApps dataSetForApps, Object source) {
	super(source);
	this.dataSetForApps = dataSetForApps;
	dataSet = dataSetForApps.getDataObjectOriginal();
    }

    /**
     * Supports .
     */
    public DataSetEvent(DataSetForApps dataSetForApps, Object source,
	    Object sTempoDataAccessor) {
	super(source);
	this.dataSetForApps = dataSetForApps;
	dataSet = dataSetForApps.getDataObjectOriginal();
	this.sTempoEventAccessor = sTempoDataAccessor;
    }

    public Object[] getDataSet() {
	return dataSet;
    }

    public DataSetForApps getDataSetForApps() {
	// lazily initialize
	if (dataSetForApps == null) {
	    dataSetForApps = new DataSetForApps(dataSet);
	}
	return dataSetForApps;
    }

    public Object getMetaData() {
	if (metaData == null) {
	    metaData = new HashMap<String, String>();
	}
	return metaData;
    }

    public Object getSpaceTimeEventAccessor() {
	if (this.sTempoEventAccessor == null) {
	    logger.severe("no space time event accessor");
	    new Throwable().printStackTrace();
	}
	return this.sTempoEventAccessor;
    }

    public void setMetaData(HashMap<String, String> metaData) {
	this.metaData = metaData;
    }
}
