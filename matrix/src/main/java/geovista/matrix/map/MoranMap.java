/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.matrix.map;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.data.DataSetBroadcaster;
import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.data.SpatialStatistics;
import geovista.common.data.SpatialWeights;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.PaletteEvent;
import geovista.common.event.PaletteListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SpatialExtentEvent;
import geovista.common.event.SpatialExtentListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.ui.VariablePicker;
import geovista.coordination.CoordinationManager;
import geovista.geoviz.map.GeoMap;
import geovista.geoviz.map.GeoMapUni;
import geovista.geoviz.scatterplot.SingleHistogram;
import geovista.geoviz.scatterplot.SingleScatterPlot;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.symbolization.event.ColorClassifierEvent;
import geovista.symbolization.event.ColorClassifierListener;

/**
 * A Moran Map has a choropleth varSigMap and a scatterplot tied together
 */
public class MoranMap extends JPanel implements SelectionListener,
		IndicationListener, DataSetListener, ColorClassifierListener,
		SpatialExtentListener, PaletteListener, TableModelListener,
		ActionListener, ListSelectionListener, SubspaceListener {
	protected final static Logger logger = Logger.getLogger(MoranMap.class
			.getName());

	GeoMapUni varMap;
	GeoMapUni moranMap;
	GeoMapUni sigMap;
	GeoMap varSigMap;
	SingleHistogram varHist;
	SingleHistogram moranHist;
	SingleHistogram sigHist;
	SingleScatterPlot varSigPlot;

	SingleScatterPlot sp;
	DataSetForApps dataSetOriginal;
	DataSetForApps dataSetZ;
	DataSetForApps dataSetMoran;
	SpatialWeights spatialWeights;
	JList varList;
	JButton sendButt;
	int monteCarloIterations = 1000;
	DataSetBroadcaster dataCaster;

	VariablePicker varPicker;

	CoordinationManager coord;

	public MoranMap() {
		super();

		varMap = new GeoMapUni();
		moranMap = new GeoMapUni();
		sigMap = new GeoMapUni();
		varSigMap = new GeoMap();
		varHist = new SingleHistogram();
		moranHist = new SingleHistogram();
		sigHist = new SingleHistogram();
		varSigPlot = new SingleScatterPlot();

		varPicker = new VariablePicker();

		dataCaster = new DataSetBroadcaster();

		coord = new CoordinationManager();

		coord.addBean(dataCaster);
		coord.addBean(varMap);
		coord.addBean(moranMap);
		coord.addBean(sigMap);
		coord.addBean(varSigMap);
		coord.addBean(varHist);
		coord.addBean(moranHist);
		coord.addBean(sigHist);
		coord.addBean(varSigPlot);
		coord.addBean(varPicker);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(2, 4));
		mainPanel.add(varMap);
		mainPanel.add(moranMap);
		mainPanel.add(sigMap);
		mainPanel.add(varSigMap);
		mainPanel.add(varHist);
		mainPanel.add(moranHist);
		mainPanel.add(sigHist);
		mainPanel.add(varSigPlot);

		mainPanel.setPreferredSize(new Dimension(1000, 400));
		// BoxLayout box = new BoxLayout(this, BoxLayout.X_AXIS);
		// varSigMap = new GeoMap();
		// sp = new SingleScatterPlot();
		// varSigMap.addSelectionListener(sp);
		// sp.addSelectionListener(varSigMap);
		// varSigMap.addIndicationListener(sp);
		// sp.addIndicationListener(varSigMap);
		// setLayout(box);
		Dimension prefSize = new Dimension(200, 200);
		varSigMap.setMaximumSize(prefSize);
		varHist.setMaximumSize(prefSize);
		// LineBorder border = (LineBorder) BorderFactory
		// .createLineBorder(Color.black);
		// varSigMap.setBorder(border);
		// sp.setBorder(border);
		setLayout(new BorderLayout());
		this.add(mainPanel);
		// this.add(sp);

		JPanel varPanel = new JPanel();

		varList = new JList();
		varList.addListSelectionListener(this);
		sendButt = new JButton("Pick Variable");
		varList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sendButt.addActionListener(this);
		varPanel.setLayout(new BorderLayout());
		varPanel.add(varList, BorderLayout.CENTER);
		// varPanel.add(varPicker, BorderLayout.CENTER);
		varPanel.add(sendButt, BorderLayout.SOUTH);

		this.add(varPanel, BorderLayout.WEST);

	}

	public void selectionChanged(SelectionEvent e) {

		if (coord.containsBean(e.getSource())) {
			fireSelectionChanged(e);
		}
		varSigMap.selectionChanged(e);
		if (sp != null) {
			sp.selectionChanged(e);
		}

	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, varSigMap.getSelectedObservations());
	}

	public void indicationChanged(IndicationEvent e) {
		varMap.indicationChanged(e);
		moranMap.indicationChanged(e);
		sigMap.indicationChanged(e);
		varSigMap.indicationChanged(e);
		varHist.indicationChanged(e);
		moranHist.indicationChanged(e);
		sigHist.indicationChanged(e);
		varSigPlot.indicationChanged(e);

	}

	public void dataSetChanged(DataSetEvent e) {
		coord.removeBean(this);
		// XXX NGA demo hack
		if (e.getSource().getClass().getName()
				.equals("geovista.toolkitcore.GeoVizToolkit") == false) {
			coord.addBean(this);
			return;
		}
		dataSetOriginal = e.getDataSetForApps();
		String[] varNames = dataSetOriginal.getAttributeNamesNumeric();
		spatialWeights = dataSetOriginal.getSpatialWeights();
		varList.setListData(varNames);
		varList.setSelectedIndex(0);
		varPicker.dataSetChanged(e);
		coord.addBean(this);
	}

	public void colorClassifierChanged(ColorClassifierEvent e) {
		// TODO Auto-generated method stub

	}

	SpatialExtentEvent savedEvent;

	public SpatialExtentEvent getSpatialExtentEvent() {
		return savedEvent;
	}

	public void spatialExtentChanged(SpatialExtentEvent e) {
		savedEvent = e;
		// TODO Auto-generated method stub

	}

	public void paletteChanged(PaletteEvent e) {
		// TODO Auto-generated method stub

	}

	public void tableChanged(TableModelEvent e) {
		// TODO Auto-generated method stub

	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == sendButt) {
			if (varList.getSelectedIndex() < 0) {
				return;
			}

			dataSetOriginal.addColumn((String) varList.getSelectedValue(),
					dataSetMoran.getNumericDataAsDouble(varList
							.getSelectedIndex()));
		}

	}

	public static void main(String[] args) {

		MoranMap varSigMap = new MoranMap();
		double[] vals = { 0, 1, 2, 3 };
		double along = DescriptiveStatistics.percentAbove(vals, 2.9);
		logger.info("" + along);

		JFrame frame = new JFrame("Moran Map");
		frame.add(varSigMap);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GeoDataGeneralizedStates geodata = new GeoDataGeneralizedStates();
		DataSetEvent e = new DataSetEvent(geodata.getDataForApps(), geodata);
		varSigMap.dataSetChanged(e);

	}

	public void valueChanged(ListSelectionEvent e) {
		if (dataSetOriginal == null) {
			return;
		}

		if (e.getSource().equals(varList)) {
			int whichItem = varList.getSelectedIndex();
			newVariableSelected(whichItem);

		}

	}

	private void newVariableSelected(int whichItem) {
		if (whichItem < 0) {
			return;
		}
		double[] newData = dataSetOriginal.getNumericDataAsDouble(whichItem);

		String itemName = (String) varList.getSelectedValue();

		double[] zData = DescriptiveStatistics.calculateZScores(newData);
		String zName = "Z_" + itemName;
		double[] moranData = SpatialStatistics.calculateMoranScores(zData,
				spatialWeights);
		String moranName = "Moran_" + itemName;
		double[] monteCarloData = SpatialStatistics.findPValues(zData,
				monteCarloIterations, spatialWeights);
		String pName = "P_" + itemName;
		String[] resultNames = { itemName, zName, moranName, pName };
		Object[] dataSetObject = { resultNames, newData, zData, moranData,
				monteCarloData, dataSetOriginal.getShapeData() };

		dataCaster.setAndFireDataSet(dataSetObject);
		moranHist.setSelectedVariable(2);
		sigHist.setSelectedVariable(3);
		moranMap.setSelectedVariable(2);
		sigMap.setSelectedVariable(3);
	}

	@Override
	public void subspaceChanged(SubspaceEvent e) {
		int[] vars = e.getSubspace();
		int firstVar = vars[0];
		varList.setSelectedIndex(firstVar);
		// newVariableSelected(firstVar);

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
	private void fireSelectionChanged(SelectionEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {

			((SelectionListener) listeners[i + 1]).selectionChanged(e);

		}

		// next i
	}

}
