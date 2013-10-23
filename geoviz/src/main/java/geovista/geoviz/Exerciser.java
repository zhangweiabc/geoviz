/*
 *  This class is designed to provide a thorough work-out for any bean or set of beans that is to be so tested.
 */
package geovista.geoviz;

import java.awt.Component;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JFrame;

import geovista.common.data.DataSetForApps;
import geovista.common.event.ConditioningEvent;
import geovista.common.event.ConditioningListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SpatialExtentEvent;
import geovista.common.event.SpatialExtentListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.coordination.CoordinationManager;
import geovista.readers.example.GeoDataGeneralizedStates;

public class Exerciser {

	CoordinationManager coord;
	ArrayList beans;
	Object beanIn;
	

	// GeoDataGeneralizedStates geoData;
	GeoDataGeneralizedStates geoData;
	final static Logger logger = Logger.getLogger(Exerciser.class.getName());
	static DataSetForApps dsa;

	public enum Event {
		Selection, Indication, DataSet, Subspace, SpatialExtent, ColorArray, Conditioning

	}

	public Exerciser() {
		geoData = new GeoDataGeneralizedStates();
		// geoData = new GeoData2008Election();
		coord = new CoordinationManager();
		beans = new ArrayList();

	}

	/* Add a bean to this harness */
	public void addBean(Object beanIn) {

		coord.addBean(beanIn);
		if (beanIn instanceof DataSetListener) {
			DataSetListener dataListener = (DataSetListener) beanIn;
			if (dsa == null) {
				logger.info("getting data for tests");
				dsa = geoData.getDataForApps();
				takeFirstVars(6);
			}
			dataListener.dataSetChanged(new DataSetEvent(dsa, this));

		}
		beans.add(beanIn);

	}

	void takeFirstVars(int howMany) {
		String[] varNames = new String[howMany];
		Object[] originalDataArrays = new Object[howMany];
		for (int i = 0; i < howMany; i++) {
			varNames[i] = dsa.getAttributeNamesOriginal()[i];
			originalDataArrays[i] = dsa.getDataObjectOriginal()[i + 1];
		}
		Shape[] shapeData = dsa.getShapeData();

		Object[] newDataArray = new Object[howMany + 2];
		newDataArray[0] = varNames;

		for (int i = 0; i < howMany; i++) {
			newDataArray[i + 1] = originalDataArrays[i];

		}
		newDataArray[howMany + 1] = shapeData;
		dsa = new DataSetForApps(newDataArray);

	}

	public void testEvent(Event eventType) {
		logger.finest("go around again! " + eventType);
		switch (eventType) {
		case Selection:
			if (beanIn instanceof SelectionListener) {
				SelectionListener selListener = (SelectionListener) beanIn;
				selListener.selectionChanged(new SelectionEvent(this,
						new int[] { 1, 2, 3 }));
			}
			break;
		case Indication:
			if (beanIn instanceof IndicationListener) {
				IndicationListener indListener = (IndicationListener) beanIn;
				indListener.indicationChanged(new IndicationEvent(this, 1));
			}
			break;

		case Subspace:
			if (beanIn instanceof SubspaceListener) {
				SubspaceListener subListener = (SubspaceListener) beanIn;
				subListener.subspaceChanged(new SubspaceEvent(this, new int[] {
						1, 2, 3 }));
			}
			break;

		case SpatialExtent:
			if (beanIn instanceof SpatialExtentListener) {
				SpatialExtentListener spaceListener = (SpatialExtentListener) beanIn;
				spaceListener.spatialExtentChanged(new SpatialExtentEvent(this,
						geoData.getDataForApps().getShapeData()[1]
								.getBounds2D()));
			}
			break;

		// skip color array for now, going to change to brewer colors...
		case Conditioning:
			if (beanIn instanceof ConditioningListener) {
				ConditioningListener selListener = (ConditioningListener) beanIn;
				selListener.conditioningChanged(new ConditioningEvent(this,
						new int[] { 1, 2, 3 }));
			}
			break;
		}
	}

	public void testAllEvents() {
		for (Event e : Event.values()) {
			testEvent(e);
		}
	}

	public void testGUIAndEvents(Component comp) {
		JFrame app = new JFrame();
		app.add(comp);
		app.setVisible(true);
		app.pack();
		addBean(comp);
		testAllEvents();
		app.setVisible(false);
		comp = null;
		app = null;

	}
}